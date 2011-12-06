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

import org.apache.commons.jexl3.internal.introspection.Uberspect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.JexlTestCase;

import org.apache.commons.jexl3.internal.introspection.AbstractExecutor;
import org.apache.commons.jexl3.internal.introspection.PropertyGetExecutor;
import org.apache.commons.jexl3.internal.introspection.PropertySetExecutor;
import org.apache.commons.jexl3.internal.introspection.DuckGetExecutor;
import org.apache.commons.jexl3.internal.introspection.DuckSetExecutor;
import org.apache.commons.jexl3.internal.introspection.ListGetExecutor;
import org.apache.commons.jexl3.internal.introspection.MapGetExecutor;
import org.apache.commons.jexl3.internal.introspection.MapSetExecutor;
import org.apache.commons.jexl3.internal.introspection.ListSetExecutor;

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

        AbstractExecutor.Get get = uber.getGetExecutor(bean, "value");
        AbstractExecutor.Set set  = uber.getSetExecutor(bean, "value", "foo");
        assertTrue("bean property getter", get instanceof PropertyGetExecutor);
        assertTrue("bean property setter", set instanceof PropertySetExecutor);
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(bean, "value", null));
        assertEquals(set, uber.getPropertySet(bean, "value", "foo", null));
        // different property should return different setter/getter
        assertFalse(get.equals(uber.getGetExecutor(bean, "eulav")));
        assertFalse(set.equals(uber.getSetExecutor(bean, "eulav", "foo")));
        // setter returns argument
        Object bar = set.execute(bean, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.execute(bean));
        // tryExecute should succeed on same property
        Object quux = set.tryExecute(bean, "value", "quux");
        assertEquals("quux", quux);
        assertEquals("quux", get.execute(bean));
        // tryExecute should fail on different property
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryExecute(bean, "eulav", "nope"));

    }

    public void testDuckIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null);
        Duck duck = new Duck("JEXL", "LXEJ");

        AbstractExecutor.Get get = uber.getGetExecutor(duck, "value");
        AbstractExecutor.Set set  = uber.getSetExecutor(duck, "value", "foo");
        assertTrue("duck property getter", get instanceof DuckGetExecutor);
        assertTrue("duck property setter", set instanceof DuckSetExecutor);
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(duck, "value", null));
        assertEquals(set, uber.getPropertySet(duck, "value", "foo", null));
        // different property should return different setter/getter
        assertFalse(get.equals(uber.getGetExecutor(duck, "eulav")));
        assertFalse(set.equals(uber.getSetExecutor(duck, "eulav", "foo")));
        // setter returns argument
        Object bar = set.execute(duck, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.execute(duck));
        // tryExecute should succeed on same property
        Object quux = set.tryExecute(duck, "value", "quux");
        assertEquals("quux", quux);
        assertEquals("quux", get.execute(duck));
        // tryExecute should fail on different property
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryExecute(duck, "eulav", "nope"));
    }

    public void testListIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null);
        List<Object> list = new ArrayList<Object>();
        list.add("LIST");
        list.add("TSIL");

        AbstractExecutor.Get get = uber.getGetExecutor(list, Integer.valueOf(1));
        AbstractExecutor.Set set  = uber.getSetExecutor(list, Integer.valueOf(1), "foo");
        assertTrue("list property getter", get instanceof ListGetExecutor);
        assertTrue("list property setter", set instanceof ListSetExecutor);
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(list, Integer.valueOf(1), null));
        assertEquals(set, uber.getPropertySet(list, Integer.valueOf(1), "foo", null));
        // different property should return different setter/getter
        assertFalse(get.equals(uber.getGetExecutor(list, Integer.valueOf(0))));
        assertFalse(get.equals(uber.getSetExecutor(list, Integer.valueOf(0), "foo")));
        // setter returns argument
        Object bar = set.execute(list, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.execute(list));
        // tryExecute should succeed on integer property
        Object quux = set.tryExecute(list, Integer.valueOf(1), "quux");
        assertEquals("quux", quux);
        // getter should return last value
        assertEquals("quux", get.execute(list));
        // tryExecute should fail on non-integer property class
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryExecute(list, "eulav", "nope"));
    }

    public void testMapIntrospection() throws Exception {
        Uberspect uber = Engine.getUberspect(null);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", "MAP");
        map.put("eulav", "PAM");

        AbstractExecutor.Get get = uber.getGetExecutor(map, "value");
        AbstractExecutor.Set set  = uber.getSetExecutor(map, "value", "foo");
        assertTrue("map property getter", get instanceof MapGetExecutor);
        assertTrue("map property setter", set instanceof MapSetExecutor);
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(map, "value", null));
        assertEquals(set, uber.getPropertySet(map, "value", "foo", null));
        // different property should return different setter/getter
        assertFalse(get.equals(uber.getGetExecutor(map, "eulav")));
        assertFalse(get.equals(uber.getSetExecutor(map, "eulav", "foo")));
        // setter returns argument
        Object bar = set.execute(map, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.execute(map));
        // tryExecute should succeed on same property class
        Object quux = set.tryExecute(map, "value", "quux");
        assertEquals("quux", quux);
        // getter should return last value
        assertEquals("quux", get.execute(map));
        // tryExecute should fail on different property class
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryExecute(map, Integer.valueOf(1), "nope"));
    }

}
