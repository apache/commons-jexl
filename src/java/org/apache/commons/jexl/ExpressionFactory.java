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

import java.io.StringReader;

import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.Parser;
import org.apache.commons.jexl.parser.SimpleNode;

/**
 *  Used to create Expression objects
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ExpressionFactory.java,v 1.4 2004/02/28 13:45:20 yoavs Exp $
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
