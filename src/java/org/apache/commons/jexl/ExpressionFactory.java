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
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", "Jexl" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
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

import java.io.StringReader;

import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.Parser;
import org.apache.commons.jexl.parser.SimpleNode;

/**
 *  Used to create Expression objects
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ExpressionFactory.java,v 1.2 2002/12/16 10:41:59 jstrachan Exp $
 */
public class ExpressionFactory
{
    /**
     *  our parser - we share it
     */
    protected static Parser parser = new Parser(new StringReader(";"));

    /**
     *  We Be Singleton
     */
    protected static ExpressionFactory ef = new ExpressionFactory();

    private ExpressionFactory()
    {
    }

    public static Expression createExpression(String expression)
        throws Exception
    {
        return getInstance().createNewExpression(expression);
    }

    protected static  ExpressionFactory getInstance()
    {
        return ef;
    }

    /**
     *  Creates a new Expression based on the expression string.
     *
     *  @param expresison valid Jexl expression
     *  @return Expression
     *  @throws Exception for a variety of reasons - mostly malformed
     *          Jexl expression
     */
    protected Expression createNewExpression(String expression)
        throws Exception
    {
        String expr = expression.trim();

        /*
         * make sure a valid statement
         */
        if (!expr.endsWith(";"))
        {
            expr = expr + ";";
        }

        /*
         *  now parse - we want to protect the parser for now
         */
        SimpleNode tree;

        synchronized(parser)
        {
            tree = parser.parse(new StringReader(expr));
        }

        /*
         *  we expect that this is a simple Reference Expression, or
         *  one can be dug out...
         *
         *  if not, chuck an exception
         */

        SimpleNode node = (SimpleNode) tree.jjtGetChild(0);

        if (node instanceof ASTReferenceExpression)
        {
            Expression e = new ExpressionImpl(expression,
                    (SimpleNode) node.jjtGetChild(0));

            return e;
        }
        else if (node instanceof ASTExpressionExpression)
        {
            Expression e = new ExpressionImpl(expression,
                        (SimpleNode) node.jjtGetChild(0));

            return e;
        }

        throw new Exception("Invalid expression");
    }
}
