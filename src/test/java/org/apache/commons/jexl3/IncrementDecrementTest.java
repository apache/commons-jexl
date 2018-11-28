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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test cases for increment/decrement assignment.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class IncrementDecrementTest extends JexlTestCase {

    public IncrementDecrementTest() {
        super("IncrementDecrementTest", new JexlBuilder().cache(512).arithmetic(new AtomicArithmetic(false)).create());
    }

    public static class AtomicArithmetic extends JexlArithmetic {
        AtomicArithmetic(boolean flag) {
            super(flag);
        }

        public Object increment(AtomicInteger val) {
            val.incrementAndGet();
            return JexlOperator.ASSIGN;
        }

        public Object decrement(AtomicInteger val) {
            val.decrementAndGet();
            return JexlOperator.ASSIGN;
        }
    }

    @Test
    public void testIncrement() throws Exception {
        JexlScript inc = JEXL.createScript("var x = 1; ++x");
        JexlContext jc = new MapContext();
        Object o = inc.execute(jc);
        Assert.assertEquals("Result is not 2", new Integer(2), o);
    }

    @Test
    public void testIncrementPost() throws Exception {
        JexlScript inc = JEXL.createScript("var x = 1; ++x; x");
        JexlContext jc = new MapContext();
        Object o = inc.execute(jc);
        Assert.assertEquals("Result is not 2", new Integer(2), o);
    }

    @Test
    public void testIncrementOverride() throws Exception {
        JexlScript inc = JEXL.createScript("++x");
        JexlContext jc = new MapContext();
        AtomicInteger value = new AtomicInteger(1);
        jc.set("x", value);
        Object o = inc.execute(jc);
        Assert.assertEquals("Result is not 2", value, o);
    }

    @Test
    public void testDecrement() throws Exception {
        JexlScript dec = JEXL.createScript("var x = 43; --x");
        JexlContext jc = new MapContext();
        Object o = dec.execute(jc);
        Assert.assertEquals("Result is not 42", new Integer(42), o);
    }

    @Test
    public void testDecrementPost() throws Exception {
        JexlScript dec = JEXL.createScript("var x = 43; --x; x");
        JexlContext jc = new MapContext();
        Object o = dec.execute(jc);
        Assert.assertEquals("Result is not 42", new Integer(42), o);
    }

    @Test
    public void testDecrementOverride() throws Exception {
        JexlScript inc = JEXL.createScript("--x");
        JexlContext jc = new MapContext();
        AtomicInteger value = new AtomicInteger(43);
        jc.set("x", value);
        Object o = inc.execute(jc);
        Assert.assertEquals("Result is not 42", value, o);
    }

    @Test
    public void testIncrementPostfix() throws Exception {
        JexlScript inc = JEXL.createScript("x = 1; x++");
        JexlContext jc = new MapContext();
        Object o = inc.execute(jc);
        Assert.assertEquals("Variable is not 2", new Integer(2), jc.get("x"));
        Assert.assertEquals("Result is not 1", new Integer(1), o);
    }

    @Test
    public void testDecrementPostfix() throws Exception {
        JexlScript dec = JEXL.createScript("x = 43; x--");
        JexlContext jc = new MapContext();
        Object o = dec.execute(jc);
        Assert.assertEquals("Variable is not 42", new Integer(42), jc.get("x"));
        Assert.assertEquals("Result is not 43", new Integer(43), o);
    }

}