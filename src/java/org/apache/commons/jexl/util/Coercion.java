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
package org.apache.commons.jexl.util;

/**
 *  Coercion utilities for the JSTL EL-like coercion.
 *
 *  @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 */
public class Coercion
{

    public static Boolean coerceBoolean(Object val)
        throws Exception
    {
        if (val == null)
        {
            return Boolean.FALSE;
        }
        else if (val instanceof Boolean)
        {
            return (Boolean) val;
        }
        else if (val instanceof String)
        {
            return Boolean.valueOf((String) val);
        }

        return null;
    }

    public static Integer coerceInteger(Object val)
        throws Exception
    {
        if (val == null)
        {
            return new Integer(0);
        }
        else if (val instanceof String)
        {
            if("".equals((String) val))
                return new Integer(0);

            return Integer.valueOf((String)val);
        }
        else if(val instanceof Character)
        {
            return new Integer((int)((Character)val).charValue());
        }
        else if(val instanceof Boolean)
        {
            throw new Exception("Boolean->Integer coercion exception");
        }
        else if(val instanceof Number)
        {
            return new Integer(((Number)val).intValue());
        }

        throw new Exception("Integer coercion exception");
    }

    public static Long coerceLong(Object val)
        throws Exception
    {
        if (val == null)
        {
            return new Long(0);
        }
        else if (val instanceof String)
        {
            if("".equals((String) val))
                return new Long(0);

            return Long.valueOf((String)val);
        }
        else if(val instanceof Character)
        {
            return new Long((long)((Character)val).charValue());
        }
        else if(val instanceof Boolean)
        {
            throw new Exception("Boolean->Integer coercion exception");
        }
        else if(val instanceof Number)
        {
            return new Long(((Number)val).longValue());
        }

        throw new Exception("Long coercion exception");
    }

    public static Double coerceDouble(Object val)
        throws Exception
    {
        if (val == null)
        {
            return new Double(0);
        }
        else if (val instanceof String)
        {
            if("".equals((String) val))
                return new Double(0);

            /*
             * the spec seems to be iffy about this.  Going to give it a wack
             *  anyway
             */

            return new Double((String) val);
        }
        else if(val instanceof Character)
        {
            int i = ((Character)val).charValue();

            return new Double(Double.parseDouble(String.valueOf(i)));
        }
        else if(val instanceof Boolean)
        {
            throw new Exception("Boolean->Integer coercion exception");
        }
        else if(val instanceof Double)
        {
            return (Double) val;
        }
        else if (val instanceof Number)
        {
            //The below construct is used rather than ((Number)val).doubleValue() to ensure
            //equality between comparint new Double( 6.4 / 3 ) and the jexl expression of 6.4 / 3
            return new Double(Double.parseDouble(String.valueOf(val)));
        }

        throw new Exception("Double coercion exception");
    }

    public static boolean isFloatingPoint( final Object o )
    {
        return o instanceof Float || o instanceof Double;
    }

    public static boolean isNumberable( final Object o )
    {
        return o instanceof Integer
            || o instanceof Long
            || o instanceof Byte
            || o instanceof Short
            || o instanceof Character;
    }

}
