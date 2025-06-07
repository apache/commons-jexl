/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Collection;

import org.junit.jupiter.api.Test;

/**
 * Tests for ranges.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class RangeTest extends JexlTestCase {

    public RangeTest() {
        super("RangeTest");
    }

    @Test
    void testIntegerContains() throws Exception {
        final JexlScript e = JEXL.createScript("(x)->{ x =~ (1..10) }");
        final JexlContext jc = new MapContext();

        Object o = e.execute(jc, 5);
        assertEquals(Boolean.TRUE, o);
        o = e.execute(jc, 0);
        assertEquals(Boolean.FALSE, o);
        o = e.execute(jc, 100);
        assertEquals(Boolean.FALSE, o);
    }

    @Test
    void testIntegerRange() throws Exception {
        final JexlExpression e = JEXL.createExpression("(1..32)");
        final JexlContext jc = new MapContext();

        final Object o0 = e.evaluate(jc);
        final Object o = e.evaluate(jc);
        assertInstanceOf(Collection.class, o);
        final Collection<?> c = (Collection<?>) o;
        assertEquals(32, c.size());

        assertNotSame(o0, o);
        assertEquals(o0.hashCode(), o.hashCode());
        assertEquals(o0, o);

        int i = 0;
        for (final Object v : c) {
            i += 1;
            assertEquals(i, ((Number) v).intValue());
        }
        assertEquals(32, i);

        Integer[] aa = c.<Integer>toArray(new Integer[32]);
        assertEquals(32, aa.length);
        for (int l = 0; l < 32; ++l) {
            assertEquals((int) aa[l], l + 1);
        }

        aa = c.<Integer>toArray(new Integer[2]);
        assertEquals(32, aa.length);
        for (int l = 0; l < 32; ++l) {
            assertEquals((int) aa[l], l + 1);
        }

        aa = c.<Integer>toArray(new Integer[34]);
        assertEquals(34, aa.length);
        for (int l = 0; l < 32; ++l) {
            assertEquals((int) aa[l], l + 1);
        }

        final Object[] oaa = c.toArray();
        assertEquals(32, oaa.length);
        for (int l = 0; l < 32; ++l) {
            assertEquals(oaa[l], l + 1);
        }
    }

    @Test
    void testIntegerRangeOne() throws Exception {
        final JexlExpression e = JEXL.createExpression("(1..1)");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertInstanceOf(Collection.class, o);
        final Collection<?> c = (Collection<?>) o;
        assertEquals(1, c.size());
        final Object[] a = c.toArray();
        assertEquals(1, a.length);
        assertEquals(1, ((Number) a[0]).intValue());
        assertFalse((Boolean) JEXL.createScript("empty x", "x").execute(null, e));
    }

    @Test
    void testIntegerSum() throws Exception {
        final JexlScript e = JEXL.createScript("var s = 0; for(var i : (1..5)) { s = s + i; }; s");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        assertEquals(15, ((Number) o).intValue());
    }

    @Test
    void testLongContains() throws Exception {
        final JexlScript e = JEXL.createScript("(x)->{ x =~ (90000000001L..90000000010L) }");
        final JexlContext jc = new MapContext();

        Object o = e.execute(jc, 90000000005L);
        assertEquals(Boolean.TRUE, o);
        o = e.execute(jc, 0);
        assertEquals(Boolean.FALSE, o);
        o = e.execute(jc, 90000000011L);
        assertEquals(Boolean.FALSE, o);
    }

    @Test
    void testLongRange() throws Exception {
        final JexlExpression e = JEXL.createExpression("(6789000001L..6789000032L)");
        final JexlContext jc = new MapContext();

        final Object o0 = e.evaluate(jc);
        final Object o = e.evaluate(jc);
        assertInstanceOf(Collection.class, o);
        final Collection<?> c = (Collection<?>) o;
        assertEquals(32, c.size());
        assertFalse((Boolean) JEXL.createScript("empty x", "x").execute(null, e));

        assertNotSame(o0, o);
        assertEquals(o0.hashCode(), o.hashCode());
        assertEquals(o0, o);

        long i = 6789000000L;
        for (final Object v : c) {
            i += 1;
            assertEquals(i, ((Number) v).longValue());
        }
        assertEquals(6789000032L, i);

        Long[] aa = c.<Long>toArray(new Long[32]);
        assertEquals(32, aa.length);
        for (int l = 0; l < 32; ++l) {
            assertEquals((long) aa[l], 6789000001L + l);
        }

        aa = c.<Long>toArray(new Long[2]);
        assertEquals(32, aa.length);
        for (int l = 0; l < 32; ++l) {
            assertEquals((long) aa[l], 6789000001L + l);
        }

        aa = c.<Long>toArray(new Long[34]);
        assertEquals(34, aa.length);
        for (int l = 0; l < 32; ++l) {
            assertEquals((long) aa[l], 6789000001L + l);
        }

        final Object[] oaa = c.toArray();
        assertEquals(32, oaa.length);
        for (int l = 0; l < 32; ++l) {
            assertEquals(oaa[l], 6789000001L + l);
        }
    }

    @Test
    void testLongSum() throws Exception {
        final JexlScript e = JEXL.createScript("var s = 0; for(var i : (6789000001L..6789000001L)) { s = s + i; }; s");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        assertEquals(6789000001L, ((Number) o).longValue());
    }
}
