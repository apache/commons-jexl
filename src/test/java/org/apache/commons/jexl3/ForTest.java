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
import java.util.Map;
import java.util.StringTokenizer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the for statement
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ForTest extends JexlTestCase {

    /** create a named test */
    public ForTest() {
        super("ForTest");
    }

    @Test
    public void testForStatement() throws Exception {
        JexlScript e = JEXL.createScript("for(;;) break");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertNull("Result is not null", o);
    }

    @Test
    public void testForInitialization() throws Exception {
        JexlScript e = JEXL.createScript("for(var i = 0;;) break; i");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 0, o);

        e = JEXL.createScript("var i = 0; for(i = 1;;) break; i");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testForTermination() throws Exception {
        JexlScript e = JEXL.createScript("for(var i = 0; i < 10;) i = i + 1; i");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 10, o);

        e = JEXL.createScript("var i = 0; for(;i < 10;) i = i + 1; i");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 10, o);
    }

    @Test
    public void testForIncrement() throws Exception {
        JexlScript e = JEXL.createScript("for(var i = 0; i < 10; i = i + 1); i");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 10, o);
    }

    @Test
    public void testForBreak() throws Exception {
        JexlScript e = JEXL.createScript("for(var i = 0; i < 10; i = i + 1) if (i > 5) break; i");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 6, o);
    }

    @Test
    public void testForContinue() throws Exception {
        JexlScript e = JEXL.createScript("var x = 0; for(var i = 0; i < 10; i = i + 1) {if (i < 5) continue; x = x + i}; x");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5+6+7+8+9, o);
    }

}
