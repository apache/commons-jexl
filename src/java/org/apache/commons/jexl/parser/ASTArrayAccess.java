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
import org.apache.commons.jexl.util.Introspector;
import org.apache.velocity.util.introspection.VelPropertyGet;
import org.apache.velocity.util.introspection.Info;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Array;


/**
 *  Like an ASTIdentifier, but with array access allowed
 *
 *    $foo[2]
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ASTArrayAccess.java,v 1.3 2002/06/07 03:35:30 geirm Exp $
 */
public class ASTArrayAccess extends SimpleNode
{
    public ASTArrayAccess(int id)
    {
        super(id);
    }

    public ASTArrayAccess(Parser p, int id)
    {
        super(p, id);
    }


    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    /*
     * evaluate array access upon a base object
     *
     *   foo.bar[2]
     *
     *  makes me rethink the array operator :)
     */
    public Object execute(Object obj, JexlContext jc)
             throws Exception
     {
         ASTIdentifier base = (ASTIdentifier) jjtGetChild(0);

         obj = base.execute(obj,jc);

         /*
          * ignore the first child - it's our identifier
          */
         for(int i=1; i<jjtGetNumChildren(); i++)
         {
             Object loc = ((SimpleNode) jjtGetChild(i)).value(jc);

             if(loc==null)
                 return null;

             obj = evaluateExpr(obj, loc);
         }

         return obj;
     }

    /**
     *  return the value of this node
     */
    public Object value(JexlContext jc)
        throws Exception
    {
        /*
         * get the base ASTIdentifier
         */

        ASTIdentifier base = (ASTIdentifier) jjtGetChild(0);

        Object o = base.value(jc);

        /*
         * ignore the first child - it's our identifier
         */
        for(int i=1; i<jjtGetNumChildren(); i++)
        {
            Object loc = ((SimpleNode) jjtGetChild(i)).value(jc);

            if(loc==null)
                return null;

            o = evaluateExpr(o, loc);
        }

        return o;
    }

    public static Object evaluateExpr(Object o, Object loc)
        throws Exception
    {
        /*
         * following the JSTL EL rules
         */

        if (o == null)
            return null;

        if (loc == null)
            return null;

        if (o instanceof Map)
        {
            if (!((Map)o).containsKey(loc))
                return null;

            return ((Map)o).get(loc);
        }
        else if (o instanceof List)
        {
            int idx = Coercion.coerceInteger(loc).intValue();

            try
            {
                return ((List)o).get(idx);
            }
            catch(IndexOutOfBoundsException iobe)
            {
                return null;
            }
        }
        else if (o.getClass().isArray())
        {
            int idx = Coercion.coerceInteger(loc).intValue();

            try
            {
                return Array.get(o, idx);
            }
            catch(ArrayIndexOutOfBoundsException aiobe)
            {
                return null;
            }
        }
        else
        {
            /*
             *  "Otherwise (a JavaBean object)..."  huh? :)
             */

            String s = loc.toString();

            VelPropertyGet vg = Introspector.getUberspect().getPropertyGet(o,s,new Info("",1,1));

            if (vg != null)
            {
                return vg.invoke(o);
            }
        }

        throw new Exception("Unsupported object type for array [] accessor");
    }

    public String getIdentifierString()
    {
        return ((ASTIdentifier) jjtGetChild(0)).getIdentifierString();
    }
}
