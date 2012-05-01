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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.jexl2.introspection.JexlMethod;
import org.apache.commons.jexl2.introspection.Uberspect;
import org.apache.commons.jexl2.parser.ASTJexlScript;
import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.commons.jexl2.parser.StringParser;

/**
 * An evaluator similar to the Unified EL evaluator used in JSP/JSF based on JEXL.
 * It is intended to be used in configuration modules, XML based frameworks or JSP taglibs
 * and facilitate the implementation of expression evaluation.
 * <p>
 * An expression can mix immediate, deferred and nested sub-expressions as well as string constants;
 * <ul>
 * <li>The "immediate" syntax is of the form <code>"...${jexl-expr}..."</code></li>
 * <li>The "deferred" syntax is of the form <code>"...#{jexl-expr}..."</code></li>
 * <li>The "nested" syntax is of the form <code>"...#{...${jexl-expr0}...}..."</code></li>
 * <li>The "composite" syntax is of the form <code>"...${jexl-expr0}... #{jexl-expr1}..."</code></li>
 * </ul>
 * </p>
 * <p>
 * Deferred & immediate expression carry different intentions:
 * <ul>
 * <li>An immediate expression indicate that evaluation is intended to be performed close to
 * the definition/parsing point.</li>
 * <li>A deferred expression indicate that evaluation is intended to occur at a later stage.</li>
 * </ul>
 * </p>
 * <p>
 * For instance: <code>"Hello ${name}, now is #{time}"</code> is a composite "deferred" expression since one
 * of its subexpressions is deferred. Furthermore, this (composite) expression intent is
 * to perform two evaluations; one close to its definition and another one in a later
 * phase.
 * </p>
 * <p>
 * The API reflects this feature in 2 methods, prepare and evaluate. The prepare method
 * will evaluate the immediate subexpression and return an expression that contains only
 * the deferred subexpressions (& constants), a prepared expression. Such a prepared expression
 * is suitable for a later phase evaluation that may occur with a different JexlContext.
 * Note that it is valid to call evaluate without prepare in which case the same JexlContext
 * is used for the 2 evaluation phases.
 * </p>
 * <p>
 * In the most common use-case where deferred expressions are to be kept around as properties of objects,
 * one should parse & prepare an expression before storing it and evaluate it each time
 * the property storing it is accessed.
 * </p>
 * <p>
 * Note that nested expression use the JEXL syntax as in:
 * <code>"#{${bar}+'.charAt(2)'}"</code>
 * The most common mistake leading to an invalid expression being the following:
 * <code>"#{${bar}charAt(2)}"</code>
 * </p>
 * <p>Also note that methods that parse evaluate expressions may throw <em>unchecked</em> exceptions;
 * The {@link UnifiedJEXL.Exception} are thrown when the engine instance is in "non-silent" mode
 * but since these are RuntimeException, user-code <em>should</em> catch them where appropriate.
 * </p>
 * @since 2.0
 */
public final class UnifiedJEXL {
    /** The JEXL engine instance. */
    private final JexlEngine jexl;
    /** The expression cache. */
    private final JexlEngine.SoftCache<String, Expression> cache;
    /** The default cache size. */
    private static final int CACHE_SIZE = 256;
    /** The first character for immediate expressions. */
    private static final char IMM_CHAR = '$';
    /** The first character for deferred expressions. */
    private static final char DEF_CHAR = '#';

    /**
     * Creates a new instance of UnifiedJEXL with a default size cache.
     * @param aJexl the JexlEngine to use.
     */
    public UnifiedJEXL(JexlEngine aJexl) {
        this(aJexl, CACHE_SIZE);
    }

    /**
     * Creates a new instance of UnifiedJEXL creating a local cache.
     * @param aJexl the JexlEngine to use.
     * @param cacheSize the number of expressions in this cache
     */
    public UnifiedJEXL(JexlEngine aJexl, int cacheSize) {
        this.jexl = aJexl;
        this.cache = aJexl.new SoftCache<String, Expression>(cacheSize);
    }

    /**
     * Types of expressions.
     * Each instance carries a counter index per (composite sub-) expression type.
     * @see ExpressionBuilder
     */
    private static enum ExpressionType {
        /** Constant expression, count index 0. */
        CONSTANT(0),
        /** Immediate expression, count index 1. */
        IMMEDIATE(1),
        /** Deferred expression, count index 2. */
        DEFERRED(2),
        /** Nested (which are deferred) expressions, count index 2. */
        NESTED(2),
        /** Composite expressions are not counted, index -1. */
        COMPOSITE(-1);
        /** The index in arrays of expression counters for composite expressions. */
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
    private static class ExpressionBuilder {
        /** Per expression type counters. */
        private final int[] counts;
        /** The list of expressions. */
        private final ArrayList<Expression> expressions;

        /**
         * Creates a builder.
         * @param size the initial expression array size
         */
        ExpressionBuilder(int size) {
            counts = new int[]{0, 0, 0};
            expressions = new ArrayList<Expression>(size <= 0 ? 3 : size);
        }

        /**
         * Adds an expression to the list of expressions, maintain per-type counts.
         * @param expr the expression to add
         */
        void add(Expression expr) {
            counts[expr.getType().index] += 1;
            expressions.add(expr);
        }

        /**
         * Builds an expression from a source, performs checks.
         * @param el the unified el instance
         * @param source the source expression
         * @return an expression
         */
        Expression build(UnifiedJEXL el, Expression source) {
            int sum = 0;
            for (int count : counts) {
                sum += count;
            }
            if (expressions.size() != sum) {
                StringBuilder error = new StringBuilder("parsing algorithm error, exprs: ");
                error.append(expressions.size());
                error.append(", constant:");
                error.append(counts[ExpressionType.CONSTANT.index]);
                error.append(", immediate:");
                error.append(counts[ExpressionType.IMMEDIATE.index]);
                error.append(", deferred:");
                error.append(counts[ExpressionType.DEFERRED.index]);
                throw new IllegalStateException(error.toString());
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
     * Gets the JexlEngine underlying the UnifiedJEXL.
     * @return the JexlEngine
     */
    public JexlEngine getEngine() {
        return jexl;
    }

    /**
     * Clears the cache.
     * @since 2.1
     */
    public void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * The sole type of (runtime) exception the UnifiedJEXL can throw.
     */
    public static class Exception extends RuntimeException {
        /** Serial version UID. */
        private static final long serialVersionUID = -8201402995815975726L;

        /**
         * Creates a UnifiedJEXL.Exception.
         * @param msg the exception message
         * @param cause the exception cause
         */
        public Exception(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * The abstract base class for all expressions, immediate '${...}' and deferred '#{...}'.
     */
    public abstract class Expression {
        /** The source of this expression (see {@link UnifiedJEXL.Expression#prepare}). */
        protected final Expression source;

        /**
         * Creates an expression.
         * @param src the source expression if any
         */
        Expression(Expression src) {
            this.source = src != null ? src : this;
        }

        /**
         * Checks whether this expression is immediate.
         * @return true if immediate, false otherwise
         */
        public boolean isImmediate() {
            return true;
        }

        /**
         * Checks whether this expression is deferred.
         * @return true if deferred, false otherwise
         */
        public final boolean isDeferred() {
            return !isImmediate();
        }

        /**
         * Gets this expression type.
         * @return its type
         */
        abstract ExpressionType getType();

        /**
         * Formats this expression, adding its source string representation in
         * comments if available: 'expression /*= source *\/'' .
         * <b>Note:</b> do not override; will be made final in a future release.
         * @return the formatted expression string
         */
        @Override
        public String toString() {
            StringBuilder strb = new StringBuilder();
            asString(strb);
            if (source != this) {
                strb.append(" /*= ");
                strb.append(source.toString());
                strb.append(" */");
            }
            return strb.toString();
        }

        /**
         * Generates this expression's string representation.
         * @return the string representation
         */
        public String asString() {
            StringBuilder strb = new StringBuilder();
            asString(strb);
            return strb.toString();
        }

        /**
         * Adds this expression's string representation to a StringBuilder.
         * @param strb the builder to fill
         * @return the builder argument
         */
        public abstract StringBuilder asString(StringBuilder strb);

        /**
         * Gets the list of variables accessed by this expression.
         * <p>This method will visit all nodes of the sub-expressions and extract all variables whether they
         * are written in 'dot' or 'bracketed' notation. (a.b is equivalent to a['b']).</p>
         * @return the set of variables, each as a list of strings (ant-ish variables use more than 1 string)
         *         or the empty set if no variables are used
         * @since 2.1
         */
        public Set<List<String>> getVariables() {
            return Collections.emptySet();
        }

        /**
         * Fills up the list of variables accessed by this expression.
         * @param refs the set of variable being filled
         * @since 2.1
         */
        protected void getVariables(Set<List<String>> refs) {
            // nothing to do
        }

        /**
         * Evaluates the immediate sub-expressions.
         * <p>
         * When the expression is dependant upon immediate and deferred sub-expressions,
         * evaluates the immediate sub-expressions with the context passed as parameter
         * and returns this expression deferred form.
         * </p>
         * <p>
         * In effect, this binds the result of the immediate sub-expressions evaluation in the
         * context, allowing to differ evaluation of the remaining (deferred) expression within another context.
         * This only has an effect to nested & composite expressions that contain differed & immediate sub-expressions.
         * </p>
         * <p>
         * If the underlying JEXL engine is silent, errors will be logged through its logger as warning.
         * </p>
         * <b>Note:</b> do not override; will be made final in a future release.
         * @param context the context to use for immediate expression evaluations
         * @return an expression or null if an error occurs and the {@link JexlEngine} is running in silent mode
         * @throws UnifiedJEXL.Exception if an error occurs and the {@link JexlEngine} is not in silent mode
         */
        public Expression prepare(JexlContext context) {
            try {
                Interpreter interpreter = new Interpreter(jexl, context, !jexl.isLenient(), jexl.isSilent());
                if (context instanceof TemplateContext) {
                    interpreter.setFrame(((TemplateContext) context).getFrame());
                }
                return prepare(interpreter);
            } catch (JexlException xjexl) {
                Exception xuel = createException("prepare", this, xjexl);
                if (jexl.isSilent()) {
                    jexl.logger.warn(xuel.getMessage(), xuel.getCause());
                    return null;
                }
                throw xuel;
            }
        }

        /**
         * Evaluates this expression.
         * <p>
         * If the underlying JEXL engine is silent, errors will be logged through its logger as warning.
         * </p>
         * <b>Note:</b> do not override; will be made final in a future release.
         * @param context the variable context
         * @return the result of this expression evaluation or null if an error occurs and the {@link JexlEngine} is
         * running in silent mode
         * @throws UnifiedJEXL.Exception if an error occurs and the {@link JexlEngine} is not silent
         */
        public Object evaluate(JexlContext context) {
            try {
                Interpreter interpreter = new Interpreter(jexl, context, !jexl.isLenient(), jexl.isSilent());
                if (context instanceof TemplateContext) {
                    interpreter.setFrame(((TemplateContext) context).getFrame());
                }
                return evaluate(interpreter);
            } catch (JexlException xjexl) {
                Exception xuel = createException("prepare", this, xjexl);
                if (jexl.isSilent()) {
                    jexl.logger.warn(xuel.getMessage(), xuel.getCause());
                    return null;
                }
                throw xuel;
            }
        }

        /**
         * Retrieves this expression's source expression.
         * If this expression was prepared, this allows to retrieve the
         * original expression that lead to it.
         * Other expressions return themselves.
         * @return the source expression
         */
        public final Expression getSource() {
            return source;
        }

        /**
         * Prepares a sub-expression for interpretation.
         * @param interpreter a JEXL interpreter
         * @return a prepared expression
         * @throws JexlException (only for nested & composite)
         */
        protected Expression prepare(Interpreter interpreter) {
            return this;
        }

        /**
         * Intreprets a sub-expression.
         * @param interpreter a JEXL interpreter
         * @return the result of interpretation
         * @throws JexlException (only for nested & composite)
         */
        protected abstract Object evaluate(Interpreter interpreter);
    }

    /** A constant expression. */
    private class ConstantExpression extends Expression {
        /** The constant held by this expression. */
        private final Object value;

        /**
         * Creates a constant expression.
         * <p>
         * If the wrapped constant is a string, it is treated
         * as a JEXL strings with respect to escaping.
         * </p>
         * @param val the constant value
         * @param source the source expression if any
         */
        ConstantExpression(Object val, Expression source) {
            super(source);
            if (val == null) {
                throw new NullPointerException("constant can not be null");
            }
            if (val instanceof String) {
                val = StringParser.buildString((String) val, false);
            }
            this.value = val;
        }

        /** {@inheritDoc} */
        @Override
        ExpressionType getType() {
            return ExpressionType.CONSTANT;
        }

        /** {@inheritDoc} */
        @Override
        public StringBuilder asString(StringBuilder strb) {
            if (value != null) {
                strb.append(value.toString());
            }
            return strb;
        }

        /** {@inheritDoc} */
        @Override
        protected Object evaluate(Interpreter interpreter) {
            return value;
        }
    }

    /** The base for Jexl based expressions. */
    private abstract class JexlBasedExpression extends Expression {
        /** The JEXL string for this expression. */
        protected final CharSequence expr;
        /** The JEXL node for this expression. */
        protected final JexlNode node;

        /**
         * Creates a JEXL interpretable expression.
         * @param theExpr the expression as a string
         * @param theNode the expression as an AST
         * @param theSource the source expression if any
         */
        protected JexlBasedExpression(CharSequence theExpr, JexlNode theNode, Expression theSource) {
            super(theSource);
            this.expr = theExpr;
            this.node = theNode;
        }

        /** {@inheritDoc} */
        @Override
        public StringBuilder asString(StringBuilder strb) {
            strb.append(isImmediate() ? IMM_CHAR : DEF_CHAR);
            strb.append("{");
            strb.append(expr);
            strb.append("}");
            return strb;
        }

        /** {@inheritDoc} */
        @Override
        protected Object evaluate(Interpreter interpreter) {
            return interpreter.interpret(node);
        }

        /** {@inheritDoc} */
        @Override
        public Set<List<String>> getVariables() {
            Set<List<String>> refs = new LinkedHashSet<List<String>>();
            getVariables(refs);
            return refs;
        }

        /** {@inheritDoc} */
        @Override
        protected void getVariables(Set<List<String>> refs) {
            jexl.getVariables(node, refs, null);
        }
    }

    /** An immediate expression: ${jexl}. */
    private class ImmediateExpression extends JexlBasedExpression {
        /**
         * Creates an immediate expression.
         * @param expr the expression as a string
         * @param node the expression as an AST
         * @param source the source expression if any
         */
        ImmediateExpression(CharSequence expr, JexlNode node, Expression source) {
            super(expr, node, source);
        }

        /** {@inheritDoc} */
        @Override
        ExpressionType getType() {
            return ExpressionType.IMMEDIATE;
        }

        /** {@inheritDoc} */
        @Override
        protected Expression prepare(Interpreter interpreter) {
            // evaluate immediate as constant
            Object value = evaluate(interpreter);
            return value != null ? new ConstantExpression(value, source) : null;
        }
    }

    /** A deferred expression: #{jexl}. */
    private class DeferredExpression extends JexlBasedExpression {
        /**
         * Creates a deferred expression.
         * @param expr the expression as a string
         * @param node the expression as an AST
         * @param source the source expression if any
         */
        DeferredExpression(CharSequence expr, JexlNode node, Expression source) {
            super(expr, node, source);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isImmediate() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        ExpressionType getType() {
            return ExpressionType.DEFERRED;
        }

        /** {@inheritDoc} */
        @Override
        protected Expression prepare(Interpreter interpreter) {
            return new ImmediateExpression(expr, node, source);
        }

        /** {@inheritDoc} */
        @Override
        protected void getVariables(Set<List<String>> refs) {
            // noop
        }
    }

    /**
     * An immediate expression nested into a deferred expression.
     * #{...${jexl}...}
     * Note that the deferred syntax is JEXL's, not UnifiedJEXL.
     */
    private class NestedExpression extends JexlBasedExpression {
        /**
         * Creates a nested expression.
         * @param expr the expression as a string
         * @param node the expression as an AST
         * @param source the source expression if any
         */
        NestedExpression(CharSequence expr, JexlNode node, Expression source) {
            super(expr, node, source);
            if (this.source != this) {
                throw new IllegalArgumentException("Nested expression can not have a source");
            }
        }

        @Override
        public StringBuilder asString(StringBuilder strb) {
            strb.append(expr);
            return strb;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isImmediate() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        ExpressionType getType() {
            return ExpressionType.NESTED;
        }

        /** {@inheritDoc} */
        @Override
        protected Expression prepare(Interpreter interpreter) {
            String value = interpreter.interpret(node).toString();
            JexlNode dnode = jexl.parse(value, jexl.isDebug() ? node.debugInfo() : null, null);
            return new ImmediateExpression(value, dnode, this);
        }

        /** {@inheritDoc} */
        @Override
        protected Object evaluate(Interpreter interpreter) {
            return prepare(interpreter).evaluate(interpreter);
        }
    }

    /** A composite expression: "... ${...} ... #{...} ...". */
    private class CompositeExpression extends Expression {
        /** Bit encoded (deferred count > 0) bit 1, (immediate count > 0) bit 0. */
        private final int meta;
        /** The list of sub-expression resulting from parsing. */
        protected final Expression[] exprs;

        /**
         * Creates a composite expression.
         * @param counters counters of expression per type
         * @param list the sub-expressions
         * @param src the source for this expresion if any
         */
        CompositeExpression(int[] counters, ArrayList<Expression> list, Expression src) {
            super(src);
            this.exprs = list.toArray(new Expression[list.size()]);
            this.meta = (counters[ExpressionType.DEFERRED.index] > 0 ? 2 : 0)
                    | (counters[ExpressionType.IMMEDIATE.index] > 0 ? 1 : 0);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isImmediate() {
            // immediate if no deferred
            return (meta & 2) == 0;
        }

        /** {@inheritDoc} */
        @Override
        ExpressionType getType() {
            return ExpressionType.COMPOSITE;
        }

        /** {@inheritDoc} */
        @Override
        public StringBuilder asString(StringBuilder strb) {
            for (Expression e : exprs) {
                e.asString(strb);
            }
            return strb;
        }

        /** {@inheritDoc} */
        @Override
        public Set<List<String>> getVariables() {
            Set<List<String>> refs = new LinkedHashSet<List<String>>();
            for (Expression expr : exprs) {
                expr.getVariables(refs);
            }
            return refs;
        }

        /** {@inheritDoc} */
        @Override
        protected Expression prepare(Interpreter interpreter) {
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
                Expression expr = exprs[e];
                Expression prepared = expr.prepare(interpreter);
                // add it if not null
                if (prepared != null) {
                    builder.add(prepared);
                }
                // keep track of expression equivalence
                eq &= expr == prepared;
            }
            Expression ready = eq ? this : builder.build(UnifiedJEXL.this, this);
            return ready;
        }

        /** {@inheritDoc} */
        @Override
        protected Object evaluate(Interpreter interpreter) {
            final int size = exprs.length;
            Object value = null;
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

    /** Creates a a {@link UnifiedJEXL.Expression} from an expression string.
     *  Uses & fills up the expression cache if any.
     * <p>
     * If the underlying JEXL engine is silent, errors will be logged through its logger as warnings.
     * </p>
     * @param expression the UnifiedJEXL string expression
     * @return the UnifiedJEXL object expression, null if silent and an error occured
     * @throws UnifiedJEXL.Exception if an error occurs and the {@link JexlEngine} is not silent
     */
    public Expression parse(String expression) {
        Exception xuel = null;
        Expression stmt = null;
        try {
            if (cache == null) {
                stmt = parseExpression(expression, null);
            } else {
                synchronized (cache) {
                    stmt = cache.get(expression);
                    if (stmt == null) {
                        stmt = parseExpression(expression, null);
                        cache.put(expression, stmt);
                    }
                }
            }
        } catch (JexlException xjexl) {
            xuel = new Exception("failed to parse '" + expression + "'", xjexl);
        } catch (Exception xany) {
            xuel = xany;
        } finally {
            if (xuel != null) {
                if (jexl.isSilent()) {
                    jexl.logger.warn(xuel.getMessage(), xuel.getCause());
                    return null;
                }
                throw xuel;
            }
        }
        return stmt;
    }

    /**
     * Creates a UnifiedJEXL.Exception from a JexlException.
     * @param action parse, prepare, evaluate
     * @param expr the expression
     * @param xany the exception
     * @return an exception containing an explicit error message
     */
    private Exception createException(String action, Expression expr, java.lang.Exception xany) {
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
        return new Exception(strb.toString(), xany);
    }

    /** The different parsing states. */
    private static enum ParseState {
        /** Parsing a constant. */
        CONST,
        /** Parsing after $ .*/
        IMMEDIATE0,
        /** Parsing after # .*/
        DEFERRED0,
        /** Parsing after ${ .*/
        IMMEDIATE1,
        /** Parsing after #{ .*/
        DEFERRED1,
        /** Parsing after \ .*/
        ESCAPE
    }

    /**
     * Parses a unified expression.
     * @param expr the string expression
     * @param scope the expression scope
     * @return the expression instance
     * @throws JexlException if an error occur during parsing
     */
    private Expression parseExpression(String expr, JexlEngine.Scope scope) {
        final int size = expr.length();
        ExpressionBuilder builder = new ExpressionBuilder(0);
        StringBuilder strb = new StringBuilder(size);
        ParseState state = ParseState.CONST;
        int inner = 0;
        boolean nested = false;
        int inested = -1;
        for (int i = 0; i < size; ++i) {
            char c = expr.charAt(i);
            switch (state) {
                default: // in case we ever add new expression type
                    throw new UnsupportedOperationException("unexpected expression type");
                case CONST:
                    if (c == IMM_CHAR) {
                        state = ParseState.IMMEDIATE0;
                    } else if (c == DEF_CHAR) {
                        inested = i;
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
                            Expression cexpr = new ConstantExpression(strb.toString(), null);
                            builder.add(cexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                        }
                    } else {
                        // revert to CONST
                        strb.append(IMM_CHAR);
                        strb.append(c);
                        state = ParseState.CONST;
                    }
                    break;
                case DEFERRED0: // #
                    if (c == '{') {
                        state = ParseState.DEFERRED1;
                        // if chars in buffer, create constant
                        if (strb.length() > 0) {
                            Expression cexpr = new ConstantExpression(strb.toString(), null);
                            builder.add(cexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                        }
                    } else {
                        // revert to CONST
                        strb.append(DEF_CHAR);
                        strb.append(c);
                        state = ParseState.CONST;
                    }
                    break;
                case IMMEDIATE1: // ${...
                    if (c == '}') {
                        // materialize the immediate expr
                        Expression iexpr = new ImmediateExpression(
                                strb.toString(),
                                jexl.parse(strb, null, scope),
                                null);
                        builder.add(iexpr);
                        strb.delete(0, Integer.MAX_VALUE);
                        state = ParseState.CONST;
                    } else {
                        // do buildup expr
                        strb.append(c);
                    }
                    break;
                case DEFERRED1: // #{...
                    // skip inner strings (for '}')
                    if (c == '"' || c == '\'') {
                        strb.append(c);
                        i = StringParser.readString(strb, expr, i + 1, c);
                        continue;
                    }
                    // nested immediate in deferred; need to balance count of '{' & '}'
                    if (c == '{') {
                        if (expr.charAt(i - 1) == IMM_CHAR) {
                            inner += 1;
                            strb.deleteCharAt(strb.length() - 1);
                            nested = true;
                        }
                        continue;
                    }
                    // closing '}'
                    if (c == '}') {
                        // balance nested immediate
                        if (inner > 0) {
                            inner -= 1;
                        } else {
                            // materialize the nested/deferred expr
                            Expression dexpr = null;
                            if (nested) {
                                dexpr = new NestedExpression(
                                        expr.substring(inested, i + 1),
                                        jexl.parse(strb, null, scope),
                                        null);
                            } else {
                                dexpr = new DeferredExpression(
                                        strb.toString(),
                                        jexl.parse(strb, null, scope),
                                        null);
                            }
                            builder.add(dexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                            nested = false;
                            state = ParseState.CONST;
                        }
                    } else {
                        // do buildup expr
                        strb.append(c);
                    }
                    break;
                case ESCAPE:
                    if (c == DEF_CHAR) {
                        strb.append(DEF_CHAR);
                    } else if (c == IMM_CHAR) {
                        strb.append(IMM_CHAR);
                    } else {
                        strb.append('\\');
                        strb.append(c);
                    }
                    state = ParseState.CONST;
            }
        }
        // we should be in that state
        if (state != ParseState.CONST) {
            throw new Exception("malformed expression: " + expr, null);
        }
        // if any chars were buffered, add them as a constant
        if (strb.length() > 0) {
            Expression cexpr = new ConstantExpression(strb.toString(), null);
            builder.add(cexpr);
        }
        return builder.build(this, null);
    }

    /**
     * The enum capturing the difference between verbatim and code source fragments.
     */
    private static enum BlockType {
        /** Block is to be output "as is". */
        VERBATIM,
        /** Block is a directive, ie a fragment of code. */
        DIRECTIVE;
    }

    /**
     * Abstract the source fragments, verbatim or immediate typed text blocks.
     * @since 2.1
     */
    private static final class TemplateBlock {
        /** The type of block, verbatim or directive. */
        private final BlockType type;
        /** The actual contexnt. */
        private final String body;

        /**
         * Creates a new block.
         * @param theType the type
         * @param theBlock the content
         */
        TemplateBlock(BlockType theType, String theBlock) {
            type = theType;
            body = theBlock;
        }

        @Override
        public String toString() {
            return body;
        }
    }

    /**
     * A Template is a script that evaluates by writing its content through a Writer.
     * This is a simplified replacement for Velocity that uses JEXL (instead of OGNL/VTL) as the scripting
     * language.
     * <p>
     * The source text is parsed considering each line beginning with '$$' (as default pattern) as JEXL script code
     * and all others as Unified JEXL expressions; those expressions will be invoked from the script during
     * evaluation and their output gathered through a writer.
     * It is thus possible to use looping or conditional construct "around" expressions generating output.
     * </p>
     * For instance:
     * <p><blockquote><pre>
     * $$ for(var x : [1, 3, 5, 42, 169]) {
     * $$   if (x == 42) {
     * Life, the universe, and everything
     * $$   } else if (x > 42) {
     * The value $(x} is over fourty-two
     * $$   } else {
     * The value ${x} is under fourty-two
     * $$   }
     * $$ }
     * </pre></blockquote>
     * Will evaluate as:
     * <p><blockquote><pre>
     * The value 1 is under fourty-two
     * The value 3 is under fourty-two
     * The value 5 is under fourty-two
     * Life, the universe, and everything
     * The value 169 is over fourty-two
     * </pre></blockquote>
     * <p>
     * During evaluation, the template context exposes its writer as '$jexl' which is safe to use in this case.
     * This allows writing directly through the writer without adding new-lines as in:
     * <p><blockquote><pre>
     * $$ for(var cell : cells) { $jexl.print(cell); $jexl.print(';') }
     * </pre></blockquote>
     * </p>
     * <p>
     * A template is expanded as one JEXL script and a list of UnifiedJEXL expressions; each UnifiedJEXL expression
     * being replace in the script by a call to jexl:print(expr) (the expr is in fact the expr number in the template).
     * This integration uses a specialized JexlContext (TemplateContext) that serves as a namespace (for jexl:)
     * and stores the expression array and the writer (java.io.Writer) that the 'jexl:print(...)'
     * delegates the output generation to.
     * </p>
     * @since 2.1
     */
    public final class Template {
        /** The prefix marker. */
        private final String prefix;
        /** The array of source blocks. */
        private final TemplateBlock[] source;
        /** The resulting script. */
        private final ASTJexlScript script;
        /** The UnifiedJEXL expressions called by the script. */
        private final Expression[] exprs;

        /**
         * Creates a new template from an input.
         * @param directive the prefix for lines of code; can not be "$", "${", "#" or "#{"
         * since this would preclude being able to differentiate directives and UnifiedJEXL expressions
         * @param reader the input reader
         * @param parms the parameter names
         * @throws NullPointerException if either the directive prefix or input is null
         * @throws IllegalArgumentException if the directive prefix is invalid
         */
        public Template(String directive, Reader reader, String... parms) {
            if (directive == null) {
                throw new NullPointerException("null prefix");
            }
            if ("$".equals(directive)
                    || "${".equals(directive)
                    || "#".equals(directive)
                    || "#{".equals(directive)) {
                throw new IllegalArgumentException(directive + ": is not a valid directive pattern");
            }
            if (reader == null) {
                throw new NullPointerException("null input");
            }
            JexlEngine.Scope scope = new JexlEngine.Scope(parms);
            prefix = directive;
            List<TemplateBlock> blocks = readTemplate(prefix, reader);
            List<Expression> uexprs = new ArrayList<Expression>();
            StringBuilder strb = new StringBuilder();
            int nuexpr = 0;
            int codeStart = -1;
            for (int b = 0; b < blocks.size(); ++b) {
                TemplateBlock block = blocks.get(b);
                if (block.type == BlockType.VERBATIM) {
                    strb.append("jexl:print(");
                    strb.append(nuexpr++);
                    strb.append(");");
                } else {
                    // keep track of first block of code, the frame creator
                    if (codeStart < 0) {
                        codeStart = b;
                    }
                    strb.append(block.body);
                }
            }
            // parse the script
            script = getEngine().parse(strb.toString(), null, scope);
            scope = script.getScope();
            // parse the exprs using the code frame for those appearing after the first block of code
            for (int b = 0; b < blocks.size(); ++b) {
                TemplateBlock block = blocks.get(b);
                if (block.type == BlockType.VERBATIM) {
                    uexprs.add(UnifiedJEXL.this.parseExpression(block.body, b > codeStart ? scope : null));
                }
            }
            source = blocks.toArray(new TemplateBlock[blocks.size()]);
            exprs = uexprs.toArray(new Expression[uexprs.size()]);
        }

        /**
         * Private ctor used to expand deferred expressions during prepare.
         * @param thePrefix the directive prefix
         * @param theSource the source
         * @param theScript the script
         * @param theExprs the expressions
         */
        private Template(String thePrefix, TemplateBlock[] theSource, ASTJexlScript theScript, Expression[] theExprs) {
            prefix = thePrefix;
            source = theSource;
            script = theScript;
            exprs = theExprs;
        }

        @Override
        public String toString() {
            StringBuilder strb = new StringBuilder();
            for (TemplateBlock block : source) {
                if (block.type == BlockType.DIRECTIVE) {
                    strb.append(prefix);
                }
                strb.append(block.toString());
                strb.append('\n');
            }
            return strb.toString();
        }

        /**
         * Recreate the template source from its inner components.
         * @return the template source rewritten
         */
        public String asString() {
            StringBuilder strb = new StringBuilder();
            int e = 0;
            for (int b = 0; b < source.length; ++b) {
                TemplateBlock block = source[b];
                if (block.type == BlockType.DIRECTIVE) {
                    strb.append(prefix);
                } else {
                    exprs[e++].asString(strb);
                }
            }
            return strb.toString();
        }

        /**
         * Prepares this template by expanding any contained deferred expression.
         * @param context the context to prepare against
         * @return the prepared version of the template
         */
        public Template prepare(JexlContext context) {
            JexlEngine.Frame frame = script.createFrame((Object[]) null);
            TemplateContext tcontext = new TemplateContext(context, frame, exprs, null);
            Expression[] immediates = new Expression[exprs.length];
            for (int e = 0; e < exprs.length; ++e) {
                immediates[e] = exprs[e].prepare(tcontext);
            }
            return new Template(prefix, source, script, immediates);
        }

        /**
         * Evaluates this template.
         * @param context the context to use during evaluation
         * @param writer the writer to use for output
         */
        public void evaluate(JexlContext context, Writer writer) {
            evaluate(context, writer, (Object[]) null);
        }

        /**
         * Evaluates this template.
         * @param context the context to use during evaluation
         * @param writer the writer to use for output
         * @param args the arguments
         */
        public void evaluate(JexlContext context, Writer writer, Object... args) {
            JexlEngine.Frame frame = script.createFrame(args);
            TemplateContext tcontext = new TemplateContext(context, frame, exprs, writer);
            Interpreter interpreter = jexl.createInterpreter(tcontext, !jexl.isLenient(), false);
            interpreter.setFrame(frame);
            interpreter.interpret(script);
        }
    }

    /**
     * The type of context to use during evaluation of templates.
     * <p>This context exposes its writer as '$jexl' to the scripts.</p>
     * <p>public for introspection purpose.</p>
     * @since 2.1
     */
    public final class TemplateContext implements JexlContext, NamespaceResolver {
        /** The wrapped context. */
        private final JexlContext wrap;
        /** The array of UnifiedJEXL expressions. */
        private final Expression[] exprs;
        /** The writer used to output. */
        private final Writer writer;
        /** The call frame. */
        private final JexlEngine.Frame frame;

        /**
         * Creates a template context instance.
         * @param jcontext the base context
         * @param jframe the calling frame
         * @param expressions the list of expression from the template to evaluate
         * @param out the output writer
         */
        protected TemplateContext(JexlContext jcontext, JexlEngine.Frame jframe, Expression[] expressions, Writer out) {
            wrap = jcontext;
            frame = jframe;
            exprs = expressions;
            writer = out;
        }

        /**
         * Gets this context calling frame.
         * @return the engine frame
         */
        public JexlEngine.Frame getFrame() {
            return frame;
        }

        /** {@inheritDoc} */
        public Object get(String name) {
            if ("$jexl".equals(name)) {
                return writer;
            } else {
                return wrap.get(name);
            }
        }

        /** {@inheritDoc} */
        public void set(String name, Object value) {
            wrap.set(name, value);
        }

        /** {@inheritDoc} */
        public boolean has(String name) {
            return wrap.has(name);
        }

        /** {@inheritDoc} */
        public Object resolveNamespace(String ns) {
            if ("jexl".equals(ns)) {
                return this;
            } else if (wrap instanceof NamespaceResolver) {
                return ((NamespaceResolver) wrap).resolveNamespace(ns);
            } else {
                return null;
            }
        }

        /**
         * Includes a call to another template.
         * <p>Evaluates a template using this template initial context and writer.</p>
         * @param template the template to evaluate
         * @param args the arguments
         */
        public void include(Template template, Object... args) {
            template.evaluate(wrap, writer, args);
        }

        /**
         * Prints an expression result.
         * @param e the expression number
         */
        public void print(int e) {
            if (e < 0 || e >= exprs.length) {
                return;
            }
            Expression expr = exprs[e];
            if (expr.isDeferred()) {
                expr = expr.prepare(wrap);
            }
            if (expr instanceof CompositeExpression) {
                printComposite((CompositeExpression) expr);
            } else {
                doPrint(expr.evaluate(this));
            }
        }

        /**
         * Prints a composite expression.
         * @param composite the composite expression
         */
        protected void printComposite(CompositeExpression composite) {
            Expression[] cexprs = composite.exprs;
            final int size = cexprs.length;
            Object value = null;
            for (int e = 0; e < size; ++e) {
                value = cexprs[e].evaluate(this);
                doPrint(value);
            }
        }

        /**
         * Prints to output.
         * <p>This will dynamically try to find the best suitable method in the writer through uberspection.
         * Subclassing Writer by adding 'print' methods should be the preferred way to specialize output.
         * </p>
         * @param arg the argument to print out
         */
        private void doPrint(Object arg) {
            try {
                if (arg instanceof CharSequence) {
                    writer.write(arg.toString());
                } else if (arg != null) {
                    Object[] value = {arg};
                    Uberspect uber = getEngine().getUberspect();
                    JexlMethod method = uber.getMethod(writer, "print", value, null);
                    if (method != null) {
                        method.invoke(writer, value);
                    } else {
                        writer.write(arg.toString());
                    }
                }
            } catch (java.io.IOException xio) {
                throw createException("call print", null, xio);
            } catch (java.lang.Exception xany) {
                throw createException("invoke print", null, xany);
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
     * Reads lines of a template grouping them by typed blocks.
     * @param prefix the directive prefix
     * @param source the source reader
     * @return the list of blocks
     * @since 2.1
     */
    protected List<TemplateBlock> readTemplate(final String prefix, Reader source) {
        try {
            int prefixLen;
            List<TemplateBlock> blocks = new ArrayList<TemplateBlock>();
            BufferedReader reader;
            if (source instanceof BufferedReader) {
                reader = (BufferedReader) source;
            } else {
                reader = new BufferedReader(source);
            }
            StringBuilder strb = new StringBuilder();
            BlockType type = null;
            while (true) {
                CharSequence line = reader.readLine();
                if (line == null) {
                    // at end
                    TemplateBlock block = new TemplateBlock(type, strb.toString());
                    blocks.add(block);
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
                        strb.append('\n');
                    }
                } else if (type == BlockType.DIRECTIVE) {
                    // switch to verbatim if necessary
                    prefixLen = startsWith(line, prefix);
                    if (prefixLen < 0) {
                        TemplateBlock code = new TemplateBlock(BlockType.DIRECTIVE, strb.toString());
                        strb.delete(0, Integer.MAX_VALUE);
                        blocks.add(code);
                        type = BlockType.VERBATIM;
                        strb.append(line.subSequence(0, line.length()));
                    } else {
                        strb.append(line.subSequence(prefixLen, line.length()));
                    }
                } else if (type == BlockType.VERBATIM) {
                    // switch to code if necessary(
                    prefixLen = startsWith(line, prefix);
                    if (prefixLen >= 0) {
                        strb.append('\n');
                        TemplateBlock verbatim = new TemplateBlock(BlockType.VERBATIM, strb.toString());
                        strb.delete(0, Integer.MAX_VALUE);
                        blocks.add(verbatim);
                        type = BlockType.DIRECTIVE;
                        strb.append(line.subSequence(prefixLen, line.length()));
                    } else {
                        strb.append(line.subSequence(0, line.length()));
                    }
                }
            }
            return blocks;
        } catch (IOException xio) {
            return null;
        }
    }

    /**
     * Creates a new template.
     * @param prefix the directive prefix
     * @param source the source
     * @param parms the parameter names
     * @return the template
     * @since 2.1
     */
    public Template createTemplate(String prefix, Reader source, String... parms) {
        return new Template(prefix, source, parms);
    }

    /**
     * Creates a new template.
     * @param source the source
     * @param parms the parameter names
     * @return the template
     * @since 2.1
     */
    public Template createTemplate(String source, String... parms) {
        return new Template("$$", new StringReader(source), parms);
    }

    /**
     * Creates a new template.
     * @param source the source
     * @return the template
     * @since 2.1
     */
    public Template createTemplate(String source) {
        return new Template("$$", new StringReader(source), (String[]) null);
    }
}