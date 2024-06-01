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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertThrows(IllegalArgumentException.class, () -> new ArrayIterator(new ArrayList<>()));
        // wih null ?
        ArrayIterator ai0 = new ArrayIterator(null);
        assertFalse(ai0.hasNext());
        assertThrows(NoSuchElementException.class, ai0::next);
        // an array
        ai0 = new ArrayIterator(new int[] { 42 });
        assertTrue(ai0.hasNext());
        assertEquals(42, ai0.next());
        assertFalse(ai0.hasNext());
        assertThrows(NoSuchElementException.class, ai0::next);
        // no remove
        assertThrows(UnsupportedOperationException.class, ai0::remove);
    }

    @Test
    public void testArrayListWrapper() {
        ArrayListWrapper alw;
        assertThrows(IllegalArgumentException.class, () -> new ArrayListWrapper(1));
        final Integer[] ai = { 1, 2 };
        alw = new ArrayListWrapper(ai);
        assertEquals(1, alw.indexOf(2));
        assertEquals(-1, alw.indexOf(null));
    }

    @Test
    public void testEmptyContext() {
        assertThrows(UnsupportedOperationException.class, () -> JexlEngine.EMPTY_CONTEXT.set("nope", 42));
    }

}
