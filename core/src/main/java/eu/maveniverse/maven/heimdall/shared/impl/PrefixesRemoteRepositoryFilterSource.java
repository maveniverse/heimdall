/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package eu.maveniverse.maven.heimdall.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.heimdall.shared.Session;
import eu.maveniverse.maven.heimdall.shared.SessionUtils;
import eu.maveniverse.maven.heimdall.shared.impl.ruletree.PrefixTree;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilter;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.transfer.NoRepositoryLayoutException;

/**
 * Remote repository filter source filtering on path prefixes. It is backed by a file that lists all allowed path
 * prefixes from remote repository. Artifact that layout converted path (using remote repository layout) results in
 * path with no corresponding prefix present in this file is filtered out.
 * <p>
 * The file can be authored manually: format is one prefix per line, comments starting with "#" (hash) and empty lines
 * for structuring are supported, The "/" (slash) character is used as file separator. Some remote repositories and
 * MRMs publish these kind of files, they can be downloaded from corresponding URLs.
 * <p>
 * The prefix file is expected on path "${basedir}/prefixes-${repository.id}.txt".
 * <p>
 * The prefixes file is once loaded and cached, so in-flight prefixes file change during component existence are not
 * noticed.
 * <p>
 * Examples of published prefix files:
 * <ul>
 *     <li>Central: <a href="https://repo.maven.apache.org/maven2/.meta/prefixes.txt">prefixes.txt</a></li>
 *     <li>Apache Releases:
 *     <a href="https://repository.apache.org/content/repositories/releases/.meta/prefixes.txt">prefixes.txt</a></li>
 * </ul>
 *
 * @since 1.9.0
 */
@Singleton
@Named
public final class PrefixesRemoteRepositoryFilterSource extends RemoteRepositoryFilterSourceSupport {
    public static final String NAME = "prefixes";

    static final String PREFIXES_FILE_PREFIX = "prefixes-";

    static final String PREFIXES_FILE_SUFFIX = ".txt";

    private static final String PREFIX_FILE_PATH = ".meta/prefixes.txt";

    private final RepositorySystem repositorySystem;

    private final RepositoryLayoutProvider repositoryLayoutProvider;

    private final ConcurrentHashMap<RemoteRepository, PrefixTree> prefixes;

    private final ConcurrentHashMap<RemoteRepository, RepositoryLayout> layouts;

    @Inject
    public PrefixesRemoteRepositoryFilterSource(
            RepositorySystem repositorySystem, RepositoryLayoutProvider repositoryLayoutProvider) {
        super(NAME);
        this.repositorySystem = requireNonNull(repositorySystem);
        this.repositoryLayoutProvider = requireNonNull(repositoryLayoutProvider);
        this.prefixes = new ConcurrentHashMap<>();
        this.layouts = new ConcurrentHashMap<>();
    }

    @Override
    public RemoteRepositoryFilter getRemoteRepositoryFilter(RepositorySystemSession session) {
        Optional<Session> so = SessionUtils.mayGetSession(session);
        if (so.isPresent() && isEnabled(session)) {
            return new PrefixesFilter(so.orElseThrow(J8Utils.OET), session, getBasedir(session, false));
        }
        return null;
    }

    /**
     * Caches layout instances for remote repository. In case of unknown layout it returns {@code null}.
     *
     * @return the layout instance of {@code null} if layout not supported.
     */
    private RepositoryLayout cacheLayout(RepositorySystemSession session, RemoteRepository remoteRepository) {
        return layouts.computeIfAbsent(remoteRepository, r -> {
            try {
                return repositoryLayoutProvider.newRepositoryLayout(session, remoteRepository);
            } catch (NoRepositoryLayoutException e) {
                return null;
            }
        });
    }

    private final ConcurrentHashMap<RemoteRepository, Boolean> ongoingUpdates = new ConcurrentHashMap<>();

    /**
     * Caches prefixes instances for remote repository.
     */
    private PrefixTree cacheNode(RepositorySystemSession session, Path basedir, RemoteRepository remoteRepository) {
        if (!remoteRepository.isBlocked() && null == ongoingUpdates.putIfAbsent(remoteRepository, Boolean.TRUE)) {
            try {
                return prefixes.computeIfAbsent(
                        remoteRepository, r -> loadRepositoryPrefixes(session, basedir, remoteRepository));
            } finally {
                ongoingUpdates.remove(remoteRepository);
            }
        }
        return PrefixTree.SENTINEL;
    }

    /**
     * Loads prefixes file and preprocesses it into {@link PrefixTree} instance.
     */
    private PrefixTree loadRepositoryPrefixes(
            RepositorySystemSession session, Path baseDir, RemoteRepository remoteRepository) {
        Path filePath = resolvePrefixesFromRemoteRepository(session, remoteRepository);
        if (filePath == null) {
            filePath = baseDir.resolve(PREFIXES_FILE_PREFIX + remoteRepository.getId() + PREFIXES_FILE_SUFFIX);
        }
        if (Files.isReadable(filePath)) {
            logger.debug(
                    "Loading prefixes for remote repository {} from file '{}'", remoteRepository.getId(), filePath);
            try (Stream<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)) {
                PrefixTree prefixTree = new PrefixTree("");
                int rules = prefixTree.loadNodes(lines);
                logger.info("Heimdall loaded {} prefixes for remote repository {}", rules, remoteRepository.getId());
                return prefixTree;
            } catch (FileNotFoundException e) {
                // strange: we tested for it above, still, we should not fail
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        logger.debug("Prefix file for remote repository {} not found at '{}'", remoteRepository, filePath);
        return PrefixTree.SENTINEL;
    }

    private Path resolvePrefixesFromRemoteRepository(
            RepositorySystemSession session, RemoteRepository remoteRepository) {
        MetadataRequest request =
                new MetadataRequest(new DefaultMetadata(PREFIX_FILE_PATH, Metadata.Nature.RELEASE_OR_SNAPSHOT));
        request.setRepository(remoteRepository);
        request.setDeleteLocalCopyIfMissing(true);
        request.setFavorLocalRepository(true);
        MetadataResult result = repositorySystem
                .resolveMetadata(session, Collections.singleton(request))
                .get(0);
        if (result.isResolved()) {
            return result.getMetadata().getFile().toPath();
        } else {
            return null;
        }
    }

    private class PrefixesFilter implements RemoteRepositoryFilter {
        private final Session session;
        private final RepositorySystemSession repoSession;
        private final Path basedir;

        private PrefixesFilter(Session session, RepositorySystemSession repoSession, Path basedir) {
            this.session = session;
            this.repoSession = repoSession;
            this.basedir = basedir;
        }

        @Override
        public Result acceptArtifact(RemoteRepository remoteRepository, Artifact artifact) {
            RepositoryLayout repositoryLayout = cacheLayout(repoSession, remoteRepository);
            if (repositoryLayout == null) {
                return new SimpleResult(true, "Unsupported layout: " + remoteRepository);
            }
            return acceptPrefix(
                    remoteRepository,
                    repositoryLayout.getLocation(artifact, false).getPath());
        }

        @Override
        public Result acceptMetadata(RemoteRepository remoteRepository, Metadata metadata) {
            RepositoryLayout repositoryLayout = cacheLayout(repoSession, remoteRepository);
            if (repositoryLayout == null) {
                return new SimpleResult(true, "Unsupported layout: " + remoteRepository);
            }
            return acceptPrefix(
                    remoteRepository,
                    repositoryLayout.getLocation(metadata, false).getPath());
        }

        private Result acceptPrefix(RemoteRepository remoteRepository, String path) {
            if (!isEnabled(repoSession)) {
                return NOT_PRESENT_RESULT;
            }
            PrefixTree root = cacheNode(repoSession, basedir, remoteRepository);
            if (PrefixTree.SENTINEL == root) {
                return NOT_PRESENT_RESULT;
            }
            if (root.acceptedPath(path)) {
                return new SimpleResult(true, "Prefix " + path + " allowed from " + remoteRepository);
            } else {
                return new SimpleResult(false, "Prefix " + path + " NOT allowed from " + remoteRepository);
            }
        }
    }

    private static final RemoteRepositoryFilter.Result NOT_PRESENT_RESULT =
            new SimpleResult(true, "Prefix rules not present");
}
