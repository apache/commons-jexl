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
 * Specialized executor to set a property of an object.
 * <p>Duck as in duck-typing for an interface like:
 * <code>
 * interface Set {
 *      Object put(Object property, Object value);
 * }
 * </code>
 * </p>
 */
public final class DuckSetExecutor extends AbstractExecutor.Set {
    /** The property. */
    private final Object property;
    
    /**
     * Creates an instance.
     *@param is the introspector
     *@param clazz the class to find the set method from
     *@param identifier the key to use as 1st argument to the set method
     *@param arg the value to use as 2nd argument to the set method
     */
    public DuckSetExecutor(Introspector is, Class<?> clazz, Object identifier, Object arg) {
        super(clazz, discover(is, clazz, identifier, arg));
        property = identifier;
    }

    /** {@inheritDoc} */
    @Override
    public Object execute(Object o, Object arg)
            throws IllegalAccessException, InvocationTargetException {
        Object[] pargs = {property, arg};
        if (method != null) {
            method.invoke(o, pargs);
        }
        return arg;
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(Object o, Object identifier, Object arg) {
        if (o != null && method !=  null
            // ensure method name matches the property name
            && property.equals(identifier)
            && objectClass.equals(o.getClass())) {
            try {
                Object[] args = {property, arg};
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
     * Discovers the method for a {@link DuckSet}.
     *@param is the introspector
     *@param clazz the class to find the set method from
     *@param identifier the key to use as 1st argument to the set method
     *@param arg the value to use as 2nd argument to the set method
     *@return the method if found, null otherwise
     */
    private static java.lang.reflect.Method discover(Introspector is,
            Class<?> clazz, Object identifier, Object arg) {
        return is.getMethod(clazz, "put", makeArgs(identifier, arg));
    }
}