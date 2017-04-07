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
import java.util.AbstractList;
import java.util.RandomAccess;

/**
 * A class that wraps an array within an AbstractList.
 * <p>
 * It overrides some methods because introspection uses this class a a marker for wrapped arrays; the declared class
 * for these method is thus ArrayListWrapper.
 * The methods are get/set/size/contains and indexOf because it is used by contains.
 * </p>
 */
public class ArrayListWrapper extends AbstractList<Object> implements RandomAccess {
    /** the array to wrap. */
    private final Object array;

    /**
     * Create the wrapper.
     * @param anArray {@link #array}
     */
    public ArrayListWrapper(Object anArray) {
        if (!anArray.getClass().isArray()) {
            throw new IllegalArgumentException(anArray.getClass() + " is not an array");
        }
        this.array = anArray;
    }

    @Override
    public Object get(int index) {
        return Array.get(array, index);
    }

    @Override
    public Object set(int index, Object element) {
        Object old = Array.get(array, index);
        Array.set(array, index, element);
        return old;
    }

    @Override
    public int size() {
        return Array.getLength(array);
    }

    @Override
    public int indexOf(Object o) {
        final int size = size();
        if (o == null) {
            for (int i = 0; i < size; i++) {
                if (get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (o.equals(get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }
}