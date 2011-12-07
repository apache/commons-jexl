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
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JxltEngine;
import org.apache.commons.jexl3.NamespaceResolver;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.StringParser;

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

/**
 * A JxltEngine implementation.
 * @since 3.0
 */
public final class TemplateEngine extends JxltEngine {
    /** The JEXL engine instance. */
    private final Engine jexl;
    /** The TemplateExpression cache. */
    private final Engine.SoftCache<String, TemplateExpression> cache;
    /** The default cache size. */
    private static final int CACHE_SIZE = 256;
    /** The first character for immediate expressions. */
    private final char immediateChar;
    /** The first character for deferred expressions. */
    private final char deferredChar;

    /**
     * Creates a new instance of {@link JxltEngine} with a default cache size.
     * @param aJexl the JexlEngine to use.
     */
    public TemplateEngine(Engine aJexl) {
        this(aJexl, CACHE_SIZE);
    }

    /**
     * Creates a new instance of {@link JxltEngine} creating a local cache.
     * @param aJexl the JexlEngine to use.
     * @param cacheSize the number of expressions in this cache
     */
    public TemplateEngine(Engine aJexl, int cacheSize) {
        this(aJexl, cacheSize, '$', '#');
    }
    
    /**
     * Creates a new instance of {@link JxltEngine} creating a local cache.
     * @param aJexl the JexlEngine to use.
     * @param cacheSize the number of expressions in this cache
     * @param immediate the immediate template expression character, default is '$'
     * @param deferred the deferred template expression character, default is '#'
     */
    public TemplateEngine(Engine aJexl, int cacheSize, char immediate, char deferred) {
        this.jexl = aJexl;
        this.cache = aJexl.new SoftCache<String, TemplateExpression>(cacheSize);
        immediateChar = immediate;
        deferredChar = deferred;
    }

    /**
     * Types of expressions.
     * Each instance carries a counter index per (composite sub-) template expression type.
     * @see ExpressionBuilder
     */
    private static enum ExpressionType {
        /** Constant TemplateExpression, count index 0. */
        CONSTANT(0),
        /** Immediate TemplateExpression, count index 1. */
        IMMEDIATE(1),
        /** Deferred TemplateExpression, count index 2. */
        DEFERRED(2),
        /** Nested (which are deferred) expressions, count index 2. */
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
    private static class ExpressionBuilder {
        /** Per TemplateExpression type counters. */
        private final int[] counts;
        /** The list of expressions. */
        private final ArrayList<TemplateExpression> expressions;

        /**
         * Creates a builder.
         * @param size the initial TemplateExpression array size
         */
        ExpressionBuilder(int size) {
            counts = new int[]{0, 0, 0};
            expressions = new ArrayList<TemplateExpression>(size <= 0 ? 3 : size);
        }

        /**
         * Adds an TemplateExpression to the list of expressions, maintain per-type counts.
         * @param expr the TemplateExpression to add
         */
        void add(TemplateExpression expr) {
            counts[expr.getType().index] += 1;
            expressions.add(expr);
        }

        /**
         * Builds an TemplateExpression from a source, performs checks.
         * @param el the unified el instance
         * @param source the source TemplateExpression
         * @return an TemplateExpression
         */
        TemplateExpression build(TemplateEngine el, TemplateExpression source) {
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
     * Gets the JexlEngine underlying this JxltEngine.
     * @return the JexlEngine
     */
    @Override
    public JexlEngine getEngine() {
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
   private abstract class TemplateExpression implements UnifiedExpression {
        /** The source of this  template expression(see {@link TemplateEngine.TemplateExpression#prepare}). */
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

        /**
         * Fills up the list of variables accessed by this unified expression.
         * @param refs the set of variable being filled
         */
        protected void getVariables(Set<List<String>> refs) {
            // nothing to do
        }

        @Override
        public final TemplateExpression prepare(JexlContext context) {
            try {
                Engine.Frame frame = context instanceof TemplateContext
                                     ? ((TemplateContext) context).getFrame()
                                     : null;
                Interpreter interpreter = jexl.createInterpreter(context, frame);
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

        @Override
        public final Object evaluate(JexlContext context) {
            try {
                Engine.Frame frame = context instanceof TemplateContext
                                     ? ((TemplateContext) context).getFrame()
                                     : null;
                Interpreter interpreter = jexl.createInterpreter(context, frame);
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

        @Override
        public final TemplateExpression getSource() {
            return source;
        }

        /**
         * Prepares a sub-expression for interpretation.
         * @param interpreter a JEXL interpreter
         * @return a prepared unified expression
         * @throws JexlException (only for nested & composite)
         */
        protected TemplateExpression prepare(Interpreter interpreter) {
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

    /** A constant unified expression. */
    private class ConstantExpression extends TemplateExpression {
        /** The constant held by this unified expression. */
        private final Object value;

        /**
         * Creates a constant unified expression.
         * <p>
         * If the wrapped constant is a string, it is treated
         * as a JEXL strings with respect to escaping.
         * </p>
         * @param val the constant value
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

    /** The base for Jexl based unified expressions. */
    private abstract class JexlBasedExpression extends TemplateExpression {
        /** The JEXL string for this unified expression. */
        protected final CharSequence expr;
        /** The JEXL node for this unified expression. */
        protected final JexlNode node;

        /**
         * Creates a JEXL interpretable unified expression.
         * @param theExpr the unified expression as a string
         * @param theNode the unified expression as an AST
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
        protected Object evaluate(Interpreter interpreter) {
            return interpreter.interpret(node);
        }

        @Override
        public Set<List<String>> getVariables() {
            Set<List<String>> refs = new LinkedHashSet<List<String>>();
            getVariables(refs);
            return refs;
        }

        @Override
        protected void getVariables(Set<List<String>> refs) {
            jexl.getVariables(node, refs, null);
        }
    }

    /** An immediate unified expression: ${jexl}. */
    private class ImmediateExpression extends JexlBasedExpression {
        /**
         * Creates an immediate unified expression.
         * @param expr the unified expression as a string
         * @param node the unified expression as an AST
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
    private class DeferredExpression extends JexlBasedExpression {
        /**
         * Creates a deferred unified expression.
         * @param expr the unified expression as a string
         * @param node the unified expression as an AST
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
        protected void getVariables(Set<List<String>> refs) {
            // noop
        }
    }

    /**
     * An immediate unified expression nested into a deferred unified expression.
     * #{...${jexl}...}
     * Note that the deferred syntax is JEXL's.
     */
    private class NestedExpression extends JexlBasedExpression {
        /**
         * Creates a nested unified expression.
         * @param expr the unified expression as a string
         * @param node the unified expression as an AST
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
            JexlNode dnode = jexl.parse(value, jexl.isDebug() ? node.jexlInfo() : null, null);
            return new ImmediateExpression(value, dnode, this);
        }

        @Override
        protected Object evaluate(Interpreter interpreter) {
            return prepare(interpreter).evaluate(interpreter);
        }
    }

    /** A composite unified expression: "... ${...} ... #{...} ...". */
    private class CompositeExpression extends TemplateExpression {
        /** Bit encoded (deferred count > 0) bit 1, (immediate count > 0) bit 0. */
        private final int meta;
        /** The list of sub-expression resulting from parsing. */
        protected final TemplateExpression[] exprs;

        /**
         * Creates a composite expression.
         * @param counters counters of expressions per type
         * @param list the sub-expressions
         * @param src the source for this expresion if any
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
            Set<List<String>> refs = new LinkedHashSet<List<String>>();
            for (TemplateExpression expr : exprs) {
                expr.getVariables(refs);
            }
            return refs;
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
            TemplateExpression ready = eq ? this : builder.build(TemplateEngine.this, this);
            return ready;
        }

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

    @Override
    public JxltEngine.UnifiedExpression createExpression(String expression) {
        Exception xuel = null;
        TemplateExpression stmt = null;
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
     * Creates a JxltEngine.Exception from a JexlException.
     * @param action createExpression, prepare, evaluate
     * @param expr the template expression
     * @param xany the exception
     * @return an exception containing an explicit error message
     */
    private Exception createException(String action, TemplateExpression expr, java.lang.Exception xany) {
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
     * @param scope the template scope
     * @return the unified expression instance
     * @throws JexlException if an error occur during parsing
     */
    private TemplateExpression parseExpression(String expr, Engine.Scope scope) {
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
                default: // in case we ever add new unified expresssion type
                    throw new UnsupportedOperationException("unexpected unified expression type");
                case CONST:
                    if (c == immediateChar) {
                        state = ParseState.IMMEDIATE0;
                    } else if (c == deferredChar) {
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
                        // materialize the immediate expr
                        TemplateExpression iexpr = new ImmediateExpression(
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
                        if (expr.charAt(i - 1) == immediateChar) {
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
                            TemplateExpression dexpr = null;
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
        }
        // we should be in that state
        if (state != ParseState.CONST) {
            throw new Exception("malformed expression: " + expr, null);
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
    private static enum BlockType {
        /** Block is to be output "as is" but may be a unified expression. */
        VERBATIM,
        /** Block is a directive, ie a fragment of JEXL code. */
        DIRECTIVE;
    }

    /**
     * Abstract the source fragments, verbatim or immediate typed text blocks.
     */
    private static final class Block {
        /** The type of block, verbatim or directive. */
        private final BlockType type;
        /** The actual contexnt. */
        private final String body;

        /**
         * Creates a new block. 
         * @param theType the type
         * @param theBlock the content
         */
        Block(BlockType theType, String theBlock) {
            type = theType;
            body = theBlock;
        }

        @Override
        public String toString() {
            return body;
        }
    }

    /**
     * A Template instance.
     */
    public final class TemplateScript implements Template {
        /** The prefix marker. */
        private final String prefix;
        /** The array of source blocks. */
        private final Block[] source;
        /** The resulting script. */
        private final ASTJexlScript script;
        /** The TemplateEngine expressions called by the script. */
        private final TemplateExpression[] exprs;

        /**
         * Creates a new template from an character input.
         * @param directive the prefix for lines of code; can not be "$", "${", "#" or "#{"
         * since this would preclude being able to differentiate directives and template expressions
         * @param reader the input reader
         * @param parms the parameter names
         * @throws NullPointerException if either the directive prefix or input is null
         * @throws IllegalArgumentException if the directive prefix is invalid
         */
        public TemplateScript(String directive, Reader reader, String... parms) {
            if (directive == null) {
                throw new NullPointerException("null prefix");
            }
            if (Character.toString(immediateChar).equals(directive)
                || (Character.toString(immediateChar) + "{").equals(directive)
                || Character.toString(deferredChar).equals(directive)
                || (Character.toString(deferredChar) + "{").equals(directive)) {
                throw new IllegalArgumentException(directive + ": is not a valid directive pattern");
            }
            if (reader == null) {
                throw new NullPointerException("null input");
            }
            Engine.Scope scope = new Engine.Scope(parms);
            prefix = directive;
            List<Block> blocks = readTemplate(prefix, reader);
            List<TemplateExpression> uexprs = new ArrayList<TemplateExpression>();
            StringBuilder strb = new StringBuilder();
            int nuexpr = 0;
            int codeStart = -1;
            for (int b = 0; b < blocks.size(); ++b) {
                Block block = blocks.get(b);
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
            // createExpression the script
            script = jexl.parse(strb.toString(), null, scope);
            scope = script.getScope();
            // createExpression the exprs using the code frame for those appearing after the first block of code
            for (int b = 0; b < blocks.size(); ++b) {
                Block block = blocks.get(b);
                if (block.type == BlockType.VERBATIM) {
                    uexprs.add(TemplateEngine.this.parseExpression(block.body, b > codeStart ? scope : null));
                }
            }
            source = blocks.toArray(new Block[blocks.size()]);
            exprs = uexprs.toArray(new TemplateExpression[uexprs.size()]);
        }

        /**
         * Private ctor used to expand deferred expressions during prepare.
         * @param thePrefix the directive prefix
         * @param theSource the source
         * @param theScript the script
         * @param theExprs the expressions
         */
        private TemplateScript(String thePrefix, Block[] theSource,
                               ASTJexlScript theScript, TemplateExpression[] theExprs) {
            prefix = thePrefix;
            source = theSource;
            script = theScript;
            exprs = theExprs;
        }

        @Override
        public String toString() {
            StringBuilder strb = new StringBuilder();
            for (Block block : source) {
                if (block.type == BlockType.DIRECTIVE) {
                    strb.append(prefix);
                }
                strb.append(block.toString());
                strb.append('\n');
            }
            return strb.toString();
        }

        @Override
        public String asString() {
            StringBuilder strb = new StringBuilder();
            int e = 0;
            for (int b = 0; b < source.length; ++b) {
                Block block = source[b];
                if (block.type == BlockType.DIRECTIVE) {
                    strb.append(prefix);
                } else {
                    exprs[e++].asString(strb);
                }
            }
            return strb.toString();
        }

        @Override
        public TemplateScript prepare(JexlContext context) {
            Engine.Frame frame = script.createFrame((Object[]) null);
            TemplateContext tcontext = new TemplateContext(context, frame, exprs, null);
            TemplateExpression[] immediates = new TemplateExpression[exprs.length];
            for (int e = 0; e < exprs.length; ++e) {
                immediates[e] = exprs[e].prepare(tcontext);
            }
            return new TemplateScript(prefix, source, script, immediates);
        }

        @Override
        public void evaluate(JexlContext context, Writer writer) {
            evaluate(context, writer, (Object[]) null);
        }

        @Override
        public void evaluate(JexlContext context, Writer writer, Object... args) {
            Engine.Frame frame = script.createFrame(args);
            TemplateContext tcontext = new TemplateContext(context, frame, exprs, writer);
            Interpreter interpreter = jexl.createInterpreter(tcontext, frame);
            interpreter.interpret(script);
        }
    }

    /**
     * The type of context to use during evaluation of templates.
     * <p>This context exposes its writer as '$jexl' to the scripts.</p>
     * <p>public for introspection purpose.</p>
     */
    public final class TemplateContext implements JexlContext, NamespaceResolver {
        /** The wrapped context. */
        private final JexlContext wrap;
        /** The array of TemplateEngine expressions. */
        private final UnifiedExpression[] exprs;
        /** The writer used to output. */
        private final Writer writer;
        /** The call frame. */
        private final Engine.Frame frame;

        /**
         * Creates a TemplateScript context instance.
         * @param jcontext the base context
         * @param jframe the calling frame
         * @param expressions the list of TemplateExpression from the TemplateScript to evaluate
         * @param out the output writer
         */
        protected TemplateContext(JexlContext jcontext, Engine.Frame jframe,
                                  UnifiedExpression[] expressions, Writer out) {
            wrap = jcontext;
            frame = jframe;
            exprs = expressions;
            writer = out;
        }

        /**
         * Gets this context calling frame.
         * @return the engine frame
         */
        public Engine.Frame getFrame() {
            return frame;
        }

        @Override
        public Object get(String name) {
            if ("$jexl".equals(name)) {
                return writer;
            } else {
                return wrap.get(name);
            }
        }

        @Override
        public void set(String name, Object value) {
            wrap.set(name, value);
        }

        @Override
        public boolean has(String name) {
            return wrap.has(name);
        }

        @Override
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
         * <p>Includes another template using this template initial context and writer.</p>
         * @param script the TemplateScript to evaluate
         * @param args the arguments
         */
        public void include(TemplateScript script, Object... args) {
            script.evaluate(wrap, writer, args);
        }

        /**
         * Prints a unified expression evaluation result.
         * @param e the expression number
         */
        public void print(int e) {
            if (e < 0 || e >= exprs.length) {
                return;
            }
            UnifiedExpression expr = exprs[e];
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
            TemplateExpression[] cexprs = composite.exprs;
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
                    JexlUberspect uber = getEngine().getUberspect();
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
     * @param pattern the pattern to match at start of sequence
     * @return the first position after end of pattern if it matches, -1 otherwise
     */
    protected int startsWith(CharSequence sequence, CharSequence pattern) {
        int s = 0;
        while (Character.isSpaceChar(sequence.charAt(s))) {
            s += 1;
        }
        sequence = sequence.subSequence(s, sequence.length());
        if (pattern.length() <= sequence.length()
                && sequence.subSequence(0, pattern.length()).equals(pattern)) {
            return s + pattern.length();
        } else {
            return -1;
        }
    }

    /**
     * Reads lines of a template grouping them by typed blocks.
     * @param prefix the directive prefix
     * @param source the source reader
     * @return the list of blocks
     */
    protected List<Block> readTemplate(final String prefix, Reader source) {
        try {
            int prefixLen = prefix.length();
            List<Block> blocks = new ArrayList<Block>();
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
                    Block block = new Block(type, strb.toString());
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
                        Block code = new Block(BlockType.DIRECTIVE, strb.toString());
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
                        Block verbatim = new Block(BlockType.VERBATIM, strb.toString());
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

    @Override
    public TemplateScript createTemplate(String prefix, Reader source, String... parms) {
        return new TemplateScript(prefix, source, parms);
    }

    @Override
    public TemplateScript createTemplate(String source, String... parms) {
        return new TemplateScript("$$", new StringReader(source), parms);
    }

    @Override
    public TemplateScript createTemplate(String source) {
        return new TemplateScript("$$", new StringReader(source), (String[]) null);
    }
}