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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Tests for blocks
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
class BlockTest extends JexlTestCase {

    /**
     * Create the test
     */
    public BlockTest() {
        super("BlockTest");
    }

    @Test
    void testBlockExecutesAll() {
        final JexlScript e = JEXL.createScript("if (true) { x = 'Hello'; y = 'World';}");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals("Hello", jc.get("x"), "First result is wrong");
        assertEquals("World", jc.get("y"), "Second result is wrong");
        assertEquals("World", o, "Block result is wrong");
    }

    @Test
    void testBlockLastExecuted01() {
        final JexlScript e = JEXL.createScript("if (true) { x = 1; } else { x = 2; }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(1), o, "Block result is wrong");
    }

    @Test
    void testBlockLastExecuted02() {
        final JexlScript e = JEXL.createScript("if (false) { x = 1; } else { x = 2; }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(2), o, "Block result is wrong");
    }

    @Test
    void testBlockSimple() {
        final JexlScript e = JEXL.createScript("if (true) { 'hello'; }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals("hello", o, "Result is wrong");
    }

    @Test
    void testEmptyBlock() {
        final JexlScript e = JEXL.createScript("if (true) { }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertNull(o, "Result is wrong");
    }

    @Test
    void testNestedBlock() {
        final JexlScript e = JEXL.createScript("if (true) { x = 'hello'; y = 'world';" + " if (true) { x; } y; }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals("world", o, "Block result is wrong");
    }

    @Test
    void testSetVSBlock() {
        final AnnotationTest.AnnotationContext jc = new AnnotationTest.AnnotationContext();
        JexlScript e;
        Object r;
        // synchronized block
        e = JEXL.createScript("let n = 41 @synchronized { n += 1; }");
        r = e.execute(jc);
        assertEquals(42, r);
        assertEquals(1, jc.getCount());
        assertTrue(jc.getNames().contains("synchronized"));
        // synchronized set
        e = JEXL.createScript("let n = 41 @synchronized { n += 1 }");
        r = e.execute(jc);
        assertEquals(Collections.singleton(42), r);
        assertEquals(2, jc.getCount());
        assertTrue(jc.getNames().contains("synchronized"));

        e = JEXL.createScript("let n = 41 { n += 1 }");
        r = e.execute(jc);
        assertEquals(Collections.singleton(42), r);

        e = JEXL.createScript("let n = 41 { n += 1; }");
        r = e.execute(jc);
        assertEquals(42, r);

        e = JEXL.createScript("{'A' : 1, 'B' : 42}['B']");
        r = e.execute(jc);
        assertEquals(42, r);

        e = JEXL.createScript("{ n = 42; }");
        r = e.execute(jc);
        assertEquals(42, r);
        e = JEXL.createScript("@synchronized(y) { n = 42; }", "y");
        r = e.execute(jc);
        assertEquals(42, r);

        e = JEXL.createScript("{ n = 42 }");
        r = e.execute(jc);
        assertEquals(Collections.singleton(42), r);
        e = JEXL.createScript("@synchronized(z) { n = 42 }", "z");
        r = e.execute(jc);
        assertEquals(Collections.singleton(42), r);

        e = JEXL.createScript("{ n = 41; m = 42 }");
        r = e.execute(jc);
        assertEquals(42, r);

        e = JEXL.createScript("{ 20 + 22; }");
        r = e.execute(jc);
        assertEquals(42, r);
        e = JEXL.createScript("@synchronized { 20 + 22; }");
        r = e.execute(jc);
        assertEquals(42, r);

        e = JEXL.createScript("{ 6 * 7 }");
        r = e.execute(jc);
        assertEquals(Collections.singleton(42), r);
    }
}
