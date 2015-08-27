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
package org.apache.commons.jexl3.internal.introspection;

import org.apache.commons.jexl3.JexlTestCase;
import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for checking introspection discovery.
 *
 * @since 2.0
 */
public class DiscoveryTest extends JexlTestCase {
    public DiscoveryTest() {
        super("DiscoveryTest");
    }

    public static class Duck {
        private String value;
        private String eulav;

        public Duck(String v, String e) {
            value = v;
            eulav = e;
        }

        public String get(String prop) {
            if ("value".equals(prop)) {
                return value;
            }
            if ("eulav".equals(prop)) {
                return eulav;
            }
            return "no such property";
        }

        public void set(String prop, String v) {
            if ("value".equals(prop)) {
                value = v;
            } else if ("eulav".equals(prop)) {
                eulav = v;
            }
        }
    }

    public static class Bean {
        private String value;
        private String eulav;
        private boolean flag;

        public Bean(String v, String e) {
            value = v;
            eulav = e;
            flag = true;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String v) {
            value = v;
        }

        public String getEulav() {
            return eulav;
        }

        public void setEulav(String v) {
            eulav = v;
        }

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean f) {
            flag = f;
        }
    }

    @Test
    public void testBeanIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null, null);
        Bean bean = new Bean("JEXL", "LXEJ");

        JexlPropertyGet get = uber.getPropertyGet(bean, "value");
        JexlPropertySet set = uber.getPropertySet(bean, "value", "foo");
        Assert.assertTrue("bean property getter", get instanceof PropertyGetExecutor);
        Assert.assertTrue("bean property setter", set instanceof PropertySetExecutor);
        // introspector and uberspect should return same result
        Assert.assertEquals(get, uber.getPropertyGet(bean, "value"));
        Assert.assertEquals(set, uber.getPropertySet(bean, "value", "foo"));
        // different property should return different setter/getter
        Assert.assertFalse(get.equals(uber.getPropertyGet(bean, "eulav")));
        Assert.assertFalse(set.equals(uber.getPropertySet(bean, "eulav", "foo")));
        // setter returns argument
        Object bar = set.invoke(bean, "bar");
        Assert.assertEquals("bar", bar);
        // getter should return last value
        Assert.assertEquals("bar", get.invoke(bean));
        // tryExecute should succeed on same property
        Object quux = set.tryInvoke(bean, "value", "quux");
        Assert.assertEquals("quux", quux);
        Assert.assertEquals("quux", get.invoke(bean));
        // tryExecute should fail on different property
        Assert.assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(bean, "eulav", "nope"));

    }

    @Test
    public void testDuckIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null, null);
        Duck duck = new Duck("JEXL", "LXEJ");

        JexlPropertyGet get = uber.getPropertyGet(duck, "value");
        JexlPropertySet set = uber.getPropertySet(duck, "value", "foo");
        Assert.assertTrue("duck property getter", get instanceof DuckGetExecutor);
        Assert.assertTrue("duck property setter", set instanceof DuckSetExecutor);
        // introspector and uberspect should return same result
        Assert.assertEquals(get, uber.getPropertyGet(duck, "value"));
        Assert.assertEquals(set, uber.getPropertySet(duck, "value", "foo"));
        // different property should return different setter/getter
        Assert.assertFalse(get.equals(uber.getPropertyGet(duck, "eulav")));
        Assert.assertFalse(set.equals(uber.getPropertySet(duck, "eulav", "foo")));
        // setter returns argument
        Object bar = set.invoke(duck, "bar");
        Assert.assertEquals("bar", bar);
        // getter should return last value
        Assert.assertEquals("bar", get.invoke(duck));
        // tryExecute should succeed on same property
        Object quux = set.tryInvoke(duck, "value", "quux");
        Assert.assertEquals("quux", quux);
        Assert.assertEquals("quux", get.invoke(duck));
        // tryExecute should fail on different property
        Assert.assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(duck, "eulav", "nope"));
    }

    @Test
    public void testListIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null, null);
        List<Object> list = new ArrayList<Object>();
        list.add("LIST");
        list.add("TSIL");

        JexlPropertyGet get = uber.getPropertyGet(list, Integer.valueOf(1));
        JexlPropertySet set = uber.getPropertySet(list, Integer.valueOf(1), "foo");
        Assert.assertTrue("list property getter", get instanceof ListGetExecutor);
        Assert.assertTrue("list property setter", set instanceof ListSetExecutor);
        // introspector and uberspect should return same result
        Assert.assertEquals(get, uber.getPropertyGet(list, Integer.valueOf(1)));
        Assert.assertEquals(set, uber.getPropertySet(list, Integer.valueOf(1), "foo"));
        // different property should return different setter/getter
        Assert.assertFalse(get.equals(uber.getPropertyGet(list, Integer.valueOf(0))));
        Assert.assertFalse(get.equals(uber.getPropertySet(list, Integer.valueOf(0), "foo")));
        // setter returns argument
        Object bar = set.invoke(list, "bar");
        Assert.assertEquals("bar", bar);
        // getter should return last value
        Assert.assertEquals("bar", get.invoke(list));
        // tryExecute should succeed on integer property
        Object quux = set.tryInvoke(list, Integer.valueOf(1), "quux");
        Assert.assertEquals("quux", quux);
        // getter should return last value
        Assert.assertEquals("quux", get.invoke(list));
        // tryExecute should fail on non-integer property class
        Assert.assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(list, "eulav", "nope"));
    }

    @Test
    public void testMapIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null, null);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", "MAP");
        map.put("eulav", "PAM");

        JexlPropertyGet get = uber.getPropertyGet(map, "value");
        JexlPropertySet set = uber.getPropertySet(map, "value", "foo");
        Assert.assertTrue("map property getter", get instanceof MapGetExecutor);
        Assert.assertTrue("map property setter", set instanceof MapSetExecutor);
        // introspector and uberspect should return same result
        Assert.assertEquals(get, uber.getPropertyGet(map, "value"));
        Assert.assertEquals(set, uber.getPropertySet(map, "value", "foo"));
        // different property should return different setter/getter
        Assert.assertFalse(get.equals(uber.getPropertyGet(map, "eulav")));
        Assert.assertFalse(get.equals(uber.getPropertySet(map, "eulav", "foo")));
        // setter returns argument
        Object bar = set.invoke(map, "bar");
        Assert.assertEquals("bar", bar);
        // getter should return last value
        Assert.assertEquals("bar", get.invoke(map));
        // tryExecute should succeed on same property class
        Object quux = set.tryInvoke(map, "value", "quux");
        Assert.assertEquals("quux", quux);
        // getter should return last value
        Assert.assertEquals("quux", get.invoke(map));
        // tryExecute should fail on different property class
        Assert.assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(map, Integer.valueOf(1), "nope"));
    }

}
