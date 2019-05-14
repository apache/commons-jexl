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

package org.apache.commons.jexl3;

import org.apache.commons.jexl3.introspection.JexlUberspect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.MathContext;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Creates and evaluates JexlExpression and JexlScript objects.
 * Determines the behavior of expressions and scripts during their evaluation with respect to:
 * <ul>
 * <li>Introspection, see {@link JexlUberspect}</li>
 * <li>Arithmetic and comparison, see {@link JexlArithmetic}</li>
 * <li>Error reporting</li>
 * <li>Logging</li>
 * </ul>
 *
 * <p>Note that methods that evaluate expressions may throw <em>unchecked</em> exceptions;
 * The {@link JexlException} are thrown in "non-silent" mode but since these are
 * RuntimeException, user-code <em>should</em> catch them wherever most appropriate.</p>
 *
 * @since 2.0
 */
public abstract class JexlEngine {

    /** A marker singleton for invocation failures in tryInvoke. */
    public static final Object TRY_FAILED = new FailObject();

    /** The failure marker class. */
    private static final class FailObject {
        /**
         * Default ctor.
         */
        private FailObject() {}

        @Override
        public String toString() {
            return "tryExecute failed";
        }
    };

    /**
     * The thread local context.
     */
    protected static final java.lang.ThreadLocal<JexlContext.ThreadLocal> CONTEXT =
                       new java.lang.ThreadLocal<JexlContext.ThreadLocal>();

    /**
     * Accesses the current thread local context.
     *
     * @return the context or null
     */
    public static JexlContext.ThreadLocal getThreadContext() {
        return CONTEXT.get();
    }

    /**
     * The thread local engine.
     */
    protected static final java.lang.ThreadLocal<JexlEngine> ENGINE =
                       new java.lang.ThreadLocal<JexlEngine>();

    /**
     * Accesses the current thread local engine.
     * <p>Advanced: you should only use this to retrieve the engine within a method/ctor called through the evaluation
     * of a script/expression.</p>
     * @return the engine or null
     */
    public static JexlEngine getThreadEngine() {
        return ENGINE.get();
    }

    /**
     * Sets the current thread local context.
     * <p>This should only be used carefully, for instance when re-evaluating a "stored" script that requires a
     * given Namespace resolver. Remember to synchronize access if context is shared between threads.
     *
     * @param tls the thread local context to set
     */
    public static void setThreadContext(JexlContext.ThreadLocal tls) {
        CONTEXT.set(tls);
    }

    /**
     * Script evaluation options.
     * <p>The JexlContext used for evaluation can implement this interface to alter behavior.</p>
     */
    public interface Options {

        /**
         * The charset used for parsing.
         *
         * @return the charset
         */
        Charset getCharset();
        /**
         * Sets whether the engine will throw a {@link JexlException} when an error is encountered during evaluation.
         *
         * @return true if silent, false otherwise
         */
        Boolean isSilent();

        /**
         * Checks whether the engine considers unknown variables, methods, functions and constructors as errors or
         * evaluates them as null.
         *
         * @return true if strict, false otherwise
         */
        Boolean isStrict();
        
        /**
         * Checks whether the arithmetic triggers errors during evaluation when null is used as an operand.
         *
         * @return true if strict, false otherwise
         */
        Boolean isStrictArithmetic();

        /**
         * Whether evaluation will throw JexlException.Cancel (true) or return null (false) when interrupted.
         * @return true when cancellable, false otherwise
         * @since 3.1
         */
        Boolean isCancellable();

        /**
         * The MathContext instance used for +,-,/,*,% operations on big decimals.
         *
         * @return the math context
         */
        MathContext getArithmeticMathContext();

        /**
         * The BigDecimal scale used for comparison and coercion operations.
         *
         * @return the scale
         */
        int getArithmeticMathScale();
    }

    /** Default features. */
    public static final JexlFeatures DEFAULT_FEATURES = new JexlFeatures();

    /**
     * An empty/static/non-mutable JexlContext singleton used instead of null context.
     */
    public static final JexlContext EMPTY_CONTEXT = new EmptyContext();

    /**
     * The empty context class, public for instrospection.
     */
    public static final class EmptyContext implements JexlContext {
        /**
         * Default ctor.
         */
        private EmptyContext() {}

        @Override
        public Object get(String name) {
            return null;
        }

        @Override
        public boolean has(String name) {
            return false;
        }

        @Override
        public void set(String name, Object value) {
            throw new UnsupportedOperationException("Not supported in void context.");
        }
    };

    /**
     * An empty/static/non-mutable JexlNamespace singleton used instead of null namespace.
     */
    public static final JexlContext.NamespaceResolver EMPTY_NS = new EmptyNamespaceResolver();

    /**
     * The  empty/static/non-mutable JexlNamespace class, public for instrospection.
     */
    public static final class EmptyNamespaceResolver implements JexlContext.NamespaceResolver {
        /**
         * Default ctor.
         */
        private EmptyNamespaceResolver() {}

        @Override
        public Object resolveNamespace(String name) {
            return null;
        }
    };

    /** The default Jxlt cache size. */
    private static final int JXLT_CACHE_SIZE = 256;

    /**
     * Gets the charset used for parsing.
     *
     * @return the charset
     */
    public abstract Charset getCharset();

    /**
     * Gets this engine underlying {@link JexlUberspect}.
     *
     * @return the uberspect
     */
    public abstract JexlUberspect getUberspect();

    /**
     * Gets this engine underlying {@link JexlArithmetic}.
     *
     * @return the arithmetic
     */
    public abstract JexlArithmetic getArithmetic();

    /**
     * Checks whether this engine is in debug mode.
     *
     * @return true if debug is on, false otherwise
     */
    public abstract boolean isDebug();

    /**
     * Checks whether this engine throws JexlException during evaluation.
     *
     * @return true if silent, false (default) otherwise
     */
    public abstract boolean isSilent();

    /**
     * Checks whether this engine considers unknown variables, methods, functions and constructors as errors.
     *
     * @return true if strict, false otherwise
     */
    public abstract boolean isStrict();

    /**
     * Checks whether this engine will throw JexlException.Cancel (true) or return null (false) when interrupted
     * during an execution.
     *
     * @return true if cancellable, false otherwise
     */
    public abstract boolean isCancellable();

    /**
     * Sets the class loader used to discover classes in 'new' expressions.
     * <p>This method is <em>not</em> thread safe; it may be called after JexlEngine
     * initialization and allow scripts to use new classes definitions.</p>
     *
     * @param loader the class loader to use
     */
    public abstract void setClassLoader(ClassLoader loader);

    /**
     * Creates a new {@link JxltEngine} instance using this engine.
     *
     * @return a JEXL Template engine
     */
    public JxltEngine createJxltEngine() {
        return createJxltEngine(true);
    }

    /**
     * Creates a new {@link JxltEngine} instance using this engine.
     *
     * @param noScript  whether the JxltEngine only allows Jexl expressions or scripts
     * @return a JEXL Template engine
     */
    public JxltEngine createJxltEngine(boolean noScript) {
        return createJxltEngine(noScript, JXLT_CACHE_SIZE, '$', '#');
    }

    /**
     * Creates a new instance of {@link JxltEngine} using this engine.
     *
     * @param noScript  whether the JxltEngine only allows JEXL expressions or scripts
     * @param cacheSize the number of expressions in this cache, default is 256
     * @param immediate the immediate template expression character, default is '$'
     * @param deferred  the deferred template expression character, default is '#'
     * @return a JEXL Template engine
     */
    public abstract JxltEngine createJxltEngine(boolean noScript, int cacheSize, char immediate, char deferred);

    /**
     * Clears the expression cache.
     */
    public abstract void clearCache();

    /**
     * Creates an JexlExpression from a String containing valid JEXL syntax.
     * This method parses the expression which must contain either a reference or an expression.
     *
     * @param info       An info structure to carry debugging information if needed
     * @param expression A String containing valid JEXL syntax
     * @return An {@link JexlExpression} which can be evaluated using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script
     */
    public abstract JexlExpression createExpression(JexlInfo info, String expression);

    /**
     * Creates a JexlExpression from a String containing valid JEXL syntax.
     * This method parses the expression which must contain either a reference or an expression.
     *
     * @param expression A String containing valid JEXL syntax
     * @return An {@link JexlExpression} which can be evaluated using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script
     */
    public final JexlExpression createExpression(String expression) {
        return createExpression(null, expression);
    }
    /**
     * Creates a JexlScript from a String containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param features A set of features that will be enforced during parsing
     * @param info   An info structure to carry debugging information if needed
     * @param source A string containing valid JEXL syntax
     * @param names  The script parameter names used during parsing; a corresponding array of arguments containing
     * values should be used during evaluation
     * @return A {@link JexlScript} which can be executed using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script
     */
    public abstract JexlScript createScript(JexlFeatures features, JexlInfo info, String source, String[] names);

    /**
     * Creates a JexlScript from a String containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param info   An info structure to carry debugging information if needed
     * @param source A string containing valid JEXL syntax
     * @param names  The script parameter names used during parsing; a corresponding array of arguments containing
     * values should be used during evaluation
     * @return A {@link JexlScript} which can be executed using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script
     */
    public final JexlScript createScript(JexlInfo info, String source, String[] names) {
        return createScript(null, info, source, names);
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @return A {@link JexlScript} which can be executed using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script.
     */
    public final JexlScript createScript(String scriptText) {
        return createScript(null, null, scriptText, null);
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @param names      The script parameter names used during parsing; a corresponding array of arguments containing
     * values should be used during evaluation
     * @return A {@link JexlScript} which can be executed using a {@link JexlContext}
     * @throws JexlException if there is a problem parsing the script
     */
    public final JexlScript createScript(String scriptText, String... names) {
        return createScript(null, null, scriptText, names);
    }

    /**
     * Creates a Script from a {@link File} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptFile A {@link File} containing valid JEXL syntax. Must not be null. Must be a readable file.
     * @return A {@link JexlScript} which can be executed with a {@link JexlContext}.
     * @throws JexlException if there is a problem reading or parsing the script.
     */
    public final JexlScript createScript(File scriptFile) {
        return createScript(null, null, readSource(scriptFile), null);
    }

    /**
     * Creates a Script from a {@link File} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptFile A {@link File} containing valid JEXL syntax. Must not be null. Must be a readable file.
     * @param names      The script parameter names used during parsing; a corresponding array of arguments containing
     * values should be used during evaluation.
     * @return A {@link JexlScript} which can be executed with a {@link JexlContext}.
     * @throws JexlException if there is a problem reading or parsing the script.
     */
    public final JexlScript createScript(File scriptFile, String... names) {
        return createScript(null, null, readSource(scriptFile), names);
    }

    /**
     * Creates a Script from a {@link File} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param info       An info structure to carry debugging information if needed
     * @param scriptFile A {@link File} containing valid JEXL syntax. Must not be null. Must be a readable file.
     * @param names      The script parameter names used during parsing; a corresponding array of arguments containing
     * values should be used during evaluation.
     * @return A {@link JexlScript} which can be executed with a {@link JexlContext}.
     * @throws JexlException if there is a problem reading or parsing the script.
     */
    public final JexlScript createScript(JexlInfo info, File scriptFile, String[] names) {
        return createScript(null, info, readSource(scriptFile), names);
    }

    /**
     * Creates a Script from a {@link URL} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptUrl A {@link URL} containing valid JEXL syntax. Must not be null.
     * @return A {@link JexlScript} which can be executed with a {@link JexlContext}.
     * @throws JexlException if there is a problem reading or parsing the script.
     */
    public final JexlScript createScript(URL scriptUrl) {
        return createScript(null, readSource(scriptUrl), null);
    }

    /**
     * Creates a Script from a {@link URL} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptUrl A {@link URL} containing valid JEXL syntax. Must not be null.
     * @param names     The script parameter names used during parsing; a corresponding array of arguments containing
     * values should be used during evaluation.
     * @return A {@link JexlScript} which can be executed with a {@link JexlContext}.
     * @throws JexlException if there is a problem reading or parsing the script.
     */
    public final JexlScript createScript(URL scriptUrl, String[] names) {
        return createScript(null, null, readSource(scriptUrl), names);
    }

    /**
     * Creates a Script from a {@link URL} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param info      An info structure to carry debugging information if needed
     * @param scriptUrl A {@link URL} containing valid JEXL syntax. Must not be null.
     * @param names     The script parameter names used during parsing; a corresponding array of arguments containing
     * values should be used during evaluation.
     * @return A {@link JexlScript} which can be executed with a {@link JexlContext}.
     * @throws JexlException if there is a problem reading or parsing the script.
     */
    public final JexlScript createScript(JexlInfo info, URL scriptUrl, String[] names) {
        return createScript(null, info, readSource(scriptUrl), names);
    }

    /**
     * Accesses properties of a bean using an expression.
     * <p>
     * jexl.get(myobject, "foo.bar"); should equate to
     * myobject.getFoo().getBar(); (or myobject.getFoo().get("bar"))
     * </p>
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     *
     * @param bean the bean to get properties from
     * @param expr the property expression
     * @return the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public abstract Object getProperty(Object bean, String expr);

    /**
     * Accesses properties of a bean using an expression.
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     *
     * @param context the evaluation context
     * @param bean    the bean to get properties from
     * @param expr    the property expression
     * @return the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public abstract Object getProperty(JexlContext context, Object bean, String expr);

    /**
     * Assign properties of a bean using an expression.
     * <p>
     * jexl.set(myobject, "foo.bar", 10); should equate to
     * myobject.getFoo().setBar(10); (or myobject.getFoo().put("bar", 10) )
     * </p>
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     *
     * @param bean  the bean to set properties in
     * @param expr  the property expression
     * @param value the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public abstract void setProperty(Object bean, String expr, Object value);

    /**
     * Assign properties of a bean using an expression. <p> If the JEXL engine is silent, errors will be logged through
     * its logger as warning. </p>
     *
     * @param context the evaluation context
     * @param bean    the bean to set properties in
     * @param expr    the property expression
     * @param value   the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public abstract void setProperty(JexlContext context, Object bean, String expr, Object value);

    /**
     * Invokes an object's method by name and arguments.
     *
     * @param obj  the method's invoker object
     * @param meth the method's name
     * @param args the method's arguments
     * @return the method returned value or null if it failed and engine is silent
     * @throws JexlException if method could not be found or failed and engine is not silent
     */
    public abstract Object invokeMethod(Object obj, String meth, Object... args);

    /**
     * Creates a new instance of an object using the most appropriate constructor based on the arguments.
     *
     * @param <T>   the type of object
     * @param clazz the class to instantiate
     * @param args  the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    public abstract <T> T newInstance(Class<? extends T> clazz, Object... args);

    /**
     * Creates a new instance of an object using the most appropriate constructor based on the arguments.
     *
     * @param clazz the name of the class to instantiate resolved through this engine's class loader
     * @param args  the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    public abstract Object newInstance(String clazz, Object... args);

    /**
     * Creates a JexlInfo instance.
     *
     * @param fn url/file/template/script user given name
     * @param l  line number
     * @param c  column number
     * @return a JexlInfo instance
     */
    public JexlInfo createInfo(String fn, int l, int c) {
        return new JexlInfo(fn, l, c);
    }

    /**
     * Create an information structure for dynamic set/get/invoke/new.
     * <p>This gathers the class, method and line number of the first calling method
     * outside of o.a.c.jexl3.</p>
     *
     * @return a JexlInfo instance
     */
    public JexlInfo createInfo() {
        return new JexlInfo();
    }

    /**
     * Creates a string from a reader.
     *
     * @param reader to be read.
     * @return the contents of the reader as a String.
     * @throws IOException on any error reading the reader.
     */
    protected static String toString(BufferedReader reader) throws IOException {
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append('\n');
        }
        return buffer.toString();
    }

    /**
     * Reads a JEXL source from a File.
     *
     * @param file the script file
     * @return the source
     */
    protected String readSource(final File file) {
        if (file == null) {
            throw new NullPointerException("source file is null");
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), getCharset()));
            return toString(reader);
        } catch (IOException xio) {
            throw new JexlException(createInfo(file.toString(), 1, 1), "could not read source File", xio);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException xignore) {
                    // cant do much
                }
            }
        }
    }

    /**
     * Reads a JEXL source from an URL.
     *
     * @param url the script url
     * @return the source
     */
    protected String readSource(final URL url) {
        if (url == null) {
            throw new NullPointerException("source URL is null");
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(url.openStream(), getCharset()));
            return toString(reader);
        } catch (IOException xio) {
            throw new JexlException(createInfo(url.toString(), 1, 1), "could not read source URL", xio);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException xignore) {
                    // cant do much
                }
            }
        }
    }
}
