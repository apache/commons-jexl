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
import org.apache.commons.jexl3.internal.introspection.Uberspect;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
//import org.apache.commons.beanutils.LazyDynaMap;

/**
 * Test cases for reported issue between JEXL-1 and JEXL-100.
 */
@SuppressWarnings({"boxing", "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class IssuesTest extends JexlTestCase {
    public IssuesTest() {
        super("IssuesTest", null);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    // JEXL-49: blocks not parsed (fixed)
    @Test
    public void test49() throws Exception {
        JexlEngine jexl = new Engine();
        Map<String, Object> vars = new HashMap<String, Object>();
        JexlContext ctxt = new MapContext(vars);
        String stmt = "a = 'b'; c = 'd';";
        JexlScript expr = jexl.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertTrue("JEXL-49 is not fixed", vars.get("a").equals("b") && vars.get("c").equals("d"));
    }

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

    public static class Foo {
        private final Another inner;

        Foo() {
            inner = new Another();
        }

        public Another getInner() {
            return inner;
        }
    }

    @Test
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
            Assert.fail("Should have failed due to invalid assignment");
        } catch (JexlException.Assignment xparse) {
            String dbg = xparse.toString();
        } catch (JexlException xjexl) {
            Assert.fail("Should have thrown a parse exception");
        }
    }

    // JEXL-47: C style comments (single & multi line) (fixed in Parser.jjt)
    // JEXL-44: comments dont allow double quotes (fixed in Parser.jjt)
    @Test
    public void test47() throws Exception {
        JexlEngine jexl = new Engine();
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);

        JexlExpression expr = jexl.createExpression("true//false\n");
        Object value = expr.evaluate(ctxt);
        Assert.assertTrue("should be true", (Boolean) value);

        expr = jexl.createExpression("/*true*/false");
        value = expr.evaluate(ctxt);
        Assert.assertFalse("should be false", (Boolean) value);

        expr = jexl.createExpression("/*\"true\"*/false");
        value = expr.evaluate(ctxt);
        Assert.assertFalse("should be false", (Boolean) value);
    }

    // JEXL-42: NullPointerException evaluating an expression
    // fixed in JexlArithmetic by allowing add operator to deal with string, null
    @Test
    public void test42() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JxltEngine uel = jexl.createJxltEngine();
        // ensure errors will throw
        //jexl.setSilent(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrict(false);
        ctxt.set("ax", "ok");

        JxltEngine.Expression expr = uel.createExpression("${ax+(bx)}");
        Object value = expr.evaluate(ctxt);
        Assert.assertTrue("should be ok", "ok".equals(value));
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

    @Test
    public void test40() throws Exception {
        JexlEngine jexl = new Engine();
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);

        ctxt.set("derived", new Derived());

        JexlExpression expr = jexl.createExpression("derived.foo()");
        Object value = expr.evaluate(ctxt);
        Assert.assertTrue("should be true", (Boolean) value);
    }

    // JEXL-52: can be implemented by deriving Interpreter.{g,s}etAttribute; later
    @Test
    public void test52base() throws Exception {
        Engine jexl = (Engine) createEngine(false);
        Uberspect uber = (Uberspect) jexl.getUberspect();
        // most likely, call will be in an Interpreter, getUberspect
        String[] names = uber.getMethodNames(Another.class);
        Assert.assertTrue("should find methods", names.length > 0);
        int found = 0;
        for (String name : names) {
            if ("foo".equals(name) || "goo".equals(name)) {
                found += 1;
            }
        }
        Assert.assertTrue("should have foo & goo", found == 2);

        names = uber.getFieldNames(Another.class);
        Assert.assertTrue("should find fields", names.length > 0);
        found = 0;
        for (String name : names) {
            if ("name".equals(name)) {
                found += 1;
            }
        }
        Assert.assertTrue("should have name", found == 1);
    }

    // JEXL-10/JEXL-11: variable checking, null operand is error
    @Test
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
                Assert.fail(exprs[e] + " : should have failed due to null argument");
            } catch (JexlException xjexl) {
                // expected
            }
        }
    }

    // JEXL-62
    @Test
    public void test62() throws Exception {
        JexlEngine jexl = createEngine(false);
        MapContext vars = new MapContext();
        JexlEvalContext ctxt = new JexlEvalContext(vars);
        ctxt.setStrict(true);
        ctxt.setSilent(true);// to avoid throwing JexlException on null method call

        JexlScript jscript;

        jscript = jexl.createScript("dummy.hashCode()");
        Assert.assertEquals(jscript.getSourceText(), null, jscript.execute(ctxt)); // OK

        ctxt.set("dummy", "abcd");
        Assert.assertEquals(jscript.getSourceText(), Integer.valueOf("abcd".hashCode()), jscript.execute(ctxt)); // OK

        jscript = jexl.createScript("dummy.hashCode");
        Assert.assertEquals(jscript.getSourceText(), null, jscript.execute(ctxt)); // OK

        JexlExpression jexpr;
        vars.clear();
        jexpr = jexl.createExpression("dummy.hashCode()");
        Assert.assertEquals(jexpr.toString(), null, jexpr.evaluate(ctxt)); // OK

        ctxt.set("dummy", "abcd");
        Assert.assertEquals(jexpr.toString(), Integer.valueOf("abcd".hashCode()), jexpr.evaluate(ctxt)); // OK

        jexpr = jexl.createExpression("dummy.hashCode");
        Assert.assertEquals(jexpr.toString(), null, jexpr.evaluate(ctxt)); // OK
    }

    // JEXL-73
    @Test
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
            Assert.fail("c.e not declared as variable");
        } catch (JexlException.Variable xjexl) {
            String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("c.e") > 0);
        }

        ctxt.set("c", "{ 'a' : 3, 'b' : 5}");
        ctxt.set("e", Integer.valueOf(2));
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e not accessible as property");
        } catch (JexlException.Property xjexl) {
            String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("e") > 0);
        }

    }

    // JEXL-87
    @Test
    public void test87() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        JexlExpression divide = jexl.createExpression("l / r");
        JexlExpression modulo = jexl.createExpression("l % r");

        ctxt.set("l", java.math.BigInteger.valueOf(7));
        ctxt.set("r", java.math.BigInteger.valueOf(2));
        Assert.assertEquals(java.math.BigInteger.valueOf(3), divide.evaluate(ctxt));
        Assert.assertTrue(jexl.getArithmetic().equals(1, modulo.evaluate(ctxt)));

        ctxt.set("l", java.math.BigDecimal.valueOf(7));
        ctxt.set("r", java.math.BigDecimal.valueOf(2));
        Assert.assertEquals(java.math.BigDecimal.valueOf(3.5), divide.evaluate(ctxt));
        Assert.assertTrue(jexl.getArithmetic().equals(1, modulo.evaluate(ctxt)));
    }

    // JEXL-90
    @Test
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
                Assert.fail(fexprs[f] + ": Should have failed in parse");
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
            Assert.assertEquals(Integer.valueOf(2), s.execute(ctxt));
        }
        debuggerCheck(jexl);
    }

    // JEXL-44
    @Test
    public void test44() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        JexlScript script;
        script = jexl.createScript("'hello world!'//commented");
        Assert.assertEquals("hello world!", script.execute(ctxt));
        script = jexl.createScript("'hello world!';//commented\n'bye...'");
        Assert.assertEquals("bye...", script.execute(ctxt));
        script = jexl.createScript("'hello world!'## commented");
        Assert.assertEquals("hello world!", script.execute(ctxt));
        script = jexl.createScript("'hello world!';## commented\n'bye...'");
        Assert.assertEquals("bye...", script.execute(ctxt));
    }

    @Test
    public void test97() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        for (char v = 'a'; v <= 'z'; ++v) {
            ctxt.set(Character.toString(v), 10);
        }
        String input
                = "(((((((((((((((((((((((((z+y)/x)*w)-v)*u)/t)-s)*r)/q)+p)-o)*n)-m)+l)*k)+j)/i)+h)*g)+f)/e)+d)-c)/b)+a)";

        JexlExpression script;
        // Make sure everything is loaded...
        long start = System.nanoTime();
        script = jexl.createExpression(input);
        Object value = script.evaluate(ctxt);
        Assert.assertEquals(Integer.valueOf(11), value);
        long end = System.nanoTime();
        double millisec = (end - start) / 1e6;
        double limit = 200.0; // Allow plenty of slack
        Assert.assertTrue("Expected parse to take less than " + limit + "ms, actual " + millisec, millisec < limit);
    }

    public static class fn98 {
        public String replace(String str, String target, String replacement) {
            return str.replace(target, replacement);
        }
    }

    @Test
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
            Assert.assertEquals(expr, "DOMAIN\\\\somename", value);
        }
    }

}
