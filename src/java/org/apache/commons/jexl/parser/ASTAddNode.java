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
 *  Addition : either integer addition or string concatenation
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id$
 */
public class ASTAddNode extends SimpleNode
{
    public ASTAddNode(int id)
    {
        super(id);
    }

    public ASTAddNode(Parser p, int id)
    {
        super(p, id);
    }


    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    public Object value(JexlContext context)
        throws Exception
    {
        Object left = ((SimpleNode) jjtGetChild(0)).value(context);
        Object right = ((SimpleNode) jjtGetChild(1)).value(context);

        /*
         *  the spec says 'and'
         */
        if (left == null && right == null)
            return new Long(0);

        /*
         *  if anything is float, double or string with ( "." | "E" | "e")
         *  coerce all to doubles and do it
         */
        if (left instanceof Float || left instanceof Double
            || right instanceof Float || right instanceof Double
            || (  left instanceof String
                  && (  ((String) left).indexOf(".") != -1 ||
                        ((String) left).indexOf("e") != -1 ||
                        ((String) left).indexOf("E") != -1 )
               )
            || (  right instanceof String
                  && (  ((String) right).indexOf(".") != -1 ||
                        ((String) right).indexOf("e") != -1 ||
                        ((String) right).indexOf("E") != -1)
               )
            )
        {

            /*
             * in the event that either is null and not both, then just make the
             * null a 0
             */

            try
            {
                Double l = Coercion.coerceDouble(left);
                Double r = Coercion.coerceDouble(right);
                return new Double(l.doubleValue() + r.doubleValue());
            }
            catch( java.lang.NumberFormatException nfe )
            {
                /*
                 * Well, use strings!
                 */
                return left.toString().concat(right.toString());
            }
        }

        /*
         * attempt to use Longs
         */
        try
        {
            Long l = Coercion.coerceLong(left);
            Long r = Coercion.coerceLong(right);
            return new Long(l.longValue() + r.longValue());
        }
        catch( java.lang.NumberFormatException nfe )
        {
            /*
             * Well, use strings!
             */
            return left.toString().concat(right.toString());
        }
    }
}
