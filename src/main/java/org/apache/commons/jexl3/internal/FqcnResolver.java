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
package org.apache.commons.jexl3.internal;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.introspection.JexlUberspect;

/**
 * Helper resolving a simple class name into a fully-qualified class name (hence FqcnResolver) using
 * package names as roots of import.
 * <p>This only keeps names of classes to avoid any class loading/reloading/permissions issue.</p>
 */
 final class FqcnResolver implements JexlContext.ClassNameResolver {
    /**
     * The class loader.
     */
    private final JexlUberspect uberspect;
    /**
     * A lock for RW concurrent ops.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * The set of packages to be used as import roots.
     */
    private final Set<String> imports = new LinkedHashSet<>();
    /**
     * The map of solved fqcns based on imports keyed on (simple) name,
     * valued as fully-qualified class name.
     */
    private final Map<String, String> fqcns = new HashMap<>();
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
        if (solver == null) {
            throw new NullPointerException("parent solver cannot be null");
        }
        this.parent = solver;
        this.uberspect = solver.uberspect;
    }

    /**
     * Creates a class name solver.
     *
     * @param uber   the optional class loader
     * @param packages the optional package names
     */
    FqcnResolver(final JexlUberspect uber, final Iterable<String> packages) {
        this.uberspect = uber;
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
            return  fqcn;
        }
        lock.readLock().lock();
        try {
            fqcn = fqcns.get(name);
        } finally {
            lock.readLock().unlock();
        }
        if (fqcn == null) {
            final ClassLoader loader = uberspect.getClassLoader();
            for (final String pkg : imports) {
                Class<?> clazz;
                try {
                    clazz = loader.loadClass(pkg + "." + name);
                } catch (final ClassNotFoundException e) {
                    // not in this package
                    continue;
                }
                // solved it, insert in map and return
                if (clazz != null) {
                    fqcn = clazz.getName();
                    lock.writeLock().lock();
                    try {
                        fqcns.put(name, fqcn);
                    } finally {
                        lock.writeLock().unlock();
                    }
                    break;
                }
            }
        }
        return fqcn;
    }

    /**
     * Adds a collection of packages as import root, checks the names are one of a package.
     * @param names the package names
     */
    private void importCheck(final Iterable<String> names) {
        if (names != null) {
            names.forEach(this::importCheck);
        }
    }

    /**
     * Adds a package as import root, checks the name if one of a package.
     * @param name the package name
     */
    private void importCheck(final String name) {
        // check the package name actually points to a package to avoid clutter
        if (name != null && Package.getPackage(name) != null) {
            imports.add(name);
        }
    }

    /**
     * Imports a list of packages as solving roots.
     *
     * @param packages the packages
     * @return this solver
     */
    FqcnResolver importPackages(final Iterable<String> packages) {
        if (packages != null) {
            lock.writeLock().lock();
            try {
                if (parent == null) {
                    importCheck(packages);
                } else {
                    packages.forEach(pkg ->{ if (!parent.isImporting(pkg)) { importCheck(pkg); }});
                }
            } finally {
                lock.writeLock().unlock();
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
        lock.readLock().lock();
        try {
            return imports.contains(pkg);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String resolveClassName(final String name) {
        return getQualifiedName(name);
    }
}
