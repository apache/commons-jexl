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
import org.apache.commons.jexl3.introspection.JexlPermissions;

/**
 * Checks whether an element (ctor, field or method) is visible by JEXL introspection.
 * <p>Default implementation does this by checking if element has been annotated with NoJexl.</p>
 *
 * <p>The NoJexl annotation allows a fine grain permissions on executable objects (methods, fields, constructors).
 * </p>
 * <ul>
 * <li>NoJexl of a package implies all classes (including derived classes) and all interfaces
 * of that package are invisible to JEXL.</li>
 * <li>NoJexl on a class implies this class and all its derived classes are invisible to JEXL.</li>
 * <li>NoJexl on a (public) field makes it not visible as a property to JEXL.</li>
 * <li>NoJexl on a constructor prevents that constructor to be used to instantiate through 'new'.</li>
 * <li>NoJexl on a method prevents that method and any of its overrides to be visible to JEXL.</li>
 * <li>NoJexl on an interface prevents all methods of that interface and their overrides to be visible to JEXL.</li>
 * </ul>
 * <p> It is possible to further refine permissions on classes used through libraries where source code form can
 * not be altered using an instance of permissions using {@link JexlPermissions#parse(String...)}.</p>
 */
public class Permissions implements JexlPermissions {

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

        boolean isEmpty() { return nojexl.isEmpty(); }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        NoJexlClass getNoJexl(Class<?> clazz) {
            return nojexl.get(classKey(clazz));
        }

        void addNoJexl(String key, NoJexlClass njc) {
            nojexl.put(key, njc);
        }
    }

    /**
     * Creates a class key joining enclosing ascendants with '$'.
     * <p>As in <code>outer$inner</code> for <code>class outer { class inner...</code>.</p>
     * @param clazz the clazz
     * @return the clazz key
     */
    private static String classKey(final Class<?> clazz) {
        return classKey(clazz, null);
    }

    /**
     * Creates a class key joining enclosing ascendants with '$'.
     * <p>As in <code>outer$inner</code> for <code>class outer { class inner...</code>.</p>
     * @param clazz the clazz
     * @param strb the buffer to compose the key
     * @return the clazz key
     */
    private static String classKey(final Class<?> clazz, final StringBuilder strb) {
        StringBuilder keyb = strb;
        Class<?> outer = clazz.getEnclosingClass();
        if (outer != null) {
            if (keyb == null) {
                keyb = new StringBuilder();
            }
            classKey(outer, keyb);
            keyb.append('$');
        }
        if (keyb != null) {
            keyb.append(clazz.getSimpleName());
            return keyb.toString();
        } else {
            return clazz.getSimpleName();
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

        boolean isEmpty() { return methodNames.isEmpty() && fieldNames.isEmpty(); }

        NoJexlClass() {
            this(new HashSet<>(), new HashSet<>());
        }

        boolean deny(Field field) {
            return fieldNames.contains(field.getName());
        }

        boolean deny(Method method) {
            return methodNames.contains(method.getName());
        }

        boolean deny(Constructor<?> method) {
            return methodNames.contains(method.getDeclaringClass().getSimpleName());
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
        boolean deny(Constructor<?> method) {
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
        boolean deny(Constructor<?> method) {
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
     * The closed world package patterns.
     */
    private final Set<String> allowed;

    /** Allow inheritance. */
    protected Permissions() {
        this(Collections.emptySet(), Collections.emptyMap());
    }

    /**
     * Default ctor.
     * @param perimeter the allowed wildcard set of packages
     * @param nojexl the NoJexl external map
     */
    protected Permissions(Set<String> perimeter, Map<String, NoJexlPackage> nojexl) {
        this.allowed = perimeter;
        this.packages = nojexl;
    }

    /**
     * The default singleton.
     */
    public static final Permissions DEFAULT = new Permissions();

    /**
     * @return the packages
     */
    Map<String, NoJexlPackage> getPackages() {
        return packages == null? Collections.emptyMap() : Collections.unmodifiableMap(packages);
    }

    /**
     * @return the wilcards
     */
    Set<String> getWildcards() {
        return allowed == null? Collections.emptySet() : Collections.unmodifiableSet(allowed);
    }

    /**
     * Gets the package constraints.
     * @param pack the package
     * @return the package constraints instance, not-null.
     */
    private NoJexlPackage getNoJexl(Package pack) {
        NoJexlPackage njp = packages.get(pack.getName());
        if (njp != null) {
            return njp;
        }
        return JEXL_PACKAGE;
    }

    /**
     * Gets the class constraints.
     * <p>If nothing was explicitly forbidden, everything is allowed.</p>
     * @param clazz the class
     * @return the class constraints instance, not-null.
     */
    private NoJexlClass getNoJexl(Class<?> clazz) {
        NoJexlPackage njp = getNoJexl(clazz.getPackage());
        if (njp != null) {
            NoJexlClass njc = njp.getNoJexl(clazz);
            if (njc != null) {
                return njc;
            }
        }
        return JEXL_CLASS;
    }

    /**
     * Whether the wilcard set of packages allows a given class to be introspected.
     * @param clazz the package name (not null)
     * @return true if allowed, false otherwise
     */
    private boolean wildcardAllow(Class<?> clazz) {
        return wildcardAllow(allowed, clazz.getPackage().getName());
    }

    /**
     * Whether the wilcard set of packages allows a given package to be introspected.
     * @param allowed the allowed set (not null, may be empty)
     * @param name the package name (not null)
     * @return true if allowed, false otherwise
     */
    static boolean wildcardAllow(Set<String> allowed, String name) {
        // allowed packages are explicit in this case
        boolean found = allowed == null || allowed.isEmpty() || allowed.contains(name);
        if (!found) {
            String wildcard = name;
            for (int i = name.length(); !found && i > 0; i = wildcard.lastIndexOf('.')) {
                wildcard = wildcard.substring(0, i);
                found = allowed.contains(wildcard + ".*");
            }
        }
        return found;
    }

    /**
     * Whether a whole package is denied Jexl visibility.
     * @param pack the package
     * @return true if denied, false otherwise
     */
    private boolean deny(Package pack) {
        // is package annotated with nojexl ?
        final NoJexl nojexl = pack.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        return Objects.equals(NOJEXL_PACKAGE, packages.get(pack.getName()));
    }

    /**
     * Whether a whole class is denied Jexl visibility.
     * <p>Also checks package visibility.</p>
     * @param clazz the class
     * @return true if denied, false otherwise
     */
    private boolean deny(Class<?> clazz) {
        // dont deny arrays
        if (clazz.isArray()) {
            return false;
        }
        // is clazz annotated with nojexl ?
        final NoJexl nojexl = clazz.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        Package pkg = clazz.getPackage();
        NoJexlPackage njp = packages.get(pkg.getName());
        return njp != null && Objects.equals(NOJEXL_CLASS, njp.getNoJexl(clazz));
    }

    /**
     * Whether a constructor is denied Jexl visibility.
     * @param ctor the constructor
     * @return true if denied, false otherwise
     */
    private boolean deny(Constructor<?> ctor) {
        // only public
        if (!Modifier.isPublic(ctor.getModifiers())) {
            return true;
        }
        // is ctor annotated with nojexl ?
        final NoJexl nojexl = ctor.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        return getNoJexl(ctor.getDeclaringClass()).deny(ctor);
    }

    /**
     * Whether a field is denied Jexl visibility.
     * @param field the field
     * @return true if denied, false otherwise
     */
    private boolean deny(Field field) {
        // only public
        if (!Modifier.isPublic(field.getModifiers())) {
            return true;
        }
        // is field annotated with nojexl ?
        final NoJexl nojexl = field.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        return getNoJexl(field.getDeclaringClass()).deny(field);
    }

    /**
     * Whether a method is denied Jexl visibility.
     * @param method the method
     * @return true if denied, false otherwise
     */
    private boolean deny(Method method) {
        // only public
        if (!Modifier.isPublic(method.getModifiers())) {
            return true;
        }
        // is method annotated with nojexl ?
        final NoJexl nojexl = method.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        return getNoJexl(method.getDeclaringClass()).deny(method);
    }

    /**
     * Checks whether a package explicitly disallows JEXL introspection.
     * @param pack the package
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Package pack) {
       return pack != null && !deny(pack);
    }

    /**
     * Checks whether a class or one of its super-classes or implemented interfaces
     * explicitly disallows JEXL introspection.
     * @param clazz the class to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        // class must be allowed
        if (deny(clazz)) {
            return false;
        }
        // all super classes must be allowed
        Class<?> walk = clazz.getSuperclass();
        while (walk != null) {
            if (deny(walk)) {
                return false;
            }
            walk = walk.getSuperclass();
        }
        return true;
    }

    /**
     * Checks whether a constructor explicitly disallows JEXL introspection.
     * @param ctor the constructor to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Constructor<?> ctor) {
        if (ctor == null) {
            return false;
        }
        // check declared restrictions
        if (deny(ctor)) {
            return false;
        }
        // class must agree
        final Class<?> clazz = ctor.getDeclaringClass();
        if (deny(clazz)) {
            return false;
        }
        // check wildcards
        return wildcardAllow(clazz);
    }

    /**
     * Checks whether a field explicitly disallows JEXL introspection.
     * @param field the field to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Field field) {
        if (field == null) {
            return false;
        }
        // check declared restrictions
        if (deny(field)) {
            return false;
        }
        // class must agree
        final Class<?> clazz = field.getDeclaringClass();
        if (deny(clazz)) {
            return false;
        }
        // check wildcards
        return wildcardAllow(clazz);
    }

    /**
     * Checks whether a method explicitly disallows JEXL introspection.
     * <p>Since methods can be overridden, this also checks that no superclass or interface
     * explicitly disallows this methods.</p>
     * @param method the method to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Method method) {
        if (method == null) {
            return false;
        }
        // method must be allowed
        if (!allowMethod(method)) {
            return false;
        }
        Class<?> clazz = method.getDeclaringClass();
        // gather if any implementation of the method is explicitly allowed by the packages
        boolean[] explicit = new boolean[]{wildcardAllow(clazz)};
        // lets walk all interfaces
        for (final Class<?> inter : clazz.getInterfaces()) {
            if (!allow(inter, method, explicit)) {
                return false;
            }
        }
        // lets walk all super classes
        clazz = clazz.getSuperclass();
        // walk all superclasses
        while (clazz != null) {
            if (!allow(clazz, method, explicit)) {
                return false;
            }
            clazz = clazz.getSuperclass();
        }
        return explicit[0];
    }

    /**
     * Checks whether a method is allowed.
     * @param method the method
     * @return true if it has not been disallowed through annotation or declaration
     */
    private boolean allowMethod(final Method method) {
        // check declared restrictions
        if (deny(method)) {
            return false;
        }
        Class<?> clazz = method.getDeclaringClass();
        // class must be allowed
        if (deny(clazz)) {
            return false;
        }
        return true;
    }

    /**
     * Check whether a method is allowed to be introspected in one superclass or interface.
     * @param clazz the superclass or interface to check
     * @param method the method
     * @param explicit carries whether the package holding the method is explicitly allowed
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    private boolean allow(final Class<?> clazz, final Method method, boolean[] explicit) {
        try {
            // check if method in that class is declared ie overrides
            final Method override = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
            // should not be possible...
            if (!allowMethod(override)) {
                return false;
            }
            // explicit |= ...
            if (!explicit[0]) {
                explicit[0] = wildcardAllow(clazz);
            }
            return true;
        } catch (final NoSuchMethodException ex) {
            // will happen if not overriding method in clazz
            return true;
        } catch (final SecurityException ex) {
            // unexpected, can't do much
            return false;
        }
    }
}
