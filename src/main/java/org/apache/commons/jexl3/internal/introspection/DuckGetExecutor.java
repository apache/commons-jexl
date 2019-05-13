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
 * Specialized executor to get a property from an object.
 * <p>Duck as in duck-typing for an interface like:
 * <code>
 * interface Get {
 *      Object get(Object key);
 * }
 * </code>
 * </p>
 * @since 2.0
 */
public final class DuckGetExecutor extends AbstractExecutor.Get {
    /** The property, may be null. */
    private final Object property;

    /**
     * Attempts to discover a DuckGetExecutor.
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param identifier the key to use as an argument to the get method
     * @return the executor if found, null otherwise
     */
    public static DuckGetExecutor discover(Introspector is, Class<?> clazz, Object identifier) {
        java.lang.reflect.Method method = is.getMethod(clazz, "get", makeArgs(identifier));
        return method == null? null : new DuckGetExecutor(clazz, method, identifier);
    }

    /**
     * Creates an instance.
     * @param clazz he class the get method applies to
     * @param method the method held by this executor
     * @param identifier the property to get
     */
    private DuckGetExecutor(Class<?> clazz, java.lang.reflect.Method method, Object identifier) {
        super(clazz, method);
        property = identifier;
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(Object obj) throws IllegalAccessException, InvocationTargetException {
        Object[] args = {property};
        return method == null ? null : method.invoke(obj, args);
    }

    @Override
    public Object tryInvoke(Object obj, Object key) {
        if (obj != null
                && objectClass.equals(obj.getClass())
                // ensure method name matches the property name
                && method != null
                && ((property == null && key == null)
                || (property != null && property.equals(key)))) {
            try {
                Object[] args = {property};
                return method.invoke(obj, args);
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
