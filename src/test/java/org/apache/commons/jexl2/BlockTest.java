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
 * Tests for blocks
 * @since 1.1
 */
public class BlockTest extends JexlTestCase {

    /**
     * Create the test
     * 
     * @param testName name of the test
     */
    public BlockTest(String testName) {
        super(testName);
    }

    public void testBlockSimple() throws Exception {
        Expression e = JEXL.createExpression("if (true) { 'hello'; }");
        JexlContext jc = new JexlContext.Mapped();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", "hello", o);
    }

    public void testBlockExecutesAll() throws Exception {
        Expression e = JEXL.createExpression("if (true) { x = 'Hello'; y = 'World';}");
        JexlContext jc = new JexlContext.Mapped();
        Object o = e.evaluate(jc);
        assertEquals("First result is wrong", "Hello", jc.getJexlVariable("x"));
        assertEquals("Second result is wrong", "World", jc.getJexlVariable("y"));
        assertEquals("Block result is wrong", "World", o);
    }

    public void testEmptyBlock() throws Exception {
        Expression e = JEXL.createExpression("if (true) { }");
        JexlContext jc = new JexlContext.Mapped();
        Object o = e.evaluate(jc);
        assertNull("Result is wrong", o);
    }

    public void testBlockLastExecuted01() throws Exception {
        Expression e = JEXL.createExpression("if (true) { x = 1; } else { x = 2; }");
        JexlContext jc = new JexlContext.Mapped();
        Object o = e.evaluate(jc);
        assertEquals("Block result is wrong", new Integer(1), o);
    }

    public void testBlockLastExecuted02() throws Exception {
        Expression e = JEXL.createExpression("if (false) { x = 1; } else { x = 2; }");
        JexlContext jc = new JexlContext.Mapped();
        Object o = e.evaluate(jc);
        assertEquals("Block result is wrong", new Integer(2), o);
    }

    public void testNestedBlock() throws Exception {
        Expression e = JEXL.createExpression("if (true) { x = 'hello'; y = 'world';" + " if (true) { x; } y; }");
        JexlContext jc = new JexlContext.Mapped();
        Object o = e.evaluate(jc);
        assertEquals("Block result is wrong", "world", o);
    }
}
