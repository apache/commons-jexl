/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import static org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED;
import static org.apache.commons.jexl3.introspection.JexlPermissions.UNRESTRICTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Closeable;
import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.jexl3.internal.Debugger;
import org.apache.commons.jexl3.internal.Engine32;
import org.apache.commons.jexl3.internal.Scope;
import org.apache.commons.jexl3.internal.TemplateEngine;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.JexlScriptParser;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.StringProvider;
import org.junit.jupiter.api.Assertions;
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
    void test402() {
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
    void test403() {
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
                "for (let item : list) {\n" +
                setmap +
                "}\n " +
                "map1";
            // @formatter:on
            final JexlEngine jexl = new JexlBuilder().cache(64).create();
            final JexlScript script = jexl.createScript(src);
            for (int i = 0; i < 2; ++i) {
                final Object result = script.execute(null);
                assertInstanceOf(Map.class, result);
                final Map<?, ?> map = (Map<?, ?>) result;
                assertEquals(1, map.size());
                final Object val = jexl.createScript("m -> m[1]").execute(null, map);
                assertEquals(1, val);
            }
        }
    }

    @Test
    void test404a() {
        final JexlEngine jexl = new JexlBuilder().cache(64).strict(true).safe(false).create();
        Map<String, Object> a = Collections.singletonMap("b", 42);
        // access is constant
        for (final String src : new String[]{"a.b", "a?.b", "a['b']", "a?['b']", "a?.`b`"}) {
            run404(jexl, src, a);
            run404(jexl, src + ";", a);
        }
        // access is variable
        for (final String src : new String[]{"a[b]", "a?[b]", "a?.`${b}`"}) {
            run404(jexl, src, a, "b");
            run404(jexl, src + ";", a, "b");
        }
        // add a 3rd access
        final Map<String, Object> b = Collections.singletonMap("c", 42);
        a = Collections.singletonMap("b", b);
        for (final String src : new String[]{"a[b].c", "a?[b]?['c']", "a?.`${b}`.c"}) {
            run404(jexl, src, a, "b");
        }
    }

    @Test
    void test404b() {
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
        Object result;
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
        assertArrayEquals(new String[]{"B"}, (String[]) result);
        script = jexl.createScript("a?['b'] ?: ['C']", "a");
        result = script.execute(null, a);
        assertEquals(b, result);
        script = jexl.createScript("a?['B'] ?: ['C']", "a");
        result = script.execute(null, a);
        assertArrayEquals(new String[]{"C"}, (String[]) result);
    }

    @Test
    void test406a() {
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
        for (final Object x : Arrays.asList(Arrays.asList(1, 2, 3, 4), new int[]{1, 2, 3, 4})) {
            Object result = script0.execute(context, x);
            assertEquals("1*2*3*4", result, src0);
            result = script1.execute(context, x);
            assertEquals("1*2*3*4", result, src1);
        }
    }

    @Test
    void test407() {
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
        result = (Number) script.execute(null, new BigDecimal("99.0"), new BigDecimal("7.82"));
        assertEquals(0d, result.doubleValue(), 3.e-32);
    }

    @Test
    void test412() {
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
            assertInstanceOf(Map.class, o);
            final Map<?, ?> map = (Map<?, ?>) o;
            assertEquals(map, ctl);
        }
    }

    @Test
    void test413a() {
        final JexlBuilder builder = new JexlBuilder();
        final JexlEngine jexl = builder.create();
        final JexlScript script = jexl.createScript("var c = 42; var f = y -> c += y; f(z)", "z");
        final Number result = (Number) script.execute(null, 12);
        assertEquals(54, result);
    }

    @Test
    void test413b() {
        final JexlBuilder builder = new JexlBuilder();
        final JexlOptions options = builder.options();
        options.setConstCapture(true);
        options.setLexical(true);
        final JexlEngine jexl = builder.create();
        final JexlScript script = jexl.createScript("var c = 42; var f = y -> c += y; f(z)", "z");
        final JexlException.Variable xvar = assertThrows(JexlException.Variable.class,
            () -> script.execute(null, 12), "c should be const");
        assertEquals("c", xvar.getVariable());
    }

    @Test
    void test413c() {
        final JexlBuilder builder = new JexlBuilder();
        final JexlEngine jexl = builder.create();
        final String pragma = "#pragma jexl.options '+constCapture'\n"
            + "var c = 42; var f = y -> c += y; f(z)";
        final JexlScript script = jexl.createScript(pragma, "z");
        final JexlException.Variable xvar = assertThrows(
            JexlException.Variable.class, () -> script.execute(null, 12), "c should be const");
        assertEquals("c", xvar.getVariable());
    }

    @Test
    void test413d() {
        final JexlBuilder builder = new JexlBuilder().features(new JexlFeatures().constCapture(true));
        final JexlEngine jexl = builder.create();
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class,
            () -> jexl.createScript("var c = 42; var f = y -> c += y; f(z)", "z"),
            "c should be const");
        assertTrue(xparse.getMessage().contains("const"));
    }

    @Test
    void test415() {
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
    void test419() throws NoSuchMethodException {
        // check RESTRICTED permissions denies call to System::currentTimeMillis()
        final Method currentTimeMillis = System.class.getMethod("currentTimeMillis");
        assertFalse(RESTRICTED.allow(currentTimeMillis));
        // compose using a positive class permission to allow just System::currentTimeMillis()
        final JexlPermissions permissions = RESTRICTED.compose("java.lang { +System { currentTimeMillis(); } }");
        // check no side effect on compose
        assertTrue(permissions.allow(currentTimeMillis));
        assertFalse(RESTRICTED.allow(currentTimeMillis));

        // An engine with the System class as namespace and the positive permissions
        final JexlEngine jexl = new JexlBuilder()
            .namespaces(Collections.singletonMap("sns", System.class))
            .permissions(permissions)
            .create();

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
    void testDocBreakContinue() {
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
    void testNamespaceVsTernary0() {
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
    void testNamespaceVsTernary1() {
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

    public static class Ns429 {
        public int f(final int x) {
            return x * 10000 + 42;
        }
    }

    @Test
    void test429a() {
        final MapContext ctxt = new MapContext();
        final JexlFeatures features = JexlFeatures.createDefault();
        final JexlEngine jexl = new JexlBuilder().features(features).safe(false).strict(true).silent(false).create();
        final JexlScript f = jexl.createScript("x -> x");
        ctxt.set("f", f);
        String src = "#pragma jexl.namespace.b " + Ns429.class.getName() + "\n" + "b ? b : f(2);";
        JexlScript script = jexl.createScript(src, "b");
        assertEquals(1, (int) script.execute(ctxt, 1));

        src = "#pragma jexl.namespace.b " + Ns429.class.getName() + "\n" + "b ? b:f(2) : 1;";
        script = jexl.createScript(src, "b");
        assertEquals(20042, (int) script.execute(ctxt, 1));
    }

    @Test
    void test429b() {
        final MapContext ctxt = new MapContext();
        ctxt.set("b", 1);
        final JexlFeatures features = JexlFeatures.createDefault();
        features.namespaceIdentifier(true);
        final JexlEngine jexl = new JexlBuilder().features(features).safe(false).strict(true).silent(false).create();
        final JexlScript f = jexl.createScript("x -> x");
        ctxt.set("f", f);
        String src = "#pragma jexl.namespace.b " + Ns429.class.getName() + "\n" + "b ? b : f(2);";
        JexlScript script = jexl.createScript(src);
        assertEquals(1, (int) script.execute(ctxt));

        src = "#pragma jexl.namespace.b " + Ns429.class.getName() + "\n" + "b ? b:f(2) : 1;";
        script = jexl.createScript(src);
        assertEquals(20042, (int) script.execute(ctxt));
    }

    @Test
    void test431a() {
        final JexlEngine jexl = new JexlBuilder().create();
        final String src = "let x = 0; try { x += 19 } catch (let error) { return 169 } "
            + "try { x += 23 } catch (let error) { return 169 }";
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    Closeable foo() {
        return null;
    }

    @Test
    void test431b() {
        final JexlEngine jexl = new JexlBuilder().create();
        final String src = "let x = 0; try(let error) { x += 19 } catch (let error) { return 169 } "
            + "try { x += 23 } catch (let error) { return 169 }";
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void test431c() {
        final JexlEngine jexl = new JexlBuilder().create();
        final String src = "let xx = 0; try { xx += 19 } catch (let xx) { return 169 }";
        try {
            final JexlScript script = jexl.createScript(src);
            fail("xx is already defined in scope");
        } catch (final JexlException.Parsing parsing) {
            assertTrue(parsing.getDetail().contains("xx"));
        }
    }

    @Test
    void test433() {
        final JexlEngine jexl = new JexlBuilder().create();
        final String src = "let condition = true; if (condition) { return; }";
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertNull(result);
        final Debugger debugger = new Debugger();
        assertTrue(debugger.debug(script));
        final String dbgStr = debugger.toString();
        assertTrue(JexlTestCase.equalsIgnoreWhiteSpace(src, dbgStr));
    }

    @Test
    void test434() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        final String src = "let foo = null; let value = foo?[bar]";
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertNull(result);
    }

    public static class Arithmetic435 extends JexlArithmetic {
        public Arithmetic435(final boolean strict) {
            super(strict);
        }

        public Object empty(final String type) {
            if ("list".equals(type)) {
                return Collections.emptyList();
            }
            return null;
        }
    }

    @Test
    void test435() {
        final JexlArithmetic arithmetic = new Arithmetic435(true);
        final JexlEngine jexl = new JexlBuilder().arithmetic(arithmetic).create();
        final String src = "empty('list')";
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertInstanceOf(List.class, result);
    }

    @Test
    void test436a() {
        final String[] srcs = {"let i = null; ++i", "let i; ++i;", "let i; i--;", "let i; i++;"};
        run436(null, srcs);
    }

    @Test
    void test436b() {
        final String[] srcs = {"var i = null; ++i", "var i; ++i;", "var i; i--;", "var i; i++;"};
        run436(null, srcs);
    }

    @Test
    void test436c() {
        final JexlContext ctxt = new MapContext();
        ctxt.set("i", null);
        final String[] srcs = {"++i", "++i;", "i--;", "i++;"};
        run436(null, srcs);
    }

    void run436(final JexlContext ctxt, final String[] srcs) {
        final JexlEngine jexl = new JexlBuilder().create();
        for (final String src : srcs) {
            final JexlScript script = jexl.createScript(src);
            assertThrows(JexlException.Operator.class, () -> script.execute(ctxt));
        }
    }

    @Test
    void test437a() {
        final JexlEngine jexl = new JexlBuilder().create();
        final String src = "let values = [...]\n"
            + "function append(const value) {\n"
            + "  values.add(value)\n"
            + "}\n"
            + "\n"
            + "append(1)\n"
            + "append(2)\n"
            + "return values ";
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertInstanceOf(List.class, result);
        final List<?> values = (List<?>) result;
        assertEquals(2, values.size());
    }

    @Test
    void test437b() {
        final JexlFeatures features = JexlFeatures.createDefault().ambiguousStatement(true);
        assertTrue(features.supportsAmbiguousStatement());
        final JexlEngine jexl = new JexlBuilder().features(features).create();
        final String src = "let values = [...]"
            + "function append(const value) {"
            + "  values.add(value)"
            + "}"
            + "append(1)"
            + "append(2)"
            + "return values ";
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertInstanceOf(List.class, result);
        final List<?> values = (List<?>) result;
        assertEquals(2, values.size());
    }

    /**
     * The set of characters that may be followed by a '='.
     */
    static final char[] EQ_FRIEND;

    static {
        final char[] eq = {'!', ':', '<', '>', '^', '|', '&', '+', '-', '/', '*', '~', '='};
        Arrays.sort(eq);
        EQ_FRIEND = eq;
    }

    /**
     * Transcodes a SQL-inspired expression to a JEXL expression.
     *
     * @param expr the expression to transcode
     * @return the resulting expression
     */
    private static String transcodeSQLExpr(final CharSequence expr) {
        final StringBuilder strb = new StringBuilder(expr.length());
        final int end = expr.length();
        char previous = 0;
        for (int i = 0; i < end; ++i) {
            final char c = expr.charAt(i);
            if (previous == '<') {
                // previous char a '<' now followed by '>'
                if (c == '>') {
                    // replace '<>' with '!='
                    strb.append("!=");
                    previous = c;
                    continue;
                }
                strb.append('<');
            }
            if (c != '<') {
                if (c == '=') {
                    // replace '=' with '==' when it does not follow a 'friend'
                    if (Arrays.binarySearch(EQ_FRIEND, previous) >= 0) {
                        strb.append(c);
                    } else {
                        strb.append("==");
                    }
                } else {
                    strb.append(c);
                    if (c == '"' || c == '\'') {
                        // read string, escape '\'
                        boolean escape = false;
                        for (i += 1; i < end; ++i) {
                            final char ec = expr.charAt(i);
                            strb.append(ec);
                            if (ec == '\\') {
                                escape = !escape;
                            } else if (escape) {
                                escape = false;
                            } else if (ec == c) {
                                break;
                            }
                        }
                    }
                }
            }
            previous = c;
        }
        return strb.toString();
    }

    public static class SQLParser implements JexlScriptParser {
        final Parser parser;

        public SQLParser() {
            parser = new Parser(new StringProvider(";"));
        }

        @Override
        public ASTJexlScript parse(final JexlInfo info, final JexlFeatures features, final String src, final Scope scope) {
            return parser.parse(info, features, transcodeSQLExpr(src), scope);
        }

        @Override
        public ASTJexlScript jxltParse(final JexlInfo info, final JexlFeatures features, final String src, final Scope scope) {
            return new Parser(parser).parse(info, features, transcodeSQLExpr(src), scope);
        }
    }


    @Test
    void testSQLTranspose() {
        final String[] e = {"a<>b", "a = 2", "a.b.c <> '1<>0'"};
        final String[] j = {"a!=b", "a == 2", "a.b.c != '1<>0'"};
        for (int i = 0; i < e.length; ++i) {
            final String je = transcodeSQLExpr(e[i]);
            Assertions.assertEquals(j[i], je);
        }
    }

    @Test
    void testSQLNoChange() {
        final String[] e = {"a <= 2", "a >= 2", "a := 2", "a + 3 << 4 > 5",};
        for (final String element : e) {
            final String je = transcodeSQLExpr(element);
            Assertions.assertEquals(element, je);
        }
    }

    @Test
    void test438() {// no local, no lambda, no loops, no-side effects
        final JexlFeatures f = new JexlFeatures()
            .localVar(false)
            .lambda(false)
            .loops(false)
            .sideEffect(false)
            .sideEffectGlobal(false);
        final JexlBuilder builder = new JexlBuilder().parserFactory(SQLParser::new).cache(32).features(f);
        final JexlEngine sqle = builder.create();
        Assertions.assertTrue((boolean) sqle.createScript("a <> 25", "a").execute(null, 24));
        Assertions.assertFalse((boolean) sqle.createScript("a <> 25", "a").execute(null, 25));
        Assertions.assertFalse((boolean) sqle.createScript("a = 25", "a").execute(null, 24));
        Assertions.assertTrue((boolean) sqle.createScript("a != 25", "a").execute(null, 24));
        Assertions.assertTrue((boolean) sqle.createScript("a = 25", "a").execute(null, 25));
        Assertions.assertFalse((boolean) sqle.createScript("a != 25", "a").execute(null, 25));
    }

    @Test
    void testIssue441() {
        final JexlEngine jexl = new JexlBuilder().create();
        String ctl = "\nab\nc`d\n";
        final JexlExpression e = jexl.createExpression("`\nab\nc\\`d\n`");
        Object o = e.evaluate(null);
        Assertions.assertEquals(ctl, o);

        final JexlContext context = new MapContext();
        context.set("name", "Hello");
        final String code = "return `${name + '\\n' + name}`;";
        final JexlScript script = jexl.createScript(code);
        o = script.execute(context);
        ctl = "Hello\nHello";
        Assertions.assertEquals(ctl, o);
    }

    @Test
    void testIssue442() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext context = new MapContext();
        final String code = "var x = 'hello';\n" + "function test(z) {\n" +
            //"x + ' ' + z\n"+
            "`${x} ${z}`;\n" + "}\n" + "test('world');";
        final JexlScript script = jexl.createScript(code);
        final Object result = script.execute(context);
        Assertions.assertEquals("hello world", result);
    }


    @Test
    void testIssue447() {
        final JexlEngine jexl = new JexlBuilder().create();
        final String src = "const c = `${a}\n?= ${b}`; "
            + "function foo(const left, const right) { `${left}\n?== ${right}` } "
            + "c+foo(a, b)";
        final JexlScript script = jexl.createScript(src, "a", "b");
        final Object result = script.execute(null, "a", "b");
        Assertions.assertEquals("a\n?= ba\n?== b", result);
        String[] locals = script.getLocalVariables();
        Assertions.assertArrayEquals(new String[]{"c", "foo"}, locals);
        final String TEST447 = "src/test/scripts/test447.jexl";
        final File src447 = new File(TEST447);
        final JexlScript script447 = jexl.createScript(src447);
        final Object result447 = script447.execute(null);
        Assertions.assertInstanceOf(List.class, result447);
        @SuppressWarnings("unchecked") final List<Boolean> list = (List<Boolean>) result447;
        for (final Boolean item : list) {
            Assertions.assertTrue(item);
        }
    }

    public static class BrkContext extends MapContext {
        public BrkContext() {
            super();
            set("SYSTEM", System.class);
            set("UNRESTRICTED", UNRESTRICTED);
        }

        public static Object brk(Object debug) {
            return debug;
        }

    }

    @Test
    void test450a() {
        JexlEngine jexl0 = new JexlBuilder().silent(false).permissions(JexlPermissions.RESTRICTED).create();
        assertThrows(JexlException.Method.class,
            () -> jexl0.newInstance("org.apache.commons.jexl3.internal.introspection.Uberspect", null, null),
            "should not be able to create Uberspect with RESTRICTED");
        JexlPermissions perm = new JexlPermissions.ClassPermissions(
            org.apache.commons.jexl3.internal.introspection.Uberspect.class);
        JexlEngine jexl1 = new JexlBuilder().silent(false).permissions(perm).create();
        assertNotNull(jexl1.newInstance(
            "org.apache.commons.jexl3.internal.introspection.Uberspect", null, null),
            "should able to create Uberspect with Uberspect permission");

    }

    @Test
    void test450b() {
        // cannot load System with RESTRICTED
        assertThrows(JexlException.Method.class, () -> run450b(JexlPermissions.RESTRICTED),
            "should not be able to load System with RESTRICTED");
        // can load System with UNRESTRICTED
        assertEquals(java.lang.System.class, run450b(UNRESTRICTED));
        // need to explicitly allow Uberspect and the current class loader to load System
        JexlPermissions perm = new JexlPermissions.ClassPermissions(
            getClass().getClassLoader().getClass(),
            org.apache.commons.jexl3.internal.introspection.Uberspect.class);
        assertEquals(java.lang.System.class, run450b(perm));
    }

    private static Object run450b(JexlPermissions perm) {
        JexlEngine jexl = new JexlBuilder().silent(false).permissions(perm).create();
        String uscript = "new('org.apache.commons.jexl3.internal.introspection.Uberspect', "
            + "null, null, perm).getClassLoader().loadClass('java.lang.System')";
        JexlScript u0 = jexl.createScript(uscript, "perm");
        return u0.execute(null, perm);
    }

    @Test
    void test450c() {
        // can reach and invoke System::currentTimeMillis with UNRESTRICTED
        assertNotNull(run450c(UNRESTRICTED));
        // need explicit permissions to ClassPermissions and Uberspect to reach and invoke
        // System::currentTimeMillis
        JexlPermissions perm = new JexlPermissions.ClassPermissions(
            JexlPermissions.ClassPermissions.class,
            org.apache.commons.jexl3.internal.introspection.Uberspect.class);
        assertNotNull(run450c(perm));
        // cannot reach and invoke System::currentTimeMillis with RESTRICTED
        assertThrows(JxltEngine.Exception.class, () -> run450c(JexlPermissions.RESTRICTED),
            "should not be able to load System with RESTRICTED");
    }

    private static Object run450c(JexlPermissions perm) {
        JexlBuilder builder = new JexlBuilder().silent(false).permissions(perm);
        Object result = new TemplateEngine(new Engine32(builder), false, 2, '$', '#')
            .createExpression(
                "${x = new ('org.apache.commons.jexl3.internal.introspection.Uberspect', "
                + "null, null, UNRESTRICTED);"
                + "sys = x?.getClassLoader()?.loadClass('java.lang.System') ?: SYSTEM;"
                + "p = new('org.apache.commons.jexl3.introspection.JexlPermissions$ClassPermissions', [sys]);"
                + "c = new('org.apache.commons.jexl3.internal.introspection.Uberspect', null, null, p);"
                + "z = c.getMethod(sys,'currentTimeMillis').invoke(x,null);}")
            .evaluate(new BrkContext());
        return result;
    }

    @Test
    void test450() {
        assertNotNull(run450(JexlPermissions.UNRESTRICTED),
            "should be able to reach and invoke System::currentTimeMillis with UNRESTRICTED");
        assertNotNull(
            run450(new JexlPermissions.ClassPermissions(
                org.apache.commons.jexl3.internal.TemplateEngine.class)),
            "should be able to reach and invoke System::currentTimeMillis with TemplateEngine permission");
        assertThrows(JexlException.Method.class, () -> run450(RESTRICTED),
            "should not be able to reach and invoke System::currentTimeMillis with RESTRICTED");
    }

    public static class Engine33 extends Engine32 {
        public Engine33() {
            this(createBuilder());
        }

        public Engine33(JexlBuilder builder) {
            super(builder);
        }

        static JexlBuilder createBuilder() {
            JexlPermissions perm = new JexlPermissions.ClassPermissions(
                Issues400Test.class.getClassLoader().getClass(),
                JexlPermissions.ClassPermissions.class,
                org.apache.commons.jexl3.internal.TemplateEngine.class,
                org.apache.commons.jexl3.internal.introspection.Uberspect.class);
            return new JexlBuilder().safe(false).silent(false).permissions(perm);
        }
    }

    private static Object run450(JexlPermissions perm) {
        JexlEngine jexl = new JexlBuilder().silent(false).strict(true).safe(false)
            .permissions(perm).create();
        String script = "new('org.apache.commons.jexl3.internal.TemplateEngine',"
            + "new('org.apache.commons.jexl3.Issues400Test$Engine33'),false,256,"
            + "'$'.charAt(0),'#'.charAt(0))"
            + ".createExpression("
            + "\"#{x = new ('org.apache.commons.jexl3.internal.introspection.Uberspect', null, null);"
            + "sys = x?.getClassLoader().loadClass('java.lang.System') ?: SYSTEM;"
            + "p = new('org.apache.commons.jexl3.introspection.JexlPermissions$ClassPermissions', [sys]);"
            + "c = new('org.apache.commons.jexl3.internal.introspection.Uberspect', null, null, p);"
            + "z = c.getMethod(sys,'currentTimeMillis').invoke(x,null);}\")"
            + ".evaluate(new('org.apache.commons.jexl3.Issues400Test$BrkContext'))";
        return jexl.createScript(script).execute(null);
    }

    @Test
    void test451() {
        JexlEngine jexl = new JexlBuilder().create();
        assertEquals("42", jexl.createScript("o.toString()", "o").execute(null, "42"));
        JexlPermissions perms = RESTRICTED.compose("java.lang { +Class { getSimpleName(); } }");
        JexlSandbox sandbox = new JexlSandbox(false, true);
        sandbox.permissions(Object.class.getName(), true, true, false, false);
        sandbox.allow(String.class.getName()).execute("toString");
        final JexlEngine jexl451 = new JexlBuilder().safe(false).silent(false).permissions(perms).sandbox(sandbox).create();
        // sandbox allows String::toString
        assertEquals("42", jexl451.createScript("o.toString()", "o").execute(null, "42"));
        // sandbox forbids getClass
        assertThrows(JexlException.Method.class, () -> jexl451.createScript("oo.getClass()", "oo").execute(null, "42"));
        // sandbox allows reading properties, permissions allow getClass
        assertEquals(String.class, jexl451.createScript("o.class", "o").execute(null, "42"));
        // sandbox allows reading properties, permissions allow getSimpleName
        assertEquals("Object", jexl451.createScript("o.class.simpleName", "o").execute(null, new Object()));
        // sandbox allows reading properties, permissions forbids getClassLoader
        assertThrows(JexlException.Property.class,
            () -> jexl451.createScript("o.class.classLoader", "o").execute(null, new Object()));
    }

    @Test
    void testIssue455a() {
        final JexlEngine jexl = new JexlBuilder().create();
        String code = "name -> `${name +\n\t\f\r name}`";
        JexlScript script = jexl.createScript(code);
        Object o = script.execute(null, "Hello");
        String ctl = "HelloHello";
        Assertions.assertEquals(ctl, o);
    }

    @Test
    void testIssue455b() {
        final JexlEngine jexl = new JexlBuilder().create();
        String code = "name -> `${name}\n${name}`;";
        JexlScript script = jexl.createScript(code);
        Object o = script.execute(null, "Hello");
        String ctl = "Hello\nHello";
        Assertions.assertEquals(ctl, o);
    }

    @Test
    void testIssue455c() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext context = new MapContext();
        context.set("name", "Hello");
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JxltEngine.Template template = jxlt.createTemplate("<b>\n\t${name\n\t+\r\f name}\n</b>");
        final StringWriter writer = new StringWriter();
        template.evaluate(context, writer);
        assertEquals("<b>\n\tHelloHello\n</b>", writer.toString());
    }

    @Test
    void testIssue455d() {
        final JexlEngine jexl = new JexlBuilder().create();
        // 'ref' contains 'greeting' which is the name of the variable to expand
        String code = "`#{${\nref\t}}\n#{${\rref\f}}`;";
        JexlScript script = jexl.createScript(code, "ref", "greeting");
        Object o = script.execute(null, "greeting", "Hello");
        String ctl = "Hello\nHello";
        Assertions.assertEquals(ctl, o);
    }

    @Test
    void testIssue455e() {
        final JexlEngine jexl = new JexlBuilder().create();
        // Evaluate nested immediate inside deferred at runtime using a parameterized script
        final String src = "(name, suffix) -> `#{name} Hello ${name} ! #{suffix}`";
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, "World", "~");
        Assertions.assertEquals("World Hello World ! ~", result);
    }

    @Test
    void testIssue455f() {
        final JexlEngine jexl = new JexlBuilder().create();
        // Evaluate nested immediate inside deferred at runtime using a parameterized script
        final String src = "(name, suffix) -> `#{name + ' Hello'} ${name + ' !'} #{suffix}`";
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, "World", "~");
        Assertions.assertEquals("World Hello World ! ~", result);
    }

    @Test
    void testIssue455g() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JxltEngine.Template template = jxlt.createTemplate("${name} #{suffix}", "name", "suffix");
        final StringWriter writer = new StringWriter();
        // prepare requires immediate arguments; evaluate needs deferred arguments
        template.prepare(null, "World", null).evaluate(null, writer, null, "~");
        Assertions.assertEquals("World ~", writer.toString());
    }

    @Test
    void testIssue455h() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JxltEngine.Template template = jxlt.createTemplate("#{name + ' Hello'} ${name + ' !'} #{suffix}", "name", "suffix");
        final StringWriter writer = new StringWriter();
        // Prepare only the immediate name argument; evaluate needs both deferred arguments - name and suffix
        template.prepare(null, "World").evaluate(null, writer, "World", "~");
        Assertions.assertEquals("World Hello World ! ~", writer.toString());
    }

    @Test
    void testIssue350_456_strict() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        // creation/parse is OK
        final JxltEngine.Template template = jxlt.createTemplate("$$ var foo = 'foo';\n$$ var bar = 'bar';\n${foo + bar}");
        final StringWriter writer = new StringWriter();
            template.evaluate(null, writer);
            Assertions.assertEquals("foobar", writer.toString());
    }

    @Test
    void testIssue350_456_notStrict() {
        final JexlEngine jexl = new JexlBuilder().strict(false).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        // creation/parse is OK
        final JxltEngine.Template template = jxlt.createTemplate("$$ var foo = 'foo';\n$$ var bar = 'bar';\n${foo + bar}");
        final StringWriter writer = new StringWriter();
        template.evaluate(null, writer);
        Assertions.assertEquals("foobar", writer.toString());
    }

    @Test
    void testIssue350_456_strictWithVariable() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        // creation/parse is OK
        final JxltEngine.Template template = jxlt.createTemplate("$$ var foo = 'foo';\n$$ var bar = 'bar';\n${foo + bar}");
        // add a '$$' global context variable
        JexlContext ctxt = new MapContext();
        ctxt.set("$$", "");
        final StringWriter writer = new StringWriter();
        template.evaluate(ctxt, writer);
        Assertions.assertEquals("foobar", writer.toString());
    }

    @Test
    void testIssue36x_456_var() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        // OK
        jexl.createScript("var foo = 0;\nfoo = 42;");
        try {
            jxlt.createTemplate("$$ var foo = 'foo'; if (true) { var foo = 'bar'; var err =&; }");
            Assertions.fail("should have thrown a parsing error in '&'");
        } catch (JexlException xjexl) {
            // parsing error in '&'
            Assertions.assertTrue(xjexl.getMessage().contains("&"));
        }
        // JEXL-456: java.lang.NullPointerException: 
        // Cannot invoke "org.apache.commons.jexl3.internal.Scope.getCaptureDeclaration(int)" because "blockScope" is null
        jexl.createScript("var foo = 0;\nfoo = 42;");
        // JEXL-456: same error with the template creation below
        jxlt.createTemplate("$$ var foo = 'foo'; foo = 'bar';");
    }

    @Test
    void testIssue36x_456_let() {
        final JexlEngine jexl = new Engine32(new JexlBuilder().strict(true));
        // OK
        jexl.createScript("let foo = 0;\nfoo = 42;");
        final JxltEngine jxlt = jexl.createJxltEngine();
        try {
            jxlt.createTemplate("$$ var err =&;");
            Assertions.fail("should have thrown a parsing error in '&'");
        } catch (JexlException xjexl) {
            // parsing error in '&'
            Assertions.assertTrue(xjexl.getMessage().contains("&"));
        }
        // JEXL-456: parsing error in 'foo: variable is already declared'
        jexl.createScript("let foo = 0;\nfoo = 42;");
        // JEXL-456: same error with the template creation below
        jxlt.createTemplate("$$ let foo = 0;\nfoo = 42;");
    }

}

