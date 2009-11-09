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
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

import org.apache.commons.logging.Log;

/**
 * This basic function of this class is to return a Method object for a
 * particular class given the name of a method and the parameters to the method
 * in the form of an Object[]
 * <p/>
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
 * This mapping is performed for all the methods in a class and stored for
 *
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:bob@werken.com">Bob McWhirter</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @author <a href="mailto:paulo.gaspar@krankikom.de">Paulo Gaspar</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id$
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
     * Gets the method defined by the <code>MethodKey</code> for the class <code>c</code>.
     *
     * @param c     Class in which the method search is taking place
     * @param key   Key of the method being searched for
     * @return The desired Method object.
     * @throws IllegalArgumentException     When the parameters passed in can not be used for introspection.
     * @throws MethodKey.AmbiguousException When the method map contains more than
     *  one match for the requested signature.
     *  
     */
    //CSOFF: RedundantThrows
    public Method getMethod(Class<?> c, MethodKey key) {
        ClassMap classMap = getMap(c);
        return classMap.findMethod(key);

    }
    // CSON: RedundantThrows

    /**
     * Gets the accessible methods names known for a given class.
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
     * A Constructor get cache-miss.
     */
    private static class CacheMiss {
        /** The constructor used as cache-miss. */
        public CacheMiss() {}
    }
    /** The cache-miss marker for the constructors map. */
    private static final Constructor<?> CTOR_MISS = CacheMiss.class.getConstructors()[0];
    
    /**
     * Sets the class loader used to solve constructors.
     * <p>Also cleans the constructors cache.</p>
     * @param cloader the class loader; if null, use this instance class loader
     */
    public void setLoader(ClassLoader cloader) {
        if (cloader == null) {
            cloader = getClass().getClassLoader();
        }
        if (!cloader.equals(loader)) {
            synchronized(constructorsMap) {
                loader = cloader;
                constructorsMap.clear();
                constructibleClasses.clear();
            }
        }
    }

    /**
     * Gets the constructor defined by the <code>MethodKey</code>.
     *
     * @param key   Key of the constructor being searched for
     * @return The desired Constructor object.
     * @throws IllegalArgumentException     When the parameters passed in can not be used for introspection.
     * @throws MethodKey.AmbiguousException When the method map contains more than
     *  one match for the requested signature.
     */
    public Constructor<?> getConstructor(final MethodKey key) {
        return getConstructor(null, key);
    }
    
    /**
     * Gets the constructor defined by the <code>MethodKey</code>.
     * @param c the class we want to instantiate
     * @param key   Key of the constructor being searched for
     * @return The desired Constructor object.
     * @throws IllegalArgumentException     When the parameters passed in can not be used for introspection.
     * @throws MethodKey.AmbiguousException When the method map contains more than
     *  one match for the requested signature.
     */
    //CSOFF: RedundantThrows
    public Constructor<?> getConstructor(final Class<?> c, final MethodKey key) {
        Constructor<?> ctor = null;
        synchronized(constructorsMap) {
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
                    for(Constructor<?> ictor : clazz.getConstructors()) {
                        l.add(ictor);
                    }
                    // try to find one
                    ctor = key.getMostSpecificConstructor(l);
                    if (ctor != null) {
                        constructorsMap.put(key, ctor);
                    } else {
                        constructorsMap.put(key, CTOR_MISS);
                    }
                } catch(ClassNotFoundException xnotfound) {
                    if (rlog.isDebugEnabled()) {
                        rlog.debug("could not load class " + cname, xnotfound);
                    }
                    ctor = null;
                } catch(MethodKey.AmbiguousException xambiguous) {
                    rlog.warn("ambiguous ctor detected for " + cname, xambiguous);
                    ctor = null;
                }
            }
        }
        return ctor;
    }
    // CSON: RedundantThrows

    /**
     * Gets the ClassMap for a given class.
     * @param c the class
     * @return the class map
     */
    private ClassMap getMap(Class<?> c) {
        synchronized (classMethodMaps) {
            ClassMap classMap = classMethodMaps.get(c);
            if (classMap == null) {
                classMap = new ClassMap(c,rlog);
                classMethodMaps.put(c, classMap);
            }
            return classMap;
        }
    }
}