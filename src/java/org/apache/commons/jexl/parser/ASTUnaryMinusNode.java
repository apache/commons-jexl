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
 *  -
 *
 *  @author <a href="mailto:mhw@kremvax.net">Mark H. Wilkinson</a>
 *  @version $Id: ASTUnaryMinusNode.java,v 1.3 2004/02/28 13:45:20 yoavs Exp $
 */
public class ASTUnaryMinusNode extends SimpleNode 
{
    public ASTUnaryMinusNode(int id)
    {
        super(id);
    }

    public ASTUnaryMinusNode(Parser p, int id)
    {
        super(p, id);
    }

    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    public Object value(JexlContext jc)
        throws Exception
    {
        Object val = ((SimpleNode) jjtGetChild(0)).value(jc);

        if (val instanceof Integer)
        {
            return new Integer(- ( ((Integer) val).intValue() ) );
        }
        else
        {
            throw new Exception("expression not integer valued");
        }
    }
}

