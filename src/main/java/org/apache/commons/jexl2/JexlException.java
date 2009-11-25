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

import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.commons.jexl2.util.introspection.Info;

/**
 * Wraps any error that might occur during interpretation of a script or expression.
 * @since 2.0
 */
public class JexlException extends RuntimeException {
    /** Serial version UID. */
    private static final long serialVersionUID = 2690666400232612395L;
    /** The point of origin for this exception. */
    protected final JexlNode mark;
    /** The debug info. */
    protected final Info info;
    /** A marker to use in NPEs stating a null operand error. */
    public static final String NULL_OPERAND = "jexl.null";
    /**
     * Creates a new JexlException.
     * @param node the node causing the error
     * @param msg the error message
     */
    public JexlException(JexlNode node, String msg) {
        super(msg);
        mark = node;
        info = node != null? node.getInfo() : null;

    }

    /**
     * Creates a new JexlException.
     * @param node the node causing the error
     * @param msg the error message
     * @param cause the exception causing the error
     */
    public JexlException(JexlNode node, String msg, Throwable cause) {
        super(msg, cause);
        mark = node;
        info = node != null? node.getInfo() : null;
    }
    
    /**
     * Creates a new JexlException.
     * @param dbg the debugging information associated
     * @param msg the error message
     */
    public JexlException(Info dbg, String msg) {
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
    public JexlException(Info dbg, String msg, Throwable cause) {
        super(msg, cause);
        mark = null;
        info = dbg;
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
            msg.append("' ");
        }
        msg.append(super.getMessage());
        Throwable cause = getCause();
        if (cause != null && NULL_OPERAND == cause.getMessage()) {
            msg.append(" caused by null operand");
        }
        return msg.toString();
    }
}