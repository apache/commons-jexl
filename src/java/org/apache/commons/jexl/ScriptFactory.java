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
import java.io.StringReader;
import java.net.URL;

import org.apache.commons.jexl.parser.Parser;


/**
 * <p>
 * Creates {@link Script}s.  To create a JEXL Script, pass
 * valid JEXL syntax to the static createScript() method:
 * </p>
 *
 * <pre>
 * String jexl = "y = x * 12 + 44; y = y * 4;";
 * Script script = ScriptFactory.createScript( jexl );
 * </pre>
 *
 * <p>
 * When an {@link Script} is created, the JEXL syntax is
 * parsed and verified.
 * </p>
 * @since 1.1
 * @version $Id$
 */
public class ScriptFactory {

    /**
     * The singleton ScriptFactory also holds a single instance of
     * {@link Parser}. When parsing expressions, ScriptFactory
     * synchronizes on Parser.
     */
    protected static Parser parser = new Parser(new StringReader(";"));

    /**
     * ScriptFactory is a singleton and this is the private
     * instance fulfilling that pattern.
     */
    protected static ScriptFactory factory = new ScriptFactory();

    /**
     * Private constructor, the single instance is always obtained
     * with a call to getInstance().
     */
    private ScriptFactory() {}
    
    /**
     * Returns the single instance of ScriptFactory.
     * @return the instance of ScriptFactory.
     */
    protected static  ScriptFactory getInstance() {
        return factory;
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws Exception An exception can be thrown if there is a
     *      problem parsing the script.
     */
    public static Script createScript(String scriptText) throws Exception {
        return JexlEngine.DEFAULT.createScript(scriptText);
    }

    /**
     * Creates a Script from a {@link File} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptFile A {@link File} containing valid JEXL syntax.
     *      Must not be null. Must be a readable file.
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws Exception An exception can be thrown if there is a problem
     *      parsing the script.
     */
    public static Script createScript(File scriptFile) throws Exception {
        return JexlEngine.DEFAULT.createScript(scriptFile);
    }

    /**
     * Creates a Script from a {@link URL} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptUrl A {@link URL} containing valid JEXL syntax.
     *      Must not be null. Must be a readable file.
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws Exception An exception can be thrown if there is a problem
     *      parsing the script.
     */
    public static Script createScript(URL scriptUrl) throws Exception {
        return JexlEngine.DEFAULT.createScript(scriptUrl);
    }

}
