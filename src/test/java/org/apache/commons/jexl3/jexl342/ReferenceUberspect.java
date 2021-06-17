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
package org.apache.commons.jexl3.jexl342;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An Uberspect that handles references (java.lang.ref.Reference) and optionals (java.util.Optional).
 * <p>This illustrates JEXL&quote;s low level customization capabilities.</p>
 * see JEXL-342.
 */
public class ReferenceUberspect implements JexlUberspect {
    /** The uberspect we delegate to. */
    private final JexlUberspect uberspect;
    /**
     * The pojo resolver list strategy.
     */
    private final List<PropertyResolver> pojoStrategy;
    /**
     * The map resolver list strategy.
     */
    private final List<PropertyResolver> mapStrategy;

    /**
     * Constructor.
     * @param jexlUberspect the uberspect to delegate to
     */
    public ReferenceUberspect(JexlUberspect jexlUberspect) {
        uberspect = jexlUberspect;
        PropertyResolver find = new PropertyResolver() {
            @Override
            public JexlPropertyGet getPropertyGet(JexlUberspect uber, Object obj, Object identifier) {
                return discoverFind(uberspect, obj.getClass(), identifier.toString());
            }

            @Override
            public JexlPropertySet getPropertySet(JexlUberspect uber, Object obj, Object identifier, Object arg) {
                return null;
            }
        };
        pojoStrategy = Arrays.asList(
            JexlResolver.PROPERTY,
            find,
            JexlResolver.MAP,
            JexlResolver.LIST,
            JexlResolver.DUCK,
            JexlResolver.FIELD,
            JexlResolver.CONTAINER);
        mapStrategy = Arrays.asList(
            JexlResolver.MAP,
            JexlResolver.LIST,
            JexlResolver.DUCK,
            JexlResolver.PROPERTY,
            find,
            JexlResolver.FIELD,
            JexlResolver.CONTAINER);
    }

    /**
     * A JEXL strategy improved with optionals/references.
     */
    @Override
    public List<PropertyResolver> getResolvers(JexlOperator op, Object obj) {
        if (op == JexlOperator.ARRAY_GET) {
            return mapStrategy;
        }
        if (op == JexlOperator.ARRAY_SET) {
            return mapStrategy;
        }
        if (op == null && obj instanceof Map) {
            return mapStrategy;
        }
        return pojoStrategy;
    }

    /**
     * Duck-reference handler, calls get().
     */
    @FunctionalInterface
    interface ReferenceHandler {
        /**
         * Performs a call to get().
         * @param ref the reference
         * @return the value pointed by the reference
         */
        Object callGet(Object ref);
    }

    /**
     * Find a reference handler for a given instance.
     * @param ref the reference
     * @return the handler or null if object can not be handled
     */
    private static ReferenceHandler discoverHandler(Object ref) {
        // optional support
        if (ref instanceof Optional<?>) {
            return ReferenceUberspect::handleOptional;
        }
        // atomic ref
        if (ref instanceof AtomicReference<?>) {
            return ReferenceUberspect::handleAtomic;
        }
        // a reference
        if (ref instanceof Reference<?>) {
            return ReferenceUberspect::handleReference;
        }
        // delegate
        return null;
    }

    /**
     * Cast ref to optional, call isPresent()/get().
     * @param ref the reference
     * @return the get() result
     */
    static Object handleOptional(Object ref) {
        // optional support
        Optional<?> optional = (Optional<?>) ref;
        return optional.isPresent() ? optional.get() : null;
    }

    /**
     * Cast ref to atomic reference, call get().
     * @param ref the reference
     * @return the get() result if not null, ref otherwise
     */
    static Object handleAtomic(Object ref) {
        Object obj = ((AtomicReference<?>) ref).get();
        return obj == null? ref : obj;
    }

    /**
     * Cast ref to reference, call get().
     * @param ref the reference
     * @return the get() result if not null, ref otherwise
     */
    static Object handleReference(Object ref) {
        Object obj = ((Reference<?>) ref).get();
        return obj == null? ref : obj;
    }

    @Override
    public void setClassLoader(ClassLoader loader) {
        uberspect.setClassLoader(loader);
    }

    @Override
    public ClassLoader getClassLoader() {
        return uberspect.getClassLoader();
    }

    @Override
    public int getVersion() {
        return uberspect.getVersion();
    }

    @Override
    public JexlMethod getConstructor(Object ctorHandle, Object... args) {
        return uberspect.getConstructor(ctorHandle, args);
    }

    @Override
    public JexlMethod getMethod(Object ref, String method, Object... args) {
        // is this is a reference of some kind?
        ReferenceHandler handler = discoverHandler(ref);
        if (handler == null) {
            return uberspect.getMethod(ref, method, args);
        }
        // do we have an object referenced ?
        Object obj = handler.callGet(ref);
        if (ref == obj) {
            return null;
        }
        JexlMethod jexlMethod = null;
        if (obj != null) {
            jexlMethod = uberspect.getMethod(obj, method, args);
            if (jexlMethod == null) {
                throw new JexlException.Method(null, method, args, null);
            }
        } else {
            jexlMethod = new OptionalNullMethod(uberspect, method);
        }
        return new ReferenceMethodExecutor(handler, jexlMethod);
    }

    @Override
    public JexlPropertyGet getPropertyGet(Object obj, Object identifier) {
        return getPropertyGet(null, obj, identifier);
    }

    @Override
    public JexlPropertyGet getPropertyGet(List<PropertyResolver> resolvers, Object ref, Object identifier) {
        // is this is a reference of some kind?
        ReferenceHandler handler = discoverHandler(ref);
        if (handler == null) {
            return uberspect.getPropertyGet(resolvers, ref, identifier);
        }
        // do we have an object referenced ?
        Object obj = handler.callGet(ref);
        if (ref == obj) {
            return null;
        }
        // obj is null means proper dereference of an optional; we dont have an object,
        // we can not determine jexlGet, not a pb till we call with a not-null object
        // since the result is likely to be not null... TryInvoke will fail and invoke will throw.
        // from that object, get the property getter if any
        JexlPropertyGet jexlGet = null;
        if (obj != null) {
            jexlGet = uberspect.getPropertyGet(resolvers, obj, identifier);
            if (jexlGet == null) {
                throw new JexlException.Property(null, Objects.toString(identifier), false, null);
            }
        } else {
            jexlGet = new OptionalNullGetter(uberspect, identifier);
        }
        return new ReferenceGetExecutor(handler, jexlGet);
    }

    @Override
    public JexlPropertySet getPropertySet(Object obj, Object identifier, Object arg) {
        return getPropertySet(null, obj, identifier, arg);
    }

    @Override
    public JexlPropertySet getPropertySet(List<PropertyResolver> resolvers, Object ref, Object identifier, Object arg) {
        // is this is a reference of some kind?
        ReferenceHandler handler = discoverHandler(ref);
        if (handler == null) {
            return uberspect.getPropertySet(resolvers, ref, identifier, arg);
        }
        // do we have an object referenced ?
        Object obj = handler.callGet(ref);
        if (ref  == obj) {
            return null;
        }
        // from that object, get the property setter if any
        JexlPropertySet jexlSet = null;
        if (obj != null) {
            jexlSet = uberspect.getPropertySet(resolvers, obj, identifier, arg);
            if (jexlSet == null) {
                throw new JexlException.Property(null, Objects.toString(identifier), false, null);
            }
        } else {
            // postpone resolution till not null
            jexlSet = new OptionalNullSetter(uberspect, identifier);
        }
        return new ReferenceSetExecutor(handler, jexlSet);
    }

    @Override
    public Iterator<?> getIterator(Object ref) {
        // is this is a reference of some kind?
        ReferenceHandler handler = discoverHandler(ref);
        if (handler == null) {
            return uberspect.getIterator(ref);
        }
        // do we have an object referenced ?
        Object obj = handler.callGet(ref);
        if (ref == obj) {
            return null;
        }
        if (obj == null) {
            return Collections.emptyIterator();
        }
        return uberspect.getIterator(obj);
    }

    @Override
    public JexlArithmetic.Uberspect getArithmetic(JexlArithmetic arithmetic) {
        return uberspect.getArithmetic(arithmetic);
    }


    /** A static signature for method(). */
    private static final Object[] EMPTY_PARAMS = {};

    /**
     * Discovers a an optional getter.
     * <p>The method to be found should be named "{find}{P,p}property and return an Optional&lt;?&gt;.</p>
     *
     * @param is the uberspector
     * @param clazz the class to find the get method from
     * @param property the property name to find
     * @return the executor if found, null otherwise
     */
    private static JexlPropertyGet discoverFind(final JexlUberspect is, final Class<?> clazz, final String property) {
        if (property == null || property.isEmpty()) {
            return null;
        }
        //  this is gross and linear, but it keeps it straightforward.
        JexlMethod method;
        final int start = 4; // "find".length() == 4
        // start with get<Property>
        final StringBuilder sb = new StringBuilder("find");
        sb.append(property);
        // uppercase nth char
        final char c = sb.charAt(start);
        sb.setCharAt(start, Character.toUpperCase(c));
        method = is.getMethod(clazz, sb.toString(), EMPTY_PARAMS);
        //lowercase nth char
        if (method == null) {
            sb.setCharAt(start, Character.toLowerCase(c));
            method = is.getMethod(clazz, sb.toString(), EMPTY_PARAMS);
        }
        if (method != null && Optional.class.equals(method.getReturnType())) {
            final JexlMethod getter = method;
            final String name = sb.toString();
            return new JexlPropertyGet() {
                @Override
                public Object invoke(Object obj) throws Exception {
                    return getter.invoke(obj);
                }

                @Override
                public Object tryInvoke(Object obj, Object key) throws JexlException.TryFailed {
                    return !Objects.equals(property, key) ? JexlEngine.TRY_FAILED : getter.tryInvoke(name, obj);
                }

                @Override
                public boolean tryFailed(Object rval) {
                    return rval == JexlEngine.TRY_FAILED;
                }

                @Override
                public boolean isCacheable() {
                    return getter.isCacheable();
                }
            };
        }
        return null;
    }

}
