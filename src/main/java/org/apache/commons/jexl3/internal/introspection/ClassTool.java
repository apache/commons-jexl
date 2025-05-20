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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Utility for Java9+ backport in Java8 of class and module related methods.
 */
final class ClassTool {
    /** The Class.getModule() method. */
    private static final MethodHandle GET_MODULE;
    /** The Class.getPackageName() method. */
    private static final MethodHandle GET_PKGNAME;
    /** The Module.isExported(String packageName) method. */
    private static final MethodHandle IS_EXPORTED;

    static {
        final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
        MethodHandle getModule = null;
        MethodHandle getPackageName = null;
        MethodHandle isExported = null;
        try {
            final Class<?> modulec = ClassTool.class.getClassLoader().loadClass("java.lang.Module");
            if (modulec != null) {
                getModule = LOOKUP.findVirtual(Class.class, "getModule", MethodType.methodType(modulec));
                if (getModule != null) {
                    getPackageName = LOOKUP.findVirtual(Class.class, "getPackageName", MethodType.methodType(String.class));
                    if (getPackageName != null) {
                        isExported = LOOKUP.findVirtual(modulec, "isExported", MethodType.methodType(boolean.class, String.class));
                    }
                }
            }
        } catch (final Exception e) {
            // ignore all
        }
        GET_MODULE = getModule;
        GET_PKGNAME = getPackageName;
        IS_EXPORTED = isExported;
    }

    /**
     * Gets the package name of a class (class.getPackage() may return null).
     *
     * @param clz the class
     * @return the class package name
     */
    static String getPackageName(final Class<?> clz) {
        String pkgName = "";
        if (clz != null) {
            // use native if we can
            if (GET_PKGNAME != null) {
                try {
                    return (String) GET_PKGNAME.invoke(clz);
                } catch (final Throwable xany) {
                    return "";
                }
            }
            // remove array
            Class<?> clazz = clz;
            while (clazz.isArray()) {
                clazz = clazz.getComponentType();
            }
            // mimic getPackageName()
            if (clazz.isPrimitive()) {
                return "java.lang";
            }
            // remove enclosing
            Class<?> walk = clazz.getEnclosingClass();
            while (walk != null) {
                clazz = walk;
                walk = walk.getEnclosingClass();
            }
            final Package pkg = clazz.getPackage();
            // pkg may be null for unobvious reasons
            if (pkg == null) {
                final String name = clazz.getName();
                final int dot = name.lastIndexOf('.');
                if (dot > 0) {
                    pkgName = name.substring(0, dot);
                }
            } else {
                pkgName = pkg.getName();
            }
        }
        return pkgName;
    }

    /**
     * Checks whether a class is exported by its module (Java 9+).
     * The code performs the following sequence through reflection (since the same jar can run
     * on a Java8 or Java9+ runtime and the module features does not exist on 8).
     * {@code
     * Module module = declarator.getModule();
     * return module.isExported(declarator.getPackageName());
     * }
     * This is required since some classes and methods may not be exported thus not callable through
     * reflection.
     *
     * @param declarator the class
     * @return true if class is exported or no module support exists
     */
    static boolean isExported(final Class<?> declarator) {
        if (IS_EXPORTED != null) {
            try {
                final Object module = GET_MODULE.invoke(declarator);
                if (module != null) {
                    final String pkgName = (String) GET_PKGNAME.invoke(declarator);
                    return (Boolean) IS_EXPORTED.invoke(module, pkgName);
                }
            } catch (final Throwable e) {
                // ignore
            }
        }
        return true;
    }

}
