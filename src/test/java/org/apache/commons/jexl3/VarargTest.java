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
public class VarargTest extends JexlTestCase {

    public VarargTest() {
        super("VarargTest");
    }

    @Test
    public void testScriptArguments() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript s = jexl.createScript("function(x...) {x[0]}");
        Assert.assertEquals(42, s.execute(null, 42));
        s = jexl.createScript("function(y, x...) {x[0]}");
        Assert.assertEquals(42, s.execute(null, 21, 42));
        s = jexl.createScript("function(x...) {size(x)}");
        Assert.assertEquals(0, s.execute(null));
        Assert.assertEquals(2, s.execute(null, 21, 42));
    }

    @Test
    public void testCurry1() throws Exception {
        JexlEngine jexl = new Engine();
        JexlScript script;
        Object result;

        JexlScript base = jexl.createScript("(x, y...)->{ x + y[0] + y[1]}");
        script = base.curry(5);
        result = script.execute(null, 15, 22);
        Assert.assertEquals(42, result);
        script = script.curry(15);
        result = script.execute(null, 22);
        Assert.assertEquals(42, result);
        script = script.curry(22);
        result = script.execute(null);
        Assert.assertEquals(42, result);
    }

}
