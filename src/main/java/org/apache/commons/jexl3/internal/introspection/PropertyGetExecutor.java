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
import org.apache.commons.jexl3.JexlException;

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
        java.lang.reflect.Method method = discoverGet(is, "get", clazz, property);
        return method == null? null : new PropertyGetExecutor(clazz, method, property);
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
    public Object invoke(Object o) throws IllegalAccessException, InvocationTargetException {
        return method == null ? null : method.invoke(o, (Object[]) null);
    }

    @Override
    public Object tryInvoke(Object o, Object identifier) {
        if (o != null && method != null
            && property.equals(castString(identifier))
            && objectClass.equals(o.getClass())) {
            try {
                return method.invoke(o, (Object[]) null);
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            } catch (IllegalArgumentException xarg) {
                return TRY_FAILED;// fail
            } catch (InvocationTargetException xinvoke) {
                throw JexlException.tryFailed(xinvoke); // throw
            } 
        }
        return TRY_FAILED;
    }

    /**
     * Base method for boolean and object property get.
     * @param is the introspector
     * @param which "is" or "get" for boolean or object
     * @param clazz The class being examined.
     * @param property The property being addressed.
     * @return The {get,is}{p,P}roperty method if one exists, null otherwise.
     */
    static java.lang.reflect.Method discoverGet(Introspector is, String which, Class<?> clazz, String property) {
        if (property == null || property.isEmpty()) {
            return null;
        }
        //  this is gross and linear, but it keeps it straightforward.
        java.lang.reflect.Method method;
        final int start = which.length(); // "get" or "is" so 3 or 2 for char case switch
        // start with get<Property>
        StringBuilder sb = new StringBuilder(which);
        sb.append(property);
        // uppercase nth char
        char c = sb.charAt(start);
        sb.setCharAt(start, Character.toUpperCase(c));
        method = is.getMethod(clazz, sb.toString(), EMPTY_PARAMS);
        //lowercase nth char
        if (method == null) {
            sb.setCharAt(start, Character.toLowerCase(c));
            method = is.getMethod(clazz, sb.toString(), EMPTY_PARAMS);
        }
        return method;
    }
}

