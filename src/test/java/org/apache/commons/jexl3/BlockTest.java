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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for blocks
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class BlockTest extends JexlTestCase {

    /**
     * Create the test
     */
    public BlockTest() {
        super("BlockTest");
    }

    @Test
    public void testBlockExecutesAll() throws Exception {
        final JexlScript e = JEXL.createScript("if (true) { x = 'Hello'; y = 'World';}");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals("Hello", jc.get("x"), "First result is wrong");
        assertEquals("World", jc.get("y"), "Second result is wrong");
        assertEquals("World", o, "Block result is wrong");
    }

    @Test
    public void testBlockLastExecuted01() throws Exception {
        final JexlScript e = JEXL.createScript("if (true) { x = 1; } else { x = 2; }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(1), o, "Block result is wrong");
    }

    @Test
    public void testBlockLastExecuted02() throws Exception {
        final JexlScript e = JEXL.createScript("if (false) { x = 1; } else { x = 2; }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(2), o, "Block result is wrong");
    }

    @Test
    public void testBlockSimple() throws Exception {
        final JexlScript e = JEXL.createScript("if (true) { 'hello'; }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals("hello", o, "Result is wrong");
    }

    @Test
    public void testEmptyBlock() throws Exception {
        final JexlScript e = JEXL.createScript("if (true) { }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertNull(o, "Result is wrong");
    }

    @Test
    public void testNestedBlock() throws Exception {
        final JexlScript e = JEXL.createScript("if (true) { x = 'hello'; y = 'world';" + " if (true) { x; } y; }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals("world", o, "Block result is wrong");
    }
}
