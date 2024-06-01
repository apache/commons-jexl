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
package org.apache.commons.jexl3.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Basic checks on ranges.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class RangeTest extends JexlTestCase {

    public RangeTest() {
        super("InternalTest");
    }

    private void checkIteration(final IntegerRange ir, final int first, final int last) throws Exception {
        final Iterator<Integer> ii = ir.iterator();
        if (ii.hasNext()) {
            int l = ii.next();
            assertEquals(first, l);
            while(ii.hasNext()) {
                l = ii.next();
            }
            assertEquals(last, l);
        } else {
            fail("empty iterator?");
        }
    }

    private void checkIteration(final LongRange lr, final long first, final long last) throws Exception {
        final Iterator<Long> ii = lr.iterator();
        if (ii.hasNext()) {
            long l = ii.next();
            assertEquals(first, l);
            while(ii.hasNext()) {
                l = ii.next();
            }
            assertEquals(last, l);
        } else {
            fail("empty iterator?");
        }
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error
        java.util.logging.Logger.getLogger(org.apache.commons.jexl3.JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testAscIterator() {
        Iterator<Integer> ii = new AscIntegerIterator(3, 5);
        Integer i = 3;
        while(ii.hasNext()) {
            assertEquals(i, ii.next());
            i += 1;
        }
        try {
            ii.next();
            fail("iterator exhausted");
        } catch(NoSuchElementException e) {
            assertNotNull(e);
        }
        try {
            ii.remove();
            fail("remove not implemented");
        } catch(UnsupportedOperationException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testAscLongIterator() {
        Iterator<Long> ii = new AscLongIterator(3L, 5L);
        Long i = 3L;
        while(ii.hasNext()) {
            assertEquals(i, ii.next());
            i += 1;
        }
        try {
            ii.next();
            fail("iterator exhausted");
        } catch(NoSuchElementException e) {
            assertNotNull(e);
        }
        try {
            ii.remove();
            fail("remove not implemented");
        } catch(UnsupportedOperationException e) {
            assertNotNull(e);
        }
    }

    @Test public void testMisc() {
        assertEquals("?", Scope.UNDEFINED.toString());
        assertEquals("??", Scope.UNDECLARED.toString());
    }


    @Test
    public void testRanges() throws Exception {
        final LongRange lr0 = LongRange.create(20,10);
        assertEquals(10L, lr0.getMin());
        assertEquals(20L, lr0.getMax());
        assertFalse(lr0.isEmpty());
        assertTrue(lr0.contains(10L));
        assertTrue(lr0.contains(20L));
        assertFalse(lr0.contains(30L));
        assertFalse(lr0.contains(5L));
        assertFalse(lr0.contains(null));
        checkIteration(lr0, 20L, 10L);
        final LongRange lr1 = LongRange.create(10,20);
        checkIteration(lr1, 10L, 20L);
        assertTrue(lr0.containsAll(lr1));
        final LongRange lr2 = LongRange.create(10,15);
        assertNotEquals(lr0, lr2);
        assertTrue(lr0.containsAll(lr2));
        assertFalse(lr2.containsAll(lr1));
        final IntegerRange ir0 = IntegerRange.create(20,10);
        checkIteration(ir0, 20, 10);
        assertEquals(10, ir0.getMin());
        assertEquals(20, ir0.getMax());
        assertFalse(ir0.isEmpty());
        assertTrue(ir0.contains(10));
        assertTrue(ir0.contains(20));
        assertFalse(ir0.contains(30));
        assertFalse(ir0.contains(5));
        assertFalse(ir0.contains(null));
        final IntegerRange ir1 = IntegerRange.create(10,20);
        checkIteration(ir1, 10, 20);
        assertTrue(ir0.containsAll(ir1));
        assertNotEquals(ir0, lr0);
        assertNotEquals(ir1, lr1);
        final IntegerRange ir2 = IntegerRange.create(10,15);
        assertNotEquals(ir0, ir2);
        assertTrue(ir0.containsAll(ir2));
        assertFalse(ir2.containsAll(ir1));

        long lc0 = 20;
        final Iterator<Long> il0 = lr0.iterator();
        while(il0.hasNext()) {
            final long v0 = il0.next();
            assertEquals(lc0, v0);
            try {
                switch((int)v0) {
                    case 10:  il0.remove(); fail(); break;
                    case 11: lr1.add(v0); fail(); break;
                    case 12: lr1.remove(v0); fail(); break;
                    case 13: lr1.addAll(Collections.singletonList(v0)); fail(); break;
                    case 14: lr1.removeAll(Collections.singletonList(v0)); fail(); break;
                    case 15: lr1.retainAll(Collections.singletonList(v0)); fail(); break;
                }
            } catch (final UnsupportedOperationException xuo) {
                // ok
            }
            lc0 -= 1;
        }
        assertEquals(9L, lc0);
        try {
            il0.next();
            fail();
        } catch (final NoSuchElementException xns) {
            // ok
        }

        int ic0 = 20;
        final Iterator<Integer> ii0 = ir0.iterator();
        while(ii0.hasNext()) {
            final int v0 = ii0.next();
            assertEquals(ic0, v0);
            try {
                switch(v0) {
                    case 10: ii0.remove(); fail(); break;
                    case 11: ir1.add(v0); fail(); break;
                    case 12: ir1.remove(v0); fail(); break;
                    case 13: ir1.addAll(Collections.singletonList(v0)); fail(); break;
                    case 14: ir1.removeAll(Collections.singletonList(v0)); fail(); break;
                    case 15: ir1.retainAll(Collections.singletonList(v0)); fail(); break;
                }
            } catch (final UnsupportedOperationException xuo) {
                // ok
            }
            ic0 -= 1;
        }
        assertEquals(9, ic0);
        try {
            ii0.next();
            fail();
        } catch (final NoSuchElementException xns) {
            // ok
        }
    }

    @Test
    public void testSource() {
        JexlFeatures features = JexlFeatures.createDefault();
        Source src0 = new Source(features, "x -> -x");
        Source src0b = new Source(features, "x -> -x");
        Source src1 = new Source(features, "x -> +x");
        assertEquals(7, src0.length());
        assertEquals(src0, src0);
        assertEquals(src0, src0b);
        assertNotEquals(src0, src1);
        assertEquals(src0.hashCode(), src0b.hashCode());
        assertNotEquals(src0.hashCode(), src1.hashCode());
        assertTrue(src0.compareTo(src0b) == 0);
        assertTrue(src0.compareTo(src1) > 0);
        assertTrue(src1.compareTo(src0) < 0);
    }
}

