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

import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilter;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.DirectoryUtils;

/**
 * Support class for {@link RemoteRepositoryFilterSource} implementations.
 * <p>
 * Support class for implementing {@link RemoteRepositoryFilterSource}. It implements basic support
 * like optional "basedir" calculation, handling of "enabled" flag.
 * <p>
 * The configuration keys supported:
 * <ul>
 *     <li><pre>heimdall.${id}.enabled</pre> (boolean) make possible to disable given filter (defaults to {@code true})</li>
 *     <li><pre>heimdall.${id}.basedir</pre> (string, path) directory from where implementation
 *     can use files. If unset, default value is ".remoteRepositoryFilters/${id}" and is resolved from local
 *     repository basedir.</li>
 * </ul>
 *
 * @since 1.9.0
 */
public abstract class RemoteRepositoryFilterSourceSupport extends ComponentSupport
        implements RemoteRepositoryFilterSource {
    private static final String CONFIG_PROP_PREFIX = "heimdall.";

    private static final String CONF_NAME_BASEDIR = "basedir";

    static final String LOCAL_REPO_PREFIX_DIR = ".remoteRepositoryFilters";

    private final String name;

    protected RemoteRepositoryFilterSourceSupport(String name) {
        this.name = requireNonNull(name);
    }

    /**
     * Utility method to create scoped configuration property key of given name.
     */
    protected String configPropKey(String name) {
        return CONFIG_PROP_PREFIX + this.name + "." + name;
    }

    /**
     * Returns enabled state of filter.
     * <p>
     * Default is {@code true}.
     */
    protected boolean isEnabled(RepositorySystemSession session) {
        return ConfigUtils.getBoolean(session, true, CONFIG_PROP_PREFIX + this.name);
    }

    /**
     * Uses common {@link DirectoryUtils#resolveDirectory(RepositorySystemSession, String, String, boolean)} to
     * calculate (and maybe create) basedir for this implementation, never returns {@code null}. The returned
     * {@link Path} may not exists, if invoked with {@code mayCreate} being {@code false}.
     * <p>
     * Default value is {@code ${LOCAL_REPOSITORY}/.checksums}.
     *
     * @return The {@link Path} of basedir, never {@code null}.
     */
    protected Path getBasedir(RepositorySystemSession session, boolean mayCreate) {
        try {
            return DirectoryUtils.resolveDirectory(
                    session, LOCAL_REPO_PREFIX_DIR, configPropKey(CONF_NAME_BASEDIR), mayCreate);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Simple {@link RemoteRepositoryFilter.Result} immutable implementation.
     */
    protected static class SimpleResult implements RemoteRepositoryFilter.Result {
        private final boolean accepted;

        private final String reasoning;

        public SimpleResult(boolean accepted, String reasoning) {
            this.accepted = accepted;
            this.reasoning = requireNonNull(reasoning);
        }

        @Override
        public boolean isAccepted() {
            return accepted;
        }

        @Override
        public String reasoning() {
            return reasoning;
        }
    }
}
