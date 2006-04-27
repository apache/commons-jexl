/*
 * Copyright 2002-2006 The Apache Software Foundation.
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

package org.apache.commons.jexl.parser;

import org.apache.commons.jexl.JexlContext;

/**
 * Simple identifier - $foo or $foo.bar (both parts are identifiers).
 * 
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class ASTIdentifier extends SimpleNode {
    protected String val;

    public ASTIdentifier(int id) {
        super(id);
    }

    public ASTIdentifier(Parser p, int id) {
        super(p, id);
    }

    /** Accept the visitor. * */
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    public Object value(JexlContext jc) throws Exception {
        return jc.getVars().get(val);
    }

    /**
     * returns the value of itself applied to the object. We assume that an
     * identifier can be gotten via a get(String)
     */
    public Object execute(Object obj, JexlContext jc) throws Exception {
        return ASTArrayAccess.evaluateExpr(obj, val);
    }

    public String getIdentifierString() {
        return val;
    }
}
