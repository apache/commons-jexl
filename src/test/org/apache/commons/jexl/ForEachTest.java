/*
 * Copyright 2002-2006 The Apache Software Foundation.
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
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.TestCase;

/**
 * Tests for the foreach statement
 * @author Dion Gillard
 * @since 1.0.1
 */
public class ForEachTest extends TestCase {

    /** create a named test */
    public ForEachTest(String name) {
        super(name);
    }

    public void testForEachWithEmptyStatement() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list) ;");
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate(jc);
        assertNull("Result is not null", o);
    }

    public void testForEachWithEmptyList() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list) 1+1");
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate(jc);
        assertNull("Result is not null", o);
    }

    public void testForEachWithArray() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list) item");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("list", new Object[] {"Hello", "World"});
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "World", o);
    }

    public void testForEachWithCollection() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list) item");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("list", Arrays.asList(new Object[] {"Hello", "World"}));
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "World", o);
    }

    public void testForEachWithEnumeration() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list) item");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("list", new StringTokenizer("Hello,World", ","));
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "World", o);
    }

    public void testForEachWithIterator() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list) item");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("list", Arrays.asList(new Object[] {"Hello", "World"}).iterator());
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "World", o);
    }

    public void testForEachWithMap() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list) item");
        JexlContext jc = JexlHelper.createContext();
        Map map = System.getProperties();
        String lastProperty = (String) new ArrayList(map.values()).get(System.getProperties().size() - 1);
        jc.getVars().put("list", map);
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", lastProperty, o);
    }

    public void testForEachWithBlock() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list) { x = x + item; }");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("list", new Object[] {"1", "1"});
        jc.getVars().put("x", new Integer(0));
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(2), o);
        assertEquals("x is wrong", new Long(2), jc.getVars().get("x"));
    }

    public void testForEachWithListExpression() throws Exception {
        Expression e = ExpressionFactory.createExpression("foreach (item in list.keySet()) item");
        JexlContext jc = JexlHelper.createContext();
        Map map = System.getProperties();
        String lastKey = (String) new ArrayList(map.keySet()).get(System.getProperties().size() - 1);
        jc.getVars().put("list", map);
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", lastKey, o);
    }
    
    public void testForEachWithProperty() throws Exception
    {
        Expression e = ExpressionFactory.createExpression("foreach (item in list.cheeseList) item");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("list", new Foo());
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "brie", o);
    }
}
