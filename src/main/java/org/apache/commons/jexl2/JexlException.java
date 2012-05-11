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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.commons.jexl2.parser.ParseException;
import org.apache.commons.jexl2.parser.TokenMgrError;

/**
 * Wraps any error that might occur during interpretation of a script or expression.
 * @since 2.0
 */
public class JexlException extends RuntimeException {
    /** The point of origin for this exception. */
    protected final transient JexlNode mark;
    /** The debug info. */
    protected final transient JexlInfo info;
    /** A marker to use in NPEs stating a null operand error. */
    public static final String NULL_OPERAND = "jexl.null";
    /** Minimum number of characters around exception location. */
    private static final int MIN_EXCHARLOC = 5;
    /** Maximum number of characters around exception location. */
    private static final int MAX_EXCHARLOC = 10;

    /**
     * Creates a new JexlException.
     * @param node the node causing the error
     * @param msg the error message
     */
    public JexlException(JexlNode node, String msg) {
        super(msg);
        mark = node;
        info = node != null ? node.debugInfo() : null;

    }

    /**
     * Creates a new JexlException.
     * @param node the node causing the error
     * @param msg the error message
     * @param cause the exception causing the error
     */
    public JexlException(JexlNode node, String msg, Throwable cause) {
        super(msg, unwrap(cause));
        mark = node;
        info = node != null ? node.debugInfo() : null;
    }

    /**
     * Creates a new JexlException.
     * @param dbg the debugging information associated
     * @param msg the error message
     */
    public JexlException(JexlInfo dbg, String msg) {
        super(msg);
        mark = null;
        info = dbg;
    }

    /**
     * Creates a new JexlException.
     * @param dbg the debugging information associated
     * @param msg the error message
     * @param cause the exception causing the error
     */
    public JexlException(JexlInfo dbg, String msg, Throwable cause) {
        super(msg, unwrap(cause));
        mark = null;
        info = dbg;
    }

    /**
     * Unwraps the cause of a throwable due to reflection.
     * @param xthrow the throwable
     * @return the cause
     */
    private static Throwable unwrap(Throwable xthrow) {
        if (xthrow instanceof InvocationTargetException) {
            return ((InvocationTargetException) xthrow).getTargetException();
        } else if (xthrow instanceof UndeclaredThrowableException) {
            return ((UndeclaredThrowableException) xthrow).getUndeclaredThrowable();
        } else {
            return xthrow;
        }
    }

    /**
     * Accesses detailed message.
     * @return  the message
     * @since 2.1
     */
    protected String detailedMessage() {
        return super.getMessage();
    }

    /**
     * Formats an error message from the parser.
     * @param prefix the prefix to the message
     * @param expr the expression in error
     * @return the formatted message
     * @since 2.1
     */
    protected String parserError(String prefix, String expr) {
        int begin = info.debugInfo().getColumn();
        int end = begin + MIN_EXCHARLOC;
        begin -= MIN_EXCHARLOC;
        if (begin < 0) {
            end += MIN_EXCHARLOC;
            begin = 0;
        }
        int length = expr.length();
        if (length < MAX_EXCHARLOC) {
            return prefix + " error in '" + expr + "'";
        } else {
            return prefix + " error near '... "
                    + expr.substring(begin, end > length ? length : end) + " ...'";
        }
    }

    /**
     * Thrown when tokenization fails.
     * @since 2.1
     */
    public static class Tokenization extends JexlException {
        /**
         * Creates a new Tokenization exception instance.
         * @param node the location info
         * @param expr the expression
         * @param cause the javacc cause
         */
        public Tokenization(JexlInfo node, CharSequence expr, TokenMgrError cause) {
            super(merge(node, cause), expr.toString(), cause);
        }

        /**
         * Merge the node info and the cause info to obtain best possible location.
         * @param node the node
         * @param cause the cause
         * @return the info to use
         */
        private static DebugInfo merge(JexlInfo node, TokenMgrError cause) {
            DebugInfo dbgn = node != null ? node.debugInfo() : null;
            if (cause == null) {
                return dbgn;
            } else if (dbgn == null) {
                return new DebugInfo("", cause.getLine(), cause.getColumn());
            } else {
                return new DebugInfo(dbgn.getName(), cause.getLine(), cause.getColumn());
            }
        }

        /**
         * @return the expression
         */
        public String getExpression() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return parserError("tokenization", getExpression());
        }
    }

    /**
     * Thrown when parsing fails.
     * @since 2.1
     */
    public static class Parsing extends JexlException {
        /**
         * Creates a new Variable exception instance.
         * @param node the offending ASTnode
         * @param expr the offending source
         * @param cause the javacc cause
         */
        public Parsing(JexlInfo node, CharSequence expr, ParseException cause) {
            super(merge(node, cause), expr.toString(), cause);
        }

        /**
         * Merge the node info and the cause info to obtain best possible location.
         * @param node the node
         * @param cause the cause
         * @return the info to use
         */
        private static DebugInfo merge(JexlInfo node, ParseException cause) {
            DebugInfo dbgn = node != null ? node.debugInfo() : null;
            if (cause == null) {
                return dbgn;
            } else if (dbgn == null) {
                return new DebugInfo("", cause.getLine(), cause.getColumn());
            } else {
                return new DebugInfo(dbgn.getName(), cause.getLine(), cause.getColumn());
            }
        }

        /**
         * @return the expression
         */
        public String getExpression() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return parserError("parsing", getExpression());
        }
    }

    /**
     * Thrown when a variable is unknown.
     * @since 2.1
     */
    public static class Variable extends JexlException {
        /**
         * Creates a new Variable exception instance.
         * @param node the offending ASTnode
         * @param var the unknown variable
         */
        public Variable(JexlNode node, String var) {
            super(node, var);
        }

        /**
         * @return the variable name
         */
        public String getVariable() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return "undefined variable " + getVariable();
        }
    }

    /**
     * Thrown when a property is unknown.
     * @since 2.1
     */
    public static class Property extends JexlException {
        /**
         * Creates a new Property exception instance.
         * @param node the offending ASTnode
         * @param var the unknown variable
         * @param cause the exception causing the error
         */
        public Property(JexlNode node, String var, Throwable cause) {
            super(node, var, cause);
        }

        /**
         * @return the property name
         */
        public String getProperty() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return "inaccessible or unknown property " + getProperty();
        }
    }

    /**
     * Thrown when a method or ctor is unknown, ambiguous or inaccessible.
     * @since 2.1
     */
    public static class Method extends JexlException {
        /**
         * Creates a new Method exception instance.
         * @param node the offending ASTnode
         * @param name the unknown method
         * @param cause the exception causing the error
         */
        public Method(JexlNode node, String name, Throwable cause) {
            super(node, name, cause);
        }

        /**
         * @return the method name
         */
        public String getMethod() {
            return super.detailedMessage();
        }

        @Override
        protected String detailedMessage() {
            return "unknown, ambiguous or inaccessible method " + getMethod();
        }
    }

    /**
     * Thrown to return a value.
     * @since 2.1
     */
    protected static class Return extends JexlException {
        /** The returned value. */
        private final Object result;

        /**
         * Creates a new instance of Return.
         * @param node the return node
         * @param msg the message
         * @param value the returned value
         */
        protected Return(JexlNode node, String msg, Object value) {
            super(node, msg);
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
     * @since 2.1
     */
    protected static class Cancel extends JexlException {
        /**
         * Creates a new instance of Cancel.
         * @param node the node where the interruption was detected
         */
        protected Cancel(JexlNode node) {
            super(node, "execution cancelled", null);
        }
    }

    /**
     * Gets information about the cause of this error.
     * <p>
     * The returned string represents the outermost expression in error.
     * The info parameter, an int[2] optionally provided by the caller, will be filled with the begin/end offset
     * characters of the precise error's trigger.
     * </p>
     * @param offsets character offset interval of the precise node triggering the error
     * @return a string representation of the offending expression, the empty string if it could not be determined
     */
    public String getInfo(int[] offsets) {
        Debugger dbg = new Debugger();
        if (dbg.debug(mark)) {
            if (offsets != null && offsets.length >= 2) {
                offsets[0] = dbg.start();
                offsets[1] = dbg.end();
            }
            return dbg.data();
        }
        return "";
    }

    /**
     * Detailed info message about this error.
     * Format is "debug![begin,end]: string \n msg" where:
     * - debug is the debugging information if it exists (@link JexlEngine.setDebug)
     * - begin, end are character offsets in the string for the precise location of the error
     * - string is the string representation of the offending expression
     * - msg is the actual explanation message for this error
     * @return this error as a string
     */
    @Override
    public String getMessage() {
        Debugger dbg = new Debugger();
        StringBuilder msg = new StringBuilder();
        if (info != null) {
            msg.append(info.debugString());
        }
        if (dbg.debug(mark)) {
            msg.append("![");
            msg.append(dbg.start());
            msg.append(",");
            msg.append(dbg.end());
            msg.append("]: '");
            msg.append(dbg.data());
            msg.append("'");
        }
        msg.append(' ');
        msg.append(detailedMessage());
        Throwable cause = getCause();
        if (cause != null && NULL_OPERAND == cause.getMessage()) {
            msg.append(" caused by null operand");
        }
        return msg.toString();
    }
}