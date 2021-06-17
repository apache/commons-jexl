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
import org.apache.commons.jexl3.introspection.JexlMethod;


/**
 * Wraps a reference or optional method executor.
 */
public class ReferenceMethodExecutor implements JexlMethod {
    /** The reference handler. */
    private final ReferenceUberspect.ReferenceHandler handler;
    /** The method to delegate to. */
    private final JexlMethod method;

    /**
     * Creates an instance.
     * @param referenceHandler the reference handler
     * @param jexlMethod the method executor
     */
    public ReferenceMethodExecutor(ReferenceUberspect.ReferenceHandler referenceHandler, JexlMethod jexlMethod) {
        if (referenceHandler == null || jexlMethod == null) {
            throw new IllegalArgumentException("handler and method cant be null");
        }
        this.method = jexlMethod;
        this.handler = referenceHandler;
    }

    /**
     * Dereference an expected optional or reference .
     * @param opt the reference
     * @return the reference value
     */
    protected Object getReference(Object opt) {
        return handler.callGet(opt);
    }

    @Override
    public Object invoke(Object ref, Object... args) throws Exception {
        Object obj = getReference(ref);
        return obj == null? null : method.invoke(obj, args);
    }

    @Override
    public Object tryInvoke(String name, Object ref, Object... args) throws JexlException.TryFailed {
        Object obj = getReference(ref);
        if (method == null) {
            return obj == null? null : JexlEngine.TRY_FAILED;
        }
        if (obj == ref) {
            return JexlEngine.TRY_FAILED;
        }
        return method.tryInvoke(name, obj, args);
    }

    @Override
    public boolean tryFailed(Object rval) {
        return method == null || method.tryFailed(rval);
    }

    @Override
    public boolean isCacheable() {
        return method != null && method.isCacheable();
    }

    @Override
    public Class<?> getReturnType() {
        return method != null?  method.getReturnType() : null;
    }
}
