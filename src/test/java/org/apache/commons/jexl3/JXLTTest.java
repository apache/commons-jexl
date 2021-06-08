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

import org.apache.commons.jexl3.internal.Debugger;
import org.apache.commons.jexl3.internal.TemplateDebugger;
import org.apache.commons.jexl3.internal.TemplateInterpreter;
import org.apache.commons.jexl3.internal.TemplateScript;
import org.apache.commons.jexl3.internal.introspection.Permissions;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Test cases for the UnifiedEL.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
@RunWith(Parameterized.class)
public class JXLTTest extends JexlTestCase {
    private static final Log LOGGER = LogFactory.getLog(JxltEngine.class);
    private final MapContext vars = new MapContext();
    private JexlEvalContext context = null;
    private final JexlBuilder BUILDER;
    private final JexlEngine ENGINE;
    private final JxltEngine JXLT;

    public JXLTTest(final JexlBuilder builder) {
        super("JXLTTest");
        BUILDER = builder;
        ENGINE = BUILDER.create();
        JXLT = ENGINE.createJxltEngine();
    }


   @Parameterized.Parameters
   public static List<JexlBuilder> engines() {
       final JexlFeatures f = new JexlFeatures();
       f.lexical(true).lexicalShade(true);
      return Arrays.<JexlBuilder>asList(new JexlBuilder().silent(false)
        .lexical(true).lexicalShade(true)
        .cache(128).strict(true), new JexlBuilder().features(f).silent(false)
        .cache(128).strict(true), new JexlBuilder().silent(false)
        .cache(128).strict(true));
   }

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error
        java.util.logging.Logger.getLogger(org.apache.commons.jexl3.JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
        context = new JexlEvalContext(vars);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        debuggerCheck(ENGINE);
        super.tearDown();
    }

    private boolean isLexicalShade() {
        JexlOptions options = context.getEngineOptions();
        if (options.isLexicalShade()) {
            return true;
        }
        options = new JexlOptions().set(ENGINE);
        return options.isLexicalShade();
    }

    private static String refactor(final TemplateDebugger td, final JxltEngine.Template ts) {
        final boolean dbg = td.debug(ts);
        if (dbg) {
            return td.toString();
        }
        return "";
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

    public static class Froboz {
        int value;

        public Froboz(final int v) {
            value = v;
        }

        public void setValue(final int v) {
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
    }

    @Test
    public void testStatement() throws Exception {
        final Froboz froboz = new Froboz(32);
        context.set("froboz", froboz);
        final JxltEngine.Expression check = JXLT.createExpression("${ froboz.plus10() }");
        final Object o = check.evaluate(context);
        Assert.assertEquals("Result is not 32", new Integer(32), o);
        Assert.assertEquals("Result is not 42", 42, froboz.getValue());
        final Set<List<String>> evars = check.getVariables();
        Assert.assertEquals(1, evars.size());
    }

    @Test
    public void testAssign() throws Exception {
        final Froboz froboz = new Froboz(32);
        context.set("froboz", froboz);
        final JxltEngine.Expression assign = JXLT.createExpression("${froboz.value = 42}");
        final JxltEngine.Expression check = JXLT.createExpression("${froboz.value}");
        Object o = assign.evaluate(context);
        Assert.assertEquals("Result is not 10", new Integer(42), o);
        o = check.evaluate(context);
        Assert.assertEquals("Result is not 10", new Integer(42), o);
    }

    @Test
    public void testComposite() throws Exception {
        final String source = "Dear ${p} ${name};";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        context.set("p", "Mr");
        context.set("name", "Doe");
        Assert.assertTrue("expression should be immediate", expr.isImmediate());
        Object o = expr.evaluate(context);
        Assert.assertEquals("Dear Mr Doe;", o);
        context.set("p", "Ms");
        context.set("name", "Jones");
        o = expr.evaluate(context);
        Assert.assertEquals("Dear Ms Jones;", o);
        Assert.assertEquals(source, getSource(expr.toString()));
    }

    boolean contains(final Set<List<String>> set, final List<String> list) {
        for (final List<String> sl : set) {
            if (sl.equals(list)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testPrepareEvaluate() throws Exception {
        final String source = "Dear #{p} ${name};";
        final JxltEngine.Expression expr = JXLT.createExpression("Dear #{p} ${name};");
        Assert.assertTrue("expression should be deferred", expr.isDeferred());

        final Set<List<String>> evars = expr.getVariables();
        Assert.assertEquals(1, evars.size());
        Assert.assertTrue(contains(evars, Collections.singletonList("name")));
        context.set("name", "Doe");
        final JxltEngine.Expression phase1 = expr.prepare(context);
        final String as = phase1.asString();
        Assert.assertEquals("Dear ${p} Doe;", as);
        final Set<List<String>> evars1 = phase1.getVariables();
        Assert.assertEquals(1, evars1.size());
        Assert.assertTrue(contains(evars1, Collections.singletonList("p")));
        vars.clear();
        context.set("p", "Mr");
        context.set("name", "Should not be used in 2nd phase");
        final Object o = phase1.evaluate(context);
        Assert.assertEquals("Dear Mr Doe;", o);

        final String p1 = getSource(phase1.toString());
        Assert.assertEquals(source, getSource(phase1.toString()));
        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testNested() throws Exception {
        final String source = "#{${hi}+'.world'}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);

        final Set<List<String>> evars = expr.getVariables();
        Assert.assertEquals(1, evars.size());
        Assert.assertTrue(contains(evars, Collections.singletonList("hi")));

        context.set("hi", "greeting");
        context.set("greeting.world", "Hello World!");
        Assert.assertTrue("expression should be deferred", expr.isDeferred());
        final Object o = expr.evaluate(context);
        Assert.assertEquals("Hello World!", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testNestedTemplate() throws Exception {
        final String source = "#{${hi}+'.world'}";
        final JxltEngine.Template expr = JXLT.createTemplate(source, "hi");

        context.set("greeting.world", "Hello World!");
        final StringWriter strw = new StringWriter();
        expr.evaluate(context, strw, "greeting");
        final String o = strw.toString();
        Assert.assertEquals("Hello World!", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testImmediate() throws Exception {
        final JexlContext none = null;
        final String source = "${'Hello ' + 'World!'}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        final JxltEngine.Expression prepared = expr.prepare(none);
        Assert.assertEquals("prepare should return same expression", "Hello World!", prepared.asString());
        final Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be immediate", expr.isImmediate());
        Assert.assertEquals("Hello World!", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testConstant() throws Exception {
        final JexlContext none = null;
        final String source = "Hello World!";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        Assert.assertSame("prepare should return same expression", expr.prepare(none), expr);
        final Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be immediate", expr.isImmediate());
        Assert.assertEquals("Hello World!", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testConstant2() throws Exception {
        final JexlContext none = null;
        final String source = "${size({'map':123,'map2':456})}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        //Assert.assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        final Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be immediate", expr.isImmediate());
        Assert.assertEquals(2, o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testConstant3() throws Exception {
        final JexlContext none = null;
        final String source = "#{size({'map':123,'map2':456})}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        //Assert.assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        final Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be deferred", expr.isDeferred());
        Assert.assertEquals(2, o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testConstant4() throws Exception {
        final JexlContext none = null;
        final String source = "#{ ${size({'1':2,'2': 3})} }";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        //Assert.assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        final Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be deferred", expr.isDeferred());
        Assert.assertEquals(2, o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testDeferred() throws Exception {
        final JexlContext none = null;
        final String source = "#{'world'}";
        final JxltEngine.Expression expr = JXLT.createExpression(source);
        Assert.assertTrue("expression should be deferred", expr.isDeferred());
        final String as = expr.prepare(none).asString();
        Assert.assertEquals("prepare should return immediate version", "${'world'}", as);
        final Object o = expr.evaluate(none);
        Assert.assertEquals("world", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testEscape() throws Exception {
        final JexlContext none = null;
        JxltEngine.Expression expr;
        Object o;
        // $ and # are escapable in TemplateEngine
        expr = JXLT.createExpression("\\#{'world'}");
        o = expr.evaluate(none);
        Assert.assertEquals("#{'world'}", o);
        expr = JXLT.createExpression("\\${'world'}");
        o = expr.evaluate(none);
        Assert.assertEquals("${'world'}", o);
    }

    @Test
    public void testEscapeString() throws Exception {
        final JxltEngine.Expression expr = JXLT.createExpression("\\\"${'world\\'s finest'}\\\"");
        final JexlContext none = null;
        final Object o = expr.evaluate(none);
        Assert.assertEquals("\"world's finest\"", o);
    }

    @Test
    public void testNonEscapeString() throws Exception {
        final JxltEngine.Expression expr = JXLT.createExpression("c:\\some\\windows\\path");
        final JexlContext none = null;
        final Object o = expr.evaluate(none);
        Assert.assertEquals("c:\\some\\windows\\path", o);
    }

    @Test
    public void testMalformed() throws Exception {
        try {
            final JxltEngine.Expression expr = JXLT.createExpression("${'world'");
            final JexlContext none = null;
            expr.evaluate(none);
            Assert.fail("should be malformed");
        } catch (final JxltEngine.Exception xjexl) {
            // expected
            final String xmsg = xjexl.getMessage();
            LOGGER.debug(xmsg);
        }
    }

    @Test
    public void testMalformedNested() throws Exception {
        try {
            final JxltEngine.Expression expr = JXLT.createExpression("#{${hi} world}");
            final JexlContext none = null;
            expr.evaluate(none);
            Assert.fail("should be malformed");
        } catch (final JxltEngine.Exception xjexl) {
            // expected
            final String xmsg = xjexl.getMessage();
            LOGGER.debug(xmsg);
        }
    }

    @Test
    public void testMalformedNested2() throws Exception {
        try {
            final JxltEngine.Expression expr = JXLT.createExpression("#{${hi} world}");
            final JexlContext ctxt = new MapContext();
            ctxt.set("hi", "hello");
            expr.evaluate(ctxt);
            Assert.fail("should be malformed");
        } catch (final JxltEngine.Exception xjexl) {
            // expected
            final String xmsg = xjexl.getMessage();
            LOGGER.debug(xmsg);
        }
    }

    @Test
    public void testBadContextNested() throws Exception {
        try {
            final JxltEngine.Expression expr = JXLT.createExpression("#{${hi}+'.world'}");
            final JexlContext none = null;
            expr.evaluate(none);
            Assert.fail("should be malformed");
        } catch (final JxltEngine.Exception xjexl) {
            // expected
            final String xmsg = xjexl.getMessage();
            LOGGER.debug(xmsg);
        }
    }

    @Test
    public void testCharAtBug() throws Exception {
        context.set("foo", "abcdef");
        final JexlOptions options = context.getEngineOptions();
        JxltEngine.Expression expr = JXLT.createExpression("${foo.substring(2,4)/*comment*/}");
        Object o = expr.evaluate(context);
        Assert.assertEquals("cd", o);

        context.set("bar", "foo");
        try {
            options.setSilent(true);
            expr = JXLT.createExpression("#{${bar}+'.charAt(-2)'}");
            expr = expr.prepare(context);
            o = expr.evaluate(context);
            Assert.assertNull(o);
        } finally {
            options.setSilent(false);
        }

    }

    @Test
    public void testTemplate0() throws Exception {
        final String source = "   $$ if(x) {\nx is ${x}\n   $$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        final JxltEngine.Template t = JXLT.createTemplate(source);

        context.set("x", 42);
        strw = new StringWriter();
        t.evaluate(context, strw);
        output = strw.toString();
        Assert.assertEquals("x is 42\n", output);

        strw = new StringWriter();
        context.set("x", "");
        t.evaluate(context, strw);
        output = strw.toString();
        Assert.assertEquals("no x\n", output);

        final String dstr = t.toString();
        Assert.assertNotNull(dstr);
    }

    @Test
    public void testTemplate10() throws Exception {
        final String source = "$$(x)->{ if(x) {\nx is ${x}\n$$ } else {\n${'no x'}\n$$ } }\n";
        StringWriter strw;
        String output;

        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), (String[]) null);
        final String dstr = t.asString();
        Assert.assertNotNull(dstr);

        final String[] ps = t.getParameters();
        Assert.assertTrue(Arrays.asList(ps).contains("x"));

        strw = new StringWriter();
        t.evaluate(context, strw, 42);
        output = strw.toString();
        Assert.assertEquals("x is 42\n", output);
    }

    @Test
    public void testTemplate1() throws Exception {
        final String source = "$$ if(x) {\nx is ${x}\n$$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), "x");
        final String dstr = t.asString();
        Assert.assertNotNull(dstr);

        strw = new StringWriter();
        t.evaluate(context, strw, 42);
        output = strw.toString();
        Assert.assertEquals("x is 42\n", output);

        strw = new StringWriter();
        t.evaluate(context, strw, "");
        output = strw.toString();
        Assert.assertEquals("no x\n", output);
    }

    @Test
    public void testTemplate2() throws Exception {
        final String source = "The answer: ${x}";
        StringWriter strw;
        String output;

        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), "x");
        final String dstr = t.asString();
        Assert.assertNotNull(dstr);

        strw = new StringWriter();
        t.evaluate(context, strw, 42);
        output = strw.toString();
        Assert.assertEquals("The answer: 42", output);
    }

    @Test
    public void testPrepareTemplate() throws Exception {
        final String source
                = "$$ for(var x : list) {\n"
                + "${l10n}=#{x}\n"
                + "$$ }\n";
        final int[] args = {42};
        final JxltEngine.Template tl10n = JXLT.createTemplate(source, "list");
        final String dstr = tl10n.asString();
        Assert.assertNotNull(dstr);
        final Set<List<String>> vars = tl10n.getVariables();
        Assert.assertFalse(vars.isEmpty());
        context.set("l10n", "valeur");
        final JxltEngine.Template tpFR = tl10n.prepare(context);
        context.set("l10n", "value");
        final JxltEngine.Template tpEN = tl10n.prepare(context);
        context.set("l10n", null);

        StringWriter strw;
        strw = new StringWriter();
        tpFR.evaluate(context, strw, args);
        final String outFR = strw.toString();
        Assert.assertEquals("valeur=42\n", outFR);

        context.set("l10n", null);
        strw = new StringWriter();
        tpEN.evaluate(context, strw, args);
        final String outEN = strw.toString();
        Assert.assertEquals("value=42\n", outEN);
    }

    @Test
    public void test42() throws Exception {
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
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(test42), "list");
        final StringWriter strw = new StringWriter();
        final int[] list = {1, 3, 5, 42, 169};
        t.evaluate(context, strw, list);
        final String output = strw.toString();
        final String out42
                = "The value 1 is under fourty-two\n"
                + "The value 3 is under fourty-two\n"
                + "The value 5 is under fourty-two\n"
                + "Life, the universe, and everything\n"
                + "The value 169 is over fourty-two\n";
        Assert.assertEquals(out42, output);

        final String dstr = t.asString();
        Assert.assertNotNull(dstr);

        final TemplateDebugger td = new TemplateDebugger();
        final String refactored = refactor(td, t);
        Assert.assertNotNull(refactored);
        Assert.assertEquals(test42, refactored);
    }

    @Test
    public void testInheritedDebugger() throws Exception {
        final String src = "if ($A) { $B + 1; } else { $C - 2 }";
        final JexlEngine jexl = JXLT.getEngine();
        final JexlScript script = jexl.createScript(src);

        final Debugger sd = new Debugger();
        final String rscript = sd.debug(script)? sd.toString() : null;
        Assert.assertNotNull(rscript);

        final TemplateDebugger td = new TemplateDebugger();
        final String refactored = td.debug(script)? td.toString() : null;
        Assert.assertNotNull(refactored);
        Assert.assertEquals(refactored, rscript);
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

    @Test
    public void testWriter() throws Exception {
        final Froboz froboz = new Froboz(42);
        final Writer writer = new FrobozWriter(new StringWriter());
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("$$$jexl.print(froboz)"), "froboz");
        t.evaluate(context, writer, froboz);
        Assert.assertEquals("froboz{42}", writer.toString());
    }

    @Test
    public void testReport() throws Exception {
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
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(context, strw);
        final String output = strw.toString();
        final String ctl = "<report>\n\n\n\n\n        11\n</report>\n";
        Assert.assertEquals(ctl, output);

        final TemplateDebugger td = new TemplateDebugger();
        final String refactored = refactor(td, t);
        Assert.assertNotNull(refactored);
        Assert.assertEquals(rpt, refactored);
    }

    @Test
    public void testReport1() throws Exception {
        final String rpt
                = "<report>\n"
                + "this is ${x}\n"
                + "${x + 1}\n"
                + "${x + 2}\n"
                + "${x + 3}\n"
                + "</report>\n";
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
        Assert.assertEquals(6, count);
        Assert.assertTrue(output.indexOf("42") > 0);
        Assert.assertTrue(output.indexOf("43") > 0);
        Assert.assertTrue(output.indexOf("44") > 0);
        Assert.assertTrue(output.indexOf("45") > 0);
    }

    @Test
    public void testReport2() throws Exception {
        final String rpt
                = "<report>\n"
                + "this is ${x}\n"
                + "${x + 1}\n"
                + "${x + 2}\n"
                + "${x + 3}\n"
                + "</report>\n";
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
        Assert.assertEquals(6, count);
        Assert.assertTrue(output.indexOf("42") > 0);
        Assert.assertTrue(output.indexOf("43") > 0);
        Assert.assertTrue(output.indexOf("44") > 0);
        Assert.assertTrue(output.indexOf("45") > 0);

        final TemplateDebugger td = new TemplateDebugger();
        final String xxx = refactor(td, t);
        Assert.assertNotNull(xxx);
        Assert.assertEquals(rpt, xxx);
    }
    @Test
    public void testOneLiner() throws Exception {
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("fourty-two"));
        final StringWriter strw = new StringWriter();
        t.evaluate(context, strw);
        final String output = strw.toString();
        Assert.assertEquals("fourty-two", output);
    }

    @Test
    public void testOneLinerVar() throws Exception {
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("fourty-${x}"));
        final StringWriter strw = new StringWriter();
        context.set("x", "two");
        t.evaluate(context, strw);
        final String output = strw.toString();
        Assert.assertEquals("fourty-two", output);
    }

    @Test
    public void testInterpolation() throws Exception {
        final String expr =  "`Hello \n${user}`";
        final JexlScript script = ENGINE.createScript(expr);
        context.set("user", "Dimitri");
        Object value = script.execute(context);
        Assert.assertEquals(expr, "Hello \nDimitri", value);
        context.set("user", "Rahul");
        value = script.execute(context);
        Assert.assertEquals(expr, "Hello \nRahul", value);
    }

    @Test
    public void testInterpolationGlobal() throws Exception {
        if (isLexicalShade()) {
            context.set("user", null);
        }
        final String expr =  "user='Dimitri'; `Hello \n${user}`";
        final Object value = ENGINE.createScript(expr).execute(context);
        Assert.assertEquals(expr, "Hello \nDimitri", value);
    }

    @Test
    public void testInterpolationLocal() throws Exception {
        final String expr =  "var user='Henrib'; `Hello \n${user}`";
        final Object value = ENGINE.createScript(expr).execute(context);
        Assert.assertEquals(expr, "Hello \nHenrib", value);
    }

    @Test
    public void testInterpolationLvsG() throws Exception {
        if (isLexicalShade()) {
            context.set("user", null);
        }
        final String expr =  "user='Dimitri'; var user='Henrib'; `H\\\"ello \n${user}`";
        final Object value = ENGINE.createScript(expr).execute(context);
        Assert.assertEquals(expr, "H\"ello \nHenrib", value);
    }

    @Test
    public void testInterpolationLvsG2() throws Exception {
        if (isLexicalShade()) {
            context.set("user", null);
        }
        final String expr =  "user='Dimitri'; var user='Henrib'; `H\\`ello \n${user}`";
        final Object value = ENGINE.createScript(expr).execute(context);
        Assert.assertEquals(expr, "H`ello \nHenrib", value);
    }

    @Test
    public void testInterpolationParameter() throws Exception {
        final String expr =  "(user)->{`Hello \n${user}`}";
        Object value = ENGINE.createScript(expr).execute(context, "Henrib");
        Assert.assertEquals(expr, "Hello \nHenrib", value);
        value = ENGINE.createScript(expr).execute(context, "Dimitri");
        Assert.assertEquals(expr, "Hello \nDimitri", value);
    }

    @Test
    public void testImmediateTemplate() throws Exception {
        context.set("tables", new String[]{"table1", "table2"});
        context.set("w" ,"x=1");
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(
             "select * from \n"+
             "$$var comma = false; \n"+
             "$$for(var c : tables) { \n"+
             "$$  if (comma) $jexl.write(','); else comma = true;\n"+
             "${c}"+
             "\n$$}\n"+
             "where ${w}\n"
        ));
        final StringWriter strw = new StringWriter();
        //vars.clear();
        t.evaluate(context, strw);
        final String output = strw.toString();
        Assert.assertTrue(output.contains("table1") && output.contains("table2"));
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

    public static class Context311 extends MapContext
      implements JexlContext.OptionsHandle, JexlContext.ThreadLocal {
        private JexlOptions options = null;

        public void setOptions(final JexlOptions o) {
            options = o;
        }

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
    }

    @Test
    public void test311a() throws Exception {
        final JexlContext ctx = null;
        final String rpt
                = "$$((a)->{\n"
                + "<p>Universe ${a}</p>\n"
                + "$$})(42)";
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx, strw);
        final String output = strw.toString();
        Assert.assertEquals("<p>Universe 42</p>\n", output);
    }

    @Test
    public void test311b() throws Exception {
        final JexlContext ctx311 = new Context311();
        final String rpt
                = "$$ exec('42').execute(()->{\n"
                + "<p>Universe 42</p>\n"
                + "$$})";
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        Assert.assertEquals("<p>Universe 42</p>\n", output);
    }

    @Test
    public void test311c() throws Exception {
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        final String rpt
                = "$$ exec('42').execute((a)->{"
                + "\n<p>Universe ${a}</p>"
                + "\n$$})";
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        Assert.assertEquals("<p>Universe 42</p>\n", output);
    }

    @Test
    public void test311d() throws Exception {
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        final String rpt
                = "$$ exec('4').execute((a, b)->{"
                + "\n<p>Universe ${a}${b}</p>"
                + "\n$$}, '2')";
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        Assert.assertEquals("<p>Universe 42</p>\n", output);
    }

    @Test
    public void test311e() throws Exception {
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        final String rpt
                = "exec('4').execute((a, b)->{"
                + " '<p>Universe ' + a + b + '</p>'"
                + "}, '2')";
        final JexlScript script = JEXL.createScript(rpt);
        final String output = script.execute(ctx311, 42).toString();
        Assert.assertEquals("<p>Universe 42</p>", output);
    }

    @Test
    public void test311f() throws Exception {
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        final String rpt
                = "exec('4').execute((a, b)->{"
                + " `<p>Universe ${a}${b}</p>`"
                + "}, '2')";
        final JexlScript script = JEXL.createScript(rpt);
        final String output = script.execute(ctx311, 42).toString();
        Assert.assertEquals("<p>Universe 42</p>", output);
    }

    @Test
    public void test311g() throws Exception {
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        final String rpt
                = "(a, b)->{"
                + " `<p>Universe ${a}${b}</p>`"
                + "}";
        final JexlScript script = JEXL.createScript(rpt);
        final String output = script.execute(ctx311, "4", "2").toString();
        Assert.assertEquals("<p>Universe 42</p>", output);
    }

    @Test
    public void test311h() throws Exception {
        final Context311 ctx311 = new Context311();
        ctx311.newOptions().setLexical(true);
        final String rpt= " `<p>Universe ${a}${b}</p>`";
        final JexlScript script = JEXL.createScript(rpt, "a", "b");
        final String output = script.execute(ctx311, "4", "2").toString();
        Assert.assertEquals("<p>Universe 42</p>", output);
    }

    @Test
    public void test311i() throws Exception {
        final JexlContext ctx311 = new Context311();
        final String rpt
                = "$$var u = 'Universe'; exec('4').execute((a, b)->{"
                + "\n<p>${u} ${a}${b}</p>"
                + "\n$$}, '2')";
        final JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        Assert.assertEquals("<p>Universe 42</p>\n", output);
    }

    @Test
    public void test315() throws Exception {
        String s315;
        StringWriter strw;
        JxltEngine.Template t315;
        String output;

        s315 = "<report/>$";
        t315 = JXLT.createTemplate("$$", new StringReader(s315));
        strw = new StringWriter();
        t315.evaluate(context, strw);
        output = strw.toString();
        Assert.assertEquals(s315, output);

        s315 = "<foo/>#";
        t315 = JXLT.createTemplate("$$", new StringReader(s315));
         strw = new StringWriter();
        t315.evaluate(context, strw);
        output = strw.toString();
        Assert.assertEquals(s315, output);

        s315 = "<bar/>\\";
        t315 = JXLT.createTemplate("$$", new StringReader(s315));
        strw = new StringWriter();
        t315.evaluate(context, strw);
        output = strw.toString();
        Assert.assertEquals(s315, output);
    }

        // define mode pro50
    static final JexlOptions MODE_PRO50 = new JexlOptions();
    static {
        MODE_PRO50.setFlags( "+strict +cancellable +lexical +lexicalShade -safe".split(" "));
    }

    public static class PragmaticContext extends MapContext implements JexlContext.PragmaProcessor, JexlContext.OptionsHandle {
        private final JexlOptions options;

        public PragmaticContext(final JexlOptions o) {
            this.options = o;
        }

        @Override
        public void processPragma(final String key, final Object value) {
            if ("script.mode".equals(key) && "pro50".equals(value)) {
                options.set(MODE_PRO50);
            }
        }

        @Override
        public Object get(final String name) {
            if ("$options".equals(name)) {
                return options;
            }
            return super.get(name);
        }

        @Override
        public JexlOptions getEngineOptions() {
            return options;
        }
    }

    @Test
    public void testLexicalTemplate() throws Exception {
        final JexlOptions opts = new JexlOptions();
        final JexlContext ctxt = new PragmaticContext(opts);
        opts.setCancellable(false);
        opts.setStrict(false);
        opts.setSafe(true);
        opts.setLexical(false);
        opts.setLexicalShade(false);
        final String src0 = "${$options.strict?'+':'-'}strict"
                + " ${$options.cancellable?'+':'-'}cancellable"
                + " ${$options.lexical?'+':'-'}lexical"
                + " ${$options.lexicalShade?'+':'-'}lexicalShade"
                + " ${$options.safe?'+':'-'}safe";

        final JxltEngine.Template tmplt0 = JXLT.createTemplate("$$", new StringReader(src0));
        final Writer strw0 = new StringWriter();
        tmplt0.evaluate(ctxt, strw0);
        final String output0 = strw0.toString();
        Assert.assertEquals( "-strict -cancellable -lexical -lexicalShade +safe", output0);

        final String src = "$$ #pragma script.mode pro50\n" + src0;

        final JxltEngine.Template tmplt = JXLT.createTemplate("$$", new StringReader(src));
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        final String output = strw.toString();
        Assert.assertEquals("+strict +cancellable +lexical +lexicalShade -safe", output);
    }

    @Test
    public void testTemplatePragmaPro50() throws Exception {
        final JexlOptions opts = new JexlOptions();
        opts.setCancellable(false);
        opts.setStrict(false);
        opts.setSafe(true);
        opts.setLexical(false);
        opts.setLexicalShade(false);
        opts.setSharedInstance(true);
        final JexlContext ctxt = new PragmaticContext(opts);
        final String src = "$$ #pragma script.mode pro50\n"
                + "$$ var tab = null;\n"
                + "$$ tab.dummy();";
        final JxltEngine.Template tmplt = JXLT.createTemplate("$$", new StringReader(src));
        final Writer strw = new StringWriter();
        try {
            tmplt.evaluate(ctxt, strw);
            Assert.fail("tab var is null");
        } catch (final JexlException.Variable xvar) {
            Assert.assertEquals("tab", xvar.getVariable());
            Assert.assertFalse(xvar.isUndefined());
        }
    }

    @Test
    public void testTemplateOutOfScope() throws Exception {
        final JexlOptions opts = new JexlOptions();
        opts.setCancellable(false);
        opts.setStrict(false);
        opts.setLexical(false);
        opts.setLexicalShade(false);
        opts.setSharedInstance(true);
        final JexlContext ctxt = new PragmaticContext(opts);
        final String src = "$$if (false) { var tab = 42; }\n"
                + "${tab}";
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
        try {
            tmplt.evaluate(ctxt, strw);
            Assert.assertTrue(strw.toString().isEmpty());
        } catch (final JexlException.Variable xvar) {
            Assert.fail("safe should prevent local shade");
        }
        opts.setStrict(true);
        opts.setSafe(false);
        try {
            tmplt.evaluate(ctxt, strw);
            Assert.fail("tab var is undefined");
        } catch (final JexlException.Variable xvar) {
            Assert.assertTrue("tab".equals(xvar.getVariable()));
            Assert.assertTrue(xvar.isUndefined());
        } catch (final JexlException xany) {
            Assert.assertTrue(xany.getMessage().contains("tab"));
        }
    }

    @Test
    public void testCommentedTemplate0() throws Exception {
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        JxltEngine.Template tmplt;
        final String src = "$$/*\n"
                + "Hello\n"
                + "$$*/";
        tmplt = jxlt.createTemplate(src);
        Assert.assertNotNull(tmplt);
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        Assert.assertTrue(strw.toString().isEmpty());
    }

    @Test
    public void testCommentedTemplate1() throws Exception {
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        JxltEngine.Template tmplt;
        final String src = "$$/*\n"
                + "one\n"
                + "$$*/\n"
                + "42\n"
                + "$$/*\n"
                + "three\n"
                + "$$*/\n";
        tmplt = jxlt.createTemplate(src);
        Assert.assertNotNull(tmplt);
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        Assert.assertEquals("42\n", strw.toString());
    }

    @Test
    public void testConstantTemplate() {
        String src = "<script>\n" +
                "      function test(src){\n" +
                "        var res = src.replace(/\\n\\t\\s/g, '\\n');\n" +
                "      }\n" +
                "      test();\n" +
                "    </script>";
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        JxltEngine.Template tmplt;
        tmplt = jxlt.createTemplate(src);
        Assert.assertNotNull(tmplt);
        final Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        String result = strw.toString();
        Assert.assertEquals(src, result);
    }

    private static final Permissions NOJEXL3 = new Permissions() {
        @Override public boolean allow(Class<?> clazz) {
            String cname = clazz.getName();
            return !cname.contains("jexl3") || cname.contains("311");
        }
    };

    @Test
    public void testSanboxedTemplate() throws Exception {
        final String src = "Hello ${user}";
        final JexlContext ctxt = new MapContext();
        ctxt.set("user", "Francesco");
        /// this uberspect can not access jexl3 classes (besides test)
        Uberspect uberspect = new Uberspect(LogFactory.getLog(JXLTTest.class), null, NOJEXL3);
        Method method = uberspect.getMethod(TemplateInterpreter.class, "print", new Object[]{Integer.TYPE});
        Assert.assertNull(method);
        // ensures JXLT sandboxed still executes
        final JexlEngine jexl= new JexlBuilder().uberspect(uberspect).create();
        final JxltEngine jxlt = jexl.createJxltEngine();

        JxltEngine.Template tmplt = jxlt.createTemplate(src);
        Writer strw = new StringWriter();
        tmplt.evaluate(ctxt, strw);
        String result = strw.toString();
        Assert.assertEquals("Hello Francesco", result);
    }

    @Test
    public void testSanboxed311i() throws Exception {
        /// this uberspect can not access jexl3 classes (besides test)
        Uberspect uberspect = new Uberspect(LogFactory.getLog(JXLTTest.class), null, NOJEXL3);
        Method method = uberspect.getMethod(TemplateInterpreter.class, "print", new Object[]{Integer.TYPE});
        final JexlEngine jexl= new JexlBuilder().uberspect(uberspect).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JexlContext ctx311 = new Context311();
        final String rpt
                = "$$var u = 'Universe'; exec('4').execute((a, b)->{"
                + "\n<p>${u} ${a}${b}</p>"
                + "\n$$}, '2')";
        final JxltEngine.Template t = jxlt.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctx311, strw, 42);
        final String output = strw.toString();
        Assert.assertEquals("<p>Universe 42</p>\n", output);
    }
}
