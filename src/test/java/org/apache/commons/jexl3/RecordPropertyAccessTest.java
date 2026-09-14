/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Checks that property access resolves a Java record's generated accessors.
 * <p>This module still builds and runs its test suite down to Java 8, where the {@code record} keyword
 * does not exist, so the test record used here cannot be a plain source declaration in this file: it is
 * compiled on the fly, and the test itself is skipped on a pre Java-16 runtime.</p>
 */
public class RecordPropertyAccessTest extends JexlTestCase {

    public RecordPropertyAccessTest() {
        super("RecordPropertyAccessTest");
    }

    private static Class<?> compileRecord(final String name, final String source) throws IOException,
        ClassNotFoundException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assumptions.assumeTrue(compiler != null, "no system Java compiler available");
        final Path dir = Files.createTempDirectory("jexl-record-test");
        try {
            final Path javaFile = dir.resolve(name + ".java");
            Files.write(javaFile, source.getBytes(StandardCharsets.UTF_8));
            final int rc = compiler.run(null, null, null, "-d", dir.toString(), javaFile.toString());
            assertEquals(0, rc, "failed to compile test record");
            try (URLClassLoader loader = new URLClassLoader(new URL[] {dir.toUri().toURL()})) {
                return Class.forName("org.apache.commons.jexl3." + name, true, loader);
            }
        } finally {
            deleteRecursively(dir);
        }
    }

    private static void deleteRecursively(final Path dir) throws IOException {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    @Test
    void testRecordComponentIsReadableAsProperty() throws Exception {
        Assumptions.assumeTrue(featureVersion() >= 16, "records require Java 16+");
        final Class<?> pointClass = compileRecord("JexlRecordPoint",
          "package org.apache.commons.jexl3; public record JexlRecordPoint(int x, int y) {}"
        );
        final Object point = pointClass.getConstructor(int.class, int.class).newInstance(1, 2);
        final Map<String, Object> vars = new HashMap<>();
        vars.put("point", point);
        final JexlContext ctx = new MapContext(vars);

        assertEquals(1, JEXL.createExpression("point.x").evaluate(ctx));
        assertEquals(2, JEXL.createExpression("point.y").evaluate(ctx));
    }

    @Test
    void testRecordAccessorOverrideStillWins() throws Exception {
        Assumptions.assumeTrue(featureVersion() >= 16, "records require Java 16+");
        // a record that overrides its canonical accessor should still be picked up through the
        // ordinary getFoo() convention first, RecordGetExecutor only fills the gap otherwise left open
        final Class<?> pointClass = compileRecord(
            "JexlRecordNamedPoint",
            "package org.apache.commons.jexl3; "
                + "public record JexlRecordNamedPoint(String name) { "
                + "public String getName() { return name() + \"!\"; } }"
        );
        final Object point = pointClass.getConstructor(String.class).newInstance("origin");
        final Map<String, Object> vars = new HashMap<>();
        vars.put("point", point);
        final JexlContext ctx = new MapContext(vars);

        assertNotNull(JEXL.createExpression("point.name").evaluate(ctx));
        assertEquals("origin!", JEXL.createExpression("point.name").evaluate(ctx));
    }
}
