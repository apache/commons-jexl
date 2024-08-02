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
package org.apache.commons.jexl3.parser;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Parses number literals.
 */
public final class NumberParser implements Serializable {
    /**
     */
    private static final long serialVersionUID = 1L;
    /** JEXL locale-neutral big decimal format. */
    static final DecimalFormat BIGDF = new DecimalFormat("0.0b", new DecimalFormatSymbols(Locale.ROOT));
    private static boolean isNegative(final Token token) {
        return token != null && "-".equals(token.image);
    }
    static Number parseDouble(final Token negative, final Token s) {
        return new NumberParser().assignReal(isNegative(negative), s.image).getLiteralValue();
    }

    static Number parseInteger(final Token negative, final Token s) {
        return new NumberParser().assignNatural(isNegative(negative), s.image).getLiteralValue();
    }

    /** The type literal value. */
    private Number literal;

    /** The expected class. */
    private Class<? extends Number> clazz;

    /**
     * Sets this node as a natural literal.
     * Originally from OGNL.
     * @param negative whether the natural should be negative
     * @param natural the natural as string
     * @return this parser instance
     */
    NumberParser assignNatural(final boolean negative, final String natural) {
        String s = natural;
        Number result;
        Class<? extends Number> rclass;
        // determine the base
        final int base;
        if (s.charAt(0) == '0') {
            if (s.length() > 1 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                base = 16;
                s = s.substring(2); // Trim the 0x off the front
            } else {
                base = 8;
            }
        } else {
            base = 10;
        }
        // switch on suffix if any
        final int last = s.length() - 1;
        switch (s.charAt(last)) {
            case 'l':
            case 'L': {
                rclass = Long.class;
                final long l = Long.parseLong(s.substring(0, last), base);
                result = negative? -l : l;
                break;
            }
            case 'h':
            case 'H': {
                rclass = BigInteger.class;
                final BigInteger bi = new BigInteger(s.substring(0, last), base);
                result = negative? bi.negate() : bi;
                break;
            }
            default: {
                // preferred literal class is integer
                rclass = Integer.class;
                try {
                    final int i = Integer.parseInt(s, base);
                    result = negative? -i : i;
                } catch (final NumberFormatException take2) {
                    try {
                        final long l = Long.parseLong(s, base);
                        result = negative? -l : l;
                    } catch (final NumberFormatException take3) {
                        final BigInteger bi = new BigInteger(s, base);
                        result = negative? bi.negate() : bi;
                    }
                }
            }
        }
        literal = result;
        clazz = rclass;
        return this;
    }

    /**
     * Sets this node as an (optionally) signed natural literal.
     * Originally from OGNL.
     * @param str the natural as string
     * @return this parser instance
     */
    NumberParser assignNatural(final String str) {
        String s;
        // determine negative sign if any, ignore +
        final boolean negative;
        switch (str.charAt(0)) {
            case '-':
                negative = true;
                s = str.substring(1);
                break;
            case '+':
                negative = false;
                s = str.substring(1);
                break;
            default:
                negative = false;
                s = str;
        }
        return assignNatural(negative, s);
    }

    /**
     * Sets this node as a real literal.
     * Originally from OGNL.
     * @param negative whether the real should be negative
     * @param s the real as string
     * @return this parser instance
     */
    NumberParser assignReal(final boolean negative, final String s) {
        Number result;
        Class<? extends Number> rclass;
        if ("#NaN".equals(s) || "NaN".equals(s)) {
            result = Double.NaN;
            rclass = Double.class;
        } else {
            final int last = s.length() - 1;
            switch (s.charAt(last)) {
                case 'b':
                case 'B': {
                    rclass = BigDecimal.class;
                    final BigDecimal bd = new BigDecimal(s.substring(0, last));
                    result = negative? bd.negate() : bd;
                    break;
                }
                case 'f':
                case 'F': {
                    rclass = Float.class;
                    final float f4 = Float.parseFloat(s.substring(0, last));
                    result = negative? -f4 : f4;
                    break;
                }
                case 'd':
                case 'D':
                    rclass = Double.class;
                    final double f8 = Double.parseDouble(s.substring(0, last));
                    result = negative? -f8 : f8;
                    break;
                default: {
                    // preferred literal class is double
                    rclass = Double.class;
                    try {
                        final double d = Double.parseDouble(s);
                        result = negative? -d : d;
                    } catch (final NumberFormatException take3) {
                        final BigDecimal bd = new BigDecimal(s);
                        result = negative? bd.negate() : bd;
                    }
                    break;
                }
            }
        }
        literal = result;
        clazz = rclass;
        return this;
    }

    /**
     * Sets this node as an (optionally) signed real literal.
     * Originally from OGNL.
     * @param str the real as string
     * @return this parser instance
     */
    NumberParser assignReal(final String str) {
        String s;
        // determine negative sign if any, ignore +
        final boolean negative;
        switch (str.charAt(0)) {
            case '-':
                negative = true;
                s = str.substring(1);
                break;
            case '+':
                negative = false;
                s = str.substring(1);
                break;
            default:
                negative = false;
                s = str;
        }
        return assignReal(negative, s);
    }

    Class<? extends Number> getLiteralClass() {
        return clazz;
    }

    Number getLiteralValue() {
        return literal;
    }

    boolean isInteger() {
        return Integer.class.equals(clazz);
    }

    @Override
    public String toString() {
        if (literal == null || clazz == null || Double.isNaN(literal.doubleValue())) {
            return "NaN";
        }
        if (BigDecimal.class.equals(clazz)) {
            synchronized (BIGDF) {
                return BIGDF.format(literal);
            }
        }
        final StringBuilder strb = new StringBuilder(literal.toString());
        if (Float.class.equals(clazz)) {
            strb.append('f');
        } else if (Double.class.equals(clazz)) {
            strb.append('d');
        } else if (BigInteger.class.equals(clazz)) {
            strb.append('h');
        } else if (Long.class.equals(clazz)) {
            strb.append('l');
        }
        return strb.toString();
    }

}
