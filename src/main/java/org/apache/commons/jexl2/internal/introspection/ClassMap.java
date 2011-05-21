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
package org.apache.commons.jexl2.internal.introspection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * A cache of introspection information for a specific class instance.
 * Keys objects by an agregation of the method name and the classes
 * that make up the parameters.
 * <p>
 * Originally taken from the Velocity tree so we can be self-sufficient.
 * </p>
 * @see MethodKey
 * @since 1.0
 */
final class ClassMap {
    /** cache of methods. */
    private final MethodCache methodCache;
    /** cache of fields. */
    private final Map<String, Field> fieldCache;

    /**
     * Standard constructor.
     *
     * @param aClass the class to deconstruct.
     * @param log the logger.
     */
    ClassMap(Class<?> aClass, Log log) {
        // eagerly cache methods
        methodCache = createMethodCache(aClass, log);
        // eagerly cache public fields
        fieldCache = createFieldCache(aClass);
    }

    /**
     * Find a Field using its name.
     * <p>The clazz parameter <strong>must</strong> be this ClassMap key.</p>
     * @param clazz the class to introspect
     * @param fname the field name
     * @return A Field object representing the field to invoke or null.
     */
    Field findField(final Class<?> clazz, final String fname) {
        return fieldCache.get(fname);
    }

    /**
     * Gets the field names cached by this map.
     * @return the array of field names
     */
    String[] getFieldNames() {
        return fieldCache.keySet().toArray(new String[fieldCache.size()]);
    }

    /**
     * Creates a map of all public fields of a given class.
     * @param clazz the class to introspect
     * @return the map of fields (may be the empty map, can not be null)
     */
    private static Map<String,Field> createFieldCache(Class<?> clazz) {
        Field[] fields = clazz.getFields();
        if (fields.length > 0) {
            Map<String, Field> cache = new HashMap<String, Field>();
            for(Field field : fields) {
                cache.put(field.getName(), field);
            }
            return cache;
        } else {
            return Collections.emptyMap();
        }
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
     * @param key the method key
     * @return A Method object representing the method to invoke or null.
     * @throws MethodKey.AmbiguousException When more than one method is a match for the parameters.
     */
    Method findMethod(final MethodKey key)
            throws MethodKey.AmbiguousException {
        return methodCache.get(key);
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
     * <p>
     * It stores the association between:
     *  - a key made of a method name & an array of argument types.
     *  - a method.
     * </p>
     * <p>
     * Since the invocation of the associated method is dynamic, there is no need (nor way) to differentiate between
     * foo(int,int) & foo(Integer,Integer) since in practise, only the latter form will be used through a call.
     * This of course, applies to all 8 primitive types.
     * </p>
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
        private final Map<MethodKey, Method> methods = new HashMap<MethodKey, Method>();
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
         * @param methodKey the method key
         * @return A Method object representing the method to invoke or null.
         * @throws MethodKey.AmbiguousException When more than one method is a match for the parameters.
         */
        Method get(final MethodKey methodKey) throws MethodKey.AmbiguousException {
            synchronized (methodMap) {
                Method cacheEntry = methods.get(methodKey);
                // We looked this up before and failed.
                if (cacheEntry == CACHE_MISS) {
                    return null;
                }

                if (cacheEntry == null) {
                    try {
                        // That one is expensive...
                        cacheEntry = methodMap.find(methodKey);
                        if (cacheEntry != null) {
                            methods.put(methodKey, cacheEntry);
                        } else {
                            methods.put(methodKey, CACHE_MISS);
                        }
                    } catch (MethodKey.AmbiguousException ae) {
                        // that's a miss :-)
                        methods.put(methodKey, CACHE_MISS);
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
                if (methods.get(methodKey) == null) {
                    methods.put(methodKey, method);
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
}