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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.jexl3.JexlException;

/**
 * Specialized executor to set a property in an object.
 * @since 2.0
 */
public class PropertySetExecutor extends AbstractExecutor.Set {
    /** Index of the first character of the set{p,P}roperty. */
    private static final int SET_START_INDEX = 3;
    /** The property. */
    protected final String property;
    /** The property value class. */
    protected final Class<?> valueClass;

    /**
     * Discovers a PropertySetExecutor.
     * <p>The method to be found should be named "set{P,p}property.</p>
     *
     * @param is       the introspector
     * @param clazz    the class to find the get method from
     * @param property the property name to find
     * @param value      the value to assign to the property
     * @return the executor if found, null otherwise
     */
    public static PropertySetExecutor discover(Introspector is, Class<?> clazz, String property, Object value) {
        if (property == null || property.isEmpty()) {
            return null;
        }
        java.lang.reflect.Method method = discoverSet(is, clazz, property, value);
        return method != null? new PropertySetExecutor(clazz, method, property, value) : null;
    }

    /**
     * Creates an instance.
     * @param clazz  the class the set method applies to
     * @param method the method called through this executor
     * @param key    the key to use as 1st argument to the set method
     * @param value    the value
     */
    protected PropertySetExecutor(Class<?> clazz, java.lang.reflect.Method method, String key, Object value) {
        super(clazz, method);
        property = key;
        valueClass = classOf(value);
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(Object o, Object arg) throws IllegalAccessException, InvocationTargetException {
        if (method != null) {
            // handle the empty array case
            if (isEmptyArray(arg)) {
                // if array is empty but its component type is different from the method first parameter component type,
                // replace argument with a new empty array instance (of the method first parameter component type)
                Class<?> componentType = method.getParameterTypes()[0].getComponentType();
                if (componentType != null && !componentType.equals(arg.getClass().getComponentType())) {
                    arg = Array.newInstance(componentType, 0);
                }
            }
                method.invoke(o, arg);
            } 
        return arg;
    }

    @Override
    public Object tryInvoke(Object o, Object identifier, Object value) {
        if (o != null && method != null
            // ensure method name matches the property name
            && property.equals(castString(identifier))
            // object class should be same as executor's method declaring class
            && objectClass.equals(o.getClass())
            // argument class should be eq
            && valueClass.equals(classOf(value))) {
            try {
                return invoke(o, value);
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
     * Checks whether an argument is an empty array.
     * @param arg the argument
     * @return true if <code>arg</code> is an empty array
     */
    private static boolean isEmptyArray(Object arg) {
        return (arg != null && arg.getClass().isArray() && Array.getLength(arg) == 0);
    }

    /**
     * Discovers the method for a {@link PropertySet}.
     * <p>The method to be found should be named "set{P,p}property.
     * As a special case, any empty array will try to find a valid array-setting non-ambiguous method.
     *
     * @param is       the introspector
     * @param clazz    the class to find the get method from
     * @param property the name of the property to set
     * @param arg      the value to assign to the property
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
            // uppercase nth char, try array
            if (method == null && isEmptyArray(arg)) {
                sb.setCharAt(SET_START_INDEX, Character.toUpperCase(c));
                method = lookupSetEmptyArray(is, clazz, sb.toString());
                // lowercase nth char
                if (method == null) {
                    sb.setCharAt(SET_START_INDEX, Character.toLowerCase(c));
                    method = lookupSetEmptyArray(is, clazz, sb.toString());
                }
            }
        }
        return method;
    }

    /**
     * Finds an empty array property setter method by <code>methodName</code>.
     * <p>This checks only one method with that name accepts an array as sole parameter.
     * @param is       the introspector
     * @param clazz    the class to find the get method from
     * @param mname    the method name to find
     * @return         the sole method that accepts an array as parameter
     */
    private static java.lang.reflect.Method lookupSetEmptyArray(Introspector is, final Class<?> clazz, String mname) {
        java.lang.reflect.Method candidate = null;
        java.lang.reflect.Method[] methods = is.getMethods(clazz, mname);
        if (methods != null) {
            for (java.lang.reflect.Method method : methods) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1 && paramTypes[0].isArray()) {
                    if (candidate != null) {
                        // because the setter method is overloaded for different parameter type,
                        // return null here to report the ambiguity.
                        return null;
                    }
                    candidate = method;
                }
            }
        }
        return candidate;
    }
}
