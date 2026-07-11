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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.introspection.JexlPermissions;
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
 * <a href="https://java.sun.com/javase/6/docs/api/javax/script/package-summary.html">Java Scripting API</a>
 * Javadoc.
 *
 * @since 2.0
 */
public class JexlScriptEngine extends AbstractScriptEngine implements Compilable {
    /**
     * A factory that shares the JexlEngine instance between all JexlScriptEngine instances it creates.
     * <p>All JexlScriptEngine instances created by this factory share the same JexlEngine instance and JexlUberspect instance.</p>
     * <p>To create a JexlScriptEngine with a different JexlEngine instance,
     * use the {@link JexlScriptEngine#JexlScriptEngine(JexlScriptEngineFactory)} constructor.</p>
     * @since 3.3
     */
    public static class Factory extends JexlScriptEngineFactory {
        /**
         * The shared engine instance.
         * <p>A single JEXL engine and JexlUberspect is shared by all instances of JexlScriptEngine
         * created by this factory.</p>
         */
        private volatile JexlEngine jexl;

        /** Default constructor. */
        public Factory() {
            this(null);
        }

        /**
         * For specialization.
         * @param permissions The permissions to use for the engine
         */
        public Factory(final JexlPermissions permissions) {
            super(permissions);
        }

        @Override
        protected JexlEngine getEngine() {
            JexlEngine engine = jexl;
            if (engine == null) {
                synchronized (this) {
                    engine = jexl;
                    if (engine == null) {
                        engine = jexl = createJexlEngine();
                    }
                }
            }
            return engine;
        }

        /**
         * Sets the shared engine instance.
         * @param engine The engine
         */
        void setEngine(final JexlEngine engine) {
            jexl = engine;
        }
    }

    /**
     * Holds singleton JexlScriptEngineFactory (IODH).
     */
    private static final class FactorySingletonHolder {

        /** The engine factory singleton instance. */
        static final Factory DEFAULT_FACTORY = new Factory();

        /** Non instantiable. */
        private FactorySingletonHolder() {}
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
        JexlCompiledScript(final JexlScript theScript) {
            script = theScript;
        }

        @Override
        public Object eval(final ScriptContext context) throws ScriptException {
            // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - JexlScript Execution)
            context.setAttribute(CONTEXT_KEY, context, ScriptContext.ENGINE_SCOPE);
            try {
                final JexlContext ctxt = new JexlContextWrapper(context);
                return script.execute(ctxt);
            } catch (final Exception e) {
                throw scriptException(e);
            }
        }

        @Override
        public ScriptEngine getEngine() {
            return JexlScriptEngine.this;
        }

        @Override
        public String toString() {
            return script.getSourceText();
        }
    }

    /**
     * Wrapper to help convert a JSR-223 ScriptContext into a JexlContext.
     * <p>The current implementation only gives access to ENGINE_SCOPE binding.</p>
     */
    private final class JexlContextWrapper implements JexlContext {

        /** The wrapped script context. */
        final ScriptContext scriptContext;

        /**
         * Creates a context wrapper.
         *
         * @param theContext The engine context.
         */
        JexlContextWrapper (final ScriptContext theContext){
            scriptContext = theContext;
        }

        @Override
        public Object get(final String name) {
            final Object o = scriptContext.getAttribute(name);
            if (JEXL_OBJECT_KEY.equals(name)) {
                if (o != null) {
                    LOG.warn("JEXL is a reserved variable name, user-defined value is ignored");
                }
                return jexlObject;
            }
            return o;
        }

        @Override
        public boolean has(final String name) {
            final Bindings bnd = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
            return bnd.containsKey(name);
        }

        @Override
        public void set(final String name, final Object value) {
            int scope = scriptContext.getAttributesScope(name);
            if (scope == -1) { // not found, default to engine
                scope = ScriptContext.ENGINE_SCOPE;
            }
            scriptContext.getBindings(scope).put(name , value);
        }

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

        /** Default constructor. */
        public JexlScriptObject() {
            // Keep Javadoc happy
        }

        /**
         * Gives access to the underlying JEXL engine shared between all ScriptEngine instances.
         * <p>Although this allows to manipulate various engine flags (lenient, debug, cache...)
         * for <strong>all</strong> JexlScriptEngine instances, you probably should only do so
         * if you are in strict control and sole user of the JEXL scripting feature.</p>
         *
         * @return The shared underlying JEXL engine
         */
        public JexlEngine getEngine() {
            return jexlEngine;
        }

        /**
         * Gives access to the engine scope error writer (defaults to System.err).
         *
         * @return The engine error writer
         */
        public PrintWriter getErr() {
            final Writer error = context.getErrorWriter();
            if (error instanceof PrintWriter) {
                return (PrintWriter) error;
            }
            if (error != null) {
                return new PrintWriter(error, true);
            }
            return null;
        }

        /**
         * Gives access to the engine scope input reader (defaults to System.in).
         *
         * @return The engine input reader
         */
        public Reader getIn() {
            return context.getReader();
        }

        /**
         * Gives access to the engine logger.
         *
         * @return The JexlScriptEngine logger
         */
        public Log getLogger() {
            return LOG;
        }

        /**
         * Gives access to the engine scope output writer (defaults to System.out).
         *
         * @return The engine output writer
         */
        public PrintWriter getOut() {
            final Writer out = context.getWriter();
            if (out instanceof PrintWriter) {
                return (PrintWriter) out;
            }
            if (out != null) {
                return new PrintWriter(out, true);
            }
            return null;
        }

        /**
         * Gives access to System class.
         *
         * @return System.class
         */
        public Class<System> getSystem() {
            return System.class;
        }
    }


    /** The logger. */
    static final Log LOG = LogFactory.getLog(JexlScriptEngine.class);

    /** The shared expression cache size. */
    static final int CACHE_SIZE = 512;

    /** Reserved key for context (mandated by JSR-223). */
    public static final String CONTEXT_KEY = "context";

    /** Reserved key for JexlScriptObject. */
    public static final String JEXL_OBJECT_KEY = "JEXL";

    /** Reserved key for script. */
    private static final String SCRIPT = "script";

    /**
     * Reads from a reader into a local buffer and return a String with
     * the contents of the reader.
     *
     * @param scriptReader to be read.
     * @return The contents of the reader as a String.
     * @throws ScriptException on any error reading the reader.
     */
    private static String readerToString(final Reader scriptReader) throws ScriptException {
        final StringBuilder buffer = new StringBuilder();
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
        } catch (final IOException e) {
            throw new ScriptException(e);
        }
    }

    static ScriptException scriptException(final Exception e) {
        Exception xany = e;
        // unwrap a jexl exception
        if (xany instanceof JexlException) {
            final Throwable cause = xany.getCause();
            if (cause instanceof Exception) {
                xany = (Exception) cause;
            }
        }
        return new ScriptException(xany);
    }

    /**
     * Sets the shared instance used for the script engine in the default factory.
     * <p>This should be called early enough to have an effect, ie before any
     * {@link javax.script.ScriptEngineManager} features.</p>
     * <p>To restore 3.2 script behavior:</p>
     * {@code
     *         JexlScriptEngine.setInstance(new JexlBuilder()
     *                 .cache(512)
     *                 .logger(LogFactory.getLog(JexlScriptEngine.class))
     *                 .permissions(JexlPermissions.UNRESTRICTED)
     *                 .create());
     * }
     *
     * @param engine The JexlEngine instance to use
     * @since 3.3
     */
    public static void setInstance(final JexlEngine engine) {
        FactorySingletonHolder.DEFAULT_FACTORY.setEngine(engine);
    }

    /**
     * Sets the permissions instance used to create the script engine.
     * <p>This method has been considered unsafe and is no longer supported.
     * Use {@link JexlScriptEngineFactory#setDefaultPermissions(JexlPermissions)} during initialization
     * - <em>before</em> requesting an engine - to achieve the intended permission injection.</p>
     * @deprecated 3.6.3
     * @param permissions unused, method will throw
     */
    @Deprecated
    public static void setPermissions(final JexlPermissions permissions) {
        throw new UnsupportedOperationException("JexlScriptEngine.setPermissions is unsafe and no longer supported");
    }

    /** The JexlScriptObject instance. */
    final JexlScriptObject jexlObject;

    /** The factory which created this instance. */
    final JexlScriptEngineFactory parentFactory;

    /** The JEXL EL engine. */
    final JexlEngine jexlEngine;

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
     * JSR-223 compatibility constructor.
     * @param scriptEngineFactory The factory which must be a {@link JexlScriptEngineFactory}
     */
    public JexlScriptEngine(final ScriptEngineFactory scriptEngineFactory) {
        this((JexlScriptEngineFactory) scriptEngineFactory);
    }

    /**
     * Create a scripting engine using the supplied factory.
     *
     * @param scriptEngineFactory The factory which creates this instance.
     * @throws NullPointerException if factory is null
     */
    public JexlScriptEngine(final JexlScriptEngineFactory scriptEngineFactory) {
        Objects.requireNonNull(scriptEngineFactory, "scriptEngineFactory");
        parentFactory = scriptEngineFactory;
        jexlEngine = scriptEngineFactory.getEngine();
        jexlObject = new JexlScriptObject();
    }

    @Override
    public CompiledScript compile(final Reader script) throws ScriptException {
        // This is mandated by JSR-223
        Objects.requireNonNull(script, SCRIPT);
        return compile(readerToString(script));
    }

    @Override
    public CompiledScript compile(final String script) throws ScriptException {
        // This is mandated by JSR-223
        Objects.requireNonNull(script, SCRIPT);
        try {
            final JexlScript jexlScript = jexlEngine.createScript(script);
            return new JexlCompiledScript(jexlScript);
        } catch (final Exception e) {
            throw scriptException(e);
        }
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public Object eval(final Reader reader, final ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(context, CONTEXT_KEY);
        return eval(readerToString(reader), context);
    }

    @Override
    public Object eval(final String script, final ScriptContext context) throws ScriptException {
        // This is mandated by JSR-223 (see SCR.5.5.2   Methods)
        Objects.requireNonNull(script, SCRIPT);
        Objects.requireNonNull(context, CONTEXT_KEY);
        // This is mandated by JSR-223 (end of section SCR.4.3.4.1.2 - JexlScript Execution)
        context.setAttribute(CONTEXT_KEY, context, ScriptContext.ENGINE_SCOPE);
        try {
            final JexlScript jexlScript = jexlEngine.createScript(script);
            final JexlContext ctxt = new JexlContextWrapper(context);
            return jexlScript.execute(ctxt);
        } catch (final Exception e) {
            throw scriptException(e);
        }
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return parentFactory;
    }
}
