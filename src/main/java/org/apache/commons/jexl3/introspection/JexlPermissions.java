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

import org.apache.commons.jexl3.internal.introspection.PermissionsParser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A JEXL dedicated 'security manager' that constraints which packages/classes/constructors/fields/methods
 * are made visible to JEXL scripts.
 */
public interface JexlPermissions {
    /**
     * Checks whether a package explicitly disallows JEXL introspection.
     * @param pack the package
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    boolean allow(final Package pack);

    /**
     * Checks whether a class or one of its super-classes or implemented interfaces
     * explicitly disallows JEXL introspection.
     * @param clazz the class to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    boolean allow(final Class<?> clazz);

    /**
     * Checks whether a constructor explicitly disallows JEXL introspection.
     * @param ctor the constructor to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    boolean allow(final Constructor<?> ctor);

    /**
     * Checks whether a method explicitly disallows JEXL introspection.
     * <p>Since methods can be overridden, this also checks that no superclass or interface
     * explicitly disallows this methods.</p>
     * @param method the method to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    boolean allow(final Method method);

    /**
     * Checks whether a field explicitly disallows JEXL introspection.
     * @param field the field to check
     * @return true if JEXL is allowed to introspect, false otherwise
     */
    boolean allow(final Field field);

    /**
     * Parses a set of permissions.
     * @param src the permissions source
     * @return the permissions instance
     */
    static JexlPermissions parse(String... src) {
        return new PermissionsParser().parse(src);
    }
}
