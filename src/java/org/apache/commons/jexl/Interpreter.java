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

package org.apache.commons.jexl;

import java.util.Stack;

import org.apache.commons.jexl.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl.parser.ASTExpression;
import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTIdentifier;
import org.apache.commons.jexl.parser.ASTIntegerLiteral;
import org.apache.commons.jexl.parser.ASTNullLiteral;
import org.apache.commons.jexl.parser.ASTReference;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.jexl.parser.VisitorAdapter;
import org.apache.commons.jexl.util.Coercion;
import org.apache.commons.jexl.util.Introspector;
import org.apache.commons.jexl.util.introspection.Uberspect;

/**
 * Starting point for an interpreter of JEXL syntax.
 * @author Dion Gillard
 */
class Interpreter extends VisitorAdapter {

    /** The uberspect. */
    private Uberspect uberspect;

    /** The context to store/retrieve variables. */
    private JexlContext context;
    
    /** the stack that holds values during expressions. */
    private Stack valueStack;

    //private Resolver resolver;

    /**
     * Create the interpreter with the default settings.
     */
    public Interpreter() {
        super();
        setUberspect(Introspector.getUberspect());
    }

    /** 
     * Interpret the given script/expression.
     * @param node the script or expression to interpret.
     * @param aContext the context to interpret against.
     * @return the result of the interpretation.
     */
    public Object interpret(SimpleNode node, JexlContext aContext) {
        setContext(aContext);
        valueStack = new Stack();
        System.out.println("node is: " + node);
        return node.jjtAccept(this, null);
    }

    /**
     * sets the uberspect to use for divining bean properties etc.
     * @param anUberspect the uberspect.
     */
    public void setUberspect(Uberspect anUberspect) {
        uberspect = anUberspect;
    }

    /** 
     * Gets the uberspect.
     * @return an {@link Uberspect}
     */
    protected Uberspect getUberspect() {
        return uberspect;
    }

    public void setContext(JexlContext aContext) {
        context = aContext;
    }

    public Object visit(ASTBitwiseAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        // coerce these two values longs and add.
        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);
        return new Long(l & r);
    }

    public Object visit(ASTBitwiseComplNode node, Object data) { 
        node.dump(" ");
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        return new Long(~l);
    }
    
    public Object visit(ASTBitwiseOrNode node, Object data) { 
        node.dump(" ");
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);
        return new Long(l | r);
    }

    public Object visit(ASTBitwiseXorNode node, Object data) { 
        node.dump(" ");
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        long l = Coercion.coercelong(left);
        long r = Coercion.coercelong(right);
        return new Long(l ^ r);
    }

    public Object visit(ASTExpression node, Object data) { 
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    public Object visit(ASTExpressionExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    public Object visit(ASTIdentifier node, Object data) { 
        node.dump(" ");
        return context.getVars().get(node.image); 
    }
    
    public Object visit(ASTIntegerLiteral node, Object data) { 
        return Integer.valueOf(node.image); 
    }

    /** visit a 'null'. */
    public Object visit(ASTNullLiteral node, Object data) { 
        return null; 
    }

    public Object visit(ASTReference node, Object data) {
        node.dump(" ");
        return node.jjtGetChild(0).jjtAccept(this, data);
    }
}