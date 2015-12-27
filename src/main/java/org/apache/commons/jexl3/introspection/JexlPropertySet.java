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

package org.apache.commons.jexl3.introspection;

/**
 * Interface used for setting values that appear to be properties.
 * Ex.
 * <code>
 * ${foo.bar = "hello"}
 * </code>
 * 
 * @since 1.0
 */
public interface JexlPropertySet {
    /**
     * Method used to set the property value of an object.
     *
     * @param obj Object on which the property setter will be called with the value
     * @param arg value to be set
     * @return the value returned from the set operation (impl specific)
     * @throws Exception on any error.
     */
    Object invoke(Object obj, Object arg) throws Exception;

    /**
     * Attempts to reuse this JexlPropertySet, checking that it is compatible with
     * the actual set of arguments.
     * 
     * @param obj the object to invoke the the get upon
     * @param key the property key to get
     * @param value the property value to set
     * @return the result of the method invocation that should be checked by tryFailed to determine if it succeeded
     * or failed.
     */
    Object tryInvoke(Object obj, Object key, Object value);

    /**
     * Checks whether a tryInvoke failed or not.
     * 
     * @param rval the value returned by tryInvoke
     * @return true if tryInvoke failed, false otherwise
     */
    boolean tryFailed(Object rval);

    /**
     * Specifies if this JexlPropertySet is cacheable and able to be reused for
     * this class of object it was returned for.
     *
     * @return true if can be reused for this class, false if not
     */
    boolean isCacheable();
}
