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

package org.apache.commons.jexl2.util.introspection;

/**
 * Interface for getting values that appear to be properties.
 * Ex.
 * <code>
 * ${foo.bar}
 * </code>
 * @since 1.0
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public interface JexlPropertyGet {
    /**
     * invocation method - called when the 'get action' should be performed and
     * a value returned.
     * @param o the object to get the property from.
     * @return the property value.
     * @throws Exception on any error.
     */
    Object invoke(Object o) throws Exception;

    /**
     * Specifies if this JexlPropertyGet is cacheable and able to be reused for
     * this class of object it was returned for.
     *
     * @return true if can be reused for this class, false if not
     */
    boolean isCacheable();

    /**
     * returns the method name used to return this 'property'.
     * @return the method name.
     */
    String getMethodName();

    /**
     * Tell whether the method underlying this 'property' is alive by
     * checking to see if represents a successful name resolution.
     *
     * @return boolean Whether 'property' is alive.
     */
    boolean isAlive();
}
