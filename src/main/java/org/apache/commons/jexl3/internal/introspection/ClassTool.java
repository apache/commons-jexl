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
 * Utility for Java9+ backport in Java8 of class and module related methods, and Java16+ backport of
 * record introspection ({@code Class#isRecord()}, {@code Class#getRecordComponents()}).
 */
final class ClassTool {

    /** {@code Class#isRecord()}; null on a pre Java-16 runtime. */
    private static final MethodHandle IS_RECORD;

    /** {@code Class#getRecordComponents()}; null on a pre Java-16 runtime. */
    private static final MethodHandle GET_RECORD_COMPONENTS;

    /** {@code java.lang.reflect.RecordComponent#getName()}; null on a pre Java-16 runtime. */
    private static final MethodHandle RECORD_COMPONENT_GET_NAME;

    /** {@code Class#getModule()}; null on a pre Java-9 runtime. */
    private static final MethodHandle GET_MODULE;

    /** {@code Class#getPackageName()}; null on a pre Java-9 runtime. */
    private static final MethodHandle GET_PKGNAME;

    /** {@code Module#isExported(String, Module)}; null on a pre Java-9 runtime. */
    private static final MethodHandle IS_EXPORTED;

    /** The {@code java.lang.Module} that declares this class; null on a pre Java-9 runtime. */
    private static final Object JEXL_MODULE;

    static {
        final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
        final ClassLoader loader = ClassTool.class.getClassLoader();

        // Java 16+ record introspection backport
        MethodHandle isRecord = null;
        MethodHandle getRecordComponents = null;
        MethodHandle recordComponentGetName = null;
        try {
            final Class<?> componentc = loader.loadClass("java.lang.reflect.RecordComponent");
            final Class<?> componentArrayc = java.lang.reflect.Array.newInstance(componentc, 0).getClass();
            isRecord = LOOKUP.findVirtual(Class.class, "isRecord", MethodType.methodType(boolean.class));
            getRecordComponents = LOOKUP.findVirtual(Class.class, "getRecordComponents", MethodType.methodType(componentArrayc));
            recordComponentGetName = LOOKUP.findVirtual(componentc, "getName", MethodType.methodType(String.class));
        } catch (final Throwable xnotfound) {
            // ignore all; records unsupported on this runtime
        }
        IS_RECORD = isRecord;
        GET_RECORD_COMPONENTS = getRecordComponents;
        RECORD_COMPONENT_GET_NAME = recordComponentGetName;

        // Java 9+ module reflection backport
        MethodHandle getModule = null;
        MethodHandle getPackageName = null;
        MethodHandle isExported = null;
        Object myModule = null;
        try {
            final Class<?> modulec = loader.loadClass("java.lang.Module");
            if (modulec != null) {
                getModule = LOOKUP.findVirtual(Class.class, "getModule", MethodType.methodType(modulec));
                if (getModule != null) {
                    getPackageName = LOOKUP.findVirtual(Class.class, "getPackageName", MethodType.methodType(String.class));
                    if (getPackageName != null) {
                        myModule = getModule.invoke(ClassTool.class);
                        isExported = LOOKUP.findVirtual(modulec, "isExported", MethodType.methodType(boolean.class, String.class, modulec));
                    }
                }
            }
        } catch (final Throwable e) {
            // ignore all
        }
        JEXL_MODULE = myModule;
        GET_MODULE = getModule;
        GET_PKGNAME = getPackageName;
        IS_EXPORTED = isExported;
    }

    /**
     * Whether the given class is a record on this runtime.
     *
     * @param clazz the class to check
     * @return true if clazz is a record, false if it is not or if records are unsupported here
     */
    static boolean isRecord(final Class<?> clazz) {
        try {
            return IS_RECORD != null && (boolean) IS_RECORD.invoke(clazz);
        } catch (final Throwable xfail) {
            return false;
        }
    }

    /**
     * Whether the given class declares a record component named {@code property}.
     * <p>Used only to confirm {@code property} is a genuine record component before the accessor is
     * resolved through the (permission-checked) {@link Introspector}; the accessor method instances
     * gathered here are discarded.</p>
     *
     * @param clazz the record class
     * @param property the property name to match against the record's components
     * @return true if clazz declares a record component named property
     */
    static boolean hasRecordComponent(final Class<?> clazz, final String property) {
        if (GET_RECORD_COMPONENTS != null && RECORD_COMPONENT_GET_NAME != null) {
            try {
                final Object[] components = (Object[]) GET_RECORD_COMPONENTS.invoke(clazz);
                for (final Object component : components) {
                    if (property.equals(RECORD_COMPONENT_GET_NAME.invoke(component))) {
                        return true;
                    }
                }
            } catch (final Throwable xfail) {
                // ignore and fall through to return false
            }
        }
        return false;
    }

    /**
     * Gets the package name of a class (class.getPackage() may return null).
     *
     * @param clz The class
     * @return The class package name
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
     * Checks whether a class is exported by its module (Java 9+) to JEXL.
     * The code performs the following sequence through reflection (since the same jar can run
     * on a Java8 or Java9+ runtime and the module features does not exist on 8).
     * {@code
     * Module jexlModule = ClassTool.class.getModule();
     * Module module = declarator.getModule();
     * return module.isExported(declarator.getPackageName(), jexlModule);
     * }
     * This is required since some classes and methods may not be exported thus not callable through
     * reflection.  A package can be non-exported, <em>unconditionally</em> exported (to all reading
     * modules), or use <em>qualified</em> exports to only export the package to specifically named
     * modules.  This method is only concerned with whether JEXL may reflectively access the
     * package, so a qualified export naming the JEXL module is the least-privilege access required.
     * The declarator's module may also use: unqualified exports, qualified {@code opens}, or
     * unqualified {@code opens}, in increasing order of privilege; the last two allow reflective
     * access to non-public members and are not recommended.
     *
     * @param declarator The class
     * @return true if class is exported (to JEXL) or no module support exists
     */
    static boolean isExported(final Class<?> declarator) {
        if (IS_EXPORTED != null) {
            try {
                final Object module = GET_MODULE.invoke(declarator);
                if (module != null) {
                    final String pkgName = (String) GET_PKGNAME.invoke(declarator);
                    return (Boolean) IS_EXPORTED.invoke(module, pkgName, JEXL_MODULE);
                }
            } catch (final Throwable e) {
                // ignore
            }
        }
        return true;
    }

}
