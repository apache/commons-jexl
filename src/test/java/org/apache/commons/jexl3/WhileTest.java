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
 * Tests for while statement.
 * @since 1.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class WhileTest extends JexlTestCase {

    public WhileTest() {
        super("WhileTest");
    }

    @Test
    public void testSimpleWhileFalse() throws Exception {
        final JexlScript e = JEXL.createScript("while (false) ;");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        Assert.assertNull("Result is not null", o);
    }

    @Test
    public void testWhileExecutesExpressionWhenLooping() throws Exception {
        final JexlScript e = JEXL.createScript("while (x < 10) x = x + 1;");
        final JexlContext jc = new MapContext();
        jc.set("x", new Integer(1));

        final Object o = e.execute(jc);
        Assert.assertEquals("Result is wrong", new Integer(10), o);
    }

    @Test
    public void testWhileWithBlock() throws Exception {
        final JexlScript e = JEXL.createScript("while (x < 10) { x = x + 1; y = y * 2; }");
        final JexlContext jc = new MapContext();
        jc.set("x", new Integer(1));
        jc.set("y", new Integer(1));

        final Object o = e.execute(jc);
        Assert.assertEquals("Result is wrong", new Integer(512), o);
        Assert.assertEquals("x is wrong", new Integer(10), jc.get("x"));
        Assert.assertEquals("y is wrong", new Integer(512), jc.get("y"));
    }
}
