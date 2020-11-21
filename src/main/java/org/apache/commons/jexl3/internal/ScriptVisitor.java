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
package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTDoWhileStatement;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTExtendedLiteral;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTJxltLiteral;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNSWNode;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNullpNode;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTRangeNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTRegexLiteral;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTSubNode;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTUnaryPlusNode;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParserVisitor;

/**
 * Fully abstract to avoid public interface exposition.
 */
public class ScriptVisitor extends ParserVisitor {
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

    @Override
    protected Object visit(final ASTJexlScript node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBlock node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIfStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTWhileStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDoWhileStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTContinue node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTBreak node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTReturnStatement node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAssignment node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTVar node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTReference node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTernaryNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNullpNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTOrNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAndNode node, final Object data) {
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
    protected Object visit(final ASTBitwiseAndNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEQNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTLTNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTLENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTGENode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMulNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTDivNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTModNode node, final Object data) {
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
    protected Object visit(final ASTBitwiseComplNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIdentifier node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNullLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTTrueNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTFalseNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTNumberLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTStringLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTRegexLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTExtendedLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMapEntry node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTEmptyFunction node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSizeFunction node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTFunctionNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTConstructorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArrayAccess node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTIdentifierAccess node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTArguments node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTReferenceExpression node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTJxltLiteral node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAnnotation node, final Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(final ASTAnnotatedStatement node, final Object data) {
        return visitNode(node, data);
    }
}
