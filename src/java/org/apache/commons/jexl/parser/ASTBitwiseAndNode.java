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
 * Bitwise And. Syntax: a & b Result is a Long
 * 
 * @author Dion Gillard
 * @since 1.1
 */
public class ASTBitwiseAndNode extends SimpleNode {
    public ASTBitwiseAndNode(int id) {
        super(id);
    }

    public ASTBitwiseAndNode(Parser p, int id) {
        super(p, id);
    }

    /** Accept the visitor. * */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * @return a {@link Long} which is the bitwise and of the two operands.
     */
    public Object value(JexlContext context) throws Exception {
        Object left = ((SimpleNode) jjtGetChild(0)).value(context);
        Object right = ((SimpleNode) jjtGetChild(1)).value(context);

        Long l = left == null ? new Long(0) : Coercion.coerceLong(left);
        Long r = right == null ? new Long(0) : Coercion.coerceLong(right);
        return new Long(l.longValue() & r.longValue());
    }
}
