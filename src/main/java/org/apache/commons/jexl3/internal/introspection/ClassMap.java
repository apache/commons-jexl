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

import org.apache.commons.logging.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A cache of introspection information for a specific class instance.
 * Keys objects by an aggregation of the method name and the classes
 * that make up the parameters.
 * <p>
 * Originally taken from the Velocity tree so we can be self-sufficient.
 * </p>
 * @see MethodKey
 * @since 1.0
 */
final class ClassMap {
    /**
     * A method that returns itself used as a marker for cache miss,
     * allows the underlying cache map to be strongly typed.
     * @return itself as a method
     */
    public static Method cacheMiss() {
        try {
            return ClassMap.class.getMethod("cacheMiss");
        } catch (Exception xio) {
            // this really cant make an error...
            return null;
        }
    }

    /** The cache miss marker method. */
    private static final Method CACHE_MISS = cacheMiss();
    /**
     * This is the cache to store and look up the method information.
     * <p>
     * It stores the association between:
     * - a key made of a method name and an array of argument types.
     * - a method.
     * </p>
     * <p>
     * Since the invocation of the associated method is dynamic, there is no need (nor way) to differentiate between
     * foo(int,int) and foo(Integer,Integer) since in practice only the latter form will be used through a call.
     * This of course, applies to all 8 primitive types.
     * </p>
     * Uses ConcurrentMap since 3.0, marginally faster than 2.1 under contention.
     */
    private final ConcurrentMap<MethodKey, Method> byKey = new ConcurrentHashMap<MethodKey, Method>();
    /**
     * Keep track of all methods with the same name; this is not modified after creation.
     */
    private final Map<String, Method[]> byName = new HashMap<String, Method[]>();
    /**
     * Cache of fields.
     */
    private final Map<String, Field> fieldCache;

    /**
     * Standard constructor.
     *
     * @param aClass the class to deconstruct.
     * @param permissions the permissions to apply during introspection
     * @param log    the logger.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    ClassMap(Class<?> aClass, Permissions permissions, Log log) {
        // eagerly cache methods
        create(this, permissions, aClass, log);
        // eagerly cache public fields
        Field[] fields = aClass.getFields();
        if (fields.length > 0) {
            Map<String, Field> cache = new HashMap<String, Field>();
            for (Field field : fields) {
                if (permissions.allow(field)) {
                    cache.put(field.getName(), field);
                }
            }
            fieldCache = cache;
        } else {
            fieldCache = Collections.emptyMap();
        }
    }

    /**
     * Find a Field using its name.
     * @param fname the field name
     * @return A Field object representing the field to invoke or null.
     */
    Field getField(final String fname) {
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
     * Gets the methods names cached by this map.
     * @return the array of method names
     */
    String[] getMethodNames() {
        return byName.keySet().toArray(new String[byName.size()]);
    }

    /**
     * Gets all the methods with a given name from this map.
     * @param methodName the seeked methods name
     * @return the array of methods (null or non-empty)
     */
    Method[] getMethods(final String methodName) {
        Method[] lm = byName.get(methodName);
        if (lm != null && lm.length > 0) {
            return lm.clone();
        } else {
            return null;
        }
    }

    /**
     * Find a Method using the method name and parameter objects.
     * <p>
     * Look in the methodMap for an entry. If found,
     * it'll either be a CACHE_MISS, in which case we
     * simply give up, or it'll be a Method, in which
     * case, we return it.
     * </p>
     * <p>
     * If nothing is found, then we must actually go
     * and introspect the method from the MethodMap.
     * </p>
     * @param methodKey the method key
     * @return A Method object representing the method to invoke or null.
     * @throws MethodKey.AmbiguousException When more than one method is a match for the parameters.
     */
    Method getMethod(final MethodKey methodKey) throws MethodKey.AmbiguousException {
        // Look up by key
        Method cacheEntry = byKey.get(methodKey);
        // We looked this up before and failed.
        if (cacheEntry == CACHE_MISS) {
            return null;
        } else if (cacheEntry == null) {
            try {
                // That one is expensive...
                Method[] methodList = byName.get(methodKey.getMethod());
                if (methodList != null) {
                    cacheEntry = methodKey.getMostSpecificMethod(methodList);
                }
                if (cacheEntry == null) {
                    byKey.put(methodKey, CACHE_MISS);
                } else {
                    byKey.put(methodKey, cacheEntry);
                }
            } catch (MethodKey.AmbiguousException ae) {
                // that's a miss :-)
                byKey.put(methodKey, CACHE_MISS);
                throw ae;
            }
        }

        // Yes, this might just be null.
        return cacheEntry;
    }

    /**
     * Populate the Map of direct hits. These are taken from all the public methods
     * that our class, its parents and their implemented interfaces provide.
     * @param cache          the ClassMap instance we create
     * @param permissions    the permissions to apply during introspection
     * @param classToReflect the class to cache
     * @param log            the Log
     */
    private static void create(ClassMap cache, Permissions permissions, Class<?> classToReflect, Log log) {
        //
        // Build a list of all elements in the class hierarchy. This one is bottom-first (i.e. we start
        // with the actual declaring class and its interfaces and then move up (superclass etc.) until we
        // hit java.lang.Object. That is important because it will give us the methods of the declaring class
        // which might in turn be abstract further up the tree.
        //
        // We also ignore all SecurityExceptions that might happen due to SecurityManager restrictions.
        //
        for (; classToReflect != null; classToReflect = classToReflect.getSuperclass()) {
            if (Modifier.isPublic(classToReflect.getModifiers())) {
                populateWithClass(cache, permissions, classToReflect, log);
            }
            Class<?>[] interfaces = classToReflect.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                populateWithInterface(cache, permissions, interfaces[i], log);
            }
        }
        // now that we've got all methods keyed in, lets organize them by name
        if (!cache.byKey.isEmpty()) {
            List<Method> lm = new ArrayList<Method>(cache.byKey.size());
            for (Method method : cache.byKey.values()) {
                lm.add(method);
            }
            // sort all methods by name
            Collections.sort(lm, new Comparator<Method>() {
                @Override
                public int compare(Method o1, Method o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            // put all lists of methods with same name in byName cache
            int start = 0;
            while (start < lm.size()) {
                String name = lm.get(start).getName();
                int end = start + 1;
                while (end < lm.size()) {
                    String walk = lm.get(end).getName();
                    if (walk.equals(name)) {
                        end += 1;
                    } else {
                        break;
                    }
                }
                Method[] lmn = lm.subList(start, end).toArray(new Method[end - start]);
                cache.byName.put(name, lmn);
                start = end;
            }
        }
    }

    /**
     * Recurses up interface hierarchy to get all super interfaces.
     * @param cache the cache to fill
     * @param permissions the permissions to apply during introspection
     * @param iface the interface to populate the cache from
     * @param log   the Log
     */
    private static void populateWithInterface(ClassMap cache, Permissions permissions, Class<?> iface, Log log) {
        if (Modifier.isPublic(iface.getModifiers())) {
            populateWithClass(cache, permissions, iface, log);
            Class<?>[] supers = iface.getInterfaces();
            for (int i = 0; i < supers.length; i++) {
                populateWithInterface(cache, permissions, supers[i], log);
            }
        }
    }

    /**
     * Recurses up class hierarchy to get all super classes.
     * @param cache the cache to fill
     * @param permissions the permissions to apply during introspection
     * @param clazz the class to populate the cache from
     * @param log   the Log
     */
    private static void populateWithClass(ClassMap cache, Permissions permissions, Class<?> clazz, Log log) {
        try {
            Method[] methods = clazz.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Method mi = methods[i];
                // add method to byKey cache; do not override
                MethodKey key = new MethodKey(mi);
                Method pmi = cache.byKey.putIfAbsent(key, permissions.allow(mi) ? mi : CACHE_MISS);
                if (pmi != null && pmi != CACHE_MISS && log.isDebugEnabled() && !key.equals(new MethodKey(pmi))) {
                    // foo(int) and foo(Integer) have the same signature for JEXL
                    log.debug("Method " + pmi + " is already registered, key: " + key.debugString());
                }
            }
        } catch (SecurityException se) {
            // Everybody feels better with...
            if (log.isDebugEnabled()) {
                log.debug("While accessing methods of " + clazz + ": ", se);
            }
        }
    }
}
