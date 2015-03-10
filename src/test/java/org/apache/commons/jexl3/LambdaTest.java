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

import org.apache.commons.jexl3.internal.Engine;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Tests function/lambda/closure features.
 */
public class LambdaTest extends JexlTestCase {

    public LambdaTest(String testName) {
        super(testName);
    }

    public void testScriptArguments() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript s = jexl.createScript(" x + x ", "x");
        JexlScript s42 = jexl.createScript("s(21)", "s");
        Object result = s42.execute(null, s);
        assertEquals(42, result);
    }

    public void testScriptContext() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript s = jexl.createScript("function(x) { x + x }");
        String fsstr = s.getParsedText();
        assertEquals("(x)->{ x + x; }", fsstr);
        assertEquals(42, s.execute(null, 21));
        JexlScript s42 = jexl.createScript("s(21)");
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.set("s", s);
        Object result = s42.execute(ctxt);
        assertEquals(42, result);
        result = s42.execute(ctxt);
        assertEquals(42, result);
         s = jexl.createScript("x-> { x + x }");
        result = s42.execute(ctxt);
        assertEquals(42, result);
    }

    public void testLambda() throws Exception {
        JexlEngine jexl = new Engine();
        String strs = "var s = function(x) { x + x }; s(21)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        assertEquals(42, result);
        strs = "var s = function(x, y) { x + y }; s(15, 27)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
    }

    public void testLambdaClosure() throws Exception {
        JexlEngine jexl = new Engine();
        String strs = "var t = 20; var s = function(x, y) { x + y + t}; s(15, 7)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        assertEquals(42, result);
        strs = "var t = 19; var s = function(x, y) { var t = 20; x + y + t}; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
        strs = "var t = 20; var s = function(x, y) {x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
        strs = "var t = 19; var s = function(x, y) { var t = 20; x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
    }

    public void testLambdaLambda() throws Exception {
        JexlEngine jexl = new Engine();
        String strs = "var t = 19; ( (x, y)->{ var t = 20; x + y + t} )(15, 7);";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        assertEquals(42, result);

        strs = "( (x, y)->{ ( (xx, yy)->{xx + yy } )(x, y) } )(15, 27)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);

        strs = "var t = 19; var s = (x, y)->{ var t = 20; x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
    }

    public void testNestLambda() throws Exception {
        JexlEngine jexl = new Engine();
        String strs = "( (x)->{ (y)->{ x + y } })(15)(27)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        assertEquals(42, result);
    }

    public void testNestLambada() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext ctx = null;
        String strs = "(x)->{ (y)->{ x + y } }";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(ctx, 15);
        assertTrue(result instanceof JexlScript);
        JexlScript s15 = (JexlScript) result;
        Callable<Object> s15b = s15.callable(ctx, 27);
        result = s15.execute(ctx, 27);
        assertEquals(42, result);
        result = s15b.call();
        assertEquals(42, result);
    }

    public void testHoistLambada() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext ctx = null;
        JexlScript s42;
        Object result;
        JexlScript s15;
        String[] localv;
        Set<List<String>> hvars;
        String strs;

        // hosted variables are NOT local variables
        strs = "(x)->{ (y)->{ x + y } }";
        s42 = jexl.createScript(strs);
        result = s42.execute(ctx, 15);
        assertTrue(result instanceof JexlScript);
        s15 = (JexlScript) result;
        localv = s15.getLocalVariables();
        assertNull(localv);
        hvars = s15.getVariables();
        assertEquals(1, hvars.size());

        // declaring a local that overrides hoisted
        strs = "(x)->{ (y)->{ var x; x + y } }";
        s42 = jexl.createScript(strs);
        result = s42.execute(ctx, 15);
        assertTrue(result instanceof JexlScript);
        s15 = (JexlScript) result;
        localv = s15.getLocalVariables();
        assertNotNull(localv);
        assertEquals(1, localv.length);
        hvars = s15.getVariables();
        assertEquals(0, hvars.size());
    }

    public void testRecurse() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext jc = new MapContext();
        try {
            JexlScript script = jexl.createScript("var fact = (x)->{ if (x <= 1) 1; else x * fact(x - 1) }; fact(5)");
            int result = (Integer) script.execute(jc);
            assertEquals(120, result);
        } catch (JexlException xany) {
            String msg = xany.toString();
            throw xany;
        }
    }

    public void testRecurse2() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext jc = new MapContext();
        // adding some hoisted vars to get it confused
        try {
            JexlScript script = jexl.createScript(
                    "var y = 1; var z = 1; "
                    +"var fact = (x)->{ if (x <= y) z; else x * fact(x - 1) }; fact(6)");
            int result = (Integer) script.execute(jc);
            assertEquals(720, result);
        } catch (JexlException xany) {
            String msg = xany.toString();
            throw xany;
        }
    }

    public void testRecurse3() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext jc = new MapContext();
        // adding some hoisted vars to get it confused
        try {
            JexlScript script = jexl.createScript(
                    "var y = 1; var z = 1;var foo = (x)->{y + z}; "
                    +"var fact = (x)->{ if (x <= y) z; else x * fact(x - 1) }; fact(6)");
            int result = (Integer) script.execute(jc);
            assertEquals(720, result);
        } catch (JexlException xany) {
            String msg = xany.toString();
            throw xany;
        }
    }
}
