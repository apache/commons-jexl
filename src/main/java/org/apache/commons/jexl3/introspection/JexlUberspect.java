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

/**
 * 'Federated' introspection/reflection interface to allow JEXL introspection
 *  behavior to be customized.
 *
 * @since 1.0
 */
public interface JexlUberspect {
    /**
     * The various property resolver types.
     * <p>These are used to compose 'strategies' to solve properties; a strategy is an array (list) of resolver types.
     * Each resolver type discovers how to set/get a property with different techniques; seeking
     * method names or field names, etc.
     * In a strategy, these are tried in sequence and the first non-null resolver stops the search.
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
     * A resolver strategy tailored for POJOs, favors '.' over '[]'.
     * This is the default strategy for getPropertyGet/getPropertySet.
     * @see JexlUberspect#getPropertyGet
     * @see JexlUberspect#getPropertySet
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
     * A resolver strategy tailored for Maps, favors '[]' over '.'.
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
     * Gets the strategy to apply for resolving properties.
     * <p>Default behavior is to use POJO if db is true, MAP if db is false.
     * @param db access operator flag, true for dot ('.' ) or false for bracket ('[]')
     * @param clazz the property owner class
     * @return the strategy
     */
    List<ResolverType> getStrategy(boolean db, Class<?> clazz);

    /**
     * Property getter.
     * <p>Seeks a JexlPropertyGet apropos to an expression like <code>bar.woogie</code>.</p>
     * @param strategy  the ordered list of resolver types, must not be null
     * @param obj the object to get the property from
     * @param identifier property name
     * @return a {@link JexlPropertyGet} or null
     * @since 3.0
     */
    JexlPropertyGet getPropertyGet(List<ResolverType> strategy, Object obj, Object identifier);

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
     * @param strategy the ordered list of resolver types, must not be null
     * @param obj the object to get the property from
     * @param identifier property name
     * @param arg value to set
     * @return a {@link JexlPropertySet} or null
     * @since 3.0
     */
    JexlPropertySet getPropertySet(List<ResolverType> strategy, Object obj, Object identifier, Object arg);

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