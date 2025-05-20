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

/**
 * Declares a try/catch/finally statement.
 */
public class ASTTryStatement extends JexlLexicalNode {
    private static final long serialVersionUID = 1L;
    /** catch() &= 1, finally &= 2. */
    private int tryForm;

    public ASTTryStatement(final int id) {
        super(id);
    }

    public void catchClause() {
        tryForm |= 1;
    }

    public void finallyClause() {
        tryForm |= 2;
    }

    public boolean hasCatchClause() {
        return (tryForm & 1) != 0;
    }

    public boolean hasFinallyClause() {
        return (tryForm & 2) != 0;
    }

    @Override
    public Object jjtAccept(final ParserVisitor visitor, final Object data) {
        return visitor.visit(this, data);
    }

}
