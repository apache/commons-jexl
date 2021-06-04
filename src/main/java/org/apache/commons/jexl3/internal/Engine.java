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
import org.apache.commons.jexl3.JexlOptions;
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
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.StringProvider;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

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
     * The name of the options pragma.
     */
    protected static final String PRAGMA_OPTIONS = "jexl.options";
    /**
     * The prefix of a namespace pragma.
     */
    protected static final String PRAGMA_JEXLNS = "jexl.namespace.";
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
     * The set of default script parsing features.
     */
    protected final JexlFeatures scriptFeatures;
    /**
     * The set of default expression parsing features.
     */
    protected final JexlFeatures expressionFeatures;
    /**
     * The default charset.
     */
    protected final Charset charset;
    /**
     * The atomic parsing flag; true whilst parsing.
     */
    protected final AtomicBoolean parsing = new AtomicBoolean(false);
    /**
     * The {@link Parser}; when parsing expressions, this engine uses the parser if it
     * is not already in use otherwise it will create a new temporary one.
     */
    protected final Parser parser = new Parser(new StringProvider(";")); //$NON-NLS-1$
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
     * Collect all or only dot references.
     */
    protected final int collectMode;
    /**
     * A cached version of the options.
     */
    protected final JexlOptions options;

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
    public Engine(final JexlBuilder conf) {
        // options:
        this.options = conf.options().copy();
        this.strict = options.isStrict();
        this.safe = options.isSafe();
        this.silent = options.isSilent();
        this.cancellable = option(conf.cancellable(), !silent && strict);
        options.setCancellable(cancellable);
        this.debug = option(conf.debug(), true);
        this.collectMode = conf.collectMode();
        this.stackOverflow = conf.stackOverflow() > 0? conf.stackOverflow() : Integer.MAX_VALUE;
        // core properties:
        final JexlUberspect uber = conf.uberspect() == null ? getUberspect(conf.logger(), conf.strategy()) : conf.uberspect();
        final ClassLoader loader = conf.loader();
        if (loader != null) {
            uber.setClassLoader(loader);
        }
        final JexlSandbox sandbox = conf.sandbox();
        if (sandbox == null) {
            this.uberspect = uber;
        } else {
            this.uberspect = new SandboxUberspect(uber, sandbox);
        }
        this.logger = conf.logger() == null ? LogFactory.getLog(JexlEngine.class) : conf.logger();
        this.arithmetic = conf.arithmetic() == null ? new JexlArithmetic(this.strict) : conf.arithmetic();
        options.setMathContext(arithmetic.getMathContext());
        options.setMathScale(arithmetic.getMathScale());
        options.setStrictArithmetic(arithmetic.isStrict());
        this.functions = conf.namespaces() == null ? Collections.emptyMap() : conf.namespaces();
        // parsing & features:
        final JexlFeatures features = conf.features() == null? DEFAULT_FEATURES : conf.features();
        Predicate<String> nsTest = features.namespaceTest();
        final Set<String> nsNames = functions.keySet();
        if (!nsNames.isEmpty()) {
            nsTest = nsTest == JexlFeatures.TEST_STR_FALSE ?nsNames::contains : nsTest.or(nsNames::contains);
        }
        this.expressionFeatures = new JexlFeatures(features).script(false).namespaceTest(nsTest);
        this.scriptFeatures = new JexlFeatures(features).script(true).namespaceTest(nsTest);
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
    public static Uberspect getUberspect(final Log logger, final JexlUberspect.ResolverStrategy strategy) {
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
        return this.strict;
    }

    @Override
    public boolean isCancellable() {
        return this.cancellable;
    }

    @Override
    public void setClassLoader(final ClassLoader loader) {
        jxlt = null;
        uberspect.setClassLoader(loader);
        if (functions != null) {
            final List<String> names = new ArrayList<String>(functions.keySet());
            for(final String name : names) {
                final Object functor = functions.get(name);
                if (functor instanceof Class<?>) {
                    final Class<?> fclass = ((Class<?>) functor);
                    try {
                        final Class<?> nclass = loader.loadClass(fclass.getName());
                        if (nclass != fclass) {
                            functions.put(name, nclass);
                        }
                    } catch (final ClassNotFoundException xany) {
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

    /**
     * Solves an optional option.
     * @param conf the option as configured, may be null
     * @param def the default value if null, shall not be null
     * @param <T> the option type
     * @return conf or def
     */
    private static <T> T option(final T conf, final T def) {
        return conf == null? def : conf;
    }

    /**
     * Extracts the engine evaluation options from context if available, the engine
     * options otherwise.
     * <p>If the context is a options handle and the handled options shared instance flag
     * is false, this method creates a copy of the options making them immutable during execution.
     * @param context the context
     * @return the options if any
     */
    protected JexlOptions options(final JexlContext context) {
        // Make a copy of the handled options if any
        if (context instanceof JexlContext.OptionsHandle) {
            final JexlOptions jexlo = ((JexlContext.OptionsHandle) context).getEngineOptions();
            if (jexlo != null) {
                return jexlo.isSharedInstance()? jexlo : jexlo.copy();
            }
        } else if (context instanceof JexlEngine.Options) {
            // This condition and block for compatibility between 3.1 and 3.2
            final JexlOptions jexlo = options.copy();
            final JexlEngine jexl = this;
            final JexlEngine.Options opts = (JexlEngine.Options) context;
            jexlo.setCancellable(option(opts.isCancellable(), jexl.isCancellable()));
            jexlo.setSilent(option(opts.isSilent(), jexl.isSilent()));
            jexlo.setStrict(option(opts.isStrict(), jexl.isStrict()));
            final JexlArithmetic jexla = jexl.getArithmetic();
            jexlo.setStrictArithmetic(option(opts.isStrictArithmetic(), jexla.isStrict()));
            jexlo.setMathContext(opts.getArithmeticMathContext());
            jexlo.setMathScale(opts.getArithmeticMathScale());
            return jexlo;
        }
        return options;
    }

    /**
     * Compute a script options for evaluation.
     * <p>This calls processPragma(...).
     * @param script the script
     * @param context the context
     * @return the options
     */
    protected JexlOptions options(final ASTJexlScript script, final JexlContext context) {
        final JexlOptions opts = options(context);
        if (opts != options) {
            // when feature lexical, try hard to run lexical
            if (scriptFeatures.isLexical()) {
                opts.setLexical(true);
            }
            if (scriptFeatures.isLexicalShade()) {
                opts.setLexicalShade(true);
            }
        }
        if (script != null) {
           // process script pragmas if any
           processPragmas(script, context, opts);
        }
        return opts;
    }

    /**
     * Processes a script pragmas.
     * <p>Only called from options(...)
     * @param script the script
     * @param context the context
     * @param opts the options
     */
    protected void processPragmas(final ASTJexlScript script, final JexlContext context, final JexlOptions opts) {
        final Map<String, Object> pragmas = script.getPragmas();
        if (pragmas != null && !pragmas.isEmpty()) {
            final JexlContext.PragmaProcessor processor =
                    context instanceof JexlContext.PragmaProcessor
                    ? (JexlContext.PragmaProcessor) context
                    : null;
            Map<String, Object> ns = null;
            for(final Map.Entry<String, Object> pragma : pragmas.entrySet()) {
                final String key = pragma.getKey();
                final Object value = pragma.getValue();
                if (value instanceof String) {
                    if (PRAGMA_OPTIONS.equals(key)) {
                        // jexl.options
                        final String[] vs = value.toString().split(" ");
                        opts.setFlags(vs);
                    } else if (key.startsWith(PRAGMA_JEXLNS)) {
                        // jexl.namespace.***
                        final String nsname = key.substring(PRAGMA_JEXLNS.length());
                        if (nsname != null && !nsname.isEmpty()) {
                            if (ns == null) {
                                ns = new HashMap<>(functions);
                            }
                            final String nsclass = value.toString();
                            try {
                                ns.put(nsname, uberspect.getClassLoader().loadClass(nsclass));
                            } catch (final ClassNotFoundException e) {
                                ns.put(nsname, nsclass);
                            }
                        }
                    }
                }
                if (processor != null) {
                    processor.processPragma(key, value);
                }
            }
            if (ns != null) {
                opts.setNamespaces(ns);
            }
        }
    }

    /**
     * Sets options from this engine options.
     * @param opts the options to set
     * @return the options
     */
    public JexlOptions optionsSet(final JexlOptions opts) {
        if (opts != null) {
            opts.set(options);
        }
        return opts;
    }

    @Override
    public TemplateEngine createJxltEngine(final boolean noScript, final int cacheSize, final char immediate, final char deferred) {
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
     * @param opts    the evaluation options
     * @return an Interpreter
     */
    protected Interpreter createInterpreter(final JexlContext context, final Frame frame, final JexlOptions opts) {
        return new Interpreter(this, opts, context, frame);
    }


    @Override
    public Script createExpression(final JexlInfo info, final String expression) {
        return createScript(expressionFeatures, info, expression, null);
    }

    @Override
    public Script createScript(final JexlFeatures features, final JexlInfo info, final String scriptText, final String... names) {
        if (scriptText == null) {
            throw new NullPointerException("source is null");
        }
        final String source = trimSource(scriptText);
        final Scope scope = names == null || names.length == 0? null : new Scope(null, names);
        final JexlFeatures ftrs = features == null? scriptFeatures : features;
        final ASTJexlScript tree = parse(info, ftrs, source, scope);
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
    public Object getProperty(final Object bean, final String expr) {
        return getProperty(null, bean, expr);
    }

    @Override
    public Object getProperty(JexlContext context, final Object bean, final String expr) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        // synthesize expr using register
        String src = trimSource(expr);
        src = "#0" + (src.charAt(0) == '[' ? "" : ".") + src;
        try {
            final Scope scope = new Scope(null, "#0");
            final ASTJexlScript script = parse(null, PROPERTY_FEATURES, src, scope);
            final JexlNode node = script.jjtGetChild(0);
            final Frame frame = script.createFrame(bean);
            final Interpreter interpreter = createInterpreter(context, frame, options);
            return interpreter.visitLexicalNode(node, null);
        } catch (final JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl.clean();
        }
    }

    @Override
    public void setProperty(final Object bean, final String expr, final Object value) {
        setProperty(null, bean, expr, value);
    }

    @Override
    public void setProperty(JexlContext context, final Object bean, final String expr, final Object value) {
        if (context == null) {
            context = EMPTY_CONTEXT;
        }
        // synthesize expr using register
        String src = trimSource(expr);
        src = "#0" + (src.charAt(0) == '[' ? "" : ".") + src + "=" + "#1";
        try {
            final Scope scope = new Scope(null, "#0", "#1");
            final ASTJexlScript script = parse(null, PROPERTY_FEATURES, src, scope);
            final JexlNode node = script.jjtGetChild(0);
            final Frame frame = script.createFrame(bean, value);
            final Interpreter interpreter = createInterpreter(context, frame, options);
            interpreter.visitLexicalNode(node, null);
        } catch (final JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return;
            }
            throw xjexl.clean();
        }
    }

    @Override
    public Object invokeMethod(final Object obj, final String meth, final Object... args) {
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
                xjexl = new JexlException.Method(info, meth, args);
            }
        } catch (final JexlException xany) {
            xjexl = xany;
        } catch (final Exception xany) {
            xjexl = new JexlException.Method(info, meth, args, xany);
        }
        if (xjexl != null) {
            if (!silent) {
                throw xjexl.clean();
            }
            logger.warn(xjexl.getMessage(), xjexl.getCause());
            result = null;
        }
        return result;
    }

    @Override
    public <T> T newInstance(final Class<? extends T> clazz, final Object... args) {
        return clazz.cast(doCreateInstance(clazz, args));
    }

    @Override
    public Object newInstance(final String clazz, final Object... args) {
        return doCreateInstance(clazz, args);
    }

    /**
     * Creates a new instance of an object using the most appropriate constructor
     * based on the arguments.
     * @param clazz the class to instantiate
     * @param args  the constructor arguments
     * @return the created object instance or null on failure when silent
     */
    protected Object doCreateInstance(final Object clazz, final Object... args) {
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
                xjexl = new JexlException.Method(info, clazz.toString(), args);
            }
        } catch (final JexlException xany) {
            xjexl = xany;
        } catch (final Exception xany) {
            xjexl = new JexlException.Method(info, clazz.toString(), args, xany);
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
    protected JexlContext.ThreadLocal putThreadLocal(final JexlContext.ThreadLocal tls) {
        final JexlContext.ThreadLocal local = CONTEXT.get();
        CONTEXT.set(tls);
        return local;
    }

    /**
     * Swaps the current thread local engine.
     * @param jexl the engine or null
     * @return the previous thread local engine
     */
    protected JexlEngine putThreadEngine(final JexlEngine jexl) {
        final JexlEngine pjexl = ENGINE.get();
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
    protected Set<List<String>> getVariables(final ASTJexlScript script) {
        final VarCollector collector = varCollector();
        getVariables(script, script, collector);
        return collector.collected();
    }

    /**
     * Creates a collector instance.
     * @return a collector instance
     */
    protected VarCollector varCollector() {
        return new VarCollector(this.collectMode);
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
         * Whether constant array-access is considered equivalent to dot-access;
         * if so, > 1 means collect any constant (set,map,...) instead of just
         * strings and numbers.
         */
        private int mode = 1;

        /**
         * Constructor.
         * @param constaa whether constant array-access is considered equivalent to dot-access
         */
        protected VarCollector(final int constaa) {
            mode = constaa;
        }

        /**
         * Starts/stops a variable collect.
         * @param node starts if not null, stop if null
         */
        public void collect(final JexlNode node) {
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
        public void add(final String name) {
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
    protected void getVariables(final ASTJexlScript script, final JexlNode node, final VarCollector collector) {
        if (node instanceof ASTIdentifier) {
            final JexlNode parent = node.jjtGetParent();
            if (parent instanceof ASTMethodNode || parent instanceof ASTFunctionNode) {
                // skip identifiers for methods and functions
                collector.collect(null);
                return;
            }
            final ASTIdentifier identifier = (ASTIdentifier) node;
            final int symbol = identifier.getSymbol();
            // symbols that are captured are considered "global" variables
            if (symbol >= 0 && script != null && !script.isCapturedSymbol(symbol)) {
                collector.collect(null);
            } else {
                // start collecting from identifier
                collector.collect(identifier);
                collector.add(identifier.getName());
            }
        } else if (node instanceof ASTIdentifierAccess) {
            final JexlNode parent = node.jjtGetParent();
            if (parent instanceof ASTMethodNode || parent instanceof ASTFunctionNode) {
                // skip identifiers for methods and functions
                collector.collect(null);
                return;
            }
            // belt and suspender since an identifier should have been seen first
            if (collector.isCollecting()) {
                collector.add(((ASTIdentifierAccess) node).getName());
            }
        } else if (node instanceof ASTArrayAccess && collector.mode > 0) {
            final int num = node.jjtGetNumChildren();
            // collect only if array access is const and follows an identifier
            boolean collecting = collector.isCollecting();
            for (int i = 0; i < num; ++i) {
                final JexlNode child = node.jjtGetChild(i);
                if (collecting && child.isConstant()) {
                    // collect all constants or only string and number literals
                    final boolean collect = collector.mode > 1
                            || (child instanceof ASTStringLiteral || child instanceof ASTNumberLiteral);
                    if (collect) {
                        final String image = child.toString();
                        collector.add(image);
                    }
                } else {
                    collecting = false;
                    collector.collect(null);
                    getVariables(script, child, collector);
                    collector.collect(null);
                }
            }
        } else {
            final int num = node.jjtGetNumChildren();
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
    protected String[] getParameters(final JexlScript script) {
        return script.getParameters();
    }

    /**
     * Gets the array of local variable from a script.
     * @param script the script
     * @return the local variables array which may be empty (but not null) if no local variables were defined
     * @since 3.0
     */
    protected String[] getLocalVariables(final JexlScript script) {
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
    protected ASTJexlScript parse(final JexlInfo info, final boolean expr, final String src, final Scope scope) {
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
    protected ASTJexlScript parse(final JexlInfo info, final JexlFeatures parsingf, final String src, final Scope scope) {
        final boolean cached = src.length() < cacheThreshold && cache != null;
        JexlFeatures features = parsingf != null? parsingf : DEFAULT_FEATURES;
       // if (features.getNameSpaces().isEmpty() && !functions.isEmpty()) {
       //     features = new JexlFeatures(features).nameSpaces(functions.keySet());
       // }
        final Source source = cached? new Source(features, src) : null;
        ASTJexlScript script = null;
        if (source != null) {
            script = cache.get(source);
            if (script != null) {
                final Scope f = script.getScope();
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
            final Parser lparser = new Parser(new StringProvider(";"));
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
     * @return trimmed expression ending in a semicolon
     */
    protected String trimSource(final CharSequence str) {
        if (str != null) {
            int start = 0;
            int end = str.length();
            if (end > 0) {
                // trim front spaces
                while (start < end && Character.isSpaceChar(str.charAt(start))) {
                    ++start;
                }
                // trim ending spaces; end is > 0 since start >= 0
                while (end > start && Character.isSpaceChar(str.charAt(end - 1))) {
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
