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
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.internal.introspection.SandboxUberspect;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.Parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A JexlEngine implementation.
 * @since 2.0
 */
public class Engine extends JexlEngine {
    /**
     * Gets the default instance of Uberspect.
     * <p>This is lazily initialized to avoid building a default instance if there
     * is no use for it. The main reason for not using the default Uberspect instance is to
     * be able to use a (low level) introspector created with a given logger
     * instead of the default one.</p>
     * <p>Implemented as on demand holder idiom.</p>
     */
    private static final class UberspectHolder {
        /** The default uberspector that handles all introspection patterns. */
        private static final Uberspect UBERSPECT =
                new Uberspect(LogFactory.getLog(JexlEngine.class), JexlUberspect.JEXL_STRATEGY);

        /** Non-instantiable. */
        private UberspectHolder() {}
    }
    /**
     * The Log to which all JexlEngine messages will be logged.
     */
    protected final Log logger;
    /**
     * The JexlUberspect instance.
     */
    protected final JexlUberspect uberspect;
    /**
     * The {@link JexlArithmetic} instance.
     */
    protected final JexlArithmetic arithmetic;
    /**
     * The map of 'prefix:function' to object implementing the namespaces.
     */
    protected final Map<String, Object> functions;
    /**
     * The maximum stack height.
     */
    protected final int stackOverflow;
    /**
     * Whether this engine considers unknown variables, methods and constructors as errors.
     */
    protected final boolean strict;
    /**
     * Whether this engine considers null in navigation expression as errors.
     */
    protected final boolean safe;
    /**
     * Whether expressions evaluated by this engine will throw exceptions (false) or return null (true) on errors.
     * Default is false.
     */
    protected final boolean silent;
    /**
     * Whether expressions evaluated by this engine will throw JexlException.Cancel (true) or return null (false) when
     * interrupted.
     * Default is true when not silent and strict.
     */
    protected final boolean cancellable;
    /**
     * Whether error messages will carry debugging information.
     */
    protected final boolean debug;
    /**
     * The atomic parsing flag; true whilst parsing.
     */
    protected final AtomicBoolean parsing = new AtomicBoolean(false);
    /**
     * The default charset.
     */
    protected final Charset charset;
    /**
     * The set of default script parsing features.
     */
    protected final JexlFeatures scriptFeatures;
    /**
     * The set of default expression parsing features.
     */
    protected final JexlFeatures expressionFeatures;
    /**
     * The {@link Parser}; when parsing expressions, this engine uses the parser if it
     * is not already in use otherwise it will create a new temporary one.
     */
    protected final Parser parser = new Parser(new StringReader(";")); //$NON-NLS-1$
    /**
     * The expression max length to hit the cache.
     */
    protected final int cacheThreshold;
    /**
     * The expression cache.
     */
    protected final SoftCache<Source, ASTJexlScript> cache;
    /**
     * The default jxlt engine.
     */
    protected volatile TemplateEngine jxlt = null;

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
        // options:
        this.strict = conf.strict() == null ? true : conf.strict();
        this.safe = conf.safe() == null ? false : conf.safe();
        this.silent = conf.silent() == null ? false : conf.silent();
        this.cancellable = conf.cancellable() == null ? !silent && strict : conf.cancellable();
        this.debug = conf.debug() == null ? true : conf.debug();
        this.stackOverflow = conf.stackOverflow() > 0? conf.stackOverflow() : Integer.MAX_VALUE;
        // core properties:
        JexlUberspect uber = conf.uberspect() == null ? getUberspect(conf.logger(), conf.strategy()) : conf.uberspect();
        ClassLoader loader = conf.loader();
        if (loader != null) {
            uber.setClassLoader(loader);
        }
        JexlSandbox sandbox = conf.sandbox();
        if (sandbox == null) {
            this.uberspect = uber;
        } else {
            this.uberspect = new SandboxUberspect(uber, sandbox);
        }
        this.logger = conf.logger() == null ? LogFactory.getLog(JexlEngine.class) : conf.logger();
        this.arithmetic = conf.arithmetic() == null ? new JexlArithmetic(this.strict) : conf.arithmetic();
        this.functions = conf.namespaces() == null ? Collections.<String, Object>emptyMap() : conf.namespaces();
        // parsing & features:
        JexlFeatures features = conf.features() == null? DEFAULT_FEATURES : conf.features();
        this.expressionFeatures = new JexlFeatures(features).script(false);
        this.scriptFeatures = new JexlFeatures(features).script(true);
        this.charset = conf.charset();
        // caching:
        this.cache = conf.cache() <= 0 ? null : new SoftCache<Source, ASTJexlScript>(conf.cache());
        this.cacheThreshold = conf.cacheThreshold();
        if (uberspect == null) {
            throw new IllegalArgumentException("uberspect can not be null");
        }
    }

    /**
     * Gets the default instance of Uberspect.
     * <p>This is lazily initialized to avoid building a default instance if there
     * is no use for it. The main reason for not using the default Uberspect instance is to
     * be able to use a (low level) introspector created with a given logger
     * instead of the default one.</p>
     * @param logger the logger to use for the underlying Uberspect
     * @param strategy the property resolver strategy
     * @return Uberspect the default uberspector instance.
     */
    public static Uberspect getUberspect(Log logger, JexlUberspect.ResolverStrategy strategy) {
        if ((logger == null || logger.equals(LogFactory.getLog(JexlEngine.class)))
            && (strategy == null || strategy == JexlUberspect.JEXL_STRATEGY)) {
            return UberspectHolder.UBERSPECT;
        }
        return new Uberspect(logger, strategy);
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
    public boolean isDebug() {
        return this.debug;
    }

    @Override
    public boolean isSilent() {
        return this.silent;
    }

    @Override
    public boolean isStrict() {
        return strict;
    }

    @Override
    public boolean isCancellable() {
        return this.cancellable;
    }

    @Override
    public void setClassLoader(ClassLoader loader) {
        jxlt = null;
        uberspect.setClassLoader(loader);
        if (functions != null) {
            List<String> names = new ArrayList<String>(functions.keySet());
            for(String name : names) {
                Object functor = functions.get(name);
                if (functor instanceof Class<?>) {
                    Class<?> fclass = ((Class<?>) functor);
                    try {
                        Class<?> nclass = loader.loadClass(fclass.getName());
                        if (nclass != fclass) {
                            functions.put(name, nclass);
                        }
                    } catch (ClassNotFoundException xany) {
                         functions.put(name, fclass.getName());
                    }
                }
            }
        }
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public TemplateEngine createJxltEngine(boolean noScript, int cacheSize, char immediate, char deferred) {
        return new TemplateEngine(this, noScript, cacheSize, immediate, deferred);
    }

    @Override
    public void clearCache() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Creates an interpreter.
     * @param context a JexlContext; if null, the empty context is used instead.
     * @param frame   the interpreter frame
     * @return an Interpreter
     */
    protected Interpreter createInterpreter(JexlContext context, Scope.Frame frame) {
        return new Interpreter(this, context, frame);
    }


    @Override
    public Script createExpression(JexlInfo info, String expression) {
        return createScript(expressionFeatures, info, expression, null);
    }

    @Override
    public Script createScript(JexlFeatures features, JexlInfo info, String scriptText, String[] names) {
        if (scriptText == null) {
            throw new NullPointerException("source is null");
        }
        String source = trimSource(scriptText);
        Scope scope = names == null ? null : new Scope(null, names);
        ASTJexlScript tree = parse(info, features == null? scriptFeatures : features, source, scope);
        return new Script(this, source, tree);
    }

    /**
     * The features allowed for property set/get methods.
     */
    protected static final JexlFeatures PROPERTY_FEATURES = new JexlFeatures()
            .localVar(false)
            .loops(false)
            .lambda(false)
            .script(false)
            .arrayReferenceExpr(false)
            .methodCall(false)
            .register(true);

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
        String src = trimSource(expr);
        src = "#0" + (src.charAt(0) == '[' ? "" : ".") + src;
        try {
            final Scope scope = new Scope(null, "#0");
            final ASTJexlScript script = parse(null, PROPERTY_FEATURES, src, scope);
            final JexlNode node = script.jjtGetChild(0);
            final Scope.Frame frame = script.createFrame(bean);
            final Interpreter interpreter = createInterpreter(context, frame);
            return node.jjtAccept(interpreter, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl.clean();
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
        // synthetize expr using register
        String src = trimSource(expr);
        src = "#0" + (src.charAt(0) == '[' ? "" : ".") + src + "=" + "#1";
        try {
            final Scope scope = new Scope(null, "#0", "#1");
            final ASTJexlScript script = parse(null, PROPERTY_FEATURES, src, scope);
            final JexlNode node = script.jjtGetChild(0);
            final Scope.Frame frame = script.createFrame(bean, value);
            final Interpreter interpreter = createInterpreter(context, frame);
            node.jjtAccept(interpreter, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return;
            }
            throw xjexl.clean();
        }
    }

    @Override
    public Object invokeMethod(Object obj, String meth, Object... args) {
        JexlException xjexl = null;
        Object result = null;
        final JexlInfo info = debug ? createInfo() : null;
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
        } catch (JexlException xany) {
            xjexl = xany;
        } catch (Exception xany) {
            xjexl = new JexlException.Method(info, meth, xany);
        }
        if (xjexl != null) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                result = null;
            } else {
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
     * @param args  the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    protected Object doCreateInstance(Object clazz, Object... args) {
        JexlException xjexl = null;
        Object result = null;
        final JexlInfo info = debug ? createInfo() : null;
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
        } catch (JexlException xany) {
            xjexl = xany;
        } catch (Exception xany) {
            xjexl = new JexlException.Method(info, clazz.toString(), xany);
        }
        if (xjexl != null) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl.clean();
        }
        return result;
    }

    /**
     * Swaps the current thread local context.
     * @param tls the context or null
     * @return the previous thread local context
     */
    protected JexlContext.ThreadLocal putThreadLocal(JexlContext.ThreadLocal tls) {
        JexlContext.ThreadLocal local = CONTEXT.get();
        CONTEXT.set(tls);
        return local;
    }

    /**
     * Swaps the current thread local engine.
     * @param jexl the engine or null
     * @return the previous thread local engine
     */
    protected JexlEngine putThreadEngine(JexlEngine jexl) {
        JexlEngine pjexl = ENGINE.get();
        ENGINE.set(jexl);
        return pjexl;
    }

    /**
     * Gets the list of variables accessed by a script.
     * <p>This method will visit all nodes of a script and extract all variables whether they
     * are written in 'dot' or 'bracketed' notation. (a.b is equivalent to a['b']).</p>
     * @param script the script
     * @return the set of variables, each as a list of strings (ant-ish variables use more than 1 string)
     *         or the empty set if no variables are used
     */
    protected Set<List<String>> getVariables(ASTJexlScript script) {
        VarCollector collector = new VarCollector();
        getVariables(script, script, collector);
        return collector.collected();
    }

    /**
     * Utility class to collect variables.
     */
    protected static class VarCollector {
        /**
         * The collected variables represented as a set of list of strings.
         */
        private final Set<List<String>> refs = new LinkedHashSet<List<String>>();
        /**
         * The current variable being collected.
         */
        private List<String> ref = new ArrayList<String>();
        /**
         * The node that started the collect.
         */
        private JexlNode root = null;

        /**
         * Starts/stops a variable collect.
         * @param node starts if not null, stop if null
         */
        public void collect(JexlNode node) {
            if (!ref.isEmpty()) {
                refs.add(ref);
                ref = new ArrayList<String>();
            }
            root = node;
        }

        /**
         * @return true if currently collecting a variable, false otherwise
         */
        public boolean isCollecting() {
            return root instanceof ASTIdentifier;
        }

        /**
         * Adds a 'segment' to the variable being collected.
         * @param name the name
         */
        public void add(String name) {
            ref.add(name);
        }

        /**
         *@return the collected variables
         */
        public Set<List<String>> collected() {
            return refs;
        }
    }

    /**
     * Fills up the list of variables accessed by a node.
     * @param script the owning script
     * @param node the node
     * @param collector the variable collector
     */
    protected void getVariables(final ASTJexlScript script, JexlNode node, VarCollector collector) {
        if (node instanceof ASTIdentifier) {
            JexlNode parent = node.jjtGetParent();
            if (parent instanceof ASTMethodNode || parent instanceof ASTFunctionNode) {
                // skip identifiers for methods and functions
                collector.collect(null);
                return;
            }
            ASTIdentifier identifier = (ASTIdentifier) node;
            int symbol = identifier.getSymbol();
            // symbols that are hoisted are considered "global" variables
            if (symbol >= 0 && script != null && !script.isHoistedSymbol(symbol)) {
                collector.collect(null);
            } else {
                // start collecting from identifier
                collector.collect(identifier);
                collector.add(identifier.getName());
            }
        } else if (node instanceof ASTIdentifierAccess) {
            JexlNode parent = node.jjtGetParent();
            if (parent instanceof ASTMethodNode || parent instanceof ASTFunctionNode) {
                // skip identifiers for methods and functions
                collector.collect(null);
                return;
            }
            // belt and suspender since an identifier should have been seen first
            if (collector.isCollecting()) {
                collector.add(((ASTIdentifierAccess) node).getName());
            }
        } else if (node instanceof ASTArrayAccess) {
            int num = node.jjtGetNumChildren();
            // collect only if array access is const and follows an identifier
            boolean collecting = collector.isCollecting();
            for (int i = 0; i < num; ++i) {
                JexlNode child = node.jjtGetChild(i);
                if (collecting && child.isConstant()) {
                    String image = child.toString();
                    collector.add(image);
                } else {
                    collecting = false;
                    collector.collect(null);
                    getVariables(script, child, collector);
                }
            }
        } else {
            int num = node.jjtGetNumChildren();
            for (int i = 0; i < num; ++i) {
                getVariables(script, node.jjtGetChild(i), collector);
            }
            collector.collect(null);
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
     *
     * @param info      information structure
     * @param expr     whether we parse an expression or a feature
     * @param src      the expression to parse
     * @param scope     the script frame
     * @return the parsed tree
     * @throws JexlException if any error occurred during parsing
     */
    protected ASTJexlScript parse(JexlInfo info, boolean expr, String src, Scope scope) {
        return parse(info, expr? this.expressionFeatures : this.scriptFeatures, src, scope);
    }

    /**
     * Parses an expression.
     *
     * @param info      information structure
     * @param parsingf  the set of parsing features
     * @param src      the expression to parse
     * @param scope     the script frame
     * @return the parsed tree
     * @throws JexlException if any error occurred during parsing
     */
    protected ASTJexlScript parse(JexlInfo info, JexlFeatures parsingf, String src, Scope scope) {
        final boolean cached = src.length() < cacheThreshold && cache != null;
        final JexlFeatures features = parsingf != null? parsingf : DEFAULT_FEATURES;
        final Source source = cached? new Source(features, src) : null;
        ASTJexlScript script = null;
        if (source != null) {
            script = cache.get(source);
            if (script != null) {
                Scope f = script.getScope();
                if ((f == null && scope == null) || (f != null && f.equals(scope))) {
                    return script;
                }
            }
        }
        final JexlInfo ninfo = info == null && debug ? createInfo() : info;
        // if parser not in use...
        if (parsing.compareAndSet(false, true)) {
            try {
                // lets parse
                script = parser.parse(ninfo, features, src, scope);
            } finally {
                // no longer in use
                parsing.set(false);
            }
        } else {
            // ...otherwise parser was in use, create a new temporary one
            Parser lparser = new Parser(new StringReader(";"));
            script = lparser.parse(ninfo, features, src, scope);
        }
        if (source != null) {
            cache.put(source, script);
        }
        return script;
    }

    /**
     * Trims the source from front and ending spaces.
     * @param str expression to clean
     * @return trimmed expression ending in a semi-colon
     */
    protected String trimSource(CharSequence str) {
        if (str != null) {
            int start = 0;
            int end = str.length();
            if (end > 0) {
                // trim front spaces
                while (start < end && Character.isSpaceChar(str.charAt(start))) {
                    ++start;
                }
                // trim ending spaces
                while (end > 0 && Character.isSpaceChar(str.charAt(end - 1))) {
                    --end;
                }
                return str.subSequence(start, end).toString();
            }
            return "";
        }
        return null;
    }

    /**
     * Gets and/or creates a default template engine.
     * @return a template engine
     */
    protected TemplateEngine jxlt() {
        TemplateEngine e = jxlt;
        if (e == null) {
            synchronized(this) {
                e = jxlt;
                if (e == null) {
                    e = new TemplateEngine(this, true, 0, '$', '#');
                    jxlt = e;
                }
            }
        }
        return e;
    }
}
