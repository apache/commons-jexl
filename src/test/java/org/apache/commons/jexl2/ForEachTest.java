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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Tests for the foreach statement
 * @since 1.1
 */
public class ForEachTest extends JexlTestCase {

    /** create a named test */
    public ForEachTest(String name) {
        super(name);
    }

    public void testForEachWithEmptyStatement() throws Exception {
        Expression e = JEXL.createExpression("for(item : list) ;");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        assertNull("Result is not null", o);
    }

    public void testForEachWithEmptyList() throws Exception {
        Expression e = JEXL.createExpression("for(item : list) 1+1");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        assertNull("Result is not null", o);
    }

    public void testForEachWithArray() throws Exception {
        Expression e = JEXL.createExpression("for(item : list) item");
        JexlContext jc = new MapContext();
        jc.set("list", new Object[] {"Hello", "World"});
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "World", o);
    }

    public void testForEachWithCollection() throws Exception {
        Expression e = JEXL.createExpression("for(item : list) item");
        JexlContext jc = new MapContext();
        jc.set("list", Arrays.asList(new Object[] {"Hello", "World"}));
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "World", o);
    }

    public void testForEachWithEnumeration() throws Exception {
        Expression e = JEXL.createExpression("for(item : list) item");
        JexlContext jc = new MapContext();
        jc.set("list", new StringTokenizer("Hello,World", ","));
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "World", o);
    }

    public void testForEachWithIterator() throws Exception {
        Expression e = JEXL.createExpression("for(item : list) item");
        JexlContext jc = new MapContext();
        jc.set("list", Arrays.asList(new Object[] {"Hello", "World"}).iterator());
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "World", o);
    }

    public void testForEachWithMap() throws Exception {
        Expression e = JEXL.createExpression("for(item : list) item");
        JexlContext jc = new MapContext();
        Map<?, ?> map = System.getProperties();
        String lastProperty = (String) new ArrayList<Object>(map.values()).get(System.getProperties().size() - 1);
        jc.set("list", map);
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", lastProperty, o);
    }

    public void testForEachWithBlock() throws Exception {
        Expression exs0 = JEXL.createExpression("for(in : list) { x = x + in; }");
        Expression exs1 = JEXL.createExpression("foreach(item in list) { x = x + item; }");
        Expression []exs = { exs0, exs1 };
        JexlContext jc = new MapContext();
        jc.set("list", new Object[] {"2", "3"});
        for(int ex = 0; ex < exs.length; ++ex) {
            jc.set("x", new Integer(1));
            Object o = exs[ex].evaluate(jc);
            assertEquals("Result is wrong", new Integer(6), o);
            assertEquals("x is wrong", new Integer(6), jc.get("x"));
        }
    }

    public void testForEachWithListExpression() throws Exception {
        Expression e = JEXL.createExpression("for(item : list.keySet()) item");
        JexlContext jc = new MapContext();
        Map<?, ?> map = System.getProperties();
        String lastKey = (String) new ArrayList<Object>(map.keySet()).get(System.getProperties().size() - 1);
        jc.set("list", map);
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", lastKey, o);
    }
    
    public void testForEachWithProperty() throws Exception {
        Expression e = JEXL.createExpression("for(item : list.cheeseList) item");
        JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "brie", o);
    }
    
    public void testForEachWithIteratorMethod() throws Exception {
        Expression e = JEXL.createExpression("for(item : list.cheezy) item");
        JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        Object o = e.evaluate(jc);
        assertEquals("Result is not last evaluated expression", "brie", o);
    }
}