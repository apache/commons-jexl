/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.apache.commons.jexl.scripting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Implements the Jexl ScriptEngineFactory for JSF-223.
 * <p>
 * Supports the following:<br.>
 * Language short names: "JEXL", "Jexl", "jexl" <br/>
 * Extension: "jexl"
 * </p>
 * <p>
 * See
 * <a href="http://java.sun.com/javase/6/docs/api/javax/script/package-summary.html">Java Scripting API</a>
 * Javadoc.
 */
public class JexlScriptEngineFactory implements ScriptEngineFactory {
    
    private static final List<String> extensions = Collections.unmodifiableList(
            Arrays.asList("jexl"));

    private final List<String> mimeTypes = Collections.unmodifiableList(
            Arrays.asList("application/x-jexl"));

    private final List<String> names = Collections.unmodifiableList(
            Arrays.asList( "JEXL", "Jexl", "jexl" ));

    /** {@inheritDoc} */
    public String getEngineName() {
        return "JEXL Engine";
    }

    /** {@inheritDoc} */
    public String getEngineVersion() {
        return "1.0"; // ensure this is updated if function changes are made to this class
    }

    /** {@inheritDoc} */
    public List<String> getExtensions() {
        return extensions;
    }

    /** {@inheritDoc} */
    public String getLanguageName() {
        return "JEXL";
    }

    /** {@inheritDoc} */
    public String getLanguageVersion() {
        return "2.0"; // TODO this should be derived from the actual version
    }

    /** {@inheritDoc} */
    public String getMethodCallSyntax(String object, String method, String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(object);
        sb.append('.');
        sb.append(method);
        sb.append('(');
        boolean needComma = false;
        for(String arg : args){
            if (needComma) {
                sb.append(',');
            }
            sb.append(arg);
            needComma = true;
        }
        sb.append(')');
        return sb.toString();
    }

    /** {@inheritDoc} */
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    /** {@inheritDoc} */
    public List<String> getNames() {
        return names;
    }

    /** {@inheritDoc} */
    public String getOutputStatement(String message) {  // TODO - is there one?
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** {@inheritDoc} */
    public Object getParameter(String key) {
        if (key == ScriptEngine.ENGINE) {
            return getEngineName();
        } else if (key == ScriptEngine.ENGINE_VERSION) {
            return getEngineVersion();
        } else if (key == ScriptEngine.NAME) {
            return getNames();
        } else if (key == ScriptEngine.LANGUAGE) {
            return getLanguageName();
        } else if(key == ScriptEngine.ENGINE_VERSION) {
            return getLanguageVersion();
        } else if (key == "THREADING") {
            return null;//"MULTITHREADED"; // TODO what is the correct value here?
        } 
        return null;
    }

    /** {@inheritDoc} */
    public String getProgram(String[] args) {
        StringBuilder sb = new StringBuilder();
        for(String arg : args){
            sb.append(arg);
            if (!arg.endsWith(";")){
                sb.append(';');
            }
        }
        return sb.toString();
    }

    /** {@inheritDoc} */
    public ScriptEngine getScriptEngine() {
        JexlScriptEngine engine = new JexlScriptEngine(this);
        return engine;
    }

}
