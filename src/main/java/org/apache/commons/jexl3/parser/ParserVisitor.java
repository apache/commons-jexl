/*
 * Copyright 2011 The Apache Software Foundation.
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
package org.apache.commons.jexl3.parser;

/**
 * Fully abstract to avoid public interface exposition.
 */
public abstract class ParserVisitor {
    protected abstract Object visit(SimpleNode node, Object data);

    protected abstract Object visit(ASTJexlScript node, Object data);

    protected abstract Object visit(ASTBlock node, Object data);

    protected abstract Object visit(ASTAmbiguous node, Object data);

    protected abstract Object visit(ASTIfStatement node, Object data);

    protected abstract Object visit(ASTWhileStatement node, Object data);

    protected abstract Object visit(ASTForeachStatement node, Object data);

    protected abstract Object visit(ASTReturnStatement node, Object data);

    protected abstract Object visit(ASTAssignment node, Object data);

    protected abstract Object visit(ASTVar node, Object data);

    protected abstract Object visit(ASTReference node, Object data);

    protected abstract Object visit(ASTTernaryNode node, Object data);

    protected abstract Object visit(ASTOrNode node, Object data);

    protected abstract Object visit(ASTAndNode node, Object data);

    protected abstract Object visit(ASTBitwiseOrNode node, Object data);

    protected abstract Object visit(ASTBitwiseXorNode node, Object data);

    protected abstract Object visit(ASTBitwiseAndNode node, Object data);

    protected abstract Object visit(ASTEQNode node, Object data);

    protected abstract Object visit(ASTNENode node, Object data);

    protected abstract Object visit(ASTLTNode node, Object data);

    protected abstract Object visit(ASTGTNode node, Object data);

    protected abstract Object visit(ASTLENode node, Object data);

    protected abstract Object visit(ASTGENode node, Object data);

    protected abstract Object visit(ASTERNode node, Object data);

    protected abstract Object visit(ASTNRNode node, Object data);

    protected abstract Object visit(ASTAdditiveNode node, Object data);

    protected abstract Object visit(ASTAdditiveOperator node, Object data);

    protected abstract Object visit(ASTMulNode node, Object data);

    protected abstract Object visit(ASTDivNode node, Object data);

    protected abstract Object visit(ASTModNode node, Object data);

    protected abstract Object visit(ASTUnaryMinusNode node, Object data);

    protected abstract Object visit(ASTBitwiseComplNode node, Object data);

    protected abstract Object visit(ASTNotNode node, Object data);

    protected abstract Object visit(ASTIdentifier node, Object data);

    protected abstract Object visit(ASTNullLiteral node, Object data);

    protected abstract Object visit(ASTTrueNode node, Object data);

    protected abstract Object visit(ASTFalseNode node, Object data);

    protected abstract Object visit(ASTNumberLiteral node, Object data);

    protected abstract Object visit(ASTStringLiteral node, Object data);

    protected abstract Object visit(ASTArrayLiteral node, Object data);

    protected abstract Object visit(ASTMapLiteral node, Object data);

    protected abstract Object visit(ASTMapEntry node, Object data);

    protected abstract Object visit(ASTEmptyFunction node, Object data);

    protected abstract Object visit(ASTSizeFunction node, Object data);

    protected abstract Object visit(ASTFunctionNode node, Object data);

    protected abstract Object visit(ASTMethodNode node, Object data);

    protected abstract Object visit(ASTSizeMethod node, Object data);

    protected abstract Object visit(ASTConstructorNode node, Object data);

    protected abstract Object visit(ASTArrayAccess node, Object data);

    protected abstract Object visit(ASTReferenceExpression node, Object data);
}
