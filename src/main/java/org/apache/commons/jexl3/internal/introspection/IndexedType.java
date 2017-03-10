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


import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import java.lang.reflect.Method;
import java.beans.IntrospectionException;

/**
 * Abstract an indexed property container.
 * This stores the container name and owning class as well as the list of available getter and setter methods.
 * It implements JexlPropertyGet since such a container can only be accessed from its owning instance (not set).
 */
public final class IndexedType implements JexlPropertyGet {
    /** The container name. */
    private final String container;
    /** The owning class. */
    private final Class<?> clazz;
    /** The array of getter methods. */
    private final Method[] getters;
    /** The array of setter methods. */
    private final Method[] setters;

    /**
     * Attempts to find an indexed-property getter in an object.
     * The code attempts to find the list of methods getXXX() and setXXX().
     * Note that this is not equivalent to the strict bean definition of indexed properties; the type of the key
     * is not necessarily an int and the set/get arrays are not resolved.
     *
     * @param is the introspector
     * @param object the object
     * @param name the container name
     * @return a JexlPropertyGet is successful, null otherwise
     */
    public static JexlPropertyGet discover(Introspector is, Object object, String name) {
        if (object != null && name != null && !name.isEmpty()) {
            String base = name.substring(0, 1).toUpperCase() + name.substring(1);
            final String container = name;
            final Class<?> clazz = object.getClass();
            final Method[] getters = is.getMethods(object.getClass(), "get" + base);
            final Method[] setters = is.getMethods(object.getClass(), "set" + base);
            if (getters != null) {
                return new IndexedType(container, clazz, getters, setters);
            }
        }
        return null;
    }

    /**
     * A generic indexed property container, exposes get(key) and set(key, value) and solves method call dynamically
     * based on arguments.
     * <p>Must remain public for introspection purpose.</p>
     */
    public static final class IndexedContainer {
        /** The instance owning the container. */
        private final Object object;
        /** The container type instance. */
        private final IndexedType type;

        /**
         * Creates a new duck container.
         * @param theType the container type
         * @param theObject the instance owning the container
         */
        private IndexedContainer(IndexedType theType, Object theObject) {
            this.type = theType;
            this.object = theObject;
        }

        /**
         * Gets a property from a container.
         * @param key the property key
         * @return the property value
         * @throws Exception if inner invocation fails
         */
        public Object get(Object key) throws Exception {
            return type.invokeGet(object, key);
        }

        /**
         * Sets a property in a container.
         * @param key the property key
         * @param value the property value
         * @return the invocation result (frequently null)
         * @throws Exception if inner invocation fails
         */
        public Object set(Object key, Object value) throws Exception {
            return type.invokeSet(object, key, value);
        }
    }

    /**
     * Creates a new indexed type.
     * @param name the container name
     * @param c the owning class
     * @param gets the array of getter methods
     * @param sets the array of setter methods
     */
    private IndexedType(String name, Class<?> c, Method[] gets, Method[] sets) {
        this.container = name;
        this.clazz = c;
        this.getters = gets;
        this.setters = sets;
    }

    @Override
    public Object invoke(Object obj) throws Exception {
        if (obj != null && clazz.equals(obj.getClass())) {
            return new IndexedContainer(this, obj);
        } else {
            throw new IntrospectionException("property resolution error");
        }
    }

    @Override
    public Object tryInvoke(Object obj, Object key) {
        if (obj != null && key != null && clazz.equals(obj.getClass()) && container.equals(key.toString())) {
            return new IndexedContainer(this, obj);
        } else {
            return Uberspect.TRY_FAILED;
        }
    }

    @Override
    public boolean tryFailed(Object rval) {
        return rval == Uberspect.TRY_FAILED;
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    /**
     * Gets the value of a property from a container.
     * @param object the instance owning the container (not null)
     * @param key the property key (not null)
     * @return the property value
     * @throws Exception if invocation failed; IntrospectionException if a property getter could not be found
     */
    private Object invokeGet(Object object, Object key) throws Exception {
        if (getters != null) {
            final Object[] args = {key};
            final Method jm;
            if (getters.length == 1) {
                jm = getters[0];
            } else {
                jm = new MethodKey(getters[0].getName(), args).getMostSpecificMethod(getters);
            }
            if (jm != null) {
                return jm.invoke(object, args);
            }
        }
        throw new IntrospectionException("property get error: " + object.getClass().toString() + "@" + key.toString());
    }

    /**
     * Sets the value of a property in a container.
     * @param object the instance owning the container (not null)
     * @param key the property key (not null)
     * @param value the property value (not null)
     * @return the result of the method invocation (frequently null)
     * @throws Exception if invocation failed; IntrospectionException if a property setter could not be found
     */
    private Object invokeSet(Object object, Object key, Object value) throws Exception {
        if (setters != null) {
            final Object[] args = {key, value};
            final Method jm;
            if (setters.length == 1) {
                jm = setters[0];
            } else {
                jm = new MethodKey(setters[0].getName(), args).getMostSpecificMethod(setters);
            }
            if (jm != null) {
                return jm.invoke(object, args);
            }
        }
        throw new IntrospectionException("property set error: " + object.getClass().toString() + "@" + key.toString());
    }

}
