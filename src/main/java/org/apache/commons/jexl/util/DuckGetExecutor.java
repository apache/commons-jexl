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
package org.apache.commons.jexl.util;

import java.lang.reflect.InvocationTargetException;

/**
 * Specialized executor to get a property from an object.
 * <p>Duck as in duck-typing for an interface like:
 * <code>
 * interface Get {
 *      Object get(Object key);
 * }
 * </code>
 * </p>
 */
/**
 * Specialized executor to get a property from an object.
 * <p>Duck as in duck-typing for classes that implement an get(object) method.</p>
 */
public final class DuckGetExecutor extends AbstractExecutor.Get {
    /** The property. */
    private final Object property;

    /**
     * Creates an instance by attempting discovery of the get method.
     * @param is the introspector
     * @param clazz the class to introspect
     * @param identifier the property to get
     */
    public DuckGetExecutor(Introspector is, Class<?> clazz, Object identifier) {
        super(clazz, discover(is, clazz, identifier));
        property = identifier;
    }

    /** {@inheritDoc} */
    @Override
    public Object getTargetProperty() {
        return property;
    }

    /**
     * Get the property from the object.
     * @param o the object.
     * @return object.get(property)
     * @throws IllegalAccessException Method is inaccessible.
     * @throws InvocationTargetException Method body throws an exception.
     */
    @Override
    public Object execute(Object o)
            throws IllegalAccessException, InvocationTargetException {
        Object[] args = {property};
        return method == null ? null : method.invoke(o, args);
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(Object o, Object identifier) {
        if (o != null && method !=  null
            // ensure method name matches the property name
            && property.equals(identifier)
            && objectClass.equals(o.getClass())) {
            try {
                Object[] args = {property};
                return method.invoke(o, args);
            } catch (InvocationTargetException xinvoke) {
                return TRY_FAILED; // fail
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            }
        }
        return TRY_FAILED;
    }

    /**
     * Discovers a method for a {@link GetExecutor.DuckGet}.
     *@param is the introspector
     *@param clazz the class to find the get method from
     *@param identifier the key to use as an argument to the get method
     *@return the method if found, null otherwise
     */
    private static java.lang.reflect.Method discover(Introspector is,
            final Class<?> clazz, Object identifier) {
        return is.getMethod(clazz, "get", makeArgs(identifier));
    }
}
