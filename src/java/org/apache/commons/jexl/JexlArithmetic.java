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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.jexl.util.Coercion;

/**
 * Perform arithmetic.
 * @since 2.0
 */
class JexlArithmetic implements Arithmetic {

    /**
     * Add two values together.
     * Rules are:<ol>
     * <li>If both are null, result is 0</li>
     * <li>If either are floating point numbers, coerce to BigDecimals
     *      and add together</li>
     * <li>Else treat as BigIntegers and add together</li>
     * <li>If either numeric add fails on coercion to the appropriate type,
     *      treat as Strings and do concatenation</li>
     * </ol>
     * @param left first value
     * @param right second value
     * @return left + right.
     */
    public Object add(Object left, Object right) {
        if (left == null && right == null) {
            return new Long(0);
        }
        
        try {
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                double l = Coercion.coercedouble(left);
                double r = Coercion.coercedouble(right);
                return new Double(l + r);
            }
        
            // if both are bigintegers use that type
            if (left instanceof BigInteger && right instanceof BigInteger) {
                BigInteger l = Coercion.coerceBigInteger(left);
                BigInteger r = Coercion.coerceBigInteger(right);
                return l.add(r);
            }
            
            // if either are bigdecimal use that type 
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                BigDecimal l = Coercion.coerceBigDecimal(left);
                BigDecimal r = Coercion.coerceBigDecimal(right);
                return l.add(r);
            }
            
            // otherwise treat as integers
            BigInteger l = Coercion.coerceBigInteger(left);
            BigInteger r = Coercion.coerceBigInteger(right);
            BigInteger result = l.add(r);
            BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE); 
            if (result.compareTo(maxLong) <= 0) {
                return new Long(result.longValue());
            }
            return result;
        } catch (java.lang.NumberFormatException nfe) {
            // Well, use strings!
            return left.toString().concat(right.toString());
        }
    }

    /**
     * Divide the left value by the right.
     * Rules are:<ol>
     * <li>If both are null, result is 0</li>
     * <li>Treat as BigDecimals and divide</li>
     * </ol>
     * @param left first value
     * @param right second value
     * @return left - right.
     */
    public Object divide(Object left, Object right) {
        if (left == null && right == null) {
            return new Long(0);
        }

        // if both are bigintegers use that type
        if (left instanceof BigInteger && right instanceof BigInteger) {
            BigInteger l = Coercion.coerceBigInteger(left);
            BigInteger r = Coercion.coerceBigInteger(right);
            if (r.compareTo(BigInteger.valueOf(0)) == 0) {
                return r;
            }
            return l.divide(r);
        }
        
        // if either are bigdecimal use that type 
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = Coercion.coerceBigDecimal(left);
            BigDecimal r = Coercion.coerceBigDecimal(right);
            if (r.compareTo(BigDecimal.valueOf(0)) == 0) {
                return r;
            }
            return l.divide(r, BigDecimal.ROUND_HALF_UP);
        }

        double l = Coercion.coercedouble(left);
        double r = Coercion.coercedouble(right);
        if (r == 0) {
            return new Double(r);
        }
        return new Double(l / r);

    }
    
    /**
     * left value mod right.
     * Rules are:<ol>
     * <li>If both are null, result is 0</li>
     * <li>Treat both as BigIntegers and perform modulus</li>
     * </ol>
     * @param left first value
     * @param right second value
     * @return left mod right.
     */
    public Object mod(Object left, Object right) {
        if (left == null && right == null) {
            return new Long(0);
        }

        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = Coercion.coercedouble(left);
            double r = Coercion.coercedouble(right);
            return new Double(l % r);
        }

        // if both are bigintegers use that type
        if (left instanceof BigInteger && right instanceof BigInteger) {
            BigInteger l = Coercion.coerceBigInteger(left);
            BigInteger r = Coercion.coerceBigInteger(right);
            if (r.compareTo(BigInteger.valueOf(0)) == 0) {
                return r;
            }
            return l.mod(r);
        }

        // if either are bigdecimal use that type 
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = Coercion.coerceBigDecimal(left);
            BigDecimal r = Coercion.coerceBigDecimal(right);
            if (r.compareTo(BigDecimal.valueOf(0)) == 0) {
                return r;
            }
            BigInteger intDiv = l.divide(r, BigDecimal.ROUND_HALF_UP).toBigInteger();
            BigInteger intValue = (r.multiply(new BigDecimal(intDiv))).toBigInteger();
            BigDecimal remainder = new BigDecimal(l.subtract(new BigDecimal(intValue)).toBigInteger());
            return remainder;
        }

        // otherwise treat as integers
        BigInteger l = Coercion.coerceBigInteger(left);
        BigInteger r = Coercion.coerceBigInteger(right);
        BigInteger result = l.mod(r);
        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE); 
        if (result.compareTo(maxLong) <= 0) {
            return new Long(result.longValue());
        }
        return result;
    }
    
    /**
     * Multiply the left value by the right.
     * Rules are:<ol>
     * <li>If both are null, result is 0</li>
     * <li>If either are floating point numbers, coerce to BigDecimals
     *      and multiply</li>
     * <li>Else treat as BigIntegers and multiply</li>
     * <li>If either numeric operation fails on coercion to the appropriate type,
     *      treat as Strings and do concatenation</li>
     * </ol>
     * @param left first value
     * @param right second value
     * @return left * right.
     */
    public Object multiply(Object left, Object right) {
        if (left == null && right == null) {
            return new Long(0);
        }
        
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = Coercion.coercedouble(left);
            double r = Coercion.coercedouble(right);
            return new Double(l * r);
        }
        
        // if both are bigintegers use that type
        if (left instanceof BigInteger && right instanceof BigInteger) {
            BigInteger l = Coercion.coerceBigInteger(left);
            BigInteger r = Coercion.coerceBigInteger(right);
            return l.multiply(r);
        }
        
        // if either are bigdecimal use that type 
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = Coercion.coerceBigDecimal(left);
            BigDecimal r = Coercion.coerceBigDecimal(right);
            return l.multiply(r);
        }

        // otherwise treat as integers
        BigInteger l = Coercion.coerceBigInteger(left);
        BigInteger r = Coercion.coerceBigInteger(right);
        BigInteger result = l.multiply(r);
        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE); 
        if (result.compareTo(maxLong) <= 0) {
            return new Long(result.longValue());
        }
        return result;
    }
    
    /**
     * Subtract the right value from the left.
     * Rules are:<ol>
     * <li>If both are null, result is 0</li>
     * <li>If either are floating point numbers, coerce to BigDecimals
     *      and subtract</li>
     * <li>Else treat as BigIntegers and subtract</li>
     * <li>If either numeric operation fails on coercion to the appropriate type,
     *      treat as Strings and do concatenation</li>
     * </ol>
     * @param left first value
     * @param right second value
     * @return left + right.
     */
    public Object subtract(Object left, Object right) {
        if (left == null && right == null) {
            return new Long(0);
        }
        
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = Coercion.coercedouble(left);
            double r = Coercion.coercedouble(right);
            return new Double(l - r);
        }
        
        // if both are bigintegers use that type
        if (left instanceof BigInteger && right instanceof BigInteger) {
            BigInteger l = Coercion.coerceBigInteger(left);
            BigInteger r = Coercion.coerceBigInteger(right);
            return l.subtract(r);
        }
        
        // if either are bigdecimal use that type 
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = Coercion.coerceBigDecimal(left);
            BigDecimal r = Coercion.coerceBigDecimal(right);
            return l.subtract(r);
        }

        // otherwise treat as integers
        BigInteger l = Coercion.coerceBigInteger(left);
        BigInteger r = Coercion.coerceBigInteger(right);
        BigInteger result = l.subtract(r);
        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE); 
        if (result.compareTo(maxLong) <= 0) {
            return new Long(result.longValue());
        }
        return result;
    }
    
    /**
     * Test if the passed value is a floating point number, i.e. a float, double
     * or string with ( "." | "E" | "e").
     *
     * @param val the object to be tested
     * @return true if it is, false otherwise.
     */
    private boolean isFloatingPointNumber(Object val) {
        if (val instanceof Float || val instanceof Double) {
            return true;
        }
        if (val instanceof String) {
            String string = (String) val;
            return string.indexOf(".") != -1 || string.indexOf("e") != -1 || string.indexOf("E") != -1;
        }
        return false;
    }
}
