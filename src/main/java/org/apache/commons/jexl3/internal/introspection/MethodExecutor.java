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

import org.apache.commons.jexl3.JexlEngine;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.jexl3.JexlException;

/**
 * Specialized executor to invoke a method on an object.
 * @since 2.0
 */
public final class MethodExecutor extends AbstractExecutor.Method {
    /** If this method is a vararg method, vaStart is the last argument index. */
    private final int vaStart;
    /** If this method is a vararg method, vaClass is the component type of the vararg array. */
    private final Class<?> vaClass;

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
        int vastart = -1;
        Class<?> vaclass = null;
        if (MethodKey.isVarArgs(method)) {
            // if the last parameter is an array, the method is considered as vararg
            Class<?>[] formal = method.getParameterTypes();
            vastart = formal.length - 1;
            vaclass = formal[vastart].getComponentType();
        }
        vaStart = vastart;
        vaClass = vaclass;
    }

    @Override
    public Object invoke(Object o, Object... args) throws IllegalAccessException, InvocationTargetException {
        if (vaClass != null && args != null) {
            args = handleVarArg(args);
        }
        if (method.getDeclaringClass() == ArrayListWrapper.class && o.getClass().isArray()) {
            return method.invoke(new ArrayListWrapper(o), args);
        } else {
            return method.invoke(o, args);
        }
    }

    @Override
    public Object tryInvoke(String name, Object obj, Object... args) {
        MethodKey tkey = new MethodKey(name, args);
        // let's assume that invocation will fly if the declaring class is the
        // same and arguments have the same type
        if (objectClass.equals(obj.getClass()) && tkey.equals(key)) {
            try {
                return invoke(obj, args);
            } catch (IllegalAccessException xill) {
                return TRY_FAILED;// fail
            } catch (IllegalArgumentException xarg) {
                return TRY_FAILED;// fail
            } catch (InvocationTargetException xinvoke) {
                throw JexlException.tryFailed(xinvoke); // throw
            }
        }
        return JexlEngine.TRY_FAILED;
    }


    /**
     * Reassembles arguments if the method is a vararg method.
     * @param actual The actual arguments being passed to this method
     * @return The actual parameters adjusted for the varargs in order
     * to fit the method declaration.
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    private Object[] handleVarArg(Object[] actual) {
        final Class<?> vaclass = vaClass;
        final int vastart = vaStart;
        // variable arguments count
        final int varargc = actual.length - vastart;
        // if no values are being passed into the vararg, size == 0
        if (varargc == 1) {
            // if one non-null value is being passed into the vararg,
            // and that arg is not the sole argument and not an array of the expected type,
            // make the last arg an array of the expected type
            if (actual[vastart] != null) {
                Class<?> aclazz = actual[vastart].getClass();
                if (!aclazz.isArray() || !vaclass.isAssignableFrom(aclazz.getComponentType())) {
                    // create a 1-length array to hold and replace the last argument
                    Object lastActual = Array.newInstance(vaclass, 1);
                    Array.set(lastActual, 0, actual[vastart]);
                    actual[vastart] = lastActual;
                }
            }
            // else, the vararg is null and used as is, considered as T[]
        } else {
            // if no or multiple values are being passed into the vararg,
            // put them in an array of the expected type
            Object varargs = Array.newInstance(vaclass, varargc);
            System.arraycopy(actual, vastart, varargs, 0, varargc);
            // put all arguments into a new actual array of the appropriate size
            Object[] newActual = new Object[vastart + 1];
            System.arraycopy(actual, 0, newActual, 0, vastart);
            newActual[vastart] = varargs;
            // replace the old actual array
            actual = newActual;
        }
        return actual;
    }
}


