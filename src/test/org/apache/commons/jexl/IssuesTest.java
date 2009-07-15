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
import java.util.Map;
import junit.framework.TestCase;

/**
 * Test cases for reported issues
 */
public class IssuesTest  extends TestCase {

    // JEXL-52: can be implemented by deriving Interpreter.{g,s}etAttribute
    // JEXL-50: can be implemented through a namespace:function or through JexlArithmetic derivation

    // JEXL-49: blocks not parsed (fixed)
    public void test49() throws Exception {
        JexlContext ctxt = JexlHelper.createContext();
        Map vars = ctxt.getVars();

        String stmt = "{a = 'b'; c = 'd';}";
        Script expr = ScriptFactory.createScript(stmt);
        Object value = expr.execute(ctxt);
        assertTrue("JEXL-49 is not fixed", vars.get("a").equals("b") && vars.get("c").equals("d"));
    }


    // JEXL-48: bad assignment detection
     public static class Another {
        private Boolean foo = true;

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
        Expression e = jexl.createExpression( jexlExp );
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Foo() );

        try {
            Object o = e.evaluate(jc);
            fail("Should have failed due to invalid assignment");
        }
        catch(JexlException xjexl) {
            // expected
        }
    }

    // JEXL-47: C style comments (single & multi line) (fixed)
    // JEXL-44: comments dont allow double quotes (fixed)
    public void test47() throws Exception {
        JexlEngine jexl = new JexlEngine();
        // ensure errors will throw
        jexl.setSilent(false);
        JexlContext ctxt = JexlHelper.createContext();

        Expression expr = jexl.createExpression( "true//false\n" );
        Object value = expr.evaluate(ctxt);
        assertTrue("should be true", (Boolean) value);

        expr = jexl.createExpression( "/*true*/false" );
        value = expr.evaluate(ctxt);
        assertFalse("should be false", (Boolean) value);

        expr = jexl.createExpression( "/*\"true\"*/false" );
        value = expr.evaluate(ctxt);
        assertFalse("should be false", (Boolean) value);
    }

    // JEXL-46: regexp syntax; should we really add more syntactic elements?
    // JEXL-45: unhandled division by zero (fixed see ArithmeticTest)

    // JEXL-42: NullPointerException evaluating an expression (fixed)
    public void test42() throws Exception {
        JexlEngine jexl = new JexlEngine();
        UnifiedJEXL uel = new UnifiedJEXL(jexl);
        // ensure errors will throw
        //jexl.setSilent(false);
        JexlContext ctxt = JexlHelper.createContext();
        ctxt.getVars().put("ax", "ok" );

        UnifiedJEXL.Expression expr = uel.parse( "${ax+(bx)}" );
        Object value = expr.evaluate(ctxt);
        assertTrue("should be ok", "ok".equals(value));
    }

    public static void main(String[] args) throws Exception {
        new IssuesTest().test42();
    }

}
