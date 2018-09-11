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

    protected Map<String, String> testMap;
    protected List<String> test;


    public ProjectionTest() {
        super("ProjectionTest");

        testMap = new HashMap<String, String>();
        testMap.put("foo", "bar");
        testMap.put("eat", "food");
        testMap.put("drink", "water");

        test = new ArrayList<String>();
        test.add("apple");
        test.add("banana");
        test.add("kiwi");
    }

    @Test
    public void testMapProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : ...m.[key]) s.add(i); s");
        JexlContext jc = new MapContext();
        jc.set("m", testMap);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("foo"));
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains("bar"));

        e = JEXL.createScript("var s = {}; for (var i : ...m.[value]) s.add(i); s");
        o = e.execute(jc);
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains("foo"));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("bar"));
    }

    @Test
    public void testMapProjection2() throws Exception {
        JexlScript e = JEXL.createScript("var s = {:}; for (var i : ...m.[key,value]) s.put(i[0],i[1]); s");
        JexlContext jc = new MapContext();
        jc.set("m", testMap);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsKey("foo"));
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsValue("food"));

        e = JEXL.createScript("var s = {:}; for (var i : ...m.{value:key}) s.put(i.key,i.value); s");
        o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsKey("bar"));
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsValue("eat"));
    }

    @Test
    public void testMapProjection3() throws Exception {
        JexlScript e = JEXL.createScript("var s = {:}; for (var i : ...m.[key.length(),key,value]) s.put(i[1],i[2]); s");
        JexlContext jc = new MapContext();
        jc.set("m", testMap);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsKey("foo"));
        Assert.assertEquals(Boolean.TRUE, ((Map)o).containsValue("food"));
    }

    @Test
    public void testLambdaMapProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : ...m.[(index, entry) -> {entry.value}]) s.add(i); s");
        JexlContext jc = new MapContext();
        jc.set("m", testMap);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("bar"));
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains("eat"));
    }

    @Test
    public void testLambdaMapProjection2() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : ...m.[entry -> {entry.value}]) s.add(i); s");
        JexlContext jc = new MapContext();
        jc.set("m", testMap);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("bar"));
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains("eat"));
    }

    @Test
    public void testListProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : ...fruits.[value -> {value.length()}]) s.add(i); s");
        JexlContext jc = new MapContext();
        jc.set("fruits", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(4));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(6));
    }

    @Test
    public void testIndexedListProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : ...fruits.[(index, value) -> {index}]) s.add(i); s");
        JexlContext jc = new MapContext();
        jc.set("fruits", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(0));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(1));

        e = JEXL.createScript("var s = {}; for (var i : ...fruits.[(index, value) -> {value}]) s.add(i); s");
        o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("banana"));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("kiwi"));

        e = JEXL.createScript("var s = {}; for (var i : ...fruits.[(index, value, dummy) -> {empty dummy ? value : index}]) s.add(i); s");
        o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("banana"));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("apple"));

        e = JEXL.createScript("var s = {}; for (var i : ...fruits.[(index, value) -> {index},(index, value) -> {value}]) s.add(i[1]); s");
        o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("banana"));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("kiwi"));
    }

    @Test
    public void testListSelection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : ...fruits.(value -> {value.length() >= 5})) s.add(i); s");
        JexlContext jc = new MapContext();
        jc.set("fruits", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains("banana"));
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains("kiwi"));
    }

    @Test
    public void testListSelectionProjection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : ...fruits.(value -> {value.length() >= 5}).[value -> {value.length()}]) s.add(i); s");
        JexlContext jc = new MapContext();
        jc.set("fruits", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains(4));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(6));
    }

    @Test
    public void testListProjectionSelection() throws Exception {
        JexlScript e = JEXL.createScript("var s = {}; for (var i : ...fruits.[value->{value.length()}].(len->{len >= 5})) s.add(i); s");
        JexlContext jc = new MapContext();
        jc.set("fruits", test);

        Object o = e.execute(jc);
        Assert.assertEquals(Boolean.FALSE, ((Set)o).contains(4));
        Assert.assertEquals(Boolean.TRUE, ((Set)o).contains(6));
    }

}
