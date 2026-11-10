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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests antish variables declared at parse time through {@link JexlFeatures#antishTest(java.util.function.Predicate)}.
 */
class AntishFeaturesTest extends JexlTestCase {

    public AntishFeaturesTest() {
        super("AntishFeaturesTest");
    }

    private static JexlEngine createEngine(final String... names) {
        final JexlFeatures features = JexlFeatures.createDefault().antishTest(JexlFeatures.namePatterns(Arrays.asList(names)));
        return new JexlBuilder().features(features).strict(true).create();
    }

    private static Map<String, Object> map(final String key, final Object value) {
        final Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    @Test
    void testRead() throws Exception {
        final JexlEngine jexl = createEngine("x.y");
        final JexlContext ctxt = new MapContext();
        ctxt.set("x.y", map("z", 42));
        final JexlScript script = jexl.createScript("x.y.z");
        assertEquals(42, script.execute(ctxt));
        // x.y is one variable, z a property
        final Set<List<String>> vars = script.getVariables();
        assertEquals(Collections.singleton(Arrays.asList("x.y", "z")), vars);
        assertEquals("x.y.z", script.getParsedText());
        debuggerCheck(jexl);
    }

    @Test
    void testWholeReference() throws Exception {
        final JexlEngine jexl = createEngine("x.y");
        final JexlContext ctxt = new MapContext();
        ctxt.set("x.y", 42);
        assertEquals(42, jexl.createScript("x.y").execute(ctxt));
        assertEquals(43, jexl.createScript("x.y = x.y + 1").execute(ctxt));
        assertEquals(43, ctxt.get("x.y"));
        assertEquals(44, jexl.createScript("x.y += 1").execute(ctxt));
        assertEquals(44, ctxt.get("x.y"));
        debuggerCheck(jexl);
    }

    @Test
    void testAssignProperty() throws Exception {
        final JexlEngine jexl = createEngine("x.y");
        final JexlContext ctxt = new MapContext();
        final Map<String, Object> xy = map("z", 1);
        ctxt.set("x.y", xy);
        assertEquals(2, jexl.createScript("x.y.z = 2").execute(ctxt));
        assertEquals(2, xy.get("z"));
        debuggerCheck(jexl);
    }

    @Test
    void testLongestPrefix() throws Exception {
        final JexlEngine jexl = createEngine("x.y", "x.y.z");
        final JexlContext ctxt = new MapContext();
        ctxt.set("x.y", map("z", map("w", 1)));
        ctxt.set("x.y.z", map("w", 2));
        assertEquals(2, jexl.createScript("x.y.z.w").execute(ctxt));
        assertEquals(1, jexl.createScript("x.y['z'].w").execute(ctxt));
        debuggerCheck(jexl);
    }

    @Test
    void testPatterns() throws Exception {
        final JexlEngine jexl = createEngine("build.*");
        final JexlContext ctxt = new MapContext();
        ctxt.set("build.version", "1.2");
        ctxt.set("build.dir", "target");
        assertEquals("target/1.2", jexl.createScript("build.dir + '/' + build.version").execute(ctxt));
        debuggerCheck(jexl);
    }

    @Test
    void testMethodCall() throws Exception {
        final JexlEngine jexl = createEngine("x.y");
        final JexlContext ctxt = new MapContext();
        ctxt.set("x.y", Arrays.asList(1, 2, 3));
        assertEquals(3, jexl.createScript("x.y.size()").execute(ctxt));
        debuggerCheck(jexl);
    }

    @Test
    void testLocalRoot() throws Exception {
        final JexlEngine jexl = createEngine("x.y");
        final JexlContext ctxt = new MapContext();
        ctxt.set("x.y", 42);
        // a local variable is never the root of an antish variable
        assertEquals(1, jexl.createScript("var x = { 'y' : 1 }; x.y").execute(ctxt));
        assertEquals(1, jexl.createScript("x.y", "x").execute(ctxt, map("y", 1)));
        debuggerCheck(jexl);
    }

    @Test
    void testSafeNavigationAndExpression() throws Exception {
        final JexlEngine jexl = createEngine("x.y");
        final JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.set("x.y", 42);
        ctxt.set("x", map("y", 1));
        assertEquals(42, jexl.createScript("x.y").execute(ctxt));
        // safe navigation or expression segments are not rewritten, y is a property of x
        assertEquals(1, jexl.createScript("x?.y").execute(ctxt));
        assertEquals(1, jexl.createScript("x.`y`").execute(ctxt));
        debuggerCheck(jexl);
    }

    @Test
    void testUndeclared() {
        final JexlEngine jexl = createEngine("x.y");
        final JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.set("a.b", 42);
        // not declared, not resolved: declaring antish names disables runtime antish
        assertThrows(JexlException.Property.class, () -> jexl.createScript("a.b").execute(ctxt));
        // declared but undefined
        final JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> jexl.createScript("x.y").execute(ctxt));
        assertEquals("x.y", xvar.getVariable());
        assertTrue(xvar.isUndefined());
    }

    @Test
    void testPartialDeclaration() {
        final JexlEngine jexl = createEngine("x.y");
        final JexlEvalContext ctxt = new JexlEvalContext();
        // even if the context options ask for it
        ctxt.getEngineOptions().setAntish(true);
        ctxt.set("x.y.z", 42);
        // x.y is the declared variable, z its property; x.y.z is never solved at runtime
        final JexlException.Property xp = assertThrows(JexlException.Property.class, () -> jexl.createScript("x.y.z").execute(ctxt));
        assertEquals(".z", xp.getProperty());
        // same with the engine options
        final JexlContext mctxt = new MapContext();
        mctxt.set("x.y.z", 42);
        assertThrows(JexlException.Property.class, () -> jexl.createScript("x.y.z").execute(mctxt));
    }

    @Test
    void testSideEffectGlobal() {
        final JexlFeatures features = JexlFeatures.createDefault()
                .sideEffectGlobal(false)
                .antishTest(JexlFeatures.namePatterns(Collections.singletonList("x.y")));
        final JexlEngine jexl = new JexlBuilder().features(features).create();
        // an antish variable is a global variable
        assertThrows(JexlException.Feature.class, () -> jexl.createScript("x.y = 1"));
        assertThrows(JexlException.Feature.class, () -> jexl.createScript("x.y += 1"));
        assertThrows(JexlException.Feature.class, () -> jexl.createScript("x = 1"));
        // assigning a property of a global value is not assigning a global
        final JexlContext ctxt = new MapContext();
        final Map<String, Object> xy = map("z", 0);
        ctxt.set("x.y", xy);
        ctxt.set("x", new HashMap<>());
        assertEquals(43, jexl.createScript("x.y.z = 43").execute(ctxt));
        assertEquals(43, xy.get("z"));
        assertEquals(44, jexl.createScript("x.y.z += 1").execute(ctxt));
        assertEquals(1, jexl.createScript("x.w = 1").execute(ctxt));
        assertEquals(1, ((Map<?, ?>) ctxt.get("x")).get("w"));
        // without declared antish names, any property assignment on a global may create an antish variable
        final JexlEngine jexl0 = new JexlBuilder().features(JexlFeatures.createDefault().sideEffectGlobal(false)).create();
        assertThrows(JexlException.Feature.class, () -> jexl0.createScript("x.y.z = 43"));
    }

    @Test
    void testSideEffectGlobalScripts() {
        final JexlFeatures features = JexlFeatures.createDefault().sideEffectGlobal(false);
        final JexlEngine jexl0 = new JexlBuilder().features(new JexlFeatures(features)).create();
        final JexlEngine jexl = new JexlBuilder()
                .features(features.antishTest(JexlFeatures.namePatterns(Collections.singletonList("x.y"))))
                .create();
        // assigning a global: always fails
        final String[] globals = {
            "x = 1", "x += 1", "x++", "--x", "x.y = 1", "x.y -= 1", "x.y++", "++x.y", "if (true) x.y = 4", "4 + (x.y = 1)"
        };
        // assigning a property of a global value: only fails without declared antish names
        final String[] properties = {
            "x.y.z = 1", "x.y.z *= 2", "x.y.z++", "--x.y.z", "x.y['z'] = 1", "x.y[0] = 1", "x['y'] = 1", "x[0] = 1",
            "x.w = 1", "x?.y = 1", "x.`y` = 1", "x().y = 1", "if (true) x.y.z = 4", "4 + (x.y.z = 1)"
        };
        for (final String src : globals) {
            assertThrows(JexlException.Feature.class, () -> jexl.createScript(src), src);
            assertThrows(JexlException.Feature.class, () -> jexl0.createScript(src), src);
        }
        for (final String src : properties) {
            assertNotNull(jexl.createScript(src), src);
            assertThrows(JexlException.Feature.class, () -> jexl0.createScript(src), src);
        }
        // with a local root, nothing is global
        for (final String src : globals) {
            assertNotNull(jexl.createScript("var x = {}; " + src), src);
            assertNotNull(jexl0.createScript("var x = {}; " + src), src);
        }
        // reading is never a side effect
        assertNotNull(jexl.createScript("x.y.z + x.y + x"));
        assertNotNull(jexl0.createScript("x.y.z + x.y + x"));
    }

    @Test
    void testScriptFeatures() {
        // engine without declared antish names, runtime antish solving
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlFeatures features = JexlFeatures.createDefault()
                .sideEffectGlobal(false)
                .antishTest(JexlFeatures.namePatterns(Collections.singletonList("x.y")));
        final JexlContext ctxt = new MapContext();
        ctxt.set("a.b.c", 42);
        // a script parsed with declared antish names does not solve antish variables at runtime
        final JexlScript assign = jexl.createScript(features, null, "a.b = 1");
        assertThrows(JexlException.class, () -> assign.execute(ctxt));
        assertFalse(ctxt.has("a.b"));
        final JexlScript read = jexl.createScript(features, null, "a.b.c");
        assertThrows(JexlException.class, () -> read.execute(ctxt));
        // the engine still solves antish variables for its own scripts
        assertEquals(42, jexl.createScript("a.b.c").execute(ctxt));
    }

    @Test
    void testFeaturesEquality() {
        final JexlFeatures f0 = JexlFeatures.createDefault().antishTest(JexlFeatures.namePatterns(Arrays.asList("x.y", "a.*")));
        final JexlFeatures f1 = JexlFeatures.createDefault().antishTest(JexlFeatures.namePatterns(Arrays.asList("a.*", "x.y")));
        final JexlFeatures f2 = JexlFeatures.createDefault().antishTest(JexlFeatures.namePatterns(Arrays.asList("x.y")));
        assertEquals(f0, f1);
        assertEquals(f0.hashCode(), f1.hashCode());
        assertEquals(f0, new JexlFeatures(f0));
        assertNotEquals(f0, f2);
        assertNotEquals(f0, JexlFeatures.createDefault());
    }
}
