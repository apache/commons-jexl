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

import java.util.concurrent.Callable;

/**
 * Represents a single JEXL expression.
 * <p>
 * This simple interface provides access to the underlying textual expression through
 * {@link JexlExpression#getSourceText()}.
 * </p>
 *
 * <p>
 * An expression is different than a script - it is simply a reference to
 * a single expression, not to multiple statements.
 * This implies 'if','for','while','var' and blocks '{'... '}'are <em>not</em> allowed in expressions.
 * </p>
 * <p>Do <em>not</em> create classes that implement this interface; delegate or compose instead.</p>
 *
 * @since 1.0
 */
public interface JexlExpression {
    /**
     * Evaluates the expression with the variables contained in the
     * supplied {@link JexlContext}.
     *
     * @param context A JexlContext containing variables.
     * @return The result of this evaluation
     * @throws JexlException on any error
     */
    Object evaluate(JexlContext context);

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
     * Creates a Callable from this expression.
     *
     * <p>This allows to submit it to an executor pool and provides support for asynchronous calls.</p>
     * <p>The interpreter will handle interruption/cancellation gracefully if needed.</p>
     *
     * @param context the context
     * @return the callable
     * @since 3.1
     */
    Callable<Object> callable(JexlContext context);
}
