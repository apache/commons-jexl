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

    /**
     * Creates an instance checking for the Map interface.
     *@param is the introspector
     *@param clazz the class that might implement the map interface
     *@param key the key to use as argument in map.put(key,value)
     *@param value the value to use as argument in map.put(key,value)
    */
    public MapSetExecutor(Introspector is, Class<?> clazz, Object key, Object value) {
        super(clazz, discover(clazz));
        property = key;
    }

    /** {@inheritDoc} */
    @Override
    public Object getTargetProperty() {
        return property;
    }
    
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public Object execute(final Object map, Object value)
    throws IllegalAccessException, InvocationTargetException {
        ((Map<Object, Object>) map).put(property, value);
        return value;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public Object tryExecute(final Object map, Object key, Object value) {
        if (map != null && method != null
            && objectClass.equals(map.getClass())
            && (key == null || property.getClass().equals(key.getClass()))) {
            ((Map<Object, Object>) map).put(key, value);
            return value;
        }
        return TRY_FAILED;
    }

    /**
     * Finds the method to perform 'set' on a map.
     * @param clazz the class to introspect
     * @return a marker method, map.get
     */
    static java.lang.reflect.Method discover(Class<?> clazz) {
        return (Map.class.isAssignableFrom(clazz))? MAP_SET : null;
    }
}
