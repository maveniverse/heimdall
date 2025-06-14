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

public class PrefixTreeTest {
    @Test
    void simpleTest() {
        PrefixTree prefixTree = new PrefixTree("test");
        int ruleCount =
                prefixTree.loadNodes(Stream.of("# some comment", "/org/apache", "", "/eu/maveniverse", "/com/foo/bar"));
        Assertions.assertEquals(3, ruleCount);

        prefixTree.dump("");

        Assertions.assertTrue(prefixTree.acceptedPath("/org/apache/maven"));
        Assertions.assertTrue(prefixTree.acceptedPath("/eu/maveniverse/maven"));
        Assertions.assertTrue(prefixTree.acceptedPath("/com/foo/bar/maven"));

        Assertions.assertFalse(prefixTree.acceptedPath("/com/apache/maven"));
        Assertions.assertFalse(prefixTree.acceptedPath("/com/maveniverse/maven"));
        Assertions.assertFalse(prefixTree.acceptedPath("/com/foo/maven"));
    }
}
