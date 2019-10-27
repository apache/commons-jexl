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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the if statement.
 *
 * @since 1.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class IfTest extends JexlTestCase {
    public IfTest() {
        super("IfTest");
    }

    /**
     * Make sure if true executes the true statement
     *
     * @throws Exception on any error
     */
    @Test
    public void testSimpleIfTrue() throws Exception {
        JexlScript e = JEXL.createScript("if (true) 1");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Assert.assertEquals("Result is not 1", new Integer(1), o);
    }

    /**
     * Make sure if false doesn't execute the true statement
     *
     * @throws Exception on any error
     */
    @Test
    public void testSimpleIfFalse() throws Exception {
        JexlScript e = JEXL.createScript("if (false) 1");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Assert.assertNull("Return value is not empty", o);
    }

    /**
     * Make sure if false executes the false statement
     *
     * @throws Exception on any error
     */
    @Test
    public void testSimpleElse() throws Exception {
        JexlScript e = JEXL.createScript("if (false) 1 else 2;");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Assert.assertEquals("Result is not 2", new Integer(2), o);
    }

    /**
     * Test the if statement handles blocks correctly
     *
     * @throws Exception on any error
     */
    @Test
    public void testBlockIfTrue() throws Exception {
        JexlScript e = JEXL.createScript("if (true) { 'hello'; }");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Assert.assertEquals("Result is wrong", "hello", o);
    }

    /**
     * Test the if statement handles blocks in the else statement correctly
     *
     * @throws Exception on any error
     */
    @Test
    public void testBlockElse() throws Exception {
        JexlScript e = JEXL.createScript("if (false) {1} else {2 ; 3}");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Assert.assertEquals("Result is wrong", new Integer(3), o);
    }

    /**
     * Test the if statement evaluates expressions correctly
     *
     * @throws Exception on any error
     */
    @Test
    public void testIfWithSimpleExpression() throws Exception {
        JexlScript e = JEXL.createScript("if (x == 1) true;");
        JexlContext jc = new MapContext();
        jc.set("x", new Integer(1));

        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testIfElseIfExpression() throws Exception {
        JexlScript e = JEXL.createScript("if (x == 1) { 10; } else if (x == 2) 20  else 30", "x");
        Object o = e.execute(null, 1);
        Assert.assertEquals(10, o);
        o = e.execute(null, 2);
        Assert.assertEquals(20, o);
        o = e.execute(null, 4);
        Assert.assertEquals(30, o);
    }

    @Test
    public void testIfElseIfReturnExpression0() throws Exception {
        JexlScript e = JEXL.createScript(
                "if (x == 1) return 10; if (x == 2)  return 20; else if (x == 3) return 30  else { return 40 }",
                "x");
        Object o = e.execute(null, 1);
        Assert.assertEquals(10, o);
        o = e.execute(null, 2);
        Assert.assertEquals(20, o);
        o = e.execute(null, 3);
        Assert.assertEquals(30, o);
        o = e.execute(null, 4);
        Assert.assertEquals(40, o);
    }

    @Test
    public void testIfElseIfReturnExpression() throws Exception {
        JexlScript e = JEXL.createScript(
                "if (x == 1) return 10;  if (x == 2) return 20  else if (x == 3) return 30; else return 40;",
                "x");
        Object o = e.execute(null, 1);
        Assert.assertEquals(10, o);
        o = e.execute(null, 2);
        Assert.assertEquals(20, o);
        o = e.execute(null, 3);
        Assert.assertEquals(30, o);
        o = e.execute(null, 4);
        Assert.assertEquals(40, o);
    }

    /**
     * Test the if statement evaluates arithmetic expressions correctly
     *
     * @throws Exception on any error
     */
    @Test
    public void testIfWithArithmeticExpression() throws Exception {
        JexlScript e = JEXL.createScript("if ((x * 2) + 1 == 5) true;");
        JexlContext jc = new MapContext();
        jc.set("x", new Integer(2));

        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    /**
     * Test the if statement evaluates decimal arithmetic expressions correctly
     *
     * @throws Exception on any error
     */
    @Test
    public void testIfWithDecimalArithmeticExpression() throws Exception {
        JexlScript e = JEXL.createScript("if ((x * 2) == 5) true");
        JexlContext jc = new MapContext();
        jc.set("x", new Float(2.5f));

        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    /**
     * Test the if statement works with assignment
     *
     * @throws Exception on any error
     */
    @Test
    public void testIfWithAssignment() throws Exception {
        JexlScript e = JEXL.createScript("if ((x * 2) == 5) {y = 1} else {y = 2;}");
        JexlContext jc = new MapContext();
        jc.set("x", new Float(2.5f));

        e.execute(jc);
        Object result = jc.get("y");
        Assert.assertEquals("y has the wrong value", new Integer(1), result);
    }

    /**
     * Ternary operator condition undefined or null evaluates to false
     * independantly of engine flags.
     * @throws Exception
     */
    @Test
    public void testTernary() throws Exception {
        JexlEngine jexl = JEXL;

        JexlEvalContext jc = new JexlEvalContext();
        JexlOptions options = jc.getEngineOptions();
        JexlExpression e = jexl.createExpression("x.y.z = foo ?'bar':'quux'");
        Object o;

        // undefined foo
        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", null);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", Boolean.FALSE);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", Boolean.TRUE);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be bar", "bar", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be bar", "bar", o);
        }

        debuggerCheck(jexl);
    }

    /**
     * Ternary operator condition undefined or null evaluates to false
     * independently of engine flags; same for null coalescing operator.
     * @throws Exception
     */
    @Test
    public void testTernaryShorthand() throws Exception {
        JexlEvalContext jc = new JexlEvalContext();
        JexlOptions options = jc.getEngineOptions();
        JexlExpression e = JEXL.createExpression("x.y.z = foo?:'quux'");
        JexlExpression f = JEXL.createExpression("foo??'quux'");
        Object o;

        // undefined foo
        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
            o = f.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", null);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
            o = f.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
        }

        jc.set("foo", Boolean.FALSE);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
            o = f.evaluate(jc);
            Assert.assertEquals("Should be false", false, o);
        }

        jc.set("foo", Double.NaN);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
            o = f.evaluate(jc);
            Assert.assertTrue("Should be NaN", Double.isNaN((Double) o));
        }

        jc.set("foo", "");

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
            o = f.evaluate(jc);
            Assert.assertEquals("Should be empty string", "", o);
        }

        jc.set("foo", "false");

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
            o = f.evaluate(jc);
            Assert.assertEquals("Should be 'false'", "false", o);
        }

        jc.set("foo", 0d);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
            o = f.evaluate(jc);
            Assert.assertEquals("Should be 0", 0.d, o);
        }

        jc.set("foo", 0);

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be quux", "quux", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be quux", "quux", o);
            o = f.evaluate(jc);
            Assert.assertEquals("Should be 0", 0, o);
        }

        jc.set("foo", "bar");

        for (int l = 0; l < 4; ++l) {
            options.setStrict((l & 1) == 0);
            options.setSilent((l & 2) != 0);
            o = e.evaluate(jc);
            Assert.assertEquals("Should be bar", "bar", o);
            o = jc.get("x.y.z");
            Assert.assertEquals("Should be bar", "bar", o);
        }

        debuggerCheck(JEXL);
    }

    @Test
    public void testNullCoaelescing() throws Exception {
        Object o;
        JexlEvalContext jc = new JexlEvalContext();
        JexlExpression xtrue = JEXL.createExpression("x??true");
        o = xtrue.evaluate(jc);
        Assert.assertEquals("Should be true", true, o);
        jc.set("x", false);
        o = xtrue.evaluate(jc);
        Assert.assertEquals("Should be false", false, o);
        JexlExpression yone = JEXL.createExpression("y??1");
        o = yone.evaluate(jc);
        Assert.assertEquals("Should be 1", 1, o);
        jc.set("y", 0);
        o = yone.evaluate(jc);
        Assert.assertEquals("Should be 0", 0, o);
        debuggerCheck(JEXL);
    }

    @Test
    public void testNullCoaelescingScript() throws Exception {
        Object o;
        JexlEvalContext jc = new JexlEvalContext();
        JexlScript xtrue = JEXL.createScript("x??true");
        o = xtrue.execute(jc);
        Assert.assertEquals("Should be true", true, o);
        jc.set("x", false);
        o = xtrue.execute(jc);
        Assert.assertEquals("Should be false", false, o);
        JexlScript yone = JEXL.createScript("y??1");
        o = yone.execute(jc);
        Assert.assertEquals("Should be 1", 1, o);
        jc.set("y", 0);
        o = yone.execute(jc);
        Assert.assertEquals("Should be 0", 0, o);
        debuggerCheck(JEXL);
    }


    @Test
    public void testTernaryFail() throws Exception {
        JexlEvalContext jc = new JexlEvalContext();
        JexlOptions options = jc.getEngineOptions();
        JexlExpression e = JEXL.createExpression("false ? bar : quux");
        Object o;
        options.setStrict(true);
        options.setSilent(false);
        try {
           o = e.evaluate(jc);
           Assert.fail("Should have failed");
        } catch (JexlException xjexl) {
           // OK
           Assert.assertTrue(xjexl.toString().contains("quux"));
        }
    }
}
