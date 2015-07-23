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

import org.apache.commons.jexl3.JexlTestCase;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic checks on ranges.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class RangeTest extends JexlTestCase {

    public RangeTest() {
        super("InternalTest");
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error
        java.util.logging.Logger.getLogger(org.apache.commons.jexl3.JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void checkIteration(LongRange lr, long first, long last) throws Exception {
        Iterator<Long> ii = lr.iterator();
        if (ii.hasNext()) {
            long l = ii.next();
            Assert.assertEquals(first, l);
            while(ii.hasNext()) {
                l = ii.next();
            }
            Assert.assertEquals(last, l);
        } else {
            Assert.fail("empty iterator?");
        }
    }

    private void checkIteration(IntegerRange ir, int first, int last) throws Exception {
        Iterator<Integer> ii = ir.iterator();
        if (ii.hasNext()) {
            int l = ii.next();
            Assert.assertEquals(first, l);
            while(ii.hasNext()) {
                l = ii.next();
            }
            Assert.assertEquals(last, l);
        } else {
            Assert.fail("empty iterator?");
        }
    }

    @Test
    public void testRanges() throws Exception {
        LongRange lr0 = LongRange.create(20,10);
        Assert.assertEquals(10L, lr0.getMin());
        Assert.assertEquals(20L, lr0.getMax());
        Assert.assertFalse(lr0.isEmpty());
        Assert.assertTrue(lr0.contains(10L));
        Assert.assertTrue(lr0.contains(20L));
        Assert.assertFalse(lr0.contains(30L));
        Assert.assertFalse(lr0.contains(5L));
        Assert.assertFalse(lr0.contains(null));
        checkIteration(lr0, 20L, 10L);
        LongRange lr1 = LongRange.create(10,20);
        checkIteration(lr1, 10L, 20L);
        Assert.assertTrue(lr0.containsAll(lr1));
        LongRange lr2 = LongRange.create(10,15);
        Assert.assertNotEquals(lr0, lr2);
        Assert.assertTrue(lr0.containsAll(lr2));
        Assert.assertFalse(lr2.containsAll(lr1));
        IntegerRange ir0 = IntegerRange.create(20,10);
        checkIteration(ir0, 20, 10);
        Assert.assertEquals(10, ir0.getMin());
        Assert.assertEquals(20, ir0.getMax());
        Assert.assertFalse(ir0.isEmpty());
        Assert.assertTrue(ir0.contains(10));
        Assert.assertTrue(ir0.contains(20));
        Assert.assertFalse(ir0.contains(30));
        Assert.assertFalse(ir0.contains(5));
        Assert.assertFalse(ir0.contains(null));
        IntegerRange ir1 = IntegerRange.create(10,20);
        checkIteration(ir1, 10, 20);
        Assert.assertTrue(ir0.containsAll(ir1));
        Assert.assertNotEquals(ir0, lr0);
        Assert.assertNotEquals(ir1, lr1);
        IntegerRange ir2 = IntegerRange.create(10,15);
        Assert.assertNotEquals(ir0, ir2);
        Assert.assertTrue(ir0.containsAll(ir2));
        Assert.assertFalse(ir2.containsAll(ir1));

        long lc0 = 20;
        Iterator<Long> il0 = lr0.iterator();
        while(il0.hasNext()) {
            long v0 = il0.next();
            Assert.assertEquals(lc0, v0);
            try {
                switch((int)v0) {
                    case 10:  il0.remove(); Assert.fail(); break;
                    case 11: lr1.add(v0); Assert.fail(); break;
                    case 12: lr1.remove(v0); Assert.fail(); break;
                    case 13: lr1.addAll(Arrays.asList(v0)); Assert.fail(); break;
                    case 14: lr1.removeAll(Arrays.asList(v0)); Assert.fail(); break;
                    case 15: lr1.retainAll(Arrays.asList(v0)); Assert.fail(); break;
                }
            } catch(UnsupportedOperationException xuo) {
                // ok
            }
            lc0 -= 1;
        }
        Assert.assertEquals(9L, lc0);
        try {
            il0.next();
            Assert.fail();
        } catch(NoSuchElementException xns) {
            // ok
        }

        int ic0 = 20;
        Iterator<Integer> ii0 = ir0.iterator();
        while(ii0.hasNext()) {
            int v0 = ii0.next();
            Assert.assertEquals(ic0, v0);
            try {
                switch(v0) {
                    case 10: ii0.remove(); Assert.fail(); break;
                    case 11: ir1.add(v0); Assert.fail(); break;
                    case 12: ir1.remove(v0); Assert.fail(); break;
                    case 13: ir1.addAll(Arrays.asList(v0)); Assert.fail(); break;
                    case 14: ir1.removeAll(Arrays.asList(v0)); Assert.fail(); break;
                    case 15: ir1.retainAll(Arrays.asList(v0)); Assert.fail(); break;
                }
            } catch(UnsupportedOperationException xuo) {
                // ok
            }
            ic0 -= 1;
        }
        Assert.assertEquals(9, ic0);
        try {
            ii0.next();
            Assert.fail();
        } catch(NoSuchElementException xns) {
            // ok
        }

    }
}

