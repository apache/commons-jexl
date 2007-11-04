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

import java.util.List;

import org.apache.commons.jexl.parser.SimpleNode;

/**
 * Instances of ExpressionImpl are created by the {@link ExpressionFactory},
 * and this is the default implementation of the {@link Expression} interface.
 *
 * @since 1.0
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
class ExpressionImpl implements Expression {

    // TODO: move resolving to interpreter
    /** resolvers called before expression evaluation. */
    protected List preResolvers;

    /** resolvers called after expression evaluation. */
    protected List postResolvers;

    /**
     * Original expression. This is just a 'snippet', not a valid statement
     * (i.e. foo.bar() vs foo.bar();
     */
    protected String expression;

    /**
     * The resulting AST we can call value() on.
     */
    protected SimpleNode node;

    /** The interpreter of the expression. */
    protected Interpreter interpreter;

    /**
     * do not let this be generally instantiated with a 'new'.
     *
     * @param expr the expression.
     * @param ref the parsed expression.
     * @param interp the interpreter to evaluate the expression
     */
    ExpressionImpl(String expr, SimpleNode ref, Interpreter interp) {
        expression = expr;
        node = ref;
        interpreter = interp;
    }

    /**
     * {@inheritDoc}
     */
    public Object evaluate(JexlContext context) throws Exception {
        return interpreter.interpret(node, context);
    }

    /**
     * {@inheritDoc}
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Provide a string representation of the expression.
     *
     * @return the expression or blank if it's null.
     */
    public String toString() {
        String expr = getExpression();
        return expr == null ? "" : expr;
    }

}
