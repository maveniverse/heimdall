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

import eu.maveniverse.maven.heimdall.shared.Session;
import eu.maveniverse.maven.heimdall.shared.SessionUtils;
import eu.maveniverse.maven.heimdall.shared.impl.ruletree.GroupTree;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilter;

/**
 * Remote repository filter source filtering on G coordinate. It is backed by a file that lists all allowed groupIds
 * and groupId not present in this file are filtered out.
 * <p>
 * The file can be authored manually: format is one groupId per line, comments starting with "#" (hash) amd empty lines
 * for structuring are supported. The file can also be pre-populated by "record" functionality of this filter.
 * When "recording", this filter will not filter out anything, but will instead populate the file with all encountered
 * groupIds.
 * <p>
 * The groupId file is expected on path "${basedir}/groupId-${repository.id}.txt".
 * <p>
 * The groupId file once loaded are cached in component, so in-flight groupId file change during component existence
 * are NOT noticed.
 *
 * @since 1.9.0
 */
@Singleton
@Named
public final class GroupIdRemoteRepositoryFilterSource extends RemoteRepositoryFilterSourceSupport {
    public static final String NAME = "groupId";

    static final String GROUP_ID_FILE_PREFIX = "groupId-";

    static final String GROUP_ID_FILE_SUFFIX = ".txt";

    private final ConcurrentHashMap<RemoteRepository, GroupTree> rules;

    @Inject
    public GroupIdRemoteRepositoryFilterSource() {
        super(NAME);
        this.rules = new ConcurrentHashMap<>();
    }

    @Override
    public RemoteRepositoryFilter getRemoteRepositoryFilter(RepositorySystemSession session) {
        Optional<Session> so = SessionUtils.mayGetSession(session);
        if (so.isPresent() && isEnabled(session)) {
            return new GroupIdFilter(so.orElseThrow(J8Utils.OET), session);
        }
        return null;
    }

    /**
     * Returns the groupId path. The file and parents may not exist, this method merely calculate the path.
     */
    private Path filePath(Path basedir, String remoteRepositoryId) {
        return basedir.resolve(GROUP_ID_FILE_PREFIX + remoteRepositoryId + GROUP_ID_FILE_SUFFIX);
    }

    private GroupTree cacheRules(RepositorySystemSession session, RemoteRepository remoteRepository) {
        return rules.computeIfAbsent(remoteRepository, r -> loadRepositoryRules(session, r));
    }

    private GroupTree loadRepositoryRules(RepositorySystemSession session, RemoteRepository remoteRepository) {
        Path filePath = filePath(getBasedir(session, false), remoteRepository.getId());
        if (Files.isReadable(filePath)) {
            try (Stream<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)) {
                GroupTree groupTree = new GroupTree("");
                int rules = groupTree.loadNodes(lines);
                logger.info("Heimdall loaded {} group rules for remote repository {}", rules, remoteRepository.getId());
                return groupTree;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return GroupTree.SENTINEL;
    }

    private class GroupIdFilter implements RemoteRepositoryFilter {
        private final Session session;
        private final RepositorySystemSession repoSession;

        private GroupIdFilter(Session session, RepositorySystemSession repoSession) {
            this.session = session;
            this.repoSession = repoSession;
        }

        @Override
        public Result acceptArtifact(RemoteRepository remoteRepository, Artifact artifact) {
            return acceptGroupId(remoteRepository, artifact.getGroupId());
        }

        @Override
        public Result acceptMetadata(RemoteRepository remoteRepository, Metadata metadata) {
            return acceptGroupId(remoteRepository, metadata.getGroupId());
        }

        private Result acceptGroupId(RemoteRepository remoteRepository, String groupId) {
            GroupTree groupIds = cacheRules(repoSession, remoteRepository);
            if (GroupTree.SENTINEL == groupIds) {
                return NOT_PRESENT_RESULT;
            }

            if (groupIds.acceptedGroupId(groupId)) {
                return new SimpleResult(true, "G:" + groupId + " allowed from " + remoteRepository);
            } else {
                return new SimpleResult(false, "G:" + groupId + " NOT allowed from " + remoteRepository);
            }
        }
    }

    private static final RemoteRepositoryFilter.Result NOT_PRESENT_RESULT =
            new SimpleResult(true, "GroupId rules not present");
}
