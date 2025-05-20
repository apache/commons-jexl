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
package org.apache.commons.jexl3.jexl342;

import java.lang.ref.Reference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.jexl3.JexlArithmetic;

/**
 * Unwraps Optional/Reference/AtomicReference on public and key methods.
 */
public class OptionalArithmetic extends JexlArithmetic {
    public OptionalArithmetic(final boolean astrict) {
        super(astrict);
    }

    @Override
    public Object add(final Object lhs, final Object rhs) {
        return super.add(star(lhs), star(rhs));
    }

    @Override
    public Object and(final Object left, final Object right) { return super.and(star(left), star(right)); }
    @Override
    public ArrayBuilder arrayBuilder(final int size, final boolean extended) {
        return new org.apache.commons.jexl3.internal.ArrayBuilder(size, extended) {
            @Override
            public void add(final Object value) {
                super.add(star(value));
            }
        };
    }

    @Override
    public Object complement(final Object o) {
        return super.complement(star(o));
    }

    @Override
    public Boolean contains(final Object lhs, final Object rhs) {
        return super.contains(star(lhs), star(rhs));
    }

    @Override
    public Object controlReturn(final Object returned) {
        return star(returned);
    }

    @Override
    public Object divide(final Object lhs, final Object rhs) {
        return super.divide(star(lhs), star(rhs));
    }

    @Override
    public Boolean empty(final Object o) {
        return super.empty(star(o));
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
    public Boolean isEmpty(final Object object, final Boolean def) {
        return super.isEmpty(star(object), def);
    }

    @Override
    protected boolean isNullOperand(final Object val) {
        return super.isNullOperand(star(val));
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
    public MapBuilder mapBuilder(final int size, final boolean extended) {
        return new org.apache.commons.jexl3.internal.MapBuilder(size, extended) {
            @Override
            public void put(final Object key, final Object value) {
                super.put(key, star(value));
            }
        };
    }

    @Override
    public Object mod(final Object lhs, final Object rhs) {
        return super.mod(star(lhs), star(rhs));
    }

    @Override
    public Object multiply(final Object lhs, final Object rhs) {
        return super.multiply(star(lhs), star(rhs));
    }

    @Override
    public boolean narrowArguments(final Object[] args) {
        boolean narrowed = false;
        if (args != null) {
            for (int a = 0; a < args.length; ++a) {
                final Object arg = args[a];
                final Object sarg = star(arg);
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

    @Override
    public Object negate(final Object o) {
        return super.negate(star(o));
    }

    @Override
    public Object or(final Object left, final Object right) { return super.or(star(left), star(right)); }

    @Override
    public Object positivize(final Object o) {
        return super.positivize(star(o));
    }

    @Override
    public SetBuilder setBuilder(final int size, final boolean extended) {
        return new org.apache.commons.jexl3.internal.SetBuilder(size, extended) {
            @Override
            public void add(final Object value) {
                super.add(star(value));
            }
        };
    }

    @Override
    public Object shiftLeft(final Object left, final Object right) { return super.shiftLeft(star(left), star(right)); }

    @Override
    public Object shiftRight(final Object left, final Object right) { return super.shiftRight(star(left), star(right)); }

    @Override
    public Object shiftRightUnsigned(final Object left, final Object right) { return super.shiftRightUnsigned(star(left), star(right)); }

    @Override
    public Integer size(final Object object, final Integer def) {
        return super.size(star(object), def);
    }

    /**
     * Dereferences an Optional, a Reference or an AtomicReference, leave other as is.
     * @param ref the reference
     * @return the referenced object
     */
    protected Object star(final Object ref) {
        if (ref instanceof Optional<?>) {
            final Optional<?> o = (Optional<?>) ref;
            return o.orElse(null);
        }
        if (ref instanceof Reference<?>) {
            final Optional<?> r = (Optional<?>) ref;
            return r.get();
        }
        if (ref instanceof AtomicReference<?>) {
            final AtomicReference<?> r = (AtomicReference<?>) ref;
            return r.get();
        }
        return ref;
    }

    @Override
    public Boolean startsWith(final Object left, final Object right) {
        return super.startsWith(star(left), star(right));
    }

    @Override
    public Object subtract(final Object lhs, final Object rhs) {
        return super.subtract(star(lhs), star(rhs));
    }

    @Override
    public BigDecimal toBigDecimal(final Object val) {
        return super.toBigDecimal(star(val));
    }

    @Override
    public BigInteger toBigInteger(final Object val) {
        return super.toBigInteger(star(val));
    }

    @Override
    public boolean toBoolean(final Object val) {
        return super.toBoolean(star(val));
    }

    @Override
    public double toDouble(final Object val) {
        return super.toDouble(star(val));
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
    public String toString(final Object val) {
        return super.toString(star(val));
    }

    @Override
    public Object xor(final Object left, final Object right) { return super.xor(star(left), star(right)); }
}
