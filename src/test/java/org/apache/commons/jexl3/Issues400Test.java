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

import static org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for reported issue between JEXL-300 and JEXL-399.
 */
public class Issues400Test {

    public static class VinzCaller {
        private final JexlContext context;

        VinzCaller(final JexlContext context) {
            this.context = context;
        }

        public Object execute(final JexlScript script) {
            return script.execute(context);
        }
    }

    public static class VinzContext extends MapContext {
        public String member(final String m, final String u) {
            return m + '.' + u;
        }
    }

    /**
     * Any function in a context can be used as a method of its first parameter. Overloads are respected.
     */
    public static class XuContext extends MapContext {

        public String join(final int[] list, final String str) {
            return join(Arrays.stream(list).iterator(), str);
        }

        public String join(final Iterable<?> list, final String str) {
            return join(list.iterator(), str);
        }

        public String join(final Iterator<?> iterator, final String str) {
            if (!iterator.hasNext()) {
                return "";
            }
            final StringBuilder strb = new StringBuilder(256);
            strb.append(iterator.next().toString());
            while (iterator.hasNext()) {
                strb.append(str);
                strb.append(Objects.toString(iterator.next(), "?"));
            }
            return strb.toString();
        }
    }

    private static void run404(final JexlEngine jexl, final String src, final Object... a) {
        final JexlScript script = jexl.createScript(src, "a", "b");
        if (!src.endsWith(";")) {
            assertEquals(script.getSourceText(), script.getParsedText());
        }
        final Object result = script.execute(null, a);
        assertEquals(42, result);
    }

    @Test
    public void test402() {
        final JexlContext jc = new MapContext();
      // @formatter:off
      final String[] sources = {
        "if (true) { return }",
        "if (true) { 3; return }",
        "(x->{ 3; return })()"
      };
      // @formatter:on
        final JexlEngine jexl = new JexlBuilder().create();
        for (final String source : sources) {
            final JexlScript e = jexl.createScript(source);
            final Object o = e.execute(jc);
            assertNull(o);
        }
    }

    @Test
    public void test403() {
        // @formatter:off
        final String[] strings = {
            "  map1.`${item.a}` = 1;\n",
            "  map1[`${item.a}`] = 1;\n",
            "  map1[item.a] = 1;\n"
         };
        // @formatter:on
        for (final String setmap : strings) {
            // @formatter:off
            final String src = "var a = {'a': 1};\n" +
                "var list = [a, a];\n" +
                "let map1 = {:};\n" +
                "for (var item : list) {\n" +
                setmap +
                "}\n " +
                "map1";
            // @formatter:on
            final JexlEngine jexl = new JexlBuilder().cache(64).create();
            final JexlScript script = jexl.createScript(src);
            for (int i = 0; i < 2; ++i) {
                final Object result = script.execute(null);
                assertTrue(result instanceof Map);
                final Map<?, ?> map = (Map<?, ?>) result;
                assertEquals(1, map.size());
                assertTrue(map.containsKey(1));
                assertTrue(map.containsValue(1));
            }
        }
    }

    @Test
    public void test404a() {
        final JexlEngine jexl = new JexlBuilder().cache(64).strict(true).safe(false).create();
        Map<String, Object> a = Collections.singletonMap("b", 42);
        // access is constant
        for (final String src : new String[] { "a.b", "a?.b", "a['b']", "a?['b']", "a?.`b`" }) {
            run404(jexl, src, a);
            run404(jexl, src + ";", a);
        }
        // access is variable
        for (final String src : new String[] { "a[b]", "a?[b]", "a?.`${b}`" }) {
            run404(jexl, src, a, "b");
            run404(jexl, src + ";", a, "b");
        }
        // add a 3rd access
        final Map<String, Object> b = Collections.singletonMap("c", 42);
        a = Collections.singletonMap("b", b);
        for (final String src : new String[] { "a[b].c", "a?[b]?['c']", "a?.`${b}`.c" }) {
            run404(jexl, src, a, "b");
        }
    }

    @Test
    public void test404b() {
      // @formatter:off
      final JexlEngine jexl = new JexlBuilder()
          .cache(64)
          .strict(true)
          .safe(false)
          .create();
      // @formatter:on
        final Map<String, Object> b = Collections.singletonMap("c", 42);
        final Map<String, Object> a = Collections.singletonMap("b", b);
        JexlScript script;
        Object result = -42;
        script = jexl.createScript("a?['B']?['C']", "a");
        result = script.execute(null, a);
        assertEquals(script.getSourceText(), script.getParsedText());
        assertNull(result);
        script = jexl.createScript("a?['b']?['C']", "a");
        assertEquals(script.getSourceText(), script.getParsedText());
        result = script.execute(null, a);
        assertNull(result);
        script = jexl.createScript("a?['b']?['c']", "a");
        assertEquals(script.getSourceText(), script.getParsedText());
        result = script.execute(null, a);
        assertEquals(42, result);
        script = jexl.createScript("a?['B']?['C']?: 1042", "a");
        assertEquals(script.getSourceText(), script.getParsedText());
        result = script.execute(null, a);
        assertEquals(1042, result);
        // can still do ternary, note the space between ? and [
        script = jexl.createScript("a? ['B']:['C']", "a");
        result = script.execute(null, a);
        assertArrayEquals(new String[] { "B" }, (String[]) result);
        script = jexl.createScript("a?['b'] ?: ['C']", "a");
        result = script.execute(null, a);
        assertEquals(b, result);
        script = jexl.createScript("a?['B'] ?: ['C']", "a");
        result = script.execute(null, a);
        assertArrayEquals(new String[] { "C" }, (String[]) result);
    }

    @Test
    public void test406a() {
        // @formatter:off
        final JexlEngine jexl = new JexlBuilder()
            .cache(64)
            .strict(true)
            .safe(false)
            .create();
        // @formatter:on

        final JexlContext context = new XuContext();
        // @formatter:off
        final List<String> list = Arrays.asList(
            "[1, 2, 3, 4, ...].join('-')", // List<Integer>
            "[1, 2, 3, 4,].join('-')", // int[]
            "(1 .. 4).join('-')", // iterable<Integer>
            "join([1, 2, 3, 4, ...], '-')",
            "join([1, 2, 3, 4], '-')",
            "join((1 .. 4), '-')");
        // @formatter:on
        for (final String src : list) {
            final JexlScript script = jexl.createScript(src);
            final Object result = script.execute(context);
            assertEquals("1-2-3-4", result, src);
        }

        final String src0 = "x.join('*')";
        final JexlScript script0 = jexl.createScript(src0, "x");
        final String src1 = "join(x, '*')";
        final JexlScript script1 = jexl.createScript(src1, "x");
        for (final Object x : Arrays.asList(Arrays.asList(1, 2, 3, 4), new int[] { 1, 2, 3, 4 })) {
            Object result = script0.execute(context, x);
            assertEquals("1*2*3*4", result, src0);
            result = script1.execute(context, x);
            assertEquals("1*2*3*4", result, src1);
        }
    }

    @Test
    public void test407() {
        // Java version
        final double r = 99.0d + 7.82d - 99.0d - 7.82d;
        assertEquals(0d, r, 8.e-15); // Not zero, IEEE 754
        // jexl
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript script = jexl.createScript("a + b - a - b", "a", "b");
        // using doubles, same as Java
        Number result = (Number) script.execute(null, 99.0d, 7.82d);
        assertEquals(0d, result.doubleValue(), 8.e-15);
        // using BigdDecimal, more precise, still not zero
        result = (Number) script.execute(null, new BigDecimal(99.0d), new BigDecimal(7.82d));
        assertEquals(0d, result.doubleValue(), 3.e-32);
    }

    @Test
    public void test412() {
        final Map<Object, Object> ctl = new HashMap<>();
        ctl.put("one", 1);
        ctl.put("two", 2);
        final String fnsrc0 = "function f(x) { x }\n" + "let one = 'one', two = 'two';\n";
        // @formatter:off
        final List<String> list = Arrays.asList(
            "{ one : f(1), two:f(2) }",
            "{ one: f(1), two: f(2) }",
            "{ one: f(1), two:f(2) }",
            "{ one :f(1), two:f(2) }");
        // @formatter:on
        for (final String map0 : list) {
            final String fnsrc = fnsrc0 + map0;
            final JexlContext jc = new MapContext();
            final JexlEngine jexl = new JexlBuilder().create();
            final JexlScript e = jexl.createScript(fnsrc);
            final Object o = e.execute(jc);
            assertTrue(o instanceof Map);
            final Map<?, ?> map = (Map<?, ?>) o;
            assertEquals(map, ctl);
        }
    }

    @Test
    public void test413a() {
        final JexlBuilder builder = new JexlBuilder();
        final JexlEngine jexl = builder.create();
        final JexlScript script = jexl.createScript("var c = 42; var f = y -> c += y; f(z)", "z");
        final Number result = (Number) script.execute(null, 12);
        assertEquals(54, result);
    }

    @Test
    public void test413b() {
        final JexlBuilder builder = new JexlBuilder();
        final JexlOptions options = builder.options();
        options.setConstCapture(true);
        options.setLexical(true);
        final JexlEngine jexl = builder.create();
        final JexlScript script = jexl.createScript("var c = 42; var f = y -> c += y; f(z)", "z");
        final JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> script.execute(null, 12), "c should be const");
        assertEquals("c", xvar.getVariable());
    }

    @Test
    public void test413c() {
        final JexlBuilder builder = new JexlBuilder();
        final JexlEngine jexl = builder.create();
        final JexlScript script = jexl.createScript("#pragma jexl.options '+constCapture'\nvar c = 42; var f = y -> c += y; f(z)", "z");
        final JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> script.execute(null, 12), "c should be const");
        assertEquals("c", xvar.getVariable());
    }

    @Test
    public void test413d() {
        final JexlBuilder builder = new JexlBuilder().features(new JexlFeatures().constCapture(true));
        final JexlEngine jexl = builder.create();
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript("var c = 42; var f = y -> c += y; f(z)", "z"),
                "c should be const");
        assertTrue(xparse.getMessage().contains("const"));
    }

    @Test
    public void test415() {
        final JexlBuilder builder = new JexlBuilder().features(new JexlFeatures().constCapture(true));
        final JexlEngine jexl = builder.create();
        JexlScript script;
        Object result;
        script = jexl.createScript("`#${c}`", "c");
        result = script.execute(null, 42);
        assertEquals("#42", result.toString());
        script = jexl.createScript("`$${c}`", "c");
        result = script.execute(null, 42);
        assertEquals("$42", result.toString());
        script = jexl.createScript("`$#{c}`", "c");
        result = script.execute(null, 42);
        assertEquals("$42", result.toString());
        script = jexl.createScript("`##{c}`", "c");
        result = script.execute(null, 42);
        assertEquals("#42", result.toString());
        script = jexl.createScript("`--##{c}`", "c");
        result = script.execute(null, 42);
        assertEquals("--#42", result.toString());
    }

    @Test
    public void test419() throws NoSuchMethodException {
        // check RESTRICTED permissions denies call to System::currentTimeMillis()
        final Method currentTimeMillis = System.class.getMethod("currentTimeMillis");
        assertFalse(RESTRICTED.allow(currentTimeMillis));
        // compose using a positive class permission to allow just System::currentTimeMillis()
        final JexlPermissions permissions = RESTRICTED.compose("java.lang { +System { currentTimeMillis(); } }");
        // check no side effect on compose
        assertTrue(permissions.allow(currentTimeMillis));
        assertFalse(RESTRICTED.allow(currentTimeMillis));

        // An engine with the System class as namespace and the positive permissions
        final JexlEngine jexl = new JexlBuilder().namespaces(Collections.singletonMap("sns", System.class)).permissions(permissions).create();

        final AtomicLong result = new AtomicLong();
        assertEquals(0, result.get());
        final long now = System.currentTimeMillis();
        // calling System::currentTimeMillis() is allowed and behaves as expected
        jexl.createScript("result.set(sns:currentTimeMillis())", "result").execute(null, result);
        assertTrue(result.get() >= now);

        // we still cant call anything else
        final JexlScript script = jexl.createScript("sns:gc()");
        final JexlException.Method method = assertThrows(JexlException.Method.class, () -> script.execute(null));
        assertEquals("gc", method.getMethod());

    }

    @Test
    public void testDocBreakContinue() {
        final JexlBuilder builder = new JexlBuilder().features(new JexlFeatures().constCapture(true));
        final JexlEngine jexl = builder.create();
        JexlScript script;
        Object result;
        // @formatter:off
        final String srcContinue = "let text = '';\n" +
            "for (let i : (4..2)) { if (i == 3) continue; text += i; }\n" +
            "text;";
        // @formatter:on
        script = jexl.createScript(srcContinue);
        result = script.execute(null);
        assertEquals("42", result);
        // @formatter:off
        final String srcBreak = "let i = 33;\n" +
            "while (i < 66) { if (i == 42) { break; } i += 1; }\n" +
            "i;";
        // @formatter:on
        script = jexl.createScript(srcBreak);
        result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    public void testNamespaceVsTernary0() {
        final VinzContext ctxt = new VinzContext();
        ctxt.set("Users", "USERS");
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).silent(false).create();
        // @formatter:off
        JexlScript script = jexl.createScript("() -> {\n"
            + "  var fn = (user) -> {\n"
            + "     user ? user : member(Users, 'user');\n"
            + "  }\n"
            + "}");
        // @formatter:on
        Object r = script.execute(ctxt);
        assertNotNull(r);
        script = (JexlScript) r;
        r = script.execute(ctxt);
        assertEquals("USERS.user", r);
    }

    @Test
    public void testNamespaceVsTernary1() {
        final VinzContext ctxt = new VinzContext();
        ctxt.set("Users", "USERS");
        ctxt.set("vinz", new VinzCaller(ctxt));
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).silent(false).create();
        // @formatter:off
        final JexlScript script = jexl.createScript(
            "vinz.execute(() -> {\n"
            + "  var test = 42;\n"
            + "  var user = useTest ? test : member(Users, 'user');\n"
            + "})\n" , "useTest");
        // @formatter:on
        Object r = script.execute(ctxt, false);
        assertNotNull(r);
        assertEquals("USERS.user", r);
        r = script.execute(ctxt, true);
        assertNotNull(r);
        assertEquals(42, r);
    }

    public static class SortingContext extends MapContext {
        /**
         * Sorts an array using a script to evaluate the property used to compare elements.
         * @param array the elements array
         * @param expr the property evaluation lambda
         */
        public void sort(Object[] array, JexlScript expr) {
            final Comparator<Object> cmp = new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    Comparable left = (Comparable<?>) expr.execute(SortingContext.this, o1);
                    Comparable right = (Comparable<?>) expr.execute(SortingContext.this, o2);
                    return left.compareTo(right);
                }
            };
            Arrays.sort(array, cmp);
        }
    }

    @Test
    public void testSortArray() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).silent(false).create();
        // test data, json like
        String src = "[{'id':1,'name':'John','type':9},{'id':2,'name':'Doe','type':7},{'id':3,'name':'Doe','type':10}]";
        Object a =  jexl.createExpression(src).evaluate(null);
        assertNotNull(a);
        // row 0 and 1 are not ordered
        Map[] m = (Map[]) a;
        assertEquals(9, m[0].get("type"));
        assertEquals(7, m[1].get("type"));
        // sort the elements on the type
        jexl.createScript("array.sort( e -> e.type )", "array").execute(new SortingContext(), a);
        // row 0 and 1 are now ordered
        assertEquals(7, m[0].get("type"));
        assertEquals(9, m[1].get("type"));
    }
}
