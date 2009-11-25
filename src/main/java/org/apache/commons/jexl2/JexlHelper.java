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

import org.apache.commons.jexl2.context.HashMapContext;

/**
 *  Helper to create a context.  In the current implementation of JEXL, there
 *  is one implementation of JexlContext - {@link HashMapContext}, and there
 *  is no reason not to directly instantiate {@link HashMapContext} in your
 *  own application.
 *
 *  @since 1.0
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id$
 */
public class JexlHelper {
    /** singleton instance. */
    protected static final JexlHelper HELPER = new JexlHelper();

    /** @return the single instance. */
    protected static JexlHelper getInstance() {
        return HELPER;
    }

    /**
     * Returns a new {@link JexlContext}.
     * @return a new JexlContext
     */
    public static JexlContext createContext() {
        return getInstance().newContext();
    }

    /**
     * Creates and returns a new {@link JexlContext}.  
     * The current implementation creates a new instance of 
     * {@link HashMapContext}.
     * @return a new JexlContext
     */
    protected JexlContext newContext() {
        return new HashMapContext();
    }
}
