/*
 * Copyright 2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.jexl.resolver.FlatResolver;

/**
 *  Simple testcases
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: JexlTest.java,v 1.36 2004/05/19 15:22:45 proyal Exp $
 */
public class JexlTest extends TestCase
{
    protected static final String METHOD_STRING = "Method string";
    protected static final String GET_METHOD_STRING = "GetMethod string";

    protected static final String[] GET_METHOD_ARRAY =
        new String[] { "One", "Two", "Three" };

    protected static final String[][] GET_METHOD_ARRAY2 =
        new String[][] { {"One", "Two", "Three"},{"Four", "Five", "Six"} };

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
        assertTrue("o incorrect", o.equals(GET_METHOD_STRING));
    }

    /**
      *  test a simple method expression
      */
    public void testMethod()
         throws Exception
    {
        /*
         *  tests a simple method expression
         */

        Expression e = ExpressionFactory.createExpression("foo.bar()");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );

        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("o incorrect", o.equals(METHOD_STRING));
    }

    /**
      *  test a simple method expression
      */
    public void testArrayAccess()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("list[1]");
        JexlContext jc = JexlHelper.createContext();

        /*
         *  test List access
         */

        List l = new ArrayList();
        l.add(new Integer(1));
        l.add(new Integer(2));
        l.add(new Integer(3));

        jc.getVars().put("list", l);

        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Integer", o instanceof Integer);
        assertTrue("o incorrect", o.equals(new Integer(2)));

        e = ExpressionFactory.createExpression("list[1+1]");

        o = e.evaluate(jc);

        assertTrue("o not instanceof Integer", o instanceof Integer);
        assertTrue("o incorrect", o.equals(new Integer(3)));

        e = ExpressionFactory.createExpression("list[loc+1]");

        jc.getVars().put("loc", new Integer(1));
        o = e.evaluate(jc);

        assertTrue("o not instanceof Integer", o instanceof Integer);
        assertTrue("o incorrect", o.equals(new Integer(3)));

        /*
         * test array access
         */

        String[] args = {"hello", "there"};

        jc.getVars().put("array", args);

        e = ExpressionFactory.createExpression("array[0]");
        o = e.evaluate(jc);

        assertTrue("array[0]", o.equals("hello"));

        jc.getVars().put("zero", new Integer(0));

        /*
         * to think that this was an intentional syntax...
         */
        e = ExpressionFactory.createExpression("array.0");
        o = e.evaluate(jc);

        assertTrue("array[0]", o.equals("hello"));

        /*
         * test map access
         */

        Map m = new HashMap();
        m.put("foo", "bar");

        jc.getVars().put("map", m);
        jc.getVars().put("key", "foo");

        e = ExpressionFactory.createExpression("map[\"foo\"]");
        o = e.evaluate(jc);
        assertTrue("map[foo]", o.equals("bar"));

        e = ExpressionFactory.createExpression("map[key]");
        o = e.evaluate(jc);
        assertTrue("map[key]", o.equals("bar"));

        /*
         *  test bean access
         */

        jc.getVars().put("foo", new Foo());

        e = ExpressionFactory.createExpression("foo[\"bar\"]");
        o = e.evaluate(jc);

        assertTrue("foo['bar']", o.equals(GET_METHOD_STRING));

        e = ExpressionFactory.createExpression("foo[\"bar\"] == foo.bar");
        o = e.evaluate(jc);

        assertTrue("foo['bar'] == foo.bar", o.equals(Boolean.TRUE));

    }

    public void testMulti()
         throws Exception
    {
        /*
         *  tests a simple property expression
         */

        Expression e = ExpressionFactory.createExpression("foo.innerFoo.bar()");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("o incorrect", o.equals(METHOD_STRING));
    }

    public void testBoolean()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo.convertBoolean(a==b)");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        jc.getVars().put("a", Boolean.TRUE);
        jc.getVars().put("b", Boolean.FALSE);

        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("1 : o incorrect", o.equals("Boolean : false"));

        e = ExpressionFactory.createExpression("foo.convertBoolean(a==true)");
        o = e.evaluate(jc);
        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("2 : o incorrect", o.equals("Boolean : true"));

        e = ExpressionFactory.createExpression("foo.convertBoolean(a==false)");
        o = e.evaluate(jc);
        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("3 : o incorrect", o.equals("Boolean : false"));

        e = ExpressionFactory.createExpression("foo.convertBoolean(true==false)");
        o = e.evaluate(jc);
        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("4 : o incorrect", o.equals("Boolean : false"));

        e = ExpressionFactory.createExpression("true eq false");
        o = e.evaluate(jc);
        assertTrue("true eq false", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("true ne false");
        o = e.evaluate(jc);
        assertTrue("true ne false", o.equals(Boolean.TRUE));
    }

    public void testStringLit()
         throws Exception
    {
        /*
         *  tests a simple property expression
         */

        Expression e = ExpressionFactory.createExpression("foo.get(\"woogie\")");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof String", o instanceof String);
        assertTrue("o incorrect", o.equals("Repeat : woogie"));
    }

    public void testExpression()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("a == b");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo() );
        jc.getVars().put("a", Boolean.TRUE);
        jc.getVars().put("b", Boolean.FALSE);
        jc.getVars().put("num", new Integer(5));

        Object o = e.evaluate(jc);

        assertTrue("1 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("a==true");
        o = e.evaluate(jc);
        assertTrue("2 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("a==false");
        o = e.evaluate(jc);
        assertTrue("3 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("true==false");
        o = e.evaluate(jc);
        assertTrue("4 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("num < 3");
        o = e.evaluate(jc);
        assertTrue("5 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("num <= 5");
        o = e.evaluate(jc);
        assertTrue("6 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("num >= 5");
        o = e.evaluate(jc);
        assertTrue("7 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("num > 4");
        o = e.evaluate(jc);
        assertTrue("8 : o incorrect", o.equals(Boolean.TRUE));

//
//   $$$ GMJ - trying to be spec conformant re addition means no string concat.
//         so get rid of it for the moment.  Will certainly revisit
//
//        e = ExpressionFactory.createExpression("\"foo\" + \"bar\" == \"foobar\"");
//        o = e.evaluate(jc);
//        assertTrue("9 : o incorrect", o.equals(Boolean.TRUE));

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

        Expression e = ExpressionFactory.createExpression("empty nullthing");
        Object o = e.evaluate(jc);
        assertTrue("1 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty string");
        o = e.evaluate(jc);
        assertTrue("2 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty array");
        o = e.evaluate(jc);
        assertTrue("3 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty map");
        o = e.evaluate(jc);
        assertTrue("4 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty list");
        o = e.evaluate(jc);
        assertTrue("5 : o incorrect", o.equals(Boolean.TRUE));

        e = ExpressionFactory.createExpression("empty longstring");
        o = e.evaluate(jc);
        assertTrue("6 : o incorrect", o.equals(Boolean.FALSE));

        e = ExpressionFactory.createExpression("not empty longstring");
        o = e.evaluate(jc);
        assertTrue("7 : o incorrect", o.equals(Boolean.TRUE));

		e = ExpressionFactory.createExpression("empty set");
		o = e.evaluate(jc);
		assertTrue("8 : o incorrect", o.equals(Boolean.TRUE));

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

        assertExpression(jc, "size(s)", new Integer(5));
        assertExpression(jc, "size(array)", new Integer(5));
        assertExpression(jc, "size(list)", new Integer(5));
        assertExpression(jc, "size(map)", new Integer(5));
        assertExpression(jc, "size(list)", new Integer(5));
        assertExpression(jc, "list.size()", new Integer(5));
        assertExpression(jc, "map.size()", new Integer(5));

        assertExpression(jc, "list.get(size(list) - 1)", "5");
        assertExpression(jc, "list[size(list) - 1]", "5");
        assertExpression(jc, "list.get(list.size() - 1)", "5");
    }

    public void testSizeAsProperty() throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("map", Collections.singletonMap( "size", "cheese"));

        assertExpression(jc, "map['size']", "cheese");
// PR - unsure whether or not we should support map.size or force usage of the above 'escaped' version        
//        assertExpression(jc, "map.size", "cheese");
    }

    /**
      *  test some String method calls
      */
    public void testStringMethods()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", "abcdef");

        assertExpression(jc, "foo.substring(3)", "def");
        assertExpression(jc, "foo.substring(0,(size(foo)-3))", "abc");
        assertExpression(jc, "foo.substring(0,size(foo)-3)", "abc");
        assertExpression(jc, "foo.substring(0,foo.length()-3)", "abc");
    }



    /**
      *  test some simple mathematical calculations
      */
    public void testCalculations()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo + 2");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Integer(2) );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Long", o instanceof Long);
        assertEquals("o incorrect", new Long(4), o);

        e = ExpressionFactory.createExpression("3 + 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(6), o );

        e = ExpressionFactory.createExpression("3 + 3 + foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(8), o );

        e = ExpressionFactory.createExpression("3 * 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(9), o );

        e = ExpressionFactory.createExpression("3 * 3 + foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(11), o );

        e = ExpressionFactory.createExpression("3 * 3 - foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", new Long(7), o );

        /*
         * test some floaty stuff
         */
        e = ExpressionFactory.createExpression("3 * \"3.0\"");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(9), o );

        e = ExpressionFactory.createExpression("3 * 3.0");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(9), o );

        /*
         *  test / and %
         */

        e = ExpressionFactory.createExpression("6 / 3");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(6/3), o);

        e = ExpressionFactory.createExpression("6.4 / 3");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(6.4 / 3), o);

        e = ExpressionFactory.createExpression("0 / 3");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(0 / 3), o);

        e = ExpressionFactory.createExpression("3 / 0");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(0), o);

        e = ExpressionFactory.createExpression("4 % 3");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Long(1), o);

        e = ExpressionFactory.createExpression("4.8 % 3");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Double(4.8 % 3), o);

        /*
         * test to ensure new string cat works
         */

        e = ExpressionFactory.createExpression("stringy + 2");

        jc.getVars().put("stringy", "thingy" );

        o = e.evaluate(jc);
        assertTrue("o not instanceof String", o instanceof String);
        assertEquals("o incorrect", "thingy2", o);

        /*
         * test new null coersion
         */

        e = ExpressionFactory.createExpression("imanull + 2");

        jc.getVars().put("imanull", null );

        o = e.evaluate(jc);
        assertEquals("o incorrect", new Long(2), o);

        e = ExpressionFactory.createExpression("imanull + imanull");
        o = e.evaluate(jc);
        assertEquals("o incorrect", new Long(0), o);

    }

    /**
      *  test some simple conditions
      */
    public void testConditions()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo == 2");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Integer(2) );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o);

        e = ExpressionFactory.createExpression("2 == 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("3 == foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("3 != foo");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("foo != 2");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );
    }

    /**
      *  test some simple conditions
      */
    public void testNotConditions()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("!x");
        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();
        jc.getVars().put("x", Boolean.TRUE );
        jc.getVars().put("foo", foo );
        jc.getVars().put("bar", "true" );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.FALSE, o);

        e = ExpressionFactory.createExpression("x");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("!bar");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("!foo.isSimple()");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("foo.isSimple()");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("!foo.simple");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("foo.simple");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("foo.getCheeseList().size() == 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("foo.cheeseList.size() == 3");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
    }


    /**
      *  GMJ : disabled - need to fix
      *
      *  test some simple conditions
      */
    public void dontDoTestNotConditionsWithDots()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("x.a == true");
        e.addPostResolver(new FlatResolver());
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("x.a", Boolean.TRUE );
        jc.getVars().put("x.b", Boolean.FALSE );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o );

        e = ExpressionFactory.createExpression("!x.a");
        e.addPreResolver(new FlatResolver());
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o);

        e = ExpressionFactory.createExpression("!x.b");
        e.addPreResolver(new FlatResolver());
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
    }

    /**
      *  test some simple conditions
      */
    public void testComparisons()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo.indexOf('quick') > 0");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", "the quick and lazy fox" );
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o);

        e = ExpressionFactory.createExpression("foo.indexOf('bar') >= 0");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.FALSE, o );

        e = ExpressionFactory.createExpression("foo.indexOf('bar') < 0");
        o = e.evaluate(jc);

        assertEquals("o incorrect", Boolean.TRUE, o );
    }



    /**
      *  test some null conditions
      */
    public void testNull()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("bar", new Integer(2) );

        Expression e = ExpressionFactory.createExpression("empty foo");
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.TRUE, o);

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

        Expression e = ExpressionFactory.createExpression("foo == ''");
        Object o = e.evaluate(jc);

        assertTrue("o not instanceof Boolean", o instanceof Boolean);
        assertEquals("o incorrect", Boolean.FALSE, o);


        assertExpression(jc, "bar == ''", Boolean.TRUE);
        assertExpression(jc, "barnotexist == ''", Boolean.FALSE);
        assertExpression(jc, "empty bar", Boolean.TRUE);
        assertExpression(jc, "bar.length() == 0", Boolean.TRUE);
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
      *  test some simple double array lookups
      */
    public void testDoubleArrays()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo[0][1]");
        JexlContext jc = JexlHelper.createContext();

        Object[][] foo = new Object[2][2];
        foo[0][0] = "one";
        foo[0][1] = "two";

        jc.getVars().put("foo", foo );
        Object o = e.evaluate(jc);

        assertEquals("o incorrect", "two", o);
    }

    /**
      *  test variables with underscore names
      */
    public void testVariableNames()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo_bar");
        JexlContext jc = JexlHelper.createContext();


        jc.getVars().put("foo_bar", "123" );
        Object o = e.evaluate(jc);

        assertEquals("o incorrect", "123", o);
    }

    /**
      *  test the use of dot notation to lookup map entries
      */
    public void testMapDot()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo.bar");
        JexlContext jc = JexlHelper.createContext();

        Map foo = new HashMap();
        foo.put( "bar", "123" );

        jc.getVars().put("foo", foo );
        Object o = e.evaluate(jc);

        assertEquals("o incorrect", "123", o);
    }

    /**
     *  Tests string literals
     */
    public void testStringLiterals()
        throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo == \"bar\"");
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", "bar" );

        Object o = e.evaluate(jc);

        assertTrue("o incorrect", Boolean.TRUE.equals(o));

        assertExpression(jc, "foo == 'bar'", Boolean.TRUE);
    }

    /**
      *  test the use of an int based property
      */
    public void testIntProperty()
         throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foo.count");
        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();

        // lets check the square function first..
        assertEquals(4, foo.square(2));
        assertEquals(4, foo.square(-2));

        jc.getVars().put("foo", foo );
        Object o = e.evaluate(jc);

        assertEquals("o incorrect", new Integer(5), o);

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

    public void testArrayProperty()
        throws Exception
    {
        Expression bracketForm =
            ExpressionFactory.createExpression("foo.array[1]");

        Expression dotForm =
            ExpressionFactory.createExpression("foo.array.1");

        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();

        jc.getVars().put("foo", foo );

        Object o1 = bracketForm.evaluate(jc);
        assertEquals("bracket form failed", GET_METHOD_ARRAY[1], o1);

        Object o2 = dotForm.evaluate(jc);
        assertEquals("dot form failed", GET_METHOD_ARRAY[1], o2);


        bracketForm =
            ExpressionFactory.createExpression("foo.array2[1][1]");

//        dotForm =
//            ExpressionFactory.createExpression("foo.array2.1.1");

        jc = JexlHelper.createContext();

        jc.getVars().put("foo", foo );

        o1 = bracketForm.evaluate(jc);
        assertEquals("bracket form failed", GET_METHOD_ARRAY2[1][1], o1);

//        o2 = dotForm.evaluate(jc);
//        assertEquals("dot form failed", GET_METHOD_ARRAY2[1][1], o2);
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
        catch (IndexOutOfBoundsException e) {
            // expected behaviour
        }
        catch (Exception e) {
            throw e;
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
        Map m = Collections.singletonMap("aList", Collections.EMPTY_LIST);

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
    }

    public void testResolver()
        throws Exception
    {
        /*
         * first, a simple override
         */

        Expression expr =
            ExpressionFactory.createExpression("foo.bar");

        expr.addPreResolver(new FlatResolver());

        JexlContext jc = JexlHelper.createContext();

        Foo foo = new Foo();

        jc.getVars().put("foo.bar", "flat value");
        jc.getVars().put("foo", foo );

        Object o = expr.evaluate(jc);

        assertEquals("flat override", o,"flat value");

        /*
         * now, let the resolver not find it and have it drop to jexl
         */

        expr =
            ExpressionFactory.createExpression("foo.bar.length()");

        expr.addPreResolver(new FlatResolver());

        o = expr.evaluate(jc);

        assertEquals("flat override 1", o,new Integer(GET_METHOD_STRING.length()));

        /*
         * now, let the resolver not find it and NOT drop to jexl
         */

        expr =
            ExpressionFactory.createExpression("foo.bar.length()");

        expr.addPreResolver(new FlatResolver(false));

        o = expr.evaluate(jc);

        assertEquals("flat override 2", o, null);

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
