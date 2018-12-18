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

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.internal.ScriptVisitor;
/**
 * Controls that a script only uses enabled features.
 */
public class FeatureController extends ScriptVisitor {
    /** The set of features. */
    private JexlFeatures features = null;

    /**
     * Creates a features controller .
     */
    public FeatureController(JexlFeatures features) {
        this.features = features;
    }

    /**
     * Sets the features to controlNode.
     * @param fdesc the features
     */
    public void setFeatures(JexlFeatures fdesc) {
        this.features = fdesc;
    }

    /**
     * @return the controlled features
     */
    public JexlFeatures getFeatures() {
        return features;
    }

    /**
     * Perform the control on a node.
     * <p>Note that controlNode() does *not* visit node children in this class.
     * @param node the node to controlNode
     * @throws JexlException.Feature if required feature is disabled
     */
    public void controlNode(JexlNode node) {
        node.jjtAccept(this, null);
    }

    @Override
    protected Object visitNode(JexlNode node, Object data) {
        // no need to visit them since we close them one by one
        return data;
    }

    /**
     * Throws a feature exception.
     * @param feature the feature code
     * @param node    the node that caused it
     */
    public void throwFeatureException(int feature, JexlNode node) {
        JexlInfo dbgInfo = node.jexlInfo();
        throw new JexlException.Feature(dbgInfo, feature, "");
    }

    /**
     * Checks whether a node is a string or an integer.
     * @param child the child node
     * @return true if string / integer, false otherwise
     */
    private boolean isArrayReferenceLiteral(JexlNode child) {
        if (child instanceof ASTStringLiteral) {
            return true;
        }
        if (child instanceof ASTNumberLiteral && ((ASTNumberLiteral) child).isInteger()) {
            return true;
        }
        return false;
    }

    @Override
    protected Object visit(ASTArrayAccess node, Object data) {
        if (!features.supportsArrayReferenceExpr()) {
            for (int i = 0; i < node.jjtGetNumChildren(); ++i) {
                JexlNode child = node.jjtGetChild(i);
                if (!isArrayReferenceLiteral(child)) {
                    throwFeatureException(JexlFeatures.ARRAY_REF_EXPR, child);
                }
            }
        }
        return data;
    }

    @Override
    protected Object visit(ASTWhileStatement node, Object data) {
        if (!features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOP, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTDoWhileStatement node, Object data) {
        if (!features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOP, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTForeachStatement node, Object data) {
        if (!features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOP, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTForStatement node, Object data) {
        if (!features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOP, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTConstructorNode node, Object data) {
        if (!features.supportsNewInstance()) {
            throwFeatureException(JexlFeatures.NEW_INSTANCE, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTQualifiedConstructorNode node, Object data) {
        if (!features.supportsNewInstance()) {
            throwFeatureException(JexlFeatures.NEW_INSTANCE, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTArrayConstructorNode node, Object data) {
        if (!features.supportsNewInstance()) {
            throwFeatureException(JexlFeatures.NEW_INSTANCE, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTInitializedArrayConstructorNode node, Object data) {
        if (!features.supportsNewInstance()) {
            throwFeatureException(JexlFeatures.NEW_INSTANCE, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTMethodNode node, Object data) {
        if (!features.supportsMethodCall()) {
            throwFeatureException(JexlFeatures.METHOD_CALL, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTAnnotation node, Object data) {
        if (!features.supportsAnnotation()) {
            throwFeatureException(JexlFeatures.ANNOTATION, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTArrayLiteral node, Object data) {
        if (!features.supportsStructuredLiteral()) {
            throwFeatureException(JexlFeatures.STRUCTURED_LITERAL, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTMapLiteral node, Object data) {
        if (!features.supportsStructuredLiteral()) {
            throwFeatureException(JexlFeatures.STRUCTURED_LITERAL, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTSetLiteral node, Object data) {
        if (!features.supportsStructuredLiteral()) {
            throwFeatureException(JexlFeatures.STRUCTURED_LITERAL, node);
        }
        return data;
    }

    @Override
    protected Object visit(ASTRangeNode node, Object data) {
        if (!features.supportsStructuredLiteral()) {
            throwFeatureException(JexlFeatures.STRUCTURED_LITERAL, node);
        }
        return data;
    }

    private Object controlSideEffect(JexlNode node, Object data) {
        JexlNode lv = node.jjtGetChild(0);
        if (!features.supportsSideEffectGlobal() && lv.isGlobalVar()) {
            throwFeatureException(JexlFeatures.SIDE_EFFECT_GLOBAL, lv);
        }
        if (!features.supportsSideEffect()) {
            throwFeatureException(JexlFeatures.SIDE_EFFECT, lv);
        }
        return data;
    }

    @Override
    protected Object visit(ASTAssignment node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTMultipleAssignment node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetAddNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetMultNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetDivNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetAndNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetOrNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetXorNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetSubNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetShlNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetSarNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTSetShrNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTIncrementNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTDecrementNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTIncrementPostfixNode node, Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(ASTDecrementPostfixNode node, Object data) {
        return controlSideEffect(node, data);
    }

}
