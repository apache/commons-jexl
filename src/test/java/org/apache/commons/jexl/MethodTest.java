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

import org.apache.commons.jexl.junit.Asserter;

/**
 * Tests for calling methods on objects
 * 
 * @since 2.0
 */
public class MethodTest extends JexlTestCase {

    private Asserter asserter;

    private static final String METHOD_STRING = "Method string";

    public static class TestClass {
        public String testVarArgs(Integer[] args) {
            return "Test";
        }
    }

    public static class Functor {
        public int ten() {
            return 10;
        }
        public int plus10(int num) {
            return num + 10;
        }
        public static int TWENTY() {
            return 20;
        }
        public static int PLUS20(int num) {
            return num + 20;
        }
        public static Class<?> NPEIfNull(Object x) {
            return x.getClass();
        }
    }

    @Override
    public void setUp() {
        asserter = new Asserter(JEXL);
    }

    public void testCallVarArgMethod() throws Exception {
        asserter.setVariable("test", new TestClass());
        asserter.assertExpression("test.testVarArgs(1,2,3,4,5)", "Test");
    }

    public void testInvoke() throws Exception {
        Functor func = new Functor();
        assertEquals(Integer.valueOf(10), JEXL.invokeMethod(func, "ten"));
        assertEquals(Integer.valueOf(42), JEXL.invokeMethod(func, "PLUS20", 22));
        try {
            JEXL.invokeMethod(func, "nonExistentMethod");
            fail("method does not exist!");
        } catch(Exception xj0) {
            // ignore
        }
        try {
            JEXL.invokeMethod(func, "NPEIfNull", null);
            fail("method should have thrown!");
        } catch(Exception xj0) {
            // ignore
        }
    }

    /**
     * test a simple method expression
     */
    public void testMethod() throws Exception {
        // tests a simple method expression
        asserter.setVariable("foo", new Foo());
        asserter.assertExpression("foo.bar()", METHOD_STRING);
    }

    public void testMulti() throws Exception {
        asserter.setVariable("foo", new Foo());
        asserter.assertExpression("foo.innerFoo.bar()", METHOD_STRING);
    }

    /**
     * test some String method calls
     */
    public void testStringMethods() throws Exception {
        asserter.setVariable("foo", "abcdef");
        asserter.assertExpression("foo.substring(3)", "def");
        asserter.assertExpression("foo.substring(0,(size(foo)-3))", "abc");
        asserter.assertExpression("foo.substring(0,size(foo)-3)", "abc");
        asserter.assertExpression("foo.substring(0,foo.length()-3)", "abc");
        asserter.assertExpression("foo.substring(0, 1+1)", "ab");
    }

    /**
     * Ensures static methods on objects can be called.
     */
    public void testStaticMethodInvocation() throws Exception {
        asserter.setVariable("aBool", Boolean.FALSE);
        asserter.assertExpression("aBool.valueOf('true')", Boolean.TRUE);
    }

    public void testStaticMethodInvocationOnClasses() throws Exception {
        asserter.setVariable("Boolean", Boolean.class);
        asserter.assertExpression("Boolean.valueOf('true')", Boolean.TRUE);
    }

    public static class MyMath {
        public double cos(double x) {
            return Math.cos(x);
        }
    }

   public void testTopLevelCall() throws Exception {
        java.util.Map<String, Object> funcs = new java.util.HashMap<String, Object>();
        funcs.put(null, new Functor());
        funcs.put("math", new MyMath());
        JEXL.setFunctions(funcs);

        JexlContext jc = JexlHelper.createContext();

        Expression e = JEXL.createExpression("ten()");
        Object o = e.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);

        e = JEXL.createExpression("plus10(10)");
        o = e.evaluate(jc);
        assertEquals("Result is not 20", new Integer(20), o);

        e = JEXL.createExpression("plus10(ten())");
        o = e.evaluate(jc);
        assertEquals("Result is not 20", new Integer(20), o);

        jc.getVars().put("pi", Math.PI);
        e = JEXL.createExpression("math:cos(pi)");
        o = e.evaluate(jc);
        assertEquals(Double.valueOf(-1),o);
    }

    public void testNamespaceCall() throws Exception {
        java.util.Map<String, Object> funcs = new java.util.HashMap<String, Object>();
        funcs.put("func", new Functor());
        funcs.put("FUNC", Functor.class);
        JEXL.setFunctions(funcs);

        Expression e = JEXL.createExpression("func:ten()");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);

        e = JEXL.createExpression("func:plus10(10)");
        jc = JexlHelper.createContext();
        o = e.evaluate(jc);
        assertEquals("Result is not 20", new Integer(20), o);

        e = JEXL.createExpression("func:plus10(func:ten())");
        jc = JexlHelper.createContext();
        o = e.evaluate(jc);
        assertEquals("Result is not 20", new Integer(20), o);

        e = JEXL.createExpression("FUNC:PLUS20(10)");
        jc = JexlHelper.createContext();
        o = e.evaluate(jc);
        assertEquals("Result is not 30", new Integer(30), o);

        e = JEXL.createExpression("FUNC:PLUS20(FUNC:TWENTY())");
        jc = JexlHelper.createContext();
        o = e.evaluate(jc);
        assertEquals("Result is not 40", new Integer(40), o);
    }

}