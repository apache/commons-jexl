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
package org.apache.commons.jexl.util.introspection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * Taken from the Velocity tree so we can be self-sufficient.
 *
 * A cache of introspection information for a specific class instance.
 * Keys objects by an agregation of the method name and the names of classes
 * that make up the parameters.
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:bob@werken.com">Bob McWhirter</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id$
 * @since 1.0
 */
public class ClassMap {
    /** cache of methods. */
    private final MethodCache methodCache;

    /**
     * Standard constructor.
     *
     * @param aClass the class to deconstruct.
     * @param log the logger.
     */
    public ClassMap(Class<?> aClass, Log log) {
        methodCache = createMethodCache(aClass, log);
    }

    /**
     * Gets the methods names cached by this map.
     * @return the array of method names
     */
    String[] getMethodNames() {
        return methodCache.names();
    }

    /**
     * Find a Method using the method name and parameter objects.
     *
     * @param name   The method name to look up.
     * @param params An array of parameters for the method.
     * @return A Method object representing the method to invoke or null.
     * @throws MethodMap.AmbiguousException When more than one method is a match for the parameters.
     */
    public Method findMethod(final String name, final Object[] params)
            throws MethodMap.AmbiguousException {
        return methodCache.get(name, params);
    }

    /**
     * Populate the Map of direct hits. These are taken from all the public methods
     * that our class, its parents and their implemented interfaces provide.
     * @param classToReflect the class to cache
     * @param log the Log
     * @return a newly allocated & filled up cache
     */
    private static MethodCache createMethodCache(Class<?> classToReflect, Log log) {
        //
        // Build a list of all elements in the class hierarchy. This one is bottom-first (i.e. we start
        // with the actual declaring class and its interfaces and then move up (superclass etc.) until we
        // hit java.lang.Object. That is important because it will give us the methods of the declaring class
        // which might in turn be abstract further up the tree.
        //
        // We also ignore all SecurityExceptions that might happen due to SecurityManager restrictions (prominently
        // hit with Tomcat 5.5).
        //
        // We can also omit all that complicated getPublic, getAccessible and upcast logic that the class map had up
        // until Velocity 1.4. As we always reflect all elements of the tree (that's what we have a cache for), we will
        // hit the public elements sooner or later because we reflect all the public elements anyway.
        //
        // Ah, the miracles of Java for(;;) ...
        MethodCache cache = new MethodCache();
        for (;classToReflect != null; classToReflect = classToReflect.getSuperclass()) {
            if (Modifier.isPublic(classToReflect.getModifiers())) {
                populateMethodCacheWith(cache, classToReflect, log);
            }
            Class<?>[] interfaces = classToReflect.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                populateMethodCacheWithInterface(cache, interfaces[i], log);
            }
        }
        return cache;
    }

    /**
     * Recurses up interface hierarchy to get all super interfaces.
     * @param cache the cache to fill
     * @param iface the interface to populate the cache from
     * @param log the Log
     */
    private static void populateMethodCacheWithInterface(MethodCache cache, Class<?> iface, Log log) {
        if (Modifier.isPublic(iface.getModifiers())) {
            populateMethodCacheWith(cache, iface, log);
        }
        Class<?>[] supers = iface.getInterfaces();
        for (int i = 0; i < supers.length; i++) {
            populateMethodCacheWithInterface(cache, supers[i], log);
        }
    }

    /**
     * Recurses up class hierarchy to get all super classes.
     * @param cache the cache to fill
     * @param clazz the class to populate the cache from
     * @param log the Log
     */
    private static void populateMethodCacheWith(MethodCache cache, Class<?> clazz, Log log) {
        try {
            Method[] methods = clazz.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                int modifiers = methods[i].getModifiers();
                if (Modifier.isPublic(modifiers)) {
                    cache.put(methods[i]);
                }
            }
        } catch (SecurityException se) {
            // Everybody feels better with...
            if (log.isDebugEnabled()) {
                log.debug("While accessing methods of " + clazz + ": ", se);
            }
        }
    }

    /**
     * This is the cache to store and look up the method information.
     *
     * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
     * @version $Id$
     * <p>
     * It stores the association between:
     *  - a key made of a method name & an array of argument types. (@see MethodCache.MethodKey)
     *  - a method.
     * </p>
     * <p>
     * Since the invocation of the associated method is dynamic, there is no need (nor way) to differentiate between
     * foo(int,int) & foo(Integer,Integer) since in practise, only the latter form will be used through a call.
     * This of course, applies to all 8 primitive types.
     * </p>
     * @version $Id$
     */
    static final class MethodCache {
        /**
         * A method that returns itself used as a marker for cache miss,
         * allows the underlying cache map to be strongly typed.
         * @return itself as a method
         */
        public static Method cacheMiss() {
            try {
                return MethodCache.class.getMethod("cacheMiss");
            } catch (Exception xio) {
                // this really cant make an error...
                return null;
            }
        }
        /** The cache miss marker method. */
        private static final Method CACHE_MISS = cacheMiss();
        /** The initial size of the primitive conversion map. */
        private static final int PRIMITIVE_SIZE = 13;
        /** The primitive type to class conversion map. */
        private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES;
        static {
            PRIMITIVE_TYPES = new HashMap<Class<?>, Class<?>>(PRIMITIVE_SIZE);
            PRIMITIVE_TYPES.put(Boolean.TYPE, Boolean.class);
            PRIMITIVE_TYPES.put(Byte.TYPE, Byte.class);
            PRIMITIVE_TYPES.put(Character.TYPE, Character.class);
            PRIMITIVE_TYPES.put(Double.TYPE, Double.class);
            PRIMITIVE_TYPES.put(Float.TYPE, Float.class);
            PRIMITIVE_TYPES.put(Integer.TYPE, Integer.class);
            PRIMITIVE_TYPES.put(Long.TYPE, Long.class);
            PRIMITIVE_TYPES.put(Short.TYPE, Short.class);
        }

        /** Converts a primitive type to its corresponding class.
         * <p>
         * If the argument type is primitive then we want to convert our
         * primitive type signature to the corresponding Object type so
         * introspection for methods with primitive types will work
         * correctly.
         * </p>
         * @param parm a may-be primitive type class
         * @return the equivalent object class 
         */
        static Class<?> primitiveClass(Class<?> parm) {
            // it is marginally faster to get from the map than call isPrimitive...
            //if (!parm.isPrimitive()) return parm;
            Class<?> prim = PRIMITIVE_TYPES.get(parm);
            return prim == null ? parm : prim;
        }
        /**
         * The method cache.
         * <p>
         * Cache of Methods, or CACHE_MISS, keyed by method
         * name and actual arguments used to find it.
         * </p>
         */
        private final Map<MethodKey, Method> cache = new HashMap<MethodKey, Method>();
        /**
         * Map of methods that are searchable according to method parameters to find a match.
         */
        private final MethodMap methodMap = new MethodMap();

        /**
         * Find a Method using the method name and parameter objects.
         *<p>
         * Look in the methodMap for an entry.  If found,
         * it'll either be a CACHE_MISS, in which case we
         * simply give up, or it'll be a Method, in which
         * case, we return it.
         *</p>
         * <p>
         * If nothing is found, then we must actually go
         * and introspect the method from the MethodMap.
         *</p>
         * @param name   The method name to look up.
         * @param params An array of parameters for the method.
         * @return A Method object representing the method to invoke or null.
         * @throws MethodMap.AmbiguousException When more than one method is a match for the parameters.
         */
        Method get(final String name, final Object[] params)
                throws MethodMap.AmbiguousException {
            return get(new MethodKey(name, params));
        }

        /**
         * Finds a Method using a MethodKey.
         * @param methodKey the method key
         * @return a method
         * @throws MethodMap.AmbiguousException if method resolution is ambiguous
         */
        Method get(final MethodKey methodKey)
                throws MethodMap.AmbiguousException {
            synchronized (methodMap) {
                Method cacheEntry = cache.get(methodKey);
                // We looked this up before and failed.
                if (cacheEntry == CACHE_MISS) {
                    return null;
                }

                if (cacheEntry == null) {
                    try {
                        // That one is expensive...
                        cacheEntry = methodMap.find(methodKey);
                        if (cacheEntry != null) {
                            cache.put(methodKey, cacheEntry);
                        } else {
                            cache.put(methodKey, CACHE_MISS);
                        }
                    } catch (MethodMap.AmbiguousException ae) {
                        // that's a miss :-)
                        cache.put(methodKey, CACHE_MISS);
                        throw ae;
                    }
                }

                // Yes, this might just be null.
                return cacheEntry;
            }
        }

        /**
         * Adds a method to the map.
         * @param method the method to add
         */
        void put(Method method) {
            synchronized (methodMap) {
                MethodKey methodKey = new MethodKey(method);
                // We don't overwrite methods. Especially not if we fill the
                // cache from defined class towards java.lang.Object because
                // abstract methods in superclasses would else overwrite concrete
                // classes further down the hierarchy.
                if (cache.get(methodKey) == null) {
                    cache.put(methodKey, method);
                    methodMap.add(method);
                }
            }
        }

        /**
         * Gets all the method names from this map.
         * @return the array of method name
         */
        String[] names() {
            synchronized (methodMap) {
                return methodMap.names();
            }
        }
    }

    /**
     * A method key for the introspector cache.
     * <p>
     * This replaces the original key scheme which used to build the key
     * by concatenating the method name and parameters class names as one string
     * with the exception that primitive types were converted to their object class equivalents.
     * </p>
     * <p>
     * The key is still based on the same information, it is just wrapped in an object instead.
     * Primitive type classes are converted to they object equivalent to make a key;
     * int foo(int) and int foo(Integer) do generate the same key.
     * </p>
     * A key can be constructed either from arguments (array of objects) or from parameters
     * (array of class).
     * Roughly 3x faster than string key to access the map & uses less memory.
     */
    static final class MethodKey {
        /** The hash code. */
        private final int hashCode;
        /** The method name. */
        private final String method;
        /** The parameters. */
        private final Class<?>[] params;
        /** A marker for empty parameter list. */
        private static final Class<?>[] NOARGS = new Class<?>[0];
        /** The hash code constants. */
        private static final int HASH = 37;

        /** Creates a MethodKey from a method.
         *  Used to store information in the method map.
         * @param aMethod the method to build the key from
         */
        MethodKey(Method aMethod) {
            this(aMethod.getName(), aMethod.getParameterTypes());
        }

        /** Creates a MethodKey from a method name and a set of arguments (objects).
         *  Used to query the method map.
         * @param aMethod the method name
         * @param args the arguments instances to match the method signature
         */
        MethodKey(String aMethod, Object[] args) {
            // !! keep this in sync with the other ctor (hash code) !!
            this.method = aMethod;
            int hash = this.method.hashCode();
            final int size;
             // CSOFF: InnerAssignment
            if (args != null && (size = args.length) > 0) {
                // CSON: InnerAssignment
                this.params = new Class<?>[size];
                for (int p = 0; p < size; ++p) {
                    // ctor(Object) : {
                    Object arg = args[p];
                    // no need to go thru primitive type conversion since these are objects
                    Class<?> parm = arg == null ? Object.class : arg.getClass();
                    // }
                    hash = (HASH * hash) + parm.hashCode();
                    this.params[p] = parm;
                }
            } else {
                this.params = NOARGS;
            }
            this.hashCode = hash;
        }

        /** Creates a MethodKey from a method name and a set of parameters (classes).
         *  Used to store information in the method map. ( @see MethodKey#primitiveClass )
         * @param aMethod the method name
         * @param args the argument classes to match the method signature
         */
        MethodKey(String aMethod, Class<?>[] args) {
            // !! keep this in sync with the other ctor (hash code) !!
            this.method = aMethod;
            int hash = this.method.hashCode();
            final int size;
            // CSOFF: InnerAssignment
            if (args != null && (size = args.length) > 0) {
                // CSON: InnerAssignment
                this.params = new Class<?>[size];
                for (int p = 0; p < size; ++p) {
                    // ctor(Class): {
                    Class<?> parm = MethodCache.primitiveClass(args[p]);
                    // }
                    hash = (HASH * hash) + parm.hashCode();
                    this.params[p] = parm;
                }
            } else {
                this.params = NOARGS;
            }
            this.hashCode = hash;
        }

        /**
         * Gets this key's method name.
         * @return the method name
         */
        String getMethod() {
            return method;
        }

        /**
         * Gets this key's method parameter classes.
         * @return the parameters
         */
        Class<?>[] getParameters() {
            return params;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object arg) {
            if (arg instanceof MethodKey) {
                MethodKey key = (MethodKey) arg;
                return method.equals(key.method) && java.util.Arrays.equals(params, key.params);
            }
            return false;
        }

        @Override
        /** Compatible with original string key. */
        public String toString() {
            StringBuilder builder = new StringBuilder(method);
            for (Class<?> c : params) {
                builder.append(c.getName());
            }
            return builder.toString();
        }
    }
}