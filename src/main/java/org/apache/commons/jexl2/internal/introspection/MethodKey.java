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
package org.apache.commons.jexl2.internal.introspection;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A method key usable by the introspector cache.
 * <p>
 * This stores a method (or class) name and parameters.
 * </p>
 * <p>
 * This replaces the original key scheme which used to build the key
 * by concatenating the method name and parameters class names as one string
 * with the exception that primitive types were converted to their object class equivalents.
 * </p>
 * <p>
 * The key is still based on the same information, it is just wrapped in an object instead.
 * Primitive type classes are converted to they object equivalent to make a key;
 * int foo(int) and int foo(Integer) do generate the same key.
 * </p>
 * A key can be constructed either from arguments (array of objects) or from parameters
 * (array of class).
 * Roughly 3x faster than string key to access the map &amp; uses less memory.
 */
public final class MethodKey {
    /** The hash code. */
    private final int hashCode;
    /** The method name. */
    private final String method;
    /** The parameters. */
    private final Class<?>[] params;
    /** A marker for empty parameter list. */
    private static final Class<?>[] NOARGS = new Class<?>[0];
    /** The hash code constants. */
    private static final int HASH = 37;

    /**
     * Creates a key from a method name and a set of arguments.
     * @param aMethod the method to generate the key from
     * @param args the intended method arguments
     */
    public MethodKey(String aMethod, Object[] args) {
        super();
        // !! keep this in sync with the other ctor (hash code) !!
        this.method = aMethod;
        int hash = this.method.hashCode();
        final int size;
        // CSOFF: InnerAssignment
        if (args != null && (size = args.length) > 0) {
            this.params = new Class<?>[size];
            for (int p = 0; p < size; ++p) {
                Object arg = args[p];
                // null arguments use void as Void.class as marker
                Class<?> parm = arg == null ? Void.class : arg.getClass();
                hash = (HASH * hash) + parm.hashCode();
                this.params[p] = parm;
            }
        } else {
            this.params = NOARGS;
        }
        this.hashCode = hash;
    }
    
    /**
     * Creates a key from a method.
     * @param aMethod the method to generate the key from.
     */
    MethodKey(Method aMethod) {
        this(aMethod.getName(), aMethod.getParameterTypes());
    }

    /**
     * Creates a key from a method name and a set of parameters.
     * @param aMethod the method to generate the key from
     * @param args the intended method parameters
     */
    MethodKey(String aMethod, Class<?>[] args) {
        super();
        // !! keep this in sync with the other ctor (hash code) !!
        this.method = aMethod.intern();
        int hash = this.method.hashCode();
        final int size;
        // CSOFF: InnerAssignment
        if (args != null && (size = args.length) > 0) {
            this.params = new Class<?>[size];
            for (int p = 0; p < size; ++p) {
                Class<?> parm = ClassMap.MethodCache.primitiveClass(args[p]);
                hash = (HASH * hash) + parm.hashCode();
                this.params[p] = parm;
            }
        } else {
            this.params = NOARGS;
        }
        this.hashCode = hash;
    }

    /**
     * Gets this key's method name.
     * @return the method name
     */
    String getMethod() {
        return method;
    }

    /**
     * Gets this key's method parameter classes.
     * @return the parameters
     */
    Class<?>[] getParameters() {
        return params;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodKey) {
            MethodKey key = (MethodKey) obj;
            return method.equals(key.method) && Arrays.equals(params, key.params);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(method);
        for (Class<?> c : params) {
            builder.append(c == Void.class ? "null" : c.getName());
        }
        return builder.toString();
    }

    /**
     * Outputs a human readable debug representation of this key.
     * @return method(p0, p1, ...)
     */
    public String debugString() {
        StringBuilder builder = new StringBuilder(method);
        builder.append('(');
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(Void.class == params[i] ? "null" : params[i].getName());
        }
        builder.append(')');
        return builder.toString();
    }

    /**
     * Gets the most specific method that is applicable to the parameters of this key.
     * @param methods a list of methods.
     * @return the most specific method.
     * @throws MethodKey.AmbiguousException if there is more than one.
     */
    public Method getMostSpecificMethod(List<Method> methods) {
        return METHODS.getMostSpecific(methods, params);
    }

    /**
     * Gets the most specific constructor that is applicable to the parameters of this key.
     * @param methods a list of constructors.
     * @return the most specific constructor.
     * @throws MethodKey.AmbiguousException if there is more than one.
     */
    public Constructor<?> getMostSpecificConstructor(List<Constructor<?>> methods) {
        return CONSTRUCTORS.getMostSpecific(methods, params);
    }
    
    /**
     * Determines whether a type represented by a class object is
     * convertible to another type represented by a class object using a
     * method invocation conversion, treating object types of primitive
     * types as if they were primitive types (that is, a Boolean actual
     * parameter type matches boolean primitive formal type). This behavior
     * is because this method is used to determine applicable methods for
     * an actual parameter list, and primitive types are represented by
     * their object duals in reflective method calls.
     *
     * @param formal         the formal parameter type to which the actual
     *                       parameter type should be convertible
     * @param actual         the actual parameter type.
     * @param possibleVarArg whether or not we're dealing with the last parameter
     *                       in the method declaration
     * @return true if either formal type is assignable from actual type,
     *         or formal is a primitive type and actual is its corresponding object
     *         type or an object type of a primitive type that can be converted to
     *         the formal type.
     */
    public static boolean isInvocationConvertible(Class<?> formal,
            Class<?> actual,
            boolean possibleVarArg) {
        /* if it's a null, it means the arg was null */
        if (actual == null && !formal.isPrimitive()) {
            return true;
        }

        /* Check for identity or widening reference conversion */
        if (actual != null && formal.isAssignableFrom(actual)) {
            return true;
        }

        /* Check for boxing with widening primitive conversion. Note that
         * actual parameters are never primitives. */
        if (formal.isPrimitive()) {
            if (formal == Boolean.TYPE && actual == Boolean.class) {
                return true;
            }
            if (formal == Character.TYPE && actual == Character.class) {
                return true;
            }
            if (formal == Byte.TYPE && actual == Byte.class) {
                return true;
            }
            if (formal == Short.TYPE
                    && (actual == Short.class || actual == Byte.class)) {
                return true;
            }
            if (formal == Integer.TYPE
                    && (actual == Integer.class || actual == Short.class
                    || actual == Byte.class)) {
                return true;
            }
            if (formal == Long.TYPE
                    && (actual == Long.class || actual == Integer.class
                    || actual == Short.class || actual == Byte.class)) {
                return true;
            }
            if (formal == Float.TYPE
                    && (actual == Float.class || actual == Long.class
                    || actual == Integer.class || actual == Short.class
                    || actual == Byte.class)) {
                return true;
            }
            if (formal == Double.TYPE
                    && (actual == Double.class || actual == Float.class
                    || actual == Long.class || actual == Integer.class
                    || actual == Short.class || actual == Byte.class)) {
                return true;
            }
        }

        /* Check for vararg conversion. */
        if (possibleVarArg && formal.isArray()) {
            if (actual != null && actual.isArray()) {
                actual = actual.getComponentType();
            }
            return isInvocationConvertible(formal.getComponentType(),
                    actual, false);
        }
        return false;
    }

    /**
     * Determines whether a type represented by a class object is
     * convertible to another type represented by a class object using a
     * method invocation conversion, without matching object and primitive
     * types. This method is used to determine the more specific type when
     * comparing signatures of methods.
     *
     * @param formal         the formal parameter type to which the actual
     *                       parameter type should be convertible
     * @param actual         the actual parameter type.
     * @param possibleVarArg whether or not we're dealing with the last parameter
     *                       in the method declaration
     * @return true if either formal type is assignable from actual type,
     *         or formal and actual are both primitive types and actual can be
     *         subject to widening conversion to formal.
     */
    public static boolean isStrictInvocationConvertible(Class<?> formal,
            Class<?> actual,
            boolean possibleVarArg) {
        /* we shouldn't get a null into, but if so */
        if (actual == null && !formal.isPrimitive()) {
            return true;
        }

        /* Check for identity or widening reference conversion */
        if (formal.isAssignableFrom(actual)) {
            return true;
        }

        /* Check for widening primitive conversion. */
        if (formal.isPrimitive()) {
            if (formal == Short.TYPE && (actual == Byte.TYPE)) {
                return true;
            }
            if (formal == Integer.TYPE
                    && (actual == Short.TYPE || actual == Byte.TYPE)) {
                return true;
            }
            if (formal == Long.TYPE
                    && (actual == Integer.TYPE || actual == Short.TYPE
                    || actual == Byte.TYPE)) {
                return true;
            }
            if (formal == Float.TYPE
                    && (actual == Long.TYPE || actual == Integer.TYPE
                    || actual == Short.TYPE || actual == Byte.TYPE)) {
                return true;
            }
            if (formal == Double.TYPE
                    && (actual == Float.TYPE || actual == Long.TYPE
                    || actual == Integer.TYPE || actual == Short.TYPE
                    || actual == Byte.TYPE)) {
                return true;
            }
        }

        /* Check for vararg conversion. */
        if (possibleVarArg && formal.isArray()) {
            if (actual != null && actual.isArray()) {
                actual = actual.getComponentType();
            }
            return isStrictInvocationConvertible(formal.getComponentType(),
                    actual, false);
        }
        return false;
    }
    
    /**
     * whether a method/ctor is more specific than a previously compared one.
     */
    private static final int MORE_SPECIFIC = 0;
    /**
     * whether a method/ctor is less specific than a previously compared one.
     */
    private static final int LESS_SPECIFIC = 1;
    /**
     * A method/ctor doesn't match a previously compared one.
     */
    private static final int INCOMPARABLE = 2;

    /**
     * Simple distinguishable exception, used when
     * we run across ambiguous overloading.  Caught
     * by the introspector.
     */
    public static class AmbiguousException extends RuntimeException {
        /**
         * Version Id for serializable.
         */
        private static final long serialVersionUID = -2314636505414551664L;
    }

    /**
     * Utility for parameters matching.
     * @param <T> Method or Constructor
     */
    private abstract static class Parameters<T> {
        /**
         * Extract the parameter types from its applicable argument.
         * @param app a method or constructor
         * @return the parameters
         */
        protected abstract Class<?>[] getParameterTypes(T app);

        // CSOFF: RedundantThrows
        /**
         * Gets the most specific method that is applicable to actual argument types.
         * @param methods a list of methods.
         * @param classes list of argument types.
         * @return the most specific method.
         * @throws MethodKey.AmbiguousException if there is more than one.
         */
        private T getMostSpecific(List<T> methods, Class<?>[] classes) {
            LinkedList<T> applicables = getApplicables(methods, classes);

            if (applicables.isEmpty()) {
                return null;
            }

            if (applicables.size() == 1) {
                return applicables.getFirst();
            }

            /*
             * This list will contain the maximally specific methods. Hopefully at
             * the end of the below loop, the list will contain exactly one method,
             * (the most specific method) otherwise we have ambiguity.
             */

            LinkedList<T> maximals = new LinkedList<T>();

            for (Iterator<T> applicable = applicables.iterator();
                    applicable.hasNext();) {
                T app = applicable.next();
                Class<?>[] appArgs = getParameterTypes(app);

                boolean lessSpecific = false;

                for (Iterator<T> maximal = maximals.iterator();
                        !lessSpecific && maximal.hasNext();) {
                    T max = maximal.next();

                    // CSOFF: MissingSwitchDefault
                    switch (moreSpecific(appArgs, getParameterTypes(max))) {
                        case MORE_SPECIFIC:
                            /*
                             * This method is more specific than the previously
                             * known maximally specific, so remove the old maximum.
                             */
                            maximal.remove();
                            break;

                        case LESS_SPECIFIC:
                            /*
                             * This method is less specific than some of the
                             * currently known maximally specific methods, so we
                             * won't add it into the set of maximally specific
                             * methods
                             */

                            lessSpecific = true;
                            break;
                    }
                } // CSON: MissingSwitchDefault

                if (!lessSpecific) {
                    maximals.addLast(app);
                }
            }
            if (maximals.size() > 1) {
                // We have more than one maximally specific method
                throw new AmbiguousException();
            }
            return maximals.getFirst();
        } // CSON: RedundantThrows

        /**
         * Determines which method signature (represented by a class array) is more
         * specific. This defines a partial ordering on the method signatures.
         *
         * @param c1 first signature to compare
         * @param c2 second signature to compare
         * @return MORE_SPECIFIC if c1 is more specific than c2, LESS_SPECIFIC if
         *         c1 is less specific than c2, INCOMPARABLE if they are incomparable.
         */
        private int moreSpecific(Class<?>[] c1, Class<?>[] c2) {
            boolean c1MoreSpecific = false;
            boolean c2MoreSpecific = false;

            // compare lengths to handle comparisons where the size of the arrays
            // doesn't match, but the methods are both applicable due to the fact
            // that one is a varargs method
            if (c1.length > c2.length) {
                return MORE_SPECIFIC;
            }
            if (c2.length > c1.length) {
                return LESS_SPECIFIC;
            }

            // ok, move on and compare those of equal lengths
            for (int i = 0; i < c1.length; ++i) {
                if (c1[i] != c2[i]) {
                    boolean last = (i == c1.length - 1);
                    c1MoreSpecific = c1MoreSpecific || isStrictConvertible(c2[i], c1[i], last);
                    c2MoreSpecific = c2MoreSpecific || isStrictConvertible(c1[i], c2[i], last);
                }
            }

            if (c1MoreSpecific) {
                if (c2MoreSpecific) {
                    // Incomparable due to cross-assignable arguments (i.e. foo(String, Object) vs. foo(Object, String))
                    return INCOMPARABLE;
                }
                return MORE_SPECIFIC;
            }
            if (c2MoreSpecific) {
                return LESS_SPECIFIC;
            }

            // attempt to choose by picking the one with the greater number of primitives or latest primitive parameter
            int primDiff = 0;
            for (int c = 0; c < c1.length; ++c) {
                if (c1[c].isPrimitive()) {
                    primDiff += 1 << c;
                }
                if (c2[c].isPrimitive()) {
                    primDiff -= 1 << c;
                }
            }
            if (primDiff > 0) {
                return MORE_SPECIFIC;
            } else if (primDiff < 0) {
                return LESS_SPECIFIC;
            }
            /*
             * Incomparable due to non-related arguments (i.e.
             * foo(Runnable) vs. foo(Serializable))
             */
            return INCOMPARABLE;
        }

        /**
         * Returns all methods that are applicable to actual argument types.
         *
         * @param methods list of all candidate methods
         * @param classes the actual types of the arguments
         * @return a list that contains only applicable methods (number of
         *         formal and actual arguments matches, and argument types are assignable
         *         to formal types through a method invocation conversion).
         */
        private LinkedList<T> getApplicables(List<T> methods, Class<?>[] classes) {
            LinkedList<T> list = new LinkedList<T>();

            for (Iterator<T> imethod = methods.iterator(); imethod.hasNext();) {
                T method = imethod.next();
                if (isApplicable(method, classes)) {
                    list.add(method);
                }

            }
            return list;
        }

        /**
         * Returns true if the supplied method is applicable to actual
         * argument types.
         *
         * @param method  method that will be called
         * @param classes arguments to method
         * @return true if method is applicable to arguments
         */
        private boolean isApplicable(T method, Class<?>[] classes) {
            Class<?>[] methodArgs = getParameterTypes(method);
            // if samee number or args or
            // there's just one more methodArg than class arg
            // and the last methodArg is an array, then treat it as a vararg
            if (methodArgs.length == classes.length
                    || methodArgs.length == classes.length + 1 && methodArgs[methodArgs.length - 1].isArray()) {
                // this will properly match when the last methodArg
                // is an array/varargs and the last class is the type of array
                // (e.g. String when the method is expecting String...)
                for (int i = 0; i < classes.length; ++i) {
                    if (!isConvertible(methodArgs[i], classes[i], false)) {
                        // if we're on the last arg and the method expects an array
                        if (i == classes.length - 1 && methodArgs[i].isArray()) {
                            // check to see if the last arg is convertible
                            // to the array's component type
                            return isConvertible(methodArgs[i], classes[i], true);
                        }
                        return false;
                    }
                }
                return true;
            }
            // more arguments given than the method accepts; check for varargs
            if (methodArgs.length > 0) {
                // check that the last methodArg is an array
                Class<?> lastarg = methodArgs[methodArgs.length - 1];
                if (!lastarg.isArray()) {
                    return false;
                }

                // check that they all match up to the last method arg
                for (int i = 0; i < methodArgs.length - 1; ++i) {
                    if (!isConvertible(methodArgs[i], classes[i], false)) {
                        return false;
                    }
                }

                // check that all remaining arguments are convertible to the vararg type
                Class<?> vararg = lastarg.getComponentType();
                for (int i = methodArgs.length - 1; i < classes.length; ++i) {
                    if (!isConvertible(vararg, classes[i], false)) {
                        return false;
                    }
                }
                return true;
            }
            // no match
            return false;
        }

        /**
         * @see #isInvocationConvertible(Class, Class, boolean)
         * @param formal         the formal parameter type to which the actual
         *                       parameter type should be convertible
         * @param actual         the actual parameter type.
         * @param possibleVarArg whether or not we're dealing with the last parameter
         *                       in the method declaration
         * @return see isMethodInvocationConvertible.
         */
        private boolean isConvertible(Class<?> formal, Class<?> actual,
                boolean possibleVarArg) {
            // if we see Void.class, the argument was null
            return isInvocationConvertible(formal, actual.equals(Void.class) ? null : actual, possibleVarArg);
        }

        /**
         * @see #isStrictInvocationConvertible(Class, Class, boolean)
         * @param formal         the formal parameter type to which the actual
         *                       parameter type should be convertible
         * @param actual         the actual parameter type.
         * @param possibleVarArg whether or not we're dealing with the last parameter
         *                       in the method declaration
         * @return see isStrictMethodInvocationConvertible.
         */
        private boolean isStrictConvertible(Class<?> formal, Class<?> actual,
                boolean possibleVarArg) {
            // if we see Void.class, the argument was null
            return isStrictInvocationConvertible(formal, actual.equals(Void.class) ? null : actual, possibleVarArg);
        }
    }
    
    /**
     * The parameter matching service for methods.
     */
    private static final Parameters<Method> METHODS = new Parameters<Method>() {
        @Override
        protected Class<?>[] getParameterTypes(Method app) {
            return app.getParameterTypes();
        }
    };
    
    /**
     * The parameter matching service for constructors.
     */
    private static final Parameters<Constructor<?>> CONSTRUCTORS = new Parameters<Constructor<?>>() {
        @Override
        protected Class<?>[] getParameterTypes(Constructor<?> app) {
            return app.getParameterTypes();
        }
    };
}
