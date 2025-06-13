/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared.impl;

import eu.maveniverse.maven.heimdall.shared.SessionConfig;
import eu.maveniverse.maven.heimdall.shared.SessionFactory;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
public class DefaultSessionFactory implements SessionFactory {

    @Override
    public DefaultSession create(SessionConfig sessionConfig) {
        return new DefaultSession(sessionConfig);
    }
}
