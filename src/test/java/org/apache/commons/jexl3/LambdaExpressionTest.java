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
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests function/lambda/closure features.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class LambdaExpressionTest extends JexlTestCase {

    public LambdaExpressionTest() {
        super("LambdaExpressionTest");
    }

    @Test
    public void testScriptContext() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript s = jexl.createScript("function(x) { x + x }");
        Assert.assertEquals(42, s.execute(null, 21));
        JexlScript s42 = jexl.createScript("s(21)");
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.set("s", s);
        Object result = s42.execute(ctxt);
        Assert.assertEquals(42, result);
        result = s42.execute(ctxt);
        Assert.assertEquals(42, result);
        s42 = jexl.createScript("x => x + x");
        result = s42.execute(ctxt, 21);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testLambda() throws Exception {
        JexlEngine jexl = new Engine();
        String strs = "var s = x => x + x; s(21)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);
        strs = "var s = (x, y) => x + y; s(15, 27)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testLambdaClosure() throws Exception {
        JexlEngine jexl = new Engine();
        String strs = "var t = 20; var s = (x, y) => x + y + t; s(15, 7)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);
        strs = "var t = 20; var s = (x, y) => x + y + t; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testLambdaLambda() throws Exception {
        JexlEngine jexl = new Engine();

        String strs = "( (x, y) => ( (xx, yy) => xx + yy)(x, y))(15, 27)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testNestLambda() throws Exception {
        JexlEngine jexl = new Engine();
        String strs = "( (x) => (y) => x + y)(15)(27)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testRecurse() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext jc = new MapContext();
        try {
            JexlScript script = jexl.createScript("var fact = x => (x <= 1) ? 1 : x * fact(x - 1); fact(5)");
            int result = (Integer) script.execute(jc);
            Assert.assertEquals(120, result);
        } catch (JexlException xany) {
            String msg = xany.toString();
            throw xany;
        }
    }

    @Test
    public void testRecurse2() throws Exception {
        JexlEngine jexl = new Engine();
        JexlContext jc = new MapContext();
        // adding some hoisted vars to get it confused
        try {
            JexlScript script = jexl.createScript(
                    "var y = 1; var z = 1; "
                    +"var fact = (x) => (x <= y) ? z : x * fact(x - 1); fact(6)");
            int result = (Integer) script.execute(jc);
            Assert.assertEquals(720, result);
        } catch (JexlException xany) {
            String msg = xany.toString();
            throw xany;
        }
    }

    @Test
    public void testIdentity() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript script;
        Object result;

        script = jexl.createScript("(x) => x");
        Assert.assertArrayEquals(new String[]{"x"}, script.getParameters());
        result = script.execute(null, 42);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCurry1() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript script;
        Object result;

        JexlScript base = jexl.createScript("(x, y, z)=>x + y + z");
        script = base.curry(5);
        script = script.curry(15);
        script = script.curry(22);
        result = script.execute(null);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCurry2() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript script;
        Object result;

        JexlScript base = jexl.createScript("(x, y, z)=>x + y + z");
        script = base.curry(5, 15);
        script = script.curry(22);
        result = script.execute(null);
        Assert.assertEquals(42, result);
    }
}
