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

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;

/**
 * This basic function of this class is to return a Method object for a
 * particular class given the name of a method and the parameters to the method
 * in the form of an Object[]
 * <p>
 * The first time the Introspector sees a class it creates a class method map
 * for the class in question. Basically the class method map is a Hastable where
 * Method objects are keyed by a concatenation of the method name and the names
 * of classes that make up the parameters.
 *
 * For example, a method with the following signature:
 *
 * public void method(String a, StringBuffer b)
 *
 * would be mapped by the key:
 *
 * "method" + "java.lang.String" + "java.lang.StringBuffer"
 *
 * This mapping is performed for all the methods in a class and stored.
 * @since 1.0
 */
public class IntrospectorBase {
    /** the logger. */
    protected final Log rlog;
    /**
     * Holds the method maps for the classes we know about, keyed by Class.
     */
    private final Map<Class<?>, ClassMap> classMethodMaps = new HashMap<Class<?>, ClassMap>();
    /**
     * The class loader used to solve constructors if needed.
     */
    private ClassLoader loader;
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
     * @param log the logger to use
     */
    public IntrospectorBase(Log log) {
        this.rlog = log;
        loader = getClass().getClassLoader();
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
     * Gets the method defined by the <code>MethodKey</code> for the class <code>c</code>.
     *
     * @param c     Class in which the method search is taking place
     * @param key   Key of the method being searched for
     * @return The desired method object
     * @throws MethodKey.AmbiguousException if no unambiguous method could be found through introspection
     */
    public Method getMethod(Class<?> c, MethodKey key) {
        try {
            ClassMap classMap = getMap(c);
            return classMap.findMethod(key);
        } catch (MethodKey.AmbiguousException xambiguous) {
            // whoops.  Ambiguous.  Make a nice log message and return null...
            if (rlog != null && rlog.isInfoEnabled()) {
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
     * @param c     Class in which the field search is taking place
     * @param key   Name of the field being searched for
     * @return the desired field or null if it does not exist or is not accessible
     * */
    public Field getField(Class<?> c, String key) {
        ClassMap classMap = getMap(c);
        return classMap.findField(c, key);
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
     * @param c the class
     * @param methodName the method name
     * @return the array of methods (null or not empty)
     */
    public Method[] getMethods(Class<?> c, String methodName) {
        if (c == null) {
            return null;
        }
        ClassMap classMap = getMap(c);
        return classMap.get(methodName);
    }

    /**
     * A Constructor get cache-miss.
     */
    private static class CacheMiss {
        /** The constructor used as cache-miss. */
        @SuppressWarnings("unused")
        public CacheMiss() {}
    }
    
    /** The cache-miss marker for the constructors map. */
    private static final Constructor<?> CTOR_MISS = CacheMiss.class.getConstructors()[0];

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
            // clean up constructor and class maps
            synchronized (constructorsMap) {
                Iterator<Map.Entry<MethodKey, Constructor<?>>> entries = constructorsMap.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<MethodKey, Constructor<?>> entry = entries.next();
                    Class<?> clazz = entry.getValue().getDeclaringClass();
                    if (isLoadedBy(previous, clazz)) {
                        entries.remove();
                        // the method name is the name of the class
                        constructibleClasses.remove(entry.getKey().getMethod());
                    }
                }
            }
            // clean up method maps
            synchronized (classMethodMaps) {
                Iterator<Map.Entry<Class<?>, ClassMap>> entries = classMethodMaps.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<Class<?>, ClassMap> entry = entries.next();
                    Class<?> clazz = entry.getKey();
                    if (isLoadedBy(previous, clazz)) {
                        entries.remove();
                    }
                }
            }
            loader = cloader;
        }
    }

    /**
     * Checks whether a class is loaded through a given class loader or one of its ascendants.
     * @param loader the class loader
     * @param clazz the class to check
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

    /**
     * Gets the constructor defined by the <code>MethodKey</code>.
     *
     * @param key   Key of the constructor being searched for
     * @return The desired constructor object
     * or null if no unambiguous constructor could be found through introspection.
     */
    public Constructor<?> getConstructor(final MethodKey key) {
        return getConstructor(null, key);
    }

    /**
     * Gets the constructor defined by the <code>MethodKey</code>.
     * @param c the class we want to instantiate
     * @param key   Key of the constructor being searched for
     * @return The desired constructor object
     * or null if no unambiguous constructor could be found through introspection.
     */
    public Constructor<?> getConstructor(final Class<?> c, final MethodKey key) {
        Constructor<?> ctor = null;
        synchronized (constructorsMap) {
            ctor = constructorsMap.get(key);
            // that's a clear miss
            if (CTOR_MISS.equals(ctor)) {
                return null;
            }
            // let's introspect...
            if (ctor == null) {
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
                    List<Constructor<?>> l = new LinkedList<Constructor<?>>();
                    for (Constructor<?> ictor : clazz.getConstructors()) {
                        l.add(ictor);
                    }
                    // try to find one
                    ctor = key.getMostSpecificConstructor(l);
                    if (ctor != null) {
                        constructorsMap.put(key, ctor);
                    } else {
                        constructorsMap.put(key, CTOR_MISS);
                    }
                } catch (ClassNotFoundException xnotfound) {
                    if (rlog != null && rlog.isInfoEnabled()) {
                        rlog.info("unable to find class: "
                                + cname + "."
                                + key.debugString(), xnotfound);
                    }
                    ctor = null;
                } catch (MethodKey.AmbiguousException xambiguous) {
                    if (rlog != null && rlog.isInfoEnabled()) {
                        rlog.info("ambiguous constructor invocation: "
                                + cname + "."
                                + key.debugString(), xambiguous);
                    }
                    ctor = null;
                }
            }
            return ctor;
        }
    }

    /**
     * Gets the ClassMap for a given class.
     * @param c the class
     * @return the class map
     */
    private ClassMap getMap(Class<?> c) {
        synchronized (classMethodMaps) {
            ClassMap classMap = classMethodMaps.get(c);
            if (classMap == null) {
                classMap = new ClassMap(c, rlog);
                classMethodMaps.put(c, classMap);
            }
            return classMap;
        }
    }
}
