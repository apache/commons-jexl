/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.junit.jupiter.api.Test;

/**
 * Test cases for the if statement.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class StrategyTest extends JexlTestCase {
    // JEXL-174
    public static class MapArithmetic extends JexlArithmetic {
        public MapArithmetic(final boolean flag) {
            super(flag);
        }

        public Object arrayGet(final Map<?,?> map, final Object identifier) {
            return map.get(identifier);
        }

        public Object arraySet(final Map<Object, Object> map, final Object identifier, final Object value) {
             map.put(identifier, value);
             return value;
        }

        public Object propertyGet(final Map<?,?> map, final Object identifier) {
            return arrayGet(map, identifier);
        }

        public Object propertySet(final Map<Object, Object> map, final Object identifier, final Object value) {
             return arraySet(map, identifier, value);
        }
    }

    public StrategyTest() {
        super("StrategyTest");
    }

    public void run171(final JexlEngine jexl, final boolean std) throws Exception {
        Object result;
        final Map<String, Object> i = new HashMap<>();

        i.put("class", 42);
        result = jexl.createScript("i['class'] ", "i").execute((JexlContext)null, i);
        assertEquals(42, result);
        result = jexl.createScript("i['class'] = 28", "i").execute((JexlContext)null, i);
        assertEquals(28, result);
        assertEquals(28, i.get("class"));
        result = jexl.createScript("i.class", "i").execute((JexlContext)null, i);
        if (std) {
            assertEquals(HashMap.class, result);
        } else {
            assertEquals(28, result);
        }
        result = jexl.createScript("i.'class'", "i").execute((JexlContext)null, i);
        if (std) {
            assertEquals(HashMap.class, result);
        } else {
            assertEquals(28, result);
        }

        i.put("size", 4242);
        result = jexl.createScript("i['size'] ", "i").execute((JexlContext)null, i);
        assertEquals(4242 ,result);
        result = jexl.createScript("i['size'] = 2828", "i").execute((JexlContext)null, i);
        assertEquals(2828, result);
        assertEquals(2828, i.get("size"));
        result = jexl.createScript("i.'size'", "i").execute((JexlContext)null, i);
        assertEquals(2828, result);
        result = jexl.createScript("size i", "i").execute((JexlContext)null, i);
        assertEquals(2, result);

        i.put("empty", 424242);
        result = jexl.createScript("i['empty'] ", "i").execute((JexlContext)null, i);
        assertEquals(424242, result);
        result = jexl.createScript("i['empty'] = 282828", "i").execute((JexlContext)null, i);
        assertEquals(282828, result);
        assertEquals(282828, i.get("empty"));
        result = jexl.createScript("i.'empty'", "i").execute((JexlContext)null, i);
        if (std) {
        assertNotEquals(282828, result);
        } else {
            assertEquals(282828, result);
        }
        result = jexl.createScript("empty i", "i").execute((JexlContext)null, i);
        assertFalse((Boolean) result);
    }

    @Test
    void testJexlStrategy() throws Exception {
        final JexlEngine jexl = new Engine();
        run171(jexl, true);
    }

    @Test
    void testMapStrategy() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strategy(JexlUberspect.MAP_STRATEGY).create();
        run171(jexl, false);
    }

    @Test
    void testMyMapStrategy() throws Exception {
        final JexlEngine jexl = new JexlBuilder().arithmetic(new MapArithmetic(true)).create();
        run171(jexl, false);
    }

    @Test
    void testRawResolvers() throws Exception {
        final Object map  = new HashMap<String, Object>();
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlUberspect uberspect = jexl.getUberspect();
        final JexlUberspect.PropertyResolver rfieldp = JexlUberspect.JexlResolver.FIELD;
        final JexlPropertyGet fget = rfieldp.getPropertyGet(uberspect, map, "key");
        assertNull(fget);
        final JexlPropertySet fset = rfieldp.getPropertySet(uberspect, map, "key", "value");
        assertNull(fset);
        final JexlUberspect.PropertyResolver rmap = JexlUberspect.JexlResolver.MAP;
        final JexlPropertyGet mget = rmap.getPropertyGet(uberspect, map, "key");
        assertNotNull(mget);
        final JexlPropertySet mset = rmap.getPropertySet(uberspect, map, "key", "value");
        assertNotNull(mset);
    }
}