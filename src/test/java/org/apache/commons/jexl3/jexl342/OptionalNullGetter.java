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
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import java.util.Objects;

/**
 * JexlPropertyGet on a reference that pointed null.
 * <p>Performs a late discovery of the actual getter.</p>
 */
public class OptionalNullGetter implements JexlPropertyGet {
    /** The Uberspect to discover the getter. */
    private final JexlUberspect uberspect;
    /** The property we are trying to discover. */
    private final Object property;
    /** The result when we solve it. */
    private JexlPropertyGet delegate;

    OptionalNullGetter(JexlUberspect jexlUberspect, Object key) {
        uberspect = jexlUberspect;
        property = key;
    }

    @Override
    public Object invoke(Object obj) throws Exception {
        if (obj == null) {
            return null;
        }
        if (delegate == null) {
            delegate = uberspect.getPropertyGet(obj, property);
            if (delegate == null) {
                throw new JexlException.Property(null, Objects.toString(property), false, null);
            }
        }
        return delegate.invoke(obj);
    }

    @Override
    public Object tryInvoke(Object obj, Object key) throws JexlException.TryFailed {
        if (obj == null) {
            return null;
        }
        return delegate != null ? delegate.tryInvoke(obj, property) : JexlEngine.TRY_FAILED;
    }

    @Override
    public boolean tryFailed(Object rval) {
        return delegate != null ? delegate.tryFailed(rval) : JexlEngine.TRY_FAILED == rval;
    }

    @Override
    public boolean isCacheable() {
        return false;
    }
}
