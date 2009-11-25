/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl2.util;
import org.apache.commons.jexl2.util.introspection.MethodKey;
import org.apache.commons.jexl2.util.introspection.JexlMethod;
import org.apache.commons.jexl2.util.introspection.JexlPropertySet;
import org.apache.commons.jexl2.util.introspection.JexlPropertyGet;
import java.lang.reflect.InvocationTargetException;

/**
 * Abstract class that is used to execute an arbitrary
 * method that is introspected. This is the superclass
 * for all other AbstractExecutor classes.
 *
 * @since 1.0
 * @author <a href="mailto:jvanzyl@apache.org">Jason van Zyl</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @version $Id$
 */
public abstract class AbstractExecutor {
    /** A marker for invocation failures in tryInvoke. */
    public static final Object TRY_FAILED = new Object() {
        @Override
        public String toString() {
            return "tryExecute failed";
        }
    };

    /**
     * A helper to initialize the marker methods (array.get, list.get, etc...).
     * @param clazz the class to introspect
     * @param name the name of the method
     * @param parms the parameters
     * @return the method
     */
    static java.lang.reflect.Method initMarker(Class<?> clazz, String name, Class<?>... parms) {
        try {
            return clazz.getMethod(name, parms);
        } catch (Exception xnever) {
            throw new Error(xnever);
        }
    }

    /**
     * Creates an arguments array.
     * @param args the list of arguments
     * @return the arguments array
     */
    static Object[] makeArgs(Object... args) {
        return args;
    }

    /** The class this executor applies to. */
    protected final Class<?> objectClass;
    /** Method to be executed. */
    protected final java.lang.reflect.Method method;

    /**
     * Default and sole constructor.
     * @param theClass the class this executor applies to
     * @param theMethod the method held by this executor
     */
    protected AbstractExecutor(Class<?> theClass, java.lang.reflect.Method theMethod) {
        objectClass = theClass;
        method = theMethod;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof AbstractExecutor && equals((AbstractExecutor) obj));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return method.hashCode();
    }

    /**
     *  Indicates whether some other executor is equivalent to this one.
     * @param arg the other executor to check
     * @return true if both executors are equivalent, false otherwise
     */
    public boolean equals(AbstractExecutor arg) {
        // common equality check
        if (!this.getClass().equals(arg.getClass())) {
            return false;
        }
        if (!this.getMethod().equals(arg.getMethod())) {
            return false;
        }
        if (!this.getTargetClass().equals(arg.getTargetClass())) {
            return false;
        }
        // specific equality check
        Object lhsp = this.getTargetProperty();
        Object rhsp = arg.getTargetProperty();
        if (lhsp == null && rhsp == null) {
            return true;
        }
        if (lhsp != null && rhsp != null) {
            return lhsp.equals(rhsp);
        }
        return false;
    }

    /**
     * Tell whether the executor is alive by looking
     * at the value of the method.
     *
     * @return boolean Whether the executor is alive.
     */
    public final boolean isAlive() {
        return (method != null);
    }

    /**
     * Specifies if this executor is cacheable and able to be reused for this
     * class of object it was returned for.
     *
     * @return true if can be reused for this class, false if not
     */
    public boolean isCacheable() {
        return method != null;
    }

    /**
     * Gets the method to be executed or used as a marker.
     * @return Method The method used by execute in derived classes.
     */
    public final java.lang.reflect.Method getMethod() {
        return method;
    }

    /**
     * Gets the object class targeted by this executor.
     * @return the target object class
     */
    public final Class<?> getTargetClass() {
        return objectClass;
    }
    
    /**
     * Gets the property targeted by this executor.
     * @return the target property
     */
    public Object getTargetProperty() {
        return null;
    }

    /**
     * Gets the method name used.
     * @return method name
     */
    public final String getMethodName() {
        return method.getName();
    }

    /**
     * Abstract class that is used to execute an arbitrary 'get' method.
     */
    public abstract static class Get extends AbstractExecutor implements JexlPropertyGet {
        /**
         * Default and sole constructor.
         * @param theClass the class this executor applies to
         * @param theMethod the method held by this executor
         */
        protected Get(Class<?> theClass, java.lang.reflect.Method theMethod) {
            super(theClass, theMethod);
        }

        /** {@inheritDoc} */
        public final Object invoke(Object obj) throws Exception {
            return execute(obj);
        }

        /**
         * Gets the property value from an object.
         *
         * @param obj The object to get the property from.
         * @return The property value.
         * @throws IllegalAccessException Method is inaccessible.
         * @throws InvocationTargetException Method body throws an exception.
         */
        public abstract Object execute(Object obj)
                throws IllegalAccessException, InvocationTargetException;

        /**
         * Tries to reuse this executor, checking that it is compatible with
         * the actual set of arguments.
         * <p>Compatibility means that:
         * <code>o</code> must be of the same class as this executor's
         * target class and
         * <code>property</code> must be of the same class as this
         * executor's target property (for list and map based executors) and have the same
         * value (for other types).</p>
         * @param obj The object to get the property from.
         * @param key The property to get from the object.
         * @return The property value or TRY_FAILED if checking failed.
         */
        public Object tryExecute(Object obj, Object key) {
            return TRY_FAILED;
        }
    }
    
    /**
     * Abstract class that is used to execute an arbitrary 'set' method.
     */
    public abstract static class Set extends AbstractExecutor implements JexlPropertySet {
        /**
         * Default and sole constructor.
         * @param theClass the class this executor applies to
         * @param theMethod the method held by this executor
         */
        protected Set(Class<?> theClass, java.lang.reflect.Method theMethod) {
            super(theClass, theMethod);
        }

        /** {@inheritDoc} */
        public Object invoke(Object obj, Object arg) throws Exception {
            return execute(obj, arg);
        }

        /**
         * Sets the property value of an object.
         *
         * @param obj The object to set the property in.
         * @param value The value.
         * @return The return value.
         * @throws IllegalAccessException Method is inaccessible.
         * @throws InvocationTargetException Method body throws an exception.
         */
        public abstract Object execute(Object obj, Object value)
                throws IllegalAccessException, InvocationTargetException;

        /**
         * Tries to reuse this executor, checking that it is compatible with
         * the actual set of arguments.
         * <p>Compatibility means that:
         * <code>o</code> must be of the same class as this executor's
         * target class,
         * <code>property</code> must be of the same class as this
         * executor's target property (for list and map based executors) and have the same
         * value (for other types)
         * and that <code>arg</code> must be a valid argument for this
         * executor underlying method.</p>
         * @param obj The object to invoke the method from.
         * @param key The property to set in the object.
         * @param value The value to use as the property value.
         * @return The return value or TRY_FAILED if checking failed.
         */
        public Object tryExecute(Object obj, Object key, Object value) {
            return TRY_FAILED;
        }
        
    }



    /**
     * Abstract class that is used to execute an arbitrary method.
     */
    public abstract static class Method extends AbstractExecutor implements JexlMethod {
        /**
         * A helper class to pass the method &amp; parameters.
         */
        protected static final class Parameter {
            /** The method. */
            private final java.lang.reflect.Method method;
            /** The method key. */
            private final MethodKey key;
            /** Creates an instance.
             * @param m the method
             * @param k the method key
             */
            public Parameter(java.lang.reflect.Method m, MethodKey k) {
                method = m;
                key = k;
            }
        }
        /** The method key discovered from the arguments. */
        protected final MethodKey key;
        /**
         * Creates a new instance.
         * @param c the class this executor applies to
         * @param km the method and MethodKey to encapsulate.
         */
        protected Method(Class<?> c, Parameter km) {
            super(c, km.method);
            key = km.key;
        }

        /** {@inheritDoc} */
        public final Object invoke(Object obj, Object[] args) throws Exception {
            return execute(obj, args);
        }

        /** {@inheritDoc} */
        @Override
        public Object getTargetProperty() {
            return key;
        }
        
        /**
         * Returns the return type of the method invoked.
         * @return return type
         */
        public final Class<?> getReturnType() {
            return method.getReturnType();
        }

        /**
         * Invokes the method to be executed.
         *
         * @param obj the object to invoke the method upon
         * @param args the method arguments
         * @return the result of the method invocation
         * @throws IllegalAccessException Method is inaccessible.
         * @throws InvocationTargetException Method body throws an exception.
         */
        public abstract Object execute(Object obj, Object[] args)
                throws IllegalAccessException, InvocationTargetException;

        /**
         * Tries to reuse this executor, checking that it is compatible with
         * the actual set of arguments.
         * @param obj the object to invoke the method upon
         * @param name the method name
         * @param args the method arguments
         * @return the result of the method invocation or INVOKE_FAILED if checking failed.
         */
        public Object tryExecute(String name, Object obj, Object[] args){
            return TRY_FAILED;
        }

    }

}
