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
import org.apache.commons.jexl.util.Introspector;
import org.apache.commons.jexl.util.introspection.Info;
import org.apache.commons.jexl.util.introspection.VelPropertyGet;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Array;


/**
 *  Like an ASTIdentifier, but with array access allowed
 *
 *    $foo[2]
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ASTArrayAccess.java,v 1.7 2004/08/22 07:42:35 dion Exp $
 */
public class ASTArrayAccess extends SimpleNode
{
    /** dummy velocity info */
    private static Info DUMMY = new Info("", 1, 1);
    
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

            VelPropertyGet vg = Introspector.getUberspect().getPropertyGet(o, s, DUMMY);

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
