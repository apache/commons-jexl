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
package org.apache.commons.jexl3.internal;

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
import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlInfoHandle;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.jexl3.parser.ParseException;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.TokenMgrError;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTReference;

import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.internal.introspection.Uberspect;

/**
 * A JexlEngine implementation.
 * @since 2.0
 */
public class Engine extends JexlEngine {
    /**
     * An empty/static/non-mutable JexlContext used instead of null context.
     */
    public static final JexlContext EMPTY_CONTEXT = new JexlContext() {
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
     *  Gets the default instance of Uberspect.
     * <p>This is lazily initialized to avoid building a default instance if there
     * is no use for it. The main reason for not using the default Uberspect instance is to
     * be able to use a (low level) introspector created with a given logger
     * instead of the default one.</p>
     * <p>Implemented as on demand holder idiom.</p>
     */
    private static final class UberspectHolder {
        /** The default uberspector that handles all introspection patterns. */
        private static final Uberspect UBERSPECT =
                new Uberspect(LogFactory.getLog(JexlEngine.class));

        /** Non-instantiable. */
        private UberspectHolder() {
        }
    }
    /**
     * The JexlUberspect instance.
     */
    protected final JexlUberspect uberspect;
    /**
     * The {@link JexlArithmetic} instance.
     */
    protected final JexlArithmetic arithmetic;
    /**
     * The Log to which all JexlEngine messages will be logged.
     */
    protected final Log logger;
    /**
     * The {@link Parser}; when parsing expressions, this engine synchronizes on the parser.
     */
    protected final Parser parser = new Parser(new StringReader(";")); //$NON-NLS-1$
    /**
     * Whether this engine considers unknown variables, methods and constructors as errors.
     */
    protected final boolean strict;
    /**
     * Whether expressions evaluated by this engine will throw exceptions (false) or 
     * return null (true) on errors. Default is false.
     */
    protected final boolean silent;
    /**
     * Whether error messages will carry debugging information.
     */
    protected final boolean debug;
    /**
     *  The map of 'prefix:function' to object implementing the namespaces.
     */
    protected final Map<String, Object> functions;
    /**
     * The expression cache.
     */
    protected final SoftCache<String, ASTJexlScript> cache;
    /**
     * The default cache load factor.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * Creates an engine with default arguments.
     */
    public Engine() {
        this(new JexlBuilder());
    }

    /**
     * Creates a JEXL engine using the provided {@link Uberspect}, (@link JexlArithmetic),
     * a function map and logger.
     * @param anUberspect to allow different introspection behaviour
     * @param anArithmetic to allow different arithmetic behaviour
     * @param theFunctions an optional map of namespaces (@link setFunctions)
     * @param log the logger for various messages
     */
    public Engine(JexlUberspect anUberspect, JexlArithmetic anArithmetic, Map<String, Object> theFunctions, Log log) {
        this(new JexlBuilder().uberspect(anUberspect).arithmetic(anArithmetic).namespaces(theFunctions).logger(log));
    }

    /**
     * Creates a JEXL engine using the provided {@link JexlBuilder}.
     * @param conf the builder
     */
    public Engine(JexlBuilder conf) {
        this.uberspect = conf.uberspect() == null ? getUberspect(conf.logger()) : conf.uberspect();
        this.logger = conf.logger() == null ? LogFactory.getLog(JexlEngine.class) : conf.logger();
        this.functions = conf.namespaces() == null ? Collections.<String, Object>emptyMap() : conf.namespaces();
        this.silent = conf.silent() == null ? false : conf.silent().booleanValue();
        this.debug = conf.debug() == null ? true : conf.debug().booleanValue();
        this.strict = conf.strict() == null ? true : conf.strict().booleanValue();
        this.arithmetic = conf.arithmetic() == null ? new JexlArithmetic(this.strict) : conf.arithmetic();
        this.cache = conf.cache() <= 0 ? null : new SoftCache<String, ASTJexlScript>(conf.cache());
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
        return new Uberspect(logger);
    }

    @Override
    public JexlUberspect getUberspect() {
        return uberspect;
    }

    @Override
    public JexlArithmetic getArithmetic() {
        return arithmetic;
    }
    
    @Override
    public TemplateEngine jxlt() {
        return new TemplateEngine(this);
    }

    /**
     * Checks whether this engine is in debug mode.
     * @return true if debug is on, false otherwise
     */
    @Override
    public boolean isDebug() {
        return this.debug;
    }

    /**
     * Checks whether this engine throws JexlException during evaluation.
     * @return true if silent, false (default) otherwise
     */
    @Override
    public boolean isSilent() {
        return this.silent;
    }


    /**
     * Checks whether this engine behaves in strict or lenient mode.
     * Equivalent to !isLenient().
     * @return true for strict, false for lenient
     */
    @Override
    public final boolean isStrict() {
        return strict;
    }

    /**
     * Sets the class loader used to discover classes in 'new' expressions.
     * <p>This method is <em>not</em> thread safe; it should be called as an optional step of the JexlEngine
     * initialization code before expression creation &amp; evaluation.</p>
     * @param loader the class loader to use
     */
    @Override
    public void setClassLoader(ClassLoader loader) {
        uberspect.setClassLoader(loader);
    }

    /**
     * Retrieves the map of function namespaces.
     *
     * @return the map passed in setFunctions or the empty map if the
     * original was null.
     */
    @Override
    public Map<String, Object> getFunctions() {
        return functions;
    }

    /**
     * An overridable through covariant return JexlExpression creator.
     * @param text the script text
     * @param tree the parse AST tree
     * @return the script instance
     */
    protected JexlExpression createExpression(ASTJexlScript tree, String text) {
        return new Script(this, text, tree);
    }

    /**
     * Creates an JexlExpression from a String containing valid
     * JEXL syntax.  This method parses the expression which
     * must contain either a reference or an expression.
     * @param expression A String containing valid JEXL syntax
     * @return An JexlExpression object which can be evaluated with a JexlContext
     * @throws JexlException An exception can be thrown if there is a problem
     *      parsing this expression, or if the expression is neither an
     *      expression nor a reference.
     */
    @Override
    public JexlExpression createExpression(String expression) {
        return createExpression(expression, null);
    }

    /**
     * Creates an JexlExpression from a String containing valid
     * JEXL syntax.  This method parses the expression which
     * must contain either a reference or an expression.
     * @param expression A String containing valid JEXL syntax
     * @return An JexlExpression object which can be evaluated with a JexlContext
     * @param info An info structure to carry debugging information if needed
     * @throws JexlException An exception can be thrown if there is a problem
     *      parsing this expression, or if the expression is neither an
     *      expression or a reference.
     */
    @Override
    public JexlExpression createExpression(String expression, JexlInfo info) {
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
    @Override
    public Script createScript(String scriptText) {
        return createScript(scriptText, null, null);
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
    @Override
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
     */
    @Override
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
        return new Script(this, text, tree);
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
    @Override
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
    @Override
    public JexlScript createScript(URL scriptUrl) throws IOException {
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
    @Override
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
    @Override
    public Object getProperty(JexlContext context, Object bean, String expr) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        // synthetize expr using register
        expr = "#0" + (expr.charAt(0) == '[' ? "" : ".") + expr + ";";
        try {
            parser.ALLOW_REGISTERS = true;
            Scope scope = new Scope("#0");
            ASTJexlScript script = parse(expr, null, scope);
            JexlNode node = script.jjtGetChild(0);
            Frame frame = script.createFrame(bean);
            Interpreter interpreter = createInterpreter(context, frame);
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
    @Override
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
    @Override
    public void setProperty(JexlContext context, Object bean, String expr, Object value) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        // synthetize expr using registers
        expr = "#0" + (expr.charAt(0) == '[' ? "" : ".") + expr + "=" + "#1" + ";";
        try {
            parser.ALLOW_REGISTERS = true;
            Scope scope = new Scope("#0", "#1");
            ASTJexlScript script = parse(expr, null, scope);
            JexlNode node = script.jjtGetChild(0);
            Frame frame = script.createFrame(bean, value);
            Interpreter interpreter = createInterpreter(context, frame);
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
    @Override
    public Object invokeMethod(Object obj, String meth, Object... args) {
        JexlException xjexl = null;
        Object result = null;
        final JexlInfo info = jexlInfo();
        JexlInfoHandle handle = new JexlInfoHandle() {
            @Override
            public JexlInfo jexlInfo() {
                return info;
            }
        };
        try {
            JexlMethod method = uberspect.getMethod(obj, meth, args, handle);
            if (method == null && arithmetic.narrowArguments(args)) {
                method = uberspect.getMethod(obj, meth, args, handle);
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
    @Override
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
    @Override
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
        final JexlInfo info = jexlInfo();
        JexlInfoHandle handle = new JexlInfoHandle() {
            @Override
            public JexlInfo jexlInfo() {
                return info;
            }
        };
        try {
            JexlMethod ctor = uberspect.getConstructor(clazz, args, handle);
            if (ctor == null && arithmetic.narrowArguments(args)) {
                ctor = uberspect.getConstructor(clazz, args, handle);
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
     * @param context a JexlContext; if null, the empty context is used instead.
     * @param frame the interpreter frame
     * @return an Interpreter
     */
    protected Interpreter createInterpreter(JexlContext context, Engine.Frame frame) {
        return new Interpreter(this, context == null ? EMPTY_CONTEXT : context, frame);
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
     */
    @Override
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
     */
    protected Set<List<String>> getVariables(JexlNode script) {
        Set<List<String>> refs = new LinkedHashSet<List<String>>();
        getVariables(script, refs, null);
        return refs;
    }

    /**
     * Fills up the list of variables accessed by a node.
     * @param node the node
     * @param refs the set of variable being filled
     * @param ref the current variable being filled
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
     * @since 3.0
     */
    protected String[] getParameters(JexlScript script) {
        return script.getParameters();
    }

    /**
     * Gets the array of local variable from a script.
     * @param script the script
     * @return the local variables array which may be empty (but not null) if no local variables were defined
     * @since 3.0
     */
    protected String[] getLocalVariables(JexlScript script) {
        return script.getLocalVariables();
    }

    /**
     * A script scope, stores the declaration of parameters and local variables.
     * @since 3.0
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
                    namedRegisters.put(parameters[p], p);
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
     * @since 3.0
     */
    public static final class Frame {
        /** Registers or arguments. */
        private final Object[] registers;
        /** Parameter and argument names if any. */
        private final String[] parameters;

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
     * @param frame the script frame to use
     * @return the parsed tree
     * @throws JexlException if any error occured during parsing
     */
    protected ASTJexlScript parse(CharSequence expression, JexlInfo info, Scope frame) {
        String expr = cleanExpression(expression);
        ASTJexlScript script = null;
        JexlInfo jexlInfo = null;
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
                    jexlInfo = jexlInfo();
                } else {
                    jexlInfo = info;
                }
                parser.setFrame(frame);
                script = parser.parse(reader, jexlInfo);
                // reaccess in case local variables have been declared
                frame = parser.getFrame();
                if (frame != null) {
                    script.setScope(frame);
                }
                if (cache != null) {
                    cache.put(expr, script);
                }
            } catch (TokenMgrError xtme) {
                throw new JexlException.Tokenization(jexlInfo, expression, xtme);
            } catch (ParseException xparse) {
                throw new JexlException.Parsing(jexlInfo, expression, xparse);
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
    protected JexlInfo createInfo(String fn, int l, int c) {
        return new JexlInfo(fn, l, c);
    }

    /**
     * Creates and fills up debugging information.
     * <p>This gathers the class, method and line number of the first calling method
     * not owned by JexlEngine, TemplateEngine or {Script,JexlExpression}Factory.</p>
     * @return an Info if debug is set, null otherwise
     */
    protected JexlInfo jexlInfo() {
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
                    // go deeper if called from JexlEngine or TemplateEngine
                    if (className.equals(JexlEngine.class.getName())) {
                        clazz = JexlEngine.class;
                    } else if (className.equals(TemplateEngine.class.getName())) {
                        clazz = TemplateEngine.class;
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

}
