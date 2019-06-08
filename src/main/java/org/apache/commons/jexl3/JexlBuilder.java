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

import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.Log;

import java.util.Map;
import java.nio.charset.Charset;

/**
 * Configure and builds a JexlEngine.
 *
 * <p>The <code>setSilent</code> and <code>setStrict</code> methods allow to fine-tune an engine instance behavior
 * according to various error control needs. The strict flag tells the engine when and if null as operand is
 * considered an error, the silent flag tells the engine what to do with the error
 * (log as warning or throw exception).</p>
 *
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

    /** The default maximum expression length to hit the expression cache. */
    protected static final int CACHE_THRESHOLD = 64;

    /** The JexlUberspect instance. */
    private JexlUberspect uberspect = null;

    /** The strategy strategy. */
    private JexlUberspect.ResolverStrategy strategy = null;

    /** The sandbox. */
    private JexlSandbox sandbox = null;

    /** The Log to which all JexlEngine messages will be logged. */
    private Log logger = null;

    /**
     * Whether expressions evaluated by this engine will throw exceptions (false) or
     * return null (true) on errors. Default is false.
     */
    private Boolean silent = null;

    /** Whether this engine is in lenient or strict mode; if unspecified, use the arithmetic lenient property. */
    private Boolean strict = null;

    /** Whether this engine is in tolerant mode. */
    private Boolean safe = false;

    /** Whether error messages will carry debugging information. */
    private Boolean debug = null;

    /** Whether interrupt throws JexlException.Cancel. */
    private Boolean cancellable = null;

    /** Whether getVariables considers all potential equivalent syntactic forms. */
    private Boolean collectAll = null;

    /** The map of 'prefix:function' to object implementing the namespaces. */
    private Map<String, Object> namespaces = null;

    /** The {@link JexlArithmetic} instance. */
    private JexlArithmetic arithmetic = null;

    /** The cache size. */
    private int cache = -1;

    /** The stack overflow limit. */
    private int stackOverflow = Integer.MAX_VALUE;

    /** The maximum expression length to hit the expression cache. */
    private int cacheThreshold = CACHE_THRESHOLD;

    /** The charset. */
    private Charset charset = Charset.defaultCharset();

    /** The class loader. */
    private ClassLoader loader = null;

    /** The features. */
    private JexlFeatures features = null;

    /**
     * Sets the JexlUberspect instance the engine will use.
     *
     * @param u the uberspect
     * @return this builder
     */
    public JexlBuilder uberspect(JexlUberspect u) {
        this.uberspect = u;
        return this;
    }

    /** @return the uberspect */
    public JexlUberspect uberspect() {
        return this.uberspect;
    }

    /**
     * Sets the JexlUberspect strategy strategy the engine will use.
     * <p>This is ignored if the uberspect has been set.
     *
     * @param rs the strategy
     * @return this builder
     */
    public JexlBuilder strategy(JexlUberspect.ResolverStrategy rs) {
        this.strategy = rs;
        return this;
    }

    /** @return the strategy strategy */
    public JexlUberspect.ResolverStrategy strategy() {
        return this.strategy;
    }

    /**
     * Sets the JexlArithmetic instance the engine will use.
     *
     * @param a the arithmetic
     * @return this builder
     */
    public JexlBuilder arithmetic(JexlArithmetic a) {
        this.arithmetic = a;
        return this;
    }

    /** @return the arithmetic */
    public JexlArithmetic arithmetic() {
        return this.arithmetic;
    }

    /**
     * Sets the sandbox the engine will use.
     *
     * @param box the sandbox
     * @return this builder
     */
    public JexlBuilder sandbox(JexlSandbox box) {
        this.sandbox = box;
        return this;
    }

    /** @return the sandbox */
    public JexlSandbox sandbox() {
        return this.sandbox;
    }

    /**
     * Sets the features the engine will use as a base by default.
     * <p>Note that the script flag will be ignored; the engine will be able to parse expressions and scripts.
     * <p>Note also that these will apply to template expressions and scripts.
     * @param f the features
     * @return this builder
     */
    public JexlBuilder features(JexlFeatures f) {
        this.features = f;
        return this;
    }

    /** @return the features */
    public JexlFeatures features() {
        return this.features;
    }

    /**
     * Sets the o.a.c.Log instance to use.
     *
     * @param l the logger
     * @return this builder
     */
    public JexlBuilder logger(Log l) {
        this.logger = l;
        return this;
    }

    /** @return the logger */
    public Log logger() {
        return this.logger;
    }

    /**
     * Sets the class loader to use.
     *
     * @param l the class loader
     * @return this builder
     */
    public JexlBuilder loader(ClassLoader l) {
        this.loader = l;
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
    public JexlBuilder loader(Charset arg) {
        return charset(arg);
    }

    /**
     * Sets the charset to use.
     *
     * @param arg the charset
     * @return this builder
     * @since 3.1
     */
    public JexlBuilder charset(Charset arg) {
        this.charset = arg;
        return this;
    }

    /** @return the charset */
    public Charset charset() {
        return charset;
    }

    /**
     * Sets whether the engine will throw JexlException during evaluation when an error is triggered.
     *
     * @param flag true means no JexlException will occur, false allows them
     * @return this builder
     */
    public JexlBuilder silent(boolean flag) {
        this.silent = flag;
        return this;
    }

    /** @return the silent error handling flag */
    public Boolean silent() {
        return this.silent;
    }

    /**
     * Sets whether the engine considers unknown variables, methods, functions and constructors as errors or
     * evaluates them as null.
     *
     * @param flag true means strict error reporting, false allows them to be evaluated as null
     * @return this builder
     */
    public JexlBuilder strict(boolean flag) {
        this.strict = flag;
        return this;
    }

    /** @return true if strict, false otherwise */
    public Boolean strict() {
        return this.strict;
    }

    /**
     * Sets whether the engine considers dereferencing null in navigation expressions
     * as errors or evaluates them as null.
     * <p><code>x.y()</code> if x is null throws an exception when not safe,
     * return null and warns if it is.<p>
     *
     * @param flag true means safe navigation, false throws exception when dereferencing null
     * @return this builder
     */
    public JexlBuilder safe(boolean flag) {
        this.safe = flag;
        return this;
    }

    /** @return true if safe, false otherwise */
    public Boolean safe() {
        return this.safe;
    }

    /**
     * Sets whether the engine will report debugging information when error occurs.
     *
     * @param flag true implies debug is on, false implies debug is off.
     * @return this builder
     */
    public JexlBuilder debug(boolean flag) {
        this.debug = flag;
        return this;
    }

    /** @return the debugging information flag */
    public Boolean debug() {
        return this.debug;
    }

    /**
     * Sets the engine behavior upon interruption: throw an JexlException.Cancel or terminates the current evaluation
     * and return null.
     *
     * @param flag true implies the engine throws the exception, false makes the engine return null.
     * @return this builder
     * @since 3.1
     */
    public JexlBuilder cancellable(boolean flag) {
        this.cancellable = flag;
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
     * Sets whether the engine variable collectors considers all potential forms of variable syntaxes.
     *
     * @param flag true means var collections considers constant array accesses equivalent to dotted references
     * @return this builder
     */
    public JexlBuilder collectAll(boolean flag) {
        this.collectAll = flag;
        return this;
    }

    /** @return true if collect all, false otherwise */
    public Boolean collectAll() {
        return this.collectAll;
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
     * If the entry value is a class that has one contructor taking a JexlContext as argument, an instance
     * of the namespace will be created at evaluation time. It might be a good idea to derive a JexlContext
     * to carry the information used by the namespace to avoid variable space pollution and strongly type
     * the constructor with this specialized JexlContext.
     * </p>
     * <p>
     * The key or prefix allows to retrieve the bean that plays the role of the namespace.
     * If the prefix is null, the namespace is the top-level namespace allowing to define
     * top-level user defined namespaces ( ie: myfunc(...) )
     * </p>
     * <p>Note that the JexlContext is also used to try to solve top-level namespaces. This allows ObjectContext
     * derived instances to call methods on the wrapped object.</p>
     *
     * @param ns the map of namespaces
     * @return this builder
     */
    public JexlBuilder namespaces(Map<String, Object> ns) {
        this.namespaces = ns;
        return this;
    }

    /**
     * @return the map of namespaces.
     */
    public Map<String, Object> namespaces() {
        return this.namespaces;
    }

    /**
     * Sets the expression cache size the engine will use.
     * <p>The cache will contain at most <code>size</code> expressions of at most <code>cacheThreshold</code> length.
     * Note that all JEXL caches are held through SoftReferences and may be garbage-collected.</p>
     *
     * @param size if not strictly positive, no cache is used.
     * @return this builder
     */
    public JexlBuilder cache(int size) {
        this.cache = size;
        return this;
    }

    /**
     * @return the cache size
     */
    public int cache() {
        return cache;
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
    public JexlBuilder cacheThreshold(int length) {
        this.cacheThreshold = length > 0? length : CACHE_THRESHOLD;
        return this;
    }

    /**
     * @return the cache threshold
     */
    public int cacheThreshold() {
        return cacheThreshold;
    }

    /**
     * Sets the number of script/expression evaluations that can be stacked.
     * @param size if not strictly positive, limit is reached when java StackOverflow is thrown.
     * @return this builder
     */
    public JexlBuilder stackOverflow(int size) {
        this.stackOverflow = size;
        return this;
    }

    /**
     * @return the cache size
     */
    public int stackOverflow() {
        return stackOverflow;
    }

    /**
     * @return a {@link JexlEngine} instance
     */
    public JexlEngine create() {
        return new Engine(this);
    }
}
