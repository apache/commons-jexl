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

package org.apache.commons.jexl.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlEngine;
import org.apache.commons.jexl.Script;

// Note: this is a generated class, so won't be present until JavaCC has been run
import org.apache.commons.jexl.parser.ParseException;

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
    @SuppressWarnings("unchecked")
    public Object eval(String scriptText, final ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        if (scriptText == null || context == null) {
            throw new NullPointerException("script and context must be non-null");
        }
        // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - Script Execution)
        context.setAttribute(CONTEXT_KEY, context, ScriptContext.ENGINE_SCOPE);
        
        try {
            Script script = jexlEngine.createScript(scriptText);
            JexlContext ctxt = new JexlContext(){
                public void setVars(Map vars) {
                    context.setBindings(new SimpleBindings(vars), ScriptContext.ENGINE_SCOPE);
                }

                public Map<String,Object> getVars() {
                    return new JexlContextWrapper(context);
                }
            };
            return script.execute(ctxt);
        } catch (ParseException e) {
            throw new ScriptException(e.toString());
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
    @SuppressWarnings("unchecked")
    private static class JexlContextWrapper implements Map<String,Object> {
        
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
        public void clear() {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            bnd.clear();
        }

        /** {@inheritDoc} */
        public boolean containsKey(final Object key) {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.containsKey(key);
        }

        /** {@inheritDoc} */
        public boolean containsValue(final Object value) {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.containsValue(value);
        }

        /** {@inheritDoc} */
        public Set entrySet() {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.entrySet();
        }

        // Fetch first match of key, either engine or global
        /** {@inheritDoc} */
        public Object get(final Object key) {
            if (key instanceof String) {
                return engineContext.getAttribute((String) key);
            }
            return null;
        }

        /** {@inheritDoc} */
        public boolean isEmpty() {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.isEmpty();
        }

        /** {@inheritDoc} */
        public Set keySet() {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.keySet();
        }

        // Update existing key if found, else create new engine key
        /** {@inheritDoc} */
        public Object put(final String key, final Object value) {
            int scope = engineContext.getAttributesScope(key);
            if (scope == -1) { // not found, default to engine
                scope = ScriptContext.ENGINE_SCOPE;
            }
            return engineContext.getBindings(scope).put(key , value);
        }

        /** {@inheritDoc} */
        public void putAll(Map t) {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            bnd.putAll(t); // N.B. SimpleBindings checks for valid keys
        }

        // N.B. if there is more than one copy of the key, only the nearest will be removed.
        /** {@inheritDoc} */
        public Object remove(Object key) {
            if (key instanceof String){
                int scope = engineContext.getAttributesScope((String) key);
                if (scope != -1) { // found an entry
                    return engineContext.removeAttribute((String)key, scope);
                }
            }
            return null;
        }

        /** {@inheritDoc} */
        public int size() {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.size();
        }

        /** {@inheritDoc} */
        public Collection values() {
            Bindings bnd = engineContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.values();
        }

    }
}
