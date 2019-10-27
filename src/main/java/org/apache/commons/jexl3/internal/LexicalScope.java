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
package org.apache.commons.jexl3.internal;

import java.util.BitSet;

/**
 * The set of symbols declared in a lexical scope.
 * <p>The symbol identifiers are determined by the functional scope.
 */
public final class LexicalScope {
    /** Number of bits in a long. */
    protected static final int LONGBITS = 64;
    /** The mask of symbols in the frame. */
    protected long symbols = 0L;
    /** Symbols after 64. */
    protected BitSet moreSymbols = null;
    /** Previous block. */
    protected final LexicalScope previous;

    /**
     * Default ctor.
     * @param scope the previous scope
     */
    public LexicalScope(LexicalScope scope) {
        this(null, scope);
    }

    /**
     * Create a scope.
     * @param frame the current execution frame
     * @param scope the previous scope
     */
    public LexicalScope(Frame frame, LexicalScope scope) {
        if (frame != null) {
            int argc = frame.getScope().getArgCount();
            for(int a  = 0; a < argc; ++a) {
                declareSymbol(a);
            }
        }
        previous = scope;
    }

    /**
     * Ensure more symbpls can be stored.
     * @return the set of more symbols
     */
    final BitSet moreSymbols() {
        if (moreSymbols == null) {
            moreSymbols = new BitSet();
        }
        return moreSymbols;
    }
    
    /**
     * Checks whether a symbol has already been declared.
     * @param symbol the symbol
     * @return true if declared, false otherwise
     */
    public boolean hasSymbol(int symbol) {
        if (symbol < LONGBITS) {
            return (symbols & (1L << symbol)) != 0L;
        } else {
            return moreSymbols == null ? false : moreSymbols.get(symbol - LONGBITS);
        }
    }

    /**
     * Declares a local symbol.
     *
     * @param symbol the symbol index
     * @return true if was not already declared, false if lexical clash (error)
     */
    public boolean declareSymbol(int symbol) {
        LexicalScope walk = previous;
        while (walk != null) {
            if (walk.hasSymbol(symbol)) {
                return false;
            }
            walk = walk.previous;
        }
        if (symbol < LONGBITS) {
            if ((symbols & (1L << symbol)) != 0L) {
                return false;
            }
            symbols |= (1L << symbol);
        } else {
            int s = symbol - LONGBITS;
            BitSet ms = moreSymbols();
            if (ms.get(s)) {
                return false;
            }
            ms.set(s, true);
        }
        return true;
    }

    /**
     * @return the number of symbols defined in this scope.
     */
    public int getSymbolCount() {
        return Long.bitCount(symbols) + (moreSymbols == null? 0 : moreSymbols.cardinality());
    }
}
