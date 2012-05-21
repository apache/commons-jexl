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

/**
 * Wraps any error that might occur during interpretation of a script or expression.
 * @since 2.0
 */
public class JexlException extends RuntimeException {
    /** The point of origin for this exception. */
    private final transient JexlNode mark;
    /** The debug info. */
    private final transient JexlInfo info;
    /** Maximum number of characters around exception location. */
    private static final int MAX_EXCHARLOC = 32;

    /**
     * Creates a new JexlException.
     * @param node the node causing the error
     * @param msg  the error message
     */
    public JexlException(JexlNode node, String msg) {
        this(node, msg, null);
    }

    /**
     * Creates a new JexlException.
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
     * @param jinfo the debugging information associated
     * @param msg   the error message
     * @param cause the exception causing the error
     */
    public JexlException(JexlInfo jinfo, String msg, Throwable cause) {
        super(msg, unwrap(cause));
        mark = null;
        info = jinfo;
    }

    /**
     * Gets the specific information for this exception.
     * @return the information
     */
    public JexlInfo getInfo() {
        return info;
    }

    /**
     * Cleans a JexlException from any org.apache.commons.jexl3.internal stack trace element.
     * @return this exception
     */
    public JexlException clean() {
        return clean(this);
    }

    /**
     * Cleans a Throwable from any org.apache.commons.jexl3.internal stack trace element.
     * @param <X>    the throwable type
     * @param xthrow the thowable
     * @return the throwable
     */
    private static <X extends Throwable> X clean(X xthrow) {
        if (xthrow != null) {
            StackTraceElement[] stack = xthrow.getStackTrace();
            List<StackTraceElement> stackJexl = new ArrayList<StackTraceElement>();
            for (int s = 0; s < stack.length; ++s) {
                StackTraceElement se = stack[s];
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
     * Merge the node info and the cause info to obtain best possible location.
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
     * @return the message
     */
    protected String detailedMessage() {
        return super.getMessage();
    }

    /**
     * Formats an error message from the parser.
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
     * Thrown when tokenization fails.
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
     * @since 3.0
     */
    public static class Parsing extends JexlException {
        /**
         * Creates a new Variable exception instance.
         * @param info  the location information
         * @param cause the javacc cause
         */
        public Parsing(JexlInfo info, ParseException cause) {
            super(merge(info, cause), cause.getAfter(), null);
        }

        /**
         * Creates a new Variable exception instance.
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
     * Thrown when a variable is unknown.
     * @since 3.0
     */
    public static class Variable extends JexlException {
        /**
         * Creates a new Variable exception instance.
         * @param node the offending ASTnode
         * @param var  the unknown variable
         */
        public Variable(JexlNode node, String var) {
            super(node, var, null);
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
     * @since 3.0
     */
    public static class Property extends JexlException {
        /**
         * Creates a new Property exception instance.
         * @param node the offending ASTnode
         * @param var  the unknown variable
         */
        public Property(JexlNode node, String var) {
            this(node, var, null);
        }

        /**
         * Creates a new Property exception instance.
         * @param node  the offending ASTnode
         * @param var   the unknown variable
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
     * @since 3.0
     */
    public static class Method extends JexlException {
        /**
         * Creates a new Method exception instance.
         * @param node  the offending ASTnode
         * @param name  the unknown method
         * @param cause the exception causing the error
         */
        public Method(JexlNode node, String name, Throwable cause) {
            super(node, name, cause);
        }

        /**
         * Creates a new Method exception instance.
         * @param info  the location information
         * @param name  the unknown method
         * @param cause the exception causing the error
         */
        public Method(JexlInfo info, String name, Throwable cause) {
            super(info, name, cause);
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
     * @since 3.0
     */
    public static class Return extends JexlException {
        /** The returned value. */
        private final Object result;

        /**
         * Creates a new instance of Return.
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
     * @since 3.0
     */
    public static class Cancel extends JexlException {
        /**
         * Creates a new instance of Cancel.
         * @param node the node where the interruption was detected
         */
        public Cancel(JexlNode node) {
            super(node, "execution cancelled", null);
        }
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
        StringBuilder msg = new StringBuilder();
        if (info != null) {
            msg.append(info.toString());
        } else {
            msg.append('?');
        }
        if (mark != null) {
            Debugger dbg = new Debugger();
            if (dbg.debug(mark)) {
                msg.append("![");
                msg.append(dbg.start());
                msg.append(",");
                msg.append(dbg.end());
                msg.append("]: '");
                msg.append(dbg.toString());
                msg.append("'");
            }
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