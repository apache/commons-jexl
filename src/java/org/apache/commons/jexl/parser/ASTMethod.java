package org.apache.commons.jexl.parser;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.util.Introspector;
import org.apache.commons.jexl.util.introspection.VelMethod;
import org.apache.commons.jexl.util.introspection.Info;

public class ASTMethod extends SimpleNode
{
    /** dummy velocity info */
    private static Info DUMMY = new Info("", 1, 1);

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
        throws Exception
    {
        String methodName = ((ASTIdentifier)jjtGetChild(0)).val;

        int paramCount = jjtGetNumChildren()-1;

        /*
         *  get our params
         */

        Object params[] = new Object[paramCount];

        try
        {
            for (int i=0; i<paramCount; i++)
            {
                params[i] = ( (SimpleNode) jjtGetChild(i+1)).value(jc);
            }

            VelMethod vm = Introspector.getUberspect().getMethod(obj, methodName, params, DUMMY);

            if (vm == null)
                return null;

            return vm.invoke(obj, params);
        }
        catch(InvocationTargetException e)
        {
            Throwable t = e.getTargetException();

            if (t instanceof Exception)
            {
                throw (Exception) t;
            }

            throw e;
        }
    }
}
