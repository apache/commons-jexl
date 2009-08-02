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

import java.io.File;
import java.net.URL;


/**
 * Tests for ScriptFactory
 * @since 1.1
 */
public class ScriptFactoryTest extends JexlTestCase {
    static final String TEST1 = "src/test/scripts/test1.jexl";
    /**
     * Creates a new test case.
     * @param name the test case name.
     */
    public ScriptFactoryTest(String name) {
        super(name);
    }
    
    /**
     * Ensure the factory can create a script from a String
     * @throws Exception on a parse error
     */
    public void testCreateFromString() throws Exception {
        String code = ";";
        assertNotNull("No script created", ScriptFactory.createScript(code));
    }

    /**
     * Ensure the factory can create a script from a String
     * @throws Exception on a parse error
     */
    public void testCreateFromSimpleString() throws Exception {
        String code = "(1 + 2) * 4";
        assertNotNull("No script created", ScriptFactory.createScript(code));
    }

    /**
     * Ensure the factory throws an NPE on an null String
     * @throws Exception on a parse error
     */
    public void testCreateFromNullString() throws Exception {
        String code = null;
        try {
            assertNotNull("No script created", ScriptFactory.createScript(code));
            fail("Null script created");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /**
     * Ensure the factory can create a script from a file.
     * @throws Exception on a parse error.
     */
    public void testCreateFromFile() throws Exception {
        File testScript = new File(TEST1);
        assertNotNull("No script created", ScriptFactory.createScript(testScript));
    }

    /**
     * Ensure the factory throws npe on a null file.
     * @throws Exception on a parse error.
     */
    public void testCreateFromNullFile() throws Exception {
        File testScript = null;
        try
        {
            assertNotNull("No script created", ScriptFactory.createScript(testScript));
            fail("Null script created");
        }
        catch (NullPointerException e)
        {
            // expected
        }
    }

    /**
     * Ensure the factory can create a script from a URL.
     * @throws Exception on a parse error.
     */
    public void testCreateFromURL() throws Exception {
        URL testUrl = new File("src/test/scripts/test1.jexl").toURI().toURL();
        assertNotNull("No script created", ScriptFactory.createScript(testUrl));
    }

    /**
     * Ensure the factory throws an NPE on an null URL
     * @throws Exception on a parse error
     */
    public void testCreateFromNullURL() throws Exception {
        URL code = null;
        try {
            assertNotNull("No script created", ScriptFactory.createScript(code));
            fail("Null script created");
        } catch (NullPointerException e) {
            // expected
        }
    }

}
