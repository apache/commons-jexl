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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A range of longs.
 * <p>
 * Behaves as a readonly collection of longs.
 */
public abstract class LongRange implements Collection<Long> {
    /** The lower boundary. */
    protected final long min;
    /** The upper boundary. */
    protected final long max;

    /**
     * Creates a range, ascending or descending depending on boundaries order.
     * @param from the lower inclusive boundary
     * @param to   the higher inclusive boundary
     * @return a range
     */
    public static LongRange create(long from, long to) {
        if (from <= to) {
            return new LongRange.Ascending(from, to);
        } else {
            return new LongRange.Descending(to, from);
        }
    }

    /**
     * Creates a new range.
     * @param from the lower inclusive boundary
     * @param to   the higher inclusive boundary
     */
    protected LongRange(long from, long to) {
        min = from;
        max = to;
    }

    /**
     * Gets the interval minimum value.
     * @return the low boundary
     */
    public long getMin() {
        return min;
    }

    /**
     * Gets the interval maximum value.
     * @return the high boundary
     */
    public long getMax() {
        return max;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        //CSOFF: MagicNumber
        hash = 13 * hash + (int) (this.min ^ (this.min >>> 32));
        hash = 13 * hash + (int) (this.max ^ (this.max >>> 32));
        //CSON: MagicNumber
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LongRange other = (LongRange) obj;
        if (this.min != other.min) {
            return false;
        }
        if (this.max != other.max) {
            return false;
        }
        return true;
    }

    @Override
    public abstract Iterator<Long> iterator();

    @Override
    public int size() {
        return (int) (max - min + 1);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Number) {
            long v = ((Number) o).longValue();
            return min <= v && v <= max;
        } else {
            return false;
        }
    }

    @Override
    public Object[] toArray() {
        final int size = size();
        Object[] array = new Object[size];
        for (int a = 0; a < size; ++a) {
            array[a] = min + a;
        }
        return array;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] array) {
        final Class<?> ct = array.getClass().getComponentType();
        final int length = size();
        T[] copy = array;
        if (ct.isAssignableFrom(Long.class)) {
            if (array.length < length) {
                copy = ct == Object.class
                       ? (T[]) new Object[length]
                       : (T[]) Array.newInstance(ct, length);
            }
            for (int a = 0; a < length; ++a) {
                Array.set(copy, a, min + a);
            }
            if (length < array.length) {
                array[length] = null;
            }
            return copy;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object cc : c) {
            if (!contains(cc)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean add(Long e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends Long> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Ascending long range.
     */
    public static class Ascending extends LongRange {
        /**
         * Constructor.
         * @param from lower boundary
         * @param to upper boundary
         */
        protected Ascending(long from, long to) {
            super(from, to);
        }

        @Override
        public Iterator<Long> iterator() {
            return new AscLongIterator(min, max);
        }
    }

    /**
     * Descending long range.
     */
    public static class Descending extends LongRange {
        /**
         * Constructor.
         * @param from upper boundary
         * @param to lower boundary
         */
        protected Descending(long from, long to) {
            super(from, to);
        }

        @Override
        public Iterator<Long> iterator() {
            return new DescLongIterator(min, max);
        }
    }
}

/**
 * An iterator on a long range.
 */
class AscLongIterator implements Iterator<Long> {
    /** The lower boundary. */
    private final long min;
    /** The upper boundary. */
    private final long max;
    /** The current value. */
    private long cursor;

    /**
     * Creates a iterator on the range.
     * @param l low boundary
     * @param h high boundary
     */
    AscLongIterator(long l, long h) {
        min = l;
        max = h;
        cursor = min;
    }

    @Override
    public boolean hasNext() {
        return cursor <= max;
    }

    @Override
    public Long next() {
        if (cursor <= max) {
            return cursor++;
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

/**
 * An iterator on a long range.
 */
class DescLongIterator implements Iterator<Long> {
    /** The lower boundary. */
    private final long min;
    /** The upper boundary. */
    private final long max;
    /** The current value. */
    private long cursor;

    /**
     * Creates a iterator on the range.
     * @param l low boundary
     * @param h high boundary
     */
    DescLongIterator(long l, long h) {
        min = l;
        max = h;
        cursor = max;
    }

    @Override
    public boolean hasNext() {
        return cursor >= min;
    }

    @Override
    public Long next() {
        if (cursor >= min) {
            return cursor--;
        }
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
