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
package org.apache.commons.jexl2.internal;

import java.lang.reflect.InvocationTargetException;

/**
 * Specialized executor to set a property of an object.
 * <p>Duck as in duck-typing for an interface like:
 * <code>
 * interface Set {
 *      Object set(Object property, Object value);
 * }
 * </code>
 * </p>
 * @since 2.0
 */
public final class DuckSetExecutor extends AbstractExecutor.Set {
    /** The property. */
    private final Object property;

    /**
     * Creates an instance.
     *@param is the introspector
     *@param clazz the class to find the set method from
     *@param key the key to use as 1st argument to the set method
     *@param value the value to use as 2nd argument to the set method
     */
    public DuckSetExecutor(Introspector is, Class<?> clazz, Object key, Object value) {
        super(clazz, discover(is, clazz, key, value));
        property = key;
    }

    /** {@inheritDoc} */
    @Override
    public Object getTargetProperty() {
        return property;
    }

    /** {@inheritDoc} */
    @Override
    public Object execute(Object obj, Object value)
            throws IllegalAccessException, InvocationTargetException {
        Object[] pargs = {property, value};
        if (method != null) {
            method.invoke(obj, pargs);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(Object obj, Object key, Object value) {
        if (obj != null
            && objectClass.equals(obj.getClass())
            && method !=  null
            && ((property != null && property.equals(key))
                || (property == null && key == null))) {
            try {
                Object[] args = {property, value};
                method.invoke(obj, args);
                return value;
            } catch (InvocationTargetException xinvoke) {
                return TRY_FAILED; // fail
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            } catch (IllegalArgumentException xarg) {
                return TRY_FAILED;// fail
            }
        }
        return TRY_FAILED;
    }

    /**
     * Discovers the method for a {@link DuckSet}.
     *@param is the introspector
     *@param clazz the class to find the set method from
     *@param key the key to use as 1st argument to the set method
     *@param value the value to use as 2nd argument to the set method
     *@return the method if found, null otherwise
     */
    private static java.lang.reflect.Method discover(Introspector is,
            Class<?> clazz, Object key, Object value) {
        return is.getMethod(clazz, "set", makeArgs(key, value));
    }
}