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
import java.lang.reflect.InvocationTargetException;
/**
 * Specialized executor to get a boolean property from an object.
 * @since 2.0
 */
public final class BooleanGetExecutor extends AbstractExecutor.Get {
    /** The property. */
    private final String property;
    /**
     * Creates an instance by attempting discovery of the get method.
     * @param is the introspector
     * @param clazz the class to introspect
     * @param key the property to get
     */
    public BooleanGetExecutor(Introspector is, Class<?> clazz, String key) {
        super(clazz, discover(is, clazz, key));
        property = key;
    }

    /** {@inheritDoc} */
    @Override
    public Object getTargetProperty() {
        return property;
    }

    /** {@inheritDoc} */
    @Override
    public Object execute(Object obj)
        throws IllegalAccessException, InvocationTargetException {
        return method == null ? null : method.invoke(obj, (Object[]) null);
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(Object obj, Object key) {
        if (obj != null && method !=  null
            // ensure method name matches the property name
            && property.equals(key)
            && objectClass.equals(obj.getClass())) {
            try {
                return method.invoke(obj, (Object[]) null);
            } catch (InvocationTargetException xinvoke) {
                return TRY_FAILED; // fail
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            }
        }
        return TRY_FAILED;
    }

    /**
     * Discovers the method for a {@link BooleanGet}.
     * <p>The method to be found should be named "is{P,p}property and return a boolean.</p>
     *@param is the introspector
     *@param clazz the class to find the get method from
     *@param property the the property name
     *@return the method if found, null otherwise
     */
    static java.lang.reflect.Method discover(Introspector is, final Class<?> clazz, String property) {
        java.lang.reflect.Method m = property != null? PropertyGetExecutor.discoverGet(is, "is", clazz, property) : null;
        return (m != null && m.getReturnType() == Boolean.TYPE) ? m : null;
    }

}