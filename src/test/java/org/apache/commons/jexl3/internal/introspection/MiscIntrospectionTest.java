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
package org.apache.commons.jexl3.internal.introspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.apache.commons.jexl3.JexlEngine;
import org.junit.jupiter.api.Test;

/**
 * Checks the CacheMap.MethodKey implementation
 */

public class MiscIntrospectionTest {
    @Test
    public void testArrayIterator() {
        // not on lists
        try {
            new ArrayIterator(new ArrayList<>());
        } catch (final IllegalArgumentException xill) {
            assertNotNull(xill);
        }
        // wih null ?
        ArrayIterator ai0 = new ArrayIterator(null);
        assertFalse(ai0.hasNext());
        try {
            ai0.next();
            fail("should have failed");
        } catch (final NoSuchElementException no) {
            assertNotNull(no);
        }
        // an array
        ai0 = new ArrayIterator(new int[]{42});
        assertTrue(ai0.hasNext());
        assertEquals(42, ai0.next());
        assertFalse(ai0.hasNext());
        try {
            ai0.next();
            fail("iterator on null ?");
        } catch (final NoSuchElementException no) {
            assertNotNull(no);
        }
        // no remove
        try {
            ai0.remove();
            fail("should have failed");
        } catch (final UnsupportedOperationException no) {
            assertNotNull(no);
        }
    }
    @Test
    public void testArrayListWrapper() {
        ArrayListWrapper alw ;
        try {
            new ArrayListWrapper(1);
            fail("non-array wrap?");
        } catch (final IllegalArgumentException xil) {
            assertNotNull(xil);
        }
        final Integer[] ai = {1, 2};
        alw = new ArrayListWrapper(ai);
        assertEquals(1, alw.indexOf(2));
        assertEquals(-1, alw.indexOf(null));
    }

    @Test
    public void testEmptyContext() {
        try {
            JexlEngine.EMPTY_CONTEXT.set("nope", 42);
            fail("empty context should be readonly");
        } catch (final UnsupportedOperationException xun) {
            assertNotNull(xun);
        }
    }

}
