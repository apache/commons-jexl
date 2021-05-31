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
import org.apache.commons.jexl3.internal.ScriptVisitor;

/**
 * Checks if node is an operator node.
 **/
class OperatorController extends ScriptVisitor {
    static final OperatorController INSTANCE  = new OperatorController();
    /**
     * Controls the operator.
     * @param node the node
     * @param safe whether we are checking for any or only null-unsafe operators
     * @return true if node is (null-unsafe) operator
     */
    boolean control(final JexlNode node, Boolean safe) {
        return Boolean.TRUE.equals(node.jjtAccept(this, safe));
    }

    @Override
    protected Object visitNode(final JexlNode node, final Object data) {
        return false;
    }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTMulNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTModNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTDivNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTBitwiseAndNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTBitwiseOrNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTBitwiseXorNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTBitwiseComplNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTEQNode node, final Object data) {
        return data;
    }

    @Override
    protected Object visit(final ASTNENode node, final Object data) {
        return data;
    }

    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTGENode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTLTNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTLENode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        return true;
    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        return true;
    }
}
