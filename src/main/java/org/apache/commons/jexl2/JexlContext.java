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

import java.util.HashMap;
import java.util.Map;

/**
 * Manages variables which can be referenced in a JEXL expression.
 *
 *  @since 1.0
 *  @version $Id$
 */
public interface JexlContext {
    /**
     * Gets the value of a variable.
     * @param name the variable's name
     * @return the value
     */
    Object getJexlVariable(String name);

    /**
     * Sets the value of a variable.
     * @param name the variable's name
     * @param value the variable's value
     */
    void setJexlVariable(String name, Object value);

    /**
     * A context that differentiates null valued variables and undefined ones.
     * <p>A non-nullable context does not allow differentiating a variable whose
     * value is null and an undefined one; thus the Nullable name for this kind of context.</p>
     */
    public interface Nullable extends JexlContext {
        /**
         * Checks whether a variable is defined in this context.
         * <p>A variable may be defined with a null value; this method checks whether the
         * value is null or if the variable is undefined.</p>
         * @param name the variable's name
         * @return true if it exists, false otherwise
         */
        boolean definesJexlVariable(String name);
    }

    /**
     * Wraps a map in a context.
     * <p>Each entry in the map is considered a variable name, value pair.</p>
     */
    public static class Mapped implements Nullable {
        /**
         * The wrapped variable map.
         */
        protected final Map<Object,Object> map;
        /**
         * Creates an instance using an HashMap as the underlying variable storage.
         */
        public Mapped() {
            this(null);
        }
        /**
         * Creates an instance using a provided map as the underlying variable storage.
         * @param vars the variables map
         */
        @SuppressWarnings("unchecked") // OK to cast Map<?,?> to Map<Object,Object>
        public Mapped(Map<?, ?> vars) {
            map = (Map<Object,Object>) (vars == null? new HashMap<String,Object>() : vars);
        }

        /** {@inheritDoc} */
        public boolean definesJexlVariable(String name) {
            return map.containsKey(name);
        }

        /** {@inheritDoc} */
        public Object getJexlVariable(String name) {
            return map.get(name);
        }

        /** {@inheritDoc} */
        public void setJexlVariable(String name, Object value) {
            map.put(name, value);
        }
    }

}
