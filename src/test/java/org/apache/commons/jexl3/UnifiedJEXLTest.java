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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Test cases for the UnifiedEL.
 */
public class UnifiedJEXLTest extends JexlTestCase {
    private static final JexlEngine ENGINE = new JexlBuilder().silent(false).cache(128).strict(true).create();

    private static final JxltEngine JXLT = ENGINE.jxlt();
    private static final Log LOG = LogFactory.getLog(JxltEngine.class);
    private JexlEvalContext context = null;

    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error
        java.util.logging.Logger.getLogger(org.apache.commons.jexl3.JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
        context = new JexlEvalContext();
    }

    @Override
    protected void tearDown() throws Exception {
        debuggerCheck(ENGINE);
        super.tearDown();
    }
    
    /** Extract the source from a toString-ed expression. */
    private String getSource(String tostring) {
        int len = tostring.length();
        int sc = tostring.lastIndexOf(" /*= ");
        if (sc >= 0)  {
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

    public UnifiedJEXLTest(String testName) {
        super(testName);
    }

    public void testStatement() throws Exception {
        context.set("froboz", new Froboz(123));
        JxltEngine.UnifiedExpression check = JXLT.createExpression("${froboz.value = 32; froboz.plus10(); froboz.value}");
        Object o = check.evaluate(context);
        assertEquals("Result is not 42", new Integer(42), o);
        Set<List<String>> evars = check.getVariables();
        assertEquals(2, evars.size());
    }

    public void testAssign() throws Exception {
        JxltEngine.UnifiedExpression assign = JXLT.createExpression("${froboz.value = 10}");
        JxltEngine.UnifiedExpression check = JXLT.createExpression("${froboz.value}");
        Object o = assign.evaluate(context);
        assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(context);
        assertEquals("Result is not 10", new Integer(10), o);
    }

    public void testComposite() throws Exception {
        String source = "Dear ${p} ${name};";
        JxltEngine.UnifiedExpression expr = JXLT.createExpression(source);
        context.set("p", "Mr");
        context.set("name", "Doe");
        assertTrue("expression should be immediate", expr.isImmediate());
        Object o = expr.evaluate(context);
        assertEquals("Dear Mr Doe;", o);
        context.set("p", "Ms");
        context.set("name", "Jones");
        o = expr.evaluate(context);
        assertEquals("Dear Ms Jones;", o);
        assertEquals(source, getSource(expr.toString()));
    }

    public void testPrepareEvaluate() throws Exception {
        final String source = "Dear #{p} ${name};";
        JxltEngine.UnifiedExpression expr = JXLT.createExpression("Dear #{p} ${name};");
        assertTrue("expression should be deferred", expr.isDeferred());

        Set<List<String>> evars = expr.getVariables();
        assertEquals(1, evars.size());
        assertTrue(evars.contains(Arrays.asList("name")));
        context.set("name", "Doe");
        JxltEngine.UnifiedExpression phase1 = expr.prepare(context);
        String as = phase1.asString();
        assertEquals("Dear ${p} Doe;", as);
        Set<List<String>> evars1 = phase1.getVariables();
        assertEquals(1, evars1.size());
        assertTrue(evars1.contains(Arrays.asList("p")));
        context.clearVariables();
        context.set("p", "Mr");
        context.set("name", "Should not be used in 2nd phase");
        Object o = phase1.evaluate(context);
        assertEquals("Dear Mr Doe;", o);
        
        String p1 = getSource(phase1.toString());
        assertEquals(source, getSource(phase1.toString()));
        assertEquals(source, getSource(expr.toString()));
    }

    public void testNested() throws Exception {
        final String source = "#{${hi}+'.world'}";
        JxltEngine.UnifiedExpression expr = JXLT.createExpression(source);

        Set<List<String>> evars = expr.getVariables();
        assertEquals(1, evars.size());
        assertTrue(evars.contains(Arrays.asList("hi")));

        context.set("hi", "greeting");
        context.set("greeting.world", "Hello World!");
        assertTrue("expression should be deferred", expr.isDeferred());
        Object o = expr.evaluate(context);
        assertEquals("Hello World!", o);
        
        assertEquals(source, getSource(expr.toString()));
    }

    public void testImmediate() throws Exception {
        JexlContext none = null;
        final String source = "${'Hello ' + 'World!'}";
        JxltEngine.UnifiedExpression expr = JXLT.createExpression(source);
        JxltEngine.UnifiedExpression prepared = expr.prepare(none);
        assertEquals("prepare should return same expression", "Hello World!", prepared.asString());
        Object o = expr.evaluate(none);
        assertTrue("expression should be immediate", expr.isImmediate());
        assertEquals("Hello World!", o);
        
        assertEquals(source, getSource(expr.toString()));
    }

    public void testConstant() throws Exception {
        JexlContext none = null;
        final String source = "Hello World!";
        JxltEngine.UnifiedExpression expr = JXLT.createExpression(source);
        assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        Object o = expr.evaluate(none);
        assertTrue("expression should be immediate", expr.isImmediate());
        assertEquals("Hello World!", o);
        
        assertEquals(source, getSource(expr.toString()));
    }

    public void testDeferred() throws Exception {
        JexlContext none = null;
        final String source = "#{'world'}";
        JxltEngine.UnifiedExpression expr = JXLT.createExpression(source);
        assertTrue("expression should be deferred", expr.isDeferred());
        String as = expr.prepare(none).asString();
        assertEquals("prepare should return immediate version", "${'world'}", as);
        Object o = expr.evaluate(none);
        assertEquals("world", o);
        
        assertEquals(source, getSource(expr.toString()));
    }

    public void testEscape() throws Exception {
        JexlContext none = null;
        JxltEngine.UnifiedExpression expr;
        Object o;
        // $ and # are escapable in TemplateEngine
        expr = JXLT.createExpression("\\#{'world'}");
        o = expr.evaluate(none);
        assertEquals("#{'world'}", o);
        expr = JXLT.createExpression("\\${'world'}");
        o = expr.evaluate(none);
        assertEquals("${'world'}", o);
    }

    public void testEscapeString() throws Exception {
        JxltEngine.UnifiedExpression expr = JXLT.createExpression("\\\"${'world\\'s finest'}\\\"");
        JexlContext none = null;
        Object o = expr.evaluate(none);
        assertEquals("\"world's finest\"", o);
    }

    public void testNonEscapeString() throws Exception {
        JxltEngine.UnifiedExpression expr = JXLT.createExpression("c:\\some\\windows\\path");
        JexlContext none = null;
        Object o = expr.evaluate(none);
        assertEquals("c:\\some\\windows\\path", o);
    }

    public void testMalformed() throws Exception {
        try {
            JxltEngine.UnifiedExpression expr = JXLT.createExpression("${'world'");
            JexlContext none = null;
            expr.evaluate(none);
            fail("should be malformed");
        } catch (JxltEngine.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    public void testMalformedNested() throws Exception {
        try {
            JxltEngine.UnifiedExpression expr = JXLT.createExpression("#{${hi} world}");
            JexlContext none = null;
            expr.evaluate(none);
            fail("should be malformed");
        } catch (JxltEngine.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    public void testBadContextNested() throws Exception {
        try {
            JxltEngine.UnifiedExpression expr = JXLT.createExpression("#{${hi}+'.world'}");
            JexlContext none = null;
            expr.evaluate(none);
            fail("should be malformed");
        } catch (JxltEngine.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    public void testCharAtBug() throws Exception {
        context.set("foo", "abcdef");
        JxltEngine.UnifiedExpression expr = JXLT.createExpression("${foo.substring(2,4)/*comment*/}");
        Object o = expr.evaluate(context);
        assertEquals("cd", o);

        context.set("bar", "foo");
        try {
            context.setSilent(true);
            expr = JXLT.createExpression("#{${bar}+'.charAt(-2)'}");
            expr = expr.prepare(context);
            o = expr.evaluate(context);
            assertEquals(null, o);
        } finally {
            context.setSilent(false);
        }

    }

    public void testTemplate0() throws Exception {
        String source = "   $$ if(x) {\nx is ${x}\n   $$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        JxltEngine.Template t = JXLT.createTemplate(source);

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
        
        String dstr = t.toString();
        assertNotNull(dstr);
    }

    public void testTemplate1() throws Exception {
        String source = "$$ if(x) {\nx is ${x}\n$$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader(source), "x");
        String dstr = t.asString();
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
    
    public void testPrepareTemplate() throws Exception {
        String source =
                 "$$ for(var x : list) {\n"
               + "${l10n}=#{x}\n"
               + "$$ }\n";
        int[] args = { 42 };
        JxltEngine.Template tl10n = JXLT.createTemplate(source, "list");
        String dstr = tl10n.asString();
        assertNotNull(dstr);
        context.set("l10n", "valeur");
        JxltEngine.Template tpFR = tl10n.prepare(context);
        context.set("l10n", "value");
        JxltEngine.Template tpEN = tl10n.prepare(context);
        context.set("l10n", null);
        
        StringWriter strw;
        strw = new StringWriter();
        tpFR.evaluate(context, strw, args);
        String outFR = strw.toString();
        assertEquals("valeur=42\n", outFR);
        
        context.set("l10n", null);
        strw = new StringWriter();
        tpEN.evaluate(context, strw, args);
        String outEN = strw.toString();
        assertEquals("value=42\n", outEN);
    }

    public void test42() throws Exception {
        String test42 =
                  "$$ for(var x : list) {\n"
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
        String out42 =
                  "The value 1 is under fourty-two\n"
                + "The value 3 is under fourty-two\n"
                + "The value 5 is under fourty-two\n"
                + "Life, the universe, and everything\n"
                + "The value 169 is over fourty-two\n";
        assertEquals(out42, output);
        
        String dstr = t.asString();
        assertNotNull(dstr);
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
    
    public void testWriter() throws Exception {
        Froboz froboz = new Froboz(42);
        Writer writer = new FrobozWriter(new StringWriter());
        JxltEngine.Template t = JXLT.createTemplate("$$", new StringReader("$$$jexl.print(froboz)"), "froboz");
        t.evaluate(context, writer, froboz);
        assertEquals("froboz{42}", writer.toString());
    }
}
