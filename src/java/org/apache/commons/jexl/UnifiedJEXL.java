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

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.jexl.parser.ParseException;

/**
 * An evaluator similar to the unified EL evaluator used in JSP/JSF based on JEXL.
 * It is intended to be used in configuration modules, XML based frameworks or JSP taglibs
 * and facilitate the implementation of expression evaluation.
 * <p>
 * An expression can mix immediate, deferred and nested sub-expressions as well as string constants;<ol>
 * <li>The "immediate" syntax is of the form "...${jexl-expr}..."</li>
 * <li>The "deferred" syntax is of the form "...#{jexl-expr}..."</li>
 * <li>The "nested" syntax is of the form "...#{...${jexl-expr0}...}..."</li>
 * <li>The "composite" syntax is of the form "...${jexl-expr0}... #{jexl-expr1}..."</li>
 * </ol>
 * </p>
 * <p>
 * Deferred & immediate expression carry different intentions:
 * <ol>
 * <li>An immediate expression indicate that evaluation is intended to be performed close to
 * the definition/parsing point.</li>
 * <li>A deferred expression indicate that evaluation is intended to occur at a later stage.</li>
 * </ol>
 * </p>
 * <p>
 * For instance: "Hello ${name}, now is #{time}" is a composite "deferred" expression since one
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
 */
public class UnifiedJEXL {
    /** The engine for this expression. */
    private final JexlEngine jexl;
    /** The expression cache. */
    private final Map<String, Expression> cache;

    /**
     * Creates a new instance of UnifiedJEXL with a default size cache.
     * @param jexl the JexlEngine to use.
     */
    public UnifiedJEXL(JexlEngine jexl) {
        this(jexl, 512);
    }

    /**
     * Creates a new instance of UnifiedJEXL creating a local cache.
     * @param jexl the JexlEngine to use.
     * @param cacheSize the number of expressions in this cache
     */
    public UnifiedJEXL(JexlEngine jexl, int cacheSize) {
        this.jexl = jexl;
        this.cache = cacheSize > 0 ? createCache(cacheSize) : null;
    }

    /**
     * Creates an expression cache.
     * @param size the cache size, must be > 0
     * @return a LinkedHashMap
     */
    static private Map<String, Expression> createCache(final int cacheSize) {
        return new LinkedHashMap<String, Expression>(cacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > cacheSize;
            }
        };
    }

    /**
     * Types of expressions.
     * Each instance carries a counter index per (composite sub-) expression type.
     * @see ExpressionBuilder
     */
    private static enum ExpressionType {
        CONSTANT(0), // constant count at index 0
        IMMEDIATE(1), // immediate count at index 1
        DEFERRED(2), // deferred count at index 2
        NESTED(2), // nested are counted as deferred thus count at index 2
        COMPOSITE(-1); // composite are not counted
        /** the index in arrays of expression counters for composite expressions. */
        private final int index;
        ExpressionType(int index) {
            this.index = index;
        }
    }

    /**
     * A helper class to build expressions.
     * Keeps count of sub-expressions by type.
     */
    private static class ExpressionBuilder {
        final int[] counts;
        final ArrayList<Expression> expressions;

        ExpressionBuilder(int size) {
           counts = new int[]{0, 0, 0};
           expressions = new ArrayList<Expression>(size <= 0? 3 : size);
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
         * Builds an expression from a source, performs checks
         * @param el the unified el instance
         * @param source the source expression
         * @return an expression
         */
        Expression build(UnifiedJEXL el, Expression source) {
            int sum = 0;
            for(int i : counts) sum += i;
            if (expressions.size() != sum) {
                String error = "parsing algorithm error, exprs: " + expressions.size() +
                   ", constant:" + counts[ExpressionType.CONSTANT.index] +
                   ", immediate:" + counts[ExpressionType.IMMEDIATE.index] +
                   ", deferred:" + counts[ExpressionType.DEFERRED.index];
                throw new IllegalStateException(error);
            }
            // if only one sub-expr, no need to create a composite
            return (expressions.size() == 1)?
                    expressions.get(0) :
                    el.new Composite(counts, expressions, source);
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
     * The sole type of (runtime) exception the UnifiedJEXL can throw.
     */
    public class Exception extends RuntimeException {
        public Exception(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * The abstract base class for all expressions, immediate '${...}' and deferred '#{...}'.
     */
    public abstract class Expression {
        final Expression source;

        Expression(Expression source) {
            this.source = source != null ? source : this;
        }

        /**
         * Generate the string corresponding to this expression.
         * @return the string representation
         */
        public String asString() {
            return toString();
        }

        /**
         * When the expression is dependant upon immediate and deferred sub-expressions,
         * evaluates the immediate sub-expressions with the context passed as parameter
         * and returns this expression deferred form.
         * <p>
         * In effect, this binds the result of the immediate sub-expressions evaluation in the
         * context, allowing to differ evaluation of the remaining (deferred) expression within another context.
         * This only has an effect to nested & composite expressions that contain differed & immediate sub-expressions.
         * </p>
         * <p>
         * If the underlying JEXL engine is silent, errors will be logged through its logger as info.
         * </p>
         * @param context the context to use for immediate expression evaluations
         * @return  an expression or null if an error occurs and the {@link JexlEngine} is silent
         * @throws {@link Exception} if any error occurs and the {@link JexlEngine} is not silent
         */
        public abstract Expression prepare(JexlContext context);

        /**
         * Evaluates this expression.
         * <p>
         * If the underlying JEXL engine is silent, errors will be logged through its logger as info.
         * </p>
         * @param context the variable context
         * @return the result of this expression evaluation or null if an error occurs and the {@link JexlEngine} is silent
         * @throws [@link Exception} if an error occurs and the {@link JexlEngine} is not silent
         */
        public abstract Object evaluate(JexlContext context);

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
         * Gets this expression type.
         * @return its type
         */
        abstract ExpressionType getType();

        /**
         * Prepares a sub-expression for interpretation.
         * @param interpreter a JEXL interpreter
         * @return a prepared expression
         * @throws ParseException (only for nested & composite)
         */
        abstract Expression prepare(Interpreter interpreter) throws ParseException;

        /**
         * Intreprets a sub-expression.
         * @param interpreter a JEXL interpreter
         * @return the result of interpretation
         * @throws ParseException (only for nested & composite)
         */
        abstract Object evaluate(Interpreter interpreter) throws ParseException;
    }


    /** A constant expression. */
    private class Constant extends Expression {
        private final Object value;

        Constant(Object value, Expression source) {
            super(source);
            if (value == null) {
                throw new NullPointerException("constant can not be null");
            }
            this.value = value;
        }

        ExpressionType getType() {
            return ExpressionType.CONSTANT;
        }

        @Override
        public String toString() {
            String str = value.toString();
            if (value instanceof String || value instanceof CharSequence) {
                StringBuilder strb = new StringBuilder(str.length() + 2);
                if (source != this) {
                    strb.append(source.toString());
                    strb.append(" /*= ");
                }
                strb.append('"');
                for (int i = 0, size = str.length(); i < size; ++i) {
                    char c = str.charAt(i);
                    if (c == '"')
                        strb.append('\\');
                    strb.append(c);
                }
                strb.append('"');
                if (source != this) {
                    strb.append(" */");
                }
                return strb.toString();
            }
            return str;
        }

        @Override
        public String asString() {
            return value.toString();
        }

        @Override
        public Expression prepare(JexlContext context) {
            return this;
        }

        @Override
        Expression prepare(Interpreter interpreter) throws ParseException {
            return this;
        }

        @Override
        public Object evaluate(JexlContext context) {
            return value;
        }

        @Override
        Object evaluate(Interpreter interpreter) throws ParseException {
            return value;
        }

    }


    /** An immediate expression: ${jexl}. */
    private class Immediate extends Expression {
        private final CharSequence expr;
        private final SimpleNode node;

        Immediate(CharSequence expr, SimpleNode node, Expression source) {
            super(source);
            this.expr = expr;
            this.node = node;
        }

        @Override
        ExpressionType getType() {
            return ExpressionType.IMMEDIATE;
        }

        @Override
        public String toString() {
            StringBuilder strb = new StringBuilder(expr.length() + 3);
            if (source != this) {
                strb.append(source.toString());
                strb.append(" /*= ");
            }
            strb.append("${");
            strb.append(expr);
            strb.append("}");
            if (source != this) {
                strb.append(" */");
            }
            return strb.toString();
        }

        @Override
        public String asString() {
            return expr.toString();
        }

        @Override
        public Expression prepare(JexlContext context) {
            return this;
        }

        @Override
        Expression prepare(Interpreter interpreter) throws ParseException {
            return this;
        }

        @Override
        public Object evaluate(JexlContext context) {
            return UnifiedJEXL.this.evaluate(context, this);
        }

        @Override
        Object evaluate(Interpreter interpreter) throws ParseException {
            return interpreter.interpret(node);
        }

    }


    /** A deferred expression: #{jexl}. */
    private class Deferred extends Expression {
        protected final CharSequence expr;
        protected final SimpleNode node;

        Deferred(CharSequence expr, SimpleNode node, Expression source) {
            super(source);
            this.expr = expr.toString();
            this.node = node;
        }

        @Override
        public boolean isImmediate() {
            return false;
        }

        ExpressionType getType() {
            return ExpressionType.DEFERRED;
        }

        @Override
        public String toString() {
            StringBuilder strb = new StringBuilder(expr.length() + 3);
            if (source != this) {
                strb.append(source.toString());
                strb.append(" /*= ");
            }
            strb.append("#{");
            strb.append(expr);
            strb.append("}");
            if (source != this) {
                strb.append(" */");
            }
            return strb.toString();
        }

        @Override
        public String asString() {
            return expr.toString();
        }

        @Override
        public Expression prepare(JexlContext context) {
            return this;
        }

        @Override
        Expression prepare(Interpreter interpreter) throws ParseException {
            return this;
        }

        @Override
        public Object evaluate(JexlContext context) {
            return UnifiedJEXL.this.evaluate(context, this);
        }

        @Override
        Object evaluate(Interpreter interpreter) throws ParseException {
            return interpreter.interpret(node);
        }
    }


    /**
     * A deferred expression that nests an immediate expression.
     * #{...${jexl}...}
     * Note that the deferred syntax is JEXL's, not UnifiedJEXL.
     */
    private class Nested extends Deferred {
        Nested(CharSequence expr, SimpleNode node, Expression source) {
            super(expr, node, source);
            if (this.source != this) {
                throw new IllegalArgumentException("Nested expression can not have a source");
            }
        }
        
        @Override
        ExpressionType getType() {
            return ExpressionType.NESTED;
        }

        @Override
        public String toString() {
            return expr.toString();
        }

        @Override
        public Expression prepare(JexlContext context) {
            return UnifiedJEXL.this.prepare(context, this);
        }

        @Override
        public Expression prepare(Interpreter interpreter) throws ParseException {
            String value = interpreter.interpret(node).toString();
            SimpleNode dnode = toNode(value);
            return new Deferred(value, dnode, this);
        }

        @Override
        public Object evaluate(Interpreter interpreter) throws ParseException {
            return prepare(interpreter).evaluate(interpreter);
        }
    }


    /** A composite expression: "... ${...} ... #{...} ...". */
    private class Composite extends Expression {
        // bit encoded (deferred count > 0) bit 1, (immediate count > 0) bit 0
        final int meta;
        // the list of expression resulting from parsing
        final Expression[] exprs;

        Composite(int[] counts, ArrayList<Expression> exprs, Expression source) {
            super(source);
            this.exprs = exprs.toArray(new Expression[exprs.size()]);
            this.meta = (counts[ExpressionType.DEFERRED.index] > 0? 2 : 0) |
                        (counts[ExpressionType.IMMEDIATE.index] > 0? 1 : 0);
        }

        int size() {
            return exprs.length;
        }

        ExpressionType getType() {
            return ExpressionType.COMPOSITE;
        }

        @Override
        public boolean isImmediate() {
            // immediate if no deferred
            return (meta & 2) != 0;
        }
        
        @Override
        public String toString() {
            StringBuilder strb = new StringBuilder();
            if (source != this) {
                strb.append(source.toString());
                strb.append(" /*= ");
            }
            for (Expression e : exprs) {
                strb.append(e.toString());
            }
            if (source != this) {
                strb.append(" */ ");
            }
            return strb.toString();
        }

        @Override
        public String asString() {
            StringBuilder strb = new StringBuilder();
            for (Expression e : exprs) {
                strb.append(e.asString());
            }
            return strb.toString();
        }

        @Override
        public Expression prepare(JexlContext context) {
            return UnifiedJEXL.this.prepare(context, this);
        }

        @Override
        Expression prepare(Interpreter interpreter) throws ParseException {
            // if this composite is not its own source, it is already prepared
            if (source != this) return this;
            // we need to eval immediate expressions if there are some deferred/nested
            // ie both immediate & deferred counts > 0, bits 1 & 0 set, (1 << 1) & 1 == 3
            final boolean evalImmediate = meta == 3;
            final int size = size();
            final ExpressionBuilder builder = new ExpressionBuilder(size);
            // tracking whether prepare will return a different expression
            boolean eq = true;
            for(int e = 0; e < size; ++e) {
                Expression expr = exprs[e];
                Expression prepared = expr.prepare(interpreter);
                if (evalImmediate && prepared instanceof Immediate) {
                    // evaluate immediate as constant
                    Object value = prepared.evaluate(interpreter);
                    prepared = value == null? null : new Constant(value, prepared);
                }
                // add it if not null
                if (prepared != null) {
                    builder.add(prepared);
                }
                // keep track of expression equivalence
                eq &= expr == prepared;
            }
            Expression ready = eq? this : builder.build(UnifiedJEXL.this, this);
            return ready;
        }


        @Override
        public Object evaluate(JexlContext context) {
            return UnifiedJEXL.this.evaluate(context, this);
        }

        @Override
        Object evaluate(Interpreter interpreter) throws ParseException {
            final int size = size();
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
     * If the underlying JEXL engine is silent, errors will be logged through its logger as info.
     * </p>
     * @param expression the UnifiedJEXL string expression
     * @return the UnifiedJEXL object expression, null if silent and an error occured
     * @throws [@link Exception} if an error occurs and the {@link JexlEngine} is not silent
     */
    public Expression parse(String expression) {
        try {
            if (cache == null) {
                return parseExpression(expression);
            }
            else synchronized (cache) {
                Expression stmt = cache.get(expression);
                if (stmt == null) {
                    stmt = parseExpression(expression);
                    cache.put(expression, stmt);
                }
                return stmt;
            }
        }
        catch(JexlException xjexl) {
            Exception xuel = new Exception("failed to parse '" + expression + "'", xjexl);
            if (jexl.isSilent()) {
                jexl.LOG.warn(xuel.getMessage(), xuel.getCause());
                return null;
            }
            throw xuel;
        }
        catch(ParseException xparse) {
            Exception xuel = new Exception("failed to parse '" + expression + "'", xparse);
            if (jexl.isSilent()) {
                jexl.LOG.warn(xuel.getMessage(), xuel.getCause());
                return null;
            }
            throw xuel;
        }
    }

    /**
     * Prepares an expression (nested & composites), handles exception reporting.
     *
     * @param context the JEXL context to use
     * @param expr the expression to prepare
     * @return a prepared expression
     */
    Expression prepare(JexlContext context, Expression expr) {
        try {
            Interpreter interpreter = jexl.createInterpreter(context);
            interpreter.setSilent(false);
            return expr.prepare(interpreter);
        }
        catch (JexlException xjexl) {
            Exception xuel = createException("prepare" , expr, xjexl);
            if (jexl.isSilent()) {
                jexl.LOG.warn(xuel.getMessage(), xuel.getCause());
                return null;
            }
            throw xuel;
        }
        catch (ParseException xparse) {
            Exception xuel = createException("prepare" , expr, xparse);
            if (jexl.isSilent()) {
                jexl.LOG.warn(xuel.getMessage(), xuel.getCause());
                return null;
            }
            throw xuel;
        }
    }

    /**
     * Evaluates an expression (nested & composites), handles exception reporting.
     *
     * @param context the JEXL context to use
     * @param expr the expression to prepare
     * @return the result of the evaluation
     */
    Object evaluate(JexlContext context, Expression expr) {
        try {
            Interpreter interpreter = jexl.createInterpreter(context);
            interpreter.setSilent(false);
            return expr.evaluate(interpreter);
        }
        catch (JexlException xjexl) {
            Exception xuel = createException("evaluate" , expr, xjexl);
            if (jexl.isSilent()) {
                jexl.LOG.warn(xuel.getMessage(), xuel.getCause());
                return null;
            }
            throw xuel;
        }
        catch (ParseException xparse) {
            Exception xuel = createException("evaluate" , expr, xparse);
            if (jexl.isSilent()) {
                jexl.LOG.warn(xuel.getMessage(), xuel.getCause());
                return null;
            }
            throw xuel;
        }
    }

    /**
     * Use the JEXL parser to create the AST for an expression.
     * @param expression the expression to parse
     * @return the AST
     */
    private SimpleNode toNode(CharSequence expression) throws ParseException {
        return (SimpleNode) jexl.parse(expression).jjtGetChild(0);
    }

    /**
     * Creates a UnifiedJEXL.Exception from a JexlException.
     * @param action parse, prepare, evaluate
     * @param expr the expression
     * @param xjexl the exception
     * @return a "meaningfull" error message
     */
    private Exception createException(String action, Expression expr, java.lang.Exception xany) {
        StringBuilder strb = new StringBuilder("failed to ");
        strb.append(action);
        strb.append(" '");
        strb.append(expr.toString());
        strb.append("'");
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
        CONST, // parsing a constant string
        IMMEDIATE0, // seen $
        DEFERRED0, // seen #
        IMMEDIATE1, // seen ${
        DEFERRED1, // seen #{
        ESCAPE // seen \
    }

    /**
     * Parses a unified expression
     * @param expr the expression
     * @param counts the expression type counters
     * @return the list of expressions
     * @throws Exception
     */
    private Expression parseExpression(String expr) throws ParseException {
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
                case CONST: {
                    if (c == '$') {
                        state = ParseState.IMMEDIATE0;
                    } else if (c == '#') {
                        inested = i;
                        state = ParseState.DEFERRED0;
                    } else if (c == '\\') {
                        state = ParseState.ESCAPE;
                    } else {
                        // do buildup expr
                        strb.append(c);
                    }
                    break;
                }
                case IMMEDIATE0: { // $
                    if (c == '{') {
                        state = ParseState.IMMEDIATE1;
                        // if chars in buffer, create constant
                        if (strb.length() > 0) {
                            Expression cexpr = new Constant(strb.toString(), null);
                            builder.add(cexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                        }
                    } else {
                        // revert to CONST
                        strb.append('$');
                        strb.append(c);
                        state = ParseState.CONST;
                    }
                    break;
                }
                case DEFERRED0: { // #
                    if (c == '{') {
                        state = ParseState.DEFERRED1;
                        // if chars in buffer, create constant
                        if (strb.length() > 0) {
                            Expression cexpr = new Constant(strb.toString(), null);
                            builder.add(cexpr);
                            strb.delete(0, Integer.MAX_VALUE);
                        }
                    } else {
                        // revert to CONST
                        strb.append('#');
                        strb.append(c);
                        state = ParseState.CONST;
                    }
                    break;
                }
                case IMMEDIATE1: { // ${...
                    if (c == '}') {
                        // materialize the immediate expr
                        //Expression iexpr = createExpression(ExpressionType.IMMEDIATE, strb, null);
                        Expression iexpr = new Immediate(strb.toString(), toNode(strb), null);
                        builder.add(iexpr);
                        strb.delete(0, Integer.MAX_VALUE);
                        state = ParseState.CONST;
                    } else {
                        // do buildup expr
                        strb.append(c);
                    }
                    break;
                }
                case DEFERRED1: { // #{...
                    // skip inner strings (for '}')
                    if (c == '"' || c == '\'') {
                        strb.append(c);
                        i = readDeferredString(strb, expr, i + 1, c);
                        continue;
                    }
                    // nested immediate in deferred; need to balance count of '{' & '}'
                    if (c == '{') {
                        if (expr.charAt(i - 1) == '$') {
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
                            Expression dexpr = nested?
                                               new Nested(expr.substring(inested, i+1), toNode(strb), null) :
                                               new Deferred(strb.toString(), toNode(strb), null);
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
                }
                case ESCAPE: {
                    if (c == '#') {
                        strb.append('#');
                    } else {
                        strb.append('\\');
                        strb.append(c);
                    }
                    state = ParseState.CONST;
                }

            }
        }
        // we should be in that state
        if (state != ParseState.CONST)
            throw new IllegalStateException("malformed expression: " + expr);
        // if any chars were buffered, add them as a constant
        if (strb.length() > 0) {
            Expression cexpr = new Constant(strb.toString(), null);
            builder.add(cexpr);
        }
        return builder.build(this, null);
    }

    /**
     * Read the remainder of a string till a given separator,
     * handles escaping through '\' syntax.
     * @param strb the destination buffer to copy characters into
     * @param str the origin
     * @param index the offset into the origin
     * @param sep the separator, single or double quote, marking end of string
     * @return the offset in origin
     */
    static private int readDeferredString(StringBuilder strb, String str, int index, char sep) {
        boolean escape = false;
        for(;index < str.length();++index) {
           char c = str.charAt(index);
           if (escape) {
               strb.append(c);
               escape = false;
               continue;
           }
           if (c == '\\') {
               escape = true;
               continue;
           }
           strb.append(c);
           if (c == sep) {
              break;
           }
        }
        return index;
    }
}
