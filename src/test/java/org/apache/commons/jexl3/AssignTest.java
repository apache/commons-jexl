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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Test cases for assignment.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class AssignTest extends JexlTestCase {

    public static class Froboz {
        int value;
        public Froboz(final int v) {
            value = v;
        }
        public int getValue() {
            return value;
        }
        public void setValue(final int v) {
            value = v;
        }
    }

    public static class Quux {
        String str;
        Froboz froboz;
        public Quux(final String str, final int fro) {
            this.str = str;
            froboz = new Froboz(fro);
        }

        public Froboz getFroboz() {
            return froboz;
        }

        public String getStr() {
            return str;
        }

        public void setFroboz(final Froboz froboz) {
            this.froboz = froboz;
        }

        public void setStr(final String str) {
            this.str = str;
        }
    }

    public AssignTest() {
        super("AssignTest", new JexlBuilder().cache(512).strict(true).silent(false).create());
    }

    @Test
    public void testAmbiguous() {
        final JexlExpression assign = JEXL.createExpression("froboz.nosuchbean = 10");
        final JexlContext jc = new MapContext();
        final Froboz froboz = new Froboz(-169);
        jc.set("froboz", froboz);
        RuntimeException xrt = assertThrows(RuntimeException.class, () -> assign.evaluate(jc));
        assertTrue(xrt.toString().contains("nosuchbean"));
    }

    /**
     * Make sure bean assignment works
     */
    @Test
    public void testAntish() {
        final JexlExpression assign = JEXL.createExpression("froboz.value = 10");
        final JexlExpression check = JEXL.createExpression("froboz.value");
        final JexlContext jc = new MapContext();
        Object o = assign.evaluate(jc);
        assertEquals( Integer.valueOf(10), o);
        o = check.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
    }

    @Test
    public void testAntishInteger() {
        final JexlExpression assign = JEXL.createExpression("froboz.0 = 10");
        final JexlExpression check = JEXL.createExpression("froboz.0");
        final JexlContext jc = new MapContext();
        Object o = assign.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
        o = check.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
    }

    @Test
    public void testArray() {
        final JexlExpression assign = JEXL.createExpression("froboz[\"value\"] = 10");
        final JexlExpression check = JEXL.createExpression("froboz[\"value\"]");
        final JexlContext jc = new MapContext();
        final Froboz froboz = new Froboz(0);
        jc.set("froboz", froboz);
        Object o = assign.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
        o = check.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
    }

    @Test
    public void testBeanish() {
        final JexlExpression assign = JEXL.createExpression("froboz.value = 10");
        final JexlExpression check = JEXL.createExpression("froboz.value");
        final JexlContext jc = new MapContext();
        final Froboz froboz = new Froboz(-169);
        jc.set("froboz", froboz);
        Object o = assign.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
        o = check.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
    }

    @Test
    public void testGetInError1() {
        try {
            JEXL.getProperty("the_x_value", "y");
        } catch (final JexlException.Property xprop) {
            assertEquals("y", xprop.getProperty());
        }
        try {
            JEXL.getProperty(null, "y");
        } catch (final JexlException xprop) {
            //
        }
    }

    @Test
    public void testMini() {
        final JexlContext jc = new MapContext();
        final JexlExpression assign = JEXL.createExpression("quux = 10");
        final Object o = assign.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);

    }

    @Test
    public void testMore() {
        final JexlContext jc = new MapContext();
        jc.set("quuxClass", Quux.class);
        final JexlExpression create = JEXL.createExpression("quux = new(quuxClass, 'xuuq', 100)");
        final JexlExpression assign = JEXL.createExpression("quux.froboz.value = 10");
        final JexlExpression check = JEXL.createExpression("quux[\"froboz\"].value");

        final Quux quux = (Quux) create.evaluate(jc);
        assertNotNull(quux, "quux is null");
        Object o = assign.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
        o = check.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
    }

    @Test
    public void testPropertyInError0() {
        JexlScript script;
        for(final String op : Arrays.asList(
                " = ", "+= ", " -= "," *= "," /= "," %= ",
                " &= ", " |= ", " ^= ",
                " <<= ", " >>= ", " >>>= ")) {
            script = JEXL.createScript("x -> x.y " +op+ "42");
            try {
                script.execute(null, "the_x_value");
            } catch (final JexlException.Property xprop) {
                assertEquals("y", xprop.getProperty());
            }
        }
        script = JEXL.createScript("x -> x.y ");
        try {
            script.execute(null, "the_x_value");
        } catch (final JexlException.Property xprop) {
            assertEquals("y", xprop.getProperty());
        }
    }

    @Test
    public void testRejectLocal() {
        final JexlContext jc = new MapContext();
        JexlScript assign = JEXL.createScript("var quux = null; quux.froboz.value = 10");
        try {
            final Object o = assign.execute(jc);
            fail("quux is local and null, should fail");
        } catch (final JexlException xjexl) {
            final String x = xjexl.toString();
            final String y = x;
        }
        // quux is a global antish var
        assign = JEXL.createScript("quux.froboz.value = 10");
        final Object o = assign.execute(jc);
        assertEquals(10, o);
    }

    @Test
    public void testSetInError1() {
        try {
            JEXL.setProperty("the_x_value", "y", 42);
        } catch (final JexlException.Property xprop) {
            assertEquals("y", xprop.getProperty());
        }
        try {
            JEXL.setProperty(null, "y", 42);
        } catch (final JexlException xprop) {
            //
        }
    }
    @Test
    public void testUtil() {
        final Quux quux = JEXL.newInstance(Quux.class, "xuuq", Integer.valueOf(100));
        assertNotNull(quux);
        JEXL.setProperty(quux, "froboz.value", Integer.valueOf(100));
        Object o = JEXL.getProperty(quux, "froboz.value");
        assertEquals(Integer.valueOf(100), o);
        JEXL.setProperty(quux, "['froboz'].value", Integer.valueOf(1000));
        o = JEXL.getProperty(quux, "['froboz']['value']");
        assertEquals(Integer.valueOf(1000), o);
    }
}
