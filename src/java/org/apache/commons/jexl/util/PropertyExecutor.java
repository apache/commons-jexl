/*
 * Copyright 2000-2002,2004 The Apache Software Foundation.
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

import org.apache.commons.jexl.util.introspection.Introspector;
import org.apache.commons.logging.Log;

/**
 * Returned the value of object property when executed.
 * @since 1.0
 */
public class PropertyExecutor extends AbstractExecutor {

    /** The JEXL introspector used. */
    protected Introspector introspector = null;

    /** The method used. */
    protected String methodUsed = null;

    /**
     * Constructor.
     *
     * @param r The log for this property executor instance.
     * @param ispctr The JEXL introspector.
     * @param clazz The class being examined.
     * @param property The property being addressed.
     */
    public PropertyExecutor(Log r, Introspector ispctr,
            Class clazz, String property) {
        rlog = r;
        introspector = ispctr;

        discover(clazz, property);
    }

    /**
     * Locate the getter method for this property.
     *
     * @param clazz The class being analyzed.
     * @param property Name of the property.
     */
    protected void discover(Class clazz, String property) {
        /*
         *  this is gross and linear, but it keeps it straightforward.
         */

        try {
            char c;
            StringBuffer sb;

            Object[] params = {};

            /*
             *  start with get<property>
             *  this leaves the property name 
             *  as is...
             */
            sb = new StringBuffer("get");
            sb.append(property);

            methodUsed = sb.toString();

            method = introspector.getMethod(clazz, methodUsed, params);
             
            if (method != null) {
                return;
            }
        
            /*
             *  now the convenience, flip the 1st character
             */
         
            sb = new StringBuffer("get");
            sb.append(property);

            c = sb.charAt(3);

            if (Character.isLowerCase(c)) {
                sb.setCharAt(3, Character.toUpperCase(c));
            } else {
                sb.setCharAt(3, Character.toLowerCase(c));
            }

            methodUsed = sb.toString();
            method = introspector.getMethod(clazz, methodUsed, params);

            if (method != null) {
                return;
            }
            
        } catch (Exception e) {
            rlog.error("PROGRAMMER ERROR : PropertyExector() : " + e);
        }
    }


    /**
     * {@inheritDoc}
     */
    public Object execute(Object o)
    throws IllegalAccessException,  InvocationTargetException {
        if (method == null) {
            return null;
        }

        return method.invoke(o, null);
    }
}

