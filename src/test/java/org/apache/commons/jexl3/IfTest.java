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
package org.apache.commons.jexl3;

import org.apache.commons.jexl3.Expression;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;

/**
 * Test cases for the if statement.
 * 
 * @since 1.1
 */
public class IfTest extends JexlTestCase {
    public IfTest(String testName) {
        super(testName);
    }

    /**
     * Make sure if true executes the true statement
     * 
     * @throws Exception on any error
     */
    public void testSimpleIfTrue() throws Exception {
        Expression e = JEXL.createExpression("if (true) 1");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        assertEquals("Result is not 1", new Integer(1), o);
    }

    /**
     * Make sure if false doesn't execute the true statement
     * 
     * @throws Exception on any error
     */
    public void testSimpleIfFalse() throws Exception {
        Expression e = JEXL.createExpression("if (false) 1");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        assertNull("Return value is not empty", o);
    }

    /**
     * Make sure if false executes the false statement
     * 
     * @throws Exception on any error
     */
    public void testSimpleElse() throws Exception {
        Expression e = JEXL.createExpression("if (false) 1 else 2;");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        assertEquals("Result is not 2", new Integer(2), o);
    }

    /**
     * Test the if statement handles blocks correctly
     * 
     * @throws Exception on any error
     */
    public void testBlockIfTrue() throws Exception {
        Expression e = JEXL.createExpression("if (true) { 'hello'; }");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", "hello", o);
    }

    /**
     * Test the if statement handles blocks in the else statement correctly
     * 
     * @throws Exception on any error
     */
    public void testBlockElse() throws Exception {
        Expression e = JEXL.createExpression("if (false) {1} else {2 ; 3}");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", new Integer(3), o);
    }

    /**
     * Test the if statement evaluates expressions correctly
     * 
     * @throws Exception on any error
     */
    public void testIfWithSimpleExpression() throws Exception {
        Expression e = JEXL.createExpression("if (x == 1) true;");
        JexlContext jc = new MapContext();
        jc.set("x", new Integer(1));

        Object o = e.evaluate(jc);
        assertEquals("Result is not true", Boolean.TRUE, o);
    }

    /**
     * Test the if statement evaluates arithmetic expressions correctly
     * 
     * @throws Exception on any error
     */
    public void testIfWithArithmeticExpression() throws Exception {
        Expression e = JEXL.createExpression("if ((x * 2) + 1 == 5) true;");
        JexlContext jc = new MapContext();
        jc.set("x", new Integer(2));

        Object o = e.evaluate(jc);
        assertEquals("Result is not true", Boolean.TRUE, o);
    }

    /**
     * Test the if statement evaluates decimal arithmetic expressions correctly
     * 
     * @throws Exception on any error
     */
    public void testIfWithDecimalArithmeticExpression() throws Exception {
        Expression e = JEXL.createExpression("if ((x * 2) == 5) true");
        JexlContext jc = new MapContext();
        jc.set("x", new Float(2.5f));

        Object o = e.evaluate(jc);
        assertEquals("Result is not true", Boolean.TRUE, o);
    }

    /**
     * Test the if statement works with assignment
     * 
     * @throws Exception on any error
     */
    public void testIfWithAssignment() throws Exception {
        Expression e = JEXL.createExpression("if ((x * 2) == 5) {y = 1} else {y = 2;}");
        JexlContext jc = new MapContext();
        jc.set("x", new Float(2.5f));

        e.evaluate(jc);
        Object result = jc.get("y");
        assertEquals("y has the wrong value", new Integer(1), result);
    }

    /**
     * Ternary operator condition undefined or null evaluates to false
     * independantly of engine flags.
     * @throws Exception
     */
    public void testTernary() throws Exception {
        JexlEngine jexl = createThreadedArithmeticEngine(true);
        jexl.setCache(64);
        JexlContext jc = new MapContext();
        Expression e = jexl.createExpression("x.y.z = foo ?'bar':'quux'");
        Object o;

        // undefined foo

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", null);

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", Boolean.FALSE);

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", Boolean.TRUE);

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be bar", "bar", o);
            o = jc.get("x.y.z");
            assertEquals("Should be bar", "bar", o);
        }

        debuggerCheck(jexl);
    }

    /**
     * Ternary operator condition undefined or null evaluates to false
     * independantly of engine flags.
     * @throws Exception
     */
    public void testTernaryShorthand() throws Exception {
        JexlEngine jexl = createThreadedArithmeticEngine(true);
        jexl.setCache(64);
        JexlContext jc = new MapContext();
        Expression e = JEXL.createExpression("x.y.z = foo?:'quux'");
        Object o;

        // undefined foo

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", null);

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", Boolean.FALSE);

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }
        
        jc.set("foo", Double.NaN);

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }
                
        jc.set("foo", "");

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }
                        
        jc.set("foo", "false");

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }               
        
        jc.set("foo", 0d);

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }
                
        jc.set("foo", 0);

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            assertEquals("Should be quux", "quux", o);
        }


        jc.set("foo", "bar");

        for (int l = 0; l < 4; ++l) {
            jexl.setLenient((l & 1) != 0);
            jexl.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("Should be bar", "bar", o);
            o = jc.get("x.y.z");
            assertEquals("Should be bar", "bar", o);
        }

        debuggerCheck(jexl);
    }

}
