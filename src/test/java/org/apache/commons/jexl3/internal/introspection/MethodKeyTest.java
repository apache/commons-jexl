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
    private static void setUpKey(final String name, final Class<?>[] parms) {
        final MethodKey key = new MethodKey(name, parms);
        final String str = key.toString();
        BY_KEY.put(key, str);
        BY_STRING.put(str, key);

    }

    /* Generate a list of method*(prims*), method(prims*, prims*), method*(prims*,prims*,prims*) */
    static {
        BY_KEY = new java.util.HashMap< MethodKey, String>();
        BY_STRING = new java.util.HashMap<String, MethodKey>();
        for (final String method : METHODS) {
            for (final Class<?> value : PRIMS) {
                final Class<?>[] arg0 = {value};
                setUpKey(method, arg0);
                for (final Class<?> aClass : PRIMS) {
                    final Class<?>[] arg1 = {value, aClass};
                    setUpKey(method, arg1);
                    for (final Class<?> prim : PRIMS) {
                        final Class<?>[] arg2 = {value, aClass, prim};
                        setUpKey(method, arg2);
                    }
                }
            }
        }
        KEY_LIST = BY_KEY.keySet().toArray(new MethodKey[BY_KEY.size()]);
    }

    /** Builds a string key */
    String makeStringKey(final String method, final Class<?>... params) {
        final StringBuilder builder = new StringBuilder(method);
        for (final Class<?> param : params) {
            builder.append(MethodKey.primitiveClass(param).getName());
        }
        return builder.toString();
    }

    /** Checks that a string key does exist */
    void checkStringKey(final String method, final Class<?>... params) {
        final String key = makeStringKey(method, params);
        final MethodKey out = BY_STRING.get(key);
        Assert.assertNotNull(out);
    }

    /** Builds a method key */
    MethodKey makeKey(final String method, final Class<?>... params) {
        return new MethodKey(method, params);
    }

    /** Checks that a method key exists */
    void checkKey(final String method, final Class<?>... params) {
        final MethodKey key = makeKey(method, params);
        final String out = BY_KEY.get(key);
        Assert.assertNotNull(out);
    }

    @Test
    public void testDebugString() throws Exception {
        final MethodKey c = KEY_LIST[0];
        final String str = c.debugString();
        Assert.assertNotNull(str);
    }

    @Test
    public void testObjectKey() throws Exception {
        for (final MethodKey ctl : KEY_LIST) {
            final MethodKey key = makeKey(ctl.getMethod(), ctl.getParameters());
            final String out = BY_KEY.get(key);
            Assert.assertNotNull(out);
            Assert.assertEquals(ctl.toString() + " != " + out, ctl.toString(), out);
        }

    }

    @Test
    public void testStringKey() throws Exception {
        for (final MethodKey ctl : KEY_LIST) {
            final String key = makeStringKey(ctl.getMethod(), ctl.getParameters());
            final MethodKey out = BY_STRING.get(key);
            Assert.assertNotNull(out);
            Assert.assertEquals(ctl.toString() + " != " + key, ctl, out);
        }

    }
    private static final int LOOP = 3;//00;

    @Test
    public void testPerfKey() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (final MethodKey ctl : KEY_LIST) {
                final MethodKey key = makeKey(ctl.getMethod(), ctl.getParameters());
                final String out = BY_KEY.get(key);
                Assert.assertNotNull(out);
            }
        }
    }

    @Test
    public void testPerfString() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (final MethodKey ctl : KEY_LIST) {
                final String key = makeStringKey(ctl.getMethod(), ctl.getParameters());
                final MethodKey out = BY_STRING.get(key);
                Assert.assertNotNull(out);
            }
        }
    }

    @Test
    public void testPerfKey2() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (final String method : METHODS) {
                for (final Object value : ARGS) {
                    checkKey(method, value.getClass());
                    for (final Object o : ARGS) {
                        checkKey(method, value.getClass(), o.getClass());
                        for (final Object arg : ARGS) {
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
            for (final String method : METHODS) {
                for (final Object value : ARGS) {
                    checkStringKey(method, value.getClass());
                    for (final Object o : ARGS) {
                        checkStringKey(method, value.getClass(), o.getClass());
                        for (final Object arg : ARGS) {
                            checkStringKey(method, value.getClass(), o.getClass(), arg.getClass());
                        }
                    }
                }
            }
        }
    }
}
