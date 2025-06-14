/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared.impl.ruletree;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class GroupTree extends Node {
    public static final GroupTree SENTINEL = new GroupTree("sentinel");

    private static final String MOD_EXCLUSION = "!";
    private static final String MOD_STOP = "=";

    private static List<String> elementsOfGroup(final String groupId) {
        return Arrays.stream(groupId.split("\\.")).filter(e -> !e.isEmpty()).collect(toList());
    }

    public GroupTree(String name) {
        super(name, false, null);
    }

    public int loadNodes(Stream<String> linesStream) {
        AtomicInteger counter = new AtomicInteger(0);
        linesStream.forEach(line -> {
            if (!line.startsWith("#") && !line.trim().isEmpty()) {
                counter.incrementAndGet();
                Node currentNode = this;
                boolean allow = true;
                if (line.startsWith(MOD_EXCLUSION)) {
                    allow = false;
                    line = line.substring(MOD_EXCLUSION.length());
                }
                boolean stop = false;
                if (line.startsWith(MOD_STOP)) {
                    stop = true;
                    line = line.substring(MOD_STOP.length());
                }
                List<String> groupElements = elementsOfGroup(line);
                for (String groupElement : groupElements.subList(0, groupElements.size() - 1)) {
                    currentNode = currentNode.addSibling(groupElement, false, null);
                }
                currentNode.addSibling(groupElements.get(groupElements.size() - 1), stop, allow);
            }
        });
        return counter.get();
    }

    public boolean acceptedGroupId(String groupId) {
        final List<String> current = new ArrayList<>();
        final List<String> groupElements = elementsOfGroup(groupId);
        Boolean accepted = null;
        Node currentNode = this;
        for (String groupElement : groupElements) {
            current.add(groupElement);
            currentNode = currentNode.getSibling(groupElement);
            if (currentNode == null) {
                break;
            }
            if (currentNode.isStop() && groupElements.equals(current)) {
                accepted = currentNode.isAllow();
            } else if (!currentNode.isStop()) {
                accepted = currentNode.isAllow();
            }
        }
        return accepted != null && accepted;
    }
}
