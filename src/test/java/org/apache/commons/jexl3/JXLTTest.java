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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jexl3.internal.Debugger;
import org.apache.commons.jexl3.internal.TemplateDebugger;
import org.apache.commons.jexl3.internal.TemplateInterpreter;
import org.apache.commons.jexl3.internal.introspection.Permissions;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for the UnifiedEL.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class JXLTTest extends JexlTestCase {
    public static class Context311 extends MapContext
      implements JexlContext.OptionsHandle, JexlContext.ThreadLocal {
        private JexlOptions options;

        public Executor311 exec(final String name) {
            return new Executor311(name);
        }

        @Override
        public JexlOptions getEngineOptions() {
            return options;
        }

        JexlOptions newOptions() {
            options = new JexlOptions();
            return options;
        }

        public void setOptions(final JexlOptions o) {
            options = o;
        }
    }
    public static class Executor311 {
        private final String name;

        public Executor311(final String name) {
            this.name = name;
        }
        // Injects name as first arg of any called script
        public Object execute(final JexlScript script, final Object ...args) {
            Object[] actuals;
            if (args != null && args.length > 0) {
                actuals = new Object[args.length + 1] ;
                System.arraycopy(args, 0, actuals, 1, args.length);
                actuals[0] = name;
            } else {
                actuals = new Object[]{name};
            }
            return script.execute(JexlEngine.getThreadContext(), actuals);
        }
    }
    public static class Froboz {
        int value;

        public Froboz(final int v) {
            value = v;
        }

        public int getValue() {
            return value;
        }

        public int plus10() {
            final int i = value;
            value += 10;
            return i;
        }

        public void setValue(final int v) {
            value = v;
        }
    }
    public static class FrobozWriter extends PrintWriter {
        public FrobozWriter(final Writer w) {
            super(w);
        }

        public void print(final Froboz froboz) {
            super.print("froboz{");
            super.print(froboz.value);
            super.print("}");
        }

        @Override
        public String toString() {
            return out.toString();
        }
    }
    private static final Log LOGGER = LogFactory.getLog(JxltEngine.class);
    private static final Permissions NOJEXL3 = new Permissions() {
        @Override public boolean allow(final Class<?> clazz) {
            final String cname = clazz.getName();
            return !cname.contains("jexl3") || cname.contains("311");
        }
    };

   public static List<JexlBuilder> engines() {
       final JexlFeatures f = new JexlFeatures();
       f.lexical(true).lexicalShade(true);
      return Arrays.asList(
              new JexlBuilder().silent(false).lexical(true).lexicalShade(true).cache(128).strict(true),
              new JexlBuilder().features(f).silent(false).cache(128).strict(true),
              new JexlBuilder().silent(false).cache(128).strict(true));
   }

   private static String refactor(final TemplateDebugger td, final JxltEngine.Template ts) {
    final boolean dbg = td.debug(ts);
    if (dbg) {
        return td.toString();
    }
    return "";
}

    private final MapContext vars = new MapContext();

    private JexlEvalContext context;

    private JexlBuilder BUILDER;

    private JexlEngine ENGINE;

    private JxltEngine JXLT;

    public JXLTTest() {
        super("JXLTTest");
    }

    boolean contains(final Set<List<String>> set, final List<String> list) {
        for (final List<String> sl : set) {
            if (sl.equals(list)) {
                return true;
            }
        }
        return false;
    }

    /** Extract the source from a toString-ed expression. */
    private String getSource(final String tostring) {
        final int len = tostring.length();
        int sc = tostring.lastIndexOf(" /*= ");
        if (sc >= 0) {
            sc += " /*= ".length();
        }
        final int ec = tostring.lastIndexOf(" */");
        if (sc >= 0 && ec >= 0 && ec > sc && ec < len) {
            return tostring.substring(sc, ec);
        }
        return tostring;

    }

    private void init(final JexlBuilder builder) {
        BUILDER = builder;
        ENGINE = BUILDER.create();
        JXLT = ENGINE.createJxltEngine();
    }

    private boolean isLexicalShade() {
        JexlOptions options = context.getEngineOptions();
        if (options.isLexicalShade()) {
            return true;
        }
        options = new JexlOptions().set(ENGINE);
        return options.isLexicalShade();
    }

    @BeforeEach
    @Override
    public void setUp() {
        // ensure jul logging is only error
        java.util.logging.Logger.getLogger(org.apache.commons.jexl3.JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
        context = new JexlEvalContext(vars);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        debuggerCheck(ENGINE);
        super.tearDown();
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311a(final JexlBuilder builder) {
        init(builder);
        final JexlContext ctx = null;
        // @formatter:off
        final String rpt
                = "$$((a)->{\n"
                + "<p>Universe ${a}</p>\n"
                + "$$})(42)";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx, strw);
        final String output = strw.toString();
        assertEquals("<p>Universe 42</p>\n", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311b(final JexlBuilder builder) {
        init(builder);
        final JexlContext ctx311 = new Context311();
        // @formatter:off
        final String rpt
                = "$$ exec('42').execute(()->{\n"
                + "<p>Universe 42</p>\n"
                + "$$})";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        assertEquals("<p>Universe 42</p>\n", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311c(final JexlBuilder builder) {
        init(builder);
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        // @formatter:off
        final String rpt
                = "$$ exec('42').execute((a)->{"
                + "\n<p>Universe ${a}</p>"
                + "\n$$})";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        assertEquals("<p>Universe 42</p>\n", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311d(final JexlBuilder builder) {
        init(builder);
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        // @formatter:off
        final String rpt
                = "$$ exec('4').execute((a, b)->{"
                + "\n<p>Universe ${a}${b}</p>"
                + "\n$$}, '2')";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        assertEquals("<p>Universe 42</p>\n", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311e(final JexlBuilder builder) {
        init(builder);
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        // @formatter:off
        final String rpt
                = "exec('4').execute((a, b)->{"
                + " '<p>Universe ' + a + b + '</p>'"
                + "}, '2')";
        // @formatter:on
        final JexlScript script = JEXL.createScript(rpt);
        final String output = script.execute(ctx311, 42).toString();
        assertEquals("<p>Universe 42</p>", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311f(final JexlBuilder builder) {
        init(builder);
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        // @formatter:off
        final String rpt
                = "exec('4').execute((a, b)->{"
                + " `<p>Universe ${a}${b}</p>`"
                + "}, '2')";
        // @formatter:on
        final JexlScript script = JEXL.createScript(rpt);
        final String output = script.execute(ctx311, 42).toString();
        assertEquals("<p>Universe 42</p>", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311g(final JexlBuilder builder) {
        init(builder);
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        // @formatter:off
        final String rpt
                = "(a, b)->{"
                + " `<p>Universe ${a}${b}</p>`"
                + "}";
        // @formatter:on
        final JexlScript script = JEXL.createScript(rpt);
        final String output = script.execute(ctx311, "4", "2").toString();
        assertEquals("<p>Universe 42</p>", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311h(final JexlBuilder builder) {
        init(builder);
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        final String rpt= " `<p>Universe ${a}${b}</p>`";
        final JexlScript script = JEXL.createScript(rpt, "a", "b");
        final String output = script.execute(ctx311, "4", "2").toString();
        assertEquals("<p>Universe 42</p>", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test311i(final JexlBuilder builder) {
        init(builder);
        final JexlContext ctx311 = new Context311();
        // @formatter:off
        final String rpt
                = "$$var u = 'Universe'; exec('4').execute((a, b)->{"
                + "\n<p>${u} ${a}${b}</p>"
                + "\n$$}, '2')";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        assertEquals("<p>Universe 42</p>\n", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test315(final JexlBuilder builder) {
        init(builder);
        String s315;
        StringWriter strw;
        JxltEngine.Template t315;
        String output;

        s315 = "<report/>$";
        t315 = JXLT.createTemplate("$$", new StringReader(s315));
        strw = new StringWriter();
        t315.evaluate(context, strw);
        output = strw.toString();
        assertEquals(s315, output);

        s315 = "<foo/>#";
        t315 = JXLT.createTemplate("$$", new StringReader(s315));
         strw = new StringWriter();
        t315.evaluate(context, strw);
        output = strw.toString();
        assertEquals(s315, output);

        s315 = "<bar/>\\";
        t315 = JXLT.createTemplate("$$", new StringReader(s315));
        strw = new StringWriter();
        t315.evaluate(context, strw);
        output = strw.toString();
        assertEquals(s315, output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void test42(final JexlBuilder builder) {
        init(builder);
        // @formatter:off
        final String test42
                = "$$ for(var x : list) {\n"
                + "$$   if (x == 42) {\n"
                + "Life, the universe, and everything\n"
                + "$$   } else if (x > 42) {\n"
                + "The value ${x} is over fourty-two\n"
                + "$$   } else {\n"
                + "The value ${x} is under fourty-two\n"
                + "$$   }\n"
                + "$$ }\n";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(test42), "list");
        final StringWriter strw = new StringWriter();
        final int[] list = {1, 3, 5, 42, 169};
        t.evaluate(context, strw, list);
        final String output = strw.toString();
        // @formatter:off
        final String out42
                = "The value 1 is under fourty-two\n"
                + "The value 3 is under fourty-two\n"
                + "The value 5 is under fourty-two\n"
                + "Life, the universe, and everything\n"
                + "The value 169 is over fourty-two\n";
        // @formatter:on
        assertEquals(out42, output);

        final String dstr = t.asString();
        assertNotNull(dstr);

        final TemplateDebugger td = new TemplateDebugger();
        final String refactored = refactor(td, t);
        assertNotNull(refactored);
        assertEquals(test42, refactored);
    }


    @Test
    void testParseIdentifier() {
        assertNull(JexlArithmetic.parseIdentifier(null));
        assertNull(JexlArithmetic.parseIdentifier(""));
        assertNull(JexlArithmetic.parseIdentifier("za"));
        assertNull(JexlArithmetic.parseIdentifier("a"));
        assertNull(JexlArithmetic.parseIdentifier("00"));
        assertNull(JexlArithmetic.parseIdentifier("01"));
        assertNull(JexlArithmetic.parseIdentifier("001"));
        assertNull(JexlArithmetic.parseIdentifier("12345678901"));

        assertEquals(0, JexlArithmetic.parseIdentifier("0"));
        assertEquals(10, JexlArithmetic.parseIdentifier("10"));
        assertEquals(100, JexlArithmetic.parseIdentifier("100"));
        assertEquals(42, JexlArithmetic.parseIdentifier("42"));
        assertEquals(42000, JexlArithmetic.parseIdentifier("42000"));

        assertEquals(42, JexlArithmetic.parseIdentifier(42));
    }

    /**
     * A remediation to strict interpolation that consistently attempts to coerce to integer
     * index for lists and keys for maps. (see JEXL-425)
     */
    public static class Arithmetic425 extends JexlArithmetic {
        public Arithmetic425(final boolean astrict) {
            super(astrict);
        }

        public Object propertyGet(final List list, final String property) {
            final Integer id = JexlArithmetic.parseIdentifier(property);
            return id == null? JexlEngine.TRY_FAILED : list.get(id);
        }

        public Object propertySet(final List list, final String property, final Object value) {
            final Integer id = JexlArithmetic.parseIdentifier(property);
            if (id != null) {
                return list.set(id, value);
            }
            return JexlEngine.TRY_FAILED;
        }

        public Object propertyGet(final Map list, final String property) {
            // determine if keys are integers by performing a check on first one
            if (list.keySet().stream().findFirst().orElse(null) instanceof Number) {
                final Integer id = JexlArithmetic.parseIdentifier(property);
                if (id != null) {
                    return list.get(id);
                }
            }
            return JexlEngine.TRY_FAILED;
        }

        public Object propertySet(final Map list, final String property, final Object value) {
            // determine if keys are integers by performing a check on first one
            if (list.keySet().stream().findFirst().orElse(null) instanceof Number) {
                final Integer id = JexlArithmetic.parseIdentifier(property);
                if (id != null) {
                    list.put(id, value);
                    return value;
                }
            }
            return JexlEngine.TRY_FAILED;
        }
    }

    @Test void test425a() {
        final String S42 = "fourty-two";
        final JexlBuilder builder = new JexlBuilder().strictInterpolation(true);
        final JexlEngine jexl = builder.create();
        JexlScript script;
        Object result;
        script = jexl.createScript("let x = 42; let y = `${x}`; y");
        result = script.execute(null);
        assertInstanceOf(String.class, result);
        assertEquals("42", result);
        final Map<Object,Object> map = Collections.singletonMap("42", S42);

        script = jexl.createScript("let x = 42; map.`${x}`", "map");
        result = script.execute(null, map);
        assertEquals(S42, result);
        final List<String> list = Collections.singletonList(S42);

        final JexlScript finalScript = script;
        assertThrows(JexlException.Property.class, () -> finalScript.execute(null, list));
        script = jexl.createScript("let x = 0; list[x]", "list");
        assertEquals(S42, result);
    }

    @Test void test425b() {
        final String S42 = "fourty-two";
        final JexlEngine jexl = new JexlBuilder().strictInterpolation(false).create();
        run425bc(jexl, false);
        run425bc(jexl, false);
    }

    @Test void test425c() {
        final JexlEngine jexl = new JexlBuilder()
                .cache(8)
                .arithmetic(new Arithmetic425(true))
                .strictInterpolation(true).create();
        run425bc(jexl, true);
        run425bc(jexl, true);
    }

    void run425bc(final JexlEngine jexl, final boolean strictInterpolation) {
        final String S42 = "fourty-two";
        JexlScript script;
        Object result;
        script = jexl.createScript("let x = 42; let y = `${x}`; y");
        result = script.execute(null);
        assertEquals(!strictInterpolation? 42 : "42", result);
        Map<Object,Object> map = Collections.singletonMap(0, S42);
        script = jexl.createScript("let x = 0; map.`${x}`", "map");
        result = script.execute(null, map);
        assertEquals(S42, result);
        List<String> list = Collections.singletonList(S42);
        result = script.execute(null, list);
        assertEquals(S42, result);

        map = new HashMap<>(map);
        map.put(0, "nothing");
        script = jexl.createScript("let x = 0; map.`${x}` = S42", "map", "S42");
        result = script.execute(null, map, S42);
        assertEquals(S42, result);
        list = new ArrayList<>(list);
        list.set(0, "nothing");
        result = script.execute(null, map, S42);
        assertEquals(S42, result);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testAssign(final JexlBuilder builder) {
        init(builder);
        final Froboz froboz = new Froboz(32);
        context.set("froboz", froboz);
        final JxltEngine.Expression assign = JXLT.createExpression("${froboz.value = 42}");
        final JxltEngine.Expression check = JXLT.createExpression("${froboz.value}");
        Object o = assign.evaluate(context);
        assertEquals(Integer.valueOf(42), o);
        o = check.evaluate(context);
        assertEquals(Integer.valueOf(42), o);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testBadContextNested(final JexlBuilder builder) {
        init(builder);
        final JxltEngine.Expression expr = JXLT.createExpression("#{${hi}+'.world'}");
        final JexlContext none = null;
        final JxltEngine.Exception xjexl = assertThrows(JxltEngine.Exception.class, () -> expr.evaluate(none), "should be malformed");
        LOGGER.debug(xjexl.getMessage());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testCharAtBug(final JexlBuilder builder) {
        init(builder);
        context.set("foo", "abcdef");
        final JexlOptions options = context.getEngineOptions();
        JxltEngine.Expression expr = JXLT.createExpression("${foo.substring(2,4)/*comment*/}");
        Object o = expr.evaluate(context);
        assertEquals("cd", o);
        context.set("bar", "foo");
        try {
            options.setSilent(true);
            expr = JXLT.createExpression("#{${bar}+'.charAt(-2)'}");
            expr = expr.prepare(context);
            o = expr.evaluate(context);
            assertNull(o);
        } finally {
            options.setSilent(false);
        }
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testCommentedTemplate0(final JexlBuilder builder) {
        init(builder);
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        JxltEngine.Template tmplt;
        // @formatter:off
        final String src = "$$/*\n"
                + "Hello\n"
                + "$$*/";
        tmplt = jxlt.createTemplate(src);
        assertNotNull(tmplt);
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        assertTrue(strw.toString().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testCommentedTemplate1(final JexlBuilder builder) {
        init(builder);
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        JxltEngine.Template tmplt;
        // @formatter:off
        final String src = "$$/*\n"
                + "one\n"
                + "$$*/\n"
                + "42\n"
                + "$$/*\n"
                + "three\n"
                + "$$*/\n";
        // @formatter:on
        tmplt = jxlt.createTemplate(src);
        assertNotNull(tmplt);
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        assertEquals("42\n", strw.toString());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testComposite(final JexlBuilder builder) {
        init(builder);
        final String source = "Dear ${p} ${name};";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        context.set("p", "Mr");
        context.set("name", "Doe");
        assertTrue(expr.isImmediate(), "expression should be immediate");
        Object o = expr.evaluate(context);
        assertEquals("Dear Mr Doe;", o);
        context.set("p", "Ms");
        context.set("name", "Jones");
        o = expr.evaluate(context);
        assertEquals("Dear Ms Jones;", o);
        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testConstant0(final JexlBuilder builder) {
        init(builder);
        final JexlContext none = null;
        final String source = "Hello World!";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        assertSame(expr.prepare(none), expr, "prepare should return same expression");
        final Object o = expr.evaluate(none);
        assertTrue(expr.isImmediate(), "expression should be immediate");
        assertEquals("Hello World!", o);

        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testConstant2(final JexlBuilder builder) {
        init(builder);
        final JexlContext none = null;
        final String source = "${size({'map':123,'map2':456})}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        //assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        final Object o = expr.evaluate(none);
        assertTrue(expr.isImmediate(), "expression should be immediate");
        assertEquals(2, o);

        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testConstant3(final JexlBuilder builder) {
        init(builder);
        final JexlContext none = null;
        final String source = "#{size({'map':123,'map2':456})}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        //assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        final Object o = expr.evaluate(none);
        assertTrue(expr.isDeferred(), "expression should be deferred");
        assertEquals(2, o);

        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testConstant4(final JexlBuilder builder) {
        init(builder);
        final JexlContext none = null;
        final String source = "#{ ${size({'1':2,'2': 3})} }";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        //assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        final Object o = expr.evaluate(none);
        assertTrue(expr.isDeferred(), "expression should be deferred");
        assertEquals(2, o);

        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testConstantTemplate(final JexlBuilder builder) {
        init(builder);
        // @formatter:off
        final String src = "<script>\n" +
                "      function test(src){\n" +
                "        var res = src.replace(/\\n\\t\\s/g, '\\n');\n" +
                "      }\n" +
                "      test();\n" +
                "    </script>";
        // @formatter:on
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        JxltEngine.Template tmplt;
        tmplt = jxlt.createTemplate(src);
        assertNotNull(tmplt);
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        final String result = strw.toString();
        assertEquals(src, result);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testDbgEscapes(final JexlBuilder builder) {
        init(builder);
        // @formatter:off
        final String[] srcs = {
                "jexl:print('hello\\'\\nworld')",
                "'hello\\tworld'",
                "'hello\\nworld'",
                "'hello\\fworld'",
                "'hello\\rworld'"
        };
        // @formatter:on
        for(final String src : srcs) {
            final JexlScript script = ENGINE.createScript(src);
            final Debugger dbg = new Debugger();
            dbg.debug(script);
            final String msrc = dbg.toString();
            assertEquals(src, msrc);
        }
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testDeferred(final JexlBuilder builder) {
        init(builder);
        final JexlContext none = null;
        final String source = "#{'world'}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        assertTrue(expr.isDeferred(), "expression should be deferred");
        final String as = expr.prepare(none).asString();
        assertEquals("${'world'}", as, "prepare should return immediate version");
        final Object o = expr.evaluate(none);
        assertEquals("world", o);

        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testEscape(final JexlBuilder builder) {
        init(builder);
        final JexlContext none = null;
        JxltEngine.Expression expr;
        Object o;
        // $ and # are escapable in TemplateEngine
        expr = JXLT.createExpression("\\#{'world'}");
        o = expr.evaluate(none);
        assertEquals("#{'world'}", o);
        expr = JXLT.createExpression("\\${'world'}");
        o = expr.evaluate(none);
        assertEquals("${'world'}", o);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testEscapeString(final JexlBuilder builder) {
        init(builder);
        final JxltEngine.Expression expr = JXLT.createExpression("\\\"${'world\\'s finest'}\\\"");
        final JexlContext none = null;
        final Object o = expr.evaluate(none);
        assertEquals("\"world's finest\"", o);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testImmediate(final JexlBuilder builder) {
        init(builder);
        final JexlContext none = null;
        final String source = "${'Hello ' + 'World!'}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        final JxltEngine.Expression prepared = expr.prepare(none);
        assertEquals("Hello World!", prepared.asString(), "prepare should return same expression");
        final Object o = expr.evaluate(none);
        assertTrue(expr.isImmediate(), "expression should be immediate");
        assertEquals("Hello World!", o);

        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testImmediateTemplate(final JexlBuilder builder) {
        init(builder);
        context.set("tables", new String[]{"table1", "table2"});
        context.set("w" ,"x=1");
        // @formatter:off
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(
             "select * from \n"+
             "$$var comma = false; \n"+
             "$$for(var c : tables) { \n"+
             "$$  if (comma) $jexl.write(','); else comma = true;\n"+
             "${c}"+
             "\n$$}\n"+
             "where ${w}\n"
        ));
        // @formatter:on
        final StringWriter strw = new StringWriter();
        //vars.clear();
        t.evaluate(context, strw);
        final String output = strw.toString();
        assertTrue(output.contains("table1") && output.contains("table2"));
    }
    @ParameterizedTest
    @MethodSource("engines")
    void testInheritedDebugger(final JexlBuilder builder) {
        init(builder);
        final String src = "if ($A) { $B + 1; } else { $C - 2 }";
        final JexlEngine jexl = JXLT.getEngine();
        final JexlScript script = jexl.createScript(src);

        final Debugger sd = new Debugger();
        final String rscript = sd.debug(script)? sd.toString() : null;
        assertNotNull(rscript);

        final TemplateDebugger td = new TemplateDebugger();
        final String refactored = td.debug(script)? td.toString() : null;
        assertNotNull(refactored);
        assertEquals(refactored, rscript);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testInterpolation(final JexlBuilder builder) {
        init(builder);
        final String expr =  "`Hello \n${user}`";
        final JexlScript script = ENGINE.createScript(expr);
        context.set("user", "Dimitri");
        Object value = script.execute(context);
        assertEquals("Hello \nDimitri", value, expr);
        context.set("user", "Rahul");
        value = script.execute(context);
        assertEquals("Hello \nRahul", value, expr);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testInterpolationGlobal(final JexlBuilder builder) {
        init(builder);
        if (isLexicalShade()) {
            context.set("user", null);
        }
        final String expr =  "user='Dimitri'; `Hello \n${user}`";
        final Object value = ENGINE.createScript(expr).execute(context);
        assertEquals("Hello \nDimitri", value, expr);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testInterpolationLocal(final JexlBuilder builder) {
        init(builder);
        final String expr =  "var user='Henrib'; `Hello \n${user}`";
        final Object value = ENGINE.createScript(expr).execute(context);
        assertEquals("Hello \nHenrib", value, expr);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testInterpolationLvsG(final JexlBuilder builder) {
        init(builder);
        if (isLexicalShade()) {
            context.set("user", null);
        }
        final String expr =  "user='Dimitri'; var user='Henrib'; `H\\\"ello \n${user}`";
        final Object value = ENGINE.createScript(expr).execute(context);
        assertEquals("H\"ello \nHenrib", value, expr);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testInterpolationLvsG2(final JexlBuilder builder) {
        init(builder);
        if (isLexicalShade()) {
            context.set("user", null);
        }
        final String expr =  "user='Dimitri'; var user='Henrib'; `H\\`ello \n${user}`";
        final Object value = ENGINE.createScript(expr).execute(context);
        assertEquals("H`ello \nHenrib", value, expr);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testInterpolationParameter(final JexlBuilder builder) {
        init(builder);
        final String expr =  "(user)->{`Hello \n${user}`}";
        final JexlScript script = ENGINE.createScript(expr);
        Object value = script.execute(context, "Henrib");
        assertEquals("Hello \nHenrib", value, expr);
        value = ENGINE.createScript(expr).execute(context, "Dimitri");
        assertEquals("Hello \nDimitri", value, expr);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testLexicalTemplate(final JexlBuilder builder) {
        init(builder);
        final JexlOptions opts = new JexlOptions();
        final JexlContext ctxt = new PragmaticContext(opts);
        opts.setCancellable(false);
        opts.setStrict(false);
        opts.setSafe(true);
        opts.setLexical(false);
        opts.setLexicalShade(false);
        // @formatter:off
        final String src0 = "${$options.strict?'+':'-'}strict"
                + " ${$options.cancellable?'+':'-'}cancellable"
                + " ${$options.lexical?'+':'-'}lexical"
                + " ${$options.lexicalShade?'+':'-'}lexicalShade"
                + " ${$options.safe?'+':'-'}safe";
        // @formatter:on
        final JxltEngine.Template tmplt0 = JXLT.createTemplate("$$", new StringReader(src0));
        final Writer strw0 = new StringWriter();
        tmplt0.evaluate(ctxt, strw0);
        final String output0 = strw0.toString();
        final JexlFeatures features = BUILDER.features();
        if (features != null && features.isLexical() && features.isLexicalShade()) {
            assertEquals("-strict -cancellable +lexical +lexicalShade +safe", output0);
        } else {
            assertEquals("-strict -cancellable -lexical -lexicalShade +safe", output0);
        }

        final String src = "$$ #pragma script.mode pro50\n" + src0;

        final JxltEngine.Template tmplt = JXLT.createTemplate("$$", new StringReader(src));
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        final String output = strw.toString();
        assertEquals("+strict +cancellable +lexical +lexicalShade -safe", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testMalformed(final JexlBuilder builder) {
        init(builder);
        final JxltEngine.Exception xjexl = assertThrows(JxltEngine.Exception.class, () -> JXLT.createExpression("${'world'"), "should be malformed");
        LOGGER.debug(xjexl.getMessage());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testMalformedNested(final JexlBuilder builder) {
        init(builder);
        final JxltEngine.Exception xjexl = assertThrows(JxltEngine.Exception.class, () -> JXLT.createExpression("#{${hi} world}"), "should be malformed");
        LOGGER.debug(xjexl.getMessage());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testMalformedNested2(final JexlBuilder builder) {
        init(builder);
        final JxltEngine.Exception xjexl = assertThrows(JxltEngine.Exception.class, () -> JXLT.createExpression("#{${hi} world}"), "should be malformed");
        LOGGER.debug(xjexl.getMessage());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testNested(final JexlBuilder builder) {
        init(builder);
        final String source = "#{${hi}+'.world'}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);

        final Set<List<String>> evars = expr.getVariables();
        assertEquals(1, evars.size());
        assertTrue(contains(evars, Collections.singletonList("hi")));

        context.set("hi", "greeting");
        context.set("greeting.world", "Hello World!");
        assertTrue(expr.isDeferred(), "expression should be deferred");
        final Object o = expr.evaluate(context);
        assertEquals("Hello World!", o);

        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testNestedTemplate(final JexlBuilder builder) {
        init(builder);
        final String source = "#{${hi}+'.world'}";
        final JxltEngine.Template expr = JXLT.createTemplate(source, "hi");

        context.set("greeting.world", "Hello World!");
        final StringWriter strw = new StringWriter();
        expr.evaluate(context, strw, "greeting");
        final String o = strw.toString();
        assertEquals("Hello World!", o);

        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testNonEscapeString(final JexlBuilder builder) {
        init(builder);
        final JxltEngine.Expression expr = JXLT.createExpression("c:\\some\\windows\\path");
        final JexlContext none = null;
        final Object o = expr.evaluate(none);
        assertEquals("c:\\some\\windows\\path", o);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testOneLiner(final JexlBuilder builder) {
        init(builder);
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("fourty-two"));
        final StringWriter strw = new StringWriter();
        t.evaluate(context, strw);
        final String output = strw.toString();
        assertEquals("fourty-two", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testOneLinerVar(final JexlBuilder builder) {
        init(builder);
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("fourty-${x}"));
        final StringWriter strw = new StringWriter();
        context.set("x", "two");
        t.evaluate(context, strw);
        final String output = strw.toString();
        assertEquals("fourty-two", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testPrepareEvaluate(final JexlBuilder builder) {
        init(builder);
        final String source = "Dear #{p} ${name};";
        final JxltEngine.Expression expr = JXLT.createExpression("Dear #{p} ${name};");
        assertTrue(expr.isDeferred(), "expression should be deferred");

        final Set<List<String>> evars = expr.getVariables();
        assertEquals(1, evars.size());
        assertTrue(contains(evars, Collections.singletonList("name")));
        context.set("name", "Doe");
        final JxltEngine.Expression phase1 = expr.prepare(context);
        final String as = phase1.asString();
        assertEquals("Dear ${p} Doe;", as);
        final Set<List<String>> evars1 = phase1.getVariables();
        assertEquals(1, evars1.size());
        assertTrue(contains(evars1, Collections.singletonList("p")));
        vars.clear();
        context.set("p", "Mr");
        context.set("name", "Should not be used in 2nd phase");
        final Object o = phase1.evaluate(context);
        assertEquals("Dear Mr Doe;", o);

        final String p1 = getSource(phase1.toString());
        assertEquals(source, getSource(phase1.toString()));
        assertEquals(source, getSource(expr.toString()));
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testPrepareTemplate(final JexlBuilder builder) {
        init(builder);
        // @formatter:off
        final String source
                = "$$ for(var x : list) {\n"
                + "${l10n}=#{x}\n"
                + "$$ }\n";
        // @formatter:on
        final int[] args = {42};
        final JxltEngine.Template tl10n = JXLT.createTemplate(source, "list");
        final String dstr = tl10n.asString();
        assertNotNull(dstr);
        final Set<List<String>> vars = tl10n.getVariables();
        assertFalse(vars.isEmpty());
        context.set("l10n", "valeur");
        final JxltEngine.Template tpFR = tl10n.prepare(context);
        context.set("l10n", "value");
        final JxltEngine.Template tpEN = tl10n.prepare(context);
        context.set("l10n", null);

        StringWriter strw;
        strw = new StringWriter();
        tpFR.evaluate(context, strw, args);
        final String outFR = strw.toString();
        assertEquals("valeur=42\n", outFR);

        context.set("l10n", null);
        strw = new StringWriter();
        tpEN.evaluate(context, strw, args);
        final String outEN = strw.toString();
        assertEquals("value=42\n", outEN);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testReport(final JexlBuilder builder) {
        init(builder);
        // @formatter:off
        final String rpt
                = "<report>\n"
                + "\n"
                + "\n$$ var a = 1;"
                + "\n$$ var x = 2;"
                + "\n"
                + "\n$$ var y = 9;"
                + "\n"
                + "\n        ${x + y}"
                + "\n</report>\n";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(context, strw);
        final String output = strw.toString();
        final String ctl = "<report>\n\n\n\n\n        11\n</report>\n";
        assertEquals(ctl, output);

        final TemplateDebugger td = new TemplateDebugger();
        final String refactored = refactor(td, t);
        assertNotNull(refactored);
        assertEquals(rpt, refactored);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testReport1(final JexlBuilder builder) {
        init(builder);
        // @formatter:off
        final String rpt
                = "<report>\n"
                + "this is ${x}\n"
                + "${x + 1}\n"
                + "${x + 2}\n"
                + "${x + 3}\n"
                + "</report>\n";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        context.set("x", 42);
        t.evaluate(context, strw, 42);
        final String output = strw.toString();
        int count = 0;
        for (int i = 0; i < output.length(); ++i) {
            final char c = output.charAt(i);
            if ('\n' == c) {
                count += 1;
            }
        }
        assertEquals(6, count);
        assertTrue(output.indexOf("42") > 0);
        assertTrue(output.indexOf("43") > 0);
        assertTrue(output.indexOf("44") > 0);
        assertTrue(output.indexOf("45") > 0);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testReport2(final JexlBuilder builder) {
        init(builder);
        // @formatter:off
        final String rpt
                = "<report>\n"
                + "this is ${x}\n"
                + "${x + 1}\n"
                + "${x + 2}\n"
                + "${x + 3}\n"
                + "</report>\n";
        // @formatter:on
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt), "x");
        final StringWriter strw = new StringWriter();
        t.evaluate(context, strw, 42);
        final String output = strw.toString();
        int count = 0;
        for (int i = 0; i < output.length(); ++i) {
            final char c = output.charAt(i);
            if ('\n' == c) {
                count += 1;
            }
        }
        assertEquals(6, count);
        assertTrue(output.indexOf("42") > 0);
        assertTrue(output.indexOf("43") > 0);
        assertTrue(output.indexOf("44") > 0);
        assertTrue(output.indexOf("45") > 0);

        final TemplateDebugger td = new TemplateDebugger();
        final String xxx = refactor(td, t);
        assertNotNull(xxx);
        assertEquals(rpt, xxx);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testSanboxed311i(final JexlBuilder builder) {
        init(builder);
        /// this uberspect cannot access jexl3 classes (besides test)
        final Uberspect uberspect = new Uberspect(LogFactory.getLog(JXLTTest.class), null, NOJEXL3);
        final Method method = uberspect.getMethod(TemplateInterpreter.class, "print", new Object[]{Integer.TYPE});
        final JexlEngine jexl= new JexlBuilder().uberspect(uberspect).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlContext ctx311 = new Context311();
        // @formatter:off
        final String rpt
                = "$$var u = 'Universe'; exec('4').execute((a, b)->{"
                + "\n<p>${u} ${a}${b}</p>"
                + "\n$$}, '2')";
        // @formatter:on
        final JxltEngine.Template t = jxlt.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        assertEquals("<p>Universe 42</p>\n", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testSanboxedTemplate(final JexlBuilder builder) {
        init(builder);
        final String src = "Hello ${user}";
        final JexlContext ctxt = new MapContext();
        ctxt.set("user", "Francesco");
        /// this uberspect cannot access jexl3 classes (besides test)
        final Uberspect uberspect = new Uberspect(LogFactory.getLog(JXLTTest.class), null, NOJEXL3);
        final Method method = uberspect.getMethod(TemplateInterpreter.class, "print", new Object[]{Integer.TYPE});
        assertNull(method);
        // ensures JXLT sandboxed still executes
        final JexlEngine jexl= new JexlBuilder().uberspect(uberspect).create();
        final JxltEngine jxlt = jexl.createJxltEngine();

        final JxltEngine.Template tmplt = jxlt.createTemplate(src);
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        final String result = strw.toString();
        assertEquals("Hello Francesco", result);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testStatement(final JexlBuilder builder) {
        init(builder);
        final Froboz froboz = new Froboz(32);
        context.set("froboz", froboz);
        final JxltEngine.Expression check = JXLT.createExpression("${ froboz.plus10() }");
        final Object o = check.evaluate(context);
        assertEquals(Integer.valueOf(32), o);
        assertEquals(42, froboz.getValue());
        final Set<List<String>> evars = check.getVariables();
        assertEquals(1, evars.size());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testTemplate0(final JexlBuilder builder) {
        init(builder);
        final String source = "   $$ if(x) {\nx is ${x}\n   $$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        final JxltEngine.Template t = JXLT.createTemplate(source);

        context.set("x", 42);
        strw = new StringWriter();
        t.evaluate(context, strw);
        output = strw.toString();
        assertEquals("x is 42\n", output);

        strw = new StringWriter();
        context.set("x", "");
        t.evaluate(context, strw);
        output = strw.toString();
        assertEquals("no x\n", output);

        final String dstr = t.toString();
        assertNotNull(dstr);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testTemplate1(final JexlBuilder builder) {
        init(builder);
        final String source = "$$ if(x) {\nx is ${x}\n$$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), "x");
        final String dstr = t.asString();
        assertNotNull(dstr);

        strw = new StringWriter();
        t.evaluate(context, strw, 42);
        output = strw.toString();
        assertEquals("x is 42\n", output);

        strw = new StringWriter();
        t.evaluate(context, strw, "");
        output = strw.toString();
        assertEquals("no x\n", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testTemplate10(final JexlBuilder builder) {
        init(builder);
        final String source = "$$(x)->{ if(x) {\nx is ${x}\n$$ } else {\n${'no x'}\n$$ } }\n";
        StringWriter strw;
        String output;

        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), (String[]) null);
        final String dstr = t.asString();
        assertNotNull(dstr);

        final String[] ps = t.getParameters();
        assertTrue(Arrays.asList(ps).contains("x"));

        strw = new StringWriter();
        t.evaluate(context, strw, 42);
        output = strw.toString();
        assertEquals("x is 42\n", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testTemplate2(final JexlBuilder builder) {
        init(builder);
        final String source = "The answer: ${x}";
        StringWriter strw;
        String output;

        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), "x");
        final String dstr = t.asString();
        assertNotNull(dstr);

        strw = new StringWriter();
        t.evaluate(context, strw, 42);
        output = strw.toString();
        assertEquals("The answer: 42", output);
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testTemplateOutOfScope(final JexlBuilder builder) {
        init(builder);
        final JexlOptions opts = new JexlOptions();
        opts.setCancellable(false);
        opts.setStrict(false);
        opts.setLexical(false);
        opts.setLexicalShade(false);
        opts.setSharedInstance(true);
        final JexlContext ctxt = new PragmaticContext(opts);
        final String src = "$$if (false) { var tab = 42; }\n" + "${tab}";
        JxltEngine.Template tmplt;
        final JexlFeatures features = BUILDER.features();
        try {
            tmplt = JXLT.createTemplate("$$", new StringReader(src));
        } catch (final JexlException xparse) {
            if (features != null && features.isLexicalShade()) {
                return;
            }
            throw xparse;
        }
        final Writer strw = new StringWriter();
        opts.setSafe(true);
        assertDoesNotThrow(() -> tmplt.evaluate(ctxt, strw), "safe should prevent local shade");
        assertTrue(strw.toString().isEmpty());
        opts.setStrict(true);
        opts.setSafe(false);
        final JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> tmplt.evaluate(ctxt, strw));
        assertTrue("tab".equals(xvar.getVariable()));
        assertTrue(xvar.isUndefined());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testTemplatePragmaPro50(final JexlBuilder builder) {
        init(builder);
        final JexlOptions opts = new JexlOptions();
        opts.setCancellable(false);
        opts.setStrict(false);
        opts.setSafe(true);
        opts.setLexical(false);
        opts.setLexicalShade(false);
        opts.setSharedInstance(true);
        final JexlContext ctxt = new PragmaticContext(opts);
        // @formatter:off
        final String src = "$$ #pragma script.mode pro50\n"
                + "$$ var tab = null;\n"
                + "$$ tab.dummy();";
        // @formatter:on
        final JxltEngine.Template tmplt = JXLT.createTemplate("$$", new StringReader(src));
        final Writer strw = new StringWriter();
        final JexlException.Variable xvar = assertThrows(JexlException.Variable.class, () -> tmplt.evaluate(ctxt, strw));
        assertEquals("tab", xvar.getVariable());
        assertFalse(xvar.isUndefined());
    }

    @ParameterizedTest
    @MethodSource("engines")
    void testWriter(final JexlBuilder builder) {
        init(builder);
        final Froboz froboz = new Froboz(42);
        final Writer writer = new FrobozWriter(new StringWriter());
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("$$$jexl.print(froboz)"), "froboz");
        t.evaluate(context, writer, froboz);
        assertEquals("froboz{42}", writer.toString());
    }
}
