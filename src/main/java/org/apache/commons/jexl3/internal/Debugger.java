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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.*;

/**
 * Helps pinpoint the cause of problems in expressions that fail during evaluation.
 * <p>
 * It rebuilds an expression string from the tree and the start/end offsets of the cause in that string.
 * This implies that exceptions during evaluation do always carry the node that's causing the error.
 * </p>
 * @since 2.0
 */
public class Debugger extends ParserVisitor implements JexlInfo.Detail {
    /** Checks identifiers that contain spaces or punctuation
     * (but underscore, at-sign, sharp-sign and dollar).
     */
    protected static final Pattern QUOTED_IDENTIFIER =
            Pattern.compile("\\s|\\p{Punct}&&[^@#$_]");
    private static  boolean isLambdaExpr(final ASTJexlLambda lambda) {
        return lambda.jjtGetNumChildren() == 1 && !isStatement(lambda.jjtGetChild(0));
    }
    /**
     * Whether a node is a statement (vs an expression).
     * @param child the node
     * @return true if node is a statement
     */
    private static boolean isStatement(final JexlNode child) {
        return child instanceof ASTJexlScript
                || child instanceof ASTBlock
                || child instanceof ASTIfStatement
                || child instanceof ASTForeachStatement
                || child instanceof ASTTryStatement
                || child instanceof ASTWhileStatement
                || child instanceof ASTDoWhileStatement
                || child instanceof ASTAnnotation
                || child instanceof ASTThrowStatement;
    }
    /**
     * Whether a script or expression ends with a semicolumn.
     * @param cs the string
     * @return true if a semicolumn is the last non-whitespace character
     */
    private static boolean semicolTerminated(final CharSequence cs) {
        for(int i = cs.length() - 1; i >= 0; --i) {
            final char c = cs.charAt(i);
            if (c == ';') {
                return true;
            }
            if (!Character.isWhitespace(c)) {
                break;
            }
        }
        return false;
    }
    /**
     * Stringifies the pragmas.
     * @param builder where to stringify
     * @param pragmas the pragmas, may be null
     */
    private static void writePragmas(final StringBuilder builder, final Map<String, Object> pragmas) {
        if (pragmas != null) {
            for (final Map.Entry<String, Object> pragma : pragmas.entrySet()) {
                final String key = pragma.getKey();
                final Object value = pragma.getValue();
                final Set<Object> values = value instanceof Set<?>
                    ? (Set<Object>) value
                    : Collections.singleton(value);
                for (final Object pragmaValue : values) {
                    builder.append("#pragma ");
                    builder.append(key);
                    builder.append(' ');
                    builder.append(pragmaValue.toString());
                    builder.append('\n');
                }
            }
        }

    }
    /** The builder to compose messages. */
    protected final StringBuilder builder = new StringBuilder();
    /** The cause of the issue to debug. */
    protected JexlNode cause;
    /** The starting character location offset of the cause in the builder. */
    protected int start;
    /** The ending character location offset of the cause in the builder. */
    protected int end;
    /** The indentation level. */
    protected int indentLevel;

    /** Perform indentation?. */
    protected int indent = 2;

    /** Accept() relative depth. */
    protected int depth = Integer.MAX_VALUE;

    /** Arrow symbol. */
    protected String arrow = "->";

    /** EOL. */
    protected String lf = "\n";

    /** Pragmas out. */
    protected boolean outputPragmas;

    /**
     * Creates a Debugger.
     */
    public Debugger() {
        // nothing to initialize
    }

    /**
     * Checks if a child node is the cause to debug &amp; adds its representation to the rebuilt expression.
     * @param node the child node
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object accept(final JexlNode node, final Object data) {
        if (depth <= 0 && builder.length() > 0) {
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
        if (!isStatement(child) && !semicolTerminated(builder)) {
            builder.append(';');
            if (indent > 0) {
                builder.append(lf);
            } else {
                builder.append(' ');
            }
        }
        return value;
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

    /**
     * Checks if a terminal node is the cause to debug &amp; adds its representation to the rebuilt expression.
     * @param node  the child node
     * @param image the child node token image (optionally null)
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
     * Rebuilds an expression from a JEXL node.
     * @param node the node to rebuilt from
     * @return the rebuilt expression
     * @since 3.0
     */
    public String data(final JexlNode node) {
        start = 0;
        end = 0;
        indentLevel = 0;
        setArrowSymbol(node);
        if (node != null) {
            builder.setLength(0);
            cause = node;
            accept(node, null);
        }
        return builder.toString();
    }

    /**
     * Position the debugger on the root of an expression.
     * @param jscript the expression
     * @return true if the expression was a {@link Script} instance, false otherwise
     */
    public boolean debug(final JexlExpression jscript) {
        if (jscript instanceof Script) {
            final Script script = (Script) jscript;
            return debug(script.script);
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
        setArrowSymbol(node);
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
     * Position the debugger on the root of a script.
     * @param jscript the script
     * @return true if the script was a {@link Script} instance, false otherwise
     */
    public boolean debug(final JexlScript jscript) {
        if (jscript instanceof Script) {
            final Script script = (Script) jscript;
            return debug(script.script);
        }
        return false;
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
     * @return The end offset location of the cause in the expression
     */
    @Override
    public int end() {
        return end;
    }

    /**
     * Tries (hard) to find the features used to parse a node.
     * @param node the node
     * @return the features or null
     */
    protected JexlFeatures getFeatures(final JexlNode node) {
        JexlNode walk = node;
        while(walk != null) {
            if (walk instanceof ASTJexlScript) {
                final ASTJexlScript script = (ASTJexlScript) walk;
                return script.getFeatures();
            }
            walk = walk.jjtGetParent();
        }
        return null;
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
     * Checks if the children of a node using infix notation is the cause to debug, adds their representation to the
     * rebuilt expression.
     * @param node  the child node
     * @param infix the child node token
     * @param paren whether the child should be parenthesized
     * @param data  visitor pattern argument
     * @return visitor pattern value
     */
    protected Object infixChildren(final JexlNode node, final String infix, final boolean paren, final Object data) {
        final int num = node.jjtGetNumChildren();
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
     * Sets this debugger line-feed string.
     * @param lf the string used to delineate lines (usually "\" or "")
     * @return this debugger instance
     */
    public Debugger lineFeed(final String lf) {
        this.lf = lf;
        return this;
    }

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

    /**
     * Lets the debugger write out pragmas if any.
     * @param flag turn on or off
     * @return this debugger instance
     */
    public Debugger outputPragmas(final boolean flag) {
        this.outputPragmas = flag;
        return this;
    }

    /**
     * Postfix operators.
     * @param node a postfix operator
     * @param prefix the postfix
     * @param data visitor pattern argument
     * @return visitor pattern value
     */
    protected Object postfixChild(final JexlNode node, final String prefix, final Object data) {
        final boolean paren = node.jjtGetChild(0).jjtGetNumChildren() > 1;
        if (paren) {
            builder.append('(');
        }
        accept(node.jjtGetChild(0), data);
        if (paren) {
            builder.append(')');
        }
        builder.append(prefix);
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
     * Sets the arrow style (fat or thin) depending on features.
     * @param node the node to start seeking features from.
     */
    protected void setArrowSymbol(final JexlNode node) {
        final JexlFeatures features = getFeatures(node);
        if (features != null && features.supportsFatArrow() && !features.supportsThinArrow()) {
            arrow = "=>";
        } else {
            arrow = "->";
        }
    }

    /**
     * Sets the indentation level.
     * @param level the number of spaces for indentation, none if less or equal to zero
     */
    public void setIndentation(final int level) {
        indentation(level);
    }

    /**
     * @return The starting offset location of the cause in the expression
     */
    @Override
    public int start() {
        return start;
    }

    /**
     * @return The rebuilt expression
     */
    @Override
    public String toString() {
        return builder.toString();
    }

    @Override
    protected Object visit(final ASTAddNode node, final Object data) {
        return additiveNode(node, " + ", data);
    }

    @Override
    protected Object visit(final ASTAndNode node, final Object data) {
        return infixChildren(node, " && ", false, data);
    }

    @Override
    protected Object visit(final ASTAnnotatedStatement node, final Object data) {
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (i > 0) {
                builder.append(' ');
            }
            final JexlNode child = node.jjtGetChild(i);
            acceptStatement(child, data);
        }
        return data;
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
    protected Object visit(final ASTArrayAccess node, final Object data) {
        final int num = node.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            if (node.isSafeChild(i)) {
                builder.append('?');
            }
            builder.append('[');
            accept(node.jjtGetChild(i), data);
            builder.append(']');
        }
        return data;
    }

    @Override
    protected Object visit(final ASTArrayLiteral node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("[ ");
        if (num > 0) {
            if (depth <= 0) {
                builder.append("...");
            } else {
                accept(node.jjtGetChild(0), data);
                for (int i = 1; i < num; ++i) {
                    builder.append(", ");
                    accept(node.jjtGetChild(i), data);
                }
            }
        }
        builder.append(" ]");
        return data;
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
            builder.append(lf);
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
        if (!Character.isSpaceChar(builder.charAt(builder.length() - 1))) {
            builder.append(' ');
        }
        builder.append('}');
        return data;
    }

    @Override
    protected Object visit(final ASTBreak node, final Object data) {
        return check(node, "break", data);
    }

    @Override
    protected Object visit(final ASTConstructorNode node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("new");
        if (num > 0) {
            final JexlNode c0 = node.jjtGetChild(0);
            boolean first = true;
            if (c0 instanceof ASTQualifiedIdentifier) {
                builder.append(' ');
                accept(c0, data);
                builder.append('(');
            } else {
                first = false;
                builder.append('(');
                accept(c0, data);
            }
            for (int i = 1; i < num; ++i) {
                if (!first) {
                    builder.append(", ");
                }
                accept(node.jjtGetChild(i), data);
            }
        }
        builder.append(")");
        return data;
    }

    @Override
    protected Object visit(final ASTContinue node, final Object data) {
        return check(node, "continue", data);
    }

    @Override
    protected Object visit(final ASTDecrementGetNode node, final Object data) {
        return prefixChild(node, "--", data);
    }

    @Override
    protected Object visit(final ASTDefineVars node, final Object data) {
        final int num = node.jjtGetNumChildren();
        if (num > 0) {
            // var, let, const
            accept(node.jjtGetChild(0), data);
            for (int i = 1; i < num; ++i) {
                builder.append(", ");
                final JexlNode child = node.jjtGetChild(i);
                if (child instanceof ASTAssignment) {
                    final ASTAssignment assign = (ASTAssignment) child;
                    final int nc = assign.jjtGetNumChildren();
                    final ASTVar avar = (ASTVar) assign.jjtGetChild(0);
                    builder.append(avar.getName());
                    if (nc > 1) {
                        builder.append(" = ");
                        accept(assign.jjtGetChild(1), data);
                    }
                } else if (child instanceof ASTVar) {
                    final ASTVar avar = (ASTVar) child;
                    builder.append(avar.getName());
                } else {
                    // that's odd
                    accept(child, data);
                }
            }
        }
        return data;
    }

    @Override
    protected Object visit(final ASTDivNode node, final Object data) {
        return infixChildren(node, " / ", false, data);
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
    protected Object visit(final ASTEQSNode node, final Object data) {
        return infixChildren(node, " === ", false, data);
    }

    @Override
    protected Object visit(final ASTERNode node, final Object data) {
        return infixChildren(node, " =~ ", false, data);
    }

    @Override
    protected Object visit(final ASTEWNode node, final Object data) {
        return infixChildren(node, " =$ ", false, data);
    }

    @Override
    protected Object visit(final ASTExtendedLiteral node, final Object data) {
        builder.append("...");
        return data;
    }

    @Override
    protected Object visit(final ASTFalseNode node, final Object data) {
        return check(node, "false", data);
    }

    @Override
    protected Object visit(final ASTForeachStatement node, final Object data) {
        final int form = node.getLoopForm();
        builder.append("for(");
        final JexlNode body;
        if (form == 0) {
            // for( .. : ...)
            accept(node.jjtGetChild(0), data);
            builder.append(" : ");
            accept(node.jjtGetChild(1), data);
            builder.append(") ");
            body = node.jjtGetNumChildren() > 2? node.jjtGetChild(2) : null;
        } else {
            // for( .. ; ... ; ..)
            int nc = 0;
            // first child is var declaration(s)
            final JexlNode vars = (form & 1) != 0 ? node.jjtGetChild(nc++) : null;
            final JexlNode predicate = (form & 2) != 0 ? node.jjtGetChild(nc++) : null;
            // the loop step
            final JexlNode step = (form & 4) != 0 ? node.jjtGetChild(nc++) : null;
            // last child is body
            body = (form & 8) != 0 ? node.jjtGetChild(nc) : null;
            if (vars != null) {
                accept(vars, data);
            }
            builder.append("; ");
            if (predicate != null) {
                accept(predicate, data);
            }
            builder.append("; ");
            if (step != null) {
                accept(step, data);
            }
            builder.append(") ");
        }
        // the body
        if (body != null) {
            accept(body, data);
        } else {
            builder.append(';');
        }
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
    protected Object visit(final ASTGENode node, final Object data) {
        return infixChildren(node, " >= ", false, data);
    }

    @Override
    protected Object visit(final ASTGetDecrementNode node, final Object data) {
        return postfixChild(node, "--", data);
    }

    @Override
    protected Object visit(final ASTGetIncrementNode node, final Object data) {
        return postfixChild(node, "++", data);
    }

    @Override
    protected Object visit(final ASTGTNode node, final Object data) {
        return infixChildren(node, " > ", false, data);
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
    protected Object visit(final ASTIncrementGetNode node, final Object data) {
        return prefixChild(node, "++", data);
    }

    @Override
    protected Object visit(final ASTInstanceOf node, final Object data) {
        return infixChildren(node, " instanceof ", false, data);
    }

    @Override
    protected Object visit(final ASTJexlScript node, final Object arg) {
        if (outputPragmas) {
            writePragmas(builder, node.getPragmas());
        }
        Object data = arg;
        boolean named = false;
        // if lambda, produce parameters
        if (node instanceof ASTJexlLambda) {
            final ASTJexlLambda lambda = (ASTJexlLambda) node;
            final JexlNode parent = node.jjtGetParent();
            // use lambda syntax if not assigned
            final boolean expr = isLambdaExpr(lambda);
            named = node.jjtGetChild(0) instanceof ASTVar;
            final boolean assigned = parent instanceof ASTAssignment || named;
            if (assigned && !expr) {
                builder.append("function");
                if (named) {
                    final ASTVar avar = (ASTVar) node.jjtGetChild(0);
                    builder.append(' ');
                    builder.append(avar.getName());
                }
            }
            builder.append('(');
            final String[] params = lambda.getParameters();
            if (params != null ) {
                final Scope scope = lambda.getScope();
                final LexicalScope lexicalScope = lambda.getLexicalScope();
                for (int p = 0; p < params.length; ++p) {
                    if (p > 0) {
                        builder.append(", ");
                    }
                    final String param = params[p];
                    final int symbol = scope.getSymbol(param);
                    if (lexicalScope.isConstant(symbol)) {
                        builder.append("const ");
                    } else if (scope.isLexical(symbol)) {
                        builder.append("let ");
                    }
                    builder.append(visitParameter(param, data));
                }
            }
            builder.append(')');
            if (assigned && !expr) {
                // block follows
                builder.append(' ');
            } else {
                builder.append(arrow);
                // add a space if lambda expr otherwise block follows
                if (expr) {
                    builder.append(' ');
                }
            }
        }
        // no parameters or done with them
        final int num = node.jjtGetNumChildren();
        if (num == 1 && !(node instanceof ASTJexlLambda)) {
            data = accept(node.jjtGetChild(0), data);
        } else {
            for (int i = named? 1 : 0; i < num; ++i) {
                final JexlNode child = node.jjtGetChild(i);
                acceptStatement(child, data);
            }
        }
        return data;
    }

    @Override
    protected Object visit(final ASTJxltLiteral node, final Object data) {
        final String img = StringParser.escapeString(node.getLiteral(), '`');
        return check(node, img, data);
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
    protected Object visit(final ASTMapLiteral node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("{ ");
        if (num > 0) {
            if (depth <= 0) {
                builder.append("...");
            } else {
                accept(node.jjtGetChild(0), data);
                for (int i = 1; i < num; ++i) {
                    builder.append(",");
                    accept(node.jjtGetChild(i), data);
                }
            }
        } else {
            builder.append(':');
        }
        builder.append(" }");
        return data;
    }

    @Override
    protected Object visit(final ASTMethodNode node, final Object data) {
        final int num = node.jjtGetNumChildren();
        if (num == 2) {
            accept(node.jjtGetChild(0), data);
            if (depth <= 0) {
                builder.append("(...)");
            } else {
                accept(node.jjtGetChild(1), data);
            }
        }
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
    protected Object visit(final ASTNESNode node, final Object data) {
        return infixChildren(node, " !== ", false, data);
    }

    @Override
    protected Object visit(final ASTNEWNode node, final Object data) {
        return infixChildren(node, " !$ ", false, data);
    }

    @Override
    protected Object visit(final ASTNotInstanceOf node, final Object data) {
        return infixChildren(node, " !instanceof ", false, data);
    }

    @Override
    protected Object visit(final ASTNotNode node, final Object data) {
        builder.append("!");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTNRNode node, final Object data) {
        return infixChildren(node, " !~ ", false, data);
    }

    @Override
    protected Object visit(final ASTNSWNode node, final Object data) {
        return infixChildren(node, " !^ ", false, data);
    }

    @Override
    protected Object visit(final ASTNullLiteral node, final Object data) {
        check(node, "null", data);
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
    protected Object visit(final ASTNumberLiteral node, final Object data) {
        return check(node, node.toString(), data);
    }

    @Override
    protected Object visit(final ASTOrNode node, final Object data) {
        // need parenthesis if not in operator precedence order
        final boolean paren = node.jjtGetParent() instanceof ASTAndNode;
        return infixChildren(node, " || ", paren, data);
    }

    @Override
    protected Object visit(final ASTQualifiedIdentifier node, final Object data) {
        final String img = node.getName();
        return check(node, img, data);
    }

    @Override
    protected Object visit(final ASTRangeNode node, final Object data) {
        if (depth <= 0) {
            builder.append("( .. )");
            return data;
        }
        return infixChildren(node, " .. ", false, data);
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
    protected Object visit(final ASTRegexLiteral node, final Object data) {
        final String img = StringParser.escapeString(node.toString(), '/');
        return check(node, "~" + img, data);
    }

    @Override
    protected Object visit(final ASTReturnStatement node, final Object data) {
        builder.append("return ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTSetAddNode node, final Object data) {
        return infixChildren(node, " += ", false, data);
    }

    @Override
    protected Object visit(final ASTSetAndNode node, final Object data) {
        return infixChildren(node, " &= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetDivNode node, final Object data) {
        return infixChildren(node, " /= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetLiteral node, final Object data) {
        final int num = node.jjtGetNumChildren();
        builder.append("{ ");
        if (num > 0) {
            if (depth <= 0) {
                builder.append("...");
            } else {
                accept(node.jjtGetChild(0), data);
                for (int i = 1; i < num; ++i) {
                    builder.append(",");
                    accept(node.jjtGetChild(i), data);
                }
            }
        }
        builder.append(" }");
        return data;
    }

    @Override
    protected Object visit(final ASTSetModNode node, final Object data) {
        return infixChildren(node, " %= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetMultNode node, final Object data) {
        return infixChildren(node, " *= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetOrNode node, final Object data) {
        return infixChildren(node, " |= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetShiftLeftNode node, final Object data) {
        return infixChildren(node, " <<= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightNode node, final Object data) {
        return infixChildren(node, " >>= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetShiftRightUnsignedNode node, final Object data) {
        return infixChildren(node, " >>>= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetSubNode node, final Object data) {
        return infixChildren(node, " -= ", false, data);
    }

    @Override
    protected Object visit(final ASTSetXorNode node, final Object data) {
        return infixChildren(node, " ^= ", false, data);
    }

    @Override
    protected Object visit(final ASTShiftLeftNode node, final Object data) {
        return infixChildren(node, " << ", false, data);
    }

    @Override
    protected Object visit(final ASTShiftRightNode node, final Object data) {
        return infixChildren(node, " >> ", false, data);
    }

    @Override
    protected Object visit(final ASTShiftRightUnsignedNode node, final Object data) {
        return infixChildren(node, " >>> ", false, data);
    }

    @Override
    protected Object visit(final ASTSizeFunction node, final Object data) {
        builder.append("size ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTStringLiteral node, final Object data) {
        final String img = StringParser.escapeString(node.getLiteral(), '\'');
        return check(node, img, data);
    }

    @Override
    protected Object visit(final ASTSubNode node, final Object data) {
        return additiveNode(node, " - ", data);
    }

    @Override
    protected Object visit(final ASTSWNode node, final Object data) {
        return infixChildren(node, " =^ ", false, data);
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
    protected Object visit(final ASTThrowStatement node, final Object data) {
        builder.append("throw ");
        accept(node.jjtGetChild(0), data);
        return data;
    }

    @Override
    protected Object visit(final ASTTrueNode node, final Object data) {
        check(node, "true", data);
        return data;
    }

    @Override
    protected Object visit(final ASTTryResources node, final Object data) {
        final int tryBody = node.jjtGetNumChildren() - 1;
        builder.append('(');
        accept(node.jjtGetChild(0), data);
        for(int c = 1; c < tryBody; ++c) {
            builder.append("; ");
            accept(node.jjtGetChild(c), data);
        }
        builder.append(") ");
        accept(node.jjtGetChild(tryBody), data);
        return data;
    }

    @Override
    protected Object visit(final ASTTryStatement node, final Object data) {
        builder.append("try");
        int nc = 0;
        // try-body (with or without resources)
        accept(node.jjtGetChild(nc++), data);
        // catch-body
        if (node.hasCatchClause()) {
            builder.append("catch(");
            accept(node.jjtGetChild(nc++), data);
            builder.append(") ");
            accept(node.jjtGetChild(nc++), data);
        }
        // finally-body
        if (node.hasFinallyClause()) {
            builder.append(indent > 0? lf : ' ');
            builder.append("finally ");
            accept(node.jjtGetChild(nc), data);
        }
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
        if (node.isConstant()) {
            builder.append("const ");
        } else  if (node.isLexical()) {
            builder.append("let ");
        } else {
            builder.append("var ");
        }
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

    /**
     * A pseudo visitor for parameters.
     * @param p the parameter name
     * @param data the visitor argument
     * @return the parameter name to use
     */
    protected String visitParameter(final String p, final Object data) {
        return p;
    }
}
