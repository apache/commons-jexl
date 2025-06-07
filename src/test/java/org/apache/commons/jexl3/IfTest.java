/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test cases for the if statement.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class IfTest extends JexlTestCase {
    public IfTest() {
        super("IfTest");
    }

    /**
     * Test the if statement handles blocks in the else statement correctly
     *
     * @throws Exception on any error
     */
    @Test
    void testBlockElse() throws Exception {
        final JexlScript e = JEXL.createScript("if (false) {1} else {2 ; 3}");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(3), o, "Result is wrong");
    }

    /**
     * Test the if statement handles blocks correctly
     *
     * @throws Exception on any error
     */
    @Test
    void testBlockIfTrue() throws Exception {
        final JexlScript e = JEXL.createScript("if (true) { 'hello'; }");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        assertEquals("hello", o, "Result is wrong");
    }

    @Test
    void testIfElseIfExpression() throws Exception {
        final JexlScript e = JEXL.createScript("if (x == 1) { 10; } else if (x == 2) 20  else 30", "x");
        Object o = e.execute(null, 1);
        assertEquals(10, o);
        o = e.execute(null, 2);
        assertEquals(20, o);
        o = e.execute(null, 4);
        assertEquals(30, o);
    }

    @Test
    void testIfElseIfReturnExpression() throws Exception {
        final JexlScript e = JEXL.createScript(
                "if (x == 1) return 10;  if (x == 2) return 20  else if (x == 3) return 30; else return 40;",
                "x");
        Object o = e.execute(null, 1);
        assertEquals(10, o);
        o = e.execute(null, 2);
        assertEquals(20, o);
        o = e.execute(null, 3);
        assertEquals(30, o);
        o = e.execute(null, 4);
        assertEquals(40, o);
    }

    @Test
    void testIfElseIfReturnExpression0() throws Exception {
        final JexlScript e = JEXL.createScript(
                "if (x == 1) return 10; if (x == 2)  return 20; else if (x == 3) return 30  else { return 40 }",
                "x");
        Object o = e.execute(null, 1);
        assertEquals(10, o);
        o = e.execute(null, 2);
        assertEquals(20, o);
        o = e.execute(null, 3);
        assertEquals(30, o);
        o = e.execute(null, 4);
        assertEquals(40, o);
    }

    /**
     * Test the if statement evaluates arithmetic expressions correctly
     *
     * @throws Exception on any error
     */
    @Test
    void testIfWithArithmeticExpression() throws Exception {
        final JexlScript e = JEXL.createScript("if ((x * 2) + 1 == 5) true;");
        final JexlContext jc = new MapContext();
        jc.set("x", Integer.valueOf(2));

        final Object o = e.execute(jc);
        assertEquals(Boolean.TRUE, o);
    }

    /**
     * Test the if statement works with assignment
     *
     * @throws Exception on any error
     */
    @Test
    void testIfWithAssignment() throws Exception {
        final JexlScript e = JEXL.createScript("if ((x * 2) == 5) {y = 1} else {y = 2;}");
        final JexlContext jc = new MapContext();
        jc.set("x", Float.valueOf(2.5f));

        e.execute(jc);
        final Object result = jc.get("y");
        assertEquals(Integer.valueOf(1), result, "y has the wrong value");
    }

    /**
     * Test the if statement evaluates decimal arithmetic expressions correctly
     *
     * @throws Exception on any error
     */
    @Test
    void testIfWithDecimalArithmeticExpression() throws Exception {
        final JexlScript e = JEXL.createScript("if ((x * 2) == 5) true");
        final JexlContext jc = new MapContext();
        jc.set("x", Float.valueOf(2.5f));

        final Object o = e.execute(jc);
        assertEquals(Boolean.TRUE, o);
    }

    /**
     * Test the if statement evaluates expressions correctly
     *
     * @throws Exception on any error
     */
    @Test
    void testIfWithSimpleExpression() throws Exception {
        final JexlScript e = JEXL.createScript("if (x == 1) true;");
        final JexlContext jc = new MapContext();
        jc.set("x", Integer.valueOf(1));

        final Object o = e.execute(jc);
        assertEquals(Boolean.TRUE, o);
    }

    @Test
    void testNullCoaelescing() throws Exception {
        Object o;
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlExpression xtrue = JEXL.createExpression("x??true");
        o = xtrue.evaluate(jc);
        assertEquals(true, o);
        jc.set("x", false);
        o = xtrue.evaluate(jc);
        assertEquals(false, o);
        final JexlExpression yone = JEXL.createExpression("y??1");
        o = yone.evaluate(jc);
        assertEquals(1, o);
        jc.set("y", 0);
        o = yone.evaluate(jc);
        assertEquals(0, o);
        debuggerCheck(JEXL);
    }

    @Test
    void testNullCoaelescingScript() throws Exception {
        Object o;
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlScript xtrue = JEXL.createScript("x??true");
        o = xtrue.execute(jc);
        assertEquals(true, o);
        jc.set("x", false);
        o = xtrue.execute(jc);
        assertEquals(false, o);
        final JexlScript yone = JEXL.createScript("y??1");
        o = yone.execute(jc);
        assertEquals(1, o);
        jc.set("y", 0);
        o = yone.execute(jc);
        assertEquals(0, o);
        debuggerCheck(JEXL);
    }

    /**
     * Make sure if false executes the false statement
     *
     * @throws Exception on any error
     */
    @Test
    void testSimpleElse() throws Exception {
        final JexlScript e = JEXL.createScript("if (false) 1 else 2;");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(2), o);
    }

    /**
     * Make sure if false doesn't execute the true statement
     *
     * @throws Exception on any error
     */
    @Test
    void testSimpleIfFalse() throws Exception {
        final JexlScript e = JEXL.createScript("if (false) 1");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        assertNull(o);
    }

    /**
     * Make sure if true executes the true statement
     *
     * @throws Exception on any error
     */
    @Test
    void testSimpleIfTrue() throws Exception {
        final JexlScript e = JEXL.createScript("if (true) 1");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(1), o);
    }

    /**
     * Ternary operator condition undefined or null evaluates to false
     * independently of engine flags.
     * @throws Exception
     */
    @Test
    void testTernary() throws Exception {
        final JexlEngine jexl = JEXL;

        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        final JexlExpression e = jexl.createExpression("x.y.z = foo ?'bar':'quux'");
        Object o;

        // undefined foo
        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
        }

        jc.set("foo", null);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
        }

        jc.set("foo", Boolean.FALSE);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
        }

        jc.set("foo", Boolean.TRUE);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("bar", o);
            o = jc.get("x.y.z");
            assertEquals("bar", o);
        }

        debuggerCheck(jexl);
    }

    @Test
    void testTernaryFail() throws Exception {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        final JexlExpression e = JEXL.createExpression("false ? bar : quux");
        options.setStrict(true);
        options.setSilent(false);
        final JexlException xjexl = assertThrows(JexlException.class, () -> e.evaluate(jc));
        assertTrue(xjexl.toString().contains("quux"));
    }

    /**
     * Ternary operator condition undefined or null evaluates to false
     * independently of engine flags; same for null coalescing operator.
     * @throws Exception
     */
    @Test
    void testTernaryShorthand() throws Exception {
        final JexlEvalContext jc = new JexlEvalContext();
        final JexlOptions options = jc.getEngineOptions();
        final JexlExpression e = JEXL.createExpression("x.y.z = foo?:'quux'");
        final JexlExpression f = JEXL.createExpression("foo??'quux'");
        Object o;

        // undefined foo
        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
            o = f.evaluate(jc);
            assertEquals("quux", o);
        }

        jc.set("foo", null);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
            o = f.evaluate(jc);
            assertEquals("quux", o);
        }

        jc.set("foo", Boolean.FALSE);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
            o = f.evaluate(jc);
            assertEquals(false, o);
        }

        jc.set("foo", Double.NaN);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
            o = f.evaluate(jc);
            assertTrue(Double.isNaN((Double) o), "Should be NaN");
        }

        jc.set("foo", "");

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
            o = f.evaluate(jc);
            assertEquals("", o, "Should be empty string");
        }

        jc.set("foo", "false");

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
            o = f.evaluate(jc);
            assertEquals("false", o);
        }

        jc.set("foo", 0d);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
            o = f.evaluate(jc);
            assertEquals(0.d, o);
        }

        jc.set("foo", 0);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("quux", o);
            o = jc.get("x.y.z");
            assertEquals("quux", o);
            o = f.evaluate(jc);
            assertEquals(0, o);
        }

        jc.set("foo", "bar");

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            assertEquals("bar", o);
            o = jc.get("x.y.z");
            assertEquals("bar", o);
        }

        debuggerCheck(JEXL);
    }
}
