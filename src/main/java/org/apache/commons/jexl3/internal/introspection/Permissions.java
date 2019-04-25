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
import org.apache.commons.jexl3.annotations.NoJexl;

/**
 * Checks whether an element (ctor, field or method) is visible by JEXL introspection.
 * Default implementation does this by checking if element has been annotated with NoJexl.
 */
public class Permissions {
    /** Allow inheritance. */
    protected Permissions() {
    }
    /**
     * The default singleton.
     */
    public static final Permissions DEFAULT = new Permissions();

    /**
     * Checks whether a package explicitly disallows JEXL introspection.
     * @param pack the package
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(Package pack) {
        if (pack != null && pack.getAnnotation(NoJexl.class) != null) {
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
    public boolean allow(Class<?> clazz) {
        return clazz != null && allow(clazz.getPackage()) && allow(clazz, true);
    }

    /**
     * Checks whether a constructor explicitly disallows JEXL introspection.
     * @param ctor the constructor to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(Constructor<?> ctor) {
        if (ctor == null) {
            return false;
        }
        if (!Modifier.isPublic(ctor.getModifiers())) {
            return false;
        }
        Class<?> clazz = ctor.getDeclaringClass();
        if (!allow(clazz, false)) {
            return false;
        }
        // is ctor annotated with nojexl ?
        NoJexl nojexl = ctor.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether a field explicitly disallows JEXL introspection.
     * @param field the field to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(Field field) {
        if (field == null) {
            return false;
        }
        if (!Modifier.isPublic(field.getModifiers())) {
            return false;
        }
        Class<?> clazz = field.getDeclaringClass();
        if (!allow(clazz, false)) {
            return false;
        }
        // is field annotated with nojexl ?
        NoJexl nojexl = field.getAnnotation(NoJexl.class);
        if (nojexl != null) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether a method explicitly disallows JEXL introspection.
     * <p>Since methods can be overridden, this also checks that no superclass or interface
     * explictly disallows this methods.</p>
     * @param method the method to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    public boolean allow(Method method) {
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
        // lets walk all interfaces
        for (Class<?> inter : clazz.getInterfaces()) {
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
    protected boolean allow(Class<?> clazz, boolean interf) {
        if (clazz == null) {
            return false;
        }
        if (!Modifier.isPublic(clazz.getModifiers())) {
            return false;
        }
        // lets walk all interfaces
        if (interf) {
            for (Class<?> inter : clazz.getInterfaces()) {
                // is clazz annotated with nojexl ?
                NoJexl nojexl = inter.getAnnotation(NoJexl.class);
                if (nojexl != null) {
                    return false;
                }
            }
        }
        // lets walk all super classes
        clazz = clazz.getSuperclass();
        // walk all superclasses
        while (clazz != null) {
            // is clazz annotated with nojexl ?
            NoJexl nojexl = clazz.getAnnotation(NoJexl.class);
            if (nojexl != null) {
                return false;
            }
            clazz = clazz.getSuperclass();
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
    protected boolean allow(Class<?> clazz, Method method) {
        if (clazz != null) {
            try {
                // check if method in that class is different from the method argument
                Method wmethod = clazz.getMethod(method.getName(), method.getParameterTypes());
                if (wmethod != null) {
                    NoJexl nojexl = clazz.getAnnotation(NoJexl.class);
                    if (nojexl != null) {
                        return false;
                    }
                    // check if parent declaring class said nojexl (transitivity)
                    nojexl = wmethod.getAnnotation(NoJexl.class);
                    if (nojexl != null) {
                        return false;
                    }
                }
            } catch (NoSuchMethodException ex) {
                // unexpected, return no
                return true;
            } catch (SecurityException ex) {
                // unexpected, cant do much
                return false;
            }
        }
        return true;
    }
}
