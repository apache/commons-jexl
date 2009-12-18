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
package org.apache.commons.jexl2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.jexl2.parser.ParseException;
import org.apache.commons.jexl2.parser.Parser;
import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.commons.jexl2.parser.TokenMgrError;
import org.apache.commons.jexl2.parser.ASTJexlScript;

import org.apache.commons.jexl2.introspection.Uberspect;
import org.apache.commons.jexl2.introspection.UberspectImpl;
import org.apache.commons.jexl2.introspection.JexlMethod;

/**
 * <p>
 * Creates and evaluates Expression and Script objects.
 * Determines the behavior of Expressions & Scripts during their evaluation with respect to:
 * <ul>
 *  <li>Introspection, see {@link Uberspect}</li>
 *  <li>Arithmetic & comparison, see {@link JexlArithmetic}</li>
 *  <li>Error reporting</li>
 *  <li>Logging</li>
 * </ul>
 * </p>
 * <p>The <code>setSilent</code>and<code>setLenient</code> methods allow to fine-tune an engine instance behavior
 * according to various error control needs.
 * </p>
 * <ul>
 * <li>When "silent" & "lenient" (not-strict):
 * <p> 0 & null should be indicators of "default" values so that even in an case of error,
 * something meaningfull can still be inferred; may be convenient for configurations.
 * </p>
 * </li>
 * <li>When "silent" & "strict":
 * <p>One should probably consider using null as an error case - ie, every object
 * manipulated by JEXL should be valued; the ternary operator, especially the '?:' form
 * can be used to workaround exceptional cases.
 * Use case could be configuration with no implicit values or defaults.
 * </p>
 * </li>
 * <li>When "not-silent" & "not-strict":
 * <p>The error control grain is roughly on par with JEXL 1.0</p>
 * </li>
 * <li>When "not-silent" & "strict":
 * <p>The finest error control grain is obtained; it is the closest to Java code -
 * still augmented by "script" capabilities regarding automated conversions & type matching.
 * </p>
 * </li>
 * </ul>
 * <p>
 * Note that methods that evaluate expressions may throw <em>unchecked</em> exceptions;
 * The {@link JexlException} are thrown in "non-silent" mode but since these are
 * RuntimeException, user-code <em>should</em> catch them wherever most appropriate.
 * </p>
 * @since 2.0
 */
public class JexlEngine {    
    /**
     * An empty/static/non-mutable JexlContext used instead of null context.
     */
    public static final JexlContext EMPTY_CONTEXT = new JexlContext() {
        /** {@inheritDoc} */
        public Object get(String name) {
            return null;
        }
        /** {@inheritDoc} */
        public boolean has(String name) {
            return false;
        }
        /** {@inheritDoc} */
        public void set(String name, Object value) {
            throw new UnsupportedOperationException("Not supported in void context.");
        }
    };

    /**
     *  Gets the default instance of Uberspect.
     * <p>This is lazily initialized to avoid building a default instance if there
     * is no use for it. The main reason for not using the default Uberspect instance is to
     * be able to use a (low level) introspector created with a given logger
     * instead of the default one.</p>
     * <p>Implemented as on demand holder idiom.</p>
     */
    private static final class UberspectHolder {
        /** The default uberspector that handles all introspection patterns. */
        private static final Uberspect UBERSPECT = new UberspectImpl(LogFactory.getLog(JexlEngine.class), false);
        /** Non-instantiable. */
        private UberspectHolder() {}
    }
    
    /**
     * The Uberspect instance.
     */
    protected final Uberspect uberspect;
    /**
     * The JexlArithmetic instance.
     */
    protected final JexlArithmetic arithmetic;
    /**
     * The Log to which all JexlEngine messages will be logged.
     */
    protected final Log logger;
    /**
     * The singleton ExpressionFactory also holds a single instance of
     * {@link Parser}.
     * When parsing expressions, ExpressionFactory synchronizes on Parser.
     */
    protected final Parser parser = new Parser(new StringReader(";")); //$NON-NLS-1$
    /**
     * Whether expressions evaluated by this engine will throw exceptions (false) or 
     * return null (true). Default is false.
     */
    protected boolean silent = false;
    /**
     * Whether error messages will carry debugging information.
     */
    protected boolean debug = true;
    /**
     *  The map of 'prefix:function' to object implementing the function.
     */
    protected Map<String, Object> functions = Collections.emptyMap();
    /**
     * The expression cache.
     */
    protected SoftCache<String, ASTJexlScript> cache = null;
    /**
     * The default cache load factor.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * Creates an engine with default arguments.
     */
    public JexlEngine() {
        this(null, null, null, null);
    }

    /**
     * Creates a JEXL engine using the provided {@link Uberspect}, (@link JexlArithmetic),
     * a function map and logger.
     * @param anUberspect to allow different introspection behaviour
     * @param anArithmetic to allow different arithmetic behaviour
     * @param theFunctions an optional map of functions (@link setFunctions)
     * @param log the logger for various messages
     */
    public JexlEngine(Uberspect anUberspect, JexlArithmetic anArithmetic, Map<String, Object> theFunctions, Log log) {
        this.uberspect = anUberspect == null ? getUberspect(log) : anUberspect;
        if (log == null) {
            log = LogFactory.getLog(JexlEngine.class);
        }
        this.logger = log;
        this.arithmetic = anArithmetic == null ? new JexlArithmetic(true) : anArithmetic;
        if (theFunctions != null) {
            this.functions = theFunctions;
        }
    }


    /**
     *  Gets the default instance of Uberspect.
     * <p>This is lazily initialized to avoid building a default instance if there
     * is no use for it. The main reason for not using the default Uberspect instance is to
     * be able to use a (low level) introspector created with a given logger
     * instead of the default one.</p>
     * @param logger the logger to use for the underlying Uberspect
     * @return Uberspect the default uberspector instance.
     */
    public static Uberspect getUberspect(Log logger) {
        if (logger == null || logger.equals(LogFactory.getLog(JexlEngine.class))) {
            return UberspectHolder.UBERSPECT;
        }
        return new UberspectImpl(logger, false);
    }

    /**
     * Gets this engine underlying uberspect.
     * @return the uberspect
     */
    public Uberspect getUberspect() {
        return uberspect;
    }

    /**
     * Sets whether this engine reports debugging information when error occurs.
     * <p>This method is <em>not</em> thread safe; it should be called as an optional step of the JexlEngine
     * initialization code before expression creation &amp; evaluation.</p>
     * @see JexlEngine#setSilent
     * @see JexlEngine#setLenient
     * @param flag true implies debug is on, false implies debug is off.
     */
    public void setDebug(boolean flag) {
        this.debug = flag;
    }

    /**
     * Checks whether this engine is in debug mode.
     * @return true if debug is on, false otherwise
     */
    public boolean isDebug() {
        return this.debug;
    }

    /**
     * Sets whether this engine throws JexlException during evaluation when an error is triggered.
     * <p>This method is <em>not</em> thread safe; it should be called as an optional step of the JexlEngine
     * initialization code before expression creation &amp; evaluation.</p>
     * @see JexlEngine#setDebug
     * @see JexlEngine#setLenient
     * @param flag true means no JexlException will occur, false allows them
     */
    public void setSilent(boolean flag) {
        this.silent = flag;
    }

    /**
     * Checks whether this engine throws JexlException during evaluation.
     * @return true if silent, false (default) otherwise
     */
    public boolean isSilent() {
        return this.silent;
    }

    /**
     * Sets whether this engine triggers errors during evaluation when null is used as
     * an operand.
     * <p>This method is <em>not</em> thread safe; it should be called as an optional step of the JexlEngine
     * initialization code before expression creation &amp; evaluation.</p>
     * @see JexlEngine#setSilent
     * @see JexlEngine#setDebug
     * @param flag true means no JexlException will occur, false allows them
     */
    public void setLenient(boolean flag) {
        this.arithmetic.setLenient(flag);
    }

    /**
     * Checks whether this engine triggers errors during evaluation when null is used as
     * an operand.
     * @return true if lenient, false if strict
     */
    public boolean isLenient() {
        return this.arithmetic.isLenient();
    }

    /**
     * Sets the class loader used to discover classes in 'new' expressions.
     * <p>This method should be called as an optional step of the JexlEngine
     * initialization code before expression creation &amp; evaluation.</p>
     * @param loader the class loader to use
     */
    public void setClassLoader(ClassLoader loader) {
        uberspect.setClassLoader(loader);
    }

    /**
     * Sets a cache of the defined size for expressions.
     * @param size if not strictly positive, no cache is used.
     */
    public void setCache(int size) {
        // since the cache is only used during parse, use same sync object
        synchronized (parser) {
            if (size <= 0) {
                cache = null;
            } else if (cache == null || cache.size() != size) {
                cache = new SoftCache<String, ASTJexlScript>(size);
            }
        }
    }

    /**
     * Sets the map of function namespaces.
     * <p>
     * This method is <em>not</em> thread safe; it should be called as an optional step of the JexlEngine
     * initialization code before expression creation &amp; evaluation.
     * </p>
     * <p>
     * Each entry key is used as a prefix, each entry value used as a bean implementing
     * methods; an expression like 'nsx:method(123)' will thus be solved by looking at
     * a registered bean named 'nsx' that implements method 'method' in that map.
     * If all methods are static, you may use the bean class instead of an instance as value.
     * </p>
     * <p>
     * If the entry value is a class that has one contructor taking a JexlContext as argument, an instance
     * of the namespace will be created at evaluation time. It might be a good idea to derive a JexlContext
     * to carry the information used by the namespace to avoid variable space pollution and strongly type
     * the constructor with this specialized JexlContext.
     * </p>
     * <p>
     * The key or prefix allows to retrieve the bean that plays the role of the namespace.
     * If the prefix is null, the namespace is the top-level namespace allowing to define
     * top-level user defined functions ( ie: myfunc(...) )
     * </p>
     * @param funcs the map of functions that should not mutate after the call; if null
     * is passed, the empty collection is used.
     */
    public void setFunctions(Map<String, Object> funcs) {
        functions = funcs != null ? funcs : Collections.<String, Object>emptyMap();
    }

    /**
     * Retrieves the map of function namespaces.
     *
     * @return the map passed in setFunctions or the empty map if the
     * original was null.
     */
    public Map<String, Object> getFunctions() {
        return functions;
    }

    /**
     * An overridable through covariant return Expression creator.
     * @param text the script text
     * @param tree the parse AST tree
     * @return the script instance
     */
    protected Expression createExpression(ASTJexlScript tree, String text) {
        return new ExpressionImpl(this, text, tree);
    }
    
    /**
     * Creates an Expression from a String containing valid
     * JEXL syntax.  This method parses the expression which
     * must contain either a reference or an expression.
     * @param expression A String containing valid JEXL syntax
     * @return An Expression object which can be evaluated with a JexlContext
     * @throws JexlException An exception can be thrown if there is a problem
     *      parsing this expression, or if the expression is neither an
     *      expression nor a reference.
     */
    public Expression createExpression(String expression) {
        return createExpression(expression, null);
    }

    /**
     * Creates an Expression from a String containing valid
     * JEXL syntax.  This method parses the expression which
     * must contain either a reference or an expression.
     * @param expression A String containing valid JEXL syntax
     * @return An Expression object which can be evaluated with a JexlContext
     * @param info An info structure to carry debugging information if needed
     * @throws JexlException An exception can be thrown if there is a problem
     *      parsing this expression, or if the expression is neither an
     *      expression or a reference.
     */
    public Expression createExpression(String expression, JexlInfo info) {
        // Parse the expression
        ASTJexlScript tree = parse(expression, info);
        if (tree.jjtGetNumChildren() > 1) {
            logger.warn("The JEXL Expression created will be a reference"
                      + " to the first expression from the supplied script: \"" + expression + "\" ");
        }
        return createExpression(tree, expression);
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @return A {@link Script} which can be executed using a {@link JexlContext}.
     * @throws JexlException if there is a problem parsing the script.
     */
    public Script createScript(String scriptText) {
        return createScript(scriptText, null);
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @param info An info structure to carry debugging information if needed
     * @return A {@link Script} which can be executed using a {@link JexlContext}.
     * @throws JexlException if there is a problem parsing the script.
     */
    public Script createScript(String scriptText, JexlInfo info) {
        if (scriptText == null) {
            throw new NullPointerException("scriptText is null");
        }
        // Parse the expression
        ASTJexlScript tree = parse(scriptText, info);
        return createScript(tree, scriptText);
    }

    /**
     * An overridable through covariant return Script creator.
     * @param text the script text
     * @param tree the parse AST tree
     * @return the script instance
     */
    protected Script createScript(ASTJexlScript tree, String text) {
        return new ExpressionImpl(this, text, tree);
    }
    
    /**
     * Creates a Script from a {@link File} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptFile A {@link File} containing valid JEXL syntax.
     *      Must not be null. Must be a readable file.
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws IOException if there is a problem reading the script.
     * @throws JexlException if there is a problem parsing the script.
     */
    public Script createScript(File scriptFile) throws IOException {
        if (scriptFile == null) {
            throw new NullPointerException("scriptFile is null");
        }
        if (!scriptFile.canRead()) {
            throw new IOException("Can't read scriptFile (" + scriptFile.getCanonicalPath() + ")");
        }
        BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
        JexlInfo info = null;
        if (debug) {
            info = createInfo(scriptFile.getName(), 0, 0);
        }
        return createScript(readerToString(reader), info);
    }

    /**
     * Creates a Script from a {@link URL} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptUrl A {@link URL} containing valid JEXL syntax.
     *      Must not be null. Must be a readable file.
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws IOException if there is a problem reading the script.
     * @throws JexlException if there is a problem parsing the script.
     */
    public Script createScript(URL scriptUrl) throws IOException {
        if (scriptUrl == null) {
            throw new NullPointerException("scriptUrl is null");
        }
        URLConnection connection = scriptUrl.openConnection();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        JexlInfo info = null;
        if (debug) {
            info = createInfo(scriptUrl.toString(), 0, 0);
        }
        return createScript(readerToString(reader), info);
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
     * @param bean the bean to get properties from
     * @param expr the property expression
     * @return the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public Object getProperty(Object bean, String expr) {
        return getProperty(null, bean, expr);
    }

    /**
     * Accesses properties of a bean using an expression.
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     * @param context the evaluation context
     * @param bean the bean to get properties from
     * @param expr the property expression
     * @return the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public Object getProperty(JexlContext context, Object bean, String expr) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        // lets build 1 unique & unused identifiers wrt context
        String r0 = "$0";
        for (int s = 0; context.has(r0); ++s) {
            r0 = r0 + s;
        }
        expr = r0 + (expr.charAt(0) == '[' ? "" : ".") + expr + ";";
        try {
            JexlNode tree = parse(expr, null);
            JexlNode node = tree.jjtGetChild(0);
            Interpreter interpreter = createInterpreter(context);
            // ensure 4 objects in register array
            Object[] r = {r0, bean, r0, bean};
            interpreter.setRegisters(r);
            return node.jjtAccept(interpreter, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl;
        }
    }

    /**
     * Assign properties of a bean using an expression.
     * <p>
     * jexl.set(myobject, "foo.bar", 10); should equate to
     * myobject.getFoo().setBar(10); (or myobject.getFoo().put("bar", 10) )
     * </p>
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     * @param bean the bean to set properties in
     * @param expr the property expression
     * @param value the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public void setProperty(Object bean, String expr, Object value) {
        setProperty(null, bean, expr, value);
    }

    /**
     * Assign properties of a bean using an expression.
     * <p>
     * If the JEXL engine is silent, errors will be logged through its logger as warning.
     * </p>
     * @param context the evaluation context
     * @param bean the bean to set properties in
     * @param expr the property expression
     * @param value the value of the property
     * @throws JexlException if there is an error parsing the expression or during evaluation
     */
    public void setProperty(JexlContext context, Object bean, String expr, Object value) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        // lets build 2 unique & unused identifiers wrt context
        String r0 = "$0", r1 = "$1";
        for (int s = 0; context.has(r0); ++s) {
            r0 = r0 + s;
        }
        for (int s = 0; context.has(r1); ++s) {
            r1 = r1 + s;
        }
        // synthetize expr
        expr = r0 + (expr.charAt(0) == '[' ? "" : ".") + expr + "=" + r1 + ";";
        try {
            JexlNode tree = parse(expr, null);
            JexlNode node = tree.jjtGetChild(0);
            Interpreter interpreter = createInterpreter(context);
            // set the registers
            Object[] r = {r0, bean, r1, value};
            interpreter.setRegisters(r);
            node.jjtAccept(interpreter, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return;
            }
            throw xjexl;
        }
    }

    /**
     * Invokes an object's method by name and arguments.
     * @param obj the method's invoker object
     * @param meth the method's name
     * @param args the method's arguments
     * @return the method returned value or null if it failed and engine is silent
     * @throws JexlException if method could not be found or failed and engine is not silent
     */
    public Object invokeMethod(Object obj, String meth, Object... args) {
        JexlException xjexl = null;
        Object result = null;
        JexlInfo info = debugInfo();
        try {
            JexlMethod method = uberspect.getMethod(obj, meth, args, info);
            if (method == null && arithmetic.narrowArguments(args)) {
                method = uberspect.getMethod(obj, meth, args, info);
            }
            if (method != null) {
                result = method.invoke(obj, args);
            } else {
                xjexl = new JexlException(info, "failed finding method " + meth);
            }
        } catch (Exception xany) {
            xjexl = new JexlException(info, "failed executing method " + meth, xany);
        } finally {
            if (xjexl != null) {
                if (silent) {
                    logger.warn(xjexl.getMessage(), xjexl.getCause());
                    return null;
                }
                throw xjexl;
            }
        }
        return result;
    }

    /**
     * Creates a new instance of an object using the most appropriate constructor
     * based on the arguments.
     * @param <T> the type of object
     * @param clazz the class to instantiate
     * @param args the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    public <T> T newInstance(Class<? extends T> clazz, Object...args) {
        return clazz.cast(doCreateInstance(clazz, args));
    }

    /**
     * Creates a new instance of an object using the most appropriate constructor
     * based on the arguments.
     * @param clazz the name of the class to instantiate resolved through this engine's class loader
     * @param args the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    public Object newInstance(String clazz, Object...args) {
       return doCreateInstance(clazz, args);
    }

    /**
     * Creates a new instance of an object using the most appropriate constructor
     * based on the arguments.
     * @param clazz the class to instantiate
     * @param args the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    protected Object doCreateInstance(Object clazz, Object...args) {
        JexlException xjexl = null;
        Object result = null;
        JexlInfo info = debugInfo();
        try {
            Constructor<?> ctor = uberspect.getConstructor(clazz, args, info);
            if (ctor == null && arithmetic.narrowArguments(args)) {
                ctor = uberspect.getConstructor(clazz, args, info);
            }
            if (ctor != null) {
                result = ctor.newInstance(args);
            } else {
                xjexl = new JexlException(info, "failed finding constructor for " + clazz.toString());
            }
        } catch (Exception xany) {
            xjexl = new JexlException(info, "failed executing constructor for " + clazz.toString(), xany);
        } finally {
            if (xjexl != null) {
                if (silent) {
                    logger.warn(xjexl.getMessage(), xjexl.getCause());
                    return null;
                }
                throw xjexl;
            }
        }
        return result;
    }

    /**
     * Creates an interpreter.
     * @param context a JexlContext; if null, the EMPTY_CONTEXT is used instead.
     * @return an Interpreter
     */
    protected Interpreter createInterpreter(JexlContext context) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        return new Interpreter(this, context);
    }

    /**
     * A soft reference on cache.
     * <p>The cache is held through a soft reference, allowing it to be GCed under
     * memory pressure.</p>
     * @param <K> the cache key entry type
     * @param <V> the cache key value type
     */
    protected class SoftCache<K, V> {
        /**
         * The cache size.
         */
        private final int size;
        /**
         * The soft reference to the cache map.
         */
        private SoftReference<Map<K, V>> ref = null;

        /**
         * Creates a new instance of a soft cache.
         * @param theSize the cache size
         */
        SoftCache(int theSize) {
            size = theSize;
        }

        /**
         * Returns the cache size.
         * @return the cache size
         */
        int size() {
            return size;
        }

        /**
         * Produces the cache entry set.
         * @return the cache entry set
         */
        Set<Entry<K, V>> entrySet() {
            Map<K, V> map = ref != null ? ref.get() : null;
            return map != null ? map.entrySet() : Collections.<Entry<K, V>>emptySet();
        }

        /**
         * Gets a value from cache.
         * @param key the cache entry key
         * @return the cache entry value
         */
        V get(K key) {
            final Map<K, V> map = ref != null ? ref.get() : null;
            return map != null ? map.get(key) : null;
        }

        /**
         * Puts a value in cache.
         * @param key the cache entry key
         * @param script the cache entry value
         */
        void put(K key, V script) {
            Map<K, V> map = ref != null ? ref.get() : null;
            if (map == null) {
                map = createCache(size);
                ref = new SoftReference<Map<K, V>>(map);
            }
            map.put(key, script);
        }
    }

    /**
     * Creates a cache.
     * @param <K> the key type
     * @param <V> the value type
     * @param cacheSize the cache size, must be > 0
     * @return a Map usable as a cache bounded to the given size
     */
    protected <K, V> Map<K, V> createCache(final int cacheSize) {
        return new java.util.LinkedHashMap<K, V>(cacheSize, LOAD_FACTOR, true) {
            /** Serial version UID. */
            private static final long serialVersionUID = 3801124242820219131L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > cacheSize;
            }
        };
    }

    /**
     * Parses an expression.
     * @param expression the expression to parse
     * @param info debug information structure
     * @return the parsed tree
     * @throws JexlException if any error occured during parsing
     */
    protected ASTJexlScript parse(CharSequence expression, JexlInfo info) {
        String expr = cleanExpression(expression);
        ASTJexlScript tree = null;
        synchronized (parser) {
            if (cache != null) {
                tree = cache.get(expr);
                if (tree != null) {
                    return tree;
                }
            }
            try {
                Reader reader = new StringReader(expr);
                // use first calling method of JexlEngine as debug info
                if (info == null) {
                    info = debugInfo();
                }
                tree = parser.parse(reader, info);
                if (cache != null) {
                    cache.put(expr, tree);
                }
            } catch (TokenMgrError xtme) {
                throw new JexlException(info, "tokenization failed", xtme);
            } catch (ParseException xparse) {
                throw new JexlException(info, "parsing failed", xparse);
            }
        }
        return tree;
    }

    /**
     * Creates a JexlInfo instance.
     * @param fn url/file name
     * @param l line number
     * @param c column number
     * @return a JexlInfo instance
     */
    protected JexlInfo createInfo(String fn, int l, int c) {
        return new DebugInfo(fn, l, c);
    }

    /**
     * Creates and fills up debugging information.
     * <p>This gathers the class, method and line number of the first calling method
     * not owned by JexlEngine, UnifiedJEXL or {Script,Expression}Factory.</p>
     * @return an Info if debug is set, null otherwise
     */
    protected JexlInfo debugInfo() {
        JexlInfo info = null;
        if (debug) {
            Throwable xinfo = new Throwable();
            xinfo.fillInStackTrace();
            StackTraceElement[] stack = xinfo.getStackTrace();
            StackTraceElement se = null;
            Class<?> clazz = getClass();
            for (int s = 1; s < stack.length; ++s, se = null) {
                se = stack[s];
                String className = se.getClassName();
                if (!className.equals(clazz.getName())) {
                    // go deeper if called from JexlEngine or UnifiedJEXL
                    if (className.equals(JexlEngine.class.getName())) {
                        clazz = JexlEngine.class;
                    } else if (className.equals(UnifiedJEXL.class.getName())) {
                        clazz = UnifiedJEXL.class;
                    } else {
                        break;
                    }
                }
            }
            if (se != null) {
                info = createInfo(se.getClassName() + "." + se.getMethodName(), se.getLineNumber(), 0);
            }
        }
        return info;
    }

    /**
     * Trims the expression from front & ending spaces.
     * @param str expression to clean
     * @return trimmed expression ending in a semi-colon
     */
    public static final String cleanExpression(CharSequence str) {
        if (str != null) {
            int start = 0;
            int end = str.length();
            if (end > 0) {
                // trim front spaces
                while (start < end && str.charAt(start) == ' ') {
                    ++start;
                }
                // trim ending spaces
                while (end > 0 && str.charAt(end - 1) == ' ') {
                    --end;
                }
                return str.subSequence(start, end).toString();
            }
            return "";
        }
        return null;
    }

    /**
     * Read from a reader into a StringBuffer and return a String with
     * the contents of the reader.
     * @param scriptReader to be read.
     * @return the contents of the reader as a String.
     * @throws IOException on any error reading the reader.
     */
    public static final String readerToString(Reader scriptReader) throws IOException {
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
        } finally {
            try {
                reader.close();
            } catch(IOException xio) {
                // ignore
            }
        }

    }
}
