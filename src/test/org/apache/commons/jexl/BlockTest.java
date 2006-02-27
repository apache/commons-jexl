/*
 * Copyright 2002-2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl;

import junit.framework.TestCase;

public class BlockTest extends TestCase {

    /**
     * Create the test
     * 
     * @param testName name of the test
     */
    public BlockTest(String testName) {
        super(testName);
    }

    public void testBlockSimple() throws Exception {
        Expression e = ExpressionFactory
                .createExpression("if (true) { 'hello'; }");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("Result is wrong", "hello", o);
    }
    
    public void testBlockExecutesAll() throws Exception {
        Expression e = ExpressionFactory
                .createExpression("if (true) { x = 'Hello'; y = 'World';}");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertEquals("First result is wrong", "Hello", jc.getVars().get("x"));
        assertEquals("Second result is wrong", "World", jc.getVars().get("y"));
        assertEquals("Block result is wrong", "World", o);
    }

    public void testEmptyBlock() throws Exception {
        Expression e = ExpressionFactory
                .createExpression("if (true) { }");
        JexlContext jc = JexlHelper.createContext();
        Object o = e.evaluate(jc);
        assertNull("Result is wrong", o);
    }
}
