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
package org.apache.commons.jexl3.internal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlUberspect;

/**
 * Helper resolving a simple class name into a Fully Qualified Class Name (hence FqcnResolver) using
 * package names and classes as roots of import.
 * <p>This only keeps the names of the classes to avoid any class loading/reloading/permissions issue.</p>
 */
public class FqcnResolver implements JexlUberspect.ClassConstantResolver {
    /**
     * The uberspect.
     */
    private final JexlUberspect uberspect;
    /**
     * The set of packages to be used as import roots.
     */
    private final Set<String> imports = Collections.synchronizedSet(new LinkedHashSet<>());
    /**
     * The map of solved fqcns based on imports keyed on (simple) name,
     * valued as fully qualified class name.
     */
    private final Map<String, String> fqcns = new ConcurrentHashMap<>();
    /**
     * Optional parent solver.
     */
    private final FqcnResolver parent;

    /**
     * Creates a class name solver.
     *
     * @param solver the parent solver
     * @throws NullPointerException if parent solver is null
     */
    FqcnResolver(final FqcnResolver solver) {
        this.parent = Objects.requireNonNull(solver, "solver");
        this.uberspect = solver.uberspect;
    }

    /**
     * Creates a class name solver.
     *
     * @param uber     the optional class loader
     * @param packages the optional package names
     */
    FqcnResolver(final JexlUberspect uber, final Iterable<String> packages) {
        this.uberspect = Objects.requireNonNull(uber, "uberspect");
        this.parent = null;
        importCheck(packages);
    }

    /**
     * Gets a fully qualified class name from a simple class name and imports.
     *
     * @param name the simple name
     * @return the fqcn
     */
    String getQualifiedName(final String name) {
        String fqcn;
        if (parent != null && (fqcn = parent.getQualifiedName(name)) != null) {
            return fqcn;
        }
        return fqcns.computeIfAbsent(name, this::solveClassName);
    }

    /**
     * Attempts to solve a fully qualified class name from a simple class name.
     * <p>It tries to solve the class name as package.classname or package$classname (inner class).</p>
     *
     * @param name the simple class name
     * @return the fully qualified class name or null if not found
     */
    private String solveClassName(String name) {
        for (final String pkg : imports) {
            // try package.classname or fqcn$classname (inner class)
            for (char dot : new char[]{'.', '$'}) {
                Class<?> clazz = uberspect.getClassByName(pkg + dot + name);
                // solved it
                if (clazz != null) {
                    return clazz.getName();
                }
            }
        }
        return null;
    }

    /**
     * Adds a collection of packages/classes as import root, check each name point to one or the other.
     *
     * @param names the package names
     */
    private void importCheck(final Iterable<String> names) {
        if (names != null) {
            names.forEach(this::importCheck);
        }
    }

    /**
     * Adds a package as import root, checks the name points to a package or a class.
     *
     * @param name the package name
     */
    private void importCheck(final String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        // check the package name actually points to a package to avoid clutter
        Package pkg = Package.getPackage(name);
        if (pkg == null) {
            // if it is a class, solve it now
            Class<?> clazz = uberspect.getClassByName(name);
            if (clazz == null) {
                throw new JexlException(null, "Cannot import '" + name + "' as it is neither a package nor a class");
            }
            fqcns.put(name, clazz.getName());
        }
        imports.add(name);
    }

    /**
     * Imports a list of packages as solving roots.
     *
     * @param packages the packages
     * @return this solver
     */
    FqcnResolver importPackages(final Iterable<String> packages) {
        if (packages != null) {
            if (parent == null) {
                importCheck(packages);
            } else {
                packages.forEach(pkg -> {
                    if (!parent.isImporting(pkg)) {
                        importCheck(pkg);
                    }
                });
            }
        }
        return this;
    }

    /**
     * Checks is a package is imported by this solver of one of its ascendants.
     *
     * @param pkg the package name
     * @return true if an import exists for this package, false otherwise
     */
    boolean isImporting(final String pkg) {
        if (parent != null && parent.isImporting(pkg)) {
            return true;
        }
        return imports.contains(pkg);
    }

    @Override
    public String resolveClassName(final String name) {
        return getQualifiedName(name);
    }

    @Override
    public Object resolveConstant(final String cname) {
        return getConstant(cname.split("\\."));
    }

    private Object getConstant(final String... ids) {
        if (ids.length == 1) {
            final String pname = ids[0];
            for (String cname : fqcns.keySet()) {
                Object constant = getConstant(cname, pname);
                if (constant != JexlEngine.TRY_FAILED) {
                    return constant;
                }
            }
        } else if (ids.length == 2) {
            String cname = ids[0];
            String id = ids[1];
            String fqcn = resolveClassName(cname);
            if (fqcn != null) {
                Class<?> clazz = uberspect.getClassByName(fqcn);
                if (clazz != null) {
                    JexlPropertyGet getter = uberspect.getPropertyGet(clazz, id);
                    if (getter != null && getter.isConstant()) {
                        try {
                            return getter.invoke(clazz);
                        } catch (Exception xany) {
                            // ignore
                        }
                    }
                }
            }
        }
        return JexlEngine.TRY_FAILED;
    }
}
