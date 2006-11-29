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
 * Size Method, e.g. size().
 * 
 * @author <a href="mailto:mhw@kremvax.net">Mark H. Wilkinson</a>
 * @version $Id$
 */
public class ASTSizeMethod extends SimpleNode {
    /**
     * Create the node given an id.
     * 
     * @param id node id.
     */
    public ASTSizeMethod(int id) {
        super(id);
    }

    /**
     * Create a node with the given parser and id.
     * 
     * @param p a parser.
     * @param id node id.
     */
    public ASTSizeMethod(Parser p, int id) {
        super(p, id);
    }

    /** {@inheritDoc} */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * evaluate size as part of an expression on a base object.
     * 
     * foo.bar.size
     * 
     * @param jc the {@link JexlContext} to evaluate against.
     * @param obj not used.
     * @return the value of the array expression.
     * @throws Exception on any error
     */
    public Object execute(Object obj, JexlContext jc) throws Exception {
        return new Integer(ASTSizeFunction.sizeOf(obj));
    }

}
