/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.extension3;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.heimdall.shared.Session;
import eu.maveniverse.maven.heimdall.shared.SessionConfig;
import eu.maveniverse.maven.heimdall.shared.SessionFactory;
import eu.maveniverse.maven.heimdall.shared.SessionUtils;
import eu.maveniverse.maven.heimdall.shared.impl.J8Utils;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle participant that creates Heimdall session.
 */
@Singleton
@Named
public class HeimdallSessionLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Provider<SessionFactory> sessionFactoryProvider;

    @Inject
    public HeimdallSessionLifecycleParticipant(Provider<SessionFactory> sessionFactoryProvider) {
        this.sessionFactoryProvider = requireNonNull(sessionFactoryProvider);
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        requireNonNull(session);
        try {
            // session config
            SessionConfig sc = SessionConfig.defaults(
                            session.getRepositorySession(),
                            RepositoryUtils.toRepos(session.getRequest().getRemoteRepositories()))
                    .build();
            if (sc.enabled()) {
                SessionUtils.lazyInit(session.getRepositorySession(), () -> {
                    Session s = sessionFactoryProvider.get().create(sc);
                    logger.info("Heimdall {} session created", sc.version());
                    return s;
                });
            } else {
                logger.info("Heimdall {} disabled", sc.version());
            }
        } catch (Exception e) {
            if ("com.google.inject.ProvisionException".equals(e.getClass().getName())) {
                logger.error("Heimdall session creation failed", e); // here runtime req will kick in
            } else {
                throw new MavenExecutionException("Error enabling Heimdall", e);
            }
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        requireNonNull(session);
        try {
            Optional<Session> ns = SessionUtils.mayGetSession(session.getRepositorySession());
            if (ns.isPresent()) {
                try (Session heimdallSession = ns.orElseThrow(J8Utils.OET)) {
                    // nothing
                }
                logger.info("Heimdall session closed");
            }
        } catch (IOException e) {
            throw new MavenExecutionException("Error closing Heimdall", e);
        }
    }
}
