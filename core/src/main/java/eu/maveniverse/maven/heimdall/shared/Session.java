/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared;

import java.io.Closeable;

public interface Session extends Closeable {
    /**
     * Returns this session configuration.
     */
    SessionConfig config();

    /**
     * Allows one to register a hook called just before this session is closed.
     */
    void registerOnCloseHook(Runnable onCloseHook);
}
