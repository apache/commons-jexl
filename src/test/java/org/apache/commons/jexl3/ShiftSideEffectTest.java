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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import org.apache.commons.jexl3.junit.Asserter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests for shift self assign operators (<<=,>>=,>>>=)
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ShiftSideEffectTest extends JexlTestCase {

    private Asserter asserter;

    public ShiftSideEffectTest() {
        super("ShiftSideEffectTest");
    }

    @Override
    @Before
    public void setUp() {
        asserter = new Asserter(JEXL);
    }

    @Test
    public void testSideEffectVar() throws Exception {
        Map<String,Object> context = asserter.getVariables();
        Integer i41 = Integer.valueOf(4141);
        Object foo = i41;

        context.put("foo", foo);
        asserter.assertExpression("foo <<= 2", i41 << 2);
        Assert.assertEquals(context.get("foo"), i41 << 2);

        context.put("foo", foo);
        asserter.assertExpression("foo >>= 2", i41 >> 2);
        Assert.assertEquals(context.get("foo"), i41 >> 2);

        context.put("foo", foo);
        asserter.assertExpression("foo >>>= 2", i41 >>> 2);
        Assert.assertEquals(context.get("foo"), i41 >>> 2);
    }

    @Test
    public void testSideEffectVarDots() throws Exception {
        Map<String,Object> context = asserter.getVariables();
        Integer i41 = Integer.valueOf(4141);
        Object foo = i41;

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux <<= 2", i41 << 2);
        Assert.assertEquals(context.get("foo.bar.quux"), i41 << 2);

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux >>= 2", i41 >> 2);
        Assert.assertEquals(context.get("foo.bar.quux"), i41 >> 2);

        context.put("foo.bar.quux", foo);
        asserter.assertExpression("foo.bar.quux >>>= 2", i41 >>> 2);
        Assert.assertEquals(context.get("foo.bar.quux"), i41 >>> 2);
    }

    @Test
    public void testSideEffectArray() throws Exception {
        Integer i41 = Integer.valueOf(4141);
        Integer i42 = Integer.valueOf(42);
        Integer i43 = Integer.valueOf(43);
        String s42 = "fourty-two";
        String s43 = "fourty-three";
        Object[] foo = new Object[3];
        foo[1] = i42;
        foo[2] = i43;
        asserter.setVariable("foo", foo);
        foo[0] = i41;
        asserter.assertExpression("foo[0] <<= 2", i41 << 2);
        Assert.assertEquals(foo[0], i41 << 2);
        foo[0] = i41;
        asserter.assertExpression("foo[0] >>= 2", i41 >> 2);
        Assert.assertEquals(foo[0], i41 >> 2);
        foo[0] = i41;
        asserter.assertExpression("foo[0] >>>= 2", i41 >>> 2);
        Assert.assertEquals(foo[0], i41 >>> 2);
    }

    @Test
    public void testSideEffectDotArray() throws Exception {
        Integer i41 = Integer.valueOf(4141);
        Integer i42 = Integer.valueOf(42);
        Integer i43 = Integer.valueOf(43);
        String s42 = "fourty-two";
        String s43 = "fourty-three";
        Object[] foo = new Object[3];
        foo[1] = i42;
        foo[2] = i43;
        asserter.setVariable("foo", foo);
        foo[0] = i41;
        asserter.assertExpression("foo.0 <<= 2", i41 << 2);
        Assert.assertEquals(foo[0], i41 << 2);
        foo[0] = i41;
        asserter.assertExpression("foo.0 >>= 2", i41 >> 2);
        Assert.assertEquals(foo[0], i41 >> 2);
        foo[0] = i41;
        asserter.assertExpression("foo.0 >>>= 2", i41 >>> 2);
        Assert.assertEquals(foo[0], i41 >>> 2);
    }

    @Test
    public void testSideEffectAntishArray() throws Exception {
        Integer i41 = Integer.valueOf(4141);
        Integer i42 = Integer.valueOf(42);
        Integer i43 = Integer.valueOf(43);
        Object[] foo = new Object[3];
        foo[1] = i42;
        foo[2] = i43;
        asserter.setVariable("foo.bar", foo);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] <<= 2", i41 << 2);
        Assert.assertEquals(foo[0], i41 << 2);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] >>= 2", i41 >> 2);
        Assert.assertEquals(foo[0], i41 >> 2);
        foo[0] = i41;
        asserter.assertExpression("foo.bar[0] >>>= 2", i41 >>> 2);
        Assert.assertEquals(foo[0], i41 >>> 2);
    }

    public static class Foo {
        int value;
        Foo(int v) {
            value = v;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        public void setValue(long v) {
            value = (int) v;
        }
        public int getValue() {
            return value;
        }

        public void setBar(int x, long v) {
            value = (int) v + x;
        }

        public int getBar(int x) {
            return value + x;
        }
    }

    @Test
    public void testSideEffectBean() throws Exception {
        Integer i41 = Integer.valueOf(4141);
        Foo foo = new Foo(0);
        asserter.setVariable("foo", foo);
        foo.value = i41;
        asserter.assertExpression("foo.value <<= 2", i41 << 2);
        Assert.assertEquals(foo.value, i41 << 2);
        foo.value = i41;
        asserter.assertExpression("foo.value >>= 2", i41 >> 2);
        Assert.assertEquals(foo.value, i41 >> 2);
        foo.value = i41;
        asserter.assertExpression("foo.value >>>= 2", i41 >>> 2);
        Assert.assertEquals(foo.value, i41 >>> 2);
    }

    @Test
    public void testSideEffectBeanContainer() throws Exception {
        Integer i41 = Integer.valueOf(4141);
        Foo foo = new Foo(0);
        asserter.setVariable("foo", foo);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] <<= 2", i41 << 2);
        Assert.assertEquals(foo.value, i41 << 2);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] >>= 2", i41 >> 2);
        Assert.assertEquals(foo.value, i41 >> 2);
        foo.value = i41;
        asserter.assertExpression("foo.bar[0] >>>= 2", i41 >>> 2);
        Assert.assertEquals(foo.value, i41 >>> 2);
    }

    @Test
    public void testArithmeticSelf() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(64).arithmetic(new SelfArithmetic(false)).create();
        JexlContext jc = null;
        runSelfOverload(jexl, jc);
        runSelfOverload(jexl, jc);
    }

    @Test
    public void testArithmeticSelfNoCache() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(0).arithmetic(new SelfArithmetic(false)).create();
        JexlContext jc = null;
        runSelfOverload(jexl, jc);
    }

    protected void runSelfOverload(JexlEngine jexl, JexlContext jc) {
        JexlScript script;
        Object result;
        script = jexl.createScript("(x, y)->{ x <<= y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertEquals(3115 << 15,  result);
        Var v0 = new Var(3115);
        result = script.execute(jc, v0, new Var(15));
        Assert.assertEquals(result, v0);
        Assert.assertEquals(3115 << 15,  v0.value);

        script = jexl.createScript("(x, y)->{ x >>= y}");
        result = script.execute(jc, 3115, 2);
        Assert.assertEquals(3115 >> 2,  result);
        Var v1 = new Var(3115);
        result = script.execute(jc, v1, new Var(2));
        Assert.assertEquals(result, v1);
        Assert.assertEquals(3115 >> 2,  ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x >>>= y }");
        result = script.execute(jc, 3115, 2);
        Assert.assertEquals(3115 >>> 2,  result);
        Var v2 = new Var(3115);
        result = script.execute(jc, v2, new Var(2));
        Assert.assertEquals(result, v2);
        Assert.assertEquals(3115 >>> 2,  v2.value);
    }

    public static class Var {
        int value;

        Var(int v) {
            value = v;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    // an arithmetic that performs side effects
    public static class SelfArithmetic extends JexlArithmetic {
        public SelfArithmetic(boolean strict) {
            super(strict);
        }

        public Object propertyGet(Var var, String property) {
            return "value".equals(property)? var.value : JexlEngine.TRY_FAILED;
        }

        public Object propertySet(Var var, String property, int v) {
            return "value".equals(property)? var.value = v : JexlEngine.TRY_FAILED;
        }

        public Object arrayGet(Var var, String property) {
            return "VALUE".equals(property)? var.value : JexlEngine.TRY_FAILED;
        }

        public Object arraySet(Var var, String property, int v) {
            return "VALUE".equals(property)? var.value = v : JexlEngine.TRY_FAILED;
        }
    
        public Var leftShift(Var lhs, Var rhs) {
            return new Var(lhs.value << rhs.value);
        }

        public JexlOperator selfLeftShift(Var lhs, Var rhs) {
            lhs.value <<= rhs.value;
            return JexlOperator.ASSIGN;
        }

        public Var rightShift(Var lhs, Var rhs) {
            return new Var(lhs.value >> rhs.value);
        }

        public JexlOperator selfRightShift(Var lhs, Var rhs) {
            lhs.value >>= rhs.value;
            return JexlOperator.ASSIGN;
        }

        public Var rightShiftUnsigned(Var lhs, Var rhs) {
            return new Var(lhs.value >>> rhs.value);
        }

        public JexlOperator selfRightShiftUnsigned(Var lhs, Var rhs) {
            lhs.value >>>= rhs.value;
            return JexlOperator.ASSIGN;
        }
    }
}
