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
package org.apache.commons.jexl2.introspection;

import org.apache.commons.jexl2.internal.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Iterator;

import java.util.Map;
import org.apache.commons.jexl2.JexlInfo;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.internal.AbstractExecutor;
import org.apache.commons.jexl2.internal.ArrayIterator;
import org.apache.commons.jexl2.internal.EnumerationIterator;
import org.apache.commons.jexl2.internal.introspection.MethodKey;
import org.apache.commons.logging.Log;

/**
 * Implementation of Uberspect to provide the default introspective
 * functionality of JEXL.
 * <p>This is the class to derive to customize introspection.</p>
 *
 * @since 1.0
 */
public class UberspectImpl extends Introspector implements Uberspect {
    /**
     * Publicly exposed special failure object returned by tryInvoke.
     */
    public static final Object TRY_FAILED = AbstractExecutor.TRY_FAILED;
    
    /**
     * Creates a new UberspectImpl.
     * @param runtimeLogger the logger used for all logging needs
     */
    public UberspectImpl(Log runtimeLogger) {
        super(runtimeLogger);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Iterator<?> getIterator(Object obj, JexlInfo info) {
        if (obj instanceof Iterator<?>) {
            return ((Iterator<?>) obj);
        }
        if (obj.getClass().isArray()) {
            return new ArrayIterator(obj);
        }
        if (obj instanceof Map<?,?>) {
            return ((Map<?,?>) obj).values().iterator();
        }
        if (obj instanceof Enumeration<?>) {
            return new EnumerationIterator<Object>((Enumeration<Object>) obj);
        }
        if (obj instanceof Iterable<?>) {
            return ((Iterable<?>) obj).iterator();
        }
        try {
            // look for an iterator() method to support the JDK5 Iterable
            // interface or any user tools/DTOs that want to work in
            // foreach without implementing the Collection interface
            AbstractExecutor.Method it = getMethodExecutor(obj, "iterator", null);
            if (it != null && Iterator.class.isAssignableFrom(it.getReturnType())) {
                return (Iterator<Object>) it.execute(obj, null);
            }
        } catch(Exception xany) {
            throw new JexlException(info, "unable to generate iterator()", xany);
        }
        return null;
    }

    /**
     * Returns a class field.
     * @param obj the object
     * @param name the field name
     * @param info debug info
     * @return a {@link Field}.
     */
    public Field getField(Object obj, String name, JexlInfo info) {
        final Class<?> clazz = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        return getField(clazz, name);
    }

    /**
     * {@inheritDoc}
     */
    public Constructor<?> getConstructor(Object ctorHandle, Object[] args, JexlInfo info) {
        return getConstructor(ctorHandle, args);
    }

    /**
     * {@inheritDoc}
     */
    public JexlMethod getMethod(Object obj, String method, Object[] args, JexlInfo info) {
        return getMethodExecutor(obj, method, args);
    }

    /**
     * A JexlPropertyGet for public fields.
     */
    public static final class FieldPropertyGet implements JexlPropertyGet {
        /**
         * The public field.
         */
        private final Field field;

        /**
         * Creates a new instance of FieldPropertyGet.
         * @param theField the class public field
         */
        public FieldPropertyGet(Field theField) {
            field = theField;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object obj) throws Exception {
            return field.get(obj);
        }

        /**
         * {@inheritDoc}
         */
        public Object tryInvoke(Object obj, Object key) {
            if (obj.getClass().equals(field.getDeclaringClass()) && key.equals(field.getName())) {
                try {
                    return field.get(obj);
                } catch (IllegalAccessException xill) {
                    return TRY_FAILED;
                }
            }
            return TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCacheable() {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier, JexlInfo info) {
        JexlPropertyGet get = getGetExecutor(obj, identifier);
        if (get == null && obj != null && identifier != null) {
            Field field = getField(obj, identifier.toString(), info);
            if (field != null) {
                return new FieldPropertyGet(field);
            }
        }
        return get;
    }

    /**
     * A JexlPropertySet for public fields.
     */
    public static final class FieldPropertySet implements JexlPropertySet {
        /**
         * The public field.
         */
        private final Field field;

        /**
         * Creates a new instance of FieldPropertySet.
         * @param theField the class public field
         */
        public FieldPropertySet(Field theField) {
            field = theField;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object obj, Object arg) throws Exception {
            field.set(obj, arg);
            return arg;
        }

        /**
         * {@inheritDoc}
         */
        public Object tryInvoke(Object obj, Object key, Object value) {
            if (obj.getClass().equals(field.getDeclaringClass())
                && key.equals(field.getName())
                && (value == null || MethodKey.isInvocationConvertible(field.getType(), value.getClass(), false))) {
                try {
                    field.set(obj, value);
                    return value;
                } catch (IllegalAccessException xill) {
                    return TRY_FAILED;
                }
            }
            return TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean tryFailed(Object rval) {
            return rval == TRY_FAILED;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCacheable() {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, Object arg, JexlInfo info) {
        JexlPropertySet set = getSetExecutor(obj, identifier, arg);
        if (set == null && obj != null && identifier != null) {
            Field field = getField(obj, identifier.toString(), info);
            if (field != null
                && !Modifier.isFinal(field.getModifiers())
                && (arg == null || MethodKey.isInvocationConvertible(field.getType(), arg.getClass(), false))) {
                return new FieldPropertySet(field);
            }
        }
        return set;
    }
}
