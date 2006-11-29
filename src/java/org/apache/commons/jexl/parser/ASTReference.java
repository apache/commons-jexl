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
 * reference - any variable expression.
 * 
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class ASTReference extends SimpleNode {
    /** first variable in the expression. */
    protected SimpleNode root;

    /**
     * Create the node given an id.
     * 
     * @param id node id.
     */
    public ASTReference(int id) {
        super(id);
    }

    /**
     * Create a node with the given parser and id.
     * 
     * @param p a parser.
     * @param id node id.
     */
    public ASTReference(Parser p, int id) {
        super(p, id);
    }

    /** {@inheritDoc} */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /** {@inheritDoc} */
    public Object value(JexlContext jc) throws Exception {
        return execute(null, jc);
    }

    /** Store the first child as {@link ASTReference#root root}. */
    public void jjtClose() {
        root = (SimpleNode) jjtGetChild(0);
    }

    /**
     * evaluate each piece of the reference.
     * 
     * e.g. foo.bar.woogie[2].name, foo is our 'root', and we need to
     * evaluate 'bar.woogie[2].name' relative to foo.
     * 
     * @param jc the {@link JexlContext} to evaluate against.
     * @param obj not used. root.value(jc) is used instead.
     * @return the value of the array expression.
     * @throws Exception on any error
     */
    public Object execute(Object obj, JexlContext jc) throws Exception {
        Object o = root.value(jc);

        /*
         * ignore the first child - it's our identifier
         */
        for (int i = 1; i < jjtGetNumChildren(); i++) {
            o = ((SimpleNode) jjtGetChild(i)).execute(o, jc);

            // check for a variable in the context named
            // child0.child1.child2 etc
            if (o == null) {
                String varName = getIdentifierToDepth(i);
                o = jc.getVars().get(varName);
            }
        }

        return o;
    }

    /**
     * This method returns a variable from this identifier and it's children.
     * For an expression like 'a.b.c', a is child zero, b is child 1 and c is
     * child 2.
     * 
     * @param i the depth of the child nodes to go to
     * @return the a dotted variable from this identifier and it's child nodes.
     */
    private String getIdentifierToDepth(int i) {
        StringBuffer varName = new StringBuffer();
        for (int j = 0; j <= i; j++) {
            SimpleNode node = (SimpleNode) jjtGetChild(j);
            if (node instanceof ASTIdentifier) {
                varName.append(((ASTIdentifier) node).getIdentifierString());
                if (j != i) {
                    varName.append('.');
                }
            }
        }
        return varName.toString();
    }

    /**
     * Gets the variable name of {@link ASTReference#root root}.
     * @return the identifier.
     * @throws Exception on any error
     * @see ASTIdentifier#getIdentifierString()
     * @see ASTArrayAccess#getIdentifierString()
     */
    public String getRootString() throws Exception {
        if (root instanceof ASTIdentifier) {
            return ((ASTIdentifier) root).getIdentifierString();
        }

        if (root instanceof ASTArrayAccess) {
            return ((ASTArrayAccess) root).getIdentifierString();
        }

        throw new Exception("programmer error : ASTReference : root not known" + root);
    }
}
