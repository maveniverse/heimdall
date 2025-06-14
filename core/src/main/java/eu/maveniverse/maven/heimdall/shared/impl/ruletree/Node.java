/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared.impl.ruletree;

import java.util.HashMap;

/**
 * A tree structure with rules.
 */
public class Node {
    private static final HashMap<String, String> EMPTY = new HashMap<>();

    private final String name;
    private final HashMap<String, Node> siblings;
    private final HashMap<String, String> rules;

    public Node(String name) {
        this.name = name;
        this.siblings = new HashMap<>();
        this.rules = EMPTY;
    }

    public String getName() {
        return name;
    }

    public boolean isLeaf() {
        return siblings.isEmpty();
    }

    public Node addSibling(String name) {
        return siblings.computeIfAbsent(name, Node::new);
    }

    public Node getSibling(String name) {
        return siblings.get(name);
    }
}
