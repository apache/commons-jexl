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
package org.apache.commons.jexl2;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test cases for the UnifiedEL.
 */
public class UnifiedJEXLTest extends JexlTestCase {
    private static final JexlEngine ENGINE = createEngine(false);

    static {
        ENGINE.setSilent(false);
        ENGINE.setCache(128);
    }
    private static final UnifiedJEXL EL = new UnifiedJEXL(ENGINE);
    private static final Log LOG = LogFactory.getLog(UnifiedJEXL.class);
    private JexlContext context = null;
    private Map<String, Object> vars = null;

    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
        vars = new HashMap<String, Object>();
        context = new MapContext(vars);
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
        vars.put("froboz", new Froboz(123));
        UnifiedJEXL.Expression check = EL.parse("${froboz.value = 32; froboz.plus10(); froboz.value}");
        Object o = check.evaluate(context);
        assertEquals("Result is not 42", new Integer(42), o);
        Set<List<String>> evars = check.getVariables();
        assertEquals(2, evars.size());
    }

    public void testAssign() throws Exception {
        UnifiedJEXL.Expression assign = EL.parse("${froboz.value = 10}");
        UnifiedJEXL.Expression check = EL.parse("${froboz.value}");
        Object o = assign.evaluate(context);
        assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(context);
        assertEquals("Result is not 10", new Integer(10), o);
    }

    public void testComposite() throws Exception {
        String source = "Dear ${p} ${name};";
        UnifiedJEXL.Expression expr = EL.parse(source);
        vars.put("p", "Mr");
        vars.put("name", "Doe");
        assertTrue("expression should be immediate", expr.isImmediate());
        Object o = expr.evaluate(context);
        assertEquals("Dear Mr Doe;", o);
        vars.put("p", "Ms");
        vars.put("name", "Jones");
        o = expr.evaluate(context);
        assertEquals("Dear Ms Jones;", o);
        assertEquals(source, getSource(expr.toString()));
    }

    public void testPrepareEvaluate() throws Exception {
        final String source = "Dear #{p} ${name};";
        UnifiedJEXL.Expression expr = EL.parse("Dear #{p} ${name};");
        assertTrue("expression should be deferred", expr.isDeferred());

        Set<List<String>> evars = expr.getVariables();
        assertEquals(1, evars.size());
        assertTrue(evars.contains(Arrays.asList("name")));
        vars.put("name", "Doe");
        UnifiedJEXL.Expression phase1 = expr.prepare(context);
        String as = phase1.asString();
        assertEquals("Dear ${p} Doe;", as);
        Set<List<String>> evars1 = phase1.getVariables();
        assertEquals(1, evars1.size());
        assertTrue(evars1.contains(Arrays.asList("p")));
        vars.put("p", "Mr");
        vars.put("name", "Should not be used in 2nd phase");
        Object o = phase1.evaluate(context);
        assertEquals("Dear Mr Doe;", o);
        
        assertEquals(source, getSource(phase1.toString()));
        assertEquals(source, getSource(expr.toString()));
    }

    public void testNested() throws Exception {
        final String source = "#{${hi}+'.world'}";
        UnifiedJEXL.Expression expr = EL.parse(source);

        Set<List<String>> evars = expr.getVariables();
        assertEquals(1, evars.size());
        assertTrue(evars.contains(Arrays.asList("hi")));

        vars.put("hi", "greeting");
        vars.put("greeting.world", "Hello World!");
        assertTrue("expression should be deferred", expr.isDeferred());
        Object o = expr.evaluate(context);
        assertEquals("Hello World!", o);
        
        assertEquals(source, getSource(expr.toString()));
    }

    public void testImmediate() throws Exception {
        JexlContext none = null;
        final String source = "${'Hello ' + 'World!'}";
        UnifiedJEXL.Expression expr = EL.parse(source);
        UnifiedJEXL.Expression prepared = expr.prepare(none);
        assertEquals("prepare should return same expression", "Hello World!", prepared.asString());
        Object o = expr.evaluate(none);
        assertTrue("expression should be immediate", expr.isImmediate());
        assertEquals("Hello World!", o);
        
        assertEquals(source, getSource(expr.toString()));
    }

    public void testConstant() throws Exception {
        JexlContext none = null;
        final String source = "Hello World!";
        UnifiedJEXL.Expression expr = EL.parse(source);
        assertTrue("prepare should return same expression", expr.prepare(none) == expr);
        Object o = expr.evaluate(none);
        assertTrue("expression should be immediate", expr.isImmediate());
        assertEquals("Hello World!", o);
        
        assertEquals(source, getSource(expr.toString()));
    }

    public void testDeferred() throws Exception {
        JexlContext none = null;
        final String source = "#{'world'}";
        UnifiedJEXL.Expression expr = EL.parse(source);
        assertTrue("expression should be deferred", expr.isDeferred());
        String as = expr.prepare(none).asString();
        assertEquals("prepare should return immediate version", "${'world'}", as);
        Object o = expr.evaluate(none);
        assertEquals("world", o);
        
        assertEquals(source, getSource(expr.toString()));
    }

    public void testEscape() throws Exception {
        JexlContext none = null;
        UnifiedJEXL.Expression expr;
        Object o;
        // $ and # are escapable in UnifiedJEXL
        expr = EL.parse("\\#{'world'}");
        o = expr.evaluate(none);
        assertEquals("#{'world'}", o);
        expr = EL.parse("\\${'world'}");
        o = expr.evaluate(none);
        assertEquals("${'world'}", o);
    }

    public void testEscapeString() throws Exception {
        UnifiedJEXL.Expression expr = EL.parse("\\\"${'world\\'s finest'}\\\"");
        JexlContext none = null;
        Object o = expr.evaluate(none);
        assertEquals("\"world's finest\"", o);
    }

    public void testNonEscapeString() throws Exception {
        UnifiedJEXL.Expression expr = EL.parse("c:\\some\\windows\\path");
        JexlContext none = null;
        Object o = expr.evaluate(none);
        assertEquals("c:\\some\\windows\\path", o);
    }

    public void testMalformed() throws Exception {
        try {
            UnifiedJEXL.Expression expr = EL.parse("${'world'");
            JexlContext none = null;
            expr.evaluate(none);
            fail("should be malformed");
        } catch (UnifiedJEXL.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    public void testMalformedNested() throws Exception {
        try {
            UnifiedJEXL.Expression expr = EL.parse("#{${hi} world}");
            JexlContext none = null;
            expr.evaluate(none);
            fail("should be malformed");
        } catch (UnifiedJEXL.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    public void testBadContextNested() throws Exception {
        try {
            UnifiedJEXL.Expression expr = EL.parse("#{${hi}+'.world'}");
            JexlContext none = null;
            expr.evaluate(none);
            fail("should be malformed");
        } catch (UnifiedJEXL.Exception xjexl) {
            // expected
            String xmsg = xjexl.getMessage();
            LOG.warn(xmsg);
        }
    }

    public void testCharAtBug() throws Exception {
        vars.put("foo", "abcdef");
        UnifiedJEXL.Expression expr = EL.parse("${foo.substring(2,4)/*comment*/}");
        Object o = expr.evaluate(context);
        assertEquals("cd", o);

        vars.put("bar", "foo");
        try {
            ENGINE.setSilent(true);
            expr = EL.parse("#{${bar}+'.charAt(-2)'}");
            expr = expr.prepare(context);
            o = expr.evaluate(context);
            assertEquals(null, o);
        } finally {
            ENGINE.setSilent(false);
        }

    }

    public void testTemplate0() throws Exception {
        String source = "   $$ if(x) {\nx is ${x}\n   $$ } else {\n${'no x'}\n$$ }\n";
        StringWriter strw;
        String output;

        UnifiedJEXL.Template t = EL.createTemplate(source);

        vars.put("x", Integer.valueOf(42));
        strw = new StringWriter();
        t.evaluate(context, strw);
        output = strw.toString();
        assertEquals("x is 42\n", output);

        strw = new StringWriter();
        vars.put("x", "");
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

        UnifiedJEXL.Template t = EL.createTemplate("$$", new StringReader(source), "x");
        String dstr = t.asString();
        assertNotNull(dstr);

        strw = new StringWriter();
        t.evaluate(context, strw, Integer.valueOf(42));
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
        UnifiedJEXL.Template tl10n = EL.createTemplate(source, "list");
        String dstr = tl10n.asString();
        assertNotNull(dstr);
        context.set("l10n", "valeur");
        UnifiedJEXL.Template tpFR = tl10n.prepare(context);
        context.set("l10n", "value");
        UnifiedJEXL.Template tpEN = tl10n.prepare(context);
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
        UnifiedJEXL.Template t = EL.createTemplate("$$", new StringReader(test42), "list");
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
        UnifiedJEXL.Template t = EL.createTemplate("$$", new StringReader("$$$jexl.print(froboz)"), "froboz");
        t.evaluate(context, writer, froboz);
        assertEquals("froboz{42}", writer.toString());
    }
}
