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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.JexlTestCase;
import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.junit.jupiter.api.Test;

/**
 * Tests for checking introspection discovery.
 */
public class DiscoveryTest extends JexlTestCase {
    public static class Bean {
        private String value;
        private String eulav;
        private boolean flag;

        public Bean(final String v, final String e) {
            value = v;
            eulav = e;
            flag = true;
        }

        public String getEulav() {
            return eulav;
        }

        public String getValue() {
            return value;
        }

        public boolean isFlag() {
            return flag;
        }

        public void setEulav(final String v) {
            eulav = v;
        }

        public void setFlag(final boolean f) {
            flag = f;
        }

        public void setValue(final String v) {
            value = v;
        }
    }

    public static class Bulgroz {
        public Object amb(final Number x) {
            return -2;
        }
        public Object amb(final Serializable x) {
            return -1;
        }
        public Object list(final int x) {
            return 0;
        }
        public Object list(final int x, final int y) {
            return 4;
        }
        public Object list(final int x, final Object...y) {
            return 3;
        }
        public Object list(final Object x) {
            return 2;
        }
        public Object list(final Object x, final Object...y) {
            return 7;
        }
        public Object list(final Object x, final Object y) {
            return 8;
        }
        public Object list(final String x) {
            return 1;
        }
        public Object list(final String x, final Object...y) {
            return 5;
        }
        public Object list(final String x, final String y) {
            return 6;
        }
    }

    public static class Duck {
        private String value;
        private String eulav;

        public Duck(final String v, final String e) {
            value = v;
            eulav = e;
        }

        public String get(final String prop) {
            if ("value".equals(prop)) {
                return value;
            }
            if ("eulav".equals(prop)) {
                return eulav;
            }
            return "no such property";
        }

        public void set(final String prop, final String v) {
            if ("value".equals(prop)) {
                value = v;
            } else if ("eulav".equals(prop)) {
                eulav = v;
            }
        }
    }

    public DiscoveryTest() {
        super("DiscoveryTest");
    }

    @Test
    public void testBeanIntrospection() throws Exception {
        final Uberspect uber = Engine.getUberspect(null, null);
        final Bean bean = new Bean("JEXL", "LXEJ");

        final JexlPropertyGet get = uber.getPropertyGet(bean, "value");
        final JexlPropertySet set = uber.getPropertySet(bean, "value", "foo");
        assertInstanceOf(PropertyGetExecutor.class, get, "bean property getter");
        assertInstanceOf(PropertySetExecutor.class, set, "bean property setter");
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(bean, "value"));
        assertEquals(set, uber.getPropertySet(bean, "value", "foo"));
        // different property should return different setter/getter
        assertNotEquals(get, uber.getPropertyGet(bean, "eulav"));
        assertNotEquals(set, uber.getPropertySet(bean, "eulav", "foo"));
        // setter returns argument
        final Object bar = set.invoke(bean, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.invoke(bean));
        // tryExecute should succeed on same property
        final Object quux = set.tryInvoke(bean, "value", "quux");
        assertEquals("quux", quux);
        assertEquals("quux", get.invoke(bean));
        // tryExecute should fail on different property
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(bean, "eulav", "nope"));

    }

    @Test
    public void testDuckIntrospection() throws Exception {
        final Uberspect uber = Engine.getUberspect(null, null);
        final Duck duck = new Duck("JEXL", "LXEJ");

        final JexlPropertyGet get = uber.getPropertyGet(duck, "value");
        final JexlPropertySet set = uber.getPropertySet(duck, "value", "foo");
        assertInstanceOf(DuckGetExecutor.class, get, "duck property getter");
        assertInstanceOf(DuckSetExecutor.class, set, "duck property setter");
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(duck, "value"));
        assertEquals(set, uber.getPropertySet(duck, "value", "foo"));
        // different property should return different setter/getter
        assertNotEquals(get, uber.getPropertyGet(duck, "eulav"));
        assertNotEquals(set, uber.getPropertySet(duck, "eulav", "foo"));
        // setter returns argument
        final Object bar = set.invoke(duck, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.invoke(duck));
        // tryExecute should succeed on same property
        final Object quux = set.tryInvoke(duck, "value", "quux");
        assertEquals("quux", quux);
        assertEquals("quux", get.invoke(duck));
        // tryExecute should fail on different property
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(duck, "eulav", "nope"));
    }

    @Test
    public void testListIntrospection() throws Exception {
        final Uberspect uber = Engine.getUberspect(null, null);
        final List<Object> list = new ArrayList<>();
        list.add("LIST");
        list.add("TSIL");

        final JexlPropertyGet get = uber.getPropertyGet(list, 1);
        final JexlPropertySet set = uber.getPropertySet(list, 1, "foo");
        assertInstanceOf(ListGetExecutor.class, get, "list property getter");
        assertInstanceOf(ListSetExecutor.class, set, "list property setter");
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(list, 1));
        assertEquals(set, uber.getPropertySet(list, 1, "foo"));
        // different property should return different setter/getter
        assertNotEquals(get, uber.getPropertyGet(list, 0));
        assertNotEquals(get, uber.getPropertySet(list, 0, "foo"));
        // setter returns argument
        final Object bar = set.invoke(list, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.invoke(list));
        // tryExecute should succeed on integer property
        final Object quux = set.tryInvoke(list, 1, "quux");
        assertEquals("quux", quux);
        // getter should return last value
        assertEquals("quux", get.invoke(list));
        // tryExecute should fail on non-integer property class
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(list, "eulav", "nope"));
    }

    @Test
    public void testMapIntrospection() throws Exception {
        final Uberspect uber = Engine.getUberspect(null, null);
        final Map<String, Object> map = new HashMap<>();
        map.put("value", "MAP");
        map.put("eulav", "PAM");

        final JexlPropertyGet get = uber.getPropertyGet(map, "value");
        final JexlPropertySet set = uber.getPropertySet(map, "value", "foo");
        assertInstanceOf(MapGetExecutor.class, get, "map property getter");
        assertInstanceOf(MapSetExecutor.class, set, "map property setter");
        // introspector and uberspect should return same result
        assertEquals(get, uber.getPropertyGet(map, "value"));
        assertEquals(set, uber.getPropertySet(map, "value", "foo"));
        // different property should return different setter/getter
        assertNotEquals(get, uber.getPropertyGet(map, "eulav"));
        assertNotEquals(get, uber.getPropertySet(map, "eulav", "foo"));
        // setter returns argument
        final Object bar = set.invoke(map, "bar");
        assertEquals("bar", bar);
        // getter should return last value
        assertEquals("bar", get.invoke(map));
        // tryExecute should succeed on same property class
        final Object quux = set.tryInvoke(map, "value", "quux");
        assertEquals("quux", quux);
        // getter should return last value
        assertEquals("quux", get.invoke(map));
        // tryExecute should fail on different property class
        assertEquals(AbstractExecutor.TRY_FAILED, set.tryInvoke(map, 1, "nope"));
    }

    @Test
    public void testMethodIntrospection() throws Exception {
        final Uberspect uber = new Uberspect(null, null);
        final Bulgroz bulgroz = new Bulgroz();
        JexlMethod jmethod;
        Object result;
        jmethod = uber.getMethod(bulgroz, "list", 0);
        result = jmethod.invoke(bulgroz, 0);
        assertEquals(0, result);
        jmethod = uber.getMethod(bulgroz, "list", "1");
        result = jmethod.invoke(bulgroz, "1");
        assertEquals(1, result);
        jmethod = uber.getMethod(bulgroz, "list", bulgroz);
        result = jmethod.invoke(bulgroz, bulgroz);
        assertEquals(2, result);
        jmethod = uber.getMethod(bulgroz, "list", 1, bulgroz);
        result = jmethod.invoke(bulgroz, 1, bulgroz);
        assertEquals(3, result);
        jmethod = uber.getMethod(bulgroz, "list", 1, bulgroz, bulgroz);
        result = jmethod.invoke(bulgroz, 1, bulgroz, bulgroz);
        assertEquals(3, result);
        jmethod = uber.getMethod(bulgroz, "list", 1, 2);
        result = jmethod.invoke(bulgroz, 1, 2);
        assertEquals(4, result);
        jmethod = uber.getMethod(bulgroz, "list", "1", bulgroz);
        result = jmethod.invoke(bulgroz, "1", bulgroz);
        assertEquals(5, result);
        jmethod = uber.getMethod(bulgroz, "list", "1", "2");
        result = jmethod.invoke(bulgroz, "1", "2");
        assertEquals(6, result);
        jmethod = uber.getMethod(bulgroz, "list", bulgroz, bulgroz);
        result = jmethod.invoke(bulgroz, bulgroz, bulgroz);
        assertEquals(8, result);
        jmethod = uber.getMethod(bulgroz, "list", bulgroz, 1, bulgroz);
        result = jmethod.invoke(bulgroz, bulgroz, 1, bulgroz);
        assertEquals(7, result);
        jmethod = uber.getMethod(bulgroz, "list", bulgroz, 1, "1");
        result = jmethod.invoke(bulgroz, bulgroz, 1, "1");
        assertEquals(7, result);
        jmethod = uber.getMethod(bulgroz, "list", (Object) null);
        result = jmethod.invoke(bulgroz,  (Object) null);
        assertEquals(2, result);
        jmethod = uber.getMethod(bulgroz, "list", bulgroz, (Object) null);
        result = jmethod.invoke(bulgroz, bulgroz, (Object) null);
        assertEquals(8, result);
        jmethod = uber.getMethod(bulgroz, "list", null, "1");
        result = jmethod.invoke(bulgroz, null, "1");
        assertEquals(8, result);
        jmethod = uber.getMethod(bulgroz, "list", bulgroz, null, null);
        result = jmethod.invoke(bulgroz, bulgroz, null, null);
        assertEquals(7, result);

        jmethod = uber.getMethod(bulgroz, "amb", 3d);
        assertNotNull(jmethod);
    }
}
