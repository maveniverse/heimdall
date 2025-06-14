/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared.impl;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.heimdall.shared.Session;
import eu.maveniverse.maven.heimdall.shared.SessionConfig;
import eu.maveniverse.maven.shared.core.component.CloseableConfigSupport;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultSession extends CloseableConfigSupport<SessionConfig> implements Session {
    private final CopyOnWriteArrayList<Runnable> hooks;

    public DefaultSession(SessionConfig sessionConfig) {
        super(sessionConfig);
        this.hooks = new CopyOnWriteArrayList<>();
    }

    @Override
    public SessionConfig config() {
        return config;
    }

    @Override
    public void registerOnCloseHook(Runnable onCloseHook) {
        requireNonNull(onCloseHook);
        checkClosed();
        this.hooks.add(onCloseHook);
    }

    @Override
    protected void doClose() throws IOException {
        for (Runnable hook : hooks) {
            try {
                hook.run();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }
    }
}
