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

import java.util.ArrayList;
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

    /**
     * do not let this be generally instantiated with a 'new'.
     *
     * @param expr the expression.
     * @param ref the parsed expression.
     */
    ExpressionImpl(String expr, SimpleNode ref) {
        expression = expr;
        node = ref;
    }

    /**
     * Evaluate the expression and return the value.
     *
     * Before JEXL evaluates the expression, any pre-resolvers will be called.
     * If the pre-resolver provides a value, it is returned. If JEXL evaluates
     * the expression as null, post-resolvers are called and any resulting value
     * returned.
     *
     * @param context Context containing objects/data used for evaluation
     * @return value of expression
     * @throws Exception on any error, but typically errors evaluating the
     *             expression.
     */
    public Object evaluate(JexlContext context) throws Exception {
        Object val = null;

        /*
         * if we have pre resolvers, give them a wack
         */
        if (preResolvers != null) {
            val = tryResolver(preResolvers, context);

            if (val != JexlExprResolver.NO_VALUE) {
                return val;
            }
        }

        val = node.value(context);

        /*
         * if null, call post resolvers
         */
        if (val == null && postResolvers != null) {
            val = tryResolver(postResolvers, context);

            if (val != JexlExprResolver.NO_VALUE) {
                return val;
            }
        }

        return val;
    }

    /**
     * Tries the resolvers in the given resolverlist against the context.
     *
     * @param resolverList list of JexlExprResolvers
     * @param context JexlContext to use for evauluation
     * @return value (including null) or JexlExprResolver.NO_VALUE
     */
    protected Object tryResolver(List resolverList, JexlContext context) {
        Object val = JexlExprResolver.NO_VALUE;
        String expr = getExpression();

        for (int i = 0; i < resolverList.size(); i++) {
            JexlExprResolver jer = (JexlExprResolver) resolverList.get(i);

            val = jer.evaluate(context, expr);

            /*
             * as long as it's not NO_VALUE, return it
             */
            if (val != JexlExprResolver.NO_VALUE) {
                return val;
            }
        }

        return val;
    }

    /**
     * Gets the original expression string.
     *
     * @return the expression.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * allows addition of a resolver to allow custom interdiction of expression
     * evaluation.
     *
     * @param resolver resolver to be called before a Jexl expression is evaluated.
     */
    public void addPreResolver(JexlExprResolver resolver) {
        if (preResolvers == null) {
            preResolvers = new ArrayList();
        }
        preResolvers.add(resolver);
    }

    /**
     * allows addition of a resolver to allow custom interdiction of expression
     * evaluation.
     *
     * @param resolver resolver to be called if Jexl expression evaluated to
     *            null
     */
    public void addPostResolver(JexlExprResolver resolver) {
        if (postResolvers == null) {
            postResolvers = new ArrayList();
        }
        postResolvers.add(resolver);
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
