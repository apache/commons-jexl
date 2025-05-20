/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jexl3;

import java.math.BigInteger;
import java.math.MathContext;

/**
 * An arithmetic that tries to keep argument types for bit-twiddling operators.
 */
public class Arithmetic360 extends JexlArithmetic {
    public Arithmetic360(final boolean astrict) {
        super(astrict);
    }

    public Arithmetic360(final boolean astrict, final MathContext bigdContext, final int bigdScale) {
        super(astrict, bigdContext, bigdScale);
    }

    /**
     * Performs a bitwise and.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left &amp; right
     */
    @Override
    public Object and(final Object left, final Object right) {
        final Number l = asLongNumber(left);
        final Number r = asLongNumber(right);
        if (l != null && r != null) {
            return narrowLong(left, right, l.longValue() & r.longValue());
        }
        return toBigInteger(left).and(toBigInteger(right));
    }

    /**
     * Checks if value class is a number that can be represented exactly in a long.
     *
     * @param value  argument
     * @return true if argument can be represented by a long
     */
    protected Number asIntNumber(final Object value) {
        return value instanceof Integer
                || value instanceof Short
                || value instanceof Byte
                ? (Number) value
                : null;
    }
    /**
     * Casts to Long if possible.
     * @param value the Long or else
     * @return the Long or null
     */
    protected Long castLongNumber(final Object value) {
        return value instanceof Long ? (Long) value : null;
    }

    /**
     * Performs a bitwise complement.
     *
     * @param val the operand
     * @return ~val
     */
    @Override
    public Object complement(final Object val) {
        final long l = toLong(val);
        return narrowLong(val, ~l);
    }

    /**
     * Given a long, attempt to narrow it to an int.
     * <p>Narrowing will only occur if the initial operand is not a Long.
     * @param operand  the operand that lead to the long result
     * @param result the long result to narrow
     * @return an Integer if narrowing is possible, the original Long otherwise
     */
    protected Number narrowLong(final Object operand, final long result) {
        if (!(operand instanceof Long)) {
            final int ir = (int) result;
            if (result == ir) {
                return ir;
            }
        }
        return result;
    }

    /**
     * Given a long, attempt to narrow it to an int.
     * <p>Narrowing will only occur if no operand is a Long.
     * @param lhs  the left-hand side operand that lead to the long result
     * @param rhs  the right-hand side operand that lead to the long result
     * @param result the long to narrow
     * @return an Integer if narrowing is possible, the original Long otherwise
     */
    @Override
    protected Number narrowLong(final Object lhs, final Object rhs, final long result) {
        if (!(lhs instanceof Long || rhs instanceof Long)) {
            final int ir = (int) result;
            if (result == ir) {
                return ir;
            }
        }
        return result;
    }

    /**
     * Performs a bitwise or.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left | right
     */
    @Override
    public Object or(final Object left, final Object right) {
        final Number l = asLongNumber(left);
        final Number r = asLongNumber(right);
        if (l != null && r != null) {
            return narrowLong(left, right, l.longValue() | r.longValue());
        }
        return toBigInteger(left).or(toBigInteger(right));
    }

    /**
     * Shifts a bit pattern to the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &lt;&lt; right.
     */
    @Override
    public Object shiftLeft(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.SHIFTLEFT);
        }
        final int r = toInteger(right);
        Number l = asIntNumber(left);
        if (l != null) {
            return l.intValue() << r;
        }
        l = castLongNumber(left);
        if (l != null) {
            return l.longValue() << r;
        }
        return toBigInteger(left).shiftLeft(r);
    }

    /**
     * Shifts a bit pattern to the right.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &gt;&gt; right.
     */
    @Override
    public Object shiftRight(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.SHIFTRIGHT);
        }
        final int r = toInteger(right);
        Number l = asIntNumber(left);
        if (l != null) {
            return l.intValue() >> r;
        }
        l = castLongNumber(left);
        if (l != null) {
            return l.longValue() >> r;
        }
        return toBigInteger(left).shiftRight(r);
    }

    /**
     * Shifts a bit pattern to the right unsigned.
     *
     * @param left  left argument
     * @param right  right argument
     * @return left &gt;&gt;&gt; right.
     */
    @Override
    public Object shiftRightUnsigned(final Object left, final Object right) {
        if (left == null && right == null) {
            return controlNullNullOperands(JexlOperator.SHIFTRIGHTU);
        }
        final int r = toInteger(right);
        Number l = asIntNumber(left);
        if (l != null) {
            return l.intValue() >>> r;
        }
        l = castLongNumber(left);
        if (l != null) {
            return l.longValue() >>> r;
        }
        final BigInteger bl = toBigInteger(left);
        return bl.signum() < 0? bl.negate().shiftRight(r) : bl.shiftRight(r);
    }

    /**
     * Performs a bitwise xor.
     *
     * @param left  the left operand
     * @param right the right operator
     * @return left ^ right
     */
    @Override
    public Object xor(final Object left, final Object right) {
        final Number l = asLongNumber(left);
        final Number r = asLongNumber(right);
        if (l != null && r != null) {
            return narrowLong(left, right, l.longValue() ^ r.longValue());
        }
        return toBigInteger(left).xor(toBigInteger(right));
    }

}
