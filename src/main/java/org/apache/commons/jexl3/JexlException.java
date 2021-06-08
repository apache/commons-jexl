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

package org.apache.commons.jexl3;

import org.apache.commons.jexl3.internal.Debugger;
import org.apache.commons.jexl3.parser.JavaccError;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParseException;
import org.apache.commons.jexl3.parser.TokenMgrException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * Wraps any error that might occur during interpretation of a script or expression.
 *
 * @since 2.0
 */
public class JexlException extends RuntimeException {
    private static final long serialVersionUID = 20210606123900L;

    /** The point of origin for this exception. */
    private final transient JexlNode mark;

    /** The debug info. */
    private final transient JexlInfo info;

    /** Maximum number of characters around exception location. */
    private static final int MAX_EXCHARLOC = 42;


    /**
     * Creates a new JexlException.
     *
     * @param node the node causing the error
     * @param msg  the error message
     */
    public JexlException(final JexlNode node, final String msg) {
        this(node, msg, null);
    }

    /**
     * Creates a new JexlException.
     *
     * @param node  the node causing the error
     * @param msg   the error message
     * @param cause the exception causing the error
     */
    public JexlException(final JexlNode node, final String msg, final Throwable cause) {
        this(node, msg != null ? msg : "", unwrap(cause), true);
    }

    /**
     * Creates a new JexlException.
     *
     * @param node  the node causing the error
     * @param msg   the error message
     * @param cause the exception causing the error
     * @param trace whether this exception has a stacktrace and can <em>not</em> be suppressed
     */
    protected JexlException(final JexlNode node, final String msg, final Throwable cause, boolean trace) {
        super(msg != null ? msg : "", unwrap(cause), !trace, trace);
        if (node != null) {
            mark = node;
            info = node.jexlInfo();
        } else {
            mark = null;
            info = null;
        }
    }

    /**
     * Creates a new JexlException.
     *
     * @param jinfo the debugging information associated
     * @param msg   the error message
     * @param cause the exception causing the error
     */
    public JexlException(final JexlInfo jinfo, final String msg, final Throwable cause) {
        super(msg != null ? msg : "", unwrap(cause));
        mark = null;
        info = jinfo;
    }

    /**
     * Gets the specific information for this exception.
     *
     * @return the information
     */
    public JexlInfo getInfo() {
        return detailedInfo(mark, info);
    }

    /**
     * Creates a string builder pre-filled with common error information (if possible).
     *
     * @param node the node
     * @return a string builder
     */
    private static StringBuilder errorAt(final JexlNode node) {
        final JexlInfo info = node != null? detailedInfo(node, node.jexlInfo()) : null;
        final StringBuilder msg = new StringBuilder();
        if (info != null) {
            msg.append(info.toString());
        } else {
            msg.append("?:");
        }
        msg.append(' ');
        return msg;
    }

    /**
     * Gets the most specific information attached to a node.
     *
     * @param node the node
     * @param info the information
     * @return the information or null
     * @deprecated 3.2
     */
    @Deprecated
    public static JexlInfo getInfo(final JexlNode node, final JexlInfo info) {
        return detailedInfo(node, info);
    }

    /**
     * Gets the most specific information attached to a node.
     *
     * @param node the node
     * @param info the information
     * @return the information or null
     */
    private static JexlInfo detailedInfo(final JexlNode node, final JexlInfo info) {
        if (info != null && node != null) {
            final Debugger dbg = new Debugger();
            if (dbg.debug(node)) {
                return new JexlInfo(info) {
                    @Override
                    public JexlInfo.Detail getDetail() {
                        return dbg;
                    }
                };
            }
        }
        return info;
    }

    /**
     * Cleans a JexlException from any org.apache.commons.jexl3.internal stack trace element.
     *
     * @return this exception
     */
    public JexlException clean() {
        return clean(this);
    }

    /**
     * Cleans a Throwable from any org.apache.commons.jexl3.internal stack trace element.
     *
     * @param <X>    the throwable type
     * @param xthrow the thowable
     * @return the throwable
     */
    private static <X extends Throwable> X clean(final X xthrow) {
        if (xthrow != null) {
            final List<StackTraceElement> stackJexl = new ArrayList<>();
            for (final StackTraceElement se : xthrow.getStackTrace()) {
                final String className = se.getClassName();
                if (!className.startsWith("org.apache.commons.jexl3.internal")
                        && !className.startsWith("org.apache.commons.jexl3.parser")) {
                    stackJexl.add(se);
                }
            }
            xthrow.setStackTrace(stackJexl.toArray(new StackTraceElement[stackJexl.size()]));
        }
        return xthrow;
    }

    /**
     * Unwraps the cause of a throwable due to reflection.
     *
     * @param xthrow the throwable
     * @return the cause
     */
    private static Throwable unwrap(final Throwable xthrow) {
        if (xthrow instanceof TryFailed
            || xthrow instanceof InvocationTargetException
            || xthrow instanceof UndeclaredThrowableException) {
            return xthrow.getCause();
        }
        return xthrow;
    }

    /**
     * Merge the node info and the cause info to obtain best possible location.
     *
     * @param info  the node
     * @param cause the cause
     * @return the info to use
     */
    private static JexlInfo merge(final JexlInfo info, final JavaccError cause) {
        if (cause == null || cause.getLine() < 0) {
            return info;
        }
        if (info == null) {
            return new JexlInfo("", cause.getLine(), cause.getColumn());
        }
        return new JexlInfo(info.getName(), cause.getLine(), cause.getColumn());
    }

    /**
     * Accesses detailed message.
     *
     * @return the message
     */
    protected String detailedMessage() {
        Class<? extends JexlException> clazz = getClass();
        String name = clazz == JexlException.class? "JEXL" : clazz.getSimpleName().toLowerCase();
        return name + " error : " + getDetail();
    }

    /**
     * @return this exception specific detail
     * @since 3.2
     */
    public final String getDetail() {
        return super.getMessage();
    }

    /**
     * Formats an error message from the parser.
     *
     * @param prefix the prefix to the message
     * @param expr   the expression in error
     * @return the formatted message
     */
    protected String parserError(final String prefix, final String expr) {
        final int length = expr.length();
        if (length < MAX_EXCHARLOC) {
            return prefix + " error in '" + expr + "'";
        }
        final int me = MAX_EXCHARLOC / 2;
        int begin = info.getColumn() - me;
        if (begin < 0 || length < me) {
            begin = 0;
        } else if (begin > length) {
            begin = me;
        }
        int end = begin + MAX_EXCHARLOC;
        if (end > length) {
            end = length;
        }
        return prefix + " error near '... "
                + expr.substring(begin, end) + " ...'";
    }

    /**
     * Pleasing checkstyle.
     * @return the info
     */
    protected JexlInfo info() {
        return info;
    }

    /**
     * Thrown when tokenization fails.
     *
     * @since 3.0
     */
    public static class Tokenization extends JexlException {
        private static final long serialVersionUID = 20210606123901L;
        /**
         * Creates a new Tokenization exception instance.
         * @param info  the location info
         * @param cause the javacc cause
         */
        public Tokenization(final JexlInfo info, final TokenMgrException cause) {
            super(merge(info, cause), Objects.requireNonNull(cause).getAfter(), null);
        }

        @Override
        protected String detailedMessage() {
            return parserError("tokenization", getDetail());
        }
    }

    /**
     * Thrown when parsing fails.
     *
     * @since 3.0
     */
    public static class Parsing extends JexlException {
        private static final long serialVersionUID = 20210606123902L;
        /**
         * Creates a new Parsing exception instance.
         *
         * @param info  the location information
         * @param cause the javacc cause
         */
        public Parsing(final JexlInfo info, final ParseException cause) {
            super(merge(info, cause), Objects.requireNonNull(cause).getAfter(), null);
        }

        /**
         * Creates a new Parsing exception instance.
         *
         * @param info the location information
         * @param msg  the message
         */
        public Parsing(final JexlInfo info, final String msg) {
            super(info, msg, null);
        }

        @Override
        protected String detailedMessage() {
            return parserError("parsing", getDetail());
        }
    }

    /**
     * Thrown when parsing fails due to an ambiguous statement.
     *
     * @since 3.0
     */
    public static class Ambiguous extends Parsing {
        private static final long serialVersionUID = 20210606123903L;
        /** The mark at which ambiguity might stop and recover. */
        private final transient JexlInfo recover;
        /**
         * Creates a new Ambiguous statement exception instance.
         * @param info  the location information
         * @param expr  the source expression line
         */
        public Ambiguous(final JexlInfo info, final String expr) {
           this(info, null, expr);
        }

        /**
         * Creates a new Ambiguous statement exception instance.
         * @param begin  the start location information
         * @param end the end location information
         * @param expr  the source expression line
         */
        public Ambiguous(final JexlInfo begin, final JexlInfo end, final String expr) {
            super(begin, expr);
            recover = end;
        }

        @Override
        protected String detailedMessage() {
            return parserError("ambiguous statement", getDetail());
        }

        /**
         * Tries to remove this ambiguity in the source.
         * @param src the source that triggered this exception
         * @return the source with the ambiguous statement removed
         *         or null if no recovery was possible
         */
        public String tryCleanSource(final String src) {
            final JexlInfo ji = info();
            return ji == null || recover == null
                  ? src
                  : sliceSource(src, ji.getLine(), ji.getColumn(), recover.getLine(), recover.getColumn());
        }
    }

    /**
     * Removes a slice from a source.
     * @param src the source
     * @param froml the begin line
     * @param fromc the begin column
     * @param tol the to line
     * @param toc the to column
     * @return the source with the (begin) to (to) zone removed
     */
    public static String sliceSource(final String src, final int froml, final int fromc, final int tol, final int toc) {
        final BufferedReader reader = new BufferedReader(new StringReader(src));
        final StringBuilder buffer = new StringBuilder();
        String line;
        int cl = 1;
        try {
            while ((line = reader.readLine()) != null) {
                if (cl < froml || cl > tol) {
                    buffer.append(line).append('\n');
                } else {
                    if (cl == froml) {
                        buffer.append(line, 0, fromc - 1);
                    }
                    if (cl == tol) {
                        buffer.append(line.substring(toc + 1));
                    }
                } // else ignore line
                cl += 1;
            }
        } catch (final IOException xignore) {
            //damn the checked exceptions :-)
        }
        return buffer.toString();
    }

    /**
     * Thrown when reaching stack-overflow.
     *
     * @since 3.2
     */
    public static class StackOverflow extends JexlException {
        private static final long serialVersionUID = 20210606123904L;
        /**
         * Creates a new stack overflow exception instance.
         *
         * @param info  the location information
         * @param name  the unknown method
         * @param cause the exception causing the error
         */
        public StackOverflow(final JexlInfo info, final String name, final Throwable cause) {
            super(info, name, cause);
        }

        @Override
        protected String detailedMessage() {
            return "stack overflow " + getDetail();
        }
    }

    /**
     * Thrown when parsing fails due to an invalid assigment.
     *
     * @since 3.0
     */
    public static class Assignment extends Parsing {
        private static final long serialVersionUID = 20210606123905L;
        /**
         * Creates a new Assignment statement exception instance.
         *
         * @param info  the location information
         * @param expr  the source expression line
         */
        public Assignment(final JexlInfo info, final String expr) {
            super(info, expr);
        }

        @Override
        protected String detailedMessage() {
            return parserError("assignment", getDetail());
        }
    }

    /**
     * Thrown when parsing fails due to a disallowed feature.
     *
     * @since 3.2
     */
    public static class Feature extends Parsing {
        private static final long serialVersionUID = 20210606123906L;
        /** The feature code. */
        private final int code;
        /**
         * Creates a new Ambiguous statement exception instance.
         * @param info  the location information
         * @param feature the feature code
         * @param expr  the source expression line
         */
        public Feature(final JexlInfo info, final int feature, final String expr) {
            super(info, expr);
            this.code = feature;
        }

        @Override
        protected String detailedMessage() {
            return parserError(JexlFeatures.stringify(code), getDetail());
        }
    }

    /** Used 3 times. */
    private static final String VARQUOTE = "variable '";

    /**
     * The various type of variable issues.
     */
    public enum VariableIssue {
        /** The variable is undefined. */
        UNDEFINED,
        /** The variable is already declared. */
        REDEFINED,
        /** The variable has a null value. */
        NULLVALUE;

        /**
         * Stringifies the variable issue.
         * @param var the variable name
         * @return the issue message
         */
        public String message(final String var) {
            switch(this) {
                case NULLVALUE : return VARQUOTE + var + "' is null";
                case REDEFINED : return VARQUOTE + var + "' is already defined";
                case UNDEFINED :
                default: return VARQUOTE + var + "' is undefined";
            }
        }
    }

    /**
     * Thrown when a variable is unknown.
     *
     * @since 3.0
     */
    public static class Variable extends JexlException {
        private static final long serialVersionUID = 20210606123907L;
        /**
         * Undefined variable flag.
         */
        private final VariableIssue issue;

        /**
         * Creates a new Variable exception instance.
         *
         * @param node the offending ASTnode
         * @param var  the unknown variable
         * @param vi   the variable issue
         */
        public Variable(final JexlNode node, final String var, final VariableIssue vi) {
            super(node, var, null);
            issue = vi;
        }

        /**
         * Creates a new Variable exception instance.
         *
         * @param node the offending ASTnode
         * @param var  the unknown variable
         * @param undef whether the variable is undefined or evaluated as null
         */
        public Variable(final JexlNode node, final String var, final boolean undef) {
            this(node, var,  undef ? VariableIssue.UNDEFINED : VariableIssue.NULLVALUE);
        }

        /**
         * Whether the variable causing an error is undefined or evaluated as null.
         *
         * @return true if undefined, false otherwise
         */
        public boolean isUndefined() {
            return issue == VariableIssue.UNDEFINED;
        }

        /**
         * @return the variable name
         */
        public String getVariable() {
            return getDetail();
        }

        @Override
        protected String detailedMessage() {
            return issue.message(getVariable());
        }
    }

    /**
     * Generates a message for a variable error.
     *
     * @param node the node where the error occurred
     * @param variable the variable
     * @param undef whether the variable is null or undefined
     * @return the error message
     * @deprecated 3.2
     */
    @Deprecated
    public static String variableError(final JexlNode node, final String variable, final boolean undef) {
        return variableError(node, variable, undef? VariableIssue.UNDEFINED : VariableIssue.NULLVALUE);
    }

    /**
     * Generates a message for a variable error.
     *
     * @param node the node where the error occurred
     * @param variable the variable
     * @param issue  the variable kind of issue
     * @return the error message
     */
    public static String variableError(final JexlNode node, final String variable, final VariableIssue issue) {
        final StringBuilder msg = errorAt(node);
        msg.append(issue.message(variable));
        return msg.toString();
    }

    /**
     * Thrown when a property is unknown.
     *
     * @since 3.0
     */
    public static class Property extends JexlException {
        private static final long serialVersionUID = 20210606123908L;
        /**
         * Undefined variable flag.
         */
        private final boolean undefined;

        /**
         * Creates a new Property exception instance.
         *
         * @param node the offending ASTnode
         * @param pty  the unknown property
         * @deprecated 3.2
         */
        @Deprecated
        public Property(final JexlNode node, final String pty) {
            this(node, pty, true, null);
        }
        /**
         * Creates a new Property exception instance.
         *
         * @param node the offending ASTnode
         * @param pty  the unknown property
         * @param cause the exception causing the error
         * @deprecated 3.2
         */
        @Deprecated
        public Property(final JexlNode node, final String pty, final Throwable cause) {
            this(node, pty, true, cause);
        }

        /**
         * Creates a new Property exception instance.
         *
         * @param node the offending ASTnode
         * @param pty  the unknown property
         * @param undef whether the variable is null or undefined
         * @param cause the exception causing the error
         */
        public Property(final JexlNode node, final String pty, final boolean undef, final Throwable cause) {
            super(node, pty, cause);
            undefined = undef;
        }

        /**
         * Whether the variable causing an error is undefined or evaluated as null.
         *
         * @return true if undefined, false otherwise
         */
        public boolean isUndefined() {
            return undefined;
        }

        /**
         * @return the property name
         */
        public String getProperty() {
            return getDetail();
        }

        @Override
        protected String detailedMessage() {
            return (undefined? "undefined" : "null value") + " property '" + getProperty() + "'";
        }
    }

    /**
     * Generates a message for an unsolvable property error.
     *
     * @param node the node where the error occurred
     * @param pty the property
     * @param undef whether the property is null or undefined
     * @return the error message
     */
    public static String propertyError(final JexlNode node, final String pty, final boolean undef) {
        final StringBuilder msg = errorAt(node);
        if (undef) {
            msg.append("unsolvable");
        } else {
            msg.append("null value");
        }
        msg.append(" property '");
        msg.append(pty);
        msg.append('\'');
        return msg.toString();
    }

    /**
     * Generates a message for an unsolvable property error.
     *
     * @param node the node where the error occurred
     * @param var the variable
     * @return the error message
     * @deprecated 3.2
     */
    @Deprecated
    public static String propertyError(final JexlNode node, final String var) {
        return propertyError(node, var, true);
    }

    /**
     * Thrown when a method or ctor is unknown, ambiguous or inaccessible.
     *
     * @since 3.0
     */
    public static class Method extends JexlException {
        private static final long serialVersionUID = 20210606123909L;
        /**
         * Creates a new Method exception instance.
         *
         * @param node  the offending ASTnode
         * @param name  the method name
         * @deprecated as of 3.2, use call with method arguments
         */
        @Deprecated
        public Method(final JexlNode node, final String name) {
            this(node, name, null);
        }

        /**
         * Creates a new Method exception instance.
         *
         * @param info  the location information
         * @param name  the unknown method
         * @param cause the exception causing the error
         * @deprecated as of 3.2, use call with method arguments
         */
        @Deprecated
        public Method(final JexlInfo info, final String name, final Throwable cause) {
            this(info, name, null, cause);
        }

        /**
         * Creates a new Method exception instance.
         *
         * @param node  the offending ASTnode
         * @param name  the method name
         * @param args  the method arguments
         * @since 3.2
         */
        public Method(final JexlNode node, final String name, final Object[] args) {
            super(node, methodSignature(name, args));
        }

        /**
         * Creates a new Method exception instance.
         *
         * @param info  the location information
         * @param name  the method name
         * @param args  the method arguments
         * @since 3.2
         */
        public Method(final JexlInfo info, final String name, final Object[] args) {
            this(info, name, args, null);
        }


        /**
         * Creates a new Method exception instance.
         *
         * @param info  the location information
         * @param name  the method name
         * @param cause the exception causing the error
         * @param args  the method arguments
         * @since 3.2
         */
        public Method(final JexlInfo info, final String name, final Object[] args, final Throwable cause) {
            super(info, methodSignature(name, args), cause);
        }

        /**
         * @return the method name
         */
        public String getMethod() {
            final String signature = getMethodSignature();
            final int lparen = signature.indexOf('(');
            return lparen > 0? signature.substring(0, lparen) : signature;
        }

        /**
         * @return the method signature
         * @since 3.2
         */
        public String getMethodSignature() {
            return getDetail();
        }

        @Override
        protected String detailedMessage() {
            return "unsolvable function/method '" + getMethodSignature() + "'";
        }
    }

    /**
     * Creates a signed-name for a given method name and arguments.
     * @param name the method name
     * @param args the method arguments
     * @return a suitable signed name
     */
    private static String methodSignature(final String name, final Object[] args) {
        if (args != null && args.length > 0) {
            final StringBuilder strb = new StringBuilder(name);
            strb.append('(');
            for (int a = 0; a < args.length; ++a) {
                if (a > 0) {
                    strb.append(", ");
                }
                final Class<?> clazz = args[a] == null ? Object.class : args[a].getClass();
                strb.append(clazz.getSimpleName());
            }
            strb.append(')');
            return strb.toString();
        }
        return name;
    }

    /**
     * Generates a message for a unsolvable method error.
     *
     * @param node the node where the error occurred
     * @param method the method name
     * @return the error message
     * @deprecated 3.2
     */
    @Deprecated
    public static String methodError(final JexlNode node, final String method) {
        return methodError(node, method, null);
    }

    /**
     * Generates a message for a unsolvable method error.
     *
     * @param node the node where the error occurred
     * @param method the method name
     * @param args the method arguments
     * @return the error message
     */
    public static String methodError(final JexlNode node, final String method, final Object[] args) {
        final StringBuilder msg = errorAt(node);
        msg.append("unsolvable function/method '");
        msg.append(methodSignature(method, args));
        msg.append('\'');
        return msg.toString();
    }

    /**
     * Thrown when an operator fails.
     *
     * @since 3.0
     */
    public static class Operator extends JexlException {
        private static final long serialVersionUID = 20210606124100L;
        /**
         * Creates a new Operator exception instance.
         *
         * @param node  the location information
         * @param symbol  the operator name
         * @param cause the exception causing the error
         */
        public Operator(final JexlNode node, final String symbol, final Throwable cause) {
            super(node, symbol, cause);
        }

        /**
         * @return the method name
         */
        public String getSymbol() {
            return getDetail();
        }

        @Override
        protected String detailedMessage() {
            return "error calling operator '" + getSymbol() + "'";
        }
    }

    /**
     * Generates a message for an operator error.
     *
     * @param node the node where the error occurred
     * @param symbol the operator name
     * @return the error message
     */
    public static String operatorError(final JexlNode node, final String symbol) {
        final StringBuilder msg = errorAt(node);
        msg.append("error calling operator '");
        msg.append(symbol);
        msg.append('\'');
        return msg.toString();
    }

    /**
     * Thrown when an annotation handler throws an exception.
     *
     * @since 3.1
     */
    public static class Annotation extends JexlException {
        private static final long serialVersionUID = 20210606124101L;
        /**
         * Creates a new Annotation exception instance.
         *
         * @param node  the annotated statement node
         * @param name  the annotation name
         * @param cause the exception causing the error
         */
        public Annotation(final JexlNode node, final String name, final Throwable cause) {
            super(node, name, cause);
        }

        /**
         * @return the annotation name
         */
        public String getAnnotation() {
            return getDetail();
        }

        @Override
        protected String detailedMessage() {
            return "error processing annotation '" + getAnnotation() + "'";
        }
    }

    /**
     * Generates a message for an annotation error.
     *
     * @param node the node where the error occurred
     * @param annotation the annotation name
     * @return the error message
     * @since 3.1
     */
    public static String annotationError(final JexlNode node, final String annotation) {
        final StringBuilder msg = errorAt(node);
        msg.append("error processing annotation '");
        msg.append(annotation);
        msg.append('\'');
        return msg.toString();
    }

    /**
     * Thrown to return a value.
     *
     * @since 3.0
     */
    public static class Return extends JexlException {
        private static final long serialVersionUID = 20210606124102L;

        /** The returned value. */
        private final transient Object result;

        /**
         * Creates a new instance of Return.
         *
         * @param node  the return node
         * @param msg   the message
         * @param value the returned value
         */
        public Return(final JexlNode node, final String msg, final Object value) {
            super(node, msg, null, false);
            this.result = value;
        }

        /**
         * @return the returned value
         */
        public Object getValue() {
            return result;
        }
    }

    /**
     * Thrown to cancel a script execution.
     *
     * @since 3.0
     */
    public static class Cancel extends JexlException {
        /**
         * Creates a new instance of Cancel.
         *
         * @param node the node where the interruption was detected
         */
        public Cancel(final JexlNode node) {
            super(node, "execution cancelled", null);
        }
    }

    /**
     * Thrown to break a loop.
     *
     * @since 3.0
     */
    public static class Break extends JexlException {
        private static final long serialVersionUID = 20210606124103L;
        /**
         * Creates a new instance of Break.
         *
         * @param node the break
         */
        public Break(final JexlNode node) {
            super(node, "break loop", null, false);
        }
    }

    /**
     * Thrown to continue a loop.
     *
     * @since 3.0
     */
    public static class Continue extends JexlException {
        private static final long serialVersionUID = 20210606124104L;
        /**
         * Creates a new instance of Continue.
         *
         * @param node the continue
         */
        public Continue(final JexlNode node) {
            super(node, "continue loop", null, false);
        }
    }

    /**
     * Thrown when method/ctor invocation fails.
     * <p>These wrap InvocationTargetException as runtime exception
     * allowing to go through without signature modifications.
     * @since 3.2
     */
    public static class TryFailed extends JexlException {
        private static final long serialVersionUID = 20210606124105L;
        /**
         * Creates a new instance.
         * @param xany the original invocation target exception
         */
        private TryFailed(final InvocationTargetException xany) {
            super((JexlInfo) null, "tryFailed", xany.getCause());
        }
    }

    /**
     * Wrap an invocation exception.
     * <p>Return the cause if it is already a JexlException.
     * @param xinvoke the invocation exception
     * @return a JexlException
     */
    public static JexlException tryFailed(final InvocationTargetException xinvoke) {
        final Throwable cause = xinvoke.getCause();
        return cause instanceof JexlException
                ? (JexlException) cause
                : new JexlException.TryFailed(xinvoke); // fail
    }


    /**
     * Detailed info message about this error.
     * Format is "debug![begin,end]: string \n msg" where:
     *
     * - debug is the debugging information if it exists (@link JexlEngine.setDebug)
     * - begin, end are character offsets in the string for the precise location of the error
     * - string is the string representation of the offending expression
     * - msg is the actual explanation message for this error
     *
     * @return this error as a string
     */
    @Override
    public String getMessage() {
        final StringBuilder msg = new StringBuilder();
        if (info != null) {
            msg.append(info.toString());
        } else {
            msg.append("?:");
        }
        msg.append(' ');
        msg.append(detailedMessage());
        final Throwable cause = getCause();
        if (cause instanceof JexlArithmetic.NullOperand) {
            msg.append(" caused by null operand");
        }
        return msg.toString();
    }
}
