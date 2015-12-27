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
 * Tests for array literals.
 * @since 2.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ArrayLiteralTest extends JexlTestCase {

    public ArrayLiteralTest() {
        super("ArrayLiteralTest");
    }

    @Test
    public void testEmptyArrayLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o;
        o = JEXL.createExpression("[]").evaluate(jc);
        Assert.assertTrue(o instanceof Object[]);
        Assert.assertEquals(0, ((Object[]) o).length);
        o = JEXL.createExpression("[...]").evaluate(jc);
        Assert.assertTrue(o instanceof List<?>);
        Assert.assertEquals(0, ((List<?>) o).size());
    }

    @Test
    public void testLiteralWithStrings() throws Exception {
        JexlExpression e = JEXL.createExpression("[ 'foo' , 'bar' ]");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Object[] check = {"foo", "bar"};
        Assert.assertTrue(Arrays.equals(check, (Object[]) o));
    }

    @Test
    public void testLiteralWithElipsis() throws Exception {
        JexlExpression e = JEXL.createExpression("[ 'foo' , 'bar', ... ]");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Object[] check = {"foo", "bar"};
        Assert.assertEquals(Arrays.asList(check), o);
        Assert.assertEquals(2, ((List<?>) o).size());
    }

    @Test
    public void testLiteralWithOneEntry() throws Exception {
        JexlExpression e = JEXL.createExpression("[ 'foo' ]");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Object[] check = {"foo"};
        Assert.assertTrue(Arrays.equals(check, (Object[]) o));
    }

    @Test
    public void testLiteralWithNumbers() throws Exception {
        JexlExpression e = JEXL.createExpression("[ 5.0 , 10 ]");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Object[] check = {new Double(5), new Integer(10)};
        Assert.assertTrue(Arrays.equals(check, (Object[]) o));
        Assert.assertTrue(o.getClass().isArray() && o.getClass().getComponentType().equals(Number.class));
    }

    @Test
    public void testLiteralWithNulls() throws Exception {
        String[] exprs = {
            "[ null , 10 ]",
            "[ 10 , null ]",
            "[ 10 , null , 10]",
            "[ '10' , null ]",
            "[ null, '10' , null ]"
        };
        Object[][] checks = {
            {null, new Integer(10)},
            {new Integer(10), null},
            {new Integer(10), null, new Integer(10)},
            {"10", null},
            {null, "10", null}
        };
        JexlContext jc = new MapContext();
        for (int t = 0; t < exprs.length; ++t) {
            JexlExpression e = JEXL.createExpression(exprs[t]);
            Object o = e.evaluate(jc);
            Assert.assertTrue(exprs[t], Arrays.equals(checks[t], (Object[]) o));
        }

    }

    @Test
    public void testLiteralWithIntegers() throws Exception {
        JexlExpression e = JEXL.createExpression("[ 5 , 10 ]");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        int[] check = {5, 10};
        Assert.assertTrue(Arrays.equals(check, (int[]) o));
    }

    @Test
    public void testSizeOfSimpleArrayLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("size([ 'foo' , 'bar' ])");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals(new Integer(2), o);
    }

    @Test
    public void notestCallingMethodsOnNewMapLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("size({ 'foo' : 'bar' }.values())");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals(new Integer(1), o);
    }

    @Test
    public void testNotEmptySimpleArrayLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("empty([ 'foo' , 'bar' ])");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertFalse((Boolean) o);
    }

    @Test
    public void testChangeThroughVariables() throws Exception {
        JexlContext jc = new MapContext();
        JexlExpression e147 = JEXL.createExpression("quux = [one, two]");

        jc.set("one", 1);
        jc.set("two", 2);
        int[] o1 = (int[]) e147.evaluate(jc);
        Assert.assertEquals(1, o1[0]);
        Assert.assertEquals(2, o1[1]);

        jc.set("one", 10);
        jc.set("two", 20);
        int[] o2 = (int[]) e147.evaluate(jc);
        Assert.assertEquals(10, o2[0]);
        Assert.assertEquals(20, o2[1]);
    }
}
