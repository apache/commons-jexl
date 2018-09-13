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

import org.apache.commons.jexl3.JexlArithmetic;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to create typed arrays.
 */
public class ArrayBuilder implements JexlArithmetic.ArrayBuilder {
    /** The number of primitive types. */
    private static final int PRIMITIVE_SIZE = 8;
    /** The boxing types to primitive conversion map. */
    private static final Map<Class<?>, Class<?>> BOXING_CLASSES;
    static {
        BOXING_CLASSES = new IdentityHashMap<Class<?>, Class<?>>(PRIMITIVE_SIZE);
        BOXING_CLASSES.put(Boolean.class, Boolean.TYPE);
        BOXING_CLASSES.put(Byte.class, Byte.TYPE);
        BOXING_CLASSES.put(Character.class, Character.TYPE);
        BOXING_CLASSES.put(Double.class, Double.TYPE);
        BOXING_CLASSES.put(Float.class, Float.TYPE);
        BOXING_CLASSES.put(Integer.class, Integer.TYPE);
        BOXING_CLASSES.put(Long.class, Long.TYPE);
        BOXING_CLASSES.put(Short.class, Short.TYPE);
    }

    /**
     * Gets the primitive type of a given class (when it exists).
     * @param parm a class
     * @return the primitive type or null it the argument is not unboxable
     */
    protected static Class<?> unboxingClass(Class<?> parm) {
        Class<?> prim = BOXING_CLASSES.get(parm);
        return prim == null ? parm : prim;
    }

    protected static Object[] EMPTY_ARRAY = new Object[0];

    /** The intended class array. */
    protected Class<?> commonClass = null;
    /** Whether the array stores numbers. */
    protected boolean isNumber = true;
    /** Whether we can try unboxing. */
    protected boolean unboxing = true;
    /** The untyped list of items being added. */
    protected final ArrayList<Object> untyped;

    /**
     * Creates a new builder.
     * @param size the initial array size
     */
    public ArrayBuilder(int size) {
        untyped = new ArrayList<Object> (size);
    }

    @Override
    public void add(Object value) {
        // for all children after first...
        if (!Object.class.equals(commonClass)) {
            if (value == null) {
                isNumber = false;
                unboxing = false;
            } else {
                Class<?> eclass = value.getClass();
                // base common class on first non-null entry
                if (commonClass == null) {
                    commonClass = eclass;
                    isNumber = isNumber && Number.class.isAssignableFrom(commonClass);
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
        untyped.add(value);
    }

    @Override
    public Object create(boolean extended) {
        if (extended) {
            return untyped;
        }

        if (commonClass == null)
            commonClass = Object.class;

        // convert untyped array to the common class
        final int size = untyped.size();
        // if the commonClass is a number, it has an equivalent primitive type, get it
        if (unboxing) {
            commonClass = unboxingClass(commonClass);
        }

        if (commonClass == Object.class) {
            return untyped.size() > 0 ? untyped.toArray() : EMPTY_ARRAY;
        }

        // allocate and fill up the typed array
        Object typed = Array.newInstance(commonClass, size);
        for (int i = 0; i < size; ++i) {
            Array.set(typed, i, untyped.get(i));
        }
        return typed;
    }
}
