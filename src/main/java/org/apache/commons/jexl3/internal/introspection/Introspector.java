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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.logging.Log;

/**
 * This basic function of this class is to return a Method object for a
 * particular class given the name of a method and the parameters to the method
 * in the form of an Object[].
 *
 * <p>The first time the Introspector sees a class it creates a class method map
 * for the class in question.
 * Basically the class method map is a Hashtable where Method objects are keyed by the aggregation of
 * the method name and the array of parameters classes.
 * This mapping is performed for all the public methods of a class and stored.</p>
 *
 * @since 1.0
 */
public final class Introspector {
    /**
     * A Constructor get cache-miss.
     */
    private static final class CacheMiss {
        /** The constructor used as cache-miss. */
        @SuppressWarnings("unused")
        public CacheMiss() {
        }
    }
    /**
     * The cache-miss marker for the constructors map.
     */
    private static final Constructor<?> CTOR_MISS = CacheMiss.class.getConstructors()[0];
    /**
     * Checks whether a class is loaded through a given class loader or one of its ascendants.
     * @param loader the class loader
     * @param clazz  the class to check
     * @return true if clazz was loaded through the loader, false otherwise
     */
    private static boolean isLoadedBy(final ClassLoader loader, final Class<?> clazz) {
        if (loader != null) {
            ClassLoader cloader = clazz.getClassLoader();
            while (cloader != null) {
                if (cloader.equals(loader)) {
                    return true;
                }
                cloader = cloader.getParent();
            }
        }
        return false;
    }
    /**
     * the logger.
     */
    private final Log logger;
    /**
     * The class loader used to solve constructors if needed.
     */
    private ClassLoader loader;
    /**
     * The permissions.
     */
    private final JexlPermissions permissions;
    /**
     * The read/write lock.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * Holds the method maps for the classes we know about, keyed by Class.
     */
    private final Map<Class<?>, ClassMap> classMethodMaps = new HashMap<>();
    /**
     * Holds the map of classes ctors we know about as well as unknown ones.
     */
    private final Map<MethodKey, Constructor<?>> constructorsMap = new HashMap<>();

    /**
     * Holds the set of classes we have introspected.
     */
    private final Map<String, Class<?>> constructibleClasses = new HashMap<>();

    /**
     * Create the introspector.
     * @param log     the logger to use
     * @param cloader the class loader
     */
    public Introspector(final Log log, final ClassLoader cloader) {
        this(log, cloader, null);
    }

    /**
     * Create the introspector.
     * @param log     the logger to use
     * @param cloader the class loader
     * @param perms the permissions
     */
    public Introspector(final Log log, final ClassLoader cloader, final JexlPermissions perms) {
        this.logger = log;
        this.loader = cloader;
        this.permissions = perms == null ? JexlPermissions.RESTRICTED : perms;
    }

    /**
     * Gets a class by name through this introspector class loader.
     * @param className the class name
     * @return the class instance or null if it could not be found
     */
    public Class<?> getClassByName(final String className) {
        try {
            final Class<?> clazz = Class.forName(className, false, loader);
            return permissions.allow(clazz)? clazz : null;
        } catch (final ClassNotFoundException xignore) {
            return null;
        }
    }

    /**
     * Gets the constructor defined by the {@code MethodKey}.
     * @param c   the class we want to instantiate
     * @param key Key of the constructor being searched for
     * @return The desired constructor object
     * or null if no unambiguous constructor could be found through introspection.
     */
    public Constructor<?> getConstructor(final Class<?> c, final MethodKey key) {
        Constructor<?> ctor;
        lock.readLock().lock();
        try {
            ctor = constructorsMap.get(key);
            if (ctor != null) {
                // miss or not?
                return CTOR_MISS.equals(ctor) ? null : ctor;
            }
        } finally {
            lock.readLock().unlock();
        }
        // let's introspect...
        lock.writeLock().lock();
        try {
            // again for kicks
            ctor = constructorsMap.get(key);
            if (ctor != null) {
                // miss or not?
                return CTOR_MISS.equals(ctor) ? null : ctor;
            }
            final String constructorName = key.getMethod();
            // do we know about this class?
            Class<?> clazz = constructibleClasses.get(constructorName);
            try {
                // do find the most specific ctor
                if (clazz == null) {
                    if (c != null && c.getName().equals(key.getMethod())) {
                        clazz = c;
                    } else {
                        clazz = loader.loadClass(constructorName);
                    }
                    // add it to list of known loaded classes
                    constructibleClasses.put(constructorName, clazz);
                }
                final List<Constructor<?>> l = new ArrayList<>();
                for (final Constructor<?> ictor : clazz.getConstructors()) {
                    if (permissions.allow(ictor)) {
                        l.add(ictor);
                    }
                }
                // try to find one
                ctor = key.getMostSpecificConstructor(l.toArray(new Constructor<?>[0]));
                if (ctor != null) {
                    constructorsMap.put(key, ctor);
                } else {
                    constructorsMap.put(key, CTOR_MISS);
                }
            } catch (final ClassNotFoundException xnotfound) {
                if (logger != null && logger.isDebugEnabled()) {
                    logger.debug("unable to find class: "
                            + constructorName + "."
                            + key.debugString(), xnotfound);
                }
            } catch (final MethodKey.AmbiguousException xambiguous) {
                if (logger != null  && xambiguous.isSevere() &&  logger.isInfoEnabled()) {
                    logger.info("ambiguous constructor invocation: "
                            + constructorName + "."
                            + key.debugString(), xambiguous);
                }
                ctor = null;
            }
            return ctor;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the constructor defined by the {@code MethodKey}.
     *
     * @param key Key of the constructor being searched for
     * @return The desired constructor object
     * or null if no unambiguous constructor could be found through introspection.
     */
    public Constructor<?> getConstructor(final MethodKey key) {
        return getConstructor(null, key);
    }

    /**
     * Gets the field named by {@code key} for the class {@code c}.
     *
     * @param c   Class in which the field search is taking place
     * @param key Name of the field being searched for
     * @return the desired field or null if it does not exist or is not accessible
     */
    public Field getField(final Class<?> c, final String key) {
        return getMap(c).getField(key);
    }

    /**
     * Gets the array of accessible field names known for a given class.
     * @param c the class
     * @return the class field names
     */
    public String[] getFieldNames(final Class<?> c) {
        if (c == null) {
            return new String[0];
        }
        final ClassMap classMap = getMap(c);
        return classMap.getFieldNames();
    }

    /**
     * Gets the class loader used by this introspector.
     * @return the class loader
     */
    public ClassLoader getLoader() {
        return loader;
    }

    /**
     * Gets the ClassMap for a given class.
     * @param c the class
     * @return the class map
     */
    private ClassMap getMap(final Class<?> c) {
        ClassMap classMap;
        lock.readLock().lock();
        try {
            classMap = classMethodMaps.get(c);
        } finally {
            lock.readLock().unlock();
        }
        if (classMap == null) {
            lock.writeLock().lock();
            try {
                // try again
                classMap = classMethodMaps.get(c);
                if (classMap == null) {
                    classMap = permissions.allow(c)
                            ? new ClassMap(c, permissions, logger)
                            : ClassMap.empty();
                    classMethodMaps.put(c, classMap);
                }
            } finally {
                lock.writeLock().unlock();
            }

        }
        return classMap;
    }

    /**
     * Gets the method defined by the {@code MethodKey} for the class {@code c}.
     *
     * @param c   Class in which the method search is taking place
     * @param key Key of the method being searched for
     * @return The desired method object
     * @throws MethodKey.AmbiguousException if no unambiguous method could be found through introspection
     */
    public Method getMethod(final Class<?> c, final MethodKey key) {
        try {
            return getMap(c).getMethod(key);
        } catch (final MethodKey.AmbiguousException xambiguous) {
            // whoops. Ambiguous and not benign. Make a nice log message and return null...
            if (logger != null && xambiguous.isSevere() && logger.isInfoEnabled()) {
                logger.info("ambiguous method invocation: "
                        + c.getName() + "."
                        + key.debugString(), xambiguous);
            }
            return null;
        }
    }

    /**
     * Gets a method defined by a class, a name and a set of parameters.
     * @param c      the class
     * @param name   the method name
     * @param params the method parameters
     * @return the desired method object
     * @throws MethodKey.AmbiguousException if no unambiguous method could be found through introspection
     */
    public Method getMethod(final Class<?> c, final String name, final Object... params) {
        return getMethod(c, new MethodKey(name, params));
    }

    /**
     * Gets the array of accessible methods names known for a given class.
     * @param c the class
     * @return the class method names
     */
    public String[] getMethodNames(final Class<?> c) {
        if (c == null) {
            return new String[0];
        }
        final ClassMap classMap = getMap(c);
        return classMap.getMethodNames();
    }

    /**
     * Gets the array of accessible method known for a given class.
     * @param c          the class
     * @param methodName the method name
     * @return the array of methods (null or not empty)
     */
    public Method[] getMethods(final Class<?> c, final String methodName) {
        if (c == null) {
            return null;
        }
        final ClassMap classMap = getMap(c);
        return classMap.getMethods(methodName);
    }

    /**
     * Sets the class loader used to solve constructors.
     * <p>Also cleans the constructors and methods caches.</p>
     * @param classLoader the class loader; if null, use this instance class loader
     */
    public void setLoader(final ClassLoader classLoader) {
        final ClassLoader previous = loader;
        final ClassLoader current = classLoader == null ? getClass().getClassLoader() : classLoader;
        if (!current.equals(loader)) {
            lock.writeLock().lock();
            try {
                // clean up constructor and class maps
                final Iterator<Map.Entry<MethodKey, Constructor<?>>> mentries = constructorsMap.entrySet().iterator();
                while (mentries.hasNext()) {
                    final Map.Entry<MethodKey, Constructor<?>> entry = mentries.next();
                    final Class<?> clazz = entry.getValue().getDeclaringClass();
                    if (isLoadedBy(previous, clazz)) {
                        mentries.remove();
                        // the method name is the name of the class
                        constructibleClasses.remove(entry.getKey().getMethod());
                    }
                }
                // clean up method maps
                final Iterator<Map.Entry<Class<?>, ClassMap>> centries = classMethodMaps.entrySet().iterator();
                while (centries.hasNext()) {
                    final Map.Entry<Class<?>, ClassMap> entry = centries.next();
                    final Class<?> clazz = entry.getKey();
                    if (isLoadedBy(previous, clazz)) {
                        centries.remove();
                    }
                }
                loader = current;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
