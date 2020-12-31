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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The set of valued symbols defined in a lexical frame.
 * <p>The symbol identifiers are determined by the functional scope.
 */
public class LexicalFrame extends LexicalScope {
    /**
     * The script frame.
     */
    private final Frame frame;
    /**
     * Previous frame.
     */
    protected final LexicalFrame previous;
    /**
     * The stack of values in the lexical frame.
     */
    private Deque<Object> stack = null;

    /**
     * Lexical frame ctor.
     *
     * @param scriptf the script frame
     * @param outerf  the previous lexical frame
     */
    public LexicalFrame(final Frame scriptf, final LexicalFrame outerf) {
        this.previous = outerf;
        this.frame = scriptf;
    }

    /**
     * Copy ctor.
     *
     * @param src the frame to copy
     */
    public LexicalFrame(final LexicalFrame src) {
        super(src.symbols, src.moreSymbols);
        frame = src.frame;
        previous = src.previous;
        stack = src.stack != null ? new ArrayDeque<>(src.stack) : null;
    }

    /**
     * Define the arguments.
     *
     * @return this frame
     */
    public LexicalFrame defineArgs() {
        if (frame != null) {
            final int argc = frame.getScope().getArgCount();
            for (int a = 0; a < argc; ++a) {
                super.addSymbol(a);
            }
        }
        return this;
    }

    /**
     * Defines a symbol.
     *
     * @param symbol  the symbol to define
     * @param capture whether this redefines a captured symbol
     * @return true if symbol is defined, false otherwise
     */
    public boolean defineSymbol(final int symbol, final boolean capture) {
        final boolean declared = addSymbol(symbol);
        if (declared && capture) {
            if (stack == null) {
                stack = new ArrayDeque<>();
            }
            stack.push(symbol);
            Object value = frame.get(symbol);
            if (value == null) {
                value = this;
            }
            stack.push(value);
        }
        return declared;
    }

    /**
     * Pops back values and lexical frame.
     *
     * @return the previous frame
     */
    public LexicalFrame pop() {
        // undefine all symbols
        clearSymbols(s ->   frame.set(s, Scope.UNDEFINED) );
        // restore values of captured symbols that were overwritten
        if (stack != null) {
            while (!stack.isEmpty()) {
                Object value = stack.pop();
                if (value == Scope.UNDECLARED) {
                    value = Scope.UNDEFINED;
                } else if (value == this) {
                    value = null;
                }
                final int symbol = (Integer) stack.pop();
                frame.set(symbol, value);
            }
        }
        return previous;
    }

}
