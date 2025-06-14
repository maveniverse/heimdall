/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.heimdall.shared.impl.ruletree;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GroupTreeTest {
    @Test
    void simpleTest() {
        GroupTree groupTree = new GroupTree("test");
        int ruleCount = groupTree.loadNodes(Stream.of(
                "# some comment", // comment; ignored
                "org.apache", // org.apache and everything below
                "", // empty line; ignored
                "=eu.maveniverse", // "eu.maveniverse" only
                "eu.maveniverse.maven", // eu.maveniverse.maven and everything below
                "!eu.maveniverse.maven.foo", // nothing eu.maveniverse.maven.foo and below
                "com.foo", // com.foo and everything below
                "!=com.foo.bar")); // not "com.foo.bar" only
        Assertions.assertEquals(6, ruleCount);

        groupTree.dump("");

        Assertions.assertFalse(groupTree.acceptedGroupId("org"));
        Assertions.assertTrue(groupTree.acceptedGroupId("org.apache"));
        Assertions.assertTrue(groupTree.acceptedGroupId("org.apache.maven"));
        Assertions.assertTrue(groupTree.acceptedGroupId("org.apache.maven.foo"));

        Assertions.assertFalse(groupTree.acceptedGroupId("eu"));
        Assertions.assertTrue(groupTree.acceptedGroupId("eu.maveniverse"));
        Assertions.assertFalse(groupTree.acceptedGroupId("eu.maveniverse.foo"));
        Assertions.assertTrue(groupTree.acceptedGroupId("eu.maveniverse.maven"));
        Assertions.assertTrue(groupTree.acceptedGroupId("eu.maveniverse.maven.bar"));
        Assertions.assertTrue(groupTree.acceptedGroupId("eu.maveniverse.maven.bar.baz"));
        Assertions.assertFalse(groupTree.acceptedGroupId("eu.maveniverse.maven.foo"));
        Assertions.assertFalse(groupTree.acceptedGroupId("eu.maveniverse.maven.foo.bar"));
        Assertions.assertFalse(groupTree.acceptedGroupId("eu.maveniverse.maven.foo.bar.baz"));

        Assertions.assertFalse(groupTree.acceptedGroupId("com"));
        Assertions.assertTrue(groupTree.acceptedGroupId("com.foo"));
        Assertions.assertTrue(groupTree.acceptedGroupId("com.foo.maven"));
        Assertions.assertFalse(groupTree.acceptedGroupId("com.foo.bar"));
        Assertions.assertTrue(groupTree.acceptedGroupId("com.foo.bar.maven"));
    }
}
