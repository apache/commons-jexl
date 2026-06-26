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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that a bare {@code new JexlBuilder().create()} uses JEXL 3.7 hardened defaults:
 * SECURE permissions and features that disable new(), global side-effects, pragmas and annotations,
 * enable lexical scoping, and allow loops in scripts (but not in expressions).
 *
 * <p>This class intentionally does NOT extend JexlTestCase — JexlTestCase sets permissive
 * defaults to keep the rest of the test suite green, which would defeat the purpose here.</p>
 */
public class DefaultsTest {

    @BeforeEach
    public void restoreHardenedDefaults() {
        // Override whatever JexlTestCase may have set when running as part of the full suite.
        JexlBuilder.setDefaultPermissions(null);  // null → SECURE
        JexlBuilder.setDefaultFeatures(null);     // null → hardened (no new, no loops, no global SE)
    }

    @AfterEach
    public void restoreTestPermissive() {
        JexlBuilder.setDefaultPermissions(JexlTestCase.TEST_PERMS);
        JexlBuilder.setDefaultFeatures(JexlFeatures.createDefault());
    }

    @Test
    public void testDefaultPermissionsAreSecure() {
        final JexlBuilder builder = new JexlBuilder();
        assertSame(org.apache.commons.jexl3.introspection.JexlPermissions.SECURE, builder.permissions());
    }

    @Test
    public void testNewInstanceDisabledByDefault() {
        final JexlEngine engine = new JexlBuilder().create();
        assertThrows(JexlException.Feature.class,
            () -> engine.createScript("new('java.util.ArrayList')"),
            "new() must be rejected at parse time under hardened defaults");
    }

    @Test
    public void testLoopsEnabledForScriptsNotExpressions() {
        final JexlEngine engine = new JexlBuilder().create();
        // loops ARE allowed in scripts
        assertNotNull(engine.createScript("var s = 0; for (var i : [1,2,3]) { s += i; } s"),
            "for-loop must be accepted in a script under hardened defaults");
        // but a loop is not valid in a single expression
        assertThrows(JexlException.class,
            () -> engine.createExpression("for (var i : [1,2,3]) { i }"),
            "for-loop must be rejected in an expression");
    }

    @Test
    public void testGlobalSideEffectDisabledByDefault() {
        final JexlEngine engine = new JexlBuilder().create();
        assertThrows(JexlException.Feature.class,
            () -> engine.createScript("x = 1"),
            "global assignment must be rejected at parse time under hardened defaults");
    }

    @Test
    public void testPragmaDisabledByDefault() {
        final JexlEngine engine = new JexlBuilder().create();
        assertThrows(JexlException.Feature.class,
            () -> engine.createScript("#pragma jexl.foo 'bar'\n42"),
            "pragma must be rejected at parse time under hardened defaults");
    }

    @Test
    public void testAnnotationDisabledByDefault() {
        final JexlEngine engine = new JexlBuilder().create();
        assertThrows(JexlException.Feature.class,
            () -> engine.createScript("@silent var x = 1"),
            "annotation must be rejected at parse time under hardened defaults");
    }

    @Test
    public void testLexicalEnabledByDefault() {
        final JexlEngine engine = new JexlBuilder().create();
        // lexical shade: a local variable cannot shadow itself / redeclare in the same scope
        assertThrows(JexlException.class,
            () -> engine.createScript("var x = 1; var x = 2; x"),
            "lexical scoping must reject redeclaration under hardened defaults");
    }

    @Test
    public void testPackageDocExampleRunsUnderSecure() {
        // mirrors the "Brief Example" in package-info.java: must run as-is under hardened defaults
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlExpression e = jexl.createExpression(
            "'Hello ' + name + ', your name has ' + name.length() + ' letters'");
        final JexlContext jc = new MapContext();
        jc.set("name", "Aiko");
        assertEquals("Hello Aiko, your name has 4 letters", e.evaluate(jc));
    }

    @Test
    public void testSecurePermsDenyRuntime() {
        final JexlEngine engine = new JexlBuilder().features(JexlFeatures.createDefault()).create();
        // java.lang.Runtime is denied under SECURE
        assertThrows(JexlException.class,
            () -> engine.createScript("Runtime.getRuntime()").execute(new MapContext()),
            "Runtime should be denied under SECURE permissions");
    }
}
