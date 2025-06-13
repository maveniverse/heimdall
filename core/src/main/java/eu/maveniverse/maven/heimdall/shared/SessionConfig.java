/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.heimdall.shared.impl.J8Utils;
import eu.maveniverse.maven.shared.core.fs.FileUtils;
import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Session config holds all the session related data.
 */
public interface SessionConfig {
    String NAME = "heimdall";

    String KEY_PREFIX = NAME + ".";

    String UNKNOWN_VERSION = "unknown";

    /**
     * Configuration key in properties (system, user or project) to enable/disable Njord. Defaults to {@code true}.
     */
    String CONFIG_ENABLED = KEY_PREFIX + "enabled";

    /**
     * Is Njord enabled? If this method returns {@code false}, Njord will step aside (like it was not loaded).
     */
    boolean enabled();

    /**
     * Returns the Njord version.
     */
    String version();

    /**
     * Njord basedir, where all the config and locally staged repositories are.
     */
    Path basedir();

    /**
     * The property path to load, defaults to {@code properties} file in {@link #basedir()}.
     */
    Path propertiesPath();

    /**
     * Properties defined in {@link #propertiesPath()} properties file.
     */
    Map<String, String> heimdallProperties();

    /**
     * User properties set in environment.
     */
    Map<String, String> userProperties();

    /**
     * System properties set in environment.
     */
    Map<String, String> systemProperties();

    /**
     * Effective properties that should be used to get configuration from (applies precedence).
     */
    Map<String, String> effectiveProperties();

    /**
     * Resolver session, never {@code null}.
     */
    RepositorySystemSession session();

    /**
     * Remote repositories provided by environment, never {@code null}.
     */
    List<RemoteRepository> remoteRepositories();

    /**
     * Remote repositories provided by environment and project, if present, never {@code null}.
     */
    List<RemoteRepository> allRemoteRepositories();

    /**
     * Returns this instance as builder.
     */
    default Builder toBuilder() {
        return new Builder(
                enabled(),
                version(),
                basedir(),
                propertiesPath(),
                userProperties(),
                systemProperties(),
                session(),
                remoteRepositories());
    }

    /**
     * Creates builder with defaults.
     */
    static Builder defaults(RepositorySystemSession session, List<RemoteRepository> remoteRepositories) {
        requireNonNull(session, "session");
        requireNonNull(remoteRepositories, "remoteRepositories");
        return new Builder(
                null,
                MavenUtils.discoverArtifactVersion(
                        SessionConfig.class.getClassLoader(), "eu.maveniverse.maven.heimdall", "core", UNKNOWN_VERSION),
                null,
                null,
                session.getUserProperties(),
                session.getSystemProperties(),
                session,
                remoteRepositories);
    }

    class Builder {
        private Boolean enabled;
        private final String version;
        private Path basedir;
        private Path propertiesPath;
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;
        private RepositorySystemSession session;
        private List<RemoteRepository> remoteRepositories;

        public Builder(
                Boolean enabled,
                String version,
                Path basedir,
                Path propertiesPath,
                Map<String, String> userProperties,
                Map<String, String> systemProperties,
                RepositorySystemSession session,
                List<RemoteRepository> remoteRepositories) {
            this.enabled = enabled;
            this.version = version;
            this.basedir = basedir;
            this.propertiesPath = propertiesPath;
            this.userProperties = userProperties;
            this.systemProperties = systemProperties;
            this.session = session;
            this.remoteRepositories = remoteRepositories;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder basedir(Path basedir) {
            this.basedir = requireNonNull(basedir);
            return this;
        }

        public Builder propertiesPath(Path propertiesPath) {
            this.propertiesPath = requireNonNull(propertiesPath);
            return this;
        }

        public Builder userProperties(Map<String, String> userProperties) {
            this.userProperties = requireNonNull(userProperties);
            return this;
        }

        public Builder systemProperties(Map<String, String> systemProperties) {
            this.systemProperties = requireNonNull(systemProperties);
            return this;
        }

        public Builder session(RepositorySystemSession session) {
            this.session = requireNonNull(session);
            return this;
        }

        public Builder remoteRepositories(List<RemoteRepository> remoteRepositories) {
            this.remoteRepositories = requireNonNull(remoteRepositories);
            return this;
        }

        public SessionConfig build() {
            return new Impl(
                    enabled,
                    version,
                    basedir,
                    propertiesPath,
                    userProperties,
                    systemProperties,
                    session,
                    remoteRepositories);
        }

        private static class Impl implements SessionConfig {
            private final boolean enabled;
            private final String version;
            private final Path basedir;
            private final Path propertiesPath;
            private final Map<String, String> heimdallProperties;
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;
            private final RepositorySystemSession session;
            private final List<RemoteRepository> remoteRepositories;
            private final List<RemoteRepository> allRemoteRepositories;

            private Impl(
                    Boolean enabled,
                    String version,
                    Path basedir,
                    Path propertiesPath,
                    Map<String, String> userProperties,
                    Map<String, String> systemProperties,
                    RepositorySystemSession session,
                    List<RemoteRepository> remoteRepositories) {
                this.version = requireNonNull(version, "version");

                this.basedir = basedir == null
                        ? FileUtils.discoverBaseDirectory("heimdall.basedir", ".heimdall")
                        : FileUtils.canonicalPath(basedir);
                if (!Files.isDirectory(this.basedir)) {
                    try {
                        Files.createDirectories(this.basedir);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Cannot create basedir", e);
                    }
                }
                this.propertiesPath = propertiesPath == null
                        ? this.basedir.resolve("heimdall.properties")
                        : FileUtils.canonicalPath(this.basedir.resolve(propertiesPath));

                Properties njordProperties = new Properties();
                if (Files.isRegularFile(this.propertiesPath)) {
                    try (InputStream inputStream = Files.newInputStream(this.propertiesPath)) {
                        njordProperties.load(inputStream);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                this.heimdallProperties = MavenUtils.toMap(njordProperties);
                this.userProperties = J8Utils.copyOf(requireNonNull(userProperties, "userProperties"));
                this.systemProperties = J8Utils.copyOf(requireNonNull(systemProperties, "systemProperties"));
                HashMap<String, String> eff = new HashMap<>();
                eff.putAll(this.systemProperties);
                eff.putAll(this.heimdallProperties);
                eff.putAll(this.userProperties);
                this.effectiveProperties = J8Utils.copyOf(eff);

                this.enabled = enabled != null
                        ? enabled
                        : Boolean.parseBoolean(
                                effectiveProperties.getOrDefault(CONFIG_ENABLED, Boolean.TRUE.toString()));
                this.session = requireNonNull(session);
                this.remoteRepositories = J8Utils.copyOf(requireNonNull(remoteRepositories));
                ArrayList<RemoteRepository> arr = new ArrayList<>(remoteRepositories);
                this.allRemoteRepositories = J8Utils.copyOf(arr);
            }

            @Override
            public boolean enabled() {
                return enabled;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public Path basedir() {
                return basedir;
            }

            @Override
            public Path propertiesPath() {
                return propertiesPath;
            }

            @Override
            public Map<String, String> heimdallProperties() {
                return heimdallProperties;
            }

            @Override
            public Map<String, String> userProperties() {
                return userProperties;
            }

            @Override
            public Map<String, String> systemProperties() {
                return systemProperties;
            }

            @Override
            public Map<String, String> effectiveProperties() {
                return effectiveProperties;
            }

            @Override
            public RepositorySystemSession session() {
                return session;
            }

            @Override
            public List<RemoteRepository> remoteRepositories() {
                return remoteRepositories;
            }

            @Override
            public List<RemoteRepository> allRemoteRepositories() {
                return allRemoteRepositories;
            }
        }
    }
}
