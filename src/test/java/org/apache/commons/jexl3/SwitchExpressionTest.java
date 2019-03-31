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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests switch expression.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class SwitchExpressionTest extends JexlTestCase {

    public SwitchExpressionTest() {
        super("SwitchExpressionTest");
    }

    @Test
    public void testSyntax() throws Exception {
        JexlScript e = JEXL.createScript("var e = switch (1) {case 1 -> 42; case 2 -> 0}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("var e = switch (1) {case 1 -> 42; case 2 -> 0; default -> -1}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("var e = switch (1) {default -> 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("var e = switch (1) {case 2 -> 0; default -> 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("var e = switch (1) {default -> -1; case 2 -> 0; case 1 -> 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        e = JEXL.createScript("var e = switch (1) {default -> -1; case 2, 1 -> 42}");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testIdentifier() throws Exception {
        JexlScript e = JEXL.createScript("var e = switch (x) {case MAX_VALUE -> 1; default -> 0}");
        JexlContext jc = new MapContext();
        jc.set("x", Integer.MAX_VALUE);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

}
