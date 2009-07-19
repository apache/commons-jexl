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

package org.apache.commons.jexl.util;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Abstract class that is used to execute an arbitrary
 * method that is introspected. This is the superclass
 * for the GetExecutor and PropertyExecutor.
 *
 * @since 1.0
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public abstract class AbstractExecutor {
    /** Empty parameters list for method matching. */
    protected static final Object[] EMPTY_PARAMS = {};
    /**
     * Method to be executed.
     */
    protected final Method method;

    /**
     * Default and sole constructor.
     * @param theMethod the method held by this executor
     */
    protected AbstractExecutor(Method theMethod) {
        method = theMethod;
    }

    /**
     * Execute method against context.
     *
     * @param o The owner.
     * @return The return value.
     * @throws IllegalAccessException Method is inaccessible.
     * @throws InvocationTargetException Method body throws an exception.
     */
    public Object execute(Object o)
            throws IllegalAccessException, InvocationTargetException {
        return method == null? null : method.invoke(o, (Object[]) null);
    }


    /**
     * Tell whether the executor is alive by looking
     * at the value of the method.
     *
     * @return boolean Whether the executor is alive.
     */
    public final boolean isAlive() {
        return (method != null);
    }

    /**
     * Gets the method to be executed.
     * @return Method The method to be executed.
     */
    public final Method getMethod() {
        return method;
    }
 }