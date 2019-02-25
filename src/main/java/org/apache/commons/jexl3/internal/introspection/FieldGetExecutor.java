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

import java.lang.reflect.Field;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;

/**
 * A JexlPropertyGet for public fields.
 */
public final class FieldGetExecutor extends AbstractExecutor.Get {
    /** The java.lang.reflect.Method.get method used as an active marker in FieldGet. */
    private static final java.lang.reflect.Method FIELD_GET = null;
    /**
     * The public field.
     */
    private final Field field;

    /**
     * Attempts to discover a FieldGetExecutor.
     *
     * @param is the introspector
     * @param clazz the class to find the get method from
     * @param identifier the key to use as an argument to the get method
     * @return the executor if found, null otherwise
     */
    public static JexlPropertyGet discover(Introspector is, Class<?> clazz, String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }
        Field field = is.getField(clazz, identifier);
        if (field != null) {
            return new FieldGetExecutor(field);
        }
        return null;
    }
    /**
     * Creates a new instance of FieldPropertyGet.
     * @param theField the class public field
     */
    private FieldGetExecutor(Field theField) {
        super(theField.getDeclaringClass(), FIELD_GET);
        field = theField;
    }

    @Override
    public Object invoke(Object obj) throws Exception {
        return field.get(obj);
    }

    @Override
    public Object tryInvoke(Object obj, Object key) {
        if (objectClass == obj.getClass() && field.getName().equals(castString(key))) {
            try {
                return field.get(obj);
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;
            }
        }
        return TRY_FAILED;
    }

    @Override
    public boolean isCacheable() {
        return true;
    }

}
