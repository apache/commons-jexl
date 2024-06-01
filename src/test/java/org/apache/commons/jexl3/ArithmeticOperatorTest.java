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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.commons.jexl3.junit.Asserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the startsWith, endsWith, match and range operators.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ArithmeticOperatorTest extends JexlTestCase {
    public static class Aggregate {
        public static int sum(final Iterable<Integer> ii) {
            int sum = 0;
            for(final Integer i : ii) {
                sum += i;
            }
            return sum;
        }
        private Aggregate() {}
    }
    public static class DateArithmetic extends JexlArithmetic {
        DateArithmetic(final boolean flag) {
            super(flag);
        }

        public Object arrayGet(final Date date, final String identifier) {
            return getDateValue(date, identifier);
        }

        public Object arraySet(final Date date, final String identifier, final Object value) throws Exception {
            return setDateValue(date, identifier, value);
        }

        protected Object getDateValue(final Date date, final String key) {
            try {
                final Calendar cal = Calendar.getInstance(UTC);
                cal.setTime(date);
                if ("yyyy".equals(key)) {
                    return cal.get(Calendar.YEAR);
                }
                if ("MM".equals(key)) {
                    return cal.get(Calendar.MONTH) + 1;
                }
                if ("dd".equals(key)) {
                    return cal.get(Calendar.DAY_OF_MONTH);
                }
                // Otherwise treat as format mask
                final SimpleDateFormat df = new SimpleDateFormat(key); //, dfs);
                return df.format(date);

            } catch (final Exception ex) {
                return null;
            }
        }

        public Date multiply(final Date d0, final Date d1) {
            throw new ArithmeticException("unsupported");
        }

        public Date now() {
            return new Date(System.currentTimeMillis());
        }

        public Object propertyGet(final Date date, final String identifier) {
            return getDateValue(date, identifier);
        }

        public Object propertySet(final Date date, final String identifier, final Object value) throws Exception {
            return setDateValue(date, identifier, value);
        }

        protected Object setDateValue(final Date date, final String key, final Object value) throws Exception {
            final Calendar cal = Calendar.getInstance(UTC);
            cal.setTime(date);
            if ("yyyy".equals(key)) {
                cal.set(Calendar.YEAR, toInteger(value));
            } else if ("MM".equals(key)) {
                cal.set(Calendar.MONTH, toInteger(value) - 1);
            } else if ("dd".equals(key)) {
                cal.set(Calendar.DAY_OF_MONTH, toInteger(value));
            }
            date.setTime(cal.getTimeInMillis());
            return date;
        }
    }

    public static class DateContext extends MapContext {
        private Locale locale = Locale.US;

        public String format(final Date date, final String fmt) {
            final SimpleDateFormat sdf = new SimpleDateFormat(fmt, locale);
            sdf.setTimeZone(UTC);
            return sdf.format(date);
        }

        public String format(final Number number, final String fmt) {
            return new DecimalFormat(fmt).format(number);
        }

        void setLocale(final Locale l10n) {
            this.locale = l10n;
        }
    }

    public static class IterableContainer implements Iterable<Integer> {
        private final SortedSet<Integer> values;

        public IterableContainer(final int[] is) {
            values = new TreeSet<>();
            for (final int value : is) {
                values.add(value);
            }
        }

        public boolean contains(final int i) {
            return values.contains(i);
        }

        public boolean contains(final int[] i) {
            for(final int ii : i) {
                if (!values.contains(ii)) {
                    return false;
                }
            }
            return true;
        }

        public boolean endsWith(final int i) {
            return values.last().equals(i);
        }

        public boolean endsWith(final int[] i) {
            final SortedSet<Integer> sw =  values.tailSet(values.size() - i.length);
            int n = 0;
            for (final Integer value : sw) {
                if (!value.equals(i[n++])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Iterator<Integer> iterator() {
            return values.iterator();
        }

        public boolean startsWith(final int i) {
            return values.first().equals(i);
        }
        public boolean startsWith(final int[] i) {
            final SortedSet<Integer> sw = values.headSet(i.length);
            int n = 0;
            for (final Integer value : sw) {
                if (!value.equals(i[n++])) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class MatchingContainer {
        private final Set<Integer> values;

        public MatchingContainer(final int[] is) {
            values = new HashSet<>();
            for (final int value : is) {
                values.add(value);
            }
        }

        public boolean contains(final int value) {
            return values.contains(value);
        }
    }

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private Asserter asserter;

    /**
     * Create the named test.
     */
    public ArithmeticOperatorTest() {
        super("ArithmeticOperatorTest");
    }

    @BeforeEach
    @Override
    public void setUp() {
        asserter = new Asserter(JEXL);
        asserter.setStrict(false);
    }

    @Test public void test373a() {
        testSelfAssignOperators("y.add(x++)", 42, 42, 43);
    }

    @Test public void test373b() {
        testSelfAssignOperators("y.add(++x)", 42, 43, 43);
    }

    @Test public void test373c() {
        testSelfAssignOperators("y.add(x--)", 42, 42, 41);
    }

    @Test public void test373d() {
        testSelfAssignOperators("y.add(--x)", 42, 41, 41);
    }

    @Test
    public void test391() throws Exception {
        // with literals
        for(final String src : Arrays.asList(
                "2 =~ [1, 2, 3, 4]",
                "[2, 3] =~ [1, 2, 3, 4]",
                "[2, 3,...] =~ [1, 2, 3, 4]",
                "3 =~ [1, 2, 3, 4,...]",
                "[2, 3] =~ [1, 2, 3, 4,...]",
                "[2, 3,...] =~ [1, 2, 3, 4,...]")) {
            asserter.assertExpression(src, Boolean.TRUE);
        }
        // with variables
        final int[] ic = {1, 2,  3, 4};
        final List<Integer> iic = new ArrayList<>();
        for(final int v : ic) { iic.add(v); }
        final int[] iv = {2, 3};
        final List<Integer> iiv = new ArrayList<>();
        for(final int v : iv) { iiv.add(v); }
        final String src = "(x,y) -> x =~ y ";
        for(final Object v : Arrays.asList(iv, iiv, 2)) {
            for(final Object c : Arrays.asList(ic, iic)) {
                asserter.assertExpression(src, Boolean.TRUE, v, c);
            }
        }
    }

    @Test
    public void testDateArithmetic() throws Exception {
        final Date d = new Date();
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().cache(32).arithmetic(new DateArithmetic(true)).create();
        final JexlScript expr0 = jexl.createScript("date.yyyy = 1969; date.MM=7; date.dd=20; ", "date");
        Object value0 = expr0.execute(jc, d);
        assertNotNull(value0);
        value0 = d;
        //d = new Date();
        assertEquals(1969, jexl.createScript("date.yyyy", "date").execute(jc, value0));
        assertEquals(7, jexl.createScript("date.MM", "date").execute(jc, value0));
        assertEquals(20, jexl.createScript("date.dd", "date").execute(jc, value0));
    }

    @Test
    public void testFormatArithmetic() throws Exception {
        final Calendar cal = Calendar.getInstance(UTC);
        cal.set(1969, Calendar.AUGUST, 20);
        final Date x0 = cal.getTime();
        final String y0 =  "MM/yy/dd";
        final Number x1 = 42.12345;
        final String y1 = "##0.##";
        final DateContext jc = new DateContext();
        final JexlEngine jexl = new JexlBuilder().cache(32).arithmetic(new DateArithmetic(true)).create();
        final JexlScript expr0 = jexl.createScript("x.format(y)", "x", "y");
        Object value10 = expr0.execute(jc, x0, y0);
        final Object value20 = expr0.execute(jc, x0, y0);
        assertEquals(value10, value20);
        Object value11 = expr0.execute(jc, x1, y1);
        final Object value21 = expr0.execute(jc, x1, y1);
        assertEquals(value11, value21);
        value10 = expr0.execute(jc, x0, y0);
        assertEquals(value10, value20);
        value11 = expr0.execute(jc, x1, y1);
        assertEquals(value11, value21);
        value10 = expr0.execute(jc, x0, y0);
        assertEquals(value10, value20);
        value11 = expr0.execute(jc, x1, y1);
        assertEquals(value11, value21);

        JexlScript expr1 = jexl.createScript("format(x, y)", "x", "y");
        value10 = expr1.execute(jc, x0, y0);
        assertEquals(value10, value20);
        Object s0 = expr1.execute(jc, x0, "EEE dd MMM yyyy");
        assertEquals("Wed 20 Aug 1969", s0);
        jc.setLocale(Locale.FRANCE);
        s0 = expr1.execute(jc, x0, "EEE dd MMM yyyy");
        assertEquals("mer. 20 ao\u00fbt 1969", s0);

        expr1 = jexl.createScript("format(now(), y)", "y");
        final Object n0 = expr1.execute(jc, y0);
        assertNotNull(n0);
        expr1 = jexl.createScript("now().format(y)", "y");
        final Object n1 = expr1.execute(jc, y0);
        assertNotNull(n0);
        assertEquals(n0, n1);
    }

    @Test
    public void testFormatArithmeticJxlt() throws Exception {
        final Map<String, Object> ns = new HashMap<>();
        ns.put("calc", Aggregate.class);
        final Calendar cal = Calendar.getInstance(UTC);
        cal.set(1969, Calendar.AUGUST, 20);
        final Date x0 = cal.getTime();
        final String y0 =  "yyyy-MM-dd";
        final DateContext jc = new DateContext();
        final JexlEngine jexl = new JexlBuilder().cache(32).namespaces(ns).arithmetic(new DateArithmetic(true)).create();
        final JxltEngine jxlt = jexl.createJxltEngine();

        JxltEngine.Template expr0 = jxlt.createTemplate("${x.format(y)}", "x", "y");
        StringWriter strw = new StringWriter();
        expr0.evaluate(jc, strw, x0, y0);
        String strws = strw.toString();
        assertEquals("1969-08-20", strws);

        expr0 = jxlt.createTemplate("${calc:sum(x .. y)}", "x", "y");
        strw = new StringWriter();
        expr0.evaluate(jc, strw, 1, 3);
        strws = strw.toString();
        assertEquals("6", strws);

        final JxltEngine.Template expr1 = jxlt.createTemplate("${jexl:include(s, x, y)}", "s", "x", "y");
        strw = new StringWriter();
        expr1.evaluate(jc, strw, expr0, 1, 3);
        strws = strw.toString();
        assertEquals("6", strws);

        expr0 = jxlt.createTemplate("${now().format(y)}", "y");
        strw = new StringWriter();
        expr0.evaluate(jc, strw, y0);
        strws = strw.toString();
        assertNotNull(strws);
    }

    @Test
    public void testIncrementOperatorOnNull() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(false).create();
        JexlScript script;
        Object result;
        script = jexl.createScript("var i = null; ++i");
        result = script.execute(null);
        assertEquals(1, result);
        script = jexl.createScript("var i = null; --i");
        result = script.execute(null);
        assertEquals(-1, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInterval() throws Exception {
        final Map<String, Object> ns = new HashMap<>();
        ns.put("calc", Aggregate.class);
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        JexlScript script;
        Object result;

        script = jexl.createScript("1 .. 3");
        result = script.execute(null);
        assertTrue(result instanceof Iterable<?>);
        Iterator<Integer> ii = ((Iterable<Integer>) result).iterator();
        assertEquals(Integer.valueOf(1), ii.next());
        assertEquals(Integer.valueOf(2), ii.next());
        assertEquals(Integer.valueOf(3), ii.next());

        script = jexl.createScript("(4 - 3) .. (9 / 3)");
        result = script.execute(null);
        assertTrue(result instanceof Iterable<?>);
        ii = ((Iterable<Integer>) result).iterator();
        assertEquals(Integer.valueOf(1), ii.next());
        assertEquals(Integer.valueOf(2), ii.next());
        assertEquals(Integer.valueOf(3), ii.next());

        // sum of 1, 2, 3
        script = jexl.createScript("var x = 0; for(var y : ((5 - 4) .. (12 / 4))) { x = x + y }; x");
        result = script.execute(null);
        assertEquals(Integer.valueOf(6), result);

        script = jexl.createScript("calc:sum(1 .. 3)");
        result = script.execute(null);
        assertEquals(Integer.valueOf(6), result);

        script = jexl.createScript("calc:sum(-3 .. 3)");
        result = script.execute(null);
        assertEquals(Integer.valueOf(0), result);
    }

    @Test
    public void testMatch() throws Exception {
        // check in/not-in on array, list, map, set and duck-type collection
        final int[] ai = {2, 4, 42, 54};
        final List<Integer> al = new ArrayList<>();
        for (final int i : ai) {
            al.add(i);
        }
        final Map<Integer, String> am = new HashMap<>();
        am.put(2, "two");
        am.put(4, "four");
        am.put(42, "forty-two");
        am.put(54, "fifty-four");
        final MatchingContainer ad = new MatchingContainer(ai);
        final IterableContainer ic = new IterableContainer(ai);
        final Set<Integer> as = ad.values;
        final Object[] vars = {ai, al, am, ad, as, ic};

        for (final Object var : vars) {
            asserter.setVariable("container", var);
            for (final int x : ai) {
                asserter.setVariable("x", x);
                asserter.assertExpression("x =~ container", Boolean.TRUE);
            }
            asserter.setVariable("x", 169);
            asserter.assertExpression("x !~ container", Boolean.TRUE);
        }
    }

    @Test
    public void testNotStartsEndsWith() throws Exception {
        asserter.setVariable("x", "foobar");
        asserter.assertExpression("x !^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x !$ 'foo'", Boolean.TRUE);
        asserter.setVariable("x", "barfoo");
        asserter.assertExpression("x !^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x !$ 'foo'", Boolean.FALSE);

        final int[] ai = {2, 4, 42, 54};
        final IterableContainer ic = new IterableContainer(ai);
        asserter.setVariable("x", ic);
        asserter.assertExpression("x !^ 2", Boolean.FALSE);
        asserter.assertExpression("x !$ 54", Boolean.FALSE);
        asserter.assertExpression("x !^ 4", Boolean.TRUE);
        asserter.assertExpression("x !$ 42", Boolean.TRUE);
        asserter.assertExpression("x !^ [2, 4]", Boolean.FALSE);
        asserter.assertExpression("x !^ [42, 54]", Boolean.FALSE);
    }

    @Test
    public void testNotStartsEndsWithString() throws Exception {
        asserter.setVariable("x", "foobar");
        asserter.assertExpression("x !^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x !$ 'foo'", Boolean.TRUE);
        asserter.setVariable("x", "barfoo");
        asserter.assertExpression("x !^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x !$ 'foo'", Boolean.FALSE);
    }

    @Test
    public void testNotStartsEndsWithStringBuilder() throws Exception {
        asserter.setVariable("x", new StringBuilder("foobar"));
        asserter.assertExpression("x !^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x !$ 'foo'", Boolean.TRUE);
        asserter.setVariable("x", new StringBuilder("barfoo"));
        asserter.assertExpression("x !^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x !$ 'foo'", Boolean.FALSE);
    }

    @Test
    public void testNotStartsEndsWithStringDot() throws Exception {
        asserter.setVariable("x.y", "foobar");
        asserter.assertExpression("x.y !^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x.y !$ 'foo'", Boolean.TRUE);
        asserter.setVariable("x.y", "barfoo");
        asserter.assertExpression("x.y !^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x.y !$ 'foo'", Boolean.FALSE);
    }

    @Test
    public void testOperatorError() throws Exception {
        testOperatorError(true);
        testOperatorError(false);
    }

    private void testOperatorError(final boolean silent) throws Exception {
        final CaptureLog log = new CaptureLog();
        final DateContext jc = new DateContext();
        final Date d = new Date();
        final JexlEngine jexl = new JexlBuilder().logger(log).strict(true).silent(silent).cache(32)
                                           .arithmetic(new DateArithmetic(true)).create();
        final JexlScript expr0 = jexl.createScript("date * date", "date");
        try {
            final Object value0 = expr0.execute(jc, d);
            if (!silent) {
                fail("should have failed");
            } else {
                assertEquals(1, log.count("warn"));
            }
        } catch (final JexlException.Operator xop) {
            assertEquals("*", xop.getSymbol());
        }
        if (!silent) {
            assertEquals(0, log.count("warn"));
        }
    }

    @Test
    public void testRegexp() throws Exception {
        asserter.setVariable("str", "abc456");
        asserter.assertExpression("str =~ '.*456'", Boolean.TRUE);
        asserter.assertExpression("str !~ 'ABC.*'", Boolean.TRUE);
        asserter.setVariable("match", "abc.*");
        asserter.setVariable("nomatch", ".*123");
        asserter.assertExpression("str =~ match", Boolean.TRUE);
        asserter.assertExpression("str !~ match", Boolean.FALSE);
        asserter.assertExpression("str !~ nomatch", Boolean.TRUE);
        asserter.assertExpression("str =~ nomatch", Boolean.FALSE);
        asserter.setVariable("match", new StringBuilder("abc.*"));
        asserter.setVariable("nomatch", new StringBuilder(".*123"));
        asserter.assertExpression("str =~ match", Boolean.TRUE);
        asserter.assertExpression("str !~ match", Boolean.FALSE);
        asserter.assertExpression("str !~ nomatch", Boolean.TRUE);
        asserter.assertExpression("str =~ nomatch", Boolean.FALSE);
        asserter.setVariable("match", java.util.regex.Pattern.compile("abc.*"));
        asserter.setVariable("nomatch", java.util.regex.Pattern.compile(".*123"));
        asserter.assertExpression("str =~ match", Boolean.TRUE);
        asserter.assertExpression("str !~ match", Boolean.FALSE);
        asserter.assertExpression("str !~ nomatch", Boolean.TRUE);
        asserter.assertExpression("str =~ nomatch", Boolean.FALSE);
        // check the in/not-in variant
        asserter.assertExpression("'a' =~ ['a','b','c','d','e','f']", Boolean.TRUE);
        asserter.assertExpression("'a' !~ ['a','b','c','d','e','f']", Boolean.FALSE);
        asserter.assertExpression("'z' =~ ['a','b','c','d','e','f']", Boolean.FALSE);
        asserter.assertExpression("'z' !~ ['a','b','c','d','e','f']", Boolean.TRUE);
    }

    @Test
    public void testRegexp2() throws Exception {
        asserter.setVariable("str", "abc456");
        asserter.assertExpression("str =~ ~/.*456/", Boolean.TRUE);
        asserter.assertExpression("str !~ ~/ABC.*/", Boolean.TRUE);
        asserter.assertExpression("str =~ ~/abc\\d{3}/", Boolean.TRUE);
        // legacy, deprecated
        asserter.assertExpression("matches(str, ~/.*456/)", Boolean.TRUE);
        asserter.setVariable("str", "4/6");
        asserter.assertExpression("str =~ ~/\\d\\/\\d/", Boolean.TRUE);
    }
    void testSelfAssignOperators(final String text, final int x, final int y0, final int x0) {
        //String text = "y.add(x++)";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(text);
        final JexlContext context = new MapContext();
        context.set("x", x);
        final List<Number> y = new ArrayList<>();
        context.set("y", y);
        final Object result = script.execute(context);
        assertEquals(x0, context.get("x"), "x0");
        assertEquals(y0, y.get(0), "y0");
    }
    @Test
    public void testStartsEndsWith() throws Exception {
        asserter.setVariable("x", "foobar");
        asserter.assertExpression("x =^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x =$ 'foo'", Boolean.FALSE);
        asserter.setVariable("x", "barfoo");
        asserter.assertExpression("x =^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x =$ 'foo'", Boolean.TRUE);

        final int[] ai = {2, 4, 42, 54};
        final IterableContainer ic = new IterableContainer(ai);
        asserter.setVariable("x", ic);
        asserter.assertExpression("x =^ 2", Boolean.TRUE);
        asserter.assertExpression("x =$ 54", Boolean.TRUE);
        asserter.assertExpression("x =^ 4", Boolean.FALSE);
        asserter.assertExpression("x =$ 42", Boolean.FALSE);
        asserter.assertExpression("x =^ [2, 4]", Boolean.TRUE);
        asserter.assertExpression("x =^ [42, 54]", Boolean.TRUE);
    }
    @Test
    public void testStartsEndsWithString() throws Exception {
        asserter.setVariable("x", "foobar");
        asserter.assertExpression("x =^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x =$ 'foo'", Boolean.FALSE);
        asserter.setVariable("x", "barfoo");
        asserter.assertExpression("x =^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x =$ 'foo'", Boolean.TRUE);
    }

    @Test
    public void testStartsEndsWithStringBuilder() throws Exception {
        asserter.setVariable("x", new StringBuilder("foobar"));
        asserter.assertExpression("x =^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x =$ 'foo'", Boolean.FALSE);
        asserter.setVariable("x", new StringBuilder("barfoo"));
        asserter.assertExpression("x =^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x =$ 'foo'", Boolean.TRUE);
    }

    @Test
    public void testStartsEndsWithStringDot() throws Exception {
        asserter.setVariable("x.y", "foobar");
        asserter.assertExpression("x.y =^ 'foo'", Boolean.TRUE);
        asserter.assertExpression("x.y =$ 'foo'", Boolean.FALSE);
        asserter.setVariable("x.y", "barfoo");
        asserter.assertExpression("x.y =^ 'foo'", Boolean.FALSE);
        asserter.assertExpression("x.y =$ 'foo'", Boolean.TRUE);
    }

}
