/*
 * Copyright 2002,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * <p>
 * Represents a single JEXL expression.  This simple interface
 * provides access to the underlying expression through getExpression(),
 * and it provides hooks to add a pre- and post- expression resolver.
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
     * @throws Exception on any error
     */
    Object evaluate(JexlContext context) throws Exception;

    /**
     * Returns the JEXL expression this Expression was created with.
     *
     * @return The JEXL expression to be evaluated
     */
    String getExpression();

    /**
     * Allows addition of a resolver to allow custom interdiction of
     * expression evaluation.
     *
     * @param resolver resolver to be called before Jexl expression evaluated
     */
    void addPreResolver(JexlExprResolver resolver);

    /**
     * Allows addition of a resolver to allow custom interdiction of
     * expression evaluation.
     *
     * @param resolver resolver to be called if Jexl expression
     *  evaluated to null.
     */
    void addPostResolver(JexlExprResolver resolver);
}
