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
package org.apache.commons.jexl3.internal;

import java.util.BitSet;

/**
 * The set of symbols declared in a lexical scope.
 * <p>The symbol identifiers are determined by the functional scope.</p>
 * <p>We use 2 bits per symbol s; bit (s*2)+0 sets the actual symbol as lexical (let), bit (s*2)+1 as a const.
 * There are actually only 2 used states: 1 and 3</p>
 */
public class LexicalScope {
    /**
     * Number of bits in a long.
     */
    protected static final int BITS_PER_LONG = 64;
    /**
     * Bits per symbol.
     * let (b + 0) + const (b + 1).
     */
    protected static final int BITS_PER_SYMBOL = 2;
    /**
     * From a symbol number to a starting symbol bit number.
     */
    protected static final int SYMBOL_SHIFT = BITS_PER_SYMBOL - 1;
    /**
     * Bitmask for symbols.
     */
    protected static final long SYMBOL_MASK = (1L << BITS_PER_SYMBOL - 1) - 1; // 3, as 1+2, 2 bits
    /**
     * Number of symbols.
     */
    protected int count;
    /**
     * The mask of symbols in the scope.
     */
    protected long symbols;
    /**
     * Symbols after bit 64 (aka symbol 32 when 2 bits per symbol).
     */
    protected BitSet moreSymbols;

    /**
     * Create a scope.
     */
    public LexicalScope() {
    }

    /**
     * Frame copy ctor base.
     */
    protected LexicalScope(final LexicalScope other) {
        this.symbols = other.symbols;
        final BitSet otherMoreSymbols = other.moreSymbols;
        this.moreSymbols = otherMoreSymbols != null ? (BitSet) otherMoreSymbols.clone() : null;
        this.count = other.count;
    }

    /**
     * Adds a constant in this scope.
     *
     * @param symbol the symbol
     * @return true if registered, false if symbol was already registered
     */
    public boolean addConstant(final int symbol) {
        final int letb = symbol << SYMBOL_SHIFT ;
        if (!isSet(letb)) {
            throw new IllegalStateException("const not declared as symbol " + symbol);
        }
        final int bit = symbol << SYMBOL_SHIFT | 1;
        return set(bit);
    }

    /**
     * Adds a symbol in this scope.
     *
     * @param symbol the symbol
     * @return true if registered, false if symbol was already registered
     */
    public boolean addSymbol(final int symbol) {
        final int bit = symbol << SYMBOL_SHIFT ;
        if (set(bit)) {
            count += 1;
            return true;
        }
        return false;
    }

    /**
     * Clear all symbols.
     *
     * @param cleanSymbol a (optional, may be null) functor to call for each cleaned symbol
     */
    public final void clearSymbols(final java.util.function.IntConsumer cleanSymbol) {
        // undefine symbols getting out of scope
        if (cleanSymbol != null) {
            long clean = symbols;
            while (clean != 0L) {
                final int bit = Long.numberOfTrailingZeros(clean);
                final int s = bit >> SYMBOL_SHIFT;
                cleanSymbol.accept(s);
                // call clean for symbol definition (3 as a mask for 2 bits,1+2)
                clean &= ~(SYMBOL_MASK << bit);
            }
            // step by bits per symbol
            int bit = moreSymbols != null ? moreSymbols.nextSetBit(0) : -1;
            while (bit >= 0) {
                final int s = bit + BITS_PER_LONG >> SYMBOL_SHIFT;
                cleanSymbol.accept(s);
                bit = moreSymbols.nextSetBit(bit + BITS_PER_SYMBOL);
            }
        }
        // internal cleansing
        symbols = 0L;
        count = 0;
        if (moreSymbols != null) {
            moreSymbols.clear();
        }
    }

    /**
     * @return the number of symbols defined in this scope.
     */
    public int getSymbolCount() {
        return count;
    }

    /**
     * Checks whether a symbol has already been declared.
     *
     * @param symbol the symbol
     * @return true if declared, false otherwise
     */
    public boolean hasSymbol(final int symbol) {
        final int bit = symbol << SYMBOL_SHIFT;
        return isSet(bit);
    }

    /**
     * Checks whether a symbol is declared as a constant.
     *
     * @param symbol the symbol
     * @return true if declared as constant, false otherwise
     */
    public boolean isConstant(final int symbol) {
        final int bit = symbol << SYMBOL_SHIFT | 1;
        return isSet(bit);
    }

    /**
     * Tests whether a given bit (not symbol) is set.
     * @param bit the bit
     * @return true if set
     */
    private boolean isSet(final int bit) {
        if (bit < BITS_PER_LONG) {
            return (symbols & 1L << bit) != 0L;
        }
        return moreSymbols != null && moreSymbols.get(bit - BITS_PER_LONG);
    }

    /**
     * Ensures more symbols can be stored.
     *
     * @return the set of more symbols
     */
    private BitSet moreBits() {
        if (moreSymbols == null) {
            moreSymbols = new BitSet();
        }
        return moreSymbols;
    }

    /**
     * Sets a given bit (not symbol).
     * @param bit the bit
     * @return true if it was actually set, false if it was set before
     */
    private boolean set(final int bit) {
        if (bit < BITS_PER_LONG) {
            if ((symbols & 1L << bit) != 0L) {
                return false;
            }
            symbols |= 1L << bit;
        } else {
            final int bit64 = bit - BITS_PER_LONG;
            final BitSet ms = moreBits();
            if (ms.get(bit64)) {
                return false;
            }
            ms.set(bit64, true);
        }
        return true;
    }
}
