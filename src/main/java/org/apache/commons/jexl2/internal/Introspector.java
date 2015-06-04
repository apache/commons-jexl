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

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.apache.commons.jexl2.internal.introspection.IntrospectorBase;
import org.apache.commons.jexl2.internal.introspection.MethodKey;

import org.apache.commons.logging.Log;

/**
 *  Default introspection services.
 *  <p>Finding methods as well as property getters &amp; setters.</p>
 * @since 1.0
 */
public class Introspector {
    /** The logger to use for all warnings &amp; errors. */
    protected final Log rlog;
    /** The soft reference to the introspector currently in use. */
    private volatile SoftReference<IntrospectorBase> ref;
   
    /**
     * Creates an introspector.
     * @param log the logger to use for warnings.
     */
    protected Introspector(Log log) {
        rlog = log;
        ref = new SoftReference<IntrospectorBase>(null);
    }

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
    protected final IntrospectorBase base() {
        IntrospectorBase intro = ref.get();
        if (intro == null) {
            // double checked locking is ok (fixed by Java 5 memory model).
            synchronized(this) {
                intro = ref.get();
                if (intro == null) {
                    intro = new IntrospectorBase(rlog);
                    ref = new SoftReference<IntrospectorBase>(intro);
                }
            }
        }
        return intro;
    }
    // CSON: DoubleCheckedLocking

    /**
     * Sets the underlying class loader for class solving resolution.
     * @param loader the loader to use
     */
    public void setClassLoader(ClassLoader loader) {
        base().setLoader(loader);
    }

    /**
     * Gets a class by name through this introspector class loader.
     * @param className the class name
     * @return the class instance or null if it could not be found
     */
    public Class<?> getClassByName(String className) {
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
     * Returns a general constructor.
     * @param ctorHandle the object
     * @param args contructor arguments
     * @return a {@link java.lang.reflect.Constructor}
     *  or null if no unambiguous contructor could be found through introspection.
     */
    public final Constructor<?> getConstructor(Object ctorHandle, Object[] args) {
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
    public final AbstractExecutor.Method getMethodExecutor(Object obj, String name, Object[] args) {
        AbstractExecutor.Method me = new MethodExecutor(this, obj, name, args);
        return me.isAlive() ? me : null;
    }

    /**
     * Return a property getter.
     * @param obj the object to base the property from.
     * @param identifier property name
     * @return a {@link AbstractExecutor.Get}.
     */
    public final AbstractExecutor.Get getGetExecutor(Object obj, Object identifier) {
        final Class<?> claz = obj.getClass();
        final String property = toString(identifier);
        AbstractExecutor.Get executor;
        // first try for a getFoo() type of property (also getfoo() )
        if (property != null) {
            executor = new PropertyGetExecutor(this, claz, property);
            if (executor.isAlive()) {
                return executor;
            }
        //}
        // look for boolean isFoo()
        //if (property != null) {
            executor = new BooleanGetExecutor(this, claz, property);
            if (executor.isAlive()) {
                return executor;
            }
        }
        // let's see if we are a map...
        executor = new MapGetExecutor(this, claz, identifier);
        if (executor.isAlive()) {
            return executor;
        }
        // let's see if we can convert the identifier to an int,
        // if obj is an array or a list, we can still do something
        Integer index = toInteger(identifier);
        if (index != null) {
            executor = new ListGetExecutor(this, claz, index);
            if (executor.isAlive()) {
                return executor;
            }
        }
        // if that didn't work, look for set("foo")
        executor = new DuckGetExecutor(this, claz, identifier);
        if (executor.isAlive()) {
            return executor;
        }
        // if that didn't work, look for set("foo")
        executor = new DuckGetExecutor(this, claz, property);
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
        AbstractExecutor.Set executor;
        // first try for a setFoo() type of property (also setfoo() )
        if (property != null) {
            executor = new PropertySetExecutor(this, claz, property, arg);
            if (executor.isAlive()) {
                return executor;
            }
        }
        // let's see if we are a map...
        executor = new MapSetExecutor(this, claz, identifier, arg);
        if (executor.isAlive()) {
            return executor;
        }
        // let's see if we can convert the identifier to an int,
        // if obj is an array or a list, we can still do something
        Integer index = toInteger(identifier);
        if (index != null) {
            executor = new ListSetExecutor(this, claz, index, arg);
            if (executor.isAlive()) {
                return executor;
            }
        }
        // if that didn't work, look for set(foo)
        executor = new DuckSetExecutor(this, claz, identifier, arg);
        if (executor.isAlive()) {
            return executor;
        }
        // if that didn't work, look for set("foo")
        executor = new DuckSetExecutor(this, claz, property, arg);
        if (executor.isAlive()) {
            return executor;
        }
        return null;
    }
}
