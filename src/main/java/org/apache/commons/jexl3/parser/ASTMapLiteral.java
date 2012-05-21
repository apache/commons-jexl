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

import java.util.Collections;
import java.util.Map;
import org.apache.commons.jexl3.internal.Debugger;

public final class ASTMapLiteral extends JexlNode implements JexlNode.Literal<Object> {
    /** The type literal value. */
    private Map<?,?> map = null;
    /** Whether this array is constant or not. */
    private boolean constant = false;

    ASTMapLiteral(int id) {
        super(id);
    }

    ASTMapLiteral(Parser p, int id) {
        super(p, id);
    }


    @Override
    public String toString() {
        Debugger dbg = new Debugger();
        return dbg.data(this);
    }

    @Override
    public Map<?,?> getLiteral() {
        return map;
    }

    /**
     * Sets the literal value only if the descendants of this node compose a constant.
     * @param literal the literal array value
     * @throws IllegalArgumentException if literal is not an array or null
     */
    void setLiteral(Object literal) {
        if (constant) {
            if (!(literal instanceof Map<?,?>)) {
                throw new IllegalArgumentException(literal.getClass() + " is not a map");
            }
            this.map = (Map<?,?>) literal;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /** {@inheritDoc} */
    @Override
    public void jjtClose() {
        if (children == null || children.length == 0) {
            map = Collections.EMPTY_MAP;
            constant = true;
        } else {
            constant = isConstant();
        }
    }
}
