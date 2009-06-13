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

/**
 * Implementation of the ParserVisitor that dumps the current node and it's
 * children and then visits them.
 * 
 * @since 2.0
 */
public class VisitorAdapter implements ParserVisitor {

    /** {@inheritDoc} */
    public Object visit(ASTAddNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTAndNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTArrayAccess node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTAssignment node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseAndNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseComplNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseOrNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseXorNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBlock node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTDivNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTEmptyFunction node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTEQNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTExpression node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTExpressionExpression node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTFalseNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTFloatLiteral node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTForeachStatement node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTGENode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTGTNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTIdentifier node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTIfStatement node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTIntegerLiteral node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTJexlScript node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTLENode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTLTNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapEntry node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapLiteral node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMethod node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTModNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMulNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNENode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNotNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNullLiteral node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTOrNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTReference node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTReferenceExpression node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeFunction node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeMethod node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTStatementExpression node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTStringLiteral node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTSubtractNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTTernaryNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }
    
    /** {@inheritDoc} */
    public Object visit(ASTTrueNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTUnaryMinusNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTWhileStatement node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(SimpleNode node, Object data) {
        node.dump(" ");
        return node.childrenAccept(this, data);
    }

}