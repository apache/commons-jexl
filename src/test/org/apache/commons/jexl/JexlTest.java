/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", "Jexl" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.jexl;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.jexl.parser.Parser;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 *  Simple testcases
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: JexlTest.java,v 1.1 2002/04/26 04:45:28 geirm Exp $
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

        e = ExpressionFactory.createExpression("\"foo\" + \"bar\" == \"foobar\"");
        o = e.evaluate(jc);
        assertTrue("9 : o incorrect", o.equals(Boolean.TRUE));

    }

    public void testEmpty()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("string", "");
        jc.getVars().put("array", new Object[0]);
        jc.getVars().put("map", new HashMap());
        jc.getVars().put("list", new ArrayList());
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

    }

    public void testSize()
         throws Exception
    {
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("string", "five!");
        jc.getVars().put("array", new Object[5]);

        Map map = new HashMap();

        map.put("1", new Integer(1));
        map.put("2", new Integer(2));
        map.put("3", new Integer(3));
        map.put("4", new Integer(4));
        map.put("5", new Integer(5));

        jc.getVars().put("map", map);

        List list = new ArrayList();

        list.add("");
        list.add("");
        list.add("");
        list.add("");
        list.add("");

        jc.getVars().put("list", list);

        Expression e = ExpressionFactory.createExpression("size(string)");
        Object o = e.evaluate(jc);
        assertTrue("1 : o incorrect", o.equals(new Integer(5)));

        e = ExpressionFactory.createExpression("size(array)");
        o = e.evaluate(jc);
        assertTrue("2 : o incorrect", o.equals(new Integer(5)));

        e = ExpressionFactory.createExpression("size(map)");
        o = e.evaluate(jc);
        assertTrue("3 : o incorrect", o.equals(new Integer(5)));

        e = ExpressionFactory.createExpression("size(list)");
        o = e.evaluate(jc);
        assertTrue("4 : o incorrect", o.equals(new Integer(5)));
    }



    public class Foo
    {
        public String bar()
        {
            return METHOD_STRING;
        }

        public String getBar()
        {
            return GET_METHOD_STRING;
        }

        public Foo getInnerFoo()
        {
            return new Foo();
        }

        public String get(String arg)
        {
            return "Repeat : " + arg;
        }

        public String convertBoolean(boolean b)
        {
            return "Boolean : " + b;
        }
    }

}
