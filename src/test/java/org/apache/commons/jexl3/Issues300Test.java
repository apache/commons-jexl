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

import static org.apache.commons.jexl3.internal.Util.debuggerCheck;
import static org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.jexl3.internal.Engine32;
import org.apache.commons.jexl3.internal.OptionsContext;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.junit.jupiter.api.Test;

/**
 * Test cases for reported issue between JEXL-300 and JEXL-399.
 */
public class Issues300Test {
    public static class Arithmetic383 extends JexlArithmetic {
        public Arithmetic383(final boolean astrict) {
            super(astrict);
        }

        @Override
        public boolean isStrict(final JexlOperator op) {
            switch (op) {
            case NOT:
            case CONDITION:
                return false;
            }
            return super.isStrict(op);
        }
    }

    public static class Arithmetic384 extends JexlArithmetic {
        public Arithmetic384(final boolean astrict) {
            super(astrict);
        }

        @Override
        public boolean isStrict(final JexlOperator op) {
            if (JexlOperator.ADD == op) {
                return false;
            }
            return super.isStrict(op);
        }
    }

    public static class Arithmetic384c extends JexlArithmetic {
        AtomicInteger cmp = new AtomicInteger();

        public Arithmetic384c(final boolean astrict) {
            super(astrict);
        }

        public Arithmetic384c(final boolean astrict, final MathContext bigdContext, final int bigdScale) {
            super(astrict, bigdContext, bigdScale);
        }

        @Override
        protected int compare(final Object l, final Object r, final String op) {
            cmp.incrementAndGet();
            return super.compare(l, r, op);
        }

        int getCmpCalls() {
            return cmp.get();
        }
    }

    public static class Arithmetic384d extends Arithmetic384c {
        public Arithmetic384d(final boolean astrict) {
            super(astrict);
        }

        public Arithmetic384d(final boolean astrict, final MathContext bigdContext, final int bigdScale) {
            super(astrict, bigdContext, bigdScale);
        }
    }

    private static class Class397 implements Interface397i {
        @Override
        public String summary() {
            return getClass().getName();
        }
    }

    public static class Context0930 extends MapContext {
        /**
         * This allows using a JEXL lambda as a filter.
         *
         * @param stream the stream
         * @param filter the lambda to use as filter
         * @return the filtered stream
         */
        public Stream<?> filter(final Stream<?> stream, final JexlScript filter) {
            return stream.filter(x -> Boolean.TRUE.equals(filter.execute(this, x)));
        }
    }

    /**
     * Mock driver.
     */
    public static class Driver0930 {
        private final String name;

        Driver0930(final String n) {
            name = n;
        }

        public String getAttributeName() {
            return name;
        }
    }

    public interface Interface397i {
        String summary();
    }

    public static class Session322 {
        public User322 getUser() {
            return new User322();
        }
    }

    public static class TestObject374 {
        private String name;
        private TestObject374 nested;

        public String getName() {
            return name;
        }

        public TestObject374 getNested() {
            return nested;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setNested(final TestObject374 nested) {
            this.nested = nested;
        }
    }

    public enum Type375 {
        DELIVERY_ADDRESS, DOMICILE
    }

    public static class User322 {
        public String getName() {
            return "user322";
        }
    }

    public static class VaContext extends MapContext {
        VaContext(final Map<String, Object> vars) {
            super(vars);
        }

        public int cell(final List<?> l, final String... ms) {
            return 42 + cell(ms);
        }

        public int cell(final String... ms) {
            return ms.length;
        }
    }

    public static class Var370 {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String s) {
            name = s;
        }
    }

    static JexlContext pragmaticContext() {
        final JexlOptions opts = new JexlOptions();
        opts.setFlags("-strict", "-cancellable", "-lexical", "-lexicalShade", "+safe", "+sharedInstance");
        return new JexlTestCase.PragmaticContext(opts);
    }

    <T> T createProxy(final JexlEngine jexl, final Object o, final Class[] clazzz) {
        // a JEX-based delegating proxy
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), clazzz, (proxy, method, args) -> jexl.invokeMethod(o, method.getName(), args));
    }

    private Object run361a(final JexlEngine jexl) {
        final String src = "()-> { ()-> { if (versionFile != null) { return 'foo'; } else { return 'bar'; }} }";
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null);
        final JexlScript rs = (JexlScript) result;
        return rs.execute(null);
    }

    private Object run361b(final JexlEngine jexl) {
        // @formatter:off
        final String src = "()-> { ()-> {" +
                "var voa = vaf.value;\n" +
                "if (voa != NaN && voa <= 0)" +
                "{ return 'foo'; } else { return 'bar'; }" +
                "} }";
        // @formatter:on
        final JexlContext context = new MapContext();
        final Map<String, Object> vaf = Collections.singletonMap("value", null);
        context.set("vaf", vaf);
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null);
        final JexlScript rs = (JexlScript) result;
        return rs.execute(context);
    }

    private String run361c(final JexlEngine jexl) {
        // @formatter:off
        final String src = "$$var t = null;\n" +
                "$$if (t < 0) {\n" +
                "'foo'\n" +
                "$$} else {\n" +
                "'bar'\n" +
                "$$}";
        // @formatter:on
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlContext context = new MapContext();
        final Map<String, Object> vaf = Collections.singletonMap("value", null);
        context.set("vaf", vaf);
        final JxltEngine.Template template = jxlt.createTemplate(src);
        final StringWriter strw = new StringWriter();
        template.evaluate(context, strw);
        return strw.toString();
    }

    private Object run361d(final JexlEngine jexl) {
        final String src = "var foo = 42; var foo = 43;";
        final JexlScript script = jexl.createScript(src);
        return script.execute(null);
    }

    @Test
    public void test301a() {
        final JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(new JexlArithmetic(false)).create();
        final String[] srcs = { "var x = null; x.0", "var x = null; x[0]", "var x = [null,1]; x[0][0]" };
        for (int i = 0; i < srcs.length; ++i) {
            final String src = srcs[i];
            final JexlScript s = jexl.createScript(src);
            try {
                final Object o = s.execute(null);
                if (i > 0) {
                    fail(src + ": Should have failed");
                }
            } catch (final Exception ex) {
                assertTrue(ex.getMessage().contains("x"));
            }
        }
    }

    @Test
    public void test302() {
        final JexlContext jc = new MapContext();
        // @formatter:off
        final String[] strs = {
                "{if (0) 1 else 2; var x = 4;}",
                "if (0) 1; else 2; ",
                "{ if (0) 1; else 2; }",
                "{ if (0) { if (false) 1 else -3 } else 2; }"
        };
        // @formatter:on
        final JexlEngine jexl = new JexlBuilder().create();
        for (final String str : strs) {
            final JexlScript e = jexl.createScript(str);
            final Object o = e.execute(jc);
            final int oo = ((Number) o).intValue() % 2;
            assertEquals(0, oo, () -> "Block result is wrong " + str);
        }
    }

    @Test
    public void test304() {
        final JexlEngine jexlEngine = new JexlBuilder().strict(false).create();
        JexlExpression e304 = jexlEngine.createExpression("overview.limit.var");

        final Map<String, Object> map3 = new HashMap<>();
        map3.put("var", "4711");
        final Map<String, Object> map2 = new HashMap<>();
        map2.put("limit", map3);
        final Map<String, Object> map = new HashMap<>();
        map.put("overview", map2);

        final JexlContext context = new MapContext(map);
        Object value = e304.evaluate(context);
        assertEquals("4711", value); // fails

        map.clear();
        map.put("overview.limit.var", 42);
        value = e304.evaluate(context);
        assertEquals(42, value);

        // @formatter:off
        final String[]  keywords = {
                "if", "else", "do", "while", "for", "break", "continue", "function", "return", "new", "size", "empty",
                "var", "let", "const",
                "null", "true", "false",
                "not", "div", "mod", "and", "or",
                "eq", "ne", "lt", "gt", "ge", "le",
        };
        // @formatter:on
        for (final String keyword : keywords) {
            final String pkw = "e304." + keyword;
            map.put(pkw, 42);
            e304 = jexlEngine.createExpression(pkw);
            value = e304.evaluate(context);
            assertEquals(42, value);
        }
        for (int i = 0; i < keywords.length; ++i) {
            final String pkw = "e304." + keywords[i] + "." + keywords[keywords.length - 1 - i];
            map.put(pkw, 42);
            e304 = jexlEngine.createExpression(pkw);
            value = e304.evaluate(context);
            assertEquals(42, value);
        }
        final String allkw = "e304." + String.join(".", keywords);
        map.put(allkw, 42);
        e304 = jexlEngine.createExpression(allkw);
        value = e304.evaluate(context);
        assertEquals(42, value);
    }

    @Test
    public void test305() {
        final JexlEngine jexl = new JexlBuilder().create();
        JexlScript e;
        e = jexl.createScript("{while(false) {}; var x = 1;}");
        final String str0 = e.getParsedText();
        e = jexl.createScript(str0);
        assertNotNull(e);
        final String str1 = e.getParsedText();
        assertEquals(str0, str1);
    }

    @Test
    public void test306() {
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript e = jexl.createScript("x.y ?: 2");
        final Object o1 = e.execute(null);
        assertEquals(2, o1);
        ctxt.set("x.y", null);
        final Object o2 = e.execute(ctxt);
        assertEquals(2, o2);
    }

    @Test
    public void test306a() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript e = jexl.createScript("x.y ?: 2", "x");
        Object o = e.execute(null, new Object());
        assertEquals(2, o);
        o = e.execute(null);
        assertEquals(2, o);
    }

    @Test
    public void test306b() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript e = jexl.createScript("x?.y ?: 2", "x");
        final Object o1 = e.execute(null, new Object());
        assertEquals(2, o1);
        final Object o2 = e.execute(null);
        assertEquals(2, o2);
    }

    @Test
    public void test306c() {
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript e = jexl.createScript("x.y ?: 2", "x");
        Object o = e.execute(null, new Object());
        assertEquals(2, o);
        o = e.execute(null);
        assertEquals(2, o);
    }

    @Test
    public void test306d() {
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript e = jexl.createScript("x.y[z.t] ?: 2", "x");
        Object o = e.execute(null, new Object());
        assertEquals(2, o);
        o = e.execute(null);
        assertEquals(2, o);
    }

    @Test
    public void test309a() {
        // @formatter:off
        final String src = "<html lang=\"en\">\n"
                + "  <body>\n"
                + "    <h1>Hello World!</h1>\n"
                + "$$ var i = 12++;\n"
                + "  </body>\n"
                + "</html>";
        // @formatter:on
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlInfo info = new JexlInfo("template", 1, 1);
        final JexlException.Parsing xerror = assertThrows(JexlException.Parsing.class, () -> jxlt.createTemplate(info, src));
        assertEquals(4, xerror.getInfo().getLine());
    }

    @Test
    public void test309b() {
        // @formatter:off
        final String src = "<html lang=\"en\">\n"
                + "  <body>\n"
                + "    <h1>Hello World!</h1>\n"
                + "$$ var i = a b c;\n"
                + "  </body>\n"
                + "</html>";
        // @formatter:on
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlInfo info = new JexlInfo("template", 1, 1);
        final JexlException.Parsing xerror = assertThrows(JexlException.Parsing.class, () -> jxlt.createTemplate(info, src));
        assertEquals(4, xerror.getInfo().getLine());
    }

    @Test
    public void test309c() {
        // @formatter:off
        final String src = "<html lang=\"en\">\n"
                + "  <body>\n"
                + "    <h1>Hello World!</h1>\n"
                + "$$ var i =12;\n"
                + "  </body>\n"
                + "</html>";
        // @formatter:on
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlInfo info = new JexlInfo("template", 1, 1);
        final JxltEngine.Template tmplt = jxlt.createTemplate(info, src);
        final String src1 = tmplt.asString();
        final String src2 = tmplt.toString();
        assertEquals(src1, src2);
    }

    @Test
    public void test314() {
        final JexlEngine jexl = new JexlBuilder().strict(true).safe(false).create();
        final Map<String, Object> vars = new HashMap<>();
        final JexlContext ctxt = new VaContext(vars);
        JexlScript script;
        Object result;
        script = jexl.createScript("cell()");
        result = script.execute(ctxt);
        assertEquals(0, result);
        script = jexl.createScript("x.cell()", "x");
        result = script.execute(ctxt, Arrays.asList(10, 20));
        assertEquals(42, result);
        script = jexl.createScript("cell('1', '2')");
        result = script.execute(ctxt);
        assertEquals(2, result);
        script = jexl.createScript("x.cell('1', '2')", "x");
        result = script.execute(ctxt, Arrays.asList(10, 20));
        assertEquals(44, result);

        vars.put("TVALOGAR", null);
        String jexlExp = "TVALOGAR==null ?'SIMON':'SIMONAZO'";
        script = jexl.createScript(jexlExp);
        result = script.execute(ctxt);
        assertEquals("SIMON", result);

        jexlExp = "TVALOGAR.PEPITO==null ?'SIMON':'SIMONAZO'";
        script = jexl.createScript(jexlExp);

        final Map<String, Object> tva = new LinkedHashMap<>();
        tva.put("PEPITO", null);
        vars.put("TVALOGAR", tva);
        result = script.execute(ctxt);
        assertEquals("SIMON", result);

        vars.remove("TVALOGAR");
        ctxt.set("TVALOGAR.PEPITO", null);
        result = script.execute(ctxt);
        assertEquals("SIMON", result);
    }

    @Test
    public void test315() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final Map<String, Object> vars = new HashMap<>();
        final JexlContext ctxt = new VaContext(vars);
        JexlScript script;
        Object result;
        script = jexl.createScript("a?? 42 + 10", "a");
        result = script.execute(ctxt, 32);
        assertEquals(32, result);
        result = script.execute(ctxt, (Object) null);
        assertEquals(52, result);
        script = jexl.createScript("- a??42 + +10", "a");
        result = script.execute(ctxt, 32);
        assertEquals(-32, result);
        result = script.execute(ctxt, (Object) null);
        assertEquals(52, result);
        // long version of ternary
        script = jexl.createScript("a? a : +42 + 10", "a");
        result = script.execute(ctxt, 32);
        assertEquals(32, result);
        result = script.execute(ctxt, (Object) null);
        assertEquals(52, result);
        // short one, elvis, equivalent
        script = jexl.createScript("a ?: +42 + 10", "a");
        result = script.execute(ctxt, 32);
        assertEquals(32, result);
        result = script.execute(ctxt, (Object) null);
        assertEquals(52, result);
    }

    @Test
    public void test317() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlContext ctxt = new MapContext();
        JexlScript script;
        Object result;
        JexlInfo info = new JexlInfo("test317", 1, 1);
        // @formatter:off
        script = jexl.createScript(info, "var f = "
                        + "()-> {x + x }; f",
                "x");
        // @formatter:on
        result = script.execute(ctxt, 21);
        assertInstanceOf(JexlScript.class, result);
        script = (JexlScript) result;
        info = JexlInfo.from(script);
        assertNotNull(info);
        assertEquals("test317", info.getName());
        result = script.execute(ctxt, 21);
        assertEquals(42, result);
    }

    @Test
    public void test322a() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlContext context = new MapContext();

        final String[] ins = { "${'{'}", "${\"{\"}", "${\"{}\"}", "${'{42}'}", "${\"{\\\"\\\"}\"}" };
        final String[] ctls = { "{", "{", "{}", "{42}", "{\"\"}" };
        StringWriter strw;
        JxltEngine.Template template;
        String output;

        for (int i = 0; i < ins.length; ++i) {
            final String src = ins[i];
            template = jxlt.createTemplate("$$", new StringReader(src));
            strw = new StringWriter();
            template.evaluate(context, strw);
            output = strw.toString();
            assertEquals(ctls[i], output);
        }
    }

    @Test
    public void test322b() {
        final JexlContext ctxt = new MapContext();
        final String src = "L'utilisateur ${session.user.name} s'est connecte";
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        StringWriter strw;
        JxltEngine.Template template;
        String output;
        template = jxlt.createTemplate("$$", new StringReader(src));

        ctxt.set("session", new Session322());
        strw = new StringWriter();
        template.evaluate(ctxt, strw);
        output = strw.toString();
        assertEquals("L'utilisateur user322 s'est connecte", output);

        ctxt.set("session.user", new User322());
        strw = new StringWriter();
        template.evaluate(ctxt, strw);
        output = strw.toString();
        assertEquals("L'utilisateur user322 s'est connecte", output);

        ctxt.set("session.user.name", "user322");
        strw = new StringWriter();
        template.evaluate(ctxt, strw);
        output = strw.toString();
        assertEquals("L'utilisateur user322 s'est connecte", output);
    }

    @Test
    public void test323() {
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final Map<String, Object> vars = new HashMap<>();
        final JexlContext jc = new MapContext(vars);
        Object result;

        // nothing in context, ex
        final JexlScript script0 = jexl.createScript("a.n.t.variable");
        JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> script0.execute(jc), "a.n.t.variable is undefined!");
        assertTrue(xvar.toString().contains("a.n.t"));

        // defined and null
        jc.set("a.n.t.variable", null);
        final JexlScript script = jexl.createScript("a.n.t.variable");
        result = script.execute(jc);
        assertNull(result);

        // defined and null, dereference
        jc.set("a.n.t", null);
        final JexlScript script1 = jexl.createScript("a.n.t[0].variable");
        xvar = assertThrows(JexlException.Variable.class, () -> script1.execute(jc), "a.n.t is null!");
        assertTrue(xvar.toString().contains("a.n.t"));

        // undefined, dereference
        vars.remove("a.n.t");
        final JexlScript script2 = jexl.createScript("a.n.t[0].variable");
        xvar = assertThrows(JexlException.Variable.class, () -> script2.execute(jc), "a.n.t is undefined!");
        assertTrue(xvar.toString().contains("a.n.t"));

        // defined, derefence undefined property
        final List<Object> inner = new ArrayList<>();
        vars.put("a.n.t", inner);
        final JexlScript script3 = jexl.createScript("a.n.t[0].variable");
        JexlException.Property xprop = assertThrows(JexlException.Property.class, () -> script3.execute(jc), "a.n.t is null!");
        assertTrue(xprop.toString().contains("0"));

        // defined, derefence undefined property
        inner.add(42);
        final JexlScript script4 = jexl.createScript("a.n.t[0].variable");
        xprop = assertThrows(JexlException.Property.class, () -> script4.execute(jc), "a.n.t is null!");
        assertTrue(xprop.toString().contains("variable"));
    }

    @Test
    public void test324() {
        final JexlEngine jexl = new JexlBuilder().create();
        final String src42 = "new('java.lang.Integer', 42)";
        final JexlExpression expr0 = jexl.createExpression(src42);
        assertEquals(42, expr0.evaluate(null));
        final String parsed = expr0.getParsedText();
        assertEquals(src42, parsed);
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createExpression("new()"), "should not parse");
        assertTrue(xparse.toString().contains(")"));
    }

    @Test
    public void test325() {
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final Map<String, Object> map = new HashMap<String, Object>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object get(final Object key) {
                return super.get(key == null ? "" : key);
            }

            @Override
            public Object put(final String key, final Object value) {
                return super.put(key == null ? "" : key, value);
            }
        };
        map.put("42", 42);
        final JexlContext jc = new MapContext();
        JexlScript script;
        Object result;

        script = jexl.createScript("map[null] = 42", "map");
        result = script.execute(jc, map);
        assertEquals(42, result);
        script = jexl.createScript("map[key]", "map", "key");
        result = script.execute(jc, map, null);
        assertEquals(42, result);
        result = script.execute(jc, map, "42");
        assertEquals(42, result);
    }

    @Test
    public void test330() {
        final JexlEngine jexl = new JexlBuilder().create();
        // Extended form of: 'literal' + VARIABLE 'literal'
        // missing + operator here ---------------^
        // @formatter:off
        final String longExpression = ""
                + //
                "'THIS IS A VERY VERY VERY VERY VERY VERY VERY "
                + //
                "VERY VERY LONG STRING CONCATENATION ' + VARIABLE ' <--- "
                + //
                "error: missing + between VARIABLE and literal'";
        // @formatter:on
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createExpression(longExpression),
                "parsing malformed expression did not throw exception");
        assertTrue(xparse.getMessage().contains("VARIABLE"));
    }

    @Test
    public void test331() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext ctxt = new MapContext();
        JexlScript script;
        Object result;
        script = jexl.createScript("a + '\\n' + b", "a", "b");
        result = script.execute(ctxt, "hello", "world");
        assertTrue(result.toString().contains("\n"));
    }

    @Test
    public void test347() {
        final String src = "A.B == 5";
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(src);
        Object result = script.execute(null);
        // safe navigation is lenient wrt null
        assertFalse((Boolean) result);

        jexl = new JexlBuilder().strict(true).safe(false).create();
        final JexlContext ctxt = new MapContext();
        final JexlScript script1 = jexl.createScript(src);
        // A and A.B undefined
        assertThrows(JexlException.class, () -> script1.execute(ctxt));
        // A is null, A.B is undefined
        ctxt.set("A", null);
        assertThrows(JexlException.class, () -> script1.execute(ctxt), "should only succeed with safe navigation");
        // A.B is null
        ctxt.set("A.B", null);
        result = script1.execute(ctxt);
        assertFalse((Boolean) result);
    }

    @Test
    public void test349() {
        final String text = "(A ? C.D : E)";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlExpression expr = jexl.createExpression(text);
        final JexlScript script = jexl.createScript(text);
    }

    @Test
    public void test361_33() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        assertThrows(JexlException.class, () -> run361c(jexl), "null arg should fail");
    }

    @Test
    public void test361a_32() {
        final JexlEngine jexl = new Engine32(new JexlBuilder().safe(false));
        final Object result = run361a(jexl);
        assertNotNull(result);
    }

    @Test
    public void test361a_33() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        assertThrows(JexlException.class, () -> run361a(jexl), "null arg should fail");
    }

    @Test
    public void test361b_32() {
        final JexlEngine jexl = new Engine32(new JexlBuilder().safe(false).strict(false));
        final Object result = run361b(jexl);
        assertNotNull(result);
    }

    @Test
    public void test361b_33() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        assertThrows(JexlException.class, () -> run361b(jexl), "null arg should fail");
    }

    @Test
    public void test361c_32() {
        final JexlEngine jexl = new Engine32(new JexlBuilder().safe(false).strict(false));
        final String result = run361c(jexl);
        assertNotNull(result);
    }

    @Test
    public void test361d_32() {
        final JexlEngine jexl = new Engine32(new JexlBuilder().lexical(false).lexicalShade(false).safe(false));
        final Object result = run361d(jexl);
        assertNotNull(result);
    }

    @Test
    public void test361d_33() {
        final JexlEngine jexl = new JexlBuilder().lexical(true).lexicalShade(true).safe(false).strict(true).create();
        assertThrows(JexlException.class, () -> run361d(jexl), "null arg should fail");
    }

    @Test
    public void test367() {
        final String text = "var toto; function foo(x) { x }; var tata = 3; foo(3)";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(text);
        final Object result = script.execute(null);
        assertEquals(3, result);
        final String s0 = script.getParsedText();
        final String s1 = script.getSourceText();
        assertNotEquals(s0, s1);
    }

    @Test
    public void test370() {
        final Var370 var370 = new Var370();
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final ObjectContext<Var370> ctxt = new ObjectContext<>(jexl, var370);
        final JexlExpression get = jexl.createExpression("name");
        // not null
        var370.setName("John");
        assertEquals("John", get.evaluate(ctxt));
        assertTrue(ctxt.has("name"));
        // null
        var370.setName(null);
        assertNull(get.evaluate(ctxt));
        assertTrue(ctxt.has("name"));
        // undefined
        final JexlExpression get1 = jexl.createExpression("phone");
        assertFalse(ctxt.has("phone"));
        final JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> get1.evaluate(ctxt), "phone should be undefined!");
        assertEquals("phone", xvar.getVariable());
    }

    @Test
    public void test373b() {
        final String src = "var i = ++1";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlInfo info = new JexlInfo("badscript", 0, 0);
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript(info, src), "should not parse");
        assertTrue(xparse.getMessage().contains("badscript"));
    }

    @Test
    public void test374() {
        final JexlEngine engine = new JexlBuilder().cache(512).strict(true).silent(false).antish(false).safe(false).create();
        // Create expression to evaluate 'name'
        final JexlExpression expr = engine.createExpression("nested.name");
        // Create an object with getter for name
        final TestObject374 myObject = new TestObject374();
        myObject.setName("John");
        final JexlContext context = new ObjectContext<>(engine, myObject);
        // Expect an exception because nested is null, so we are doing null.name
        assertThrows(JexlException.class, () -> expr.evaluate(context));
    }

    @Test
    public void test375() {
        final JexlSandbox jexlSandbox = new JexlSandbox(false);
        jexlSandbox.allow(Type375.class.getName());
        final JexlEngine engine = new JexlBuilder().sandbox(jexlSandbox).create();

        final JexlContext context = new MapContext();
        context.set("Type", Type375.class);

        Object result = engine.createScript("Type.valueOf('DOMICILE')").execute(context);
        assertEquals(Type375.DOMICILE, result);

        result = engine.createScript("Type.DOMICILE").execute(context);
        assertEquals(Type375.DOMICILE, result);
    }

    @Test
    public void test377() {
        final String text = "function add(x, y) { x + y } add(a, b)";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(text, "a", "b");
        final Object result = script.execute(null, 20, 22);
        assertEquals(42, result);
    }

    @Test
    public void test379a() {
        // @formatter:off
        final String src =
                "#pragma jexl.import java.util\n"+
                        "const map = new LinkedHashMap({0 : 'zero'});";
        // @formatter:on
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertNotNull(result);
        assertInstanceOf(LinkedHashMap.class, result);
        assertEquals(1, ((Map) result).size());
    }

    @Test
    public void test383() {
        final JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(new Arithmetic383(true)).create();
        final String src0 = "if (a) 1; else 2;";
        final String src1 = "if (!a) 1; else 2;";
        // local var
        final JexlScript s0 = jexl.createScript(src0, "a");
        final JexlScript s1 = jexl.createScript(src1, "a");
        assertEquals(2, s0.execute(null, (Object) null));
        assertEquals(1, s1.execute(null, (Object) null));
        // global var undefined
        final JexlScript s2 = jexl.createScript(src0);
        final JexlScript s3 = jexl.createScript(src1);
        JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> s2.execute(null, (Object) null));
        assertEquals("a", xvar.getVariable());
        xvar = assertThrows(JexlException.Variable.class, () -> s3.execute(null, (Object) null));
        assertEquals("a", xvar.getVariable());

        // global var null
        final MapContext ctxt = new MapContext();
        ctxt.set("a", null);
        assertEquals(2, s2.execute(ctxt, (Object) null));
        assertEquals(1, s3.execute(ctxt, (Object) null));
    }

    @Test
    public void test384a() {
        final JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        // constant
        for (final String src0 : Arrays.asList("'ABC' + null", "null + 'ABC'")) {
            final JexlContext ctxt = new MapContext();
            final JexlScript s0 = jexl.createScript(src0);
            final JexlException xvar = assertThrows(JexlException.class, () -> s0.execute(ctxt, (Object) null), "null argument should throw");
            assertTrue(xvar.toString().contains("+"));
        }
        // null local a
        for (final String src1 : Arrays.asList("'ABC' + a", "a + 'ABC'")) {
            final JexlContext ctxt = new MapContext();
            final JexlScript s1 = jexl.createScript(src1, "a");
            JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> s1.execute(ctxt, (Object) null), "null argument should throw");
            assertEquals("a", xvar.getVariable());
            // undefined a
            final JexlScript s2 = jexl.createScript(src1);
            xvar = assertThrows(JexlException.Variable.class, () -> s2.execute(ctxt, (Object) null), "null argument should throw");
            assertEquals("a", xvar.getVariable());
            assertTrue(xvar.isUndefined());
            // null a
            ctxt.set("a", null);
            xvar = assertThrows(JexlException.Variable.class, () -> s2.execute(ctxt, (Object) null), "null argument should throw");
            assertEquals("a", xvar.getVariable());
            assertFalse(xvar.isUndefined());
        }
    }

    @Test
    public void test384b() {
        // be explicit about + handling null
        // @formatter:off
        final JexlEngine jexl = new JexlBuilder()
                .arithmetic(new Arithmetic384(true))
                .safe(false)
                .strict(true)
                .create();
        // @formatter:on
        // constant
        for (final String src0 : Arrays.asList("'ABC' + null", "null + 'ABC'")) {
            final JexlContext ctxt = new MapContext();
            final JexlScript s0 = jexl.createScript(src0);
            assertEquals("ABC", s0.execute(ctxt));
        }
        // null local a
        for (final String src1 : Arrays.asList("'ABC' + a", "a + 'ABC'")) {
            final JexlContext ctxt = new MapContext();
            final JexlScript s1 = jexl.createScript(src1, "a");
            assertEquals("ABC", s1.execute(ctxt, (Object) null));
            // undefined a
            final JexlScript s2 = jexl.createScript(src1);
            final JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> s2.execute(ctxt, (Object) null), "null argument should throw");
            assertEquals("a", xvar.getVariable());
            assertTrue(xvar.isUndefined());

            // null a
            ctxt.set("a", null);
            assertEquals("ABC", s1.execute(ctxt, (Object) null));
        }
    }

    @Test
    public void test384c() {
        final Arithmetic384c ja = new Arithmetic384c(true);
        // @formatter:off
        final JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .arithmetic(ja)
                .create();
        // @formatter:on
        assertTrue(ja.toBoolean(jexl.createExpression("3 < 4").evaluate(null)));
        assertTrue(ja.toBoolean(jexl.createExpression("6 <= 8").evaluate(null)));
        assertFalse(ja.toBoolean(jexl.createExpression("6 == 7").evaluate(null)));
        assertTrue(ja.toBoolean(jexl.createExpression("4 > 2").evaluate(null)));
        assertTrue(ja.toBoolean(jexl.createExpression("8 > 6").evaluate(null)));
        assertTrue(ja.toBoolean(jexl.createExpression("7 != 6").evaluate(null)));
        assertEquals(6, ja.getCmpCalls());
    }

    @Test
    public void test384d() {
        final Arithmetic384c ja = new Arithmetic384d(true);
        // @formatter:off
        final JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .arithmetic(ja)
                .create();
        // @formatter:on
        assertTrue(ja.toBoolean(jexl.createExpression("3 < 4").evaluate(null)));
        assertTrue(ja.toBoolean(jexl.createExpression("6 <= 8").evaluate(null)));
        assertFalse(ja.toBoolean(jexl.createExpression("6 == 7").evaluate(null)));
        assertTrue(ja.toBoolean(jexl.createExpression("4 > 2").evaluate(null)));
        assertTrue(ja.toBoolean(jexl.createExpression("8 > 6").evaluate(null)));
        assertTrue(ja.toBoolean(jexl.createExpression("7 != 6").evaluate(null)));
        assertEquals(6, ja.getCmpCalls());
    }

    @Test
    public void test390() throws Exception {
        // @formatter:off
        final JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .debug(true)
                .create();
        // @formatter:on
        JexlScript script = null;
        final String src = "if (true) #pragma one 42";
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript(src), "should have failed parsing");
        assertTrue(xparse.getDetail().contains("pragma"));

        final String src1 = "if (true) { #pragma one 42 }";
        script = jexl.createScript(src1);
        final Object result = script.execute(null);
        debuggerCheck(jexl);
    }

    @Test
    public void test393() {
        // @formatter:off
        final String src = "const total = 0;\n" +
                "if (true) {\n" +
                "  total = 1;\n" +
                "}\n" +
                "total; ";
        final JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .create();
        // @formatter:on
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript(src), "should fail on const total assignment");
        assertTrue(xparse.getMessage().contains("total"));
    }

    @Test
    public void testBackslashes() throws Exception {
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final String src = "\"\b\t\f\"";
        JexlScript s = jexl.createScript(src);
        assertNotNull(s);
        final String ctl = "\b\t\f";
        assertEquals(ctl, s.execute(null));
        String parsed = s.getParsedText();
        assertEquals("'\\b\\t\\f'", parsed);
        s = jexl.createScript(src);
        assertNotNull(s);
        assertEquals(ctl, s.execute(null));
        parsed = s.getParsedText();
        assertEquals("'\\b\\t\\f'", parsed);
    }

    @Test
    public void testDow() {
        // @formatter:off
        final String src = "(y, m, d)->{\n" +
                "// will return 0 for Sunday, 6 for Saturday\n" +
                "const t = [0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4];\n"+
                "if (m < 3) { --y }\n" +
                "(y + y/4 - y/100 + y/400 + t[m-1] + d) % 7;\n" +
            "}";
        // @formatter:on
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript script = jexl.createScript(src);
        Object r = script.execute(null, 2023, 3, 1);
        assertInstanceOf(Number.class, r);
        Number dow = (Number) r;
        assertEquals(3, dow.intValue());
        r = script.execute(null, 1969, 7, 20);
        assertInstanceOf(Number.class, r);
        dow = (Number) r;
        assertEquals(0, dow.intValue());
    }

    @Test
    public void testIssue394() {
        final StringBuilder x = new StringBuilder("foobar");
        assertEquals("foobar", x.toString());
        final String src = "x -> x.setLength(3)";
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, x);
        assertEquals("foo", x.toString());
    }

    @Test
    public void testIssue397() {
        String result;
        final String control = Class397.class.getName();
        final JexlEngine jexl = new JexlBuilder().permissions(RESTRICTED).create();

        final Interface397i instance = new Class397();
        result = (String) jexl.invokeMethod(instance, "summary");
        assertEquals(control, result);

        final Interface397i proxy = createProxy(jexl, instance, new Class[] { Interface397i.class });
        result = (String) jexl.invokeMethod(proxy, "summary");
        assertEquals(control, result);

        final JexlScript script = jexl.createScript("dan.summary()", "dan");
        result = (String) script.execute(null, instance);
        assertEquals(control, result);
        result = (String) script.execute(null, proxy);
        assertEquals(control, result);
    }

    @Test
    public void testIssue398a() {
        // @formatter:off
        final String src = "let m = {\n" +
            "  \"foo\": 1,\n" +
            "  \"bar\": 2,\n" +
            "}";
        // @formatter:on
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null);
        assertInstanceOf(Map.class, result);
        final Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
    }

    @Test
    public void testIssue398b() {
        final Map<String, Object> foo = Collections.singletonMap("X", "x");
        final Map<String, Object> bar = Collections.singletonMap("Y", "y");
        final JexlContext ctxt = new MapContext();
        ctxt.set("foo", foo);
        ctxt.set("bar", bar);
        // @formatter:off
        final String src = "let m = {\n" +
                "  foo.X: 1,\n" +
                "  bar.Y: 2,\n" +
                "}";
        // @formatter:on
        final JexlEngine jexl = new JexlBuilder().create();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(ctxt);
        assertInstanceOf(Map.class, result);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertEquals(1, map.get("x"));
        assertEquals(2, map.get("y"));

        script = jexl.createScript(src, "foo", "bar");
        result = script.execute(null, foo, bar);
        assertInstanceOf(Map.class, result);
        map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertEquals(1, map.get("x"));
        assertEquals(2, map.get("y"));
    }

    @Test
    public void testIssue398c() {
        final JexlEngine jexl = new JexlBuilder().create();
        final Object empty = jexl.createScript("[,...]").execute(null);
        assertNotNull(empty);
        assertTrue(jexl.createScript("[1]").execute(null) instanceof int[]);
        assertTrue(jexl.createScript("[1,...]").execute(null) instanceof ArrayList<?>);
        assertTrue(jexl.createScript("{1}").execute(null) instanceof HashSet<?>);
        assertTrue(jexl.createScript("{1,...}").execute(null) instanceof LinkedHashSet<?>);
        assertTrue(jexl.createScript("{'one': 1}").execute(null) instanceof HashMap<?, ?>);
        assertTrue(jexl.createScript("{'one': 1,...}").execute(null) instanceof LinkedHashMap<?, ?>);
    }

    @Test
    public void testPropagateOptions() {
        // @formatter:off
        final String src0 = "`${$options.strict?'+':'-'}strict"
                + " ${$options.cancellable?'+':'-'}cancellable"
                + " ${$options.lexical?'+':'-'}lexical"
                + " ${$options.lexicalShade?'+':'-'}lexicalShade"
                + " ${$options.sharedInstance?'+':'-'}sharedInstance"
                + " ${$options.safe?'+':'-'}safe`";
        // @formatter:on
        final String text = "#pragma script.mode pro50\n" + "()->{ ()->{ " + src0 + "; } }";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(text);
        JexlContext context = pragmaticContext();
        final JexlScript closure = (JexlScript) script.execute(context);
        final JexlContext opts = new OptionsContext();
        final Object result = closure.execute(opts);
        assertEquals("+strict +cancellable +lexical +lexicalShade -sharedInstance -safe", result);

        final String text0 = "#pragma script.mode pro50\n" + "()->{ " + src0 + "; }";
        final JexlScript script0 = jexl.createScript(text0);
        context = pragmaticContext();
        final Object result0 = script0.execute(context);
        assertEquals("+strict +cancellable +lexical +lexicalShade -sharedInstance -safe", result0);

        final String text1 = "#pragma script.mode pro50\n" + src0;
        final JexlScript script1 = jexl.createScript(text1);
        context = pragmaticContext();
        final Object result1 = script1.execute(context);
        assertEquals("+strict +cancellable +lexical +lexicalShade -sharedInstance -safe", result1);

        final String text2 = src0;
        final JexlScript script2 = jexl.createScript(text2);
        context = pragmaticContext();
        final Object result2 = script2.execute(context);
        assertEquals("-strict -cancellable -lexical -lexicalShade +sharedInstance +safe", result2);
    }

    @Test
    public void tests301b() {
        final JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(new JexlArithmetic(false)).create();
        final Object[] xs = { null, null, new Object[] { null, 1 } };
        final String[] srcs = { "x.0", "x[0]", "x[0][0]" };
        final JexlContext ctxt = new MapContext();
        for (int i = 0; i < xs.length; ++i) {
            ctxt.set("x", xs[i]);
            final String src = srcs[i];
            final JexlScript s = jexl.createScript(src);
            assertThrows(JexlException.class, () -> s.execute(null));
        }
    }

    @Test
    public void testSO20220930() {
        // fill some drivers in a list
        final List<Driver0930> values = new ArrayList<>();
        for (int i = 0; i < 8; ++i) {
            values.add(new Driver0930("drvr" + Integer.toOctalString(i)));
        }
        for (int i = 0; i < 4; ++i) {
            values.add(new Driver0930("favorite" + Integer.toOctalString(i)));
        }
        // Use a context that can filter and that exposes Collectors
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final JexlContext context = new Context0930();
        context.set("values", values);
        context.set("Collectors", Collectors.class);
        // The script with a JEXL 3.2 (lambda function) and 3.3 syntax (lambda expression)
        final String src32 = "values.stream().filter((driver) ->{ driver.attributeName =^ 'favorite' }).collect(Collectors.toList())";
        final String src33 = "values.stream().filter(driver -> driver.attributeName =^ 'favorite').collect(Collectors.toList())";
        for (final String src : Arrays.asList(src32, src33)) {
            final JexlExpression s = jexl.createExpression(src);
            assertNotNull(s);

            final Object r = s.evaluate(context);
            assertNotNull(r);
            // got a filtered list of 4 drivers whose attribute name starts with 'favorite'
            final List<Driver0930> l = (List<Driver0930>) r;
            assertEquals(4, l.size());
            for (final Driver0930 d : l) {
                assertTrue(d.getAttributeName().startsWith("favorite"));
            }
        }
    }

    @Test
    public void testUnsolvableMethod() throws Exception {
        final JexlEngine jexl = new JexlBuilder().create();
        // @formatter:off
        final JexlScript script = jexl.createScript(
            "var myFunction1 = function(object) {"
                + " myNonExistentFunction();"
                + "}"
                + "var myFunction2 = function(object) {"
                + " myFunction1();"
                + "}"
                + "myFunction2();");
        // @formatter:on
        final JexlException.Method unsolvable = assertThrows(JexlException.Method.class, () -> script.execute(new MapContext()));
        assertEquals("myNonExistentFunction", unsolvable.getMethod());
    }
}
