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

import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for blocks
 * @since 1.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class InlinePropertyTest extends JexlTestCase {

    /**
     * Create the test
     */
    public InlinePropertyTest() {
        super("InlinePropertyTest");
    }

    public class Address {

        protected String postalCode;
        protected String city;

        protected Map<String, Object> tags;
        protected List<String> lines;
        protected List<AbstractMap.SimpleEntry> keys;

        public Address() {
           tags = new HashMap<String, Object> ();

           lines = new ArrayList<String> ();
           keys = new ArrayList<AbstractMap.SimpleEntry> ();

           lines.add("Test");

           keys.add(new AbstractMap.SimpleEntry(1, "Test"));
           keys.add(new AbstractMap.SimpleEntry(2, "Test 2"));
        }

        public String getPostalCode() {
           return postalCode;
        }

        public void setPostalCode(String value) {
           postalCode = value;
        }

        public String getCity() {
           return city;
        }

        public void setCity(String value) {
           city = value;
        }

        public Map<String, Object> getTags() {
           return tags;
        }

        public List<AbstractMap.SimpleEntry> getKeys() {
           return keys;
        }

        public List<String> getLines() {
           return lines;
        }

    }

    @Test
    public void inlinePropertySimple() throws Exception {
        JexlScript e = JEXL.createScript("addr { PostalCode : '123456'}; addr.PostalCode");
        JexlContext jc = new MapContext();
        jc.set("addr", new Address());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "123456", o);
    }

    @Test
    public void inlinePropertySimpleString() throws Exception {
        JexlScript e = JEXL.createScript("addr { 'postalCode' : '123456'}; addr.postalCode");
        JexlContext jc = new MapContext();
        jc.set("addr", new Address());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "123456", o);
    }

    @Test
    public void inlinePropertySimpleJxlt() throws Exception {
        JexlScript e = JEXL.createScript("var name = 'postalCode'; addr { `${name}` : '123456'}; addr.postalCode");
        JexlContext jc = new MapContext();
        jc.set("addr", new Address());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "123456", o);
    }

    @Test
    public void inlinePropertyNestedProp() throws Exception {
        JexlScript e = JEXL.createScript("addr { tags { PostalCode : '123456'}}; addr.tags.PostalCode");
        JexlContext jc = new MapContext();
        jc.set("addr", new Address());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "123456", o);
    }

    @Test
    public void inlinePropertyNestedArray() throws Exception {
        JexlScript e = JEXL.createScript("var i = 0; addr { keys[i] { value : '123456'}}; addr.keys[0].value");
        JexlContext jc = new MapContext();
        jc.set("addr", new Address());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "123456", o);
    }

    @Test
    public void inlinePropertyNestedArray2() throws Exception {
        JexlScript e = JEXL.createScript("var i = 0; addr { keys { [i] { value : '123456'}}}; addr.keys[0].value");
        JexlContext jc = new MapContext();
        jc.set("addr", new Address());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "123456", o);
    }

    @Test
    public void inlinePropertyArray() throws Exception {
        JexlScript e = JEXL.createScript("var i = 0; addr.lines{[i] : '123456'}; addr.lines[0]");
        JexlContext jc = new MapContext();
        jc.set("addr", new Address());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", "123456", o);
    }


}
