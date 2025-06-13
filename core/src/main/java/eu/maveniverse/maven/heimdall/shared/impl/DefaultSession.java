/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared.impl;

import eu.maveniverse.maven.heimdall.shared.Session;
import eu.maveniverse.maven.heimdall.shared.SessionConfig;
import eu.maveniverse.maven.shared.core.component.CloseableConfigSupport;

public class DefaultSession extends CloseableConfigSupport<SessionConfig> implements Session {
    public DefaultSession(SessionConfig sessionConfig) {
        super(sessionConfig);
    }

    @Override
    public SessionConfig config() {
        return config;
    }
}
