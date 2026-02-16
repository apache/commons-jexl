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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests public field set/get.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
class PublicFieldsTest extends JexlTestCase {
    public enum Gender { MALE, FEMALE }

    /**
     * An Inner class.
     */
    public static class Inner {
        public static double NOT42 = -42.0;
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

    // some constants
    private static final String LOWER42 = "fourty-two";

    private static final String UPPER42 = "FOURTY-TWO";
    // a pub instance
    private Struct pub;

    // the JexlContext to use
    private JexlContext ctxt;

    public PublicFieldsTest() {
        super("PublicFieldsTest");
    }

    @BeforeEach
    @Override
    public void setUp() {
        pub = new Struct();
        ctxt = new MapContext();
        ctxt.set("pub", pub);
    }

    @Test
    void testGetEnum() throws Exception {
        ctxt.set("com.jexl.gender", Gender.class);
        final String src = "x = com.jexl.gender.FEMALE";
        final JexlScript script = JEXL.createScript(src);
        final Object result = script.execute(ctxt);
        assertEquals(Gender.FEMALE, result);
        assertEquals(Gender.FEMALE, ctxt.get("x"));
    }

    @Test
    void testGetInnerDouble() throws Exception {
        final JexlExpression get = JEXL.createExpression("pub.inner.aDouble");
        assertEquals(42.0, get.evaluate(ctxt));
        JEXL.setProperty(pub, "inner.aDouble", -42);
        assertEquals(-42.0, get.evaluate(ctxt));
    }

    @Test
    void testGetInt() throws Exception {
        final JexlExpression get = JEXL.createExpression("pub.anInt");
        assertEquals(42, get.evaluate(ctxt));
        JEXL.setProperty(pub, "anInt", -42);
        assertEquals(-42, get.evaluate(ctxt));
    }

    @Test
    void testGetStaticField() throws Exception {
        ctxt.set("com.jexl", Inner.class);
        final String src = "x = com.jexl.NOT42";
        final JexlScript script = JEXL.createScript(src);
        final Object result = script.execute(ctxt);
        assertEquals(Inner.NOT42, result);
        assertEquals(Inner.NOT42, ctxt.get("x"));
    }

    @Test
    void testGetString() throws Exception {
        final JexlExpression get = JEXL.createExpression("pub.aString");
        assertEquals(LOWER42, get.evaluate(ctxt));
        JEXL.setProperty(pub, "aString", UPPER42);
        assertEquals(UPPER42, get.evaluate(ctxt));
    }

    @Test
    void testSetInnerDouble() throws Exception {
        final JexlExpression set = JEXL.createExpression("pub.inner.aDouble = value");
        ctxt.set("value", -42.0);
        assertEquals(-42.0, set.evaluate(ctxt));
        assertEquals(-42.0, JEXL.getProperty(pub, "inner.aDouble"));
        ctxt.set("value", 42.0);
        assertEquals(42.0, set.evaluate(ctxt));
        assertEquals(42.0, JEXL.getProperty(pub, "inner.aDouble"));
        assertThrows(JexlException.class, () -> {
            ctxt.set("value", UPPER42);
            assertNull(set.evaluate(ctxt));
        });
    }

    @Test
    void testSetInt() throws Exception {
        final JexlExpression set = JEXL.createExpression("pub.anInt = value");
        ctxt.set("value", -42);
        assertEquals(-42, set.evaluate(ctxt));
        assertEquals(-42, JEXL.getProperty(pub, "anInt"));
        ctxt.set("value", 42);
        assertEquals(42, set.evaluate(ctxt));
        assertEquals(42, JEXL.getProperty(pub, "anInt"));
        assertThrows(JexlException.class, () -> {
            ctxt.set("value", UPPER42);
            assertNull(set.evaluate(ctxt));
        });
    }

    @Test
    void testSetString() throws Exception {
        final JexlExpression set = JEXL.createExpression("pub.aString = value");
        ctxt.set("value", UPPER42);
        assertEquals(UPPER42, set.evaluate(ctxt));
        assertEquals(UPPER42, JEXL.getProperty(pub, "aString"));
        ctxt.set("value", LOWER42);
        assertEquals(LOWER42, set.evaluate(ctxt));
        assertEquals(LOWER42, JEXL.getProperty(pub, "aString"));
    }
}
