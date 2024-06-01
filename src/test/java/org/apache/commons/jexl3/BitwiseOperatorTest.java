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

import org.apache.commons.jexl3.junit.Asserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the bitwise operators.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class BitwiseOperatorTest extends JexlTestCase {
    private Asserter asserter;

    /**
     * Create the named test.
     */
    public BitwiseOperatorTest() {
        super("BitwiseOperatorTest");
    }

    @Override
    @BeforeEach
    public void setUp() {
        asserter = new Asserter(JEXL);
        asserter.setStrict(false, false);
    }

    @Test
    public void testAndSimple() throws Exception {
        asserter.assertExpression("15 & 3", Long.valueOf(15 & 3));
    }

    @Test
    public void testAndVariableNumberCoercion() throws Exception {
        asserter.setVariable("x", Integer.valueOf(15));
        asserter.setVariable("y", Short.valueOf((short) 7));
        asserter.assertExpression("x & y", Long.valueOf(15 & 7));
    }

    @Test
    public void testAndVariableStringCoercion() throws Exception {
        asserter.setVariable("x", Integer.valueOf(15));
        asserter.setVariable("y", "7");
        asserter.assertExpression("x & y", Long.valueOf(15 & 7));
    }

    @Test
    public void testAndWithLeftNull() throws Exception {
        asserter.assertExpression("null & 1", Long.valueOf(0));
    }

    @Test
    public void testAndWithRightNull() throws Exception {
        asserter.assertExpression("1 & null", Long.valueOf(0));
    }

    @Test
    public void testAndWithTwoNulls() throws Exception {
        asserter.assertExpression("null & null", Long.valueOf(0));
    }

    @Test
    public void testComplementSimple() throws Exception {
        asserter.assertExpression("~128", Long.valueOf(-129));
    }

    @Test
    public void testComplementVariableNumberCoercion() throws Exception {
        asserter.setVariable("x", Integer.valueOf(15));
        asserter.assertExpression("~x", Long.valueOf(~15));
    }

    @Test
    public void testComplementVariableStringCoercion() throws Exception {
        asserter.setVariable("x", "15");
        asserter.assertExpression("~x", Long.valueOf(~15));
    }

    @Test
    public void testComplementWithNull() throws Exception {
        asserter.assertExpression("~null", Long.valueOf(-1));
    }

    @Test
    public void testOrSimple() throws Exception {
        asserter.assertExpression("12 | 3", Long.valueOf(15));
    }

    @Test
    public void testOrVariableNumberCoercion() throws Exception {
        asserter.setVariable("x", Integer.valueOf(12));
        asserter.setVariable("y", Short.valueOf((short) 3));
        asserter.assertExpression("x | y", Long.valueOf(15));
    }

    @Test
    public void testOrVariableStringCoercion() throws Exception {
        asserter.setVariable("x", Integer.valueOf(12));
        asserter.setVariable("y", "3");
        asserter.assertExpression("x | y", Long.valueOf(15));
    }

    @Test
    public void testOrWithLeftNull() throws Exception {
        asserter.assertExpression("null | 1", Long.valueOf(1));
    }

    @Test
    public void testOrWithRightNull() throws Exception {
        asserter.assertExpression("1 | null", Long.valueOf(1));
    }

    @Test
    public void testOrWithTwoNulls() throws Exception {
        asserter.assertExpression("null | null", Long.valueOf(0));
    }

    @Test
    public void testParenthesized() throws Exception {
        asserter.assertExpression("(2 | 1) & 3", Long.valueOf(3L));
        asserter.assertExpression("(2 & 1) | 3", Long.valueOf(3L));
        asserter.assertExpression("~(120 | 42)", Long.valueOf(~(120 | 42)));
    }

    @Test
    public void testXorSimple() throws Exception {
        asserter.assertExpression("1 ^ 3", Long.valueOf(1 ^ 3));
    }

    @Test
    public void testXorVariableNumberCoercion() throws Exception {
        asserter.setVariable("x", Integer.valueOf(1));
        asserter.setVariable("y", Short.valueOf((short) 3));
        asserter.assertExpression("x ^ y", Long.valueOf(1 ^ 3));
    }

    @Test
    public void testXorVariableStringCoercion() throws Exception {
        asserter.setVariable("x", Integer.valueOf(1));
        asserter.setVariable("y", "3");
        asserter.assertExpression("x ^ y", Long.valueOf(1 ^ 3));
    }

    @Test
    public void testXorWithLeftNull() throws Exception {
        asserter.assertExpression("null ^ 1", Long.valueOf(1));
    }

    @Test
    public void testXorWithRightNull() throws Exception {
        asserter.assertExpression("1 ^ null", Long.valueOf(1));
    }

    @Test
    public void testXorWithTwoNulls() throws Exception {
        asserter.assertExpression("null ^ null", Long.valueOf(0));
    }

}
