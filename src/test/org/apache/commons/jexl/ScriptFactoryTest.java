/*
 * Copyright 2002-2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import junit.framework.TestCase;

/**
 * Tests for ScriptFactory
 * @since 1.1
 */
public class ScriptFactoryTest extends TestCase {

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
     * Ensure the factory can create a script from a file.
     * @throws Exception on a parse error.
     */
    public void testCreateFromFile() throws Exception {
        File testScript = new File("src/test-scripts/test1.jexl");
        assertNotNull("No script created", ScriptFactory.createScript(testScript));
    }

    /**
     * Ensure the factory can create a script from a URL.
     * @throws Exception on a parse error.
     */
    public void testCreateFromURL() throws Exception {
        URL testUrl = new File("src/test-scripts/test1.jexl").toURL();
        assertNotNull("No script created", ScriptFactory.createScript(testUrl));
    }
}
