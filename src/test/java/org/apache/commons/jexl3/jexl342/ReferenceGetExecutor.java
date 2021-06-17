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
import org.apache.commons.jexl3.introspection.JexlPropertyGet;


/**
 * Wraps a reference or optional property get executor.
 */
public class ReferenceGetExecutor implements JexlPropertyGet {
    /** The reference handler. */
    private final ReferenceUberspect.ReferenceHandler handler;
    /** The previous getter we did delegate to. */
    private final JexlPropertyGet getter;

    /**
     * Creates an instance.
     * @param referenceHandler the reference handler
     * @param jexlGet the property getter
     */
    public ReferenceGetExecutor(ReferenceUberspect.ReferenceHandler referenceHandler, JexlPropertyGet jexlGet) {
        if (referenceHandler == null || jexlGet == null) {
            throw new IllegalArgumentException("handler and getter cant be null");
        }
        handler = referenceHandler;
        getter = jexlGet;
    }

    /**
     * Dereference an expected optional or reference .
     * @param ref the reference
     * @return the reference value or the reference
     */
    protected Object getReference(Object ref) {
        return handler.callGet(ref);
    }

    @Override
    public Object invoke(final Object ref) throws Exception {
        Object obj = getReference(ref);
        return getter.invoke(obj);
    }

    @Override
    public Object tryInvoke(final Object ref, final Object key) {
        Object obj = getReference(ref);
        return obj == ref ? JexlEngine.TRY_FAILED : getter.tryInvoke(obj, key);
    }

    @Override
    public boolean tryFailed(Object rval) {
        return getter.tryFailed(rval);
    }

    @Override
    public boolean isCacheable() {
        return getter.isCacheable();
    }
}
