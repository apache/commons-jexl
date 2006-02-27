/*
 * Copyright 2002-2006 The Apache Software Foundation.
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

import junit.framework.TestCase;

/**
 * Test cases for the if statement.
 * 
 * @author Dion Gillard
 */
public class IfTest extends TestCase {

    public IfTest(String testName) {
        super(testName);
    }

    /**
     * Make sure if true executes the true statement
     * @throws Exception on any error
     */
    public void testSimpleIfTrue() throws Exception
    {
        Expression e = ExpressionFactory.createExpression("if (true) 1");
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate(jc);
        assertNotNull("Return value is empty", o);
        assertTrue("Result is not an integer", o instanceof Integer);
        assertEquals("Result is not 1", Integer.valueOf(1), o);
    }

    /**
     * Make sure if false doesn't execute the true statement
     * @throws Exception on any error
     */
    public void testSimpleIfFalse() throws Exception
    {
        Expression e = ExpressionFactory.createExpression("if (false) 1");
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate(jc);
        assertNull("Return value is not empty", o);
    }

    /**
     * Make sure if false executes the false statement
     * @throws Exception on any error
     */
    public void testSimpleElse() throws Exception
    {
        Expression e = ExpressionFactory.createExpression("if (false) 1; else 2;");
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate(jc);
        assertNotNull("Return value is empty", o);
        assertTrue("Result is not an integer", o instanceof Integer);
        assertEquals("Result is not 2", Integer.valueOf(2), o);
    }

    /**
     * Test the if statement handles blocks correctly
     * @throws Exception on any error
     */
    public void testBlockIfTrue() throws Exception
    {
        Expression e = ExpressionFactory.createExpression("if (true) { 'hello'; }");
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate(jc);
        assertNull("Return value is not empty", o);
    }

    /**
     * Test the if statement handles blocks in the else statement correctly
     * @throws Exception on any error
     */
    public void testBlockElse() throws Exception
    {
        Expression e = ExpressionFactory.createExpression("if (false) {1;} else {2;}");
        JexlContext jc = JexlHelper.createContext();

        Object o = e.evaluate(jc);
        assertNull("Return value is not empty", o);
    }
}
