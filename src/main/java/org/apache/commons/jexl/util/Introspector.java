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

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import org.apache.commons.jexl.util.introspection.Uberspect;
import org.apache.commons.jexl.util.introspection.UberspectImpl;
import org.apache.commons.jexl.util.introspection.MethodKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Default introspection services.
 *  <p>Finding methods as well as property getters & setters.</p>
 */
public class Introspector  {
    /** The default uberspector that handles all introspection patterns. */
    private static volatile Uberspect uberSpect;
    /** The logger to use for all warnings & errors. */
    protected final Log rlog;
    /** The (low level) introspector to use for introspection services. */
    protected final org.apache.commons.jexl.util.introspection.Introspector introspector;

    /**
     *  Gets the default instance of Uberspect.
     * <p>This is lazily initialized to avoid building a default instance if there
     * is no use for it. The main reason for not using the default Uberspect instance is to
     * be able to use a (low level) introspector created with a given logger
     * instead of the default one.</p>
     *  @return Uberspect the default uberspector instance.
     */
    // CSOFF: DoubleCheckedLocking
    public static Uberspect getUberspect() {
        // uses a double-locking pattern since java5 memory model allows it
        if (uberSpect == null) {
            synchronized (Uberspect.class) {
                if (uberSpect == null) {
                    Log logger = LogFactory.getLog(Introspector.class);
                    uberSpect = new UberspectImpl(logger);
                }
            }
        }
        return uberSpect;
    }
    // CSON: DoubleCheckedLocking

    /**
     * Creates a new instance of Uberspect.
     * @param logger the logger used by this Uberspect.
     * @return the new instance
     */
    public static Uberspect getUberspect(Log logger) {
        return new UberspectImpl(logger);
    }

    /**
     * Creates an introspector.
     * @param log the logger to use for warnings.
     * @param is the low level introspector.
     */
    public Introspector(Log log, org.apache.commons.jexl.util.introspection.Introspector is) {
        rlog = log;
        introspector = is;
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
        } catch(NumberFormatException xnumber) {
            return null;
        }
    }

    /**
     * Coerce an Object to a String.
     * @param arg the Object to coerce
     * @return a String if it can be converted, null otherwise
     */
    protected String toString(Object arg) {
        return arg == null? null : arg.toString();
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
     * @return The desired Method object.
     * @throws IllegalArgumentException When the parameters passed in can not be used for introspection.
     * CSOFF: RedundantThrows
     */
    protected final Method getMethod(Class<?> c, String name, Object[] params) throws IllegalArgumentException {
        return introspector.getMethod(c, new MethodKey(name, params));
    }

    /**
     * Gets the method defined by <code>key</code> and for the Class <code>c</code>.
     *
     * @param c Class in which the method search is taking place
     * @param key MethodKey of the method being searched for
     *
     * @return The desired Method object.
     * @throws IllegalArgumentException When the parameters passed in can not be used for introspection.
     * CSOFF: RedundantThrows
     */
    protected final Method getMethod(Class<?> c, MethodKey key) throws IllegalArgumentException {
        return introspector.getMethod(c, key);
    }

    /**
     * Returns a general constructor.
     * @param ctorHandle the object
     * @param args contrusctor arguments
     * @return a {@link java.lang.reflect.Constructor}
     */
    public final Constructor<?> getConstructor(Object ctorHandle, Object[] args) {
        String className = null;
        if (ctorHandle instanceof Class<?>) {
            className = ((Class<?>) ctorHandle).getName();
        } else if (ctorHandle != null) {
            className = ctorHandle.toString();
        } else {
            return null;
        }
        return introspector.getConstructor(new MethodKey(className, args));
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
        return me.isAlive()? me : null;
    }

    /**
     * Return a property getter.
     * @param obj the object to get the property from.
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
        }
        // look for boolean isFoo()
        if (property != null) {
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
        // if that didn't work, look for get("foo")
        executor = new DuckGetExecutor(this, claz, identifier);
        if (executor.isAlive()) {
            return executor;
        }
        return null;
    }

    /**
     * Return a property setter.
     * @param obj the object to get the property from.
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
        // if that didn't work, look for get("foo")
        executor = new DuckSetExecutor(this, claz, property, arg);
        if (executor.isAlive()) {
            return executor;
        }
        return null;
     }

}
