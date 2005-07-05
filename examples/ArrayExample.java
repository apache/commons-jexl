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

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;

import java.util.List;
import java.util.ArrayList;

/**
 *  simple example to show how to access arrays
 *
 *  @since 1.0
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id$
 */
public class ArrayExample
{
    public static void main(String args[])
        throws Exception
    {
        /*
         *  First make a jexlContext and put stuff in it
         */
        JexlContext jc = JexlHelper.createContext();

        List l = new ArrayList();
        l.add("Hello from location 0");
        l.add(new Integer(2));
        jc.getVars().put("array", l);

        Expression e = ExpressionFactory.createExpression("array[1]");
        Object o = e.evaluate(jc);
        System.out.println("Object @ location 1 = " + o);

        e = ExpressionFactory.createExpression("array[0].length()");
        o = e.evaluate(jc);

        System.out.println("The length of the string at location 0 is : " + o);
    }
}
