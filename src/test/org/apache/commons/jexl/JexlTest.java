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
package org.apache.commons.jexl;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.jexl.parser.ParseException;
import org.apache.commons.jexl.parser.Parser;

/**
 *  Simple testcases
 *
 *  @since 1.0
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id$
 */
public class JexlTest extends TestCase
{
    protected static final String METHOD_STRING = "Method string";
    protected static final String GET_METHOD_STRING = "GetMethod string";

    public static Test suite()
    {
        return new TestSuite(JexlTest.class);
    }

    public JexlTest(String testName)
    {
        super(testName);
    }

    /**
      *  test a simple property expression
      */
    public void testProperty()
         throws Exception
    {
        /*
         *  tests a simple property expression
         */

        Expression e = ExpressionFactory.createExpression("foo.bar");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertEquals("o incorrect", GET_METHOD_STRING, o);
    }

    public void testBoolean()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Foo() );
        jc.getVars().put("a", Boolean.TRUE);
        jc.getVars().put("b", Boolean.FALSE);

        assertExpression(jc, "foo.convertBoolean(a==b)", "Boolean : false");
        assertExpression(jc, "foo.convertBoolean(a==true)", "Boolean : true");
        assertExpression(jc, "foo.convertBoolean(a==false)", "Boolean : false");
        assertExpression(jc, "foo.convertBoolean(true==false)", "Boolean : false");
        assertExpression(jc, "true eq false", Boolean.FALSE);
        assertExpression(jc, "true ne false", Boolean.TRUE);
    }

    public void testStringLit()
         throws Exception
    {
        /*
         *  tests a simple property expression
         */
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Foo() );
        assertExpression(jc, "foo.get(\"woogie\")", "Repeat : woogie");
    }

    public void testExpression()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Foo() );
        jc.getVars().put("a", Boolean.TRUE);
        jc.getVars().put("b", Boolean.FALSE);
        jc.getVars().put("num", new Integer(5));
        jc.getVars().put("now", Calendar.getInstance().getTime());
        GregorianCalendar gc = new GregorianCalendar(5000, 11, 20);
        jc.getVars().put("now2", gc.getTime());

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

    }

    public void testEmpty()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("string", "");
        jc.getVars().put("array", new Object[0]);
        jc.getVars().put("map", new HashMap());
        jc.getVars().put("list", new ArrayList());
        jc.getVars().put("set", (new HashMap()).keySet());
        jc.getVars().put("longstring", "thingthing");

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

    public void testSize()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("s", "five!");
        jc.getVars().put("array", new Object[5]);

        Map map = new HashMap();

        map.put("1", new Integer(1));
        map.put("2", new Integer(2));
        map.put("3", new Integer(3));
        map.put("4", new Integer(4));
        map.put("5", new Integer(5));

        jc.getVars().put("map", map);

        List list = new ArrayList();

        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");

        jc.getVars().put("list", list);

        // 30652 - support for set
        Set set = new HashSet();
        set.addAll(list);
        set.add("1");
        
        jc.getVars().put("set", set);
        
        // support generic int size() method
        BitSet bitset = new BitSet(5);
        jc.getVars().put("bitset", bitset);

        assertExpression(jc, "size(s)", new Integer(5));
        assertExpression(jc, "size(array)", new Integer(5));
        assertExpression(jc, "size(list)", new Integer(5));
        assertExpression(jc, "size(map)", new Integer(5));
        assertExpression(jc, "size(set)", new Integer(5));
        assertExpression(jc, "size(bitset)", new Integer(64));
        assertExpression(jc, "list.size()", new Integer(5));
        assertExpression(jc, "map.size()", new Integer(5));
        assertExpression(jc, "set.size()", new Integer(5));
        assertExpression(jc, "bitset.size()", new Integer(64));

        assertExpression(jc, "list.get(size(list) - 1)", "5");
        assertExpression(jc, "list[size(list) - 1]", "5");
        assertExpression(jc, "list.get(list.size() - 1)", "5");
    }

    public void testSizeAsProperty() throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        Map map = new HashMap();
        map.put("size", "cheese");
        jc.getVars().put("map", map);
        jc.getVars().put("foo", new Foo());

        assertExpression(jc, "map['size']", "cheese");
// PR - unsure whether or not we should support map.size or force usage of the above 'escaped' version        
//        assertExpression(jc, "map.size", "cheese");
        assertExpression(jc, "foo.getSize()", new Integer(22));
        // failing assertion for size property
        //assertExpression(jc, "foo.size", new Integer(22));
    }

   /**
    *  test some simple mathematical calculations
    */
   public void testUnaryMinus()
        throws Exception
   {
       JexlContext jc = JexlHelper.createContext();

       jc.getVars().put("aByte", new Byte((byte)1));
       jc.getVars().put("aShort", new Short((short)2));
       jc.getVars().put("anInteger", new Integer(3));
       jc.getVars().put("aLong", new Long(4));
       jc.getVars().put("aFloat", new Float(5.5));
       jc.getVars().put("aDouble", new Double(6.6));
       jc.getVars().put("aBigInteger", new BigInteger("7"));
       jc.getVars().put("aBigDecimal", new BigDecimal("8.8"));
       assertExpression(jc, "-3", new Integer("-3"));
       assertExpression(jc, "-3.0", new Float("-3.0"));
       assertExpression(jc, "-aByte", new Byte((byte)-1));
       assertExpression(jc, "-aShort", new Short((short)-2));
       assertExpression(jc, "-anInteger", new Integer(-3));
       assertExpression(jc, "-aLong", new Long(-4));
       assertExpression(jc, "-aFloat", new Float(-5.5));
       assertExpression(jc, "-aDouble", new Double(-6.6));
       assertExpression(jc, "-aBigInteger", new BigInteger("-7"));
       assertExpression(jc, "-aBigDecimal", new BigDecimal("-8.8"));
   }

    
    /**
      *  test some simple mathematical calculations
      */
    public void testCalculations()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Integer(2) );

        assertExpression(jc, "foo + 2", new Long(4));
        assertExpression(jc, "3 + 3", new Long(6));
        assertExpression(jc, "3 + 3 + foo", new Long(8));
        assertExpression(jc, "3 * 3", new Long(9));
        assertExpression(jc, "3 * 3 + foo", new Long(11));
        assertExpression(jc, "3 * 3 - foo", new Long(7));

        /*
         * test some floaty stuff
         */
        assertExpression(jc, "3 * \"3.0\"", new Double(9));
        assertExpression(jc, "3 * 3.0", new Double(9));

        /*
         *  test / and %
         */
        assertExpression(jc, "6 / 3", new Double(6/3));
        assertExpression(jc, "6.4 / 3", new Double(6.4 / 3));
        assertExpression(jc, "0 / 3", new Double(0 / 3));
        assertExpression(jc, "3 / 0", new Double(0));
        assertExpression(jc, "4 % 3", new Long(1));
        assertExpression(jc, "4.8 % 3", new Double(4.8 % 3));

        /*
         * test to ensure new string cat works
         */
        jc.getVars().put("stringy", "thingy" );
        assertExpression(jc, "stringy + 2", "thingy2");

        /*
         * test new null coersion
         */
        jc.getVars().put("imanull", null );
        assertExpression(jc, "imanull + 2", new Long(2));
        assertExpression(jc, "imanull + imanull", new Long(0));
        
        /* test for bugzilla 31577 */
        jc.getVars().put("n", new Integer(0));
        assertExpression(jc, "n != null && n != 0", Boolean.FALSE);
    }

    /**
      *  test some simple conditions
      */
    public void testConditions()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Integer(2) );
        jc.getVars().put("aFloat", new Float(1));
        jc.getVars().put("aDouble", new Double(2));
        jc.getVars().put("aChar", new Character('A'));
        jc.getVars().put("aBool", Boolean.TRUE);
        StringBuffer buffer = new StringBuffer("abc");
        List list = new ArrayList();
        List list2 = new LinkedList();
        jc.getVars().put("aBuffer", buffer);
        jc.getVars().put("aList", list);
        jc.getVars().put("bList", list2);
        
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
        assertExpression(jc, "aBool == notThere", Boolean.FALSE);
        assertExpression(jc, "aBool != notThere", Boolean.TRUE);
        // anything and string as a string comparison
        assertExpression(jc, "aBuffer == 'abc'", Boolean.TRUE);
        assertExpression(jc, "aBuffer != 'abc'", Boolean.FALSE);
        // arbitrary equals
        assertExpression(jc, "aList == bList", Boolean.TRUE);
        assertExpression(jc, "aList != bList", Boolean.FALSE);
    }

    /**
      *  test some simple conditions
      */
    public void testNotConditions()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();
        jc.getVars().put("x", Boolean.TRUE );
        jc.getVars().put("foo", foo );
        jc.getVars().put("bar", "true" );

        assertExpression(jc, "!x", Boolean.FALSE);
        assertExpression(jc, "x", Boolean.TRUE);
        assertExpression(jc, "!bar", Boolean.FALSE);
        assertExpression(jc, "!foo.isSimple()", Boolean.FALSE);
        assertExpression(jc, "foo.isSimple()", Boolean.TRUE);
        assertExpression(jc, "!foo.simple", Boolean.FALSE);
        assertExpression(jc, "foo.simple", Boolean.TRUE);
        assertExpression(jc, "foo.getCheeseList().size() == 3", Boolean.TRUE);
        assertExpression(jc, "foo.cheeseList.size() == 3", Boolean.TRUE);

        jc.getVars().put("string", "");
        assertExpression(jc, "not empty string", Boolean.FALSE);
        assertExpression(jc, "not(empty string)", Boolean.FALSE);
        assertExpression(jc, "not empty(string)", Boolean.FALSE);
        assertExpression(jc, "! empty string", Boolean.FALSE);
        assertExpression(jc, "!(empty string)", Boolean.FALSE);
        assertExpression(jc, "!empty(string)", Boolean.FALSE);

    }


    /**
      *  test some simple conditions
      */
    public void testNotConditionsWithDots()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("x.a", Boolean.TRUE );
        jc.getVars().put("x.b", Boolean.FALSE );

        assertExpression(jc, "x.a", Boolean.TRUE);
        assertExpression(jc, "!x.a", Boolean.FALSE);
        assertExpression(jc, "!x.b", Boolean.TRUE);
    }

    /**
      *  test some simple conditions
      */
    public void testComparisons()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", "the quick and lazy fox" );

        assertExpression(jc, "foo.indexOf('quick') > 0", Boolean.TRUE);
        assertExpression(jc, "foo.indexOf('bar') >= 0", Boolean.FALSE);
        assertExpression(jc, "foo.indexOf('bar') < 0", Boolean.TRUE);
    }

    /**
      *  test some null conditions
      */
    public void testNull()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("bar", new Integer(2) );

        assertExpression(jc, "empty foo", Boolean.TRUE);
        assertExpression(jc, "bar == null", Boolean.FALSE);
        assertExpression(jc, "foo == null", Boolean.TRUE);
        assertExpression(jc, "bar != null", Boolean.TRUE);
        assertExpression(jc, "foo != null", Boolean.FALSE);
        assertExpression(jc, "empty(bar)", Boolean.FALSE);
        assertExpression(jc, "empty(foo)", Boolean.TRUE);
    }

    /**
      *  test some blank strings
      */
    public void testBlankStrings()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("bar", "" );

        assertExpression(jc, "foo == ''", Boolean.FALSE);
        assertExpression(jc, "bar == ''", Boolean.TRUE);
        assertExpression(jc, "barnotexist == ''", Boolean.FALSE);
        assertExpression(jc, "empty bar", Boolean.TRUE);
        assertExpression(jc, "bar.length() == 0", Boolean.TRUE);
        assertExpression(jc, "size(bar) == 0", Boolean.TRUE);
    }

    /**
      *  test some blank strings
      */
    public void testLogicExpressions()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", "abc" );
        jc.getVars().put("bar", "def" );

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
      *  test variables with underscore names
      */
    public void testVariableNames()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo_bar", "123" );
        
        assertExpression(jc, "foo_bar", "123");
    }

    /**
      *  test the use of dot notation to lookup map entries
      */
    public void testMapDot()
         throws Exception
    {
        Map foo = new HashMap();
        foo.put( "bar", "123" );

        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", foo );
        
        assertExpression(jc, "foo.bar", "123");
    }

    /**
     *  Tests string literals
     */
    public void testStringLiterals()
        throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", "bar" );

        assertExpression(jc, "foo == \"bar\"", Boolean.TRUE);
        assertExpression(jc, "foo == 'bar'", Boolean.TRUE);
    }

    /**
      *  test the use of an int based property
      */
    public void testIntProperty()
         throws Exception
    {
        Foo foo = new Foo();

        // lets check the square function first..
        assertEquals(4, foo.square(2));
        assertEquals(4, foo.square(-2));

        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", foo );

        assertExpression(jc, "foo.count", new Integer(5));
        assertExpression(jc, "foo.square(2)", new Integer(4));
        assertExpression(jc, "foo.square(-2)", new Integer(4));
    }

    /**
      *  test the -1 comparison bug
      */
    public void testNegativeIntComparison()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        Foo foo = new Foo();
        jc.getVars().put("foo", foo );

        assertExpression(jc, "foo.count != -1", Boolean.TRUE);
        assertExpression(jc, "foo.count == 5", Boolean.TRUE);
        assertExpression(jc, "foo.count == -1", Boolean.FALSE);
    }

    /**
     * Attempts to recreate bug http://jira.werken.com/ViewIssue.jspa?key=JELLY-8
     */
    public void testCharAtBug()
        throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", "abcdef");

        assertExpression(jc, "foo.substring(2,4)", "cd");
        assertExpression(jc, "foo.charAt(2)", new Character('c'));

        try {
            assertExpression(jc, "foo.charAt(-2)", null);
            fail("this test should have thrown an exception" );
        }
        catch (Exception e) {
            // expected behaviour
        }
    }

    public void testEmptyDottedVariableName() throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put( "this.is.a.test", "");

        assertExpression(jc, "empty(this.is.a.test)", Boolean.TRUE);
    }

    public void testEmptySubListOfMap() throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        Map m = new HashMap();
        m.put("aList", new ArrayList());

        jc.getVars().put( "aMap", m );

        assertExpression( jc, "empty( aMap.aList )", Boolean.TRUE );
    }

    public void testCoercionWithComparisionOperators()
        throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

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
     * Test that 'and' only evaluates the second item if needed
     * @throws Exception if there are errors
     */
    public void testBooleanShortCircuitAnd() throws Exception
    {
        // handle false for the left arg of 'and'
        Foo tester = new Foo();
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("first", Boolean.FALSE);
        jc.getVars().put("foo", tester);
        Expression expr = ExpressionFactory.createExpression("first and foo.trueAndModify");
        expr.evaluate(jc);
        assertTrue("Short circuit failure: rhs evaluated when lhs FALSE", !tester.getModified());
        // handle true for the left arg of 'and' 
        tester = new Foo();
        jc.getVars().put("first", Boolean.TRUE);
        jc.getVars().put("foo", tester);
        expr.evaluate(jc);
        assertTrue("Short circuit failure: rhs not evaluated when lhs TRUE", tester.getModified());
    }
    
    /**
     * Test that 'or' only evaluates the second item if needed
     * @throws Exception if there are errors
     */
    public void testBooleanShortCircuitOr() throws Exception
    {
        // handle false for the left arg of 'or'
        Foo tester = new Foo();
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("first", Boolean.FALSE);
        jc.getVars().put("foo", tester);
        Expression expr = ExpressionFactory.createExpression("first or foo.trueAndModify");
        expr.evaluate(jc);
        assertTrue("Short circuit failure: rhs not evaluated when lhs FALSE", tester.getModified());
        // handle true for the left arg of 'or' 
        tester = new Foo();
        jc.getVars().put("first", Boolean.TRUE);
        jc.getVars().put("foo", tester);
        expr.evaluate(jc);
        assertTrue("Short circuit failure: rhs evaluated when lhs TRUE", !tester.getModified());
    }

    /**
     * Simple test of '+' as a string concatenation operator
     * @throws Exception
     */
    public void testStringConcatenation() throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("first", "Hello");
        jc.getVars().put("second", "World");
        assertExpression(jc, "first + ' ' + second", "Hello World");
    }

    public void testToString() throws Exception {
        String code = "abcd";
        Expression expr = ExpressionFactory.createExpression(code);
        assertEquals("Bad expression value", code, expr.toString());
    }
    
    /**
     * Make sure bad syntax throws ParseException
     * @throws Exception on errors
     */
    public void testBadParse() throws Exception
    {
        try
        {
            assertExpression(JexlHelper.createContext(), "empty()", null);
            fail("Bad expression didn't throw ParseException");
        }
        catch (ParseException pe)
        {
            // expected behaviour
        }
    }

    /**
     * Test the ## comment in a string
     * @throws Exception
     */
    public void testComment() throws Exception
    {
        assertExpression(JexlHelper.createContext(), "## double or nothing\n 1 + 1", Long.valueOf("2"));
    }
    
    /**
     * Test assignment.
     * @throws Exception
     */
    public void testAssignment() throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("aString", "Hello");
        Foo foo = new Foo();
        jc.getVars().put("foo", foo);
        Parser parser = new Parser(new StringReader(";"));
        parser.parse(new StringReader("aString = 'World';"));
        
        assertExpression(jc, "hello = 'world'", "world");
        assertEquals("hello variable not changed", "world", jc.getVars().get("hello"));
        assertExpression(jc, "result = 1 + 1", new Long(2));
        assertEquals("result variable not changed", new Long(2), jc.getVars().get("result"));
        // todo: make sure properties can be assigned to, fall back to flat var if no property
        // assertExpression(jc, "foo.property1 = '99'", "99");
        // assertEquals("property not set", "99", foo.getProperty1());
    }
    
    public void testAntPropertiesWithMethods() throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        String value = "Stinky Cheese";
        jc.getVars().put("maven.bob.food", value);
        assertExpression(jc, "maven.bob.food.length()", new Integer(value.length()));
        assertExpression(jc, "empty(maven.bob.food)", Boolean.FALSE);
        assertExpression(jc, "size(maven.bob.food)", new Integer(value.length()));
        assertExpression(jc, "maven.bob.food + ' is good'", value + " is good");

        // DG: Note the following ant properties don't work
//        String version = "1.0.3";
//        jc.getVars().put("commons-logging", version);
//        assertExpression(jc, "commons-logging", version);
    }
    
    public void testUnicodeSupport() throws Exception
    {
        assertExpression(JexlHelper.createContext(), "myvar == 'Użytkownik'", Boolean.FALSE);
    }

    /**
     * Asserts that the given expression returns the given value when applied to the
     * given context
     */
    protected void assertExpression(JexlContext jc, String expression, Object expected) throws Exception
    {
        Expression e = ExpressionFactory.createExpression(expression);
        Object actual = e.evaluate(jc);
        assertEquals(expression, expected, actual);
    }


    /**
     *  Helps in debugging the testcases when working with it
     *
     */
    public static void main(String[] args)
        throws Exception
    {
        JexlTest jt = new JexlTest("foo");
        jt.testEmpty();
    }

}
