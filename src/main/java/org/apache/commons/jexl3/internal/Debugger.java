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
import org.apache.commons.jexl3.parser.ASTJexlLambda;
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
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.ASTAnnotatedStatement;
import org.apache.commons.jexl3.parser.ASTAnnotation;
import org.apache.commons.jexl3.parser.ASTNullpNode;

import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParserVisitor;

import java.util.regex.Pattern;
import org.apache.commons.jexl3.parser.ASTUnaryPlusNode;
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
    public boolean debug(final JexlExpression jscript) {
        if (jscript instanceof Script) {
            return debug(((Script) jscript).script);
        }
        return false;
    }

    /**
     * Position the debugger on the root of a script.
     * @param jscript the script
     * @return true if the script was a {@link Script} instance, false otherwise
     */
    public boolean debug(final JexlScript jscript) {
        if (jscript instanceof Script) {
            return debug(((Script) jscript).script);
        }
        return false;
    }

    /**
     * Seeks the location of an error cause (a node) in an expression.
     * @param node the node to debug
     * @return true if the cause was located, false otherwise
     */
    public boolean debug(final JexlNode node) {
        return debug(node, true);
    }

    /**
     * Seeks the location of an error cause (a node) in an expression.
     * @param node the node to debug
     * @param r whether we should actively find the root node of the debugged node
     * @return true if the cause was located, false otherwise
     */
    public boolean debug(final JexlNode node, final boolean r) {
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
    public String data(final JexlNode node) {
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
    public void setIndentation(final int level) {
        indentation(level);
    }

    /**
     * Sets the indentation level.
     * @param level the number of spaces for indentation, none if less or equal to zero
     * @return this debugger instance
     */
    public Debugger indentation(final int level) {
        indent = Math.max(level, 0);
        indentLevel = 0;
        return this;
    }

    /**
     * Sets this debugger relative maximum depth.
     * @param rdepth the maximum relative depth from the debugged node
     * @return this debugger instance
     */
    public Debugger depth(final int rdepth) {
        this.depth = rdepth;
        return this;
    }

    /**
     * Checks if a child node is the cause to debug &amp; adds its representation to the rebuilt expression.
     * @param node the child node
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object accept(final JexlNode node, final Object data) {
        if (depth <= 0) {
            builder.append("...");
            return data;
        }
        if (node == cause) {
            start = builder.length();
        }
        depth -= 1;
        final Object value = node.jjtAccept(this, data);
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
    protected Object acceptStatement(final JexlNode child, final Object data) {
        final JexlNode parent = child.jjtGetParent();
        if (indent > 0 && (parent instanceof ASTBlock || parent instanceof ASTJexlScript)) {
            for (int i = 0; i < indentLevel; ++i) {
                for(int s = 0; s < indent; ++s) {
                    builder.append(' ');
                }
            }
        }
        depth -= 1;
        final Object value = accept(child, data);
        depth += 1;
        // blocks, if, for & while don't need a ';' at end
        if (!(child instanceof ASTJexlScript
            || child instanceof ASTBlock
            || child instanceof ASTIfStatement
            || child instanceof ASTForeachStatement
            || child instanceof ASTWhileStatement
            || child instanceof ASTDoWhileStatement
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
     * Checks if a terminal node is the cause to debug &amp; adds its representation to the rebuilt expression.
     * @param node  the child node
     * @param image the child node token image (may be null)
     * @param data  visitor pattern argument
     * @return visitor pattern value
     */
    protected Object check(final JexlNode node, final String image, final Object data) {
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
    protected Object infixChildren(final JexlNode node, final String infix, final boolean paren, final Object data) {
        final int num = node.jjtGetNumChildren(); //child.jjtGetNumChildren() > 1;
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
    protected Object prefixChild(final JexlNode node, final String prefix, final Object data) {
        final boolean paren = node.jjtGetChild(0).jjtGetNumChildren() > 1;
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

    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        return additiveNode(node, " + ", data);
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        return additiveNode(node, " - ", data);
    }

    /**
     * Rebuilds an additive expression.
     * @param node the node
     * @param op   the operator
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object additiveNode(final JexlNode node, final String op, final Object data) {
        // need parenthesis if not in operator precedence order
        final boolean paren = node.jjtGetParent() instanceof ASTMulNode
                || node.jjtGetParent() instanceof ASTDivNode
                || node.jjtGetParent() instanceof ASTModNode;
        final int num = node.jjtGetNumChildren();
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
    protected Object visit(final ASTAndNode node, final Object data) {
        return infixChildren(node, " && ", false, data);
    }

    @Override
    protected Object visit(final ASTArrayAccess node, final Object data) {
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            builder.append('[');
            accept(node.jjtGetChild(i), data);
            builder.append(']');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTExtendedLiteral node, final Object data) {
        builder.append("...");
        return data;
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, final Object data) {
        final int num = node.jjtGetNumChildren();
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

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        return infixChildren(node, " .. ", false, data);
    }

    @Override
    protected Object visit(final ASTAssignment node, final Object data) {
        return infixChildren(node, " = ", false, data);
    }

    @Override
    protected Object visit(final ASTBitwiseAndNode node, final Object data) {
        return infixChildren(node, " & ", false, data);
    }

    @Override
    protected Object visit(final ASTBitwiseComplNode node, final Object data) {
        return prefixChild(node, "~", data);
    }

    @Override
    protected Object visit(final ASTBitwiseOrNode node, final Object data) {
        final boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
        return infixChildren(node, " | ", paren, data);
    }

    @Override
    protected Object visit(final ASTBitwiseXorNode node, final Object data) {
        final boolean paren = node.jjtGetParent() instanceof ASTBitwiseAndNode;
        return infixChildren(node, " ^ ", paren, data);
    }

    @Override
    protected Object visit(final ASTBlock node, final Object data) {
        builder.append('{');
        if (indent > 0) {
            indentLevel += 1;
            builder.append('\n');
        } else {
            builder.append(' ');
        }
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            final JexlNode child = node.jjtGetChild(i);
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
    protected Object visit(final ASTDivNode node, final Object data) {
        return infixChildren(node, " / ", false, data);
    }

    @Override
    protected Object visit(final ASTEmptyFunction node, final Object data) {
        builder.append("empty ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTEQNode node, final Object data) {
        return infixChildren(node, " == ", false, data);
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        return infixChildren(node, " =~ ", false, data);
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        return infixChildren(node, " =^ ", false, data);
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        return infixChildren(node, " =$ ", false, data);
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        return infixChildren(node, " !^ ", false, data);
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        return infixChildren(node, " !$ ", false, data);
    }

    @Override
    protected Object visit(final ASTFalseNode node, final Object data) {
        return check(node, "false", data);
    }

    @Override
    protected Object visit(final ASTContinue node, final Object data) {
        return check(node, "continue", data);
    }

    @Override
    protected Object visit(final ASTBreak node, final Object data) {
        return check(node, "break", data);
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
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
    protected Object visit(final ASTGENode node, final Object data) {
        return infixChildren(node, " >= ", false, data);
    }

    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
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
    protected boolean needQuotes(final String str) {
        return QUOTED_IDENTIFIER.matcher(str).find()
            || "size".equals(str)
            || "empty".equals(str);
    }

    @Override
    protected Object visit(final ASTIdentifier node, final Object data) {
        final String ns = node.getNamespace();
        final String image = StringParser.escapeIdentifier(node.getName());
        if (ns == null) {
            return check(node, image, data);
        }
        final String nsid = StringParser.escapeIdentifier(ns) + ":" + image;
        return check(node, nsid, data);
    }

    @Override
    protected Object visit(final ASTIdentifierAccess node, final Object data) {
        builder.append(node.isSafe() ? "?." : ".");
        final String image = node.getName();
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
    protected Object visit(final ASTIfStatement node, final Object data) {
        final int numChildren = node.jjtGetNumChildren();
        // if (...) ...
        builder.append("if (");
        accept(node.jjtGetChild(0), data);
        builder.append(") ");
        acceptStatement(node.jjtGetChild(1), data);
        //.. else if (...) ...
        for(int c = 2; c <  numChildren - 1; c += 2) {
            builder.append(" else if (");
            accept(node.jjtGetChild(c), data);
            builder.append(") ");
            acceptStatement(node.jjtGetChild(c + 1), data);
        }
        // else... (if odd)
        if ((numChildren & 1) == 1) {
            builder.append(" else ");
            acceptStatement(node.jjtGetChild(numChildren - 1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTNumberLiteral node, final Object data) {
        return check(node, node.toString(), data);
    }

    /**
     * A pseudo visitor for parameters.
     * @param p the parameter name
     * @param data the visitor argument
     * @return the parameter name to use
     */
    protected String visitParameter(final String p, final Object data) {
        return p;
    }

    @Override
    protected Object visit(final ASTJexlScript node, Object data) {
        // if lambda, produce parameters
        if (node instanceof ASTJexlLambda) {
            final JexlNode parent = node.jjtGetParent();
            // use lambda syntax if not assigned
            final boolean named = parent instanceof ASTAssignment;
            if (named) {
                builder.append("function");
            }
            builder.append('(');
            final String[] params = node.getParameters();
            if (params != null && params.length > 0) {
                builder.append(visitParameter(params[0], data));
                for (int p = 1; p < params.length; ++p) {
                    builder.append(", ");
                    builder.append(visitParameter(params[p], data));
                }
            }
            builder.append(')');
            if (named) {
                builder.append(' ');
            } else {
                builder.append("->");
            }
            // we will need a block...
        }
        // no parameters or done with them
        final int num = node.jjtGetNumChildren();
        if (num == 1 && !(node instanceof ASTJexlLambda)) {
            data = accept(node.jjtGetChild(0), data);
        } else {
            for (int i = 0; i < num; ++i) {
                final JexlNode child = node.jjtGetChild(i);
                acceptStatement(child, data);
            }
        }
        return data;
    }

    @Override
    protected Object visit(final ASTLENode node, final Object data) {
        return infixChildren(node, " <= ", false, data);
    }

    @Override
    protected Object visit(final ASTLTNode node, final Object data) {
        return infixChildren(node, " < ", false, data);
    }

    @Override
    protected Object visit(final ASTMapEntry node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append(" : ");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("{ ");
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
    protected Object visit(final ASTMapLiteral node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("{ ");
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
    protected Object visit(final ASTConstructorNode node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("new(");
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
    protected Object visit(final ASTFunctionNode node, final Object data) {
        final int num = node.jjtGetNumChildren();
        if (num == 3) {
            accept(node.jjtGetChild(0), data);
            builder.append(":");
            accept(node.jjtGetChild(1), data);
            accept(node.jjtGetChild(2), data);
        } else if (num == 2) {
            accept(node.jjtGetChild(0), data);
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        final int num = node.jjtGetNumChildren();
        if (num == 2) {
            accept(node.jjtGetChild(0), data);
            accept(node.jjtGetChild(1), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTArguments node, final Object data) {
        final int num = node.jjtGetNumChildren();
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
    protected Object visit(final ASTModNode node, final Object data) {
        return infixChildren(node, " % ", false, data);
    }

    @Override
    protected Object visit(final ASTMulNode node, final Object data) {
        return infixChildren(node, " * ", false, data);
    }

    @Override
    protected Object visit(final ASTNENode node, final Object data) {
        return infixChildren(node, " != ", false, data);
    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        return infixChildren(node, " !~ ", false, data);
    }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        builder.append("!");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTNullLiteral node, final Object data) {
        check(node, "null", data);
        return data;
    }

    @Override
    protected Object visit(final ASTOrNode node, final Object data) {
        // need parenthesis if not in operator precedence order
        final boolean paren = node.jjtGetParent() instanceof ASTAndNode;
        return infixChildren(node, " || ", paren, data);
    }

    @Override
    protected Object visit(final ASTReference node, final Object data) {
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            accept(node.jjtGetChild(i), data);
        }
        return data;
    }

    @Override
    protected Object visit(final ASTReferenceExpression node, final Object data) {
        final JexlNode first = node.jjtGetChild(0);
        builder.append('(');
        accept(first, data);
        builder.append(')');
        final int num = node.jjtGetNumChildren();
        for (int i = 1; i < num; ++i) {
            builder.append("[");
            accept(node.jjtGetChild(i), data);
            builder.append("]");
        }
        return data;
    }

    @Override
    protected Object visit(final ASTReturnStatement node, final Object data) {
        builder.append("return ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTSizeFunction node, final Object data) {
        builder.append("size ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTStringLiteral node, final Object data) {
        final String img = node.getLiteral().replace("'", "\\'");
        return check(node, "'" + img + "'", data);
    }

    @Override
    protected Object visit(final ASTRegexLiteral node, final Object data) {
        final String img = node.toString().replace("/", "\\/");
        return check(node, "~/" + img + "/", data);
    }

    @Override
    protected Object visit(final ASTTernaryNode node, final Object data) {
        accept(node.jjtGetChild(0), data);
        if (node.jjtGetNumChildren() > 2) {
            builder.append("? ");
            accept(node.jjtGetChild(1), data);
            builder.append(" : ");
            accept(node.jjtGetChild(2), data);
        } else {
            builder.append("?: ");
            accept(node.jjtGetChild(1), data);

        }
        return data;
    }

    @Override
    protected Object visit(final ASTNullpNode node, final Object data) {
        accept(node.jjtGetChild(0), data);
        builder.append("??");
        accept(node.jjtGetChild(1), data);
        return data;
    }

    @Override
    protected Object visit(final ASTTrueNode node, final Object data) {
        check(node, "true", data);
        return data;
    }

    @Override
    protected Object visit(final ASTUnaryMinusNode node, final Object data) {
        return prefixChild(node, "-", data);
    }

    @Override
    protected Object visit(final ASTUnaryPlusNode node, final Object data) {
        return prefixChild(node, "+", data);
    }

    @Override
    protected Object visit(final ASTVar node, final Object data) {
        builder.append("var ");
        check(node, node.getName(), data);
        return data;
    }

    @Override
    protected Object visit(final ASTWhileStatement node, final Object data) {
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
    protected Object visit(final ASTDoWhileStatement node, final Object data) {
        builder.append("do ");
        final int nc = node.jjtGetNumChildren();
        if (nc > 1) {
            acceptStatement(node.jjtGetChild(0), data);
        } else {
            builder.append(";");
        }
        builder.append(" while (");
        accept(node.jjtGetChild(nc - 1), data);
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return infixChildren(node, " += ", false, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return infixChildren(node, " -= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return infixChildren(node, " *= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return infixChildren(node, " /= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        return infixChildren(node, " %= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return infixChildren(node, " &= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return infixChildren(node, " |= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return infixChildren(node, " ^= ", false, data);
    }

    @Override
    protected Object visit(final ASTJxltLiteral node, final Object data) {
        final String img = node.getLiteral().replace("`", "\\`");
        return check(node, "`" + img + "`", data);
    }

    @Override
    protected Object visit(final ASTAnnotation node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append('@');
        builder.append(node.getName());
        if (num > 0) {
            accept(node.jjtGetChild(0), data); // zut
        }
        return null;
    }

    @Override
    protected Object visit(final ASTAnnotatedStatement node, final Object data) {
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (i > 0) {// && child instanceof ASTBlock) {
                builder.append(' ');
            }
            final JexlNode child = node.jjtGetChild(i);
            acceptStatement(child, data);
        }
        return data;
    }
}