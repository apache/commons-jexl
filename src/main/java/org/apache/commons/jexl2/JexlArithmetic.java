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
package org.apache.commons.jexl2;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * Perform arithmetic.
 * <p>
 * All arithmetic operators (+, - , *, /, %) follow the same rules regarding their arguments.
 * <ol>
 * <li>If both are null, result is 0</li>
 * <li>If either is a floating point number, coerce both to Double and perform operation</li>
 * <li>If both are BigInteger, treat as BigInteger and perform operation</li>
 * <li>If either is a BigDecimal, coerce both to BigDecimal and and perform operation</li>
 * <li>Else treat as BigInteger, perform operation and attempt to narrow result:
 * <ol>
 * <li>if both arguments can be narrowed to Integer, narrow result to Integer</li>
 * <li>if both arguments can be narrowed to Long, narrow result to Long</li>
 * <li>Else return result as BigInteger</li>
 * </ol>
 * </li>
 * </ol>
 * </p>
 * @since 2.0
 */
public class JexlArithmetic {
    /** Double.MAX_VALUE as BigDecimal. */
    protected static final BigDecimal BIGD_DOUBLE_MAX_VALUE = BigDecimal.valueOf(Double.MAX_VALUE);
    /** Double.MIN_VALUE as BigDecimal. */
    protected static final BigDecimal BIGD_DOUBLE_MIN_VALUE = BigDecimal.valueOf(Double.MIN_VALUE);
    /** Long.MAX_VALUE as BigInteger. */
    protected static final BigInteger BIGI_LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);
    /** Long.MIN_VALUE as BigInteger. */
    protected static final BigInteger BIGI_LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);
    /** Whether this JexlArithmetic instance behaves in strict or lenient mode. */
    private boolean strict;
    /** The big decimal math context. */
    protected final MathContext mathContext;

    /**
     * Creates a JexlArithmetic.
     * @param lenient whether this arithmetic is lenient or strict
     */
    public JexlArithmetic(boolean lenient) {
        this(lenient, MathContext.DECIMAL128);
    }

    /**
     * Creates a JexlArithmetic.
     * @param lenient whether this arithmetic is lenient or strict
     * @param bigdContext the math context instance to use for +,-,/,*,% operations on big decimals.
     */
    public JexlArithmetic(boolean lenient, MathContext bigdContext) {
        this.strict = !lenient;
        this.mathContext = bigdContext;
    }

    /**
     * Sets whether this JexlArithmetic instance triggers errors during evaluation when
     * null is used as an operand.
     * <p>This method is <em>not</em> thread safe; it may be called as an optional step by the JexlEngine
     * in its initialization code before expression creation &amp; evaluation.</p>
     * @see JexlEngine#setSilent
     * @see JexlEngine#setDebug
     * @param flag true means no JexlException will occur, false allows them
     */
    void setLenient(boolean flag) {
        this.strict = !flag;
    }

    /**
     * Checks whether this JexlArithmetic instance triggers errors during evaluation
     * when null is used as an operand.
     * @return true if lenient, false if strict
     */
    public boolean isLenient() {
        return !this.strict;
    }

    /**
     * The MathContext instance used for +,-,/,*,% operations on big decimals.
     * @return the math context
     */
    public MathContext getMathContext() {
        return mathContext;
    }

    /**
     * The result of +,/,-,*,% when both operands are null.
     * @return Integer(0) if lenient
     * @throws NullPointerException if strict
     */
    protected Object controlNullNullOperands() {
        if (strict) {
            throw new NullPointerException(JexlException.NULL_OPERAND);
        }
        return Integer.valueOf(0);
    }

    /**
     * Throw a NPE if arithmetic is strict.
     * @throws NullPointerException if strict
     */
    protected void controlNullOperand() {
        if (strict) {
            throw new NullPointerException(JexlException.NULL_OPERAND);
        }
    }

    /**
     * Test if either left or right are either a Float or Double.
     * @param left one object to test
     * @param right the other
     * @return the result of the test.
     */
    protected boolean isFloatingPointType(Object left, Object right) {
        return left instanceof Float || left instanceof Double || right instanceof Float || right instanceof Double;
    }

    /**
     * Test if the passed value is a floating point number, i.e. a float, double
     * or string with ( "." | "E" | "e").
     *
     * @param val the object to be tested
     * @return true if it is, false otherwise.
     */
    protected boolean isFloatingPointNumber(Object val) {
        if (val instanceof Float || val instanceof Double) {
            return true;
        }
        if (val instanceof String) {
            String string = (String) val;
            return string.indexOf('.') != -1 || string.indexOf('e') != -1 || string.indexOf('E') != -1;
        }
        return false;
    }

    /**
     * Is Object a floating point number.
     *
     * @param o Object to be analyzed.
     * @return true if it is a Float or a Double.
     */
    protected boolean isFloatingPoint(final Object o) {
        return o instanceof Float || o instanceof Double;
    }

    /**
     * Is Object a whole number.
     *
     * @param o Object to be analyzed.
     * @return true if Integer, Long, Byte, Short or Character.
     */
    protected boolean isNumberable(final Object o) {
        return o instanceof Integer
            || o instanceof Long
            || o instanceof Byte
            || o instanceof Short
            || o instanceof Character;
    }

    /**
     * Given a BigInteger, narrow it to an Integer or Long if it fits and the arguments
     * class allow it.
     * <p>
     * The rules are:
     * if either arguments is a BigInteger, no narrowing will occur
     * if either arguments is a Long, no narrowing to Integer will occur
     * </p>
     * @param lhs the left hand side operand that lead to the bigi result
     * @param rhs the right hand side operand that lead to the bigi result
     * @param bigi the BigInteger to narrow
     * @return an Integer or Long if narrowing is possible, the original BigInteger otherwise
     */
    protected Number narrowBigInteger(Object lhs, Object rhs, BigInteger bigi) {
        //coerce to long if possible
        if (!(lhs instanceof BigInteger || rhs instanceof BigInteger)
            && bigi.compareTo(BIGI_LONG_MAX_VALUE) <= 0
            && bigi.compareTo(BIGI_LONG_MIN_VALUE) >= 0) {
            // coerce to int if possible
            long l = bigi.longValue();
            // coerce to int when possible (int being so often used in method parms)
            if (!(lhs instanceof Long || rhs instanceof Long)
                && l <= Integer.MAX_VALUE
                && l >= Integer.MIN_VALUE) {
                return Integer.valueOf((int) l);
            }
            return Long.valueOf(l);
        }
        return bigi;
    }

    /**
     * Given an array of objects, attempt to type it more strictly.
     * <ul>
     * <li>If all objects are of the same type, the array returned will be an array of that same type</li>
     * <li>If all objects are Numbers, the array returned will be an array of Numbers</li>
     * <li>If all objects are convertible to a primitive type, the array returned will be an array
     * of the primitive type</li>
     * </ul>
     * @param untyped an untyped array
     * @return the original array if the attempt to strictly type the array fails, a typed array otherwise
     */
    protected Object narrowArrayType(Object[] untyped) {
        final int size = untyped.length;
        Class<?> commonClass = null;
        if (size > 0) {
            boolean isNumber = true;
            // for all children after first...
            for (int u = 0; u < size && !Object.class.equals(commonClass); ++u) {
                if (untyped[u] != null) {
                    Class<?> eclass = untyped[u].getClass();
                    // base common class on first non-null entry
                    if (commonClass == null) {
                        commonClass = eclass;
                        isNumber &= Number.class.isAssignableFrom(commonClass);
                    } else if (!commonClass.equals(eclass)) {
                        // if both are numbers...
                        if (isNumber && Number.class.isAssignableFrom(eclass)) {
                            commonClass = Number.class;
                        } else {
                            // attempt to find valid superclass
                            do {
                                eclass = eclass.getSuperclass();
                                if (eclass == null) {
                                    commonClass = Object.class;
                                    break;
                                }
                            } while(!commonClass.isAssignableFrom(eclass));
                        }
                    }
                } else {
                    isNumber = false;
                }
            }
            // convert array to the common class if not Object.class
            if (commonClass != null && !Object.class.equals(commonClass)) {
                // if the commonClass has an equivalent primitive type, get it
                if (isNumber) {
                    try {
                        final Field type = commonClass.getField("TYPE");
                        commonClass = (Class<?>) type.get(null);
                    } catch (Exception xany) {
                        // ignore
                    }
                }
                // allocate and fill up the typed array
                Object typed = Array.newInstance(commonClass, size);
                for(int i = 0; i < size; ++i) {
                    Array.set(typed, i, untyped[i]);
                }
                return typed;
            }
        }
        return untyped;
    }

    /**
     * Replace all numbers in an arguments array with the smallest type that will fit.
     * @param args the argument array
     * @return true if some arguments were narrowed and args array is modified,
     *         false if no narrowing occured and args array has not been modified
     */
    protected boolean narrowArguments(Object[] args) {
        boolean narrowed = false;
        for (int a = 0; a < args.length; ++a) {
            Object arg = args[a];
            if (arg instanceof Number) {
                Object narg = narrow((Number) arg);
                if (narg != arg) {
                    narrowed = true;
                }
                args[a] = narg;
            }
        }
        return narrowed;
    }

    /**
     * Add two values together.
     * <p>
     * If any numeric add fails on coercion to the appropriate type,
     * treat as Strings and do concatenation.
     * </p>
     * @param left first value
     * @param right second value
     * @return left + right.
     */
    public Object add(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        
        try {
            // if either are floating point (double or float) use double
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                double l = toDouble(left);
                double r = toDouble(right);
                return new Double(l + r);
            }
        
            // if both are bigintegers use that type
            if (left instanceof BigInteger && right instanceof BigInteger) {
                BigInteger l = toBigInteger(left);
                BigInteger r = toBigInteger(right);
                return l.add(r);
            }
            
            // if either are bigdecimal use that type 
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                BigDecimal l = toBigDecimal(left);
                BigDecimal r = toBigDecimal(right);
                return l.add(r, mathContext);
            }
            
            // otherwise treat as integers
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            BigInteger result = l.add(r);
            return narrowBigInteger(left, right, result);
        } catch (java.lang.NumberFormatException nfe) {
            // Well, use strings!
            return toString(left).concat(toString(right));
        }
    }

    /**
     * Divide the left value by the right.
     * @param left first value
     * @param right second value
     * @return left / right
     * @throws ArithmeticException if right == 0
     */
    public Object divide(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }

        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = toDouble(left);
            double r = toDouble(right);
            if (r == 0.0) {
                throw new ArithmeticException("/");
            }
            return new Double(l / r);
        }

        // if both are bigintegers use that type
        if (left instanceof BigInteger && right instanceof BigInteger) {
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            return l.divide(r);
        }

        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = toBigDecimal(left);
            BigDecimal r = toBigDecimal(right);
            BigDecimal d = l.divide(r, mathContext);
            return d;
        }

        // otherwise treat as integers
        BigInteger l = toBigInteger(left);
        BigInteger r = toBigInteger(right);
        BigInteger result = l.divide(r);
        return narrowBigInteger(left, right, result);
    }
    
    /**
     * left value mod right.
     * @param left first value
     * @param right second value
     * @return left mod right
     * @throws ArithmeticException if right == 0.0
     */
    public Object mod(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }

        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = toDouble(left);
            double r = toDouble(right);
            if (r == 0.0) {
                throw new ArithmeticException("%");
            }
            return new Double(l % r);
        }

        // if both are bigintegers use that type
        if (left instanceof BigInteger && right instanceof BigInteger) {
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            return l.mod(r);
        }

        // if either are bigdecimal use that type 
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = toBigDecimal(left);
            BigDecimal r = toBigDecimal(right);
            BigDecimal remainder = l.remainder(r, mathContext);
            return remainder;
        }

        // otherwise treat as integers
        BigInteger l = toBigInteger(left);
        BigInteger r = toBigInteger(right);
        BigInteger result = l.mod(r);
        return narrowBigInteger(left, right, result);
    }
    
    /**
     * Multiply the left value by the right.
     * @param left first value
     * @param right second value
     * @return left * right.
     */
    public Object multiply(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }

        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = toDouble(left);
            double r = toDouble(right);
            return new Double(l * r);
        }
        
        // if both are bigintegers use that type
        if (left instanceof BigInteger && right instanceof BigInteger) {
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            return l.multiply(r);
        }
        
        // if either are bigdecimal use that type 
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = toBigDecimal(left);
            BigDecimal r = toBigDecimal(right);
            return l.multiply(r, mathContext);
        }

        // otherwise treat as integers
        BigInteger l = toBigInteger(left);
        BigInteger r = toBigInteger(right);
        BigInteger result = l.multiply(r);
        return narrowBigInteger(left, right, result);
    }
    
    /**
     * Subtract the right value from the left.
     * @param left first value
     * @param right second value
     * @return left - right.
     */
    public Object subtract(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }

        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = toDouble(left);
            double r = toDouble(right);
            return new Double(l - r);
        }
        
        // if both are bigintegers use that type
        if (left instanceof BigInteger && right instanceof BigInteger) {
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            return l.subtract(r);
        }
        
        // if either are bigdecimal use that type 
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = toBigDecimal(left);
            BigDecimal r = toBigDecimal(right);
            return l.subtract(r, mathContext);
        }

        // otherwise treat as integers
        BigInteger l = toBigInteger(left);
        BigInteger r = toBigInteger(right);
        BigInteger result = l.subtract(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Test if left regexp matches right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    public boolean matches(Object left, Object right) {
        if (left == null && right == null) {
            //if both are null L == R
            return true;
        }
        if (left == null || right == null) {
            // we know both aren't null, therefore L != R
            return false;
        }
        final String arg = left.toString();
        if (right instanceof java.util.regex.Pattern) {
            return ((java.util.regex.Pattern) right).matcher(arg).matches();
        } else {
            return arg.matches(right.toString());
        }
    }

    /**
     * Test if left and right are equal.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    public boolean equals(Object left, Object right) {
        if (left == null && right == null) {
            /*
             * if both are null L == R
             */
            return true;
        } else if (left == null || right == null) {
            /*
             * we know both aren't null, therefore L != R
             */
            return false;
        } else if (left.getClass().equals(right.getClass())) {
            return left.equals(right);
        } else if (left instanceof BigDecimal || right instanceof BigDecimal) {
            return toBigDecimal(left).compareTo(toBigDecimal(right)) == 0;
        } else if (isFloatingPointType(left, right)) {
            return toDouble(left) == toDouble(right);
        } else if (left instanceof Number || right instanceof Number || left instanceof Character
            || right instanceof Character) {
            return toLong(left) == toLong(right);
        } else if (left instanceof Boolean || right instanceof Boolean) {
            return toBoolean(left) == toBoolean(right);
        } else if (left instanceof java.lang.String || right instanceof String) {
            return left.toString().equals(right.toString());
        }

        return left.equals(right);
    }


    /**
     * Test if left < right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    public boolean lessThan(Object left, Object right) {
        if ((left == right) || (left == null) || (right == null)) {
            return false;
        } else if (isFloatingPoint(left) || isFloatingPoint(right)) {
            double leftDouble = toDouble(left);
            double rightDouble = toDouble(right);
            return leftDouble < rightDouble;
        } else if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l  = toBigDecimal(left);
            BigDecimal r  = toBigDecimal(right);
            return l.compareTo(r) < 0;
        } else if (isNumberable(left) || isNumberable(right)) {
            long leftLong = toLong(left);
            long rightLong = toLong(right);
            return leftLong < rightLong;
        } else if (left instanceof String || right instanceof String) {
            String leftString = left.toString();
            String rightString = right.toString();
            return leftString.compareTo(rightString) < 0;
        } else if (left instanceof Comparable<?>) {
            @SuppressWarnings("unchecked") // OK because of instanceof check above
            final Comparable<Object> comparable = (Comparable<Object>) left;
            return comparable.compareTo(right) < 0;
        } else if (right instanceof Comparable<?>) {
            @SuppressWarnings("unchecked") // OK because of instanceof check above
            final Comparable<Object> comparable = (Comparable<Object>) right;
            return comparable.compareTo(left) > 0;
        }

        throw new IllegalArgumentException("Invalid comparison : comparing cardinality for left: " + left
            + " and right: " + right);

    }

    /**
     * Test if left > right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    public boolean greaterThan(Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }
        return !equals(left, right) && !lessThan(left, right);
    }

    /**
     * Test if left <= right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    public boolean lessThanOrEqual(Object left, Object right) {
        return equals(left, right) || lessThan(left, right);
    }

    /**
     * Test if left >= right.
     *
     * @param left first value
     * @param right second value
     * @return test result.
     */
    public boolean greaterThanOrEqual(Object left, Object right) {
        return equals(left, right) || greaterThan(left, right);
    }
    
    /**
     * Coerce to a boolean (not a java.lang.Boolean).
     *
     * @param val Object to be coerced.
     * @return The boolean coerced value, or false if none possible.
     */
    public boolean toBoolean(Object val) {
        if (val == null) {
            controlNullOperand();
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
     * Coerce to a int.
     *
     * @param val Object to be coerced.
     * @return The int coerced value.
     */
    public int toInteger(Object val) {
        if (val == null) {
            controlNullOperand();
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

        throw new IllegalArgumentException("Integer coercion exception. Can't coerce type: "
                + val.getClass().getName());
    }

    
    /**
     * Coerce to a long (not a java.lang.Long).
     *
     * @param val Object to be coerced.
     * @return The long coerced value.
     */
    public long toLong(Object val) {
        if (val == null) {
            controlNullOperand();
            return 0L;
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

        throw new NumberFormatException("Long coercion exception. Can't coerce type: " + val.getClass().getName());
    }

    /**
     * Get a BigInteger from the object passed.
     * Null and empty string maps to zero.
     * @param val the object to be coerced.
     * @return a BigDecimal.
     * @throws NullPointerException if val is null and mode is strict.
     */
    public BigInteger toBigInteger(Object val) {
        if (val instanceof BigInteger) {
            return (BigInteger) val;
        } else if (val == null) {
            controlNullOperand();
            return BigInteger.valueOf(0);
        } else if (val instanceof String) {
            String string = (String) val;
            if ("".equals(string.trim())) {
                return BigInteger.ZERO;
            }
            return new BigInteger(string);
        } else if (val instanceof Number) {
            return new BigInteger(val.toString());
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();
            return BigInteger.valueOf(i);
        }
        
        throw new IllegalArgumentException("BigInteger coercion exception. Can't coerce type: "
                + val.getClass().getName());
    }
    
    /**
     * Get a BigDecimal from the object passed.
     * Null and empty string maps to zero.
     * @param val the object to be coerced.
     * @return a BigDecimal.
     * @throws NullPointerException if val is null and mode is strict.
     */
    public BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        } else if (val == null) {
            controlNullOperand();
            return BigDecimal.ZERO;
        } else if (val instanceof String) {
            String string = ((String) val).trim();
            if ("".equals(string)) {
                return BigDecimal.valueOf(0);
            }
            return new BigDecimal(string, mathContext);
        } else if (val instanceof Number) {
            return new BigDecimal(val.toString(), mathContext);
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();
            return new BigDecimal(i);
        }
        
        throw new IllegalArgumentException("BigDecimal coercion exception. Can't coerce type: "
                + val.getClass().getName());
    }
    
    /**
     * Coerce to a double.
     *
     * @param val Object to be coerced.
     * @return The double coerced value.
     * @throws NullPointerException if val is null and mode is strict.
     */
    public double toDouble(Object val) {
        if (val == null) {
            controlNullOperand();
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

        throw new IllegalArgumentException("Double coercion exception. Can't coerce type: "
                + val.getClass().getName());
    }


    /**
     * Coerce to a string.
     *
     * @param val Object to be coerced.
     * @return The String coerced value.
     * @throws NullPointerException if val is null and mode is strict.
     */
    public String toString(Object val) {
        if (val == null) {
            controlNullOperand();
            val = "";
        }
        return val.toString();
    }

    /**
     * Given a Number, return back the value using the smallest type the result
     * will fit into. This works hand in hand with parameter 'widening' in java
     * method calls, e.g. a call to substring(int,int) with an int and a long
     * will fail, but a call to substring(int,int) with an int and a short will
     * succeed.
     *
     * @param original the original number.
     * @return a value of the smallest type the original number will fit into.
     */
    public Number narrow(Number original) {
        if (original == null) {
            return original;
        }
        Number result = original;
        if (original instanceof BigDecimal) {
            BigDecimal bigd = (BigDecimal) original;
            // if it's bigger than a double it can't be narrowed
            if (bigd.compareTo(BIGD_DOUBLE_MAX_VALUE) > 0) {
                return original;
            }
        }
        if (original instanceof Double || original instanceof Float || original instanceof BigDecimal) {
            double value = original.doubleValue();
            if (value <= Float.MAX_VALUE && value >= Float.MIN_VALUE) {
                result = Float.valueOf(result.floatValue());
            }
            // else it fits in a double only
        } else {
            if (original instanceof BigInteger) {
                BigInteger bigi = (BigInteger) original;
                // if it's bigger than a Long it can't be narrowed
                if (bigi.compareTo(BIGI_LONG_MAX_VALUE) > 0
                    || bigi.compareTo(BIGI_LONG_MIN_VALUE) < 0) {
                    return original;
                }
            }
            long value = original.longValue();
            if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
                // it will fit in a byte
                result = Byte.valueOf((byte) value);
            } else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
                result = Short.valueOf((short) value);
            } else if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
                result = Integer.valueOf((int) value);
            }
            // else it fits in a long
        }
        return result;
    }

}