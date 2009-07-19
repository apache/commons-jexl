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
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;


/**
 * GetExecutor that is smart about Maps. If it detects one, it does not
 * use Reflection but a cast to access the getter.
 *
 * @author <a href="mailto:henning@apache.org">Henning P. Schmiedehausen</a>
 * @version $Id$
 */
public class MapGetExecutor extends AbstractExecutor {
    /** A one argument signature to find method. */
    private static final Class<?>[] OBJECT_PARM = new Class<?>[]{Object.class};
    /** the property to get. */
    private final String property;

    /**
     * Creates the instance.
     * @param rlog The logger.
     * @param clazz the class to execute the get on.
     * @param aProperty the property or key to get.
     */
    public MapGetExecutor(final Log rlog, final Class<?> clazz, final String aProperty) {
        super(aProperty != null? discover(rlog, clazz, aProperty) : null);
        this.property = aProperty;
    }

    /**
     * Finds the method for a MapGetExecutor.
     *
     * @param rlog The logger.
     * @param clazz The class being analyzed.
     * @param property The boolean property.
     * @return The method.
     */
    private static Method discover(final Log rlog, final Class<?> clazz, final String property) {
        Method m = null;
        Class<?>[] interfaces = clazz.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(Map.class)) {
                try {
                    m = Map.class.getMethod("get", OBJECT_PARM);
                    // pass through application level runtime exceptions
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    rlog.error("While looking for get('" + property + "') method:", e);
                }
                break;
            }
        }
        return m;
    }


    /** 
     * Get the property from the map.
     * @param o the map.
     * @return o.get(property)
     */
    @Override
    public Object execute(final Object o) {
        return ((Map<String,?>) o).get(property);
    }
}