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
public class DoWhileTest extends JexlTestCase {

    public DoWhileTest() {
        super("DoWhileTest");
    }

    @Test
    public void testSimpleWhileFalse() throws Exception {
        JexlScript e = JEXL.createScript("do {} while (false)");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Assert.assertNull("Result is not null", o);
    }

    @Test
    public void testWhileExecutesExpressionWhenLooping() throws Exception {
        JexlScript e = JEXL.createScript("do x = x + 1 while (x < 10)");
        JexlContext jc = new MapContext();
        jc.set("x", new Integer(1));

        Object o = e.execute(jc);
        Assert.assertEquals("Result is wrong", new Integer(10), o);
    }

    @Test
    public void testWhileWithBlock() throws Exception {
        JexlScript e = JEXL.createScript("do { x = x + 1; y = y * 2; } while (x < 10)");
        JexlContext jc = new MapContext();
        jc.set("x", new Integer(1));
        jc.set("y", new Integer(1));

        Object o = e.execute(jc);
        Assert.assertEquals("Result is wrong", new Integer(512), o);
        Assert.assertEquals("x is wrong", new Integer(10), jc.get("x"));
        Assert.assertEquals("y is wrong", new Integer(512), jc.get("y"));
    }

    @Test
    public void testWhileRemoveBroken() throws Exception {
        try {
            JexlScript e = JEXL.createScript("do remove while (x < 10)");
            Assert.fail("remove is out of loop!");
        } catch (JexlException.Parsing xparse) {
            String str = xparse.detailedMessage();
            Assert.assertTrue(str.contains("remove"));
        }
    }

}
