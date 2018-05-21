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
    private static class CacheMiss {
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
     * the logger.
     */
    protected final Log rlog;
    /**
     * The class loader used to solve constructors if needed.
     */
    private ClassLoader loader;
    /**
     * The permissions.
     */
    private final Permissions permissions;
    /**
     * The read/write lock.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * Holds the method maps for the classes we know about, keyed by Class.
     */
    private final Map<Class<?>, ClassMap> classMethodMaps = new HashMap<Class<?>, ClassMap>();
    /**
     * Holds the map of classes ctors we know about as well as unknown ones.
     */
    private final Map<MethodKey, Constructor<?>> constructorsMap = new HashMap<MethodKey, Constructor<?>>();
    /**
     * Holds the set of classes we have introspected.
     */
    private final Map<String, Class<?>> constructibleClasses = new HashMap<String, Class<?>>();

    /**
     * Create the introspector.
     * @param log     the logger to use
     * @param cloader the class loader
     */
    public Introspector(Log log, ClassLoader cloader) {
        this(log, cloader, null);
    }

    /**
     * Create the introspector.
     * @param log     the logger to use
     * @param cloader the class loader
     * @param perms the permissions
     */
    public Introspector(Log log, ClassLoader cloader, Permissions perms) {
        this.rlog = log;
        this.loader = cloader;
        this.permissions = perms != null? perms : Permissions.DEFAULT;
    }

    /**
     * Gets a class by name through this introspector class loader.
     * @param className the class name
     * @return the class instance or null if it could not be found
     */
    public Class<?> getClassByName(String className) {
        try {
            return Class.forName(className, false, loader);
        } catch (ClassNotFoundException xignore) {
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
    public Method getMethod(Class<?> c, String name, Object[] params) {
        return getMethod(c, new MethodKey(name, params));
    }

    /**
     * Gets the method defined by the <code>MethodKey</code> for the class <code>c</code>.
     *
     * @param c   Class in which the method search is taking place
     * @param key Key of the method being searched for
     * @return The desired method object
     * @throws MethodKey.AmbiguousException if no unambiguous method could be found through introspection
     */
    public Method getMethod(Class<?> c, MethodKey key) {
        try {
            return getMap(c).getMethod(key);
        } catch (MethodKey.AmbiguousException xambiguous) {
            // whoops. Ambiguous and not benign. Make a nice log message and return null...
            if (rlog != null && xambiguous.isSevere() && rlog.isInfoEnabled()) {
                rlog.info("ambiguous method invocation: "
                        + c.getName() + "."
                        + key.debugString(), xambiguous);
            }
            return null;
        }
    }

    /**
     * Gets the field named by <code>key</code> for the class <code>c</code>.
     *
     * @param c   Class in which the field search is taking place
     * @param key Name of the field being searched for
     * @return the desired field or null if it does not exist or is not accessible
     * */
    public Field getField(Class<?> c, String key) {
        return getMap(c).getField(key);
    }

    /**
     * Gets the array of accessible field names known for a given class.
     * @param c the class
     * @return the class field names
     */
    public String[] getFieldNames(Class<?> c) {
        if (c == null) {
            return new String[0];
        }
        ClassMap classMap = getMap(c);
        return classMap.getFieldNames();
    }

    /**
     * Gets the array of accessible methods names known for a given class.
     * @param c the class
     * @return the class method names
     */
    public String[] getMethodNames(Class<?> c) {
        if (c == null) {
            return new String[0];
        }
        ClassMap classMap = getMap(c);
        return classMap.getMethodNames();
    }

    /**
     * Gets the array of accessible method known for a given class.
     * @param c          the class
     * @param methodName the method name
     * @return the array of methods (null or not empty)
     */
    public Method[] getMethods(Class<?> c, String methodName) {
        if (c == null) {
            return null;
        }
        ClassMap classMap = getMap(c);
        return classMap.getMethods(methodName);
    }

    /**
     * Gets the constructor defined by the <code>MethodKey</code>.
     *
     * @param key Key of the constructor being searched for
     * @return The desired constructor object
     * or null if no unambiguous constructor could be found through introspection.
     */
    public Constructor<?> getConstructor(final MethodKey key) {
        return getConstructor(null, key);
    }

    /**
     * Gets the constructor defined by the <code>MethodKey</code>.
     * @param c   the class we want to instantiate
     * @param key Key of the constructor being searched for
     * @return The desired constructor object
     * or null if no unambiguous constructor could be found through introspection.
     */
    public Constructor<?> getConstructor(final Class<?> c, final MethodKey key) {
        Constructor<?> ctor;
        try {
            lock.readLock().lock();
            ctor = constructorsMap.get(key);
            if (ctor != null) {
                // miss or not?
                return CTOR_MISS.equals(ctor) ? null : ctor;
            }
        } finally {
            lock.readLock().unlock();
        }
        // let's introspect...
        try {
            lock.writeLock().lock();
            // again for kicks
            ctor = constructorsMap.get(key);
            if (ctor != null) {
                // miss or not?
                return CTOR_MISS.equals(ctor) ? null : ctor;
            }
            final String cname = key.getMethod();
            // do we know about this class?
            Class<?> clazz = constructibleClasses.get(cname);
            try {
                // do find the most specific ctor
                if (clazz == null) {
                    if (c != null && c.getName().equals(key.getMethod())) {
                        clazz = c;
                    } else {
                        clazz = loader.loadClass(cname);
                    }
                    // add it to list of known loaded classes
                    constructibleClasses.put(cname, clazz);
                }
                List<Constructor<?>> l = new ArrayList<Constructor<?>>();
                for (Constructor<?> ictor : clazz.getConstructors()) {
                    if (permissions.allow(ictor)) {
                        l.add(ictor);
                    }
                }
                // try to find one
                ctor = key.getMostSpecificConstructor(l.toArray(new Constructor<?>[l.size()]));
                if (ctor != null) {
                    constructorsMap.put(key, ctor);
                } else {
                    constructorsMap.put(key, CTOR_MISS);
                }
            } catch (ClassNotFoundException xnotfound) {
                if (rlog != null && rlog.isDebugEnabled()) {
                    rlog.debug("unable to find class: "
                            + cname + "."
                            + key.debugString(), xnotfound);
                }
                ctor = null;
            } catch (MethodKey.AmbiguousException xambiguous) {
                if (rlog != null  && xambiguous.isSevere() &&  rlog.isInfoEnabled()) {
                    rlog.info("ambiguous constructor invocation: "
                            + cname + "."
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
     * Gets the ClassMap for a given class.
     * @param c the class
     * @return the class map
     */
    private ClassMap getMap(Class<?> c) {
        ClassMap classMap;
        try {
            lock.readLock().lock();
            classMap = classMethodMaps.get(c);
        } finally {
            lock.readLock().unlock();
        }
        if (classMap == null) {
            try {
                lock.writeLock().lock();
                // try again
                classMap = classMethodMaps.get(c);
                if (classMap == null) {
                    classMap = new ClassMap(c, permissions, rlog);
                    classMethodMaps.put(c, classMap);
                }
            } finally {
                lock.writeLock().unlock();
            }

        }
        return classMap;
    }

    /**
     * Sets the class loader used to solve constructors.
     * <p>Also cleans the constructors and methods caches.</p>
     * @param cloader the class loader; if null, use this instance class loader
     */
    public void setLoader(ClassLoader cloader) {
        ClassLoader previous = loader;
        if (cloader == null) {
            cloader = getClass().getClassLoader();
        }
        if (!cloader.equals(loader)) {
            try {
                lock.writeLock().lock();
                // clean up constructor and class maps
                Iterator<Map.Entry<MethodKey, Constructor<?>>> mentries = constructorsMap.entrySet().iterator();
                while (mentries.hasNext()) {
                    Map.Entry<MethodKey, Constructor<?>> entry = mentries.next();
                    Class<?> clazz = entry.getValue().getDeclaringClass();
                    if (isLoadedBy(previous, clazz)) {
                        mentries.remove();
                        // the method name is the name of the class
                        constructibleClasses.remove(entry.getKey().getMethod());
                    }
                }
                // clean up method maps
                Iterator<Map.Entry<Class<?>, ClassMap>> centries = classMethodMaps.entrySet().iterator();
                while (centries.hasNext()) {
                    Map.Entry<Class<?>, ClassMap> entry = centries.next();
                    Class<?> clazz = entry.getKey();
                    if (isLoadedBy(previous, clazz)) {
                        centries.remove();
                    }
                }
                loader = cloader;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Gets the class loader used by this introspector.
     * @return the class loader
     */
    public ClassLoader getLoader() {
        return loader;
    }

    /**
     * Checks whether a class is loaded through a given class loader or one of its ascendants.
     * @param loader the class loader
     * @param clazz  the class to check
     * @return true if clazz was loaded through the loader, false otherwise
     */
    private static boolean isLoadedBy(ClassLoader loader, Class<?> clazz) {
        if (loader != null) {
            ClassLoader cloader = clazz.getClassLoader();
            while (cloader != null) {
                if (cloader.equals(loader)) {
                    return true;
                } else {
                    cloader = cloader.getParent();
                }
            }
        }
        return false;
    }
}