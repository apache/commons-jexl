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
 * Tests for the bitwise operators.
 * @author Dion Gillard
 * @since 1.0.1
 */
public class BitwiseOperatorTest extends TestCase {

    /**
     * Create the named test
     * @param name test name
     */
    public BitwiseOperatorTest(String name) {
        super(name);
    }
    
    public void testAndWithTwoNulls() throws Exception {
        Expression e = ExpressionFactory.createExpression("null & null");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(0), o);
    }

    public void testAndWithLeftNull() throws Exception {
        Expression e = ExpressionFactory.createExpression("null & 1");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(0), o);
    }

    public void testAndWithRightNull() throws Exception {
        Expression e = ExpressionFactory.createExpression("1 & null");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(0), o);
    }

    public void testAndSimple() throws Exception {
        Expression e = ExpressionFactory.createExpression("15 & 3");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(3), o);
    }

    public void testAndVariableNumberCoercion() throws Exception {
        Expression e = ExpressionFactory.createExpression("x & y");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("x", new Integer(15));
        jc.getVars().put("y", new Short((short)7));
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(7), o);
    }

    public void testAndVariableStringCoercion() throws Exception {
        Expression e = ExpressionFactory.createExpression("x & y");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("x", new Integer(15));
        jc.getVars().put("y", "7");
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(7), o);
    }
}
