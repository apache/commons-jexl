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
import java.util.Map;
import java.lang.reflect.InvocationTargetException;

/**
 * Specialized executor to set a property in a Map.
 * @since 2.0
 */
public final class MapSetExecutor extends AbstractExecutor.Set {
    /** The java.util.map.put method used as an active marker in MapSet. */
    private static final java.lang.reflect.Method MAP_SET = initMarker(Map.class, "put", Object.class, Object.class);
    /** The property. */
    private final Object property;
    /** The property value class. */
    private final Class<?> valueClass;

    /**
     * Attempts to discover a MapSetExecutor.
     *
     * @param is the introspector
     * @param clazz the class to find the set method from
     * @param identifier the key to use as an argument to the get method
     * @param value the value to use as argument in map.put(key,value)
     * @return the executor if found, null otherwise
     */
    public static MapSetExecutor discover(Introspector is, Class<?> clazz, Object identifier, Object value) {
        if (Map.class.isAssignableFrom(clazz)) {
            return new MapSetExecutor(clazz, MAP_SET, identifier, value);
        } else {
            return null;
        }
    }

    /**
     * Creates an instance.
     * @param clazz the class the set method applies to
     * @param method the method called through this executor
     * @param key the key to use as 1st argument to the set method
     * @param value the value to use as 2nd argument to the set method
     */
    private MapSetExecutor(Class<?> clazz, java.lang.reflect.Method method, Object key, Object value) {
        super(clazz, method);
        property = key;
        valueClass = classOf(value);
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(final Object obj, Object value) throws IllegalAccessException, InvocationTargetException {
        @SuppressWarnings("unchecked") // ctor only allows Map instances - see discover() method
        final Map<Object,Object> map = ((Map<Object, Object>) obj);
        map.put(property, value);
        return value;
    }

    @Override
    public Object tryInvoke(final Object obj, Object key, Object value) {
        if (obj != null
            && method != null
            && objectClass.equals(obj.getClass())
            && ((property == null && key == null)
                || (property != null && key != null && property.getClass().equals(key.getClass())))
            && valueClass.equals(classOf(value))) {
            @SuppressWarnings("unchecked") // ctor only allows Map instances - see discover() method
            final Map<Object,Object> map = ((Map<Object, Object>) obj);
            map.put(key, value);
            return value;
        }
        return TRY_FAILED;
    }
}
