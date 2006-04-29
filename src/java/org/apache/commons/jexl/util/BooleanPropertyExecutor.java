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
public class BooleanPropertyExecutor extends PropertyExecutor {

    /**
     * Constructor.
     *
     * @param rlog The instance log.
     * @param is The JEXL introspector.
     * @param clazz The class being analyzed.
     * @param property The boolean property.
     */
    public BooleanPropertyExecutor(Log rlog,
        org.apache.commons.jexl.util.introspection.Introspector is,
        Class clazz, String property) {
            super(rlog, is, clazz, property);
    }

    /**
     * Locate the getter method for this boolean property.
     *
     * @param clazz The class being analyzed.
     * @param property Name of boolean property.
     */
    protected void discover(Class clazz, String property) {
        try {
            char c;
            StringBuffer sb;

            Object[] params = {};

            /*
             *  now look for a boolean isFoo
             */

            sb = new StringBuffer("is");
            sb.append(property);

            c = sb.charAt(2);

            if (Character.isLowerCase(c)) {
                sb.setCharAt(2, Character.toUpperCase(c));
            }

            methodUsed = sb.toString();
            method = introspector.getMethod(clazz, methodUsed, params);

            if (method != null) {
                /*
                 *  now, this has to return a boolean
                 */

                if (method.getReturnType() == Boolean.TYPE) {
                    return;
                }

                method = null;
            }
        } catch (Exception e) {
            rlog.error("PROGRAMMER ERROR : BooleanPropertyExector() : " + e);
        }
    }
}
