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

import java.util.Iterator;

/**
 * A range of integers.
 */
public class IntegerRange implements Iterable<Integer> {
    /** The lower boundary. */
    private final int low;
    /** The upper boundary. */
    private final int high;

    /**
     * Creates a new range.
     * @param from the lower inclusive boundary
     * @param to  the higher inclusive boundary
     */
    public IntegerRange(int from, int to) {
        low = from;
        high = to;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new IntegerIterator(low, high);
    }
}

/**
 * An iterator on an integer range.
 */
class IntegerIterator implements Iterator<Integer> {
    /** The lower boundary. */
    private final int low;
    /** The upper boundary. */
    private final int high;
    /** The current value. */
    private int cursor;
    /**
     * Creates a iterator on the range.
     * @param l low boundary
     * @param h high boundary
     */
    public IntegerIterator(int l, int h) {
        low = l;
        high = h;
        cursor = low;
    }

    @Override
    public boolean hasNext() {
        return cursor <= high;
    }

    @Override
    public Integer next() {
        if (cursor <= high) {
            int next = cursor;
            cursor += 1;
            return Integer.valueOf(next);
        } else {
            return null;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }
}
