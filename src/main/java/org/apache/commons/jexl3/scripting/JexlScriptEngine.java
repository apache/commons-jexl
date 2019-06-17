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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements the JEXL ScriptEngine for JSF-223.
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
 * 
 * @since 2.0
 */
public class JexlScriptEngine extends AbstractScriptEngine implements Compilable {

    /** The logger. */
    private static final Log LOG = LogFactory.getLog(JexlScriptEngine.class);

    /** The shared expression cache size. */
    private static final int CACHE_SIZE = 512;

    /** Reserved key for context (mandated by JSR-223). */
    public static final String CONTEXT_KEY = "context";

    /** Reserved key for JexlScriptObject. */
    public static final String JEXL_OBJECT_KEY = "JEXL";

    /** The JexlScriptObject instance. */
    private final JexlScriptObject jexlObject;

    /** The factory which created this instance. */
    private final ScriptEngineFactory parentFactory;

    /** The JEXL EL engine. */
    private final JexlEngine jexlEngine;

    /**
     * Default constructor.
     *
     * <p>Only intended for use when not using a factory.
     * Sets the factory to {@link JexlScriptEngineFactory}.</p>
     */
    public JexlScriptEngine() {
        this(FactorySingletonHolder.DEFAULT_FACTORY);
    }

    /**
     * Implements engine and engine context properties for use by JEXL scripts.
     * Those properties are always bound to the default engine scope context.
     *
     * <p>The following properties are defined:</p>
     * 
     * <ul>
     *   <li>in - refers to the engine scope reader that defaults to reading System.err</li>
     *   <li>out - refers the engine scope writer that defaults to writing in System.out</li>
     *   <li>err - refers to the engine scope writer that defaults to writing in System.err</li>
     *   <li>logger - the JexlScriptEngine logger</li>
     *   <li>System - the System.class</li>
     * </ul>
     *
     * @since 2.0
     */
    public class JexlScriptObject {

        /**
         * Gives access to the underlying JEXL engine shared between all ScriptEngine instances.
         * <p>Although this allows to manipulate various engine flags (lenient, debug, cache...)
         * for <strong>all</strong> JexlScriptEngine instances, you probably should only do so
         * if you are in strict control and sole user of the JEXL scripting feature.</p>
         * 
         * @return the shared underlying JEXL engine
         */
        public JexlEngine getEngine() {
            return jexlEngine;
        }

        /**
         * Gives access to the engine scope output writer (defaults to System.out).
         * 
         * @return the engine output writer
         */
        public PrintWriter getOut() {
            final Writer out = context.getWriter();
            if (out instanceof PrintWriter) {
                return (PrintWriter) out;
            } else if (out != null) {
                return new PrintWriter(out, true);
            } else {
                return null;
            }
        }

        /**
         * Gives access to the engine scope error writer (defaults to System.err).
         * 
         * @return the engine error writer
         */
        public PrintWriter getErr() {
            final Writer error = context.getErrorWriter();
            if (error instanceof PrintWriter) {
                return (PrintWriter) error;
            } else if (error != null) {
                return new PrintWriter(error, true);
            } else {
                return null;
            }
        }

        /**
         * Gives access to the engine scope input reader (defaults to System.in).
         * 
         * @return the engine input reader
         */
        public Reader getIn() {
            return context.getReader();
        }

        /**
         * Gives access to System class.
         * 
         * @return System.class
         */
        public Class<System> getSystem() {
            return System.class;
        }

        /**
         * Gives access to the engine logger.
         * 
         * @return the JexlScriptEngine logger
         */
        public Log getLogger() {
            return LOG;
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
        jexlEngine = EngineSingletonHolder.DEFAULT_ENGINE;
        jexlObject = new JexlScriptObject();
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public Object eval(final Reader reader, final ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        if (reader == null || context == null) {
            throw new NullPointerException("script and context must be non-null");
        }
        return eval(readerToString(reader), context);
    }

    @Override
    public Object eval(final String script, final ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        if (script == null || context == null) {
            throw new NullPointerException("script and context must be non-null");
        }
        // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - JexlScript Execution)
        context.setAttribute(CONTEXT_KEY, context, ScriptContext.ENGINE_SCOPE);
        try {
            JexlScript jexlScript = jexlEngine.createScript(script);
            JexlContext ctxt = new JexlContextWrapper(context);
            return jexlScript.execute(ctxt);
        } catch (Exception e) {
            throw new ScriptException(e.toString());
        }
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return parentFactory;
    }

    @Override
    public CompiledScript compile(final String script) throws ScriptException {
        // This is mandated by JSR-223
        if (script == null) {
            throw new NullPointerException("script must be non-null");
        }
        try {
            JexlScript jexlScript = jexlEngine.createScript(script);
            return new JexlCompiledScript(jexlScript);
        } catch (Exception e) {
            throw new ScriptException(e.toString());
        }
    }

    @Override
    public CompiledScript compile(final Reader script) throws ScriptException {
        // This is mandated by JSR-223
        if (script == null) {
            throw new NullPointerException("script must be non-null");
        }
        return compile(readerToString(script));
    }

    /**
     * Read from a reader into a local buffer and return a String with
     * the contents of the reader.
     * 
     * @param scriptReader to be read.
     * @return the contents of the reader as a String.
     * @throws ScriptException on any error reading the reader.
     */
    private static String readerToString(Reader scriptReader) throws ScriptException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader;
        if (scriptReader instanceof BufferedReader) {
            reader = (BufferedReader) scriptReader;
        } else {
            reader = new BufferedReader(scriptReader);
        }
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            return buffer.toString();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }

    /**
     * Holds singleton JexlScriptEngineFactory (IODH).
     */
    private static class FactorySingletonHolder {
        /** non instantiable. */
        private FactorySingletonHolder() {}

        /** The engine factory singleton instance. */
        private static final JexlScriptEngineFactory DEFAULT_FACTORY = new JexlScriptEngineFactory();
    }

    /**
     * Holds singleton JexlScriptEngine (IODH).
     * <p>A single JEXL engine and JexlUberspect is shared by all instances of JexlScriptEngine.</p>
     */
    private static class EngineSingletonHolder {
        /** non instantiable. */
        private EngineSingletonHolder() {}

        /** The JEXL engine singleton instance. */
        private static final JexlEngine DEFAULT_ENGINE = new JexlBuilder().logger(LOG).cache(CACHE_SIZE).create();
    }

    /**
     * Wrapper to help convert a JSR-223 ScriptContext into a JexlContext.
     *
     * Current implementation only gives access to ENGINE_SCOPE binding.
     */
    private final class JexlContextWrapper implements JexlContext {
        /** The wrapped script context. */
        private final ScriptContext scriptContext;

        /**
         * Creates a context wrapper.
         * 
         * @param theContext the engine context.
         */
        private JexlContextWrapper (final ScriptContext theContext){
            scriptContext = theContext;
        }

        @Override
        public Object get(final String name) {
            final Object o = scriptContext.getAttribute(name);
            if (JEXL_OBJECT_KEY.equals(name)) {
                if (o != null) {
                    LOG.warn("JEXL is a reserved variable name, user defined value is ignored");
                }
                return jexlObject;
            }
            return o;
        }

        @Override
        public void set(final String name, final Object value) {
            int scope = scriptContext.getAttributesScope(name);
            if (scope == -1) { // not found, default to engine
                scope = ScriptContext.ENGINE_SCOPE;
            }
            scriptContext.getBindings(scope).put(name , value);
        }

        @Override
        public boolean has(final String name) {
            Bindings bnd = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.containsKey(name);
        }

    }

    /**
     * Wrapper to help convert a JEXL JexlScript into a JSR-223 CompiledScript.
     */
    private final class JexlCompiledScript extends CompiledScript {
        /** The underlying JEXL expression instance. */
        private final JexlScript script;

        /**
         * Creates an instance.
         * 
         * @param theScript to wrap
         */
        private JexlCompiledScript(final JexlScript theScript) {
            script = theScript;
        }

        @Override
        public String toString() {
            return script.getSourceText();
        }

        @Override
        public Object eval(final ScriptContext context) throws ScriptException {
            // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - JexlScript Execution)
            context.setAttribute(CONTEXT_KEY, context, ScriptContext.ENGINE_SCOPE);
            try {
                JexlContext ctxt = new JexlContextWrapper(context);
                return script.execute(ctxt);
            } catch (Exception e) {
                throw new ScriptException(e.toString());
            }
        }

        @Override
        public ScriptEngine getEngine() {
            return JexlScriptEngine.this;
        }
    }
}
