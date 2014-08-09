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

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import java.util.Iterator;

/**
 * An uberspect that controls usage of properties, methods and contructors through a sandbox.
 * @since 3.0
 */
public final class SandboxUberspect implements JexlUberspect {
    /** The base uberspect. */
    private final JexlUberspect uberspect;
    /**  The sandbox. */
    private final JexlSandbox sandbox;

    /**
     * A constructor for JexlSandbox uberspect.
     * @param theUberspect the JexlUberspect to sandbox
     * @param theSandbox the sandbox which is copied to avoid changes at runtime
     */
    public SandboxUberspect(JexlUberspect theUberspect, JexlSandbox theSandbox) {
        if (theSandbox == null) {
            throw new NullPointerException("sandbox can not be null");
        }
        if (theUberspect == null) {
            throw new NullPointerException("uberspect can not be null");
        }
        this.uberspect = theUberspect;
        this.sandbox = theSandbox.copy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setClassLoader(ClassLoader loader) {
        uberspect.setClassLoader(loader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getVersion() {
        return uberspect.getVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JexlMethod getConstructor(Object ctorHandle, Object... args) {
        final String className;
        if (ctorHandle instanceof Class<?>) {
            Class<?> clazz = (Class<?>) ctorHandle;
            className = clazz.getName();
        } else if (ctorHandle != null) {
            className = ctorHandle.toString();
        } else {
            return null;
        }
        if (sandbox.execute(className, "") != null) {
            return uberspect.getConstructor(className, args);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JexlMethod getMethod(Object obj, String method, Object... args) {
        if (obj != null && method != null) {
            String objClassName = (obj instanceof Class) ? ((Class<?>)obj).getName() : obj.getClass().getName();
            String actual = sandbox.execute(objClassName, method);
            if (actual != null) {
                return uberspect.getMethod(obj, actual, args);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier) {
        if (obj != null && identifier != null) {
            String actual = sandbox.read(obj.getClass().getName(), identifier.toString());
            if (actual != null) {
                return uberspect.getPropertyGet(obj, actual);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, Object arg) {
        if (obj != null && identifier != null) {
            String actual = sandbox.write(obj.getClass().getName(), identifier.toString());
            if (actual != null) {
                return uberspect.getPropertySet(obj, actual, arg);
            }
        }
        return null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<?> getIterator(Object obj) {
        return uberspect.getIterator(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JexlArithmetic.Uberspect getArithmetic(JexlArithmetic arithmetic) {
        return uberspect.getArithmetic(arithmetic);
    }
}
