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
 * Specialized executor to set a property in an object.
 * @since 2.0
 */
public final class PropertySetExecutor extends AbstractExecutor.Set {
    /** Index of the first character of the set{p,P}roperty. */
    private static final int SET_START_INDEX = 3;
    /** The property. */
    private final String property;
    
    /**
     * Discovers a PropertySetExecutor.
     * <p>The method to be found should be named "set{P,p}property.</p>
     * 
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param property the property name to find
     * @param arg the value to assign to the property
     * @return the executor if found, null otherwise
     */
    public static PropertySetExecutor discover(Introspector is, Class<?> clazz, String property, Object arg) {
        java.lang.reflect.Method method = discoverSet(is, clazz, property, arg);
        return method == null? null : new PropertySetExecutor(clazz, method, property);
    }
    
    /**
     * Creates an instance.
     * @param clazz the class the set method applies to
     * @param method the method called through this executor
     * @param key the key to use as 1st argument to the set method
     */
    private PropertySetExecutor(Class<?> clazz, java.lang.reflect.Method method, String key) {
        super(clazz, method);
        property = key;

    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(Object o, Object arg) throws IllegalAccessException, InvocationTargetException {
        Object[] pargs = {arg};
        if (method != null) {
            method.invoke(o, pargs);
        }
        return arg;
    }

    @Override
    public Object tryInvoke(Object o, Object identifier, Object arg) {
        if (o != null && method != null
            // ensure method name matches the property name
            && property.equals(toString(identifier))
            // object class should be same as executor's method declaring class
            && objectClass.equals(o.getClass())
            // we are guaranteed the method has one parameter since it is a set(x)
            && (arg == null || method.getParameterTypes()[0].equals(arg.getClass()))) {
            try {
                return invoke(o, arg);
            } catch (InvocationTargetException xinvoke) {
                return TRY_FAILED; // fail
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            }
        }
        return TRY_FAILED;
    }


    /**
     * Discovers the method for a {@link PropertySet}.
     * <p>The method to be found should be named "set{P,p}property.</p>
     * 
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param property the name of the property to set
     * @param arg the value to assign to the property
     * @return the method if found, null otherwise
     */
    private static java.lang.reflect.Method discoverSet(Introspector is, Class<?> clazz, String property, Object arg) {
        // first, we introspect for the set<identifier> setter method
        Object[] params = {arg};
        StringBuilder sb = new StringBuilder("set");
        sb.append(property);
        // uppercase nth char
        char c = sb.charAt(SET_START_INDEX);
        sb.setCharAt(SET_START_INDEX, Character.toUpperCase(c));
        java.lang.reflect.Method method = is.getMethod(clazz, sb.toString(), params);
        // lowercase nth char
        if (method == null) {
            sb.setCharAt(SET_START_INDEX, Character.toLowerCase(c));
            method = is.getMethod(clazz, sb.toString(), params);
        }
        return method;
    }
}

