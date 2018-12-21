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
package org.apache.commons.jexl3.parser;

/**
 * Fully abstract to avoid public interface exposition.
 */
public abstract class ParserVisitor {
    /**
     * Unused, satisfy ParserVisitor interface.
     * @param node a node
     * @param data the data
     * @return does not return
     */
    protected final Object visit(SimpleNode node, Object data) {
        throw new UnsupportedOperationException(node.getClass().getSimpleName() + " : not supported yet.");
    }

    /**
     * Unused, should throw in Parser.
     * @param node a node
     * @param data the data
     * @return does not return
     */
    protected final Object visit(ASTAmbiguous node, Object data) {
        throw new UnsupportedOperationException("unexpected type of node");
    }

    protected abstract Object visit(ASTJexlScript node, Object data);

    protected abstract Object visit(ASTBlock node, Object data);

    protected abstract Object visit(ASTExpressionStatement node, Object data);

    protected abstract Object visit(ASTIfStatement node, Object data);

    protected abstract Object visit(ASTWhileStatement node, Object data);

    protected abstract Object visit(ASTDoWhileStatement node, Object data);

    protected abstract Object visit(ASTContinue node, Object data);

    protected abstract Object visit(ASTRemove node, Object data);

    protected abstract Object visit(ASTBreak node, Object data);

    protected abstract Object visit(ASTForStatement node, Object data);

    protected abstract Object visit(ASTForInitializationNode node, Object data);

    protected abstract Object visit(ASTForTerminationNode node, Object data);

    protected abstract Object visit(ASTForIncrementNode node, Object data);

    protected abstract Object visit(ASTForeachStatement node, Object data);

    protected abstract Object visit(ASTForeachVar node, Object data);

    protected abstract Object visit(ASTTryStatement node, Object data);

    protected abstract Object visit(ASTTryVar node, Object data);

    protected abstract Object visit(ASTTryWithResourceStatement node, Object data);

    protected abstract Object visit(ASTTryResource node, Object data);

    protected abstract Object visit(ASTThrowStatement node, Object data);

    protected abstract Object visit(ASTAssertStatement node, Object data);

    protected abstract Object visit(ASTSynchronizedStatement node, Object data);

    protected abstract Object visit(ASTSwitchStatement node, Object data);

    protected abstract Object visit(ASTSwitchStatementCase node, Object data);

    protected abstract Object visit(ASTSwitchStatementDefault node, Object data);

    protected abstract Object visit(ASTReturnStatement node, Object data);

    protected abstract Object visit(ASTMultipleAssignment node, Object data);

    protected abstract Object visit(ASTAssignment node, Object data);

    protected abstract Object visit(ASTVar node, Object data);

    protected abstract Object visit(ASTExtVar node, Object data);

    protected abstract Object visit(ASTReference node, Object data);

    protected abstract Object visit(ASTTernaryNode node, Object data);

    protected abstract Object visit(ASTElvisNode node, Object data);

    protected abstract Object visit(ASTNullpNode node, Object data);

    protected abstract Object visit(ASTOrNode node, Object data);

    protected abstract Object visit(ASTAndNode node, Object data);

    protected abstract Object visit(ASTBitwiseOrNode node, Object data);

    protected abstract Object visit(ASTBitwiseXorNode node, Object data);

    protected abstract Object visit(ASTBitwiseAndNode node, Object data);

    protected abstract Object visit(ASTISNode node, Object data);

    protected abstract Object visit(ASTNINode node, Object data);

    protected abstract Object visit(ASTEQNode node, Object data);

    protected abstract Object visit(ASTNENode node, Object data);

    protected abstract Object visit(ASTLTNode node, Object data);

    protected abstract Object visit(ASTGTNode node, Object data);

    protected abstract Object visit(ASTLENode node, Object data);

    protected abstract Object visit(ASTGENode node, Object data);

    protected abstract Object visit(ASTERNode node, Object data);

    protected abstract Object visit(ASTNRNode node, Object data);

    protected abstract Object visit(ASTSWNode node, Object data);

    protected abstract Object visit(ASTNSWNode node, Object data);

    protected abstract Object visit(ASTEWNode node, Object data);

    protected abstract Object visit(ASTNEWNode node, Object data);

    protected abstract Object visit(ASTIOFNode node, Object data);

    protected abstract Object visit(ASTAddNode node, Object data);

    protected abstract Object visit(ASTSubNode node, Object data);

    protected abstract Object visit(ASTMulNode node, Object data);

    protected abstract Object visit(ASTDivNode node, Object data);

    protected abstract Object visit(ASTModNode node, Object data);

    protected abstract Object visit(ASTShiftLeftNode node, Object data);

    protected abstract Object visit(ASTShiftRightNode node, Object data);

    protected abstract Object visit(ASTShiftRightUnsignedNode node, Object data);

    protected abstract Object visit(ASTUnaryMinusNode node, Object data);

    protected abstract Object visit(ASTUnaryPlusNode node, Object data);

    protected abstract Object visit(ASTIncrementNode node, Object data);

    protected abstract Object visit(ASTDecrementNode node, Object data);

    protected abstract Object visit(ASTIncrementPostfixNode node, Object data);

    protected abstract Object visit(ASTDecrementPostfixNode node, Object data);

    protected abstract Object visit(ASTIndirectNode node, Object data);

    protected abstract Object visit(ASTPointerNode node, Object data);

    protected abstract Object visit(ASTBitwiseComplNode node, Object data);

    protected abstract Object visit(ASTNotNode node, Object data);

    protected abstract Object visit(ASTCastNode node, Object data);

    protected abstract Object visit(ASTIdentifier node, Object data);

    protected abstract Object visit(ASTNullLiteral node, Object data);

    protected abstract Object visit(ASTThisNode node, Object data);

    protected abstract Object visit(ASTTrueNode node, Object data);

    protected abstract Object visit(ASTFalseNode node, Object data);

    protected abstract Object visit(ASTNumberLiteral node, Object data);

    protected abstract Object visit(ASTStringLiteral node, Object data);

    protected abstract Object visit(ASTRegexLiteral node, Object data);

    protected abstract Object visit(ASTClassLiteral node, Object data);

    protected abstract Object visit(ASTSetLiteral node, Object data);

    protected abstract Object visit(ASTArrayLiteral node, Object data);

    protected abstract Object visit(ASTRangeNode node, Object data);

    protected abstract Object visit(ASTMapLiteral node, Object data);

    protected abstract Object visit(ASTMapEntry node, Object data);

    protected abstract Object visit(ASTMapEnumerationNode node, Object data);

    protected abstract Object visit(ASTInlinePropertyAssignment node, Object data);

    protected abstract Object visit(ASTInlinePropertyArrayEntry node, Object data);

    protected abstract Object visit(ASTInlinePropertyEntry node, Object data);

    protected abstract Object visit(ASTEmptyFunction node, Object data);

    protected abstract Object visit(ASTEmptyMethod node, Object data);

    protected abstract Object visit(ASTSizeFunction node, Object data);

    protected abstract Object visit(ASTFunctionNode node, Object data);

    protected abstract Object visit(ASTMethodNode node, Object data);

    protected abstract Object visit(ASTSizeMethod node, Object data);

    protected abstract Object visit(ASTConstructorNode node, Object data);

    protected abstract Object visit(ASTQualifiedConstructorNode node, Object data);

    protected abstract Object visit(ASTArrayConstructorNode node, Object data);

    protected abstract Object visit(ASTInitializedArrayConstructorNode node, Object data);

    protected abstract Object visit(ASTArrayAccess node, Object data);

    protected abstract Object visit(ASTIdentifierAccess node, Object data);

    protected abstract Object visit(ASTArguments node, Object data);

    protected abstract Object visit(ASTReferenceExpression node, Object data);

    protected abstract Object visit(ASTSetAddNode node, Object data);

    protected abstract Object visit(ASTSetSubNode node, Object data);

    protected abstract Object visit(ASTSetMultNode node, Object data);

    protected abstract Object visit(ASTSetDivNode node, Object data);

    protected abstract Object visit(ASTSetModNode node, Object data);

    protected abstract Object visit(ASTSetAndNode node, Object data);

    protected abstract Object visit(ASTSetOrNode node, Object data);

    protected abstract Object visit(ASTSetXorNode node, Object data);

    protected abstract Object visit(ASTSetShlNode node, Object data);

    protected abstract Object visit(ASTSetSarNode node, Object data);

    protected abstract Object visit(ASTSetShrNode node, Object data);

    protected abstract Object visit(ASTJxltLiteral node, Object data);

    protected abstract Object visit(ASTAnnotation node, Object data);

    protected abstract Object visit(ASTAnnotatedStatement node, Object data);

    protected abstract Object visit(ASTEnumerationNode node, Object data);

    protected abstract Object visit(ASTEnumerationReference node, Object data);

    protected abstract Object visit(ASTProjectionNode node, Object data);

    protected abstract Object visit(ASTMapProjectionNode node, Object data);

    protected abstract Object visit(ASTSelectionNode node, Object data);

    protected abstract Object visit(ASTStartCountNode node, Object data);

    protected abstract Object visit(ASTStopCountNode node, Object data);

    protected abstract Object visit(ASTReductionNode node, Object data);
}
