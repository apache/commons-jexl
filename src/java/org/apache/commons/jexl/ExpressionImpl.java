/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", "Jexl" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.commons.jexl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.jexl.parser.SimpleNode;

/**
 *  Implelmentation of an Expression.  Created by the ExpressionFactory.
 *	
 *  The expression is different than a script - it's really just either
 *  a reference or expression that we can get the value of.
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ExpressionImpl.java,v 1.5 2003/10/09 21:28:55 rdonkin Exp $
 */
class ExpressionImpl implements Expression
{
    List preResolvers;
    List postResolvers;

    /**
     *  Original expression - this is just a 'snippet', not a valid
     *  statement (i.e.  foo.bar() vs foo.bar();
     */
    protected String expression;

    /**
     *  The resulting AST we can call value() on
     */
    protected SimpleNode node;

    /**
     *  do not let this be generally instantiated with a 'new'
     */
    ExpressionImpl(String expr, SimpleNode ref)
    {
        expression = expr;
        node = ref;
    }

    /**
     *  evaluate the expression and return the value
     *
     *  @param context Context containing objects/data used for evaluation
     *  @return value of expression
     */
    public Object evaluate(JexlContext context)
        throws Exception
    {
        Object val = null;

        /*
         * if we have pre resolvers, give them a wack
         */
        if (preResolvers != null)
        {
            val = tryResolver(preResolvers, context);

            if (val != JexlExprResolver.NO_VALUE)
            {
                return val;
            }
        }

        val = node.value(context);

        /*
         * if null, call post resolvers
         */
        if (val == null && postResolvers != null)
        {
            val = tryResolver(postResolvers, context);

            if (val != JexlExprResolver.NO_VALUE)
            {
                return val;
            }
        }

        return val;
    }

    /**
     *  Tries the resolvers in the given resolverlist against the context
     *
     *  @param resolverList list of JexlExprResolvers
     *  @param context JexlContext to use for evauluation
     *  @return value (including null) or JexlExprResolver.NO_VALUE
     */
    protected Object tryResolver(List resolverList, JexlContext context)
    {
        Object val = JexlExprResolver.NO_VALUE;
        String expr = getExpression();

        for (int i = 0; i < resolverList.size(); i++)
        {
            JexlExprResolver jer = (JexlExprResolver) resolverList.get(i);

            val = jer.evaluate(context, expr);

            /*
            * as long as it's not NO_VALUE, return it
            */
            if (val != JexlExprResolver.NO_VALUE)
            {
               return val;
            }
        }

        return val;
    }

    /**
     *  returns original expression string
     */
    public String getExpression()
    {
        return expression;
    }

    public void addPreResolver(JexlExprResolver resolver)
    {
        if (preResolvers == null) 
        {
            preResolvers = new ArrayList();
        }
        preResolvers.add(resolver);
    }

    /**
     *  allows addition of a resolver to allow custom interdiction of
     *  expression evaluation
     *
     *  @param resolver resolver to be called if Jexl expression evaluated to null
     */
    public void addPostResolver(JexlExprResolver resolver)
    {
        if (postResolvers == null) 
        {
            postResolvers = new ArrayList();
        }
        postResolvers.add(resolver);
    }

}
