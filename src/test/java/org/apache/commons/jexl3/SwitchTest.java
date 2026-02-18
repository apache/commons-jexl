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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.jexl3.internal.Debugger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests switch/case statement and expression.
 */
@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
public class SwitchTest extends JexlTestCase {
    public SwitchTest() {
        super("SwitchTest");
    }

    @Test
    void testSwitchExpression() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        final JexlScript e = jexl.createScript("var j = switch(i) { case 1 -> 2; case 2 -> 3; default -> 4; }; j", "i");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc, 1);
        assertEquals(2, o);
    }

    @Test
    void testBrokenSwitchExpression0() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        try {
            jexl.createScript("var j = switch(i) { case 1 -> return 2;  }; j", "i");
            fail("should not be able to create script with return in switch expression");
        } catch (final JexlException.Parsing xparse) {
            assertTrue(xparse.getMessage().contains("return"));
        }
        try {
            jexl.createScript("var j = switch(i) { case 1 -> break; }; j", "i");
            fail("should not be able to create script with break in switch expression");
        } catch (final JexlException.Parsing xparse) {
            assertTrue(xparse.getMessage().contains("break"));
        }
    }

    @Test
    void testSwitchStatement() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        final JexlScript e = jexl.createScript("switch(i) { case 1: i += 2; case 2: i += 3; default: i += 4; }; i + 33", "i");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc, 2);
        assertEquals(42, o);
    }

    @Test
    void test440a() {
        final JexlFeatures f = JexlFeatures.createDefault().ambiguousStatement(true);
        final JexlEngine jexl = new JexlBuilder().features(f).safe(false).strict(true).create();
        String src =
            "let y = switch (x) { case 10,11 -> 3 case 20, 21 -> 4\n" + "default -> { let z = 4; z + x } } y";
        JexlScript script = jexl.createScript(src, "x");
        assertNotNull(script);
        final String dbgStr = script.getParsedText();
        assertNotNull(dbgStr);

        Object result = script.execute(null, 10);
        Assertions.assertEquals(3, result);
        result = script.execute(null, 11);
        Assertions.assertEquals(3, result);
        result = script.execute(null, 20);
        Assertions.assertEquals(4, result);
        result = script.execute(null, 21);
        Assertions.assertEquals(4, result);
        result = script.execute(null, 38);
        Assertions.assertEquals(42, result);
        src = "let y = switch (x) { case 10,11 -> break; case 20, 21 -> 4; } y";
        try {
            script = jexl.createScript(src, "x");
            fail("should not be able to create script with break in switch");
        } catch (final JexlException.Parsing xparse) {
            assertTrue(xparse.getMessage().contains("break"));
        }
        assertNotNull(script);
    }

    @Test
    void test440b() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        final String src =
            "switch (x) {\n" +
                " case 10 : return 3\n;" +
                " case 20 : case 21 : return 4;\n" +
                " case 32: break; \n" +
                " default : return x + 4;\n" +
                " }\n" +
                " 169";
        final JexlScript script = jexl.createScript(src, "x");
        assertNotNull(script);
        final String dbgStr = script.getParsedText();
        assertNotNull(dbgStr);

        Object result = script.execute(null, 10);
        Assertions.assertEquals(3, result);
        result = script.execute(null, 20);
        Assertions.assertEquals(4, result);
        result = script.execute(null, 21);
        Assertions.assertEquals(4, result);
        result = script.execute(null, 32);
        Assertions.assertEquals(169, result);
        result = script.execute(null, 38);
        Assertions.assertEquals(42, result);
    }

    public enum Scope440 {
        UNDEFINED, UNDECLARED, GLOBAL, LOCAL, THIS, SUPER;
    }

    @Test
    void test440c() {
        final JexlEngine jexl = new JexlBuilder().loader(getClass().getClassLoader()).imports(this.getClass().getName()).create();
        final String src =
            "let s = switch (x) { case Scope440.UNDEFINED -> 'undefined'; case Scope440.THIS -> 'this'; default -> 'OTHER'; } s";
        final JexlScript script = jexl.createScript(src, "x");
        assertNotNull(script);
        final String dbgStr = script.getParsedText();
        assertNotNull(dbgStr);

        Object result = script.execute(null, Scope440.UNDEFINED);
        Assertions.assertEquals("undefined", result);
        result = script.execute(null, Scope440.THIS);
        Assertions.assertEquals("this", result);
        result = script.execute(null, 21);
        Assertions.assertEquals("OTHER", result);
    }

    @Test
    void test440d() {
        final JexlEngine jexl = new JexlBuilder().loader(getClass().getClassLoader()).imports(this.getClass().getName()).create();
        final String src = "let s = switch (x) { case Scope440.UNDEFINED -> 'undefined'; } s";
        final JexlScript script = jexl.createScript(src, "x");
        try {
            script.execute(null, Scope440.THIS);
            fail("should not be able to execute script with switch expression with no default");
        } catch (final JexlException xjexl) {
            assertTrue(xjexl.getMessage().contains("switch"));
        }
    }

    @Test
    void test440e() {
        final JexlEngine jexl = new JexlBuilder().loader(getClass().getClassLoader()).imports(this.getClass().getName()).create();
        final String src = "function f(x) { switch (x) { case Scope440.UNDEFINED : return 'undefined'; } } f(x)";
        final JexlScript script = jexl.createScript(src, "x");
        final Object result = script.execute(null, Scope440.UNDEFINED);
        Assertions.assertEquals("undefined", result);
        try {
            script.execute(null, Scope440.THIS);
            fail("should not be able to execute script with switch expression with no default");
        } catch (final JexlException xjexl) {
            assertTrue(xjexl.getMessage().contains("switch"));
        }
    }

    @Test
    void testSwitchStatement1() {
        // a one statement script
        final String src = "switch (x) {\n"
            + "  case 1: return 'one';\n"
            + "  case 2: return 'two';\n"
            + "  case 3: return 'three';\n"
            + "  default: return 'many';\n"
            + "}";
        runSwitch(src);
        final String src2 = "if (true) { " + src + " }";
        runSwitch(src2);
    }

    @Test
    void testSwitchExpression1() {
        // a one statement script that uses the expression syntax
        final String src = "switch (x) {\n"
            + "  case 1 -> 'one';\n"
            + "  case 2 -> 'two';\n"
            + "  case 3 -> 'three';\n"
            + "  default -> 'many';\n"
            + "}";
        runSwitch(src);
        final String src2 = "if (true) { " + src + " }";
        runSwitch(src2);
    }

    void runSwitch(final String src) {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript script = jexl.createScript(src, "x");
        Object result;
        result = script.execute(null, 1);
        Assertions.assertEquals("one", result);
        result = script.execute(null, 2);
        Assertions.assertEquals("two", result);
        result = script.execute(null, 3);
        Assertions.assertEquals("three", result);
        result = script.execute(null, 4);
        Assertions.assertEquals("many", result);
        result = script.execute(null, 42);
        Assertions.assertEquals("many", result);
        final Debugger debugger = new Debugger();
        assertTrue(debugger.debug(script));
        final String dbgStr = debugger.toString();
        assertTrue(JexlTestCase.equalsIgnoreWhiteSpace(src, dbgStr));
    }

    @Test
    void testSwitchExpressionFail() {
        final List<String> err = Arrays.asList("break", "continue");
        for (final String keyword : err) {
            switchExpressionFailKeyword(keyword);
        }
    }

    private void switchExpressionFailKeyword(final String keyword) {
        // a one statement script that uses the expression syntax
        final String src = "switch (x) {\n"
            + "  case 1 -> 'one';\n"
            + "  case 2 -> " + keyword + ";\n"
            + "  default -> 'many';\n"
            + "}";
        final JexlEngine jexl = new JexlBuilder().create();
        try {
            jexl.createScript(src, "x");
            fail("should not be able to create script with " + keyword + " in switch expression");
        } catch (final JexlException.Parsing xparse) {
            assertTrue(xparse.getMessage().contains(keyword));
        }
    }

    @Test
    void testTryCatchFinally() {
        JexlEngine jexl = new JexlBuilder().create();
        final String src = "try { return 42 } catch(const xerror){} finally { return 169 }";
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(169, result);
    }

    @Test
    void testSwitchNaN0() {
        // a one statement script
        final String src = "switch (x) {\n"
            + "  case NaN: return 'NotANumber';\n"
            + "  case 1: return 'one';\n"
            + "  case 2: return 'two';\n"
            + "  case 3: return 'three';\n"
            + "  default: return 'many';\n"
            + "}";
        runSwitchNaN(src);
    }

    @Test
    void testSwitchNaN1() {
        // a one statement script
        final String src = "switch (x) {\n"
            + "  case NaN -> 'NotANumber';\n"
            + "  case 1 -> 'one';\n"
            + "  case 2 -> 'two';\n"
            + "  case 3 -> 'three';\n"
            + "  default -> 'many';\n"
            + "}";
        runSwitchNaN(src);
    }

    static void runSwitchNaN(final String src) {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript script = jexl.createScript(src, "x");
        Object result = script.execute(null, Double.NaN);
        assertEquals("NotANumber", result);
        result = script.execute(null, 1);
        assertEquals("one", result);
        result = script.execute(null, 42);
        assertEquals("many", result);
    }
}
