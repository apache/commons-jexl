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
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.jexl.parser.ParseException;
import org.apache.commons.jexl.resolver.FlatResolver;

/**
 *  Simple testcases
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: JexlTest.java,v 1.54 2004/08/20 08:22:20 dion Exp $
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
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Foo() );
        assertExpression(jc, "foo.bar()", METHOD_STRING);
    }

    /**
      *  test a simple method expression
      */
    public void testArrayAccess()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        /*
         *  test List access
         */

        List l = new ArrayList();
        l.add(new Integer(1));
        l.add(new Integer(2));
        l.add(new Integer(3));

        jc.getVars().put("list", l);

        assertExpression(jc, "list[1]", new Integer(2));
        assertExpression(jc, "list[1+1]", new Integer(3));
        jc.getVars().put("loc", new Integer(1));
        assertExpression(jc, "list[loc+1]", new Integer(3));

        /*
         * test array access
         */

        String[] args = {"hello", "there"};
        jc.getVars().put("array", args);
        assertExpression(jc, "array[0]", "hello");

        /*
         * to think that this was an intentional syntax...
         */
        assertExpression(jc, "array.0", "hello");

        /*
         * test map access
         */
        Map m = new HashMap();
        m.put("foo", "bar");

        jc.getVars().put("map", m);
        jc.getVars().put("key", "foo");

        assertExpression(jc, "map[\"foo\"]", "bar");
        assertExpression(jc, "map[key]", "bar");

        /*
         *  test bean access
         */
        jc.getVars().put("foo", new Foo());
        assertExpression(jc, "foo[\"bar\"]", GET_METHOD_STRING);
        assertExpression(jc, "foo[\"bar\"] == foo.bar", Boolean.TRUE);

    }

    public void testMulti()
         throws Exception
    {
        /*
         *  tests a simple property expression
         */
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Foo() );
        assertExpression(jc, "foo.innerFoo.bar()", METHOD_STRING);
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

        assertExpression(jc, "a == b", Boolean.FALSE);
        assertExpression(jc, "a==true", Boolean.TRUE);
        assertExpression(jc, "a==false", Boolean.FALSE);
        assertExpression(jc, "true==false", Boolean.FALSE);
        assertExpression(jc, "num < 3", Boolean.FALSE);
        assertExpression(jc, "num <= 5", Boolean.TRUE);
        assertExpression(jc, "num >= 5", Boolean.TRUE);
        assertExpression(jc, "num > 4", Boolean.TRUE);

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
        Expression e = null;
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Integer(2) );
        Object o = null;

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
    }

    /**
      *  test some simple conditions
      */
    public void testConditions()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", new Integer(2) );
        
        assertExpression(jc, "foo == 2", Boolean.TRUE);
        assertExpression(jc, "2 == 3", Boolean.FALSE);
        assertExpression(jc, "3 == foo", Boolean.FALSE);
        assertExpression(jc, "3 != foo", Boolean.TRUE);
        assertExpression(jc, "foo != 2", Boolean.FALSE);
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
      *  test some simple double array lookups
      */
    public void testDoubleArrays()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();

        Object[][] foo = new Object[2][2];
        foo[0][0] = "one";
        foo[0][1] = "two";

        jc.getVars().put("foo", foo );

        assertExpression(jc, "foo[0][1]", "two");
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

    public void testArrayProperty()
        throws Exception
    {
        Foo foo = new Foo();

        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("foo", foo );

        Expression bracketForm =
            ExpressionFactory.createExpression("foo.array[1]");

        Expression dotForm =
            ExpressionFactory.createExpression("foo.array.1");

        assertExpression(jc, "foo.array[1]", GET_METHOD_ARRAY[1]);
        assertExpression(jc, "foo.array.1", GET_METHOD_ARRAY[1]);
        assertExpression(jc, "foo.array2[1][1]", GET_METHOD_ARRAY2[1][1]);

//        dotForm =
//            ExpressionFactory.createExpression("foo.array2.1.1");
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

        assertExpression(jc, "2 > '1'", Boolean.TRUE);
        assertExpression(jc, "2 >= '1'", Boolean.TRUE);
        assertExpression(jc, "2 >= '2'", Boolean.TRUE);
        assertExpression(jc, "2 < '1'", Boolean.FALSE);
        assertExpression(jc, "2 <= '1'", Boolean.FALSE);
        assertExpression(jc, "2 <= '2'", Boolean.TRUE);
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
     * Ensures static methods on objects can be called.
     */
    public void testStaticMethodInvocation() throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("aBool", Boolean.FALSE);
        assertExpression(jc, "aBool.valueOf('true')", Boolean.TRUE);
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
        }
        catch (ParseException pe)
        {
            System.err.println("Expecting a parse exception: " + pe.getMessage());
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
