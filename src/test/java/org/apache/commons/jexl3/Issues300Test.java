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

import org.apache.commons.jexl3.internal.Engine32;
import org.apache.commons.jexl3.internal.OptionsContext;
import static org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.jexl3.internal.Util.debuggerCheck;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for reported issue between JEXL-300 and JEXL-399.
 */
public class Issues300Test {
    @Test
    public void test301a() {
        final JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(new JexlArithmetic(false)).create();
        final String[] srcs = new String[]{
                "var x = null; x.0", "var x = null; x[0]", "var x = [null,1]; x[0][0]"
        };
        for (int i = 0; i < srcs.length; ++i) {
            final String src = srcs[i];
            final JexlScript s = jexl.createScript(src);
            try {
                final Object o = s.execute(null);
                if (i > 0) {
                    Assert.fail(src + ": Should have failed");
                }
            } catch (final Exception ex) {
                Assert.assertTrue(ex.getMessage().contains("x"));
            }
        }
    }

    @Test
    public void tests301b() {
        final JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(new JexlArithmetic(false)).create();
        final Object[] xs = new Object[]{null, null, new Object[]{null, 1}};
        final String[] srcs = new String[]{
                "x.0", "x[0]", "x[0][0]"
        };
        final JexlContext ctxt = new MapContext();
        for (int i = 0; i < xs.length; ++i) {
            ctxt.set("x", xs[i]);
            final String src = srcs[i];
            final JexlScript s = jexl.createScript(src);
            try {
                final Object o = s.execute(null);
                Assert.fail(src + ": Should have failed");
            } catch (final Exception ex) {
                //
            }
        }
    }

    @Test
    public void test302() {
        final JexlContext jc = new MapContext();
        final String[] strs = new String[]{
                "{if (0) 1 else 2; var x = 4;}",
                "if (0) 1; else 2; ",
                "{ if (0) 1; else 2; }",
                "{ if (0) { if (false) 1 else -3 } else 2; }"
        };
        final JexlEngine jexl = new JexlBuilder().create();
        for (final String str : strs) {
            final JexlScript e = jexl.createScript(str);
            final Object o = e.execute(jc);
            final int oo = ((Number) o).intValue() % 2;
            Assert.assertEquals("Block result is wrong " + str, 0, oo);
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

        final String[]  keywords = new String[]{
                "if", "else", "do", "while", "for", "break", "continue", "function", "return", "new", "size", "empty",
                "var", "let", "const",
                "null", "true", "false",
                "not", "div", "mod", "and", "or",
                "eq", "ne", "lt", "gt", "ge", "le",

        };
        for(int i = 0; i < keywords.length; ++i) {
            String pkw = "e304." + keywords[i];
            map.put(pkw, 42);
            e304 = jexlEngine.createExpression(pkw);
            value = e304.evaluate(context);
            assertEquals(42, value);
        }
        for(int i = 0; i < keywords.length; ++i) {
            String pkw = "e304." + keywords[i] + "." + keywords[keywords.length - 1 - i];
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
        Assert.assertNotNull(e);
        final String str1 = e.getParsedText();
        Assert.assertEquals(str0, str1);
    }

    @Test
    public void test306() {
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript e = jexl.createScript("x.y ?: 2");
        final Object o1 = e.execute(null);
        Assert.assertEquals(2, o1);
        ctxt.set("x.y", null);
        final Object o2 = e.execute(ctxt);
        Assert.assertEquals(2, o2);
    }

    @Test
    public void test306a() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript e = jexl.createScript("x.y ?: 2", "x");
        Object o = e.execute(null, new Object());
        Assert.assertEquals(2, o);
        o = e.execute(null);
        Assert.assertEquals(2, o);
    }

    @Test
    public void test306b() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlScript e = jexl.createScript("x?.y ?: 2", "x");
        final Object o1 = e.execute(null, new Object());
        Assert.assertEquals(2, o1);
        final Object o2 = e.execute(null);
        Assert.assertEquals(2, o2);
    }

    @Test
    public void test306c() {
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript e = jexl.createScript("x.y ?: 2", "x");
        Object o = e.execute(null, new Object());
        Assert.assertEquals(2, o);
        o = e.execute(null);
        Assert.assertEquals(2, o);
    }

    @Test
    public void test306d() {
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript e = jexl.createScript("x.y[z.t] ?: 2", "x");
        Object o = e.execute(null, new Object());
        Assert.assertEquals(2, o);
        o = e.execute(null);
        Assert.assertEquals(2, o);
    }

    @Test
    public void test309a() {
        final String src = "<html lang=\"en\">\n"
                + "  <body>\n"
                + "    <h1>Hello World!</h1>\n"
                + "$$ var i = 12++;\n"
                + "  </body>\n"
                + "</html>";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlInfo info = new JexlInfo("template", 1, 1);
        try {
            final JxltEngine.Template tmplt = jxlt.createTemplate(info, src);
            Assert.fail("shoud have thrown exception");
        } catch (final JexlException.Parsing xerror) {
            Assert.assertEquals(4, xerror.getInfo().getLine());
        }
    }

    @Test
    public void test309b() {
        final String src = "<html lang=\"en\">\n"
                + "  <body>\n"
                + "    <h1>Hello World!</h1>\n"
                + "$$ var i = a b c;\n"
                + "  </body>\n"
                + "</html>";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlInfo info = new JexlInfo("template", 1, 1);
        try {
            final JxltEngine.Template tmplt = jxlt.createTemplate(info, src);
            Assert.fail("shoud have thrown exception");
        } catch (final JexlException.Parsing xerror) {
            Assert.assertEquals(4, xerror.getInfo().getLine());
        }
    }

    @Test
    public void test309c() {
        final String src = "<html lang=\"en\">\n"
                + "  <body>\n"
                + "    <h1>Hello World!</h1>\n"
                + "$$ var i =12;\n"
                + "  </body>\n"
                + "</html>";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlInfo info = new JexlInfo("template", 1, 1);
        try {
            final JxltEngine.Template tmplt = jxlt.createTemplate(info, src);
            final String src1 = tmplt.asString();
            final String src2 = tmplt.toString();
            Assert.assertEquals(src1, src2);
        } catch (final JexlException.Parsing xerror) {
            Assert.assertEquals(4, xerror.getInfo().getLine());
        }
    }

    public static class VaContext extends MapContext {
        VaContext(final Map<String, Object> vars) {
            super(vars);
        }

        public int cell(final String... ms) {
            return ms.length;
        }

        public int cell(final List<?> l, final String... ms) {
            return 42 + cell(ms);
        }
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
        Assert.assertEquals(0, result);
        script = jexl.createScript("x.cell()", "x");
        result = script.execute(ctxt, Arrays.asList(10, 20));
        Assert.assertEquals(42, result);
        script = jexl.createScript("cell('1', '2')");
        result = script.execute(ctxt);
        Assert.assertEquals(2, result);
        script = jexl.createScript("x.cell('1', '2')", "x");
        result = script.execute(ctxt, Arrays.asList(10, 20));
        Assert.assertEquals(44, result);

        vars.put("TVALOGAR", null);
        String jexlExp = "TVALOGAR==null?'SIMON':'SIMONAZO'";
        script = jexl.createScript(jexlExp);
        result = script.execute(ctxt);
        Assert.assertEquals("SIMON", result);

        jexlExp = "TVALOGAR.PEPITO==null?'SIMON':'SIMONAZO'";
        script = jexl.createScript(jexlExp);

        final Map<String, Object> tva = new LinkedHashMap<>();
        tva.put("PEPITO", null);
        vars.put("TVALOGAR", tva);
        result = script.execute(ctxt);
        Assert.assertEquals("SIMON", result);

        vars.remove("TVALOGAR");
        ctxt.set("TVALOGAR.PEPITO", null);
        result = script.execute(ctxt);
        Assert.assertEquals("SIMON", result);
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
        Assert.assertEquals(32, result);
        result = script.execute(ctxt, (Object) null);
        Assert.assertEquals(52, result);
        script = jexl.createScript("- a??42 + +10", "a");
        result = script.execute(ctxt, 32);
        Assert.assertEquals(-32, result);
        result = script.execute(ctxt, (Object) null);
        Assert.assertEquals(52, result);
        // long version of ternary
        script = jexl.createScript("a? a : +42 + 10", "a");
        result = script.execute(ctxt, 32);
        Assert.assertEquals(32, result);
        result = script.execute(ctxt, (Object) null);
        Assert.assertEquals(52, result);
        // short one, elvis, equivalent
        script = jexl.createScript("a ?: +42 + 10", "a");
        result = script.execute(ctxt, 32);
        Assert.assertEquals(32, result);
        result = script.execute(ctxt, (Object) null);
        Assert.assertEquals(52, result);
    }


    @Test
    public void test317() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlContext ctxt = new MapContext();
        JexlScript script;
        Object result;
        JexlInfo info = new JexlInfo("test317", 1, 1);
        script = jexl.createScript(info, "var f = "
                        + "()-> {x + x }; f",
                "x");
        result = script.execute(ctxt, 21);
        Assert.assertTrue(result instanceof JexlScript);
        script = (JexlScript) result;
        info = JexlInfo.from(script);
        Assert.assertNotNull(info);
        Assert.assertEquals("test317", info.getName());
        result = script.execute(ctxt, 21);
        Assert.assertEquals(42, result);
    }

    @Test
    public void test322a() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlContext context = new MapContext();

        final String[] ins = new String[]{
                "${'{'}", "${\"{\"}", "${\"{}\"}", "${'{42}'}", "${\"{\\\"\\\"}\"}"
        };
        final String[] ctls = new String[]{
                "{", "{", "{}", "{42}", "{\"\"}"
        };
        StringWriter strw;
        JxltEngine.Template template;
        String output;

        for (int i = 0; i < ins.length; ++i) {
            final String src = ins[i];
            try {
                template = jxlt.createTemplate("$$", new StringReader(src));
            } catch (final JexlException xany) {
                Assert.fail(src);
                throw xany;
            }
            strw = new StringWriter();
            template.evaluate(context, strw);
            output = strw.toString();
            Assert.assertEquals(ctls[i], output);
        }
    }

    public static class User322 {
        public String getName() {
            return "user322";
        }
    }

    public static class Session322 {
        public User322 getUser() {
            return new User322();
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
        Assert.assertEquals("L'utilisateur user322 s'est connecte", output);

        ctxt.set("session.user", new User322());
        strw = new StringWriter();
        template.evaluate(ctxt, strw);
        output = strw.toString();
        Assert.assertEquals("L'utilisateur user322 s'est connecte", output);

        ctxt.set("session.user.name", "user322");
        strw = new StringWriter();
        template.evaluate(ctxt, strw);
        output = strw.toString();
        Assert.assertEquals("L'utilisateur user322 s'est connecte", output);
    }

    @Test
    public void test323() {
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final Map<String, Object> vars = new HashMap<>();
        final JexlContext jc = new MapContext(vars);
        JexlScript script;
        Object result;

        // nothing in context, ex
        try {
            script = jexl.createScript("a.n.t.variable");
            result = script.execute(jc);
            Assert.fail("a.n.t.variable is undefined!");
        } catch (final JexlException.Variable xvar) {
            Assert.assertTrue(xvar.toString().contains("a.n.t"));
        }

        // defined and null
        jc.set("a.n.t.variable", null);
        script = jexl.createScript("a.n.t.variable");
        result = script.execute(jc);
        Assert.assertNull(result);

        // defined and null, dereference
        jc.set("a.n.t", null);
        try {
            script = jexl.createScript("a.n.t[0].variable");
            result = script.execute(jc);
            Assert.fail("a.n.t is null!");
        } catch (final JexlException.Variable xvar) {
            Assert.assertTrue(xvar.toString().contains("a.n.t"));
        }

        // undefined, dereference
        vars.remove("a.n.t");
        try {
            script = jexl.createScript("a.n.t[0].variable");
            result = script.execute(jc);
            Assert.fail("a.n.t is undefined!");
        } catch (final JexlException.Variable xvar) {
            Assert.assertTrue(xvar.toString().contains("a.n.t"));
        }
        // defined, derefence undefined property
        final List<Object> inner = new ArrayList<>();
        vars.put("a.n.t", inner);
        try {
            script = jexl.createScript("a.n.t[0].variable");
            result = script.execute(jc);
            Assert.fail("a.n.t is null!");
        } catch (final JexlException.Property xprop) {
            Assert.assertTrue(xprop.toString().contains("0"));
        }
        // defined, derefence undefined property
        inner.add(42);
        try {
            script = jexl.createScript("a.n.t[0].variable");
            result = script.execute(jc);
            Assert.fail("a.n.t is null!");
        } catch (final JexlException.Property xprop) {
            Assert.assertTrue(xprop.toString().contains("variable"));
        }

    }

    @Test
    public void test324() {
        final JexlEngine jexl = new JexlBuilder().create();
        final String src42 = "new('java.lang.Integer', 42)";
        final JexlExpression expr0 = jexl.createExpression(src42);
        Assert.assertEquals(42, expr0.evaluate(null));
        final String parsed = expr0.getParsedText();
        Assert.assertEquals(src42, parsed);
        try {
            final JexlExpression expr = jexl.createExpression("new()");
            Assert.fail("should not parse");
        } catch (final JexlException.Parsing xparse) {
            Assert.assertTrue(xparse.toString().contains(")"));
        }
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
        Assert.assertEquals(42, result);
        script = jexl.createScript("map[key]", "map", "key");
        result = script.execute(jc, map, null);
        Assert.assertEquals(42, result);
        result = script.execute(jc, map, "42");
        Assert.assertEquals(42, result);
    }

    @Test
    public void test330() {
        final JexlEngine jexl = new JexlBuilder().create();
        // Extended form of: 'literal' + VARIABLE   'literal'
        // missing + operator here ---------------^
        final String longExpression = ""
                + //
                "'THIS IS A VERY VERY VERY VERY VERY VERY VERY "
                + //
                "VERY VERY LONG STRING CONCATENATION ' + VARIABLE ' <--- "
                + //
                "error: missing + between VARIABLE and literal'";
        try {
            jexl.createExpression(longExpression);
            Assert.fail("parsing malformed expression did not throw exception");
        } catch (final JexlException.Parsing exception) {
            Assert.assertTrue(exception.getMessage().contains("VARIABLE"));
        }
    }

    @Test
    public void test331() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext ctxt = new MapContext();
        JexlScript script;
        Object result;
        script = jexl.createScript("a + '\\n' + b", "a", "b");
        result = script.execute(ctxt, "hello", "world");
        Assert.assertTrue(result.toString().contains("\n"));
    }

    @Test
    public void test347() {
        final String src = "A.B == 5";
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null);
        // safe navigation is lenient wrt null
        Assert.assertFalse((Boolean) result);

        jexl = new JexlBuilder().strict(true).safe(false).create();
        JexlContext ctxt = new MapContext();
        script = jexl.createScript(src);
        // A and A.B undefined
        try {
            result = script.execute(ctxt);
            Assert.fail("should only succeed with safe navigation");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
        // A is null, A.B is undefined
        ctxt.set("A", null);
        try {
            result = script.execute(ctxt);
            Assert.fail("should only succeed with safe navigation");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
        // A.B is null
        ctxt.set("A.B", null);
        result = script.execute(ctxt);
        Assert.assertFalse((Boolean) result);
    }


    @Test public void test349() {
        String text = "(A ? C.D : E)";
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlExpression expr = jexl.createExpression(text);
        JexlScript script = jexl.createScript(text);
    }

    static JexlContext pragmaticContext() {
        final JexlOptions opts = new JexlOptions();
        opts.setFlags( "-strict", "-cancellable", "-lexical", "-lexicalShade", "+safe", "+sharedInstance");
        return new JexlTestCase.PragmaticContext(opts);
    }

    @Test public void testPropagateOptions() {
        final String src0 = "`${$options.strict?'+':'-'}strict"
                + " ${$options.cancellable?'+':'-'}cancellable"
                + " ${$options.lexical?'+':'-'}lexical"
                + " ${$options.lexicalShade?'+':'-'}lexicalShade"
                + " ${$options.sharedInstance?'+':'-'}sharedInstance"
                + " ${$options.safe?'+':'-'}safe`";
        String text = "#pragma script.mode pro50\n" +
                "()->{ ()->{ "+src0+"; } }";
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlScript script = jexl.createScript(text);
        JexlContext context = pragmaticContext();
        JexlScript closure = (JexlScript) script.execute(context);
        JexlContext opts = new OptionsContext();
        Object result = closure.execute(opts);
        Assert.assertEquals("+strict +cancellable +lexical +lexicalShade -sharedInstance -safe", result);

        String text0 = "#pragma script.mode pro50\n" +
                "()->{ "+src0+"; }";
        JexlScript script0 = jexl.createScript(text0);
        context = pragmaticContext();
        Object result0 = script0.execute(context);
        Assert.assertEquals("+strict +cancellable +lexical +lexicalShade -sharedInstance -safe", result0);

        String text1 = "#pragma script.mode pro50\n"+src0;
        JexlScript script1 = jexl.createScript(text1);
        context = pragmaticContext();
        Object result1 = script1.execute(context);
        Assert.assertEquals("+strict +cancellable +lexical +lexicalShade -sharedInstance -safe", result1);

        String text2 = src0;
        JexlScript script2 = jexl.createScript(text2);
        context = pragmaticContext();
        Object result2 = script2.execute(context);
        Assert.assertEquals("-strict -cancellable -lexical -lexicalShade +sharedInstance +safe", result2);
    }

    @Test
    public void test361a_32() {
        JexlEngine jexl = new Engine32(new JexlBuilder().safe(false));
        Object result  = run361a(jexl);
        Assert.assertNotNull(result);
    }

    @Test
    public void test361a_33() {
        JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        try {
            Object result = run361a(jexl);
            Assert.fail("null arg should fail");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    private Object run361a(JexlEngine jexl) {
        String src = "()-> { ()-> { if (versionFile != null) { return 'foo'; } else { return 'bar'; }} }";
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null);
        JexlScript rs = (JexlScript) result;
        return rs.execute(null);
    }

    @Test
    public void test361b_33() {
        JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        try {
            Object result = run361b(jexl);
            Assert.fail("null arg should fail");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    @Test
    public void test361b_32() {
        JexlEngine jexl = new Engine32(new JexlBuilder().safe(false).strict(false));
        Object result = run361b(jexl);
        Assert.assertNotNull(result);
    }

    private Object run361b(JexlEngine jexl) {
        String src = "()-> { ()-> {" +
                "var voa = vaf.value;\n" +
                "if (voa != NaN && voa <= 0)" +
                "{ return 'foo'; } else { return 'bar'; }" +
                "} }";
        JexlContext context = new MapContext();
        Map<String,Object> vaf = Collections.singletonMap("value", null);
        context.set("vaf", vaf);
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null);
        JexlScript rs = (JexlScript) result;
        return rs.execute(context);
    }

    @Test
    public void test361_33() {
        JexlEngine jexl = new JexlBuilder().safe(false).strict(true).create();
        try {
            run361c(jexl);
            Assert.fail("null arg should fail");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    @Test
    public void test361c_32() {
        JexlEngine jexl = new Engine32(new JexlBuilder().safe(false).strict(false));
        String result = run361c(jexl);
        Assert.assertNotNull(result);
    }

    private String run361c(JexlEngine jexl) {
        String src = "$$var t = null;\n" +
                "$$if (t < 0) {\n" +
                "'foo'\n" +
                "$$} else {\n" +
                "'bar'\n" +
                "$$}";
        JxltEngine jxlt = jexl.createJxltEngine();
        JexlContext context = new MapContext();
        Map<String,Object> vaf = Collections.singletonMap("value", null);
        context.set("vaf", vaf);
        JxltEngine.Template template = jxlt.createTemplate(src);
        StringWriter strw = new StringWriter();
        template.evaluate(context, strw);
        return strw.toString();
    }

    @Test
    public void test361d_32() {
        JexlEngine jexl = new Engine32(new JexlBuilder().lexical(false).lexicalShade(false).safe(false));
        Object result  = run361d(jexl);
        Assert.assertNotNull(result);
    }

    @Test
    public void test361d_33() {
        JexlEngine jexl = new JexlBuilder().lexical(true).lexicalShade(true).safe(false).strict(true).create();
        try {
            Object result = run361d(jexl);
            Assert.fail("null arg should fail");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    private Object run361d(JexlEngine jexl) {
        String src = "var foo = 42; var foo = 43;";
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null);
        return result;
    }


    @Test public void test367() {
        String text = "var toto; function foo(x) { x }; var tata = 3; foo(3)";
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlScript script = jexl.createScript(text);
        Object result = script.execute(null);
        Assert.assertEquals(3, result);
        String s0 = script.getParsedText();
        String s1 = script.getSourceText();
        Assert.assertNotEquals(s0, s1);
    }

    public static class Var370 {
        private String name = null;
        public void setName(String s) {
            name = s;
        }
        public String getName() {
            return name;
        }
    }

    @Test public void test370() {
        Var370 var370 = new Var370();
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        ObjectContext<Var370> ctxt = new ObjectContext<>(jexl, var370);
        JexlExpression get = jexl.createExpression("name");
        // not null
        var370.setName("John");
        Assert.assertEquals("John",get.evaluate(ctxt));
        Assert.assertTrue(ctxt.has("name"));
        // null
        var370.setName(null);
        Assert.assertNull(get.evaluate(ctxt));
        Assert.assertTrue(ctxt.has("name"));
        // undefined
        get = jexl.createExpression("phone");
        Assert.assertFalse(ctxt.has("phone"));
        try {
            get.evaluate(ctxt);
            Assert.fail("phone should be undefined!");
        } catch (JexlException.Variable xvar) {
            Assert.assertEquals("phone", xvar.getVariable());
        }
    }

    public static class TestObject374 {
        private String name;
        private TestObject374 nested = null;
        public String getName() {
            return name;
        }
        public void setName(String pName) {
            this.name = pName;
        }
        public TestObject374 getNested() {
            return nested;
        }
        public void setNested(TestObject374 pNested) {
            nested = pNested;
        }
    }

    @Test
    public void test374() {
        JexlEngine engine = new JexlBuilder().cache(512).strict(true).silent(false).antish(false).safe(false).create();
        // Create expression to evaluate 'name'
        JexlExpression expr = engine.createExpression("nested.name");
        // Create an object with getter for name
        TestObject374 myObject = new TestObject374();
        myObject.setName("John");
        JexlContext context = new ObjectContext<>(engine, myObject);
        // Expect an exception because nested is null, so we are doing null.name
        try {
            Object result = expr.evaluate(context);
            Assert.fail("An exception expected, but got: " + result);
        } catch (JexlException ex) {
            // Expected
            //ex.printStackTrace();
        }
    }

    @Test
    public void test375() {
        JexlSandbox jexlSandbox = new JexlSandbox(false);
        jexlSandbox.allow(Type375.class.getName());
        JexlEngine engine = new JexlBuilder().sandbox(jexlSandbox).create();

        JexlContext context = new MapContext();
        context.set("Type", Type375.class);

        Object result = engine.createScript("Type.valueOf('DOMICILE')").execute(context);
        Assert.assertEquals(Type375.DOMICILE, result);

        result = engine.createScript("Type.DOMICILE").execute(context);
        Assert.assertEquals(Type375.DOMICILE, result);
    }

    public enum Type375 {
        DELIVERY_ADDRESS,
        DOMICILE
    }


    @Test
    public void test377() {
        String text = "function add(x, y) { x + y } add(a, b)";
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlScript script = jexl.createScript(text, "a", "b");
        Object result = script.execute(null, 20, 22);
        Assert.assertEquals(42, result);
    }

    @Test
    public void test379a() {
        final String src =
                "#pragma jexl.import java.util\n"+
                        "const map = new LinkedHashMap({0 : 'zero'});";
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlScript script = jexl.createScript(src);
        Assert.assertNotNull(script);
        Object result = script.execute(null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof LinkedHashMap);
        Assert.assertEquals(1, ((Map) result).size());
    }

    @Test
    public void test373b() {
        final String src = "var i = ++1";
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlInfo info = new JexlInfo("badscript", 0, 0);
        try {
            JexlScript script = jexl.createScript(info, src);
            Assert.fail("should not parse");
        } catch (JexlException.Parsing xparse) {
            String msg = xparse.getMessage();
            Assert.assertTrue(msg.contains("badscript"));
        }
    }

    @Test
    public void testBackslashes() throws Exception {
        JexlEngine jexl = new JexlBuilder().safe(false).create();
        String src = "\"\b\t\f\"";
        JexlScript s = jexl.createScript(src);
        Assert.assertNotNull(s);
        String ctl = "\b\t\f";
        Assert.assertEquals(ctl, s.execute(null));
        String parsed = s.getParsedText();
        Assert.assertEquals("'\\b\\t\\f'", parsed);
        s = jexl.createScript(src);
        Assert.assertNotNull(s);
        Assert.assertEquals(ctl, s.execute(null));
        parsed = s.getParsedText();
        Assert.assertEquals("'\\b\\t\\f'", parsed);
    }

    /**
     * Mock driver.
     */
    public static class Driver0930 {
        private String name;
        Driver0930(String n) {
            name = n;
        }
        public String getAttributeName() {
            return name;
        }
    }

    public static class Context0930 extends MapContext {
        /**
         * This allows using a JEXL lambda as a filter.
         * @param stream the stream
         * @param filter the lambda to use as filter
         * @return the filtered stream
         */
        public Stream<?> filter(Stream<?> stream, JexlScript filter) {
            return stream.filter(x -> Boolean.TRUE.equals(filter.execute(this, x)));
        }
    }

    @Test
    public void testSO20220930() {
        // fill some drivers in a list
        List<Driver0930> values = new ArrayList<>();
        for(int i = 0; i < 8; ++i) {
            values.add(new Driver0930("drvr" + Integer.toOctalString(i)));
        }
        for(int i = 0; i < 4; ++i) {
            values.add(new Driver0930("favorite" + Integer.toOctalString(i)));
        }
        // Use a context that can filter and that exposes Collectors
        JexlEngine jexl = new JexlBuilder().safe(false).create();
        JexlContext context = new Context0930();
        context.set("values", values);
        context.set("Collectors", Collectors.class);
        // The script with a JEXL 3.2 (lambda function) and 3.3 syntax (lambda expression)
        String src32 = "values.stream().filter((driver) ->{ driver.attributeName =^ 'favorite' }).collect(Collectors.toList())";
        String src33 = "values.stream().filter(driver -> driver.attributeName =^ 'favorite').collect(Collectors.toList())";
        for(String src : Arrays.asList(src32, src33)) {
            JexlExpression s = jexl.createExpression(src);
            Assert.assertNotNull(s);

            Object r = s.evaluate(context);
            Assert.assertNotNull(r);
            // got a filtered list of 4 drivers whose attribute name starts with 'favorite'
            List<Driver0930> l = (List<Driver0930>) r;
            Assert.assertEquals(4, l.size());
            for (Driver0930 d : l) {
                Assert.assertTrue(d.getAttributeName().startsWith("favorite"));
            }
        }
    }

    public static class Arithmetic383 extends JexlArithmetic {
        public Arithmetic383(boolean astrict) {
            super(astrict);
        }
        @Override
        public boolean isStrict(JexlOperator op) {
            switch(op) {
                case NOT:
                case CONDITION:
                    return false;
            }
            return super.isStrict(op);
        }
    }

    @Test public void test383() {
        JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(new Arithmetic383(true)).create();
        String src0 =  "if (a) 1; else 2;";
        String src1 = "if (!a) 1; else 2;";
        // local var
        JexlScript s0 = jexl.createScript(src0, "a");
        JexlScript s1 = jexl.createScript(src1, "a");
        Assert.assertEquals(2, s0.execute(null, (Object) null));
        Assert.assertEquals(1, s1.execute(null, (Object) null));
        // global var undefined
        s0 = jexl.createScript(src0);
        s1 = jexl.createScript(src1);
        try {
            Assert.assertEquals(2, s0.execute(null, (Object) null));
        } catch (JexlException.Variable xvar) {
            Assert.assertEquals("a", xvar.getVariable());
        }
        try {
            Assert.assertEquals(1, s1.execute(null, (Object) null));
        } catch (JexlException.Variable xvar) {
            Assert.assertEquals("a", xvar.getVariable());
        }
        // global var null
        MapContext ctxt = new MapContext();
        ctxt.set("a", null);
        Assert.assertEquals(2, s0.execute(ctxt, (Object) null));
        Assert.assertEquals(1, s1.execute(ctxt, (Object) null));
    }

    public static class Arithmetic384 extends JexlArithmetic {
        public Arithmetic384(boolean astrict) {
            super(astrict);
        }

        @Override
        public boolean isStrict(JexlOperator op) {
            if (JexlOperator.ADD == op) {
                return false;
            }
            return super.isStrict(op);
        }
    }
    @Test
    public void test384a() {
        JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .create();
        // constant
        for(String src0 : Arrays.asList("'ABC' + null", "null + 'ABC'")) {
            JexlContext ctxt = new MapContext();
            JexlScript s0 = jexl.createScript(src0);
            try {
                s0.execute(ctxt, (Object) null);
                Assert.fail("null argument should throw");
            } catch (JexlException xvar) {
                Assert.assertTrue(xvar.toString().contains("+"));
            }
        }
        // null local a
        for(String src1 : Arrays.asList("'ABC' + a", "a + 'ABC'")) {
            JexlContext ctxt = new MapContext();
            JexlScript s1 = jexl.createScript(src1, "a");
            try {
                s1.execute(ctxt, (Object) null);
                Assert.fail("null argument should throw");
            } catch (JexlException.Variable xvar) {
                Assert.assertEquals("a", xvar.getVariable());
            }
            // undefined a
            s1 = jexl.createScript(src1);
            try {
                s1.execute(ctxt, (Object) null);
                Assert.fail("null argument should throw");
            } catch (JexlException.Variable xvar) {
                Assert.assertEquals("a", xvar.getVariable());
                Assert.assertTrue(xvar.isUndefined());
            }
            // null a
            ctxt.set("a", null);
            try {
                s1.execute(ctxt, (Object) null);
                Assert.fail("null argument should throw");
            } catch (JexlException.Variable xvar) {
                Assert.assertEquals("a", xvar.getVariable());
                Assert.assertFalse(xvar.isUndefined());
            }
        }
    }
    @Test
    public void test384b() {
        // be explicit about + handling null
        JexlEngine jexl = new JexlBuilder()
                .arithmetic(new Arithmetic384(true))
                .safe(false)
                .strict(true)
                .create();
        // constant
        for(String src0 : Arrays.asList("'ABC' + null", "null + 'ABC'")) {
            JexlContext ctxt = new MapContext();
            JexlScript s0 = jexl.createScript(src0);
            Assert.assertEquals("ABC", s0.execute(ctxt));
        }
        // null local a
        for(String src1 : Arrays.asList("'ABC' + a", "a + 'ABC'")) {
            JexlContext ctxt = new MapContext();
            JexlScript s1 = jexl.createScript(src1, "a");
            Assert.assertEquals("ABC", s1.execute(ctxt, (Object) null));
            // undefined a
            s1 = jexl.createScript(src1);
            try {
                s1.execute(ctxt, (Object) null);
                Assert.fail("null argument should throw");
            } catch (JexlException.Variable xvar) {
                Assert.assertEquals("a", xvar.getVariable());
                Assert.assertTrue(xvar.isUndefined());
            }
            // null a
            ctxt.set("a", null);
            Assert.assertEquals("ABC", s1.execute(ctxt, (Object) null));
        }
    }
    @Test
    public void test390() throws Exception {
        final JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .debug(true)
                .create();
        JexlScript script = null;
        String src;
        src = "if (true) #pragma one 42";
        try {
            script = jexl.createScript(src);
            Assert.fail("should have failed parsing");
        } catch (JexlException.Parsing xparse) {
            Assert.assertTrue(xparse.getDetail().contains("pragma"));
        }
        src = "if (true) { #pragma one 42 }";
        script = jexl.createScript(src);
        Object result = script.execute(null);
        debuggerCheck(jexl);
    }

    public static class Arithmetic384c extends JexlArithmetic {
        AtomicInteger cmp = new AtomicInteger(0);
        int getCmpCalls() {
            return cmp.get();
        }
        public Arithmetic384c(boolean astrict) {
            super(astrict);
        }
        public Arithmetic384c(boolean astrict, MathContext bigdContext, int bigdScale) {
            super(astrict, bigdContext, bigdScale);
        }
        @Override
        protected int compare(Object l, Object r, String op) {
            cmp.incrementAndGet();
            return super.compare(l, r, op);
        }
    }

    public static class Arithmetic384d extends Arithmetic384c {
        public Arithmetic384d(boolean astrict) {
            super(astrict);
        }
        public Arithmetic384d(boolean astrict, MathContext bigdContext, int bigdScale) {
            super(astrict, bigdContext, bigdScale);
        }
    }

    @Test
    public void test384c() {
        Arithmetic384c ja = new Arithmetic384c(true);
        JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .arithmetic(ja)
                .create();
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("3 < 4").evaluate(null)));
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("6 <= 8").evaluate(null)));
        Assert.assertFalse(ja.toBoolean(jexl.createExpression("6 == 7").evaluate(null)));
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("4 > 2").evaluate(null)));
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("8 > 6").evaluate(null)));
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("7 != 6").evaluate(null)));
        Assert.assertEquals(6, ja.getCmpCalls());
    }

    @Test
    public void test384d() {
        Arithmetic384c ja = new Arithmetic384d(true);
        JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .arithmetic(ja)
                .create();
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("3 < 4").evaluate(null)));
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("6 <= 8").evaluate(null)));
        Assert.assertFalse(ja.toBoolean(jexl.createExpression("6 == 7").evaluate(null)));
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("4 > 2").evaluate(null)));
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("8 > 6").evaluate(null)));
        Assert.assertTrue(ja.toBoolean(jexl.createExpression("7 != 6").evaluate(null)));
        Assert.assertEquals(6, ja.getCmpCalls());
    }

    @Test
    public void test393() {
        String src = "const total = 0;\n" +
                "if (true) {\n" +
                "  total = 1;\n" +
                "}\n" +
                "total; ";
        JexlEngine jexl = new JexlBuilder()
                .safe(false)
                .strict(true)
                .create();
        try {
            JexlScript script = jexl.createScript(src);
            Assert.fail("should fail on const total assignment");
        } catch (JexlException.Parsing xparse) {
            Assert.assertTrue(xparse.getMessage().contains("total"));
        }
    }

    @Test public void testDow() {
        String src = "(y, m, d)->{\n" +
                "// will return 0 for Sunday, 6 for Saturday\n" +
                "const t = [0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4];\n"+
                "if (m < 3) { --y }\n" +
                "(y + y/4 - y/100 + y/400 + t[m-1] + d) % 7;\n" +
            "}";
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript script = jexl.createScript(src);
        Object r = script.execute(null, 2023, 3, 1);
        Assert.assertTrue(r instanceof Number);
        Number dow = (Number) r;
        Assert.assertEquals(3, dow.intValue());
        r = script.execute(null, 1969, 7, 20);
        Assert.assertTrue(r instanceof Number);
        dow = (Number) r;
        Assert.assertEquals(0, dow.intValue());
    }

    @Test public void testIssue394() {
        StringBuilder x = new StringBuilder("foobar");
        Assert.assertEquals("foobar", x.toString());
        String src = "x -> x.setLength(3)";
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript script = jexl.createScript(src);
        Object result = script.execute(null, x);
        Assert.assertEquals("foo", x.toString());
    }

    public interface Interface397i {
        String summary();
    }
    static private class Class397 implements Interface397i {
        @Override public String summary() {
            return getClass().getName();
        }
    }
    <T> T createProxy(final JexlEngine jexl, final Object o, final Class[] clazzz) {
        // a JEX-based delegating proxy
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), clazzz,
            (proxy, method, args) ->  jexl.invokeMethod(o, method.getName(), args)
        );
    }

    @Test public void testIssue397() {
        String result;
        final String control = Class397.class.getName();
        final JexlEngine jexl = new JexlBuilder().permissions(RESTRICTED).create();

        Interface397i instance = new Class397();
        result = (String) jexl.invokeMethod(instance, "summary");
        Assert.assertEquals(control, result);

        Interface397i proxy = createProxy(jexl, instance, new Class[] { Interface397i.class }) ;
        result = (String) jexl.invokeMethod(proxy, "summary");
        Assert.assertEquals(control, result);

        JexlScript script = jexl.createScript("dan.summary()", "dan");
        result = (String) script.execute(null, instance);
        Assert.assertEquals(control, result);
        result = (String) script.execute(null, proxy);
        Assert.assertEquals(control, result);
    }
}
