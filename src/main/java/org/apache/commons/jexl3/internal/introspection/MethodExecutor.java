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
import java.lang.reflect.InvocationTargetException;

/**
 * Specialized executor to invoke a method on an object.
 * @since 2.0
 */
public final class MethodExecutor extends AbstractExecutor.Method {
    /** Whether this method handles varargs. */
    private final boolean isVarArgs;


    /**
     * Discovers a {@link MethodExecutor}.
     * <p>
     * If the object is an array, an attempt will be made to find the
     * method in a List (see {@link ArrayListWrapper})
     * </p>
     * <p>
     * If the object is a class, an attempt will be made to find the
     * method as a static method of that class.
     * </p>
     * @param is the introspector used to discover the method
     * @param obj the object to introspect
     * @param method the name of the method to find
     * @param args the method arguments
     * @return a filled up parameter (may contain a null method)
     */
    public static MethodExecutor discover(Introspector is, Object obj, String method, Object[] args) {
        final Class<?> clazz = obj.getClass();
        final MethodKey key = new MethodKey(method, args);
        java.lang.reflect.Method m = is.getMethod(clazz, key);
        if (m == null && clazz.isArray()) {
            // check for support via our array->list wrapper
            m = is.getMethod(ArrayListWrapper.class, key);
        }
        if (m == null && obj instanceof Class<?>) {
            m = is.getMethod((Class<?>) obj, key);
        }
        return m == null? null : new MethodExecutor(clazz, m, key);
    }

    /**
     * Creates a new instance.
     * @param c the class this executor applies to
     * @param m the method
     * @param k the MethodKey
     */
    private MethodExecutor(Class<?> c, java.lang.reflect.Method m, MethodKey k) {
        super(c, m, k);
        isVarArgs = method != null && isVarArgMethod(method);
    }

    @Override
    public Object invoke(Object o, Object[] args) throws IllegalAccessException, InvocationTargetException  {
        if (isVarArgs) {
            Class<?>[] formal = method.getParameterTypes();
            int index = formal.length - 1;
            Class<?> type = formal[index].getComponentType();
            if (args.length >= index) {
                args = handleVarArg(type, index, args);
            }
        }
        if (method.getDeclaringClass() == ArrayListWrapper.class && o.getClass().isArray()) {
            return method.invoke(new ArrayListWrapper(o), args);
        } else {
            return method.invoke(o, args);
        }
    }

    @Override
    public Object tryInvoke(String name, Object obj, Object[] args) {
        MethodKey tkey = new MethodKey(name, args);
        // let's assume that invocation will fly if the declaring class is the
        // same and arguments have the same type
        if (objectClass.equals(obj.getClass()) && tkey.equals(key)) {
            try {
                return invoke(obj, args);
            } catch (InvocationTargetException xinvoke) {
                return TRY_FAILED; // fail
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            }
        }
        return TRY_FAILED;
    }


    /**
     * Reassembles arguments if the method is a vararg method.
     * @param type   The vararg class type (aka component type
     *               of the expected array arg)
     * @param index  The index of the vararg in the method declaration
     *               (This will always be one less than the number of
     *               expected arguments.)
     * @param actual The actual parameters being passed to this method
     * @return The actual parameters adjusted for the varargs in order
     * to fit the method declaration.
     */
    private Object[] handleVarArg(Class<?> type, int index, Object[] actual) {
        final int size = actual.length - index;
        // if no values are being passed into the vararg, size == 0
        if (size == 1) {
            // if one non-null value is being passed into the vararg,
            // and that arg is not the sole argument and not an array of the expected type,
            // make the last arg an array of the expected type
            if (actual[index] != null) {
                Class<?> aclazz = actual[index].getClass();
                if (!aclazz.isArray() || !type.isAssignableFrom(aclazz.getComponentType())) {
                    // create a 1-length array to hold and replace the last argument
                    Object lastActual = Array.newInstance(type, 1);
                    Array.set(lastActual, 0, actual[index]);
                    actual[index] = lastActual;
                }
            }
            // else, the vararg is null and used as is, considered as T[]
        } else {
            // if no or multiple values are being passed into the vararg,
            // put them in an array of the expected type
            Object lastActual = Array.newInstance(type, size);
            for (int i = 0; i < size; i++) {
                Array.set(lastActual, i, actual[index + i]);
            }

            // put all arguments into a new actual array of the appropriate size
            Object[] newActual = new Object[index + 1];
            System.arraycopy(actual, 0, newActual, 0, index);
            newActual[index] = lastActual;

            // replace the old actual array
            actual = newActual;
        }
        return actual;
    }

   /**
     * Determines if a method can accept a variable number of arguments.
     * @param m a the method to check
     * @return true if method is vararg, false otherwise
     */
    private static boolean isVarArgMethod(java.lang.reflect.Method m) {
        Class<?>[] formal = m.getParameterTypes();
        if (formal == null || formal.length == 0) {
            return false;
        } else {
            Class<?> last = formal[formal.length - 1];
            // if the last arg is an array, then
            // we consider this a varargs method
            return last.isArray();
        }
    }
}


