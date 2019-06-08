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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


import org.apache.commons.jexl3.internal.Util;
import org.junit.After;
import org.junit.Assert;

/**
 * Implements runTest methods to dynamically instantiate and invoke a test,
 * wrapping the call with setUp(), tearDown() calls.
 * Eases the implementation of main methods to debug.
 */
public class JexlTestCase {
    /** No parameters signature for test run. */
    private static final Class<?>[] NO_PARMS = {};
    /** String parameter signature for test run. */
    private static final Class<?>[] STRING_PARM = {String.class};

    /** A default JEXL engine instance. */
    protected final JexlEngine JEXL;

    public JexlTestCase(String name) {
        this(name, new JexlBuilder().strict(true).silent(false).cache(32).create());
    }

    protected JexlTestCase(String name, JexlEngine jexl) {
        //super(name);
        JEXL = jexl;
    }

    public void setUp() throws Exception {
        // nothing to do
    }

    @After
    public void tearDown() throws Exception {
        debuggerCheck(JEXL);
    }
   
    static JexlEngine createEngine() {
        return new JexlBuilder().create();
    }
    
    public static JexlEngine createEngine(boolean lenient) {
        return new JexlBuilder().arithmetic(new JexlArithmetic(!lenient)).cache(512).create();
    }

    /**
     * Will force testing the debugger for each derived test class by
     * recreating each expression from the JexlNode in the JexlEngine cache &
     * testing them for equality with the origin.
     * @throws Exception
     */
    public static void debuggerCheck(JexlEngine ijexl) throws Exception {
         Util.debuggerCheck(ijexl);
    }

    /**
     * Dynamically runs a test method.
     * @param name the test method to run
     * @throws Exception if anything goes wrong
     */
    public void runTest(String name) throws Exception {
        if ("runTest".equals(name)) {
            return;
        }
        Method method = null;
        try {
            method = this.getClass().getDeclaredMethod(name, NO_PARMS);
        }
        catch(Exception xany) {
            Assert.fail("no such test: " + name);
            return;
        }
        try {
            this.setUp();
            method.invoke(this);
        } finally {
            this.tearDown();
        }
    }

    /**
     * Instantiate and runs a test method; useful for debugging purpose.
     * For instance:
     * <code>
     * public static void main(String[] args) throws Exception {
     *   runTest("BitwiseOperatorTest","testAndVariableNumberCoercion");
     * }
     * </code>
     * @param tname the test class name
     * @param mname the test class method
     * @throws Exception
     */
    public static void runTest(String tname, String mname) throws Exception {
        String testClassName = "org.apache.commons.jexl3."+tname;
        Class<JexlTestCase> clazz = null;
        JexlTestCase test = null;
        // find the class
        try {
            clazz = (Class<JexlTestCase>) Class.forName(testClassName);
        }
        catch(ClassNotFoundException xclass) {
            Assert.fail("no such class: " + testClassName);
            return;
        }
        // find ctor & instantiate
        Constructor<JexlTestCase> ctor = null;
        try {
            ctor = clazz.getConstructor(STRING_PARM);
            test = ctor.newInstance("debug");
        }
        catch(NoSuchMethodException xctor) {
            // instantiate default class ctor
            try {
                test = clazz.newInstance();
            }
            catch(Exception xany) {
                Assert.fail("cant instantiate test: " + xany);
                return;
            }
        }
        catch(Exception xany) {
            Assert.fail("cant instantiate test: " + xany);
            return;
        }
        // Run the test
        test.runTest(mname);
    }

    /**
     * Runs a test.
     * @param args where args[0] is the test class name and args[1] the test class method
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        runTest(args[0], args[1]);
    }
}
