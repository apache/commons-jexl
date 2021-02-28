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

import java.math.BigInteger;
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
    public static class ClassReference {
        final Class<?> clazz;
        ClassReference(final Class<?> c) {
            this.clazz = c;
        }
    }

    /**
     * Considers any call using a class reference as functor as a call to its constructor.
     * <p>Note that before 3.2, a class was not considered a functor.
     * @param ref the ClassReference of the class we seek to instantiate
     * @param args the constructor arguments
     * @return an instance if that was possible
     */
    public static Object callConstructor(final JexlEngine engine, final ClassReference ref, final Object... args) {
        return callConstructor(engine, ref.clazz, args);
    }
    public static Object callConstructor(final JexlEngine engine, final Class<?> clazz, final Object... args) {
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
        public CallSupportArithmetic(final boolean strict) {
            super(strict);
        }

        public Object call(final ClassReference clazz, final Object... args) {
            return callConstructor(null, clazz, args);
        }

        public Object call(final Class<?> clazz, final Object... args) {
            return callConstructor(null, clazz, args);
        }
    }

    /**
     * A context that considers class references as callable.
     */
    public static class CallSupportContext extends MapContext {
        CallSupportContext(final Map<String, Object> map) {
            super(map);
        }
        private JexlEngine engine;

        @Override public Object get(final String str) {
            if (!super.has(str)) {
                try {
                    return CallSupportContext.class.getClassLoader().loadClass(str);
                } catch(final Exception xany) {
                    return null;
                }
            }
            return super.get(str);
        }

        @Override public boolean has(final String str) {
            if (!super.has(str)){
                try {
                    return CallSupportContext.class.getClassLoader().loadClass(str) != null;
                } catch(final Exception xany) {
                    return false;
                }
            }
            return true;
        }

        CallSupportContext engine(final JexlEngine j) {
            engine = j;
            return this;
        }

        public Object call(final ClassReference clazz, final Object... args) {
            return callConstructor(engine, clazz, args);
        }

        public Object call(final Class<?> clazz, final Object... args) {
            return callConstructor(engine, clazz, args);
        }
    }

    @Test
    public void testAntishContextVar() throws Exception {
        final Map<String,Object> lmap = new TreeMap<String,Object>();
        final JexlContext jc = new CallSupportContext(lmap).engine(JEXL);
        runTestCall(JEXL, jc);
        lmap.put("java.math.BigInteger", new ClassReference(BigInteger.class));
        runTestCall(JEXL, jc);
        lmap.remove("java.math.BigInteger");
        runTestCall(JEXL, jc);
    }

    @Test
    public void testAntishArithmetic() throws Exception {
        final CallSupportArithmetic ja = new CallSupportArithmetic(true);
        final JexlEngine jexl = new JexlBuilder().cache(512).arithmetic(ja).create();
        final Map<String,Object> lmap = new TreeMap<String,Object>();
        final JexlContext jc = new MapContext(lmap);
        lmap.put("java.math.BigInteger", java.math.BigInteger.class);
        runTestCall(jexl, jc);
        lmap.put("java.math.BigInteger", new ClassReference(BigInteger.class));
        runTestCall(jexl, jc);
        lmap.remove("java.math.BigInteger");
        try {
            runTestCall(jexl, jc);
        Assert.fail("should have failed");
        } catch(final JexlException xjexl) {
            //
        }
    }

    void runTestCall(final JexlEngine jexl, final JexlContext jc) throws Exception {
        final JexlScript check1 = jexl.createScript("var x = java.math.BigInteger; x('1234')");
        final JexlScript check2 = jexl.createScript("java.math.BigInteger('4321')");

        final Object o1 = check1.execute(jc);
        Assert.assertEquals("Result is not 1234", new java.math.BigInteger("1234"), o1);

        final Object o2 = check2.execute(jc);
        Assert.assertEquals("Result is not 4321", new java.math.BigInteger("4321"), o2);
    }

    // JEXL-300
    @Test
    public void testSafeAnt() throws Exception {
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        ctxt.set("x.y.z", 42);
        JexlScript script;
        Object result;

        script = JEXL.createScript("x.y.z");
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
        Assert.assertEquals(42, ctxt.get("x.y.z"));

        options.setAntish(false);
        try {
            result = script.execute(ctxt);
            Assert.fail("antish var shall not be resolved");
        } catch(final JexlException.Variable xvar) {
            Assert.assertEquals("x", xvar.getVariable());
        } catch(final JexlException xother) {
            Assert.assertNotNull(xother);
        } finally {
            options.setAntish(true);
        }

        result = null;
        script = JEXL.createScript("x?.y?.z");
        result = script.execute(ctxt);
        Assert.assertNull(result); // safe navigation, null

        result = null;
        script = JEXL.createScript("x?.y?.z = 3");
        try {
             result = script.execute(ctxt);
             Assert.fail("not antish assign");
        } catch(final JexlException xjexl) {
            Assert.assertNull(result);
        }

        result = null;
        script = JEXL.createScript("x.y?.z");
        try {
             result = script.execute(ctxt);
             Assert.fail("x not defined");
        } catch(final JexlException xjexl) {
            Assert.assertNull(result);
        }

        result = null;
        script = JEXL.createScript("x.y?.z = 3");
        try {
             result = script.execute(ctxt);
             Assert.fail("not antish assign");
        } catch(final JexlException xjexl) {
            Assert.assertNull(result);
        }

        result = null;
        script = JEXL.createScript("x.`'y'`.z = 3");
        try {
             result = script.execute(ctxt);
             Assert.fail("not antish assign");
        } catch(final JexlException xjexl) {
            Assert.assertNull(result);
        }
    }

}