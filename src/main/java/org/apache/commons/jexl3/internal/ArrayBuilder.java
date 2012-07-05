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
import java.lang.reflect.Field;
import org.apache.commons.jexl3.JexlArithmetic;

/**
 * Helper class to create typed arrays.
 */
public class ArrayBuilder implements JexlArithmetic.ArrayBuilder {
    /** The intended class array. */
    private Class<?> commonClass = null;
    /** Whether the array stores numbers. */
    private boolean isNumber = true;
    /** The untyped list of items being added. */
    private final Object[] untyped;
    /** Number of added items. */
    private int added = 0;

    /**
     * Creates a new builder.
     * @param size the exact array size
     */
    public ArrayBuilder(int size) {
        untyped = new Object[size];
    }

    @Override
    public void add(Object value) {
        // for all children after first...
        if (!Object.class.equals(commonClass)) {
            if (value == null) {
                isNumber = false;
            } else {
                Class<?> eclass = value.getClass();
                // base common class on first non-null entry
                if (commonClass == null) {
                    commonClass = eclass;
                    isNumber &= Number.class.isAssignableFrom(commonClass);
                } else if (!commonClass.equals(eclass)) {
                    // if both are numbers...
                    if (isNumber && Number.class.isAssignableFrom(eclass)) {
                        commonClass = Number.class;
                    } else {
                        // attempt to find valid superclass
                        do {
                            eclass = eclass.getSuperclass();
                            if (eclass == null) {
                                commonClass = Object.class;
                                break;
                            }
                        } while (!commonClass.isAssignableFrom(eclass));
                    }
                }
            }
        }
        if (added >= untyped.length) {
            throw new IllegalArgumentException("add() over size");
        }
        untyped[added++] = value;
    }

    @Override
    public Object create() {
        if (untyped != null) {
            int size = untyped.length;
            // convert untyped array to the common class if not Object.class
            if (commonClass != null && !Object.class.equals(commonClass)) {
                // if the commonClass is a number, it has an equivalent primitive type, get it
                if (isNumber) {
                    try {
                        final Field type = commonClass.getField("TYPE");
                        commonClass = (Class<?>) type.get(null);
                    } catch (Exception xany) {
                        // ignore
                    }
                }
                // allocate and fill up the typed array
                Object typed = Array.newInstance(commonClass, size);
                for (int i = 0; i < size; ++i) {
                    Array.set(typed, i, untyped[i]);
                }
                return typed;
            } else {
                return untyped;
            }
        } else {
            return new Object[0];
        }
    }
}
