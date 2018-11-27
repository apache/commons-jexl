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

import java.math.BigInteger;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests shift operators.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ShiftOperatorsTest extends JexlTestCase {

    public ShiftOperatorsTest() {
        super("ShiftOperatorsTest");
    }

    @Test
    public void testLeftShiftIntValue() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("1 << 2");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 1 << 2, o);

        e = JEXL.createScript("1 << -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 1 << -2, o);

        e = JEXL.createScript("-1 << 2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -1 << 2, o);

        e = JEXL.createScript("-1 << -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -1 << -2, o);
    }

    @Test
    public void testRightShiftIntValue() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("42 >> 2");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42 >> 2, o);

        e = JEXL.createScript("42 >> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42 >> -2, o);

        e = JEXL.createScript("-42 >> 2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -42 >> 2, o);

        e = JEXL.createScript("-42 >> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -42 >> -2, o);
    }

    @Test
    public void testRightShiftUnsignedIntValue() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("42 >>> 2");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42 >>> 2, o);

        e = JEXL.createScript("42 >>> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42 >>> -2, o);

        e = JEXL.createScript("-42 >>> 2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -42 >>> 2, o);

        e = JEXL.createScript("-42 >>> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -42 >>> -2, o);
    }

    @Test
    public void testLeftShiftLongValue() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("2147483648 << 2");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 2147483648L << 2, o);

        e = JEXL.createScript("2147483648 << -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 2147483648L << -2, o);

        e = JEXL.createScript("-2147483649 << 2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -2147483649L << 2, o);

        e = JEXL.createScript("-2147483649 << -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -2147483649L << -2, o);
    }

    @Test
    public void testRightShiftLongValue() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("8589934592 >> 2");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 8589934592L >> 2, o);

        e = JEXL.createScript("8589934592 >> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 8589934592L >> -2, o);

        e = JEXL.createScript("-8589934596 >> 2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -8589934596L >> 2, o);

        e = JEXL.createScript("-8589934596 >> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -8589934596L >> -2, o);
    }

    @Test
    public void testRightShiftUnsignedLongValue() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("8589934592 >>> 2");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 8589934592L >>> 2, o);

        e = JEXL.createScript("8589934592 >>> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 8589934592L >>> -2, o);

        e = JEXL.createScript("-8589934596 >>> 2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -8589934596L >>> 2, o);

        e = JEXL.createScript("-8589934596 >>> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", -8589934596L >>> -2, o);
    }

    @Test
    public void testLeftShiftBigValue() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("9223372036854775808 << 2");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", new BigInteger("9223372036854775808").shiftLeft(2), o);

        e = JEXL.createScript("9223372036854775808 << -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", new BigInteger("9223372036854775808").shiftLeft(-2), o);

        e = JEXL.createScript("-9223372036854775809 << 2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", new BigInteger("-9223372036854775809").shiftLeft(2), o);

        e = JEXL.createScript("-9223372036854775809 << -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", new BigInteger("-9223372036854775809").shiftLeft(-2), o);
    }

    @Test
    public void testRightShiftBigValue() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("9223372036854775808 >> 2");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", new BigInteger("9223372036854775808").shiftRight(2), o);

        e = JEXL.createScript("9223372036854775808 >> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", new BigInteger("9223372036854775808").shiftRight(-2), o);

        e = JEXL.createScript("-9223372036854775809 >> 2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", new BigInteger("-9223372036854775809").shiftRight(2), o);

        e = JEXL.createScript("-9223372036854775809 >> -2");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", new BigInteger("-9223372036854775809").shiftRight(-2), o);
    }

    public static class ShiftArithmetic extends JexlArithmetic {
        ShiftArithmetic(boolean flag) {
            super(flag);
        }
       
        public Object leftShift(StringBuilder c, String value) {
            c.append(value);
            return c;
        }

        public Object rightShift(String value, StringBuilder c) {
            c.append(value);
            return c;
        }

        public Object rightShiftUnsigned(String value, StringBuilder c) {
            c.append(value.toLowerCase());
            return c;
        }
    }

    @Test
    public void testOverloadedShift() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new ShiftArithmetic(true)).create();
        JexlScript e = jexl.createScript("x << 'Left'");
        JexlContext jc = new MapContext();
        StringBuilder c = new StringBuilder("1");
        jc.set("x", c);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", "1Left", c.toString());

        e = jexl.createScript("'Right' >> x");
        jc = new MapContext();
        c = new StringBuilder("1");
        jc.set("x", c);
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", "1Right", c.toString());

        e = jexl.createScript("'Right' >>> x");
        jc = new MapContext();
        c = new StringBuilder("1");
        jc.set("x", c);
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", "1right", c.toString());
    }

    @Test
    public void testPrecedence() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("40 + 2 << 1 + 1");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 40 + 2 << 1 + 1, o);

        e = JEXL.createScript("40 + (2 << 1) + 1");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 40 + (2 << 1) + 1, o);

        e = JEXL.createScript("(40 + 2) << (1 + 1)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", (40 + 2) << (1 + 1), o);
    }

}
