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
 * The set of valued symbols declared in a lexical scope.
 * <p>The symbol identifiers are determined by the functional scope.
 */
public class LexicalFrame extends LexicalScope {
    /** The script frame. */
    private final Frame frame;
    /** The stack of values in the lexical frame. */
    private Deque<Object> stack = null;
    /**
     * Lexical frame ctor.
     * @param frame the script frame
     * @param previous the previous lexical frame
     */
    public LexicalFrame(Frame frame, LexicalFrame previous) {
        super(previous);
        this.frame = frame;
    }

    public LexicalFrame declareArgs() {
        if (frame != null) {
            int argc = frame.getScope().getArgCount();
            for(int a  = 0; a < argc; ++a) {
                super.registerSymbol(a);
            }
        }
        return this;
    }

    @Override
    public boolean declareSymbol(int symbol) {
        boolean declared = super.declareSymbol(symbol);
        if (declared && frame.getScope().isHoistedSymbol(symbol)) {
            if (stack == null) {
                stack = new ArrayDeque<Object>() ;
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
     * @return the previous frame
     */
    public LexicalFrame pop() {
        if (stack != null) {
            while(!stack.isEmpty()) {
                Object value = stack.pop();
                if (value == Scope.UNDECLARED) {
                    value = Scope.UNDEFINED;
                } else if (value == this) {// || value == Scope.UNDEFINED) {
                    value = null;
                }
                int symbol = (Integer) stack.pop();
                frame.set(symbol, value);
            }
        }
        return (LexicalFrame) previous;
    }
}
