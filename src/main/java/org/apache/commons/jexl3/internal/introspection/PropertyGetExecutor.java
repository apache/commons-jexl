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

/**
 * Specialized executor to get a property from an object.
 * @since 2.0
 */
public final class PropertyGetExecutor extends AbstractExecutor.Get {
    /** A static signature for method(). */
    private static final Object[] EMPTY_PARAMS = {};
    /** The property. */
    private final String property;

    /**
     * Discovers a PropertyGetExecutor.
     * <p>The method to be found should be named "get{P,p}property.</p>
     *
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param property the property name to find
     * @return the executor if found, null otherwise
     */
    public static PropertyGetExecutor discover(Introspector is, Class<?> clazz, String property) {
        java.lang.reflect.Method m = is.getPropertyGet(clazz, property);
        return m == null ? null : new PropertyGetExecutor(clazz, m, property);
    }

    /**
     * Creates an instance.
     * @param clazz he class the get method applies to
     * @param method the method held by this executor
     * @param identifier the property to get
     */
    private PropertyGetExecutor(Class<?> clazz, java.lang.reflect.Method method, String identifier) {
        super(clazz, method);
        property = identifier;
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(Object o)
        throws IllegalAccessException, InvocationTargetException {
        return method.invoke(o, (Object[]) null);
    }

    @Override
    public Object tryInvoke(Object o, Object identifier) {
        if (o != null && objectClass == o.getClass() && property.equals(castString(identifier))) {
            try {
                return method.invoke(o, (Object[]) null);
            } catch (InvocationTargetException xinvoke) {
                return TRY_FAILED; // fail
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            } catch (IllegalArgumentException xarg) {
                return TRY_FAILED;// fail
            }
        }
        return TRY_FAILED;
    }
}

