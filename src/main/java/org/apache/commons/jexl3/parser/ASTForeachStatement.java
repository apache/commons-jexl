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
 * Declares a for each loop.
 */
public class ASTForeachStatement extends JexlLexicalNode {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int loopForm;

    void setLoopForm(int form) {
        loopForm = form;
    }

    public int getLoopForm() {
        return loopForm;
    }

    public ASTForeachStatement(final int id) {
        super(id);
    }

    public ASTForeachStatement(final Parser p, final int id) {
        super(p, id);
    }

    @Override
    public Object jjtAccept(final ParserVisitor visitor, final Object data) {
        return visitor.visit(this, data);
    }

}
