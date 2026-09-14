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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.jexl3.JexlException;

/**
 * Specialized executor to get a property from a Java record component.
 * <p>A record (JEP 395, Java 16+) exposes one accessor per component, named exactly like the
 * component - {@code x()}, not {@code getX()}. {@link PropertyGetExecutor} only looks for the bean
 * convention, so a record component was otherwise never resolved as a property.</p>
 * <p>Record detection and lookup are done entirely through reflection so this class - like the rest of
 * this module - remains usable on the Java 8 baseline this project still targets; on such a runtime,
 * {@code Class#isRecord()} and {@code Class#getRecordComponents()} simply do not exist and discovery
 * quietly reports no match instead of failing to link.</p>
 *
 * @since 3.7.2
 */
public final class RecordGetExecutor extends AbstractExecutor.Get {

    /** {@code Class#isRecord()}, resolved once; null on a pre Java-16 runtime. */
    private static final java.lang.reflect.Method IS_RECORD = findNoArgMethod(Class.class, "isRecord");

    /** {@code Class#getRecordComponents()}, resolved once; null on a pre Java-16 runtime. */
    private static final java.lang.reflect.Method GET_RECORD_COMPONENTS = findNoArgMethod(
        Class.class, "getRecordComponents"
    );

    /** {@code java.lang.reflect.RecordComponent#getName()}, resolved once; null on a pre Java-16 runtime. */
    private static final java.lang.reflect.Method COMPONENT_GET_NAME = findComponentMethod("getName");

    /** A static signature for method(). */
    private static final Object[] EMPTY_PARAMS = {};

    /**
     * Looks up a public no-argument method by name, tolerating its absence.
     *
     * @param onClass the class to look the method up on
     * @param name the method name
     * @return the method, or null if it does not exist on this runtime
     */
    private static java.lang.reflect.Method findNoArgMethod(final Class<?> onClass, final String name) {
        try {
            return onClass.getMethod(name);
        } catch (final NoSuchMethodException xnotfound) {
            return null;
        }
    }

    /**
     * Looks up a public no-argument method on {@code java.lang.reflect.RecordComponent}, tolerating the
     * type itself being absent on this runtime.
     *
     * @param name the method name
     * @return the method, or null if unavailable on this runtime
     */
    private static java.lang.reflect.Method findComponentMethod(final String name) {
        try {
            final Class<?> recordComponent = Class.forName("java.lang.reflect.RecordComponent");
            return recordComponent.getMethod(name);
        } catch (final ReflectiveOperationException xnotfound) {
            return null;
        }
    }

    /**
     * Whether the given class is a record on this runtime.
     *
     * @param clazz the class to check
     * @return true if clazz is a record, false if it is not or if records are unsupported here
     */
    private static boolean isRecord(final Class<?> clazz) {
        if (IS_RECORD == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(IS_RECORD.invoke(clazz));
        } catch (final ReflectiveOperationException xfail) {
            return false;
        }
    }

    /**
     * Whether the given class declares a record component named {@code property}.
     * <p>Used only to confirm {@code property} is a genuine record component before the accessor is
     * resolved through the (permission-checked) {@link Introspector}; the accessor method instances
     * gathered here are discarded.</p>
     *
     * @param clazz the record class
     * @param property the property name to match against the record's components
     * @return true if clazz declares a record component named property
     */
    private static boolean hasComponent(final Class<?> clazz, final String property) {
        if (GET_RECORD_COMPONENTS == null || COMPONENT_GET_NAME == null) {
            return false;
        }
        try {
            final Object[] components = (Object[]) GET_RECORD_COMPONENTS.invoke(clazz);
            for (final Object component : components) {
                if (property.equals(COMPONENT_GET_NAME.invoke(component))) {
                    return true;
                }
            }
        } catch (final ReflectiveOperationException xfail) {
            return false;
        }
        return false;
    }

    /**
     * Discovers a RecordGetExecutor.
     * <p>The class must be a record and declare a component named {@code property}; the accessor
     * method itself is resolved through {@code is} so it goes through the same permission checks as
     * every other property accessor.</p>
     *
     * @param is the introspector
     * @param clazz the class to find the accessor method from
     * @param property the property (record component) name to find
     * @return the executor if found, null otherwise
     */
    public static RecordGetExecutor discover(final Introspector is, final Class<?> clazz, final String property) {
        if (property == null || property.isEmpty() || !isRecord(clazz) || !hasComponent(clazz, property)) {
            return null;
        }
        final java.lang.reflect.Method method = is.getMethod(clazz, property, EMPTY_PARAMS);
        return method == null ? null : new RecordGetExecutor(clazz, method, property);
    }

    /** The property (record component name). */
    private final String property;

    /**
     * Creates an instance.
     *
     * @param clazz the class the accessor applies to
     * @param method the accessor method held by this executor
     * @param identifier the property to get
     */
    private RecordGetExecutor(final Class<?> clazz, final java.lang.reflect.Method method, final String identifier) {
        super(clazz, method);
        property = identifier;
    }

    @Override
    public Object getTargetProperty() {
        return property;
    }

    @Override
    public Object invoke(final Object o) throws IllegalAccessException, InvocationTargetException {
        return method == null ? null : method.invoke(o, (Object[]) null);
    }

    @Override
    public Object tryInvoke(final Object o, final Object identifier) {
        if (o != null && method != null
            && property.equals(castString(identifier))
            && objectClass.equals(o.getClass())) {
            try {
                return method.invoke(o, (Object[]) null);
            } catch (IllegalAccessException | IllegalArgumentException xill) {
                return TRY_FAILED; // fail
            } catch (final InvocationTargetException xinvoke) {
                throw JexlException.tryFailed(xinvoke); // throw
            }
        }
        return TRY_FAILED;
    }
}
