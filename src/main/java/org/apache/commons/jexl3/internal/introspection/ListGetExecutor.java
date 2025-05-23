/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal.introspection;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Specialized executor to get a property from a List or array.
 * @since 2.0
 */
public final class ListGetExecutor extends AbstractExecutor.Get {
    /** The java.lang.reflect.Array.get method used as an active marker in ListGet. */
    private static final java.lang.reflect.Method ARRAY_GET =
            initMarker(Array.class, "get", Object.class, Integer.TYPE);
    /** The java.util.obj.get method used as an active marker in ListGet. */
    private static final java.lang.reflect.Method LIST_GET =
            initMarker(List.class, "get", Integer.TYPE);
    /**
     * Attempts to discover a ListGetExecutor.
     *
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param index the index to use as an argument to the get method
     * @return the executor if found, null otherwise
     */
    public static ListGetExecutor discover(final Introspector is, final Class<?> clazz, final Integer index) {
        if (index != null) {
            if (clazz.isArray()) {
                return new ListGetExecutor(clazz, ARRAY_GET, index);
            }
            // we still need to ensure permissions grant access to get(...)
            if (List.class.isAssignableFrom(clazz) && is.getMethod(clazz, "get", index) != null) {
                return new ListGetExecutor(clazz, LIST_GET, index);
            }
        }
        return null;
    }

    /** The property. */
    private final Integer property;

    /**
     * Creates an instance.
     * @param clazz he class the get method applies to
     * @param method the method held by this executor
     * @param index the index to use as an argument to the get method
     */
    private ListGetExecutor(final Class<?> clazz, final java.lang.reflect.Method method, final Integer index) {
        super(clazz, method);
        property = index;
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(final Object obj) {
        if (method == ARRAY_GET) {
            return Array.get(obj, property);
        }
        return ((List<?>) obj).get(property);
    }

    @Override
    public Object tryInvoke(final Object obj, final Object identifier) {
        final Integer index = castInteger(identifier);
        if (obj != null && method != null
            && objectClass.equals(obj.getClass())
            && index != null) {
            if (method == ARRAY_GET) {
                return Array.get(obj, index);
            }
            return ((List<?>) obj).get(index);
        }
        return TRY_FAILED;
    }
}
