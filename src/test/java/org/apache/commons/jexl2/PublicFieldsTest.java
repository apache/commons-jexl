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
 * Tests public field set/get.
 */
public class PublicFieldsTest extends JexlTestCase {
    // some constants
    private static final String LOWER42 = "fourty-two";
    private static final String UPPER42 = "FOURTY-TWO";
    /**
     * An Inner class.
     */
    public static class Inner {
        public double aDouble = 42.0;
    }

    /**
     * A Struct, all fields public
     */
    public static class Struct {
        public Inner inner = new Inner();
        public int anInt = 42;
        public String aString = LOWER42;
    }

    // a pub instance
    private Struct pub;
    // the JexlContext to use
    private JexlContext ctxt;

    public PublicFieldsTest() {
        super(createEngine(false));
    }

    @Override
    public void setUp() {
        pub = new Struct();
        ctxt = new MapContext();
        ctxt.set("pub", pub);
    }

    public void testGetInt() throws Exception {
        Expression get = JEXL.createExpression("pub.anInt");
        assertEquals(42, get.evaluate(ctxt));
        JEXL.setProperty(pub, "anInt", -42);
        assertEquals(-42, get.evaluate(ctxt));
    }

    public void testSetInt() throws Exception {
        Expression set = JEXL.createExpression("pub.anInt = value");
        ctxt.set("value", -42);
        assertEquals(-42, set.evaluate(ctxt));
        assertEquals(-42, JEXL.getProperty(pub, "anInt"));
        ctxt.set("value", 42);
        assertEquals(42, set.evaluate(ctxt));
        assertEquals(42, JEXL.getProperty(pub, "anInt"));
        try {
            ctxt.set("value", UPPER42);
            assertEquals(null, set.evaluate(ctxt));
            fail("should have thrown");
        } catch(JexlException xjexl) {}
    }

    public void testGetString() throws Exception {
        Expression get = JEXL.createExpression("pub.aString");
        assertEquals(LOWER42, get.evaluate(ctxt));
        JEXL.setProperty(pub, "aString", UPPER42);
        assertEquals(UPPER42, get.evaluate(ctxt));
    }

    public void testSetString() throws Exception {
        Expression set = JEXL.createExpression("pub.aString = value");
        ctxt.set("value", UPPER42);
        assertEquals(UPPER42, set.evaluate(ctxt));
        assertEquals(UPPER42, JEXL.getProperty(pub, "aString"));
        ctxt.set("value", LOWER42);
        assertEquals(LOWER42, set.evaluate(ctxt));
        assertEquals(LOWER42, JEXL.getProperty(pub, "aString"));
    }

    public void testGetInnerDouble() throws Exception {
        Expression get = JEXL.createExpression("pub.inner.aDouble");
        assertEquals(42.0, get.evaluate(ctxt));
        JEXL.setProperty(pub, "inner.aDouble", -42);
        assertEquals(-42.0, get.evaluate(ctxt));
    }

    public void testSetInnerDouble() throws Exception {
        Expression set = JEXL.createExpression("pub.inner.aDouble = value");
        ctxt.set("value", -42.0);
        assertEquals(-42.0, set.evaluate(ctxt));
        assertEquals(-42.0, JEXL.getProperty(pub, "inner.aDouble"));
        ctxt.set("value", 42.0);
        assertEquals(42.0, set.evaluate(ctxt));
        assertEquals(42.0, JEXL.getProperty(pub, "inner.aDouble"));
        try {
            ctxt.set("value", UPPER42);
            assertEquals(null, set.evaluate(ctxt));
            fail("should have thrown");
        } catch(JexlException xjexl) {}
    }

}
