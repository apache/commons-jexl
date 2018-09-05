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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for projections
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ProjectionTest extends JexlTestCase {
    public ProjectionTest() {
        super("ProjectionTest");
    }

    @Test
    public void testMapProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : m.(key)) s.add(i); s");
        JexlContext jc = new MapContext();

        Map<String, String> test = new HashMap<String, String>();
        test.put("foo", "bar");
        test.put("eat", "food");

        jc.set("m", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("foo"));
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains("bar"));
    }

    @Test
    public void testMapProjection2() throws Exception {
        JexlScript e = JEXL.createScript("var s = {:}; for (var i : m.(key,value)) s.put(i.key,i.value); s");
        JexlContext jc = new MapContext();

        Map<String, String> test = new HashMap<String, String>();
        test.put("foo", "bar");
        test.put("eat", "food");

        jc.set("m", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsKey("foo"));
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsValue("food"));
    }

    @Test
    public void testMapProjection3() throws Exception {
        JexlScript e = JEXL.createScript("var s = {:}; for (var i : m.(key.length(),key,value)) s.put(i[1],i[2]); s");
        JexlContext jc = new MapContext();

        Map<String, String> test = new HashMap<String, String>();
        test.put("foo", "bar");
        test.put("eat", "food");

        jc.set("m", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsKey("foo"));
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsValue("food"));
    }

    @Test
    public void testLambdaMapProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : m.((key,value) -> {value})) s.add(i); s");
        JexlContext jc = new MapContext();

        Map<String, String> test = new HashMap<String, String>();
        test.put("foo", "bar");
        test.put("eat", "food");

        jc.set("m", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("bar"));
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains("eat"));
    }

    @Test
    public void testLambdaMapProjection2() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : m.(x -> {x.value})) s.add(i); s");
        JexlContext jc = new MapContext();

        Map<String, String> test = new HashMap<String, String>();
        test.put("foo", "bar");
        test.put("eat", "food");

        jc.set("m", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("bar"));
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains("eat"));
    }

    @Test
    public void testListProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : l.(key -> {key.length()})) s.add(i); s");
        JexlContext jc = new MapContext();

        List<String> test = new ArrayList<String>();
        test.add("banana");
        test.add("apple");

        jc.set("l", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(5));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(6));
    }

    @Test
    public void testIndexedListProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : l.((x, key) -> {x})) s.add(i); s");
        JexlContext jc = new MapContext();

        List<String> test = new ArrayList<String>();
        test.add("apple");
        test.add("banana");

        jc.set("l", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(0));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(1));
    }

}
