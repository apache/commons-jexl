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

package org.apache.commons.jexl3.scripting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.apache.commons.jexl3.parser.StringParser;

/**
 * Implements the JEXL ScriptEngineFactory for JSF-223.
 * <p>
 * Supports the following:<br>
 * Language short names: "JEXL", "Jexl", "jexl", "JEXL2", "Jexl2", "jexl2", "JEXL3", "Jexl3", "jexl3" <br>
 * File Extensions: ".jexl", ".jexl2", ".jexl3"<br>
 * "jexl3" etc. were added for engineVersion="3.0".
 * </p>
 * <p>
 * See
 * <a href="http://java.sun.com/javase/6/docs/api/javax/script/package-summary.html">Java Scripting API</a>
 * Javadoc.
 *
 * @since 2.0
 */
public class JexlScriptEngineFactory implements ScriptEngineFactory {

    @Override
    public String getEngineName() {
        return "JEXL Engine";
    }

    @Override
    public String getEngineVersion() {
        return "3.2"; // ensure this is updated if function changes are made to this class
    }

    @Override
    public String getLanguageName() {
        return "JEXL";
    }

    @Override
    public String getLanguageVersion() {
        return "3.2"; // TODO this should be derived from the actual version
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(obj);
        sb.append('.');
        sb.append(m);
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

    @Override
    public List<String> getExtensions() {
        return Collections.unmodifiableList(Arrays.asList("jexl", "jexl2", "jexl3"));
    }

    @Override
    public List<String> getMimeTypes() {
        return Collections.unmodifiableList(Arrays.asList("application/x-jexl",
                                                          "application/x-jexl2",
                                                          "application/x-jexl3"));
    }

    @Override
    public List<String> getNames() {
        return Collections.unmodifiableList(Arrays.asList("JEXL", "Jexl", "jexl",
                                                          "JEXL2", "Jexl2", "jexl2",
                                                          "JEXL3", "Jexl3", "jexl3"));
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        if (toDisplay == null) {
            return "JEXL.out.print(null)";
        } else {
            return "JEXL.out.print("+StringParser.escapeString(toDisplay, '\'')+")";
        }
    }

    @Override
    public Object getParameter(String key) {
        if (key.equals(ScriptEngine.ENGINE)) {
            return getEngineName();
        } else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            return getEngineVersion();
        } else if (key.equals(ScriptEngine.NAME)) {
            return getNames();
        } else if (key.equals(ScriptEngine.LANGUAGE)) {
            return getLanguageName();
        } else if(key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return getLanguageVersion();
        } else if (key.equals("THREADING")) {
            /*
             * To implement multithreading, the scripting engine context (inherited from AbstractScriptEngine)
             * would need to be made thread-safe; so would the setContext/getContext methods.
             * It is easier to share the underlying Uberspect and JEXL engine instance, especially
             * with an expression cache.
             */
            return null;
        }
        return null;
    }

    @Override
    public String getProgram(String... statements) {
        StringBuilder sb = new StringBuilder();
        for(String statement : statements){
            sb.append(statement.trim());
            if (!statement.endsWith(";")){
                sb.append(';');
            }
        }
        return sb.toString();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new JexlScriptEngine(this);
    }

}
