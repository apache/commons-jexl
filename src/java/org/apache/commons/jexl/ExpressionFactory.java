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
import org.apache.commons.jexl.parser.ASTForeachStatement;
import org.apache.commons.jexl.parser.ASTIfStatement;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.ASTStatementExpression;
import org.apache.commons.jexl.parser.ASTWhileStatement;
import org.apache.commons.jexl.parser.Parser;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * Creates Expression objects.  To create a JEXL Expression object, pass
 * valid JEXL syntax to the static createExpression() method:
 * </p>
 *
 * <pre>
 * String jexl = "array[1]";
 * Expression expression = ExpressionFactory.createExpression( jexl );
 * </pre>
 *
 * <p>
 * When an {@link Expression} object is created, the JEXL syntax is
 * parsed and verified.  If the supplied expression is neither an
 * expression nor a reference, an exception is thrown from createException().
 * </p>
 * @since 1.0
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class ExpressionFactory {
    /**
     * The Log to which all ExpressionFactory messages will be logged.
     */
    protected static Log log =
        LogFactory.getLog("org.apache.commons.jexl.ExpressionFactory");

    /**
     * The singleton ExpressionFactory also holds a single instance of
     * {@link Parser}.
     * When parsing expressions, ExpressionFactory synchronizes on Parser.
     */
    protected static Parser parser =
            new Parser(new StringReader(";")); //$NON-NLS-1$

    /**
     * ExpressionFactory is a singleton and this is the private
     * instance fufilling that pattern.
     */
    protected static ExpressionFactory ef = new ExpressionFactory();

    /**
     * Private constructor, the single instance is always obtained
     * with a call to getInstance().
     */
    private ExpressionFactory() {
    }

    /**
     * Returns the single instance of ExpressionFactory.
     * @return the instance of ExpressionFactory.
     */
    protected static  ExpressionFactory getInstance() {
        return ef;
    }

    /**
     * Creates an Expression from a String containing valid
     * JEXL syntax.  This method parses the expression which
     * must contain either a reference or an expression.
     * @param expression A String containing valid JEXL syntax
     * @return An Expression object which can be evaluated with a JexlContext
     * @throws Exception An exception can be thrown if there is a problem
     *      parsing this expression, or if the expression is neither an
     *      expression or a reference.
     */
    public static Expression createExpression(String expression)
        throws Exception {
        return getInstance().createNewExpression(expression);
    }


    /**
     *  Creates a new Expression based on the expression string.
     *
     *  @param expression valid Jexl expression
     *  @return Expression
     *  @throws Exception for a variety of reasons - mostly malformed
     *          Jexl expression
     */
    protected Expression createNewExpression(final String expression)
        throws Exception {

        String expr = cleanExpression(expression);

        // Parse the Expression
        SimpleNode tree;
        synchronized (parser) {
            log.debug("Parsing expression: " + expr);
            tree = parser.parse(new StringReader(expr));
        }

        if (tree.jjtGetNumChildren() > 1 && log.isWarnEnabled()) {
            log.warn("The JEXL Expression created will be a reference"
                + " to the first expression from the supplied script: \""
                + expression + "\" ");
        }

        // Must be a simple reference, expression, statement or if, otherwise
        // throw an exception.
        SimpleNode node = (SimpleNode) tree.jjtGetChild(0);

        // TODO: Can we get rid of these checks?
        if (node instanceof ASTReferenceExpression
            || node instanceof ASTExpressionExpression
            || node instanceof ASTStatementExpression
            || node instanceof ASTIfStatement
            || node instanceof ASTWhileStatement
            || node instanceof ASTForeachStatement
            ) {
            return new ExpressionImpl(expression, node);
        }
        log.error("Invalid Expression, node of type: "
            + node.getClass().getName());
        throw new Exception("Invalid Expression: not a Reference, Expression, "
            + "Statement or If");
    }

    /**
     * Trims the expression and adds a semi-colon if missing.
     * @param expression to clean
     * @return trimmed expression ending in a semi-colon
     */
    private String cleanExpression(String expression) {
        String expr = expression.trim();
        if (!expr.endsWith(";")) {
            expr += ";";
        }
        return expr;
    }
}
