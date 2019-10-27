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

import org.apache.commons.jexl3.annotations.NoJexl;

/**
 * A readonly context wrapper.
 * @since 3.0
 */
public final class ReadonlyContext implements JexlContext, JexlContext.OptionsHandle {
    /** The wrapped context. */
    private final JexlContext wrapped;
    /** The wrapped engine options. */
    private final JexlOptions options;

    /**
     * Creates a new readonly context.
     * @param context the wrapped context
     * @param eopts the engine evaluation options
     */
    public ReadonlyContext(JexlContext context, JexlOptions eopts) {
        wrapped = context;
        options = eopts;
    }

    @Override
    @NoJexl
    public Object get(String name) {
        return wrapped.get(name);
    }

    /**
     * Will throw an UnsupportedOperationException when called; the JexlEngine deals with it appropriately.
     * @param name the unused variable name
     * @param value the unused variable value
     */
    @Override
    @NoJexl
    public void set(String name, Object value) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    @NoJexl
    public boolean has(String name) {
        return wrapped.has(name);
    }

    @Override
    @NoJexl
    public JexlOptions getEngineOptions() {
        return options;
    }
}
