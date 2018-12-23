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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the primitive variables.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class PrimitivesTest extends JexlTestCase {

    JexlEngine jexl = new JexlBuilder().strict(false).arithmetic(new JexlArithmetic(false)).create();

    public PrimitivesTest() {
        super("PrimitivesTest");
    }

    @Test
    public void testUninitializedDeclaration() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("boolean x");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Boolean);
        Assert.assertEquals("Result is not true", Boolean.FALSE, o);

        e = jexl.createScript("char x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Character);
        Assert.assertEquals("Result is not true", '\0', o);

        e = jexl.createScript("byte x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Byte);
        Assert.assertEquals("Result is not true", (byte) 0, o);

        e = jexl.createScript("short x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Short);
        Assert.assertEquals("Result is not true", (short) 0, o);

        e = jexl.createScript("int x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Integer);
        Assert.assertEquals("Result is not true", 0, o);

        e = jexl.createScript("long x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
        Assert.assertEquals("Result is not true", 0L, o);

        e = jexl.createScript("float x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 0.f, o);

        e = jexl.createScript("double x");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 0., o);
    }

    @Test
    public void testBoolean() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("boolean x = 1");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Boolean);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);

        e = jexl.createScript("boolean x = 0");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Boolean);
        Assert.assertEquals("Result is not true", Boolean.FALSE, o);
    }

    @Test
    public void testCharacter() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("char x = 42");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Character);
        Assert.assertEquals("Result is not true", '*', o);

        e = jexl.createScript("char x = '*'");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Character);
        Assert.assertEquals("Result is not true", '*', o);
    }

    @Test
    public void testByte() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("byte x = 42");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Byte);
        Assert.assertEquals("Result is not true", (byte) 42, o);

        e = jexl.createScript("byte x = '42'");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Byte);
        Assert.assertEquals("Result is not true", (byte) 42, o);
    }

    @Test
    public void testShort() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("short x = 42");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Short);
        Assert.assertEquals("Result is not true", (short) 42, o);

        e = jexl.createScript("short x = '42'");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Short);
        Assert.assertEquals("Result is not true", (short) 42, o);
    }

    @Test
    public void testInteger() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("int x = 42.0");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Integer);
        Assert.assertEquals("Result is not true", 42, o);

        e = jexl.createScript("int x = '42'");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Integer);
        Assert.assertEquals("Result is not true", 42, o);
    }

    @Test
    public void testLong() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("long x = 42.0");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
        Assert.assertEquals("Result is not true", 42L, o);

        e = jexl.createScript("long x = '42'");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
        Assert.assertEquals("Result is not true", 42L, o);
    }

    @Test
    public void testFloat() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("float x = 42");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 42.f, o);

        e = jexl.createScript("float x = '42'");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 42.f, o);
    }

    @Test
    public void testDouble() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("double x = 42");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);

        e = jexl.createScript("double x = '42'");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);
    }

    @Test
    public void testHoisted() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("double x = 0; var y = () -> {x = 42}; y()");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);
    }

    @Test
    public void testStrictLong() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("s", (short) 42);
        jc.set("b", (byte) 42);
        JexlScript e = JEXL.createScript("long x = 42L");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
        Assert.assertEquals("Result is not true", 42L, o);

        e = JEXL.createScript("long x = 42");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
        Assert.assertEquals("Result is not true", 42L, o);

        e = JEXL.createScript("long x = b");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
        Assert.assertEquals("Result is not true", 42L, o);

        e = JEXL.createScript("long x = s");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
        Assert.assertEquals("Result is not true", 42L, o);
    }

    @Test
    public void testStrictInt() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("s", (short) 42);
        jc.set("b", (byte) 42);
        JexlScript e = JEXL.createScript("int x = 42");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Integer);
        Assert.assertEquals("Result is not true", 42, o);

        e = JEXL.createScript("int x = s");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Integer);
        Assert.assertEquals("Result is not true", 42, o);

        e = JEXL.createScript("int x = b");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Integer);
        Assert.assertEquals("Result is not true", 42, o);
    }

    @Test
    public void testStrictFloat() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("s", (short) 42);
        jc.set("b", (byte) 42);
        jc.set("f", 42.f);
        JexlScript e = JEXL.createScript("float x = 42L");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 42.f, o);

        e = JEXL.createScript("float x = f");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 42.f, o);

        e = JEXL.createScript("float x = 42");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 42.f, o);

        e = JEXL.createScript("float x = b");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 42.f, o);

        e = JEXL.createScript("float x = s");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 42.f, o);
    }

    @Test
    public void testStrictDouble() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("s", (short) 42);
        jc.set("b", (byte) 42);
        jc.set("f", 42.f);
        JexlScript e = JEXL.createScript("double x = 42.0");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);

        e = JEXL.createScript("double x = f");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);

        e = JEXL.createScript("double x = 42L");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);

        e = JEXL.createScript("double x = 42");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);

        e = JEXL.createScript("double x = b");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);

        e = JEXL.createScript("double x = s");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 42., o);
    }

    @Test
    public void testStrictChar() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = JEXL.createScript("char x = '*'");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Character);
        Assert.assertEquals("Result is not true", '*', o);
    }

}
