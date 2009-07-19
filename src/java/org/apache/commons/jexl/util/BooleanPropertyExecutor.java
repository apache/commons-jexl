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
 *  Handles discovery and valuation of a
 *  boolean object property, of the
 *  form public boolean is&lt;Property&gt; when executed.
 *
 *  We do this separately as to preserve the current
 *  quasi-broken semantics of get &lt;as is property&gt;
 *  get&lt;flip 1st char&gt; get("property") and now followed
 *  by is&lt;Property&gt;.
 *
 *  @since 1.0
 *  @author <a href="geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id$
 */
public class BooleanPropertyExecutor extends AbstractExecutor {
    /** index of the first character of the property. */
    private static final int PROPERTY_START_INDEX = 2;
    /**
     * Constructor.
     *
     * @param rlog The logger.
     * @param is The JEXL introspector.
     * @param clazz The class being analyzed.
     * @param property The boolean property.
     */
    public BooleanPropertyExecutor(final Log rlog, Introspector is, Class<?> clazz, String property) {
        super(discover(rlog, is, clazz, property));
    }

    /**
     * Finds the method for a BooleanPropertyExecutor.
     *
     * @param rlog The logger.
     * @param is The JEXL introspector.
     * @param clazz The class being analyzed.
     * @param property The boolean property.
     * @return The method.
     */
   private static Method discover(final Log rlog, Introspector is, Class<?> clazz, String property) {
        String mname = null;
        Method m = null;
        try {
            char c;
            //  now look for a boolean isFoo
            StringBuilder  sb = new StringBuilder("is");
            sb.append(property);

            mname = sb.toString();
            m = is.getMethod(clazz, mname, EMPTY_PARAMS);
            if (null == m) {
                //now the convenience, flip the 1st character
                c = sb.charAt(PROPERTY_START_INDEX);
                if (Character.isLowerCase(c)) {
                    sb.setCharAt(PROPERTY_START_INDEX, Character.toUpperCase(c));
                } else {
                    sb.setCharAt(PROPERTY_START_INDEX, Character.toLowerCase(c));
                }

                mname = sb.toString();
                m = is.getMethod(clazz, mname, EMPTY_PARAMS);
            }

            //  now, this has to return a boolean
            if (m != null && m.getReturnType() != Boolean.TYPE) {
                m = null;
            }
            // pass through application level runtime exceptions
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
                rlog.error("PROGRAMMER ERROR : BooleanPropertyExector()", e);
        }
        return m;
    }
}