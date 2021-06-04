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
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import java.util.Iterator;
import java.util.List;

/**
 * An uberspect that controls usage of properties, methods and constructors through a sandbox.
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
    public SandboxUberspect(final JexlUberspect theUberspect, final JexlSandbox theSandbox) {
        if (theSandbox == null) {
            throw new NullPointerException("sandbox can not be null");
        }
        if (theUberspect == null) {
            throw new NullPointerException("uberspect can not be null");
        }
        this.uberspect = theUberspect;
        this.sandbox = theSandbox.copy();
    }

    @Override
    public void setClassLoader(final ClassLoader loader) {
        uberspect.setClassLoader(loader);
    }

    @Override
    public ClassLoader getClassLoader() {
        return uberspect.getClassLoader();
    }

    @Override
    public int getVersion() {
        return uberspect.getVersion();
    }

    @Override
    public JexlMethod getConstructor(final Object ctorHandle, final Object... args) {
        final String className;
        if (ctorHandle instanceof Class<?>) {
            className = sandbox.execute((Class<?>) ctorHandle, "");
        } else if (ctorHandle != null) {
            className = sandbox.execute(ctorHandle.toString(), "");
        } else {
            className = null;
        }
        return className != null && className != JexlSandbox.NULL ? uberspect.getConstructor(className, args) : null;
    }

    @Override
    public JexlMethod getMethod(final Object obj, final String method, final Object... args) {
        if (obj != null && method != null) {
            final Class<?> clazz = (obj instanceof Class) ? (Class<?>) obj : obj.getClass();
            final String actual = sandbox.execute(clazz, method);
            if (actual != null && actual != JexlSandbox.NULL) {
                return uberspect.getMethod(obj, actual, args);
            }
        }
        return null;
    }

    @Override
    public List<PropertyResolver> getResolvers(final JexlOperator op, final Object obj) {
        return uberspect.getResolvers(op, obj);
    }

    @Override
    public JexlPropertyGet getPropertyGet(final Object obj, final Object identifier) {
        return getPropertyGet(null, obj, identifier);
    }

    @Override
    public JexlPropertyGet getPropertyGet(final List<PropertyResolver> resolvers,
                                          final Object obj,
                                          final Object identifier) {
        if (obj != null) {
            if (identifier != null) {
                final String property = identifier.toString();
                final String actual = sandbox.read(obj.getClass(), property);
                if (actual != null) {
                    // no transformation, strict equality: use identifier before string conversion
                    final Object pty = actual == property ? identifier : actual;
                    return uberspect.getPropertyGet(resolvers, obj, pty);
                }
            } else {
                final String actual = sandbox.read(obj.getClass(), null);
                if (actual != JexlSandbox.NULL) {
                     return uberspect.getPropertyGet(resolvers, obj, null);
                }
            }
        }
        return null;
    }

    @Override
    public JexlPropertySet getPropertySet(final Object obj,final Object identifier,final Object arg) {
        return getPropertySet(null, obj, identifier, arg);
    }

    @Override
    public JexlPropertySet getPropertySet(final List<PropertyResolver> resolvers,
                                          final Object obj,
                                          final Object identifier,
                                          final Object arg) {
        if (obj != null) {
            if (identifier != null) {
                final String property = identifier.toString();
                final String actual = sandbox.write(obj.getClass(), property);
                if (actual != null) {
                    // no transformation, strict equality: use identifier before string conversion
                    final Object pty = actual == property ? identifier : actual;
                    return uberspect.getPropertySet(resolvers, obj, pty, arg);
                }
            } else {
                final String actual = sandbox.write(obj.getClass(), null);
                if (actual != JexlSandbox.NULL) {
                    return uberspect.getPropertySet(resolvers, obj, null, arg);
                }
            }
        }
        return null;
    }

    @Override
    public Iterator<?> getIterator(final Object obj) {
        return uberspect.getIterator(obj);
    }

    @Override
    public JexlArithmetic.Uberspect getArithmetic(final JexlArithmetic arithmetic) {
        return uberspect.getArithmetic(arithmetic);
    }
}
