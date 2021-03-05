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

import org.apache.commons.jexl3.internal.Engine;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for reported issue between JEXL-100 and JEXL-199.
 */
@SuppressWarnings({"boxing", "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class Issues100Test extends JexlTestCase {
    public Issues100Test() {
        super("Issues100Test", null);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }


    @Test
    public void test100() throws Exception {
        final JexlEngine jexl = new JexlBuilder().cache(4).create();
        final JexlContext ctxt = new MapContext();
        final int[] foo = {42};
        ctxt.set("foo", foo);
        Object value;
        for (int l = 0; l < 2; ++l) {
            value = jexl.createExpression("foo[0]").evaluate(ctxt);
            Assert.assertEquals(42, value);
            value = jexl.createExpression("foo[0] = 43").evaluate(ctxt);
            Assert.assertEquals(43, value);
            value = jexl.createExpression("foo.0").evaluate(ctxt);
            Assert.assertEquals(43, value);
            value = jexl.createExpression("foo.0 = 42").evaluate(ctxt);
            Assert.assertEquals(42, value);
        }
    }

// A's class definition
    public static class A105 {
        String nameA;
        String propA;

        public A105(final String nameA, final String propA) {
            this.nameA = nameA;
            this.propA = propA;
        }

        @Override
        public String toString() {
            return "A [nameA=" + nameA + ", propA=" + propA + "]";
        }

        public String getNameA() {
            return nameA;
        }

        public String getPropA() {
            return propA;
        }

        public String uppercase(final String str) {
            return str.toUpperCase();
        }
    }

    @Test
    public void test105() throws Exception {
        final JexlContext context = new MapContext();
        final JexlExpression selectExp = new Engine().createExpression("[a.propA]");
        context.set("a", new A105("a1", "p1"));
        Object[] r = (Object[]) selectExp.evaluate(context);
        Assert.assertEquals("p1", r[0]);

//selectExp = new Engine().createExpression("[a.propA]");
        context.set("a", new A105("a2", "p2"));
        r = (Object[]) selectExp.evaluate(context);
        Assert.assertEquals("p2", r[0]);
    }

    @Test
    public void test106() throws Exception {
        final JexlEvalContext context = new JexlEvalContext();
        final JexlOptions options = context.getEngineOptions();
        options.setStrict(true);
        options.setStrictArithmetic(true);
        context.set("a", new BigDecimal(1));
        context.set("b", new BigDecimal(3));
        final JexlEngine jexl = new Engine();
        try {
            final Object value = jexl.createExpression("a / b").evaluate(context);
            Assert.assertNotNull(value);
        } catch (final JexlException xjexl) {
            Assert.fail("should not occur");
        }
        options.setMathContext(MathContext.UNLIMITED);
        options.setMathScale(2);
        try {
            jexl.createExpression("a / b").evaluate(context);
            Assert.fail("should fail");
        } catch (final JexlException xjexl) {
            //ok  to fail
        }
    }

    @Test
    public void test107() throws Exception {
        final String[] exprs = {
            "'Q4'.toLowerCase()", "q4",
            "(Q4).toLowerCase()", "q4",
            "(4).toString()", "4",
            "(1 + 3).toString()", "4",
            "({ 'q' : 'Q4'}).get('q').toLowerCase()", "q4",
            "{ 'q' : 'Q4'}.get('q').toLowerCase()", "q4",
            "({ 'q' : 'Q4'})['q'].toLowerCase()", "q4",
            "(['Q4'])[0].toLowerCase()", "q4"
        };

        final JexlContext context = new MapContext();
        context.set("Q4", "Q4");
        final JexlEngine jexl = new Engine();
        for (int e = 0; e < exprs.length; e += 2) {
            JexlExpression expr = jexl.createExpression(exprs[e]);
            final Object expected = exprs[e + 1];
            Object value = expr.evaluate(context);
            Assert.assertEquals(expected, value);
            expr = jexl.createExpression(expr.getParsedText());
            value = expr.evaluate(context);
            Assert.assertEquals(expected, value);
        }
    }

    @Test
    public void test108() throws Exception {
        JexlScript expr;
        Object value;
        final JexlEngine jexl = new Engine();
        expr = jexl.createScript("size([])");
        value = expr.execute(null);
        Assert.assertEquals(0, value);
        expr = jexl.createScript(expr.getParsedText());
        value = expr.execute(null);
        Assert.assertEquals(0, value);

        expr = jexl.createScript("if (true) { [] } else { {:} }");
        value = expr.execute(null);
        Assert.assertTrue(value.getClass().isArray());
        expr = jexl.createScript(expr.getParsedText());
        value = expr.execute(null);
        Assert.assertTrue(value.getClass().isArray());

        expr = jexl.createScript("size({:})");
        value = expr.execute(null);
        Assert.assertEquals(0, value);
        expr = jexl.createScript(expr.getParsedText());
        value = expr.execute(null);
        Assert.assertEquals(0, value);

        expr = jexl.createScript("if (false) { [] } else { {:} }");
        value = expr.execute(null);
        Assert.assertTrue(value instanceof Map<?, ?>);
        expr = jexl.createScript(expr.getParsedText());
        value = expr.execute(null);
        Assert.assertTrue(value instanceof Map<?, ?>);
    }

    @Test
    public void test109() throws Exception {
        final JexlEngine jexl = new Engine();
        Object value;
        final JexlContext context = new MapContext();
        context.set("foo.bar", 40);
        value = jexl.createExpression("foo.bar + 2").evaluate(context);
        Assert.assertEquals(42, value);
    }

    @Test
    public void test110() throws Exception {
        final JexlEngine jexl = new Engine();
        final String[] names = {"foo"};
        Object value;
        final JexlContext context = new MapContext();
        value = jexl.createScript("foo + 2", names).execute(context, 40);
        Assert.assertEquals(42, value);
        context.set("frak.foo", -40);
        value = jexl.createScript("frak.foo - 2", names).execute(context, 40);
        Assert.assertEquals(-42, value);
    }

    public static class RichContext extends ObjectContext<A105> {
        RichContext(final JexlEngine jexl, final A105 a105) {
            super(jexl, a105);
        }
    }

    @Test
    public void testRichContext() throws Exception {
        final A105 a105 = new A105("foo", "bar");
        final JexlEngine jexl = new Engine();
        Object value;
        final JexlContext context = new RichContext(jexl, a105);
        value = jexl.createScript("uppercase(nameA + propA)").execute(context);
        Assert.assertEquals("FOOBAR", value);
    }

    @Test
    public void test111() throws Exception {
        final JexlEngine jexl = new Engine();
        Object value;
        final JexlContext context = new MapContext();
        final String strExpr = "((x>0)?\"FirstValue=\"+(y-x):\"SecondValue=\"+x)";
        final JexlExpression expr = jexl.createExpression(strExpr);

        context.set("x", 1);
        context.set("y", 10);
        value = expr.evaluate(context);
        Assert.assertEquals("FirstValue=9", value);

        context.set("x", 1.0d);
        context.set("y", 10.0d);
        value = expr.evaluate(context);
        Assert.assertEquals("FirstValue=9.0", value);

        context.set("x", 1);
        context.set("y", 10.0d);
        value = expr.evaluate(context);
        Assert.assertEquals("FirstValue=9.0", value);

        context.set("x", 1.0d);
        context.set("y", 10);
        value = expr.evaluate(context);
        Assert.assertEquals("FirstValue=9.0", value);

        context.set("x", -10);
        context.set("y", 1);
        value = expr.evaluate(context);
        Assert.assertEquals("SecondValue=-10", value);

        context.set("x", -10.0d);
        context.set("y", 1.0d);
        value = expr.evaluate(context);
        Assert.assertEquals("SecondValue=-10.0", value);

        context.set("x", -10);
        context.set("y", 1.0d);
        value = expr.evaluate(context);
        Assert.assertEquals("SecondValue=-10", value);

        context.set("x", -10.0d);
        context.set("y", 1);
        value = expr.evaluate(context);
        Assert.assertEquals("SecondValue=-10.0", value);
    }

    @Test
    public void testScaleIssue() throws Exception {
        final JexlEngine jexlX = new Engine();
        final String expStr1 = "result == salary/month * work.percent/100.00";
        final JexlExpression exp1 = jexlX.createExpression(expStr1);
        final JexlEvalContext ctx = new JexlEvalContext();
        final JexlOptions options = ctx.getEngineOptions();
        ctx.set("result", new BigDecimal("9958.33"));
        ctx.set("salary", new BigDecimal("119500.00"));
        ctx.set("month", new BigDecimal("12.00"));
        ctx.set("work.percent", new BigDecimal("100.00"));

        // will fail because default scale is 5
        Assert.assertFalse((Boolean) exp1.evaluate(ctx));

        // will succeed with scale = 2
        options.setMathScale(2);
        Assert.assertTrue((Boolean) exp1.evaluate(ctx));
    }

    @Test
    public void test112() throws Exception {
        Object result;
        final JexlEngine jexl = new Engine();
        result = jexl.createScript(Integer.toString(Integer.MAX_VALUE)).execute(null);
        Assert.assertEquals(Integer.MAX_VALUE, result);
        result = jexl.createScript(Integer.toString(Integer.MIN_VALUE + 1)).execute(null);
        Assert.assertEquals(Integer.MIN_VALUE + 1, result);
        result = jexl.createScript(Integer.toString(Integer.MIN_VALUE)).execute(null);
        Assert.assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    public void test117() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlExpression e = jexl.createExpression("TIMESTAMP > 20100102000000");
        final JexlContext ctx = new MapContext();
        ctx.set("TIMESTAMP", new Long("20100103000000"));
        final Object result = e.evaluate(ctx);
        Assert.assertTrue((Boolean) result);
    }

    public static class Foo125 {
        public String method() {
            return "OK";
        }

        public String total(final String tt) {
            return "total " + tt;
        }
    }

    public static class Foo125Context extends ObjectContext<Foo125> {
        public Foo125Context(final JexlEngine engine, final Foo125 wrapped) {
            super(engine, wrapped);
        }
    }

    @Test
    public void test125() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlExpression e = jexl.createExpression("method()");
        final JexlContext jc = new Foo125Context(jexl, new Foo125());
        Assert.assertEquals("OK", e.evaluate(jc));
    }

    @Test
    public void test130a() throws Exception {
        final String myName = "Test.Name";
        final Object myValue = "Test.Value";

        final JexlEngine myJexlEngine = new Engine();
        final MapContext myMapContext = new MapContext();
        myMapContext.set(myName, myValue);

        final Object myObjectWithTernaryConditional = myJexlEngine.createScript(myName + "?:null").execute(myMapContext);
        Assert.assertEquals(myValue, myObjectWithTernaryConditional);
    }

    @Test
    public void test130b() throws Exception {
        final String myName = "Test.Name";
        final Object myValue = new Object() {
            @Override
            public String toString() {
                return "Test.Value";
            }
        };

        final JexlEngine myJexlEngine = new Engine();
        final MapContext myMapContext = new MapContext();
        myMapContext.set(myName, myValue);

        final Object myObjectWithTernaryConditional = myJexlEngine.createScript(myName + "?:null").execute(myMapContext);
        Assert.assertEquals(myValue, myObjectWithTernaryConditional);
    }

    @Test
    public void test135() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlContext jc = new MapContext();
        JexlScript script;
        Object result;
        final Map<Integer, Object> foo = new HashMap<Integer, Object>();
        foo.put(3, 42);
        jc.set("state", foo);

        script = jexl.createScript("var y = state[3]; y");
        result = script.execute(jc, foo);
        Assert.assertEquals(42, result);

        jc.set("a", 3);
        script = jexl.createScript("var y = state[a]; y");
        result = script.execute(jc, foo);
        Assert.assertEquals(42, result);

        jc.set("a", 2);
        script = jexl.createScript("var y = state[a + 1]; y");
        result = script.execute(jc, foo);
        Assert.assertEquals(42, result);

        jc.set("a", 2);
        jc.set("b", 1);
        script = jexl.createScript("var y = state[a + b]; y");
        result = script.execute(jc, foo);
        Assert.assertEquals(42, result);

        script = jexl.createScript("var y = state[3]; y", "state");
        result = script.execute(null, foo, 3);
        Assert.assertEquals(42, result);

        script = jexl.createScript("var y = state[a]; y", "state", "a");
        result = script.execute(null, foo, 3);
        Assert.assertEquals(42, result);

        script = jexl.createScript("var y = state[a + 1]; y", "state", "a");
        result = script.execute(null, foo, 2);
        Assert.assertEquals(42, result);

        script = jexl.createScript("var y = state[a + b]; y", "state", "a", "b");
        result = script.execute(null, foo, 2, 1);
        Assert.assertEquals(42, result);
    }

    @Test
    public void test136() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlContext jc = new MapContext();
        JexlScript script;
        JexlExpression expr;
        Object result;

        script = jexl.createScript("var x = $TAB[idx]; return x;", "idx");
        jc.set("fn01", script);

        script = jexl.createScript("$TAB = { 1:11, 2:22, 3:33}; IDX=2;");
        script.execute(jc);

        expr = jexl.createExpression("fn01(IDX)");
        result = expr.evaluate(jc);
        Assert.assertEquals("EXPR01 result", 22, result);
    }

//    @Test
//    public void test138() throws Exception {
//        MapContext ctxt = new MapContext();
//        ctxt.set("tz", java.util.TimeZone.class);
//        String source = ""
//                + "var currentDate = new('java.util.Date');"
//                +  "var gmt = tz.getTimeZone('GMT');"
//                +  "var cet = tz.getTimeZone('CET');"
//                +  "var calendarGMT = new('java.util.GregorianCalendar' , gmt);"
//                +  "var calendarCET = new('java.util.GregorianCalendar', cet);"
//                +  "var diff = calendarCET.getTime() - calendarGMT.getTime();"
//                + "return diff";
//
//        JexlEngine jexl = new Engine();
//        JexlScript script = jexl.createScript(source);
//        Object result = script.execute(ctxt);
//        Assert.Assert.assertNotNull(result);
//    }
    @Test
    public void test143() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlContext jc = new MapContext();
        JexlScript script;
        Object result;

        script = jexl.createScript("var total = 10; total = (total - ((x < 3)? y : z)) / (total / 10); total", "x", "y", "z");
        result = script.execute(jc, 2, 2, 1);
        Assert.assertEquals(8, result);
        script = jexl.createScript("var total = 10; total = (total - ((x < 3)? y : 1)) / (total / 10); total", "x", "y", "z");
        result = script.execute(jc, 2, 2, 1);
        Assert.assertEquals(8, result);
    }

    @Test
    public void test144() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlContext jc = new MapContext();
        JexlScript script;
        Object result;
        script = jexl.createScript("var total = 10; total('tt')");
        try {
            result = script.execute(jc);
            Assert.fail("total() is not solvable");
        } catch (final JexlException.Method ambiguous) {
            Assert.assertEquals("total", ambiguous.getMethod());
        }
    }

    /**
     * Test cases for empty array assignment.
     */
    public static class Quux144 {
        String[] arr;
        String[] arr2;

        public Quux144() {
        }

        public String[] getArr() {
            return arr;
        }

        public String[] getArr2() {
            return arr2;
        }

        public void setArr(final String[] arr) {
            this.arr = arr;
        }

        public void setArr2(final String[] arr2) {
            this.arr2 = arr2;
        }

        // Overloaded setter with different argument type.
        public void setArr2(final Integer[] arr2) {
        }
    }

    @Test
    public void test144a() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlContext jc = new MapContext();
        jc.set("quuxClass", Quux144.class);
        final JexlExpression create = jexl.createExpression("quux = new(quuxClass)");
        JexlExpression assignArray = jexl.createExpression("quux.arr = [ 'hello', 'world' ]");
        final JexlExpression checkArray = jexl.createExpression("quux.arr");

        // test with a string
        final Quux144 quux = (Quux144) create.evaluate(jc);
        Assert.assertNotNull("quux is null", quux);

        // test with a nonempty string array
        Object o = assignArray.evaluate(jc);
        Assert.assertEquals("Result is not a string array", String[].class, o.getClass());
        o = checkArray.evaluate(jc);
        Assert.assertEquals("The array elements are equal", Arrays.asList("hello", "world"), Arrays.asList((String[]) o));

        // test with a null array
        assignArray = jexl.createExpression("quux.arr = null");
        o = assignArray.evaluate(jc);
        Assert.assertNull("Result is not null", o);
        o = checkArray.evaluate(jc);
        Assert.assertNull("Result is not null", o);

        // test with an empty array
        assignArray = jexl.createExpression("quux.arr = [ ]");
        o = assignArray.evaluate(jc);
        Assert.assertNotNull("Result is null", o);
        o = checkArray.evaluate(jc);
        Assert.assertEquals("The array elements are not equal", Collections.emptyList(), Arrays.asList((String[]) o));
        Assert.assertEquals("The array size is not zero", 0, ((String[]) o).length);

        // test with an empty array on the overloaded setter for different types.
        // so, the assignment should fail with logging 'The ambiguous property, arr2, should have failed.'
        try {
            assignArray = jexl.createExpression("quux.arr2 = [ ]");
            o = assignArray.evaluate(jc);
            Assert.fail("The arr2 property shouldn't be set due to its ambiguity (overloaded setters with different types).");
        } catch (final JexlException.Property e) {
            //System.out.println("Expected ambiguous property setting exception: " + e);
        }
        Assert.assertNull("The arr2 property value should remain as null, not an empty array.", quux.arr2);
    }

    @Test
    public void test147b() throws Exception {
        final String[] scripts = {"var x = new ('java.util.HashMap'); x.one = 1; x.two = 2; x.one", // results to 1
            "x = new ('java.util.HashMap'); x.one = 1; x.two = 2; x.one",// results to 1
            "x = new ('java.util.HashMap'); x.one = 1; x.two = 2; x['one']",//results to 1
            "var x = new ('java.util.HashMap'); x.one = 1; x.two = 2; x['one']"// result to null?
        };

        final JexlEngine jexl = new Engine();
        final JexlContext jc = new MapContext();
        for (final String s : scripts) {
            final Object o = jexl.createScript(s).execute(jc);
            Assert.assertEquals(1, o);
        }
    }

    @Test
    public void test147c() throws Exception {
        final String[] scripts = {
            "var x = new ('java.util.HashMap'); x.one = 1; x.two = 2; x.one",
            "x = new ('java.util.HashMap'); x.one = 1; x.two = 2; x.one",
            "x = new ('java.util.HashMap'); x.one = 1; x.two = 2; x['one']",
            "var x = new ('java.util.HashMap'); x.one = 1; x.two = 2; x['one']"
        };
        final JexlEngine jexl = new Engine();
        for (final String s : scripts) {
            final JexlContext jc = new MapContext();
            final Object o = jexl.createScript(s).execute(jc);
            Assert.assertEquals(1, o);
        }
    }

    @Test
    public void test5115a() throws Exception {
        final String str = "{\n"
                + "  var x = \"A comment\";\n"
                + "  var y = \"A comment\";\n"
                + "}";
        final JexlEngine jexl = new Engine();
        final JexlScript s = jexl.createScript(str);
    }

    @Test
    public void test5115b() throws Exception {
        final String str = "{\n"
                + "  var x = \"A comment\";\n"
                + "}";
        final JexlEngine jexl = new Engine();
        final JexlScript s = jexl.createScript(str);
    }

    static final String TESTA = "src/test/scripts/testA.jexl";

    @Test
    public void test5115c() throws Exception {
        final URL testUrl = new File(TESTA).toURI().toURL();
        final JexlEngine jexl = new Engine();
        final JexlScript s = jexl.createScript(testUrl);
    }

    public static class Utils {
        public <T> List<T> asList(final T[] array) {
            return Arrays.asList(array);
        }

        public List<Integer> asList(final int[] array) {
            final List<Integer> l = new ArrayList<Integer>(array.length);
            for (final int i : array) {
                l.add(i);
            }
            return l;
        }
    }

    @Test
    public void test148a() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlContext jc = new MapContext();
        jc.set("u", new Utils());

        String src = "u.asList(['foo', 'bar'])";
        JexlScript e = jexl.createScript(src);
        Object o = e.execute(jc);
        Assert.assertTrue(o instanceof List);
        Assert.assertEquals(Arrays.asList("foo", "bar"), o);

        src = "u.asList([1, 2])";
        e = jexl.createScript(src);
        o = e.execute(jc);
        Assert.assertTrue(o instanceof List);
        Assert.assertEquals(Arrays.asList(1, 2), o);
    }

    @Test
    public void test155() throws Exception {
        final JexlEngine jexlEngine = new Engine();
        final JexlExpression jexlExpresssion = jexlEngine.createExpression("first.second.name");
        final JexlContext jc = new MapContext();
        jc.set("first.second.name", "RIGHT");
        jc.set("name", "WRONG");
        final Object value = jexlExpresssion.evaluate(jc);
        Assert.assertEquals("RIGHT", value.toString());
    }

    public static class Question42 extends MapContext {
        public String functionA(final String arg) {
            return "a".equals(arg) ? "A" : "";
        }

        public String functionB(final String arg) {
            return "b".equals(arg) ? "B" : "";
        }

        public String functionC(final String arg) {
            return "c".equals(arg) ? "C" : "";
        }

        public String functionD(final String arg) {
            return "d".equals(arg) ? "D" : "";
        }
    }

    public static class Arithmetic42 extends JexlArithmetic {
        public Arithmetic42() {
            super(false);
        }

        public Object and(final String lhs, final String rhs) {
            if (rhs.isEmpty()) {
                return "";
            }
            if (lhs.isEmpty()) {
                return "";
            }
            return lhs + rhs;
        }

        public Object or(final String lhs, final String rhs) {
            if (rhs.isEmpty()) {
                return lhs;
            }
            if (lhs.isEmpty()) {
                return rhs;
            }
            return lhs + rhs;
        }
    }

    @Test
    public void testQuestion42() throws Exception {
        final JexlEngine jexl = new JexlBuilder().arithmetic(new Arithmetic42()).create();
        final JexlContext jc = new Question42();

        final String str0 = "(functionA('z') | functionB('b')) &  (functionC('c') |  functionD('d') ) ";
        final JexlExpression expr0 = jexl.createExpression(str0);
        final Object value0 = expr0.evaluate(jc);
        Assert.assertEquals("BCD", value0);

        final String str1 = "(functionA('z') & functionB('b')) |  (functionC('c') &  functionD('d') ) ";
        final JexlExpression expr1 = jexl.createExpression(str1);
        final Object value1 = expr1.evaluate(jc);
        Assert.assertEquals("CD", value1);
    }

    @Test
    public void test179() throws Exception {
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().create();
        final String src = "x = new ('java.util.HashSet'); x.add(1); x";
        final JexlScript e = jexl.createScript(src);
        final Object o = e.execute(jc);
        Assert.assertTrue(o instanceof Set);
        Assert.assertTrue(((Set) o).contains(1));
    }

    public static class C192 {
        public C192() {
        }

        public static Integer callme(final Integer n) {
            if (n == null) {
                return null;
            }
            return n >= 0 ? 42 : -42;
        }

        public static Object kickme() {
            return C192.class;
        }
    }

    @Test
    public void test192() throws Exception {
        final JexlContext jc = new MapContext();
        jc.set("x.y.z", C192.class);
        final JexlEngine jexl = new JexlBuilder().create();
        JexlExpression js0 = jexl.createExpression("x.y.z.callme(t)");
        jc.set("t", null);
        Assert.assertNull(js0.evaluate(jc));
        jc.set("t", 10);
        Assert.assertEquals(42, js0.evaluate(jc));
        jc.set("t", -10);
        Assert.assertEquals(-42, js0.evaluate(jc));
        jc.set("t", null);
        Assert.assertNull(js0.evaluate(jc));
        js0 = jexl.createExpression("x.y.z.kickme().callme(t)");
        jc.set("t", null);
        Assert.assertNull(js0.evaluate(jc));
        jc.set("t", 10);
        Assert.assertEquals(42, js0.evaluate(jc));
        jc.set("t", -10);
        Assert.assertEquals(-42, js0.evaluate(jc));
        jc.set("t", null);
        Assert.assertNull(js0.evaluate(jc));
    }

    @Test
    public void test199() throws Exception {
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic(false)).create();

        final JexlScript e = jexl.createScript("(x, y)->{ x + y }");
        Object r = e.execute(jc, true, "EURT");
        Assert.assertEquals("trueEURT", r);
        r = e.execute(jc, "ELSAF", false);
        Assert.assertEquals("ELSAFfalse", r);
    }

}
