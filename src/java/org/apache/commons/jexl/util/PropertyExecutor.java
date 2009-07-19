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

import java.lang.reflect.Method;
import org.apache.commons.jexl.util.introspection.Introspector;
import org.apache.commons.logging.Log;

/**
 * Returned the value of object property when executed.
 * @since 1.0
 */
public class PropertyExecutor extends AbstractExecutor {
    /** index of the first character of the property. */
    private static final int PROPERTY_START_INDEX = 3;
    /**
     * Constructor.
     *
     * @param rlog The logger.
     * @param is The JEXL introspector.
     * @param clazz The class being examined.
     * @param property The property being addressed.
     */
    public PropertyExecutor(final Log rlog, Introspector is, Class<?> clazz, String property) {
        super(discover(rlog, is, clazz, property));
    }


    /**
     * Finds the method to create a PropertyExecutor.
     *
     * @param rlog The logger.
     * @param is The JEXL introspector.
     * @param clazz The class being examined.
     * @param property The property being addressed.
     * @return The method.
     */
     private static Method discover(final Log rlog, Introspector is, Class<?> clazz, String property) {
        //  this is gross and linear, but it keeps it straightforward.
        String mname = null;
        Method m = null;
        try {
            // start with get<property>, this leaves the property name as is...
            StringBuilder sb = new StringBuilder("get");
            sb.append(property);

            mname = sb.toString();
            m = is.getMethod(clazz, mname, EMPTY_PARAMS);
            if (m == null) {
                //now the convenience, flip the 1st character
                char c = sb.charAt(PROPERTY_START_INDEX);
                if (Character.isLowerCase(c)) {
                    sb.setCharAt(PROPERTY_START_INDEX, Character.toUpperCase(c));
                } else {
                    sb.setCharAt(PROPERTY_START_INDEX, Character.toLowerCase(c));
                }

                mname = sb.toString();
                m = is.getMethod(clazz, mname, EMPTY_PARAMS);
            }

        /**
         * pass through application level runtime exceptions
         */
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            rlog.error("PROGRAMMER ERROR : PropertyExecutor() : ", e);
        }
        return m;
    }
}