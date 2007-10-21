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
package org.apache.commons.jexl.parser;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.util.Coercion;

/**
 * Represents equality between values.
 * 
 * If the values are of the same class, .equals() is used.
 * 
 * If either value is a {@link Float} or {@link Double} (but both are not the same class),
 * the values are coerced to {@link Double}s before comparing.
 * 
 * If either value is a {@link Number} or {@link Character} (but both are not the same class),
 * the values are coerced to {@link Long}s before comparing.
 *
 * If either value is a {@link Boolean} (but both are not the same class),
 * the values are coerced to {@link Boolean}s before comparing.
 * 
 * If either value is a {@link String} (but both are not the same class),
 * toString() is called on both before comparing.
 * 
 * Otherwise left.equals(right) is returned.
 * 
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class ASTEQNode extends SimpleNode {
    /**
     * Create the node given an id.
     * 
     * @param id node id.
     */
    public ASTEQNode(int id) {
        super(id);
    }

    /**
     * Create a node with the given parser and id.
     * 
     * @param p a parser.
     * @param id node id.
     */
    public ASTEQNode(Parser p, int id) {
        super(p, id);
    }

    /** {@inheritDoc} */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /** {@inheritDoc} */
    public Object value(JexlContext pc) throws Exception {
        Object left = ((SimpleNode) jjtGetChild(0)).value(pc);
        Object right = ((SimpleNode) jjtGetChild(1)).value(pc);

        if (left == null && right == null) {
            /*
             * if both are null L == R
             */
            return Boolean.TRUE;
        } else if (left == null || right == null) {
            /*
             * we know both aren't null, therefore L != R
             */
            return Boolean.FALSE;
        } else if (left.getClass().equals(right.getClass())) {
            return left.equals(right) ? Boolean.TRUE : Boolean.FALSE;
        } else if (left instanceof Float || left instanceof Double
                || right instanceof Float || right instanceof Double) {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            return l.equals(r) ? Boolean.TRUE : Boolean.FALSE;
        } else if (left instanceof Number || right instanceof Number
                || left instanceof Character || right instanceof Character) {
            return Coercion.coerceLong(left).equals(Coercion.coerceLong(right)) ? Boolean.TRUE
                    : Boolean.FALSE;
        } else if (left instanceof Boolean || right instanceof Boolean) {
            return Coercion.coerceBoolean(left).equals(
                    Coercion.coerceBoolean(right)) ? Boolean.TRUE
                    : Boolean.FALSE;
        } else if (left instanceof java.lang.String || right instanceof String) {
            return left.toString().equals(right.toString()) ? Boolean.TRUE
                    : Boolean.FALSE;
        }

        return left.equals(right) ? Boolean.TRUE : Boolean.FALSE;
    }
}
