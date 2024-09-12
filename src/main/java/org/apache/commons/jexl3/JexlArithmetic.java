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

package org.apache.commons.jexl3;

import static java.lang.StrictMath.floor;
import static org.apache.commons.jexl3.JexlOperator.EQ;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.introspection.JexlMethod;

/**
 * Perform arithmetic, implements JexlOperator methods.
 *
 * <p>This is the class to derive to implement new operator behaviors.</p>
 *
 * <p>The 5 base arithmetic operators (+, - , *, /, %) follow the same evaluation rules regarding their arguments.</p>
 * <ol>
 *   <li>If both are null, result is 0 if arithmetic (or operator) is non-strict, ArithmeticException is thrown
 *   otherwise</li>
 *   <li>If both arguments are numberable - any kind of integer including boolean -, coerce both to Long and coerce
 *   result to the most precise argument class ({@code boolean < byte < short < int < long});
 *   if long operation would cause overflow, return a BigInteger</li>
 *   <li>If either argument is a BigDecimal, coerce both to BigDecimal, operator returns BigDecimal</li>
 *   <li>If either argument is a floating point number, coerce both to Double, operator returns Double</li>
 *   <li>Else treat as BigInteger, perform operation and narrow result to the most precise argument class
 *   </li>
 * </ol>
 *
 * Note that the only exception thrown by JexlArithmetic is and must be ArithmeticException.
 *
 * @see JexlOperator
 * @since 2.0
 */
public class JexlArithmetic {
    /**
     * Helper interface used when creating an array literal.
     *
     * <p>The default implementation creates an array and attempts to type it strictly.</p>
     *
     * <ul>
     *   <li>If all objects are of the same type, the array returned will be an array of that same type</li>
     *   <li>If all objects are Numbers, the array returned will be an array of Numbers</li>
     *   <li>If all objects are convertible to a primitive type, the array returned will be an array
     *       of the primitive type</li>
     * </ul>
     */
    public interface ArrayBuilder {

        /**
         * Adds a literal to the array.
         *
         * @param value the item to add
         */
        void add(Object value);

        /**
         * Creates the actual "array" instance.
         *
         * @param extended true when the last argument is ', ...'
         * @return the array
         */
        Object create(boolean extended);
    }

    /** Marker class for coercion operand exceptions. */
    public static class CoercionException extends ArithmeticException {
        private static final long serialVersionUID = 202402081150L;

        /**
         * Simple ctor.
         * @param msg the exception message
         */
        public CoercionException(final String msg) {
            super(msg);
        }
    }

    /**
     * Helper interface used when creating a map literal.
     * <p>The default implementation creates a java.util.HashMap.</p>
     */
    public interface MapBuilder {
        /**
         * Creates the actual "map" instance.
         *
         * @return the map
         */
        Object create();

        /**
         * Adds a new entry to the map.
         *
         * @param key   the map entry key
         * @param value the map entry value
         */
        void put(Object key, Object value);
    }

    /** Marker class for null operand exceptions. */
    public static class NullOperand extends ArithmeticException {
        private static final long serialVersionUID = 4720876194840764770L;
    }

    /**
     * Helper interface used when creating a set literal.
     * <p>The default implementation creates a java.util.HashSet.</p>
     */
    public interface SetBuilder {
        /**
         * Adds a literal to the set.
         *
         * @param value the item to add
         */
        void add(Object value);

        /**
         * Creates the actual "set" instance.
         *
         * @return the set
         */
        Object create();
    }

    /**
     * The interface that uberspects JexlArithmetic classes.
     * <p>This allows overloaded operator methods discovery.</p>
     */
    public interface Uberspect {
        /**
         * Gets the most specific method for an operator.
         *
         * @param operator the operator
         * @param arg      the arguments
         * @return the most specific method or null if no specific override could be found
         */
        JexlMethod getOperator(JexlOperator operator, Object... arg);

        /**
         * Checks whether this uberspect has overloads for a given operator.
         *
         * @param operator the operator to check
         * @return true if an overload exists, false otherwise
         */
        boolean overloads(JexlOperator operator);
    }

    /** Double.MAX_VALUE as BigDecimal. */
    protected static final BigDecimal BIGD_DOUBLE_MAX_VALUE = BigDecimal.valueOf(Double.MAX_VALUE);

    /** -Double.MAX_VALUE as BigDecimal. */
    protected static final BigDecimal BIGD_DOUBLE_MIN_VALUE = BigDecimal.valueOf(-Double.MAX_VALUE);

    /** Long.MAX_VALUE as BigInteger. */
    protected static final BigInteger BIGI_LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    /** Long.MIN_VALUE as BigInteger. */
    protected static final BigInteger BIGI_LONG_MIN_VALUE = BigInteger.valueOf(Long.MIN_VALUE);

    /** Default BigDecimal scale. */
    protected static final int BIGD_SCALE = -1;

    /**
     * The float regular expression pattern.
     * <p>
     * The decimal and exponent parts are optional and captured allowing to determine if the number is a real
     * by checking whether one of these 2 capturing groups is not empty.
     */
    public static final Pattern FLOAT_PATTERN = Pattern.compile("^[+-]?\\d*(\\.\\d*)?([eE][+-]?\\d+)?$");

    /**
     * Attempts transformation of potential array in an abstract list or leave as is.
     * <p>An array (as in int[]) is not convenient to call methods so when encountered we turn them into lists</p>
     * @param container an array or on object
     * @return an abstract list wrapping the array instance or the initial argument
     * @see org.apache.commons.jexl3.internal.introspection.ArrayListWrapper
     */
    private static Object arrayWrap(final Object container) {
        return container.getClass().isArray()
                ? new org.apache.commons.jexl3.internal.introspection.ArrayListWrapper(container)
                : container;
    }

    private static boolean computeCompare321(final JexlArithmetic arithmetic) {
        Class<?> arithmeticClass = arithmetic.getClass();
        while(arithmeticClass != JexlArithmetic.class) {
            try {
                final Method cmp = arithmeticClass.getDeclaredMethod("compare", Object.class, Object.class, String.class);
               if (cmp.getDeclaringClass() != JexlArithmetic.class) {
                   return true;
               }
            } catch (final NoSuchMethodException xany) {
                arithmeticClass = arithmeticClass.getSuperclass();
            }
        }
        return false;
    }

    /**
     * Checks if the product of the arguments overflows a {@code long}.
     * <p>see java8 Math.multiplyExact
     * @param x the first value
     * @param y the second value
     * @param r the product
     * @return true if product fits a long, false if it overflows
     */
    @SuppressWarnings("MagicNumber")
    protected static boolean isMultiplyExact(final long x, final long y, final long r) {
        final long ax = Math.abs(x);
        final long ay = Math.abs(y);
        // Some bits greater than 2^31 that might cause overflow
        // Check the result using the divide operator
        // and check for the special case of Long.MIN_VALUE * -1
        return !((ax | ay) >>> Integer.SIZE - 1 != 0
                 && (y != 0 && r / y != x
                     || x == Long.MIN_VALUE && y == -1));
    }

    /** Whether this JexlArithmetic instance behaves in strict or lenient mode. */
    private final boolean strict;

    /** The big decimal math context. */
    private final MathContext mathContext;

    /** The big decimal scale. */
    private final int mathScale;

    /** The dynamic constructor. */
    private final Constructor<? extends JexlArithmetic> ctor;

    /**
     * Determines if the compare method(Object, Object, String) is overriden in this class or one of its
     * superclasses.
     */
    private final boolean compare321 = computeCompare321(this);

    /**
     * Creates a JexlArithmetic.
     * <p>If you derive your own arithmetic, implement the
     * other constructor that may be needed when dealing with options.
     *
     * @param astrict whether this arithmetic is strict or lenient
     */
    public JexlArithmetic(final boolean astrict) {
        this(astrict, null, Integer.MIN_VALUE);
    }

    /**
     * Creates a JexlArithmetic.
     * <p>The constructor to define in derived classes.
     *
     * @param astrict     whether this arithmetic is lenient or strict
     * @param bigdContext the math context instance to use for +,-,/,*,% operations on big decimals.
     * @param bigdScale   the scale used for big decimals.
     */
    public JexlArithmetic(final boolean astrict, final MathContext bigdContext, final int bigdScale) {
        this.strict = astrict;
        this.mathContext = bigdContext == null ? MathContext.DECIMAL128 : bigdContext;
        this.mathScale = bigdScale == Integer.MIN_VALUE ? BIGD_SCALE : bigdScale;
        Constructor<? extends JexlArithmetic> actor = null;
        try {
            actor = getClass().getConstructor(boolean.class, MathContext.class, int.class);
        } catch (final Exception xany) {
            // ignore
        }
        this.ctor = actor;
    }

    /**
     * Add two values together.
     * <p>
     * If any numeric add fails on coercion to the appropriate type,
     * treat as Strings and do concatenation.
     * </p>
     *
     * @param left  left argument
     * @param right  right argument
     * @return left + right.
     */
    public Object add(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.ADD);
        }
        final boolean strconcat = strict
                            ? left instanceof String || right instanceof String
                            : left instanceof String && right instanceof String;
        if (!strconcat) {
            try {
                final boolean strictCast = isStrict(JexlOperator.ADD);
                // if both (non-null) args fit as long
                final Number ln = asLongNumber(strictCast, left);
                final Number rn = asLongNumber(strictCast, right);
                if (ln != null && rn != null) {
                    final long x = ln.longValue();
                    final long y = rn.longValue();
                    final long result = x + y;
                    // detect overflow, see java8 Math.addExact
                    if (((x ^ result) & (y ^ result)) < 0) {
                        return BigInteger.valueOf(x).add(BigInteger.valueOf(y));
                    }
                    return narrowLong(left, right, result);
                }
                // if either are BigDecimal, use that type
                if (left instanceof BigDecimal || right instanceof BigDecimal) {
                    final BigDecimal l = toBigDecimal(strictCast, left);
                    final BigDecimal r = toBigDecimal(strictCast, right);
                    return l.add(r, getMathContext());
                }
                // if either are floating point (double or float), use double
                if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                    final double l = toDouble(strictCast, left);
                    final double r = toDouble(strictCast, right);
                    return l + r;
                }
                // otherwise treat as BigInteger
                final BigInteger l = toBigInteger(strictCast, left);
                final BigInteger r = toBigInteger(strictCast, right);
                final BigInteger result = l.add(r);
                return narrowBigInteger(left, right, result);
            } catch (final ArithmeticException nfe) {
                // ignore and continue in sequence
            }
        }
        return (left == null ? "" : toString(left)).concat(right == null ? "" : toString(right));
    }

    /**
     * Performs a bitwise and.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left &amp; right
     */
    public Object and(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toLong(right);
        return l & r;
    }

    /**
     * Creates an array builder.
     * @param size the number of elements in the array
     * @return an array builder instance
     * @deprecated since 3.3.1
     */
    @Deprecated
    public ArrayBuilder arrayBuilder(final int size) {
        return arrayBuilder(size, false);
    }

    /**
     * Called by the interpreter when evaluating a literal array.
     *
     * @param size the number of elements in the array
     * @param extended whether the map is extended or not
     * @return the array builder
     */
    public ArrayBuilder arrayBuilder(final int size, final boolean extended) {
        return new org.apache.commons.jexl3.internal.ArrayBuilder(size, extended);
    }

    /**
     * Checks if value class is a number that can be represented exactly in a long.
     * <p>For convenience, booleans are converted as 1/0 (true/false).</p>
     *
     * @param strict whether null argument is converted as 0 or remains null
     * @param value  argument
     * @return a non-null value if argument can be represented by a long
     */
    protected Number asLongNumber(final boolean strict, final Object value) {
        if (value instanceof Long
            || value instanceof Integer
            || value instanceof Short
            || value instanceof Byte) {
            return (Number) value;
        }
        if (value instanceof Boolean) {
            final Boolean b = (Boolean) value;
            return b ? 1L : 0L;
        }
        if (value instanceof AtomicBoolean) {
            final AtomicBoolean b = (AtomicBoolean) value;
            return b.get() ? 1L : 0L;
        }
        if (value == null && !strict) {
            return 0L;
        }
        return null;
    }

    /**
     * Checks if value class is a number that can be represented exactly in a long.
     * <p>For convenience, booleans are converted as 1/0 (true/false).</p>
     *
     * @param value  argument
     * @return a non-null value if argument can be represented by a long
     */
    protected Number asLongNumber(final Object value) {
        return asLongNumber(strict, value);
    }

    /**
     * Use or overload and() instead.
     * @param lhs left hand side
     * @param rhs right hand side
     * @return lhs &amp; rhs
     * @see JexlArithmetic#and
     * @deprecated 3.0
     */
    @Deprecated
    public final Object bitwiseAnd(final Object lhs, final Object rhs) {
        return and(lhs, rhs);
    }

    /**
     * Use or overload or() instead.
     *
     * @param lhs left hand side
     * @param rhs right hand side
     * @return lhs | rhs
     * @see JexlArithmetic#or
     * @deprecated 3.0
     */
    @Deprecated
    public final Object bitwiseOr(final Object lhs, final Object rhs) {
        return or(lhs, rhs);
    }

    /**
     * Use or overload xor() instead.
     *
     * @param lhs left hand side
     * @param rhs right hand side
     * @return lhs ^ rhs
     * @see JexlArithmetic#xor
     * @deprecated 3.0
     */
    @Deprecated
    public final Object bitwiseXor(final Object lhs, final Object rhs) {
        return xor(lhs, rhs);
    }

    /**
     * Checks whether a potential collection contains another.
     * <p>Made protected to make it easier to override if needed.</p>
     * @param collection the container which can be a collection or an array (even of primitive)
     * @param value the value which can be a collection or an array (even of primitive) or a singleton
     * @return test result or null if there is no arithmetic solution
     */
    protected Boolean collectionContains(final Object collection, final Object value) {
        // convert arrays if needed
        final Object left = arrayWrap(collection);
        if (left instanceof Collection<?>) {
            final Object right = arrayWrap(value);
            if (right instanceof Collection<?>) {
                return ((Collection<?>) left).containsAll((Collection<?>) right);
            }
            return ((Collection<?>) left).contains(value);
        }
        return null;
    }

    /**
     * Performs a comparison.
     *
     * @param left     the left operand
     * @param right    the right operator
     * @param operator the operator
     * @return -1 if left &lt; right; +1 if left &gt; right; 0 if left == right
     * @throws ArithmeticException if either left or right is null
     */
    protected int compare(final Object left, final Object right, final JexlOperator operator) {
        // this is a temporary way of allowing pre-3.3 code that overrode compare() to still call
        // the user method. This method will merge with doCompare in 3.4 and the compare321 flag will disappear.
        return compare321
                ? compare(left, right, operator.toString())
                : doCompare(left, right, operator);
    }

    /**
     * Any override of this method (pre 3.3) should be modified to match the new signature.
     * @param left left operand
     * @param right right operand
     * @param symbol the operator symbol
     * @return -1 if left &lt; right; +1 if left &gt; right; 0 if left == right
     * {@link JexlArithmetic#compare(Object, Object, JexlOperator)}
     * @deprecated 3.3
     */
    @Deprecated
    protected int compare(final Object left, final Object right, final String symbol) {
        JexlOperator operator;
        try {
            operator = JexlOperator.valueOf(symbol);
        } catch (final IllegalArgumentException xill) {
            // ignore
            operator = EQ;
        }
        return doCompare(left, right, operator);
    }

    /**
     * Performs a bitwise complement.
     *
     * @param val the operand
     * @return ~val
     */
    public Object complement(final Object val) {
        final boolean strictCast = isStrict(JexlOperator.COMPLEMENT);
        final long l = toLong(strictCast, val);
        return ~l;
    }

    /**
     * Test if left contains right (right matches/in left).
     * <p>Beware that this &quot;contains &quot; method arguments order is the opposite of the
     * &quot;in/matches&quot; operator arguments.
     * {@code x =~ y} means {@code y contains x} thus {@code contains(x, y)}.</p>
     * <p>When this method returns null during evaluation, the operator code continues trying to find
     * one through the uberspect.</p>
     * @param container the container
     * @param value the value
     * @return test result or null if there is no arithmetic solution
     */
    public Boolean contains(final Object container, final Object value) {
        if (value == null && container == null) {
            //if both are null L == R
            return true;
        }
        if (value == null || container == null) {
            // we know both aren't null, therefore L != R
            return false;
        }
        // use arithmetic / pattern matching ?
        if (container instanceof java.util.regex.Pattern) {
            return ((java.util.regex.Pattern) container).matcher(value.toString()).matches();
        }
        if (container instanceof CharSequence) {
            return value.toString().matches(container.toString());
        }
        // try contains on map key
        if (container instanceof Map<?, ?>) {
            if (value instanceof Map<?, ?>) {
                return ((Map<?, ?>) container).keySet().containsAll(((Map<?, ?>) value).keySet());
            }
            return ((Map<?, ?>) container).containsKey(value);
        }
        // try contains on collection
        return collectionContains(container, value);
    }

    /**
     * The result of +,/,-,*,% when both operands are null.
     *
     * @return Integer(0) if lenient
     * @throws JexlArithmetic.NullOperand if strict
     * @deprecated 3.3
     */
    @Deprecated
    protected Object controlNullNullOperands() {
        if (isStrict()) {
            throw new NullOperand();
        }
        return 0;
    }

    /**
     * The result of +,/,-,*,% when both operands are null.
     * @param operator the actual operator
     * @return Integer(0) if lenient
     * @throws  JexlArithmetic.NullOperand if strict-cast
     * @since 3.3
     */
    protected Object controlNullNullOperands(final JexlOperator operator) {
        if (isStrict(operator)) {
            throw new NullOperand();
        }
        return 0;
    }

    /**
     * Throws an NullOperand exception if arithmetic is strict-cast.
     *
     * @throws  JexlArithmetic.NullOperand if strict
     * @deprecated 3.3
     */
    @Deprecated
    protected void controlNullOperand() {
        if (isStrict()) {
            throw new NullOperand();
        }
    }

    /**
     * Throws an NullOperand exception if arithmetic is strict-cast.
     * <p>This method is called by the cast methods ({@link #toBoolean(boolean, Object)},
     * {@link #toInteger(boolean, Object)}, {@link #toDouble(boolean, Object)},
     * {@link #toString(boolean, Object)}, {@link #toBigInteger(boolean, Object)},
     * {@link #toBigDecimal(boolean, Object)}) when they encounter a null argument.</p>
     *
     * @param strictCast whether strict cast is required
     * @param defaultValue the default value to return, if not strict
     * @param <T> the value type
     * @return the default value is strict is false
     * @throws JexlArithmetic.NullOperand if strict-cast
     * @since 3.3
     */
    protected <T> T controlNullOperand(final boolean strictCast, final T defaultValue) {
        if (strictCast) {
            throw new NullOperand();
        }
        return defaultValue;
    }

    /**
     * The last method called before returning a result from a script execution.
     * @param returned the returned value
     * @return the controlled returned value
     */
    public Object controlReturn(final Object returned) {
        return returned;
    }

    /**
     * Creates a literal range.
     * <p>The default implementation only accepts integers and longs.</p>
     *
     * @param from the included lower bound value (null if none)
     * @param to   the included upper bound value (null if none)
     * @return the range as an iterable
     * @throws ArithmeticException as an option if creation fails
     */
    public Iterable<?> createRange(final Object from, final Object to) throws ArithmeticException {
        final long lfrom = toLong(from);
        final long lto = toLong(to);
        if (lfrom >= Integer.MIN_VALUE && lfrom <= Integer.MAX_VALUE
                && lto >= Integer.MIN_VALUE && lto <= Integer.MAX_VALUE) {
            return org.apache.commons.jexl3.internal.IntegerRange.create((int) lfrom, (int) lto);
        }
        return org.apache.commons.jexl3.internal.LongRange.create(lfrom, lto);
    }

    /**
     * Creates a JexlArithmetic instance.
     * Called by options(...) method when another instance of the same class of arithmetic is required.
     * @see #options(org.apache.commons.jexl3.JexlEngine.Options)
     *
     * @param astrict     whether this arithmetic is lenient or strict
     * @param bigdContext the math context instance to use for +,-,/,*,% operations on big decimals.
     * @param bigdScale   the scale used for big decimals.
     * @return default is a new JexlArithmetic instance
     * @since 3.1
     */
    protected JexlArithmetic createWithOptions(final boolean astrict, final MathContext bigdContext, final int bigdScale) {
        if (ctor != null) {
            try {
                return ctor.newInstance(astrict, bigdContext, bigdScale);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InstantiationException | InvocationTargetException xany) {
                // it was worth the try
            }
        }
        return new JexlArithmetic(astrict, bigdContext, bigdScale);
    }

    /**
     * Decrements argument by 1.
     * @param val the argument
     * @return val - 1
     */
    public Object decrement(final Object val) {
        return increment(val, -1);
    }

    /**
     * Divide the left value by the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left / right
     * @throws ArithmeticException if right == 0
     */
    public Object divide(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.DIVIDE);
        }
        final boolean strictCast = isStrict(JexlOperator.DIVIDE);
        // if both (non-null) args fit as long
        final Number ln = asLongNumber(strictCast, left);
        final Number rn = asLongNumber(strictCast, right);
        if (ln != null && rn != null) {
            final long x = ln.longValue();
            final long y = rn.longValue();
            if (y == 0L) {
                throw new ArithmeticException("/");
            }
            final long result = x  / y;
            return narrowLong(left, right, result);
        }
        // if either are BigDecimal, use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            final BigDecimal l = toBigDecimal(strictCast, left);
            final BigDecimal r = toBigDecimal(strictCast, right);
            if (BigDecimal.ZERO.equals(r)) {
                throw new ArithmeticException("/");
            }
            return l.divide(r, getMathContext());
        }
        // if either are floating point (double or float), use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            final double l = toDouble(strictCast, left);
            final double r = toDouble(strictCast, right);
            if (r == 0.0) {
                throw new ArithmeticException("/");
            }
            return l / r;
        }
        // otherwise treat as BigInteger
        final BigInteger l = toBigInteger(strictCast, left);
        final BigInteger r = toBigInteger(strictCast, right);
        if (BigInteger.ZERO.equals(r)) {
            throw new ArithmeticException("/");
        }
        final BigInteger result = l.divide(r);
        return narrowBigInteger(left, right, result);
    }

    private int doCompare(final Object left, final Object right, final JexlOperator operator) {
        final boolean strictCast = isStrict(operator);
        if (left != null && right != null) {
            try {
                if (left instanceof BigDecimal || right instanceof BigDecimal) {
                    final BigDecimal l = toBigDecimal(strictCast, left);
                    final BigDecimal r = toBigDecimal(strictCast, right);
                    return l.compareTo(r);
                }
                if (left instanceof BigInteger || right instanceof BigInteger) {
                    final BigInteger l = toBigInteger(strictCast, left);
                    final BigInteger r = toBigInteger(strictCast, right);
                    return l.compareTo(r);
                }
                if (isFloatingPoint(left) || isFloatingPoint(right)) {
                    final double lhs = toDouble(strictCast, left);
                    final double rhs = toDouble(strictCast, right);
                    if (Double.isNaN(lhs)) {
                        if (Double.isNaN(rhs)) {
                            return 0;
                        }
                        return -1;
                    }
                    if (Double.isNaN(rhs)) {
                        // lhs is not NaN
                        return +1;
                    }
                    return Double.compare(lhs, rhs);
                }
                if (isNumberable(left) || isNumberable(right)) {
                    final long lhs = toLong(strictCast, left);
                    final long rhs = toLong(strictCast, right);
                    return Long.compare(lhs, rhs);
                }
                if (left instanceof String || right instanceof String) {
                    return toString(left).compareTo(toString(right));
                }
            } catch (final CoercionException ignore) {
                // ignore it, continue in sequence
            }
            if (EQ == operator) {
                return left.equals(right) ? 0 : -1;
            }
            if (left instanceof Comparable<?>) {
                @SuppressWarnings("unchecked") // OK because of instanceof check above
                final Comparable<Object> comparable = (Comparable<Object>) left;
                try {
                    return comparable.compareTo(right);
                } catch(final ClassCastException castException) {
                    // ignore it, continue in sequence
                }
            }
        }
        throw new ArithmeticException("Object comparison:(" + left + " " + operator + " " + right + ")");
    }

    /**
     * Check for emptiness of various types: Number, Collection, Array, Map, String.
     * <p>Override or overload this method to add new signatures to the size operators.
     * @param object the object to check the emptiness of
     * @return the boolean or false if object is not null
     * @since 3.2
     */
    public Boolean empty(final Object object) {
        return object == null || isEmpty(object, false);
    }

    /**
     * Test if left ends with right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left $= right if there is no arithmetic solution
     */
    public Boolean endsWith(final Object left, final Object right) {
        if (left == null && right == null) {
            //if both are null L == R
            return true;
        }
        if (left == null || right == null) {
            // we know both aren't null, therefore L != R
            return false;
        }
        if (left instanceof CharSequence) {
            return toString(left).endsWith(toString(right));
        }
        return null;
    }

    /**
     * Test if left and right are equal.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean equals(final Object left, final Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        final boolean strictCast = isStrict(EQ);
        if (left instanceof Boolean || right instanceof Boolean) {
            return toBoolean(left) == toBoolean(strictCast, right);
        }
        return compare(left, right, EQ) == 0;
    }

    /**
     * The MathContext instance used for +,-,/,*,% operations on big decimals.
     *
     * @return the math context
     */
    public MathContext getMathContext() {
        return mathContext;
    }

    /**
     * The BigDecimal scale used for comparison and coercion operations.
     *
     * @return the scale
     */
    public int getMathScale() {
        return mathScale;
    }

    /**
     * Test if left &gt; right.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean greaterThan(final Object left, final Object right) {
        if (left == right || left == null || right == null) {
            return false;
        }
        return compare(left, right, JexlOperator.GT) > 0;
    }

    /**
     * Test if left &gt;= right.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean greaterThanOrEqual(final Object left, final Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return compare(left, right, JexlOperator.GTE) >= 0;
    }

    /**
     * Increments argument by 1.
     * @param val the argument
     * @return val + 1
     */
    public Object increment(final Object val) {
        return increment(val, 1);
    }

    /**
     * Add value to number argument.
     * @param val the number
     * @param incr the value to add
     * @return val + incr
     */
    protected Object increment(final Object val, final int incr) {
        if (val == null) {
            return incr;
        }
        if (val instanceof Integer) {
            return (Integer) val + incr;
        }
        if (val instanceof Double) {
            return (Double) val + incr;
        }
        if (val instanceof Long) {
            return (Long) val + incr;
        }
        if (val instanceof BigDecimal) {
            final BigDecimal bd = (BigDecimal) val;
            return bd.add(BigDecimal.valueOf(incr), this.mathContext);
        }
        if (val instanceof BigInteger) {
            final BigInteger bi = (BigInteger) val;
            return bi.add(BigInteger.valueOf(incr));
        }
        if (val instanceof Float) {
            return (Float) val + incr;
        }
        if (val instanceof Short) {
            return (short) ((Short) val + incr);
        }
        if (val instanceof Byte) {
            return (byte) ((Byte) val + incr);
        }
        throw new ArithmeticException("Object "+(incr < 0? "decrement":"increment")+":(" + val + ")");
    }

    /**
     * Check for emptiness of various types: Number, Collection, Array, Map, String.
     *
     * @param object the object to check the emptiness of
     * @return the boolean or null if there is no arithmetic solution
     */
    public Boolean isEmpty(final Object object) {
        return isEmpty(object, object == null);
    }

    /**
     * Check for emptiness of various types: Number, Collection, Array, Map, String.
     *
     * @param object the object to check the emptiness of
     * @param def the default value if object emptyness can not be determined
     * @return the boolean or null if there is no arithmetic solution
     */
    public Boolean isEmpty(final Object object, final Boolean def) {
        if (object != null) {
            if (object instanceof Number) {
                final double d = ((Number) object).doubleValue();
                return Double.isNaN(d) || d == 0.d;
            }
            if (object instanceof CharSequence) {
                return ((CharSequence) object).length() == 0;
            }
            if (object.getClass().isArray()) {
                return Array.getLength(object) == 0;
            }
            if (object instanceof Collection<?>) {
                return ((Collection<?>) object).isEmpty();
            }
            // Map isn't a collection
            if (object instanceof Map<?, ?>) {
                return ((Map<?, ?>) object).isEmpty();
            }
        }
        return def;
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
     * Test if the passed value is a floating point number, i.e. a float, double
     * or string with ( "." | "E" | "e").
     *
     * @param val the object to be tested
     * @return true if it is, false otherwise.
     */
    protected boolean isFloatingPointNumber(final Object val) {
        if (val instanceof Float || val instanceof Double) {
            return true;
        }
        if (val instanceof CharSequence) {
            final Matcher m = FLOAT_PATTERN.matcher((CharSequence) val);
            // first group is decimal, second is exponent;
            // one of them must exist hence start({1,2}) >= 0
            return m.matches() && (m.start(1) >= 0 || m.start(2) >= 0);
        }
        return false;
    }

    /**
     * Whether negate called with a given argument will always return the same result.
     * <p>This is used to determine whether negate results on number literals can be cached.
     * If the result on calling negate with the same constant argument may change between calls,
     * which means the function is not deterministic, this method must return false.
     * @return true if negate is idempotent, false otherwise
     */
    public boolean isNegateStable() {
        return true;
    }

    /**
     * Checks if an operand is considered null.
     * @param value the operand
     * @return true if operand is considered null
     */
    protected boolean isNullOperand(final Object value) {
        return value == null;
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
     * Whether positivize called with a given argument will always return the same result.
     * <p>This is used to determine whether positivize results on number literals can be cached.
     * If the result on calling positivize with the same constant argument may change between calls,
     * which means the function is not deterministic, this method must return false.
     * @return true if positivize is idempotent, false otherwise
     */
    public boolean isPositivizeStable() {
        return true;
    }

    /**
     * Checks whether this JexlArithmetic instance
     * strictly considers null as an error when used as operand unexpectedly.
     *
     * @return true if strict, false if lenient
     */
    public boolean isStrict() {
        return strict;
    }

    /**
     * Checks whether this arithmetic considers a given operator as strict or null-safe.
     * <p>When an operator is strict, it does <em>not</em> accept null arguments when the arithmetic is strict.
     * If null-safe (ie not-strict), the operator does accept null arguments even if the arithmetic itself is strict.</p>
     * <p>The default implementation considers equal/not-equal operators as null-safe so one can check for null as in
     * <code>if (myvar == null) {...}</code>. Note that this operator is used for equal and not-equal syntax. The complete
     * list of operators that are not strict are (==, [], []=, ., .=, empty, size, contains).</p>
     * <p>An arithmetic refining its strict behavior handling for more operators must declare which by overriding
     * this method.</p>
     * @param operator the operator to check for null-argument(s) handling
     * @return true if operator considers null arguments as errors, false if operator has appropriate semantics
     * for null argument(s)
     */
    public boolean isStrict(final JexlOperator operator) {
        if (operator != null) {
            switch (operator) {
                case EQ:
                case EQSTRICT:
                case ARRAY_GET:
                case ARRAY_SET:
                case PROPERTY_GET:
                case PROPERTY_SET:
                case EMPTY:
                case SIZE:
                case CONTAINS:
                    return false;
                default:
                    return isStrict();
            }
        }
        return isStrict();
    }

    /**
     * Test if left &lt; right.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean lessThan(final Object left, final Object right) {
        if (left == right || left == null || right == null) {
            return false;
        }
        return compare(left, right, JexlOperator.LT) < 0;

    }

    /**
     * Test if left &lt;= right.
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean lessThanOrEqual(final Object left, final Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return compare(left, right, JexlOperator.LTE) <= 0;
    }

    /**
     * Use or overload not() instead.
     *
     * @param arg argument
     * @return !arg
     * @see JexlArithmetic#not
     * @deprecated 3.0
     */
    @Deprecated
    public final Object logicalNot(final Object arg) {
        return not(arg);
    }

    /**
     * Creates a map-builder.
     * @param size the number of elements in the map
     * @return a map-builder instance
     */
    @Deprecated
    public MapBuilder mapBuilder(final int size) {
        return mapBuilder(size, false);
    }

    /**
     * Called by the interpreter when evaluating a literal map.
     *
     * @param size the number of elements in the map
     * @param extended whether the map is extended or not
     * @return the map builder
     */
    public MapBuilder mapBuilder(final int size, final boolean extended) {
        return new org.apache.commons.jexl3.internal.MapBuilder(size, extended);
    }

    /**
     * Use or overload contains() instead.
     *
     * @param lhs left hand side
     * @param rhs right hand side
     * @return contains(rhs, lhs)
     * @see JexlArithmetic#contains
     * @deprecated 3.0
     */
    @Deprecated
    public final Object matches(final Object lhs, final Object rhs) {
        return contains(rhs, lhs);
    }

    /**
     * left value modulo right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left % right
     * @throws ArithmeticException if right == 0.0
     */
    public Object mod(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.MOD);
        }
        final boolean strictCast = isStrict(JexlOperator.MOD);
        // if both (non-null) args fit as long
        final Number ln = asLongNumber(strictCast, left);
        final Number rn = asLongNumber(strictCast, right);
        if (ln != null && rn != null) {
            final long x = ln.longValue();
            final long y = rn.longValue();
            if (y == 0L) {
                throw new ArithmeticException("%");
            }
            final long result = x % y;
            return narrowLong(left, right,  result);
        }
        // if either are BigDecimal, use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            final BigDecimal l = toBigDecimal(strictCast, left);
            final BigDecimal r = toBigDecimal(strictCast, right);
            if (BigDecimal.ZERO.equals(r)) {
                throw new ArithmeticException("%");
            }
            return l.remainder(r, getMathContext());
        }
        // if either are floating point (double or float), use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            final double l = toDouble(strictCast, left);
            final double r = toDouble(strictCast, right);
            if (r == 0.0) {
                throw new ArithmeticException("%");
            }
            return l % r;
        }
        // otherwise treat as BigInteger
        final BigInteger l = toBigInteger(strictCast, left);
        final BigInteger r = toBigInteger(strictCast, right);
        if (BigInteger.ZERO.equals(r)) {
            throw new ArithmeticException("%");
        }
        final BigInteger result = l.mod(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Multiply the left value by the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left * right.
     */
    public Object multiply(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.MULTIPLY);
        }
        final boolean strictCast = isStrict(JexlOperator.MULTIPLY);
        // if both (non-null) args fit as long
        final Number ln = asLongNumber(strictCast, left);
        final Number rn = asLongNumber(strictCast, right);
        if (ln != null && rn != null) {
            final long x = ln.longValue();
            final long y = rn.longValue();
            final long result = x * y;
            // detect overflow
            if (!isMultiplyExact(x, y, result)) {
                return BigInteger.valueOf(x).multiply(BigInteger.valueOf(y));
            }
            return narrowLong(left, right, result);
        }
        // if either are BigDecimal, use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            final BigDecimal l = toBigDecimal(strictCast, left);
            final BigDecimal r = toBigDecimal(strictCast, right);
            return l.multiply(r, getMathContext());
        }
        // if either are floating point (double or float), use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            final double l = toDouble(strictCast, left);
            final double r = toDouble(strictCast, right);
            return l * r;
        }
        // otherwise treat as BigInteger
        final BigInteger l = toBigInteger(strictCast, left);
        final BigInteger r = toBigInteger(strictCast, right);
        final BigInteger result = l.multiply(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Narrows a double to a float if there is no information loss.
     * @param value the double value
     * @param narrow the target narrow class
     * @return the narrowed or initial number
     */
    private Number narrow(final Class<?> narrow, final double value) {
        return narrowAccept(narrow, Float.class) && (float) value == value
            ? (float) value
            : value;
    }

    /**
     * Given a Number, return the value using the smallest type the result
     * will fit into.
     * <p>This works hand in hand with parameter 'widening' in Java
     * method calls, e.g. a call to substring(int,int) with an int and a long
     * will fail, but a call to substring(int,int) with an int and a short will
     * succeed.</p>
     *
     * @param original the original number.
     * @return a value of the smallest type the original number will fit into.
     */
    public Number narrow(final Number original) {
        return narrowNumber(original, null);
    }

    /**
     * Whether we consider the narrow class as a potential candidate for narrowing the source.
     *
     * @param narrow the target narrow class
     * @param source the original source class
     * @return true if attempt to narrow source to target is accepted
     */
    protected boolean narrowAccept(final Class<?> narrow, final Class<?> source) {
        return narrow == null || narrow.equals(source);
    }

    /**
     * Replace all numbers in an arguments array with the smallest type that will fit.
     *
     * @param args the argument array
     * @return true if some arguments were narrowed and args array is modified,
     *         false if no narrowing occurred and args array has not been modified
     */
    public boolean narrowArguments(final Object[] args) {
        boolean narrowed = false;
        if (args != null) {
            for (int a = 0; a < args.length; ++a) {
                final Object arg = args[a];
                if (arg instanceof Number) {
                    final Number narg = (Number) arg;
                    final Number narrow = narrow(narg);
                    if (!narg.equals(narrow)) {
                        args[a] = narrow;
                        narrowed = true;
                    }
                }
            }
        }
        return narrowed;
    }

    /**
     * Given a BigDecimal, attempt to narrow it to an Integer or Long if it fits and
     * one of the arguments is numberable.
     *
     * @param lhs  the left-hand side operand that lead to the bigd result
     * @param rhs  the right-hand side operand that lead to the bigd result
     * @param big the BigDecimal to narrow
     * @return an Integer or Long if narrowing is possible, the original BigDecimal otherwise
     */
    protected Number narrowBigDecimal(final Object lhs, final Object rhs, final BigDecimal big) {
        if (isNumberable(lhs) || isNumberable(rhs)) {
            try {
                final long l = big.longValueExact();
                // coerce to int when possible (int being so often used in method parms)
                if ((int) l == l) {
                    return (int) l;
                }
                return l;
            } catch (final ArithmeticException xa) {
                // ignore, no exact value possible
            }
        }
        return big;
    }

    /**
     * Given a BigInteger, narrow it to an Integer or Long if it fits and the arguments
     * class allow it.
     * <p>
     * The rules are:
     * if either arguments is a BigInteger, no narrowing will occur
     * if either arguments is a Long, no narrowing to Integer will occur
     * </p>
     *
     * @param lhs  the left-hand side operand that lead to the bigi result
     * @param rhs  the right-hand side operand that lead to the bigi result
     * @param big the BigInteger to narrow
     * @return an Integer or Long if narrowing is possible, the original BigInteger otherwise
     */
    protected Number narrowBigInteger(final Object lhs, final Object rhs, final BigInteger big) {
        if (isNumberable(lhs) || isNumberable(rhs)) {
            try {
                final long l = big.longValueExact();
                // coerce to int when possible (int being so often used in method parms)
                if ((int) l == l) {
                    return (int) l;
                }
                return l;
            } catch (final ArithmeticException xa) {
                // ignore, no exact value possible
            }
        }
        return big;
    }

    /**
     * Given a long, attempt to narrow it to an int.
     * <p>Narrowing will only occur if no operand is a Long.
     * @param lhs  the left hand side operand that lead to the long result
     * @param rhs  the right hand side operand that lead to the long result
     * @param r the long to narrow
     * @return an Integer if narrowing is possible, the original Long otherwise
     */
    protected Number narrowLong(final Object lhs, final Object rhs, final long r) {
        if (!(lhs instanceof Long || rhs instanceof Long) && (int) r == r) {
            return (int) r;
        }
        return r;
    }

    /**
     * Given a Number, return the value attempting to narrow it to a target class.
     *
     * @param original the original number
     * @param narrow   the attempted target class
     * @return the narrowed number or the source if no narrowing was possible
     */
    public Number narrowNumber(final Number original, final Class<?> narrow) {
        if (original != null) {
            final long value;
            if (original instanceof BigDecimal) {
                final BigDecimal big = (BigDecimal) original;
                try {
                    // can it be represented as a long?
                    value = big.longValueExact();
                    // continue in sequence to try and further reduce
                } catch (final ArithmeticException xa) {
                    // if it is bigger than a double, it can not be narrowed
                    if (big.compareTo(BIGD_DOUBLE_MAX_VALUE) > 0
                        || big.compareTo(BIGD_DOUBLE_MIN_VALUE) < 0) {
                        return original;
                    }
                    // represent as double
                    return narrow(narrow, original.doubleValue());
                }
                // this continues with value as long
            } else {
                if (isFloatingPoint(original)) {
                    final double doubleValue = original.doubleValue();
                    // if it is not equivalent to a Long...
                    if ((long) doubleValue != doubleValue) {
                        return narrow(narrow, doubleValue);
                    }
                    // else it can be represented as a long
                } else if (original instanceof BigInteger) {
                    final BigInteger bigi = (BigInteger) original;
                    // if it is bigger than a Long, it can not be narrowed
                    if (!BigInteger.valueOf(bigi.longValue()).equals(bigi)) {
                        return original;
                    }
                    // else it can be represented as a long
                }
                value = original.longValue();
            }
            // it can be represented as a long; determine the smallest possible numberable representation
            if (narrowAccept(narrow, Byte.class) && (byte) value == value) {
                // it will fit in a byte
                return (byte) value;
            }
            if (narrowAccept(narrow, Short.class) && (short) value == value) {
                return (short) value;
            }
            if (narrowAccept(narrow, Integer.class) && (int) value == value) {
                return (int) value;
            }
        }
        return original;
    }

    /**
     * Negates a value (unary minus for numbers).
     *
     * @see #isNegateStable()
     * @param val the value to negate
     * @return the negated value
     */
    public Object negate(final Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Integer) {
            return -((Integer) val);
        }
        if (val instanceof Double) {
            return - ((Double) val);
        }
        if (val instanceof Long) {
            return -((Long) val);
        }
        if (val instanceof BigDecimal) {
            return ((BigDecimal) val).negate();
        }
        if (val instanceof BigInteger) {
            return ((BigInteger) val).negate();
        }
        if (val instanceof Float) {
            return -((Float) val);
        }
        if (val instanceof Short) {
            return (short) -((Short) val);
        }
        if (val instanceof Byte) {
            return (byte) -((Byte) val);
        }
        if (val instanceof Boolean) {
            return !(Boolean) val;
        }
        if (val instanceof AtomicBoolean) {
            return !((AtomicBoolean) val).get();
        }
        throw new ArithmeticException("Object negate:(" + val + ")");
    }

    /**
     * Performs a logical not.
     *
     * @param val the operand
     * @return !val
     */
    public Object not(final Object val) {
        final boolean strictCast = isStrict(JexlOperator.NOT);
        return !toBoolean(strictCast, val);
    }

    /**
     * Apply options to this arithmetic which eventually may create another instance.
     * @see #createWithOptions(boolean, java.math.MathContext, int)
     *
     * @param context the context that may extend {@link JexlContext.OptionsHandle} to use
     * @return a new arithmetic instance or this
     * @since 3.1
     */
    public JexlArithmetic options(final JexlContext context) {
        if (context instanceof JexlContext.OptionsHandle) {
            return options(((JexlContext.OptionsHandle) context).getEngineOptions());
        }
        if (context instanceof JexlEngine.Options) {
            return options((JexlEngine.Options) context);
        }
        return this;
    }

    /**
     * Apply options to this arithmetic which eventually may create another instance.
     * @see #createWithOptions(boolean, java.math.MathContext, int)
     *
     * @param options the {@link JexlEngine.Options} to use
     * @return an arithmetic with those options set
     * @deprecated 3.2
     */
    @Deprecated
    public JexlArithmetic options(final JexlEngine.Options options) {
        if (options != null) {
            Boolean ostrict = options.isStrictArithmetic();
            if (ostrict == null) {
                ostrict = isStrict();
            }
            MathContext bigdContext = options.getArithmeticMathContext();
            if (bigdContext == null) {
                bigdContext = getMathContext();
            }
            int bigdScale = options.getArithmeticMathScale();
            if (bigdScale == Integer.MIN_VALUE) {
                bigdScale = getMathScale();
            }
            if (ostrict != isStrict()
                || bigdScale != getMathScale()
                || bigdContext != getMathContext()) {
                return createWithOptions(ostrict, bigdContext, bigdScale);
            }
        }
        return this;
    }

    /**
     * Apply options to this arithmetic which eventually may create another instance.
     * @see #createWithOptions(boolean, java.math.MathContext, int)
     *
     * @param options the {@link JexlEngine.Options} to use
     * @return an arithmetic with those options set
     */
    public JexlArithmetic options(final JexlOptions options) {
        if (options != null) {
            final boolean ostrict = options.isStrictArithmetic();
            MathContext bigdContext = options.getMathContext();
            if (bigdContext == null) {
                bigdContext = getMathContext();
            }
            int bigdScale = options.getMathScale();
            if (bigdScale == Integer.MIN_VALUE) {
                bigdScale = getMathScale();
            }
            if (ostrict != isStrict()
                || bigdScale != getMathScale()
                || bigdContext != getMathContext()) {
                return createWithOptions(ostrict, bigdContext, bigdScale);
            }
        }
        return this;
    }

    /**
     * Performs a bitwise or.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left | right
     */
    public Object or(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toLong(right);
        return l | r;
    }

    /**
     * Convert a string to a BigDecimal.
     * <>Empty string is considered as 0.</>
     * @param arg the arg
     * @return a BigDecimal
     * @throws CoercionException if the string can not be coerced into a BigDecimal
     */
    private BigDecimal parseBigDecimal(final String arg) throws ArithmeticException {
        try {
            return arg.isEmpty()? BigDecimal.ZERO : new BigDecimal(arg, getMathContext());
        } catch (final NumberFormatException e) {
            final ArithmeticException arithmeticException = new CoercionException("BigDecimal coercion: ("+ arg +")");
            arithmeticException.initCause(e);
            throw arithmeticException;
        }
    }

    /**
     * Converts a string to a big integer.
     * <>Empty string is considered as 0.</>
     * @param arg the arg
     * @return a big integer
     * @throws ArithmeticException if the string can not be coerced into a big integer
     */
    private BigInteger parseBigInteger(final String arg) throws ArithmeticException {
        try {
            return arg.isEmpty()? BigInteger.ZERO : new BigInteger(arg);
        } catch (final NumberFormatException e) {
            final ArithmeticException arithmeticException = new CoercionException("BigDecimal coercion: ("+ arg +")");
            arithmeticException.initCause(e);
            throw arithmeticException;
        }
    }

    /**
     * Convert a string to a double.
     * <>Empty string is considered as NaN.</>
     * @param arg the arg
     * @return a double
     * @throws ArithmeticException if the string can not be coerced into a double
     */
    private double parseDouble(final String arg) throws ArithmeticException {
        try {
            return arg.isEmpty()? Double.NaN : Double.parseDouble(arg);
        } catch (final NumberFormatException e) {
            final ArithmeticException arithmeticException = new CoercionException("Double coercion: ("+ arg +")");
            arithmeticException.initCause(e);
            throw arithmeticException;
        }
    }

    /**
     * Converts a string to an int.
     * <p>This ensure the represented number is a natural (not a real).</p>
     * @param arg the arg
     * @return an int
     * @throws ArithmeticException if the string can not be coerced into a long
     */
    private int parseInteger(final String arg) throws ArithmeticException {
        final long l = parseLong(arg);
        final int i = (int) l;
        if (i == l) {
            return i;
        }
        throw new CoercionException("Int coercion: ("+ arg +")");
    }

    /**
     * Converts a string to a long.
     * <p>This ensure the represented number is a natural (not a real).</p>
     * @param arg the arg
     * @return a long
     * @throws ArithmeticException if the string can not be coerced into a long
     */
    private long parseLong(final String arg) throws ArithmeticException {
        final double d = parseDouble(arg);
        if (Double.isNaN(d)) {
            return 0L;
        }
        final double f = floor(d);
        if (d == f) {
            return (long) d;
        }
        throw new CoercionException("Long coercion: ("+ arg +")");
    }

    /**
     * Parse an identifier which must be of the form:
     * 0|([1-9][0-9]*)
     * @param id the identifier
     * @return an integer or null
     */
    public static Integer parseIdentifier(final Object id) {
        if (id instanceof Number) {
            return ((Number) id).intValue();
        }
        // hand coded because the was no way to fail on leading '0's using NumberFormat
        if (id instanceof CharSequence) {
            final CharSequence str = (CharSequence) id;
            final int length = str.length();
            // can not be empty string and can not be longer than Integer.MAX_VALUE representation
            if (length > 0 && length <= 10) {
                int val = 0;
                for (int i = 0; i < length; ++i) {
                    final char c = str.charAt(i);
                    // leading 0s but no just 0, numeric only
                    if (c == '0' && val == 0 && length > 1 || c < '0' || c > '9') {
                        return null;
                    }
                    val *= 10;
                    val += c - '0';
                }
                return val;
            }
        }
        return null;
    }

    /**
     * Positivize value (unary plus for numbers).
     * <p>C/C++/C#/Java perform integral promotion of the operand, ie
     * cast to int if type can be represented as int without loss of precision.
     * @see #isPositivizeStable()
     * @param val the value to positivize
     * @return the positive value
     */
    public Object positivize(final Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Short) {
            return ((Short) val).intValue();
        }
        if (val instanceof Byte) {
            return ((Byte) val).intValue();
        }
        if (val instanceof Number) {
            return val;
        }
        if (val instanceof Character) {
            return (int) (Character) val;
        }
        if (val instanceof Boolean) {
            return val;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get();
        }
        throw new ArithmeticException("Object positivize:(" + val + ")");
    }

    /**
     * Ensure a big decimal is rounded by this arithmetic scale and rounding mode.
     *
     * @param number the big decimal to round
     * @return the rounded big decimal
     */
    protected BigDecimal roundBigDecimal(final BigDecimal number) {
        final int mscale = getMathScale();
        if (mscale >= 0) {
            return number.setScale(mscale, getMathContext().getRoundingMode());
        }
        return number;
    }

    /**
     * Creates a set-builder.
     * @param size the number of elements in the set
     * @return a set-builder instance
     * @deprecated since 3.3.1
     */
    @Deprecated
    public SetBuilder setBuilder(final int size) {
        return setBuilder(size, false);
    }

    /**
     * Called by the interpreter when evaluating a literal set.
     *
     * @param size the number of elements in the set
     * @param extended whether the set is extended or not
     * @return the array builder
     */
    public SetBuilder setBuilder(final int size, final boolean extended) {
        return new org.apache.commons.jexl3.internal.SetBuilder(size, extended);
    }

    /**
     * Shifts a bit pattern to the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &lt;&lt; right.
     */
    public Object shiftLeft(final Object left, final Object right) {
        final long l = toLong(left);
        final int r = toInteger(right);
        return l << r;
    }

    /**
     * Shifts a bit pattern to the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &gt;&gt; right.
     */
    public Object shiftRight(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toInteger(right);
        return l >> r;
    }

    /**
     * Shifts a bit pattern to the right unsigned.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &gt;&gt;&gt; right.
     */
    public Object shiftRightUnsigned(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toInteger(right);
        return l >>> r;
    }

    /**
     * Calculate the {@code size} of various types: Collection, Array, Map, String.
     *
     * @param object the object to get the size of
     * @return the <em>size</em> of object, 0 if null, 1 if there is no <em>better</em> solution
     */
    public Integer size(final Object object) {
        return size(object, object == null ? 0 : 1);
    }

    /**
     * Calculate the {@code size} of various types: Collection, Array, Map, String.
     *
     * @param object the object to get the size of
     * @param def the default value if object size can not be determined
     * @return the size of object or null if there is no arithmetic solution
     */
    public Integer size(final Object object, final Integer def) {
        if (object instanceof CharSequence) {
            return ((CharSequence) object).length();
        }
        if (object.getClass().isArray()) {
            return Array.getLength(object);
        }
        if (object instanceof Collection<?>) {
            return ((Collection<?>) object).size();
        }
        if (object instanceof Map<?, ?>) {
            return ((Map<?, ?>) object).size();
        }
        return def;
    }
    /**
     * Test if left starts with right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left ^= right or null if there is no arithmetic solution
     */
    public Boolean startsWith(final Object left, final Object right) {
        if (left == null && right == null) {
            //if both are null L == R
            return true;
        }
        if (left == null || right == null) {
            // we know both aren't null, therefore L != R
            return false;
        }
        if (left instanceof CharSequence) {
            return toString(left).startsWith(toString(right));
        }
        return null;
    }

    /**
     * Test if left and right are strictly equal.
     * <p>They must have the same class, comparable and the comparison returns 0.</p>
     *
     * @param left  left argument
     * @param right right argument
     * @return the test result
     */
    public boolean strictEquals(final Object left, final Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left.getClass().equals(right.getClass())) {
            return left.equals(right);
        }
        return false;
    }

    /**
     * Subtract the right value from the left.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left - right.
     */
    public Object subtract(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.SUBTRACT);
        }
        final boolean strictCast = isStrict(JexlOperator.SUBTRACT);
        // if both (non-null) args fit as long
        final Number ln = asLongNumber(strictCast, left);
        final Number rn = asLongNumber(strictCast, right);
        if (ln != null && rn != null) {
            final long x = ln.longValue();
            final long y = rn.longValue();
            final long result = x - y;
            // detect overflow, see java8 Math.subtractExact
            if (((x ^ y) & (x ^ result)) < 0) {
                return BigInteger.valueOf(x).subtract(BigInteger.valueOf(y));
            }
            return narrowLong(left, right, result);
        }
        // if either are BigDecimal, use that type
        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            final BigDecimal l = toBigDecimal(strictCast, left);
            final BigDecimal r = toBigDecimal(strictCast, right);
            return l.subtract(r, getMathContext());
        }
        // if either are floating point (double or float), use double
        if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
            final double l = toDouble(strictCast, left);
            final double r = toDouble(strictCast, right);
            return l - r;
        }
        // otherwise treat as BigInteger
        final BigInteger l = toBigInteger(strictCast, left);
        final BigInteger r = toBigInteger(strictCast, right);
        final BigInteger result = l.subtract(r);
        return narrowBigInteger(left, right, result);
    }

    /**
     * Test if a condition is true or false.
     * @param object the object to use as condition
     * @return true or false
     * @since 3.3
     */
    public boolean testPredicate(final Object object) {
        final boolean strictCast = isStrict(JexlOperator.CONDITION);
        return toBoolean(strictCast, object);
    }

    /**
     * Coerce to a BigDecimal.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val the object to be coerced.
     * @return a BigDecimal.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected BigDecimal toBigDecimal(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, BigDecimal.ZERO) : toBigDecimal(val);
    }

    /**
     * Coerce to a BigDecimal.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val the object to be coerced.
     * @return a BigDecimal.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public BigDecimal toBigDecimal(final Object val) {
        if (val instanceof BigDecimal) {
            return roundBigDecimal((BigDecimal) val);
        }
        if (val instanceof Double) {
            if (Double.isNaN((Double) val)) {
                return BigDecimal.ZERO;
            }
            return roundBigDecimal(new BigDecimal(val.toString(), getMathContext()));
        }
        if (val instanceof Number) {
            return roundBigDecimal(parseBigDecimal(val.toString()));
        }
        if (val instanceof Boolean) {
            return BigDecimal.valueOf((Boolean) val ? 1. : 0.);
        }
        if (val instanceof AtomicBoolean) {
            return BigDecimal.valueOf(((AtomicBoolean) val).get() ? 1L : 0L);
        }
        if (val instanceof String) {
            return roundBigDecimal(parseBigDecimal((String) val));
        }
        if (val instanceof Character) {
            return new BigDecimal((Character) val);
        }
        if (val == null) {
            return controlNullOperand(strict, BigDecimal.ZERO);
        }
        throw new CoercionException("BigDecimal coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a BigInteger.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val the object to be coerced.
     * @return a BigDecimal
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected BigInteger toBigInteger(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, BigInteger.ZERO) : toBigInteger(val);
    }

    /**
     * Coerce to a BigInteger.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val the object to be coerced.
     * @return a BigDecimal
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public BigInteger toBigInteger(final Object val) {
        if (val instanceof BigInteger) {
            return (BigInteger) val;
        }
        if (val instanceof Double) {
            final Double dval = (Double) val;
            if (Double.isNaN(dval)) {
                return BigInteger.ZERO;
            }
            return BigInteger.valueOf(dval.longValue());
        }
        if (val instanceof BigDecimal) {
            return ((BigDecimal) val).toBigInteger();
        }
        if (val instanceof Number) {
            return BigInteger.valueOf(((Number) val).longValue());
        }
        if (val instanceof Boolean) {
            return BigInteger.valueOf((Boolean) val ? 1L : 0L);
        }
        if (val instanceof AtomicBoolean) {
            return BigInteger.valueOf(((AtomicBoolean) val).get() ? 1L : 0L);
        }
        if (val instanceof String) {
            return parseBigInteger((String) val);
        }
        if (val instanceof Character) {
            final int i = (Character) val;
            return BigInteger.valueOf(i);
        }
        if (val == null) {
            return controlNullOperand(strict, BigInteger.ZERO);
        }
        throw new CoercionException("BigInteger coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive boolean.
     * <p>Double.NaN, null, "false" and empty string coerce to false.</p>
     *
     * @param val value to coerce
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @return the boolean value if coercion is possible, true if value was not null.
     */
    protected boolean toBoolean(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, false) : toBoolean(val);
    }

    /**
     * Coerce to a primitive boolean.
     * <p>Double.NaN, null, "false" and empty string coerce to false.</p>
     *
     * @param val value to coerce
     * @return the boolean value if coercion is possible, true if value was not null.
     */
    public boolean toBoolean(final Object val) {
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        if (val instanceof Number) {
            final double number = toDouble(strict, val);
            return !Double.isNaN(number) && number != 0.d;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get();
        }
        if (val instanceof String) {
            final String strval = val.toString();
            return !strval.isEmpty() && !"false".equals(strval);
        }
        if (val == null) {
            return controlNullOperand(strict, false);
        }
        // non-null value is true
        return true;
    }

    /**
     * Coerce to a primitive double.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val value to coerce.
     * @return The double coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected double toDouble(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, 0.d) : toDouble(val);
    }

    /**
     * Coerce to a primitive double.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce.
     * @return The double coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public double toDouble(final Object val) {
        if (val instanceof Double) {
            return (Double) val;
        }
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof Boolean) {
            return (Boolean) val ? 1. : 0.;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? 1. : 0.;
        }
        if (val instanceof String) {
            return parseDouble((String) val);
        }
        if (val instanceof Character) {
            return (Character) val;
        }
        if (val == null) {
            return controlNullOperand(strict, 0.d);
        }
        throw new CoercionException("Double coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive int.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val value to coerce
     * @return the value coerced to int
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected int toInteger(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, 0) : toInteger(val);
    }

    /**
     * Coerce to a primitive int.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce
     * @return the value coerced to int
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public int toInteger(final Object val) {
        if (val instanceof Double) {
            final double dval = (Double) val;
            return Double.isNaN(dval)? 0 : (int) dval;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            return parseInteger((String) val);
        }
        if (val instanceof Boolean) {
            return (Boolean) val ? 1 : 0;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? 1 : 0;
        }
        if (val instanceof Character) {
            return (Character) val;
        }
        if (val == null) {
            return controlNullOperand(strict, 0);
        }
        throw new CoercionException("Integer coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a primitive long.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val value to coerce
     * @return the value coerced to long
     * @throws ArithmeticException if value is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected long toLong(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, 0L) : toLong(val);
    }

    /**
     * Coerce to a primitive long.
     * <p>Double.NaN, null and empty string coerce to zero.</p>
     * <p>Boolean false is 0, true is 1.</p>
     *
     * @param val value to coerce
     * @return the value coerced to long
     * @throws ArithmeticException if value is null and mode is strict or if coercion is not possible
     */
    public long toLong(final Object val) {
        if (val instanceof Double) {
            final double dval = (Double) val;
            return Double.isNaN(dval)? 0L : (long) dval;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val instanceof String) {
            return parseLong((String) val);
        }
        if (val instanceof Boolean) {
            return (Boolean) val ? 1L : 0L;
        }
        if (val instanceof AtomicBoolean) {
            return ((AtomicBoolean) val).get() ? 1L : 0L;
        }
        if (val instanceof Character) {
            return (Character) val;
        }
        if (val == null) {
            return controlNullOperand(strict, 0L);
        }
        throw new CoercionException("Long coercion: "
                + val.getClass().getName() + ":(" + val + ")");
    }

    /**
     * Coerce to a string.
     * <p>Double.NaN coerce to the empty string.</p>
     *
     * @param strict true if the calling operator or casting is strict, false otherwise
     * @param val value to coerce.
     * @return The String coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     * @since 3.3
     */
    protected String toString(final boolean strict, final Object val) {
        return isNullOperand(val)? controlNullOperand(strict, "") : toString(val);
    }

    /**
     * Coerce to a string.
     * <p>Double.NaN coerce to the empty string.</p>
     *
     * @param val value to coerce.
     * @return The String coerced value.
     * @throws ArithmeticException if val is null and mode is strict or if coercion is not possible
     */
    public String toString(final Object val) {
        if (val instanceof Double) {
            final Double dval = (Double) val;
            if (Double.isNaN(dval)) {
                return "";
            }
            return dval.toString();
        }
        return val == null ? controlNullOperand(strict, "") : val.toString();
    }

    /**
     * Performs a bitwise xor.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left ^ right
     */
    public Object xor(final Object left, final Object right) {
        final long l = toLong(left);
        final long r = toLong(right);
        return l ^ r;
    }
}
