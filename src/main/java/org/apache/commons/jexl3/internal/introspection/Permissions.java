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
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
     * A positive NoJexl construct that defines what is denied by absence in the set.
     * <p>Field or method that are named are the only one allowed access.</p>
     */
    static class JexlClass extends NoJexlClass {
        @Override boolean deny(final Constructor<?> method) { return !super.deny(method); }
        @Override boolean deny(final Field field) { return !super.deny(field); }
        @Override boolean deny(final Method method) { return !super.deny(method); }
    }

    /**
     * Equivalent of @NoJexl on a ctor, a method or a field in a class.
     * <p>Field or method that are named are denied access.</p>
     */
    static class NoJexlClass {
        // the NoJexl method names (including ctor, name of class)
        protected final Set<String> methodNames;
        // the NoJexl field names
        protected final Set<String> fieldNames;

        NoJexlClass() {
            this(new HashSet<>(), new HashSet<>());
        }

        NoJexlClass(final Set<String> methods, final Set<String> fields) {
            methodNames = methods;
            fieldNames = fields;
        }

        boolean deny(final Constructor<?> method) {
            return methodNames.contains(method.getDeclaringClass().getSimpleName());
        }

        boolean deny(final Field field) {
            return fieldNames.contains(field.getName());
        }

        boolean deny(final Method method) {
            return methodNames.contains(method.getName());
        }

        boolean isEmpty() { return methodNames.isEmpty() && fieldNames.isEmpty(); }
    }

    /**
     * Equivalent of @NoJexl on a class in a package.
     */
    static class NoJexlPackage {
        // the NoJexl class names
        protected final Map<String, NoJexlClass> nojexl;

        /**
         * Default ctor.
         */
        NoJexlPackage() {
            this(null);
        }

        /**
         * Ctor.
         * @param map the map of NoJexl classes
         */
        NoJexlPackage(final Map<String, NoJexlClass> map) {
            this.nojexl = new ConcurrentHashMap<>(map == null ? Collections.emptyMap() : map);
        }

        void addNoJexl(final String key, final NoJexlClass njc) {
            if (njc == null) {
                nojexl.remove(key);
            } else {
                nojexl.put(key, njc);
            }
        }

        NoJexlClass getNoJexl(final Class<?> clazz) {
            return nojexl.get(classKey(clazz));
        }

        boolean isEmpty() { return nojexl.isEmpty(); }
    }

    /** Marker for whole NoJexl class. */
    static final NoJexlClass NOJEXL_CLASS = new NoJexlClass(Collections.emptySet(), Collections.emptySet()) {
        @Override boolean deny(final Constructor<?> method) {
            return true;
        }

        @Override boolean deny(final Field field) {
            return true;
        }

        @Override boolean deny(final Method method) {
            return true;
        }
    };

    /** Marker for allowed class. */
    static final NoJexlClass JEXL_CLASS = new NoJexlClass(Collections.emptySet(), Collections.emptySet()) {
        @Override boolean deny(final Constructor<?> method) {
            return false;
        }

        @Override boolean deny(final Field field) {
            return false;
        }

        @Override  boolean deny(final Method method) {
            return false;
        }
    };

    /** Marker for @NoJexl package. */
    static final NoJexlPackage NOJEXL_PACKAGE = new NoJexlPackage(Collections.emptyMap()) {
        @Override NoJexlClass getNoJexl(final Class<?> clazz) {
            return NOJEXL_CLASS;
        }
    };

    /** Marker for fully allowed package. */
    static final NoJexlPackage JEXL_PACKAGE = new NoJexlPackage(Collections.emptyMap()) {
        @Override NoJexlClass getNoJexl(final Class<?> clazz) {
            return JEXL_CLASS;
        }
    };

    /**
     * The no-restriction introspection permission singleton.
     */
    static final Permissions UNRESTRICTED = new Permissions();

    /**
     * Creates a class key joining enclosing ascendants with '$'.
     * <p>As in {@code outer$inner} for <code>class outer { class inner...</code>.</p>
     * @param clazz the clazz
     * @return the clazz key
     */
    static String classKey(final Class<?> clazz) {
        return classKey(clazz, null);
    }

    /**
     * Creates a class key joining enclosing ascendants with '$'.
     * <p>As in {@code outer$inner} for <code>class outer { class inner...</code>.</p>
     * @param clazz the clazz
     * @param strb the buffer to compose the key
     * @return the clazz key
     */
    static String classKey(final Class<?> clazz, final StringBuilder strb) {
        StringBuilder keyb = strb;
        final Class<?> outer = clazz.getEnclosingClass();
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
        }
        return clazz.getSimpleName();
    }
    /**
     * Whether the wilcard set of packages allows a given package to be introspected.
     * @param allowed the allowed set (not null, may be empty)
     * @param name the package name (not null)
     * @return true if allowed, false otherwise
     */
    static boolean wildcardAllow(final Set<String> allowed, final String name) {
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
    protected Permissions(final Set<String> perimeter, final Map<String, NoJexlPackage> nojexl) {
        this.allowed = perimeter;
        this.packages = nojexl;
    }

    /**
     * Checks whether a class or one of its super-classes or implemented interfaces
     * explicitly disallows JEXL introspection.
     * @param clazz the class to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Class<?> clazz) {
        // clazz must be not null
        if (!validate(clazz)) {
            return false;
        }
        // proxy goes through
        if (Proxy.isProxyClass(clazz)) {
            return true;
        }
        // class must be allowed
        if (deny(clazz)) {
            return false;
        }
        // no super class can be denied and at least one must be allowed
        boolean explicit = wildcardAllow(clazz);
        Class<?> walk = clazz.getSuperclass();
        while (walk != null) {
            if (deny(walk)) {
                return false;
            }
            if (!explicit) {
                explicit = wildcardAllow(walk);
            }
            walk = walk.getSuperclass();
        }
        // check wildcards
        return explicit;
    }

    /**
     * Check whether a method is allowed to be introspected in one superclass or interface.
     * @param clazz the superclass or interface to check
     * @param method the method
     * @param explicit carries whether the package holding the method is explicitly allowed
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    private boolean allow(final Class<?> clazz, final Method method, final boolean[] explicit) {
        try {
            // check if method in that class is declared ie overrides
            final Method override = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
            // should not be possible...
            if (denyMethod(override)) {
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

    /**
     * Checks whether a constructor explicitly disallows JEXL introspection.
     * @param ctor the constructor to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Constructor<?> ctor) {
        // method must be not null, public
        if (!validate(ctor)) {
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
        // field must be public
        if (!validate(field)) {
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
        // method must be not null, public, not synthetic, not bridge
        if (!validate(method)) {
            return false;
        }
        // method must be allowed
        if (denyMethod(method)) {
            return false;
        }
        Class<?> clazz = method.getDeclaringClass();
        // gather if any implementation of the method is explicitly allowed by the packages
        final boolean[] explicit = { wildcardAllow(clazz) };
        // let's walk all interfaces
        for (final Class<?> inter : clazz.getInterfaces()) {
            if (!allow(inter, method, explicit)) {
                return false;
            }
        }
        // let's walk all super classes
        clazz = clazz.getSuperclass();
        while (clazz != null) {
            if (!allow(clazz, method, explicit)) {
                return false;
            }
            clazz = clazz.getSuperclass();
        }
        return explicit[0];
    }

    /**
     * Checks whether a package explicitly disallows JEXL introspection.
     * @param pack the package
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Package pack) {
       return validate(pack) && !deny(pack);
    }

    /**
     * Creates a new set of permissions by composing these permissions with a new set of rules.
     * @param src the rules
     * @return the new permissions
     */
    @Override
    public Permissions compose(final String... src) {
        return new PermissionsParser().parse(new LinkedHashSet<>(allowed),new ConcurrentHashMap<>(packages), src);
    }

    /**
     * Tests whether a whole class is denied Jexl visibility.
     * <p>Also checks package visibility.</p>
     * @param clazz the class
     * @return true if denied, false otherwise
     */
    private boolean deny(final Class<?> clazz) {
        // Don't deny arrays
        if (clazz.isArray()) {
            return false;
        }
        // is clazz annotated with nojexl ?
        final NoJexl nojexl = clazz.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        final NoJexlPackage njp = packages.get(ClassTool.getPackageName(clazz));
        return njp != null && Objects.equals(NOJEXL_CLASS, njp.getNoJexl(clazz));
    }

    /**
     * Tests whether a constructor is denied Jexl visibility.
     * @param ctor the constructor
     * @return true if denied, false otherwise
     */
    private boolean deny(final Constructor<?> ctor) {
        // is ctor annotated with nojexl ?
        final NoJexl nojexl = ctor.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        return getNoJexl(ctor.getDeclaringClass()).deny(ctor);
    }

    /**
     * Tests whether a field is denied Jexl visibility.
     * @param field the field
     * @return true if denied, false otherwise
     */
    private boolean deny(final Field field) {
        // is field annotated with nojexl ?
        final NoJexl nojexl = field.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        return getNoJexl(field.getDeclaringClass()).deny(field);
    }

    /**
     * Tests whether a method is denied Jexl visibility.
     * @param method the method
     * @return true if denied, false otherwise
     */
    private boolean deny(final Method method) {
        // is method annotated with nojexl ?
        final NoJexl nojexl = method.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        return getNoJexl(method.getDeclaringClass()).deny(method);
    }

    /**
     * Tests whether a whole package is denied Jexl visibility.
     * @param pack the package
     * @return true if denied, false otherwise
     */
    private boolean deny(final Package pack) {
        // is package annotated with nojexl ?
        final NoJexl nojexl = pack.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return true;
        }
        return Objects.equals(NOJEXL_PACKAGE, packages.get(pack.getName()));
    }

    /**
     * Tests whether a method is denied.
     * @param method the method
     * @return true if it has been disallowed through annotation or declaration
     */
    private boolean denyMethod(final Method method) {
        // check declared restrictions, class must not be denied
        return deny(method) || deny(method.getDeclaringClass());
    }

    /**
     * Gets the class constraints.
     * <p>If nothing was explicitly forbidden, everything is allowed.</p>
     * @param clazz the class
     * @return the class constraints instance, not-null.
     */
    private NoJexlClass getNoJexl(final Class<?> clazz) {
        final String pkgName = ClassTool.getPackageName(clazz);
        final NoJexlPackage njp = getNoJexlPackage(pkgName);
        if (njp != null) {
            final NoJexlClass njc = njp.getNoJexl(clazz);
            if (njc != null) {
                return njc;
            }
        }
        return JEXL_CLASS;
    }

    /**
     * Gets the package constraints.
     * @param packageName the package name
     * @return the package constraints instance, not-null.
     */
    private NoJexlPackage getNoJexlPackage(final String packageName) {
        return packages.getOrDefault(packageName, JEXL_PACKAGE);
    }

    /**
     * @return the packages
     */
    Map<String, NoJexlPackage> getPackages() {
        return packages == null ? Collections.emptyMap() : Collections.unmodifiableMap(packages);
    }

    /**
     * @return the wilcards
     */
    Set<String> getWildcards() {
        return allowed == null ? Collections.emptySet() : Collections.unmodifiableSet(allowed);
    }

    /**
     * Whether the wildcard set of packages allows a given class to be introspected.
     * @param clazz the package name (not null)
     * @return true if allowed, false otherwise
     */
    private boolean wildcardAllow(final Class<?> clazz) {
        return wildcardAllow(allowed, ClassTool.getPackageName(clazz));
    }
}
