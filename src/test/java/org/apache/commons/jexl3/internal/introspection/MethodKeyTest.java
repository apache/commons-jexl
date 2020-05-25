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

import org.junit.Assert;
import org.junit.Test;

/**
 * Checks the CacheMap.MethodKey implementation
 */
public class MethodKeyTest {
    // A set of classes (most of them primitives)
    private static final Class<?>[] PRIMS = {
        Boolean.TYPE,
        Byte.TYPE,
        Character.TYPE,
        Double.TYPE,
        Float.TYPE,
        Integer.TYPE,
        Long.TYPE,
        Short.TYPE,
        String.class,
        java.util.Date.class
    };
    // A set of instances corresponding to the classes
    private static final Object[] ARGS = {
        Boolean.TRUE,
            (byte) 1,
            '2',
            4d,
            8f,
            16,
            32L,
            (short) 64,
        "foobar",
        new java.util.Date()
    };
    // A set of (pseudo) method names
    private static final String[] METHODS = {
        "plus",
        "minus",
        "execute",
        "activate",
        "perform",
        "apply",
        "invoke",
        "executeAction",
        "activateAction",
        "performAction",
        "applyAction",
        "invokeAction",
        "executeFunctor",
        "activateFunctor",
        "performFunctor",
        "applyFunctor",
        "invokeFunctor",
        "executeIt",
        "activateIt",
        "performIt",
        "applyIt",
        "invokeIt"
    };
    /** from key to string */
    private static final java.util.Map< MethodKey, String> BY_KEY;
    /** form string to key */
    private static final java.util.Map<String, MethodKey> BY_STRING;
    /** the list of keys we generated & test against */
    private static final MethodKey[] KEY_LIST;

    /** * Creates & inserts a key into the BY_KEY & byString map */
    private static void setUpKey(String name, Class<?>[] parms) {
        MethodKey key = new MethodKey(name, parms);
        String str = key.toString();
        BY_KEY.put(key, str);
        BY_STRING.put(str, key);

    }

    /* Generate a list of method*(prims*), method(prims*, prims*), method*(prims*,prims*,prims*) */
    static {
        BY_KEY = new java.util.HashMap< MethodKey, String>();
        BY_STRING = new java.util.HashMap<String, MethodKey>();
        for (String method : METHODS) {
            for (Class<?> value : PRIMS) {
                Class<?>[] arg0 = {value};
                setUpKey(method, arg0);
                for (Class<?> aClass : PRIMS) {
                    Class<?>[] arg1 = {value, aClass};
                    setUpKey(method, arg1);
                    for (Class<?> prim : PRIMS) {
                        Class<?>[] arg2 = {value, aClass, prim};
                        setUpKey(method, arg2);
                    }
                }
            }
        }
        KEY_LIST = BY_KEY.keySet().toArray(new MethodKey[BY_KEY.size()]);
    }

    /** Builds a string key */
    String makeStringKey(String method, Class<?>... params) {
        StringBuilder builder = new StringBuilder(method);
        for (Class<?> param : params) {
            builder.append(MethodKey.primitiveClass(param).getName());
        }
        return builder.toString();
    }

    /** Checks that a string key does exist */
    void checkStringKey(String method, Class<?>... params) {
        String key = makeStringKey(method, params);
        MethodKey out = BY_STRING.get(key);
        Assert.assertNotNull(out);
    }

    /** Builds a method key */
    MethodKey makeKey(String method, Class<?>... params) {
        return new MethodKey(method, params);
    }

    /** Checks that a method key exists */
    void checkKey(String method, Class<?>... params) {
        MethodKey key = makeKey(method, params);
        String out = BY_KEY.get(key);
        Assert.assertNotNull(out);
    }

    @Test
    public void testDebugString() throws Exception {
        MethodKey c = KEY_LIST[0];
        String str = c.debugString();
        Assert.assertNotNull(str);
    }

    @Test
    public void testObjectKey() throws Exception {
        for (MethodKey ctl : KEY_LIST) {
            MethodKey key = makeKey(ctl.getMethod(), ctl.getParameters());
            String out = BY_KEY.get(key);
            Assert.assertNotNull(out);
            Assert.assertEquals(ctl.toString() + " != " + out, ctl.toString(), out);
        }

    }

    @Test
    public void testStringKey() throws Exception {
        for (MethodKey ctl : KEY_LIST) {
            String key = makeStringKey(ctl.getMethod(), ctl.getParameters());
            MethodKey out = BY_STRING.get(key);
            Assert.assertNotNull(out);
            Assert.assertEquals(ctl.toString() + " != " + key, ctl, out);
        }

    }
    private static final int LOOP = 3;//00;

    @Test
    public void testPerfKey() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (MethodKey ctl : KEY_LIST) {
                MethodKey key = makeKey(ctl.getMethod(), ctl.getParameters());
                String out = BY_KEY.get(key);
                Assert.assertNotNull(out);
            }
        }
    }

    @Test
    public void testPerfString() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (MethodKey ctl : KEY_LIST) {
                String key = makeStringKey(ctl.getMethod(), ctl.getParameters());
                MethodKey out = BY_STRING.get(key);
                Assert.assertNotNull(out);
            }
        }
    }

    @Test
    public void testPerfKey2() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (String method : METHODS) {
                for (Object value : ARGS) {
                    checkKey(method, value.getClass());
                    for (Object o : ARGS) {
                        checkKey(method, value.getClass(), o.getClass());
                        for (Object arg : ARGS) {
                            checkKey(method, value.getClass(), o.getClass(), arg.getClass());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testPerfStringKey2() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (String method : METHODS) {
                for (Object value : ARGS) {
                    checkStringKey(method, value.getClass());
                    for (Object o : ARGS) {
                        checkStringKey(method, value.getClass(), o.getClass());
                        for (Object arg : ARGS) {
                            checkStringKey(method, value.getClass(), o.getClass(), arg.getClass());
                        }
                    }
                }
            }
        }
    }
}
