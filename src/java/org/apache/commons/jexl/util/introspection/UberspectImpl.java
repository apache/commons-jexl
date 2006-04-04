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

package org.apache.commons.jexl.util.introspection;

import org.apache.commons.jexl.util.ArrayIterator;
import org.apache.commons.jexl.util.EnumerationIterator;
import org.apache.commons.jexl.util.AbstractExecutor;
import org.apache.commons.jexl.util.GetExecutor;
import org.apache.commons.jexl.util.BooleanPropertyExecutor;
import org.apache.commons.jexl.util.PropertyExecutor;
import org.apache.commons.logging.Log;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Collection;
import java.util.Map;
import java.util.Enumeration;
import java.util.ArrayList;

/**
 *  Implementation of Uberspect to provide the default introspective
 *  functionality of Velocity
 *
 * @since 1.0
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public class UberspectImpl implements Uberspect, UberspectLoggable
{
    /**
     *  Our runtime logger.
     */
    private Log rlog;

    /**
     *  the default Velocity introspector
     */
    private static Introspector introspector;

    /**
     *  init - does nothing - we need to have setRuntimeLogger
     *  called before getting our introspector, as the default
     *  vel introspector depends upon it.
     */
    public void init()
        throws Exception
    {
    }

    /**
     *  Sets the runtime logger - this must be called before anything
     *  else besides init() as to get the logger.  Makes the pull
     *  model appealing...
     */
    public void setRuntimeLogger(Log runtimeLogger)
    {
        rlog = runtimeLogger;
        introspector = new Introspector(rlog);
    }

    /**
     *  To support iteratives - #foreach()
     */
    public Iterator getIterator(Object obj, Info i)
            throws Exception
    {
        if (obj.getClass().isArray())
        {
            return new ArrayIterator(obj);
        }
        else if (obj instanceof Collection)
        {
            return ((Collection) obj).iterator();
        }
        else if (obj instanceof Map)
        {
            return ((Map) obj).values().iterator();
        }
        else if (obj instanceof Iterator)
        {
            rlog.warn ("Warning! The iterative "
                          + " is an Iterator in the #foreach() loop at ["
                          + i.getLine() + "," + i.getColumn() + "]"
                          + " in template " + i.getTemplateName()
                          + ". Because it's not resetable,"
                          + " if used in more than once, this may lead to"
                          + " unexpected results.");

            return ((Iterator) obj);
        }
        else if (obj instanceof Enumeration)
        {
            rlog.warn ("Warning! The iterative "
                          + " is an Enumeration in the #foreach() loop at ["
                          + i.getLine() + "," + i.getColumn() + "]"
                          + " in template " + i.getTemplateName()
                          + ". Because it's not resetable,"
                          + " if used in more than once, this may lead to"
                          + " unexpected results.");

            return new EnumerationIterator((Enumeration) obj);
        }

        /*  we have no clue what this is  */
        rlog.warn ("Could not determine type of iterator in "
                      +  "#foreach loop "
                      + " at [" + i.getLine() + "," + i.getColumn() + "]"
                      + " in template " + i.getTemplateName() );

        return null;
    }

    /**
     *  Method
     */
    public VelMethod getMethod(Object obj, String methodName, Object[] args, Info i)
            throws Exception
    {
        if (obj == null)
            return null;

        Method m = introspector.getMethod(obj.getClass(), methodName, args);
        if (m == null && obj instanceof Class) {
            m = introspector.getMethod((Class) obj, methodName, args);
        }

        return (m == null) ? null : new VelMethodImpl(m);
    }

    /**
     * Property  getter
     */
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info i)
            throws Exception
    {
        AbstractExecutor executor;

        Class claz = obj.getClass();

        /*
         *  first try for a getFoo() type of property
         *  (also getfoo() )
         */

        executor = new PropertyExecutor(rlog,introspector, claz, identifier);

        /*
         *  look for boolean isFoo()
         */

        if( !executor.isAlive())
        {
            executor = new BooleanPropertyExecutor(rlog, introspector, claz, identifier);
        }

        /*
         *  if that didn't work, look for get("foo")
         */

        if (!executor.isAlive())
        {
            executor = new GetExecutor(rlog, introspector, claz, identifier);
        }

        return (executor == null) ? null : new VelGetterImpl(executor);
    }

    /**
     * Property setter
     */
    public VelPropertySet getPropertySet(Object obj, String identifier, Object arg, Info i)
            throws Exception
    {
        Class claz = obj.getClass();

        VelMethod vm = null;
        try
        {
            /*
             *  first, we introspect for the set<identifier> setter method
             */

            Object[] params = {arg};

            try
            {
                vm = getMethod(obj, "set" + identifier, params, i);

                if (vm == null)
                {
                   throw new NoSuchMethodException();
                }
            }
            catch(NoSuchMethodException nsme2)
            {
                StringBuffer sb = new StringBuffer("set");
                sb.append(identifier);

                if (Character.isLowerCase( sb.charAt(3)))
                {
                    sb.setCharAt(3, Character.toUpperCase(sb.charAt(3)));
                }
                else
                {
                    sb.setCharAt(3, Character.toLowerCase(sb.charAt(3)));
                }

                vm = getMethod(obj, sb.toString(), params, i);

                if (vm == null)
                {
                   throw new NoSuchMethodException();
                }
            }
        }
        catch (NoSuchMethodException nsme)
        {
            /*
             *  right now, we only support the Map interface
             */

            if (Map.class.isAssignableFrom(claz))
            {
                Object[] params = {new Object(), new Object()};

                vm = getMethod(obj, "put", params, i);

                if (vm!=null)
                    return new VelSetterImpl(vm, identifier);
            }
       }

       return (vm ==null) ?  null : new VelSetterImpl(vm);
    }

    /**
     *  Implementation of VelMethod
     */
    public class VelMethodImpl implements VelMethod
    {
        Method method = null;

        public VelMethodImpl(Method m)
        {
            method = m;
        }

        private VelMethodImpl()
        {
        }

        public Object invoke(Object o, Object[] params)
            throws Exception
        {
            try
            {
                return method.invoke(o, params);
            }
            catch( InvocationTargetException e )
            {
                final Throwable t = e.getTargetException();

                if( t instanceof Exception )
                {
                    throw (Exception)t;
                }
                else if (t instanceof Error)
                {
                    throw (Error)t;
                }
                else
                {
                    throw e;
                }
            }
        }

        public boolean isCacheable()
        {
            return true;
        }

        public String getMethodName()
        {
            return method.getName();
        }

        public Class getReturnType()
        {
            return method.getReturnType();
        }
    }

    public class VelGetterImpl implements VelPropertyGet
    {
        AbstractExecutor ae = null;

        public VelGetterImpl(AbstractExecutor exec)
        {
            ae = exec;
        }

        private VelGetterImpl()
        {
        }

        public Object invoke(Object o)
            throws Exception
        {
            return ae.execute(o);
        }

        public boolean isCacheable()
        {
            return true;
        }

        public String getMethodName()
        {
            return ae.getMethod().getName();
        }

    }

    public class VelSetterImpl implements VelPropertySet
    {
        VelMethod vm = null;
        String putKey = null;

        public VelSetterImpl(VelMethod velmethod)
        {
            this.vm = velmethod;
        }

        public VelSetterImpl(VelMethod velmethod, String key)
        {
            this.vm = velmethod;
            putKey = key;
        }

        private VelSetterImpl()
        {
        }

        public Object invoke(Object o, Object value)
            throws Exception
        {
            ArrayList al = new ArrayList();

            if (putKey == null)
            {
                al.add(value);
            }
            else
            {
                al.add(putKey);
                al.add(value);
            }

            return vm.invoke(o,al.toArray());
        }

        public boolean isCacheable()
        {
            return true;
        }

        public String getMethodName()
        {
            return vm.getMethodName();
        }

    }
}
