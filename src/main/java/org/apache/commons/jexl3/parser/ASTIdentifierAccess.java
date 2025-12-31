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

import org.apache.commons.jexl3.JexlArithmetic;

/**
 * Identifiers, variables and registers.
 */
public class ASTIdentifierAccess extends JexlNode {
    /**
     */
    private static final long serialVersionUID = 1L;

    private String name;
    private Integer identifier;

    ASTIdentifierAccess(final int id) {
        super(id);
    }

    public Object getIdentifier() {
        return identifier != null ? identifier : name;
    }

    public String getName() {
        return name;
    }

    /**
     * Tests whether this is a Jxlt based identifier.
     *
     * @return true if `..${...}...`, false otherwise
     */
    public boolean isExpression() {
        return false;
    }

    @Override
    public boolean isGlobalVar() {
        return !isSafe() && !isExpression();
    }

    /**
     * Tests whether this is a dot or a question-mark-dot aka safe-navigation access.
     *
     * @return true is ?., false if.
     */
    public boolean isSafe() {
        return false;
    }

    @Override
    public Object jjtAccept(final ParserVisitor visitor, final Object data) {
        return visitor.visit(this, data);
    }

    void setIdentifier(final String id) {
        name = id;
        identifier = JexlArithmetic.parseIdentifier(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
