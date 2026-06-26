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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JexlConfigLoader}.
 */
public class JexlConfigLoaderTest {

    /** Namespace example: static math helpers callable as {@code mymath:square(n)}. */
    public static class MathNs {
        public static int square(final int n) { return n * n; }
        public static double abs(final double x) { return Math.abs(x); }
    }

    /** Permissions example: explicitly allowed by a compose() rule in the YAML. */
    public static class Permitted {
        public String greet(final String name) { return "Hello, " + name; }
    }

    private static InputStream yaml(final String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private InputStream resource(final String name) {
        final InputStream in = getClass().getResourceAsStream(name);
        Assertions.assertNotNull(in, "test resource not found: " + name);
        return in;
    }

    @Test
    public void testLoadFromResource() throws Exception {
        try (InputStream in = resource("test-config.yaml")) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            Assertions.assertNotNull(builder);
        }
    }

    @Test
    public void testStrictModeFromYaml() throws Exception {
        try (InputStream in = yaml("strict: true\nsafe: false\n")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            Assertions.assertThrows(JexlException.class,
                () -> engine.createExpression("undeclaredVar + 1").evaluate(new MapContext()));
        }
    }

    @Test
    public void testFeaturesFromYaml() throws Exception {
        try (InputStream in = yaml("features:\n  loops: true\n  lambda: true\n  sideEffect: true\n")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            final JexlScript loop = engine.createScript(
                "var s = 0; for (var i : [1,2,3]) { s += i; } s");
            Assertions.assertEquals(6, ((Number) loop.execute(new MapContext())).intValue());
        }
    }

    @Test
    public void testNamespaceFromYaml() throws Exception {
        final String ns = MathNs.class.getName();
        try (InputStream in = yaml("permissions:\n  base: RESTRICTED\nnamespaces:\n  mymath: " + ns + "\n")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            final Object result = engine.createScript("mymath:square(4)")
                .execute(new MapContext());
            Assertions.assertEquals(16, result);
        }
    }

    @Test
    public void testPermissionsFromYaml() throws Exception {
        try (InputStream in = resource("test-config.yaml")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            Assertions.assertThrows(JexlException.class,
                () -> engine.createScript("new('java.lang.Runtime')").execute(new MapContext()));
        }
    }

    @Test
    public void testFullResourceConfig() throws Exception {
        try (InputStream in = resource("test-config.yaml")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            Assertions.assertNotNull(engine);

            Assertions.assertThrows(JexlException.class,
                () -> engine.createExpression("ghost + 1").evaluate(new MapContext()));

            final Object r = engine.createScript("math:random() >= 0").execute(new MapContext());
            Assertions.assertEquals(Boolean.TRUE, r);

            final Number sum = (Number) engine.createScript(
                "var s = 0; for (var i : [1,2,3]) { s += i; } s").execute(new MapContext());
            Assertions.assertEquals(6, sum.intValue());
        }
    }

    @Test
    public void testEngineConvenience() throws Exception {
        try (InputStream in = yaml("strict: false\n")) {
            final JexlEngine engine = JexlConfigLoader.engine(in);
            Assertions.assertNotNull(engine);
        }
    }

    @Test
    public void testInvalidArithmeticClass() throws Exception {
        try (InputStream in = yaml("arithmetic:\n  clazz: com.example.NoSuchClass\n  strict: true\n")) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> JexlConfigLoader.load(in));
        }
    }

    @Test
    public void testInvalidNamespaceClass() throws Exception {
        try (InputStream in = yaml("namespaces:\n  bad: com.example.NoSuchClass\n")) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> JexlConfigLoader.load(in));
        }
    }

    @Test
    public void testEmptyYaml() throws Exception {
        try (InputStream in = yaml("")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    public void testCommentOnlyYaml() throws Exception {
        try (InputStream in = yaml("# just a comment\n")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    public void testInlineComment() throws Exception {
        try (InputStream in = yaml("strict: true  # enable strict mode\ncache: 256 # cache size\n")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    public void testImportsList() throws Exception {
        try (InputStream in = yaml("imports:\n  - java.lang\n  - java.util\n")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    public void testPermissionsWithRules() throws Exception {
        final String canonical = Permitted.class.getCanonicalName();
        final String binary    = Permitted.class.getName();
        final String content =
            "permissions:\n"
            + "  base: RESTRICTED\n"
            + "  classes:\n"
            + "    - " + canonical + "\n";
        try (InputStream in = yaml(content)) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            final Object result = engine.createScript(
                "new('" + binary + "').greet('World')").execute(new MapContext());
            Assertions.assertEquals("Hello, World", result);
        }
    }

    @Test
    public void testUnrestrictedBase() throws Exception {
        try (InputStream in = yaml("permissions:\n  base: UNRESTRICTED\n")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    public void testSecureBase() throws Exception {
        try (InputStream in = yaml("permissions:\n  base: SECURE\n")) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            Assertions.assertSame(org.apache.commons.jexl3.introspection.JexlPermissions.SECURE,
                builder.permissions());
        }
        // no base: → defaults to SECURE
        try (InputStream in = yaml("permissions:\n  rules:\n")) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            // base is SECURE (default); no rules means same object
            Assertions.assertNotNull(builder.permissions());
        }
    }

    @Test
    public void testMigrationYaml() throws Exception {
        // jexl.yaml restores RESTRICTED permissions + legacy features (new, loops, side-effects)
        try (InputStream in = resource("jexl.yaml")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            // loop works
            final Number sum = (Number) engine.createScript(
                "var s = 0; for (var i : [1,2,3]) { s += i; } s").execute(new MapContext());
            Assertions.assertEquals(6, sum.intValue());
            // new() works
            final Object list = engine.createScript(
                "new('java.util.ArrayList')").execute(new MapContext());
            Assertions.assertNotNull(list);
        }
    }

    @Test
    public void testLoggingFromYaml() throws Exception {
        // a named logger wraps the permissions in a logging view
        try (InputStream in = yaml("permissions:\n  base: RESTRICTED\n  logging: jexl.config.permissions\n")) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            Assertions.assertTrue(builder.permissions()
                instanceof org.apache.commons.jexl3.introspection.JexlPermissions.LoggingPermissions);
        }
        // a bare "logging:" entry uses the default logger, still a logging view
        try (InputStream in = yaml("permissions:\n  base: RESTRICTED\n  logging:\n")) {
            Assertions.assertTrue(JexlConfigLoader.load(in).permissions()
                instanceof org.apache.commons.jexl3.introspection.JexlPermissions.LoggingPermissions);
        }
        // without a logging entry, no wrapper is applied
        try (InputStream in = yaml("permissions:\n  base: RESTRICTED\n")) {
            Assertions.assertFalse(JexlConfigLoader.load(in).permissions()
                instanceof org.apache.commons.jexl3.introspection.JexlPermissions.LoggingPermissions);
        }
    }
}
