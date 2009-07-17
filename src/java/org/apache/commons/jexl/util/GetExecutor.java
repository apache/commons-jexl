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

import org.apache.commons.logging.Log;


/**
 * Executor that simply tries to execute a get(key)
 * operation. This will try to find a get(key) method
 * for any type of object, not just objects that
 * implement the Map interface as was previously
 * the case.
 *
 * @since 1.0
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @version $Id$
 */
public class GetExecutor extends AbstractExecutor {
    /**
     * Container to hold the 'key' part of 
     * get(key).
     */
    private final Object[] args = new Object[1];
    
    /**
     * Default constructor.
     *
     * @param r The instance log.
     * @param ispect The JEXL introspector.
     * @param c The class being examined.
     * @param key The key for the get(key) operation.
     */
    public GetExecutor(Log r,
            org.apache.commons.jexl.util.introspection.Introspector ispect,
            Class<?> c, String key) {
        rlog = r;
        args[0] = key;
        method = ispect.getMethod(c, "get", args);
    }

    /**
     * {@inheritDoc}
     */
    public Object execute(Object o)
    throws IllegalAccessException, InvocationTargetException {
        if (method == null) {
            return null;
        }

        return method.invoke(o, args);
    }

}
