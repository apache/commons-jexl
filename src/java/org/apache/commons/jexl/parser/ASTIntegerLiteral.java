/*
 * Copyright 2002,2004 The Apache Software Foundation.
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
 *  represents an integer
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ASTIntegerLiteral.java,v 1.4 2004/02/28 13:45:20 yoavs Exp $
 */
public class ASTIntegerLiteral extends SimpleNode
{
    Integer val;

    public ASTIntegerLiteral(int id)
    {
        super(id);
    }

    public ASTIntegerLiteral(Parser p, int id)
    {
        super(p, id);
    }


    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    /**
     *  Part of reference resolution - wierd...  in JSTL EL you can
     *  have
     *          foo.2
     *  which is equiv to
     *          foo[2]
     *  it appears...
     */
    public Object execute(Object o, JexlContext ctx)
            throws Exception
    {
        return ASTArrayAccess.evaluateExpr(o, val);
    }

    public Object value(JexlContext jc)
        throws Exception
    {
        return val;
    }
}
