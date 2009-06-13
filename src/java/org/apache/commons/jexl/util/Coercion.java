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

}
class noCoercion {

    public boolean toBoolean(Object arg) {
        return coerceboolean(arg);
    }
    public int toInteger(Object arg) {
        return coerceinteger(arg);
    }
    public long toLong(Object arg) {
        return coerceLong(arg);
    }
    public double toDouble(Object arg) {
        return coercedouble(arg);
    }
    public java.math.BigInteger toBigInteger(Object arg) {
        return coerceBigInteger(arg);
    }
    public java.math.BigDecimal toBigDecimal(Object arg) {
        return coerceBigDecimal(arg);
    }
    /**
     * Coerce to a boolean (not a java.lang.Boolean).
     *
     * @param val Object to be coerced.
     * @return The Boolean coerced value, or false if none possible.
     */
    public static boolean coerceboolean(Object val) {
        if (val == null) {
            return false;
        } else if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue();
        } else if (val instanceof String) {
            return Boolean.valueOf((String) val).booleanValue();
        }
        // TODO: is this a reasonable default?
        return false;
    }

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
     */
    public static Integer coerceInteger(Object val) {
        return new Integer(coerceinteger(val));
    }
    
    /**
     * Coerce to a Integer.
     *
     * @param val Object to be coerced.
     * @return The Integer coerced value.
     */
    public static int coerceinteger(Object val) {
        if (val == null) {
            return 0;
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return 0;
            }
            return Integer.parseInt((String) val);
        } else if (val instanceof Character) {
            return ((Character) val).charValue();
        } else if (val instanceof Boolean) {
            throw new IllegalArgumentException("Boolean->Integer coercion exception");
        } else if (val instanceof Number) {
            return ((Number) val).intValue();
        }

        throw new IllegalArgumentException("Integer coercion exception, don't know how to convert " + val);
    }

    
    /**
     * Coerce to a long (not a java.lang.Long).
     *
     * @param val Object to be coerced.
     * @return The Long coerced value.
     */
    public static long coercelong(Object val) {
        if (val == null) {
            return 0;
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return 0;
            }
            return Long.parseLong((String) val);
        } else if (val instanceof Character) {
            return ((Character) val).charValue();
        } else if (val instanceof Boolean) {
            throw new NumberFormatException("Boolean->Long coercion exception");
        } else if (val instanceof Number) {
            return ((Number) val).longValue();
        }

        throw new NumberFormatException("Long coercion exception for '" + val + "'");
    }

    /**
     * Coerce to a Long.
     *
     * @param val Object to be coerced.
     * @return The Long coerced value.
     */
    public static Long coerceLong(Object val) {
        return new Long(coercelong(val));
    }

    /**
     * Coerce to a Double.
     *
     * @param val Object to be coerced.
     * @return The Double coerced value.
     */
    public static Double coerceDouble(Object val) {
        return new Double(coercedouble(val));
    }
    
    /**
     * Get a BigInteger from the object passed.
     * Null and empty string maps to zero.
     * @param val the object to be coerced.
     * @return a BigDecimal.
     */
    public static BigInteger coerceBigInteger(Object val) {
        if (val instanceof BigInteger) {
            return (BigInteger) val;
        } else if (val == null) {
            return BigInteger.valueOf(0);
        } else if (val instanceof String) {
            String string = (String) val;
            if ("".equals(string.trim())) {
                return BigInteger.valueOf(0);
            }
            return new BigInteger(string);
        } else if (val instanceof Number) {
            return new BigInteger(val.toString());
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();
            return BigInteger.valueOf(i);
        }
        
        throw new IllegalArgumentException("BigInteger coercion. Can't coerce type " + val.getClass().getName());
    }
    
    /**
     * Get a BigDecimal from the object passed.
     * Null and empty string maps to zero.
     * @param val the object to be coerced.
     * @return a BigDecimal.
     */
    public static BigDecimal coerceBigDecimal(Object val) {
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        } else if (val == null) {
            return BigDecimal.valueOf(0);
        } else if (val instanceof String) {
            String string = (String) val;
            if ("".equals(string.trim())) {
                return BigDecimal.valueOf(0);
            }
            return new BigDecimal(string);
        } else if (val instanceof Number) {
            return new BigDecimal(val.toString());
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();
            return new BigDecimal(i);
        }
        
        throw new IllegalArgumentException("BigDecimal coercion. Can't coerce type " + val.getClass().getName());
    }
    
    /**
     * Coerce to a double.
     *
     * @param val Object to be coerced.
     * @return The Double coerced value.
     */
    public static double coercedouble(Object val) {
        if (val == null) {
            return 0;
        } else if (val instanceof String) {
            String string = (String) val;
            if ("".equals(string.trim())) {
                return 0;
            }
            // the spec seems to be iffy about this.  Going to give it a wack anyway
            return Double.parseDouble(string);
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();

            return i;
        } else if (val instanceof Double) {
            return ((Double) val).doubleValue();
        } else if (val instanceof Number) {
            //The below construct is used rather than ((Number)val).doubleValue() to ensure
            //equality between comparing new Double( 6.4 / 3 ) and the jexl expression of 6.4 / 3
            return Double.parseDouble(String.valueOf(val));
        } else if (val instanceof Boolean) {
            throw new IllegalArgumentException("Boolean->Double coercion exception");
        }

        throw new IllegalArgumentException("Double coercion exception, don't know how to convert " + val);
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
