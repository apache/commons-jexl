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
        new Byte((byte) 1),
        new Character('2'),
        new Double(4d),
        new Float(8f),
        new Integer(16),
        new Long(32l),
        new Short((short) 64),
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

    /** Generate a list of method*(prims*), method(prims*, prims*), method*(prims*,prims*,prims*) */
    static {
        BY_KEY = new java.util.HashMap< MethodKey, String>();
        BY_STRING = new java.util.HashMap<String, MethodKey>();
        for (int m = 0; m < METHODS.length; ++m) {
            String method = METHODS[m];
            for (int p0 = 0; p0 < PRIMS.length; ++p0) {
                Class<?>[] arg0 = {PRIMS[p0]};
                setUpKey(method, arg0);
                for (int p1 = 0; p1 < PRIMS.length; ++p1) {
                    Class<?>[] arg1 = {PRIMS[p0], PRIMS[p1]};
                    setUpKey(method, arg1);
                    for (int p2 = 0; p2 < PRIMS.length; ++p2) {
                        Class<?>[] arg2 = {PRIMS[p0], PRIMS[p1], PRIMS[p2]};
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
        for (int p = 0; p < params.length; ++p) {
            builder.append(MethodKey.primitiveClass(params[p]).getName());
        }
        return builder.toString();
    }

    /** Checks that a string key does exist */
    void checkStringKey(String method, Class<?>... params) {
        String key = makeStringKey(method, params);
        MethodKey out = BY_STRING.get(key);
        Assert.assertTrue(out != null);
    }

    /** Builds a method key */
    MethodKey makeKey(String method, Class<?>... params) {
        return new MethodKey(method, params);
    }

    /** Checks that a method key exists */
    void checkKey(String method, Class<?>... params) {
        MethodKey key = makeKey(method, params);
        String out = BY_KEY.get(key);
        Assert.assertTrue(out != null);
    }

    @Test
    public void testDebugString() throws Exception {
        MethodKey c = KEY_LIST[0];
        String str = c.debugString();
        Assert.assertNotNull(str);
    }

    @Test
    public void testObjectKey() throws Exception {
        for (int k = 0; k < KEY_LIST.length; ++k) {
            MethodKey ctl = KEY_LIST[k];
            MethodKey key = makeKey(ctl.getMethod(), ctl.getParameters());
            String out = BY_KEY.get(key);
            Assert.assertTrue(out != null);
            Assert.assertTrue(ctl.toString() + " != " + out, ctl.toString().equals(out));
        }

    }

    @Test
    public void testStringKey() throws Exception {
        for (int k = 0; k < KEY_LIST.length; ++k) {
            MethodKey ctl = KEY_LIST[k];
            String key = makeStringKey(ctl.getMethod(), ctl.getParameters());
            MethodKey out = BY_STRING.get(key);
            Assert.assertTrue(out != null);
            Assert.assertTrue(ctl.toString() + " != " + key, ctl.equals(out));
        }

    }
    private static final int LOOP = 3;//00;

    @Test
    public void testPerfKey() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (int k = 0; k < KEY_LIST.length; ++k) {
                MethodKey ctl = KEY_LIST[k];
                MethodKey key = makeKey(ctl.getMethod(), ctl.getParameters());
                String out = BY_KEY.get(key);
                Assert.assertTrue(out != null);
            }
        }
    }

    @Test
    public void testPerfString() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (int k = 0; k < KEY_LIST.length; ++k) {
                MethodKey ctl = KEY_LIST[k];
                String key = makeStringKey(ctl.getMethod(), ctl.getParameters());
                MethodKey out = BY_STRING.get(key);
                Assert.assertTrue(out != null);
            }
        }
    }

    @Test
    public void testPerfKey2() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (int m = 0; m < METHODS.length; ++m) {
                String method = METHODS[m];
                for (int p0 = 0; p0 < ARGS.length; ++p0) {
                    checkKey(method, ARGS[p0].getClass());
                    for (int p1 = 0; p1 < ARGS.length; ++p1) {
                        checkKey(method, ARGS[p0].getClass(), ARGS[p1].getClass());
                        for (int p2 = 0; p2 < ARGS.length; ++p2) {
                            checkKey(method, ARGS[p0].getClass(), ARGS[p1].getClass(), ARGS[p2].getClass());
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testPerfStringKey2() throws Exception {
        for (int l = 0; l < LOOP; ++l) {
            for (int m = 0; m < METHODS.length; ++m) {
                String method = METHODS[m];
                for (int p0 = 0; p0 < ARGS.length; ++p0) {
                    checkStringKey(method, ARGS[p0].getClass());
                    for (int p1 = 0; p1 < ARGS.length; ++p1) {
                        checkStringKey(method, ARGS[p0].getClass(), ARGS[p1].getClass());
                        for (int p2 = 0; p2 < ARGS.length; ++p2) {
                            checkStringKey(method, ARGS[p0].getClass(), ARGS[p1].getClass(), ARGS[p2].getClass());
                        }
                    }
                }
            }
        }
    }
}
