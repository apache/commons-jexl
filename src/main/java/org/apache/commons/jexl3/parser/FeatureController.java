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
    public FeatureController(final JexlFeatures features) {
        this.features = features;
    }

    /**
     * Sets the features to controlNode.
     * @param fdesc the features
     */
    public void setFeatures(final JexlFeatures fdesc) {
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
    public void controlNode(final JexlNode node) {
        node.jjtAccept(this, null);
    }

    @Override
    protected Object visitNode(final JexlNode node, final Object data) {
        // no need to visit them since we close them one by one
        return data;
    }

    /**
     * Throws a feature exception.
     * @param feature the feature code
     * @param node    the node that caused it
     */
    public void throwFeatureException(final int feature, final JexlNode node) {
        final JexlInfo dbgInfo = node.jexlInfo();
        throw new JexlException.Feature(dbgInfo, feature, "");
    }

    /**
     * Checks whether a node is a string or an integer.
     * @param child the child node
     * @return true if string / integer, false otherwise
     */
    private boolean isArrayReferenceLiteral(final JexlNode child) {
        if (child instanceof ASTStringLiteral) {
            return true;
        }
        if (child instanceof ASTNumberLiteral && ((ASTNumberLiteral) child).isInteger()) {
            return true;
        }
        return false;
    }

    @Override
    protected Object visit(final ASTArrayAccess node, final Object data) {
        if (!features.supportsArrayReferenceExpr()) {
            for (int i = 0; i < node.jjtGetNumChildren(); ++i) {
                final JexlNode child = node.jjtGetChild(i);
                if (!isArrayReferenceLiteral(child)) {
                    throwFeatureException(JexlFeatures.ARRAY_REF_EXPR, child);
                }
            }
        }
        return data;
    }

    @Override
    protected Object visit(final ASTWhileStatement node, final Object data) {
        if (!features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOP, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTDoWhileStatement node, final Object data) {
        if (!features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOP, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
        if (!features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOP, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTConstructorNode node, final Object data) {
        if (!features.supportsNewInstance()) {
            throwFeatureException(JexlFeatures.NEW_INSTANCE, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        if (!features.supportsMethodCall()) {
            throwFeatureException(JexlFeatures.METHOD_CALL, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTAnnotation node, final Object data) {
        if (!features.supportsAnnotation()) {
            throwFeatureException(JexlFeatures.ANNOTATION, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, final Object data) {
        if (!features.supportsStructuredLiteral()) {
            throwFeatureException(JexlFeatures.STRUCTURED_LITERAL, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTMapLiteral node, final Object data) {
        if (!features.supportsStructuredLiteral()) {
            throwFeatureException(JexlFeatures.STRUCTURED_LITERAL, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        if (!features.supportsStructuredLiteral()) {
            throwFeatureException(JexlFeatures.STRUCTURED_LITERAL, node);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        if (!features.supportsStructuredLiteral()) {
            throwFeatureException(JexlFeatures.STRUCTURED_LITERAL, node);
        }
        return data;
    }

    private Object controlSideEffect(final JexlNode node, final Object data) {
        final JexlNode lv = node.jjtGetChild(0);
        if (!features.supportsSideEffectGlobal() && lv.isGlobalVar()) {
            throwFeatureException(JexlFeatures.SIDE_EFFECT_GLOBAL, lv);
        }
        if (!features.supportsSideEffect()) {
            throwFeatureException(JexlFeatures.SIDE_EFFECT, lv);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTAssignment node, final Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return controlSideEffect(node, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return controlSideEffect(node, data);
    }

}
