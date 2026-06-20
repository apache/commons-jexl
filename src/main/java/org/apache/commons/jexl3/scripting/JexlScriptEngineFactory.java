/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.parser.StringParser;

/**
 * Implements the JEXL ScriptEngineFactory for JSF-223.
 * <p>
 * Supports the following:<br>
 * </p>
 * <ul>
 * <li>Language short names: "JEXL", "Jexl", "jexl", "JEXL2", "Jexl2", "jexl2", "JEXL3", "Jexl3", "jexl3"</li>
 * <li>File Extensions: ".jexl", ".jexl2", ".jexl3"</li>
 * <li>"jexl3" etc. were added for engineVersion="3.0"</li>
 * </ul>
 * <p>
 * See
 * <a href="https://java.sun.com/javase/6/docs/api/javax/script/package-summary.html">Java Scripting API</a>
 * Javadoc.
 *
 * @since 2.0
 */
public class JexlScriptEngineFactory implements ScriptEngineFactory {
    /**
     * The default factory permissions.
     */
    private static JexlPermissions defaultPermissions;

    /**
     * The engine permissions.
     */
    private final JexlPermissions permissions;

    /**
     * Default constructor.
     */
    public JexlScriptEngineFactory() { this(null); } // Keep Javadoc happy

    /**
     * Constructor with permissions.
     * <p>Meant to reduce dependency to JEXL for extraordinary use case of JSR233.</p>
     * @param jexlPermissions the permissions instance to use or null to use the {@link JexlScriptEngineFactory} default
     */
    public JexlScriptEngineFactory(JexlPermissions jexlPermissions) {
        permissions = jexlPermissions != null ? jexlPermissions : defaultPermissions;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new JexlScriptEngine(this);
    }

    /**
     * Creates an engine.
     * @return the JexlEngine instance, create it if necessary
     */
    protected JexlEngine getEngine() {
        return createJexlEngine();
    }

    /**
     * Creates a new JexlEngine instance.
     * @return a new JexlEngine instance
     */
    protected JexlEngine createJexlEngine() {
        JexlBuilder builder = new JexlBuilder()
          .strict(true)
          .safe(false)
          .logger(JexlScriptEngine.LOG)
          .cache(JexlScriptEngine.CACHE_SIZE);
        JexlPermissions p = permissions;
        if (p == null) {
            p = defaultPermissions;
            if (p == null) {
                p = builder.permissions();
                if (p == null) {
                    p = JexlPermissions.RESTRICTED;
                }
            }
        }
        JexlPermissions required = new JexlPermissions.ClassPermissions(p, JexlScriptEngine.JexlScriptObject.class);
        builder.permissions(required);
        return builder.create();
    }

    /**
     * Sets the permissions instance used to create the script engine.
     * <p>To restore 3.2 <em>unsafe</em> script behavior:</p>
     * {@code
     *         JexlScriptEngineFactory.setDefaultPermissions(JexlPermissions.UNRESTRICTED);
     * }
     *
     * @param permissions the permissions instance to use or null to use the {@link JexlBuilder} default
     * @since 3.6.3
     */
    public static void setDefaultPermissions(final JexlPermissions permissions) {
        defaultPermissions = permissions;
    }

    @Override
    public String getEngineName() {
        return "JEXL Engine";
    }

    @Override
    public String getEngineVersion() {
        return "3.6"; // ensure this is updated if function changes are made to this class
    }

    @Override
    public List<String> getExtensions() {
        return Collections.unmodifiableList(Arrays.asList("jexl", "jexl2", "jexl3"));
    }

    @Override
    public String getLanguageName() {
        return "JEXL";
    }

    @Override
    public String getLanguageVersion() {
        return "3.6"; // this should be derived from the actual version
    }

    @Override
    public String getMethodCallSyntax(final String obj, final String m, final String... args) {
        final StringBuilder sb = new StringBuilder();
        sb.append(obj);
        sb.append('.');
        sb.append(m);
        sb.append('(');
        boolean needComma = false;
        for(final String arg : args){
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
    public String getOutputStatement(final String toDisplay) {
        if (toDisplay == null) {
            return "JEXL.out.print(null)";
        }
        return "JEXL.out.print("+StringParser.escapeString(toDisplay, '\'')+")";
    }

    @Override
    public Object getParameter(final String key) {
        switch (key) {
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.NAME:
                return getNames();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            case "THREADING":
                /*
                 * To implement multithreading, the scripting engine context (inherited from AbstractScriptEngine)
                 * would need to be made thread-safe; so would the setContext/getContext methods.
                 * It is easier to share the underlying Uberspect and JEXL engine instance, especially
                 * with an expression cache.
                 */
            default:
                return null;
        }
    }

    @Override
    public String getProgram(final String... statements) {
        final StringBuilder sb = new StringBuilder();
        for(final String statement : statements){
            sb.append(statement.trim());
            if (!statement.endsWith(";")){
                sb.append(';');
            }
        }
        return sb.toString();
    }

}
