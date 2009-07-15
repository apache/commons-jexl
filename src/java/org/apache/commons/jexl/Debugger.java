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
package org.apache.commons.jexl;

import org.apache.commons.jexl.parser.ASTAddNode;
import org.apache.commons.jexl.parser.ASTAndNode;
import org.apache.commons.jexl.parser.ASTArrayAccess;
import org.apache.commons.jexl.parser.ASTAssignment;
import org.apache.commons.jexl.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl.parser.ASTBlock;
import org.apache.commons.jexl.parser.ASTDivNode;
import org.apache.commons.jexl.parser.ASTEQNode;
import org.apache.commons.jexl.parser.ASTEmptyFunction;
import org.apache.commons.jexl.parser.ASTExpression;
import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTFalseNode;
import org.apache.commons.jexl.parser.ASTFloatLiteral;
import org.apache.commons.jexl.parser.ASTForeachStatement;
import org.apache.commons.jexl.parser.ASTFunctionNode;
import org.apache.commons.jexl.parser.ASTGENode;
import org.apache.commons.jexl.parser.ASTGTNode;
import org.apache.commons.jexl.parser.ASTIdentifier;
import org.apache.commons.jexl.parser.ASTIfStatement;
import org.apache.commons.jexl.parser.ASTIntegerLiteral;
import org.apache.commons.jexl.parser.ASTJexlScript;
import org.apache.commons.jexl.parser.ASTLENode;
import org.apache.commons.jexl.parser.ASTLTNode;
import org.apache.commons.jexl.parser.ASTMapEntry;
import org.apache.commons.jexl.parser.ASTMapLiteral;
import org.apache.commons.jexl.parser.ASTMethodNode;
import org.apache.commons.jexl.parser.ASTModNode;
import org.apache.commons.jexl.parser.ASTMulNode;
import org.apache.commons.jexl.parser.ASTNENode;
import org.apache.commons.jexl.parser.ASTNotNode;
import org.apache.commons.jexl.parser.ASTNullLiteral;
import org.apache.commons.jexl.parser.ASTOrNode;
import org.apache.commons.jexl.parser.ASTReference;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.ASTSizeFunction;
import org.apache.commons.jexl.parser.ASTSizeMethod;
import org.apache.commons.jexl.parser.ASTStatementExpression;
import org.apache.commons.jexl.parser.ASTStringLiteral;
import org.apache.commons.jexl.parser.ASTSubtractNode;
import org.apache.commons.jexl.parser.ASTTernaryNode;
import org.apache.commons.jexl.parser.ASTTrueNode;
import org.apache.commons.jexl.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl.parser.ASTWhileStatement;
import org.apache.commons.jexl.parser.Node;
import org.apache.commons.jexl.parser.SimpleNode;

import org.apache.commons.jexl.parser.ParserVisitor;
/**
 * Helps pinpoint the cause of problems in expressions that fail during evaluation.
 * It rebuilds an expression string from the tree and the start/end offsets of the cause
 * in that string (TODO pretty print).
 * This implies that exceptions during evaluation should allways carry the node that's causing
 * the error.
 */
public class Debugger implements ParserVisitor {
    StringBuilder builder;
    Node cause;
    int start = 0;
    int end = 0;

    public Debugger() {
        builder = new StringBuilder();
        cause = null;
        start = 0;
        end = 0;
    }
    
    /**
     * Seeks the location of an error cause (a node) in an expression.
     * @return true if the cause was located, false otherwise
     */
    public boolean debug(Node cause) {
        start = 0;
        end = 0;
        if (cause != null) {
            builder = new StringBuilder();
            this.cause = cause;
            // make arg cause become the root cause
            Node root = cause;
            while (root.jjtGetParent() != null) {
                root = root.jjtGetParent();
            }
            root.jjtAccept(this, null);
        }
        return end > 0;
    }

    /**
     * @return The rebuilt expression
     */
    public String data() {
        return builder.toString();
    }

    /**
     * @return The starting offset location of the cause in the expression
     */
    public int start() {
        return start;
    }
    /**
     * @return The end offset location of the cause in the expression
     */
    public int end() {
        return end;
    }

    /**
     * Checks if a child node is the cause to debug & adds its representation to the rebuilt expression
     */
    private Object accept(Node node, Object data) {
        if (node == cause) {
            start = builder.length();
        }
        Object value = node.jjtAccept(this, data);
        if (node == cause) {
            end = builder.length();
        }
        return value;
    }

    /**
     * Checks if a terminal node is the the cause to debug & adds its representation to the rebuilt expression
     */
    private Object check(Node node, String image, Object data) {
        if (node == cause) {
            start = builder.length();
        }
        if (image != null) {
            builder.append(image);
        } else {
            builder.append(node.toString());
        }
        if (node == cause) {
            end = builder.length();
        }
        return data;
    }
    
    /**
     * Checks if the children of a node using infix notation is the cause to debug,
     * adds their representation to the rebuilt expression
     */
    private Object infixChildren(Node node, String infix, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(infix);
            }
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAddNode node, Object data) {
        return infixChildren(node, " + ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTAndNode node, Object data) {
        return infixChildren(node, " && ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTArrayAccess node, Object data) {
        accept(node.jjtGetChild(0), data);
        int num = node.jjtGetNumChildren();
        for (int i = 1; i < num; ++i) {
            builder.append("[");
            accept(node.jjtGetChild(i), data);
            builder.append("]");
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAssignment node, Object data) {
        return infixChildren(node, " = ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseAndNode node, Object data) {
        return infixChildren(node, " & ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseComplNode node, Object data) {
        builder.append("~");
        return accept(node.jjtGetChild(0), data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseOrNode node, Object data) {
        return infixChildren(node, " | ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseXorNode node, Object data) {
        return infixChildren(node, " ^ ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBlock node, Object data) {
        builder.append("{ ");
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(0), data);
        }
        builder.append(" }");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTDivNode node, Object data) {
        return infixChildren(node, " / ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTEmptyFunction node, Object data) {
        builder.append("empty(");
        accept(node.jjtGetChild(0), data);
        builder.append(")");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTEQNode node, Object data) {
        return infixChildren(node, " == ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTExpression node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTExpressionExpression node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        builder.append(";");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTFalseNode node, Object data) {
        return check(node, "false", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTFloatLiteral node, Object data) {
        return check(node, node.image, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTForeachStatement node, Object data) {
        builder.append("foreach(");
        accept(node.jjtGetChild(0), data);
        builder.append(" in ");
        accept(node.jjtGetChild(1), data);
        builder.append(") ");
        accept(node.jjtGetChild(2), data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTGENode node, Object data) {
        return infixChildren(node, " >= ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTGTNode node, Object data) {
        return infixChildren(node, " > ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTIdentifier node, Object data) {
        return check(node, node.image, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTIfStatement node, Object data) {
        builder.append("if(");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        accept(node.jjtGetChild(1), data);
        if (node.jjtGetNumChildren() > 2) {
            builder.append(" else ");
            accept(node.jjtGetChild(2), data);
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTIntegerLiteral node, Object data) {
        return check(node, node.image, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTJexlScript node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTLENode node, Object data) {
        return infixChildren(node, " <= ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTLTNode node, Object data) {
        return infixChildren(node, " < ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" => ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapLiteral node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("[");
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(", ");
            accept(node.jjtGetChild(i), data);
        }
        builder.append("]");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTFunctionNode node, Object data) {
        int num = node.jjtGetNumChildren();
        accept(node.jjtGetChild(0), data);
        builder.append(":");
        accept(node.jjtGetChild(1), data);
        builder.append("(");
        for (int i = 2; i < num; ++i) {
            if (i > 2) {
                builder.append(", ");
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append(")");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTMethodNode node, Object data) {
        int num = node.jjtGetNumChildren();
        accept(node.jjtGetChild(0), data);
        builder.append("(");
        for (int i = 1; i < num; ++i) {
            if (i > 1) {
                builder.append(", ");
            }
            accept(node.jjtGetChild(i), data);
        }
        builder.append(")");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTModNode node, Object data) {
        return infixChildren(node, " % ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMulNode node, Object data) {
        return infixChildren(node, " * ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNENode node, Object data) {
        return infixChildren(node, " != ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNotNode node, Object data) {
        builder.append("!");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTNullLiteral node, Object data) {
        check(node, "null", data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTOrNode node, Object data) {
        return infixChildren(node, " ||", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTReference node, Object data) {
        int num = node.jjtGetNumChildren();
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(".");
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTReferenceExpression node, Object data) {
        int num = node.jjtGetNumChildren();
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(".");
            accept(node.jjtGetChild(i), data);
        }
        builder.append(";");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeFunction node, Object data) {
        builder.append("size(");
        accept(node.jjtGetChild(0), data);
        builder.append(")");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeMethod node, Object data) {
        check(node, "size()", data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTStatementExpression node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
            if (i > 0) {
                builder.append(";");
            }
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTStringLiteral node, Object data) {
        String img = node.image.replace("'", "\\'");
        return check(node, "'" + img + "'", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTSubtractNode node, Object data) {
        return infixChildren(node, " - ", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTTernaryNode node, Object data) {
        accept(node.jjtGetChild(0), data);
        if (node.jjtGetNumChildren() > 2) {
            builder.append("? ");
            accept(node.jjtGetChild(1), data);
            builder.append(" : ");
            accept(node.jjtGetChild(2), data);
        } else {
            builder.append("?:");
            accept(node.jjtGetChild(1), data);

        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTTrueNode node, Object data) {
        check(node, "true", data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTUnaryMinusNode node, Object data) {
        builder.append("-");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTWhileStatement node, Object data) {
        builder.append("while(");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(SimpleNode node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }
}
