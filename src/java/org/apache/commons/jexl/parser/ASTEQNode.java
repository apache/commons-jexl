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
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", "Jexl" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
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

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.util.Coercion;

/**
 *  represents equality between integers - use .equals() for strings
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ASTEQNode.java,v 1.2 2002/07/08 00:21:54 geirm Exp $
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
