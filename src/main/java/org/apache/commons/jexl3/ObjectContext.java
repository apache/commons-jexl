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
package org.apache.commons.jexl3;

import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;

/**
 * Wraps an Object as a JEXL context and NamespaceResolver.
 *
 * @param <T> the wrapped object type to use
 * @since 3.0
 */
public class ObjectContext<T> implements JexlContext, JexlContext.NamespaceResolver {

    /** The property solving jexl engine. */
    private final JexlEngine jexl;

    /** The object serving as context provider. */
    private final T object;

    /**
     * @return the Jexl engine
     */
    protected JexlEngine getJexl() {
        return jexl;
    }

    /**
     * @return the object exposed by this context
     */
    protected T getObject() {
        return object;
    }

    /**
     * Creates a new ObjectContext.
     *
     * @param engine  the jexl engine to use to solve properties
     * @param wrapped the object to wrap in this context
     */
    public ObjectContext(JexlEngine engine, T wrapped) {
        this.jexl = engine;
        this.object = wrapped;
    }

    @Override
    public Object get(String name) {
        JexlPropertyGet jget = jexl.getUberspect().getPropertyGet(object, name);
        if (jget != null) {
            try {
                return jget.invoke(object);
            } catch (Exception xany) {
                if (jexl.isStrict()) {
                    throw new JexlException.Property(null, name, true, xany);
                }
            }
        }
        return null;
    }

    @Override
    public void set(String name, Object value) {
        JexlPropertySet jset = jexl.getUberspect().getPropertySet(object, name, value);
        if (jset != null) {
            try {
                jset.invoke(object, value);
            } catch (Exception xany) {
                // ignore
                if (jexl.isStrict()) {
                    throw new JexlException.Property(null, name, true, xany);
                }
            }
        }
    }

    @Override
    public boolean has(String name) {
        JexlPropertyGet jget = jexl.getUberspect().getPropertyGet(object, name);
        try {
            return jget != null && jget.invoke(object) != null;
        } catch (Exception xany) {
            return false;
        }
    }

    @Override
    public Object resolveNamespace(String name) {
        if (name == null || name.isEmpty()) {
            return object;
        } else {
            return null;
        }
    }
}
