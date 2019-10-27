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

import org.apache.commons.jexl3.internal.LexicalScope;

/**
 * Declares a local variable.
 */
public class ASTForeachStatement extends JexlNode implements JexlParser.LexicalUnit {
    private LexicalScope locals = null;
    
    public ASTForeachStatement(int id) {
        super(id);
    }

    public ASTForeachStatement(Parser p, int id) {
        super(p, id);
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
    
    @Override
    public boolean declareSymbol(int symbol) {
        if (locals == null) {
            locals  = new LexicalScope(null);
        }
        return locals.declareSymbol(symbol);
    }
    
    @Override
    public int getSymbolCount() {
        return locals == null? 0 : locals.getSymbolCount();
    }

    @Override
    public boolean hasSymbol(int symbol) {
        return locals == null? false : locals.hasSymbol(symbol);
    }    
    
    @Override
    public void clearUnit() {
        locals = null;
    }
}