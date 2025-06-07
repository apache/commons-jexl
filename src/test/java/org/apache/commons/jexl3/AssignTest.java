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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void testAmbiguous() {
        final JexlExpression assign = JEXL.createExpression("froboz.nosuchbean = 10");
        final JexlContext jc = new MapContext();
        final Froboz froboz = new Froboz(-169);
        jc.set("froboz", froboz);
        final RuntimeException xrt = assertThrows(RuntimeException.class, () -> assign.evaluate(jc));
        assertTrue(xrt.toString().contains("nosuchbean"));
    }

    /**
     * Make sure bean assignment works
     */
    @Test
    void testAntish() {
        final JexlExpression assign = JEXL.createExpression("froboz.value = 10");
        final JexlExpression check = JEXL.createExpression("froboz.value");
        final JexlContext jc = new MapContext();
        Object o = assign.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
        o = check.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
    }

    @Test
    void testAntishInteger() {
        final JexlExpression assign = JEXL.createExpression("froboz.0 = 10");
        final JexlExpression check = JEXL.createExpression("froboz.0");
        final JexlContext jc = new MapContext();
        Object o = assign.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
        o = check.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);
    }

    @Test
    void testArray() {
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
    void testBeanish() {
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
    void testGetInError1() {
        final JexlException.Property e = assertThrows(JexlException.Property.class, () -> JEXL.getProperty("the_x_value", "y"));
        assertEquals("y", e.getProperty());
        assertThrows(JexlException.class, () -> JEXL.getProperty(null, "y"));
    }

    @Test
    void testMini() {
        final JexlContext jc = new MapContext();
        final JexlExpression assign = JEXL.createExpression("quux = 10");
        final Object o = assign.evaluate(jc);
        assertEquals(Integer.valueOf(10), o);

    }

    @Test
    void testMore() {
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
    void testPropertyInError0() {
        for (final String op : Arrays.asList(" = ", "+= ", " -= ", " *= ", " /= ", " %= ", " &= ", " |= ", " ^= ", " <<= ", " >>= ", " >>>= ")) {
            final JexlScript script = JEXL.createScript("x -> x.y " + op + "42");
            final JexlException.Property xprop = assertThrows(JexlException.Property.class, () -> script.execute(null, "the_x_value"));
            assertEquals("y", xprop.getProperty());
        }
        final JexlScript script = JEXL.createScript("x -> x.y ");
        final JexlException.Property xprop = assertThrows(JexlException.Property.class, () -> script.execute(null, "the_x_value"));
        assertEquals("y", xprop.getProperty());
    }

    @Test
    void testRejectLocal() {
        final JexlContext jc = new MapContext();
        final JexlScript assign = JEXL.createScript("var quux = null; quux.froboz.value = 10");
        assertNotNull(assertThrows(JexlException.class, () -> assign.execute(jc)).toString());
        // quux is a global antish var
        final JexlScript assign2 = JEXL.createScript("quux.froboz.value = 10");
        final Object o = assign2.execute(jc);
        assertEquals(10, o);
    }

    @Test
    void testSetInError1() {
        final JexlException.Property xprop = assertThrows(JexlException.Property.class, () -> JEXL.setProperty("the_x_value", "y", 42));
        assertEquals("y", xprop.getProperty());
        assertThrows(JexlException.Property.class, () -> JEXL.setProperty(null, "y", 42));
    }
    @Test
    void testUtil() {
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
