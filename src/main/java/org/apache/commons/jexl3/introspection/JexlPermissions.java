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
package org.apache.commons.jexl3.introspection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.jexl3.internal.introspection.PermissionsParser;

/**
 * This interface describes permissions used by JEXL introspection that constrain which
 * packages/classes/constructors/fields/methods are made visible to JEXL scripts.
 * <p>By specifying or implementing permissions, it is possible to constrain precisely which objects can be manipulated
 * by JEXL, allowing users to enter their own expressions or scripts whilst maintaining tight control
 * over what can be executed. JEXL introspection mechanism will check whether it is permitted to
 * access a constructor, method or field before exposition to the {@link JexlUberspect}. The restrictions
 * are applied in all cases, for any {@link org.apache.commons.jexl3.introspection.JexlUberspect.ResolverStrategy}.
 * </p>
 * <p>This complements using a dedicated {@link ClassLoader} and/or {@link SecurityManager} - being deprecated -
 * and possibly {@link JexlSandbox} with a simpler mechanism. The {@link org.apache.commons.jexl3.annotations.NoJexl}
 * annotation processing is actually performed using the result of calling {@link #parse(String...)} with no arguments;
 * implementations shall delegate calls to its methods for {@link org.apache.commons.jexl3.annotations.NoJexl} to be
 * processed.</p>
 * <p>A simple textual configuration can be used to create user-defined permissions using
 * {@link JexlPermissions#parse(String...)}.</p>
 *
 *<p>To instantiate a JEXL engine using permissions, one should use a {@link org.apache.commons.jexl3.JexlBuilder}
 * and call {@link org.apache.commons.jexl3.JexlBuilder#permissions(JexlPermissions)}. Another approach would
 * be to instantiate a {@link JexlUberspect} with those permissions and call
 * {@link org.apache.commons.jexl3.JexlBuilder#uberspect(JexlUberspect)}.</p>
 *
 * <p>
 *     To help migration from earlier versions, it is possible to revert to the JEXL 3.2 default lenient behavior
 *     by calling {@link org.apache.commons.jexl3.JexlBuilder#setDefaultPermissions(JexlPermissions)} with
 *     {@link #UNRESTRICTED} as parameter before creating a JEXL engine instance.
 * </p>
 * <p>
 *     For the same reason, using JEXL through scripting, it is possible to revert the underlying JEXL behavior to
 *     JEXL 3.2 default by calling {@link org.apache.commons.jexl3.scripting.JexlScriptEngine#setPermissions(JexlPermissions)}
 *     with {@link #UNRESTRICTED} as parameter.
 * </p>
 * @since 3.3
 */
public interface JexlPermissions {

    /**
     * A permission delegation that augments the RESTRICTED permission with an explicit
     * set of classes.
     * <p>Typical use case is to deny access to a package - and thus all its classes - but allow
     * a few specific classes.</p>
     * <p>Note that the newer positive restriction syntax is preferable as in:
     * <code>RESTRICTED.compose("java.lang { +Class {} }")</code>.</p>
     */
     final class ClassPermissions extends JexlPermissions.Delegate {
        /** The set of explicitly allowed classes, overriding the delegate permissions. */
        private final Set<String> allowedClasses;

        /**
         * Creates permissions based on the RESTRICTED set but allowing an explicit set.
         * @param allow the set of allowed classes
         */
        public ClassPermissions(final Class<?>... allow) {
            this(JexlPermissions.RESTRICTED,
                    Arrays.stream(Objects.requireNonNull(allow))
                        .map(Class::getCanonicalName)
                        .collect(Collectors.toList()));
        }

        /**
         * Required for compose().
         * @param delegate the base to delegate to
         * @param allow the list of class canonical names
         */
        public ClassPermissions(final JexlPermissions delegate, final Collection<String> allow) {
            super(Objects.requireNonNull(delegate));
            allowedClasses = new HashSet<>(Objects.requireNonNull(allow));
        }

        @Override
        public boolean allow(final Class<?> clazz) {
            return validate(clazz) && isClassAllowed(clazz) || super.allow(clazz);
        }

        @Override
        public boolean allow(final Constructor<?> constructor) {
            return validate(constructor) && isClassAllowed(constructor.getDeclaringClass()) || super.allow(constructor);
        }

        @Override
        public boolean allow(final Method method) {
            return validate(method) && isClassAllowed(method.getDeclaringClass()) || super.allow(method);
        }

        @Override
        public JexlPermissions compose(final String... src) {
            return new ClassPermissions(base.compose(src), allowedClasses);
        }

        private boolean isClassAllowed(final Class<?> clazz) {
            return allowedClasses.contains(clazz.getCanonicalName());
        }
    }

    /**
     * A base for permission delegation allowing functional refinement.
     * Overloads should call the appropriate validate() method early in their body.
     */
     class Delegate implements JexlPermissions {
         /** The permissions we delegate to. */
        protected final JexlPermissions base;

        /**
         * Constructs a new instance.
         *
         * @param delegate the delegate.
         */
        protected Delegate(final JexlPermissions delegate) {
            base = delegate;
        }

        @Override
        public boolean allow(final Class<?> clazz) {
            return base.allow(clazz);
        }

        @Override
        public boolean allow(final Constructor<?> ctor) {
            return base.allow(ctor);
        }

        @Override
        public boolean allow(final Field field) {
            return base.allow(field);
        }

        @Override
        public boolean allow(final Method method) {
            return base.allow(method);
        }

        @Override
        public boolean allow(final Package pack) {
            return base.allow(pack);
        }

        @Override
        public JexlPermissions compose(final String... src) {
            return new Delegate(base.compose(src));
        }
    }

    /**
     * The unrestricted permissions.
     * <p>This enables any public class, method, constructor or field to be visible to JEXL and used in scripts.</p>
     * @since 3.3
     */
    JexlPermissions UNRESTRICTED = JexlPermissions.parse();

    /**
     * A restricted singleton.
     * <p>The RESTRICTED set is built using the following allowed packages and denied packages/classes.</p>
     * <p>Of particular importance are the restrictions on the {@link System},
     * {@link Runtime}, {@link ProcessBuilder}, {@link Class} and those on {@link java.net}, {@link java.net},
     * {@link java.io} and {@link java.lang.reflect} that should provide a decent level of isolation between the scripts
     * and its host.
     * </p>
     * <p>
     * As a simple guide, any line that ends with &quot;.*&quot; is allowing a package, any other is
     * denying a package, class or method.
     * </p>
     * <ul>
     * <li>java.nio.*</li>
     * <li>java.io.*</li>
     * <li>java.lang.*</li>
     * <li>java.math.*</li>
     * <li>java.text.*</li>
     * <li>java.util.*</li>
     * <li>org.w3c.dom.*</li>
     * <li>org.apache.commons.jexl3.*</li>
     *
     * <li>org.apache.commons.jexl3 { JexlBuilder {} }</li>
     * <li>org.apache.commons.jexl3.internal { Engine {} }</li>
     * <li>java.lang { Runtime {} System {} ProcessBuilder {} Class {} }</li>
     * <li>java.lang.annotation {}</li>
     * <li>java.lang.instrument {}</li>
     * <li>java.lang.invoke {}</li>
     * <li>java.lang.management {}</li>
     * <li>java.lang.ref {}</li>
     * <li>java.lang.reflect {}</li>
     * <li>java.net {}</li>
     * <li>java.io { File { } }</li>
     * <li>java.nio { Path { } Paths { } Files { } }</li>
     * <li>java.rmi {}</li>
     * </ul>
     */
    JexlPermissions RESTRICTED = JexlPermissions.parse(
            "# Restricted Uberspect Permissions",
            "java.nio.*",
            "java.io.*",
            "java.lang.*",
            "java.math.*",
            "java.text.*",
            "java.util.*",
            "org.w3c.dom.*",
            "org.apache.commons.jexl3.*",
            "org.apache.commons.jexl3 { JexlBuilder {} }",
            "org.apache.commons.jexl3.internal { Engine {} }",
            "java.lang { Runtime{} System{} ProcessBuilder{} Process{}" +
                    " RuntimePermission{} SecurityManager{}" +
                    " Thread{} ThreadGroup{} Class{} }",
            "java.lang.annotation {}",
            "java.lang.instrument {}",
            "java.lang.invoke {}",
            "java.lang.management {}",
            "java.lang.ref {}",
            "java.lang.reflect {}",
            "java.net {}",
            "java.io { File{} FileDescriptor{} }",
            "java.nio { Path { } Paths { } Files { } }",
            "java.rmi"
    );

    /**
     * Parses a set of permissions.
     * <p>
     * In JEXL 3.3, the syntax recognizes 2 types of permissions:
     * </p>
     * <ul>
     * <li>Allowing access to a wildcard restricted set of packages. </li>
     * <li>Denying access to packages, classes (and inner classes), methods and fields</li>
     * </ul>
     * <p>Wildcards specifications determine the set of allowed packages. When empty, all packages can be
     * used. When using JEXL to expose functional elements, their packages should be exposed through wildcards.
     * These allow composing the volume of what is allowed by addition.</p>
     * <p>Restrictions behave exactly like the {@link org.apache.commons.jexl3.annotations.NoJexl} annotation;
     * they can restrict access to package, class, inner-class, methods and fields.
     *  These allow refining the volume of what is allowed by extrusion.</p>
     *  An example of a tight environment that would not allow scripts to wander could be:
     *  <pre>
     *  # allow a very restricted set of base classes
     *  java.math.*
     *  java.text.*
     *  java.util.*
     *  # deny classes that could pose a security risk
     *  java.lang { Runtime {} System {} ProcessBuilder {} Class {} }
     *  org.apache.commons.jexl3 { JexlBuilder {} }
     *  </pre>
     *  <ul>
     *  <li>Syntax for wildcards is the name of the package suffixed by {@code .*}.</li>
     *  <li>Syntax for restrictions is a list of package restrictions.</li>
     *  <li>A package restriction is a package name followed by a block (as in curly-bracket block {})
     *  that contains a list of class restrictions.</li>
     *  <li>A class restriction is a class name prefixed by an optional {@code -} or {@code +} sign
     *  followed by a block of member restrictions.</li>
     *  <li>A member restriction can be a class restriction - to restrict
     *  nested classes -, a field which is the Java field name suffixed with {@code ;}, a method composed of
     *  its Java name suffixed with {@code ();}. Constructor restrictions are specified like methods using the
     *  class name as method name.</li>
     *  <li>By default or when prefixed with a {@code -}, a class restriction is explicitly denying the members
     *  declared in its block (or the whole class)</li>
     *  <li>When prefixed with a {@code +}, a class restriction is explicitly allowing the members
     *  declared in its block (or the whole class)</li>
     *  </ul>
     *  <p>
     *  All overrides and overloads of a constructors or method are allowed or restricted at the same time,
     *  the restriction being based on their names, not their whole signature. This differs from the @NoJexl annotation.
     *  </p>
     *  <pre>
     *  # some wildcards
     *  java.lang.*; # java.lang is pretty much a must have
     *  my.allowed.package0.*
     *  another.allowed.package1.*
     *  # nojexl like restrictions
     *  my.package.internal {} # the whole package is hidden
     *  my.package {
     *   +class4 { theMethod(); } # only theMethod can be called in class4
     *   class0 {
     *     class1 {} # the whole class1 is hidden
     *     class2 {
     *         class2(); # class2 constructors cannot be invoked
     *         class3 {
     *             aMethod(); # aMethod cannot be called
     *             aField; # aField cannot be accessed
     *         }
     *     } # end of class2
     *     class0(); # class0 constructors cannot be invoked
     *     method(); # method cannot be called
     *     field; # field cannot be accessed
     *   } # end class0
     * } # end package my.package
     * </pre>
     *
     * @param src the permissions source, the default (NoJexl aware) permissions if null
     * @return the permissions instance
     * @since 3.3
     */
    static JexlPermissions parse(final String... src) {
        return new PermissionsParser().parse(src);
    }

    /**
     * Checks whether a class allows JEXL introspection.
     * <p>If the class disallows JEXL introspection, none of its constructors, methods or fields
     * as well as derived classes are visible to JEXL and cannot be used in scripts or expressions.
     * If one of its super-classes is not allowed, tbe class is not allowed either.</p>
     * <p>For interfaces, only methods and fields are disallowed in derived interfaces or implementing classes.</p>
     * @param clazz the class to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(final Class<?> clazz);

    /**
     * Checks whether a constructor allows JEXL introspection.
     * <p>If a constructor is not allowed, the new operator cannot be used to instantiate its declared class
     * in scripts or expressions.</p>
     * @param ctor the constructor to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(final Constructor<?> ctor);

    /**
     * Checks whether a field explicitly disallows JEXL introspection.
     * <p>If a field is not allowed, it cannot resolved and accessed in scripts or expressions.</p>
     * @param field the field to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(final Field field);
    /**
     * Checks whether a method allows JEXL introspection.
     * <p>If a method is not allowed, it cannot resolved and called in scripts or expressions.</p>
     * <p>Since methods can be overridden and overloaded, this also checks that no superclass or interface
     * explicitly disallows this methods.</p>
     * @param method the method to check
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(final Method method);

    /**
     * Checks whether a package allows JEXL introspection.
     * <p>If the package disallows JEXL introspection, none of its classes or interfaces are visible
     * to JEXL and cannot be used in scripts or expression.</p>
     * @param pack the package
     * @return true if JEXL is allowed to introspect, false otherwise
     * @since 3.3
     */
    boolean allow(final Package pack);

    /**
     * Compose these permissions with a new set.
     * <p>This is a convenience method meant to easily give access to the packages JEXL is
     * used to integrate with. For instance, using <code>{@link #RESTRICTED}.compose("com.my.app.*")</code>
     * would extend the restricted set of permissions by allowing the com.my.app package.</p>
     * @param src the new constraints
     * @return the new permissions
     */
    JexlPermissions compose(final String... src);

    /**
     * Checks that a class is valid for permission check.
     * @param clazz the class
     * @return true if the class is not null, false otherwise
     */
    default boolean validate(final Class<?> clazz) {
        return clazz != null;
    }

    /**
     * Checks that a constructor is valid for permission check.
     * @param constructor the constructor
     * @return true if constructor is not null and public, false otherwise
     */
    default boolean validate(final Constructor<?> constructor) {
        return constructor != null && Modifier.isPublic(constructor.getModifiers());
    }

    /**
     * Checks that a field is valid for permission check.
     * @param field the constructor
     * @return true if field is not null and public, false otherwise
     */
    default boolean validate(final Field field) {
        return field != null && Modifier.isPublic(field.getModifiers());
    }

    /**
     * Checks that a method is valid for permission check.
     * @param method the method
     * @return true if method is not null and public, false otherwise
     */
    default boolean validate(final Method method) {
        return method != null && Modifier.isPublic(method.getModifiers());
    }

    /**
     * Checks that a package is valid for permission check.
     * @param pack the package
     * @return true if the class is not null, false otherwise
     */
    default boolean validate(final Package pack) {
        return pack != null;
    }
}
