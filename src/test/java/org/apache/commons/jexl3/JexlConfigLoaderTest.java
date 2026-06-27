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
import java.math.MathContext;
import java.nio.charset.StandardCharsets;

import org.apache.commons.jexl3.introspection.JexlUberspect;
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
    void testLoadFromResource() throws Exception {
        try (InputStream in = resource("test-config.yaml")) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            Assertions.assertNotNull(builder);
        }
    }

    @Test
    void testStrictModeFromYaml() throws Exception {
        try (InputStream in = yaml("strict: true\nsafe: false\n")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            Assertions.assertThrows(JexlException.class,
                () -> engine.createExpression("undeclaredVar + 1").evaluate(new MapContext()));
        }
    }

    @Test
    void testFeaturesFromYaml() throws Exception {
        try (InputStream in = yaml("features:\n  loops: true\n  lambda: true\n  sideEffect: true\n")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            final JexlScript loop = engine.createScript(
                "var s = 0; for (var i : [1,2,3]) { s += i; } s");
            Assertions.assertEquals(6, ((Number) loop.execute(new MapContext())).intValue());
        }
    }

    @Test
    void testNamespaceFromYaml() throws Exception {
        final String ns = MathNs.class.getName();
        try (InputStream in = yaml("permissions:\n  base: RESTRICTED\nnamespaces:\n  mymath: " + ns + "\n")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            final Object result = engine.createScript("mymath:square(4)")
                .execute(new MapContext());
            Assertions.assertEquals(16, result);
        }
    }

    @Test
    void testPermissionsFromYaml() throws Exception {
        try (InputStream in = resource("test-config.yaml")) {
            final JexlEngine engine = JexlConfigLoader.load(in).create();
            Assertions.assertThrows(JexlException.class,
                () -> engine.createScript("new('java.lang.Runtime')").execute(new MapContext()));
        }
    }

    @Test
    void testFullResourceConfig() throws Exception {
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
    void testEngineConvenience() throws Exception {
        try (InputStream in = yaml("strict: false\n")) {
            final JexlEngine engine = JexlConfigLoader.engine(in);
            Assertions.assertNotNull(engine);
        }
    }

    @Test
    void testInvalidArithmeticClass() throws Exception {
        try (InputStream in = yaml("arithmetic:\n  clazz: com.example.NoSuchClass\n  strict: true\n")) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> JexlConfigLoader.load(in));
        }
    }

    @Test
    void testInvalidNamespaceClass() throws Exception {
        try (InputStream in = yaml("namespaces:\n  bad: com.example.NoSuchClass\n")) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> JexlConfigLoader.load(in));
        }
    }

    @Test
    void testEmptyYaml() throws Exception {
        try (InputStream in = yaml("")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    void testCommentOnlyYaml() throws Exception {
        try (InputStream in = yaml("# just a comment\n")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    void testInlineComment() throws Exception {
        try (InputStream in = yaml("strict: true  # enable strict mode\ncache: 256 # cache size\n")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    void testImportsList() throws Exception {
        try (InputStream in = yaml("imports:\n  - java.lang\n  - java.util\n")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    void testPermissionsWithRules() throws Exception {
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
    void testUnrestrictedBase() throws Exception {
        try (InputStream in = yaml("permissions:\n  base: UNRESTRICTED\n")) {
            Assertions.assertNotNull(JexlConfigLoader.load(in).create());
        }
    }

    @Test
    void testSecureBase() throws Exception {
        try (InputStream in = yaml("permissions:\n  base: SECURE\n")) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            Assertions.assertSame(org.apache.commons.jexl3.introspection.JexlPermissions.SECURE,
                builder.permissions());
        }
        // no base: → defaults to NONE (deny everything, build from scratch)
        try (InputStream in = yaml("permissions:\n  rules:\n")) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            Assertions.assertSame(org.apache.commons.jexl3.introspection.JexlPermissions.NONE,
                builder.permissions());
        }
    }

    @Test
    void testNoneBaseFromYaml() throws Exception {
        try (InputStream in = yaml("permissions:\n  base: NONE\n  rules:\n    - \"java.lang { +String{} }\"\n")) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            final org.apache.commons.jexl3.introspection.JexlPermissions perms = builder.permissions();
            Assertions.assertTrue(perms.allow(String.class));
            Assertions.assertFalse(perms.allow(Integer.class));
            Assertions.assertFalse(perms.allow(System.class));
        }
    }

    @Test
    void testMigrationYaml() throws Exception {
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
    void testLoggingFromYaml() throws Exception {
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

    @Test
    void testOptionFlagsFromYaml() throws Exception {
        final String content =
            "silent: true\n"
            + "safe: false\n"
            + "cancellable: false\n"
            + "antish: false\n"
            + "lexical: true\n"
            + "lexicalShade: true\n"
            + "strictInterpolation: true\n"
            + "booleanLogical: true\n";
        try (InputStream in = yaml(content)) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            final JexlOptions o = builder.options();
            Assertions.assertTrue(o.isSilent());
            Assertions.assertFalse(o.isSafe());
            Assertions.assertFalse(o.isCancellable());
            Assertions.assertFalse(o.isAntish());
            Assertions.assertFalse(builder.antish());
            Assertions.assertTrue(o.isLexical());
            Assertions.assertTrue(builder.lexical());
            Assertions.assertTrue(o.isLexicalShade());
            Assertions.assertTrue(o.isStrictInterpolation());
            Assertions.assertTrue(o.isBooleanLogical());
        }
    }

    @Test
    void testNumericAndMiscOptionsFromYaml() throws Exception {
        final String content =
            "cache: 256\n"
            + "cacheThreshold: 32\n"
            + "stackOverflow: 1024\n"
            + "collectMode: 1\n"
            + "debug: true\n"
            + "charset: UTF-16\n"
            + "strategy: MAP_STRATEGY\n";
        try (InputStream in = yaml(content)) {
            final JexlBuilder builder = JexlConfigLoader.load(in);
            Assertions.assertEquals(256, builder.cache());
            Assertions.assertEquals(java.nio.charset.StandardCharsets.UTF_16, builder.charset());
            Assertions.assertSame(JexlUberspect.MAP_STRATEGY, builder.strategy());
            Assertions.assertNotNull(builder.create());
        }
    }

    @Test
    void testParseBoolVariants() throws Exception {
        // "yes" and "on" are truthy; "off" is falsy
        try (InputStream in = yaml("strict: yes\nsafe: on\ncancellable: off\n")) {
            final JexlOptions o = JexlConfigLoader.load(in).options();
            Assertions.assertTrue(o.isStrict());
            Assertions.assertTrue(o.isSafe());
            Assertions.assertFalse(o.isCancellable());
        }
    }

    @Test
    void testArithmeticMathContextFromYaml() throws Exception {
        // reflective 3-arg ctor branch: strict + mathContext + mathScale
        try (InputStream in = yaml("arithmetic:\n  strict: false\n  mathContext: DECIMAL64\n  mathScale: 5\n")) {
            final JexlArithmetic a = JexlConfigLoader.load(in).arithmetic();
            Assertions.assertNotNull(a);
            Assertions.assertEquals(MathContext.DECIMAL64, a.getMathContext());
            Assertions.assertEquals(5, a.getMathScale());
            Assertions.assertFalse(a.isStrict());
        }
        // 1-arg ctor branch: no mathContext
        try (InputStream in = yaml("arithmetic:\n  strict: true\n")) {
            final JexlArithmetic a = JexlConfigLoader.load(in).arithmetic();
            Assertions.assertNotNull(a);
            Assertions.assertTrue(a.isStrict());
        }
    }

    @Test
    void testStrategyJexl() throws Exception {
        // any non-MAP strategy value routes through parseStrategy's else branch → JEXL_STRATEGY
        try (InputStream in = yaml("strategy: JEXL_STRATEGY\n")) {
            Assertions.assertSame(JexlUberspect.JEXL_STRATEGY, JexlConfigLoader.load(in).strategy());
        }
        // no strategy: key → left unset (null), engine uses its internal default
        try (InputStream in = yaml("cache: 16\n")) {
            Assertions.assertNull(JexlConfigLoader.load(in).strategy());
        }
    }
}
