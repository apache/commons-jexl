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

import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Utility to dump AST, useful in debug sessions.
 */
public class Dumper {
    private StringBuilder strb = new StringBuilder();
    private int indent = 0;

    private void indent() {
        for (int i = 0; i < indent; ++i) {
            strb.append("  ");
        }
    }

    private void dump(JexlNode node, Object data) {
        final int num = node.jjtGetNumChildren();
        indent();
        strb.append(node.getClass().getSimpleName());
        if (node instanceof ASTIdentifier) {
            strb.append("@");
            strb.append(node.toString());
        } else if (node instanceof ASTIdentifierAccess) {
            strb.append("@");
            strb.append(node.toString());
        }
        strb.append('(');
        indent += 1;
        for (int c = 0; c < num; ++c) {
            JexlNode child = node.jjtGetChild(c);
            if (c > 0) {
                strb.append(',');
            }
            strb.append('\n');
            dump(child, data);
        }
        indent -= 1;
        if (num > 0) {
            strb.append('\n');
            indent();
        }
        strb.append(')');
    }

    private Dumper(JexlScript script) {
        dump(((Script) script).script, null);
    }

    @Override
    public String toString() {
        return strb.toString();
    }

    public static String toString(JexlScript script) {
        return new Dumper(script).toString();
    }
}
