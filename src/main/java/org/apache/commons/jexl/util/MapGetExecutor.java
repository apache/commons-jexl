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

 /**
 * Specialized executor to get a property from a Map.
 */
public final class MapGetExecutor extends AbstractExecutor.Get {
    /** The java.util.map.get method used as an active marker in MapGet. */
    private static final java.lang.reflect.Method MAP_GET =
            initMarker(Map.class, "get", Object.class);
    /** The property. */
    private final Object property;

    /**
     * Creates an instance checking for the Map interface.
     * @param is the introspector
     * @param clazz the class that might implement the map interface
     * @param key the key to use in map.get(key)
     */
    public MapGetExecutor(Introspector is, Class<?> clazz, Object key) {
        super(clazz, discover(clazz));
        property = key;
    }

    /**
     * Get the property from the map.
     * @param map the map.
     * @return map.get(property)
     */
    @Override
    public Object execute(final Object map) {
        return ((Map<Object, ?>) map).get(property);
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(final Object map, Object key) {
        if (objectClass.equals(map.getClass())
            && (key == null || property.getClass().equals(key.getClass()))) {
            return ((Map<Object, ?>) map).get(key);
        }
        return TRY_FAILED;
    }

    /**
     * Finds the method to perform 'get' on a map.
     * @param clazz the class to introspect
     * @return a marker method, map.get
     */
    static java.lang.reflect.Method discover(Class<?> clazz) {
        return (Map.class.isAssignableFrom(clazz))? MAP_GET : null;
    }
}
