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

import org.apache.commons.jexl.JexlHelper;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;

/**
 *  simple example to show how to access method and properties
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *  @version $Id: MethodPropertyExample.java,v 1.4 2004/02/28 13:45:20 yoavs Exp $
 */
public class MethodPropertyExample
{
    public static void main(String args[])
        throws Exception
    {
        /*
         *  First make a jexlContext and put stuff in it
         */
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", new Foo());
        jc.getVars().put("number", new Integer(10));

        /*
         *  access a method w/o args
         */
        Expression e = ExpressionFactory.createExpression("foo.getFoo()");
        Object o = e.evaluate(jc);
        System.out.println("value returned by the method getFoo() is : " + o);

        /*
         *  access a method w/ args
         */
        e = ExpressionFactory.createExpression("foo.convert(1)");
        o = e.evaluate(jc);
        System.out.println("value returned by the method convert() w/ arg = 1 is : " + o);

        e = ExpressionFactory.createExpression("foo.convert(1+7)");
        o = e.evaluate(jc);
        System.out.println("value returned by the method convert() w/ arg = 1+7 is : " + o);

        e = ExpressionFactory.createExpression("foo.convert(1+number)");
        o = e.evaluate(jc);
        System.out.println("value returned by the method convert() w/ arg = 1+number is : " + o);

        /*
         * access a property
         */
        e = ExpressionFactory.createExpression("foo.bar");
        o = e.evaluate(jc);
        System.out.println("value returned for the property 'bar' is : " + o);

    }

    public static class Foo
    {
        public String getFoo()
        {
            return "This is from getFoo()";
        }

        public String get(String arg)
        {
            return "This is the property " + arg;
        }

        public String convert(long i)
        {
            return "The value is : " + i;
        }
    }

}
