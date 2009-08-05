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

package org.apache.commons.jexl;
import java.lang.reflect.Method;
import junit.framework.TestCase;
/**
 * Implements a runTest method to dynamically invoke a test,
 * wrapping the call with setUp(), tearDown() calls.
 * Eases the implementation of main methods to debug.
 */
public class JexlTestCase extends TestCase {
    /** No parameters signature for test run. */
    private static final Class<?>[] noParms = {};

    /** A default Jexl engine instance. */
    protected final JexlEngine JEXL = new JexlEngine();

    public JexlTestCase(String name) {
        super(name);
    }
    public JexlTestCase() {
        super();
    }

    public void runTest(String name) throws Exception {
        if ("runTest".equals(name)) {
            return;
        }
        Method method = null;
        try {
            method = this.getClass().getDeclaredMethod(name, noParms);
        }
        catch(Exception xany) {
            fail("no such test: " + name);
            return;
        }
        try {
            this.setUp();
            method.invoke(this);
        } finally {
            this.tearDown();
        }
    }

    /*public void testRunTest() throws Exception {
        new JexlTestCase().runTest("runTest");
    }*/

}
