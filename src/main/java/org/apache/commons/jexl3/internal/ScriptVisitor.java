/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.*;

/**
 * Concrete visitor base, used for feature and operator controllers.
 */
public class ScriptVisitor extends ParserVisitor {
    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAndNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAnnotatedStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAnnotation node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArguments node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayAccess node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAssignment node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseAndNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseComplNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseOrNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBitwiseXorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBlock node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBreak node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTCaseStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSwitchExpression node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTCaseExpression node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTContinue node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDecrementGetNode node, final Object data) { return visitNode(node, data); }

    @Override
    protected Object visit(final ASTDefineVars node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDivNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDoWhileStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEmptyFunction node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEQNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEQSNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTExtendedLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTFalseNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTFunctionNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGetDecrementNode node, final Object data) { return visitNode(node, data); }

    @Override
    protected Object visit(final ASTGetIncrementNode node, final Object data) { return visitNode(node, data); }
    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
        return visitNode(node, data);
    }
    @Override
    protected Object visit(final ASTIdentifier node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIdentifierAccess node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIfStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIncrementGetNode node, final Object data) { return visitNode(node, data); }

    @Override
    protected Object visit(final ASTInstanceOf node, final Object data) { return visitNode(node, data); }

    @Override
    protected Object visit(final ASTJexlScript node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTJxltLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTLENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTLTNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTModNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMulNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNESNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNotInstanceOf node, final Object data) { return visitNode(node, data); }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNullLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNullpNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNumberLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTOrNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTQualifiedIdentifier node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTReference node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTReferenceExpression node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTRegexLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTReturnStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetShiftLeftNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightUnsignedNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTShiftLeftNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTShiftRightNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTShiftRightUnsignedNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSizeFunction node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTStringLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTernaryNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTThrowStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTrueNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTryResources node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTryStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTUnaryMinusNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTUnaryPlusNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTVar node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTWhileStatement node, final Object data) {
        return visitNode(node, data);
    }

    /**
     * Visits all AST constituents of a JEXL expression.
     * @param jscript the expression
     * @param data some data context
     * @return the visit result or null if jscript was not a Script implementation
     */
    public Object visitExpression (final JexlExpression jscript, final Object data) {
        if (jscript instanceof Script) {
            return ((Script) jscript).getScript().jjtAccept(this, data);
        }
        return null;
    }

    /**
     * Visits a node.
     * Default implementation visits all its children.
     * @param node the node to visit
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object visitNode(final JexlNode node, final Object data) {
        return node.childrenAccept(this, data);
    }

    /**
     * Visits all AST constituents of a JEXL script.
     * @param jscript the expression
     * @param data some data context
     * @return the visit result or null if jscript was not a Script implementation
     */
    public Object visitScript(final JexlScript jscript, final Object data) {
        if (jscript instanceof Script) {
            return ((Script) jscript).getScript().jjtAccept(this, data);
        }
        return null;
    }
}
