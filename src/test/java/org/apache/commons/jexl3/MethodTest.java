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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.junit.Asserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for calling methods on objects
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class MethodTest extends JexlTestCase {
    public static class ContextualFunctor {
        private final EnhancedContext context;

        public ContextualFunctor(final EnhancedContext theContext) {
            context = theContext;
        }

        public int ratio(final int n) {
            context.factor -= 1;
            return n / context.factor;
        }
    }
    public static class Edge {
        private Edge() {
        }

        public int exec(final Boolean x, final int arg) {
            return 1;
        }

        public int exec(final Boolean x, final int[] arg) {
            return 20;
        }

        public int exec(final Boolean x, final Object args) {
            return 3;
        }

        public int exec(final Boolean x, final Object... args) {
            return 4;
        }

        public int exec(final Boolean x, final String arg) {
            return 2;
        }

        public int exec(final int arg) {
            return 1;
        }

        public int exec(final int[] arg) {
            return 20;
        }

        public int exec(final Object args) {
            return 3;
        }

        public int exec(final Object... args) {
            return 4;
        }

        public int exec(final String arg) {
            return 2;
        }

        public int exec(final String... arg) {
            return 200;
        }

        public Class<?>[] execute(final Object... args) {
            final Class<?>[] clazz = new Class<?>[args.length];
            for (int a = 0; a < args.length; ++a) {
                clazz[a] = args[a] != null ? args[a].getClass() : Void.class;
            }
            return clazz;
        }
    }

    public static class EnhancedContext extends JexlEvalContext {
        int factor = 6;
        final Map<String, Object> funcs;

        EnhancedContext(final Map<String, Object> funcs) {
            this.funcs = funcs;
        }

        @Override
        public Object resolveNamespace(final String name) {
            return funcs.get(name);
        }
    }

    public static class Functor {
        public static Class<?> NPEIfNull(final Object x) {
            return x.getClass();
        }
        public static int PLUS20(final int num) {
            return num + 20;
        }

        public static int TWENTY() {
            return 20;
        }

        private boolean overKill;

        private String under;

        public String getUnder() {
            if (overKill) {
                throw new UnsupportedOperationException("kill " + under);
            }
            return under;
        }

        public Object over(final String f, final Date g) {
            return f + " + " + g;
        }

        public Object over(final String f, final int i) {
            if (overKill) {
                throw new UnsupportedOperationException("kill " + f + " + " + i);
            }
            return f + " + " + i;
        }

        public Object over(final String f, final String g) {
            return f + " + " + g;
        }

        public int plus10(final int num) {
            return num + 10;
        }

        void setKill(final boolean ok) {
            overKill = ok;
        }

        public void setUnder(final String str) {
            if (overKill) {
                throw new UnsupportedOperationException("kill " + str);
            }
            under = str;
        }

        public int ten() {
            return 10;
        }
    }

    public static class FunctorOver extends Functor {

        public Object over(final Object f, final Object g) {
            return f + " + " + g;
        }
    }

    public static class MyMath {
        public double cos(final double x) {
            return Math.cos(x);
        }
    }

    public static class ScriptContext extends MapContext implements JexlContext.NamespaceResolver {
        Map<String, Object> nsScript;

        ScriptContext(final Map<String, Object> ns) {
            nsScript = ns;
        }

        @Override
        public Object resolveNamespace(final String name) {
            if (name == null) {
                return this;
            }
            if ("script".equals(name)) {
                return nsScript;
            }
            if ("functor".equals(name)) {
                return (NamespaceFunctor) context -> {
                    final Map<String, Object> values = new HashMap<>();
                    if ("gin".equals(context.get("base"))) {
                        values.put("drink", "gin fizz");
                    } else {
                        values.put("drink", "champaign");
                    }
                    return values;
                };
            }
            return null;
        }
    }

    public static class VarArgs {
        public String callInts() {
            final int result = -5000;
            return "Varargs:" + result;
        }

        public String callInts(final Integer... args) {
            int result = 0;
            if (args != null) {
                for (final Integer arg : args) {
                    result += arg != null ? arg : -100;
                }
            } else {
                result = -1000;
            }
            return "Varargs:" + result;
        }

        public String callMixed(final Integer fixed, final Integer... args) {
            int result = fixed;
            if (args != null) {
                for (final Integer arg : args) {
                    result += arg != null ? arg : -100;
                }
            } else {
                result -= 1000;
            }
            return "Mixed:" + result;
        }

        public String callMixed(final String mixed, final Integer... args) {
            int result = 0;
            if (args != null) {
                for (final Integer arg : args) {
                    result += arg != null ? arg : -100;
                }
            } else {
                result = -1000;
            }
            return mixed + ":" + result;
        }

        public String concat(final String... strs) {
            if (strs.length <= 0) {
                return "";
            }
            final StringBuilder strb = new StringBuilder(strs[0]);
            for (int s = 1; s < strs.length; ++s) {
                strb.append(", ");
                strb.append(strs[s]);
            }
            return strb.toString();
        }
    }

    public static class ZArithmetic extends JexlArithmetic {
        public ZArithmetic(final boolean astrict) {
            super(astrict);
        }

        public int zzzz(final int z) {
            return 38 + z;
        }
    }

    public static class ZContext extends MapContext {
        public ZContext(final Map<String,Object> map) {
            super(map);
        }

        public int z(final int z) {
            return 181 + z;
        }

        public int zz(final int z) {
            return 40 + z;
        }
    }

    public static class ZSpace {
        public int zzz(final int z) {
            return 39 + z;
        }
    }

    private static final String METHOD_STRING = "Method string";

    private Asserter asserter;

    public MethodTest() {
        super("MethodTest");
    }

    private boolean eqExecute(final Object lhs, final Object rhs) {
        if (lhs instanceof Class<?>[] && rhs instanceof Class<?>[]) {
            final Class<?>[] lhsa = (Class<?>[]) lhs;
            final Class<?>[] rhsa = (Class<?>[]) rhs;
            return Arrays.deepEquals(lhsa, rhsa);
        }
        return false;
    }

    @BeforeEach
    @Override
    public void setUp() {
        asserter = new Asserter(JEXL);
    }

    @Test
    public void testAmbiguousInvoke() throws Exception {
        // JEXL-299
        final Functor func = new Functor();
        final JexlContext ctxt = new MapContext();
        ctxt.set("func", func);
        Object result;
        // basic call works
        result = JEXL.invokeMethod(func, "over", "foo", 42);
        assertEquals("foo + 42", result);
        // ambiguous call fails
        try {
            JEXL.invokeMethod(func, "over", "not null", null);
            fail("should be ambiguous");
        } catch (final JexlException.Method xinvoke) {
            assertEquals("over(String, Object)", xinvoke.getMethodSignature());
        }

        // another ambiguous call fails
        try {
            final String[] arg2 = {"more", "than", "one"};
            JEXL.invokeMethod(func, "over", "not null", arg2);
            fail("should be ambiguous");
        } catch (final JexlException.Method xinvoke) {
            assertEquals("over(String, String[])", xinvoke.getMethodSignature());
        }
    }

    @Test
    public void testCallJexlVarArgMethod() throws Exception {
        final VarArgs test = new VarArgs();
        asserter.setVariable("test", test);
        assertEquals("jexl:0", test.callMixed("jexl"));
        asserter.assertExpression("test.callMixed('jexl')", "jexl:0");
        // Java and JEXL equivalent behavior: 'jexl:-1000' expected
        //{
        assertEquals("jexl:-1000", test.callMixed("jexl", (Integer[]) null));
        asserter.assertExpression("test.callMixed('jexl', null)", "jexl:-1000");
        //}
        asserter.assertExpression("test.callMixed('jexl', 2)", test.callMixed("jexl", 2));
        asserter.assertExpression("test.callMixed('jexl',2,3,4,5)", test.callMixed("jexl", 2, 3, 4, 5));
    }

    @Test
    public void testCallMixedVarArgMethod() throws Exception {
        final VarArgs test = new VarArgs();
        asserter.setVariable("test", test);
        assertEquals("Mixed:1", test.callMixed(Integer.valueOf(1)));
        asserter.assertExpression("test.callMixed(1)", test.callMixed(1));
        // Java and JEXL equivalent behavior: 'Mixed:-999' expected
        //{
        assertEquals("Mixed:-999", test.callMixed(Integer.valueOf(1), (Integer[]) null));
        asserter.assertExpression("test.callMixed(1, null)", "Mixed:-999");
        //}
        asserter.assertExpression("test.callMixed(1,2)", test.callMixed(1, 2));
        asserter.assertExpression("test.callMixed(1,2,3,4,5)", test.callMixed(1, 2, 3, 4, 5));
    }

    @Test
    public void testCallVarArgMethod() throws Exception {
        final VarArgs test = new VarArgs();
        asserter.setVariable("test", test);
        asserter.assertExpression("test.callInts()", test.callInts());
        asserter.assertExpression("test.callInts(1)", test.callInts(1));
        asserter.assertExpression("test.callInts(1,2,3,4,5)", test.callInts(1, 2, 3, 4, 5));
        asserter.assertExpression("test.concat(['1', '2', '3'])", test.concat(new String[]{"1", "2", "3"}));
        asserter.assertExpression("test.concat('1', '2', '3')", test.concat("1", "2", "3"));

    }

    @Test
    public void testFizzCall() throws Exception {
        final ScriptContext context = new ScriptContext(new HashMap<>());

        JexlScript bar = JEXL.createScript("functor:get('drink')");
        Object o;
        o = bar.execute(context);
        assertEquals("champaign", o, "Wrong choice");
        context.set("base", "gin");
        o = bar.execute(context);
        assertEquals("gin fizz", o, "Wrong choice");

        // despite being called twice, the functor is created only once.
        context.set("base", "wine");
        bar = JEXL.createScript("var glass = functor:get('drink'); base = 'gin'; functor:get('drink')");
        o = bar.execute(context);
        assertEquals("champaign", o, "Wrong choice");
    }

    @Test
    public void testInvoke() throws Exception {
        Functor func = new Functor();
        assertEquals(Integer.valueOf(10), JEXL.invokeMethod(func, "ten"));
        assertEquals(Integer.valueOf(42), JEXL.invokeMethod(func, "PLUS20", Integer.valueOf(22)));
        try {
            JEXL.invokeMethod(func, "nonExistentMethod");
            fail("method does not exist!");
        } catch (final Exception xj0) {
            // ignore
        }
        try {
            JEXL.invokeMethod(func, "NPEIfNull", (Object[]) null);
            fail("method should have thrown!");
        } catch (final Exception xj0) {
            // ignore
        }

        Object result;
        try {
            result = JEXL.invokeMethod(func, "over", "foo", 42);
            assertEquals("foo + 42", result);
        } catch (final Exception xj0) {
            // ignore
            result = xj0;
        }

        try {
            result = JEXL.invokeMethod(func, "over", null, null);
            fail("method should have thrown!");
        } catch (final Exception xj0) {
            // ignore
            result = xj0;
        }

        func = new FunctorOver();
        try {
            result = JEXL.invokeMethod(func, "over", null, null);
            assertEquals("null + null", result);
        } catch (final Exception xj0) {
            fail("method should not have thrown!");
        }
    }

    /**
     * test a simple method expression
     */
    @Test
    public void testMethod() throws Exception {
        // tests a simple method expression
        asserter.setVariable("foo", new Foo());
        asserter.assertExpression("foo.bar()", METHOD_STRING);
    }

    @Test
    public void testMulti() throws Exception {
        asserter.setVariable("foo", new Foo());
        asserter.assertExpression("foo.innerFoo.bar()", METHOD_STRING);
    }

    @Test
    public void testNamespaceCall() throws Exception {
        final java.util.Map<String, Object> funcs = new java.util.HashMap<>();
        funcs.put("func", new Functor());
        funcs.put("FUNC", Functor.class);

        JexlExpression e = JEXL.createExpression("func:ten()");
        final JexlEvalContext jc = new EnhancedContext(funcs);

        Object o = e.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);

        e = JEXL.createExpression("func:plus10(10)");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(20), o);

        e = JEXL.createExpression("func:plus10(func:ten())");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(20), o);

        e = JEXL.createExpression("FUNC:PLUS20(10)");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(30), o);

        e = JEXL.createExpression("FUNC:PLUS20(FUNC:TWENTY())");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(40), o);
    }

    @Test
    public void testNamespaceCallEdge() throws Exception {
        final java.util.Map<String, Object> funcs = new java.util.HashMap<>();
        final Edge func = new Edge();
        funcs.put("func", func);

        Object o;
        Object c;
        JexlExpression e;
        final JexlEvalContext jc = new EnhancedContext(funcs);
        try {
            for (int i = 0; i < 2; ++i) {
                e = JEXL.createExpression("func:exec([1, 2])");
                o = e.evaluate(jc);
                assertEquals(20, o, "exec(int[] arg): " + i);

                e = JEXL.createExpression("func:exec(1, 2)");
                o = e.evaluate(jc);
                assertEquals(4, o, "exec(Object... args): " + i);

                e = JEXL.createExpression("func:exec([10.0, 20.0])");
                o = e.evaluate(jc);
                assertEquals(3, o, "exec(Object args): " + i);

                e = JEXL.createExpression("func:exec('1', 2)");
                o = e.evaluate(jc);
                assertEquals(4, o, "exec(Object... args): " + i);

                // no way to differentiate between a single arg call with an array and a vararg call with same args
                assertEquals(func.exec("1", "2"), func.exec(new String[]{"1", "2"}), "exec(String... args): " + i);
                e = JEXL.createExpression("func:exec(['1', '2'])");
                o = e.evaluate(jc);
                assertEquals(func.exec(new String[]{"1", "2"}), o, "exec(String... args): " + i);
                e = JEXL.createExpression("func:exec('1', '2')");
                o = e.evaluate(jc);
                assertEquals(func.exec("1", "2"), o, "exec(String... args): " + i);

                e = JEXL.createExpression("func:exec(true, [1, 2])");
                o = e.evaluate(jc);
                assertEquals(20, o, "exec(int[] arg): " + i);

                e = JEXL.createExpression("func:exec(true, 1, 2)");
                o = e.evaluate(jc);
                assertEquals(4, o, "exec(Object... args): " + i);

                e = JEXL.createExpression("func:exec(true, ['1', '2'])");
                o = e.evaluate(jc);
                assertEquals(3, o, "exec(Object args): " + i);

                e = JEXL.createExpression("func:exec(true, '1', '2')");
                o = e.evaluate(jc);
                assertEquals(4, o, "exec(Object... args): " + i);

                e = JEXL.createExpression("func:execute(true, '1', '2')");
                o = e.evaluate(jc);
                c = func.execute(Boolean.TRUE, "1", "2");
                assertTrue(eqExecute(o, c), "execute(Object... args): " + i);

                e = JEXL.createExpression("func:execute([true])");
                o = e.evaluate(jc);
                c = func.execute(new boolean[]{true});
                assertTrue(eqExecute(o, c), "execute(Object... args): " + i);
            }
        } catch (final JexlException xjexl) {
            fail(xjexl.toString());
        }
    }

    @Test
    public void testScriptCall() throws Exception {
        JexlContext context = new MapContext();
        final JexlScript plus = JEXL.createScript("a + b", new String[]{"a", "b"});
        context.set("plus", plus);
        JexlScript forty2 = JEXL.createScript("plus(4, 2) * plus(4, 3)");
        Object o = forty2.execute(context);
        assertEquals(Integer.valueOf(42), o);

        final Map<String, Object> foo = new HashMap<>();
        foo.put("plus", plus);
        context.set("foo", foo);
        forty2 = JEXL.createScript("foo.plus(4, 2) * foo.plus(4, 3)");
        o = forty2.execute(context);
        assertEquals(Integer.valueOf(42), o);

        context = new ScriptContext(foo);
        forty2 = JEXL.createScript("script:plus(4, 2) * script:plus(4, 3)");
        o = forty2.execute(context);
        assertEquals(Integer.valueOf(42), o);

        final JexlArithmetic ja = JEXL.getArithmetic();
        final JexlMethod mplus = new JexlMethod() {
            @Override
            public Class<?> getReturnType() {
                return Object.class;
            }

            @Override
            public Object invoke(final Object obj, final Object ... params) throws Exception {
                if (obj instanceof Map<?, ?>) {
                    return ja.add(params[0], params[1]);
                }
                throw new Exception("not a script context");
            }

            @Override
            public boolean isCacheable() {
                return true;
            }

            @Override
            public boolean tryFailed(final Object rval) {
                // this is the marker for failure
                return rval == this;
            }

            @Override
            public Object tryInvoke(final String name, final Object obj, final Object ... params) {
                try {
                    if ("plus".equals(name)) {
                        return invoke(obj, params);
                    }
                } catch (final Exception xany) {
                    // ignore and fail by returning this
                }
                return this;
            }
        };

        foo.put("PLUS", mplus);
        forty2 = JEXL.createScript("script:PLUS(4, 2) * script:PLUS(4, 3)");
        o = forty2.execute(context);
        assertEquals(Integer.valueOf(42), o);

        context.set("foo.bar", foo);
        forty2 = JEXL.createScript("foo.'bar'.PLUS(4, 2) * foo.bar.PLUS(4, 3)");
        o = forty2.execute(context);
        assertEquals(Integer.valueOf(42), o);
    }

    /**
     * Ensures static methods on objects can be called.
     */
    @Test
    public void testStaticMethodInvocation() throws Exception {
        asserter.setVariable("aBool", Boolean.FALSE);
        asserter.assertExpression("aBool.valueOf('true')", Boolean.TRUE);
    }

    @Test
    public void testStaticMethodInvocationOnClasses() throws Exception {
        asserter.setVariable("Boolean", Boolean.class);
        asserter.assertExpression("Boolean.valueOf('true')", Boolean.TRUE);
    }

    /**
     * test some String method calls
     */
    @Test
    public void testStringMethods() throws Exception {
        asserter.setVariable("foo", "abcdef");
        asserter.assertExpression("foo.substring(3)", "def");
        asserter.assertExpression("foo.substring(0,(size(foo)-3))", "abc");
        asserter.assertExpression("foo.substring(0,size(foo)-3)", "abc");
        asserter.assertExpression("foo.substring(0,foo.length()-3)", "abc");
        asserter.assertExpression("foo.substring(0, 1+1)", "ab");
    }

    @Test
    public void testTopLevelCall() throws Exception {
        final java.util.Map<String, Object> funcs = new java.util.HashMap<>();
        funcs.put(null, new Functor());
        funcs.put("math", new MyMath());
        funcs.put("cx", ContextualFunctor.class);

        final EnhancedContext jc = new EnhancedContext(funcs);

        JexlExpression e = JEXL.createExpression("ten()");
        Object o = e.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);

        e = JEXL.createExpression("plus10(10)");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(20), o);

        e = JEXL.createExpression("plus10(ten())");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(20), o);

        jc.set("pi", Double.valueOf(Math.PI));
        e = JEXL.createExpression("math:cos(pi)");
        o = e.evaluate(jc);
        assertEquals(Double.valueOf(-1), o);

        e = JEXL.createExpression("cx:ratio(10) + cx:ratio(20)");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(7), o);
    }

    @Test
    public void testTryFailed() throws Exception {
        // JEXL-257
        final Functor func = new Functor();
        final JexlContext ctxt = new MapContext();
        ctxt.set("func", func);
        Object result;
        final JexlUberspect uber = JEXL.getUberspect();
        // tryInvoke
        final JexlMethod method = uber.getMethod(func, "over", "foo", 42);
        assertNotNull(method);
        // tryInvoke succeeds
        result = method.tryInvoke("over", func, "foo", 42);
        assertEquals("foo + 42", result);
        // tryInvoke fails
        func.setKill(true);
        try {
            /*result = */method.tryInvoke("over", func, "foo", 42);
            fail("should throw TryFailed");
        } catch (final JexlException.TryFailed xfail) {
            assertEquals(UnsupportedOperationException.class, xfail.getCause().getClass());
        }

        func.setKill(false);
        final JexlPropertySet setter = uber.getPropertySet(func, "under", "42");
        result = setter.tryInvoke(func, "under", "42");
        assertFalse(setter.tryFailed(result));
        assertEquals("42", result);

        final JexlPropertyGet getter = uber.getPropertyGet(func, "under");
        result = getter.tryInvoke(func, "under");
        assertFalse(getter.tryFailed(result));
        assertEquals("42", result);

        func.setKill(true);
        try {
            /*result = */setter.tryInvoke(func, "under", "42");
            fail("should throw TryFailed");
        } catch (final JexlException.TryFailed xfail) {
            assertEquals(UnsupportedOperationException.class, xfail.getCause().getClass());
        }
        func.setKill(false);
        result = setter.tryInvoke(func, "under", "-42");
        assertEquals("-42", result);

        func.setKill(true);
        try {
            /*result = */getter.tryInvoke(func, "under");
            fail("should throw TryFailed");
        } catch (final JexlException.TryFailed xfail) {
            assertEquals(UnsupportedOperationException.class, xfail.getCause().getClass());
        }

        func.setKill(false);
        result = getter.tryInvoke(func, "under");
        assertFalse(getter.tryFailed(result));
        assertEquals("-42", result);
    }

    @Test
    public void testTryFailedScript() throws Exception {
        // JEXL-257
        final Functor func = new Functor();
        final JexlContext ctxt = new MapContext();
        ctxt.set("func", func);
        Object result;
        final JexlUberspect uber = JEXL.getUberspect();
        final JexlScript method = JEXL.createScript("(x, y)->{ func.over(x, y) }");
        // tryInvoke
        //JexlMethod method = uber.getMethod(func, "over", "foo", 42);
        assertNotNull(method);
        // tryInvoke succeeds
        result = method.execute(ctxt, "foo", 42);
        assertEquals("foo + 42", result);
        // tryInvoke fails
        func.setKill(true);
        try {
            /*result = */method.execute(ctxt, "foo", 42);
            fail("should throw TryFailed");
        } catch (final JexlException xfail) {
            assertEquals(UnsupportedOperationException.class, xfail.getCause().getClass());
        }

        func.setKill(false);
        final JexlScript setter = JEXL.createScript("(x)->{ func.under = x }");
        //JexlPropertySet setter = uber.getPropertySet(func, "under", "42");
        result = setter.execute(ctxt, "42");
        assertEquals("42", result);

        final JexlScript getter = JEXL.createScript("func.under");
        assertEquals("42", result);

        func.setKill(true);
        try {
            /*result = */setter.execute(ctxt, "42");
            fail("should throw TryFailed");
        } catch (final JexlException xfail) {
            assertEquals(UnsupportedOperationException.class, xfail.getCause().getClass());
        }
        func.setKill(false);
        result = setter.execute(ctxt, "-42");
        assertEquals("-42", result);

        func.setKill(true);
        try {
            /*result = */getter.execute(ctxt);
            fail("should throw TryFailed");
        } catch (final JexlException xfail) {
            assertEquals(UnsupportedOperationException.class, xfail.getCause().getClass());
        }

        func.setKill(false);
        result = getter.execute(ctxt);
        assertEquals("-42", result);
    }

    @Test
    public void testVariousFunctionLocation() throws Exception {
        // see JEXL-190
        final Map<String, Object> vars = new HashMap<>();
        final Map<String,Object> funcs = new HashMap<>();
        funcs.put(null, new ZSpace());
        final JexlEngine jexl = new JexlBuilder().namespaces(funcs).arithmetic(new ZArithmetic(true)).create();

        final JexlContext zjc = new ZContext(vars); // that implements a z(int x) function
        final String z41 = "z(41)";
        final JexlScript callz41 = jexl.createScript(z41);
        Object onovar = callz41.execute(zjc);
        assertEquals(222, onovar);

        // override z() with global var
        final JexlScript z241 = jexl.createScript("(x)->{ return x + 241}");
        vars.put("z", z241);
        final Object oglobal = callz41.execute(zjc);
        assertEquals(282, oglobal);
        // clear global and execute again
        vars.remove("z");
        onovar = callz41.execute(zjc);
        assertEquals(222, onovar);

        // override z() with local var
        final String slocal = "var z = (x)->{ return x + 141}; z(1)";
        final JexlScript jlocal = jexl.createScript(slocal);
        final Object olocal = jlocal.execute(zjc);
        assertEquals(142, olocal);

        // and now try the context, the null namespace and the arithmetic
        assertEquals(42, jexl.createScript("zz(2)").execute(zjc));
        assertEquals(42, jexl.createScript("zzz(3)").execute(zjc));
        assertEquals(42, jexl.createScript("zzzz(4)").execute(zjc));
    }

}
