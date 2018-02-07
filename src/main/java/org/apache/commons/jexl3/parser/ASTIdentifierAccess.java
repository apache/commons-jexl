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
 * Identifiers, variables and registers.
 */
public class ASTIdentifierAccess extends JexlNode {
    private String name = null;
    private Integer identifier = null;

    ASTIdentifierAccess(int id) {
        super(id);
    }

    ASTIdentifierAccess(Parser p, int id) {
        super(p, id);
    }

    void setIdentifier(String id) {
        name = id;
        identifier = parseIdentifier(id);
    }

    @Override
    public boolean isGlobalVar() {
        return !isSafe() && !isExpression();
    }

    /**
     * Whether this is a dot or a question-mark-dot aka safe-navigation access.
     * @return true is ?., false if .
     */
    public boolean isSafe() {
        return false;
    }

    /**
     * Whether this is a Jxlt based identifier.
     * @return true if `..${...}...`, false otherwise
     */
    public boolean isExpression() {
        return false;
    }

    /**
     * Parse an identifier which must be of the form:
     * 0|([1-9][0-9]*)
     * @param id the identifier
     * @return an integer or null
     */
    public static Integer parseIdentifier(String id) {
        // hand coded because the was no way to fail on leading '0's using NumberFormat
        if (id != null) {
            final int length = id.length();
            int val = 0;
            for (int i = 0; i < length; ++i) {
                char c = id.charAt(i);
                // leading 0s but no just 0, NaN
                if (c == '0') {
                    if (length == 1) {
                        return 0;
                    } else if (val == 0) {
                        return null;
                    }
                } // any non numeric, NaN
                else if (c < '0' || c > '9') {
                    return null;
                }
                val *= 10;
                val += (c - '0');
            }
            return val;
        }
        return null;
    }

    public Object getIdentifier() {
        return identifier != null? identifier : name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    @Override
    public String toString() {
        return name;
    }
}
