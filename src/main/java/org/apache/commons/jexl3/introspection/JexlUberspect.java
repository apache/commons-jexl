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

import java.util.Iterator;

/**
 * 'Federated' introspection/reflection interface to allow JEXL introspection
 *  behavior to be customized.
 *
 * @since 1.0
 */
public interface JexlUberspect {
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
    JexlMethod getConstructor(Object ctorHandle, Object[] args);

    /**
     * Returns a JexlMethod.
     * @param obj the object
     * @param method the method name
     * @param args method arguments
     * @return a {@link JexlMethod}
     */
    JexlMethod getMethod(Object obj, String method, Object[] args);

    /**
     * Property getter.
     * <p>Returns JexlPropertyGet appropos for ${bar.woogie}.
     * @param obj the object to get the property from
     * @param identifier property name
     * @return a {@link JexlPropertyGet}
     */
    JexlPropertyGet getPropertyGet(Object obj, Object identifier);

    /**
     * Property setter.
     * <p>returns JelPropertySet appropos for ${foo.bar = "geir"}</p>.
     * @param obj the object to get the property from.
     * @param identifier property name
     * @param arg value to set
     * @return a {@link JexlPropertySet}.
     */
    JexlPropertySet getPropertySet(Object obj, Object identifier, Object arg);

    /**
     * Gets an iterator from an object.
     * @param obj to get the iterator from
     * @return an iterator over obj
     */
    Iterator<?> getIterator(Object obj);

}