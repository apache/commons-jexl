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

import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.internal.TemplateEngine.CompositeExpression;
import org.apache.commons.jexl3.internal.TemplateEngine.ConstantExpression;
import org.apache.commons.jexl3.internal.TemplateEngine.DeferredExpression;
import org.apache.commons.jexl3.internal.TemplateEngine.ImmediateExpression;
import org.apache.commons.jexl3.internal.TemplateEngine.NestedExpression;
import org.apache.commons.jexl3.internal.TemplateEngine.TemplateExpression;

import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * A visitor for templates.
 * <p>A friend (ala C++) of template engine.
 */
public class TemplateDebugger extends Debugger {
    /** The outer script. */
    private ASTJexlScript script;
    /** The expressions called by the script through jexl:print. */
    private TemplateExpression[] exprs;

    /**
     * Default ctor.
     */
    public TemplateDebugger() {
    }

    @Override
    public void reset() {
        super.reset();
        // so we can use it more than one time
        exprs = null;
        script = null;
    }

    /**
     * Position the debugger on the root of a template expression.
     * @param je the expression
     * @return true if the expression was a {@link TemplateExpression} instance, false otherwise
     */
    public boolean debug(final JxltEngine.Expression je) {
        if (je instanceof TemplateExpression) {
            final TemplateEngine.TemplateExpression te = (TemplateEngine.TemplateExpression) je;
            return visit(te, this) != null;
        }
        return false;
    }

    /**
     * Position the debugger on the root of a template script.
     * @param jt the template
     * @return true if the template was a {@link TemplateScript} instance, false otherwise
     */
    public boolean debug(final JxltEngine.Template jt) {
        if (!(jt instanceof TemplateScript)) {
            return false;
        }
        final TemplateScript ts = (TemplateScript) jt;
        // ensure expr is not null for templates
        this.exprs = ts.getExpressions() == null? new TemplateExpression[0] : ts.getExpressions();
        this.script = ts.getScript();
        start = 0;
        end = 0;
        indentLevel = 0;
        builder.setLength(0);
        cause = script;
        final int num = script.jjtGetNumChildren();
        for (int i = 0; i < num; ++i) {
            final JexlNode child = script.jjtGetChild(i);
            acceptStatement(child, null);
        }
        // the last line
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
        end = builder.length();
        return end > 0;
    }


    @Override
    protected Object visit(final ASTBlock node, final Object data) {
        // if not really a template, must use super impl
        if (exprs == null) {
            return super.visit(node, data);
        }
        // open the block
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
        // before we close this block node, $$ might be needed
        newJexlLine();
        if (indent > 0) {
            indentLevel -= 1;
            for (int i = 0; i < indentLevel; ++i) {
                for(int s = 0; s < indent; ++s) {
                    builder.append(' ');
                }
            }
        }
        builder.append('}');
        // closed the block
        return data;
    }

    @Override
    protected Object acceptStatement(final JexlNode child, final Object data) {
        // if not really a template, must use super impl
        if (exprs == null) {
            return super.acceptStatement(child, data);
        }
        final TemplateExpression te = getPrintStatement(child);
        if (te != null) {
            // if statement is a jexl:print(...), may need to prepend '\n'
            newJxltLine();
            return visit(te, data);
        }
        // if statement is not a jexl:print(...), need to prepend '$$'
        newJexlLine();
        return super.acceptStatement(child, data);
    }

    /**
     * In a template, any statement that is not 'jexl:print(n)' must be prefixed by "$$".
     * @param child the node to check
     * @return the expression number or -1 if the node is not a jexl:print
     */
    private TemplateExpression getPrintStatement(final JexlNode child) {
        if (exprs != null && child instanceof ASTFunctionNode) {
            final ASTFunctionNode node = (ASTFunctionNode) child;
            final ASTIdentifier ns = (ASTIdentifier) node.jjtGetChild(0);
            final JexlNode args = node.jjtGetChild(1);
            if ("jexl".equals(ns.getNamespace())
                && "print".equals(ns.getName())
                && args.jjtGetNumChildren() == 1
                && args.jjtGetChild(0) instanceof ASTNumberLiteral) {
                final ASTNumberLiteral exprn = (ASTNumberLiteral) args.jjtGetChild(0);
                final int n = exprn.getLiteral().intValue();
                if (n >= 0 && n < exprs.length) {
                    return exprs[n];
                }
            }
        }
        return null;
    }

    /**
     * Insert $$ and \n when needed.
     */
    private void newJexlLine() {
        final int length = builder.length();
        if (length == 0) {
            builder.append("$$ ");
        } else {
            for (int i = length - 1; i >= 0; --i) {
                final char c = builder.charAt(i);
                switch (c) {
                    case '\n':
                        builder.append("$$ ");
                        return;
                    case '}':
                        builder.append("\n$$ ");
                        return;
                    case ' ':
                    case ';':
                        return;
                    default: // continue
                }
            }
        }
    }

    /**
     * Insert \n when needed.
     */
    private void newJxltLine() {
        final int length = builder.length();
        for (int i = length - 1; i >= 0; --i) {
            final char c = builder.charAt(i);
            switch (c) {
                case '\n':
                case ';':
                    return;
                case '}':
                    builder.append('\n');
                    return;
                default: // continue
            }
        }
    }

    /**
     * Visit a template expression.
     * @param expr the constant expression
     * @param data the visitor argument
     * @return the visitor argument
     */
    private Object visit(final TemplateExpression expr, final Object data) {
        Object r;
        switch (expr.getType()) {
            case CONSTANT:
                r = visit((ConstantExpression) expr, data);
                break;
            case IMMEDIATE:
                r = visit((ImmediateExpression) expr, data);
                break;
            case DEFERRED:
                r = visit((DeferredExpression) expr, data);
                break;
            case NESTED:
                r = visit((NestedExpression) expr, data);
                break;
            case COMPOSITE:
                r = visit((CompositeExpression) expr, data);
                break;
            default:
                r = null;
        }
        return r;
    }

    /**
     * Visit a constant expression.
     * @param expr the constant expression
     * @param data the visitor argument
     * @return the visitor argument
     */
    private Object visit(final ConstantExpression expr, final Object data) {
        expr.asString(builder);
        return data;
    }

    /**
     * Visit an immediate expression.
     * @param expr the immediate expression
     * @param data the visitor argument
     * @return the visitor argument
     */
    private Object visit(final ImmediateExpression expr, final Object data) {
        builder.append(expr.isImmediate() ? '$' : '#');
        builder.append('{');
        super.accept(expr.node, data);
        builder.append('}');
        return data;
    }

    /**
     * Visit a deferred expression.
     * @param expr the deferred expression
     * @param data the visitor argument
     * @return the visitor argument
     */
    private Object visit(final DeferredExpression expr, final Object data) {
        builder.append(expr.isImmediate() ? '$' : '#');
        builder.append('{');
        super.accept(expr.node, data);
        builder.append('}');
        return data;
    }

    /**
     * Visit a nested expression.
     * @param expr the nested expression
     * @param data the visitor argument
     * @return the visitor argument
     */
    private Object visit(final NestedExpression expr, final Object data) {
        super.accept(expr.node, data);
        return data;
    }
    /**
     * Visit a composite expression.
     * @param expr the composite expression
     * @param data the visitor argument
     * @return the visitor argument
     */
    private Object visit(final CompositeExpression expr, final Object data) {
        for (final TemplateExpression ce : expr.exprs) {
            visit(ce, data);
        }
        return data;
    }

}
