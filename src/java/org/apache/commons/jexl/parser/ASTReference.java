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
 *  reference - any variable expression
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: ASTReference.java,v 1.3 2004/02/28 13:45:20 yoavs Exp $
 */
public class ASTReference extends SimpleNode
{
    SimpleNode root;

    public ASTReference(int id)
    {
        super(id);
    }

    public ASTReference(Parser p, int id)
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
        return  execute(null,jc);
    }

    public void jjtClose()
    {
        root = (SimpleNode) jjtGetChild(0);
    }

    public Object execute(Object obj, JexlContext jc)
             throws Exception
     {
         Object o = root.value(jc);

         /*
          * ignore the first child - it's our identifier
          */
         for(int i=1; i<jjtGetNumChildren(); i++)
         {
             o = ( (SimpleNode) jjtGetChild(i)).execute(o,jc);

             if(o==null)
                 return null;
         }

         return o;
     }

    public String getRootString()
        throws Exception
    {
        if ( root instanceof ASTIdentifier)
            return ((ASTIdentifier) root).getIdentifierString();

        if (root instanceof ASTArrayAccess)
            return ((ASTArrayAccess) root).getIdentifierString();

        throw new Exception("programmer error : ASTReference : root not known"
                    + root );
    }
}
