package org.apache.commons.jexl.parser;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.util.Introspector;
import org.apache.commons.jexl.util.introspection.VelMethod;
import org.apache.commons.jexl.util.introspection.Info;

public class ASTMethod extends SimpleNode
{
    public ASTMethod(int id)
    {
        super(id);
    }

    public ASTMethod(Parser p, int id)
    {
        super(p, id);
    }


    /** Accept the visitor. **/
    public Object jjtAccept(ParserVisitor visitor, Object data)
    {
        return visitor.visit(this, data);
    }

    /**
     *  returns the value of itself applied to the object.
     *   We assume that an identifier can be gotten via a get(String)
     */
    public Object execute(Object obj, JexlContext jc)
    {
        String methodName = ((ASTIdentifier)jjtGetChild(0)).val;

        try
        {
            int paramCount = jjtGetNumChildren()-1;

            /*
             *  get our params
             */

            Object params[] = new Object[paramCount];

            for (int i=0; i<paramCount; i++)
            {
                params[i] = ( (SimpleNode) jjtGetChild(i+1)).value(jc);
            }

            VelMethod vm = Introspector.getUberspect().getMethod(obj, methodName,
                params, new Info("",1,1));

            if (vm == null)
                return null;

            return vm.invoke(obj, params);
        }
        catch(Exception e)
        {
            System.out.println("ASTIdentifier : "+ e);
            e.printStackTrace();;
        }

        return null;
    }

}
