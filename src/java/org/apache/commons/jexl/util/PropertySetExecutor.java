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
import java.lang.reflect.InvocationTargetException;
/**
 * Specialized executor to set a property in an object.
 */
public final class PropertySetExecutor extends AbstractExecutor.Set {
    /** Index of the first character of the set{p,P}roperty. */
    private static final int SET_START_INDEX = 3;
    /** The property. */
    private final String property;

    /**
     * Creates an instance by attempting discovery of the set method.
     * @param is the introspector
     * @param clazz the class to introspect
     * @param identifier the property to set
     * @param arg the value to set into the property
     */
    public PropertySetExecutor(Introspector is, Class<?> clazz, String identifier, Object arg) {
        super(clazz, discover(is, clazz, identifier, arg));
        property = identifier;

    }

    /** {@inheritDoc} */
    @Override
    public Object execute(Object o, Object arg)
            throws IllegalAccessException, InvocationTargetException {
        Object[] pargs = {arg};
        if (method != null) {
            method.invoke(o, pargs);
        }
        return arg;
    }

    /** {@inheritDoc} */
    @Override
    public Object tryExecute(Object o, Object identifier, Object arg) {
        if (o != null && method != null
            // ensure method name matches the property name
            && property.equals(identifier)
            // object class should be same as executor's method declaring class
            && objectClass.equals(o.getClass())
            // we are guaranteed the method has one parameter since it is a set(x)
            && (arg == null || method.getParameterTypes()[0].equals(arg.getClass()))) {
            try {
                return execute(o, arg);
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
     *@param is the introspector
     *@param clazz the class to find the get method from
     *@param property the name of the property to set
     *@param arg the value to assign to the property
     *@return the method if found, null otherwise
     */
    private static java.lang.reflect.Method discover(Introspector is,
            final Class<?> clazz, String property, Object arg) {
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

