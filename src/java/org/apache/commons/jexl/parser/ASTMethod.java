package org.apache.commons.jexl.parser;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;

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
            /*
             * DG: If we can't find an exact match, narrow the parameters and try again! 
             */
            if (vm == null)
            {
            	
            	// replace all numbers with the smallest type that will fit
            	for (int i = 0; i < params.length; i++)
            	{
            		Object param = params[i];
            		if (param instanceof Number)
            		{
            			params[i] = narrow((Number)param);
            		}
            	}
            	vm = Introspector.getUberspect().getMethod(obj, methodName, params, DUMMY);
                if (vm == null) return null;
            }

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

    /**
     * Given a Number, return back the value using the smallest type the result will fit into.
     * This works hand in hand with parameter 'widening' in java method calls,
     * e.g. a call to substring(int,int) with an int and a long will fail, but
     * a call to substring(int,int) with an int and a short will succeed.
     * @since 1.0.1
     */
    private Number narrow(Number original)
    {
    	if (original == null || original instanceof BigDecimal || original instanceof BigInteger) return original; 
    	Number result = original;
    	if (original instanceof Double || original instanceof Float)
    	{
    		double value = original.doubleValue();
    		if (value <= Float.MAX_VALUE && value >= Float.MIN_VALUE)
    		{
    			result = new Float(result.floatValue());
    		}
   			// else it was already a double
    	}
    	else
    	{
    		long value = original.longValue();
	        if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE)
	        {
	            // it will fit in a byte
	            result = new Byte((byte)value);
	        }
	        else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE)
	        {
	        	result = new Short((short)value);
	        }
	        else if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE)
	        {
	        	result = new Integer((int)value);
	        }
   			// else it was already a long
    	}
        return result;
    }

}
