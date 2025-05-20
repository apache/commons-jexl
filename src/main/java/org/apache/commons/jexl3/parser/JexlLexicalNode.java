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
package org.apache.commons.jexl3.parser;

import org.apache.commons.jexl3.internal.LexicalScope;

/**
 * Base class for AST nodes behaving as lexical units.
 * @since 3.2
 */
public abstract class JexlLexicalNode extends JexlNode implements JexlParser.LexicalUnit {
    private static final long serialVersionUID = 1L;
    /** The local lexical scope, local information about let/const. */
    private LexicalScope lexicalScope;

    public JexlLexicalNode(final int id) {
        super(id);
    }

    @Override
    public boolean declareSymbol(final int symbol) {
        if (lexicalScope == null) {
            lexicalScope = new LexicalScope();
        }
        return lexicalScope.addSymbol(symbol);
    }

    @Override
    public LexicalScope getLexicalScope() {
        return lexicalScope;
    }

    @Override
    public int getSymbolCount() {
        return lexicalScope == null ? 0 : lexicalScope.getSymbolCount();
    }

    @Override
    public boolean hasSymbol(final int symbol) {
        return lexicalScope != null && lexicalScope.hasSymbol(symbol);
    }

    @Override
    public boolean isConstant(final int symbol) {
        return lexicalScope != null && lexicalScope.isConstant(symbol);
    }

    @Override
    public void jjtClose() {

    }

    @Override
    public void setConstant(final int symbol) {
        lexicalScope.addConstant(symbol);
    }
}
