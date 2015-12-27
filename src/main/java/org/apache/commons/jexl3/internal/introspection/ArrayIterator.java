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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *  <p>
 *  An Iterator wrapper for an Object[]. This will
 *  allow us to deal with all array like structures
 *  in a consistent manner.
 *  </p>
 *  <p>
 *  WARNING : this class's operations are NOT synchronized.
 *  It is meant to be used in a single thread, newly created
 *  for each use in the #foreach() directive.
 *  If this is used or shared, synchronize in the
 *  next() method.
 *  </p>
 *
 * @since 1.0
 */
public class ArrayIterator implements Iterator<Object> {
    /** The objects to iterate over. */
    private final Object array;
    /** The size of the array. */
    private final int size;
    /** The current position and size in the array. */
    private int pos;

    /**
     * Creates a new iterator instance for the specified array.
     * @param arr The array for which an iterator is desired.
     */
    public ArrayIterator(Object arr) {
        if (arr == null) {
            array = null;
            pos = 0;
            size = 0;
        } else if (!arr.getClass().isArray()) {
            throw new IllegalArgumentException(arr.getClass() + " is not an array");
        } else {
            array = arr;
            pos = 0;
            size = Array.getLength(array);
        }
    }

    /**
     * Move to next element in the array.
     *
     * @return The next object in the array.
     */
    public Object next() {
        if (pos < size) {
            return Array.get(array, pos++);
        }
        // we screwed up...
        throw new NoSuchElementException("No more elements: " + pos
                                         + " / " + size);
    }

    /**
     * Check to see if there is another element in the array.
     *
     * @return Whether there is another element.
     */
    public boolean hasNext() {
        return (pos < size);
    }

    /**
     * No op--merely added to satify the <code>Iterator</code> interface.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}