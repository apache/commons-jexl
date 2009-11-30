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

package org.apache.commons.jexl2.introspection;

import org.apache.commons.jexl2.util.Introspector;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Iterator;

import java.util.Map;
import org.apache.commons.jexl2.JexlInfo;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.util.AbstractExecutor;
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
public class UberspectImpl extends Introspector implements Uberspect {
    /**
     * Creates a new UberspectImpl.
     * @param runtimeLogger the logger used for all logging needs
     */
    public UberspectImpl(Log runtimeLogger) {
        super(runtimeLogger);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Iterator<?> getIterator(Object obj, JexlInfo info) {
        if (obj instanceof Iterator<?>) {
            return ((Iterator<?>) obj);
        }
        if (obj.getClass().isArray()) {
            return new ArrayIterator(obj);
        }
        if (obj instanceof Map<?,?>) {
            return ((Map<?,?>) obj).values().iterator();
        }
        if (obj instanceof Enumeration<?>) {
            return new EnumerationIterator<Object>((Enumeration<Object>) obj);
        }
        if (obj instanceof Iterable<?>) {
            return ((Iterable<?>) obj).iterator();
        }
        try {
            // look for an iterator() method to support the JDK5 Iterable
            // interface or any user tools/DTOs that want to work in
            // foreach without implementing the Collection interface
            AbstractExecutor.Method it = getMethodExecutor(obj, "iterator", null);
            if (it != null && Iterator.class.isAssignableFrom(it.getReturnType())) {
                return (Iterator<Object>) it.execute(obj, null);
            }
        } catch(Exception xany) {
            throw new JexlException(info, "unable to generate iterator()", xany);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
   public Constructor<?> getConstructor(Object ctorHandle, Object[] args, JexlInfo info) {
        return getConstructor(ctorHandle, args);
   }

    /**
     * {@inheritDoc}
     */
    public JexlMethod getMethod(Object obj, String method, Object[] args, JexlInfo info) {
        return getMethodExecutor(obj, method, args);
    }

    /**
     * {@inheritDoc}
     */
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier, JexlInfo info) {
        return getGetExecutor(obj, identifier);
    }

    /**
     * {@inheritDoc}
     */
    public JexlPropertySet getPropertySet(final Object obj, final Object identifier, Object arg, JexlInfo info) {
        return getSetExecutor(obj, identifier, arg);
    }
}