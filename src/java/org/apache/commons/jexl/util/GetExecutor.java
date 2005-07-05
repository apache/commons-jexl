/*
 * Copyright 2000-2001,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
public class GetExecutor extends AbstractExecutor
{
    /**
     * Container to hold the 'key' part of 
     * get(key).
     */
    private Object[] args = new Object[1];
    
    /**
     * Default constructor.
     */
    public GetExecutor(Log r, org.apache.commons.jexl.util.introspection.Introspector ispect, Class c, String key)
        throws Exception
    {
        rlog = r;
        args[0] = key;
        method = ispect.getMethod(c, "get", args);
    }

    /**
     * Execute method against context.
     */
    public Object execute(Object o)
        throws IllegalAccessException, InvocationTargetException
    {
        if (method == null)
            return null;

        return method.invoke(o, args);
    }

}









