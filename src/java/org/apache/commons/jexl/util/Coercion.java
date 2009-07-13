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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 *  Coercion utilities for the JSTL EL-like coercion.
 *
 *  @since 1.0
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 */
public class Coercion {

    /**
     * Coerce to a Boolean.
     *
     * @param val Object to be coerced.
     * @return The Boolean coerced value, or null if none possible.
     */
    public static Boolean coerceBoolean(Object val) {
        if (val == null) {
            return Boolean.FALSE;
        } else if (val instanceof Boolean) {
            return (Boolean) val;
        } else if (val instanceof String) {
            return Boolean.valueOf((String) val);
        }
        return null;
    }

    /**
     * Coerce to a Integer.
     *
     * @param val Object to be coerced.
     * @return The Integer coerced value.
     * @throws Exception If Integer coercion fails.
     */
    public static Integer coerceInteger(Object val)
    throws Exception {
        if (val == null) {
            return new Integer(0);
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return new Integer(0);
            }
            return Integer.valueOf((String) val);
        } else if (val instanceof Character) {
            return new Integer(((Character) val).charValue());
        } else if (val instanceof Boolean) {
            throw new Exception("Boolean->Integer coercion exception");
        } else if (val instanceof Number) {
            return new Integer(((Number) val).intValue());
        }

        throw new Exception("Integer coercion exception");
    }

    /**
     * Coerce to a Long.
     *
     * @param val Object to be coerced.
     * @return The Long coerced value.
     * @throws Exception If Long coercion fails.
     */
    public static Long coerceLong(Object val)
    throws Exception {
        if (val == null) {
            return new Long(0);
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return new Long(0);
            }
            return Long.valueOf((String) val);
        } else if (val instanceof Character) {
            return new Long(((Character) val).charValue());
        } else if (val instanceof Boolean) {
            throw new Exception("Boolean->Long coercion exception");
        } else if (val instanceof Number) {
            return new Long(((Number) val).longValue());
        }

        throw new Exception("Long coercion exception");
    }

    /**
     * Coerce to a Double.
     *
     * @param val Object to be coerced.
     * @return The Double coerced value.
     * @throws Exception If Double coercion fails.
     */
    public static Double coerceDouble(Object val)
    throws Exception {
        if (val == null) {
            return new Double(0);
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return new Double(0);
            }

            /*
             * the spec seems to be iffy about this.  Going to give it a wack
             *  anyway
             */

            return new Double((String) val);
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();

            return new Double(Double.parseDouble(String.valueOf(i)));
        } else if (val instanceof Boolean) {
            throw new Exception("Boolean->Double coercion exception");
        } else if (val instanceof Double) {
            return (Double) val;
        } else if (val instanceof Number) {
            //The below construct is used rather than ((Number)val).doubleValue() to ensure
            //equality between comparint new Double( 6.4 / 3 ) and the jexl expression of 6.4 / 3
            return new Double(Double.parseDouble(String.valueOf(val)));
        }

        throw new Exception("Double coercion exception");
    }

    /**
     * Coerce to a BigDecimal.
     *
     * @param val Object to be coerced.
     * @return The Double coerced value.
     * @throws Exception If coercion fails.
     */
    public static BigDecimal coerceBigDecimal(Object val)
    throws Exception {
        if (val == null) {
            return BigDecimal.valueOf(0);
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return BigDecimal.valueOf(0);
            }

            /*
             * the spec seems to be iffy about this.  Going to give it a wack
             *  anyway
             */

            return new BigDecimal((String) val);
        } else if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        } else if (val instanceof Character) {
            return BigDecimal.valueOf(((Character) val).charValue());
        } else if (val instanceof Number) {
            return new BigDecimal(val.toString());
        } else if (val instanceof Boolean) {
            throw new Exception("Boolean->BigDecimal coercion exception");
        }

        throw new Exception("BigDecimal coercion exception");
    }

    /**
     * Coerce to a BigInteger.
     *
     * @param val Object to be coerced.
     * @return The Integer coerced value.
     * @throws Exception If coercion fails.
     */
    public static BigInteger coerceBigInteger(Object val)
    throws Exception {
        if (val == null) {
            return BigInteger.ZERO;
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return BigInteger.ZERO;
            }
            return new BigInteger((String) val);
        } else if (val instanceof BigInteger) {
            return (BigInteger) val;
        } else if (val instanceof Character) {
            return BigInteger.valueOf(((Character) val).charValue());
        } else if (val instanceof Number) {
            return BigInteger.valueOf(((Number) val).longValue());
        } else if (val instanceof Boolean) {
            throw new Exception("Boolean->BigInteger coercion exception");
        }

        throw new Exception("BigInteger coercion exception");
    }

    /**
     * Is Object a floating point number.
     *
     * @param o Object to be analyzed.
     * @return true if it is a Float or a Double.
     */
    public static boolean isFloatingPoint(final Object o) {
        return o instanceof Float || o instanceof Double;
    }

    /**
     * Is Object a whole number.
     *
     * @param o Object to be analyzed.
     * @return true if Integer, Long, Byte, Short or Character.
     */
    public static boolean isNumberable(final Object o) {
        return o instanceof Integer
            || o instanceof Long
            || o instanceof Byte
            || o instanceof Short
            || o instanceof Character;
    }

}
