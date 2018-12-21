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
 * Test cases for the cast operator.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class CastTest extends JexlTestCase {

    public CastTest() {
        super("CastTest");
    }

    @Test
    public void testTrueBoolean() throws Exception {
        String[] scripts = {"(boolean)b", "(boolean)s", "(boolean)c", "(boolean)1", "(boolean)1L", "(boolean)'true'", "(boolean)1H", "(boolean)1.0B", "(boolean)1f", "(boolean)1d"};
        JexlContext jc = new MapContext();
        jc.set("b",(byte) 1);
        jc.set("s",(short) 1);
        jc.set("c",'t');
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Boolean);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o);
        }
    }

    @Test
    public void testFalseBoolean() throws Exception {
        String[] scripts = {"(boolean)b", "(boolean)s", "(boolean)c", "(boolean)0", "(boolean)0L", "(boolean)'false'", "(boolean)0H", "(boolean)0.0B", "(boolean)0f", "(boolean)0d"};
        JexlContext jc = new MapContext();
        jc.set("b",(byte) 0);
        jc.set("s",(short) 0);
        jc.set("c",'\0');
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Boolean);
           Assert.assertEquals("Result is not true", Boolean.FALSE, o);
        }
    }

    @Test
    public void testChar() throws Exception {
        String[] scripts = {"(char)b", "(char)s", "(char)65", "(char)65L", "(char)'ABC'", "(char)65H", "(char)65.0B", "(char)65f", "(char)65d"};
        JexlContext jc = new MapContext();
        jc.set("b",(byte) 65);
        jc.set("s",(short) 65);
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Character);
           Assert.assertEquals("Result is not true", 'A', o);
        }
    }

    @Test
    public void testByte() throws Exception {
        String[] scripts = {"(byte)b", "(byte)s", "(byte)42", "(byte)42L", "(byte)c", "(byte)42H", "(byte)42.0B", "(byte)42f", "(byte)42d"};
        JexlContext jc = new MapContext();
        jc.set("c", '*');
        jc.set("b",(byte) 42);
        jc.set("s",(short) 42);
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Byte);
           Assert.assertEquals("Result is not true", (byte) 42, o);
        }
    }

    @Test
    public void testShort() throws Exception {
        String[] scripts = {"(short)b", "(short)s", "(short)42", "(short)42L", "(short)c", "(short)42H", "(short)42.0B", "(short)42f", "(short)42d"};
        JexlContext jc = new MapContext();
        jc.set("c", '*');
        jc.set("b",(byte) 42);
        jc.set("s",(short) 42);
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Short);
           Assert.assertEquals("Result is not true", (short) 42, o);
        }
    }

    @Test
    public void testInt() throws Exception {
        String[] scripts = {"(int)b", "(int)s", "(int)42", "(int)42L", "(int)c", "(int)42H", "(int)42.0B", "(int)42f", "(int)42d"};
        JexlContext jc = new MapContext();
        jc.set("c", '*');
        jc.set("b",(byte) 42);
        jc.set("s",(short) 42);
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Integer);
           Assert.assertEquals("Result is not true", 42, o);
        }
    }

    @Test
    public void testLong() throws Exception {
        String[] scripts = {"(long)b", "(long)s", "(long)42", "(long)42L", "(long)c", "(long)42H", "(long)42.0B", "(long)42f", "(long)42d"};
        JexlContext jc = new MapContext();
        jc.set("c", '*');
        jc.set("b",(byte) 42);
        jc.set("s",(short) 42);
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
           Assert.assertEquals("Result is not true", 42L, o);
        }
    }

    @Test
    public void testFloat() throws Exception {
        String[] scripts = {"(float)b", "(float)s", "(float)42", "(float)42L", "(float)c", "(float)42H", "(float)42.0B", "(float)42f", "(float)42d"};
        JexlContext jc = new MapContext();
        jc.set("c", '*');
        jc.set("b",(byte) 42);
        jc.set("s",(short) 42);
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
           Assert.assertEquals("Result is not true", 42.f, o);
        }
    }

    @Test
    public void testDouble() throws Exception {
        String[] scripts = {"(double)b", "(double)s", "(double)42", "(double)42L", "(double)c", "(double)42H", "(double)42.0B", "(double)42f", "(double)42d"};
        JexlContext jc = new MapContext();
        jc.set("c", '*');
        jc.set("b",(byte) 42);
        jc.set("s",(short) 42);
        for (String s : scripts) {
           JexlScript e = JEXL.createScript(s);
           Object o = e.execute(jc);
           Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
           Assert.assertEquals("Result is not true", 42., o);
        }
    }

    @Test
    public void testNull() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic(false)).create();
        JexlContext jc = new MapContext();
        JexlScript e = jexl.createScript("(int)null");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Integer);
        Assert.assertEquals("Result is not true", 0, o);

        e = jexl.createScript("(long)null");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Long);
        Assert.assertEquals("Result is not true", 0L, o);

        e = jexl.createScript("(short)null");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Short);
        Assert.assertEquals("Result is not true", (short) 0, o);

        e = jexl.createScript("(byte)null");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Byte);
        Assert.assertEquals("Result is not true", (byte) 0, o);

        e = jexl.createScript("(float)null");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Float);
        Assert.assertEquals("Result is not true", 0.f, o);

        e = jexl.createScript("(double)null");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Double);
        Assert.assertEquals("Result is not true", 0., o);

        e = jexl.createScript("(char)null");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Character);
        Assert.assertEquals("Result is not true", '\0', o);

        e = jexl.createScript("(boolean)null");
        o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o instanceof Boolean);
        Assert.assertEquals("Result is not true", Boolean.FALSE, o);

    }

}
