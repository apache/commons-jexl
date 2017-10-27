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

import java.util.Map;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for calling antish variables as method names (JEXL-240);
 * Also tests that a class instance is a functor that invokes the constructor when called.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class AntishCallTest extends JexlTestCase {

    public AntishCallTest() {
        super("AntishCallTest");
    }

    /**
     * Wraps a class.
     */
    public class ClassReference {
        final Class<?> clazz;
        ClassReference(Class<?> c) {
            this.clazz = c;
        }
    }

    /**
     * Considers any call using a class reference as functor as a call to its constructor.
     * <p>Note that before 3.2, a class was not considered a functor.
     * @param clazz the class we seek to instantiate
     * @param args the constructor arguments
     * @return an instance if that was possible
     */
    public static Object callConstructor(JexlEngine engine, ClassReference ref, Object... args) {
        return callConstructor(engine, ref.clazz, args);
    }
    public static Object callConstructor(JexlEngine engine, Class<?> clazz, Object... args) {
        if (clazz == null || clazz.isPrimitive() || clazz.isInterface()
            || clazz.isMemberClass() || clazz.isAnnotation() || clazz.isArray()) {
            throw new ArithmeticException("not a constructible object");
        }
        JexlEngine jexl = engine;
        if (jexl == null) {
            jexl = JexlEngine.getThreadEngine();
            if (jexl == null) {
                throw new ArithmeticException("no engine to solve constructor");
            }
        }
        return jexl.newInstance(clazz, args);
    }

    /**
     * An arithmetic that considers class objects as callable.
     */
    public class CallSupportArithmetic extends JexlArithmetic {
        public CallSupportArithmetic(boolean strict) {
            super(strict);
        }

        public Object call(ClassReference clazz, Object... args) {
            return callConstructor(null, clazz, args);
        }

        public Object call(Class<?> clazz, Object... args) {
            return callConstructor(null, clazz, args);
        }
    }

    /**
     * A context that considers class references as callable.
     */
    public static class CallSupportContext extends MapContext {
        CallSupportContext(Map<String, Object> map) {
            super(map);
        }
        private JexlEngine engine;

        @Override public Object get(String str) {
            if (!super.has(str)) {
                try {
                    return CallSupportContext.class.getClassLoader().loadClass(str);
                } catch(Exception xany) {
                    return null;
                }
            }
            return super.get(str);
        }

        @Override public boolean has(String str) {
            if (!super.has(str)){
                try {
                    return CallSupportContext.class.getClassLoader().loadClass(str) != null;
                } catch(Exception xany) {
                    return false;
                }
            }
            return true;
        }

        CallSupportContext engine(JexlEngine j) {
            engine = j;
            return this;
        }

        public Object call(ClassReference clazz, Object... args) {
            return callConstructor(engine, clazz, args);
        }

        public Object call(Class<?> clazz, Object... args) {
            return callConstructor(engine, clazz, args);
        }
    }

    @Test
    public void testAntishContextVar() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();
        Map<String,Object> lmap = new TreeMap<String,Object>();
        JexlContext jc = new CallSupportContext(lmap).engine(jexl);
        runTestCall(jexl, jc);
        lmap.put("java.math.BigInteger", new ClassReference(java.math.BigInteger.class));
        runTestCall(jexl, jc);
        lmap.remove("java.math.BigInteger");
        runTestCall(jexl, jc);
    }

    @Test
    public void testAntishArithmetic() throws Exception {
        CallSupportArithmetic ja = new CallSupportArithmetic(true);
        JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).arithmetic(ja).create();
        Map<String,Object> lmap = new TreeMap<String,Object>();
        JexlContext jc = new MapContext(lmap);
        lmap.put("java.math.BigInteger", java.math.BigInteger.class);
        runTestCall(jexl, jc);
        lmap.put("java.math.BigInteger", new ClassReference(java.math.BigInteger.class));
        runTestCall(jexl, jc);
        lmap.remove("java.math.BigInteger");
        try {
            runTestCall(jexl, jc);
        Assert.fail("should have failed");
        } catch(JexlException xjexl) {
            //
        }
    }

    void runTestCall(JexlEngine jexl, JexlContext jc) throws Exception {
        JexlScript check1 = jexl.createScript("var x = java.math.BigInteger; x('1234')");
        JexlScript check2 = jexl.createScript("java.math.BigInteger('4321')");

        Object o1 = check1.execute(jc);
        Assert.assertEquals("Result is not 1234", new java.math.BigInteger("1234"), o1);

        Object o2 = check2.execute(jc);
        Assert.assertEquals("Result is not 4321", new java.math.BigInteger("4321"), o2);
    }

}