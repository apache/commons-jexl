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

package org.apache.commons.jexl;


/**
 * Represents a single JEXL expression.
 * <p>
 * This simple interface provides access to the underlying expression through
 * {@link Epxression#getExpression()}.
 * </p>
 *
 * <p>
 * An expression is different than a script - it is simply a reference of
 * an expression.
 * </p>
 *
 * @since 1.0
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public interface Expression {
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
     * Returns the JEXL expression this Expression was created with.
     *
     * @return The JEXL expression to be evaluated
     */
    String getExpression();

    /**
     * Returns the JEXL expression by reconstructing it from the parsed tree.
     * @return the JEXL expression
     */
    String dump();
}
