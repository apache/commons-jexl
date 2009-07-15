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

import org.apache.commons.jexl.parser.Node;

/**
 * Wraps any error that might occur during interpretation of a script or expression.
 */
public class JexlException extends RuntimeException {
    Node mark;

    public JexlException(Node node, String msg) {
        super(msg);
        mark = node;
    }

    public JexlException(Node node, String msg, Throwable cause) {
        super(msg, cause);
        mark = node;
    }
    
    /**
     * Gets information about the cause of this error.
     * The returned string represents the outermost expression in error.
     * The info parameter, an int[2] optionally provided by the caller, will be filled with the begin/end offset characters of the precise error's trigger.
     * @param info character offset interval of the precise node triggering the error
     * @return a string representation of the offending expression, the empty string if it could not be determined
     */
    public String getInfo(int[] info) {
        Debugger dbg = new Debugger();
        if (dbg.debug(mark)) {
            if (info != null && info.length >= 2) {
                info[0] = dbg.start();
                info[1] = dbg.end();
            }
            return dbg.data();
        }
        return "";
    }
    
    /**
     * Detailed info message about this error.
     * Format is "@[begin,end]: string \n msg" where:
     * - begin, end are character offsets in the string for the precise location of the error
     * - string is the string representation of the offending expression
     * - msg is the actual explanation message for this error
     * @return this error as a string
     */
    @Override
    public String getMessage() {
        Debugger dbg = new Debugger();
        StringBuilder msg = new StringBuilder();
        if (dbg.debug(mark)) {
            msg.append("@[");
            msg.append(dbg.start());
            msg.append(",");
            msg.append(dbg.end());
            msg.append("]: ");
            msg.append(dbg.data());
            msg.append("\n");
        }
        msg.append(super.getMessage());
        return msg.toString();
    }
}
