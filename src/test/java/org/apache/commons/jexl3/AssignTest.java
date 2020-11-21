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
 * Test cases for assignment.
 *
 * @since 1.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class AssignTest extends JexlTestCase {

    public static class Froboz {
        int value;
        public Froboz(final int v) {
            value = v;
        }
        public void setValue(final int v) {
            value = v;
        }
        public int getValue() {
            return value;
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

        public void setFroboz(final Froboz froboz) {
            this.froboz = froboz;
        }

        public String getStr() {
            return str;
        }

        public void setStr(final String str) {
            this.str = str;
        }
    }

    public AssignTest() {
        super("AssignTest", new JexlBuilder().cache(512).strict(true).silent(false).create());
    }

    /**
     * Make sure bean assignment works
     *
     * @throws Exception on any error
     */
    @Test
    public void testAntish() throws Exception {
        final JexlExpression assign = JEXL.createExpression("froboz.value = 10");
        final JexlExpression check = JEXL.createExpression("froboz.value");
        final JexlContext jc = new MapContext();
        Object o = assign.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
    }

    @Test
    public void testAntishInteger() throws Exception {
        final JexlExpression assign = JEXL.createExpression("froboz.0 = 10");
        final JexlExpression check = JEXL.createExpression("froboz.0");
        final JexlContext jc = new MapContext();
        Object o = assign.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
    }

    @Test
    public void testBeanish() throws Exception {
        final JexlExpression assign = JEXL.createExpression("froboz.value = 10");
        final JexlExpression check = JEXL.createExpression("froboz.value");
        final JexlContext jc = new MapContext();
        final Froboz froboz = new Froboz(-169);
        jc.set("froboz", froboz);
        Object o = assign.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
    }

    @Test
    public void testAmbiguous() throws Exception {
        final JexlExpression assign = JEXL.createExpression("froboz.nosuchbean = 10");
        final JexlContext jc = new MapContext();
        final Froboz froboz = new Froboz(-169);
        jc.set("froboz", froboz);
        Object o = null;
        try {
            o = assign.evaluate(jc);
        }
        catch(final RuntimeException xrt) {
            final String str = xrt.toString();
            Assert.assertTrue(str.contains("nosuchbean"));
        }
        finally {
            Assert.assertNull("Should have failed", o);
        }
    }

    @Test
    public void testArray() throws Exception {
        final JexlExpression assign = JEXL.createExpression("froboz[\"value\"] = 10");
        final JexlExpression check = JEXL.createExpression("froboz[\"value\"]");
        final JexlContext jc = new MapContext();
        final Froboz froboz = new Froboz(0);
        jc.set("froboz", froboz);
        Object o = assign.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
    }

    @Test
    public void testMini() throws Exception {
        final JexlContext jc = new MapContext();
        final JexlExpression assign = JEXL.createExpression("quux = 10");
        final Object o = assign.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);

    }

    @Test
    public void testMore() throws Exception {
        final JexlContext jc = new MapContext();
        jc.set("quuxClass", Quux.class);
        final JexlExpression create = JEXL.createExpression("quux = new(quuxClass, 'xuuq', 100)");
        final JexlExpression assign = JEXL.createExpression("quux.froboz.value = 10");
        final JexlExpression check = JEXL.createExpression("quux[\"froboz\"].value");

        final Quux quux = (Quux) create.evaluate(jc);
        Assert.assertNotNull("quux is null", quux);
        Object o = assign.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        Assert.assertEquals("Result is not 10", new Integer(10), o);
    }

    @Test
    public void testUtil() throws Exception {
        final Quux quux = JEXL.newInstance(Quux.class, "xuuq", Integer.valueOf(100));
        Assert.assertNotNull(quux);
        JEXL.setProperty(quux, "froboz.value", Integer.valueOf(100));
        Object o = JEXL.getProperty(quux, "froboz.value");
        Assert.assertEquals("Result is not 100", new Integer(100), o);
        JEXL.setProperty(quux, "['froboz'].value", Integer.valueOf(1000));
        o = JEXL.getProperty(quux, "['froboz']['value']");
        Assert.assertEquals("Result is not 1000", new Integer(1000), o);
    }

    @Test
    public void testRejectLocal() throws Exception {
        final JexlContext jc = new MapContext();
        JexlScript assign = JEXL.createScript("var quux = null; quux.froboz.value = 10");
        try {
            final Object o = assign.execute(jc);
            Assert.fail("quux is local and null, should fail");
        } catch (final JexlException xjexl) {
            final String x = xjexl.toString();
            final String y = x;
        }
        // quux is a global antish var
        assign = JEXL.createScript("quux.froboz.value = 10");
        final Object o = assign.execute(jc);
        Assert.assertEquals(10, o);
    }
}