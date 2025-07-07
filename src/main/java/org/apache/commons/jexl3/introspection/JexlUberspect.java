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

package org.apache.commons.jexl3.introspection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlOperator;

/**
 * 'Federated' introspection/reflection interface to allow JEXL introspection
 * behavior to be customized.
 *
 * @since 1.0
 */
public interface JexlUberspect {
    /**
     * The various builtin property resolvers.
     * <p>
     * Each resolver discovers how to set/get a property with different techniques; seeking
     * method names or field names, etc.
     *
     * @since 3.0
     */
    enum JexlResolver implements PropertyResolver {
        /** Seeks methods named get{P,p}property and is{P,p}property. */
        PROPERTY,

        /** Seeks map methods get/put. */
        MAP,

        /** Seeks list methods to get/set. */
        LIST,

        /** Seeks any get/{set,put} method (quacking like a list or a map). */
        DUCK,

        /**  Seeks public instance members.*/
        FIELD,

        /** Seeks a getContainer(property) and setContainer(property, value) as in {@code x.container.property}. */
        CONTAINER;

        @Override
        public final JexlPropertyGet getPropertyGet(final JexlUberspect uber,
                                                    final Object obj,
                                                    final Object identifier) {
            return uber.getPropertyGet(Collections.singletonList(this), obj, identifier);
        }

        @Override
        public final JexlPropertySet getPropertySet(final JexlUberspect uber,
                                                    final Object obj,
                                                    final Object identifier,
                                                    final Object arg) {
            return uber.getPropertySet(Collections.singletonList(this), obj, identifier, arg);
        }
    }

    /**
     * A marker interface that solves a simple class name into a fully qualified one.
     * <p>The base implementation uses imports.</p>
     * @since 3.3
     */
    interface ClassNameResolver {
        /**
         * Resolves a class name.
         * @param name the simple class name
         * @return the fully qualified class name
         */
        String resolveClassName(String name);
    }

    /**
     * A marker interface that solves a class constant by name.
     * <p>The base implementation uses imports to solve enums and public static final fields.</p>
     * @since 3.6
     */
    interface ClassConstantResolver extends ClassNameResolver {
        /**
         * Resolves a constant by its name.
         * @param name the constant name, a qualified name
         * @return the constant value or TRY_FAILED if not found
         */
        Object resolveConstant(String name);
    }

    /**
     * The factory type for creating constant resolvers.
     */
    interface ConstantResolverFactory {
        /**
         * Creates a constant resolver.
         * @param imports the collection of imports (packages and classes) to use
         * @return a constant resolver
         */
        ClassConstantResolver createConstantResolver(Collection<String> imports);
    }

    /**
     * Abstracts getting property setter and getter.
     * <p>
     * These are used through 'strategies' to solve properties; a strategy orders a list of resolver types,
     * and each resolver type is tried in sequence; the first resolver that discovers a non-null {s,g}etter
     * stops the search.
     *
     * @see JexlResolver
     * @see JexlUberspect#getPropertyGet
     * @see JexlUberspect#getPropertySet
     * @since 3.0
     */
    interface PropertyResolver {

        /**
         * Gets a property getter.
         *
         * @param uber       the uberspect
         * @param obj        the object
         * @param identifier the property identifier
         * @return the property getter or null
         */
        JexlPropertyGet getPropertyGet(JexlUberspect uber, Object obj, Object identifier);

        /**
         * Gets a property setter.
         *
         * @param uber       the uberspect
         * @param obj        the object
         * @param identifier the property identifier
         * @param arg        the property value
         * @return the property setter or null
         */
        JexlPropertySet getPropertySet(JexlUberspect uber, Object obj, Object identifier, Object arg);
    }

    /**
     * Determines property resolution strategy.
     *
     * <p>To use a strategy, you have to set it at engine creation using
     * {@link org.apache.commons.jexl3.JexlBuilder#strategy(JexlUberspect.ResolverStrategy)}
     * as in:</p>
     *
     * {@code JexlEngine jexl = new JexlBuilder().strategy(MY_STRATEGY).create();}
     *
     * @since 3.0
     */
    interface ResolverStrategy {
        /**
         * Applies this strategy to a list of resolver types.
         *
         * @param operator the property access operator, can be null
         * @param obj      the instance we seek to obtain a property setter/getter from, cannot be null
         * @return the ordered list of resolver types, cannot be null
         */
        List<PropertyResolver> apply(JexlOperator operator, Object obj);
    }

    /**
     * A resolver types list tailored for POJOs, favors '.' over '[]'.
     */
    List<PropertyResolver> POJO = Collections.unmodifiableList(Arrays.asList(
            JexlResolver.PROPERTY,
            JexlResolver.MAP,
            JexlResolver.LIST,
            JexlResolver.DUCK,
            JexlResolver.FIELD,
            JexlResolver.CONTAINER
    ));

    /**
     * A resolver types list tailored for Maps, favors '[]' over '.'.
     */
    List<PropertyResolver> MAP = Collections.unmodifiableList(Arrays.asList(
            JexlResolver.MAP,
            JexlResolver.LIST,
            JexlResolver.DUCK,
            JexlResolver.PROPERTY,
            JexlResolver.FIELD,
            JexlResolver.CONTAINER
    ));

    /**
     * The default strategy.
     * <p>
     * If the operator is '[]' or if the operator is null and the object is a map, use the MAP list of resolvers;
     * Other cases use the POJO list of resolvers.
     */
    ResolverStrategy JEXL_STRATEGY = (op, obj) -> {
        if (op == JexlOperator.ARRAY_GET) {
            return MAP;
        }
        if (op == JexlOperator.ARRAY_SET) {
            return MAP;
        }
        if (op == null && obj instanceof Map) {
            return MAP;
        }
        return POJO;
    };

    /**
     * The map strategy.
     *
     * <p>If the operator is '[]' or if the object is a map, use the MAP list of resolvers.
     * Otherwise, use the POJO list of resolvers.</p>
     */
    ResolverStrategy MAP_STRATEGY = (op, obj) -> {
        if (op == JexlOperator.ARRAY_GET) {
            return MAP;
        }
        if (op == JexlOperator.ARRAY_SET) {
            return MAP;
        }
        if (obj instanceof Map) {
            return MAP;
        }
        return POJO;
    };

    /**
     * Gets an arithmetic operator resolver for a given arithmetic instance.
     *
     * @param arithmetic the arithmetic instance
     * @return the arithmetic uberspect or null if no operator method were overridden
     * @since 3.0
     * @see #getOperator(JexlArithmetic)
     */
    JexlArithmetic.Uberspect getArithmetic(JexlArithmetic arithmetic);

    /**
     * Gets an arithmetic operator executor for a given arithmetic instance.
     *
     * @param arithmetic the arithmetic instance
     * @return an operator uberspect instance
     * @since 3.5.0
     */
    default JexlOperator.Uberspect getOperator(final JexlArithmetic arithmetic) {
        return null;
    }

    /**
     * Seeks a class by name using this uberspect class-loader.
     * @param className the class name
     * @return the class instance or null if the class cannot be located by this uberspect class loader or if
     * permissions deny access to the class
     */
    default Class<?> getClassByName(final String className) {
        try {
            return Class.forName(className, false, getClassLoader());
        } catch (final ClassNotFoundException ignore) {
            return null;
        }
    }

    /**
     * Gets the current class loader.
     * @return the class loader
     */
    ClassLoader getClassLoader();

    /**
     * Returns a class constructor.
     *
     * @param ctorHandle a class or class name
     * @param args       constructor arguments
     * @return a {@link JexlMethod}
     * @since 3.0
     */
    JexlMethod getConstructor(Object ctorHandle, Object... args);

    /**
     * Gets an iterator from an object.
     *
     * @param obj to get the iterator from
     * @return an iterator over obj or null
     */
    Iterator<?> getIterator(Object obj);

    /**
     * Returns a JexlMethod.
     *
     * @param obj    the object
     * @param method the method name
     * @param args   method arguments
     * @return a {@link JexlMethod}
     */
    JexlMethod getMethod(Object obj, String method, Object... args);

    /**
     * Property getter.
     * <p>
     * Seeks a JexlPropertyGet apropos to an expression like {@code bar.woogie}.</p>
     * See {@link ResolverStrategy#apply(JexlOperator, java.lang.Object)}
     *
     * @param resolvers  the list of property resolvers to try
     * @param obj        the object to get the property from
     * @param identifier property name
     * @return a {@link JexlPropertyGet} or null
     * @since 3.0
     */
    JexlPropertyGet getPropertyGet(List<PropertyResolver> resolvers, Object obj, Object identifier);

    /**
     * Property getter.
     *
     * <p>returns a JelPropertySet apropos to an expression like {@code bar.woogie}.</p>
     *
     * @param obj        the object to get the property from
     * @param identifier property name
     * @return a {@link JexlPropertyGet} or null
     */
    JexlPropertyGet getPropertyGet(Object obj, Object identifier);

    /**
     * Property setter.
     * <p>
     * Seeks a JelPropertySet apropos to an expression like {@code foo.bar = "geir"}.</p>
     * See {@link ResolverStrategy#apply(JexlOperator, java.lang.Object)}
     *
     * @param resolvers  the list of property resolvers to try,
     * @param obj        the object to get the property from
     * @param identifier property name
     * @param arg        value to set
     * @return a {@link JexlPropertySet} or null
     * @since 3.0
     */
    JexlPropertySet getPropertySet(List<PropertyResolver> resolvers, Object obj, Object identifier, Object arg);

    /**
     * Property setter.
     * <p>
     * Seeks a JelPropertySet apropos to an expression like  {@code foo.bar = "geir"}.</p>
     *
     * @param obj        the object to get the property from.
     * @param identifier property name
     * @param arg        value to set
     * @return a {@link JexlPropertySet} or null
     */
    JexlPropertySet getPropertySet(Object obj, Object identifier, Object arg);

    /**
     * Applies this uberspect property resolver strategy.
     *
     * @param op the operator
     * @param obj the object
     * @return the applied strategy resolver list
     */
    List<PropertyResolver> getResolvers(JexlOperator op, Object obj);

    /**
     * Gets this uberspect version.
     *
     * @return the class loader modification count
     */
    int getVersion();

    /**
     * Sets the class loader to use.
     *
     * <p>This increments the version.</p>
     *
     * @param loader the class loader
     */
    void setClassLoader(ClassLoader loader);

}
