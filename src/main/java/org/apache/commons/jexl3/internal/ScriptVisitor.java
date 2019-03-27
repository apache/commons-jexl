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
import org.apache.commons.jexl3.parser.ASTEmptyMethod;
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
import org.apache.commons.jexl3.parser.ASTSizeMethod;
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
    public Object visitExpression (JexlExpression jscript, Object data) {
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
    public Object visitScript(JexlScript jscript, Object data) {
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
    protected Object visitNode(JexlNode node, Object data) {
        return node.childrenAccept(this, data);
    }

    @Override
    protected Object visit(ASTJexlScript node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTBlock node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTIfStatement node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTWhileStatement node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTDoWhileStatement node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTContinue node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTBreak node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTForeachStatement node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTReturnStatement node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTAssignment node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTVar node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTReference node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTTernaryNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTNullpNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTOrNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTAndNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTBitwiseOrNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTBitwiseXorNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTBitwiseAndNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTEQNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTNENode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTLTNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTGTNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTLENode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTGENode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTERNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTNRNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSWNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTNSWNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTEWNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTNEWNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTAddNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSubNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTMulNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTDivNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTModNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTUnaryMinusNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTUnaryPlusNode node, Object data) {
        return visitNode(node, data);
    }
    
    @Override
    protected Object visit(ASTBitwiseComplNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTNotNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTIdentifier node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTNullLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTTrueNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTFalseNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTNumberLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTStringLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTRegexLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTExtendedLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTArrayLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTRangeNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTMapLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTMapEntry node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTEmptyFunction node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTEmptyMethod node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSizeFunction node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTFunctionNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTMethodNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSizeMethod node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTConstructorNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTArrayAccess node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTIdentifierAccess node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTArguments node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTReferenceExpression node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetAddNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetSubNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetMultNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetDivNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetModNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetAndNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetOrNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTSetXorNode node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTJxltLiteral node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTAnnotation node, Object data) {
        return visitNode(node, data);
    }

    @Override
    protected Object visit(ASTAnnotatedStatement node, Object data) {
        return visitNode(node, data);
    }
}
