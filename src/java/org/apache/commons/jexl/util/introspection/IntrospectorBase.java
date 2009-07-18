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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private final Log rlog;

    /**
     * Holds the method maps for the classes we know about, keyed by Class
     * Made WeakHashMap so we wont prevent a class from being GCed.
     * object.
     */
    protected final Map<Class<?>, ClassMap> classMethodMaps = new java.util.WeakHashMap<Class<?>, ClassMap>();

    /**
     * Holds the qualified class names for the classes we hold in the
     * classMethodMaps hash.
     */
    private Set<String> cachedClassNames = new HashSet<String>();

    /**
     * Create the introspector.
     * @param log the logger to use
     */
    public IntrospectorBase(Log log) {
        this.rlog = log;
    }

    /**
     * Gets the method defined by <code>name</code> and <code>params</code>
     * for the Class <code>c</code>.
     *
     * @param c      Class in which the method search is taking place
     * @param name   Name of the method being searched for
     * @param params An array of Objects (not Classes) that describe the the
     *               parameters
     * @return The desired Method object.
     * @throws IllegalArgumentException     When the parameters passed in can not be used for introspection.
     * @throws MethodMap.AmbiguousException When the method map contains more than 
     *  one match for the requested signature.
     *  CSOFF: RedundantThrows
     */
    public Method getMethod(Class<?> c, String name, Object[] params)
    throws IllegalArgumentException, MethodMap.AmbiguousException {
        if (c == null) {
            throw new IllegalArgumentException("Introspector.getMethod(): Class method key was null: " + name);
        }

        if (params == null) {
            throw new IllegalArgumentException("params object is null!");
        }

        ClassMap classMap = getMap(c);

        return classMap.findMethod(name, params);
    } // CSON: RedundantThrows

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
     * Gets the ClassMap for a given class.
     * @return the class map
     */
    private ClassMap getMap(Class<?> c) {
        synchronized (classMethodMaps) {
            ClassMap classMap = classMethodMaps.get(c);
            /*
             * if we don't have this, check to see if we have it by name. if so,
             * then we have a classloader change so dump our caches.
             */

            if (classMap == null) {
                if (cachedClassNames.contains(c.getName())) {
                    /*
                     * we have a map for a class with same name, but not this
                     * class we are looking at. This implies a classloader
                     * change, so dump
                     */
                    clearCache();
                }

                classMap = createClassMap(c);
            }
            return classMap;
        }
    }

    /**
     * Creates a class map for specific class and registers it in the cache.
     * Also adds the qualified name to the name->class map for later Classloader
     * change detection.
     * @param c class.
     * @return a {@link ClassMap}
     */
    protected ClassMap createClassMap(Class<?> c) {
        ClassMap classMap = new ClassMap(c,rlog);
        classMethodMaps.put(c, classMap);
        cachedClassNames.add(c.getName());

        return classMap;
    }

    /**
     * Clears the classmap and classname caches.
     */
    protected void clearCache() {
        /*
         * since we are synchronizing on this object, we have to clear it rather
         * than just dump it.
         */
        classMethodMaps.clear();

        /*
         * for speed, we can just make a new one and let the old one be GC'd
         */
        cachedClassNames = new HashSet<String>();
    }
}