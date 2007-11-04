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
package org.apache.commons.jexl;
/**
 * Pluggable arithmetic operators. 
 */
interface Arithmetic {

    /**
     * Add two values together.
     *
     * @param left first value
     * @param right second value
     * @return left + right.
     */
    Object add(Object left, Object right);

    /**
     * Divide the left value by the right.
     *
     * @param left first value
     * @param right second value
     * @return left - right.
     */
    Object divide(Object left, Object right);

    /**
     * left value mod right.
     *
     * @param left first value
     * @param right second value
     * @return left mod right.
     */
    Object mod(Object left, Object right);

    /**
     * Multiply the left value by the right.
     *
     * @param left first value
     * @param right second value
     * @return left * right.
     */
    Object multiply(Object left, Object right);

    /**
     * Subtract the right value from the left.
     *
     * @param left first value
     * @param right second value
     * @return left + right.
     */
    Object subtract(Object left, Object right);
}