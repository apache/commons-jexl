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

import org.apache.commons.jexl.parser.ParseException;

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
 * 
 * <p>
 * This is a convenience class; using an instance of a {@link JexlEngine}
 * that serves the same purpose with more control is recommended.
 * </p>
 * @since 1.0
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
@Deprecated
public final class ExpressionFactory {
    /**
     * Private constructor, the single instance is always obtained
     * with a call to getInstance().
     */
    private ExpressionFactory() {}


    /**
     * Creates an Expression from a String containing valid
     * JEXL syntax.  This method parses the expression which
     * must contain either a reference or an expression.
     * @param expression A String containing valid JEXL syntax
     * @return An Expression object which can be evaluated with a JexlContext
     * @throws ParseException An exception can be thrown if there is a problem
     *      parsing this expression, or if the expression is neither an
     *      expression or a reference.
     */
    public static Expression createExpression(String expression)
        throws ParseException {
        return JexlEngine.getDefault().createExpression(expression);
    }

}
