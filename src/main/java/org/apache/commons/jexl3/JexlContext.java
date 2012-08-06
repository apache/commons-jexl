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

/**
 * Manages variables which can be referenced in a JEXL expression.
 * <p>
 * Jexl variable names in their simplest form are 'java-like' identifiers.
 * Jexl also considers 'ant' inspired variables expressions as valid.
 * For instance, the expression 'x.y.z' is an 'antish' variable and will be resolved as a whole by the context,
 * i.e. using the key "x.y.z". This proves to be useful to solve "fully qualified class names".
 * <p>
 * </p>
 * The interpreter variable resolution algorithm will try the different sequences of identifiers till it finds
 * one that exists in the context; if "x" is an object known in the context (JexlContext.has("x") returns true),
 * "x.y" will <em>not</em> be looked up in the context but will most likely refer to "x.getY()".
 * </p>
 * <p>
 * Note that JEXL may use '$jexl' and '$ujexl' variables for internal purpose; setting or getting those
 * variables may lead to unexpected results unless specified otherwise.
 * </p>
 *  @since 1.0
 */
public interface JexlContext {
    /**
     * Gets the value of a variable.
     * @param name the variable's name
     * @return the value
     */
    Object get(String name);

    /**
     * Sets the value of a variable.
     * @param name the variable's name
     * @param value the variable's value
     */
    void set(String name, Object value);

    /**
     * Checks whether a variable is defined in this context.
     * <p>A variable may be defined with a null value; this method checks whether the
     * value is null or if the variable is undefined.</p>
     * @param name the variable's name
     * @return true if it exists, false otherwise
     */
    boolean has(String name);

    /**
     *
     * This interface declares how to resolve a namespace from its name; it is used by the interpreter during
     * evalutation.
     * <p>
     * In JEXL, a namespace is an object that serves the purpose of encapsulating functions; for instance,
     * the "math" namespace would be the proper object to expose functions like "log(...)", "sinus(...)", etc.
     * </p>
     * In expressions like "ns:function(...)", the resolver is called with resolveNamespace("ns").
     * <p>
     * JEXL itself reserves 'jexl' and 'ujexl' as namespaces for internal purpose; resolving those may lead to
     * unexpected results.
     * </p>
     * @since 3.0
     */
    public interface NamespaceResolver {
        /**
         * Resolves a namespace by its name.
         * @param name the name
         * @return the namespace object
         */
        Object resolveNamespace(String name);
    }
}
