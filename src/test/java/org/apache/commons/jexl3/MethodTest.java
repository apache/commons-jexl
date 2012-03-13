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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.junit.Asserter;

/**
 * Tests for calling methods on objects
 *
 * @since 2.0
 */
public class MethodTest extends JexlTestCase {
    private Asserter asserter;
    private static final String METHOD_STRING = "Method string";

    public MethodTest() {
        super("MethodTest");
    }

    public static class VarArgs {
        public String callInts(Integer... args) {
            int result = 0;
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    result += args[i] != null ? args[i].intValue() : -100;
                }
            } else {
                result = -1000;
            }
            return "Varargs:" + result;
        }

        public String callMixed(Integer fixed, Integer... args) {
            int result = fixed.intValue();
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    result += args[i] != null ? args[i].intValue() : -100;
                }
            } else {
                result -= 1000;
            }
            return "Mixed:" + result;
        }

        public String callMixed(String mixed, Integer... args) {
            int result = 0;
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    result += args[i] != null ? args[i].intValue() : -100;
                }
            } else {
                result = -1000;
            }
            return mixed + ":" + result;
        }

        public String concat(String... strs) {
            if (strs.length > 0) {
                StringBuilder strb = new StringBuilder(strs[0]);
                for(int s = 1; s < strs.length; ++s) {
                    strb.append(", ");
                    strb.append(strs[s]);
                }
                return strb.toString();
            } else {
                return "";
            }
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

    public static class EnhancedContext extends JexlEvalContext {
        int factor = 6;
        final Map<String, Object> funcs;

        EnhancedContext(Map<String, Object> funcs) {
            super();
            this.funcs = funcs;
        }

        @Override
        public Object resolveNamespace(String name) {
            return funcs.get(name);
        }
    }

    public static class ContextualFunctor {
        private final EnhancedContext context;

        public ContextualFunctor(EnhancedContext theContext) {
            context = theContext;
        }

        public int ratio(int n) {
            context.factor -= 1;
            return n / context.factor;
        }
    }

    @Override
    public void setUp() {
        asserter = new Asserter(JEXL);
    }

    public void testCallVarArgMethod() throws Exception {
        VarArgs test = new VarArgs();
        asserter.setVariable("test", test);
        asserter.assertExpression("test.callInts()", "Varargs:0");
        asserter.assertExpression("test.callInts(1)", "Varargs:1");
        asserter.assertExpression("test.callInts(1,2,3,4,5)", "Varargs:15");
        asserter.assertExpression("test.concat(['1', '2', '3'])", "1, 2, 3");
        asserter.assertExpression("test.concat('1', '2', '3')", "1, 2, 3");

    }

    public void testCallMixedVarArgMethod() throws Exception {
        VarArgs test = new VarArgs();
        asserter.setVariable("test", test);
        assertEquals("Mixed:1", test.callMixed(Integer.valueOf(1)));
        asserter.assertExpression("test.callMixed(1)", "Mixed:1");
        // Java and JEXL equivalent behavior: 'Mixed:-999' expected
        //{
        assertEquals("Mixed:-999", test.callMixed(Integer.valueOf(1), (Integer[]) null));
        asserter.assertExpression("test.callMixed(1, null)", "Mixed:-999");
        //}
        asserter.assertExpression("test.callMixed(1,2)", "Mixed:3");
        asserter.assertExpression("test.callMixed(1,2,3,4,5)", "Mixed:15");
    }

    public void testCallJexlVarArgMethod() throws Exception {
        VarArgs test = new VarArgs();
        asserter.setVariable("test", test);
        assertEquals("jexl:0", test.callMixed("jexl"));
        asserter.assertExpression("test.callMixed('jexl')", "jexl:0");
        // Java and JEXL equivalent behavior: 'jexl:-1000' expected
        //{
        assertEquals("jexl:-1000", test.callMixed("jexl", (Integer[]) null));
        asserter.assertExpression("test.callMixed('jexl', null)", "jexl:-1000");
        //}
        asserter.assertExpression("test.callMixed('jexl', 2)", "jexl:2");
        asserter.assertExpression("test.callMixed('jexl',2,3,4,5)", "jexl:14");
    }

    public void testInvoke() throws Exception {
        Functor func = new Functor();
        assertEquals(Integer.valueOf(10), JEXL.invokeMethod(func, "ten"));
        assertEquals(Integer.valueOf(42), JEXL.invokeMethod(func, "PLUS20", Integer.valueOf(22)));
        try {
            JEXL.invokeMethod(func, "nonExistentMethod");
            fail("method does not exist!");
        } catch (Exception xj0) {
            // ignore
        }
        try {
            JEXL.invokeMethod(func, "NPEIfNull", (Object[]) null);
            fail("method should have thrown!");
        } catch (Exception xj0) {
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
        funcs.put("cx", ContextualFunctor.class);

        EnhancedContext jc = new EnhancedContext(funcs);

        JexlExpression e = JEXL.createExpression("ten()");
        Object o = e.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);

        e = JEXL.createExpression("plus10(10)");
        o = e.evaluate(jc);
        assertEquals("Result is not 20", new Integer(20), o);

        e = JEXL.createExpression("plus10(ten())");
        o = e.evaluate(jc);
        assertEquals("Result is not 20", new Integer(20), o);

        jc.set("pi", new Double(Math.PI));
        e = JEXL.createExpression("math:cos(pi)");
        o = e.evaluate(jc);
        assertEquals(Double.valueOf(-1), o);

        e = JEXL.createExpression("cx:ratio(10) + cx:ratio(20)");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(7), o);
    }

    public void testNamespaceCall() throws Exception {
        java.util.Map<String, Object> funcs = new java.util.HashMap<String, Object>();
        funcs.put("func", new Functor());
        funcs.put("FUNC", Functor.class);

        JexlExpression e = JEXL.createExpression("func:ten()");
        JexlEvalContext jc = new EnhancedContext(funcs);

        Object o = e.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);

        e = JEXL.createExpression("func:plus10(10)");
        o = e.evaluate(jc);
        assertEquals("Result is not 20", new Integer(20), o);

        e = JEXL.createExpression("func:plus10(func:ten())");
        o = e.evaluate(jc);
        assertEquals("Result is not 20", new Integer(20), o);

        e = JEXL.createExpression("FUNC:PLUS20(10)");
        o = e.evaluate(jc);
        assertEquals("Result is not 30", new Integer(30), o);

        e = JEXL.createExpression("FUNC:PLUS20(FUNC:TWENTY())");
        o = e.evaluate(jc);
        assertEquals("Result is not 40", new Integer(40), o);
    }

    public static class ScriptContext extends MapContext implements JexlContext.NamespaceResolver {
        Map<String, Object> nsScript;

        ScriptContext(Map<String, Object> ns) {
            nsScript = ns;
        }

        @Override
        public Object resolveNamespace(String name) {
            if (name == null) {
                return this;
            }
            if ("script".equals(name)) {
                return nsScript;
            }
            return null;
        }
    }

    public void testScriptCall() throws Exception {
        JexlContext context = new MapContext();
        JexlScript plus = JEXL.createScript("a + b", new String[]{"a", "b"});
        context.set("plus", plus);
        JexlScript forty2 = JEXL.createScript("plus(4, 2) * plus(4, 3)");
        Object o = forty2.execute(context);
        assertEquals("Result is not 42", new Integer(42), o);

        Map<String, Object> foo = new HashMap<String, Object>();
        foo.put("plus", plus);
        context.set("foo", foo);
        forty2 = JEXL.createScript("foo.plus(4, 2) * foo.plus(4, 3)");
        o = forty2.execute(context);
        assertEquals("Result is not 42", new Integer(42), o);

        context = new ScriptContext(foo);
        forty2 = JEXL.createScript("script:plus(4, 2) * script:plus(4, 3)");
        o = forty2.execute(context);
        assertEquals("Result is not 42", new Integer(42), o);

        final JexlArithmetic ja = JEXL.getArithmetic();
        JexlMethod mplus = new JexlMethod() {
            @Override
            public Object invoke(Object obj, Object[] params) throws Exception {
                if (obj instanceof Map<?, ?>) {
                    return ja.add(params[0], params[1]);
                } else {
                    throw new Exception("not a script context");
                }
            }

            @Override
            public Object tryInvoke(String name, Object obj, Object[] params) {
                try {
                    if ("plus".equals(name)) {
                        return invoke(obj, params);
                    }
                } catch (Exception xany) {
                    // ignore and fail by returning this
                }
                return this;
            }

            @Override
            public boolean tryFailed(Object rval) {
                // this is the marker for failure
                return rval == this;
            }

            @Override
            public boolean isCacheable() {
                return true;
            }

            @Override
            public Class<?> getReturnType() {
                return Object.class;
            }
        };

        foo.put("PLUS", mplus);
        forty2 = JEXL.createScript("script:PLUS(4, 2) * script:PLUS(4, 3)");
        o = forty2.execute(context);
        assertEquals("Result is not 42", new Integer(42), o);

        context.set("foo.bar", foo);
        forty2 = JEXL.createScript("foo.'bar'.PLUS(4, 2) * foo.bar.PLUS(4, 3)");
        o = forty2.execute(context);
        assertEquals("Result is not 42", new Integer(42), o);
    }
}