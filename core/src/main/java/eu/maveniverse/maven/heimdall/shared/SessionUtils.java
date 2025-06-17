/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Supplier;
import org.eclipse.aether.RepositorySystemSession;

public final class SessionUtils {
    private SessionUtils() {}

    /**
     * Performs a "lazy init" of Heimdall, does not fail if config and session already exists within this Resolver session.
     * Returns the existing or newly created {@link Session} instance, never {@code null}.
     */
    public static synchronized Session lazyInit(RepositorySystemSession session, Supplier<Session> sessionFactory) {
        requireNonNull(session, "session");
        requireNonNull(sessionFactory, "sessionFactory");
        Session s = (Session) session.getData().get(Session.class.getName());
        if (s == null) {
            s = sessionFactory.get();
            session.getData().set(Session.class.getName(), s);
        }
        return s;
    }

    /**
     * Returns Heimdall session instance, if initialized in this Repository Session.
     */
    public static synchronized Optional<Session> mayGetSession(RepositorySystemSession repositorySystemSession) {
        requireNonNull(repositorySystemSession, "repositorySystemSession");
        return Optional.ofNullable((Session) repositorySystemSession.getData().get(Session.class.getName()));
    }
}
