/*
 * Copyright 2011 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Helper methods for validate sessions.
 */
public class Util {
    /**
     * Will force testing the debugger for each derived test class by
     * recreating each expression from the JexlNode in the JexlEngine cache &
     * testing them for equality with the origin.
     * @throws Exception
     */
    public static void debuggerCheck(JexlEngine ijexl) throws Exception {
        Engine jexl = (Engine) ijexl;
        // without a cache, nothing to check
        if (jexl == null || jexl.cache == null) {
            return;
        }
        Engine jdbg = new Engine();
        jdbg.parser.allowRegisters(true);
        Debugger dbg = new Debugger();
        // iterate over all expression in
        Iterator<Map.Entry<Source, ASTJexlScript>> inodes = jexl.cache.entries().iterator();
        while (inodes.hasNext()) {
            Map.Entry<Source, ASTJexlScript> entry = inodes.next();
            JexlNode node = entry.getValue();
            // recreate expr string from AST
            dbg.debug(node);
            String expressiondbg = dbg.toString();
            JexlFeatures features = entry.getKey().getFeatures();
            // recreate expr from string
            try {
                Script exprdbg = jdbg.createScript(features, null, expressiondbg, null);
                // make arg cause become the root cause
                JexlNode root = exprdbg.script;
                while (root.jjtGetParent() != null) {
                    root = root.jjtGetParent();
                }
                // test equality
                String reason = checkEquals(root, node);
                if (reason != null) {
                    throw new RuntimeException("check equal failed: "
                            + expressiondbg
                            + " /**** " + reason + " **** */ "
                            + entry.getKey());
                }
            } catch (JexlException xjexl) {
                throw new RuntimeException("check parse failed: "
                        + expressiondbg
                        + " /*********/ "
                        + entry.getKey(), xjexl);

            }
        }
    }

    /**
     * Creates a list of all descendants of a script including itself.
     * @param script the script to flatten
     * @return the descendants-and-self list
     */
    protected static ArrayList<JexlNode> flatten(JexlNode node) {
        ArrayList<JexlNode> list = new ArrayList<JexlNode>();
        flatten(list, node);
        return list;
    }

    /**
     * Recursively adds all children of a script to the list of descendants.
     * @param list   the list of descendants to add to
     * @param script the script & descendants to add
     */
    private static void flatten(List<JexlNode> list, JexlNode node) {
        int nc = node.jjtGetNumChildren();
        list.add(node);
        for (int c = 0; c < nc; ++c) {
            flatten(list, node.jjtGetChild(c));
        }
    }

    /**
     * Checks the equality of 2 nodes by comparing all their descendants.
     * Descendants must have the same class and same image if non null.
     * @param lhs the left script
     * @param rhs the right script
     * @return null if true, a reason otherwise
     */
    private static String checkEquals(JexlNode lhs, JexlNode rhs) {
        if (lhs != rhs) {
            ArrayList<JexlNode> lhsl = flatten(lhs);
            ArrayList<JexlNode> rhsl = flatten(rhs);
            if (lhsl.size() != rhsl.size()) {
                return "size: " + lhsl.size() + " != " + rhsl.size();
            }
            for (int n = 0; n < lhsl.size(); ++n) {
                lhs = lhsl.get(n);
                rhs = rhsl.get(n);
                if (lhs.getClass() != rhs.getClass()) {
                    return "class: " + lhs.getClass() + " != " + rhs.getClass();
                }
                String lhss = lhs.toString();
                String rhss = rhs.toString();
                if ((lhss == null && rhss != null)
                        || (lhss != null && rhss == null)) {
                    return "image: " + lhss + " != " + rhss;
                }
                if (lhss != null && !lhss.equals(rhss)) {
                    return "image: " + lhss + " != " + rhss;
                }
            }
        }
        return null;
    }

    /**
     * A helper class to help validate AST problems.
     * @param e the script
     * @return an indented version of the AST
     */
    protected static String flattenedStr(JexlScript e) {
        return "";//e.getText() + "\n" + flattenedStr(((Script)e).script);
    }

    static private String indent(JexlNode node) {
        StringBuilder strb = new StringBuilder();
        while (node != null) {
            strb.append("  ");
            node = node.jjtGetParent();
        }
        return strb.toString();
    }

    private String flattenedStr(JexlNode node) {
        ArrayList<JexlNode> flattened = flatten(node);
        StringBuilder strb = new StringBuilder();
        for (JexlNode flat : flattened) {
            strb.append(indent(flat));
            strb.append(flat.getClass().getSimpleName());
            String sflat = flat.toString();
            if (sflat != null) {
                strb.append(" = ");
                strb.append(sflat);
            }
            strb.append("\n");
        }
        return strb.toString();
    }
}
