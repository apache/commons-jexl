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
package org.apache.commons.jexl;

import org.apache.commons.jexl.parser.ParseException;
import java.util.Map;

/**
 * Test cases for reported issues
 */
public class IssuesTest extends JexlTestCase {
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        //java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    // JEXL-49: blocks not parsed (fixed)
    public void test49() throws Exception {
        JexlContext ctxt = JexlHelper.createContext();
        String stmt = "{a = 'b'; c = 'd';}";
        Script expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Map<String, Object> vars = ctxt.getVars();
        assertTrue("JEXL-49 is not fixed", vars.get("a").equals("b") && vars.get("c").equals("d"));
    }

    // JEXL-48: bad assignment detection
    public static class Another {
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
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Foo());

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
        JexlContext ctxt = JexlHelper.createContext();

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
    // fixed in JexlArithmetic by allowing add to deal with string, null
    public void test42() throws Exception {
        JexlEngine jexl = new JexlEngine();
        UnifiedJEXL uel = new UnifiedJEXL(jexl);
        // ensure errors will throw
        //jexl.setSilent(false);
        JexlContext ctxt = JexlHelper.createContext();
        ctxt.getVars().put("ax", "ok");

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
        JexlContext ctxt = JexlHelper.createContext();
        ctxt.getVars().put("derived", new Derived());

        Expression expr = jexl.createExpression("derived.foo()");
        Object value = expr.evaluate(ctxt);
        assertTrue("should be true", ((Boolean) value).booleanValue());
    }

    // JEXL-52: can be implemented by deriving Interpreter.{g,s}etAttribute; later
    public void test52base() throws Exception {
        JexlEngine jexl = new JexlEngine();
        // most likely, call will be in an Interpreter, getUberspect
        String[] names = jexl.uberspect.getIntrospector().getMethodNames(Another.class);
        assertTrue("should find methods", names.length > 0);
        int found = 0;
        for (String name : names) {
            if ("foo".equals(name) || "goo".equals(name)) {
                found += 1;
            }
        }
        assertTrue("should have foo & goo", found == 2);
    }

    // JEXL-10/JEXL-11: variable checking, null operand is error
    public void test11() throws Exception {
        JexlEngine jexl = new JexlEngine();
        // ensure errors will throw
        jexl.setSilent(false);
        jexl.setLenient(false);
        JexlContext ctxt = JexlHelper.createContext();
        ctxt.getVars().put("a", null);

        String[] exprs = {
            "10 + null",
            "a - 10",
            "b * 10",
            "a % b",
            "1000 / a"
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
        JexlEngine jexl = new JexlEngine();
        jexl.setSilent(true); // to avoid throwing JexlException on null method call

        Script jscript;

        ctxt = JexlHelper.createContext();
        jscript = jexl.createScript("dummy.hashCode()");
        assertEquals(jscript.getText(), null, jscript.execute(ctxt)); // OK

        ctxt.getVars().put("dummy", "abcd");
        assertEquals(jscript.getText(), Integer.valueOf("abcd".hashCode()), jscript.execute(ctxt)); // OK

        jscript = jexl.createScript("dummy.hashCode");
        assertEquals(jscript.getText(), null, jscript.execute(ctxt)); // OK

        Expression jexpr;

        ctxt = JexlHelper.createContext();
        jexpr = jexl.createExpression("dummy.hashCode()");
        assertEquals(jexpr.getExpression(), null, jexpr.evaluate(ctxt)); // OK

        ctxt.getVars().put("dummy", "abcd");
        assertEquals(jexpr.getExpression(), Integer.valueOf("abcd".hashCode()), jexpr.evaluate(ctxt)); // OK

        jexpr = jexl.createExpression("dummy.hashCode");
        assertEquals(jexpr.getExpression(), null, jexpr.evaluate(ctxt)); // OK
    }

    // JEXL-73
    public void test73() throws Exception {
        JexlContext ctxt = JexlHelper.createContext();
        JexlEngine jexl = new JexlEngine();
        jexl.setSilent(false);
        jexl.setLenient(false);
        Expression e;
        e = jexl.createExpression("c.e");
        try {
            /* Object o = */ e.evaluate(ctxt);
        } catch (JexlException xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("variable c.e") > 0);
        }

        ctxt.getVars().put("c", "{ 'a' : 3, 'b' : 5}");
        ctxt.getVars().put("e", Integer.valueOf(2));
        try {
            /* Object o = */ e.evaluate(ctxt);
        } catch (JexlException xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("variable c.e") > 0);
        }

    }

    // JEXL-87
    public void test87() throws Exception {
        JexlContext ctxt = JexlHelper.createContext();
        JexlEngine jexl = new JexlEngine();
        jexl.setSilent(false);
        jexl.setLenient(false);
        Expression divide = jexl.createExpression("l / r");
        Expression modulo = jexl.createExpression("l % r");

        ctxt.getVars().put("l", java.math.BigInteger.valueOf(7));
        ctxt.getVars().put("r", java.math.BigInteger.valueOf(2));
        assertEquals("3", divide.evaluate(ctxt).toString());
        assertEquals("1", modulo.evaluate(ctxt).toString());

        ctxt.getVars().put("l", java.math.BigDecimal.valueOf(7));
        ctxt.getVars().put("r", java.math.BigDecimal.valueOf(2));
        assertEquals("3.5", divide.evaluate(ctxt).toString());
        assertEquals("1", modulo.evaluate(ctxt).toString());
    }

    // JEXL-90
    public void test90() throws Exception {
        JexlContext ctxt = JexlHelper.createContext();
        JexlEngine jexl = new JexlEngine();
        jexl.setSilent(false);
        jexl.setLenient(false);
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
            } catch (ParseException xany) {
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
        ctxt.getVars().put("x", Boolean.FALSE);
        ctxt.getVars().put("y", Boolean.TRUE);
        for (int e = 0; e < exprs.length; ++e) {
            Script s = jexl.createScript(exprs[e]);
            assertEquals(Integer.valueOf(2), s.execute(ctxt));
        }
        debuggerCheck(jexl);
    }

    // JEXL-44
    public void test44() throws Exception {
        JexlContext ctxt = JexlHelper.createContext();
        JexlEngine jexl = new JexlEngine();
        jexl.setSilent(false);
        jexl.setLenient(false);
        Script script;
        script = jexl.createScript("'hello world!'//commented");
        assertEquals("hello world!", script.execute(ctxt));
        script = jexl.createScript("'hello world!';//commented\n'bye...'");
        assertEquals("bye...", script.execute(ctxt));
    }
}
