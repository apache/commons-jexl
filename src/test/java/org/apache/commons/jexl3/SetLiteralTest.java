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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for set literals
 * @since 3.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class SetLiteralTest extends JexlTestCase {

    public SetLiteralTest() {
        super("SetLiteralTest");
    }

    static private Set<?> createSet(Object... args) {
        return new HashSet<Object>(Arrays.asList(args));
    }

    @Test
    public void testSetLiteralWithStrings() throws Exception {
        JexlExpression e = JEXL.createExpression("{ 'foo' , 'bar' }");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Set<?> check = createSet("foo", "bar");
        Assert.assertTrue(check.equals(o));
    }

    @Test
    public void testLiteralWithOneEntry() throws Exception {
        JexlExpression e = JEXL.createExpression("{ 'foo' }");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Set<?> check = createSet("foo");
        Assert.assertTrue(check.equals(o));
    }

    @Test
    public void testSetLiteralWithStringsScript() throws Exception {
        JexlScript e = JEXL.createScript("{ 'foo' , 'bar' }");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Set<?> check = createSet("foo", "bar");
        Assert.assertTrue(check.equals(o));
    }

    @Test
    public void testSetLiteralWithOneEntryScript() throws Exception {
        JexlScript e = JEXL.createScript("{ 'foo' }");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Set<?> check = createSet("foo");
        Assert.assertTrue(check.equals(o));
    }

    @Test
    public void testSetLiteralWithOneEntryBlock() throws Exception {
        JexlScript e = JEXL.createScript("{ { 'foo' }; }");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Set<?> check = createSet("foo");
        Assert.assertTrue(check.equals(o));
    }

    @Test
    public void testSetLiteralWithOneNestedSet() throws Exception {
        JexlScript e = JEXL.createScript("{ { 'foo' } }");
        JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        Set<?> check = createSet(createSet("foo"));
        Assert.assertTrue(check.equals(o));
    }

    @Test
    public void testSetLiteralWithNumbers() throws Exception {
        JexlExpression e = JEXL.createExpression("{ 5.0 , 10 }");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Set<?> check = createSet(new Double(5.0), new Integer(10));
        Assert.assertTrue(check.equals(o));
    }

    @Test
    public void testSetLiteralWithNulls() throws Exception {
        String[] exprs = {
            "{  }",
            "{ 10 }",
            "{ 10 , null }",
            "{ 10 , null , 20}",
            "{ '10' , null }",
            "{ null, '10' , 20 }"
        };
        Set<?>[] checks = {
            Collections.emptySet(),
            createSet(new Integer(10)),
            createSet(new Integer(10), null),
            createSet(new Integer(10), null, new Integer(20)),
            createSet("10", null),
            createSet(null, "10", new Integer(20))
        };
        JexlContext jc = new MapContext();
        for (int t = 0; t < exprs.length; ++t) {
            JexlScript e = JEXL.createScript(exprs[t]);
            Object o = e.execute(jc);
            Assert.assertTrue(exprs[t], checks[t].equals(o));
        }

    }

    @Test
    public void testSizeOfSimpleSetLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("size({ 'foo' , 'bar'})");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals(new Integer(2), o);
    }

    @Test
    public void testNotEmptySimpleSetLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("empty({ 'foo' , 'bar' })");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertFalse(((Boolean) o).booleanValue());
    }

}
