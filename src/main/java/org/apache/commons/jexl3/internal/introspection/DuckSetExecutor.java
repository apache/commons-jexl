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
package org.apache.commons.jexl3.internal.introspection;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.jexl3.JexlException;

/**
 * Specialized executor to set a property of an object.
 * <p>Duck as in duck-typing for an interface like:
 * <code>
 * interface Setable {
 *      Object set(Object property, Object value);
 * }
 * </code>
 * or
 * <code>
 * interface Putable {
 *      Object put(Object property, Object value);
 * }
 * </code>
 * </p>
 * @since 2.0
 */
public final class DuckSetExecutor extends AbstractExecutor.Set {
    /** The property, may be null. */
    private final Object property;
    /** The property value class. */
    private final Class<?> valueClass;

    /**
     * Discovers a DuckSetExecutor.
     *
     * @param is the introspector
     * @param clazz the class to find the set method from
     * @param key the key to use as 1st argument to the set method
     * @param value the value to use as 2nd argument to the set method
     * @return the executor if found, null otherwise
     */
    public static DuckSetExecutor discover(Introspector is, Class<?> clazz, Object key, Object value) {
        java.lang.reflect.Method method = is.getMethod(clazz, "set", makeArgs(key, value));
        if (method == null) {
            method = is.getMethod(clazz, "put", makeArgs(key, value));
        }
        return method == null? null : new DuckSetExecutor(clazz, method, key, value);
    }

    /**
     * Creates an instance.
     * @param clazz the class the set method applies to
     * @param method the method called through this executor
     * @param key the key to use as 1st argument to the set method
     * @param value the value to use as 2nd argument to the set method
     */
    private DuckSetExecutor(Class<?> clazz, java.lang.reflect.Method method, Object key, Object value) {
        super(clazz, method);
        property = key;
        valueClass = classOf(value);
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(Object obj, Object value) throws IllegalAccessException, InvocationTargetException {
        Object[] pargs = {property, value};
        if (method != null) {
                method.invoke(obj, pargs);
            }
        return value;
    }

    @Override
    public Object tryInvoke(Object obj, Object key, Object value) {
        if (obj != null
            && objectClass.equals(obj.getClass())
            && method !=  null
            && ((property != null && property.equals(key))
                || (property == null && key == null))
            && valueClass.equals(classOf(value))) {
            try {
                Object[] args = {property, value};
                method.invoke(obj, args);
                return value;
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            } catch (IllegalArgumentException xarg) {
                return TRY_FAILED;// fail
            } catch (InvocationTargetException xinvoke) {
                throw JexlException.tryFailed(xinvoke); // throw
            } 
        }
        return TRY_FAILED;
    }
}