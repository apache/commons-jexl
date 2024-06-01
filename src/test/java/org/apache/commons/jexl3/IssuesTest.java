/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for reported issue between JEXL-1 and JEXL-100.
 */
@SuppressWarnings({"boxing", "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class IssuesTest extends JexlTestCase {
    // JEXL-48: bad assignment detection
    public static class Another {
        public String name = "whatever";
        private final Boolean foo = Boolean.TRUE;

        public Boolean foo() {
            return foo;
        }

        public int goo() {
            return 100;
        }
    }

    // JEXL-40: failed to discover all methods (non public class implements public method)
    // fixed in ClassMap by taking newer version of populateCache from Velocity
    public abstract static class Base {
        public abstract boolean foo();
    }

    static class Derived extends Base {
        @Override
        public boolean foo() {
            return true;
        }
    }

    public static class fn98 {
        public String replace(final String str, final String target, final String replacement) {
            return str.replace(target, replacement);
        }
    }

    public static class Foo {
        private final Another inner;

        Foo() {
            inner = new Another();
        }

        public Another getInner() {
            return inner;
        }
    }

    public IssuesTest() {
        super("IssuesTest", null);
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    // JEXL-10/JEXL-11: variable checking, null operand is error
    @Test
    public void test11() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        options.setStrict(true);

        ctxt.set("a", null);

        final String[] exprs = {
            //"10 + null",
            //"a - 10",
            //"b * 10",
            "a % b"//,
        //"1000 / a"
        };
        for (final String s : exprs) {
            final JexlExpression expr = jexl.createExpression(s);
            assertThrows(JexlException.class, () -> expr.evaluate(ctxt), () -> s + " : should have failed due to null argument");
        }
    }

    @Test
    public void test40() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.set(jexl);
        // ensure errors will throw
        options.setSilent(false);

        ctxt.set("derived", new Derived());

        final JexlExpression expr = jexl.createExpression("derived.foo()");
        final Object value = expr.evaluate(ctxt);
        assertTrue((Boolean) value);
    }

    // JEXL-42: NullPointerException evaluating an expression
    // fixed in JexlArithmetic by allowing add operator to deal with string, null
    @Test
    public void test42() throws Exception {
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine uel = jexl.createJxltEngine();
        // ensure errors will throw
        //jexl.setSilent(false);
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.set(jexl);
        options.setStrict(false);
        options.setStrictArithmetic(false);
        ctxt.set("ax", "ok");

        final JxltEngine.Expression expr = uel.createExpression("${ax+(bx)}");
        final Object value = expr.evaluate(ctxt);
        assertEquals("ok", value);
    }

    // JEXL-44
    @Test
    public void test44() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        JexlScript script;
        script = jexl.createScript("'hello world!'//commented");
        assertEquals("hello world!", script.execute(ctxt));
        script = jexl.createScript("'hello world!'; //commented\n'bye...'");
        assertEquals("bye...", script.execute(ctxt));
        script = jexl.createScript("'hello world!'## commented");
        assertEquals("hello world!", script.execute(ctxt));
        script = jexl.createScript("'hello world!';## commented\n'bye...'");
        assertEquals("bye...", script.execute(ctxt));
    }

    // JEXL-47: C style comments (single & multi line) (fixed in Parser.jjt)
    // JEXL-44: comments don't allow double quotes (fixed in Parser.jjt)
    @Test
    public void test47() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);

        JexlExpression expr = jexl.createExpression("true//false\n");
        Object value = expr.evaluate(ctxt);
        assertTrue((Boolean) value);

        expr = jexl.createExpression("/*true*/false");
        value = expr.evaluate(ctxt);
        assertFalse((Boolean) value);

        expr = jexl.createExpression("/*\"true\"*/false");
        value = expr.evaluate(ctxt);
        assertFalse((Boolean) value);
    }

    @Test
    public void test48() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        // ensure errors will throw
        options.setStrict(true);
        options.setSilent(false);
        final String jexlExp = "(foo.getInner().foo() eq true) and (foo.getInner().goo() = (foo.getInner().goo()+1-1))";
        jc.set("foo", new Foo());
        assertNotNull(assertThrows(JexlException.Assignment.class, () -> jexl.createExpression(jexlExp)).toString());
    }

    // JEXL-49: blocks not parsed (fixed)
    @Test
    public void test49() throws Exception {
        final JexlEngine jexl = new Engine();
        final Map<String, Object> vars = new HashMap<>();
        final JexlContext ctxt = new MapContext(vars);
        final String stmt = "a = 'b'; c = 'd';";
        final JexlScript expr = jexl.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        assertTrue(vars.get("a").equals("b") && vars.get("c").equals("d"), "JEXL-49 is not fixed");
    }

    // JEXL-52: can be implemented by deriving Interpreter.{g,s}etAttribute; later
    @Test
    public void test52base() throws Exception {
        final Engine jexl = (Engine) createEngine(false);
        final Uberspect uber = (Uberspect) jexl.getUberspect();
        // most likely, call will be in an Interpreter, getUberspect
        String[] names = uber.getMethodNames(Another.class);
        assertTrue(names.length > 0, "should find methods");
        int found = 0;
        for (final String name : names) {
            if ("foo".equals(name) || "goo".equals(name)) {
                found += 1;
            }
        }
        assertEquals(2, found, "should have foo & goo");

        names = uber.getFieldNames(Another.class);
        assertTrue(names.length > 0, "should find fields");
        found = 0;
        for (final String name : names) {
            if ("name".equals(name)) {
                found += 1;
            }
        }
        assertEquals(1, found, "should have name");
    }

    // JEXL-62
    @Test
    public void test62() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final MapContext vars = new MapContext();
        final JexlEvalContext ctxt = new JexlEvalContext(vars);
        final JexlOptions options = ctxt.getEngineOptions();
        options.setStrict(true);
        options.setSilent(true); // to avoid throwing JexlException on null method call

        JexlScript jscript;

        jscript = jexl.createScript("dummy.hashCode()");
        assertNull(jscript.execute(ctxt), jscript::getSourceText); // OK

        ctxt.set("dummy", "abcd");
        assertEquals(Integer.valueOf("abcd".hashCode()), jscript.execute(ctxt), jscript::getSourceText); // OK

        jscript = jexl.createScript("dummy.hashCode");
        assertNull(jscript.execute(ctxt), jscript::getSourceText); // OK

        JexlExpression jexpr;
        vars.clear();
        jexpr = jexl.createExpression("dummy.hashCode()");
        assertNull(jexpr.evaluate(ctxt), jexpr::toString); // OK

        ctxt.set("dummy", "abcd");
        assertEquals(Integer.valueOf("abcd".hashCode()), jexpr.evaluate(ctxt), jexpr::toString); // OK

        jexpr = jexl.createExpression("dummy.hashCode");
        assertNull(jexpr.evaluate(ctxt), jexpr::toString); // OK
    }

    // JEXL-87
    @Test
    public void test87() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        final JexlExpression divide = jexl.createExpression("l / r");
        final JexlExpression modulo = jexl.createExpression("l % r");

        ctxt.set("l", java.math.BigInteger.valueOf(7));
        ctxt.set("r", java.math.BigInteger.valueOf(2));
        assertEquals(java.math.BigInteger.valueOf(3), divide.evaluate(ctxt));
        assertTrue(jexl.getArithmetic().equals(1, modulo.evaluate(ctxt)));

        ctxt.set("l", java.math.BigDecimal.valueOf(7));
        ctxt.set("r", java.math.BigDecimal.valueOf(2));
        assertEquals(java.math.BigDecimal.valueOf(3.5), divide.evaluate(ctxt));
        assertTrue(jexl.getArithmetic().equals(1, modulo.evaluate(ctxt)));
    }

    // JEXL-90
    @Test
    public void test90() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        // ';' is necessary between expressions
        // @formatter:off
        final String[] fexprs = {
            "a=3 b=4",
            "while(a) while(a)",
            "1 2",
            "if (true) 2; 3 {}",
            "while (x) 1 if (y) 2 3"
        };
        // @formatter:on
        for (final String fexpr : fexprs) {
            assertThrows(JexlException.class, () -> jexl.createScript(fexpr));
        }
        // ';' is necessary between expressions and only expressions
        // @formatter:off
        final String[] exprs = {
            "if (x) {1} if (y) {2}",
            "if (x) 1 if (y) 2",
            "while (x) 1 if (y) 2 else 3",
            "for(z : [3, 4, 5]) { z } y ? 2 : 1",
            "for(z : [3, 4, 5]) { z } if (y) 2 else 1"
        };
        // @formatter:on
        ctxt.set("x", Boolean.FALSE);
        ctxt.set("y", Boolean.TRUE);
        for (final String expr : exprs) {
            final JexlScript s = jexl.createScript(expr);
            assertEquals(Integer.valueOf(2), s.execute(ctxt));
        }
        debuggerCheck(jexl);
    }

    @Test
    public void test97() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        for (char v = 'a'; v <= 'z'; ++v) {
            ctxt.set(Character.toString(v), 10);
        }
        final String input
                = "(((((((((((((((((((((((((z+y)/x)*w)-v)*u)/t)-s)*r)/q)+p)-o)*n)-m)+l)*k)+j)/i)+h)*g)+f)/e)+d)-c)/b)+a)";

        JexlExpression script;
        // Make sure everything is loaded...
        final long start = System.nanoTime();
        script = jexl.createExpression(input);
        final Object value = script.evaluate(ctxt);
        assertEquals(Integer.valueOf(11), value);
        final long end = System.nanoTime();
        final double millisec = (end - start) / 1e6;
        final double limit = 200.0; // Allow plenty of slack
        assertTrue(millisec < limit, () -> "Expected parse to take less than " + limit + "ms, actual " + millisec);
    }

    @Test
    public void test98() throws Exception {
        final String[] exprs = {
            "fn:replace('DOMAIN\\somename', '\\\\', '\\\\\\\\')",
            "fn:replace(\"DOMAIN\\somename\", \"\\\\\", \"\\\\\\\\\")",
            "fn:replace('DOMAIN\\somename', '\\u005c', '\\u005c\\u005c')"
        };
        final Map<String, Object> funcs = new HashMap<>();
        funcs.put("fn", new fn98());
        final JexlEngine jexl = new JexlBuilder().namespaces(funcs).create();
        for (final String expr : exprs) {
            final Object value = jexl.createExpression(expr).evaluate(null);
            assertEquals("DOMAIN\\\\somename", value, expr);
        }
    }

}
