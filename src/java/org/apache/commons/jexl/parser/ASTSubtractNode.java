/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", "Jexl" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.commons.jexl.parser;

import org.apache.commons.jexl.util.Coercion;
import org.apache.commons.jexl.JexlContext;

/**
 *  Subtraction
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @author <a href="mailto:mhw@kremvax.net">Mark H. Wilkinson</a>
 *  @version $Id: ASTSubtractNode.java,v 1.4 2003/10/09 21:28:55 rdonkin Exp $
 */
public class ASTSubtractNode extends SimpleNode
{
    public ASTSubtractNode(int id)
    {
        super(id);
    }

    public ASTSubtractNode(Parser p, int id)
    {
        super(p, id);
    }

    public Object value(JexlContext context)
        throws Exception
    {
        Object left = ((SimpleNode) jjtGetChild(0)).value(context);
        Object right = ((SimpleNode) jjtGetChild(1)).value(context);

        /*
         *  the spec says 'and', I think 'or'
         */
        if (left == null && right == null)
            return new Integer(0);

        /*
         *  if anything is float, double or string with ( "." | "E" | "e")
         *  coerce all to doubles and do it
         */
        if ( left instanceof Float || left instanceof Double
            || right instanceof Float || right instanceof Double
            || (  left instanceof String
                 && (  ((String) left).indexOf(".") != -1 ||
                    ((String) left).indexOf("e") != -1 ||
                    ((String) left).indexOf("E") != -1 )
            )
            || (  right instanceof String
                && (  ((String) right).indexOf(".") != -1 ||
                ((String) right).indexOf("e") != -1 ||
                ((String) right).indexOf("E") != -1 )
               )
        )
        {
            Double l = Coercion.coerceDouble(left);
            Double r = Coercion.coerceDouble(right);

            return new Double(l.doubleValue() - r.doubleValue());
        }

        /*
         * otherwise to longs with thee!
         */

        Long l = Coercion.coerceLong(left);
        Long r = Coercion.coerceLong(right);

        /*
         *  TODO - this is actually wrong - JSTL says to return a Long
         *  but we have problems where you have something like 
         * 
         *     foo.bar( a - b )
         * 
         *  where bar wants an int... 
         * 
         */
        long v = l.longValue() - r.longValue();

        if ( left instanceof Integer && right instanceof Integer )
        {
            return new Integer((int) v);
        }
        else
        {
            return new Long(v);
        }
     }
    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }
}
