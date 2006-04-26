/*
 * Copyright 2003-2006 The Apache Software Foundation.
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

package org.apache.commons.jexl.junit;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;

/**
 * A utility class for performing JUnit based assertions using Jexl
 * expressions. This class can make it easier to do unit tests using
 * Jexl navigation expressions.
 *
 * @since 1.0
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @version $Revision$
 */
public class Asserter extends Assert {

    /** variables used during asserts. */
    private final Map variables = new HashMap();
    /** context to use during asserts. */
    private final JexlContext context = JexlHelper.createContext();

    /**
     * 
     * Create an asserter.
     */
    public Asserter() {

    }

    /**
     * This constructor will register the given variableValue as the
     * "this" variable.
     * 
     * @param variableValue 'this'.
     */
    public Asserter(Object variableValue) {
        setVariable("this", variableValue);
    }

    /**
     * Performs an assertion that the value of the given Jexl expression 
     * evaluates to the given expected value.
     * 
     * @param expression is the Jexl expression to evaluate
     * @param expected is the expected value of the expression
     * @throws Exception if the expression could not be evaluationed or an assertion
     * fails
     */
    public void assertExpression(String expression, Object expected) throws Exception {
        Expression exp = ExpressionFactory.createExpression(expression);

        context.setVars(variables);
        Object value = exp.evaluate(context);

        assertEquals("expression: " + expression, expected, value);
    }

    /**
     * Puts a variable of a certain name in the context so that it can be used from
     * assertion expressions.
     * 
     * @param name variable name
     * @param value variable value
     */
    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

}
