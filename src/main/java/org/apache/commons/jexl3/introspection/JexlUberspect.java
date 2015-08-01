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

package org.apache.commons.jexl3.introspection;

import org.apache.commons.jexl3.JexlArithmetic;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 'Federated' introspection/reflection interface to allow JEXL introspection
 *  behavior to be customized.
 *
 * @since 1.0
 */
public interface JexlUberspect {
    /**
     * The various property resolver types.
     * <p>
     * Each resolver type discovers how to set/get a property with different techniques; seeking
     * method names or field names, etc.
     * <p>
     * These are used through 'strategies' to solve properties; a strategy orders a list of resolver types,
     * and each resolver type is tried in sequence; the first resolver that discovers a non null {s,g}etter
     * stops the search.
     * @see ResolverStrategy
     * @see JexlUberspect#getPropertyGet
     * @see JexlUberspect#getPropertySet
     * @since 3.0
     */
    enum ResolverType {
        /**
         * Seeks methods named get{P,p}property and is{P,p}property.
         */
        PROPERTY,
        /**
         * Seeks map methods get/put.
         */
        MAP,
        /**
         * Seeks list methods get/set.
         */
        LIST,
        /**
         * Seeks any get/{set,put} method (quacking like a list or a map).
         */
        DUCK,
        /**
         * Seeks public instance members.
         */
        FIELD,
        /**
         * Seeks a getContainer(property) and setContainer(property, value)
         * as in <code>x.container.property</code>.
         */
        CONTAINER
    }

    /**
     * A resolver types list tailored for POJOs, favors '.' over '[]'.
     */
    List<ResolverType> POJO = Collections.unmodifiableList(Arrays.asList(
        ResolverType.PROPERTY,
        ResolverType.MAP,
        ResolverType.LIST,
        ResolverType.DUCK,
        ResolverType.FIELD,
        ResolverType.CONTAINER
    ));

    /**
     * A resolver types list tailored for Maps, favors '[]' over '.'.
     */
    List<ResolverType> MAP = Collections.unmodifiableList(Arrays.asList(
        ResolverType.MAP,
        ResolverType.LIST,
        ResolverType.DUCK,
        ResolverType.PROPERTY,
        ResolverType.FIELD,
        ResolverType.CONTAINER
     ));

    /**
     * Determine property resolution strategies.
     * <p>
     * To use a strategy instance, you have to set it at engine creation using
     * {@link org.apache.commons.jexl3.JexlBuilder#strategy(JexlUberspect.ResolverStrategy)}
     * as in:<br/>
     * <code>JexlEngine jexl = new JexlBuilder().strategy(MY_STRATEGY).create();</code>
     * @see ResolverType
     * @since 3.0
     */
    interface ResolverStrategy {
        /**
         * Applies this strategy to a list of resolver types.
         * <p>
         * <ul>In the default implementation, the resolvers argument depends on the calling situation:
         * <li>{@link #POJO} for dot operator resolution (foo.bar )</li>
         * <li>{@link #MAP} for bracket operator resolution (foo['bar'])</li>
         * <li>null when called from {@link #getPropertyGet(java.lang.Object, java.lang.Object) }
         * or {@link #getPropertySet(java.lang.Object, java.lang.Object, java.lang.Object)}</li>
         * </ul>
         *
         * @param resolvers candidate resolver types list
         * @param obj the instance we seek to obtain a property setter/getter from, can not be null
         * @return the ordered list of resolvers types, must not be null
         */
        List<ResolverType> apply(List<ResolverType> resolvers, Object obj);
    }

    /**
     * The default strategy.
     * <p>
     * If the resolvers list is not null, use that list.
     * Otherwise, if the object is a map, use the MAP list, otherwise use the POJO list.
     */
    ResolverStrategy JEXL_STRATEGY = new ResolverStrategy() {
        @Override
        public List<ResolverType> apply(List<ResolverType> resolvers, Object obj) {
            return resolvers != null ? resolvers : obj instanceof Map? JexlUberspect.MAP : JexlUberspect.POJO;
        }
    };

    /**
     * The map strategy.
     * <p>
     * If the object is a map, use the MAP list.
     * Otherwise, if the resolvers list is not null, use that list, otherwise use the POJO list.
     */
    ResolverStrategy MAP_STRATEGY = new ResolverStrategy() {
        @Override
        public List<ResolverType> apply(List<ResolverType> strategy, Object obj) {
            return obj instanceof Map? JexlUberspect.MAP : strategy != null ? strategy : JexlUberspect.POJO;
        }
    };

    /**
     * Sets the class loader to use.
     * <p>This increments the version.</p>
     * @param loader the class loader
     */
    void setClassLoader(ClassLoader loader);

    /**
     * Gets this uberspect version.
     * @return the class loader modification count
     */
    int getVersion();

    /**
     * Returns a class constructor.
     * @param ctorHandle a class or class name
     * @param args constructor arguments
     * @return a {@link JexlMethod}
     * @since 3.0
     */
    JexlMethod getConstructor(Object ctorHandle, Object... args);

    /**
     * Returns a JexlMethod.
     * @param obj the object
     * @param method the method name
     * @param args method arguments
     * @return a {@link JexlMethod}
     */
    JexlMethod getMethod(Object obj, String method, Object... args);

    /**
     * Property getter.
     * <p>returns a JelPropertySet apropos to an expression like <code>bar.woogie</code>.</p>
     * @param obj the object to get the property from
     * @param identifier property name
     * @return a {@link JexlPropertyGet} or null
     */
    JexlPropertyGet getPropertyGet(Object obj, Object identifier);

    /**
     * Property getter.
     * <p>Seeks a JexlPropertyGet apropos to an expression like <code>bar.woogie</code>.</p>
     * @param resolvers  the list of resolver types,
     *                   argument to {@link ResolverStrategy#apply(java.util.List, java.lang.Object) }
     * @param obj        the object to get the property from,
     *                   argument to {@link ResolverStrategy#apply(java.util.List, java.lang.Object) }
     * @param identifier property name
     * @return a {@link JexlPropertyGet} or null
     * @since 3.0
     */
    JexlPropertyGet getPropertyGet(List<ResolverType> resolvers, Object obj, Object identifier);

    /**
     * Property setter.
     * <p>Seeks a JelPropertySet apropos to an expression like  <code>foo.bar = "geir"</code>.</p>
     * @param obj the object to get the property from.
     * @param identifier property name
     * @param arg value to set
     * @return a {@link JexlPropertySet} or null
     */
    JexlPropertySet getPropertySet(Object obj, Object identifier, Object arg);

    /**
     * Property setter.
     * <p>Seeks a JelPropertySet apropos to an expression like <code>foo.bar = "geir"</code>.</p>
     * @param resolvers  the list of resolver types,
     *                   argument to {@link ResolverStrategy#apply(java.util.List, java.lang.Object) }
     * @param obj        the object to get the property from,
     *                   argument to {@link ResolverStrategy#apply(java.util.List, java.lang.Object) }
     * @param identifier property name
     * @param arg        value to set
     * @return a {@link JexlPropertySet} or null
     * @since 3.0
     */
    JexlPropertySet getPropertySet(List<ResolverType> resolvers, Object obj, Object identifier, Object arg);

    /**
     * Gets an iterator from an object.
     * @param obj to get the iterator from
     * @return an iterator over obj or null
     */
    Iterator<?> getIterator(Object obj);

    /**
     * Gets an arithmetic operator resolver for a given arithmetic instance.
     * @param arithmetic the arithmetic instance
     * @return the arithmetic uberspect or null if no operator method were overridden
     * @since 3.0
     */
    JexlArithmetic.Uberspect getArithmetic(JexlArithmetic arithmetic);

}