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
import java.util.List;
import java.lang.reflect.Array;
/**
 * Specialized executor to get a property from a List or array.
 */
public final class ListGetExecutor extends AbstractExecutor.Get {
    /** The java.lang.reflect.Array.get method used as an active marker in ListGet. */
    private static final java.lang.reflect.Method ARRAY_GET =
            initMarker(Array.class, "get", Object.class, Integer.TYPE);
    /** The java.util.list.get method used as an active marker in ListGet. */
    private static final java.lang.reflect.Method LIST_GET =
            initMarker(List.class, "get", Integer.TYPE);
    /** The property. */
    private final Integer property;

    /**
     * Creates an instance checking for the List interface or Array capability.
     * @param is the introspector
     * @param clazz the class to introspect
     * @param index the index to use in list.get(index)
     */
    public ListGetExecutor(Introspector is, Class<?> clazz, Integer index) {
        super(clazz, discover(clazz));
        property = index;
    }

    /**
     * Get the property from the list or array.
     * @param list the List/array.
     * @return list.get(index)
     */
    @Override
    public Object execute(final Object list) {
        if (method == ARRAY_GET) {
            return java.lang.reflect.Array.get(list, property);
        } else {
            return ((List<?>) list).get(property);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(final Object list, Object index) {
        if (method == discover(list.getClass())
            && objectClass.equals(list.getClass())
            && index instanceof Integer) {
            if (method == ARRAY_GET) {
                return java.lang.reflect.Array.get(list, property);
            } else {
                return ((List<?>) list).get(property);
            }
        }
        return TRY_FAILED;
    }


    /**
     * Finds the method to perform the get on a list of array.
     * @param clazz the class to introspect
     * @return a marker method, list.get or array.get
     */
    static java.lang.reflect.Method discover(Class<?> clazz) {
        //return discoverList(false, clazz, property);
        if (clazz.isArray()) {
            return ARRAY_GET;
        }
        if (List.class.isAssignableFrom(clazz)) {
            return LIST_GET;
        }
        return null;
    }
}
