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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A JEXL Script.
 *
 * <p>A script is some valid JEXL syntax to be executed with a given set of {@link JexlContext} variables.</p>
 *
 * <p>A script is a group of statements, separated by semicolons.</p>
 *
 * <p>The statements can be <code>blocks</code> (curly braces containing code),
 * Control statements such as <code>if</code> and <code>while</code>
 * as well as expressions and assignment statements.</p>
 *
 * <p>Do <em>not</em> create classes that implement this interface; delegate or compose instead.</p>
 *
 * @since 1.1
 */
public interface JexlScript {

     /**
     * Returns the source text of this expression.
      *
     * @return the source text
     */
    String getSourceText();

    /**
     * Recreates the source text of this expression from the internal syntactic tree.
     *
     * @return the source text
     */
    String getParsedText();

    /**
     * Recreates the source text of this expression from the internal syntactic tree.
     *
     * @param indent the number of spaces for indentation, 0 meaning no indentation
     * @return the source text
     */
    String getParsedText(int indent);

    /**
     * Executes the script with the variables contained in the
     * supplied {@link JexlContext}.
     *
     * @param context A JexlContext containing variables.
     * @return The result of this script, usually the result of
     *         the last statement.
     */
    Object execute(JexlContext context);

    /**
     * Executes the script with the variables contained in the
     * supplied {@link JexlContext} and a set of arguments corresponding to the
     * parameters used during parsing.
     *
     * @param context A JexlContext containing variables.
     * @param args the arguments
     * @return The result of this script, usually the result of
     *         the last statement.
     * @since 2.1
     */
    Object execute(JexlContext context, Object... args);

    /**
     * Gets this script parameters.
     *
     * @return the parameters or null
     * @since 2.1
     */
    String[] getParameters();

    /**
     * Returns true if script supports taking variable number of arguments.
     *
     * @return boolean
     * @since 3.2
     */
    boolean isVarArgs();

    /**
     * Gets this script unbound parameters.
     * <p>Parameters that haven't been bound by a previous call to curry().</p>
     * @return the parameters or null
     * @since 3.2
     */
    String[] getUnboundParameters();

    /**
     * Gets this script local variables.
     *
     * @return the local variables or null
     * @since 2.1
     */
    String[] getLocalVariables();

    /**
     * Gets this script variables.
     * <p>Note that since variables can be in an ant-ish form (ie foo.bar.quux), each variable is returned as
     * a list of strings where each entry is a fragment of the variable ({"foo", "bar", "quux"} in the example.</p>
     *
     * @return the variables or null
     * @since 2.1
     */
    Set<List<String>> getVariables();

    /**
     * Gets this script pragmas.
     *
     * @return the (non null, may be empty) pragmas map
     */
    Map<String, Object> getPragmas();

    /**
     * Creates a Callable from this script.
     *
     * <p>This allows to submit it to an executor pool and provides support for asynchronous calls.</p>
     * <p>The interpreter will handle interruption/cancellation gracefully if needed.</p>
     *
     * @param context the context
     * @return the callable
     * @since 2.1
     */
    Callable<Object> callable(JexlContext context);

    /**
     * Creates a Callable from this script.
     *
     * <p>This allows to submit it to an executor pool and provides support for asynchronous calls.</p>
     * <p>The interpreter will handle interruption/cancellation gracefully if needed.</p>
     *
     * @param context the context
     * @param args the script arguments
     * @return the callable
     * @since 2.1
     */
    Callable<Object> callable(JexlContext context, Object... args);

    /**
     * Curries this script, returning a script with bound arguments.
     *
     * <p>If this script does not declare parameters or if all of them are already bound,
     * no error is generated and this script is returned.</p>
     *
     * @param args the arguments to bind
     * @return the curried script or this script if no binding can occur
     */
    JexlScript curry(Object... args);
}
