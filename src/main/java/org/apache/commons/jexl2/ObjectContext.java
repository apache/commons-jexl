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

package org.apache.commons.jexl2;

/**
 * Wraps an Object as a Jexl context.
 * @param <T> the object type to use
 */
public class ObjectContext<T> implements JexlContext {
    private final JexlEngine jexl;
    private final T object;

    /**
     * Creates a new ObjectContext.
     * @param jexl the jexl engine to use to solve properties
     * @param object the object to wrap in this context
     */
    public ObjectContext(JexlEngine jexl, T object) {
        this.jexl = jexl;
        this.object = object;
    }

    /** {@inheritDoc} */
    public Object get(String name) {
        return jexl.getProperty(object, name);
    }
    /** {@inheritDoc} */
    public void set(String name, Object value) {
        jexl.setProperty(object, name, value);
    }
    /** {@inheritDoc} */
    public boolean has(String name) {
        return jexl.getUberspect().getPropertyGet(object, name, null) != null;
    }
}
