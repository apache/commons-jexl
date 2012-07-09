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

import org.apache.commons.jexl3.internal.Engine;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.junit.Test;

/**
 * Test cases for reported issues
 */
@SuppressWarnings("boxing")
public class IssuesTest extends JexlTestCase {
    public IssuesTest() {
        super("IssuesTest", null);
    }

    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }


    // JEXL-49: blocks not parsed (fixed)
    public void test49() throws Exception {
        JexlEngine jexl = new Engine();
        Map<String, Object> vars = new HashMap<String, Object>();
        JexlContext ctxt = new MapContext(vars);
        String stmt = "{a = 'b'; c = 'd';}";
        JexlScript expr = jexl.createScript(stmt);
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
        JexlEngine jexl = new Engine();
        JexlEvalContext jc = new JexlEvalContext();
        // ensure errors will throw
        jc.setStrict(true);
        jc.setSilent(false);
        try {
            String jexlExp = "(foo.getInner().foo() eq true) and (foo.getInner().goo() = (foo.getInner().goo()+1-1))";
            JexlExpression e = jexl.createExpression(jexlExp);
            jc.set("foo", new Foo());
            /* Object o = */ e.evaluate(jc);
            fail("Should have failed due to invalid assignment");
        } catch (JexlException.Assignment xparse) {
            String dbg = xparse.toString();
        } catch (JexlException xjexl) {
            fail("Should have thrown a parse exception");
        }
    }

    // JEXL-47: C style comments (single & multi line) (fixed in Parser.jjt)
    // JEXL-44: comments dont allow double quotes (fixed in Parser.jjt)
    public void test47() throws Exception {
        JexlEngine jexl = new Engine();
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);

        JexlExpression expr = jexl.createExpression("true//false\n");
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
        JexlEngine jexl = new JexlBuilder().create();
        JxltEngine uel = jexl.createJxltEngine();
        // ensure errors will throw
        //jexl.setSilent(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrict(false);
        ctxt.set("ax", "ok");

        JxltEngine.UnifiedExpression expr = uel.createExpression("${ax+(bx)}");
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
        JexlEngine jexl = new Engine();
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);

        ctxt.set("derived", new Derived());

        JexlExpression expr = jexl.createExpression("derived.foo()");
        Object value = expr.evaluate(ctxt);
        assertTrue("should be true", ((Boolean) value).booleanValue());
    }

    // JEXL-52: can be implemented by deriving Interpreter.{g,s}etAttribute; later
    public void test52base() throws Exception {
        Engine jexl = (Engine) createEngine(false);
        Uberspect uber = (Uberspect) jexl.getUberspect();
        // most likely, call will be in an Interpreter, getUberspect
        String[] names = uber.getMethodNames(Another.class);
        assertTrue("should find methods", names.length > 0);
        int found = 0;
        for (String name : names) {
            if ("foo".equals(name) || "goo".equals(name)) {
                found += 1;
            }
        }
        assertTrue("should have foo & goo", found == 2);

        names = uber.getFieldNames(Another.class);
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
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        ctxt.setStrict(true);

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
                JexlExpression expr = jexl.createExpression(exprs[e]);
                /* Object value = */ expr.evaluate(ctxt);
                fail(exprs[e] + " : should have failed due to null argument");
            } catch (JexlException xjexl) {
                // expected
            }
        }
    }

    // JEXL-62
    public void test62() throws Exception {
        JexlEngine jexl = createEngine(false);
        MapContext vars = new MapContext();
        JexlEvalContext ctxt = new JexlEvalContext(vars);
        ctxt.setStrict(true);
        ctxt.setSilent(true);// to avoid throwing JexlException on null method call

        JexlScript jscript;

        jscript = jexl.createScript("dummy.hashCode()");
        assertEquals(jscript.getSourceText(), null, jscript.execute(ctxt)); // OK

        ctxt.set("dummy", "abcd");
        assertEquals(jscript.getSourceText(), Integer.valueOf("abcd".hashCode()), jscript.execute(ctxt)); // OK

        jscript = jexl.createScript("dummy.hashCode");
        assertEquals(jscript.getSourceText(), null, jscript.execute(ctxt)); // OK

        JexlExpression jexpr;
        vars.clear();
        jexpr = jexl.createExpression("dummy.hashCode()");
        assertEquals(jexpr.toString(), null, jexpr.evaluate(ctxt)); // OK

        ctxt.set("dummy", "abcd");
        assertEquals(jexpr.toString(), Integer.valueOf("abcd".hashCode()), jexpr.evaluate(ctxt)); // OK

        jexpr = jexl.createExpression("dummy.hashCode");
        assertEquals(jexpr.toString(), null, jexpr.evaluate(ctxt)); // OK
    }

    // JEXL-73
    public void test73() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        ctxt.setStrict(true);
        JexlExpression e;
        e = jexl.createExpression("c.e");
        try {
            /* Object o = */ e.evaluate(ctxt);
            fail("c.e not declared as variable");
        } catch (JexlException.Variable xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("c.e") > 0);
        }

        ctxt.set("c", "{ 'a' : 3, 'b' : 5}");
        ctxt.set("e", Integer.valueOf(2));
        try {
            /* Object o = */ e.evaluate(ctxt);
            fail("c.e not accessible as property");
        } catch (JexlException.Property xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("e") > 0);
        }

    }

    // JEXL-87
    public void test87() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        JexlExpression divide = jexl.createExpression("l / r");
        JexlExpression modulo = jexl.createExpression("l % r");

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
        JexlEngine jexl = createEngine(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
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
                // expected to fail in createExpression
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
            JexlScript s = jexl.createScript(exprs[e]);
            assertEquals(Integer.valueOf(2), s.execute(ctxt));
        }
        debuggerCheck(jexl);
    }

    // JEXL-44
    public void test44() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        JexlScript script;
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
        JexlEngine jexl = createEngine(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        for (char v = 'a'; v <= 'z'; ++v) {
            ctxt.set(Character.toString(v), 10);
        }
        String input =
                "(((((((((((((((((((((((((z+y)/x)*w)-v)*u)/t)-s)*r)/q)+p)-o)*n)-m)+l)*k)+j)/i)+h)*g)+f)/e)+d)-c)/b)+a)";

        JexlExpression script;
        // Make sure everything is loaded...
        long start = System.nanoTime();
        script = jexl.createExpression(input);
        Object value = script.evaluate(ctxt);
        assertEquals(Integer.valueOf(11), value);
        long end = System.nanoTime();
        double millisec = (end - start) / 1e6;
        double limit = 200.0; // Allow plenty of slack
        assertTrue("Expected parse to take less than " + limit + "ms, actual " + millisec, millisec < limit);
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
        Map<String, Object> funcs = new HashMap<String, Object>();
        funcs.put("fn", new fn98());
        JexlEngine jexl = new JexlBuilder().namespaces(funcs).create();
        for (String expr : exprs) {
            Object value = jexl.createExpression(expr).evaluate(null);
            assertEquals(expr, "DOMAIN\\\\somename", value);
        }
    }

    public void test100() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(4).create();
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

        public String uppercase(String str) {
            return str.toUpperCase();
        }
    }

    public void test105() throws Exception {
        JexlContext context = new MapContext();
        JexlExpression selectExp = new Engine().createExpression("[a.propA]");
        context.set("a", new A105("a1", "p1"));
        Object[] r = (Object[]) selectExp.evaluate(context);
        assertEquals("p1", r[0]);

//selectExp = new Engine().createExpression("[a.propA]");
        context.set("a", new A105("a2", "p2"));
        r = (Object[]) selectExp.evaluate(context);
        assertEquals("p2", r[0]);
    }

    public void test106() throws Exception {
        JexlEvalContext context = new JexlEvalContext();
        context.setStrict(true, true);
        context.set("a", new BigDecimal(1));
        context.set("b", new BigDecimal(3));
        JexlEngine jexl = new Engine();
        try {
            Object value = jexl.createExpression("a / b").evaluate(context);
            assertNotNull(value);
        } catch (JexlException xjexl) {
            fail("should not occur");
        }
        context.setMathContext(MathContext.UNLIMITED);
        context.setMathScale(2);
        try {
            jexl.createExpression("a / b").evaluate(context);
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
        JexlEngine jexl = new Engine();
        for (int e = 0; e < exprs.length; e += 2) {
            JexlExpression expr = jexl.createExpression(exprs[e]);
            Object expected = exprs[e + 1];
            Object value = expr.evaluate(context);
            assertEquals(expected, value);
            expr = jexl.createExpression(expr.getParsedText());
            value = expr.evaluate(context);
            assertEquals(expected, value);
        }
    }

    public void test108() throws Exception {
        JexlExpression expr;
        Object value;
        JexlEngine jexl = new Engine();
        expr = jexl.createExpression("size([])");
        value = expr.evaluate(null);
        assertEquals(0, value);
        expr = jexl.createExpression(expr.getParsedText());
        value = expr.evaluate(null);
        assertEquals(0, value);

        expr = jexl.createExpression("if (true) { [] } else { {:} }");
        value = expr.evaluate(null);
        assertTrue(value.getClass().isArray());
        expr = jexl.createExpression(expr.getParsedText());
        value = expr.evaluate(null);
        assertTrue(value.getClass().isArray());

        expr = jexl.createExpression("size({:})");
        value = expr.evaluate(null);
        assertEquals(0, value);
        expr = jexl.createExpression(expr.getParsedText());
        value = expr.evaluate(null);
        assertEquals(0, value);

        expr = jexl.createExpression("if (false) { [] } else { {:} }");
        value = expr.evaluate(null);
        assertTrue(value instanceof Map<?, ?>);
        expr = jexl.createExpression(expr.getParsedText());
        value = expr.evaluate(null);
        assertTrue(value instanceof Map<?, ?>);
    }

    public void test109() throws Exception {
        JexlEngine jexl = new Engine();
        Object value;
        JexlContext context = new MapContext();
        context.set("foo.bar", 40);
        value = jexl.createExpression("foo.bar + 2").evaluate(context);
        assertEquals(42, value);
    }

    public void test110() throws Exception {
        JexlEngine jexl = new Engine();
        String[] names = {"foo"};
        Object value;
        JexlContext context = new MapContext();
        value = jexl.createScript("foo + 2", names).execute(context, 40);
        assertEquals(42, value);
        context.set("frak.foo", -40);
        value = jexl.createScript("frak.foo - 2", names).execute(context, 40);
        assertEquals(-42, value);
    }

    static public class RichContext extends ObjectContext<A105> {
        RichContext(JexlEngine jexl, A105 a105) {
            super(jexl, a105);
        }
    }

    public void testRichContext() throws Exception {
        A105 a105 = new A105("foo", "bar");
        JexlEngine jexl = new Engine();
        Object value;
        JexlContext context = new RichContext(jexl, a105);
        value = jexl.createScript("uppercase(nameA + propA)").execute(context);
        assertEquals("FOOBAR", value);
    }

    public void test111() throws Exception {
        JexlEngine jexl = new Engine();
        Object value;
        JexlContext context = new MapContext();
        String strExpr = "((x>0)?\"FirstValue=\"+(y-x):\"SecondValue=\"+x)";
        JexlExpression expr = jexl.createExpression(strExpr);

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
        JexlEngine jexlX = new Engine();
        String expStr1 = "result == salary/month * work.percent/100.00";
        JexlExpression exp1 = jexlX.createExpression(expStr1);
        JexlEvalContext ctx = new JexlEvalContext();
        ctx.set("result", new BigDecimal("9958.33"));
        ctx.set("salary", new BigDecimal("119500.00"));
        ctx.set("month", new BigDecimal("12.00"));
        ctx.set("work.percent", new BigDecimal("100.00"));

        // will fail because default scale is 5
        assertFalse((Boolean) exp1.evaluate(ctx));

        // will succeed with scale = 2
        ctx.setMathScale(2);
        assertTrue((Boolean) exp1.evaluate(ctx));
    }

    public void test112() throws Exception {
        Object result;
        JexlEngine jexl = new Engine();
        result = jexl.createScript(Integer.toString(Integer.MAX_VALUE)).execute(null);
        assertEquals(Integer.MAX_VALUE, result);
        result = jexl.createScript(Integer.toString(Integer.MIN_VALUE + 1)).execute(null);
        assertEquals(Integer.MIN_VALUE + 1, result);
        result = jexl.createScript(Integer.toString(Integer.MIN_VALUE)).execute(null);
        assertEquals(Integer.MIN_VALUE, result);
    }

    public void test117() throws Exception {
        JexlEngine jexl = new Engine();
        JexlExpression e = jexl.createExpression("TIMESTAMP > 20100102000000");
        JexlContext ctx = new MapContext();
        ctx.set("TIMESTAMP", new Long("20100103000000"));
        Object result = e.evaluate(ctx);
        assertTrue((Boolean) result);
    }

    public static class Foo125 {
        public String method() {
            return "OK";
        }
    }

    public static class Foo125Context extends ObjectContext<Foo125> {
        public Foo125Context(JexlEngine engine, Foo125 wrapped) {
            super(engine, wrapped);
        }
    }

    public void test125() throws Exception {
        JexlEngine jexl = new Engine();
        JexlExpression e = jexl.createExpression("method()");
        JexlContext jc = new Foo125Context(jexl, new Foo125());
        assertEquals("OK", e.evaluate(jc));
    }

    public void test130a() throws Exception {
        String myName = "Test.Name";
        Object myValue = "Test.Value";

        JexlEngine myJexlEngine = new Engine();
        MapContext myMapContext = new MapContext();
        myMapContext.set(myName, myValue);

        Object myObjectWithTernaryConditional = myJexlEngine.createScript(myName + "?:null").execute(myMapContext);
        assertEquals(myValue, myObjectWithTernaryConditional);
    }

    public void test130b() throws Exception {
        String myName = "Test.Name";
        Object myValue = new Object() {
            @Override
            public String toString() {
                return "Test.Value";
            }
        };

        JexlEngine myJexlEngine = new Engine();
        MapContext myMapContext = new MapContext();
        myMapContext.set(myName, myValue);

        Object myObjectWithTernaryConditional = myJexlEngine.createScript(myName + "?:null").execute(myMapContext);
        assertEquals(myValue, myObjectWithTernaryConditional);
    }

    public void testBigdOp() throws Exception {
        BigDecimal sevendot475 = new BigDecimal("7.475");
        BigDecimal SO = new BigDecimal("325");
        JexlContext jc = new MapContext();
        jc.set("SO", SO);

        JexlEngine jexl = new Engine();
        String expr = "2.3*SO/100";

        Object evaluate = jexl.createExpression(expr).evaluate(jc);
        assertEquals(sevendot475, (BigDecimal) evaluate);
    }

    public static class Arithmetic132 extends JexlArithmetic {
        public Arithmetic132() {
            super(false);
        }

        protected double divideZero(BigDecimal x) {
            int ls = x.signum();
            if (ls < 0) {
                return Double.NEGATIVE_INFINITY;
            } else if (ls > 0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return Double.NaN;
            }
        }

        protected double divideZero(BigInteger x) {
            int ls = x.signum();
            if (ls < 0) {
                return Double.NEGATIVE_INFINITY;
            } else if (ls > 0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return Double.NaN;
            }
        }

        @Override
        public Object divide(Object left, Object right) {
            if (left == null && right == null) {
                return controlNullNullOperands();
            }
            // if either are bigdecimal use that type
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                BigDecimal l = toBigDecimal(left);
                BigDecimal r = toBigDecimal(right);
                if (BigDecimal.ZERO.equals(r)) {
                    return divideZero(l);
                }
                BigDecimal result = l.divide(r, getMathContext());
                return narrowBigDecimal(left, right, result);
            }
            // if either are floating point (double or float) use double
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                double l = toDouble(left);
                double r = toDouble(right);
                return new Double(l / r);
            }
            // otherwise treat as integers
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            if (BigInteger.ZERO.equals(r)) {
                return divideZero(l);
            }
            BigInteger result = l.divide(r);
            return narrowBigInteger(left, right, result);
        }

        @Override
        public Object mod(Object left, Object right) {
            if (left == null && right == null) {
                return controlNullNullOperands();
            }
            // if either are bigdecimal use that type
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                BigDecimal l = toBigDecimal(left);
                BigDecimal r = toBigDecimal(right);
                if (BigDecimal.ZERO.equals(r)) {
                    return divideZero(l);
                }
                BigDecimal remainder = l.remainder(r, getMathContext());
                return narrowBigDecimal(left, right, remainder);
            }
            // if either are floating point (double or float) use double
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                double l = toDouble(left);
                double r = toDouble(right);
                return new Double(l % r);
            }
            // otherwise treat as integers
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            BigInteger result = l.mod(r);
            if (BigInteger.ZERO.equals(r)) {
                return divideZero(l);
            }
            return narrowBigInteger(left, right, result);
        }
    }

    public void test132a() throws Exception {
        Map<String, Object> ns = new HashMap<String, Object>();
        ns.put("math", Math.class);
        JexlEngine jexl = new JexlBuilder().arithmetic(new Arithmetic132()).namespaces(ns).create();

        Object evaluate = jexl.createExpression("1/0").evaluate(null);
        assertTrue(Double.isInfinite((Double)evaluate));

        evaluate = jexl.createExpression("-1/0").evaluate(null);
        assertTrue(Double.isInfinite((Double)evaluate));

        evaluate = jexl.createExpression("1.0/0.0").evaluate(null);
        assertTrue(Double.isInfinite((Double)evaluate));

        evaluate = jexl.createExpression("-1.0/0.0").evaluate(null);
        assertTrue(Double.isInfinite((Double)evaluate));

        evaluate = jexl.createExpression("math:abs(-42)").evaluate(null);
        assertEquals(42, evaluate);
    }

    public void test135() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext jc = new MapContext();
        JexlScript script;
        Object result;
        Map<Integer, Object> foo = new HashMap<Integer, Object>();
        foo.put(3, 42);
        jc.set("state", foo);

        script = jexl.createScript("var y = state[3]; y");
        result = script.execute(jc, foo);
        assertEquals(42, result);

        jc.set("a", 3);
        script = jexl.createScript("var y = state[a]; y");
        result = script.execute(jc, foo);
        assertEquals(42, result);

        jc.set("a", 2);
        script = jexl.createScript("var y = state[a + 1]; y");
        result = script.execute(jc, foo);
        assertEquals(42, result);

        jc.set("a", 2);
        jc.set("b", 1);
        script = jexl.createScript("var y = state[a + b]; y");
        result = script.execute(jc, foo);
        assertEquals(42, result);

        script = jexl.createScript("var y = state[3]; y", "state");
        result = script.execute(null, foo, 3);
        assertEquals(42, result);

        script = jexl.createScript("var y = state[a]; y", "state", "a");
        result = script.execute(null, foo, 3);
        assertEquals(42, result);

        script = jexl.createScript("var y = state[a + 1]; y", "state", "a");
        result = script.execute(null, foo, 2);
        assertEquals(42, result);

        script = jexl.createScript("var y = state[a + b]; y", "state", "a", "b");
        result = script.execute(null, foo, 2, 1);
        assertEquals(42, result);
    }


    @Test
    public void test136() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext jc = new MapContext();
        JexlScript script;
        JexlExpression expr;
        Object result;

        script = jexl.createScript("var x = $TAB[idx]; return x;", "idx");
        jc.set("fn01", script);

        script = jexl.createScript("$TAB = { 1:11, 2:22, 3:33}; IDX=2;");
        script.execute(jc);

        expr = jexl.createExpression("fn01(IDX)");
        result = expr.evaluate(jc);
        assertEquals("EXPR01 result", 22, result);
    }
}
