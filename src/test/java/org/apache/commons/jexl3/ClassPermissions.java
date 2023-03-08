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
package org.apache.commons.jexl3;

import org.apache.commons.jexl3.introspection.JexlPermissions;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An example of permission delegation that augments the RESTRICTED permission with an explicit
 * set of classes.
 * <p>Typical use case is to deny access to a package - and thus all its classes - but allow
 * a few specific classes.</p>
 */
public class ClassPermissions extends JexlPermissions.Delegate {
    /** The set of explicitly allowed classes, overriding the delegate permissions. */
    private final Set<String> allowedClasses;

    /**
     * Creates permissions based on the RESTRICTED set but allowing an explicit set.
     * @param allow the set of allowed classes
     */
    public ClassPermissions(Class... allow) {
        this(JexlPermissions.RESTRICTED, allow != null
                ? Arrays.asList(allow).stream().map(Class::getCanonicalName).collect(Collectors.toList())
                : null);
    }

    /**
     * Required for compose().
     * @param delegate the base to delegate to
     * @param allow the list of class canonical names
     */
    public ClassPermissions(JexlPermissions delegate, Collection<String> allow) {
        super(delegate);
        if (allow != null && !allow.isEmpty()) {
            allowedClasses = new HashSet<>();
            allow.forEach(c -> allowedClasses.add(c));
        } else {
            allowedClasses = Collections.emptySet();
        }
    }

    private boolean isClassAllowed(Class<?> clazz) {
        return allowedClasses.contains(clazz.getCanonicalName());
    }

    @Override
    public boolean allow(Class<?> clazz) {
        return (validate(clazz) && isClassAllowed(clazz)) || super.allow(clazz);
    }

    @Override
    public boolean allow(Method method) {
        return (validate(method) && isClassAllowed(method.getDeclaringClass())) || super.allow(method);
    }

    @Override
    public JexlPermissions compose(String... src) {
        return new ClassPermissions(base.compose(src), allowedClasses);
    }
}
