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

package org.apache.commons.jexl2.util.introspection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.jexl2.util.ArrayIterator;
import org.apache.commons.jexl2.util.EnumerationIterator;
import org.apache.commons.logging.Log;

/**
 * Implementation of Uberspect to provide the default introspective
 * functionality of JEXL.
 * <p>This is the class to derive to customize introspection.</p>
 *
 * @since 1.0
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class UberspectImpl extends org.apache.commons.jexl2.util.Introspector implements Uberspect {
    /** {@inheritDoc} */
    public Introspector getIntrospector() {
        return introspector.get();
    }

    /**
     * Creates a new UberspectImpl.
     * @param runtimeLogger the logger used for all logging needs
     */
    public UberspectImpl(Log runtimeLogger) {
        this(runtimeLogger, new Introspector(runtimeLogger));
    }

    /**
     * Creates a new UberspectImpl.
     * @param runtimeLogger the logger used for all logging needs
     * @param intro the introspector to use
     */
    public UberspectImpl(Log runtimeLogger, Introspector intro) {
        super(runtimeLogger, intro);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<?> getIterator(Object obj, DebugInfo info) {
        if (obj.getClass().isArray()) {
            return new ArrayIterator(obj);
        } else if (obj instanceof Collection<?>) {
            return ((Collection<?>) obj).iterator();
        } else if (obj instanceof Map<?,?>) {
            return ((Map<?,?>) obj).values().iterator();
        } else if (obj instanceof Iterator<?>) {
            rlog.warn(info.debugString()
                    + "The iterator is not resetable; if used more than once, "
                    + "this may lead to unexpected results.");

            return ((Iterator<?>) obj);
        } else if (obj instanceof Enumeration<?>) {
            rlog.warn(info.debugString()
                    + "The iterator is not resetable; if used more than once, "
                    + "this may lead to unexpected results.");
            @SuppressWarnings("unchecked") // OK because of instanceof check above
            final Enumeration<Object> enumObj = (Enumeration<Object>) obj;
            return new EnumerationIterator<Object>(enumObj);
        } else {
            // look for an iterator() method to support the JDK5 Iterable
            // interface or any user tools/DTOs that want to work in
            // foreach without implementing the Collection interface
            Class<?> type = obj.getClass();
            try {
                Method iter = type.getMethod("iterator", (Class<?>[]) null);
                Class<?> returns = iter.getReturnType();
                if (Iterator.class.isAssignableFrom(returns)) {
                    return (Iterator<?>) iter.invoke(obj, (Object[])null);
                } else {
                    rlog.error(info.debugString()
                            + "iterator() method does not return a true Iterator.");
                }
            // CSOFF: EmptyBlock
            } catch (NoSuchMethodException nsme) {
                // eat this one, but let all other exceptions thru
            } catch (IllegalArgumentException e) { // CSON: EmptyBlock
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        /*  we have no clue what this is  */
        rlog.warn(info.toString()
                + "Could not determine type of iterator");

        return null;
    }

    /**
     * {@inheritDoc}
     */
   public Constructor<?> getConstructor(Object ctorHandle, Object[] args, DebugInfo info) {
        return getConstructor(ctorHandle, args);
   }

    /**
     * {@inheritDoc}
     */
    public JexlMethod getMethod(Object obj, String method, Object[] args, DebugInfo info) {
        return getMethodExecutor(obj, method, args);
    }

    /**
     * {@inheritDoc}
     */
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier, DebugInfo info) {
        return getGetExecutor(obj, identifier);
    }

    /**
     * {@inheritDoc}
     */
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, Object arg, DebugInfo info) {
        return getSetExecutor(obj, identifier, arg);
    }
}