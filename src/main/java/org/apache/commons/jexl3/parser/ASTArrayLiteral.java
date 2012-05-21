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

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.internal.Debugger;

public final class ASTArrayLiteral extends JexlNode implements JexlNode.Literal<Object> {
    /** The type literal value. */
    private Object array = null;
    /** Whether this array is constant or not. */
    private boolean constant = false;

    ASTArrayLiteral(int id) {
        super(id);
    }

    ASTArrayLiteral(Parser p, int id) {
        super(p, id);
    }

    @Override
    public String toString() {
        Debugger dbg = new Debugger();
        return dbg.data(this);
    }

    @Override
    public Object getLiteral() {
        return array;
    }

    /** {@inheritDoc} */
    @Override
    public void jjtClose() {
        if (children == null || children.length == 0) {
            array = new Object[0];
            constant = true;
        } else {
            constant = isConstant();
            if (constant) {
                Object[] cc = new Object[children.length];
                for(int c = 0; c < children.length; ++c) {
                    cc[c] = ((JexlNode.Literal<?>) children[c]).getLiteral();
                }
                array = JexlArithmetic.typeArray(cc);
            } else {
                array = null;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
