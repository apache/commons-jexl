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
 * <p>Record detection and lookup are delegated to {@link ClassTool}, which resolves the relevant
 * methods through reflection so this module remains usable on the Java 8 baseline this project still
 * targets; on such a runtime, {@code Class#isRecord()} and {@code Class#getRecordComponents()} simply
 * do not exist and discovery quietly reports no match instead of failing to link.</p>
 *
 * @since 3.7.2
 */
public final class RecordGetExecutor extends AbstractExecutor.Get {

    /** A static signature for method(). */
    private static final Object[] EMPTY_PARAMS = {};

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
        if (property == null || property.isEmpty() || !ClassTool.isRecord(clazz)
            || !ClassTool.hasRecordComponent(clazz, property)) {
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
