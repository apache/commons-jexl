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
package org.apache.commons.jexl3.jexl342;

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.introspection.JexlPropertySet;


/**
 * Wraps a reference or optional property set executor.
 */
public class ReferenceSetExecutor implements JexlPropertySet {
    /** The reference handler. */
    private final ReferenceUberspect.ReferenceHandler handler;
    /** The previous setter we did delegate to. */
    private final JexlPropertySet setter;

    /**
     * Creates an instance.
     * @param referenceHandler the reference handler
     * @param jexlSet the property setter
     */
    public ReferenceSetExecutor(ReferenceUberspect.ReferenceHandler referenceHandler, JexlPropertySet jexlSet) {
        if (referenceHandler == null || jexlSet == null) {
            throw new IllegalArgumentException("handler and setter cant be null");
        }
        this.handler = referenceHandler;
        this.setter = jexlSet;
    }

    /**
     * Dereference an expected optional or reference .
     * @param opt the reference
     * @return the reference value, TRY_FAILED if null
     */
    protected Object getReference(Object opt) {
        return handler.callGet(opt);
    }

    @Override
    public Object invoke(final Object opt, final Object arg) throws Exception {
        Object obj = getReference(opt);
        return setter.invoke(obj, arg);
    }

    @Override
    public Object tryInvoke(final Object opt, final Object key, final Object arg) throws JexlException.TryFailed {
        Object obj = getReference(opt);
        return obj == opt? JexlEngine.TRY_FAILED : obj == null? null : setter.tryInvoke(key, obj, arg);
    }

    @Override
    public boolean tryFailed(Object rval) {
        return setter.tryFailed(rval);
    }

    @Override
    public boolean isCacheable() {
        return setter.isCacheable();
    }
}
