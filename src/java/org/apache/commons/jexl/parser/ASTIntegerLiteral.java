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

/**
 * represents an integer.
 * 
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class ASTIntegerLiteral extends SimpleNode {
    /** literal value. */
    protected Integer val;

    /**
     * Create the node given an id.
     * 
     * @param id node id.
     */
    public ASTIntegerLiteral(int id) {
        super(id);
    }

    /**
     * Create a node with the given parser and id.
     * 
     * @param p a parser.
     * @param id node id.
     */
    public ASTIntegerLiteral(Parser p, int id) {
        super(p, id);
    }

    /** {@inheritDoc} */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Part of reference resolution - wierd... in JSTL EL you can have foo.2
     * which is equiv to foo[2] it appears...
     *
     * @param obj the object to evaluate against.
     * @param ctx the {@link JexlContext}.
     * @throws Exception on any error.
     * @return the resulting value.
     * @see ASTArrayAccess#evaluateExpr(Object, Object)
     */
    public Object execute(Object obj, JexlContext ctx) throws Exception {
        return ASTArrayAccess.evaluateExpr(obj, val);
    }

    /** {@inheritDoc} */
    public Object value(JexlContext jc) throws Exception {
        return val;
    }
}
