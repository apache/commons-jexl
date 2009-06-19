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
package org.apache.commons.jexl.util.introspection;
import junit.framework.TestCase;
/**
 * Checks the CacheMap.MethodKey implementation
 */
public class MethodKeyTest extends TestCase {
    // A set of classes (most of them primitives)
    static Class[] PRIMS = {
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
    static Object[] ARGS = {
        new Boolean(true),
        new Byte((byte) 1),
        new Character('2'),
        new Double(4d),
        new Float(8f),
        new Integer(16),
        new Long(32l),
        new Short((short)64),
        "foobar",
        new java.util.Date()
    };
    
    // A set of (pseudo) method names
    static String[] METHODS = {
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
    static java.util.Map<ClassMap.MethodKey, String> byKey;
    /** form string to key */
    static java.util.Map<String, ClassMap.MethodKey> byString;
    /** the list of keys we generated & test against */
    static ClassMap.MethodKey[] keyList;
    
    /** Creates & inserts a key into the byKey & byString map */
    static void setUpKey(String name, Class[] parms) {
        ClassMap.MethodKey key = new ClassMap.MethodKey(name, parms);
        String str = key.toString();
        byKey.put(key, str);
        byString.put(str, key);
        
    }

    /** Generate a list of method*(prims*), method(prims*, prims*), method*(prims*,prims*,prims*) */
    static {
        byKey = new java.util.HashMap<ClassMap.MethodKey, String>();
        byString = new java.util.HashMap<String, ClassMap.MethodKey>();
        for (int m = 0; m < METHODS.length; ++m) {
            String method = METHODS[m];
            for (int p0 = 0; p0 < PRIMS.length; ++p0) {
                Class[] arg0 = {PRIMS[p0]};
                setUpKey(method, arg0);
                for (int p1 = 0; p1 < PRIMS.length; ++p1) {
                    Class[] arg1 = {PRIMS[p0], PRIMS[p1]};
                    setUpKey(method, arg1);
                    for (int p2 = 0; p2 < PRIMS.length; ++p2) {
                        Class[] arg2 = {PRIMS[p0], PRIMS[p1], PRIMS[p2]};
                        setUpKey(method, arg2);
                    }
                }
            }
        }
        keyList = byKey.keySet().toArray(new ClassMap.MethodKey[byKey.size()]);
    }

    /** Builds a string key */
    String makeStringKey(String method, Class... params) {
            StringBuilder builder = new StringBuilder(method);
            for(int p = 0; p < params.length; ++p) {
                builder.append(ClassMap.MethodCache.primitiveClass(params[p]).getName());
            }
            return builder.toString();
    }
    
    /** Checks that a string key does exist */
    void checkStringKey(String method, Class... params) {
        String key = makeStringKey(method, params);
        ClassMap.MethodKey out = byString.get(key);
        assertTrue(out != null);
    }
        
    /** Builds a method key */
    ClassMap.MethodKey makeKey(String method, Class... params) {
        return new ClassMap.MethodKey(method, params);
    }
    
    /** Checks that a method key exists */
    void checkKey(String method, Class... params) {
        ClassMap.MethodKey key = makeKey(method, params);
        String out = byKey.get(key);
        assertTrue(out != null);
    }
    
    public void testObjectKey() throws Exception {
        for(int k = 0; k < keyList.length; ++k) {
            ClassMap.MethodKey ctl = keyList[k];
            ClassMap.MethodKey key = makeKey(ctl.method, ctl.params);
            String out = byKey.get(key);
            assertTrue(out != null);
            assertTrue(ctl.toString() + " != " + out, ctl.toString().equals(out));
        }
        
    }
    
    public void testStringKey() throws Exception {
        for(int k = 0; k < keyList.length; ++k) {
            ClassMap.MethodKey ctl = keyList[k];
            String key = makeStringKey(ctl.method, ctl.params);
            ClassMap.MethodKey out = byString.get(key);
            assertTrue(out != null);
            assertTrue(ctl.toString() + " != " + key, ctl.equals(out));
        }
        
    }
    
    static final int LOOP = 3;//00;
    
    public void testPerfKey() throws Exception {
        for(int l = 0; l < LOOP; ++l)
        for(int k = 0; k < keyList.length; ++k) {
            ClassMap.MethodKey ctl = keyList[k];
            ClassMap.MethodKey key = makeKey(ctl.method, ctl.params);
            String out = byKey.get(key);
            assertTrue(out != null);
        }
    }
    
    public void testPerfString() throws Exception {
        for(int l = 0; l < LOOP; ++l)
        for(int k = 0; k < keyList.length; ++k) {
            ClassMap.MethodKey ctl = keyList[k];
            String key = makeStringKey(ctl.method, ctl.params);
            ClassMap.MethodKey out = byString.get(key);
            assertTrue(out != null);
        }
    }
    
    public void testPerfKey2() throws Exception {
        for(int l = 0; l < LOOP; ++l)
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
    
    public void testPerfStringKey2() throws Exception {
        for(int l = 0; l < LOOP; ++l)
        for (int m = 0; m < METHODS.length; ++m) {
            String method = METHODS[m];
            for (int p0 = 0; p0 < ARGS.length; ++p0) {
                checkStringKey(method, ARGS[p0].getClass());
                for (int p1 = 0; p1 < ARGS.length; ++p1) {
                    checkStringKey(method,  ARGS[p0].getClass(), ARGS[p1].getClass());
                    for (int p2 = 0; p2 < ARGS.length; ++p2) {
                        checkStringKey(method, ARGS[p0].getClass(), ARGS[p1].getClass(), ARGS[p2].getClass());
                    }
                }
            }
        }
    }
}
