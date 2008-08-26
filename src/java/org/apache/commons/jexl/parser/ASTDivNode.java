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

import java.math.BigInteger;
import java.math.BigDecimal;

import org.apache.commons.jexl.util.Coercion;
import org.apache.commons.jexl.JexlContext;

/**
 * a / b, mathematical divide.
 *
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class ASTDivNode extends SimpleNode {
    /**
     * Create the node given an id.
     *
     * @param id node id.
     */
    public ASTDivNode(int id) {
        super(id);
    }

    /**
     * Create a node with the given parser and id.
     *
     * @param p a parser.
     * @param id node id.
     */
    public ASTDivNode(Parser p, int id) {
        super(p, id);
    }

    /** {@inheritDoc} */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /** {@inheritDoc} */
    public Object value(JexlContext jc) throws Exception {
        Object left = ((SimpleNode) jjtGetChild(0)).value(jc);
        Object right = ((SimpleNode) jjtGetChild(1)).value(jc);

        /*
         * the spec says 'and', I think 'or'
         */
        if (left == null && right == null) {
            return new Byte((byte) 0);
        }

        if (left instanceof BigInteger || right instanceof BigInteger) {
            BigInteger l = Coercion.coerceBigInteger(left);
            BigInteger r = Coercion.coerceBigInteger(right);
            return l.divide(r);
        }

        if (left instanceof BigDecimal || right instanceof BigDecimal) {
            BigDecimal l = Coercion.coerceBigDecimal(left);
            BigDecimal r = Coercion.coerceBigDecimal(right);
            return l.divide(r);
        }

        Double l = Coercion.coerceDouble(left);
        Double r = Coercion.coerceDouble(right);

        /*
         * catch div/0
         */
        if (r.doubleValue() == 0.0) {
            return new Double(0.0);
        }

        return new Double(l.doubleValue() / r.doubleValue());

    }
}
