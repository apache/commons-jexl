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


import java.util.Iterator;
import java.util.Enumeration;

/**
 * An Iterator wrapper for an Enumeration.
 * @param <T> the type of object this iterator returns
 * @since 1.0
 */
public class EnumerationIterator<T> implements Iterator<T> {
    /**
     * The enumeration to iterate over.
     */
    private final Enumeration<T> enumeration;

    /**
     * Creates a new iteratorwrapper instance for the specified
     * Enumeration.
     *
     * @param enumer  The Enumeration to wrap.
     */
    public EnumerationIterator(Enumeration<T> enumer) {
        enumeration = enumer;
    }

    @Override
    public T next() {
        return enumeration.nextElement();
    }

    @Override
    public boolean hasNext() {
        return enumeration.hasMoreElements();
    }

    @Override
    public void remove() {
        // not implemented
    }
}