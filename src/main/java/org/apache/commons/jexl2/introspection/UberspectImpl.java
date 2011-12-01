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
package org.apache.commons.jexl2.introspection;

import java.beans.IntrospectionException;
import org.apache.commons.jexl2.internal.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jexl2.JexlInfo;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.internal.AbstractExecutor;
import org.apache.commons.jexl2.internal.ArrayIterator;
import org.apache.commons.jexl2.internal.EnumerationIterator;
import org.apache.commons.jexl2.internal.introspection.MethodKey;
import org.apache.commons.logging.Log;

/**
 * Implementation of Uberspect to provide the default introspective
 * functionality of JEXL.
 * <p>This is the class to derive to customize introspection.</p>
 *
 * @since 1.0
 */
public class UberspectImpl extends Introspector implements Uberspect {
    /**
     * Publicly exposed special failure object returned by tryInvoke.
     */
    public static final Object TRY_FAILED = AbstractExecutor.TRY_FAILED;

    /**
     * Creates a new UberspectImpl.
     * @param runtimeLogger the logger used for all logging needs
     */
    public UberspectImpl(Log runtimeLogger) {
        super(runtimeLogger);
    }

    /**
     * Resets this Uberspect class loader.
     * @param cloader the class loader to use
     * @since 2.1
     */
    public void setLoader(ClassLoader cloader) {
        base().setLoader(cloader);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Iterator<?> getIterator(Object obj, JexlInfo info) {
        if (obj instanceof Iterator<?>) {
            return ((Iterator<?>) obj);
        }
        if (obj.getClass().isArray()) {
            return new ArrayIterator(obj);
        }
        if (obj instanceof Map<?, ?>) {
            return ((Map<?, ?>) obj).values().iterator();
        }
        if (obj instanceof Enumeration<?>) {
            return new EnumerationIterator<Object>((Enumeration<Object>) obj);
        }
        if (obj instanceof Iterable<?>) {
            return ((Iterable<?>) obj).iterator();
        }
        try {
            // look for an iterator() method to support the JDK5 Iterable
            // interface or any user tools/DTOs that want to work in
            // foreach without implementing the Collection interface
            AbstractExecutor.Method it = getMethodExecutor(obj, "iterator", null);
            if (it != null && Iterator.class.isAssignableFrom(it.getReturnType())) {
                return (Iterator<Object>) it.execute(obj, null);
            }
        } catch (Exception xany) {
            throw new JexlException(info, "unable to generate iterator()", xany);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public JexlMethod getMethod(Object obj, String method, Object[] args, JexlInfo info) {
        return getMethodExecutor(obj, method, args);
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public Constructor<?> getConstructor(Object ctorHandle, Object[] args, JexlInfo info) {
        return getConstructor(ctorHandle, args);
    }
    
    /**
     * {@inheritDoc}
     * @since 2.1
     */
    public JexlMethod getConstructorMethod(Object ctorHandle, Object[] args, JexlInfo info) {
        final Constructor<?> ctor = getConstructor(ctorHandle, args);
        if (ctor != null) {
            return new ConstructorMethod(ctor);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier, JexlInfo info) {
        JexlPropertyGet get = getGetExecutor(obj, identifier);
        if (get == null && obj != null && identifier != null) {
            get = getIndexedGet(obj, identifier.toString());
            if (get == null) {
                Field field = getField(obj, identifier.toString(), info);
                if (field != null) {
                    return new FieldPropertyGet(field);
                }
            }
        }
        return get;
    }

    /**
     * {@inheritDoc}
     */
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, Object arg, JexlInfo info) {
        JexlPropertySet set = getSetExecutor(obj, identifier, arg);
        if (set == null && obj != null && identifier != null) {
            Field field = getField(obj, identifier.toString(), info);
            if (field != null
                    && !Modifier.isFinal(field.getModifiers())
                    && (arg == null || MethodKey.isInvocationConvertible(field.getType(), arg.getClass(), false))) {
                return new FieldPropertySet(field);
            }
        }
        return set;
    }

    /**
     * Returns a class field.
     * Only for use by sub-classes, will be made protected in a later version
     * @param obj the object
     * @param name the field name
     * @param info debug info
     * @return a {@link Field}.
     */
    public Field getField(Object obj, String name, JexlInfo info) {
        final Class<?> clazz = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        return getField(clazz, name);
    }

    /**
     * Attempts to find an indexed-property getter in an object.
     * The code attempts to find the list of methods getXXX() and setXXX().
     * Note that this is not equivalent to the strict bean definition of indexed properties; the type of the key
     * is not necessarily an int and the set/get arrays are not resolved.
     * @param object the object
     * @param name the container name
     * @return a JexlPropertyGet is successfull, null otherwise
     * @since 2.1
     */
    protected JexlPropertyGet getIndexedGet(Object object, String name) {
        if (object != null && name != null) {
            String base = name.substring(0, 1).toUpperCase() + name.substring(1);
            final String container = name;
            final Class<?> clazz = object.getClass();
            final Method[] getters = getMethods(object.getClass(), "get" + base);
            final Method[] setters = getMethods(object.getClass(), "set" + base);
            if (getters != null) {
                return new IndexedType(container, clazz, getters, setters);
            }
        }
        return null;
    }

    /**
     * Abstract an indexed property container.
     * This stores the container name and owning class as well as the list of available getter and setter methods.
     * It implements JexlPropertyGet since such a container can only be accessed from its owning instance (not set).
     * @since 2.1
     */
    private static final class IndexedType implements JexlPropertyGet {
        /** The container name. */
        private final String container;
        /** The owning class. */
        private final Class<?> clazz;
        /** The array of getter methods. */
        private final Method[] getters;
        /** The array of setter methods. */
        private final Method[] setters;

        /**
         * Creates a new indexed type.
         * @param name the container name
         * @param c the owning class
         * @param gets the array of getter methods
         * @param sets the array of setter methods
         */
        IndexedType(String name, Class<?> c, Method[] gets, Method[] sets) {
            this.container = name;
            this.clazz = c;
            this.getters = gets;
            this.setters = sets;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object obj) throws Exception {
            if (obj != null && clazz.equals(obj.getClass())) {
                return new IndexedContainer(this, obj);
            } else {
                throw new IntrospectionException("property resolution error");
            }
        }

        /**
         * {@inheritDoc}
         */
        public Object tryInvoke(Object obj, Object key) {
            if (obj != null && key != null && clazz.equals(obj.getClass()) && container.equals(key.toString())) {
                return new IndexedContainer(this, obj);
            } else {
                return TRY_FAILED;
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
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
                    jm = new MethodKey(getters[0].getName(), args).getMostSpecificMethod(Arrays.asList(getters));
                }
                if (jm != null) {
                    return jm.invoke(object, args);
                }
            }
            throw new IntrospectionException("property get error: "
                    + object.getClass().toString() + "@" + key.toString());
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
                    jm = new MethodKey(setters[0].getName(), args).getMostSpecificMethod(Arrays.asList(setters));
                }
                if (jm != null) {
                    return jm.invoke(object, args);
                }
            }
            throw new IntrospectionException("property set error: "
                    + object.getClass().toString() + "@" + key.toString());
        }
    }

    /**
     * A generic indexed property container, exposes get(key) and set(key, value) and solves method call dynamically
     * based on arguments.
     * @since 2.1
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
     * A JexlMethod that wraps constructor.
     * @since 2.1
     */
    private final class ConstructorMethod implements JexlMethod {
        /** The wrapped constructor. */
        private final Constructor<?> ctor;

        /**
         * Creates a constructor method.
         * @param theCtor the constructor to wrap
         */
        private ConstructorMethod(Constructor<?> theCtor) {
            this.ctor = theCtor;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object obj, Object[] params) throws Exception {
            Class<?> clazz = null;
            if (obj instanceof Class<?>) {
                clazz = (Class<?>) obj;
            } else if (obj != null) {
                clazz = getClassByName(obj.toString());
            } else {
                clazz = ctor.getDeclaringClass();
            }
            if (clazz.equals(ctor.getDeclaringClass())) {
                return ctor.newInstance(params);
            } else {
                throw new IntrospectionException("constructor resolution error");
            }
        }

        /**
         * {@inheritDoc}
         */
        public Object tryInvoke(String name, Object obj, Object[] params) {
            Class<?> clazz = null;
            if (obj instanceof Class<?>) {
                clazz = (Class<?>) obj;
            } else if (obj != null) {
                clazz = getClassByName(obj.toString());
            } else {
                clazz = ctor.getDeclaringClass();
            }
            if (clazz.equals(ctor.getDeclaringClass())
                    && (name == null || name.equals(clazz.getName()))) {
                try {
                    return ctor.newInstance(params);
                } catch (InstantiationException xinstance) {
                    return TRY_FAILED;
                } catch (IllegalAccessException xaccess) {
                    return TRY_FAILED;
                } catch (IllegalArgumentException xargument) {
                    return TRY_FAILED;
                } catch (InvocationTargetException xinvoke) {
                    return TRY_FAILED;
                }
            }
            return TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCacheable() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public Class<?> getReturnType() {
            return ctor.getDeclaringClass();
        }
    }

    /**
     * A JexlPropertyGet for public fields.
     * @deprecated Do not use externally - will be made private in a later version
     */
    @Deprecated
    public static final class FieldPropertyGet implements JexlPropertyGet {
        /**
         * The public field.
         */
        private final Field field;

        /**
         * Creates a new instance of FieldPropertyGet.
         * @param theField the class public field
         */
        public FieldPropertyGet(Field theField) {
            field = theField;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object obj) throws Exception {
            return field.get(obj);
        }

        /**
         * {@inheritDoc}
         */
        public Object tryInvoke(Object obj, Object key) {
            if (obj.getClass().equals(field.getDeclaringClass()) && key.equals(field.getName())) {
                try {
                    return field.get(obj);
                } catch (IllegalAccessException xill) {
                    return TRY_FAILED;
                }
            }
            return TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCacheable() {
            return true;
        }
    }

    /**
     * A JexlPropertySet for public fields.
    * @deprecated Do not use externally - will be made private in a later version
     */
    @Deprecated
    public static final class FieldPropertySet implements JexlPropertySet {
        /**
         * The public field.
         */
        private final Field field;

        /**
         * Creates a new instance of FieldPropertySet.
         * @param theField the class public field
         */
        public FieldPropertySet(Field theField) {
            field = theField;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object obj, Object arg) throws Exception {
            field.set(obj, arg);
            return arg;
        }

        /**
         * {@inheritDoc}
         */
        public Object tryInvoke(Object obj, Object key, Object value) {
            if (obj.getClass().equals(field.getDeclaringClass())
                    && key.equals(field.getName())
                    && (value == null || MethodKey.isInvocationConvertible(field.getType(), value.getClass(), false))) {
                try {
                    field.set(obj, value);
                    return value;
                } catch (IllegalAccessException xill) {
                    return TRY_FAILED;
                }
            }
            return TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCacheable() {
            return true;
        }
    }
}
