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

/**
 * Tests function/lambda/closure features.
 */
public class LambdaTest extends JexlTestCase {

    public LambdaTest(String testName) {
        super(testName);
    }
    
    public void testScriptArguments() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript s = jexl.createScript("{ x + x }", "x");
        String strs = "s(21)";
        JexlScript s42 = jexl.createScript("s(21)", "s");
        Object result = s42.execute(null, s);
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
}
