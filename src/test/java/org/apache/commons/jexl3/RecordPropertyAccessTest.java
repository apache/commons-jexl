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
import java.util.HashMap;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Checks that property access resolves a Java record's generated accessors.
 * <p>This module still builds and runs its test suite down to Java 8, where the {@code record} keyword
 * does not exist, so the test record used here cannot be a plain source declaration in this file: it is
 * compiled on the fly, and the test itself is skipped on a pre Java-16 runtime.</p>
 */
class RecordPropertyAccessTest {

    private static int featureVersion() {
        final String spec = System.getProperty("java.specification.version");
        return spec.startsWith("1.") ? Integer.parseInt(spec.substring(2)) : Integer.parseInt(spec);
    }

    private static Class<?> compileRecord(final String name, final String source) throws IOException,
        ClassNotFoundException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assumptions.assumeTrue(compiler != null, "no system Java compiler available");
        final Path dir = Files.createTempDirectory("jexl-record-test");
        final Path javaFile = dir.resolve(name + ".java");
        Files.write(javaFile, source.getBytes(StandardCharsets.UTF_8));
        final int rc = compiler.run(null, null, null, javaFile.toString());
        assertEquals(0, rc, "failed to compile test record");
        try (URLClassLoader loader = new URLClassLoader(new URL[] {dir.toUri().toURL()})) {
            return Class.forName(name, true, loader);
        }
    }

    @Test
    void testRecordComponentIsReadableAsProperty() throws Exception {
        Assumptions.assumeTrue(featureVersion() >= 16, "records require Java 16+");
        final Class<?> pointClass = compileRecord(
            "JexlRecordPoint", "public record JexlRecordPoint(int x, int y) {}"
        );
        final Object point = pointClass.getConstructor(int.class, int.class).newInstance(1, 2);

        final JexlEngine jexl = new JexlBuilder().permissions(JexlPermissions.UNRESTRICTED).create();
        final Map<String, Object> vars = new HashMap<>();
        vars.put("point", point);
        final JexlContext ctx = new MapContext(vars);

        assertEquals(1, jexl.createExpression("point.x").evaluate(ctx));
        assertEquals(2, jexl.createExpression("point.y").evaluate(ctx));
    }

    @Test
    void testRecordAccessorOverrideStillWins() throws Exception {
        Assumptions.assumeTrue(featureVersion() >= 16, "records require Java 16+");
        // a record that overrides its canonical accessor should still be picked up through the
        // ordinary getFoo() convention first, RecordGetExecutor only fills the gap otherwise left open
        final Class<?> pointClass = compileRecord(
            "JexlRecordNamedPoint",
            "public record JexlRecordNamedPoint(String name) { "
                + "public String getName() { return name() + \"!\"; } }"
        );
        final Object point = pointClass.getConstructor(String.class).newInstance("origin");

        final JexlEngine jexl = new JexlBuilder().permissions(JexlPermissions.UNRESTRICTED).create();
        final Map<String, Object> vars = new HashMap<>();
        vars.put("point", point);
        final JexlContext ctx = new MapContext(vars);

        assertNotNull(jexl.createExpression("point.name").evaluate(ctx));
        assertEquals("origin!", jexl.createExpression("point.name").evaluate(ctx));
    }
}
