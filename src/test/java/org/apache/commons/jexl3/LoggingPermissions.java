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

package org.apache.commons.jexl3;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.apache.commons.jexl3.introspection.JexlPermissions;

/**
 * A debugging class to determine what is allowed/denied.
 */
public class LoggingPermissions extends JexlPermissions.Delegate {
    private final Logger logger = Logger.getLogger(LoggingPermissions.class.getName());

    /**
     * Constructs a new instance.
     *
     * @param delegate the delegate.
     */
    public LoggingPermissions(JexlPermissions delegate) {
        super(delegate);
    }

    @Override
    public boolean allow(Class<?> clazz) {
        boolean allowed = super.allow(clazz);
        logger.info(String.format("Class %s is %s",
            clazz.getCanonicalName(), allowed ? "allowed" : "denied"));
        return allowed;
    }

    @Override
    public boolean allow(Constructor<?> ctor) {
        boolean allowed = super.allow(ctor);
        logger.info(String.format("Constructor %s.%s() is %s",
            ctor.getDeclaringClass().getCanonicalName(), ctor.getName(),
            allowed ? "allowed" : "denied"));
        return allowed;
    }

    @Override
    public boolean allow(Field field) {
        boolean allowed = super.allow(field);
        logger.info(String.format("Field %s.%s is %s",
            field.getDeclaringClass().getCanonicalName(), field.getName(),
            allowed ? "allowed" : "denied"));
        return allowed;
    }

    @Override
    public boolean allow(Class<?> clazz, Field field) {
        boolean allowed = super.allow(clazz, field);
        logger.info(String.format("Field %s.%s is %s for class %s",
            field.getDeclaringClass().getCanonicalName(), field.getName(),
            allowed ? "allowed" : "denied", clazz.getCanonicalName()));
        return allowed;
    }

    @Override
    public boolean allow(Method method) {
        boolean allowed = super.allow(method);
        logger.info(String.format("Method %s.%s() is %s",
            method.getDeclaringClass().getCanonicalName(), method.getName(),
            allowed ? "allowed" : "denied"));
        return allowed;
    }

    @Override
    public boolean allow(Class<?> clazz, Method method) {
        boolean allowed = super.allow(clazz, method);
        logger.info(String.format("Method %s.%s() is %s for class %s",
            method.getDeclaringClass().getCanonicalName(), method.getName(),
            allowed ? "allowed" : "denied", clazz.getCanonicalName()));
        return allowed;
    }
}
