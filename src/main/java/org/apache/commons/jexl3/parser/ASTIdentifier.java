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

/**
 * Identifiers, variables, ie symbols.
 */
public class ASTIdentifier extends JexlNode {
    protected String name = null;
    protected int symbol = -1;
    protected boolean isFinal;

    ASTIdentifier(int id) {
        super(id);
    }

    ASTIdentifier(Parser p, int id) {
        super(p, id);
    }

    @Override
    public String toString() {
        return name;
    }

    void setSymbol(String identifier) {
        if (identifier.charAt(0) == '#') {
            symbol = Integer.parseInt(identifier.substring(1));
        }
        name = identifier;
    }

    void setSymbol(int r, String identifier) {
        symbol = r;
        name = identifier;
    }

    public int getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public boolean isFinal() {
        return isFinal;
    }

    void setFinal() {
        isFinal = true;
    }
    
    public String getNamespace() {
        return null;
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
