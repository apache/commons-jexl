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

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.jexl.parser.JexlNode;
import org.apache.commons.jexl.parser.ParseException;

import junit.framework.TestCase;
/**
 * Implements a runTest method to dynamically invoke a test,
 * wrapping the call with setUp(), tearDown() calls.
 * Eases the implementation of main methods to debug.
 */
public class JexlTestCase extends TestCase {
    /** No parameters signature for test run. */
    private static final Class<?>[] noParms = {};

    /** A default Jexl engine instance. */
    protected final JexlEngine JEXL = new JexlEngine();

    public JexlTestCase(String name) {
        super(name);
        JEXL.setCache(512);
    }
    public JexlTestCase() {
        super();
        JEXL.setCache(512);
    }

    @Override
    protected void tearDown() throws Exception {
        debuggerCheck(JEXL);
    }
    
    /**
     * Will force testing the debugger for each derived test class by
     * recreating each expression from the JexlNode in the JexlEngine cache &
     * testing them for equality with the origin.
     * @throws Exception
     */
    public static void debuggerCheck(JexlEngine jexl) throws Exception {
        // without a cache, nothing to check
        if (jexl.cache == null) {
            return;
        }
        JexlEngine jdbg = new JexlEngine();
        Debugger dbg = new Debugger();
        // iterate over all expression in cache
        Iterator<Map.Entry<String,JexlNode>> inodes = jexl.cache.entrySet().iterator();
        while (inodes.hasNext()) {
            Map.Entry<String,JexlNode> entry = inodes.next();
            JexlNode node = entry.getValue();
            // recreate expr string from AST
            dbg.debug(node);
            String expressiondbg = dbg.data();
            try {
                // recreate expr from string
                Expression exprdbg = jdbg.createExpression(expressiondbg);
                // make arg cause become the root cause
                JexlNode root = ((ExpressionImpl) exprdbg).node;
                while (root.jjtGetParent() != null) {
                    root = root.jjtGetParent();
                }
                // test equality
                String reason = JexlTestCase.checkEquals(root, node);
                if (reason != null) {
                    throw new RuntimeException("debugger equal failed: "
                                               + expressiondbg
                                               +" /**** "  +reason+" **** */ "
                                               + entry.getKey());
                }
            }
            catch(ParseException xparse) {
                    throw new RuntimeException("debugger parse failed: "
                                               + expressiondbg
                                               +" /**** != **** */ "
                                               + entry.getKey());
            }
        }
    }

    /**
     * Creates a list of all descendants of a node including itself.
     * @param node the node to flatten
     * @return the descendants-and-self list
     */
    private static ArrayList<JexlNode> flatten(JexlNode node) {
        ArrayList<JexlNode> list = new ArrayList<JexlNode>();
        flatten(list, node);
        return list;
    }

    /**
     * Recursively adds all children of a node to the list of descendants.
     * @param list the list of descendants to add to
     * @param node the node & descendants to add
     */
    private static void flatten(List<JexlNode> list, JexlNode node) {
        int nc = node.jjtGetNumChildren();
        list.add(node);
        for(int c = 0; c < nc; ++c) {
            flatten(list, node.jjtGetChild(c));
        }
    }

    /**
     * Checks the equality of 2 nodes by comparing all their descendants.
     * Descendants must have the same class and same image if non null.
     * @param lhs the left node
     * @param rhs the right node
     * @return null if true, a reason otherwise
     */
    private static String checkEquals(JexlNode lhs, JexlNode rhs) {
        if (lhs != rhs) {
            ArrayList<JexlNode> lhsl = flatten(lhs);
            ArrayList<JexlNode> rhsl = flatten(rhs);
            if (lhsl.size() != rhsl.size()) {
                 return "size: " + lhsl.size() + " != " + rhsl.size();
            }
            for(int n = 0; n < lhsl.size(); ++n) {
                lhs = lhsl.get(n);
                rhs = rhsl.get(n);
                if (lhs.getClass() != rhs.getClass()) {
                    return "class: " + lhs.getClass() + " != " + rhs.getClass();
                }
                if ((lhs.image == null && rhs.image != null)
                    || (lhs.image != null && rhs.image == null)) {
                    return "image: " + lhs.image + " != " + rhs.image;
                }
                if (lhs.image != null && !lhs.image.equals(rhs.image)) {
                    return "image: " + lhs.image + " != " + rhs.image;
                }
            }
        }
        return null;
    }

    public void runTest(String name) throws Exception {
        if ("runTest".equals(name)) {
            return;
        }
        Method method = null;
        try {
            method = this.getClass().getDeclaredMethod(name, noParms);
        }
        catch(Exception xany) {
            fail("no such test: " + name);
            return;
        }
        try {
            this.setUp();
            method.invoke(this);
        } finally {
            this.tearDown();
        }
    }

    /*public void testRunTest() throws Exception {
        new JexlTestCase().runTest("runTest");
    }*/

}
