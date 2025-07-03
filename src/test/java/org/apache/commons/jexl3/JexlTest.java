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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jexl3.parser.Parser;
import org.junit.jupiter.api.Test;

/**
 * Simple test cases.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public final class JexlTest extends JexlTestCase {
    public static final class Duck {
        int user = 10;

        @SuppressWarnings("boxing")
        public Integer get(final String val) {
            switch (val) {
            case "zero":
                return 0;
            case "one":
                return 1;
            case "user":
                return user;
            default:
                break;
            }
            return -1;
        }

        @SuppressWarnings("boxing")
        public void set(final String val, final Object value) {
            if ("user".equals(val)) {
                if ("zero".equals(value)) {
                    user = 0;
                } else if ("one".equals(value)) {
                    user = 1;
                } else {
                    user = value instanceof Integer ? (Integer) value : -1;
                }
            }
        }
    }
    static final String METHOD_STRING = "Method string";

    static final String GET_METHOD_STRING = "GetMethod string";

    public JexlTest() {
        super("JexlTest",
                new JexlBuilder()
                        .strict(false)
                        .imports(Arrays.asList("java.lang","java.math"))
                        .permissions(null)
                        .cache(128).create());
    }

    /**
     * Asserts that the given expression returns the given value when applied to the
     * given context
     */
    protected void assertExpression(final JexlContext jc, final String expression, final Object expected) {
        final JexlExpression e = JEXL.createExpression(expression);
        final Object actual = e.evaluate(jc);
        assertEquals(expected, actual, expression);
    }

    @Test
    public void testAntPropertiesWithMethods() {
        final JexlContext jc = new MapContext();
        final String value = "Stinky Cheese";
        jc.set("maven.bob.food", value);
        assertExpression(jc, "maven.bob.food.length()", Integer.valueOf(value.length()));
        assertExpression(jc, "empty(maven.bob.food)", Boolean.FALSE);
        assertExpression(jc, "size(maven.bob.food)", Integer.valueOf(value.length()));
        assertExpression(jc, "maven.bob.food + ' is good'", value + " is good");

        // DG: Note the following ant properties don't work
//        String version = "1.0.3";
//        jc.set("commons-logging", version);
//        assertExpression(jc, "commons-logging", version);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testArray() {
        final int[] array = {100, 101, 102};
        final JexlEngine jexl = JEXL;
        final JexlContext jc = new MapContext();
        jc.set("array", array);
        JexlExpression expr;
        Object result;
        expr = jexl.createExpression("array.1");
        result = expr.evaluate(jc);
        assertEquals(101, result);
        expr = jexl.createExpression("array[1] = 1010");
        result = expr.evaluate(jc);
        assertEquals(1010, result, expr::toString);
        expr = jexl.createExpression("array.0");
        result = expr.evaluate(jc);
        assertEquals(100, result, expr::toString);
    }

    /**
     * Test assignment.
     * @throws Exception
     */
    @Test
    public void testAssignment() {
        final JexlContext jc = new MapContext();
        jc.set("aString", "Hello");
        final Foo foo = new Foo();
        jc.set("foo", foo);
        final Parser parser = new Parser(";");
        parser.parse(null, new JexlFeatures().register(false), "aString = 'World';", null);

        assertExpression(jc, "hello = 'world'", "world");
        assertEquals("world", jc.get("hello"), "hello variable not changed");
        assertExpression(jc, "result = 1 + 1", Integer.valueOf(2));
        assertEquals(Integer.valueOf(2), jc.get("result"), "result variable not changed");
        // todo: make sure properties can be assigned to, fall back to flat var if no property
        // assertExpression(jc, "foo.property1 = '99'", "99");
        // assertEquals("property not set", "99", foo.getProperty1());
    }

    /**
     * Make sure bad syntax throws ParseException
     * @throws Exception on errors
     */
    @Test
    public void testBadParse() {
        assertThrows(JexlException.class, () -> assertExpression(new MapContext(), "empty()", null));
    }

    /**
     * test some blank strings
     */
    @Test
    public void testBlankStrings() {
        final JexlContext jc = new MapContext();
        jc.set("bar", "");

        assertExpression(jc, "bar == ''", Boolean.TRUE);
        assertExpression(jc, "empty bar", Boolean.TRUE);
        assertExpression(jc, "bar.length() == 0", Boolean.TRUE);
        assertExpression(jc, "size(bar) == 0", Boolean.TRUE);
    }

    @Test
    public void testBoolean() {
        final JexlContext jc = new MapContext();
        jc.set("foo", new Foo());
        jc.set("a", Boolean.TRUE);
        jc.set("b", Boolean.FALSE);

        assertExpression(jc, "foo.convertBoolean(a==b)", "Boolean : false");
        assertExpression(jc, "foo.convertBoolean(a==true)", "Boolean : true");
        assertExpression(jc, "foo.convertBoolean(a==false)", "Boolean : false");
        assertExpression(jc, "foo.convertBoolean(true==false)", "Boolean : false");
        assertExpression(jc, "true eq false", Boolean.FALSE);
        assertExpression(jc, "true ne false", Boolean.TRUE);
    }

    /**
     * Test that 'and' only evaluates the second item if needed
     * @throws Exception if there are errors
     */
    @Test
    public void testBooleanShortCircuitAnd() {
        // handle false for the left arg of 'and'
        Foo tester = new Foo();
        final JexlContext jc = new MapContext();
        jc.set("first", Boolean.FALSE);
        jc.set("foo", tester);
        final JexlExpression expr = JEXL.createExpression("first and foo.trueAndModify");
        expr.evaluate(jc);
        assertFalse(tester.getModified(), "Short circuit failure: rhs evaluated when lhs FALSE");
        // handle true for the left arg of 'and'
        tester = new Foo();
        jc.set("first", Boolean.TRUE);
        jc.set("foo", tester);
        expr.evaluate(jc);
        assertTrue(tester.getModified(), "Short circuit failure: rhs not evaluated when lhs TRUE");
    }

    /**
     * Test that 'or' only evaluates the second item if needed
     * @throws Exception if there are errors
     */
    @Test
    public void testBooleanShortCircuitOr() {
        // handle false for the left arg of 'or'
        Foo tester = new Foo();
        final JexlContext jc = new MapContext();
        jc.set("first", Boolean.FALSE);
        jc.set("foo", tester);
        final JexlExpression expr = JEXL.createExpression("first or foo.trueAndModify");
        expr.evaluate(jc);
        assertTrue(tester.getModified(), "Short circuit failure: rhs not evaluated when lhs FALSE");
        // handle true for the left arg of 'or'
        tester = new Foo();
        jc.set("first", Boolean.TRUE);
        jc.set("foo", tester);
        expr.evaluate(jc);
        assertFalse(tester.getModified(), "Short circuit failure: rhs evaluated when lhs TRUE");
    }

    /**
     * test some simple mathematical calculations
     */
    @Test
    public void testCalculations() {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        options.setStrict(false);
        options.setStrictArithmetic(false);

        /*
         * test to ensure new string cat works
         */
        jc.set("stringy", "thingy");
        assertExpression(jc, "stringy + 2", "thingy2");

        /*
         * test new null coersion
         */
        jc.set("imanull", null);
        assertExpression(jc, "imanull + 2", Integer.valueOf(2));
        assertExpression(jc, "imanull + imanull", Integer.valueOf(0));

        /* test for bugzilla 31577 */
        jc.set("n", Integer.valueOf(0));
        assertExpression(jc, "n != null && n != 0", Boolean.FALSE);
    }

    /**
     * Attempts to recreate bug https://jira.werken.com/ViewIssue.jspa?key=JELLY-8
     */
    @Test
    public void testCharAtBug() {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        options.setSilent(true);

        jc.set("foo", "abcdef");

        assertExpression(jc, "foo.substring(2,4)", "cd");
        assertExpression(jc, "foo.charAt(2)", Character.valueOf('c'));
        assertExpression(jc, "foo.charAt(-2)", null);

    }

    @Test
    public void testCoercionWithComparisionOperators() {
        final JexlContext jc = new MapContext();

        assertExpression(jc, "'2' > 1", Boolean.TRUE);
        assertExpression(jc, "'2' >= 1", Boolean.TRUE);
        assertExpression(jc, "'2' >= 2", Boolean.TRUE);
        assertExpression(jc, "'2' < 1", Boolean.FALSE);
        assertExpression(jc, "'2' <= 1", Boolean.FALSE);
        assertExpression(jc, "'2' <= 2", Boolean.TRUE);

        assertExpression(jc, "2 > '1'", Boolean.TRUE);
        assertExpression(jc, "2 >= '1'", Boolean.TRUE);
        assertExpression(jc, "2 >= '2'", Boolean.TRUE);
        assertExpression(jc, "2 < '1'", Boolean.FALSE);
        assertExpression(jc, "2 <= '1'", Boolean.FALSE);
        assertExpression(jc, "2 <= '2'", Boolean.TRUE);
    }

    /**
     * Test the ## comment in a string
     * @throws Exception
     */
    @Test
    public void testComment() {
        assertExpression(new MapContext(), "## double or nothing\n 1 + 1", Integer.valueOf("2"));
    }

    /**
     * test some simple conditions
     */
    @Test
    public void testComparisons() {
        final JexlContext jc = new MapContext();
        jc.set("foo", "the quick and lazy fox");

        assertExpression(jc, "foo.indexOf('quick') > 0", Boolean.TRUE);
        assertExpression(jc, "foo.indexOf('bar') >= 0", Boolean.FALSE);
        assertExpression(jc, "foo.indexOf('bar') < 0", Boolean.TRUE);
    }

    /**
     * test some simple conditions
     */
    @Test
    public void testConditions() {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        jc.set("foo", Integer.valueOf(2));
        jc.set("aFloat", Float.valueOf(1));
        jc.set("aDouble", Double.valueOf(2));
        jc.set("aChar", Character.valueOf('A'));
        jc.set("aBool", Boolean.TRUE);
        final StringBuilder buffer = new StringBuilder("abc");
        final List<Object> list = new ArrayList<>();
        final List<Object> list2 = new LinkedList<>();
        jc.set("aBuffer", buffer);
        jc.set("aList", list);
        jc.set("bList", list2);

        assertExpression(jc, "foo == 2", Boolean.TRUE);
        assertExpression(jc, "2 == 3", Boolean.FALSE);
        assertExpression(jc, "3 == foo", Boolean.FALSE);
        assertExpression(jc, "3 != foo", Boolean.TRUE);
        assertExpression(jc, "foo != 2", Boolean.FALSE);
        // test float and double equality
        assertExpression(jc, "aFloat eq aDouble", Boolean.FALSE);
        assertExpression(jc, "aFloat ne aDouble", Boolean.TRUE);
        assertExpression(jc, "aFloat == aDouble", Boolean.FALSE);
        assertExpression(jc, "aFloat != aDouble", Boolean.TRUE);
        // test number and character equality
        assertExpression(jc, "foo == aChar", Boolean.FALSE);
        assertExpression(jc, "foo != aChar", Boolean.TRUE);
        // test string and boolean
        assertExpression(jc, "aBool == 'true'", Boolean.TRUE);
        assertExpression(jc, "aBool == 'false'", Boolean.FALSE);
        assertExpression(jc, "aBool != 'false'", Boolean.TRUE);
        // test null and boolean
        options.setStrict(false);
        assertExpression(jc, "aBool == notThere", Boolean.FALSE);
        assertExpression(jc, "aBool != notThere", Boolean.TRUE);
        // anything and string as a string comparison
        options.setStrict(true);
        assertExpression(jc, "aBuffer == 'abc'", Boolean.TRUE);
        assertExpression(jc, "aBuffer != 'abc'", Boolean.FALSE);
        // arbitrary equals
        assertExpression(jc, "aList == bList", Boolean.TRUE);
        assertExpression(jc, "aList != bList", Boolean.FALSE);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testDuck() {
        final JexlEngine jexl = JEXL;
        final JexlContext jc = new MapContext();
        jc.set("duck", new Duck());
        JexlExpression expr;
        Object result;
        expr = jexl.createExpression("duck.zero");
        result = expr.evaluate(jc);
        assertEquals(0, result, expr::toString);
        expr = jexl.createExpression("duck.one");
        result = expr.evaluate(jc);
        assertEquals(1, result, expr::toString);
        expr = jexl.createExpression("duck.user = 20");
        result = expr.evaluate(jc);
        assertEquals(20, result, expr::toString);
        expr = jexl.createExpression("duck.user");
        result = expr.evaluate(jc);
        assertEquals(20, result, expr::toString);
        expr = jexl.createExpression("duck.user = 'zero'");
        result = expr.evaluate(jc);
        assertEquals("zero", result, expr::toString);
        expr = jexl.createExpression("duck.user");
        result = expr.evaluate(jc);
        assertEquals(0, result, expr::toString);
    }

    @Test
    public void testEmpty() {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        options.setStrict(false);
        jc.set("string", "");
        jc.set("array", new Object[0]);
        jc.set("map", new HashMap<>());
        jc.set("list", new ArrayList<>());
        jc.set("set", new HashMap<>().keySet());
        jc.set("longstring", "thingthing");

        /*
         *  I can't believe anyone thinks this is a syntax.. :)
         */
        assertExpression(jc, "empty nullthing", Boolean.TRUE);
        assertExpression(jc, "empty string", Boolean.TRUE);
        assertExpression(jc, "empty array", Boolean.TRUE);
        assertExpression(jc, "empty map", Boolean.TRUE);
        assertExpression(jc, "empty set", Boolean.TRUE);
        assertExpression(jc, "empty list", Boolean.TRUE);
        assertExpression(jc, "empty longstring", Boolean.FALSE);
        assertExpression(jc, "not empty longstring", Boolean.TRUE);
    }

    @Test
    public void testEmptyDottedVariableName() {
        final JexlContext jc = new MapContext();

        jc.set("this.is.a.test", "");

        assertExpression(jc, "empty(this.is.a.test)", Boolean.TRUE);
    }

    @Test
    public void testEmptySubListOfMap() {
        final JexlContext jc = new MapContext();
        final Map<String, ArrayList<?>> m = new HashMap<>();
        m.put("aList", new ArrayList<>());

        jc.set("aMap", m);

        assertExpression(jc, "empty( aMap.aList )", Boolean.TRUE);
    }

    @Test
    public void testExpression() {
        final JexlContext jc = new MapContext();
        jc.set("foo", new Foo());
        jc.set("a", Boolean.TRUE);
        jc.set("b", Boolean.FALSE);
        jc.set("num", Integer.valueOf(5));
        jc.set("now", Calendar.getInstance().getTime());
        final GregorianCalendar gc = new GregorianCalendar(5000, 11, 20);
        jc.set("now2", gc.getTime());
        jc.set("bdec", new BigDecimal("7"));
        jc.set("bint", new BigInteger("7"));

        assertExpression(jc, "a == b", Boolean.FALSE);
        assertExpression(jc, "a==true", Boolean.TRUE);
        assertExpression(jc, "a==false", Boolean.FALSE);
        assertExpression(jc, "true==false", Boolean.FALSE);

        assertExpression(jc, "2 < 3", Boolean.TRUE);
        assertExpression(jc, "num < 5", Boolean.FALSE);
        assertExpression(jc, "num < num", Boolean.FALSE);
        assertExpression(jc, "num < null", Boolean.FALSE);
        assertExpression(jc, "num < 2.5", Boolean.FALSE);
        assertExpression(jc, "now2 < now", Boolean.FALSE); // test comparable
//
        assertExpression(jc, "'6' <= '5'", Boolean.FALSE);
        assertExpression(jc, "num <= 5", Boolean.TRUE);
        assertExpression(jc, "num <= num", Boolean.TRUE);
        assertExpression(jc, "num <= null", Boolean.FALSE);
        assertExpression(jc, "num <= 2.5", Boolean.FALSE);
        assertExpression(jc, "now2 <= now", Boolean.FALSE); // test comparable

//
        assertExpression(jc, "'6' >= '5'", Boolean.TRUE);
        assertExpression(jc, "num >= 5", Boolean.TRUE);
        assertExpression(jc, "num >= num", Boolean.TRUE);
        assertExpression(jc, "num >= null", Boolean.FALSE);
        assertExpression(jc, "num >= 2.5", Boolean.TRUE);
        assertExpression(jc, "now2 >= now", Boolean.TRUE); // test comparable

        assertExpression(jc, "'6' > '5'", Boolean.TRUE);
        assertExpression(jc, "num > 4", Boolean.TRUE);
        assertExpression(jc, "num > num", Boolean.FALSE);
        assertExpression(jc, "num > null", Boolean.FALSE);
        assertExpression(jc, "num > 2.5", Boolean.TRUE);
        assertExpression(jc, "now2 > now", Boolean.TRUE); // test comparable

        assertExpression(jc, "\"foo\" + \"bar\" == \"foobar\"", Boolean.TRUE);

        assertExpression(jc, "bdec > num", Boolean.TRUE);
        assertExpression(jc, "bdec >= num", Boolean.TRUE);
        assertExpression(jc, "num <= bdec", Boolean.TRUE);
        assertExpression(jc, "num < bdec", Boolean.TRUE);
        assertExpression(jc, "bint > num", Boolean.TRUE);
        assertExpression(jc, "bint == bdec", Boolean.TRUE);
        assertExpression(jc, "bint >= num", Boolean.TRUE);
        assertExpression(jc, "num <= bint", Boolean.TRUE);
        assertExpression(jc, "num < bint", Boolean.TRUE);
    }

    /**
     * test the use of an int based property
     */
    @Test
    public void testIntProperty() {
        final Foo foo = new Foo();

        // lets check the square function first.
        assertEquals(4, foo.square(2));
        assertEquals(4, foo.square(-2));

        final JexlContext jc = new MapContext();
        jc.set("foo", foo);

        assertExpression(jc, "foo.count", Integer.valueOf(5));
        assertExpression(jc, "foo.square(2)", Integer.valueOf(4));
        assertExpression(jc, "foo.square(-2)", Integer.valueOf(4));
    }

    /**
     * test some blank strings
     */
    @Test
    public void testLogicExpressions() {
        final JexlContext jc = new MapContext();
        jc.set("foo", "abc");
        jc.set("bar", "def");

        assertExpression(jc, "foo == 'abc' || bar == 'abc'", Boolean.TRUE);
        assertExpression(jc, "foo == 'abc' or bar == 'abc'", Boolean.TRUE);
        assertExpression(jc, "foo == 'abc' && bar == 'abc'", Boolean.FALSE);
        assertExpression(jc, "foo == 'abc' and bar == 'abc'", Boolean.FALSE);

        assertExpression(jc, "foo == 'def' || bar == 'abc'", Boolean.FALSE);
        assertExpression(jc, "foo == 'def' or bar == 'abc'", Boolean.FALSE);
        assertExpression(jc, "foo == 'abc' && bar == 'def'", Boolean.TRUE);
        assertExpression(jc, "foo == 'abc' and bar == 'def'", Boolean.TRUE);
    }

    /**
     * test the use of dot notation to lookup map entries
     */
    @Test
    public void testMapDot() {
        final Map<String, String> foo = new HashMap<>();
        foo.put("bar", "123");

        final JexlContext jc = new MapContext();
        jc.set("foo", foo);

        assertExpression(jc, "foo.bar", "123");
    }

    /**
     * test the -1 comparison bug
     */
    @Test
    public void testNegativeIntComparison() {
        final JexlContext jc = new MapContext();
        final Foo foo = new Foo();
        jc.set("foo", foo);

        assertExpression(jc, "foo.count != -1", Boolean.TRUE);
        assertExpression(jc, "foo.count == 5", Boolean.TRUE);
        assertExpression(jc, "foo.count == -1", Boolean.FALSE);
    }

    /**
     * test the new function e.g constructor invocation
     */
    @Test
    public void testNew() {
        final JexlContext jc = new MapContext();
        jc.set("double", Double.class);
        jc.set("foo", "org.apache.commons.jexl3.Foo");
        JexlExpression expr;
        Object value;
        expr = JEXL.createExpression("new(double, 1)");
        value = expr.evaluate(jc);
        assertEquals(Double.valueOf(1.0), value, expr::toString);
        expr = JEXL.createExpression("new('java.lang.Float', 100)");
        value = expr.evaluate(jc);
        assertEquals(Float.valueOf((float) 100.0), value, expr::toString);
        expr = JEXL.createExpression("new(foo).quux");
        value = expr.evaluate(jc);
        assertEquals("String : quux", value, expr::toString);
    }

    @Test
    public void testNewImports() {
        final JexlEngine jexl = new JexlBuilder().imports("java.lang", "java.util").create();
        JexlExpression expr;
        Object result;
        expr = jexl.createExpression("new LinkedList([1,2,3,...])");
        result = expr.evaluate(null);
        assertInstanceOf(LinkedList.class, result);
    }

    /**
     * test some simple conditions
     */
    @Test
    public void testNotConditions() {
        final JexlContext jc = new MapContext();

        final Foo foo = new Foo();
        jc.set("x", Boolean.TRUE);
        jc.set("foo", foo);
        jc.set("bar", "true");

        assertExpression(jc, "!x", Boolean.FALSE);
        assertExpression(jc, "x", Boolean.TRUE);
        assertExpression(jc, "!bar", Boolean.FALSE);
        assertExpression(jc, "!foo.isSimple()", Boolean.FALSE);
        assertExpression(jc, "foo.isSimple()", Boolean.TRUE);
        assertExpression(jc, "!foo.simple", Boolean.FALSE);
        assertExpression(jc, "foo.simple", Boolean.TRUE);
        assertExpression(jc, "foo.getCheeseList().size() == 3", Boolean.TRUE);
        assertExpression(jc, "foo.cheeseList.size() == 3", Boolean.TRUE);

        jc.set("string", "");
        assertExpression(jc, "not empty string", Boolean.FALSE);
        assertExpression(jc, "not(empty string)", Boolean.FALSE);
        assertExpression(jc, "not empty(string)", Boolean.FALSE);
        assertExpression(jc, "! empty string", Boolean.FALSE);
        assertExpression(jc, "!(empty string)", Boolean.FALSE);
        assertExpression(jc, "! empty(string)", Boolean.FALSE);

    }

    /**
     * test some simple conditions
     */
    @Test
    public void testNotConditionsWithDots() {
        final JexlContext jc = new MapContext();

        jc.set("x.a", Boolean.TRUE);
        jc.set("x.b", Boolean.FALSE);

        assertExpression(jc, "x.a", Boolean.TRUE);
        assertExpression(jc, "!x.a", Boolean.FALSE);
        assertExpression(jc, "!x.b", Boolean.TRUE);
    }

    /**
     * test some null conditions
     */
    @Test
    public void testNull() {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        options.setStrict(false);
        jc.set("bar", Integer.valueOf(2));

        assertExpression(jc, "empty foo", Boolean.TRUE);
        assertExpression(jc, "bar == null", Boolean.FALSE);
        assertExpression(jc, "foo == null", Boolean.TRUE);
        assertExpression(jc, "bar != null", Boolean.TRUE);
        assertExpression(jc, "foo != null", Boolean.FALSE);
        assertExpression(jc, "empty(bar)", Boolean.FALSE);
        assertExpression(jc, "empty(foo)", Boolean.TRUE);
    }

    /**
     * test a simple property expression
     */
    @Test
    public void testProperty() {
        /*
         *  tests a simple property expression
         */

        final JexlExpression e = JEXL.createExpression("foo.bar");
        final JexlContext jc = new MapContext();

        jc.set("foo", new Foo());
        final Object o = e.evaluate(jc);

        assertInstanceOf(String.class, o, "o not instanceof String");
        assertEquals(GET_METHOD_STRING, o, "o incorrect");
    }

    @Test
    public void testSize() {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        options.setStrict(false);
        jc.set("s", "five!");
        jc.set("array", new Object[5]);

        final Map<String, Integer> map = new HashMap<>();

        map.put("1", Integer.valueOf(1));
        map.put("2", Integer.valueOf(2));
        map.put("3", Integer.valueOf(3));
        map.put("4", Integer.valueOf(4));
        map.put("5", Integer.valueOf(5));

        jc.set("map", map);

        final List<String> list = new ArrayList<>();

        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");

        jc.set("list", list);

        // 30652 - support for set
        final Set<String> set = new HashSet<>(list);
        set.add("1");

        jc.set("set", set);

        // support generic int size() method
        final BitSet bitset = new BitSet(5);
        jc.set("bitset", bitset);

        assertExpression(jc, "size(s)", Integer.valueOf(5));
        assertExpression(jc, "size(array)", Integer.valueOf(5));
        assertExpression(jc, "size(list)", Integer.valueOf(5));
        assertExpression(jc, "size(map)", Integer.valueOf(5));
        assertExpression(jc, "size(set)", Integer.valueOf(5));
        assertExpression(jc, "size(bitset)", Integer.valueOf(64));
        assertExpression(jc, "list.size()", Integer.valueOf(5));
        assertExpression(jc, "map.size()", Integer.valueOf(5));
        assertExpression(jc, "set.size()", Integer.valueOf(5));
        assertExpression(jc, "bitset.size()", Integer.valueOf(64));

        assertExpression(jc, "list.get(size(list) - 1)", "5");
        assertExpression(jc, "list[size(list) - 1]", "5");
        assertExpression(jc, "list.get(list.size() - 1)", "5");
    }

    @Test
    public void testSizeAsProperty() {
        final JexlContext jc = new MapContext();
        final Map<String, Object> map = new HashMap<>();
        map.put("size", "cheese");
        map.put("si & ze", "cheese");
        jc.set("map", map);
        jc.set("foo", new Foo());

        assertExpression(jc, "map['size']", "cheese");
        assertExpression(jc, "map['si & ze']", "cheese");
        assertExpression(jc, "map.'si & ze'", "cheese");
        assertExpression(jc, "map.size()", 2);
        assertExpression(jc, "size(map)", 2);
        assertExpression(jc, "foo.getSize()", 22);
        assertExpression(jc, "foo.'size'", 22);
    }

    /**
     * Simple test of '+' as a string concatenation operator
     * @throws Exception
     */
    @Test
    public void testStringConcatenation() {
        final JexlContext jc = new MapContext();
        jc.set("first", "Hello");
        jc.set("second", "World");
        assertExpression(jc, "first + ' ' + second", "Hello World");
    }

    @Test
    public void testStringLit() {
        /*
         *  tests a simple property expression
         */
        final JexlContext jc = new MapContext();
        jc.set("foo", new Foo());
        assertExpression(jc, "foo.repeat(\"woogie\")", "Repeat : woogie");
    }

    /**
     * Tests string literals
     */
    @Test
    public void testStringLiterals() {
        final JexlContext jc = new MapContext();
        jc.set("foo", "bar");

        assertExpression(jc, "foo == \"bar\"", Boolean.TRUE);
        assertExpression(jc, "foo == 'bar'", Boolean.TRUE);
    }

    /**
     * test quoting in strings
     */
    @Test
    public void testStringQuoting() {
        final JexlContext jc = new MapContext();
        assertExpression(jc, "'\"Hello\"'", "\"Hello\"");
        assertExpression(jc, "\"I'm testing\"", "I'm testing");
    }

    @Test
    public void testToString() {
        final String code = "abcd";
        final JexlExpression expr = JEXL.createExpression(code);
        assertEquals(code, expr.toString(), "Bad expression value");
    }

    @Test
    public void testUnicodeSupport() {
        final JexlContext jc = new MapContext();
        assertExpression(jc, "'x' == '\\u0032?ytkownik'", Boolean.FALSE);
        assertExpression(jc, "'c:\\some\\windows\\path'", "c:\\some\\windows\\path");
        assertExpression(jc, "'foo\\u0020bar'", "foo\u0020bar");
        assertExpression(jc, "'foo\\u0020\\u0020bar'", "foo\u0020\u0020bar");
        assertExpression(jc, "'\\u0020foobar\\u0020'", "\u0020foobar\u0020");
    }

    /**
     * test variables with underscore names
     */
    @Test
    public void testVariableNames() {
        final JexlContext jc = new MapContext();
        jc.set("foo_bar", "123");

        assertExpression(jc, "foo_bar", "123");
    }
}
