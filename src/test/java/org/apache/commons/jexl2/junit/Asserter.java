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

package org.apache.commons.jexl2.junit;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;

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
    private final Map<String, Object> variables = new HashMap<String, Object>();
    /** context to use during asserts. */
    private final JexlContext context = new MapContext(variables);

    /** Jexl engine to use during Asserts. */
    private final JexlEngine engine;

    /**
     * 
     * Create an asserter.
     * @param jexl the JEXL engine to use
     */
    public Asserter(JexlEngine jexl) {
        engine = jexl;
    }

    /**
     * Retrieves the underlying JEXL engine.
     * @return the JEXL engine
     */
    public JexlEngine getEngine() {
        return engine;
    }

    /**
     * Retrieves the underlying JEXL context.
     * @return the JEXL context
     */
    public JexlContext getContext() {
        return context;
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
        Expression exp = engine.createExpression(expression);
        Object value = exp.evaluate(context);

        assertEquals("expression: " + expression, expected, value);
    }

    /**
     * Performs an assertion that the expression fails throwing an exception.
     * If matchException is not null, the exception message is expected to match it as a regexp.
     * The engine is temporarily switched to strict * verbose to maximize error detection abilities.
     * @param expression the expression that should fail
     * @param matchException the exception message pattern
     * @throws Exception if the expression did not fail or the exception did not match the expected pattern
     */
    public void failExpression(String expression, String matchException) throws Exception {
        boolean[] flags = { engine.isLenient(), engine.isSilent() };
        try {
            engine.setLenient(false);
            engine.setSilent(false);
            Expression exp = engine.createExpression(expression);
            Object value = exp.evaluate(context);
            fail("expression: " + expression);
        } catch(JexlException xjexl) {
            if (matchException != null && !xjexl.getMessage().matches(matchException)) {
                fail("expression: " + expression + ", expected: " + matchException + ", got " + xjexl.getMessage());
            }
        } finally {
            engine.setLenient(flags[0]);
            engine.setSilent(flags[1]);
        }
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

    /**
     * Removes a variable of a certain name from the context.
     * @param name variable name
     * @return variable value
     */
    public Object removeVariable(String name) {
        return variables.remove(name);
    }

}
