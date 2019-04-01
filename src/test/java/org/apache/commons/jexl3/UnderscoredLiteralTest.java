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

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for underscored literals.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class UnderscoredLiteralTest extends JexlTestCase {

    public UnderscoredLiteralTest() {
        super("UnderscoredLiteralTest");
    }

    @Test
    public void testNaturalLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o;
        o = JEXL.createExpression("01_000_000").evaluate(jc);
        Assert.assertEquals(01_000_000, o);

        o = JEXL.createExpression("1_000_000").evaluate(jc);
        Assert.assertEquals(1_000_000, o);

        o = JEXL.createExpression("1_000_000L").evaluate(jc);
        Assert.assertEquals(1_000_000L, o);

        o = JEXL.createExpression("0x1_000_000").evaluate(jc);
        Assert.assertEquals(0x1_000_000, o);

        o = JEXL.createExpression("0b1_000_000").evaluate(jc);
        Assert.assertEquals(0b1_000_000, o);
    }

    @Test
    public void testRealLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o;
        o = JEXL.createExpression("3.14_15f").evaluate(jc);
        Assert.assertEquals(3.14_15f, o);
        o = JEXL.createExpression("3.14_15d").evaluate(jc);
        Assert.assertEquals(3.14_15d, o);
        o = JEXL.createExpression("1.e1_0d").evaluate(jc);
        Assert.assertEquals(1.e1_0d, o);
        o = JEXL.createExpression("1_0.e1_0d").evaluate(jc);
        Assert.assertEquals(1_0.e1_0d, o);
        o = JEXL.createExpression("1_0.1_0e1_0d").evaluate(jc);
        Assert.assertEquals(1_0.1_0e1_0d, o);
        o = JEXL.createExpression("1e10").evaluate(jc);
        Assert.assertEquals(1e10, o);
        o = JEXL.createExpression(".1e1d").evaluate(jc);
        Assert.assertEquals(.1e1d, o);
    }

    @Test
    public void testRealLiteral2() throws Exception {
        JexlContext jc = new MapContext();
        Object o;
        o = JEXL.createExpression("1e10").evaluate(jc);
        Assert.assertEquals(1e10, o);
        o = JEXL.createExpression(".1e1d").evaluate(jc);
        Assert.assertEquals(.1e1d, o);
    }

}
