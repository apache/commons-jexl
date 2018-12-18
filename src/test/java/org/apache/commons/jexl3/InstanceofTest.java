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
 * Test cases for the instanceof operator.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class InstanceofTest extends JexlTestCase {
    public InstanceofTest() {
        super("InstanceofTest");
    }

    @Test
    public void testSimpleName() throws Exception {
        JexlScript e = JEXL.createScript("var x = '123'; x instanceof String");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testArray() throws Exception {
        JexlScript e = JEXL.createScript("var x = ['123','456']; x instanceof String[]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testPrimitiveArray() throws Exception {
        JexlScript e = JEXL.createScript("var x = [123,456]; x instanceof int[]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);

        e = JEXL.createScript("var x = [123,456]; not (x instanceof int)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testQualifiedName() throws Exception {
        JexlScript e = JEXL.createScript("var x = new('java.util.concurrent.atomic.AtomicLong'); x instanceof java.util.concurrent.atomic.AtomicLong");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testQualifiedArray() throws Exception {
        JexlScript e = JEXL.createScript("var a = new('java.util.concurrent.atomic.AtomicLong'); var x = [a,a,a]; x instanceof java.util.concurrent.atomic.AtomicLong[]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testPrimitive() throws Exception {
        JexlScript e = JEXL.createScript("var x = 123; x instanceof int");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is true", Boolean.FALSE, o);
    }

    @Test
    public void testNull() throws Exception {
        JexlScript e = JEXL.createScript("var x = null; x instanceof Object");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is true", Boolean.FALSE, o);
    }

    @Test
    public void testDefaultImports() throws Exception {
        JexlScript e = JEXL.createScript("var x = {1,2,3}; x instanceof Set");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);

        e = JEXL.createScript("var x = 100H; x instanceof BigInteger");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);

        e = JEXL.createScript("var x = 100.1B; x instanceof BigDecimal");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testUntypedArray() throws Exception {
        JexlScript e = JEXL.createScript("var x = [123,'456']; x instanceof []");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);

        e = JEXL.createScript("var x = 100H; x instanceof []");
        o = e.execute(jc);
        Assert.assertEquals("Result is true", Boolean.FALSE, o);
    }

    @Test
    public void testMultidimensionalArray() throws Exception {
        JexlScript e = JEXL.createScript("x instanceof []");
        JexlContext jc = new MapContext();
        jc.set("x", new int[5][6]);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);

        e = JEXL.createScript("x instanceof [][]");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);

        e = JEXL.createScript("x instanceof int[][]");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);

        e = JEXL.createScript("x instanceof long[][]");
        o = e.execute(jc);
        Assert.assertEquals("Result is true", Boolean.FALSE, o);

        e = JEXL.createScript("x instanceof [][][]");
        o = e.execute(jc);
        Assert.assertEquals("Result is true", Boolean.FALSE, o);
    }

}
