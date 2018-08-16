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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.StringTokenizer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the foreach statement
 * @since 1.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ForEachTest extends JexlTestCase {

    /** create a named test */
    public ForEachTest() {
        super("ForEachTest");
    }

    @Test
    public void testForEachWithEmptyStatement() throws Exception {
        JexlScript e = JEXL.createScript("for(item : list) ;");
        JexlContext jc = new MapContext();
        jc.set("list", Collections.emptyList());

        Object o = e.execute(jc);
        Assert.assertNull("Result is not null", o);
    }

    @Test
    public void testForEachWithEmptyList() throws Exception {
        JexlScript e = JEXL.createScript("for(item : list) 1+1");
        JexlContext jc = new MapContext();
        jc.set("list", Collections.emptyList());

        Object o = e.execute(jc);
        Assert.assertNull("Result is not null", o);
    }

    @Test
    public void testForEachWithArray() throws Exception {
        JexlScript e = JEXL.createScript("for(item : list) item");
        JexlContext jc = new MapContext();
        jc.set("list", new Object[]{"Hello", "World"});
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "World", o);
    }

    @Test
    public void testForEachWithCollection() throws Exception {
        JexlScript e = JEXL.createScript("for(var item : list) item");
        JexlContext jc = new MapContext();
        jc.set("list", Arrays.asList(new Object[]{"Hello", "World"}));
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "World", o);
    }

    @Test
    public void testForEachWithEnumeration() throws Exception {
        JexlScript e = JEXL.createScript("for(var item : list) item");
        JexlContext jc = new MapContext();
        jc.set("list", new StringTokenizer("Hello,World", ","));
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "World", o);
    }

    @Test
    public void testForEachWithIterator() throws Exception {
        JexlScript e = JEXL.createScript("for(var item : list) item");
        JexlContext jc = new MapContext();
        jc.set("list", Arrays.asList(new Object[]{"Hello", "World"}).iterator());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "World", o);
    }

    @Test
    public void testForEachWithMap() throws Exception {
        JexlScript e = JEXL.createScript("for(item : list) item");
        JexlContext jc = new MapContext();
        Map<?, ?> map = System.getProperties();
        String lastProperty = (String) new ArrayList<Object>(map.values()).get(System.getProperties().size() - 1);
        jc.set("list", map);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", lastProperty, o);
    }

    @Test
    public void testForEachWithBlock() throws Exception {
        JexlScript exs0 = JEXL.createScript("for(var i : list) { x = x + i; }");
        JexlContext jc = new MapContext();
        jc.set("list", new Object[]{2, 3});
            jc.set("x", new Integer(1));
        Object o = exs0.execute(jc);
            Assert.assertEquals("Result is wrong", new Integer(6), o);
            Assert.assertEquals("x is wrong", new Integer(6), jc.get("x"));
        }

    @Test
    public void testForEachWithListExpression() throws Exception {
        JexlScript e = JEXL.createScript("for(var item : list.keySet()) item");
        JexlContext jc = new MapContext();
        Map<?, ?> map = System.getProperties();
        String lastKey = (String) new ArrayList<Object>(map.keySet()).get(System.getProperties().size() - 1);
        jc.set("list", map);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", lastKey, o);
    }

    @Test
    public void testForEachWithProperty() throws Exception {
        JexlScript e = JEXL.createScript("for(var item : list.cheeseList) item");
        JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "brie", o);
    }

    @Test
    public void testForEachWithIteratorMethod() throws Exception {
        JexlScript e = JEXL.createScript("for(var item : list.cheezy) item");
        JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "brie", o);
    }

    @Test
    public void testForEachBreakMethod() throws Exception {
        JexlScript e = JEXL.createScript(
                "var rr = -1; for(var item : [1, 2, 3 ,4 ,5, 6]) { if (item == 3) { rr = item; break; }} rr"
        );
        JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", 3, o);
    }

    @Test
    public void testForEachContinueMethod() throws Exception {
        JexlScript e = JEXL.createScript(
                "var rr = 0; for(var item : [1, 2, 3 ,4 ,5, 6]) { if (item <= 3) continue; rr = rr + item;}"
        );
        JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", 15, o);
    }

    @Test
    public void testForEachRemoveMethod() throws Exception {
        JexlScript e = JEXL.createScript(
                "var set = {1, 2, 3 ,4 ,5, 6}; for(var item : set) { if (item <= 3) remove} size(set)"
        );
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", 3, o);
    }

    @Test
    public void testForEachContinueBroken() throws Exception {
        try {
            JexlScript e = JEXL.createScript("var rr = 0; continue;");
            Assert.fail("continue is out of loop!");
        } catch (JexlException.Parsing xparse) {
            String str = xparse.detailedMessage();
            Assert.assertTrue(str.contains("continue"));
        }
    }

    @Test
    public void testForEachRemoveBroken() throws Exception {
        try {
            JexlScript e = JEXL.createScript("var rr = 0; remove;");
            Assert.fail("remove is out of loop!");
        } catch (JexlException.Parsing xparse) {
            String str = xparse.detailedMessage();
            Assert.assertTrue(str.contains("remove"));
        }
    }

    @Test
    public void testForEachBreakBroken() throws Exception {
        try {
            JexlScript e = JEXL.createScript("if (true) { break; }");
            Assert.fail("break is out of loop!");
        } catch (JexlException.Parsing xparse) {
            String str = xparse.detailedMessage();
            Assert.assertTrue(str.contains("break"));
        }
    }
}
