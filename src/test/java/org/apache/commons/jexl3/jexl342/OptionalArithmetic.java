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
package org.apache.commons.jexl3.jexl342;

import org.apache.commons.jexl3.JexlArithmetic;

import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unwraps Optional/Reference/AtomicReference on public and key methods.
 */
public class OptionalArithmetic extends JexlArithmetic {
    public OptionalArithmetic(boolean astrict) {
        super(astrict);
    }

    /**
     * Dereferences an Optional, a Reference or an AtomicReference, leave other as is.
     * @param ref the reference
     * @return the referenced object
     */
    protected Object star(Object ref) {
        if (ref instanceof Optional<?>) {
            Optional<?> o = (Optional<?>) ref;
            return o.orElse(null);
        }
        if (ref instanceof Reference<?>) {
            Optional<?> r = (Optional<?>) ref;
            return r.get();
        }
        if (ref instanceof AtomicReference<?>) {
            AtomicReference<?> r = (AtomicReference<?>) ref;
            return r.get();
        }
        return ref;
    }

    @Override
    public Object controlReturn(Object returned) {
        return star(returned);
    }

    @Override
    public boolean toBoolean(final Object val) {
        return super.toBoolean(star(val));
    }

    @Override
    public String toString(final Object val) {
        return super.toString(star(val));
    }

    @Override
    public int toInteger(final Object val) {
        return super.toInteger(star(val));
    }

    @Override
    public long toLong(final Object val) {
        return super.toLong(star(val));
    }

    @Override
    public double toDouble(final Object val) {
        return super.toDouble(star(val));
    }

    @Override
    public BigInteger toBigInteger(final Object val) {
        return super.toBigInteger(star(val));
    }

    @Override
    public BigDecimal toBigDecimal(final Object val) {
        return super.toBigDecimal(star(val));
    }

    @Override
    public Integer size(final Object object, final Integer def) {
        return super.size(star(object), def);
    }

    @Override
    public Boolean empty(Object o) {
        return super.empty(star(o));
    }

    @Override
    public Boolean isEmpty(final Object object, final Boolean def) {
        return super.isEmpty(star(object), def);
    }

    @Override
    public Object positivize(Object o) {
        return super.positivize(star(o));
    }

    @Override
    public Object negate(Object o) {
        return super.negate(star(o));
    }

    @Override
    public Object complement(Object o) {
        return super.complement(star(o));
    }

    @Override
    public Boolean contains(Object lhs, Object rhs) {
        return super.contains(star(lhs), star(rhs));
    }

    @Override
    public Object add(Object lhs, Object rhs) {
        return super.add(star(lhs), star(rhs));
    }

    @Override
    public Object subtract(Object lhs, Object rhs) {
        return super.subtract(star(lhs), star(rhs));
    }

    @Override
    public Object multiply(Object lhs, Object rhs) {
        return super.multiply(star(lhs), star(rhs));
    }

    @Override
    public Object divide(Object lhs, Object rhs) {
        return super.divide(star(lhs), star(rhs));
    }

    @Override
    public Object mod(Object lhs, Object rhs) {
        return super.mod(star(lhs), star(rhs));
    }

    @Override
    public Object and(final Object left, final Object right) { return super.and(star(left), star(right)); }

    @Override
    public Object or(final Object left, final Object right) { return super.or(star(left), star(right)); }

    @Override
    public Object xor(final Object left, final Object right) { return super.xor(star(left), star(right)); }

    @Override
    public Object shiftLeft(final Object left, final Object right) { return super.shiftLeft(star(left), star(right)); }

    @Override
    public Object shiftRight(final Object left, final Object right) { return super.shiftRight(star(left), star(right)); }

    @Override
    public Object shiftRightUnsigned(final Object left, final Object right) { return super.shiftRightUnsigned(star(left), star(right)); }

    @Override
    public Boolean startsWith(final Object left, final Object right) {
        return super.startsWith(star(left), star(right));
    }

    @Override
    public Boolean endsWith(final Object left, final Object right) {
        return super.endsWith(star(left), star(right));
    }

    @Override
    public boolean equals(final Object left, final Object right) {
        return equals(star(left), star(right));
    }

    @Override
    public boolean greaterThan(final Object left, final Object right) {
        return greaterThan(star(left), star(right));
    }

    @Override
    public boolean greaterThanOrEqual(final Object left, final Object right) {
        return greaterThanOrEqual(star(left), star(right));
    }

    @Override
    public boolean lessThan(final Object left, final Object right) {
        return lessThan(star(left), star(right));
    }

    @Override
    public boolean lessThanOrEqual(final Object left, final Object right) {
        return lessThanOrEqual(star(left), star(right));
    }

    @Override
    public boolean narrowArguments(final Object[] args) {
        boolean narrowed = false;
        if (args != null) {
            for (int a = 0; a < args.length; ++a) {
                final Object arg = args[a];
                Object sarg = star(arg);
                if (sarg != arg) {
                    narrowed = true;
                }
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
}
