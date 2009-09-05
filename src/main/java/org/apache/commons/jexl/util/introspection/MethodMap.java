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

package org.apache.commons.jexl.util.introspection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:bob@werken.com">Bob McWhirter</a>
 * @author <a href="mailto:Christoph.Reck@dlr.de">Christoph Reck</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="mailto:szegedia@freemail.hu">Attila Szegedi</a>
 * @version $Id$
 * @since 1.0
 */
final class MethodMap {
    /**
     * Keep track of all methods with the same name.
     */
    // CSOFF: VisibilityModifier
    private Map<String, List<Method>> methodByNameMap = new HashMap<String, List<Method>>();
    // CSON: VisibilityModifier

    /**
     * Add a method to a list of methods by name. For a particular class we are
     * keeping track of all the methods with the same name.
     *
     * @param method the method.
     */
    public synchronized void add(Method method) {
        String methodName = method.getName();

        List<Method> l = methodByNameMap.get(methodName);

        if (l == null) {
            l = new ArrayList<Method>();
            methodByNameMap.put(methodName, l);
        }

        l.add(method);
    }

    /**
     * Return a list of methods with the same name.
     *
     * @param key the name.
     * @return List list of methods.
     */
    public synchronized List<Method> get(String key) {
        return methodByNameMap.get(key);
    }

    /**
     * Returns the array of method names accessible in this class.
     * @return the array of names
     */
    public synchronized String[] names() {
        java.util.Set<String> set = methodByNameMap.keySet();
        return set.toArray(new String[set.size()]);
    }

    /**
     * <p>
     * Find a method.  Attempts to find the
     * most specific applicable method using the
     * algorithm described in the JLS section
     * 15.12.2 (with the exception that it can't
     * distinguish a primitive type argument from
     * an object type argument, since in reflection
     * primitive type arguments are represented by
     * their object counterparts, so for an argument of
     * type (say) java.lang.Integer, it will not be able
     * to decide between a method that takes int and a
     * method that takes java.lang.Integer as a parameter.
     * </p>
     *
     * <p>
     * This turns out to be a relatively rare case
     * where this is needed - however, functionality
     * like this is needed.
     * </p>
     *
     * @param methodName name of method
     * @param args       the actual arguments with which the method is called
     * @return the most specific applicable method, or null if no
     *         method is applicable.
     * @throws MethodKey.AmbiguousException if there is more than one maximally
     *                            specific applicable method
     */
    // CSOFF: RedundantThrows
    public Method find(String methodName, Object[] args) throws MethodKey.AmbiguousException {
        return find(new MethodKey(methodName, args));
    }

    /**
     * Finds a method by key.
     * @param methodKey the key
     * @return the method
     * @throws MethodKey.AmbiguousException if find is ambiguous
     */
    Method find(MethodKey methodKey) throws MethodKey.AmbiguousException {
        List<Method> methodList = get(methodKey.getMethod());
        if (methodList == null) {
            return null;
        }
        return methodKey.getMostSpecificMethod(methodList);
    } // CSON: RedundantThrows

}