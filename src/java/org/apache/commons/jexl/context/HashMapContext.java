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

package org.apache.commons.jexl.context;

import org.apache.commons.jexl.JexlContext;

import java.util.HashMap;
import java.util.Map;

/**
 *  Implementation of JexlContext based on a HashMap.
 *
 *  @since 1.0
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id$
 */
public class HashMapContext extends HashMap implements JexlContext {
    /** serialization version id jdk13 generated. */
    static final long serialVersionUID = 5715964743204418854L;
    /**
     * {@inheritDoc}
     */
    public void setVars(Map vars) {
        clear();
        putAll(vars);
    }

    /**
     * {@inheritDoc}
     */
    public Map getVars() {
        return this;
    }
}
