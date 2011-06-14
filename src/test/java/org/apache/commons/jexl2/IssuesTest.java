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
package org.apache.commons.jexl2;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import org.apache.commons.jexl2.introspection.Uberspect;
import org.apache.commons.jexl2.internal.Introspector;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl2.introspection.UberspectImpl;

/**
 * Test cases for reported issues
 */
@SuppressWarnings("boxing")
public class IssuesTest extends JexlTestCase {

    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    // JEXL-24: long integers (and doubles)
    public void test24() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        JexlContext ctxt = new MapContext(vars);
        String stmt = "{a = 10L; b = 10l; c = 42.0D; d = 42.0d; e=56.3F; f=56.3f; g=63.5}";
        Script expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        assertEquals(10L, vars.get("a"));
        assertEquals(10l, vars.get("b"));
        assertEquals(42.0D, vars.get("c"));
        assertEquals(42.0d, vars.get("d"));
        assertEquals(56.3f, vars.get("e"));
        assertEquals(56.3f, vars.get("f"));
        assertEquals(63.5f, vars.get("g"));
    }

    // JEXL-24: big integers and big decimals
    public void test24B() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        JexlContext ctxt = new MapContext(vars);
        String stmt = "{a = 10H; b = 10h; c = 42.0B; d = 42.0b;}";
        Script expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        assertEquals(new BigInteger("10"), vars.get("a"));
        assertEquals(new BigInteger("10"), vars.get("b"));
        assertEquals(new BigDecimal("42.0"), vars.get("c"));
        assertEquals(new BigDecimal("42.0"), vars.get("d"));
        
    }

    // JEXL-49: blocks not parsed (fixed)
    public void test49() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        JexlContext ctxt = new MapContext(vars);
        String stmt = "{a = 'b'; c = 'd';}";
        Script expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        assertTrue("JEXL-49 is not fixed", vars.get("a").equals("b") && vars.get("c").equals("d"));
    }

    // JEXL-48: bad assignment detection
    public static class Another {
        public String name = "whatever";
        private Boolean foo = Boolean.TRUE;

        public Boolean foo() {
            return foo;
        }

        public int goo() {
            return 100;
        }
    }

    public static class Foo {
        private Another inner;

        Foo() {
            inner = new Another();
        }

        public Another getInner() {
            return inner;
        }
    }

    public void test48() throws Exception {
        JexlEngine jexl = new JexlEngine();
        // ensure errors will throw
        jexl.setSilent(false);
        String jexlExp = "(foo.getInner().foo() eq true) and (foo.getInner().goo() = (foo.getInner().goo()+1-1))";
        Expression e = jexl.createExpression(jexlExp);
        JexlContext jc = new MapContext();
        jc.set("foo", new Foo());

        try {
            /* Object o = */ e.evaluate(jc);
            fail("Should have failed due to invalid assignment");
        } catch (JexlException xjexl) {
            // expected
        }
    }

    // JEXL-47: C style comments (single & multi line) (fixed in Parser.jjt)
    // JEXL-44: comments dont allow double quotes (fixed in Parser.jjt)
    public void test47() throws Exception {
        JexlEngine jexl = new JexlEngine();
        // ensure errors will throw
        jexl.setSilent(false);
        JexlContext ctxt = new MapContext();

        Expression expr = jexl.createExpression("true//false\n");
        Object value = expr.evaluate(ctxt);
        assertTrue("should be true", ((Boolean) value).booleanValue());

        expr = jexl.createExpression("/*true*/false");
        value = expr.evaluate(ctxt);
        assertFalse("should be false", ((Boolean) value).booleanValue());

        expr = jexl.createExpression("/*\"true\"*/false");
        value = expr.evaluate(ctxt);
        assertFalse("should be false", ((Boolean) value).booleanValue());
    }

    // JEXL-42: NullPointerException evaluating an expression
    // fixed in JexlArithmetic by allowing add operator to deal with string, null
    public void test42() throws Exception {
        JexlEngine jexl = new JexlEngine();
        UnifiedJEXL uel = new UnifiedJEXL(jexl);
        // ensure errors will throw
        //jexl.setSilent(false);
        JexlContext ctxt = new MapContext();
        ctxt.set("ax", "ok");

        UnifiedJEXL.Expression expr = uel.parse("${ax+(bx)}");
        Object value = expr.evaluate(ctxt);
        assertTrue("should be ok", "ok".equals(value));
    }

    // JEXL-40: failed to discover all methods (non public class implements public method)
    // fixed in ClassMap by taking newer version of populateCache from Velocity
    public static abstract class Base {
        public abstract boolean foo();
    }

    class Derived extends Base {
        @Override
        public boolean foo() {
            return true;
        }
    }

    public void test40() throws Exception {
        JexlEngine jexl = new JexlEngine();
        // ensure errors will throw
        jexl.setSilent(false);
        JexlContext ctxt = new MapContext();
        ctxt.set("derived", new Derived());

        Expression expr = jexl.createExpression("derived.foo()");
        Object value = expr.evaluate(ctxt);
        assertTrue("should be true", ((Boolean) value).booleanValue());
    }

    // JEXL-52: can be implemented by deriving Interpreter.{g,s}etAttribute; later
    public void test52base() throws Exception {
        JexlEngine jexl = createEngine(false);
        Uberspect uber = jexl.getUberspect();
        // most likely, call will be in an Interpreter, getUberspect
        String[] names = ((Introspector) uber).getMethodNames(Another.class);
        assertTrue("should find methods", names.length > 0);
        int found = 0;
        for (String name : names) {
            if ("foo".equals(name) || "goo".equals(name)) {
                found += 1;
            }
        }
        assertTrue("should have foo & goo", found == 2);

        names = ((UberspectImpl) uber).getFieldNames(Another.class);
        assertTrue("should find fields", names.length > 0);
        found = 0;
        for (String name : names) {
            if ("name".equals(name)) {
                found += 1;
            }
        }
        assertTrue("should have name", found == 1);
    }

    // JEXL-10/JEXL-11: variable checking, null operand is error
    public void test11() throws Exception {
        JexlEngine jexl = createEngine(false);
        // ensure errors will throw
        jexl.setSilent(false);
        JexlContext ctxt = new MapContext();
        ctxt.set("a", null);

        String[] exprs = {
            //"10 + null",
            //"a - 10",
            //"b * 10",
            "a % b"//,
        //"1000 / a"
        };
        for (int e = 0; e < exprs.length; ++e) {
            try {
                Expression expr = jexl.createExpression(exprs[e]);
                /* Object value = */ expr.evaluate(ctxt);
                fail(exprs[e] + " : should have failed due to null argument");
            } catch (JexlException xjexl) {
                // expected
            }
        }
    }

    // JEXL-62
    public void test62() throws Exception {
        JexlContext ctxt;
        JexlEngine jexl = createEngine(false);
        jexl.setSilent(true); // to avoid throwing JexlException on null method call

        Script jscript;

        ctxt = new MapContext();
        jscript = jexl.createScript("dummy.hashCode()");
        assertEquals(jscript.getText(), null, jscript.execute(ctxt)); // OK

        ctxt.set("dummy", "abcd");
        assertEquals(jscript.getText(), Integer.valueOf("abcd".hashCode()), jscript.execute(ctxt)); // OK

        jscript = jexl.createScript("dummy.hashCode");
        assertEquals(jscript.getText(), null, jscript.execute(ctxt)); // OK

        Expression jexpr;

        ctxt = new MapContext();
        jexpr = jexl.createExpression("dummy.hashCode()");
        assertEquals(jexpr.getExpression(), null, jexpr.evaluate(ctxt)); // OK

        ctxt.set("dummy", "abcd");
        assertEquals(jexpr.getExpression(), Integer.valueOf("abcd".hashCode()), jexpr.evaluate(ctxt)); // OK

        jexpr = jexl.createExpression("dummy.hashCode");
        assertEquals(jexpr.getExpression(), null, jexpr.evaluate(ctxt)); // OK
    }

    // JEXL-73
    public void test73() throws Exception {
        JexlContext ctxt = new MapContext();
        JexlEngine jexl = createEngine(false);
        jexl.setSilent(false);
        Expression e;
        e = jexl.createExpression("c.e");
        try {
            /* Object o = */ e.evaluate(ctxt);
        } catch (JexlException xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("variable c.e") > 0);
        }

        ctxt.set("c", "{ 'a' : 3, 'b' : 5}");
        ctxt.set("e", Integer.valueOf(2));
        try {
            /* Object o = */ e.evaluate(ctxt);
        } catch (JexlException xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("variable c.e") > 0);
        }

    }

    // JEXL-87
    public void test87() throws Exception {
        JexlContext ctxt = new MapContext();
        JexlEngine jexl = createEngine(false);
        jexl.setSilent(false);
        Expression divide = jexl.createExpression("l / r");
        Expression modulo = jexl.createExpression("l % r");

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
    public void test90() throws Exception {
        JexlContext ctxt = new MapContext();
        JexlEngine jexl = createEngine(false);
        jexl.setSilent(false);
        jexl.setCache(16);
        // ';' is necessary between expressions
        String[] fexprs = {
            "a=3 b=4",
            "while(a) while(a)",
            "1 2",
            "if (true) 2; 3 {}",
            "while (x) 1 if (y) 2 3"
        };
        for (int f = 0; f < fexprs.length; ++f) {
            try {
                jexl.createScript(fexprs[f]);
                fail(fexprs[f] + ": Should have failed in parse");
            } catch (JexlException xany) {
                // expected to fail in parse
            }
        }
        // ';' is necessary between expressions and only expressions
        String[] exprs = {
            "if (x) {1} if (y) {2}",
            "if (x) 1 if (y) 2",
            "while (x) 1 if (y) 2 else 3",
            "for(z : [3, 4, 5]) { z } y ? 2 : 1",
            "for(z : [3, 4, 5]) { z } if (y) 2 else 1"
        };
        ctxt.set("x", Boolean.FALSE);
        ctxt.set("y", Boolean.TRUE);
        for (int e = 0; e < exprs.length; ++e) {
            Script s = jexl.createScript(exprs[e]);
            assertEquals(Integer.valueOf(2), s.execute(ctxt));
        }
        debuggerCheck(jexl);
    }

    // JEXL-44
    public void test44() throws Exception {
        JexlContext ctxt = new MapContext();
        JexlEngine jexl = createEngine(false);
        jexl.setSilent(false);
        Script script;
        script = jexl.createScript("'hello world!'//commented");
        assertEquals("hello world!", script.execute(ctxt));
        script = jexl.createScript("'hello world!';//commented\n'bye...'");
        assertEquals("bye...", script.execute(ctxt));
        script = jexl.createScript("'hello world!'## commented");
        assertEquals("hello world!", script.execute(ctxt));
        script = jexl.createScript("'hello world!';## commented\n'bye...'");
        assertEquals("bye...", script.execute(ctxt));
    }

    public void test97() throws Exception {
        JexlContext ctxt = new MapContext();
        for (char v = 'a'; v <= 'z'; ++v) {
            ctxt.set(Character.toString(v), 10);
        }
        String input =
                "(((((((((((((((((((((((((z+y)/x)*w)-v)*u)/t)-s)*r)/q)+p)-o)*n)-m)+l)*k)+j)/i)+h)*g)+f)/e)+d)-c)/b)+a)";

        JexlEngine jexl = new JexlEngine();
        Expression script;
        // Make sure everything is loaded...
        long start = System.nanoTime();
        script = jexl.createExpression(input);
        Object value = script.evaluate(ctxt);
        assertEquals(Integer.valueOf(11), value);
        long end = System.nanoTime();
        double millisec = (end - start) / 1e6;
        double limit = 100.0; // Allow plenty of slack
        assertTrue("Expected parse to take less than "+limit+"ms, actual "+millisec, millisec < limit);
    }

    public static class fn98 {
        public String replace(String str, String target, String replacement) {
            return str.replace(target, replacement);
        }
    }

    public void test98() throws Exception {
        String[] exprs = {
            "fn:replace('DOMAIN\\somename', '\\\\', '\\\\\\\\')",
            "fn:replace(\"DOMAIN\\somename\", \"\\\\\", \"\\\\\\\\\")",
            "fn:replace('DOMAIN\\somename', '\\u005c', '\\u005c\\u005c')"
        };
        JexlEngine jexl = new JexlEngine();
        Map<String, Object> funcs = new HashMap<String, Object>();
        funcs.put("fn", new fn98());
        jexl.setFunctions(funcs);
        for (String expr : exprs) {
            Object value = jexl.createExpression(expr).evaluate(null);
            assertEquals(expr, "DOMAIN\\\\somename", value);
        }
    }

    public void test100() throws Exception {
        JexlEngine jexl = new JexlEngine();
        jexl.setCache(4);
        JexlContext ctxt = new MapContext();
        int[] foo = {42};
        ctxt.set("foo", foo);
        Object value;
        for (int l = 0; l < 2; ++l) {
            value = jexl.createExpression("foo[0]").evaluate(ctxt);
            assertEquals(42, value);
            value = jexl.createExpression("foo[0] = 43").evaluate(ctxt);
            assertEquals(43, value);
            value = jexl.createExpression("foo.0").evaluate(ctxt);
            assertEquals(43, value);
            value = jexl.createExpression("foo.0 = 42").evaluate(ctxt);
            assertEquals(42, value);
        }
    }

// A's class definition 
    public static class A105 {
        String nameA;
        String propA;

        public A105(String nameA, String propA) {
            this.nameA = nameA;
            this.propA = propA;
        }

        @Override
        public String toString() {
            return "A [nameA=" + nameA + ", propA=" + propA + "]";
        }

        public String getNameA() {
            return nameA;
        }

        public String getPropA() {
            return propA;
        }
    }

    public void test105() throws Exception {
        JexlContext context = new MapContext();
        Expression selectExp = new JexlEngine().createExpression("[a.propA]");
        context.set("a", new A105("a1", "p1"));
        Object[] r = (Object[]) selectExp.evaluate(context);
        assertEquals("p1", r[0]);

//selectExp = new JexlEngine().createExpression("[a.propA]");
        context.set("a", new A105("a2", "p2"));
        r = (Object[]) selectExp.evaluate(context);
        assertEquals("p2", r[0]);
    }

    public void test106() throws Exception {
        JexlContext context = new MapContext();
        context.set("a", new BigDecimal(1));
        context.set("b", new BigDecimal(3));
        JexlEngine jexl = new JexlEngine();
        try {
            Object value = jexl.createExpression("a / b").evaluate(context);
            assertNotNull(value);
        } catch (JexlException xjexl) {
            fail("should not occur");
        }
        JexlArithmetic arithmetic = new JexlArithmetic(false, MathContext.UNLIMITED, 2);
        JexlEngine jexlX = new JexlEngine(null, arithmetic, null, null);
        try {
            jexlX.createExpression("a / b").evaluate(context);
            fail("should fail");
        } catch (JexlException xjexl) {
            //ok  to fail
        }
    }

    public void test107() throws Exception {
        String[] exprs = {
            "'Q4'.toLowerCase()", "q4",
            "(Q4).toLowerCase()", "q4",
            "(4).toString()", "4",
            "(1 + 3).toString()", "4",
            "({ 'q' : 'Q4'}).get('q').toLowerCase()", "q4",
            "{ 'q' : 'Q4'}.get('q').toLowerCase()", "q4",
            "({ 'q' : 'Q4'})['q'].toLowerCase()", "q4",
            "(['Q4'])[0].toLowerCase()", "q4"
        };

        JexlContext context = new MapContext();
        context.set("Q4", "Q4");
        JexlEngine jexl = new JexlEngine();
        for (int e = 0; e < exprs.length; e += 2) {
            Expression expr = jexl.createExpression(exprs[e]);
            Object expected = exprs[e + 1];
            Object value = expr.evaluate(context);
            assertEquals(expected, value);
            expr = jexl.createExpression(expr.dump());
            value = expr.evaluate(context);
            assertEquals(expected, value);
        }
    }

    public void test108() throws Exception {
        Expression expr;
        Object value;
        JexlEngine jexl = new JexlEngine();
        expr = jexl.createExpression("size([])");
        value = expr.evaluate(null);
        assertEquals(0, value);
        expr = jexl.createExpression(expr.dump());
        value = expr.evaluate(null);
        assertEquals(0, value);

        expr = jexl.createExpression("if (true) { [] } else { {:} }");
        value = expr.evaluate(null);
        assertTrue(value.getClass().isArray());
        expr = jexl.createExpression(expr.dump());
        value = expr.evaluate(null);
        assertTrue(value.getClass().isArray());

        expr = jexl.createExpression("size({:})");
        value = expr.evaluate(null);
        assertEquals(0, value);
        expr = jexl.createExpression(expr.dump());
        value = expr.evaluate(null);
        assertEquals(0, value);

        expr = jexl.createExpression("if (false) { [] } else { {:} }");
        value = expr.evaluate(null);
        assertTrue(value instanceof Map<?, ?>);
        expr = jexl.createExpression(expr.dump());
        value = expr.evaluate(null);
        assertTrue(value instanceof Map<?, ?>);
    }

    public void test109() throws Exception {
        JexlEngine jexl = new JexlEngine();
        Object value;
        JexlContext context = new MapContext();
        context.set("foo.bar", 40);
        value = jexl.createExpression("foo.bar + 2").evaluate(context);
        assertEquals(42, value);
    }

    public void test110() throws Exception {
        JexlEngine jexl = new JexlEngine();
        String[] names = {"foo"};
        Object value;
        JexlContext context = new MapContext();
        value = jexl.createScript("foo + 2", null, names).execute(context, 40);
        assertEquals(42, value);
        context.set("frak.foo", -40);
        value = jexl.createScript("frak.foo - 2", null, names).execute(context, 40);
        assertEquals(-42, value);
    }

    static public class RichContext extends ObjectContext<A105> {
        RichContext(JexlEngine jexl, A105 a105) {
            super(jexl, a105);
        }

        public String uppercase(String str) {
            return str.toUpperCase();
        }
    }

    public void testRichContext() throws Exception {
        A105 a105 = new A105("foo", "bar");
        JexlEngine jexl = new JexlEngine();
        Object value;
        JexlContext context = new RichContext(jexl, a105);
        value = jexl.createScript("uppercase(nameA + propA)").execute(context);
        assertEquals("FOOBAR", value);
    }

    public void test111() throws Exception {
        JexlEngine jexl = new JexlEngine();
        Object value;
        JexlContext context = new MapContext();
        String strExpr = "((x>0)?\"FirstValue=\"+(y-x):\"SecondValue=\"+x)";
        Expression expr = jexl.createExpression(strExpr);

        context.set("x", 1);
        context.set("y", 10);
        value = expr.evaluate(context);
        assertEquals("FirstValue=9", value);

        context.set("x", 1.0d);
        context.set("y", 10.0d);
        value = expr.evaluate(context);
        assertEquals("FirstValue=9.0", value);

        context.set("x", 1);
        context.set("y", 10.0d);
        value = expr.evaluate(context);
        assertEquals("FirstValue=9.0", value);

        context.set("x", 1.0d);
        context.set("y", 10);
        value = expr.evaluate(context);
        assertEquals("FirstValue=9.0", value);


        context.set("x", -10);
        context.set("y", 1);
        value = expr.evaluate(context);
        assertEquals("SecondValue=-10", value);

        context.set("x", -10.0d);
        context.set("y", 1.0d);
        value = expr.evaluate(context);
        assertEquals("SecondValue=-10.0", value);

        context.set("x", -10);
        context.set("y", 1.0d);
        value = expr.evaluate(context);
        assertEquals("SecondValue=-10", value);

        context.set("x", -10.0d);
        context.set("y", 1);
        value = expr.evaluate(context);
        assertEquals("SecondValue=-10.0", value);
    }

    public void testScaleIssue() throws Exception {
        JexlArithmetic arithmetic = new JexlThreadedArithmetic(false);
        JexlEngine jexlX = new JexlEngine(null, arithmetic, null, null);
        String expStr1 = "result == salary/month * work.percent/100.00";
        Expression exp1 = jexlX.createExpression(expStr1);
        JexlContext ctx = new MapContext();
        ctx.set("result", new BigDecimal("9958.33"));
        ctx.set("salary", new BigDecimal("119500.00"));
        ctx.set("month", new BigDecimal("12.00"));
        ctx.set("percent", new BigDecimal("100.00"));
        
        // will fail because default scale is 5
        assertFalse((Boolean) exp1.evaluate(ctx));
        
        // will succeed with scale = 2
        JexlThreadedArithmetic.setMathScale(2);
        assertFalse((Boolean) exp1.evaluate(ctx));
    }
    
    public void test112() throws Exception {
        Object result;
        JexlEngine jexl = new JexlEngine();
        result = jexl.createScript(Integer.toString(Integer.MAX_VALUE)).execute(null);
        assertEquals(Integer.MAX_VALUE, result);
        result = jexl.createScript(Integer.toString(Integer.MIN_VALUE+1)).execute(null); 
        assertEquals(Integer.MIN_VALUE+1, result);
        result = jexl.createScript(Integer.toString(Integer.MIN_VALUE)).execute(null);
        assertEquals(Integer.MIN_VALUE, result);
    }

}
