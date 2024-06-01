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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.jupiter.api.Test;

/**
 * Tests for the foreach statement
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ForEachTest extends JexlTestCase {

    /** Create a named test */
    public ForEachTest() {
        super("ForEachTest");
    }

    @Test
    public void testForEachBreakBroken() throws Exception {
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> JEXL.createScript("if (true) { break; }"),
                "break is out of loop!");
        assertTrue(xparse.detailedMessage().contains("break"));

    }

    @Test
    public void testForEachBreakMethod() throws Exception {
        final JexlScript e = JEXL.createScript(
                "var rr = -1; for(var item : [1, 2, 3 ,4 ,5, 6]) { if (item == 3) { rr = item; break; }} rr"
        );
        final JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        final Object o = e.execute(jc);
        assertEquals(3, o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachContinueBroken() throws Exception {
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> JEXL.createScript("var rr = 0; continue;"),
                "continue is out of loop!");
        assertTrue(xparse.detailedMessage().contains("continue"));
    }

    @Test
    public void testForEachContinueMethod() throws Exception {
        final JexlScript e = JEXL.createScript(
                "var rr = 0; for(var item : [1, 2, 3 ,4 ,5, 6]) { if (item <= 3) continue; rr = rr + item;}"
        );
        final JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        final Object o = e.execute(jc);
        assertEquals(15, o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachWithArray() throws Exception {
        final JexlScript e = JEXL.createScript("for(item : list) item");
        final JexlContext jc = new MapContext();
        jc.set("list", new Object[]{"Hello", "World"});
        final Object o = e.execute(jc);
        assertEquals("World", o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachWithBlock() throws Exception {
        final JexlScript exs0 = JEXL.createScript("for(var in : list) { x = x + in; }");
        final JexlContext jc = new MapContext();
        jc.set("list", new Object[]{2, 3});
            jc.set("x", Integer.valueOf(1));
        final Object o = exs0.execute(jc);
            assertEquals(Integer.valueOf(6), o, "Result is wrong");
            assertEquals(Integer.valueOf(6), jc.get("x"), "x is wrong");
        }

    @Test
    public void testForEachWithCollection() throws Exception {
        final JexlScript e = JEXL.createScript("for(var item : list) item");
        final JexlContext jc = new MapContext();
        jc.set("list", Arrays.asList("Hello", "World"));
        final Object o = e.execute(jc);
        assertEquals("World", o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachWithEmptyList() throws Exception {
        final JexlScript e = JEXL.createScript("for(item : list) 1+1");
        final JexlContext jc = new MapContext();
        jc.set("list", Collections.emptyList());

        final Object o = e.execute(jc);
        assertNull(o);
    }

    @Test
    public void testForEachWithEmptyStatement() throws Exception {
        final JexlScript e = JEXL.createScript("for(item : list) ;");
        final JexlContext jc = new MapContext();
        jc.set("list", Collections.emptyList());

        final Object o = e.execute(jc);
        assertNull(o);
    }

    @Test
    public void testForEachWithEnumeration() throws Exception {
        final JexlScript e = JEXL.createScript("for(var item : list) item");
        final JexlContext jc = new MapContext();
        jc.set("list", new StringTokenizer("Hello,World", ","));
        final Object o = e.execute(jc);
        assertEquals("World", o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachWithIterator() throws Exception {
        final JexlScript e = JEXL.createScript("for(var item : list) item");
        final JexlContext jc = new MapContext();
        jc.set("list", Arrays.asList("Hello", "World").iterator());
        final Object o = e.execute(jc);
        assertEquals("World", o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachWithIteratorMethod() throws Exception {
        final JexlScript e = JEXL.createScript("for(var item : list.cheezy) item");
        final JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        final Object o = e.execute(jc);
        assertEquals("brie", o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachWithListExpression() throws Exception {
        final JexlScript e = JEXL.createScript("for(var item : list.keySet()) item");
        final JexlContext jc = new MapContext();
        final Map<?, ?> map = System.getProperties();
        final String lastKey = (String) new ArrayList<Object>(map.keySet()).get(System.getProperties().size() - 1);
        jc.set("list", map);
        final Object o = e.execute(jc);
        assertEquals(lastKey, o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachWithMap() throws Exception {
        final JexlScript e = JEXL.createScript("for(item : list) item");
        final JexlContext jc = new MapContext();
        final Map<?, ?> map = System.getProperties();
        final String lastProperty = (String) new ArrayList<Object>(map.values()).get(System.getProperties().size() - 1);
        jc.set("list", map);
        final Object o = e.execute(jc);
        assertEquals(lastProperty, o, "Result is not last evaluated expression");
    }

    @Test
    public void testForEachWithProperty() throws Exception {
        final JexlScript e = JEXL.createScript("for(var item : list.cheeseList) item");
        final JexlContext jc = new MapContext();
        jc.set("list", new Foo());
        final Object o = e.execute(jc);
        assertEquals("brie", o, "Result is not last evaluated expression");
    }

    @Test public void testForLoop0a() {
        final String src = "(l)->{ for(let x = 0; x < 4; ++x) { l.add(x); } }";
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final JexlScript script = jexl.createScript(src);
        final List<Integer> l = new ArrayList<>();
        final Object result = script.execute(null, l);
        assertNotNull(result);
        assertEquals(Arrays.asList(0, 1, 2, 3), l);
        final String resrc = toString(script);
        assertEquals(src, resrc);
    }

    @Test public void testForLoop0b0() {
        final String src = "(l)->{ for(let x = 0, y = 0; x < 4; ++x) l.add(x) }";
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final JexlScript script = jexl.createScript(src);
        final List<Integer> l = new ArrayList<>();
        final Object result = script.execute(null, l);
        assertNotNull(result);
        assertEquals(Arrays.asList(0, 1, 2, 3), l);
        final String resrc = toString(script);
        assertEquals(src, resrc);
    }
}
