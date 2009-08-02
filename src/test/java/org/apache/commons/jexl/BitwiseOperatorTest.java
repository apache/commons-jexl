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


/**
 * Tests for the bitwise operators.
 * @author Dion Gillard
 * @since 1.1
 */
public class BitwiseOperatorTest extends JexlTestCase {

    /**
     * Create the named test.
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

    public void testComplementWithNull() throws Exception {
        Expression e = ExpressionFactory.createExpression("~null");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(-1), o);
    }
    
    public void testComplementSimple() throws Exception {
        Expression e = ExpressionFactory.createExpression("~128");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(-129), o);
    }

    public void testComplementVariableNumberCoercion() throws Exception {
        Expression e = ExpressionFactory.createExpression("~x");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("x", new Integer(15));
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(-16), o);
    }

    public void testComplementVariableStringCoercion() throws Exception {
        Expression e = ExpressionFactory.createExpression("~x");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("x", "15");
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(-16), o);
    }

    public void testOrWithTwoNulls() throws Exception {
        Expression e = ExpressionFactory.createExpression("null | null");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(0), o);
    }

    public void testOrWithLeftNull() throws Exception {
        Expression e = ExpressionFactory.createExpression("null | 1");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(1), o);
    }

    public void testOrWithRightNull() throws Exception {
        Expression e = ExpressionFactory.createExpression("1 | null");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(1), o);
    }

    public void testOrSimple() throws Exception {
        Expression e = ExpressionFactory.createExpression("12 | 3");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(15), o);
    }

    public void testOrVariableNumberCoercion() throws Exception {
        Expression e = ExpressionFactory.createExpression("x | y");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("x", new Integer(12));
        jc.getVars().put("y", new Short((short) 3));
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(15), o);
    }

    public void testOrVariableStringCoercion() throws Exception {
        Expression e = ExpressionFactory.createExpression("x | y");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("x", new Integer(12));
        jc.getVars().put("y", "3");
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(15), o);
    }

    public void testXorWithTwoNulls() throws Exception {
        Expression e = ExpressionFactory.createExpression("null ^ null");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(0), o);
    }

    public void testXorWithLeftNull() throws Exception {
        Expression e = ExpressionFactory.createExpression("null ^ 1");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(1), o);
    }

    public void testXorWithRightNull() throws Exception {
        Expression e = ExpressionFactory.createExpression("1 ^ null");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(1), o);
    }

    public void testXorSimple() throws Exception {
        Expression e = ExpressionFactory.createExpression("1 ^ 3");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(2), o);
    }

    public void testXorVariableNumberCoercion() throws Exception {
        Expression e = ExpressionFactory.createExpression("x ^ y");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("x", new Integer(1));
        jc.getVars().put("y", new Short((short) 3));
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(2), o);
    }

    public void testXorVariableStringCoercion() throws Exception {
        Expression e = ExpressionFactory.createExpression("x ^ y");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("x", new Integer(1));
        jc.getVars().put("y", "3");
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Long(2), o);
    }
}
