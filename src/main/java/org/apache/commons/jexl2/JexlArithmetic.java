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
 * <li>If either is a BigDecimal, coerce both to BigDecimal and and perform operation</li>
 * <li>If either is a floating point number, coerce both to Double and perform operation</li>
 * <li>If both are BigInteger, treat as BigInteger and perform operation</li>
 * <li>Else treat as BigInteger, perform operation and attempt to narrow result:
 * <ol>
 * <li>if both arguments can be narrowed to Integer, narrow result to Integer</li>
 * <li>if both arguments can be narrowed to Long, narrow result to Long</li>
 * <li>Else return result as BigInteger</li>
 * </ol>
 * </li>
 * </ol>
 * </p>
 * Note that the only exception throw by JexlArithmetic is ArithmeticException.
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
    /**
     * Default BigDecimal scale.
     * @since 2.1
     */
    protected static final int BIGD_SCALE = -1;
    /**
     * Whether this JexlArithmetic instance behaves in strict or lenient mode.
     * May be made final in a later version.
     */
    private volatile boolean strict;
    /**
     * The big decimal math context.
     * @since 2.1
     */
    protected final MathContext mathContext;
    /**
     * The big decimal scale.
     * @since 2.1
     */
    protected final int mathScale;

    /**
     * Creates a JexlArithmetic.
     * @param lenient whether this arithmetic is lenient or strict
     */
    public JexlArithmetic(boolean lenient) {
        this(lenient, MathContext.DECIMAL128, BIGD_SCALE);
    }

    /**
     * Creates a JexlArithmetic.
     * @param lenient whether this arithmetic is lenient or strict
     * @param bigdContext the math context instance to use for +,-,/,*,% operations on big decimals.
     * @param bigdScale the scale used for big decimals.
     * @since 2.1
     */
    public JexlArithmetic(boolean lenient, MathContext bigdContext, int bigdScale) {
        this.strict = !lenient;
        this.mathContext = bigdContext;
        this.mathScale = bigdScale;
    }


    /**
     * Sets whether this JexlArithmetic instance triggers errors during evaluation when
     * null is used as an operand.
     * <p>This method is <em>not</em> thread safe; it may be called as an optional step by the JexlEngine
     * in its initialization code before expression creation &amp; evaluation.</p>
     * @see JexlEngine#setLenient
     * @see JexlEngine#setSilent
     * @see JexlEngine#setDebug
     * @param flag true means no JexlException will occur, false allows them
     * @deprecated as of 2.1 - may be removed in a later release
     */
    @Deprecated
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
     * @since 2.1
     */
    public MathContext getMathContext() {
        return mathContext;
    }

    /**
     * The BigDecimal scale used for comparison and coercion operations.
     * @return the scale
     * @since 2.1
     */
    public int getMathScale() {
        return mathScale;
    }

    /**
     * Ensure a big decimal is rounded by this arithmetic scale and rounding mode.
     * @param number the big decimal to round
     * @return the rounded big decimal
     * @since 2.1
     */
    public BigDecimal roundBigDecimal(final BigDecimal number) {
        int mscale = getMathScale();
        if (mscale >= 0) {
            return number.setScale(mscale, getMathContext().getRoundingMode());
        } else {
            return number;
        }
    }

    /**
     * The result of +,/,-,*,% when both operands are null.
     * @return Integer(0) if lenient
     * @throws ArithmeticException if strict
     */
    protected Object controlNullNullOperands() {
        if (!isLenient()) {
            throw new ArithmeticException(JexlException.NULL_OPERAND);
        }
        return Integer.valueOf(0);
    }

    /**
     * Throw a NPE if arithmetic is strict.
     * @throws ArithmeticException if strict
     */
    protected void controlNullOperand() {
        if (!isLenient()) {
            throw new ArithmeticException(JexlException.NULL_OPERAND);
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
        return narrowNumber(original, null);
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
     * Whether we consider the narrow class as a potential candidate for narrowing the source.
     * @param narrow the target narrow class
     * @param source the orginal source class
     * @return true if attempt to narrow source to target is accepted
     * @since 2.1
     */
    protected boolean narrowAccept(Class<?> narrow, Class<?> source) {
        return narrow == null || narrow.equals(source);
    }

    /**
     * Given a Number, return back the value attempting to narrow it to a target class.
     * @param original the original number
     * @param narrow the attempted target class
     * @return  the narrowed number or the source if no narrowing was possible
     * @since 2.1
     */
    protected Number narrowNumber(Number original, Class<?> narrow) {
        if (original == null) {
            return original;
        }
        Number result = original;
        if (original instanceof BigDecimal) {
            BigDecimal bigd = (BigDecimal) original;
            // if it's bigger than a double it can't be narrowed
            if (bigd.compareTo(BIGD_DOUBLE_MAX_VALUE) > 0) {
                return original;
            } else {
                try {
                    long l = bigd.longValueExact();
                    // coerce to int when possible (int being so often used in method parms)
                    if (narrowAccept(narrow, Integer.class)
                            && l <= Integer.MAX_VALUE
                            && l >= Integer.MIN_VALUE) {
                        return Integer.valueOf((int) l);
                    } else if (narrowAccept(narrow, Long.class)) {
                        return Long.valueOf(l);
                    }
                } catch (ArithmeticException xa) {
                    // ignore, no exact value narrowing possible
                }
            }
        }
        if (original instanceof Double || original instanceof Float || original instanceof BigDecimal) {
            double value = original.doubleValue();
            if (narrowAccept(narrow, Float.class)
                    && value <= Float.MAX_VALUE
                    && value >= Float.MIN_VALUE) {
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
            if (narrowAccept(narrow, Byte.class)
                    && value <= Byte.MAX_VALUE
                    && value >= Byte.MIN_VALUE) {
                // it will fit in a byte
                result = Byte.valueOf((byte) value);
            } else if (narrowAccept(narrow, Short.class)
                    && value <= Short.MAX_VALUE
                    && value >= Short.MIN_VALUE) {
                result = Short.valueOf((short) value);
            } else if (narrowAccept(narrow, Integer.class)
                    && value <= Integer.MAX_VALUE
                    && value >= Integer.MIN_VALUE) {
                result = Integer.valueOf((int) value);
            }
            // else it fits in a long
        }
        return result;
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
     * Given a BigDecimal, attempt to narrow it to an Integer or Long if it fits if
     * one of the arguments is a numberable.
     *
     * @param lhs the left hand side operand that lead to the bigd result
     * @param rhs the right hand side operand that lead to the bigd result
     * @param bigd the BigDecimal to narrow
     * @return an Integer or Long if narrowing is possible, the original BigInteger otherwise
     * @since 2.1
     */
    protected Number narrowBigDecimal(Object lhs, Object rhs, BigDecimal bigd) {
        if (isNumberable(lhs) || isNumberable(rhs)) {
            try {
                long l = bigd.longValueExact();
                // coerce to int when possible (int being so often used in method parms)
                if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) {
                    return Integer.valueOf((int) l);
                } else {
                    return Long.valueOf(l);
                }
            } catch (ArithmeticException xa) {
                // ignore, no exact value narrowing possible
            }
        }
        return bigd;
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
                            } while (!commonClass.isAssignableFrom(eclass));
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
                        // ignore, no accessible field TYPE in common class, not a primitive class
                    }
                }
                // allocate and fill up the typed array
                Object typed = Array.newInstance(commonClass, size);
                for (int i = 0; i < size; ++i) {
                    Array.set(typed, i, untyped[i]);
                }
                return typed;
            }
        }
        return untyped;
    }

    /**
     * Add two values together.
     * <p>
     * If any numeric add fails on coercion to the appropriate type,
     * treat as Strings and do concatenation.
     * </p>
     * @param left  first value
     * @param right second value
     * @return left + right.
     */
    public Object add(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        try {
            // if either are bigdecimal use that type
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                BigDecimal l = toBigDecimal(left);
                BigDecimal r = toBigDecimal(right);
                BigDecimal result = l.add(r, getMathContext());
                return narrowBigDecimal(left, right, result);
            }
            // if either are floating point (double or float) use double
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                double l = toDouble(left);
                double r = toDouble(right);
                return new Double(l + r);
            }
            // otherwise treat as integers
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            BigInteger result = l.add(r);
            return narrowBigInteger(left, right, result);
        } catch (java.lang.NumberFormatException nfe) {
            // Well, use strings!
            if (left == null || right == null) {
                controlNullOperand();
            }
            return toString(left).concat(toString(right));
        }
    }

    /**
     * Divide the left value by the right.
     * @param left  first value
     * @param right second value
     * @return left / right
     * @throws ArithmeticException if right == 0
     */
    public Object divide(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = toBigDecimal(left);
            BigDecimal r = toBigDecimal(right);
            if (BigDecimal.ZERO.equals(r)) {
                throw new ArithmeticException("/");
                }
            BigDecimal result = l.divide(r, getMathContext());
            return narrowBigDecimal(left, right, result);
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
        // otherwise treat as integers
        BigInteger l = toBigInteger(left);
        BigInteger r = toBigInteger(right);
        if (BigInteger.ZERO.equals(r)) {
            throw new ArithmeticException("/");
            }
        BigInteger result = l.divide(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * left value mod right.
     * @param left  first value
     * @param right second value
     * @return left mod right
     * @throws ArithmeticException if right == 0.0
     */
    public Object mod(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = toBigDecimal(left);
            BigDecimal r = toBigDecimal(right);
            if (BigDecimal.ZERO.equals(r)) {
                throw new ArithmeticException("%");
                }
            BigDecimal remainder = l.remainder(r, getMathContext());
            return narrowBigDecimal(left, right, remainder);
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
        // otherwise treat as integers
        BigInteger l = toBigInteger(left);
        BigInteger r = toBigInteger(right);
        BigInteger result = l.mod(r);
        if (BigInteger.ZERO.equals(r)) {
            throw new ArithmeticException("%");
            }
        return narrowBigInteger(left, right, result);
    }

    /**
     * Multiply the left value by the right.
     * @param left  first value
     * @param right second value
     * @return left * right.
     */
    public Object multiply(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = toBigDecimal(left);
            BigDecimal r = toBigDecimal(right);
            BigDecimal result = l.multiply(r, getMathContext());
            return narrowBigDecimal(left, right, result);
        }
        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = toDouble(left);
            double r = toDouble(right);
            return new Double(l * r);
        }
        // otherwise treat as integers
        BigInteger l = toBigInteger(left);
        BigInteger r = toBigInteger(right);
        BigInteger result = l.multiply(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Subtract the right value from the left.
     * @param left  first value
     * @param right second value
     * @return left - right.
     */
    public Object subtract(Object left, Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands();
        }
        // if either are bigdecimal use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = toBigDecimal(left);
            BigDecimal r = toBigDecimal(right);
            BigDecimal result = l.subtract(r, getMathContext());
            return narrowBigDecimal(left, right, result);
        }
        // if either are floating point (double or float) use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            double l = toDouble(left);
            double r = toDouble(right);
            return new Double(l - r);
        }
        // otherwise treat as integers
        BigInteger l = toBigInteger(left);
        BigInteger r = toBigInteger(right);
        BigInteger result = l.subtract(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Negates a value (unary minus for numbers).
     * @param val the value to negate
     * @return the negated value
     */
    public Object negate(Object val) {
        if (val instanceof Integer) {
            int valueAsInt = ((Integer) val).intValue();
            return Integer.valueOf(-valueAsInt);
        } else if (val instanceof Double) {
            double valueAsDouble = ((Double) val).doubleValue();
            return new Double(-valueAsDouble);
        } else if (val instanceof Long) {
            long valueAsLong = -((Long) val).longValue();
            return Long.valueOf(valueAsLong);
        } else if (val instanceof BigDecimal) {
            BigDecimal valueAsBigD = (BigDecimal) val;
            return valueAsBigD.negate();
        } else if (val instanceof BigInteger) {
            BigInteger valueAsBigI = (BigInteger) val;
            return valueAsBigI.negate();
        } else if (val instanceof Float) {
            float valueAsFloat = ((Float) val).floatValue();
            return new Float(-valueAsFloat);
        } else if (val instanceof Short) {
            short valueAsShort = ((Short) val).shortValue();
            return Short.valueOf((short) -valueAsShort);
        } else if (val instanceof Byte) {
            byte valueAsByte = ((Byte) val).byteValue();
            return Byte.valueOf((byte) -valueAsByte);
        } else if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue() ? Boolean.FALSE : Boolean.TRUE;
        }
        throw new ArithmeticException("Object negation:(" + val + ")");
    }

    /**
     * Test if left regexp matches right.
     *
     * @param left  first value
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
     * Performs a bitwise and.
     * @param left  the left operand
     * @param right the right operator
     * @return left & right
     */
    public Object bitwiseAnd(Object left, Object right) {
        long l = toLong(left);
        long r = toLong(right);
        return Long.valueOf(l & r);
    }

    /**
     * Performs a bitwise or.
     * @param left  the left operand
     * @param right the right operator
     * @return left | right
     */
    public Object bitwiseOr(Object left, Object right) {
        long l = toLong(left);
        long r = toLong(right);
        return Long.valueOf(l | r);
    }

    /**
     * Performs a bitwise xor.
     * @param left  the left operand
     * @param right the right operator
     * @return left right
     */
    public Object bitwiseXor(Object left, Object right) {
        long l = toLong(left);
        long r = toLong(right);
        return Long.valueOf(l ^ r);
    }

    /**
     * Performs a bitwise complement.
     * @param val the operand
     * @return ~val
     */
    public Object bitwiseComplement(Object val) {
        long l = toLong(val);
        return Long.valueOf(~l);
    }

    /**
     * Performs a comparison.
     * @param left     the left operand
     * @param right    the right operator
     * @param operator the operator
     * @return -1 if left &lt; right; +1 if left &gt > right; 0 if left == right
     * @throws ArithmeticException if either left or right is null
     */
    protected int compare(Object left, Object right, String operator) {
        if (left != null && right != null) {
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                BigDecimal l = toBigDecimal(left);
                BigDecimal r = toBigDecimal(right);
                return l.compareTo(r);
            } else if (left instanceof BigInteger || right instanceof BigInteger) {
                BigInteger l = toBigInteger(left);
                BigInteger r = toBigInteger(right);
                return l.compareTo(r);
            } else if (isFloatingPoint(left) || isFloatingPoint(right)) {
                double lhs = toDouble(left);
                double rhs = toDouble(right);
                if (Double.isNaN(lhs)) {
                    if (Double.isNaN(rhs)) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else if (Double.isNaN(rhs)) {
                    // lhs is not NaN
                    return +1;
                } else if (lhs < rhs) {
                    return -1;
                } else if (lhs > rhs) {
                    return +1;
                } else {
                    return 0;
                }
            } else if (isNumberable(left) || isNumberable(right)) {
                long lhs = toLong(left);
                long rhs = toLong(right);
                if (lhs < rhs) {
                    return -1;
                } else if (lhs > rhs) {
                    return +1;
                } else {
                    return 0;
                }
            } else if (left instanceof String || right instanceof String) {
                return toString(left).compareTo(toString(right));
            } else if ("==".equals(operator)) {
                return left.equals(right) ? 0 : -1;
            } else if (left instanceof Comparable<?>) {
                @SuppressWarnings("unchecked") // OK because of instanceof check above
                final Comparable<Object> comparable = (Comparable<Object>) left;
                return comparable.compareTo(right);
            } else if (right instanceof Comparable<?>) {
                @SuppressWarnings("unchecked") // OK because of instanceof check above
                final Comparable<Object> comparable = (Comparable<Object>) right;
                return comparable.compareTo(left);
            }
        }
        throw new ArithmeticException("Object comparison:(" + left + " " + operator + " " + right + ")");
    }

    /**
     * Test if left and right are equal.
     *
     * @param left  first value
     * @param right second value
     * @return test result.
     */
    public boolean equals(Object left, Object right) {
        if (left == right) {
            return true;
        } else if (left == null || right == null) {
            return false;
        } else if (left instanceof Boolean || right instanceof Boolean) {
            return toBoolean(left) == toBoolean(right);
        } else {
            return compare(left, right, "==") == 0;
        }
    }

    /**
     * Test if left &lt; right.
     *
     * @param left  first value
     * @param right second value
     * @return test result.
     */
    public boolean lessThan(Object left, Object right) {
        if ((left == right) || (left == null) || (right == null)) {
            return false;
        } else {
            return compare(left, right, "<") < 0;
        }

    }

    /**
     * Test if left &gt; right.
     *
     * @param left  first value
     * @param right second value
     * @return test result.
     */
    public boolean greaterThan(Object left, Object right) {
        if ((left == right) || left == null || right == null) {
            return false;
        } else {
            return compare(left, right, ">") > 0;
        }
    }

    /**
     * Test if left &lt;= right.
     *
     * @param left  first value
     * @param right second value
     * @return test result.
     */
    public boolean lessThanOrEqual(Object left, Object right) {
        if (left == right) {
            return true;
        } else if (left == null || right == null) {
            return false;
        } else {
            return compare(left, right, "<=") <= 0;
        }
    }

    /**
     * Test if left &gt;= right.
     *
     * @param left  first value
     * @param right second value
     * @return test result.
     */
    public boolean greaterThanOrEqual(Object left, Object right) {
        if (left == right) {
            return true;
        } else if (left == null || right == null) {
            return false;
        } else {
            return compare(left, right, ">=") >= 0;
        }
    }

    /**
     * Coerce to a primitive boolean.
     * <p>Double.NaN, null, "false" and empty string coerce to false.</p>
     *
     * @param val Object to be coerced.
     * @return the boolean value if coercion is possible, true if value was not null.
     */
    public boolean toBoolean(Object val) {
        if (val == null) {
            controlNullOperand();
            return false;
        } else if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue();
        } else if (val instanceof Number) {
            double number = toDouble(val);
            return !Double.isNaN(number) && number != 0.d;
        } else if (val instanceof String) {
            String strval = val.toString();
            return strval.length() > 0 && !"false".equals(strval);
        } else {
            // non null value is true
            return true;
        }
    }

    /**
     * Coerce to a primitive int.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val Object to be coerced.
     * @return The int coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible.
     */
    public int toInteger(Object val) {
        if (val == null) {
            controlNullOperand();
            return 0;
        } else if (val instanceof Double) {
            Double dval = (Double) val;
            if (Double.isNaN(dval.doubleValue())) {
                return 0;
            } else {
                return dval.intValue();
            }
        } else if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return 0;
            }
            return Integer.parseInt((String) val);
        } else if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue() ? 1 : 0;
        } else if (val instanceof Character) {
            return ((Character) val).charValue();
        }

        throw new ArithmeticException("Integer coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive long.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val Object to be coerced.
     * @return The long coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible.
     */
    public long toLong(Object val) {
        if (val == null) {
            controlNullOperand();
            return 0L;
        } else if (val instanceof Double) {
            Double dval = (Double) val;
            if (Double.isNaN(dval.doubleValue())) {
                return 0L;
            } else {
                return dval.longValue();
            }
        } else if (val instanceof Number) {
            return ((Number) val).longValue();
        } else if (val instanceof String) {
            if ("".equals(val)) {
                return 0L;
            } else {
                return Long.parseLong((String) val);
            }
        } else if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue() ? 1L : 0L;
        } else if (val instanceof Character) {
            return ((Character) val).charValue();
        }

        throw new ArithmeticException("Long coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a BigInteger.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val the object to be coerced.
     * @return a BigDecimal.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible.
     */
    public BigInteger toBigInteger(Object val) {
        if (val == null) {
            controlNullOperand();
            return BigInteger.ZERO;
        } else if (val instanceof BigInteger) {
            return (BigInteger) val;
        } else if (val instanceof Double) {
            Double dval = (Double) val;
            if (Double.isNaN(dval.doubleValue())) {
                return BigInteger.ZERO;
            } else {
                return new BigInteger(dval.toString());
            }
        } else if (val instanceof Number) {
            return new BigInteger(val.toString());
        } else if (val instanceof String) {
            String string = (String) val;
            if ("".equals(string)) {
                return BigInteger.ZERO;
            } else {
                return new BigInteger(string);
            }
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();
            return BigInteger.valueOf(i);
        }

        throw new ArithmeticException("BigInteger coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a BigDecimal.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val the object to be coerced.
     * @return a BigDecimal.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible.
     */
    public BigDecimal toBigDecimal(Object val) {
        if (val instanceof BigDecimal) {
            return roundBigDecimal((BigDecimal) val);
        } else if (val == null) {
            controlNullOperand();
            return BigDecimal.ZERO;
        } else if (val instanceof String) {
            String string = (String) val;
            if ("".equals(string)) {
                return BigDecimal.ZERO;
            }
            return roundBigDecimal(new BigDecimal(string, getMathContext()));
        } else if (val instanceof Double) {
            if (Double.isNaN(((Double) val).doubleValue())) {
                return BigDecimal.ZERO;
            } else {
                return roundBigDecimal(new BigDecimal(val.toString(), getMathContext()));
            }
        } else if (val instanceof Number) {
            return roundBigDecimal(new BigDecimal(val.toString(), getMathContext()));
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();
            return new BigDecimal(i);
        }
        throw new ArithmeticException("BigDecimal coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive double.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     * @param val Object to be coerced.
     * @return The double coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible.
     */
    public double toDouble(Object val) {
        if (val == null) {
            controlNullOperand();
            return 0;
        } else if (val instanceof Double) {
            return ((Double) val).doubleValue();
        } else if (val instanceof Number) {
            //The below construct is used rather than ((Number)val).doubleValue() to ensure
            //equality between comparing new Double( 6.4 / 3 ) and the jexl expression of 6.4 / 3
            return Double.parseDouble(String.valueOf(val));
        } else if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue() ? 1. : 0.;
        } else if (val instanceof String) {
            String string = (String) val;
            if ("".equals(string)) {
                return Double.NaN;
            } else {
                // the spec seems to be iffy about this.  Going to give it a wack anyway
                return Double.parseDouble(string);
            }
        } else if (val instanceof Character) {
            int i = ((Character) val).charValue();
            return i;
        }
        throw new ArithmeticException("Double coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a string.
     * <p>Double.NaN coerce to the empty string.</p>
     *
     * @param val Object to be coerced.
     * @return The String coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible.
     */
    public String toString(Object val) {
        if (val == null) {
            controlNullOperand();
            return "";
        } else if (val instanceof Double) {
            Double dval = (Double) val;
            if (Double.isNaN(dval.doubleValue())) {
                return "";
            } else {
                return dval.toString();
            }
        } else {
            return val.toString();
        }
    }

}