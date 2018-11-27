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
package org.apache.commons.jexl3.parser;

import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;

/**
 * Base class for parser nodes - holds an 'image' of the token for later use.
 *
 * @since 2.0
 */
public abstract class JexlNode extends SimpleNode {
    // line + column encoded: up to 4096 columns (ie 20 bits for line + 12 bits for column)
    private int lc = -1;

    /**
     * A marker interface for constants.
     * @param <T> the literal type
     */
    public interface Constant<T> {
        T getLiteral();
    }

    public JexlNode(int id) {
        super(id);
    }

    public JexlNode(Parser p, int id) {
        super(p, id);
    }

    public void jjtSetFirstToken(Token t) {
        // 0xc = 12, 12 bits -> 4096
        // 0xfff, 12 bits mask
        this.lc = (t.beginLine << 0xc) | (0xfff & t.beginColumn);
    }

    public void jjtSetLastToken(Token t) {
        // nothing
    }

    /**
     * Gets the associated JexlInfo instance.
     *
     * @return the info
     */
    public JexlInfo jexlInfo() {
        JexlInfo info = null;
        JexlNode node = this;
        while (node != null) {
            if (node.jjtGetValue() instanceof JexlInfo) {
                info = (JexlInfo) node.jjtGetValue();
                break;
            }
            node = node.jjtGetParent();
        }
        if (lc >= 0) {
            int c = lc & 0xfff;
            int l = lc >> 0xc;
            // at least an info with line/column number
            return info != null? info.at(l, c) : new JexlInfo(null, l, c);
        } else {
            // weird though; no jjSetFirstToken(...) ever called?
            return info;
        }
    }
    
    /**
     * Marker interface for cachable function calls.
     */
    public interface Funcall {} 

    /**
     * Clears any cached value of type JexlProperty{G,S}et or JexlMethod.
     * <p>
     * This is called when the engine detects the evaluation of a script occurs with a class loader
     * different that the one that created it.</p>
     */
    public void clearCache() {
        final Object value = jjtGetValue();
        if (value instanceof JexlPropertyGet
            || value instanceof JexlPropertySet
            || value instanceof JexlMethod
            || value instanceof Funcall ) {
            jjtSetValue(null);
        }
        for (int n = 0; n < jjtGetNumChildren(); ++n) {
            jjtGetChild(n).clearCache();
        }
    }

    /**
     * Whether this node is a constant node Its value can not change after the first evaluation and can be cached
     * indefinitely.
     *
     * @return true if constant, false otherwise
     */
    public boolean isConstant() {
        return isConstant(this instanceof JexlNode.Constant<?>);
    }

    protected boolean isConstant(boolean literal) {
        if (literal) {
            for (int n = 0; n < jjtGetNumChildren(); ++n) {
                JexlNode child = jjtGetChild(n);
                if (child instanceof ASTReference) {
                    boolean is = child.isConstant(true);
                    if (!is) {
                        return false;
                    }
                } else if (child instanceof ASTMapEntry) {
                    boolean is = child.isConstant(true);
                    if (!is) {
                        return false;
                    }
                } else if (!child.isConstant()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Whether this node is a left value.
     * @return true if node is assignable, false otherwise
     */
    public boolean isLeftValue() {
        JexlNode walk = this;
        do {
            if (walk instanceof ASTIdentifier
                || walk instanceof ASTIdentifierAccess
                || walk instanceof ASTArrayAccess
                || walk instanceof ASTIndirectNode) {
                return true;
            }
            int nc = walk.jjtGetNumChildren() - 1;
            if (nc >= 0) {
                walk = walk.jjtGetChild(nc);
            } else if (walk.jjtGetParent() instanceof ASTReference) {
                return true;
            } else {
                return false;
            }
        } while (walk != null);
        return false;
    }

    /**
     * @return true if this node looks like a global var
     */
    public boolean isGlobalVar() {
        if (this instanceof ASTVar) {
            return false;
        }
        if (this instanceof ASTIdentifier) {
            return ((ASTIdentifier) this).getSymbol() < 0;
        }
        int nc = this.jjtGetNumChildren() - 1;
        if (nc >= 0) {
            JexlNode first = this.jjtGetChild(0);
            return first.isGlobalVar();
        }
        if (jjtGetParent() instanceof ASTReference) {
            return true;
        }
        return false;
    }

    /**
     * Whether this node is a local variable.
     * @return true if local, false otherwise
     */
    public boolean isLocalVar() {
        return this instanceof ASTIdentifier && ((ASTIdentifier) this).getSymbol() >= 0;
    }

    /**
     * Whether this node is the left-hand side of a safe access identifier as in.
     * For instance, in 'x?.y' , 'x' is safe.
     * @param safe whether the engine is in safe-navigation mode
     * @return true if safe lhs, false otherwise
     */
    public boolean isSafeLhs(boolean safe) {
        if (this instanceof ASTReference) {
            return jjtGetChild(0).isSafeLhs(safe);
        }
        JexlNode parent = this.jjtGetParent();
        if (parent == null) {
            return false;
        }
        // find this node in its parent
        int nsiblings = parent.jjtGetNumChildren();
        int rhs = -1;
        for(int s = 0; s < nsiblings; ++s) {
            JexlNode sibling = parent.jjtGetChild(s);
            if (sibling == this) {
                // the next chid offset of this nodes parent
                rhs = s + 1;
                break;
            }
        }
        // seek next child in parent
        if (rhs >= 0 && rhs < nsiblings) {
            JexlNode rsibling = parent.jjtGetChild(rhs);
            if (rsibling instanceof ASTMethodNode || rsibling instanceof ASTFunctionNode) {
                rsibling = rsibling.jjtGetChild(0);
            }
            if (rsibling instanceof ASTIdentifierAccess
                && (((ASTIdentifierAccess) rsibling).isSafe() || safe)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a null evaluated expression is protected by a ternary expression.
     * <p>
     * The rationale is that the ternary / elvis expressions are meant for the user to explictly take control
     * over the error generation; ie, ternaries can return null even if the engine in strict mode
     * would normally throw an exception.
     * </p>
     * @return true if nullable variable, false otherwise
     */
    public boolean isTernaryProtected() {
        JexlNode node = this;
        for (JexlNode walk = node.jjtGetParent(); walk != null; walk = walk.jjtGetParent()) {
            if (walk instanceof ASTTernaryNode) {
                return true;
            }
            if (walk instanceof ASTElvisNode) {
                return true;
            }
            if (walk instanceof ASTNullpNode) {
                return true;
            }
            if (!(walk instanceof ASTReference || walk instanceof ASTArrayAccess)) {
                break;
            }
        }
        return false;
    }


}
