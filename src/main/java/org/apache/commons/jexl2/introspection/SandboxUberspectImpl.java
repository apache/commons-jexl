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

import java.lang.reflect.Constructor;
import org.apache.commons.jexl2.JexlInfo;
import org.apache.commons.logging.Log;

/**
 * An uberspect that controls usage of properties, methods and contructors through a sandbox.
 * @since 2.1
 */
public class SandboxUberspectImpl extends UberspectImpl {
    /**  The sandbox. */
    protected final Sandbox sandbox;

    /**
     * A constructor for Sandbox uberspect.
     * @param runtimeLogger the logger to use or null to use default
     * @param theSandbox the sandbox instance to use
     */
    public SandboxUberspectImpl(Log runtimeLogger, Sandbox theSandbox) {
        super(runtimeLogger);
        if (theSandbox == null) {
            throw new NullPointerException("sandbox can not be null");
        }
        this.sandbox = theSandbox;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLoader(ClassLoader cloader) {
        base().setLoader(cloader);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Constructor<?> getConstructor(Object ctorHandle, Object[] args, JexlInfo info) {
        String className = null;
        Class<?> clazz = null;
        if (ctorHandle instanceof Class<?>) {
            clazz = (Class<?>) ctorHandle;
            className = clazz.getName();
        } else if (ctorHandle != null) {
            className = ctorHandle.toString();
        } else {
            return null;
        }
        Sandbox.Permissions box = sandbox.get(className);
        if (box == null || box.execute().get("") != null) {
            return getConstructor(className, args);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JexlMethod getMethod(Object obj, String method, Object[] args, JexlInfo info) {
        if (obj != null && method != null) {
            Sandbox.Permissions box = sandbox.get(obj.getClass().getName());
            String actual = method;
            if (box != null) {
                actual = box.execute().get(actual);
            }
            if (actual != null) {
                return getMethodExecutor(obj, actual, args);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier, JexlInfo info) {
        if (obj != null && identifier != null) {
            Sandbox.Permissions box = sandbox.get(obj.getClass().getName());
            String actual = identifier.toString();
            if (box != null) {
                actual = box.read().get(actual);
            }
            if (actual != null) {
                return super.getPropertyGet(obj, actual, info);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, Object arg, JexlInfo info) {
        if (obj != null && identifier != null) {
            Sandbox.Permissions box = sandbox.get(obj.getClass().getName());
            String actual = identifier.toString();
            if (box != null) {
                actual = box.write().get(actual);
            }
            if (actual != null) {
                return super.getPropertySet(obj, actual, arg, info);
            }
        }
        return null;

    }
}
