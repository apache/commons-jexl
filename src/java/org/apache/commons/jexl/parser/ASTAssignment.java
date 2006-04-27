/*
 * Copyright 2002-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jexl.parser;

import org.apache.commons.jexl.JexlContext;

/**
 * x = y, assigns a value to a variable in the context.
 * 
 * @author Dion Gillard
 * 
 */
public class ASTAssignment extends SimpleNode {
    public ASTAssignment(int id) {
        super(id);
    }

    public ASTAssignment(Parser p, int id) {
        super(p, id);
    }

    /** Accept the visitor. * */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Handle assignment ( left = right )
     */
    public Object value(JexlContext context) throws Exception {
        // left should be the variable (reference) to assign to
        SimpleNode left = (SimpleNode) jjtGetChild(0);
        // right should be the expression to evaluate
        Object right = ((SimpleNode) jjtGetChild(1)).value(context);
        if (left instanceof ASTReference) {
            ASTReference reference = (ASTReference) left;
            left = (SimpleNode) reference.jjtGetChild(0);
            if (left instanceof ASTIdentifier) {
                String identifier = ((ASTIdentifier) left)
                        .getIdentifierString();
                context.getVars().put(identifier, right);
            }
        }
        return right;
    }
}