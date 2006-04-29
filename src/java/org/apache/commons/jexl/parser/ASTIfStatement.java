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
import org.apache.commons.jexl.util.Coercion;

/**
 * if ( expression ) statement [else statement].
 * 
 * @author Dion Gillard
 * @since 1.1
 */
public class ASTIfStatement extends SimpleNode {
    /** child index of the else statement to execute. */
    private static final int ELSE_STATEMENT_INDEX = 2;
    /**
     * Create the node given an id.
     * 
     * @param id node id.
     */
    public ASTIfStatement(int id) {
        super(id);
    }

    /**
     * Create a node with the given parser and id.
     * 
     * @param p a parser.
     * @param id node id.
     */
    public ASTIfStatement(Parser p, int id) {
        super(p, id);
    }

    /** {@inheritDoc} */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /** {@inheritDoc} */
    public Object value(JexlContext jc) throws Exception {
        Object result = null;
        /* first child is the expression */
        Object expression = ((SimpleNode) jjtGetChild(0)).value(jc);
        if (Coercion.coerceBoolean(expression).booleanValue()) {
            // true statement
            result = ((SimpleNode) jjtGetChild(1)).value(jc);
        } else {
            // if there is a false, execute it
            if (jjtGetNumChildren() == ELSE_STATEMENT_INDEX + 1) {
                result = ((SimpleNode) jjtGetChild(2)).value(jc);
            }
        }

        return result;
    }

}
