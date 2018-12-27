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
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayConstructorNode;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTAssertStatement;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTCastNode;
import org.apache.commons.jexl3.parser.ASTClassLiteral;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTDecrementNode;
import org.apache.commons.jexl3.parser.ASTDecrementPostfixNode;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTDoWhileStatement;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTElvisNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTEmptyMethod;
import org.apache.commons.jexl3.parser.ASTEnumerationNode;
import org.apache.commons.jexl3.parser.ASTEnumerationReference;
import org.apache.commons.jexl3.parser.ASTExpressionStatement;
import org.apache.commons.jexl3.parser.ASTExtVar;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForStatement;
import org.apache.commons.jexl3.parser.ASTForInitializationNode;
import org.apache.commons.jexl3.parser.ASTForTerminationNode;
import org.apache.commons.jexl3.parser.ASTForIncrementNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTForeachVar;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIncrementNode;
import org.apache.commons.jexl3.parser.ASTIncrementPostfixNode;
import org.apache.commons.jexl3.parser.ASTIndirectNode;
import org.apache.commons.jexl3.parser.ASTInitialization;
import org.apache.commons.jexl3.parser.ASTInitializedArrayConstructorNode;
import org.apache.commons.jexl3.parser.ASTInlinePropertyAssignment;
import org.apache.commons.jexl3.parser.ASTInlinePropertyArrayEntry;
import org.apache.commons.jexl3.parser.ASTInlinePropertyEntry;
import org.apache.commons.jexl3.parser.ASTIOFNode;
import org.apache.commons.jexl3.parser.ASTISNode;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTJxltLiteral;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapEnumerationNode;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMapProjectionNode;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTMultipleAssignment;
import org.apache.commons.jexl3.parser.ASTMultipleIdentifier;
import org.apache.commons.jexl3.parser.ASTMultipleInitialization;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNINode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNSWNode;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTPointerNode;
import org.apache.commons.jexl3.parser.ASTProjectionNode;
import org.apache.commons.jexl3.parser.ASTQualifiedConstructorNode;
import org.apache.commons.jexl3.parser.ASTRangeNode;
import org.apache.commons.jexl3.parser.ASTReductionNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTRegexLiteral;
import org.apache.commons.jexl3.parser.ASTRemove;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTSelectionNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetShlNode;
import org.apache.commons.jexl3.parser.ASTSetSarNode;
import org.apache.commons.jexl3.parser.ASTSetShrNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTShiftLeftNode;
import org.apache.commons.jexl3.parser.ASTShiftRightNode;
import org.apache.commons.jexl3.parser.ASTShiftRightUnsignedNode;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTSizeMethod;
import org.apache.commons.jexl3.parser.ASTStartCountNode;
import org.apache.commons.jexl3.parser.ASTStopCountNode;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTSubNode;
import org.apache.commons.jexl3.parser.ASTSwitchStatement;
import org.apache.commons.jexl3.parser.ASTSwitchStatementCase;
import org.apache.commons.jexl3.parser.ASTSwitchStatementDefault;
import org.apache.commons.jexl3.parser.ASTSynchronizedStatement;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTThisNode;
import org.apache.commons.jexl3.parser.ASTThrowStatement;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTTryStatement;
import org.apache.commons.jexl3.parser.ASTTryVar;
import org.apache.commons.jexl3.parser.ASTTryWithResourceStatement;
import org.apache.commons.jexl3.parser.ASTTryResource;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTUnaryPlusNode;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.ASTNullpNode;

import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParserVisitor;

import java.util.regex.Pattern;
import org.apache.commons.jexl3.parser.StringParser;

/**
 * Helps pinpoint the cause of problems in expressions that fail during evaluation.
 * <p>
 * It rebuilds an expression string from the tree and the start/end offsets of the cause in that string.
 * This implies that exceptions during evaluation do always carry the node that's causing the error.
 * </p>
 * @since 2.0
 */
public class Debugger extends ParserVisitor implements JexlInfo.Detail {
    /** The builder to compose messages. */
    protected final StringBuilder builder = new StringBuilder();
    /** The cause of the issue to debug. */
    protected JexlNode cause = null;
    /** The starting character location offset of the cause in the builder. */
    protected int start = 0;
    /** The ending character location offset of the cause in the builder. */
    protected int end = 0;
    /** The indentation level. */
    protected int indentLevel = 0;
    /** Perform indentation?. */
    protected int indent = 2;
    /** accept() relative depth. */
    protected int depth = Integer.MAX_VALUE;

    /**
     * Creates a Debugger.
     */
    public Debugger() {
    }

    /**
     * Resets this debugger state.
     */
    public void reset() {
        builder.setLength(0);
        cause = null;
        start = 0;
        end = 0;
        indentLevel = 0;
        indent = 2;
        depth = Integer.MAX_VALUE;
    }

    /**
     * Position the debugger on the root of an expression.
     * @param jscript the expression
     * @return true if the expression was a {@link Script} instance, false otherwise
     */
    public boolean debug(JexlExpression jscript) {
        if (jscript instanceof Script) {
            return debug(((Script) jscript).script);
        } else {
            return false;
        }
    }

    /**
     * Position the debugger on the root of a script.
     * @param jscript the script
     * @return true if the script was a {@link Script} instance, false otherwise
     */
    public boolean debug(JexlScript jscript) {
        if (jscript instanceof Script) {
            return debug(((Script) jscript).script);
        } else {
            return false;
        }
    }

    /**
     * Seeks the location of an error cause (a node) in an expression.
     * @param node the node to debug
     * @return true if the cause was located, false otherwise
     */
    public boolean debug(JexlNode node) {
        return debug(node, true);
    }

    /**
     * Seeks the location of an error cause (a node) in an expression.
     * @param node the node to debug
     * @param r whether we should actively find the root node of the debugged node
     * @return true if the cause was located, false otherwise
     */
    public boolean debug(JexlNode node, boolean r) {
        start = 0;
        end = 0;
        indentLevel = 0;
        if (node != null) {
            builder.setLength(0);
            cause = node;
            // make arg cause become the root cause
            JexlNode walk = node;
            if (r) {
                while (walk.jjtGetParent() != null) {
                    walk = walk.jjtGetParent();
                }
            }
            accept(walk, null);
        }
        return end > 0;
    }

    /**
     * @return The rebuilt expression
     */
    @Override
    public String toString() {
        return builder.toString();
    }

    /**
     * Rebuilds an expression from a JEXL node.
     * @param node the node to rebuilt from
     * @return the rebuilt expression
     * @since 3.0
     */
    public String data(JexlNode node) {
        start = 0;
        end = 0;
        indentLevel = 0;
        if (node != null) {
            builder.setLength(0);
            cause = node;
            accept(node, null);
        }
        return builder.toString();
    }

    /**
     * @return The starting offset location of the cause in the expression
     */
    @Override
    public int start() {
        return start;
    }

    /**
     * @return The end offset location of the cause in the expression
     */
    @Override
    public int end() {
        return end;
    }

    /**
     * Sets the indentation level.
     * @param level the number of spaces for indentation, none if less or equal to zero
     */
    public void setIndentation(int level) {
        indentation(level);
    }
    
    /**
     * Sets the indentation level.
     * @param level the number of spaces for indentation, none if less or equal to zero
     * @return this debugger instance
     */
    public Debugger indentation(int level) {
        if (level <= 0) {
            indent = 0;
        } else {
            indent = level;
        }
        indentLevel = 0;
        return this;
    }
    
    /**
     * Sets this debugger relative maximum depth.
     * @param rdepth the maximum relative depth from the debugged node
     * @return this debugger instance
     */
    public Debugger depth(int rdepth) {
        this.depth = rdepth;
        return this;
    }

    /**
     * Checks if a child node is the cause to debug &amp; adds its representation to the rebuilt expression.
     * @param node the child node
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object accept(JexlNode node, Object data) {
        if (depth <= 0) {
            builder.append("...");
            return data;
        }
        if (node == cause) {
            start = builder.length();
        }
        depth -= 1;
        Object value = node.jjtAccept(this, data);
        depth += 1;
        if (node == cause) {
            end = builder.length();
        }
        return value;
    }

    /**
     * Adds a statement node to the rebuilt expression.
     * @param child the child node
     * @param data  visitor pattern argument
     * @return visitor pattern value
     */
    protected Object acceptStatement(JexlNode child, Object data) {
        JexlNode parent = child.jjtGetParent();
        if (indent > 0 && (parent instanceof ASTBlock || parent instanceof ASTJexlScript)) {
            for (int i = 0; i < indentLevel; ++i) {
                for(int s = 0; s < indent; ++s) {
                    builder.append(' ');
                }
            }
        }
        depth -= 1;
        Object value = accept(child, data);
        depth += 1;
        // blocks, if, for & while dont need a ';' at end
        if (!(child instanceof ASTJexlScript
            || child instanceof ASTBlock
            || child instanceof ASTIfStatement
            || child instanceof ASTForStatement
            || child instanceof ASTForeachStatement
            || child instanceof ASTWhileStatement
            || child instanceof ASTDoWhileStatement
            || child instanceof ASTTryStatement
            || child instanceof ASTTryWithResourceStatement
            || child instanceof ASTSwitchStatement
            || child instanceof ASTSynchronizedStatement
            || child instanceof ASTAnnotation)) {
            builder.append(';');
            if (indent > 0) {
                builder.append('\n');
            } else {
                builder.append(' ');
            }
        }
        return value;
    }

    /**
     * Checks if a terminal node is the the cause to debug &amp; adds its representation to the rebuilt expression.
     * @param node  the child node
     * @param image the child node token image (may be null)
     * @param data  visitor pattern argument
     * @return visitor pattern value
     */
    protected Object check(JexlNode node, String image, Object data) {
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
     * Checks if the children of a node using infix notation is the cause to debug, adds their representation to the
     * rebuilt expression.
     * @param node  the child node
     * @param infix the child node token
     * @param paren whether the child should be parenthesized
     * @param data  visitor pattern argument
     * @return visitor pattern value
     */
    protected Object infixChildren(JexlNode node, String infix, boolean paren, Object data) {
        int num = node.jjtGetNumChildren(); //child.jjtGetNumChildren() > 1;
        if (paren) {
            builder.append('(');
        }
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(infix);
            }
            accept(node.jjtGetChild(i), data);
        }
        if (paren) {
            builder.append(')');
        }
        return data;
    }

    /**
     * Checks if the child of a node using prefix notation is the cause to debug, adds their representation to the
     * rebuilt expression.
     * @param node   the node
     * @param prefix the node token
     * @param data   visitor pattern argument
     * @return visitor pattern value
     */
    protected Object prefixChild(JexlNode node, String prefix, Object data) {
        boolean paren = node.jjtGetChild(0).jjtGetNumChildren() > 1;
        builder.append(prefix);
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        if (paren) {
            builder.append(')');
        }
        return data;
    }

    /**
     * Checks if the child of a node using postfix notation is the cause to debug, adds their representation to the
     * rebuilt expression.
     * @param node   the node
     * @param suffix the node token
     * @param data   visitor pattern argument
     * @return visitor pattern value
     */
    protected Object postfixChild(JexlNode node, String suffix, Object data) {
        boolean paren = node.jjtGetChild(0).jjtGetNumChildren() > 1;
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        if (paren) {
            builder.append(')');
        }
        builder.append(suffix);
        return data;
    }

    @Override
    protected Object visit(ASTAddNode node, Object data) {
        return additiveNode(node, " + ", data);
    }

    @Override
    protected Object visit(ASTSubNode node, Object data) {
        return additiveNode(node, " - ", data);
    }

    /**
     * Rebuilds an additive expression.
     * @param node the node
     * @param op   the operator
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object additiveNode(JexlNode node, String op, Object data) {
        // need parenthesis if not in operator precedence order
        boolean paren = node.jjtGetParent() instanceof ASTMulNode
                || node.jjtGetParent() instanceof ASTDivNode
                || node.jjtGetParent() instanceof ASTModNode;
        int num = node.jjtGetNumChildren();
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(op);
            accept(node.jjtGetChild(i), data);
        }
        if (paren) {
            builder.append(')');
        }
        return data;
    }

    /**
     * Rebuilds a shift expression.
     * @param node the node
     * @param op   the operator
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object shiftNode(JexlNode node, String op, Object data) {
        // need parenthesis if not in operator precedence order
        boolean paren = node.jjtGetParent() instanceof ASTAddNode
                || node.jjtGetParent() instanceof ASTSubNode;
        int num = node.jjtGetNumChildren();
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(op);
            accept(node.jjtGetChild(i), data);
        }
        if (paren) {
            builder.append(')');
        }
        return data;
    }

    @Override
    protected Object visit(ASTAndNode node, Object data) {
        return infixChildren(node, " && ", false, data);
    }

    @Override
    protected Object visit(ASTArrayAccess node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            builder.append('[');
            accept(node.jjtGetChild(i), data);
            builder.append(']');
        }
        return data;
    }

    @Override
    protected Object visit(ASTArrayLiteral node, Object data) {
        int num = node.jjtGetNumChildren();

        if (node.isImmutable())
            builder.append("#");

        builder.append("[ ");
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(", ");
                accept(node.jjtGetChild(i), data);
            }
        }
        if (node.isExtended()) {
            if (num > 0)
                builder.append(",");
            builder.append("...");
        }

        builder.append(" ]");
        return data;
    }

    @Override
    protected Object visit(ASTRangeNode node, Object data) {
        return infixChildren(node, " .. ", false, data);
    }

    @Override
    protected Object visit(ASTAssignment node, Object data) {
        return infixChildren(node, " = ", false, data);
    }

    @Override
    protected Object visit(ASTInitialization node, Object data) {
        return infixChildren(node, " = ", false, data);
    }

    @Override
    protected Object visit(ASTMultipleIdentifier node, Object data) {
        int num = node.jjtGetNumChildren();
        boolean isVarDeclare = node.jjtGetChild(0) instanceof ASTExtVar;

        if (isVarDeclare)
            builder.append("var");

        builder.append('(');
        for (int i = 0; i < num; ++i) {
            if (i > 0)
                builder.append(',');
            accept(node.jjtGetChild(i), data);
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(ASTMultipleAssignment node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" = ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTMultipleInitialization node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" = ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTBitwiseAndNode node, Object data) {
        return infixChildren(node, " & ", false, data);
    }

    @Override
    protected Object visit(ASTBitwiseComplNode node, Object data) {
        return prefixChild(node, "~", data);
    }

    @Override
    protected Object visit(ASTBitwiseOrNode node, Object data) {
        boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
        return infixChildren(node, " | ", paren, data);
    }

    @Override
    protected Object visit(ASTBitwiseXorNode node, Object data) {
        boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
        return infixChildren(node, " ^ ", paren, data);
    }

    @Override
    protected Object visit(ASTBlock node, Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append('{');
        if (indent > 0) {
            indentLevel += 1;
            builder.append('\n');
        } else {
            builder.append(' ');
        }
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            JexlNode child = node.jjtGetChild(i);
            acceptStatement(child, data);
        }
        if (indent > 0) {
            indentLevel -= 1;
            for (int i = 0; i < indentLevel; ++i) {
                for(int s = 0; s < indent; ++s) {
                    builder.append(' ');
                }
            }
        }
        builder.append('}');
        return data;
    }

    @Override
    protected Object visit(ASTDivNode node, Object data) {
        return infixChildren(node, " / ", false, data);
    }

    @Override
    protected Object visit(ASTEmptyFunction node, Object data) {
        builder.append("empty ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTEmptyMethod node, Object data) {
        accept(node.jjtGetChild(0), data);
        check(node, ".empty()", data);
        return data;
    }

    @Override
    protected Object visit(ASTISNode node, Object data) {
        return infixChildren(node, " === ", false, data);
    }

    @Override
    protected Object visit(ASTNINode node, Object data) {
        return infixChildren(node, " !== ", false, data);
    }

    @Override
    protected Object visit(ASTEQNode node, Object data) {
        return infixChildren(node, " == ", false, data);
    }

    @Override
    protected Object visit(ASTERNode node, Object data) {
        return infixChildren(node, " =~ ", false, data);
    }

    @Override
    protected Object visit(ASTSWNode node, Object data) {
        return infixChildren(node, " =^ ", false, data);
    }

    @Override
    protected Object visit(ASTEWNode node, Object data) {
        return infixChildren(node, " =$ ", false, data);
    }

    @Override
    protected Object visit(ASTNSWNode node, Object data) {
        return infixChildren(node, " !^ ", false, data);
    }

    @Override
    protected Object visit(ASTNEWNode node, Object data) {
        return infixChildren(node, " !$ ", false, data);
    }

    @Override
    protected Object visit(ASTFalseNode node, Object data) {
        return check(node, "false", data);
    }

    @Override
    protected Object visit(ASTContinue node, Object data) {
        builder.append("continue");
        String label = node.getLabel();
        if (label != null) {
            builder.append(' ');
            builder.append(label);
        }
        return data;
    }

    @Override
    protected Object visit(ASTRemove node, Object data) {
        builder.append("remove");
        String label = node.getLabel();
        if (label != null) {
            builder.append(' ');
            builder.append(label);
        }
        return data;
    }

    @Override
    protected Object visit(ASTBreak node, Object data) {
        builder.append("break");
        String label = node.getLabel();
        if (label != null) {
            builder.append(' ');
            builder.append(label);
        }
        return data;
    }

    @Override
    protected Object visit(ASTForStatement node, Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append("for(");
        accept(node.jjtGetChild(0), data);
        builder.append(" ; ");
        accept(node.jjtGetChild(1), data);
        builder.append(" ; ");
        accept(node.jjtGetChild(2), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 3) {
            acceptStatement(node.jjtGetChild(3), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(ASTForInitializationNode node, Object data) {
        if (node.jjtGetNumChildren() > 0)
            accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTForTerminationNode node, Object data) {
        if (node.jjtGetNumChildren() > 0)
            accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTForIncrementNode node, Object data) {
        if (node.jjtGetNumChildren() > 0)
            accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTForeachStatement node, Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
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

    @Override
    protected Object visit(ASTForeachVar node, Object data) {
        accept(node.jjtGetChild(0), data);
        if (node.jjtGetNumChildren() > 1) {
            builder.append(", ");
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTTryStatement node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("try ");
        accept(node.jjtGetChild(0), data);
        if (num == 2) {
            builder.append(" finally ");
            accept(node.jjtGetChild(1), data);
        } else if (num >= 3) {
            builder.append(" catch (");
            accept(node.jjtGetChild(1), data);
            builder.append(") ");
            accept(node.jjtGetChild(2), data);
            if (num == 4) {
                builder.append(" finally ");
                accept(node.jjtGetChild(3), data);
            }
        }
        return data;
    }

    @Override
    protected Object visit(ASTTryVar node, Object data) {
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTTryWithResourceStatement node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("try ");
        builder.append("(");
        accept(node.jjtGetChild(0), data);
        builder.append(")");
        accept(node.jjtGetChild(1), data);
        if (num == 3) {
            builder.append(" finally ");
            accept(node.jjtGetChild(2), data);
        } else if (num >= 4) {
            builder.append(" catch (");
            accept(node.jjtGetChild(2), data);
            builder.append(") ");
            accept(node.jjtGetChild(3), data);
            if (num == 5) {
                builder.append(" finally ");
                accept(node.jjtGetChild(4), data);
            }
        }
        return data;
    }

    @Override
    protected Object visit(ASTTryResource node, Object data) {
        accept(node.jjtGetChild(0), data);
        if (node.jjtGetNumChildren() > 1) {
            builder.append("=");
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTGENode node, Object data) {
        return infixChildren(node, " >= ", false, data);
    }

    @Override
    protected Object visit(ASTGTNode node, Object data) {
        return infixChildren(node, " > ", false, data);
    }

    /** Checks identifiers that contain spaces or punctuation
     * (but underscore, at-sign, sharp-sign and dollar).
     */
    protected static final Pattern QUOTED_IDENTIFIER =
            Pattern.compile("[\\s]|[\\p{Punct}&&[^@#\\$_]]");

    /**
     * Checks whether an identifier should be quoted or not.
     * @param str the identifier
     * @return true if needing quotes, false otherwise
     */
    protected boolean needQuotes(String str) {
        return QUOTED_IDENTIFIER.matcher(str).find()
            || "size".equals(str)
            || "empty".equals(str);
    }

    @Override
    protected Object visit(ASTIdentifier node, Object data) {
        String ns = node.getNamespace();
        String image = StringParser.escapeIdentifier(node.getName());
        if (ns == null) {
            return check(node, image, data);
        } else {
            String nsid = StringParser.escapeIdentifier(ns) + ":" + image;
            return check(node, nsid, data);
        }
    }

    @Override
    protected Object visit(ASTIdentifierAccess node, Object data) {
        builder.append(node.isSafe() ? "?." : ".");
        String image = node.getName();
        if (node.isExpression()) {
            builder.append('`');
            builder.append(image.replace("`", "\\`"));
            builder.append('`');
        } else if (needQuotes(image)) {
            // quote it
            builder.append('\'');
            builder.append(image.replace("'", "\\'"));
            builder.append('\'');
        } else {
            builder.append(image);
        }
        return data;
    }

    @Override
    protected Object visit(ASTExpressionStatement node, Object data) {
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTIfStatement node, Object data) {
        final int numChildren = node.jjtGetNumChildren();
        // if (...) ...
        builder.append("if (");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        acceptStatement(node.jjtGetChild(1), data);
        // else... 
        if (numChildren > 2) {
            builder.append(" else ");
            acceptStatement(node.jjtGetChild(2), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(ASTNumberLiteral node, Object data) {
        return check(node, node.toString(), data);
    }

    /**
     * A pseudo visitor for parameters.
     * @param p the parameter name
     * @param data the visitor argument
     * @return the parameter name to use
     */
    protected String visitParameter(String p, Object data) {
        return p;
    }

    @Override
    protected Object visit(ASTJexlScript node, Object data) {
        // if lambda, produce parameters
        if (node instanceof ASTJexlLambda) {
            boolean expr = false;

            if (node.jjtGetNumChildren() == 1) {
               JexlNode child = node.jjtGetChild(0);

               if (!(child instanceof ASTBlock))
                   expr = true;
            }

            JexlNode parent = node.jjtGetParent();
            // use lambda syntax if not assigned
            boolean named = parent instanceof ASTAssignment && !expr;
            if (named) {
                builder.append("function");
            }

            String[] params = node.getParameters();

            if (named || params == null || params.length != 1 || node.isVarArgs())
                builder.append('(');

            if (params != null && params.length > 0) {
                builder.append(visitParameter(params[0], data));
                for (int p = 1; p < params.length; ++p) {
                    builder.append(", ");
                    builder.append(visitParameter(params[p], data));
                }

                if (node.isVarArgs())
                    builder.append("...");
            }

            if (named || params == null || params.length != 1 || node.isVarArgs())
                builder.append(')');

            if (named) {
                builder.append(' ');
            } else {
                if (expr) {
                    builder.append("=>");
                } else {
                    builder.append("->");
                }
            }
            // we will need a block...
        }
        // no parameters or done with them
        int num = node.jjtGetNumChildren();
        if (num == 1 && !(node instanceof ASTJexlLambda)) {
            data = accept(node.jjtGetChild(0), data);
        } else {
            for (int i = 0; i < num; ++i) {
                JexlNode child = node.jjtGetChild(i);
                acceptStatement(child, data);
            }
        }
        return data;
    }

    @Override
    protected Object visit(ASTLENode node, Object data) {
        return infixChildren(node, " <= ", false, data);
    }

    @Override
    protected Object visit(ASTLTNode node, Object data) {
        return infixChildren(node, " < ", false, data);
    }

    @Override
    protected Object visit(ASTIOFNode node, Object data) {
        return infixChildren(node, " instanceof ", false, data);
    }

    @Override
    protected Object visit(ASTMapEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTMapEnumerationNode node, Object data) {
        return prefixChild(node, "*:...", data);
    }

    @Override
    protected Object visit(ASTSetLiteral node, Object data) {
        if (node.isImmutable())
            builder.append("#");
        builder.append("{ ");
        int num = node.jjtGetNumChildren();
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(",");
                accept(node.jjtGetChild(i), data);
            }
        }
        builder.append(" }");
        return data;
    }

    @Override
    protected Object visit(ASTMapLiteral node, Object data) {
        if (node.isImmutable())
            builder.append("#");
        builder.append("{ ");
        int num = node.jjtGetNumChildren();
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(",");
                accept(node.jjtGetChild(i), data);
            }
        } else {
            builder.append(':');
        }
        builder.append(" }");
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyArrayEntry node, Object data) {
        builder.append("[");
        accept(node.jjtGetChild(0), data);
        builder.append("] : ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyEntry node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTInlinePropertyAssignment node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("{ ");
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(",");
            accept(node.jjtGetChild(i), data);
        }
        builder.append(" }");
        return data;
    }

    @Override
    protected Object visit(ASTConstructorNode node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new(");
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append(", ");
            accept(node.jjtGetChild(i), data);
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(ASTQualifiedConstructorNode node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        accept(node.jjtGetChild(0), data);
        builder.append("(");
        for (int i = 1; i < num; ++i) {
            if (i > 1)
                builder.append(", ");
            accept(node.jjtGetChild(i), data);
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(ASTArrayConstructorNode node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        accept(node.jjtGetChild(0), data);
        for (int i = 1; i < num; ++i) {
            builder.append("[");
            accept(node.jjtGetChild(i), data);
            builder.append("]");
        }
        return data;
    }

    @Override
    protected Object visit(ASTInitializedArrayConstructorNode node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("new ");
        accept(node.jjtGetChild(0), data);
        builder.append("[]");
        builder.append("{");
        for (int i = 1; i < num; ++i) {
            if (i > 1)
                builder.append(", ");
            accept(node.jjtGetChild(i), data);
        }
        builder.append("}");
        return data;
    }

    @Override
    protected Object visit(ASTFunctionNode node, Object data) {
        int num = node.jjtGetNumChildren();
        if (num == 3) {
            accept(node.jjtGetChild(0), data);
            builder.append(":");
            accept(node.jjtGetChild(1), data);
            accept(node.jjtGetChild(2), data);
        } else {
            accept(node.jjtGetChild(0), data);
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTMethodNode node, Object data) {
        int num = node.jjtGetNumChildren();
        if (num == 2) {
            accept(node.jjtGetChild(0), data);
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTArguments node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append("(");
        if (num > 0) {
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(", ");
                accept(node.jjtGetChild(i), data);
            }
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(ASTModNode node, Object data) {
        return infixChildren(node, " % ", false, data);
    }

    @Override
    protected Object visit(ASTMulNode node, Object data) {
        return infixChildren(node, " * ", false, data);
    }

    @Override
    protected Object visit(ASTShiftLeftNode node, Object data) {
        return shiftNode(node, " << ", data);
    }

    @Override
    protected Object visit(ASTShiftRightNode node, Object data) {
        return shiftNode(node, " >> ", data);
    }

    @Override
    protected Object visit(ASTShiftRightUnsignedNode node, Object data) {
        return shiftNode(node, " >>> ", data);
    }

    @Override
    protected Object visit(ASTNENode node, Object data) {
        return infixChildren(node, " != ", false, data);
    }

    @Override
    protected Object visit(ASTNRNode node, Object data) {
        return infixChildren(node, " !~ ", false, data);
    }

    @Override
    protected Object visit(ASTNotNode node, Object data) {
        builder.append("!");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTCastNode node, Object data) {
        builder.append("(");
        accept(node.jjtGetChild(0), data);
        builder.append(")");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTNullLiteral node, Object data) {
        check(node, "null", data);
        return data;
    }

    @Override
    protected Object visit(ASTOrNode node, Object data) {
        // need parenthesis if not in operator precedence order
        boolean paren = node.jjtGetParent() instanceof ASTAndNode;
        return infixChildren(node, " || ", paren, data);
    }

    @Override
    protected Object visit(ASTReference node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTReferenceExpression node, Object data) {
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

    @Override
    protected Object visit(ASTReturnStatement node, Object data) {
        builder.append("return ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTThrowStatement node, Object data) {
        builder.append("throw ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTAssertStatement node, Object data) {
        builder.append("assert ");
        accept(node.jjtGetChild(0), data);
        int num = node.jjtGetNumChildren();
        if (num > 1) {
            builder.append(" : ");
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTSizeFunction node, Object data) {
        builder.append("size ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTSizeMethod node, Object data) {
        accept(node.jjtGetChild(0), data);
        check(node, ".size()", data);
        return data;
    }

    @Override
    protected Object visit(ASTStringLiteral node, Object data) {
        String img = node.getLiteral().replace("'", "\\'");
        return check(node, "'" + img + "'", data);
    }

    @Override
    protected Object visit(ASTRegexLiteral node, Object data) {
        String img = node.toString().replace("/", "\\/");
        return check(node, "~/" + img + "/", data);
    }

    @Override
    protected Object visit(ASTClassLiteral node, Object data) {
        builder.append(node.toString());
        return data;
    }

    @Override
    protected Object visit(ASTTernaryNode node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append("? ");
        accept(node.jjtGetChild(1), data);
        if (node.jjtGetNumChildren() > 2) {
            builder.append(" : ");
            accept(node.jjtGetChild(2), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTElvisNode node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append("?:");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTNullpNode node, Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append("??");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(ASTTrueNode node, Object data) {
        check(node, "true", data);
        return data;
    }

    @Override
    protected Object visit(ASTThisNode node, Object data) {
        check(node, "this", data);
        return data;
    }

    @Override
    protected Object visit(ASTUnaryMinusNode node, Object data) {
        return prefixChild(node, "-", data);
    }

    @Override
    protected Object visit(ASTUnaryPlusNode node, Object data) {
        return prefixChild(node, "+", data);
    }

    @Override
    protected Object visit(ASTIncrementNode node, Object data) {
        return prefixChild(node, "++", data);
    }

    @Override
    protected Object visit(ASTDecrementNode node, Object data) {
        return prefixChild(node, "--", data);
    }

    @Override
    protected Object visit(ASTIncrementPostfixNode node, Object data) {
        return postfixChild(node, "++", data);
    }

    @Override
    protected Object visit(ASTDecrementPostfixNode node, Object data) {
        return postfixChild(node, "--", data);
    }

    @Override
    protected Object visit(ASTIndirectNode node, Object data) {
        return prefixChild(node, "*", data);
    }

    @Override
    protected Object visit(ASTPointerNode node, Object data) {
        builder.append("&");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(ASTEnumerationNode node, Object data) {
        int num = node.jjtGetNumChildren();
        if (num == 1) {
            builder.append("...");
            accept(node.jjtGetChild(0), data);
            return data;
        } else {
            builder.append("...(");
            accept(node.jjtGetChild(0), data);
            builder.append(':');
            accept(node.jjtGetChild(1), data);
            builder.append(")");
            return data;
        }
    }

    @Override
    protected Object visit(ASTEnumerationReference node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTVar node, Object data) {
        boolean isFinal = node.isFinal();
        if (isFinal) {
           builder.append("final ");
        }
        Class type = node.getType();
        if (type == null) {
           builder.append("var ");
        } else if (type.isPrimitive()) {
           builder.append(type.getSimpleName()).append(" ");
        } else {
           builder.append(type.getName()).append(" ");
        }
        check(node, node.getName(), data);
        return data;
    }

    @Override
    protected Object visit(ASTExtVar node, Object data) {
        check(node, node.getName(), data);
        return data;
    }

    @Override
    protected Object visit(ASTWhileStatement node, Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
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

    @Override
    protected Object visit(ASTDoWhileStatement node, Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append("do ");
        acceptStatement(node.jjtGetChild(0), data);
        builder.append(" while (");
        accept(node.jjtGetChild(1), data);
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(ASTSynchronizedStatement node, Object data) {
        builder.append("synchronized (");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        if (node.jjtGetNumChildren() > 1) {
            acceptStatement(node.jjtGetChild(1), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(ASTSwitchStatement node, Object data) {
        String label = node.getLabel();
        if (label != null) {
            builder.append(label);
            builder.append(" : ");
        }
        builder.append("switch (");
        accept(node.jjtGetChild(0), data);
        builder.append(") {");
        for (int i = 1; i < node.jjtGetNumChildren(); i++)
            accept(node.jjtGetChild(i), data);
        builder.append("}");
        return data;
    }

    @Override
    protected Object visit(ASTSwitchStatementCase node, Object data) {
        builder.append("case ");
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        if (node.jjtGetNumChildren() > 1) {
            for (int i = 1; i < node.jjtGetNumChildren(); i++)
                acceptStatement(node.jjtGetChild(i), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(ASTSwitchStatementDefault node, Object data) {
        builder.append("default : ");
        if (node.jjtGetNumChildren() > 0) {
            for (int i = 0; i < node.jjtGetNumChildren(); i++)
                acceptStatement(node.jjtGetChild(i), data);
        } else {
            builder.append(';');
        }
        return data;
    }

    @Override
    protected Object visit(ASTSetAddNode node, Object data) {
        return infixChildren(node, " += ", false, data);
    }

    @Override
    protected Object visit(ASTSetSubNode node, Object data) {
        return infixChildren(node, " -= ", false, data);
    }

    @Override
    protected Object visit(ASTSetMultNode node, Object data) {
        return infixChildren(node, " *= ", false, data);
    }

    @Override
    protected Object visit(ASTSetDivNode node, Object data) {
        return infixChildren(node, " /= ", false, data);
    }

    @Override
    protected Object visit(ASTSetModNode node, Object data) {
        return infixChildren(node, " %= ", false, data);
    }

    @Override
    protected Object visit(ASTSetAndNode node, Object data) {
        return infixChildren(node, " &= ", false, data);
    }

    @Override
    protected Object visit(ASTSetOrNode node, Object data) {
        return infixChildren(node, " |= ", false, data);
    }

    @Override
    protected Object visit(ASTSetXorNode node, Object data) {
        return infixChildren(node, " ^= ", false, data);
    }

    @Override
    protected Object visit(ASTSetShlNode node, Object data) {
        return infixChildren(node, " <<= ", false, data);
    }

    @Override
    protected Object visit(ASTSetSarNode node, Object data) {
        return infixChildren(node, " >>= ", false, data);
    }

    @Override
    protected Object visit(ASTSetShrNode node, Object data) {
        return infixChildren(node, " >>>= ", false, data);
    }

    @Override
    protected Object visit(ASTJxltLiteral node, Object data) {
        String img = node.getLiteral().replace("`", "\\`");
        return check(node, "`" + img + "`", data);
    }

    @Override
    protected Object visit(ASTAnnotation node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append('@');
        builder.append(node.getName());
        if (num > 0) {
            accept(node.jjtGetChild(0), data); // zut
        }
        return null;
    }

    @Override
    protected Object visit(ASTAnnotatedStatement node, Object data) {
        int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (i > 0) {// && child instanceof ASTBlock) {
                builder.append(' ');
            }
            JexlNode child = node.jjtGetChild(i);
            acceptStatement(child, data);
        }
        return data;
    }

    @Override
    protected Object visit(ASTProjectionNode node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append(".[");
        for (int i = 0; i < num; ++i) {
            if (i > 0)
                builder.append(',');
            accept(node.jjtGetChild(i), data);
        }
        builder.append(']');
        return data;
    }

    @Override
    protected Object visit(ASTMapProjectionNode node, Object data) {
        builder.append(".{");
        accept(node.jjtGetChild(0), data);
        builder.append(':');
        accept(node.jjtGetChild(1), data);
        builder.append('}');
        return data;
    }

    @Override
    protected Object visit(ASTSelectionNode node, Object data) {
        builder.append(".(");
        accept(node.jjtGetChild(0), data);
        builder.append(')');
        return data;
    }

    @Override
    protected Object visit(ASTStartCountNode node, Object data) {
        return prefixChild(node, ">", data);
    }

    @Override
    protected Object visit(ASTStopCountNode node, Object data) {
        return prefixChild(node, "<", data);
    }

    @Override
    protected Object visit(ASTReductionNode node, Object data) {
        int num = node.jjtGetNumChildren();
        builder.append(".$(");
        accept(node.jjtGetChild(0), data);
        if (num > 1) {
            builder.append(':');
            accept(node.jjtGetChild(1), data);
        }
        builder.append(')');
        return data;
    }

}
