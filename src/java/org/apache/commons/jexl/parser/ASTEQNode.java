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
import org.apache.commons.jexl.util.Coercion;

/**
 *  represents equality between integers - use .equals() for strings
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ASTEQNode.java,v 1.6 2004/08/21 10:01:18 dion Exp $
 */
public class ASTEQNode extends SimpleNode
{
    public ASTEQNode(int id)
    {
        super(id);
    }

    public ASTEQNode(Parser p, int id)
    {
        super(p, id);
    }

    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    public Object value(JexlContext pc)
        throws Exception
    {
        Object left = ( (SimpleNode) jjtGetChild(0)).value(pc);
        Object right = ( (SimpleNode) jjtGetChild(1)).value(pc);

        if (left == null && right == null)
        {
            /*
             * if both are null L == R
             */
            return Boolean.TRUE;
        }
        else if (left==null || right==null)
        {
            /*
             * we know both aren't null, therefore L != R
             */
            return Boolean.FALSE;
        }
        else if (left.getClass().equals(right.getClass()))
        {
            return new Boolean(left.equals(right));
        }
        else if(left instanceof Float || left instanceof Double ||
                right instanceof Float || right instanceof Double)
        {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            return new Boolean(l.equals(r));
        }
        else if ( left instanceof Number || right instanceof Number ||
                   left instanceof Character || right instanceof Character)
        {
            return new Boolean(
                    Coercion.coerceLong(left).equals(Coercion.coerceLong(right)));
        }
        else if (left instanceof Boolean || right instanceof Boolean)
        {
            return new Boolean(
                    Coercion.coerceBoolean(left).equals(Coercion.coerceBoolean(right)));
        }
        else if (left instanceof java.lang.String || right instanceof String)
        {
            return new Boolean(left.toString().equals(right.toString()));
        }

        return new Boolean(left.equals(right));
    }
}
