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
package org.apache.commons.jexl2;

import java.util.regex.Pattern;
import org.apache.commons.jexl2.parser.ASTAdditiveNode;
import org.apache.commons.jexl2.parser.ASTAdditiveOperator;
import org.apache.commons.jexl2.parser.ASTAmbiguous;
import org.apache.commons.jexl2.parser.ASTAndNode;
import org.apache.commons.jexl2.parser.ASTArrayAccess;
import org.apache.commons.jexl2.parser.ASTArrayLiteral;
import org.apache.commons.jexl2.parser.ASTAssignment;
import org.apache.commons.jexl2.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl2.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl2.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl2.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl2.parser.ASTBlock;
import org.apache.commons.jexl2.parser.ASTConstructorNode;
import org.apache.commons.jexl2.parser.ASTDivNode;
import org.apache.commons.jexl2.parser.ASTEQNode;
import org.apache.commons.jexl2.parser.ASTERNode;
import org.apache.commons.jexl2.parser.ASTEmptyFunction;
import org.apache.commons.jexl2.parser.ASTFalseNode;
import org.apache.commons.jexl2.parser.ASTForeachStatement;
import org.apache.commons.jexl2.parser.ASTFunctionNode;
import org.apache.commons.jexl2.parser.ASTGENode;
import org.apache.commons.jexl2.parser.ASTGTNode;
import org.apache.commons.jexl2.parser.ASTIdentifier;
import org.apache.commons.jexl2.parser.ASTIfStatement;
import org.apache.commons.jexl2.parser.ASTJexlScript;
import org.apache.commons.jexl2.parser.ASTLENode;
import org.apache.commons.jexl2.parser.ASTLTNode;
import org.apache.commons.jexl2.parser.ASTMapEntry;
import org.apache.commons.jexl2.parser.ASTMapLiteral;
import org.apache.commons.jexl2.parser.ASTMethodNode;
import org.apache.commons.jexl2.parser.ASTModNode;
import org.apache.commons.jexl2.parser.ASTMulNode;
import org.apache.commons.jexl2.parser.ASTNENode;
import org.apache.commons.jexl2.parser.ASTNRNode;
import org.apache.commons.jexl2.parser.ASTNotNode;
import org.apache.commons.jexl2.parser.ASTNullLiteral;
import org.apache.commons.jexl2.parser.ASTNumberLiteral;
import org.apache.commons.jexl2.parser.ASTOrNode;
import org.apache.commons.jexl2.parser.ASTReference;
import org.apache.commons.jexl2.parser.ASTReferenceExpression;
import org.apache.commons.jexl2.parser.ASTReturnStatement;
import org.apache.commons.jexl2.parser.ASTSizeFunction;
import org.apache.commons.jexl2.parser.ASTSizeMethod;
import org.apache.commons.jexl2.parser.ASTStringLiteral;
import org.apache.commons.jexl2.parser.ASTTernaryNode;
import org.apache.commons.jexl2.parser.ASTTrueNode;
import org.apache.commons.jexl2.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl2.parser.ASTVar;
import org.apache.commons.jexl2.parser.ASTWhileStatement;
import org.apache.commons.jexl2.parser.JexlNode;

import org.apache.commons.jexl2.parser.ParserVisitor;
import org.apache.commons.jexl2.parser.SimpleNode;

/**
 * Helps pinpoint the cause of problems in expressions that fail during evaluation.
 * <p>
 * It rebuilds an expression string from the tree and the start/end offsets of the cause
 * in that string.
 * </p>
 * This implies that exceptions during evaluation do allways carry the node that's causing
 * the error.
 * @since 2.0
 */
final class Debugger implements ParserVisitor {
    /** The builder to compose messages. */
    private final StringBuilder builder;
    /** The cause of the issue to debug. */
    private JexlNode cause;
    /** The starting character location offset of the cause in the builder. */
    private int start;
    /** The ending character location offset of the cause in the builder. */
    private int end;

    /**
     * Creates a Debugger.
     */
    Debugger() {
        builder = new StringBuilder();
        cause = null;
        start = 0;
        end = 0;
    }

    /**
     * Seeks the location of an error cause (a node) in an expression.
     * @param node the node to debug
     * @return true if the cause was located, false otherwise
     */
    public boolean debug(JexlNode node) {
        start = 0;
        end = 0;
        if (node != null) {
            builder.setLength(0);
            this.cause = node;
            // make arg cause become the root cause
            JexlNode root = node;
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
     * Rebuilds an expression from a Jexl node.
     * @param node the node to rebuilt from
     * @return the rebuilt expression
     * @since 2.1
     */
    public String data(JexlNode node) {
        start = 0;
        end = 0;
        if (node != null) {
            builder.setLength(0);
            this.cause = node;
            node.jjtAccept(this, null);
        }
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
     * Checks if a child node is the cause to debug &amp; adds its representation
     * to the rebuilt expression.
     * @param node the child node
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    private Object accept(JexlNode node, Object data) {
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
     * Adds a statement node to the rebuilt expression.
     * @param child the child node
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    private Object acceptStatement(JexlNode child, Object data) {
        Object value = accept(child, data);
        // blocks, if, for & while dont need a ';' at end
        if (child instanceof ASTBlock
                || child instanceof ASTIfStatement
                || child instanceof ASTForeachStatement
                || child instanceof ASTWhileStatement) {
            return value;
        }
        builder.append(";");
        return value;
    }

    /**
     * Checks if a terminal node is the the cause to debug &amp; adds its
     * representation to the rebuilt expression.
     * @param node the child node
     * @param image the child node token image (may be null)
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    private Object check(JexlNode node, String image, Object data) {
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
     * adds their representation to the rebuilt expression.
     * @param node the child node
     * @param infix the child node token
     * @param paren whether the child should be parenthesized
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    private Object infixChildren(JexlNode node, String infix, boolean paren, Object data) {
        int num = node.jjtGetNumChildren(); //child.jjtGetNumChildren() > 1;
        if (paren) {
            builder.append("(");
        }
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(infix);
            }
            accept(node.jjtGetChild(i), data);
        }
        if (paren) {
            builder.append(")");
        }
        return data;
    }

    /**
     * Checks if the child of a node using prefix notation is the cause to debug,
     * adds their representation to the rebuilt expression.
     * @param node the node
     * @param prefix the node token
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    private Object prefixChild(JexlNode node, String prefix, Object data) {
        boolean paren = node.jjtGetChild(0).jjtGetNumChildren() > 1;
        builder.append(prefix);
        if (paren) {
            builder.append("(");
        }
        accept(node.jjtGetChild(0), data);
        if (paren) {
            builder.append(")");
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAdditiveNode node, Object data) {
        // need parenthesis if not in operator precedence order
        boolean paren = node.jjtGetParent() instanceof ASTMulNode
                || node.jjtGetParent() instanceof ASTDivNode
                || node.jjtGetParent() instanceof ASTModNode;
        int num = node.jjtGetNumChildren(); //child.jjtGetNumChildren() > 1;
        if (paren) {
            builder.append("(");
        }
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        if (paren) {
            builder.append(")");
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAdditiveOperator node, Object data) {
        builder.append(' ');
        builder.append(node.image);
        builder.append(' ');
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAndNode node, Object data) {
        return infixChildren(node, " && ", false, data);
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
    public Object visit(ASTArrayLiteral node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("[ ");
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(", ");
                accept(node.jjtGetChild(i), data);
            }
        }
        builder.append(" ]");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAssignment node, Object data) {
        return infixChildren(node, " = ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseAndNode node, Object data) {
        return infixChildren(node, " & ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseComplNode node, Object data) {
        return prefixChild(node, "~", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseOrNode node, Object data) {
        boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
        return infixChildren(node, " | ", paren, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseXorNode node, Object data) {
        boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
        return infixChildren(node, " ^ ", paren, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTBlock node, Object data) {
        builder.append("{ ");
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            JexlNode child = node.jjtGetChild(i);
            acceptStatement(child, data);
        }
        builder.append(" }");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTDivNode node, Object data) {
        return infixChildren(node, " / ", false, data);
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
        return infixChildren(node, " == ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTERNode node, Object data) {
        return infixChildren(node, " =~ ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTFalseNode node, Object data) {
        return check(node, "false", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTForeachStatement node, Object data) {
        builder.append("for(");
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 2) {
            acceptStatement(node.jjtGetChild(2), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTGENode node, Object data) {
        return infixChildren(node, " >= ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTGTNode node, Object data) {
        return infixChildren(node, " > ", false, data);
    }

    /** Checks identifiers that contain space, quote, double-quotes or backspace. */
    private static final Pattern QUOTED_IDENTIFIER = Pattern.compile("['\"\\s\\\\]");
    
    /** {@inheritDoc} */
    public Object visit(ASTIdentifier node, Object data) {
        String image = node.image;
        if (QUOTED_IDENTIFIER.matcher(image).find()) {
            // quote it
            image = "'" + node.image.replace("'", "\\'") + "'";
        }
        return check(node, image, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTIfStatement node, Object data) {
        builder.append("if (");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 1) {
            acceptStatement(node.jjtGetChild(1), data);
            if (node.jjtGetNumChildren() > 2) {
                builder.append(" else ");
                acceptStatement(node.jjtGetChild(2), data);
            } else {
                builder.append(';');
            }
        } else {
            builder.append(';');
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTNumberLiteral node, Object data) {
        return check(node, node.image, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTJexlScript node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            JexlNode child = node.jjtGetChild(i);
            acceptStatement(child, data);
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTLENode node, Object data) {
        return infixChildren(node, " <= ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTLTNode node, Object data) {
        return infixChildren(node, " < ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapLiteral node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("{ ");
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(", ");
                accept(node.jjtGetChild(i), data);
            }
        } else {
            builder.append(':');
        }
        builder.append(" }");
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTConstructorNode node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        builder.append("(");
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(", ");
            accept(node.jjtGetChild(i), data);
        }
        builder.append(")");
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
        return infixChildren(node, " % ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTMulNode node, Object data) {
        return infixChildren(node, " * ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNENode node, Object data) {
        return infixChildren(node, " != ", false, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTNRNode node, Object data) {
        return infixChildren(node, " !~ ", false, data);
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
        // need parenthesis if not in operator precedence order
        boolean paren = node.jjtGetParent() instanceof ASTAndNode;
        return infixChildren(node, " || ", paren, data);
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
        JexlNode first = node.jjtGetChild(0);
        builder.append('(');
        accept(first, data);
        builder.append(')');
        int num = node.jjtGetNumChildren();
        for (int i = 1; i < num; ++i) {
            builder.append("[");
            accept(node.jjtGetChild(i), data);
            builder.append("]");
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTReturnStatement node, Object data) {
        builder.append("return ");
        accept(node.jjtGetChild(0), data);
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
    public Object visit(ASTStringLiteral node, Object data) {
        String img = node.image.replace("'", "\\'");
        return check(node, "'" + img + "'", data);
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
        return prefixChild(node, "-", data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTVar node, Object data) {
        builder.append("var ");
        check(node, node.image, data);
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(ASTWhileStatement node, Object data) {
        builder.append("while (");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 1) {
            acceptStatement(node.jjtGetChild(1), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    /** {@inheritDoc} */
    public Object visit(SimpleNode node, Object data) {
        throw new UnsupportedOperationException("unexpected type of node");
    }

    /** {@inheritDoc} */
    public Object visit(ASTAmbiguous node, Object data) {
        throw new UnsupportedOperationException("unexpected type of node");
    }
}