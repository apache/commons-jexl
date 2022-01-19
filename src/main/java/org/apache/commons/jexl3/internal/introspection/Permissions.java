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
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jexl3.annotations.NoJexl;

/**
 * Checks whether an element (ctor, field or method) is visible by JEXL introspection.
 * Default implementation does this by checking if element has been annotated with NoJexl.
 */
public class Permissions {
    /** Allow inheritance. */
    protected Permissions() {
        packages = Collections.emptyMap();
    }
    /**
     * The default singleton.
     */
    public static final Permissions DEFAULT = new Permissions();

    /**
     * Equivalent of @NoJexl on a class in a package.
     */
    static class NoJexlPackage {
        // the NoJexl class names
        protected Map<String, NoJexlClass> nojexl;

        /**
         * Ctor.
         * @param map the map of NoJexl classes
         */
        NoJexlPackage(Map<String, NoJexlClass> map) {
            this.nojexl = map;
        }

        /**
         * Default ctor.
         */
        NoJexlPackage() {
            this(new ConcurrentHashMap<>());
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        NoJexlClass getNoJexl(Class<?> clazz) {
            return nojexl.get(clazz.getName());
        }
    }

    /**
     * Equivalent of @NoJexl on a ctor, a method or a field in a class.
     */
    static class NoJexlClass {
        // the NoJexl method names (including ctor, name of class)
        protected Set<String> methodNames;
        // the NoJexl field names
        protected Set<String> fieldNames;

        NoJexlClass(Set<String> methods, Set<String> fields) {
            methodNames = methods;
            fieldNames = fields;
        }

        NoJexlClass() {
            this(new HashSet<>(), new HashSet<>());
        }

        boolean deny(Field field) {
            return fieldNames.contains(field.getName());
        }

        boolean deny(Method method) {
            return methodNames.contains(method.getName());
        }

        boolean deny(Constructor method) {
            return methodNames.contains(method.getName());
        }
    }

    /** Marker for whole NoJexl class. */
    static final NoJexlClass NOJEXL_CLASS = new NoJexlClass(Collections.emptySet(), Collections.emptySet()) {
        @Override
        boolean deny(Field field) {
            return true;
        }
        @Override
        boolean deny(Method method) {
            return true;
        }
        @Override
        boolean deny(Constructor method) {
            return true;
        }
    };

    /** Marker for allowed class. */
    static final NoJexlClass JEXL_CLASS = new NoJexlClass(Collections.emptySet(), Collections.emptySet()) {
        @Override
        boolean deny(Field field) {
            return false;
        }
        @Override
        boolean deny(Method method) {
            return false;
        }
        @Override
        boolean deny(Constructor method) {
            return false;
        }
    };

    /** Marker for @NoJexl package. */
    static final NoJexlPackage NOJEXL_PACKAGE = new NoJexlPackage(Collections.emptyMap()) {
        NoJexlClass getNoJexl(Class<?> clazz) {
            return NOJEXL_CLASS;
        }
    };

    /** Marker for fully allowed package. */
    static final NoJexlPackage JEXL_PACKAGE = new NoJexlPackage(Collections.emptyMap()) {
        NoJexlClass getNoJexl(Class<?> clazz) {
            return JEXL_CLASS;
        }
    };

    /**
     * The @NoJexl execution-time map.
     */
    private final Map<String, NoJexlPackage> packages;

    /**
     * Gets the package constraints.
     * @param pack the package
     * @return the package constraints instance, not-null.
     */
    private NoJexlPackage getNoJexl(Package pack) {
        NoJexlPackage njp = packages.get(pack.getName());
        return njp == null? JEXL_PACKAGE : njp;
    }

    /**
     * Gets the class constraints.
     * @param clazz the class
     * @return the class constraints instance, not-null.
     */
    private NoJexlClass getNoJexl(Class<?> clazz) {
        NoJexlPackage njp = getNoJexl(clazz.getPackage());
        return njp == null? JEXL_CLASS : njp.getNoJexl(clazz);
    }

    /**
     * Whether a whole package is denied Jexl visibility.
     * @param pack the package
     * @return true if denied, false otherwise
     */
    private boolean deny(Package pack) {
        return Objects.equals(NOJEXL_PACKAGE, packages.get(pack.getName()));
    }

    /**
     * Whether a whole class is denied Jexl visibility.
     * @param clazz the class
     * @return true if denied, false otherwise
     */
    private boolean deny(Class<?> clazz) {
        NoJexlPackage njp = packages.get(clazz.getPackage().getName());
        return njp != null && NOJEXL_CLASS.equals(njp.getNoJexl(clazz));
    }

    /**
     * Whether a constructor is denied Jexl visibility.
     * @param ctor the constructor
     * @return true if denied, false otherwise
     */
    private boolean deny(Constructor<?> ctor) {
        return getNoJexl(ctor.getDeclaringClass()).deny(ctor);
    }

    /**
     * Whether a field is denied Jexl visibility.
     * @param field the field
     * @return true if denied, false otherwise
     */
    private boolean deny(Field field) {
        return getNoJexl(field.getDeclaringClass()).deny(field);
    }

    /**
     * Whether a method is denied Jexl visibility.
     * @param method the method
     * @return true if denied, false otherwise
     */
    private boolean deny(Method method) {
        return getNoJexl(method.getDeclaringClass()).deny(method);
    }

    /**
     * Checks whether a package explicitly disallows JEXL introspection.
     * @param pack the package
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(final Package pack) {
        if (pack == null || pack.getAnnotation(NoJexl.class) != null || deny(pack)) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether a class or one of its super-classes or implemented interfaces
     * explicitly disallows JEXL introspection.
     * @param clazz the class to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(final Class<?> clazz) {
        return clazz != null && allow(clazz.getPackage()) && allow(clazz, true);
    }

    /**
     * Checks whether a constructor explicitly disallows JEXL introspection.
     * @param ctor the constructor to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(final Constructor<?> ctor) {
        if (ctor == null) {
            return false;
        }
        if (!Modifier.isPublic(ctor.getModifiers())) {
            return false;
        }
        final Class<?> clazz = ctor.getDeclaringClass();
        if (!allow(clazz, false)) {
            return false;
        }
        // is ctor annotated with nojexl ?
        final NoJexl nojexl = ctor.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return false;
        }
        // check added restrictions
        if (deny(ctor)) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether a field explicitly disallows JEXL introspection.
     * @param field the field to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(final Field field) {
        if (field == null) {
            return false;
        }
        if (!Modifier.isPublic(field.getModifiers())) {
            return false;
        }
        final Class<?> clazz = field.getDeclaringClass();
        if (!allow(clazz, false)) {
            return false;
        }
        // is field annotated with nojexl ?
        final NoJexl nojexl = field.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return false;
        }
        // check added restrictions
        if (deny(field)) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether a method explicitly disallows JEXL introspection.
     * <p>Since methods can be overridden, this also checks that no superclass or interface
     * explicitly disallows this methods.</p>
     * @param method the method to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(final Method method) {
        if (method == null) {
            return false;
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        // is method annotated with nojexl ?
        NoJexl nojexl = method.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return false;
        }
        // is the class annotated with nojexl ?
        Class<?> clazz = method.getDeclaringClass();
        nojexl = clazz.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return false;
        }
        // check added restrictions
        if (deny(method)) {
            return false;
        }
        // lets walk all interfaces
        for (final Class<?> inter : clazz.getInterfaces()) {
            if (!allow(inter, method)) {
                return false;
            }
        }
        // lets walk all super classes
        clazz = clazz.getSuperclass();
        // walk all superclasses
        while (clazz != null) {
            if (!allow(clazz, method)) {
                return false;
            }
            clazz = clazz.getSuperclass();
        }
        return true;
    }

    /**
     * Checks whether a class or one of its superclasses or implemented interfaces
     * explicitly disallows JEXL introspection.
     * @param clazz the class to check
     * @param interf whether interfaces should be checked as well
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    protected boolean allow(final Class<?> clazz, final boolean interf) {
        if (clazz == null) {
            return false;
        }
        if (!Modifier.isPublic(clazz.getModifiers())) {
            return false;
        }
        // lets walk all interfaces
        if (interf) {
            for (final Class<?> inter : clazz.getInterfaces()) {
                // is clazz annotated with nojexl ?
                final NoJexl nojexl = inter.getAnnotation(NoJexl.class);
                if (nojexl != null) {
                    return false;
                }
                // check added restrictions
                if (deny(inter)) {
                    return false;
                }
            }
        }
        // lets walk all super classes
        Class<?> walk = clazz.getSuperclass();
        // walk all superclasses
        while (walk != null) {
            // is clazz annotated with nojexl ?
            final NoJexl nojexl = walk.getAnnotation(NoJexl.class);
            if (nojexl != null) {
                return false;
            }
            // check added restrictions
            if (deny(walk)) {
                return false;
            }
            walk = walk.getSuperclass();
        }
        return true;
    }

    /**
     * Check whether a method is allowed to be JEXL introspected in all its
     * superclasses and interfaces.
     * @param clazz the class
     * @param method the method
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    protected boolean allow(final Class<?> clazz, final Method method) {
        if (clazz != null) {
            try {
                // check if method in that class is different from the method argument
                final Method wmethod = clazz.getMethod(method.getName(), method.getParameterTypes());
                if (wmethod != null) {
                    NoJexl nojexl = clazz.getAnnotation(NoJexl.class);
                    if (nojexl != null) {
                        return false;
                    }
                    // check if parent declaring class said nojexl on that method (transitivity)
                    nojexl = wmethod.getAnnotation(NoJexl.class);
                    if (nojexl != null) {
                        return false;
                    }
                    // check added restrictions
                    if (deny(wmethod)) {
                        return false;
                    }
                }
            } catch (final NoSuchMethodException ex) {
                // unexpected, return no
                return true;
            } catch (final SecurityException ex) {
                // unexpected, can't do much
                return false;
            }
        }
        return true;
    }
}
