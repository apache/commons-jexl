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

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
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
public class JexlScriptEngine extends AbstractScriptEngine implements Compilable {

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
     * The set of functions exposed in the default namespace.
     */
    public static final class JexlFunctions {
        /**
         * Calls System.out.println.
         * @param arg the argument
         */
        public void print(String arg) {
            System.out.println(arg);
        }
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
        // Add the jexl functions, ie print and escape
        Map<String,Object> funcs = new HashMap<String,Object>();
        funcs.put(null, new JexlFunctions());
        jexlEngine.setFunctions(funcs);
        // Add utility object
        put(JEXL_OBJECT_KEY, new JexlScriptObject());
    }

    /** {@inheritDoc} */
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    /** {@inheritDoc} */
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        if (reader == null || context == null) {
            throw new NullPointerException("script and context must be non-null");
        }

        return eval(readerToString(reader), context);
    }

    /** {@inheritDoc} */
    public Object eval(String script, final ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        if (script == null || context == null) {
            throw new NullPointerException("script and context must be non-null");
        }
        // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - Script Execution)
        context.setAttribute(CONTEXT_KEY, context, ScriptContext.ENGINE_SCOPE);
        
        try {
            Script jexlScript = jexlEngine.createScript(script);
            JexlContext ctxt = new JexlContextWrapper(context);
            return jexlScript.execute(ctxt);
        } catch (Exception e) {
            throw new ScriptException(e.toString());
        }
    }

    /** {@inheritDoc} */
    public ScriptEngineFactory getFactory() {
        return parentFactory;
    }

    /** {@inheritDoc} */
    public CompiledScript compile(String script) throws ScriptException {
        // This is mandated by JSR-223
        if (script == null) {
            throw new NullPointerException("script must be non-null");
        }
        try {
            Script jexlScript = jexlEngine.createScript(script);
            return new JexlCompiledScript(jexlScript);
        } catch (Exception e) {
            throw new ScriptException(e.toString());
        }
    }

    /** {@inheritDoc} */
    public CompiledScript compile(Reader script) throws ScriptException {
        // This is mandated by JSR-223
        if (script == null) {
            throw new NullPointerException("script must be non-null");
        }
        return compile(readerToString(script));
    }

    /**
     * Reads a script.
     * @param script the script reader
     * @return the script as a string
     * @throws ScriptException if an exception occurs during read
     */
    private String readerToString(Reader script) throws ScriptException {
        try {
           return JexlEngine.readerToString(script);
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    /**
     * Holds singleton JexlScriptEngineFactory (IODH). 
     */
    private static class SingletonHolder {
        /** non instantiable. */
        private SingletonHolder() {}
        /** The singleton instance. */
        private static final JexlScriptEngineFactory DEFAULT_FACTORY = new JexlScriptEngineFactory();
    }

    /**
     * Wrapper to help convert a JSR-223 ScriptContext into a JexlContext.
     *
     * Current implementation only gives access to ENGINE_SCOPE binding.
     */
    private static final class JexlContextWrapper implements JexlContext {
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

    /**
     * Wrapper to help convert a Jexl Script into a JSR-223 CompiledScript.
     */
    private final class JexlCompiledScript extends CompiledScript {
        /** The underlying Jexl expression instance. */
        private final Script script;

        /**
         * Creates an instance.
         * @param theScript to wrap
         */
        private JexlCompiledScript(Script theScript) {
            script = theScript;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return script.getText();
        }
        
        /** {@inheritDoc} */
        @Override
        public Object eval(ScriptContext context) throws ScriptException {
            // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - Script Execution)
            context.setAttribute(CONTEXT_KEY, context, ScriptContext.ENGINE_SCOPE);
            try {
                JexlContext ctxt = new JexlContextWrapper(context);
                return script.execute(ctxt);
            } catch (Exception e) {
                throw new ScriptException(e.toString());
            }
        }
        
        /** {@inheritDoc} */
        @Override
        public ScriptEngine getEngine() {
            return JexlScriptEngine.this;
        }
    }


}
