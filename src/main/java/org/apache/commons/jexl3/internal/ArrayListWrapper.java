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
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A class that wraps an array within an AbstractList.
 * <p>
 * It overrides all methods because introspection uses this class a a marker for wrapped arrays; the declared class
 * for any method is thus always ArrayListWrapper.
 * </p>
 */
public class ArrayListWrapper extends AbstractList<Object> {
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

    /** {@inheritDoc} */
    @Override
    public Object get(int index) {
        return Array.get(array, index);
    }

    /** {@inheritDoc} */
    @Override
    public Object set(int index, Object element) {
        Object old = get(index);
        Array.set(array, index, element);
        return old;
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return Array.getLength(array);
    }

    @Override
    public Object[] toArray() {
        final int size = size();
        Object[] a = new Object[size];
        for(int i = 0; i < size; ++i) {
            a[i] = get(i);
        }
        return a;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size) {
            T[] x = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            System.arraycopy(a, a.length, x, 0, a.length);
        }
        for(int i = 0; i < size; ++i) {
            a[i] = (T) get(i);
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
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
    
    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return super.iterator();
    }
    
    @Override
    public boolean containsAll(Collection<?> c) {
        return super.containsAll(c);
    }

    @Override
    public int lastIndexOf(Object o) {
        return super.lastIndexOf(o);
    }

    @Override
    public ListIterator<Object> listIterator() {
        return super.listIterator();
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        return super.listIterator(index);
    }

    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return super.subList(fromIndex, toIndex);
    }
    
    @Override
    public boolean add(Object o) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean addAll(Collection<? extends Object> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean addAll(int index, Collection<? extends Object> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void add(int index, Object element) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Object remove(int index) {
        throw new UnsupportedOperationException("Not supported.");
    }

}