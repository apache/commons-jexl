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
 * Tests for blocks
 * @since 1.1
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
    public void testBlockSimple() throws Exception {
        JexlScript e = JEXL.createScript("if (true) { 'hello'; }");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is wrong", "hello", o);
    }

    @Test
    public void testBlockExecutesAll() throws Exception {
        JexlScript e = JEXL.createScript("if (true) { x = 'Hello'; y = 'World';}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("First result is wrong", "Hello", jc.get("x"));
        Assert.assertEquals("Second result is wrong", "World", jc.get("y"));
        Assert.assertEquals("Block result is wrong", "World", o);
    }

    @Test
    public void testEmptyBlock() throws Exception {
        JexlScript e = JEXL.createScript("if (true) { }");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertNull("Result is wrong", o);
    }

    @Test
    public void testBlockLastExecuted01() throws Exception {
        JexlScript e = JEXL.createScript("if (true) { x = 1; } else { x = 2; }");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Block result is wrong", new Integer(1), o);
    }

    @Test
    public void testBlockLastExecuted02() throws Exception {
        JexlScript e = JEXL.createScript("if (false) { x = 1; } else { x = 2; }");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Block result is wrong", new Integer(2), o);
    }

    @Test
    public void testNestedBlock() throws Exception {
        JexlScript e = JEXL.createScript("if (true) { x = 'hello'; y = 'world';" + " if (true) { x; } y; }");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Block result is wrong", "world", o);
    }
}
