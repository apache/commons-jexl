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

import java.beans.IntrospectionException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfoHandle;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.Log;

/**
 * Implementation of Uberspect to provide the default introspective
 * functionality of JEXL.
 * <p>This is the class to derive to customize introspection.</p>
 *
 * @since 1.0
 */
public class Uberspect implements JexlUberspect {
    /**
     * Publicly exposed special failure object returned by tryInvoke.
     */
    public static final Object TRY_FAILED = AbstractExecutor.TRY_FAILED;
/** The logger to use for all warnings & errors. */
    protected final Log rlog;
    /** The soft reference to the introspector currently in use. */
    private volatile SoftReference<Introspector> ref;

    /**
     * Coerce an Object  to an Integer.
     * @param arg the Object to coerce
     * @return an Integer if it can be converted, null otherwise
     */
    protected Integer toInteger(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof Number) {
            return Integer.valueOf(((Number) arg).intValue());
        }
        try {
            return Integer.valueOf(arg.toString());
        } catch (NumberFormatException xnumber) {
            return null;
        }
    }

    /**
     * Coerce an Object to a String.
     * @param arg the Object to coerce
     * @return a String if it can be converted, null otherwise
     */
    protected String toString(Object arg) {
        return arg == null ? null : arg.toString();
    }

    /**
     * Gets the current introspector base.
     * <p>If the reference has been collected, this method will recreate the underlying introspector.</p>
     * @return the introspector
     */
    // CSOFF: DoubleCheckedLocking
    protected final Introspector base() {
        Introspector intro = ref.get();
        if (intro == null) {
            // double checked locking is ok (fixed by Java 5 memory model).
            synchronized(this) {
                intro = ref.get();
                if (intro == null) {
                    intro = new Introspector(rlog);
                    ref = new SoftReference<Introspector>(intro);
                }
            }
        }
        return intro;
    }
    // CSON: DoubleCheckedLocking

    @Override
    public void setClassLoader(ClassLoader loader) {
        base().setLoader(loader);
    }

    /**
     * Gets a class by name through this introspector class loader.
     * @param className the class name
     * @return the class instance or null if it could not be found
     */
    public final Class<?> getClassByName(String className) {
        return base().getClassByName(className);
    }
    
    /**
     * Gets the field named by <code>key</code> for the class <code>c</code>.
     *
     * @param c     Class in which the field search is taking place
     * @param key   Name of the field being searched for
     * @return a {@link java.lang.reflect.Field} or null if it does not exist or is not accessible
     * */
    public final Field getField(Class<?> c, String key) {
        return base().getField(c, key);
    }

    /**
     * Gets the accessible field names known for a given class.
     * @param c the class
     * @return the class field names
     */
    public final String[] getFieldNames(Class<?> c) {
        return base().getFieldNames(c);
    }

    /**
     * Gets the method defined by <code>name</code> and
     * <code>params</code> for the Class <code>c</code>.
     *
     * @param c Class in which the method search is taking place
     * @param name Name of the method being searched for
     * @param params An array of Objects (not Classes) that describe the
     *               the parameters
     *
     * @return a {@link java.lang.reflect.Method}
     *  or null if no unambiguous method could be found through introspection.
     */
    public final Method getMethod(Class<?> c, String name, Object[] params) {
        return base().getMethod(c, new MethodKey(name, params));
    }

    /**
     * Gets the method defined by <code>key</code> and for the Class <code>c</code>.
     *
     * @param c Class in which the method search is taking place
     * @param key MethodKey of the method being searched for
     *
     * @return a {@link java.lang.reflect.Method}
     *  or null if no unambiguous method could be found through introspection.
     */
    public final Method getMethod(Class<?> c, MethodKey key) {
        return base().getMethod(c, key);
    }


    /**
     * Gets the accessible methods names known for a given class.
     * @param c the class
     * @return the class method names
     */
    public final String[] getMethodNames(Class<?> c) {
        return base().getMethodNames(c);
    }
            
    /**
     * Gets all the methods with a given name from this map.
     * @param c the class
     * @param methodName the seeked methods name
     * @return the array of methods
     */
    public final Method[] getMethods(Class<?> c, final String methodName) {
        return base().getMethods(c, methodName);
    }
    /**
     * Creates a new UberspectImpl.
     * @param runtimeLogger the logger used for all logging needs
     */
    public Uberspect(Log runtimeLogger) {
        rlog = runtimeLogger;
        ref = new SoftReference<Introspector>(null);
    }

    /**
     * Resets this Uberspect class loader.
     * @param cloader the class loader to use
     */
    public void setLoader(ClassLoader cloader) {
        base().setLoader(cloader);
    }


    /**
     * Returns a general constructor.
     * @param ctorHandle the object
     * @param args contructor arguments
     * @return a {@link java.lang.reflect.Constructor}
     *  or null if no unambiguous contructor could be found through introspection.
     */
    protected final Constructor<?> getConstructor(Object ctorHandle, Object[] args) {
        String className = null;
        Class<?> clazz = null;
        if (ctorHandle instanceof Class<?>) {
            clazz = (Class<?>) ctorHandle;
            className = clazz.getName();
        } else if (ctorHandle != null) {
            className = ctorHandle.toString();
        } else {
            return null;
        }
        return base().getConstructor(clazz, new MethodKey(className, args));
    }

    /**
     * Returns a general method.
     * @param obj the object
     * @param name the method name
     * @param args method arguments
     * @return a {@link AbstractExecutor.Method}.
     */
    protected final AbstractExecutor.Method getMethodExecutor(Object obj, String name, Object[] args) {
        AbstractExecutor.Method me = new MethodExecutor(base(), obj, name, args);
        return me.isAlive() ? me : null;
    }

    /**
     * Return a property getter.
     * @param obj the object to base the property from.
     * @param identifier property name
     * @return a {@link AbstractExecutor.Get}.
     */
    protected final AbstractExecutor.Get getGetExecutor(Object obj, Object identifier) {
        final Class<?> claz = obj.getClass();
        final String property = toString(identifier);
        final Introspector is = base();
        AbstractExecutor.Get executor;
        // first try for a getFoo() type of property (also getfoo() )
        if (property != null) {
            executor = new PropertyGetExecutor(is, claz, property);
            if (executor.isAlive()) {
                return executor;
            }
        //}
        // look for boolean isFoo()
        //if (property != null) {
            executor = new BooleanGetExecutor(is, claz, property);
            if (executor.isAlive()) {
                return executor;
            }
        }
        // let's see if we are a map...
        executor = new MapGetExecutor(is, claz, identifier);
        if (executor.isAlive()) {
            return executor;
        }
        // let's see if we can convert the identifier to an int,
        // if obj is an array or a list, we can still do something
        Integer index = toInteger(identifier);
        if (index != null) {
            executor = new ListGetExecutor(is, claz, index);
            if (executor.isAlive()) {
                return executor;
            }
        }
        // if that didn't work, look for set("foo")
        executor = new DuckGetExecutor(is, claz, identifier);
        if (executor.isAlive()) {
            return executor;
        }
        // if that didn't work, look for set("foo")
        executor = new DuckGetExecutor(is, claz, property);
        if (executor.isAlive()) {
            return executor;
        }
        return null;
    }

    /**
     * Return a property setter.
     * @param obj the object to base the property from.
     * @param identifier property name (or identifier)
     * @param arg value to set
     * @return a {@link AbstractExecutor.Set}.
     */
    public final AbstractExecutor.Set getSetExecutor(final Object obj, final Object identifier, Object arg) {
        final Class<?> claz = obj.getClass();
        final String property = toString(identifier);
        final Introspector is = base();
        AbstractExecutor.Set executor;
        // first try for a setFoo() type of property (also setfoo() )
        if (property != null) {
            executor = new PropertySetExecutor(is, claz, property, arg);
            if (executor.isAlive()) {
                return executor;
            }
        }
        // let's see if we are a map...
        executor = new MapSetExecutor(is, claz, identifier, arg);
        if (executor.isAlive()) {
            return executor;
        }
        // let's see if we can convert the identifier to an int,
        // if obj is an array or a list, we can still do something
        Integer index = toInteger(identifier);
        if (index != null) {
            executor = new ListSetExecutor(is, claz, index, arg);
            if (executor.isAlive()) {
                return executor;
            }
        }
        // if that didn't work, look for set(foo)
        executor = new DuckSetExecutor(is, claz, identifier, arg);
        if (executor.isAlive()) {
            return executor;
        }
        // if that didn't work, look for set("foo")
        executor = new DuckSetExecutor(is, claz, property, arg);
        if (executor.isAlive()) {
            return executor;
        }
        return null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<?> getIterator(Object obj, JexlInfoHandle info) {
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
            throw new JexlException(info.jexlInfo(), "unable to generate iterator()", xany);
        }
        return null;
    }
    
    @Override
    public JexlMethod getMethod(Object obj, String method, Object[] args, JexlInfoHandle info) {
        return getMethodExecutor(obj, method, args);
    }

    @Override
    public JexlMethod getConstructor(Object ctorHandle, Object[] args, JexlInfoHandle info) {
        final Constructor<?> ctor = getConstructor(ctorHandle, args);
        if (ctor != null) {
            return new ConstructorMethod(ctor);
        } else {
            return null;
        }
    }

    @Override
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier, JexlInfoHandle info) {
        JexlPropertyGet get = getGetExecutor(obj, identifier);
        if (get == null && obj != null && identifier != null) {
            get = getIndexedGet(obj, identifier.toString());
            if (get == null) {
                Field field = getField(obj, identifier.toString(), info.jexlInfo());
                if (field != null) {
                    return new FieldPropertyGet(field);
                }
            }
        }
        return get;
    }

    @Override
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, Object arg, JexlInfoHandle info) {
        JexlPropertySet set = getSetExecutor(obj, identifier, arg);
        if (set == null && obj != null && identifier != null) {
            Field field = getField(obj, identifier.toString(), info.jexlInfo());
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
     * @param obj the object
     * @param name the field name
     * @param info debug info
     * @return a {@link Field}.
     */
    protected Field getField(Object obj, String name, JexlInfo info) {
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
     */
    protected static final class IndexedType implements JexlPropertyGet {
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
        protected IndexedType(String name, Class<?> c, Method[] gets, Method[] sets) {
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
                return TRY_FAILED;
            }
        }

        @Override
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
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
        public Object invokeGet(Object object, Object key) throws Exception {
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
        public Object invokeSet(Object object, Object key, Object value) throws Exception {
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
     * A JexlMethod that wraps constructor.
     */
    protected final class ConstructorMethod implements JexlMethod {
        /** The wrapped constructor. */
        private final Constructor<?> ctor;

        /**
         * Creates a constructor method.
         * @param theCtor the constructor to wrap
         */
        protected ConstructorMethod(Constructor<?> theCtor) {
            this.ctor = theCtor;
        }

        @Override
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

        @Override
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

        @Override
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        @Override
        public boolean isCacheable() {
            return true;
        }

        @Override
        public Class<?> getReturnType() {
            return ctor.getDeclaringClass();
        }
    }

    /**
     * A JexlPropertyGet for public fields.
     */
    protected static final class FieldPropertyGet implements JexlPropertyGet {
        /**
         * The public field.
         */
        private final Field field;

        /**
         * Creates a new instance of FieldPropertyGet.
         * @param theField the class public field
         */
        protected FieldPropertyGet(Field theField) {
            field = theField;
        }

        @Override
        public Object invoke(Object obj) throws Exception {
            return field.get(obj);
        }

        @Override
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

        @Override
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        @Override
        public boolean isCacheable() {
            return true;
        }
    }

    /**
     * A JexlPropertySet for public fields.
     */
    protected static final class FieldPropertySet implements JexlPropertySet {
        /**
         * The public field.
         */
        private final Field field;

        /**
         * Creates a new instance of FieldPropertySet.
         * @param theField the class public field
         */
        protected FieldPropertySet(Field theField) {
            field = theField;
        }

        @Override
        public Object invoke(Object obj, Object arg) throws Exception {
            field.set(obj, arg);
            return arg;
        }

        @Override
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
        @Override
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCacheable() {
            return true;
        }
    }
}
