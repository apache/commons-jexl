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
package org.apache.commons.jexl3.parser;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.internal.ScriptVisitor;

/**
 * Checks if node is an operator node.
 **/
final class OperatorController extends ScriptVisitor {
    static final OperatorController INSTANCE  = new OperatorController();

    /**
     * Checks whether an operator is strict for a given arithmetic.
     * @param node the node which should delegate to an operator
     * @return true if node points to a (null-unsafe) operator
     */
    boolean isStrict(final JexlArithmetic arithmetic, final JexlNode node) {
        if (arithmetic.isStrict()) {
            final Object ctl = node.jjtAccept(this, arithmetic);
            if (ctl instanceof JexlOperator) {
                final JexlOperator operator = (JexlOperator) ctl;
                return arithmetic.isStrict(operator);
            }
        }
        return false;
    }

    @Override
    protected JexlOperator visit(final ASTAddNode node, final Object data) {
        return JexlOperator.ADD;
    }

    @Override
    protected JexlOperator visit(final ASTBitwiseAndNode node, final Object data) {
        return JexlOperator.AND;
    }

    @Override
    protected JexlOperator visit(final ASTBitwiseComplNode node, final Object data) {
        return JexlOperator.COMPLEMENT;
    }

    @Override
    protected JexlOperator visit(final ASTBitwiseOrNode node, final Object data) {
        return JexlOperator.OR;
    }

    @Override
    protected JexlOperator visit(final ASTBitwiseXorNode node, final Object data) {
        return JexlOperator.XOR;
    }

    @Override
    protected JexlOperator visit(final ASTDivNode node, final Object data) {
        return JexlOperator.DIVIDE;
    }

    @Override
    protected JexlOperator visit(final ASTEQNode node, final Object data) {
        return JexlOperator.EQ;
    }

    @Override
    protected JexlOperator visit(final ASTEQSNode node, final Object data) {
        return JexlOperator.EQSTRICT;
    }

    @Override
    protected JexlOperator visit(final ASTERNode node, final Object data) {
        return JexlOperator.CONTAINS;
    }

    @Override
    protected JexlOperator visit(final ASTEWNode node, final Object data) {
        return JexlOperator.ENDSWITH;
    }

    @Override
    protected JexlOperator visit(final ASTGENode node, final Object data) {
        return JexlOperator.GTE;
    }

    @Override
    protected JexlOperator visit(final ASTGTNode node, final Object data) {
        return JexlOperator.GT;
    }

    @Override
    protected JexlOperator visit(final ASTLENode node, final Object data) {
        return JexlOperator.LTE;
    }

    @Override
    protected JexlOperator visit(final ASTLTNode node, final Object data) {
        return JexlOperator.LT;
    }

    @Override
    protected JexlOperator visit(final ASTModNode node, final Object data) {
        return JexlOperator.MOD;
    }

    @Override
    protected JexlOperator visit(final ASTMulNode node, final Object data) {
        return JexlOperator.MULTIPLY;
    }

    @Override
    protected JexlOperator visit(final ASTNENode node, final Object data) {
        return JexlOperator.EQ;
    }

    @Override
    protected JexlOperator visit(final ASTNESNode node, final Object data) {
        return JexlOperator.EQSTRICT;
    }

    @Override
    protected JexlOperator visit(final ASTNEWNode node, final Object data) {
        return JexlOperator.ENDSWITH;
    }

    @Override
    protected JexlOperator visit(final ASTNotNode node, final Object data) {
        return JexlOperator.NOT;
    }

    @Override
    protected JexlOperator visit(final ASTNRNode node, final Object data) {
        return JexlOperator.CONTAINS;
    }

    @Override
    protected JexlOperator visit(final ASTNSWNode node, final Object data) {
        return JexlOperator.STARTSWITH;
    }

    @Override
    protected JexlOperator visit(final ASTSetAddNode node, final Object data) {
        return JexlOperator.SELF_ADD;
    }

    @Override
    protected JexlOperator visit(final ASTSetAndNode node, final Object data) {
        return JexlOperator.SELF_AND;
    }

    @Override
    protected JexlOperator visit(final ASTSetDivNode node, final Object data) {
        return JexlOperator.SELF_DIVIDE;
    }

    @Override
    protected JexlOperator visit(final ASTSetModNode node, final Object data) {
        return JexlOperator.SELF_MOD;
    }

    @Override
    protected JexlOperator visit(final ASTSetMultNode node, final Object data) {
        return JexlOperator.SELF_MULTIPLY;
    }

    @Override
    protected JexlOperator visit(final ASTSetOrNode node, final Object data) {
        return JexlOperator.SELF_OR;
    }

    @Override
    protected JexlOperator visit(final ASTSetSubNode node, final Object data) {
        return JexlOperator.SELF_SUBTRACT;
    }

    @Override
    protected JexlOperator visit(final ASTSetXorNode node, final Object data) {
        return JexlOperator.SELF_OR;
    }

    @Override
    protected JexlOperator visit(final ASTSubNode node, final Object data) {
        return JexlOperator.SUBTRACT;
    }

    @Override
    protected JexlOperator visit(final ASTSWNode node, final Object data) {
        return JexlOperator.STARTSWITH;
    }

    @Override
    protected JexlOperator visitNode(final JexlNode node, final Object data) {
        return null;
    }
}
