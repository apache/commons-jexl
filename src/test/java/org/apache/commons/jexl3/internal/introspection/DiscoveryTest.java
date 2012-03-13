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


    public void testBeanIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null);
        Bean bean = new Bean("JEXL", "LXEJ");

        JexlPropertyGet get = uber.getPropertyGet(bean, "value");
        JexlPropertySet set  = uber.getPropertySet(bean, "value", "foo");
        assertTrue("bean property getter", get instanceof PropertyGetExecutor);
        assertTrue("bean property setter", set instanceof PropertySetExecutor);
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(bean, "value"));
        assertEquals(set, uber.getPropertySet(bean, "value", "foo"));
        // different property should return different setter/getter
        assertFalse(get.equals(uber.getPropertyGet(bean, "eulav")));
        assertFalse(set.equals(uber.getPropertySet(bean, "eulav", "foo")));
        // setter returns argument
        Object bar = set.invoke(bean, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.invoke(bean));
        // tryExecute should succeed on same property
        Object quux = set.tryInvoke(bean, "value", "quux");
        assertEquals("quux", quux);
        assertEquals("quux", get.invoke(bean));
        // tryExecute should fail on different property
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(bean, "eulav", "nope"));

    }

    public void testDuckIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null);
        Duck duck = new Duck("JEXL", "LXEJ");

        JexlPropertyGet get = uber.getPropertyGet(duck, "value");
        JexlPropertySet set  = uber.getPropertySet(duck, "value", "foo");
        assertTrue("duck property getter", get instanceof DuckGetExecutor);
        assertTrue("duck property setter", set instanceof DuckSetExecutor);
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(duck, "value"));
        assertEquals(set, uber.getPropertySet(duck, "value", "foo"));
        // different property should return different setter/getter
        assertFalse(get.equals(uber.getPropertyGet(duck, "eulav")));
        assertFalse(set.equals(uber.getPropertySet(duck, "eulav", "foo")));
        // setter returns argument
        Object bar = set.invoke(duck, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.invoke(duck));
        // tryExecute should succeed on same property
        Object quux = set.tryInvoke(duck, "value", "quux");
        assertEquals("quux", quux);
        assertEquals("quux", get.invoke(duck));
        // tryExecute should fail on different property
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(duck, "eulav", "nope"));
    }

    public void testListIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null);
        List<Object> list = new ArrayList<Object>();
        list.add("LIST");
        list.add("TSIL");

        JexlPropertyGet get = uber.getPropertyGet(list, Integer.valueOf(1));
        JexlPropertySet set  = uber.getPropertySet(list, Integer.valueOf(1), "foo");
        assertTrue("list property getter", get instanceof ListGetExecutor);
        assertTrue("list property setter", set instanceof ListSetExecutor);
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(list, Integer.valueOf(1)));
        assertEquals(set, uber.getPropertySet(list, Integer.valueOf(1), "foo"));
        // different property should return different setter/getter
        assertFalse(get.equals(uber.getPropertyGet(list, Integer.valueOf(0))));
        assertFalse(get.equals(uber.getPropertySet(list, Integer.valueOf(0), "foo")));
        // setter returns argument
        Object bar = set.invoke(list, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.invoke(list));
        // tryExecute should succeed on integer property
        Object quux = set.tryInvoke(list, Integer.valueOf(1), "quux");
        assertEquals("quux", quux);
        // getter should return last value
        assertEquals("quux", get.invoke(list));
        // tryExecute should fail on non-integer property class
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(list, "eulav", "nope"));
    }

    public void testMapIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", "MAP");
        map.put("eulav", "PAM");

        JexlPropertyGet get = uber.getPropertyGet(map, "value");
        JexlPropertySet set  = uber.getPropertySet(map, "value", "foo");
        assertTrue("map property getter", get instanceof MapGetExecutor);
        assertTrue("map property setter", set instanceof MapSetExecutor);
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(map, "value"));
        assertEquals(set, uber.getPropertySet(map, "value", "foo"));
        // different property should return different setter/getter
        assertFalse(get.equals(uber.getPropertyGet(map, "eulav")));
        assertFalse(get.equals(uber.getPropertySet(map, "eulav", "foo")));
        // setter returns argument
        Object bar = set.invoke(map, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.invoke(map));
        // tryExecute should succeed on same property class
        Object quux = set.tryInvoke(map, "value", "quux");
        assertEquals("quux", quux);
        // getter should return last value
        assertEquals("quux", get.invoke(map));
        // tryExecute should fail on different property class
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(map, Integer.valueOf(1), "nope"));
    }

}
