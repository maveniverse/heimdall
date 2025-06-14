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
class Node {
    private final String name;
    private final boolean stop;
    private final Boolean allow;
    private final HashMap<String, Node> siblings;

    protected Node(String name, boolean stop, Boolean allow) {
        this.name = name;
        this.stop = stop;
        this.allow = allow;
        this.siblings = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public boolean isLeaf() {
        return siblings.isEmpty();
    }

    public boolean isStop() {
        return stop;
    }

    public Boolean isAllow() {
        return allow;
    }

    protected Node addSibling(String name, boolean stop, Boolean allow) {
        return siblings.computeIfAbsent(name, n -> new Node(n, stop, allow));
    }

    protected Node getSibling(String name) {
        return siblings.get(name);
    }

    @Override
    public String toString() {
        return (stop ? "=" : "") + name + (allow != null && allow ? "=1" : "=0");
    }

    public void dump(String prefix) {
        System.out.println(prefix + this);
        for (Node node : siblings.values()) {
            node.dump(prefix + "  ");
        }
    }
}
