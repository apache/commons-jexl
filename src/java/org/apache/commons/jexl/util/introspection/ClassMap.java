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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

/**
 * Taken from the Velocity tree so we can be self-sufficient
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

    /**
     * Class passed into the constructor used to as the basis for the Method map.
     */
    private final Class clazz;
    /** logger. */
    private final Log rlog;
    /** cache of methods. */
    private final MethodCache methodCache;

    /**
     * Standard constructor.
     *
     * @param aClass the class to deconstruct.
     * @param log the logger.
     */
    public ClassMap(Class aClass, Log log) {
        clazz = aClass;
        this.rlog = log;
        methodCache = new MethodCache();

        populateMethodCache();
    }

    /**
     * @return the class object whose methods are cached by this map.
     */
    Class getCachedClass() {
        return clazz;
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
     * Populate the Map of direct hits. These
     * are taken from all the public methods
     * that our class, its parents and their implemented interfaces provide.
     */
    private void populateMethodCache() {
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
        List<Class> classesToReflect = new ArrayList<Class>();

        // Ah, the miracles of Java for(;;) ...
        for (Class classToReflect = getCachedClass(); classToReflect != null;
                classToReflect = classToReflect.getSuperclass()) {
            if (Modifier.isPublic(classToReflect.getModifiers())) {
                classesToReflect.add(classToReflect);
            }
            Class[] interfaces = classToReflect.getInterfaces();
            for (int i = 0; i < interfaces.length; i++) {
                if (Modifier.isPublic(interfaces[i].getModifiers())) {
                    classesToReflect.add(interfaces[i]);
                }
            }
        }

        for (Iterator<Class> it = classesToReflect.iterator(); it.hasNext();) {
            Class classToReflect = it.next();

            try {
                Method[] methods = classToReflect.getMethods();

                for (int i = 0; i < methods.length; i++) {
                    // Strictly spoken that check shouldn't be necessary
                    // because getMethods only returns public methods.
                    int modifiers = methods[i].getModifiers();
                    if (Modifier.isPublic(modifiers)) //  && !)
                    {
                        // Some of the interfaces contain abstract methods. That is fine, because the actual object must
                        // implement them anyway (else it wouldn't be implementing the interface).
                        // If we find an abstract method in a non-interface, we skip it, because we do want to make sure
                        // that no abstract methods end up in  the cache.
                        if (classToReflect.isInterface() || !Modifier.isAbstract(modifiers)) {
                            methodCache.put(methods[i]);
                        }
                    }
                }
            } catch (SecurityException se) // Everybody feels better with...
            {
                if (rlog != null && rlog.isDebugEnabled()) {
                    rlog.debug("While accessing methods of " + classToReflect + ": ", se);
                }
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
     *  a key made of a method name & an array of argument types. (@see MethodCache.MethodKey)
     *  a method.
     * </p>
     * <p>
     * Since the invocation of the associated method is dynamic, there is no need (nor way) to differentiate between
     * foo(int,int) & foo(Integer,Integer) since in practise, only the latter form will be used through a call.
     * This of course, applies to all 8 primitive types.
     * </p>
     * @version $Id$
     */
    static class MethodCache {

        /**
         * A method that returns itself used as a marker for cache miss,
         * allows the underlying cache map to be strongly typed.
         */
        public static Method CacheMiss() {
            try {
                return MethodCache.class.getMethod("CacheMiss");
            } // this really cant make an error...
            catch (Exception xio) {
            }
            return null;
        }
        private static final Method CACHE_MISS = CacheMiss();
        private static final Map<Class, Class> convertPrimitives;
        static {
            convertPrimitives = new HashMap<Class, Class>(13);
            convertPrimitives.put(Boolean.TYPE, Boolean.class);
            convertPrimitives.put(Byte.TYPE, Byte.class);
            convertPrimitives.put(Character.TYPE, Character.class);
            convertPrimitives.put(Double.TYPE, Double.class);
            convertPrimitives.put(Float.TYPE, Float.class);
            convertPrimitives.put(Integer.TYPE, Integer.class);
            convertPrimitives.put(Long.TYPE, Long.class);
            convertPrimitives.put(Short.TYPE, Short.class);
        }

        /** Converts a primitive type to its corresponding class.
         * <p>
         * If the argument type is primitive then we want to convert our
         * primitive type signature to the corresponding Object type so
         * introspection for methods with primitive types will work
         * correctly.
         * </p>
         */
        static final Class primitiveClass(Class parm) {
            // it is marginally faster to get from the map than call isPrimitive...
            //if (!parm.isPrimitive()) return parm;
            Class prim = convertPrimitives.get(parm);
            return prim == null ? parm : prim;
        }
        /**
         * Cache of Methods, or CACHE_MISS, keyed by method
         * name and actual arguments used to find it.
         */
        private final Map<MethodKey, Method> cache = new HashMap<MethodKey, Method>();
        /**
         * Map of methods that are searchable according to method parameters to find a match
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
        public Method get(final String name, final Object[] params)
                throws MethodMap.AmbiguousException {
            return get(new MethodKey(name, params));
        }

        public Method get(final MethodKey methodKey)
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

        public void put(Method method) {
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
    }

    /**
     * A method key for the introspector cache:
     * This replaces the original key scheme which used to build the key by concatenating the method name and parameters
     * class names as one string with the exception that primitive types were converted to their object class equivalents.
     * The key is still based on the same information, it is just wrapped in an object instead.
     * Primitive type classes are converted to they object equivalent to make a key; int foo(int) and int foo(Integer) do
     * generate the same key.
     * A key can be constructed either from arguments (array of objects) or from parameters (array of class).
     * Roughly 3x faster than string key to access the map & uses less memory.
     */
    static class MethodKey {
        /** The hash code */
        final int hash;
        /** The method name. */
        final String method;
        /** The parameters. */
        final Class[] params;
        /** A marker for empty parameter list. */
        static final Class[] NOARGS = new Class[0];

        /** Builds a MethodKey from a method.
         *  Used to store information in the method map.
         */
        MethodKey(Method method) {
            this(method.getName(), method.getParameterTypes());
        }

        /** Builds a MethodKey from a method name and a set of arguments (objects).
         *  Used to query the method map.
         */
        MethodKey(String method, Object[] args) {
            // !! keep this in sync with the other ctor (hash code) !!
            this.method = method;
            int hash = this.method.hashCode();
            final int size;
            if (args != null && (size = args.length) > 0) {
                this.params = new Class[size];
                for (int p = 0; p < size; ++p) {
                    // ctor(Object) : {
                    Object arg = args[p];
                    // no need to go thru primitive type conversion since these are objects
                    Class parm = arg == null ? Object.class : arg.getClass();
                    // }
                    hash = (37 * hash) + parm.hashCode();
                    this.params[p] = parm;
                }
            } else {
                this.params = NOARGS;
            }
            this.hash = hash;
        }

        /** Builds a MethodKey from a method name and a set of parameters (classes).
         *  Used to store information in the method map. ( @see MethodKey#primitiveClass )
         */
        MethodKey(String method, Class[] args) {
            // !! keep this in sync with the other ctor (hash code) !!
            this.method = method;
            int hash = this.method.hashCode();
            final int size;
            if (args != null && (size = args.length) > 0) {
                this.params = new Class[size];
                for (int p = 0; p < size; ++p) {
                    // ctor(Class): {
                    Class parm = MethodCache.primitiveClass(args[p]);
                    // }
                    hash = (37 * hash) + parm.hashCode();
                    this.params[p] = parm;
                }
            } else {
                this.params = NOARGS;
            }
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object arg) {
            MethodKey key = (MethodKey) arg;
            return method.equals(key.method) && java.util.Arrays.equals(params, key.params);
        }

        @Override
        /** Compatible with original string key. */
        public String toString() {
            StringBuilder builder = new StringBuilder(method);
            for (Class c : params) {
                builder.append(c.getName());
            }
            return builder.toString();
        }
    }
}
