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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.IntFunction;

import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.internal.SoftCache;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.Log;

/**
 * Configures and builds a JexlEngine.
 *
 * <p>
 *     The builder allow fine-tuning an engine instance behavior according to various control needs.
 *     Check <em>{@link #JexlBuilder()}</em> for permission impacts starting with <em>JEXL 3.3</em>.
 * </p><p>
 *     Broad configurations elements are controlled through the features ({@link JexlFeatures}) that can restrict JEXL
 *  syntax - for instance, only expressions with no-side effects - and permissions ({@link JexlPermissions}) that control
 *  the visible set of objects - for instance, avoiding access to any object in java.rmi.* -.
 *  </p><p>
 *     Fine error control and runtime-overridable behaviors are implemented through options ({@link JexlOptions}). Most
 * common flags accessible from the builder are reflected in its options ({@link #options()}).
 * </p><p>
 *     The {@code silent} flag tells the engine what to do with the error; when true, errors are logged as
 * warning, when false, they throw {@link JexlException} exceptions.
 * </p><p>
 *     The {@code strict} flag tells the engine when and if null as operand is considered an error. The {@code safe}
 * flog determines if safe-navigation is used. Safe-navigation allows an  evaluation shortcut and return null in expressions
 * that attempts dereferencing null, typically a method call or accessing a property.
 * </p><p>
 *     The {@code lexical} and {@code lexicalShade} flags can be used to enforce a lexical scope for
 * variables and parameters. The {@code lexicalShade} can be used to further ensure no global variable can be
 * used with the same name as a local one even after it goes out of scope. The corresponding feature flags should be
 * preferred since they will detect violations at parsing time. (see {@link JexlFeatures})
 * </p><p>
 *     The following rules apply on silent and strict flags:
 * </p>
 * <ul>
 * <li>When "silent" &amp; "not-strict":
 * <p> 0 &amp; null should be indicators of "default" values so that even in an case of error,
 * something meaningful can still be inferred; may be convenient for configurations.
 * </p>
 * </li>
 * <li>When "silent" &amp; "strict":
 * <p>One should probably consider using null as an error case - ie, every object
 * manipulated by JEXL should be valued; the ternary operator, especially the '?:' form
 * can be used to workaround exceptional cases.
 * Use case could be configuration with no implicit values or defaults.
 * </p>
 * </li>
 * <li>When "not-silent" &amp; "not-strict":
 * <p>The error control grain is roughly on par with JEXL 1.0</p>
 * </li>
 * <li>When "not-silent" &amp; "strict":
 * <p>The finest error control grain is obtained; it is the closest to Java code -
 * still augmented by "script" capabilities regarding automated conversions and type matching.
 * </p>
 * </li>
 * </ul>
 */
public class JexlBuilder {
    /**
     * The set of default permissions used when creating a new builder.
     * <p>Static but modifiable so these default permissions can be changed to a purposeful set.</p>
     * <p>In JEXL 3.3, these are {@link JexlPermissions#RESTRICTED}.</p>
     * <p>In JEXL 3.2, these were equivalent to {@link JexlPermissions#UNRESTRICTED}.</p>
     */
    private static JexlPermissions PERMISSIONS = JexlPermissions.RESTRICTED;

    /** The default maximum expression length to hit the expression cache. */
    protected static final int CACHE_THRESHOLD = 64;

    /**
     * Sets the default permissions.
     * @param permissions the permissions
     */
    public static void setDefaultPermissions(final JexlPermissions permissions) {
        PERMISSIONS = permissions == null ? JexlPermissions.RESTRICTED : permissions;
    }

    /** The JexlUberspect instance. */
    private JexlUberspect uberspect;

    /** The {@link JexlUberspect} resolver strategy. */
    private JexlUberspect.ResolverStrategy strategy;

    /** The set of permissions. */
    private JexlPermissions permissions;

    /** The sandbox. */
    private JexlSandbox sandbox;

    /** The Log to which all JexlEngine messages will be logged. */
    private Log logger;

    /** Whether error messages will carry debugging information. */
    private Boolean debug;

    /** Whether interrupt throws JexlException.Cancel. */
    private Boolean cancellable;

    /** The options. */
    private final JexlOptions options = new JexlOptions();

    /** Whether getVariables considers all potential equivalent syntactic forms. */
    private int collectMode = 1;

    /** The {@link JexlArithmetic} instance. */
    private JexlArithmetic arithmetic;

    /** The cache size. */
    private int cache = -1;

    /** The cache class factory. */
    private IntFunction<JexlCache<?,?>> cacheFactory = SoftCache::new;

    /** The stack overflow limit. */
    private int stackOverflow = Integer.MAX_VALUE;

    /** The maximum expression length to hit the expression cache. */
    private int cacheThreshold = CACHE_THRESHOLD;

    /** The charset. */
    private Charset charset = Charset.defaultCharset();

    /** The class loader. */
    private ClassLoader loader;

    /** The features. */
    private JexlFeatures features;

    /**
     * Default constructor.
     * <p>
     * As of JEXL 3.3, to reduce the security risks inherent to JEXL&quot;s purpose, the builder will use a set of
     * restricted permissions as a default to create the {@link JexlEngine} instance. This will greatly reduce which classes
     * and methods are visible to JEXL and usable in scripts using default implicit behaviors.
     * </p><p>
     * However, without mitigation, this change will likely break some scripts at runtime, especially those exposing
     * your own class instances through arguments, contexts or namespaces.
     * The new default set of allowed packages and denied classes is described by {@link JexlPermissions#RESTRICTED}.
     * </p><p>
     * The recommended mitigation if your usage of JEXL is impacted is to first thoroughly review what should be
     * allowed and exposed to script authors and implement those through a set of {@link JexlPermissions};
     * those are easily created using {@link JexlPermissions#parse(String...)}.
     * </p><p>
     * In the urgent case of a strict 3.2 compatibility, the simplest and fastest mitigation is to use the 'unrestricted'
     * set of permissions. The builder must be explicit about it either by setting the default permissions with a
     * statement like {@code JexlBuilder.setDefaultPermissions(JexlPermissions.UNRESTRICTED);} or with a more precise
     * one like <code>new JexlBuilder().permissions({@link JexlPermissions#UNRESTRICTED})</code>.
     * </p><p>
     * Note that an explicit call to {@link #uberspect(JexlUberspect)} will supersede any permissions related behavior
     * by using the {@link JexlUberspect} provided as argument used as-is in the created {@link JexlEngine}.
     * </p>
     * @since 3.3
     */
    public JexlBuilder() {
        this.permissions = PERMISSIONS;
    }

    /** @return whether antish resolution is enabled */
    public boolean antish() {
        return options.isAntish();
    }

    /**
     * Sets whether the engine will resolve antish variable names.
     *
     * @param flag true means antish resolution is enabled, false disables it
     * @return this builder
     */
    public JexlBuilder antish(final boolean flag) {
        options.setAntish(flag);
        return this;
    }

    /** @return the arithmetic */
    public JexlArithmetic arithmetic() {
        return this.arithmetic;
    }

    /**
     * Sets the JexlArithmetic instance the engine will use.
     *
     * @param a the arithmetic
     * @return this builder
     */
    public JexlBuilder arithmetic(final JexlArithmetic a) {
        this.arithmetic = a;
        options.setStrictArithmetic(a.isStrict());
        options.setMathContext(a.getMathContext());
        options.setMathScale(a.getMathScale());
        return this;
    }

    /**
     * Sets whether logical expressions (&quot;&quot; , ||) coerce their result to boolean.
     * @param flag true or false
     * @return this builder
     */
    public JexlBuilder booleanLogical(final boolean flag) {
      options.setBooleanLogical(flag);
      return this;
    }

    /**
     * @return the cache size
     */
    public int cache() {
      return cache;
    }

    /**
     * Sets the expression cache size the engine will use.
     * <p>The cache will contain at most {@code size} expressions of at most {@code cacheThreshold} length.
     * Note that all JEXL caches are held through SoftReferences and may be garbage-collected.</p>
     *
     * @param size if not strictly positive, no cache is used.
     * @return this builder
     */
    public JexlBuilder cache(final int size) {
        this.cache = size;
        return this;
    }

    /**
     * @return the cache factory
     */
    public IntFunction<JexlCache<?, ?>> cacheFactory() {
      return this.cacheFactory;
    }

    /**
     * Sets the expression cache size the engine will use.
     *
     * @param factory the function to produce a cache.
     * @return this builder
     */
    public JexlBuilder cacheFactory(final IntFunction<JexlCache<?, ?>> factory) {
      this.cacheFactory = factory;
      return this;
    }

    /**
     * @return the cache threshold
     */
    public int cacheThreshold() {
        return cacheThreshold;
    }

    /**
     * Sets the maximum length for an expression to be cached.
     * <p>Expression whose length is greater than this expression cache length threshold will
     * bypass the cache.</p>
     * <p>It is expected that a "long" script will be parsed once and its reference kept
     * around in user-space structures; the jexl expression cache has no added-value in this case.</p>
     *
     * @param length if not strictly positive, the value is silently replaced by the default value (64).
     * @return this builder
     */
    public JexlBuilder cacheThreshold(final int length) {
        this.cacheThreshold = length > 0? length : CACHE_THRESHOLD;
        return this;
    }

    /**
     * @return the cancellable information flag
     * @since 3.1
     */
    public Boolean cancellable() {
        return this.cancellable;
    }

    /**
     * Sets the engine behavior upon interruption: throw an JexlException.Cancel or terminates the current evaluation
     * and return null.
     *
     * @param flag true implies the engine throws the exception, false makes the engine return null.
     * @return this builder
     * @since 3.1
     */
    public JexlBuilder cancellable(final boolean flag) {
        this.cancellable = flag;
        options.setCancellable(flag);
        return this;
    }

    /** @return the charset */
    public Charset charset() {
        return charset;
    }

    /**
     * Sets the charset to use.
     *
     * @param arg the charset
     * @return this builder
     * @since 3.1
     */
    public JexlBuilder charset(final Charset arg) {
        this.charset = arg;
        return this;
    }

    /**
     * @return true if variable collection follows strict syntactic rule
     * @since 3.2
     */
    public boolean collectAll() {
        return this.collectMode != 0;
    }

    /**
     * Sets whether the engine variable collectors considers all potential forms of variable syntaxes.
     *
     * @param flag true means var collections considers constant array accesses equivalent to dotted references
     * @return this builder
     * @since 3.2
     */
    public JexlBuilder collectAll(final boolean flag) {
        return collectMode(flag? 1 : 0);
    }

    /**
     * @return 0 if variable collection follows strict syntactic rule
     * @since 3.2
     */
    public int collectMode() {
        return this.collectMode;
    }

    /**
     * Experimental collector mode setter.
     *
     * @param mode 0 or 1 as equivalents to false and true, other values are experimental
     * @return this builder
     * @since 3.2
     */
    public JexlBuilder collectMode(final int mode) {
        this.collectMode = mode;
        return this;
    }

    /**
     * @return a {@link JexlEngine} instance
     */
    public JexlEngine create() {
        return new Engine(this);
    }

    /** @return the debugging information flag */
    public Boolean debug() {
        return this.debug;
    }

   /**
     * Sets whether the engine will report debugging information when error occurs.
     *
     * @param flag true implies debug is on, false implies debug is off.
     * @return this builder
     */
    public JexlBuilder debug(final boolean flag) {
        this.debug = flag;
        return this;
    }

    /** @return the features */
    public JexlFeatures features() {
        return this.features;
    }

    /**
     * Sets the features the engine will use as a base by default.
     * <p>Note that the script flag will be ignored; the engine will be able to parse expressions and scripts.
     * <p>Note also that these will apply to template expressions and scripts.
     * <p>As a last remark, if lexical or lexicalShade are set as features, this
     * method will also set the corresponding options.
     * @param f the features
     * @return this builder
     */
    public JexlBuilder features(final JexlFeatures f) {
        this.features = f;
        if (features != null) {
            if (features.isLexical()) {
                options.setLexical(true);
            }
            if (features.isLexicalShade()) {
                options.setLexicalShade(true);
            }
        }
        return this;
    }

    /**
     * Gets the optional set of imported packages.
     * @return the set of imports, may be empty, not null
     */
    public Collection<String> imports() {
        return options.getImports();
    }

    /**
     * Sets the optional set of imports.
     * @param imports the imported packages
     * @return this builder
     */
    public JexlBuilder imports(final Collection<String> imports) {
        options.setImports(imports);
        return this;
    }

    /**
     * Sets the optional set of imports.
     * @param imports the imported packages
     * @return this builder
     */
    public JexlBuilder imports(final String... imports) {
        return imports(Arrays.asList(imports));
    }

    /**
     * @see JexlOptions#isLexical()
     * @return whether lexical scope is enabled
     * @deprecated 3.4.1
     */
    @Deprecated
    public boolean lexical() {
        return options.isLexical();
    }

    /**
     * Sets whether the engine is in lexical mode.
     *
     * @param flag true means lexical function scope is in effect, false implies non-lexical scoping
     * @return this builder
     * @since 3.2
     */
    public JexlBuilder lexical(final boolean flag) {
        options.setLexical(flag);
        return this;
    }

    /**
     * @see JexlOptions#isLexicalShade()
     * @return whether lexical shading is enabled
     * @deprecated 3.4.1
     */
    @Deprecated
    public boolean lexicalShade() {
        return options.isLexicalShade();
    }

    /**
     * Sets whether the engine is in lexical shading mode.
     *
     * @param flag true means lexical shading is in effect, false implies no lexical shading
     * @return this builder
     * @since 3.2
     */
    public JexlBuilder lexicalShade(final boolean flag) {
        options.setLexicalShade(flag);
        return this;
    }

    /** @return the class loader */
    public ClassLoader loader() {
        return loader;
    }

    /**
     * Sets the charset to use.
     *
     * @param arg the charset
     * @return this builder
     * @deprecated since 3.1 use {@link #charset(Charset)} instead
     */
    @Deprecated
    public JexlBuilder loader(final Charset arg) {
        return charset(arg);
    }

    /**
     * Sets the class loader to use.
     *
     * @param l the class loader
     * @return this builder
     */
    public JexlBuilder loader(final ClassLoader l) {
        this.loader = l;
        return this;
    }

    /** @return the logger */
    public Log logger() {
        return this.logger;
    }

    /**
     * Sets the o.a.c.Log instance to use.
     *
     * @param log the logger
     * @return this builder
     */
    public JexlBuilder logger(final Log log) {
        this.logger = log;
        return this;
    }

    /**
     * @return the map of namespaces.
     */
    public Map<String, Object> namespaces() {
        return options.getNamespaces();
    }

    /**
     * Sets the default namespaces map the engine will use.
     * <p>
     * Each entry key is used as a prefix, each entry value used as a bean implementing
     * methods; an expression like 'nsx:method(123)' will thus be solved by looking at
     * a registered bean named 'nsx' that implements method 'method' in that map.
     * If all methods are static, you may use the bean class instead of an instance as value.
     * </p>
     * <p>
     * If the entry value is a class that has one constructor taking a JexlContext as argument, an instance
     * of the namespace will be created at evaluation time. It might be a good idea to derive a JexlContext
     * to carry the information used by the namespace to avoid variable space pollution and strongly type
     * the constructor with this specialized JexlContext.
     * </p>
     * <p>
     * The key or prefix allows to retrieve the bean that plays the role of the namespace.
     * If the prefix is null, the namespace is the top-level namespace allowing to define
     * top-level user-defined namespaces ( ie: myfunc(...) )
     * </p>
     * <p>Note that the JexlContext is also used to try to solve top-level namespaces. This allows ObjectContext
     * derived instances to call methods on the wrapped object.</p>
     *
     * @param ns the map of namespaces
     * @return this builder
     */
    public JexlBuilder namespaces(final Map<String, Object> ns) {
        options.setNamespaces(ns);
        return this;
    }

    /**
     * @return the current set of options
     */
    public JexlOptions options() {
      return options;
    }

    /** @return the permissions */
    public JexlPermissions permissions() {
        return this.permissions;
    }

    /**
     * Sets the JexlPermissions instance the engine will use.
     *
     * @param p the permissions
     * @return this builder
     */
    public JexlBuilder permissions(final JexlPermissions p) {
        this.permissions = p;
        return this;
    }

    /** @return true if safe, false otherwise */
    public Boolean safe() {
        return options.isSafe();
    }

    /**
     * Sets whether the engine considers dereferencing null in navigation expressions
     * as null or triggers an error.
     * <p>{@code x.y()} if x is null throws an exception when not safe,
     * return null and warns if it is.</p>
     * <p>It is recommended to use <em>safe(false)</em> as an explicit default.</p>
     *
     * @param flag true means safe navigation, false throws exception when dereferencing null
     * @return this builder
     */
    public JexlBuilder safe(final boolean flag) {
        options.setSafe(flag);
        return this;
    }

    /** @return the sandbox */
    public JexlSandbox sandbox() {
        return this.sandbox;
    }

    /**
     * Sets the sandbox the engine will use.
     *
     * @param box the sandbox
     * @return this builder
     */
    public JexlBuilder sandbox(final JexlSandbox box) {
        this.sandbox = box;
        return this;
    }

    /** @return the silent error handling flag */
    public Boolean silent() {
        return options.isSilent();
    }

    /**
     * Sets whether the engine will throw JexlException during evaluation when an error is triggered.
     * <p>When <em>not</em> silent, the engine throws an exception when the evaluation triggers an exception or an
     * error.</p>
     * <p>It is recommended to use <em>silent(true)</em> as an explicit default.</p>
     * @param flag true means no JexlException will occur, false allows them
     * @return this builder
     */
    public JexlBuilder silent(final boolean flag) {
        options.setSilent(flag);
        return this;
    }

    /**
     * @return the cache size
     */
    public int stackOverflow() {
        return stackOverflow;
    }

    /**
     * Sets the number of script/expression evaluations that can be stacked.
     * @param size if not strictly positive, limit is reached when Java StackOverflow is thrown.
     * @return this builder
     */
    public JexlBuilder stackOverflow(final int size) {
        this.stackOverflow = size;
        return this;
    }

    /** @return the JexlUberspect strategy */
    public JexlUberspect.ResolverStrategy strategy() {
        return this.strategy;
    }

    /**
     * Sets the JexlUberspect strategy the engine will use.
     * <p>This is ignored if the uberspect has been set.
     *
     * @param rs the strategy
     * @return this builder
     */
    public JexlBuilder strategy(final JexlUberspect.ResolverStrategy rs) {
        this.strategy = rs;
        return this;
    }

    /** @return true if strict, false otherwise */
    public Boolean strict() {
        return options.isStrict();
    }

    /**
     * Sets whether the engine considers unknown variables, methods, functions and constructors as errors or
     * evaluates them as null.
     * <p>When <em>not</em> strict, operators or functions using null operands return null on evaluation. When
     * strict, those raise exceptions.</p>
     * <p>It is recommended to use <em>strict(true)</em> as an explicit default.</p>
     *
     * @param flag true means strict error reporting, false allows them to be evaluated as null
     * @return this builder
     */
    public JexlBuilder strict(final boolean flag) {
        options.setStrict(flag);
        return this;
    }

    /**
     * @see JexlOptions#setStrictInterpolation(boolean)
     * @param flag strict interpolation flag
     * @return this builder
     */
    public JexlBuilder strictInterpolation(final boolean flag) {
        options.setStrictInterpolation(flag);
        return this;
    }

    /** @return the uberspect */
    public JexlUberspect uberspect() {
        return this.uberspect;
    }

    /**
     * Sets the JexlUberspect instance the engine will use.
     *
     * @param u the uberspect
     * @return this builder
     */
    public JexlBuilder uberspect(final JexlUberspect u) {
        this.uberspect = u;
        return this;
    }
}
