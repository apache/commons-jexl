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
 * Pluggable arithmetic, coercion & comparison operators. 
 */
public interface Arithmetic {

    /** Sets how to behave when null is used as an operand.
     * @param lenient
     * If true, some reasonable default conversion occurs (0 for numbers, empty string,).
     * If false, encountering null as an operand is considered an error.
     */
    void setLenient(boolean lenient);

    /**
     * Checks whether arithmetic is lenient.
     * @return true when lenient, false when strict
     */
    boolean isLenient();

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
    
    /**
     * Coerce an object into a boolean.
     * @param arg the value to convert
     * @return a boolean
     */
    boolean toBoolean(Object arg);
    
    /**
     * Coerce an object into an integer.
     * @param arg the object to coerce
     * @return an int
     */
    int toInteger(Object arg);    
    
    /**
     * Coerce an object into a long.
     * @param arg the object to coerce
     * @return a long
     */
    long toLong(Object arg);   
    
    /**
     * Coerce an object into a double.
     * @param arg the object to coerce
     * @return a double
     */
    double toDouble(Object arg);

    /**
     * Coerce an object into a string.
     * @param arg the object to coerce
     * @return a double
     */
    String toString(Object arg);
        
    /**
     * Coerce an object into a big integer.
     * @param arg the object to coerce
     * @return a big integer
     */
    java.math.BigInteger toBigInteger(Object arg);     
    
    /**
     * Coerce an object into a big decimal.
     * @param arg the object to coerce
     * @return a big decimal
     */
    java.math.BigDecimal toBigDecimal(Object arg);
    
    /**
     * Given a Number, return back the value using the smallest type the result
     * will fit into. This works hand in hand with parameter 'widening' in java
     * method calls, e.g. a call to substring(int,int) with an int and a long
     * will fail, but a call to substring(int,int) with an int and a short will
     * succeed.
     *
     * @param original the original number.
     * @return a value of the smallest type the original number will fit into.
     * @since 1.1
     */
    Number narrow(Number original);

    /**
     * Test if left == right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    boolean equals(Object left, Object right);
    
    /**
     * Test if left < right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    boolean lessThan(Object left, Object right);

    /**
     * Test if left > right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    boolean greaterThan(Object left, Object right);

    /**
     * Test if left <= right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    boolean lessThanOrEqual(Object left, Object right);

    /**
     * Test if left >= right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    boolean greaterThanOrEqual(Object left, Object right);
}
