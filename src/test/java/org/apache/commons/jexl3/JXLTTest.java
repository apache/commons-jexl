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

import org.apache.commons.jexl3.internal.TemplateDebugger;
import org.apache.commons.jexl3.internal.TemplateScript;
import org.apache.commons.jexl3.internal.Debugger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the UnifiedEL.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class JXLTTest extends JexlTestCase {
    private static final JexlEngine ENGINE = new JexlBuilder().silent(false).cache(128).strict(true).create();
    private static final JxltEngine JXLT = ENGINE.createJxltEngine();
    private static final Log LOG = LogFactory.getLog(JxltEngine.class);
    private final MapContext vars = new MapContext();
    private JexlEvalContext context = null;

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

    private static String refactor(TemplateDebugger td, JxltEngine.Template ts) {
        boolean dbg = td.debug((TemplateScript)ts);
        if (dbg) {
            return td.toString();
        } else {
            return "";
        }
    }

    /** Extract the source from a toString-ed expression. */
    private String getSource(String tostring) {
        int len = tostring.length();
        int sc = tostring.lastIndexOf(" /*= ");
        if (sc >= 0) {
            sc += " /*= ".length();
        }
        int ec = tostring.lastIndexOf(" */");
        if (sc >= 0 && ec >= 0 && ec > sc && ec < len) {
            return tostring.substring(sc, ec);
        } else {
            return tostring;
        }

    }

    public static class Froboz {
        int value;

        public Froboz(int v) {
            value = v;
        }

        public void setValue(int v) {
            value = v;
        }

        public int getValue() {
            return value;
        }

        public int plus10() {
            int i = value;
            value += 10;
            return i;
        }
    }

    public JXLTTest() {
        super("JXLTTest");
    }

    @Test
    public void testStatement() throws Exception {
        Froboz froboz = new Froboz(32);
        context.set("froboz", froboz);
        JxltEngine.Expression check = JXLT.createExpression("${ froboz.plus10() }");
        Object o = check.evaluate(context);
        Assert.assertEquals("Result is not 32", new Integer(32), o);
        Assert.assertEquals("Result is not 42", 42, froboz.getValue());
        Set<List<String>> evars = check.getVariables();
        Assert.assertEquals(1, evars.size());
    }

    @Test
    public void testAssign() throws Exception {
        JxltEngine.Expression assign = JXLT.createExpression("${froboz.value = 10}");
        JxltEngine.Expression check = JXLT.createExpression("${froboz.value}");
        Object o = assign.evaluate(context);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(context);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
    }

    @Test
    public void testComposite() throws Exception {
        String source = "Dear ${p} ${name};";
        JxltEngine.Expression expr = JXLT.createExpression(source);
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

    boolean contains(Set<List<String>> set, List<String> list) {
        for (List<String> sl : set) {
            if (sl.equals(list)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testPrepareEvaluate() throws Exception {
        final String source = "Dear #{p} ${name};";
        JxltEngine.Expression expr = JXLT.createExpression("Dear #{p} ${name};");
        Assert.assertTrue("expression should be deferred", expr.isDeferred());

        Set<List<String>> evars = expr.getVariables();
        Assert.assertEquals(1, evars.size());
        Assert.assertTrue(contains(evars, Arrays.asList("name")));
        context.set("name", "Doe");
        JxltEngine.Expression phase1 = expr.prepare(context);
        String as = phase1.asString();
        Assert.assertEquals("Dear ${p} Doe;", as);
        Set<List<String>> evars1 = phase1.getVariables();
        Assert.assertEquals(1, evars1.size());
        Assert.assertTrue(contains(evars1, Arrays.asList("p")));
        vars.clear();
        context.set("p", "Mr");
        context.set("name", "Should not be used in 2nd phase");
        Object o = phase1.evaluate(context);
        Assert.assertEquals("Dear Mr Doe;", o);

        String p1 = getSource(phase1.toString());
        Assert.assertEquals(source, getSource(phase1.toString()));
        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testNested() throws Exception {
        final String source = "#{${hi}+'.world'}";
        JxltEngine.Expression expr = JXLT.createExpression(source);

        Set<List<String>> evars = expr.getVariables();
        Assert.assertEquals(1, evars.size());
        Assert.assertTrue(contains(evars, Arrays.asList("hi")));

        context.set("hi", "greeting");
        context.set("greeting.world", "Hello World!");
        Assert.assertTrue("expression should be deferred", expr.isDeferred());
        Object o = expr.evaluate(context);
        Assert.assertEquals("Hello World!", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testNestedTemplate() throws Exception {
        final String source = "#{${hi}+'.world'}";
        JxltEngine.Template expr = JXLT.createTemplate(source, "hi");

        context.set("greeting.world", "Hello World!");
        StringWriter strw = new StringWriter();
        expr.evaluate(context, strw, "greeting");
        String o = strw.toString();
        Assert.assertEquals("Hello World!", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testImmediate() throws Exception {
        JexlContext none = null;
        final String source = "${'Hello ' + 'World!'}";
        JxltEngine.Expression expr = JXLT.createExpression(source);
        JxltEngine.Expression prepared = expr.prepare(none);
        Assert.assertEquals("prepare should return same expression", "Hello World!", prepared.asString());
        Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be immediate", expr.isImmediate());
        Assert.assertEquals("Hello World!", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testConstant() throws Exception {
        JexlContext none = null;
        final String source = "Hello World!";
        JxltEngine.Expression expr = JXLT.createExpression(source);
        Assert.assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be immediate", expr.isImmediate());
        Assert.assertEquals("Hello World!", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testConstant2() throws Exception {
        JexlContext none = null;
        final String source = "${size({'map':123,'map2':456})}";
        JxltEngine.Expression expr = JXLT.createExpression(source);
        //Assert.assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be immediate", expr.isImmediate());
        Assert.assertEquals(2, o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testConstant3() throws Exception {
        JexlContext none = null;
        final String source = "#{size({'map':123,'map2':456})}";
        JxltEngine.Expression expr = JXLT.createExpression(source);
        //Assert.assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be deferred", expr.isDeferred());
        Assert.assertEquals(2, o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testConstant4() throws Exception {
        JexlContext none = null;
        final String source = "#{ ${size({'1':2,'2': 3})} }";
        JxltEngine.Expression expr = JXLT.createExpression(source);
        //Assert.assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        Object o = expr.evaluate(none);
        Assert.assertTrue("expression should be deferred", expr.isDeferred());
        Assert.assertEquals(2, o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testDeferred() throws Exception {
        JexlContext none = null;
        final String source = "#{'world'}";
        JxltEngine.Expression expr = JXLT.createExpression(source);
        Assert.assertTrue("expression should be deferred", expr.isDeferred());
        String as = expr.prepare(none).asString();
        Assert.assertEquals("prepare should return immediate version", "${'world'}", as);
        Object o = expr.evaluate(none);
        Assert.assertEquals("world", o);

        Assert.assertEquals(source, getSource(expr.toString()));
    }

    @Test
    public void testEscape() throws Exception {
        JexlContext none = null;
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
        JxltEngine.Expression expr = JXLT.createExpression("\\\"${'world\\'s finest'}\\\"");
        JexlContext none = null;
        Object o = expr.evaluate(none);
        Assert.assertEquals("\"world's finest\"", o);
    }

    @Test
    public void testNonEscapeString() throws Exception {
        JxltEngine.Expression expr = JXLT.createExpression("c:\\some\\windows\\path");
        JexlContext none = null;
        Object o = expr.evaluate(none);
        Assert.assertEquals("c:\\some\\windows\\path", o);
    }

    @Test
    public void testMalformed() throws Exception {
        try {
            JxltEngine.Expression expr = JXLT.createExpression("${'world'");
            JexlContext none = null;
            expr.evaluate(none);
            Assert.fail("should be malformed");
        } catch (JxltEngine.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    @Test
    public void testMalformedNested() throws Exception {
        try {
            JxltEngine.Expression expr = JXLT.createExpression("#{${hi} world}");
            JexlContext none = null;
            expr.evaluate(none);
            Assert.fail("should be malformed");
        } catch (JxltEngine.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    @Test
    public void testMalformedNested2() throws Exception {
        try {
            JxltEngine.Expression expr = JXLT.createExpression("#{${hi} world}");
            JexlContext ctxt = new MapContext();
            ctxt.set("hi", "hello");
            expr.evaluate(ctxt);
            Assert.fail("should be malformed");
        } catch (JxltEngine.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    @Test
    public void testBadContextNested() throws Exception {
        try {
            JxltEngine.Expression expr = JXLT.createExpression("#{${hi}+'.world'}");
            JexlContext none = null;
            expr.evaluate(none);
            Assert.fail("should be malformed");
        } catch (JxltEngine.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    @Test
    public void testCharAtBug() throws Exception {
        context.set("foo", "abcdef");
        JxltEngine.Expression expr = JXLT.createExpression("${foo.substring(2,4)/*comment*/}");
        Object o = expr.evaluate(context);
        Assert.assertEquals("cd", o);

        context.set("bar", "foo");
        try {
            context.setSilent(true);
            expr = JXLT.createExpression("#{${bar}+'.charAt(-2)'}");
            expr = expr.prepare(context);
            o = expr.evaluate(context);
            Assert.assertEquals(null, o);
        } finally {
            context.setSilent(false);
        }

    }

    @Test
    public void testTemplate0() throws Exception {
        String source = "   $$ if(x) {\nx is ${x}\n   $$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        JxltEngine.Template t = JXLT.createTemplate(source);

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

        String dstr = t.toString();
        Assert.assertNotNull(dstr);
    }

    @Test
    public void testTemplate10() throws Exception {
        String source = "$$(x)->{ if(x) {\nx is ${x}\n$$ } else {\n${'no x'}\n$$ } }\n";
        StringWriter strw;
        String output;

        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), (String[]) null);
        String dstr = t.asString();
        Assert.assertNotNull(dstr);

        String[] ps = t.getParameters();
        Assert.assertTrue(Arrays.asList(ps).contains("x"));

        strw = new StringWriter();
        t.evaluate(context, strw, 42);
        output = strw.toString();
        Assert.assertEquals("x is 42\n", output);
    }

    @Test
    public void testTemplate1() throws Exception {
        String source = "$$ if(x) {\nx is ${x}\n$$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), "x");
        String dstr = t.asString();
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
        String source = "The answer: ${x}";
        StringWriter strw;
        String output;

        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), "x");
        String dstr = t.asString();
        Assert.assertNotNull(dstr);

        strw = new StringWriter();
        t.evaluate(context, strw, 42);
        output = strw.toString();
        Assert.assertEquals("The answer: 42", output);
    }

    @Test
    public void testPrepareTemplate() throws Exception {
        String source
                = "$$ for(var x : list) {\n"
                + "${l10n}=#{x}\n"
                + "$$ }\n";
        int[] args = {42};
        JxltEngine.Template tl10n = JXLT.createTemplate(source, "list");
        String dstr = tl10n.asString();
        Assert.assertNotNull(dstr);
        Set<List<String>> vars = tl10n.getVariables();
        Assert.assertFalse(vars.isEmpty());
        context.set("l10n", "valeur");
        JxltEngine.Template tpFR = tl10n.prepare(context);
        context.set("l10n", "value");
        JxltEngine.Template tpEN = tl10n.prepare(context);
        context.set("l10n", null);

        StringWriter strw;
        strw = new StringWriter();
        tpFR.evaluate(context, strw, args);
        String outFR = strw.toString();
        Assert.assertEquals("valeur=42\n", outFR);

        context.set("l10n", null);
        strw = new StringWriter();
        tpEN.evaluate(context, strw, args);
        String outEN = strw.toString();
        Assert.assertEquals("value=42\n", outEN);
    }

    @Test
    public void test42() throws Exception {
        String test42
                = "$$ for(var x : list) {\n"
                + "$$   if (x == 42) {\n"
                + "Life, the universe, and everything\n"
                + "$$   } else if (x > 42) {\n"
                + "The value ${x} is over fourty-two\n"
                + "$$   } else {\n"
                + "The value ${x} is under fourty-two\n"
                + "$$   }\n"
                + "$$ }\n";
        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(test42), "list");
        StringWriter strw = new StringWriter();
        int[] list = {1, 3, 5, 42, 169};
        t.evaluate(context, strw, list);
        String output = strw.toString();
        String out42
                = "The value 1 is under fourty-two\n"
                + "The value 3 is under fourty-two\n"
                + "The value 5 is under fourty-two\n"
                + "Life, the universe, and everything\n"
                + "The value 169 is over fourty-two\n";
        Assert.assertEquals(out42, output);

        String dstr = t.asString();
        Assert.assertNotNull(dstr);

        TemplateDebugger td = new TemplateDebugger();
        String refactored = refactor(td, (TemplateScript) t);
        Assert.assertNotNull(refactored);
        Assert.assertEquals(test42, refactored);
    }
    
    @Test
    public void testInheritedDebugger() throws Exception {
        String src = "if ($A) { $B + 1; } else { $C - 2 }";
        JexlEngine jexl = JXLT.getEngine();
        JexlScript script = jexl.createScript(src);
                
        Debugger sd = new Debugger();
        String rscript = sd.debug(script)? sd.toString() : null;
        Assert.assertNotNull(rscript);
        
        TemplateDebugger td = new TemplateDebugger();
        String refactored = td.debug(script)? td.toString() : null;
        Assert.assertNotNull(refactored);
        Assert.assertEquals(refactored, rscript);
    }

    public static class FrobozWriter extends PrintWriter {
        public FrobozWriter(Writer w) {
            super(w);
        }

        public void print(Froboz froboz) {
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
        Froboz froboz = new Froboz(42);
        Writer writer = new FrobozWriter(new StringWriter());
        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("$$$jexl.print(froboz)"), "froboz");
        t.evaluate(context, writer, froboz);
        Assert.assertEquals("froboz{42}", writer.toString());
    }

    @Test
    public void testReport() throws Exception {
        String rpt
                = "<report>\n"
                + "\n"
                + "\n$$ var a = 1;"
                + "\n$$ var x = 2;"
                + "\n"
                + "\n$$ var y = 9;"
                + "\n"
                + "\n        ${x + y}"
                + "\n</report>\n";
        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        StringWriter strw = new StringWriter();
        t.evaluate(context, strw);
        String output = strw.toString();
        String ctl = "<report>\n\n\n\n\n        11\n</report>\n";
        Assert.assertEquals(ctl, output);

        TemplateDebugger td = new TemplateDebugger();
        String refactored = refactor(td, (TemplateScript) t);
        Assert.assertNotNull(refactored);
        Assert.assertEquals(rpt, refactored);
    }

    @Test
    public void testReport1() throws Exception {
        String rpt
                = "<report>\n"
                + "this is ${x}\n"
                + "${x + 1}\n"
                + "${x + 2}\n"
                + "${x + 3}\n"
                + "</report>\n";
        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt));
        StringWriter strw = new StringWriter();
        context.set("x", 42);
        t.evaluate(context, strw, 42);
        String output = strw.toString();
        int count = 0;
        for (int i = 0; i < output.length(); ++i) {
            char c = output.charAt(i);
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
        String rpt
                = "<report>\n"
                + "this is ${x}\n"
                + "${x + 1}\n"
                + "${x + 2}\n"
                + "${x + 3}\n"
                + "</report>\n";
        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(rpt), "x");
        StringWriter strw = new StringWriter();
        t.evaluate(context, strw, 42);
        String output = strw.toString();
        int count = 0;
        for (int i = 0; i < output.length(); ++i) {
            char c = output.charAt(i);
            if ('\n' == c) {
                count += 1;
            }
        }
        Assert.assertEquals(6, count);
        Assert.assertTrue(output.indexOf("42") > 0);
        Assert.assertTrue(output.indexOf("43") > 0);
        Assert.assertTrue(output.indexOf("44") > 0);
        Assert.assertTrue(output.indexOf("45") > 0);

        TemplateDebugger td = new TemplateDebugger();
        String xxx = refactor(td, (TemplateScript) t);
        Assert.assertNotNull(xxx);
        Assert.assertEquals(rpt, xxx);
    }
    @Test
    public void testOneLiner() throws Exception {
        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("fourty-two"));
        StringWriter strw = new StringWriter();
        t.evaluate(context, strw);
        String output = strw.toString();
        Assert.assertEquals("fourty-two", output);
    }

    @Test
    public void testOneLinerVar() throws Exception {
        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("fourty-${x}"));
        StringWriter strw = new StringWriter();
        context.set("x", "two");
        t.evaluate(context, strw);
        String output = strw.toString();
        Assert.assertEquals("fourty-two", output);
    }

    @Test
    public void testInterpolation() throws Exception {
        String expr =  "`Hello \n${user}`";
        JexlScript script = JEXL.createScript(expr);
        context.set("user", "Dimitri");
        Object value = script.execute(context);
        Assert.assertEquals(expr, "Hello \nDimitri", value);
        context.set("user", "Rahul");
        value = script.execute(context);
        Assert.assertEquals(expr, "Hello \nRahul", value);
    }

    @Test
    public void testInterpolationGlobal() throws Exception {
        String expr =  "user='Dimitri'; `Hello \n${user}`";
        Object value = JEXL.createScript(expr).execute(context);
        Assert.assertEquals(expr, "Hello \nDimitri", value);
    }

    @Test
    public void testInterpolationLocal() throws Exception {
        String expr =  "var user='Henrib'; `Hello \n${user}`";
        Object value = JEXL.createScript(expr).execute(context);
        Assert.assertEquals(expr, "Hello \nHenrib", value);
    }

    @Test
    public void testInterpolationLvsG() throws Exception {
        String expr =  "user='Dimitri'; var user='Henrib'; `H\\\"ello \n${user}`";
        Object value = JEXL.createScript(expr).execute(context);
        Assert.assertEquals(expr, "H\"ello \nHenrib", value);
    }
        @Test
    public void testInterpolationLvsG2() throws Exception {
        String expr =  "user='Dimitri'; var user='Henrib'; `H\\`ello \n${user}`";
        Object value = JEXL.createScript(expr).execute(context);
        Assert.assertEquals(expr, "H`ello \nHenrib", value);
    }

    @Test
    public void testInterpolationParameter() throws Exception {
        String expr =  "(user)->{`Hello \n${user}`}";
        Object value = JEXL.createScript(expr).execute(context, "Henrib");
        Assert.assertEquals(expr, "Hello \nHenrib", value);
        value = JEXL.createScript(expr).execute(context, "Dimitri");
        Assert.assertEquals(expr, "Hello \nDimitri", value);
    }
//
//
//    @Test
//    public void testDeferredTemplate() throws Exception {
//        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(
//             "select * from \n"+
//             "##for(var c : tables) {\n"+
//             "#{c} \n"+
//             "##}\n"+
//             "where $(w}\n"
//                ));
//        StringWriter strw = new StringWriter();
//        context.set("tables", new String[]{"table1", "table2"});
//        t = t.prepare(context);
//        vars.clear();
//        context.set("w" ,"x=1");
//        t.evaluate(context, strw);
//        String output = strw.toString();
//        Assert.assertEquals("fourty-two", output);
//
//    }
}
