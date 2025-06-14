/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared.impl.ruletree;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RootNode extends Node {
    public static final RootNode SENTINEL = new RootNode("sentinel");

    private static List<String> elementsOfPath(final String path) {
        return Arrays.stream(path.split("/")).filter(e -> !e.isEmpty()).collect(toList());
    }

    public RootNode(String name) {
        super(name);
    }

    public int loadNodes(Stream<String> linesStream) {
        AtomicInteger counter = new AtomicInteger(0);
        linesStream.forEach(line -> {
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                counter.incrementAndGet();
                Node currentNode = this;
                for (String element : elementsOfPath(line)) {
                    currentNode = currentNode.addSibling(element);
                }
            }
        });
        return counter.get();
    }

    public boolean accepted(String path) {
        final List<String> pathElements = elementsOfPath(path);
        Node currentNode = this;
        for (String pathElement : pathElements) {
            currentNode = currentNode.getSibling(pathElement);
            if (currentNode == null || currentNode.isLeaf()) {
                break;
            }
        }
        return currentNode != null && currentNode.isLeaf();
    }
}
