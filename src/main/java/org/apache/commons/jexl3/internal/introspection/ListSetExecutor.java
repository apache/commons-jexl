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

import java.util.List;
import java.lang.reflect.Array;

/**
 * Specialized executor to set a property in a List or array.
 * @since 2.0
 */
public final class ListSetExecutor extends AbstractExecutor.Set {
    /** The java.lang.reflect.Array.get method used as an active marker in ListGet. */
    private static final java.lang.reflect.Method ARRAY_SET =
            initMarker(Array.class, "set", Object.class, Integer.TYPE, Object.class);
    /** The java.util.obj.set method used as an active marker in ListSet. */
    private static final java.lang.reflect.Method LIST_SET =
            initMarker(List.class, "set", Integer.TYPE, Object.class);
    /** The property. */
    private final Integer property;

    /**
     * Attempts to discover a ListSetExecutor.
     *
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param identifier the key to use as an argument to the get method
     * @param value the value to use as argument in list.put(key,value)
     * @return the executor if found, null otherwise
     */
    public static ListSetExecutor discover(Introspector is, Class<?> clazz, Object identifier, Object value) {
        Integer index = castInteger(identifier);
        if (index != null) {
            if (clazz.isArray()) {
                // we could verify if the call can be performed but it does not change
                // the fact we would fail...
                // Class<?> formal = clazz.getComponentType();
                // Class<?> actual = value == null? Object.class : value.getClass();
                // if (IntrospectionUtils.isMethodInvocationConvertible(formal, actual, false)) {
                return new ListSetExecutor(clazz, ARRAY_SET, index);
                // }
            }
            if (List.class.isAssignableFrom(clazz)) {
                return new ListSetExecutor(clazz, LIST_SET, index);
            }
        }
        return null;
    }

    /**
     * Creates an instance.
     *
     * @param clazz the class the set method applies to
     * @param method the method called through this executor
     * @param key the key to use as 1st argument to the set method
     */
    private ListSetExecutor(Class<?> clazz, java.lang.reflect.Method method, Integer key) {
        super(clazz, method);
        property = key;
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(final Object obj, Object value) {
        if (method == ARRAY_SET) {
            Array.set(obj, property.intValue(), value);
        } else {
            @SuppressWarnings("unchecked") // LSE should only be created for array or list types
            final List<Object> list = (List<Object>) obj;
            list.set(property.intValue(), value);
        }
        return value;
    }

    @Override
    public Object tryInvoke(final Object obj, Object key, Object value) {
        Integer index = castInteger(key);
        if (obj != null && method != null
                && objectClass.equals(obj.getClass())
                && index != null) {
            if (method == ARRAY_SET) {
                Array.set(obj, index.intValue(), value);
            } else {
                @SuppressWarnings("unchecked")  // LSE should only be created for array or list types
                final List<Object> list = (List<Object>) obj;
                list.set(index.intValue(), value);
            }
            return value;
        }
        return TRY_FAILED;
    }
}
