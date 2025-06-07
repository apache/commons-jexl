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

import java.math.BigInteger;

import org.apache.commons.jexl3.junit.Asserter;
import org.junit.jupiter.api.Test;

/**
 * Tests shift operators.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ShiftOperatorsTest extends JexlTestCase {
    public static class ShiftArithmetic extends JexlArithmetic {
        ShiftArithmetic(final boolean flag) {
            super(flag);
        }

        public Object shiftLeft(final StringBuilder c, final String value) {
            c.append(value);
            return c;
        }

        public Object shiftRight(final String value, final StringBuilder c) {
            c.append(value);
            return c;
        }

        public Object shiftRightUnsigned(final String value, final StringBuilder c) {
            c.append(value.toLowerCase());
            return c;
        }
    }

    static BigInteger shiftRightUnsigned(final BigInteger bl, final int r) {
        return bl.signum() < 0 ? bl.negate().shiftRight(r) : bl.shiftRight(r);
    }

    static BigInteger shiftRightUnsigned(final String bl, final int r) {
        return shiftRightUnsigned(new BigInteger(bl), r);
    }

    private final Asserter asserter;

    private final Asserter a360;

    public ShiftOperatorsTest() {
        super("ShiftOperatorsTest");
        asserter = new Asserter(JEXL);
        asserter.setStrict(false, false);

        final JexlEngine j360 = new JexlBuilder().arithmetic(new Arithmetic360(true)).strict(true).create();
        a360 = new Asserter(j360);
        a360.setStrict(false, false);
    }

    @Test
    void testLeftShiftIntValue() throws Exception {
        final String expr = "(x, y)-> x << y";
        asserter.assertExpression(expr, 1L << 2, 1L, 2);
        asserter.assertExpression(expr, 1L << -2, 1L, -2);
        asserter.assertExpression(expr, -1L << 2, -1L, 2);
        asserter.assertExpression(expr, -1L << -2, -1L, -2);

        a360.assertExpression(expr, 1L << 2, 1L, 2);
        a360.assertExpression(expr, 1L << -2, 1L, -2);
        a360.assertExpression(expr, -1L << 2, -1L, 2);
        a360.assertExpression(expr, -1L << -2, -1L, -2);

        a360.assertExpression(expr, 1 << 2, 1, 2);
        a360.assertExpression(expr, 1 << -2, 1, -2);
        a360.assertExpression(expr, -1 << 2, -1, 2);
        a360.assertExpression(expr, -1 << -2, -1, -2);
    }

    @Test
    void testLeftShiftLongValue() throws Exception {
        a360.assertExpression("2147483648 << 2", 2147483648L << 2);
        a360.assertExpression("2147483648 << -2", 2147483648L << -2);
        a360.assertExpression("-2147483649 << 2", -2147483649L << 2);
        a360.assertExpression("-2147483649 << -2", -2147483649L << -2);
    }

    @Test
    void testOverloadedShift() throws Exception {
        final JexlEngine jexl = new JexlBuilder().arithmetic(new ShiftArithmetic(true)).create();
        StringBuilder x;
        JexlScript e;
        Object o;

        x = new StringBuilder("1");
        e = jexl.createScript("x << 'Left'", "x");
        o = e.execute(null, x);
        assertEquals("1Left", o.toString(), e::getSourceText);

        e = jexl.createScript("'Right' >> x", "x");
        x = new StringBuilder("1");
        o = e.execute(null, x);
        assertEquals("1Right", x.toString(), e::getSourceText);

        e = jexl.createScript("'Right' >>> x", "x");
        x = new StringBuilder("1");
        o = e.execute(null, x);
        assertEquals("1right", x.toString(), e::getSourceText);
    }

    @Test
    void testPrecedence() throws Exception {
        a360.assertExpression("40 + 2 << 1 + 1", 40 + 2 << 1 + 1);
        a360.assertExpression("40 + (2 << 1) + 1", 40 + (2 << 1) + 1);
        a360.assertExpression("(40 + 2) << (1 + 1)", 40 + 2 << 1 + 1);

        a360.assertExpression("40 + 2L << 1 + 1", 40 + 2L << 1 + 1);
        a360.assertExpression("40 + (2L << 1) + 1", 40 + (2L << 1) + 1);
        a360.assertExpression("(40 + 2L) << (1 + 1)", 40 + 2L << 1 + 1);

        a360.assertExpression("40L + 2 << 1 + 1", 40L + 2L << 1 + 1);
        a360.assertExpression("40L + (2 << 1) + 1", 40L + (2L << 1) + 1);
        a360.assertExpression("(40L + 2) << (1 + 1)", 40L + 2L << 1 + 1);
    }

    @Test
    void testRightShiftBigValue() throws Exception {
        a360.assertExpression("9223372036854775808 >> 2", new BigInteger("9223372036854775808").shiftRight(2));
        a360.assertExpression("9223372036854775808 >> -2", new BigInteger("9223372036854775808").shiftRight(-2));
        a360.assertExpression("-9223372036854775809 >> 2", new BigInteger("-9223372036854775809").shiftRight(2));
        a360.assertExpression("-9223372036854775809 >> -2", new BigInteger("-9223372036854775809").shiftRight(-2));
    }

    @Test
    void testRightShiftIntValue() throws Exception {
        final String expr = "(x, y)-> x >> y";
        asserter.assertExpression(expr, 42L >> 2, 42L, 2);
        asserter.assertExpression(expr, 42L >> -2, 42L, -2);
        asserter.assertExpression(expr, -42L >> 2, -42L, 2);
        asserter.assertExpression(expr, -42L >> -2, -42L, -2);

        a360.assertExpression(expr, 42L >> 2, 42L, 2);
        a360.assertExpression(expr, 42L >> -2, 42L, -2);
        a360.assertExpression(expr, -42L >> 2, -42L, 2);
        a360.assertExpression(expr, -42L >> -2, -42L, -2);

        a360.assertExpression(expr, 42 >> 2, 42, 2);
        a360.assertExpression(expr, 42 >> -2, 42, -2);
        a360.assertExpression(expr, -42 >> 2, -42, 2);
        a360.assertExpression(expr, -42 >> -2, -42, -2);
    }

    @Test
    void testRightShiftLongValue() throws Exception {
        a360.assertExpression("8589934592 >> 2", 8589934592L >> 2);
        a360.assertExpression("8589934592 >> -2", 8589934592L >> -2);
        a360.assertExpression("-8589934592 >> 2", -8589934592L >> 2);
        a360.assertExpression("-8589934592 >> -2", -8589934592L >> -2);
    }

    @Test
    void testRightShiftUnsignedBigValue() throws Exception {
        a360.assertExpression("9223372036854775808 >>> 2", shiftRightUnsigned("9223372036854775808", 2));
        a360.assertExpression("9223372036854775808 >>> -2", shiftRightUnsigned("9223372036854775808", -2));
        a360.assertExpression("-9223372036854775809 >>> 2", shiftRightUnsigned("-9223372036854775809", 2));
        a360.assertExpression("-9223372036854775809 >>> -2", shiftRightUnsigned("-9223372036854775809", -2));
    }

    @Test
    void testRightShiftUnsignedIntValue() throws Exception {
        final String expr = "(x, y)-> x >>> y";
        asserter.assertExpression(expr, 42L >>> 2, 42L, 2);
        asserter.assertExpression(expr, 42L >>> -2, 42L, -2);
        asserter.assertExpression(expr, -42L >>> 2, -42L, 2);
        asserter.assertExpression(expr, -42L >>> -2, -42L, -2);
    }

}
