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

public class PrefixTree extends Node {
    public static final PrefixTree SENTINEL = new PrefixTree("sentinel");

    private static List<String> elementsOfPath(final String path) {
        return Arrays.stream(path.split("/")).filter(e -> !e.isEmpty()).collect(toList());
    }

    public PrefixTree(String name) {
        super(name, false, null);
    }

    public int loadNodes(Stream<String> linesStream) {
        AtomicInteger counter = new AtomicInteger(0);
        linesStream.forEach(line -> {
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                counter.incrementAndGet();
                Node currentNode = this;
                for (String element : elementsOfPath(line)) {
                    currentNode = currentNode.addSibling(element, false, null);
                }
            }
        });
        return counter.get();
    }

    public boolean acceptedPath(String path) {
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
