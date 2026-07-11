/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import org.apache.commons.jexl3.annotations.NoJexl;
import org.apache.commons.jexl3.introspection.JexlPermissions;

/**
 * Checks whether an element (ctor, field, or method) is visible by JEXL introspection.
 * <p>The default implementation does this by checking if an element has been annotated with NoJexl.</p>
 *
 * <p>The NoJexl annotation allows a fine grain permission on executable objects (methods, fields, constructors).
 * </p>
 * <ul>
 * <li>NoJexl of a package implies all classes (including derived classes), and all interfaces
 * of that package are invisible to JEXL.</li>
 * <li>NoJexl on a class implies this class, and all its derived classes are invisible to JEXL.</li>
 * <li>NoJexl on a (public) field makes it not visible as a property to JEXL.</li>
 * <li>NoJexl on a constructor prevents that constructor to be used to instantiate through 'new'.</li>
 * <li>NoJexl on a method prevents that method and any of its overrides to be visible to JEXL.</li>
 * <li>NoJexl on an interface prevents all methods of that interface and their overrides to be visible to JEXL.</li>
 * </ul>
 * <p>It is possible to define permissions on external library classes used for which the source code
 * cannot be altered using an instance of permissions using {@link JexlPermissions#parse(String...)}.</p>
 */
public class Permissions implements JexlPermissions {
    /**
     * Represents the ability to create a copy of an object.
     * Any class implementing this interface must provide a concrete
     * implementation for the {@code copy()} method, which returns
     * a new instance of the object that is a logical copy of the original.
     *
     * @param <T> The type of object that can be copied
     */
    interface Copyable<T> {
        T copy() ;
    }

    /**
     * Creates a copy of a map containing copyable values.
     * @param map the map to copy
     * @return The copy of the map
     * @param <T> The type of Copyable values
     */
    static <T extends Copyable<T>> Map<String, T> copyMap(final Map<String, T> map) {
        final Map<String, T> copy = new HashMap<>(map.size());
        for (final Map.Entry<String, T> entry : map.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    /**
     * Equivalent of @NoJexl on a ctor, a method, or a field in a class.
     * <p>Field or method that are named are denied access.</p>
     */
    static class NoJexlClass implements Copyable<NoJexlClass> {
        // the NoJexl method names (including ctor, name of class)
        final Set<String> methodNames;
        // the NoJexl field names
        final Set<String> fieldNames;

        NoJexlClass() {
            this(new HashSet<>(), new HashSet<>());
        }

        NoJexlClass(final Set<String> methods, final Set<String> fields) {
            methodNames = methods;
            fieldNames = fields;
        }

        @Override public NoJexlClass copy() {
            return new NoJexlClass(new HashSet<>(methodNames), new HashSet<>(fieldNames));
        }

        boolean deny(final Constructor<?> method) {
            return methodNames.contains(method.getDeclaringClass().getSimpleName());
        }

        boolean deny(final Field field) {
            return isEmpty() || fieldNames.contains(field.getName());
        }

        boolean deny(final Method method) {
            return isEmpty() || methodNames.contains(method.getName());
        }

        boolean isEmpty() { return methodNames.isEmpty() && fieldNames.isEmpty(); }

        /**
         * Whether this is a positive (allow-oriented) class declaration.
         * <p>A positive class is explicitly allowed in its package (it contributes to
         * {@link NoJexlPackage#hasAllowedClass()} and an empty one means &quot;allow the whole class&quot;).
         * A plain deny-list {@code NoJexlClass} is not positive.</p>
         *
         * @return true if the class is positively allowed, false if it is a deny-list
         */
        boolean isPositive() { return false; }
    }

    /**
     * A positive NoJexl construct that defines what is denied by absence in the set.
     * <p>Field or method that are named are the only one allowed access.</p>
     */
    static class JexlClass extends NoJexlClass {
        JexlClass(final Set<String> methods, final Set<String> fields) {
            super(methods, fields);
        }
        JexlClass() {
        }
        @Override public JexlClass copy() {
            return new JexlClass(new HashSet<>(methodNames), new HashSet<>(fieldNames));
        }
        @Override boolean deny(final Constructor<?> method) { return !super.deny(method); }
        @Override boolean deny(final Field field) { return !super.deny(field); }
        @Override boolean deny(final Method method) { return !super.deny(method); }
        @Override boolean isPositive() { return true; }
    }

    /**
     * A class that is positively allowed in its package yet carries deny-list member semantics.
     * <p>Used for {@code +ClassName{ -member(); }} inside a deny package: the class is added to the
     * allowed set (so unlisted classes in the package stay denied) but the named members are denied
     * while all others remain allowed. It is a {@link NoJexlClass} (deny-list) tagged as positive;
     * the deny-list behavior is inherited unchanged.</p>
     */
    static class AllowedNoJexlClass extends NoJexlClass {
        AllowedNoJexlClass() {
        }
        AllowedNoJexlClass(final Set<String> methods, final Set<String> fields) {
            super(methods, fields);
        }
        @Override public AllowedNoJexlClass copy() {
            return new AllowedNoJexlClass(new HashSet<>(methodNames), new HashSet<>(fieldNames));
        }
        @Override boolean isPositive() { return true; }
    }

    /**
     * Equivalent of @NoJexl on a class in a package.
     */
    protected static class NoJexlPackage implements Copyable<NoJexlPackage> {
        // the NoJexl class names
        final Map<String, NoJexlClass> nojexl;

        /**
         * Default ctor.
         */
        NoJexlPackage() {
            this(null);
        }

        /**
         * Ctor.
         *
         * @param map the map of NoJexl classes
         */
        NoJexlPackage(final Map<String, NoJexlClass> map) {
            this.nojexl = map == null || map.isEmpty() ? new HashMap<>() : map;
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

        /**
         * Whether this package has at least one explicitly-allowed class (a positive entry).
         * <p>A package with allowed-class entries acts as an allow-list: unlisted classes are denied.
         * A package with only denied-class entries acts as a deny-list: unlisted classes are allowed.</p>
         *
         * @return true if at least one class is explicitly allowed
         */
        boolean hasAllowedClass() {
            for (final NoJexlClass njc : nojexl.values()) {
                if (njc.isPositive()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Whether this is a positive (allow-oriented) package declaration.
         * <p>A positive package allows its own classes by default; a deny-list package does not.</p>
         *
         * @return true if the package is positively allowed, false otherwise
         */
        boolean isPositive() { return false; }

        @Override public NoJexlPackage copy() {
            return new NoJexlPackage(copyMap(nojexl));
        }
    }

    /**
     * A package where classes are allowed by default.
     */
    static class JexlPackage extends NoJexlPackage {
        JexlPackage(final Map<String, NoJexlClass> map) {
            super(map);
        }

        @Override
        NoJexlClass getNoJexl(final Class<?> clazz) {
            final NoJexlClass njc = nojexl.get(classKey(clazz));
            return njc != null ? njc : JEXL_CLASS;
        }

        @Override boolean isPositive() { return true; }

        @Override public JexlPackage copy() {
            return new JexlPackage(copyMap(nojexl));
        }
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
    static final NoJexlClass JEXL_CLASS = new JexlClass(Collections.emptySet(), Collections.emptySet()) {
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
        @Override boolean isPositive() {
            return true;
        }
        // a constant singleton survives copy as itself, preserving its positive nature through compose()
        @Override public NoJexlPackage copy() {
            return this;
        }
    };

    /**
     * The no-restriction introspection permission singleton.
     */
    static final Permissions UNRESTRICTED = new Permissions();

    /**
     * The @NoJexl execution-time map.
     */
    private final Map<String, NoJexlPackage> packages;
    /**
     * The allowed package patterns (wildcards or exact package names).
     * <p>Empty together with an empty {@link #packages} map means open-world: every package is accessible
     * and only explicitly denied elements are carved out — the behavior of {@link #UNRESTRICTED}.
     * Empty with a non-empty {@link #packages} map, or non-empty, means closed-world: only declared
     * packages are accessible.</p>
     */
    private final Set<String> allowed;

    /** Allow inheritance. */
    protected Permissions() {
        this.allowed = Collections.emptySet();
        this.packages = Collections.emptyMap();
    }

    /**
     * Default ctor.
     *
     * @param perimeter the allowed wildcard set of packages
     * @param nojexl the NoJexl external map
     */
    protected Permissions(final Set<String> perimeter, final Map<String, NoJexlPackage> nojexl) {
        this.allowed = perimeter;
        this.packages = nojexl;
    }

    /**
     * Creates a new set of permissions by composing these permissions with a new set of rules.
     *
     * @param src the rules
     * @return The new permissions
     */
    @Override
    public Permissions compose(final String... src) {
        return new PermissionsParser().parse(new HashSet<>(allowed), copyMap(packages), src);
    }

    /**
     * Creates a class key joining enclosing ascendants with '$'.
     * <p>As in {@code outer$inner} for <code>class outer { class inner...</code>.</p>
     *
     * @param clazz the clazz
     * @return The clazz key
     */
    static String classKey(final Class<?> clazz) {
        return classKey(clazz, null);
    }

    /**
     * Creates a class key joining enclosing ascendants with '$'.
     * <p>As in {@code outer$inner} for <code>class outer { class inner...</code>.</p>
     *
     * @param clazz the clazz
     * @param strb the buffer to compose the key
     * @return The clazz key
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
     * Whether the wildcard set of packages allows a given package to be introspected.
     *
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
     * Gets the package constraints.
     *
     * @param packageName the package name
     * @return The package constraints instance, not-null.
     */
    private NoJexlPackage getNoJexlPackage(final String packageName) {
        return packages.getOrDefault(packageName, JEXL_PACKAGE);
    }

    /**
     * @return The packages
     */
    Map<String, NoJexlPackage> getPackages() {
        return packages == null ? Collections.emptyMap() : Collections.unmodifiableMap(packages);
    }

    /**
     * @return The wildcards
     */
    Set<String> getWildcards() {
        return allowed == null ? Collections.emptySet() : Collections.unmodifiableSet(allowed);
    }

    /**
     * Whether a package belongs to the allowed perimeter.
     * <p>Open-world ({@link #UNRESTRICTED}: no rules at all) allows every package. Closed-world requires the
     * package to match an entry in {@link #allowed}; an empty perimeter in closed-world matches nothing.</p>
     *
     * @param packageName the package name (not null)
     * @return true if allowed, false otherwise
     */
    private boolean allowedPackage(final String packageName) {
        if ((allowed.isEmpty() && packages.isEmpty()) || (!allowed.isEmpty() && wildcardAllow(allowed, packageName))) {
            return true;
        }
        // a package explicitly declared positive allows itself (exact match only, no sub-package inference)
        final NoJexlPackage njp = packages.get(packageName);
        return njp != null && njp.isPositive();
    }

    /**
     * Determines whether a specified permission check is allowed for a given class.
     * The check involves verifying if a class or its corresponding package explicitly permits
     * a name (e.g., method) based on a given condition.
     *
     * @param <T> The type of the name to check (e.g., method, constructor)
     * @param clazz the class to evaluate (not null)
     * @param name the name to verify (not null)
     * @param check the condition to test whether the specified name is allowed (not null)
     * @return true if the specified name is allowed based on the condition, false otherwise
     */
    private <T> boolean specifiedAllow(final Class<?> clazz, final T name, final BiPredicate<NoJexlClass, T> check) {
        final String packageName = ClassTool.getPackageName(clazz);
        if (allowedPackage(packageName)) {
            return true;
        }
        final NoJexlPackage njp = packages.get(packageName);
        if (njp != null && check != null) {
            // there is a package permission, check if there is a class permission
            final NoJexlClass njc = njp.getNoJexl(clazz);
            if (njc != null) {
                return check.test(njc, name);
            }
            // class not listed: allowed if the package is a deny-list (no explicit class allows);
            // denied if the package is an allow-list (e.g. java.io -{ +PrintWriter{} ... })
            return !njp.hasAllowedClass();
        }
        // package not declared at all
        return false;
    }

    /**
     * Checks whether a class or one of its super-classes or implemented interfaces
     * explicitly allows JEXL introspection.
     *
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
        // class must not be denied, nor extend a denied class
        if (deny(clazz)) {
            return false;
        }
        // a subclass of a denied class is also denied; Object (the universal root) is skipped, and the
        // null guard keeps the walk safe when clazz is an interface or Object itself (getSuperclass() null).
        for (Class<?> walk = clazz.getSuperclass(); walk != null && walk != Object.class; walk = walk.getSuperclass()) {
            if (deny(walk)) {
                return false;
            }
        }
        // a class is allowed only by its own package or class-level declaration: there is deliberately no
        // reach-through via an allowed super-type, which would otherwise expose the whole foreign class.
        return allowedClass(clazz);
    }

    /**
     * Whether a class is allowed by its own package or class-level declaration.
     *
     * @param clazz the class to check (not null)
     * @return true if explicitly allowed
     */
    private boolean allowedClass(final Class<?> clazz) {
        return specifiedAllow(clazz, clazz, (njc, c) -> njc.isPositive() || !njc.isEmpty());
    }

    /**
     * Check whether a method is allowed to be introspected in one superclass or interface.
     *
     * @param clazz the superclass or interface to check
     * @param method the method
     * @param explicit carries whether the package holding the method is explicitly allowed
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    private boolean allow(final Class<?> clazz, final Method method, final boolean[] explicit) {
        try {
            // check if the method in that class is declared thus overrides
            final Method override = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
            if (override != method) {
                // should not be possible...
                if (denyMethod(override)) {
                    return false;
                }
                // explicit |= ...
                if (!explicit[0]) {
                    explicit[0] = specifiedAllow(clazz, override, (njc, m) -> !njc.deny(m));
                }
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
     * Checks whether a constructor explicitly allows JEXL introspection.
     *
     * @param ctor the constructor to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Constructor<?> ctor) {
        // method must be not null, public
        // check declared restrictions
        if (!validate(ctor) || deny(ctor)) {
            return false;
        }
        // class must agree
        final Class<?> clazz = ctor.getDeclaringClass();
        if (deny(clazz)) {
            return false;
        }
        // check wildcards
        return specifiedAllow(clazz, clazz, (njc, c) -> !njc.deny(ctor));
    }


    /**
     * Checks whether a field explicitly allows JEXL introspection.
     *
     * @param field the field to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Field field) {
        // field must be public
        // check declared restrictions
        if (!validate(field) || deny(field)) {
            return false;
        }
        // class must agree
        final Class<?> clazz = field.getDeclaringClass();
        if (deny(clazz)) {
            return false;
        }
        // check wildcards
        return specifiedAllow(clazz, field, (njc, m) -> !njc.deny(m));
    }

    @Override
    public boolean allow(final Class<?> clazz, final Method method) {
        if (!validate(clazz) || !validate(method)) {
            return false;
        }
        if ((method.getModifiers() & Modifier.STATIC) == 0) {
            final Class<?> declaring = method.getDeclaringClass();
            if (clazz != declaring) {
                // just check this is an override of a method in clazz, if not, it is not allowed (obviously)
                if (deny(clazz) || !declaring.isAssignableFrom(clazz)) {
                    return false;
                }
                // an explicit permission on the concrete class can deny; otherwise fall through to
                // allow(method) which honors carve-outs on the declaring class/interfaces (e.g. Object.getClass())
                final NoJexlClass njc = getNoJexl(clazz, null);
                if (njc != null && njc.deny(method)) {
                    return false;
                }
            }
        }
        return allow(method);
    }

    @Override
    public boolean allow(final Class<?> clazz, final Field field) {
        if (!validate(clazz) || !validate(field)) {
            return false;
        }
        if ((field.getModifiers() & Modifier.STATIC) == 0) {
            final Class<?> declaring = field.getDeclaringClass();
            if (clazz != declaring) {
                // just check this clazz extends/inherits from declaring, if not, it is not allowed (obviously)
                if (deny(clazz) || !declaring.isAssignableFrom(clazz)) {
                    return false;
                }
                // an explicit permission on the concrete class can deny; otherwise fall through to
                // allow(field) which honors carve-outs on the declaring class
                final NoJexlClass njc = getNoJexl(clazz, null);
                if (njc != null && njc.deny(field)) {
                    return false;
                }
            }
        }
        return allow(field);
    }

    /**
     * Checks whether a method explicitly allows JEXL introspection.
     * <p>Since methods can be overridden, this also checks that no superclass or interface
     * explicitly disallows this method.</p>
     *
     * @param method the method to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Method method) {
        // method must be not null, public, not synthetic, not bridge
        // method must be allowed
        if (!validate(method) || denyMethod(method)) {
            return false;
        }
        Class<?> clazz = method.getDeclaringClass();
        // gather if the packages explicitly allow any implementation of the method
        final boolean[] explicit = { specifiedAllow(clazz, method, (njc, m) -> !njc.deny(m)) };
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
     *
     * @param pack the package
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    @Override
    public boolean allow(final Package pack) {
        // field must be public
        // check declared restrictions
        if (!validate(pack) || deny(pack)) {
            return false;
        }
        // an explicit package entry is allowed unless it is the deny marker
        final String name = pack.getName();
        final NoJexlPackage njp = packages.get(name);
        return njp == null ? allowedPackage(name) : !Objects.equals(NOJEXL_PACKAGE, njp);
    }


    /**
     * Tests whether a whole class is denied Jexl visibility.
     * <p>Also checks package visibility.</p>
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
     * @param clazz the class
     * @return The class constraints instance, not-null.
     */
    private NoJexlClass getNoJexl(final Class<?> clazz) {
        return getNoJexl(clazz, JEXL_CLASS);
    }
    private NoJexlClass getNoJexl(final Class<?> clazz, final NoJexlClass ifNone) {
        final String pkgName = ClassTool.getPackageName(clazz);
        final NoJexlPackage njp = getNoJexlPackage(pkgName);
        if (njp != null) {
            final NoJexlClass njc = njp.getNoJexl(clazz);
            if (njc != null) {
                return njc;
            }
        }
        return ifNone;
    }
}
