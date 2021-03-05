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

import java.beans.IntrospectionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.introspection.JexlMethod;

/**
 * A JexlMethod that wraps a constructor.
 */
public final class ConstructorMethod implements JexlMethod {
    /** The wrapped constructor. */
    private final Constructor<?> ctor;

    /**
     * Discovers a class constructor and wrap it as a JexlMethod.
     * @param is the introspector
     * @param ctorHandle a class or class name
     * @param args constructor arguments
     * @return a {@link JexlMethod}
     */
    public static ConstructorMethod discover(final Introspector is, final Object ctorHandle, final Object... args) {
        String className;
        Class<?> clazz = null;
        if (ctorHandle instanceof Class<?>) {
            clazz = (Class<?>) ctorHandle;
            className = clazz.getName();
        } else if (ctorHandle != null) {
            className = ctorHandle.toString();
        } else {
            return null;
        }
        final Constructor<?> ctor = is.getConstructor(clazz, new MethodKey(className, args));
        if (ctor != null) {
            return new ConstructorMethod(ctor);
        }
        return null;
    }
    /**
     * Creates a constructor method.
     * @param theCtor the constructor to wrap
     */
    ConstructorMethod(final Constructor<?> theCtor) {
        this.ctor = theCtor;
    }

    @Override
    public Object invoke(final Object obj, final Object... params) throws Exception {
        final Class<?> ctorClass = ctor.getDeclaringClass();
        boolean invoke = true;
        if (obj != null) {
            if (obj instanceof Class<?>) {
                invoke = ctorClass.equals(obj);
            } else {
                invoke = ctorClass.getName().equals(obj.toString());
            }
        }
        if (invoke) {
                return ctor.newInstance(params);
            }
        throw new IntrospectionException("constructor resolution error");
    }

    @Override
    public Object tryInvoke(final String name, final Object obj, final Object... params) {
        try {
            final Class<?> ctorClass = ctor.getDeclaringClass();
            boolean invoke = true;
            if (obj != null) {
                if (obj instanceof Class<?>) {
                    invoke = ctorClass.equals(obj);
                } else {
                    invoke = ctorClass.getName().equals(obj.toString());
                }
            }
            invoke &= name == null || ctorClass.getName().equals(name);
            if (invoke) {
                return ctor.newInstance(params);
            }
        } catch (InstantiationException | IllegalArgumentException | IllegalAccessException xinstance) {
            return Uberspect.TRY_FAILED;
        } catch (final InvocationTargetException xinvoke) {
            throw JexlException.tryFailed(xinvoke); // throw
        }
        return Uberspect.TRY_FAILED;
    }

    @Override
    public boolean tryFailed(final Object rval) {
        return rval == Uberspect.TRY_FAILED;
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return ctor.getDeclaringClass();
    }

}
