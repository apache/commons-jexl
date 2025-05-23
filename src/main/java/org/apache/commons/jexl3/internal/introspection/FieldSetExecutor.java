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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.jexl3.introspection.JexlPropertySet;

/**
 * A JexlPropertySet for public fields.
 */
public final class FieldSetExecutor implements JexlPropertySet {
    /**
     * Attempts to discover a FieldSetExecutor.
     *
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param identifier the key to use as an argument to the get method
     * @param value the value to set the field to
     * @return the executor if found, null otherwise
     */
    public static JexlPropertySet discover(final Introspector is,
                                           final Class<?> clazz,
                                           final String identifier,
                                           final Object value) {
        if (identifier != null) {
            final Field field = is.getField(clazz, identifier);
            if (field != null
                && !Modifier.isFinal(field.getModifiers())
                && (value == null || MethodKey.isInvocationConvertible(field.getType(), value.getClass(), false))) {
                return new FieldSetExecutor(field);
            }
        }
        return null;
    }

    /**
     * The public field.
     */
    private final Field field;

    /**
     * Creates a new instance of FieldPropertySet.
     * @param theField the class public field
     */
    private FieldSetExecutor(final Field theField) {
        field = theField;
    }

    @Override
    public Object invoke(final Object obj, final Object arg) throws Exception {
        field.set(obj, arg);
        return arg;
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

    @Override
    public boolean tryFailed(final Object rval) {
        return rval == Uberspect.TRY_FAILED;
    }

    @Override
    public Object tryInvoke(final Object obj, final Object key, final Object value) {
        if (obj.getClass().equals(field.getDeclaringClass())
            && key.equals(field.getName())
            && (value == null || MethodKey.isInvocationConvertible(field.getType(), value.getClass(), false))) {
            try {
                field.set(obj, value);
                return value;
            } catch (final IllegalAccessException xill) {
                return Uberspect.TRY_FAILED;
            }
        }
        return Uberspect.TRY_FAILED;
    }
}
