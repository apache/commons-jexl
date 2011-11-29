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
package org.apache.commons.jexl2;

/**
 * Test cases for assignment.
 * 
 * @since 1.1
 */
public class AssignTest extends JexlTestCase {
    private static final JexlArithmetic ARITH = new JexlArithmetic(false);
    private static final JexlEngine ENGINE = new JexlEngine(null, ARITH, null, null);
    static {
        ENGINE.setSilent(false);
    }
    
    public static class Froboz {
        int value;
        public Froboz(int v) {
            value = v;
        }
        public void setValue(int v) {
            value = v;
        }
        public int getValue() {
            return value;
        }
    }
    
    public static class Quux {
        String str;
        Froboz froboz;
        public Quux(String str, int fro) {
            this.str = str;
            froboz = new Froboz(fro);
        }
        
        public Froboz getFroboz() {
            return froboz;
        }
        
        public void setFroboz(Froboz froboz) {
            this.froboz = froboz;
        }
        
        public String getStr() {
            return str;
        }
        
        public void setStr(String str) {
            this.str = str;
        }
    }

    public AssignTest(String testName) {
        super(testName);
    }

    /**
     * Make sure bean assignment works
     * 
     * @throws Exception on any error
     */
    public void testAntish() throws Exception {
        Expression assign = ENGINE.createExpression("froboz.value = 10");
        Expression check = ENGINE.createExpression("froboz.value");
        JexlContext jc = new MapContext();
        Object o = assign.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
    }

    public void testAntishInteger() throws Exception {
        Expression assign = ENGINE.createExpression("froboz.0 = 10");
        Expression check = ENGINE.createExpression("froboz.0");
        JexlContext jc = new MapContext();
        Object o = assign.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
    }
    
    public void testBeanish() throws Exception {
        Expression assign = ENGINE.createExpression("froboz.value = 10");
        Expression check = ENGINE.createExpression("froboz.value");
        JexlContext jc = new MapContext();
        Froboz froboz = new Froboz(-169);
        jc.set("froboz", froboz);
        Object o = assign.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
    }
    
    public void testAmbiguous() throws Exception {
        Expression assign = ENGINE.createExpression("froboz.nosuchbean = 10");
        JexlContext jc = new MapContext();
        Froboz froboz = new Froboz(-169);
        jc.set("froboz", froboz);
        Object o = null;
        try {
            o = assign.evaluate(jc);
        }
        catch(RuntimeException xrt) {
            String str = xrt.toString();
            assertTrue(str.contains("nosuchbean"));
            return;
        }
        finally {
            assertEquals("Should have failed", null, o);
        }
    }
        
    public void testArray() throws Exception {
        Expression assign = ENGINE.createExpression("froboz[\"value\"] = 10");
        Expression check = ENGINE.createExpression("froboz[\"value\"]");
        JexlContext jc = new MapContext();
        Froboz froboz = new Froboz(0);
        jc.set("froboz", froboz);
        Object o = assign.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
    }
    
    public void testMore() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("quuxClass", Quux.class);
        Expression create = ENGINE.createExpression("quux = new(quuxClass, 'xuuq', 100)");
        Expression assign = ENGINE.createExpression("quux.froboz.value = 10");
        Expression check = ENGINE.createExpression("quux[\"froboz\"].value");

        Quux quux = (Quux) create.evaluate(jc);
        assertNotNull("quux is null", quux);
        Object o = assign.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
    }

    public void testUtil() throws Exception {
        Quux quux = ENGINE.newInstance(Quux.class, "xuuq", Integer.valueOf(100));
        ENGINE.setProperty(quux, "froboz.value", Integer.valueOf(100));
        Object o = ENGINE.getProperty(quux, "froboz.value");
        assertEquals("Result is not 100", new Integer(100), o);
        ENGINE.setProperty(quux, "['froboz'].value", Integer.valueOf(1000));
        o = ENGINE.getProperty(quux, "['froboz']['value']");
        assertEquals("Result is not 1000", new Integer(1000), o);
    }


}