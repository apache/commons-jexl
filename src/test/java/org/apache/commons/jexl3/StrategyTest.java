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

import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the if statement.
 *
 * @since 1.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class StrategyTest extends JexlTestCase {
    public StrategyTest() {
        super("StrategyTest");
    }

    // JEXL-174
    public static class MapArithmetic extends JexlArithmetic {
        public MapArithmetic(boolean flag) {
            super(flag);
        }

        public Object propertyGet(Map<?,?> map, Object identifier) {
            return arrayGet(map, identifier);
        }

        public Object propertySet(Map<Object, Object> map, Object identifier, Object value) {
             return arraySet(map, identifier, value);
        }

        public Object arrayGet(Map<?,?> map, Object identifier) {
            return map.get(identifier);
        }

        public Object arraySet(Map<Object, Object> map, Object identifier, Object value) {
             map.put(identifier, value);
             return value;
        }
    }

    @Test
    public void testRawResolvers() throws Exception {
        Object map  = new HashMap<String, Object>();
        final JexlEngine jexl = new JexlBuilder().create();
        JexlUberspect uberspect = jexl.getUberspect();
        JexlUberspect.PropertyResolver rfieldp = JexlUberspect.JexlResolver.FIELD;
        JexlPropertyGet fget = rfieldp.getPropertyGet(uberspect, map, "key");
        Assert.assertNull(fget);
        JexlPropertySet fset = rfieldp.getPropertySet(uberspect, map, "key", "value");
        Assert.assertNull(fset);
        JexlUberspect.PropertyResolver rmap = JexlUberspect.JexlResolver.MAP;
        JexlPropertyGet mget = rmap.getPropertyGet(uberspect, map, "key");
        Assert.assertNotNull(mget);
        JexlPropertySet mset = rmap.getPropertySet(uberspect, map, "key", "value");
        Assert.assertNotNull(mset);
    }

    @Test
    public void testJexlStrategy() throws Exception {
        final JexlEngine jexl = new Engine();
        run171(jexl, true);
    }

    @Test
    public void testMyMapStrategy() throws Exception {
        final JexlEngine jexl = new JexlBuilder().arithmetic( new MapArithmetic(true)).create();
        run171(jexl, false);
    }

    @Test
    public void testMapStrategy() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strategy(JexlUberspect.MAP_STRATEGY).create();
        run171(jexl, false);
    }

    public void run171(JexlEngine jexl, boolean std) throws Exception {
        Object result;
        Map<String, Object> i = new HashMap<String, Object>();

        i.put("class", 42);
        result = jexl.createScript("i['class'] ", "i").execute((JexlContext)null, i);
        Assert.assertEquals(42, result);
        result = jexl.createScript("i['class'] = 28", "i").execute((JexlContext)null, i);
        Assert.assertEquals(28, result);
        Assert.assertEquals(28, i.get("class"));
        result = jexl.createScript("i.class", "i").execute((JexlContext)null, i);
        if (std) {
            Assert.assertEquals(java.util.HashMap.class, result);
        } else {
            Assert.assertEquals(28, result);
        }
        result = jexl.createScript("i.'class'", "i").execute((JexlContext)null, i);
        if (std) {
            Assert.assertEquals(java.util.HashMap.class, result);
        } else {
            Assert.assertEquals(28, result);
        }

        i.put("size", 4242);
        result = jexl.createScript("i['size'] ", "i").execute((JexlContext)null, i);
        Assert.assertEquals(4242 ,result);
        result = jexl.createScript("i['size'] = 2828", "i").execute((JexlContext)null, i);
        Assert.assertEquals(2828, result);
        Assert.assertEquals(2828, i.get("size"));
        result = jexl.createScript("i.'size'", "i").execute((JexlContext)null, i);
        Assert.assertEquals(2828, result);
        result = jexl.createScript("size i", "i").execute((JexlContext)null, i);
        Assert.assertEquals(2, result);

        i.put("empty", 424242);
        result = jexl.createScript("i['empty'] ", "i").execute((JexlContext)null, i);
        Assert.assertEquals(424242, result);
        result = jexl.createScript("i['empty'] = 282828", "i").execute((JexlContext)null, i);
        Assert.assertEquals(282828, result);
        Assert.assertEquals(282828, i.get("empty"));
        result = jexl.createScript("i.'empty'", "i").execute((JexlContext)null, i);
        if (std) {
        Assert.assertNotEquals(282828, result);
        } else {
            Assert.assertEquals(282828, result);
        }
        result = jexl.createScript("empty i", "i").execute((JexlContext)null, i);
        Assert.assertFalse((Boolean) result);
    }
}