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

import java.util.Map;

 /**
 * Specialized executor to get a property from a Map.
  * @since 2.0
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

    /** {@inheritDoc} */
    @Override
    public Object getTargetProperty() {
        return property;
    }

    /**
     * Get the property from the map.
     * @param obj the map.
     * @return map.get(property)
     */
    @Override
    public Object execute(final Object obj) {
        @SuppressWarnings("unchecked") // ctor only allows Map instances - see discover() method
        final Map<Object,?> map = (Map<Object, ?>) obj;
        return map.get(property);
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(final Object obj, Object key) {
        if (obj != null
            && method != null
            && objectClass.equals(obj.getClass())
            && ((property == null && key == null)
                || (property != null && key != null && property.getClass().equals(key.getClass())))) {
            @SuppressWarnings("unchecked") // ctor only allows Map instances - see discover() method
            final Map<Object, ?> map = (Map<Object, ?>) obj;
            return map.get(key);
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
