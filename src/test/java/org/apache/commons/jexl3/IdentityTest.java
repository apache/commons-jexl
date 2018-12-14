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
 * Test cases for the identity operators.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class IdentityTest extends JexlTestCase {
    public IdentityTest() {
        super("IdentityTest");
    }

    @Test
    public void testSimpleIdentical() throws Exception {
        JexlScript e = JEXL.createScript("var x = '123'; var y = x; x === y");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testSimpleNotIdentical() throws Exception {
        JexlScript e = JEXL.createScript("var x = '123'; var y = '123'; x !== y");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testNullIdentical() throws Exception {
        JexlScript e = JEXL.createScript("var x = null; var y = null; x === y");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

    @Test
    public void testNullNotIdentical() throws Exception {
        JexlScript e = JEXL.createScript("var x = 123; var y = null; x !== y");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not true", Boolean.TRUE, o);
    }

}
