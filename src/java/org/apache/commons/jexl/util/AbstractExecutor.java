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
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;

/**
 * Abstract class that is used to execute an arbitrary
 * method that is in introspected. This is the superclass
 * for the GetExecutor and PropertyExecutor.
 *
 * @since 1.0
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public abstract class AbstractExecutor {
    /** The executor instance log. */
    protected Log rlog = null;
    
    /**
     * Method to be executed.
     */
    protected Method method = null;
    
    /**
     * Execute method against context.
     *
     * @param o The owner.
     * @return The return value.
     * @throws IllegalAccessException Method is inaccessible.
     * @throws InvocationTargetException Method body throws an exception.
     */
     public abstract Object execute(Object o)
         throws IllegalAccessException, InvocationTargetException;

    /**
     * Tell whether the executor is alive by looking
     * at the value of the method.
     *
     * @return boolean Whether the executor is alive.
     */
    public boolean isAlive() {
        return (method != null);
    }

    /**
     * Get the method to be executed.
     *
     * @return Method The method to be executed.
     */
    public Method getMethod() {
        return method;
    }
}
