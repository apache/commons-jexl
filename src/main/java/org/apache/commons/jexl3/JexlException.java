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
import org.apache.commons.jexl3.parser.TokenMgrError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
/**
 * Wraps any error that might occur during interpretation of a script or expression.
 *
 * @since 2.0
 */
public class JexlException extends RuntimeException {

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
    public JexlException(JexlNode node, String msg) {
        this(node, msg, null);
    }

    /**
     * Creates a new JexlException.
     *
     * @param node  the node causing the error
     * @param msg   the error message
     * @param cause the exception causing the error
     */
    public JexlException(JexlNode node, String msg, Throwable cause) {
        super(msg != null ? msg : "", unwrap(cause));
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
    public JexlException(JexlInfo jinfo, String msg, Throwable cause) {
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
        return getInfo(mark, info);
    }
    
    /**
     * Creates a string builder pre-filled with common error information (if possible).
     *
     * @param node the node
     * @return a string builder
     */
    private static StringBuilder errorAt(JexlNode node) {
        JexlInfo info = node != null? getInfo(node, node.jexlInfo()) : null;
        StringBuilder msg = new StringBuilder();
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
     */
    public static JexlInfo getInfo(JexlNode node, JexlInfo info) {
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
    private static <X extends Throwable> X clean(X xthrow) {
        if (xthrow != null) {
            List<StackTraceElement> stackJexl = new ArrayList<StackTraceElement>();
            for (StackTraceElement se : xthrow.getStackTrace()) {
                String className = se.getClassName();
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
    private static Throwable unwrap(Throwable xthrow) {
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
    private static JexlInfo merge(JexlInfo info, JavaccError cause) {
        JexlInfo dbgn = info != null ? info : null;
        if (cause == null) {
            return dbgn;
        } else if (dbgn == null) {
            return new JexlInfo("", cause.getLine(), cause.getColumn());
        } else {
            return new JexlInfo(dbgn.getName(), cause.getLine(), cause.getColumn());
        }
    }

    /**
     * Accesses detailed message.
     *
     * @return the message
     */
    protected String detailedMessage() {
        return super.getMessage();
    }

    /**
     * Formats an error message from the parser.
     *
     * @param prefix the prefix to the message
     * @param expr   the expression in error
     * @return the formatted message
     */
    protected String parserError(String prefix, String expr) {
        int length = expr.length();
        if (length < MAX_EXCHARLOC) {
            return prefix + " error in '" + expr + "'";
        } else {
            int begin = info.getColumn();
            int end = begin + (MAX_EXCHARLOC / 2);
            begin -= (MAX_EXCHARLOC / 2);
            if (begin < 0) {
                end -= begin;
                begin = 0;
            }
            return prefix + " error near '... "
                    + expr.substring(begin, end > length ? length : end) + " ...'";
        }
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
        /**
         * Creates a new Tokenization exception instance.
         * @param info  the location info
         * @param cause the javacc cause
         */
        public Tokenization(JexlInfo info, TokenMgrError cause) {
            super(merge(info, cause), cause.getAfter(), null);
        }

        /**
         * @return the specific detailed message
         */
        public String getDetail() {
            return super.detailedMessage();
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
        /**
         * Creates a new Parsing exception instance.
         *
         * @param info  the location information
         * @param cause the javacc cause
         */
        public Parsing(JexlInfo info, ParseException cause) {
            super(merge(info, cause), cause.getAfter(), null);
        }

        /**
         * Creates a new Parsing exception instance.
         *
         * @param info the location information
         * @param msg  the message
         */
        public Parsing(JexlInfo info, String msg) {
            super(info, msg, null);
        }

        /**
         * @return the specific detailed message
         */
        public String getDetail() {
            return super.detailedMessage();
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
        /** The mark at which ambiguity might stop and recover. */
        private JexlInfo recover = null;
        /**
         * Creates a new Ambiguous statement exception instance.
         * @param info  the location information
         * @param expr  the source expression line
         */
        public Ambiguous(JexlInfo info, String expr) {
           this(info, null, expr);
        }
                
        /**
         * Creates a new Ambiguous statement exception instance.
         * @param begin  the start location information
         * @param end the end location information
         * @param expr  the source expression line
         */
        public Ambiguous(JexlInfo begin, JexlInfo end, String expr) {
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
        public String tryCleanSource(String src) {
            JexlInfo ji = info();
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
    public static String sliceSource(String src, int froml, int fromc, int tol, int toc) {
        BufferedReader reader = new BufferedReader(new StringReader(src));
        StringBuilder buffer = new StringBuilder();
        String line;
        int cl = 1;
        try {
            while ((line = reader.readLine()) != null) {
                if (cl < froml || cl > tol) {
                    buffer.append(line).append('\n');
                } else {
                    if (cl == froml) {
                        buffer.append(line.substring(0, fromc - 1));
                    }
                    if (cl == tol) {
                        buffer.append(line.substring(toc + 1));
                    }
                } // else ignore line
                cl += 1;
            }
        } catch (IOException xignore) {
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
        /**
         * Creates a new stack overflow exception instance.
         *
         * @param info  the location information
         * @param name  the unknown method
         * @param cause the exception causing the error
         */
        public StackOverflow(JexlInfo info, String name, Throwable cause) {
            super(info, name, cause);
        }

        /**
         * @return the specific detailed message
         */
        public String getDetail() {
            return super.detailedMessage();
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
        /**
         * Creates a new Assignment statement exception instance.
         *
         * @param info  the location information
         * @param expr  the source expression line
         */
        public Assignment(JexlInfo info, String expr) {
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
        /** The feature code. */
        private final int code;
        /**
         * Creates a new Ambiguous statement exception instance.
         * @param info  the location information
         * @param feature the feature code
         * @param expr  the source expression line
         */
        public Feature(JexlInfo info, int feature, String expr) {
            super(info, expr);
            this.code = feature;
        }

        @Override
        protected String detailedMessage() {
            return parserError(JexlFeatures.stringify(code), getDetail());
        }
    }

    /**
     * Thrown when a variable is unknown.
     *
     * @since 3.0
     */
    public static class Variable extends JexlException {
        /**
         * Undefined variable flag.
         */
        private final boolean undefined;
        /**
         * Creates a new Variable exception instance.
         *
         * @param node the offending ASTnode
         * @param var  the unknown variable
         * @param undef whether the variable is undefined or evaluated as null
         */
        public Variable(JexlNode node, String var, boolean undef) {
            super(node, var, null);
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
         * @return the variable name
         */
        public String getVariable() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return (undefined? "undefined" : "null value") + " variable " + getVariable();
        }
    }

    /**
     * Generates a message for a variable error.
     *
     * @param node the node where the error occurred
     * @param variable the variable
     * @param undef whether the variable is null or undefined
     * @return the error message
     */
    public static String variableError(JexlNode node, String variable, boolean undef) {
        StringBuilder msg = errorAt(node);
        if (undef) {
            msg.append("undefined");
        } else {
            msg.append("null value");
        }
        msg.append(" variable ");
        msg.append(variable);
        return msg.toString();
    }

    /**
     * Thrown when a property is unknown.
     *
     * @since 3.0
     */
    public static class Property extends JexlException {
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
        public Property(JexlNode node, String pty) {
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
        public Property(JexlNode node, String pty, Throwable cause) {
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
        public Property(JexlNode node, String pty, boolean undef, Throwable cause) {
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
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return (undefined? "undefined" : "null value") + " property " + getProperty();
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
    public static String propertyError(JexlNode node, String pty, boolean undef) {
        StringBuilder msg = errorAt(node);
        if (undef) {
            msg.append("unsolvable");
        } else {
            msg.append("null value");
        }
        msg.append(" property ");
        msg.append(pty);
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
    public static String propertyError(JexlNode node, String var) {
        return propertyError(node, var, true);
    }

    /**
     * Thrown when a method or ctor is unknown, ambiguous or inaccessible.
     *
     * @since 3.0
     */
    public static class Method extends JexlException {
        /**
         * Creates a new Method exception instance.
         *
         * @param node  the offending ASTnode
         * @param name  the method name
         * @deprecated as of 3.2, use call with method arguments
         */
        @Deprecated
        public Method(JexlNode node, String name) {
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
        public Method(JexlInfo info, String name, Throwable cause) {
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
        public Method(JexlNode node, String name, Object[] args) {
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
        public Method(JexlInfo info, String name, Object[] args) {
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
        public Method(JexlInfo info, String name, Object[] args, Throwable cause) {
            super(info, methodSignature(name, args), cause);
        }
        
        /**
         * @return the method name
         */
        public String getMethod() {
            String signature = getMethodSignature();
            int lparen = signature.indexOf('(');
            return lparen > 0? signature.substring(0, lparen) : signature;
        }  
        
        /**
         * @return the method signature
         * @since 3.2
         */
        public String getMethodSignature() {
            return super.detailedMessage();
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
    private static String methodSignature(String name, Object[] args) {
        if (args != null && args.length > 0) {
            StringBuilder strb = new StringBuilder(name);
            strb.append('(');
            for (int a = 0; a < args.length; ++a) {
                if (a > 0) {
                    strb.append(", ");
                }
                Class<?> clazz = args[a] == null ? Object.class : args[a].getClass();
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
     */
    public static String methodError(JexlNode node, String method) {
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
    public static String methodError(JexlNode node, String method, Object[] args) {
        StringBuilder msg = errorAt(node);
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
        /**
         * Creates a new Operator exception instance.
         *
         * @param node  the location information
         * @param symbol  the operator name
         * @param cause the exception causing the error
         */
        public Operator(JexlNode node, String symbol, Throwable cause) {
            super(node, symbol, cause);
        }

        /**
         * @return the method name
         */
        public String getSymbol() {
            return super.detailedMessage();
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
    public static String operatorError(JexlNode node, String symbol) {
        StringBuilder msg = errorAt(node);
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
        /**
         * Creates a new Annotation exception instance.
         *
         * @param node  the annotated statement node
         * @param name  the annotation name
         * @param cause the exception causing the error
         */
        public Annotation(JexlNode node, String name, Throwable cause) {
            super(node, name, cause);
        }

        /**
         * @return the annotation name
         */
        public String getAnnotation() {
            return super.detailedMessage();
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
    public static String annotationError(JexlNode node, String annotation) {
        StringBuilder msg = errorAt(node);
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

        /** The returned value. */
        private final Object result;

        /**
         * Creates a new instance of Return.
         *
         * @param node  the return node
         * @param msg   the message
         * @param value the returned value
         */
        public Return(JexlNode node, String msg, Object value) {
            super(node, msg, null);
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
        public Cancel(JexlNode node) {
            super(node, "execution cancelled", null);
        }
    }

    /**
     * Thrown to break a loop.
     *
     * @since 3.0
     */
    public static class Break extends JexlException {
        /**
         * Creates a new instance of Break.
         *
         * @param node the break
         */
        public Break(JexlNode node) {
            super(node, "break loop", null);
        }
    }

    /**
     * Thrown to continue a loop.
     *
     * @since 3.0
     */
    public static class Continue extends JexlException {
        /**
         * Creates a new instance of Continue.
         *
         * @param node the continue
         */
        public Continue(JexlNode node) {
            super(node, "continue loop", null);
        }
    }

    /**
     * Thrown when method/ctor invocation fails.
     * <p>These wrap InvocationTargetException as runtime exception
     * allowing to go through without signature modifications.
     * @since 3.2
     */
    public static class TryFailed extends JexlException {
        /**
         * Creates a new instance.
         * @param xany the original invocation target exception
         */
        private TryFailed(InvocationTargetException xany) {
            super((JexlInfo) null, "tryFailed", xany.getCause());
        }
    }
    
    /**
     * Wrap an invocation exception.
     * <p>Return the cause if it is already a JexlException.
     * @param xinvoke the invocation exception
     * @return a JexlException
     */
    public static RuntimeException tryFailed(InvocationTargetException xinvoke) {
        return new JexlException.TryFailed(xinvoke); // fail
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
        StringBuilder msg = new StringBuilder();
        if (info != null) {
            msg.append(info.toString());
        } else {
            msg.append("?:");
        }
        msg.append(' ');
        msg.append(detailedMessage());
        Throwable cause = getCause();
        if (cause instanceof JexlArithmetic.NullOperand) {
            msg.append(" caused by null operand");
        }
        return msg.toString();
    }
}