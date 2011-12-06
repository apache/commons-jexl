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
     * Creates an instance checking for the List interface or Array capability.
     * @param is the introspector
     * @param clazz the class that might implement the map interface
     * @param key the key to use in obj.set(key,value)
     * @param value the value to use in obj.set(key,value)
     */
    public ListSetExecutor(Introspector is, Class<?> clazz, Integer key, Object value) {
        super(clazz, discover(clazz));
        property = key;
    }

    /** {@inheritDoc} */
    @Override
    public Object getTargetProperty() {
        return property;
    }
    
    /** {@inheritDoc} */
    @Override
    public Object execute(final Object obj, Object value) {
        if (method == ARRAY_SET) {
            java.lang.reflect.Array.set(obj, property.intValue(), value);
        } else {
            @SuppressWarnings("unchecked") // LSE should only be created for array or list types
            final List<Object> list = (List<Object>) obj;
            list.set(property.intValue(), value);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(final Object obj, Object key, Object value) {
        if (obj != null && method != null
            && objectClass.equals(obj.getClass())
            && key instanceof Integer) {
            if (method == ARRAY_SET) {
                Array.set(obj, ((Integer) key).intValue(), value);
            } else {
                @SuppressWarnings("unchecked")  // LSE should only be created for array or list types
                final List<Object> list = (List<Object>) obj;
                list.set(((Integer) key).intValue(), value);
            }
            return value;
        }
        return TRY_FAILED;
    }


    /**
     * Finds the method to perform 'set' on a obj of array.
     * @param clazz the class to introspect
     * @return a marker method, obj.set or array.set
     */
    static java.lang.reflect.Method discover(Class<?> clazz) {
        if (clazz.isArray()) {
            // we could verify if the call can be performed but it does not change
            // the fact we would fail...
            // Class<?> formal = clazz.getComponentType();
            // Class<?> actual = value == null? Object.class : value.getClass();
            // if (IntrospectionUtils.isMethodInvocationConvertible(formal, actual, false)) {
                return ARRAY_SET;
            // }
        }
        if (List.class.isAssignableFrom(clazz)) {
            return LIST_SET;
        }
        return null;
    }
}
