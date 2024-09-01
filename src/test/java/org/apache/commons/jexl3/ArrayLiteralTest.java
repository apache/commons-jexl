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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for array literals.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ArrayLiteralTest extends JexlTestCase {

    public ArrayLiteralTest() {
        super("ArrayLiteralTest");
    }

    @Test
    public void testChangeThroughVariables() throws Exception {
        final JexlContext jc = new MapContext();
        final JexlExpression e147 = JEXL.createExpression("quux = [one, two]");

        jc.set("one", 1);
        jc.set("two", 2);
        final int[] o1 = (int[]) e147.evaluate(jc);
        assertEquals(1, o1[0]);
        assertEquals(2, o1[1]);

        jc.set("one", 10);
        jc.set("two", 20);
        final int[] o2 = (int[]) e147.evaluate(jc);
        assertEquals(10, o2[0]);
        assertEquals(20, o2[1]);
    }

    @Test
    public void testEmptyArrayLiteral() throws Exception {
        final JexlContext jc = new MapContext();
        Object o;
        o = JEXL.createExpression("[]").evaluate(jc);
        assertInstanceOf(Object[].class, o);
        assertEquals(0, ((Object[]) o).length);
        o = JEXL.createExpression("[...]").evaluate(jc);
        assertInstanceOf(List.class, o);
        assertEquals(0, ((List<?>) o).size());
        assertThrows(JexlException.Parsing.class, () -> JEXL.createExpression("[ , ]"));
        assertThrows(JexlException.Parsing.class, () -> JEXL.createExpression("[ ... , ]"));
    }

    @Test
    public void testLiteralWithElipsis() throws Exception {
        final JexlExpression e = JEXL.createExpression("[ 'foo' , 'bar', ... ]");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        final Object[] check = { "foo", "bar" };
        assertEquals(Arrays.asList(check), o);
        assertEquals(2, ((List<?>) o).size());
        assertThrows(JexlException.Parsing.class, () -> JEXL.createExpression("[ 'foo' , 'bar', ... , ]"));
    }

    @Test
    public void testLiteralWithIntegers() throws Exception {
        final JexlExpression e = JEXL.createExpression("[ 5 , 10 ]");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        final int[] check = {5, 10};
        assertArrayEquals(check, (int[]) o);
    }

    @Test
    public void testLiteralWithNulls() throws Exception {
        final String[] exprs = {
            "[ null , 10 ]",
            "[ 10 , null ]",
            "[ 10 , null , 10]",
            "[ '10' , null ]",
            "[ null, '10' , null ]"
        };
        final Object[][] checks = {
            {null, Integer.valueOf(10)},
            {Integer.valueOf(10), null},
            {Integer.valueOf(10), null, Integer.valueOf(10)},
            {"10", null},
            {null, "10", null}
        };
        final JexlContext jc = new MapContext();
        for (int t = 0; t < exprs.length; ++t) {
            final JexlExpression e = JEXL.createExpression(exprs[t]);
            final Object o = e.evaluate(jc);
            assertArrayEquals(checks[t], (Object[]) o, exprs[t]);
        }

    }

    @Test
    public void testLiteralWithNumbers() throws Exception {
        final JexlExpression e = JEXL.createExpression("[ 5.0 , 10 ]");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        final Object[] check = {Double.valueOf(5), Integer.valueOf(10)};
        assertArrayEquals(check, (Object[]) o);
        assertTrue(o.getClass().isArray() && o.getClass().getComponentType().equals(Number.class));
    }

    @Test
    public void testLiteralWithOneEntry() throws Exception {
        final Object[] check = {"foo"};
        final List<String> sources = Arrays.asList("[ 'foo']", "[ 'foo' , ]");
        for(final String src : sources) {
            final JexlExpression e = JEXL.createExpression(src);
            final Object o = e.evaluate(null);
            assertArrayEquals(check, (Object[]) o);
        }
    }

    @Test
    public void testLiteralWithStrings() throws Exception {
        final Object[] check = {"foo", "bar"};
        final List<String> sources = Arrays.asList("[ 'foo' , 'bar' ]", "[ 'foo' , 'bar', ]");
        for(final String src : sources) {
            final JexlExpression e = JEXL.createExpression(src);
            final Object o = e.evaluate(null);
            assertArrayEquals(check, (Object[]) o);
        }
    }

    @Test
    public void testNotEmptySimpleArrayLiteral() throws Exception {
        final JexlExpression e = JEXL.createExpression("empty([ 'foo' , 'bar' ])");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertFalse((Boolean) o);
    }

    @Test
    public void testNotestCallingMethodsOnNewMapLiteral() throws Exception {
        final JexlExpression e = JEXL.createExpression("size({ 'foo' : 'bar' }.values())");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertEquals(Integer.valueOf(1), o);
    }

    @Test
    public void testSizeOfSimpleArrayLiteral() throws Exception {
        final JexlExpression e = JEXL.createExpression("size([ 'foo' , 'bar' ])");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertEquals(Integer.valueOf(2), o);
    }
}
