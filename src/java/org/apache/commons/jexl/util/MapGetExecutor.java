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

import java.util.Map;

import org.apache.commons.logging.Log;


/**
 * GetExecutor that is smart about Maps. If it detects one, it does not
 * use Reflection but a cast to access the getter.
 *
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class MapGetExecutor extends AbstractExecutor {
    /** the property to get. */
    private final String property;

    /**
     * Create the instance.
     * @param rlog the logger.
     * @param clazz the class to execute the get on.
     * @param aProperty the property or key to get.
     */
    public MapGetExecutor(final Log rlog, final Class clazz, final String aProperty) {
        this.rlog = rlog;
        this.property = aProperty;
        discover(clazz);
    }

    /**
     * Discover the method to call.
     * @param clazz the class to find the method on.
     */
    protected void discover(final Class clazz) {
        Class[] interfaces = clazz.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(Map.class)) {
                try {
                    if (property != null) {
                        method = Map.class.getMethod("get", new Class[]{Object.class});
                    }
                    /**
                     * pass through application level runtime exceptions
                     */
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    rlog.error("While looking for get('" + property + "') method:", e);
                }
                break;
            }
        }
    }

    /** 
     * Get the property from the map.
     * @param o the map.
     * @return o.get(property)
     */
    public Object execute(final Object o) {
        return ((Map) o).get(property);
    }
}
