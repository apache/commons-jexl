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

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import org.apache.commons.logging.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.List;

/**
 * Implementation of Uberspect to provide the default introspective
 * functionality of JEXL.
 * <p>
 * This is the class to derive to customize introspection.</p>
 *
 * @since 1.0
 */
public class Uberspect implements JexlUberspect {
    /** Publicly exposed special failure object returned by tryInvoke. */
    public static final Object TRY_FAILED = JexlEngine.TRY_FAILED;
    /** The logger to use for all warnings and errors. */
    protected final Log logger;
    /** The resolver strategy. */
    private final JexlUberspect.ResolverStrategy strategy;
    /** The permissions. */
    private final Permissions permissions;
    /** The introspector version. */
    private final AtomicInteger version;
    /** The soft reference to the introspector currently in use. */
    private volatile Reference<Introspector> ref;
    /** The class loader reference; used to recreate the introspector when necessary. */
    private volatile Reference<ClassLoader> loader;
    /**
     * The map from arithmetic classes to overloaded operator sets.
     * <p>
     * This keeps track of which operator methods are overloaded per JexlArithemtic classes
     * allowing a fail fast test during interpretation by avoiding seeking a method when there is none.
     */
    private final Map<Class<? extends JexlArithmetic>, Set<JexlOperator>> operatorMap;

    /**
     * Creates a new Uberspect.
     * @param runtimeLogger the logger used for all logging needs
     * @param sty the resolver strategy
     */
    public Uberspect(Log runtimeLogger, JexlUberspect.ResolverStrategy sty) {
        this(runtimeLogger, sty, null);
    }

    /**
     * Creates a new Uberspect.
     * @param runtimeLogger the logger used for all logging needs
     * @param sty the resolver strategy
     * @param perms the introspector permissions
     */
    public Uberspect(Log runtimeLogger, JexlUberspect.ResolverStrategy sty, Permissions perms) {
        logger = runtimeLogger;
        strategy = sty == null? JexlUberspect.JEXL_STRATEGY : sty;
        permissions  = perms;
        ref = new SoftReference<Introspector>(null);
        loader = new SoftReference<ClassLoader>(getClass().getClassLoader());
        operatorMap = new ConcurrentHashMap<Class<? extends JexlArithmetic>, Set<JexlOperator>>();
        version = new AtomicInteger(0);
    }

    /**
     * Gets the current introspector base.
     * <p>
     * If the reference has been collected, this method will recreate the underlying introspector.</p>
     * @return the introspector
     */
    // CSOFF: DoubleCheckedLocking
    protected final Introspector base() {
        Introspector intro = ref.get();
        if (intro == null) {
            // double checked locking is ok (fixed by Java 5 memory model).
            synchronized (this) {
                intro = ref.get();
                if (intro == null) {
                    intro = new Introspector(logger, loader.get(), permissions);
                    ref = new SoftReference<Introspector>(intro);
                    loader = new SoftReference<ClassLoader>(intro.getLoader());
                    version.incrementAndGet();
                }
            }
        }
        return intro;
    }
    // CSON: DoubleCheckedLocking

    @Override
    public void setClassLoader(ClassLoader nloader) {
        synchronized (this) {
            Introspector intro = ref.get();
            if (intro != null) {
                intro.setLoader(nloader);
            } else {
                intro = new Introspector(logger, nloader, permissions);
                ref = new SoftReference<Introspector>(intro);
            }
            loader = new SoftReference<ClassLoader>(intro.getLoader());
            operatorMap.clear();
            version.incrementAndGet();
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return loader.get();
    }

    @Override
    public int getVersion() {
        return version.intValue();
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
     * Gets the field named by
     * <code>key</code> for the class
     * <code>c</code>.
     *
     * @param c   Class in which the field search is taking place
     * @param key Name of the field being searched for
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
     * Gets the method defined by
     * <code>name</code> and
     * <code>params</code> for the Class
     * <code>c</code>.
     *
     * @param c      Class in which the method search is taking place
     * @param name   Name of the method being searched for
     * @param params An array of Objects (not Classes) that describe the
     *               the parameters
     *
     * @return a {@link java.lang.reflect.Method}
     *         or null if no unambiguous method could be found through introspection.
     */
    public final Method getMethod(Class<?> c, String name, Object[] params) {
        return base().getMethod(c, new MethodKey(name, params));
    }

    /**
     * Gets the method defined by
     * <code>key</code> and for the Class
     * <code>c</code>.
     *
     * @param c   Class in which the method search is taking place
     * @param key MethodKey of the method being searched for
     *
     * @return a {@link java.lang.reflect.Method}
     *         or null if no unambiguous method could be found through introspection.
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
     * @param c          the class
     * @param methodName the seeked methods name
     * @return the array of methods
     */
    public final Method[] getMethods(Class<?> c, final String methodName) {
        return base().getMethods(c, methodName);
    }

    @Override
    public JexlMethod getMethod(Object obj, String method, Object... args) {
        return MethodExecutor.discover(base(), obj, method, args);
    }

    @Override
    public List<PropertyResolver> getResolvers(JexlOperator op, Object obj) {
        return strategy.apply(op, obj);
    }

    @Override
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier) {
        return getPropertyGet(null, obj, identifier);
    }

    @Override
    public JexlPropertyGet getPropertyGet(
            final List<PropertyResolver> resolvers, final Object obj, final Object identifier
    ) {
        final Class<?> claz = obj.getClass();
        final String property = AbstractExecutor.castString(identifier);
        final Introspector is = base();
        final List<PropertyResolver> r = resolvers == null? strategy.apply(null, obj) : resolvers;
        JexlPropertyGet executor = null;
        for (PropertyResolver resolver : r) {
            if (resolver instanceof JexlResolver) {
                switch ((JexlResolver) resolver) {
                    case PROPERTY:
                        // first try for a getFoo() type of property (also getfoo() )
                        executor = PropertyGetExecutor.discover(is, claz, property);
                        if (executor == null) {
                            executor = BooleanGetExecutor.discover(is, claz, property);
                        }
                        break;
                    case MAP:
                        // let's see if we are a map...
                        executor = MapGetExecutor.discover(is, claz, identifier);
                        break;
                    case LIST:
                        // let's see if this is a list or array
                        Integer index = AbstractExecutor.castInteger(identifier);
                        if (index != null) {
                            executor = ListGetExecutor.discover(is, claz, index);
                        }
                        break;
                    case DUCK:
                        // if that didn't work, look for get(foo)
                        executor = DuckGetExecutor.discover(is, claz, identifier);
                        if (executor == null && property != null && property != identifier) {
                            // look for get("foo") if we did not try yet (just above)
                            executor = DuckGetExecutor.discover(is, claz, property);
                        }
                        break;
                    case FIELD:
                        // a field may be? (can not be a number)
                        executor = FieldGetExecutor.discover(is, claz, property);
                        // static class fields (enums included)
                        if (obj instanceof Class<?>) {
                            executor = FieldGetExecutor.discover(is, (Class<?>) obj, property);
                        }
                        break;
                    case CONTAINER:
                        // or an indexed property?
                        executor = IndexedType.discover(is, obj, property);
                        break;
                    default:
                        continue; // in case we add new ones in enum
                }
            } else {
                executor = resolver.getPropertyGet(this, obj, identifier);
            }
            if (executor != null) {
                return executor;
            }
        }
        return null;
    }

    @Override
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, final Object arg) {
        return getPropertySet(null, obj, identifier, arg);
    }

    @Override
    public JexlPropertySet getPropertySet(
            final List<PropertyResolver> resolvers, final Object obj, final Object identifier, final Object arg
    ) {
        final Class<?> claz = obj.getClass();
        final String property = AbstractExecutor.castString(identifier);
        final Introspector is = base();
        final List<PropertyResolver> actual = resolvers == null? strategy.apply(null, obj) : resolvers;
        JexlPropertySet executor = null;
        for (PropertyResolver resolver : actual) {
            if (resolver instanceof JexlResolver) {
                switch ((JexlResolver) resolver) {
                    case PROPERTY:
                        // first try for a setFoo() type of property (also setfoo() )
                        executor = PropertySetExecutor.discover(is, claz, property, arg);
                        break;
                    case MAP:
                        // let's see if we are a map...
                        executor = MapSetExecutor.discover(is, claz, identifier, arg);
                        break;
                    case LIST:
                    // let's see if we can convert the identifier to an int,
                        // if obj is an array or a list, we can still do something
                        Integer index = AbstractExecutor.castInteger(identifier);
                        if (index != null) {
                            executor = ListSetExecutor.discover(is, claz, identifier, arg);
                        }
                        break;
                    case DUCK:
                        // if that didn't work, look for set(foo)
                        executor = DuckSetExecutor.discover(is, claz, identifier, arg);
                        if (executor == null && property != null && property != identifier) {
                            executor = DuckSetExecutor.discover(is, claz, property, arg);
                        }
                        break;
                    case FIELD:
                        // a field may be?
                        executor = FieldSetExecutor.discover(is, claz, property, arg);
                        break;
                    case CONTAINER:
                    default:
                        continue; // in case we add new ones in enum
                }
            } else {
                executor = resolver.getPropertySet(this, obj, identifier, arg);
            }
            if (executor != null) {
                return executor;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<?> getIterator(Object obj) {
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
            JexlMethod it = getMethod(obj, "iterator", (Object[]) null);
            if (it != null && Iterator.class.isAssignableFrom(it.getReturnType())) {
                return (Iterator<Object>) it.invoke(obj, (Object[]) null);
            }
        } catch (Exception xany) {
            if (logger != null && logger.isDebugEnabled()) {
                logger.info("unable to solve iterator()", xany);
            }
        }
        return null;
    }

    @Override
    public JexlMethod getConstructor(Object ctorHandle, Object... args) {
        return ConstructorMethod.discover(base(), ctorHandle, args);
    }

    /**
     * The concrete uberspect Arithmetic class.
     */
    protected class ArithmeticUberspect implements JexlArithmetic.Uberspect {
        /** The arithmetic instance being analyzed. */
        private final JexlArithmetic arithmetic;
        /** The set of overloaded operators. */
        private final Set<JexlOperator> overloads;

        /**
         * Creates an instance.
         * @param theArithmetic the arithmetic instance
         * @param theOverloads  the overloaded operators
         */
        private ArithmeticUberspect(JexlArithmetic theArithmetic, Set<JexlOperator> theOverloads) {
            this.arithmetic = theArithmetic;
            this.overloads = theOverloads;
        }

        @Override
        public JexlMethod getOperator(JexlOperator operator, Object... args) {
            return overloads.contains(operator) && args != null
                   ? getMethod(arithmetic, operator.getMethodName(), args)
                   : null;
        }

        @Override
        public boolean overloads(JexlOperator operator) {
            return overloads.contains(operator);
        }
    }

    @Override
    public JexlArithmetic.Uberspect getArithmetic(JexlArithmetic arithmetic) {
        JexlArithmetic.Uberspect jau = null;
        if (arithmetic != null) {
            final Class<? extends JexlArithmetic> aclass = arithmetic.getClass();
            Set<JexlOperator> ops = operatorMap.get(aclass);
            if (ops == null) {
                ops = EnumSet.noneOf(JexlOperator.class);
                // deal only with derived classes
                if (!JexlArithmetic.class.equals(aclass)) {
                    for (JexlOperator op : JexlOperator.values()) {
                        Method[] methods = getMethods(arithmetic.getClass(), op.getMethodName());
                        if (methods != null) {
                            mloop:
                            for (Method method : methods) {
                                Class<?>[] parms = method.getParameterTypes();
                                if (parms.length != op.getArity()) {
                                    continue;
                                }
                                // filter method that is an actual overload:
                                // - not inherited (not declared by base class)
                                // - nor overriden (not present in base class)
                                if (!JexlArithmetic.class.equals(method.getDeclaringClass())) {
                                    try {
                                        JexlArithmetic.class.getMethod(method.getName(), method.getParameterTypes());
                                    } catch (NoSuchMethodException xmethod) {
                                        // method was not found in JexlArithmetic; this is an operator definition
                                        ops.add(op);
                                    }
                                }
                            }
                        }
                    }
                }
                // register this arithmetic class in the operator map
                operatorMap.put(aclass, ops);
            }
            jau = new ArithmeticUberspect(arithmetic, ops);
        }
        return jau;
    }
}
