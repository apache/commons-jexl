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

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;

import org.apache.commons.jexl3.internal.introspection.SandboxUberspect;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParseException;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.TokenMgrError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.lang.ref.SoftReference;

import java.net.URL;
import java.net.URLConnection;

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
     * Creates a JEXL engine using the provided {@link JexlBuilder}.
     * @param conf the builder
     */
    public Engine(JexlBuilder conf) {
        JexlSandbox sandbox = conf.sandbox();
        JexlUberspect uber = conf.uberspect() == null ? getUberspect(conf.logger()) : conf.uberspect();
        ClassLoader loader = conf.loader();
        if (loader != null) {
            uber.setClassLoader(loader);
        }
        if (sandbox == null) {
            this.uberspect = uber;
        } else {
            this.uberspect = new SandboxUberspect(uber, sandbox);
        }
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
    public TemplateEngine createJxltEngine() {
        return new TemplateEngine(this);
    }

    @Override
    public boolean isDebug() {
        return this.debug;
    }

    @Override
    public boolean isSilent() {
        return this.silent;
    }

    @Override
    public final boolean isStrict() {
        return strict;
    }

    @Override
    public void setClassLoader(ClassLoader loader) {
        uberspect.setClassLoader(loader);
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

    @Override
    public JexlExpression createExpression(String expression) {
        return createExpression(expression, null);
    }

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

    @Override
    public Script createScript(String scriptText) {
        return createScript(scriptText, null, null);
    }

    @Override
    public Script createScript(String scriptText, String... names) {
        return createScript(scriptText, null, names);
    }

    @Override
    public Script createScript(String scriptText, JexlInfo info, String[] names) {
        if (scriptText == null) {
            throw new NullPointerException("scriptText is null");
        }
        // Parse the expression
        ASTJexlScript tree = parse(scriptText, info, new Scope(null, names));
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

    @Override
    public Object getProperty(Object bean, String expr) {
        return getProperty(null, bean, expr);
    }

    @Override
    public Object getProperty(JexlContext context, Object bean, String expr) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        // synthetize expr using register
        expr = "#0" + (expr.charAt(0) == '[' ? "" : ".") + expr + ";";
        try {
            parser.ALLOW_REGISTERS = true;
            Scope scope = new Scope(null, "#0");
            ASTJexlScript script = parse(expr, null, scope);
            JexlNode node = script.jjtGetChild(0);
            Scope.Frame frame = script.createFrame(bean);
            Interpreter interpreter = createInterpreter(context, frame);
            return node.jjtAccept(interpreter, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl.clean();
        } finally {
            parser.ALLOW_REGISTERS = false;
        }
    }

    @Override
    public void setProperty(Object bean, String expr, Object value) {
        setProperty(null, bean, expr, value);
    }

    @Override
    public void setProperty(JexlContext context, Object bean, String expr, Object value) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        // synthetize expr using registers
        expr = "#0" + (expr.charAt(0) == '[' ? "" : ".") + expr + "=" + "#1" + ";";
        try {
            parser.ALLOW_REGISTERS = true;
            Scope scope = new Scope(null, "#0", "#1");
            ASTJexlScript script = parse(expr, null, scope);
            JexlNode node = script.jjtGetChild(0);
            Scope.Frame frame = script.createFrame(bean, value);
            Interpreter interpreter = createInterpreter(context, frame);
            node.jjtAccept(interpreter, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return;
            }
            throw xjexl.clean();
        } finally {
            parser.ALLOW_REGISTERS = false;
        }
    }

    @Override
    public Object invokeMethod(Object obj, String meth, Object... args) {
        JexlException xjexl = null;
        Object result = null;
        final JexlInfo info = jexlInfo();
        try {
            JexlMethod method = uberspect.getMethod(obj, meth, args);
            if (method == null && arithmetic.narrowArguments(args)) {
                method = uberspect.getMethod(obj, meth, args);
            }
            if (method != null) {
                result = method.invoke(obj, args);
            } else {
                xjexl = new JexlException.Method(info, meth, null);
            }
        } catch (Exception xany) {
            xjexl = new JexlException.Method(info, meth, xany);
        } finally {
            if (xjexl != null) {
                if (silent) {
                    logger.warn(xjexl.getMessage(), xjexl.getCause());
                    return null;
                }
                throw xjexl.clean();
            }
        }
        return result;
    }

    @Override
    public <T> T newInstance(Class<? extends T> clazz, Object... args) {
        return clazz.cast(doCreateInstance(clazz, args));
    }

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
        try {
            JexlMethod ctor = uberspect.getConstructor(clazz, args);
            if (ctor == null && arithmetic.narrowArguments(args)) {
                ctor = uberspect.getConstructor(clazz, args);
            }
            if (ctor != null) {
                result = ctor.invoke(clazz, args);
            } else {
                xjexl = new JexlException.Method(info, clazz.toString(), null);
            }
        } catch (Exception xany) {
            xjexl = new JexlException.Method(info, clazz.toString(), xany);
        } finally {
            if (xjexl != null) {
                if (silent) {
                    logger.warn(xjexl.getMessage(), xjexl.getCause());
                    return null;
                }
                throw xjexl.clean();
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
    protected Interpreter createInterpreter(JexlContext context, Scope.Frame frame) {
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
                script = parser.parse(reader, frame, jexlInfo);
                if (cache != null) {
                    cache.put(expr, script);
                }
            } catch (TokenMgrError xtme) {
                throw new JexlException.Tokenization(jexlInfo, expression, xtme).clean();
            } catch (ParseException xparse) {
                throw new JexlException.Parsing(jexlInfo, expression, xparse).clean();
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
            for (int s = 1; s < stack.length; ++s) {
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
