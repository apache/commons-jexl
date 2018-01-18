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

package org.apache.commons.jexl3.examples;

import org.junit.Assert;

/**
 * Abstracts using a test within Junit or through a main method.
 */
public abstract class Output {
    /**
     * Creates an output using System.out.
     */
    private Output() {
        // nothing to do
    }

    /**
     * Outputs the actual and value or checks the actual equals the expected value.
     * @param expr the message to output
     * @param actual the actual value to output
     * @param expected the expected value
     */
    public abstract void print(String expr, Object actual, Object expected);

    /**
     * The output instance for Junit TestCase calling assertEquals.
     */
    public static final Output JUNIT = new Output() {
        @Override
        public void print(String expr, Object actual, Object expected) {
            Assert.assertEquals(expr, expected, actual);
        }
    };


    /**
     * The output instance for the general outputing to System.out.
     */
    public static final Output SYSTEM = new Output() {
        @Override
        public void print(String expr, Object actual, Object expected) {
            System.out.print(expr);
            System.out.println(actual);
        }
    };
}