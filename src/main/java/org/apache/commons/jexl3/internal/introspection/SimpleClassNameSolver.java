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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Helper solving a simple class name into a fully-qualified class name using packages.
 */
public class SimpleClassNameSolver {
    /**
     * The class loader.
     */
    private final ClassLoader loader;
    /**
     * A lock for RW concurrent ops.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * The set of packages to be used as import.
     */
    private final Set<String> imports = new LinkedHashSet<>();
    /**
     * The set of solved fqcns based on imports.
     */
    private final Map<String, String> fqcns = new HashMap<>();
    /**
     * Optional parent solver.
     */
    private final SimpleClassNameSolver parent;

    /**
     * Creates a class name solver.
     *
     * @param loader   the optional class loader
     * @param packages the optional package names
     */
    public SimpleClassNameSolver(ClassLoader loader, List<String> packages) {
        this.loader = loader == null ? SimpleClassNameSolver.class.getClassLoader() : loader;
        if (packages != null) {
            imports.addAll(packages);
        }
        this.parent = null;
    }

    /**
     * Creates a class name solver.
     * @param solver the parent solver
     * @throws NullPointerException if parent solver is null
     */
    public SimpleClassNameSolver(SimpleClassNameSolver solver) {
        if (solver == null) {
            throw new NullPointerException("parent solver can not be null");
        }
        this.parent = solver;
        this.loader = solver.loader;
    }

    /**
     * Checks is a package is imported by this solver of one of its ascendants.
     * @param pkg the package name
     * @return true if an import exists for this package, false otherwise
     */
    boolean isImporting(String pkg) {
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

    /**
     * Adds a list of packages as solving roots.
     *
     * @param packages the packages
     */
    public void addPackages(Collection<String> packages) {
        if (packages != null) {
            lock.writeLock().lock();
            try {
                if (parent == null) {
                    imports.addAll(packages);
                } else {
                    for(String pkg : packages) {
                        if (!parent.isImporting(pkg)) {
                            imports.add(pkg);
                        }
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Gets a fully qualified class name from a simple class name and imports.
     *
     * @param name the simple name
     * @return the fqcn
     */
    public String getQualifiedName(String name) {
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
            Class<?> clazz;
            for (String pkg : imports) {
                try {
                    clazz = loader.loadClass(pkg + "." + name);
                    lock.writeLock().lock();
                    try {
                        fqcns.put(name, fqcn = clazz.getName());
                        break;
                    } finally {
                        lock.writeLock().unlock();
                    }
                } catch (ClassNotFoundException e) {
                    // nope
                }
            }
        }
        return fqcn;
    }
}
