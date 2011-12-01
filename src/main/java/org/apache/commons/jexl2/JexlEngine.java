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
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.apache.commons.jexl2.parser.ASTArrayAccess;
import org.apache.commons.jexl2.parser.ASTIdentifier;
import org.apache.commons.jexl2.parser.ASTReference;

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
 * <p>The <code>setSilent</code> and <code>setLenient</code> methods allow to fine-tune an engine instance behavior
 * according to various error control needs. The lenient/strict flag tells the engine when and if null as operand is
 * considered an error, the silent/verbose flag tells the engine what to do with the error
 * (log as warning or throw exception).
 * </p>
 * <ul>
 * <li>When "silent" &amp; "lenient":
 * <p> 0 & null should be indicators of "default" values so that even in an case of error,
 * something meaningfull can still be inferred; may be convenient for configurations.
 * </p>
 * </li>
 * <li>When "silent" &amp; "strict":
 * <p>One should probably consider using null as an error case - ie, every object
 * manipulated by JEXL should be valued; the ternary operator, especially the '?:' form
 * can be used to workaround exceptional cases.
 * Use case could be configuration with no implicit values or defaults.
 * </p>
 * </li>
 * <li>When "verbose" &amp; "lenient":
 * <p>The error control grain is roughly on par with JEXL 1.0</p>
 * </li>
 * <li>When "verbose" &amp; "strict":
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
        private static final Uberspect UBERSPECT = new UberspectImpl(LogFactory.getLog(JexlEngine.class));

        /** Non-instantiable. */
        private UberspectHolder() {
        }
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
     * return null (true) on errors. Default is false.
     */
    // TODO could this be private?
    protected volatile boolean silent = false;
    /**
     * Whether error messages will carry debugging information.
     */
    // TODO could be made private
    protected volatile boolean debug = true;
    /**
     *  The map of 'prefix:function' to object implementing the functions.
     */
    // TODO this could probably be private; is it threadsafe?
    protected Map<String, Object> functions = Collections.emptyMap();
    /**
     * The expression cache.
     */
    // TODO is this thread-safe? Could it be made private?
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
        return new UberspectImpl(logger);
    }

    /**
     * Gets this engine underlying uberspect.
     * @return the uberspect
     */
    public Uberspect getUberspect() {
        return uberspect;
    }

    /**
     * Gets this engine underlying arithmetic.
     * @return the arithmetic
     * @since 2.1
     */
    public JexlArithmetic getArithmetic() {
        return arithmetic;
    }

    /**
     * Sets whether this engine reports debugging information when error occurs.
     * <p>This method is <em>conditionally</em> thread safe.
     * If a single JexlEngine is shared between threads, 
     * it should only be called when the engine is not being used.</p>
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
     * <p>This method is <em>conditionally</em> thread safe.
     * If a single JexlEngine is shared between threads, 
     * it should only be called when the engine is not being used.</p>
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
     * Sets whether this engine considers unknown variables, methods and constructors as errors or evaluates them
     * as null or zero.
     * <p>This method is <em>conditionally</em> thread safe.
     * If a single JexlEngine is shared between threads, 
     * it should only be called when the engine is not being used.</p>
     * <p>As of 2.1, you can use JexlThreadedArithmetic instance to allow the JexlArithmetic
     * leniency behavior to be independently specified per thread, whilst still using a single engine</p>
     * @see JexlEngine#setSilent
     * @see JexlEngine#setDebug
     * @param flag true means no JexlException will occur, false allows them
     */
    public void setLenient(boolean flag) {
        if (arithmetic instanceof JexlThreadedArithmetic) {
            JexlThreadedArithmetic.setLenient(Boolean.valueOf(flag));
        } else {
            arithmetic.setLenient(flag);
        }
    }

    /**
     * Checks whether this engine considers unknown variables, methods and constructors as errors.
     * @return true if lenient, false if strict
     */
    public boolean isLenient() {
        return arithmetic.isLenient();
    }

    /**
     * Sets whether this engine behaves in strict or lenient mode.
     * Equivalent to setLenient(!flag).
     * <p>This method is <em>conditionally</em> thread safe.
     * If a single JexlEngine is shared between threads, 
     * it should only be called when the engine is not being used.</p>
     * @param flag true for strict, false for lenient
     * @since 2.1
     */
    public final void setStrict(boolean flag) {
        setLenient(!flag);
    }

    /**
     * Checks whether this engine behaves in strict or lenient mode.
     * Equivalent to !isLenient().
     * @return true for strict, false for lenient
     * @since 2.1
     */
    public final boolean isStrict() {
        return !isLenient();
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
     * Sets a cache for expressions of the defined size.
     * <p>The cache will contain at most <code>size</code> expressions. Note that
     * all JEXL caches are held through SoftReferences and may be garbage-collected.</p>
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
     * <p>Note that the JexlContext is also used to try to solve top-level functions. This allows ObjectContext
     * derived instances to call methods on the wrapped object.</p>
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
        ASTJexlScript tree = parse(expression, info, null);
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
        return createScript(scriptText, null, null);
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @param info An info structure to carry debugging information if needed
     * @return A {@link Script} which can be executed using a {@link JexlContext}.
     * @throws JexlException if there is a problem parsing the script.
     * @deprecated Use {@link #createScript(String, JexlInfo, String[])}
     */
    @Deprecated
    public Script createScript(String scriptText, JexlInfo info) {
        if (scriptText == null) {
            throw new NullPointerException("scriptText is null");
        }
        // Parse the expression
        ASTJexlScript tree = parse(scriptText, info);
        return createScript(tree, scriptText);
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @param names the script parameter names
     * @return A {@link Script} which can be executed using a {@link JexlContext}.
     * @throws JexlException if there is a problem parsing the script.
     */
    public Script createScript(String scriptText, String... names) {
        return createScript(scriptText, null, names);
    }

    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     * It uses an array of parameter names that will be resolved during parsing;
     * a corresponding array of arguments containing values should be used during evaluation.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @param info An info structure to carry debugging information if needed
     * @param names the script parameter names
     * @return A {@link Script} which can be executed using a {@link JexlContext}.
     * @throws JexlException if there is a problem parsing the script.
     * @since 2.1
     */
    public Script createScript(String scriptText, JexlInfo info, String[] names) {
        if (scriptText == null) {
            throw new NullPointerException("scriptText is null");
        }
        // Parse the expression
        ASTJexlScript tree = parse(scriptText, info, new Scope(names));
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
        return createScript(readerToString(reader), info, null);
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
        return createScript(readerToString(reader), info, null);
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
        // synthetize expr using register
        expr = "#0" + (expr.charAt(0) == '[' ? "" : ".") + expr + ";";
        try {
            parser.ALLOW_REGISTERS = true;
            Scope frame = new Scope("#0");
            ASTJexlScript script = parse(expr, null, frame);
            JexlNode node = script.jjtGetChild(0);
            Interpreter interpreter = createInterpreter(context);
            // set frame
            interpreter.setFrame(script.createFrame(bean));
            return node.jjtAccept(interpreter, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl;
        } finally {
            parser.ALLOW_REGISTERS = false;
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
        // synthetize expr using registers
        expr = "#0" + (expr.charAt(0) == '[' ? "" : ".") + expr + "=" + "#1" + ";";
        try {
            parser.ALLOW_REGISTERS = true;
            Scope frame = new Scope("#0", "#1");
            ASTJexlScript script = parse(expr, null, frame);
            JexlNode node = script.jjtGetChild(0);
            Interpreter interpreter = createInterpreter(context);
            // set the registers
            interpreter.setFrame(script.createFrame(bean, value));
            node.jjtAccept(interpreter, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return;
            }
            throw xjexl;
        } finally {
            parser.ALLOW_REGISTERS = false;
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
    public <T> T newInstance(Class<? extends T> clazz, Object... args) {
        return clazz.cast(doCreateInstance(clazz, args));
    }

    /**
     * Creates a new instance of an object using the most appropriate constructor
     * based on the arguments.
     * @param clazz the name of the class to instantiate resolved through this engine's class loader
     * @param args the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    public Object newInstance(String clazz, Object... args) {
        return doCreateInstance(clazz, args);
    }

    /**
     * Creates a new instance of an object using the most appropriate constructor
     * based on the arguments.
     * @param clazz the class to instantiate
     * @param args the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    protected Object doCreateInstance(Object clazz, Object... args) {
        JexlException xjexl = null;
        Object result = null;
        JexlInfo info = debugInfo();
        try {
            JexlMethod ctor = uberspect.getConstructorMethod(clazz, args, info);
            if (ctor == null && arithmetic.narrowArguments(args)) {
                ctor = uberspect.getConstructorMethod(clazz, args, info);
            }
            if (ctor != null) {
                result = ctor.invoke(clazz, args);
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
        return createInterpreter(context, isStrict(), isSilent());
    }

    /**
     * Creates an interpreter.
     * @param context a JexlContext; if null, the EMPTY_CONTEXT is used instead.
     * @param strictFlag whether the interpreter runs in strict mode
     * @param silentFlag whether the interpreter runs in silent mode
     * @return an Interpreter
     * @since 2.1
     */
    protected Interpreter createInterpreter(JexlContext context, boolean strictFlag, boolean silentFlag) {
        return new Interpreter(this, context == null ? EMPTY_CONTEXT : context, strictFlag, silentFlag);
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
         * Clears the cache.
         */
        void clear() {
            ref = null;
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
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > cacheSize;
            }
        };
    }

    /**
     * Clears the expression cache.
     * @since 2.1
     */
    public void clearCache() {
        synchronized (parser) {
            cache.clear();
        }
    }

    /**
     * Gets the list of variables accessed by a script.
     * <p>This method will visit all nodes of a script and extract all variables whether they
     * are written in 'dot' or 'bracketed' notation. (a.b is equivalent to a['b']).</p>
     * @param script the script
     * @return the set of variables, each as a list of strings (ant-ish variables use more than 1 string)
     *         or the empty set if no variables are used
     * @since 2.1
     */
    public Set<List<String>> getVariables(Script script) {
        if (script instanceof ExpressionImpl) {
            Set<List<String>> refs = new LinkedHashSet<List<String>>();
            getVariables(((ExpressionImpl) script).script, refs, null);
            return refs;
        } else {
            return Collections.<List<String>>emptySet();
        }
    }

    /**
     * Fills up the list of variables accessed by a node.
     * @param node the node
     * @param refs the set of variable being filled
     * @param ref the current variable being filled
     * @since 2.1
     */
    protected void getVariables(JexlNode node, Set<List<String>> refs, List<String> ref) {
        boolean array = node instanceof ASTArrayAccess;
        boolean reference = node instanceof ASTReference;
        int num = node.jjtGetNumChildren();
        if (array || reference) {
            List<String> var = ref != null ? ref : new ArrayList<String>();
            boolean varf = true;
            for (int i = 0; i < num; ++i) {
                JexlNode child = node.jjtGetChild(i);
                if (array) {
                    if (child instanceof ASTReference && child.jjtGetNumChildren() == 1) {
                        JexlNode desc = child.jjtGetChild(0);
                        if (varf && desc.isConstant()) {
                            String image = desc.image;
                            if (image == null) {
                                var.add(new Debugger().data(desc));
                            } else {
                                var.add(image); 
                            }
                        } else if (desc instanceof ASTIdentifier) {
                            if (((ASTIdentifier) desc).getRegister() < 0) {
                                List<String> di = new ArrayList<String>(1);
                                di.add(desc.image);
                                refs.add(di);
                            }
                            var = new ArrayList<String>();
                            varf = false;
                        }
                        continue;
                    } else if (child instanceof ASTIdentifier) {
                        if (i == 0 && (((ASTIdentifier) child).getRegister() < 0)) {
                            var.add(child.image);
                        }
                        continue;
                    }
                } else {//if (reference) {
                    if (child instanceof ASTIdentifier) {
                        if (((ASTIdentifier) child).getRegister() < 0) {
                            var.add(child.image);
                        }
                        continue;
                    }
                }
                getVariables(child, refs, var);
            }
            if (!var.isEmpty() && var != ref) {
                refs.add(var);
            }
        } else {
            for (int i = 0; i < num; ++i) {
                getVariables(node.jjtGetChild(i), refs, null);
            }
        }
    }

    /**
     * Gets the array of parameters from a script.
     * @param script the script
     * @return the parameters which may be empty (but not null) if no parameters were defined
     * @since 2.1
     */
    protected String[] getParameters(Script script) {
        if (script instanceof ExpressionImpl) {
            return ((ExpressionImpl) script).getParameters();
        } else {
            return new String[0];
        }
    }

    /**
     * Gets the array of local variable from a script.
     * @param script the script
     * @return the local variables array which may be empty (but not null) if no local variables were defined
     * @since 2.1
     */
    protected String[] getLocalVariables(Script script) {
        if (script instanceof ExpressionImpl) {
            return ((ExpressionImpl) script).getLocalVariables();
        } else {
            return new String[0];
        }
    }

    /**
     * A script scope, stores the declaration of parameters and local variables.
     * @since 2.1
     */
    public static final class Scope {
        /**
         * The number of parameters.
         */
        private final int parms;
        /**
         * The map of named registers aka script parameters.
         * Each parameter is associated to a register and is materialized as an offset in the registers array used
         * during evaluation.
         */
        private Map<String, Integer> namedRegisters = null;

        /**
         * Creates a new scope with a list of parameters.
         * @param parameters the list of parameters
         */
        public Scope(String... parameters) {
            if (parameters != null) {
                parms = parameters.length;
                namedRegisters = new LinkedHashMap<String, Integer>();
                for (int p = 0; p < parms; ++p) {
                    namedRegisters.put(parameters[p], Integer.valueOf(p));
                }
            } else {
                parms = 0;
            }
        }

        @Override
        public int hashCode() {
            return namedRegisters == null ? 0 : parms ^ namedRegisters.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Scope && equals((Scope) o);
        }

        /**
         * Whether this frame is equal to another.
         * @param frame the frame to compare to
         * @return true if equal, false otherwise
         */
        public boolean equals(Scope frame) {
            if (this == frame) {
                return true;
            } else if (frame == null || parms != frame.parms) {
                return false;
            } else if (namedRegisters == null) {
                return frame.namedRegisters == null;
            } else {
                return namedRegisters.equals(frame.namedRegisters);
            }
        }

        /**
         * Checks whether an identifier is a local variable or argument, ie stored in a register. 
         * @param name the register name
         * @return the register index
         */
        public Integer getRegister(String name) {
            return namedRegisters != null ? namedRegisters.get(name) : null;
        }

        /**
         * Declares a local variable.
         * <p>
         * This method creates an new entry in the named register map.
         * </p>
         * @param name the variable name
         * @return the register index storing this variable
         */
        public Integer declareVariable(String name) {
            if (namedRegisters == null) {
                namedRegisters = new LinkedHashMap<String, Integer>();
            }
            Integer register = namedRegisters.get(name);
            if (register == null) {
                register = Integer.valueOf(namedRegisters.size());
                namedRegisters.put(name, register);
            }
            return register;
        }

        /**
         * Creates a frame by copying values up to the number of parameters.
         * @param values the argument values
         * @return the arguments array
         */
        public Frame createFrame(Object... values) {
            if (namedRegisters != null) {
                Object[] arguments = new Object[namedRegisters.size()];
                if (values != null) {
                    System.arraycopy(values, 0, arguments, 0, Math.min(parms, values.length));
                }
                return new Frame(arguments, namedRegisters.keySet().toArray(new String[0]));
            } else {
                return null;
            }
        }

        /**
         * Gets the (maximum) number of arguments this script expects.
         * @return the number of parameters
         */
        public int getArgCount() {
            return parms;
        }

        /**
         * Gets this script registers, i.e. parameters and local variables.
         * @return the register names
         */
        public String[] getRegisters() {
            return namedRegisters != null ? namedRegisters.keySet().toArray(new String[0]) : new String[0];
        }

        /**
         * Gets this script parameters, i.e. registers assigned before creating local variables.
         * @return the parameter names
         */
        public String[] getParameters() {
            if (namedRegisters != null && parms > 0) {
                String[] pa = new String[parms];
                int p = 0;
                for (Map.Entry<String, Integer> entry : namedRegisters.entrySet()) {
                    if (entry.getValue().intValue() < parms) {
                        pa[p++] = entry.getKey();
                    }
                }
                return pa;
            } else {
                return null;
            }
        }

        /**
         * Gets this script local variable, i.e. registers assigned to local variables.
         * @return the parameter names
         */
        public String[] getLocalVariables() {
            if (namedRegisters != null && parms > 0) {
                String[] pa = new String[parms];
                int p = 0;
                for (Map.Entry<String, Integer> entry : namedRegisters.entrySet()) {
                    if (entry.getValue().intValue() >= parms) {
                        pa[p++] = entry.getKey();
                    }
                }
                return pa;
            } else {
                return null;
            }
        }
    }

    /**
     * A call frame, created from a scope, stores the arguments and local variables as "registers".
     * @since 2.1
     */
    public static final class Frame {
        /** Registers or arguments. */
        private Object[] registers = null;
        /** Parameter and argument names if any. */
        private String[] parameters = null;
        
        /**
         * Creates a new frame.
         * @param r the registers
         * @param p the parameters
         */
        Frame(Object[] r, String[] p) {
            registers = r;
            parameters = p;
        }
        
        /**
         * @return the registers
         */
        public Object[] getRegisters() {
            return registers;
        }
                
        /**
         * @return the parameters
         */
        public String[] getParameters() {
            return parameters;
        }
    }

    /**
     * Parses an expression.
     * @param expression the expression to parse
     * @param info debug information structure
     * @return the parsed tree
     * @throws JexlException if any error occured during parsing
     * @deprecated Use {@link #parse(CharSequence, JexlInfo, Scope)} instead
     */
    @Deprecated
    protected ASTJexlScript parse(CharSequence expression, JexlInfo info) {
        return parse(expression, info, null);
    }

    /**
     * Parses an expression.
     * @param expression the expression to parse
     * @param info debug information structure
     * @param frame the script frame to use
     * @return the parsed tree
     * @throws JexlException if any error occured during parsing
     */
    protected ASTJexlScript parse(CharSequence expression, JexlInfo info, Scope frame) {
        String expr = cleanExpression(expression);
        ASTJexlScript script = null;
        DebugInfo dbgInfo = null;
        synchronized (parser) {
            if (cache != null) {
                script = cache.get(expr);
                if (script != null) {
                    Scope f = script.getScope();
                    if ((f == null && frame == null) || (f != null && f.equals(frame))) {
                        return script;
                    }
                }
            }
            try {
                Reader reader = new StringReader(expr);
                // use first calling method of JexlEngine as debug info
                if (info == null) {
                    dbgInfo = debugInfo();
                } else if (info instanceof DebugInfo) {
                    dbgInfo = (DebugInfo) info;
                } else {
                    dbgInfo = info.debugInfo();
                }
                parser.setFrame(frame);
                script = parser.parse(reader, dbgInfo);
                // reaccess in case local variables have been declared
                frame = parser.getFrame();
                if (frame != null) {
                    script.setScope(frame);
                }
                if (cache != null) {
                    cache.put(expr, script);
                }
            } catch (TokenMgrError xtme) {
                throw new JexlException.Tokenization(dbgInfo, expression, xtme);
            } catch (ParseException xparse) {
                throw new JexlException.Parsing(dbgInfo, expression, xparse);
            } finally {
                parser.setFrame(null);
            }
        }
        return script;
    }

    /**
     * Creates a JexlInfo instance.
     * @param fn url/file name
     * @param l line number
     * @param c column number
     * @return a JexlInfo instance
     */
    protected DebugInfo createInfo(String fn, int l, int c) {
        return new DebugInfo(fn, l, c);
    }

    /**
     * Creates and fills up debugging information.
     * <p>This gathers the class, method and line number of the first calling method
     * not owned by JexlEngine, UnifiedJEXL or {Script,Expression}Factory.</p>
     * @return an Info if debug is set, null otherwise
     */
    protected DebugInfo debugInfo() {
        DebugInfo info = null;
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
    public static String cleanExpression(CharSequence str) {
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
     * Read from a reader into a local buffer and return a String with
     * the contents of the reader.
     * @param scriptReader to be read.
     * @return the contents of the reader as a String.
     * @throws IOException on any error reading the reader.
     */
    public static String readerToString(Reader scriptReader) throws IOException {
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
            } catch (IOException xio) {
                // ignore
            }
        }

    }
}
