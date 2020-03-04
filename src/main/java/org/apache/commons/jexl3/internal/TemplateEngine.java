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

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.StringParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A JxltEngine implementation.
 * @since 3.0
 */
public final class TemplateEngine extends JxltEngine {
    /** The TemplateExpression cache. */
    private final SoftCache<String, TemplateExpression> cache;
    /** The JEXL engine instance. */
    private final Engine jexl;
    /** The first character for immediate expressions. */
    private final char immediateChar;
    /** The first character for deferred expressions. */
    private final char deferredChar;
    /** Whether expressions can use JEXL script or only expressions (ie, no for, var, etc). */
    private boolean noscript = true;

    /**
     * Creates a new instance of {@link JxltEngine} creating a local cache.
     * @param aJexl     the JexlEngine to use.
     * @param noScript  whether this engine only allows JEXL expressions or scripts
     * @param cacheSize the number of expressions in this cache, default is 256
     * @param immediate the immediate template expression character, default is '$'
     * @param deferred  the deferred template expression character, default is '#'
     */
    public TemplateEngine(Engine aJexl, boolean noScript, int cacheSize, char immediate, char deferred) {
        this.jexl = aJexl;
        this.cache = new SoftCache<String, TemplateExpression>(cacheSize);
        immediateChar = immediate;
        deferredChar = deferred;
        noscript = noScript;
    }

    /**
     * @return the immediate character
     */
    char getImmediateChar() {
        return immediateChar;
    }

    /**
     * @return the deferred character
     */
    char getDeferredChar() {
        return deferredChar;
    }

    /**
     * Types of expressions.
     * Each instance carries a counter index per (composite sub-) template expression type.
     * @see ExpressionBuilder
     */
    enum ExpressionType {
        /** Constant TemplateExpression, count index 0. */
        CONSTANT(0),
        /** Immediate TemplateExpression, count index 1. */
        IMMEDIATE(1),
        /** Deferred TemplateExpression, count index 2. */
        DEFERRED(2),
        /** Nested (which are deferred) expressions, count
         * index 2. */
        NESTED(2),
        /** Composite expressions are not counted, index -1. */
        COMPOSITE(-1);
        /** The index in arrays of TemplateExpression counters for composite expressions. */
        private final int index;

        /**
         * Creates an ExpressionType.
         * @param idx the index for this type in counters arrays.
         */
        ExpressionType(int idx) {
            this.index = idx;
        }
    }

    /**
     * A helper class to build expressions.
     * Keeps count of sub-expressions by type.
     */
    static final class ExpressionBuilder {
        /** Per TemplateExpression type counters. */
        private final int[] counts;
        /** The list of expressions. */
        private final ArrayList<TemplateExpression> expressions;

        /**
         * Creates a builder.
         * @param size the initial TemplateExpression array size
         */
        private ExpressionBuilder(int size) {
            counts = new int[]{0, 0, 0};
            expressions = new ArrayList<TemplateExpression>(size <= 0 ? 3 : size);
        }

        /**
         * Adds an TemplateExpression to the list of expressions, maintain per-type counts.
         * @param expr the TemplateExpression to add
         */
        private void add(TemplateExpression expr) {
            counts[expr.getType().index] += 1;
            expressions.add(expr);
        }

        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }

        /**
         * Base for to-string.
         * @param error the builder to fill
         * @return the builder
         */
        private StringBuilder toString(StringBuilder error) {
            error.append("exprs{");
            error.append(expressions.size());
            error.append(", constant:");
            error.append(counts[ExpressionType.CONSTANT.index]);
            error.append(", immediate:");
            error.append(counts[ExpressionType.IMMEDIATE.index]);
            error.append(", deferred:");
            error.append(counts[ExpressionType.DEFERRED.index]);
            error.append("}");
            return error;
        }

        /**
         * Builds an TemplateExpression from a source, performs checks.
         * @param el     the unified el instance
         * @param source the source TemplateExpression
         * @return an TemplateExpression
         */
        private TemplateExpression build(TemplateEngine el, TemplateExpression source) {
            int sum = 0;
            for (int count : counts) {
                sum += count;
            }
            if (expressions.size() != sum) {
                StringBuilder error = new StringBuilder("parsing algorithm error: ");
                throw new IllegalStateException(toString(error).toString());
            }
            // if only one sub-expr, no need to create a composite
            if (expressions.size() == 1) {
                return expressions.get(0);
            } else {
                return el.new CompositeExpression(counts, expressions, source);
            }
        }
    }

    /**
     * Gets the JexlEngine underlying this JxltEngine.
     * @return the JexlEngine
     */
    @Override
    public Engine getEngine() {
        return jexl;
    }

    /**
     * Clears the cache.
     */
    @Override
    public void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * The abstract base class for all unified expressions, immediate '${...}' and deferred '#{...}'.
     */
    abstract class TemplateExpression implements Expression {
        /** The source of this template expression(see {@link TemplateEngine.TemplateExpression#prepare}). */
        protected final TemplateExpression source;

        /**
         * Creates an TemplateExpression.
         * @param src the source TemplateExpression if any
         */
        TemplateExpression(TemplateExpression src) {
            this.source = src != null ? src : this;
        }

        @Override
        public boolean isImmediate() {
            return true;
        }

        @Override
        public final boolean isDeferred() {
            return !isImmediate();
        }

        /**
         * Gets this TemplateExpression type.
         * @return its type
         */
        abstract ExpressionType getType();

        /** @return the info */
        JexlInfo getInfo() {
            return null;
        }

        @Override
        public final String toString() {
            StringBuilder strb = new StringBuilder();
            asString(strb);
            if (source != this) {
                strb.append(" /*= ");
                strb.append(source.toString());
                strb.append(" */");
            }
            return strb.toString();
        }

        @Override
        public String asString() {
            StringBuilder strb = new StringBuilder();
            asString(strb);
            return strb.toString();
        }

        @Override
        public Set<List<String>> getVariables() {
            return Collections.emptySet();
        }

        @Override
        public final TemplateExpression getSource() {
            return source;
        }

        /**
         * Fills up the list of variables accessed by this unified expression.
         * @param collector the variable collector
         */
        protected void getVariables(Engine.VarCollector collector) {
            // nothing to do
        }

        @Override
        public final TemplateExpression prepare(JexlContext context) {
                return prepare(null, context);
        }

        /**
         * Prepares this expression.
         * @param frame the frame storing parameters and local variables
         * @param context the context storing global variables
         * @return the expression value
         * @throws JexlException
         */
        protected final TemplateExpression prepare(Frame frame, JexlContext context) {
            try {
                Interpreter interpreter = jexl.createInterpreter(context, frame, jexl.options(context));
                return prepare(interpreter);
            } catch (JexlException xjexl) {
                JexlException xuel = createException(xjexl.getInfo(), "prepare", this, xjexl);
                if (jexl.isSilent()) {
                    jexl.logger.warn(xuel.getMessage(), xuel.getCause());
                    return null;
                }
                throw xuel;
            }
        }

        /**
         * Prepares a sub-expression for interpretation.
         * @param interpreter a JEXL interpreter
         * @return a prepared unified expression
         * @throws JexlException (only for nested and composite)
         */
        protected TemplateExpression prepare(Interpreter interpreter) {
            return this;
        }

        @Override
        public final Object evaluate(JexlContext context) {
            return evaluate(null, context);
        }

        /**
         * The options to use during evaluation.
         * @param context the context
         * @return the options
         */
        protected JexlOptions options(JexlContext context) {
            return jexl.options(null, context);
        }
        
        /**
         * Evaluates this expression.
         * @param frame the frame storing parameters and local variables
         * @param context the context storing global variables
         * @return the expression value
         * @throws JexlException
         */
        protected final Object evaluate(Frame frame, JexlContext context) {
            try {
                JexlOptions options = options(context);
                TemplateInterpreter.Arguments args = new TemplateInterpreter
                        .Arguments(jexl)
                        .context(context)
                        .options(options)
                        .frame(frame);
                Interpreter interpreter = new TemplateInterpreter(args);
                return evaluate(interpreter);
            } catch (JexlException xjexl) {
                JexlException xuel = createException(xjexl.getInfo(), "evaluate", this, xjexl);
                if (jexl.isSilent()) {
                    jexl.logger.warn(xuel.getMessage(), xuel.getCause());
                    return null;
                }
                throw xuel;
            }
        }

        /**
         * Interprets a sub-expression.
         * @param interpreter a JEXL interpreter
         * @return the result of interpretation
         * @throws JexlException (only for nested and composite)
         */
        protected abstract Object evaluate(Interpreter interpreter);

    }

    /** A constant unified expression. */
    class ConstantExpression extends TemplateExpression {
        /** The constant held by this unified expression. */
        private final Object value;

        /**
         * Creates a constant unified expression.
         * <p>
         * If the wrapped constant is a string, it is treated
         * as a JEXL strings with respect to escaping.
         * </p>
         * @param val    the constant value
         * @param source the source TemplateExpression if any
         */
        ConstantExpression(Object val, TemplateExpression source) {
            super(source);
            if (val == null) {
                throw new NullPointerException("constant can not be null");
            }
            if (val instanceof String) {
                val = StringParser.buildString((String) val, false);
            }
            this.value = val;
        }

        @Override
        ExpressionType getType() {
            return ExpressionType.CONSTANT;
        }

        @Override
        public StringBuilder asString(StringBuilder strb) {
            if (value != null) {
                strb.append(value.toString());
            }
            return strb;
        }

        @Override
        protected Object evaluate(Interpreter interpreter) {
            return value;
        }
    }

    /** The base for JEXL based unified expressions. */
    abstract class JexlBasedExpression extends TemplateExpression {
        /** The JEXL string for this unified expression. */
        protected final CharSequence expr;
        /** The JEXL node for this unified expression. */
        protected final JexlNode node;

        /**
         * Creates a JEXL interpretable unified expression.
         * @param theExpr   the unified expression as a string
         * @param theNode   the unified expression as an AST
         * @param theSource the source unified expression if any
         */
        protected JexlBasedExpression(CharSequence theExpr, JexlNode theNode, TemplateExpression theSource) {
            super(theSource);
            this.expr = theExpr;
            this.node = theNode;
        }

        @Override
        public StringBuilder asString(StringBuilder strb) {
            strb.append(isImmediate() ? immediateChar : deferredChar);
            strb.append("{");
            strb.append(expr);
            strb.append("}");
            return strb;
        }
        
        @Override
        protected JexlOptions options(JexlContext context) {
            return jexl.options(node instanceof ASTJexlScript? (ASTJexlScript) node : null, context);
        }

        @Override
        protected Object evaluate(Interpreter interpreter) {
            return interpreter.interpret(node);
        }

        @Override
        public Set<List<String>> getVariables() {
            Engine.VarCollector collector = jexl.varCollector();
            getVariables(collector);
            return collector.collected();
        }

        @Override
        protected void getVariables(Engine.VarCollector collector) {
            jexl.getVariables(node instanceof ASTJexlScript? (ASTJexlScript) node : null, node, collector);
        }

        @Override
        JexlInfo getInfo() {
            return node.jexlInfo();
        }
    }

    /** An immediate unified expression: ${jexl}. */
    class ImmediateExpression extends JexlBasedExpression {
        /**
         * Creates an immediate unified expression.
         * @param expr   the unified expression as a string
         * @param node   the unified expression as an AST
         * @param source the source unified expression if any
         */
        ImmediateExpression(CharSequence expr, JexlNode node, TemplateExpression source) {
            super(expr, node, source);
        }

        @Override
        ExpressionType getType() {
            return ExpressionType.IMMEDIATE;
        }

        @Override
        protected TemplateExpression prepare(Interpreter interpreter) {
            // evaluate immediate as constant
            Object value = evaluate(interpreter);
            return value != null ? new ConstantExpression(value, source) : null;
        }
    }

    /** A deferred unified expression: #{jexl}. */
    class DeferredExpression extends JexlBasedExpression {
        /**
         * Creates a deferred unified expression.
         * @param expr   the unified expression as a string
         * @param node   the unified expression as an AST
         * @param source the source unified expression if any
         */
        DeferredExpression(CharSequence expr, JexlNode node, TemplateExpression source) {
            super(expr, node, source);
        }

        @Override
        public boolean isImmediate() {
            return false;
        }

        @Override
        ExpressionType getType() {
            return ExpressionType.DEFERRED;
        }

        @Override
        protected TemplateExpression prepare(Interpreter interpreter) {
            return new ImmediateExpression(expr, node, source);
        }

        @Override
        protected void getVariables(Engine.VarCollector collector) {
            // noop
        }
    }

    /**
     * An immediate unified expression nested into a deferred unified expression.
     * #{...${jexl}...}
     * Note that the deferred syntax is JEXL's.
     */
    class NestedExpression extends JexlBasedExpression {
        /**
         * Creates a nested unified expression.
         * @param expr   the unified expression as a string
         * @param node   the unified expression as an AST
         * @param source the source unified expression if any
         */
        NestedExpression(CharSequence expr, JexlNode node, TemplateExpression source) {
            super(expr, node, source);
            if (this.source != this) {
                throw new IllegalArgumentException("Nested TemplateExpression can not have a source");
            }
        }

        @Override
        public StringBuilder asString(StringBuilder strb) {
            strb.append(expr);
            return strb;
        }

        @Override
        public boolean isImmediate() {
            return false;
        }

        @Override
        ExpressionType getType() {
            return ExpressionType.NESTED;
        }

        @Override
        protected TemplateExpression prepare(Interpreter interpreter) {
            String value = interpreter.interpret(node).toString();
            JexlNode dnode = jexl.parse(node.jexlInfo(), noscript, value, null);
            return new ImmediateExpression(value, dnode, this);
        }

        @Override
        protected Object evaluate(Interpreter interpreter) {
            return prepare(interpreter).evaluate(interpreter);
        }
    }

    /** A composite unified expression: "... ${...} ... #{...} ...". */
    class CompositeExpression extends TemplateExpression {
        /** Bit encoded (deferred count > 0) bit 1, (immediate count > 0) bit 0. */
        private final int meta;
        /** The list of sub-expression resulting from parsing. */
        protected final TemplateExpression[] exprs;

        /**
         * Creates a composite expression.
         * @param counters counters of expressions per type
         * @param list     the sub-expressions
         * @param src      the source for this expresion if any
         */
        CompositeExpression(int[] counters, ArrayList<TemplateExpression> list, TemplateExpression src) {
            super(src);
            this.exprs = list.toArray(new TemplateExpression[list.size()]);
            this.meta = (counters[ExpressionType.DEFERRED.index] > 0 ? 2 : 0)
                    | (counters[ExpressionType.IMMEDIATE.index] > 0 ? 1 : 0);
        }

        @Override
        public boolean isImmediate() {
            // immediate if no deferred
            return (meta & 2) == 0;
        }

        @Override
        ExpressionType getType() {
            return ExpressionType.COMPOSITE;
        }

        @Override
        public StringBuilder asString(StringBuilder strb) {
            for (TemplateExpression e : exprs) {
                e.asString(strb);
            }
            return strb;
        }

        @Override
        public Set<List<String>> getVariables() {
            Engine.VarCollector collector = jexl.varCollector();
            for (TemplateExpression expr : exprs) {
                expr.getVariables(collector);
            }
            return collector.collected();
        }

        /**
         * Fills up the list of variables accessed by this unified expression.
         * @param collector the variable collector
         */
        @Override
        protected void getVariables(Engine.VarCollector collector) {
            for (TemplateExpression expr : exprs) {
                expr.getVariables(collector);
            }
        }

        @Override
        protected TemplateExpression prepare(Interpreter interpreter) {
            // if this composite is not its own source, it is already prepared
            if (source != this) {
                return this;
            }
            // we need to prepare all sub-expressions
            final int size = exprs.length;
            final ExpressionBuilder builder = new ExpressionBuilder(size);
            // tracking whether prepare will return a different expression
            boolean eq = true;
            for (int e = 0; e < size; ++e) {
                TemplateExpression expr = exprs[e];
                TemplateExpression prepared = expr.prepare(interpreter);
                // add it if not null
                if (prepared != null) {
                    builder.add(prepared);
                }
                // keep track of TemplateExpression equivalence
                eq &= expr == prepared;
            }
            return eq ? this : builder.build(TemplateEngine.this, this);
        }

        @Override
        protected Object evaluate(Interpreter interpreter) {
            final int size = exprs.length;
            Object value;
            // common case: evaluate all expressions & concatenate them as a string
            StringBuilder strb = new StringBuilder();
            for (int e = 0; e < size; ++e) {
                value = exprs[e].evaluate(interpreter);
                if (value != null) {
                    strb.append(value.toString());
                }
            }
            value = strb.toString();
            return value;
        }
    }


    @Override
    public JxltEngine.Expression createExpression(JexlInfo info, String expression) {
        if (info == null) {
            info = jexl.createInfo();
        }
        Exception xuel = null;
        TemplateExpression stmt = null;
        try {
            stmt = cache.get(expression);
            if (stmt == null) {
                stmt = parseExpression(info, expression, null);
                cache.put(expression, stmt);
            }
        } catch (JexlException xjexl) {
            xuel = new Exception(xjexl.getInfo(), "failed to parse '" + expression + "'", xjexl);
        }
        if (xuel != null) {
            if (jexl.isSilent()) {
                jexl.logger.warn(xuel.getMessage(), xuel.getCause());
                stmt = null;
            } else {
                throw xuel;
            }
        }
        return stmt;
    }

    /**
     * Creates a JxltEngine.Exception from a JexlException.
     * @param info   the source info
     * @param action createExpression, prepare, evaluate
     * @param expr   the template expression
     * @param xany   the exception
     * @return an exception containing an explicit error message
     */
    static Exception createException(JexlInfo info, String action, TemplateExpression expr, java.lang.Exception xany) {
        StringBuilder strb = new StringBuilder("failed to ");
        strb.append(action);
        if (expr != null) {
            strb.append(" '");
            strb.append(expr.toString());
            strb.append("'");
        }
        Throwable cause = xany.getCause();
        if (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null) {
                strb.append(", ");
                strb.append(causeMsg);
            }
        }
        return new Exception(info, strb.toString(), xany);
    }

    /** The different parsing states. */
    private enum ParseState {
        /** Parsing a constant. */
        CONST,
        /** Parsing after $ . */
        IMMEDIATE0,
        /** Parsing after # . */
        DEFERRED0,
        /** Parsing after ${ . */
        IMMEDIATE1,
        /** Parsing after #{ . */
        DEFERRED1,
        /** Parsing after \ . */
        ESCAPE
    }

    /**
     * Helper for expression dealing with embedded strings.
     * @param strb the expression buffer to copy characters into
     * @param expr the source
     * @param position the offset into the source
     * @param c the separator character
     * @return the new position to read the source from 
     */
    private static int append(StringBuilder strb, CharSequence expr, int position, char c) {
        strb.append(c);
        if (c != '"' && c != '\'') {
            return position;
        } 
        // read thru strings
        int end = expr.length();
        boolean escape= false;
        int index = position + 1;
        for (; index < end; ++index) {
            char ec = expr.charAt(index);
            strb.append(ec);
            if (ec == '\\') {
                escape = !escape;
            } else if (escape) {
                escape = false;
            } else if (ec == c) {
                break;
            }
        }
        return index;
    }
    
    /**
     * Parses a unified expression.
     * @param info  the source info
     * @param expr  the string expression
     * @param scope the template scope
     * @return the unified expression instance
     * @throws JexlException if an error occur during parsing
     */
    TemplateExpression parseExpression(JexlInfo info, String expr, Scope scope) {  // CSOFF: MethodLength
        final int size = expr.length();
        final ExpressionBuilder builder = new ExpressionBuilder(0);
        final StringBuilder strb = new StringBuilder(size);
        ParseState state = ParseState.CONST;
        int immediate1 = 0;
        int deferred1 = 0;
        int inner1 = 0;
        boolean nested = false;
        int inested = -1;
        int lineno = info.getLine();
        for (int column = 0; column < size; ++column) {
            char c = expr.charAt(column);
            switch (state) {
                default: // in case we ever add new unified expresssion type
                    throw new UnsupportedOperationException("unexpected unified expression type");
                case CONST:
                    if (c == immediateChar) {
                        state = ParseState.IMMEDIATE0;
                    } else if (c == deferredChar) {
                        inested = column;
                        state = ParseState.DEFERRED0;
                    } else if (c == '\\') {
                        state = ParseState.ESCAPE;
                    } else {
                        // do buildup expr
                        strb.append(c);
                    }
                    break;
                case IMMEDIATE0: // $
                    if (c == '{') {
                        state = ParseState.IMMEDIATE1;
                        // if chars in buffer, create constant
                        if (strb.length() > 0) {
                            TemplateExpression cexpr = new ConstantExpression(strb.toString(), null);
                            builder.add(cexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                        }
                    } else {
                        // revert to CONST
                        strb.append(immediateChar);
                        strb.append(c);
                        state = ParseState.CONST;
                    }
                    break;
                case DEFERRED0: // #
                    if (c == '{') {
                        state = ParseState.DEFERRED1;
                        // if chars in buffer, create constant
                        if (strb.length() > 0) {
                            TemplateExpression cexpr = new ConstantExpression(strb.toString(), null);
                            builder.add(cexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                        }
                    } else {
                        // revert to CONST
                        strb.append(deferredChar);
                        strb.append(c);
                        state = ParseState.CONST;
                    }
                    break;
                case IMMEDIATE1: // ${...
                    if (c == '}') {
                        if (immediate1 > 0) {
                            immediate1 -= 1;
                            strb.append(c);
                        } else {
                            // materialize the immediate expr
                            String src = strb.toString();
                            TemplateExpression iexpr = new ImmediateExpression(
                                    src,
                                    jexl.parse(info.at(lineno, column), noscript, src, scope),
                                    null);
                            builder.add(iexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                            state = ParseState.CONST;
                        }
                    } else {
                        if (c == '{') {
                            immediate1 += 1;
                        }
                        // do buildup expr
                        column = append(strb, expr, column, c);
                    }
                    break;
                case DEFERRED1: // #{...
                    // skip inner strings (for '}')
                    if (c == '"' || c == '\'') {
                        strb.append(c);
                        column = StringParser.readString(strb, expr, column + 1, c);
                        continue;
                    }
                    // nested immediate in deferred; need to balance count of '{' & '}'
                    if (c == '{') {
                        if (expr.charAt(column - 1) == immediateChar) {
                            inner1 += 1;
                            strb.deleteCharAt(strb.length() - 1);
                            nested = true;
                        } else {
                            deferred1 += 1;
                            strb.append(c);
                        }
                        continue;
                    }
                    // closing '}'
                    if (c == '}') {
                        // balance nested immediate
                        if (deferred1 > 0) {
                            deferred1 -= 1;
                            strb.append(c);
                        } else if (inner1 > 0) {
                            inner1 -= 1;
                        } else  {
                            // materialize the nested/deferred expr
                            String src = strb.toString();
                            TemplateExpression dexpr;
                            if (nested) {
                                dexpr = new NestedExpression(
                                        expr.substring(inested, column + 1),
                                        jexl.parse(info.at(lineno, column), noscript, src, scope),
                                        null);
                            } else {
                                dexpr = new DeferredExpression(
                                        strb.toString(),
                                        jexl.parse(info.at(lineno, column), noscript, src, scope),
                                        null);
                            }
                            builder.add(dexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                            nested = false;
                            state = ParseState.CONST;
                        }
                    } else {
                        // do buildup expr
                        column = append(strb, expr, column, c);
                    }
                    break;
                case ESCAPE:
                    if (c == deferredChar) {
                        strb.append(deferredChar);
                    } else if (c == immediateChar) {
                        strb.append(immediateChar);
                    } else {
                        strb.append('\\');
                        strb.append(c);
                    }
                    state = ParseState.CONST;
            }
            if (c == '\n') {
                lineno += 1;
            }
        }
        // we should be in that state
        if (state != ParseState.CONST) {
            // otherwise, we ended a line with a \, $ or #
            switch (state) {
                case ESCAPE:
                    strb.append('\\');
                    strb.append('\\');
                    break;
                case DEFERRED0:
                    strb.append(deferredChar);
                    break;
                case IMMEDIATE0:
                    strb.append(immediateChar);
                    break;
                default:
                    throw new Exception(info.at(lineno, 0), "malformed expression: " + expr, null);
            }
        }
        // if any chars were buffered, add them as a constant
        if (strb.length() > 0) {
            TemplateExpression cexpr = new ConstantExpression(strb.toString(), null);
            builder.add(cexpr);
        }
        return builder.build(this, null);
    }

    /**
     * The enum capturing the difference between verbatim and code source fragments.
     */
    enum BlockType {
        /** Block is to be output "as is" but may be a unified expression. */
        VERBATIM,
        /** Block is a directive, ie a fragment of JEXL code. */
        DIRECTIVE
    }

    /**
     * Abstract the source fragments, verbatim or immediate typed text blocks.
     */
    static final class Block {
        /** The type of block, verbatim or directive. */
        private final BlockType type;
        /** The block start line info. */
        private final int line;
        /** The actual content. */
        private final String body;

        /**
         * Creates a new block.
         * @param theType  the block type
         * @param theLine  the line number
         * @param theBlock the content
         */
        Block(BlockType theType, int theLine, String theBlock) {
            type = theType;
            line = theLine;
            body = theBlock;
        }
        
        /**
         * @return type
         */
        BlockType getType() {
            return type;
        }

        /**
         * @return line
         */
        int getLine() {
            return line;
        }

        /**
         * @return body
         */
        String getBody() {
            return body;
        }

        @Override
        public String toString() {
            if (BlockType.VERBATIM.equals(type)) {
                return body;
            } else {
                // CHECKSTYLE:OFF
                StringBuilder strb = new StringBuilder(64); // CSOFF: MagicNumber
                // CHECKSTYLE:ON
                Iterator<CharSequence> lines = readLines(new StringReader(body));
                while (lines.hasNext()) {
                    strb.append("$$").append(lines.next());
                }
                return strb.toString();
            }
        }

        /**
         * Appends this block string representation to a builder.
         * @param strb   the string builder to append to
         * @param prefix the line prefix (immediate or deferred)
         */
        protected void toString(StringBuilder strb, String prefix) {
            if (BlockType.VERBATIM.equals(type)) {
                strb.append(body);
            } else {
                Iterator<CharSequence> lines = readLines(new StringReader(body));
                while (lines.hasNext()) {
                    strb.append(prefix).append(lines.next());
                }
            }
        }
    }

    /**
     * Whether a sequence starts with a given set of characters (following spaces).
     * <p>Space characters at beginning of line before the pattern are discarded.</p>
     * @param sequence the sequence
     * @param pattern  the pattern to match at start of sequence
     * @return the first position after end of pattern if it matches, -1 otherwise
     */
    protected int startsWith(CharSequence sequence, CharSequence pattern) {
        int length = sequence.length();
        int s = 0;
        while (s < length && Character.isSpaceChar(sequence.charAt(s))) {
            s += 1;
        }
        if (s < length && pattern.length() <= (length - s)) {
            sequence = sequence.subSequence(s, length);
            if (sequence.subSequence(0, pattern.length()).equals(pattern)) {
                return s + pattern.length();
            }
        }
        return -1;
    }

    /**
     * Read lines from a (buffered / mark-able) reader keeping all new-lines and line-feeds.
     * @param reader the reader
     * @return the line iterator
     */
    protected static Iterator<CharSequence> readLines(final Reader reader) {
        if (!reader.markSupported()) {
            throw new IllegalArgumentException("mark support in reader required");
        }
        return new Iterator<CharSequence>() {
            private CharSequence next = doNext();

            private CharSequence doNext() {
                StringBuffer strb = new StringBuffer(64); // CSOFF: MagicNumber
                int c;
                boolean eol = false;
                try {
                    while ((c = reader.read()) >= 0) {
                        if (eol) {// && (c != '\n' && c != '\r')) {
                            reader.reset();
                            break;
                        }
                        if (c == '\n') {
                            eol = true;
                        }
                        strb.append((char) c);
                        reader.mark(1);
                    }
                } catch (IOException xio) {
                    return null;
                }
                return strb.length() > 0 ? strb : null;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public CharSequence next() {
                CharSequence current = next;
                if (current != null) {
                    next = doNext();
                }
                return current;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }

    /**
     * Reads lines of a template grouping them by typed blocks.
     * @param prefix the directive prefix
     * @param source the source reader
     * @return the list of blocks
     */
    protected List<Block> readTemplate(final String prefix, Reader source) {
        final ArrayList<Block> blocks = new ArrayList<Block>();
        final BufferedReader reader;
        if (source instanceof BufferedReader) {
            reader = (BufferedReader) source;
        } else {
            reader = new BufferedReader(source);
        }
        final StringBuilder strb = new StringBuilder();
        BlockType type = null;
        int prefixLen;
        Iterator<CharSequence> lines = readLines(reader);
        int lineno = 1;
        int start = 0;
        while (lines.hasNext()) {
            CharSequence line = lines.next();
            if (line == null) {
                break;
            } else if (type == null) {
                // determine starting type if not known yet
                prefixLen = startsWith(line, prefix);
                if (prefixLen >= 0) {
                    type = BlockType.DIRECTIVE;
                    strb.append(line.subSequence(prefixLen, line.length()));
                } else {
                    type = BlockType.VERBATIM;
                    strb.append(line.subSequence(0, line.length()));
                }
                start = lineno;
            } else if (type == BlockType.DIRECTIVE) {
                // switch to verbatim if necessary
                prefixLen = startsWith(line, prefix);
                if (prefixLen < 0) {
                    Block directive = new Block(BlockType.DIRECTIVE, start, strb.toString());
                    strb.delete(0, Integer.MAX_VALUE);
                    blocks.add(directive);
                    type = BlockType.VERBATIM;
                    strb.append(line.subSequence(0, line.length()));
                    start = lineno;
                } else {
                    // still a directive
                    strb.append(line.subSequence(prefixLen, line.length()));
                }
            } else if (type == BlockType.VERBATIM) {
                // switch to directive if necessary
                prefixLen = startsWith(line, prefix);
                if (prefixLen >= 0) {
                    Block verbatim = new Block(BlockType.VERBATIM, start, strb.toString());
                    strb.delete(0, Integer.MAX_VALUE);
                    blocks.add(verbatim);
                    type = BlockType.DIRECTIVE;
                    strb.append(line.subSequence(prefixLen, line.length()));
                    start = lineno;
                } else {
                    strb.append(line.subSequence(0, line.length()));
                }
            }
            lineno += 1;
        }
        // input may be null
        if (type != null && strb.length() > 0) {
            Block block = new Block(type, start, strb.toString());
            blocks.add(block);
        }
        blocks.trimToSize();
        return blocks;
    }

    @Override
    public TemplateScript createTemplate(JexlInfo info, String prefix, Reader source, String... parms) {
        return new TemplateScript(this, info, prefix, source,  parms);
    }
}