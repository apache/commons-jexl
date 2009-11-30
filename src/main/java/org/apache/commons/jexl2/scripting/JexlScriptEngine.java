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

package org.apache.commons.jexl2.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.Script;

/**
 * Implements the Jexl ScriptEngine for JSF-223.
 * <p>
 * This implementation gives access to both ENGINE_SCOPE and GLOBAL_SCOPE bindings.
 * When a JEXL script accesses a variable for read or write,
 * this implementation checks first ENGINE and then GLOBAL scope.
 * The first one found is used. 
 * If no variable is found, and the JEXL script is writing to a variable,
 * it will be stored in the ENGINE scope.
 * </p>
 * <p>
 * The implementation also creates the "JEXL" script object as an instance of the
 * class {@link JexlScriptObject} for access to utility methods and variables.
 * </p>
 * See
 * <a href="http://java.sun.com/javase/6/docs/api/javax/script/package-summary.html">Java Scripting API</a>
 * Javadoc.
 * @since 2.0
 */
public class JexlScriptEngine extends AbstractScriptEngine {

    /** Reserved key for JexlScriptObject. */
    public static final String JEXL_OBJECT_KEY = "JEXL";

    /** Reserved key for context (mandated by JSR-223). */
    public static final String CONTEXT_KEY = "context";

    /** The factory which created this instance. */
    private final ScriptEngineFactory parentFactory;
    
    /** The JEXL EL engine. */
    private final JexlEngine jexlEngine;
    
    /**
     * Default constructor.
     * <p>
     * Only intended for use when not using a factory.
     * Sets the factory to {@link JexlScriptEngineFactory}.
     */
    public JexlScriptEngine() {
        this(SingletonHolder.DEFAULT_FACTORY);
    }

    /**
     * Create a scripting engine using the supplied factory.
     * 
     * @param factory the factory which created this instance.
     * @throws NullPointerException if factory is null
     */
    public JexlScriptEngine(final ScriptEngineFactory factory) {
        if (factory == null) {
            throw new NullPointerException("ScriptEngineFactory must not be null");
        }
        parentFactory = factory;
        jexlEngine = new JexlEngine();
        // Add utility object
        put(JEXL_OBJECT_KEY, new JexlScriptObject());
    }

    /** {@inheritDoc} */
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    /** {@inheritDoc} */
    public Object eval(Reader script, ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        if (script == null || context == null) {
            throw new NullPointerException("script and context must be non-null");
        }
        BufferedReader reader = new BufferedReader(script);
        StringBuilder buffer = new StringBuilder();
        try {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append('\n');
                }
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // NOOP
            }
        }
        return eval(buffer.toString(), context);
    }

    /** {@inheritDoc} */
    public Object eval(String scriptText, final ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        if (scriptText == null || context == null) {
            throw new NullPointerException("script and context must be non-null");
        }
        // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - Script Execution)
        context.setAttribute(CONTEXT_KEY, context, ScriptContext.ENGINE_SCOPE);
        
        try {
            Script script = jexlEngine.createScript(scriptText);
            JexlContext ctxt = new JexlContextWrapper(context);
            return script.execute(ctxt);
        } catch (Exception e) {
            throw new ScriptException(e.toString());
        }
    }

    /** {@inheritDoc} */
    public ScriptEngineFactory getFactory() {
        return parentFactory;
    }

    /**
     * Holds singleton JexlScriptEngineFactory (IODH). 
     */
    private static class SingletonHolder {
        /** The singleton instance. */
        private static final JexlScriptEngineFactory DEFAULT_FACTORY = new JexlScriptEngineFactory();
    }

    /**
     * Wrapper to help convert a JSR-223 ScriptContext into a JexlContext.
     *
     * Current implementation only gives access to ENGINE_SCOPE binding.
     */
    private static class JexlContextWrapper implements JexlContext {
        /** The engine context. */
        private final ScriptContext engineContext;
        /**
         * Create the class.
         *
         * @param context the engine context.
         */
        private JexlContextWrapper (final ScriptContext  context){
            engineContext = context;
        }

        /** {@inheritDoc} */
        public Object get(String name) {
            return engineContext.getAttribute(name);
        }

        /** {@inheritDoc} */
        public void set(String name, Object value) {
            int scope = engineContext.getAttributesScope(name);
            if (scope == -1) { // not found, default to engine
                scope = ScriptContext.ENGINE_SCOPE;
            }
            engineContext.getBindings(scope).put(name , value);
        }

        /** {@inheritDoc} */
        public boolean has(String name) {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.containsKey(name);
        }

    }
}
