/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
import org.apache.commons.jexl.util.Introspector;
import org.apache.commons.jexl.util.introspection.Info;
import org.apache.commons.jexl.util.introspection.VelMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Array;

/**
 *  generalized size() function for all classes we can think of
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @author <a href="hw@kremvax.net">Mark H. Wilkinson</a>
 *  @version $Id: ASTSizeFunction.java,v 1.6 2004/08/15 16:01:12 dion Exp $
 */
public class ASTSizeFunction extends SimpleNode
{
    public ASTSizeFunction(int id)
    {
        super(id);
    }

    public ASTSizeFunction(Parser p, int id)
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
        SimpleNode arg = (SimpleNode) jjtGetChild(0);

        Object val = arg.value(jc);

        if (val == null)
        {
            throw new Exception("size() : null arg");
        }
        
        return new Integer(ASTSizeFunction.sizeOf(val));
    }
    
    public static int sizeOf(Object val)
        throws Exception
    {
        if (val instanceof List)
        {
            return ((List)val).size();
        }
        else if (val.getClass().isArray())
        {
            return Array.getLength(val);
        }
        else if (val instanceof Map)
        {
            return ((Map)val).size();
        }
        else if (val instanceof String)
        {
            return ((String)val).length();
        }
        else if (val instanceof Set)
        {
            return ((Set)val).size();
        }
        else {
        	// check if there is a size method on the object that returns an integer
        	// and if so, just use it
        	Object[] params = new Object[0];
        	Info velInfo = new Info("",1,1);
            VelMethod vm = Introspector.getUberspect().getMethod(val, "size", params, velInfo);
            if (vm != null && vm.getReturnType() == Integer.TYPE)
            {
            	Integer result = (Integer)vm.invoke(val, params);
            	return result.intValue();
            }
            else
            {
                throw new Exception("size() : unknown type : " + val.getClass());
            }
        }
    }

}
