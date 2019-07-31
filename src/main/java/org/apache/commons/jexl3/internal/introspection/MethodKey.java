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
package org.apache.commons.jexl3.internal.introspection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 * Roughly 3x faster than string key to access the map and uses less memory.
 */
public final class MethodKey {
    /** The initial size of the primitive conversion map. */
    private static final int PRIMITIVE_SIZE = 11;
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
     * @param args    the intended method arguments
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
     * Creates a key from a constructor.
     * @param aCtor the constructor to generate the key from.
     */
    MethodKey(Constructor<?> aCtor) {
        this(aCtor.getDeclaringClass().getName(), aCtor.getParameterTypes());
    }

    /**
     * Creates a key from a method name and a set of parameters.
     * @param aMethod the method to generate the key from
     * @param args    the intended method parameters
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
                Class<?> parm = primitiveClass(args[p]);
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

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodKey) {
            MethodKey key = (MethodKey) obj;
            return method.equals(key.method) && Arrays.equals(params, key.params);
        }
        return false;
    }

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
     * Checks whether a method accepts a variable number of arguments.
     * <p>May be due to a subtle bug in some JVMs, if a varargs method is an override, depending on (may be) the
     * class introspection order, the isVarargs flag on the method itself will be false.
     * To circumvent the potential problem, fetch the method with the same signature from the super-classes,
     * - which will be different if override  -and get the varargs flag from it.
     * @param method the method to check for varargs
     * @return true if declared varargs, false otherwise
     */
    public static boolean isVarArgs(final Method method) {
        if (method == null) {
            return false;
        }
        if (method.isVarArgs()) {
            return true;
        }
        // before climbing up the hierarchy, verify that the last parameter is an array
        final Class<?>[] ptypes = method.getParameterTypes();
        if (ptypes.length == 0 || ptypes[ptypes.length - 1].getComponentType() == null) {
            return false;
        }
        final String mname = method.getName();
        // if this is an override, was it actually declared as varargs?
        Class<?> clazz = method.getDeclaringClass();
        do {
            try {
                Method m = clazz.getMethod(mname, ptypes);
                if (m.isVarArgs()) {
                    return true;
                }
            } catch (NoSuchMethodException xignore) {
                // this should not happen...
            }
            clazz = clazz.getSuperclass();
        } while(clazz != null);
        return false;
    }

    /**
     * Gets the most specific method that is applicable to the parameters of this key.
     * @param methods a list of methods.
     * @return the most specific method.
     * @throws MethodKey.AmbiguousException if there is more than one.
     */
    public Method getMostSpecificMethod(Method[] methods) {
        return METHODS.getMostSpecific(this, methods);
    }

    /**
     * Gets the most specific constructor that is applicable to the parameters of this key.
     * @param methods a list of constructors.
     * @return the most specific constructor.
     * @throws MethodKey.AmbiguousException if there is more than one.
     */
    public Constructor<?> getMostSpecificConstructor(Constructor<?>[] methods) {
        return CONSTRUCTORS.getMostSpecific(this, methods);
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
    public static boolean isInvocationConvertible(Class<?> formal, Class<?> actual, boolean possibleVarArg) {
        return isInvocationConvertible(formal, actual, false, possibleVarArg);
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
    public static boolean isStrictInvocationConvertible(Class<?> formal, Class<?> actual, boolean possibleVarArg) {
        return isInvocationConvertible(formal, actual, true, possibleVarArg);
    }

    /** Converts a primitive type to its corresponding class.
     * <p>
     * If the argument type is primitive then we want to convert our
     * primitive type signature to the corresponding Object type so
     * introspection for methods with primitive types will work
     * correctly.
     * </p>
     * @param parm a may-be primitive type class
     * @return the equivalent object class
     */
    static Class<?> primitiveClass(Class<?> parm) {
        // it was marginally faster to get from the map than call isPrimitive...
        //if (!parm.isPrimitive()) return parm;
        Class<?>[] prim = CONVERTIBLES.get(parm);
        return prim == null ? parm : prim[0];
    }

    /**
     * Helper to build class arrays.
     * @param args the classes
     * @return the array
     */
    private static Class<?>[] asArray(Class<?>... args) {
        return args;
    }

    /**
     * Maps from primitive types to invocation compatible classes.
     * <p>Considering the key as a parameter type, the value is the list of argument classes that are invocation
     *   compatible with the parameter. Example is Long is invocation convertible to long.
     */
    private static final Map<Class<?>, Class<?>[]> CONVERTIBLES;
    static {
        CONVERTIBLES = new HashMap<Class<?>, Class<?>[]>(PRIMITIVE_SIZE);
        CONVERTIBLES.put(Boolean.TYPE,
                asArray(Boolean.class));
        CONVERTIBLES.put(Character.TYPE,
                asArray(Character.class));
        CONVERTIBLES.put(Byte.TYPE,
                asArray(Byte.class));
        CONVERTIBLES.put(Short.TYPE,
                asArray(Short.class, Byte.class));
        CONVERTIBLES.put(Integer.TYPE,
                asArray(Integer.class, Short.class, Byte.class));
        CONVERTIBLES.put(Long.TYPE,
                asArray(Long.class, Integer.class, Short.class, Byte.class));
        CONVERTIBLES.put(Float.TYPE,
                asArray(Float.class, Long.class, Integer.class, Short.class, Byte.class));
        CONVERTIBLES.put(Double.TYPE,
            asArray(Double.class, Float.class, Long.class, Integer.class, Short.class, Byte.class));
    }

    /**
     * Maps from primitive types to invocation compatible primitive types.
     * <p>Considering the key as a parameter type, the value is the list of argument types that are invocation
     * compatible with the parameter. Example is 'int' is invocation convertible to 'long'.
     */
    private static final Map<Class<?>, Class<?>[]> STRICT_CONVERTIBLES;
    static {
        STRICT_CONVERTIBLES = new HashMap<Class<?>, Class<?>[]>(PRIMITIVE_SIZE);
        STRICT_CONVERTIBLES.put(Short.TYPE,
                asArray(Byte.TYPE));
        STRICT_CONVERTIBLES.put(Integer.TYPE,
                asArray(Short.TYPE, Byte.TYPE));
        STRICT_CONVERTIBLES.put(Long.TYPE,
                asArray(Integer.TYPE, Short.TYPE, Byte.TYPE));
        STRICT_CONVERTIBLES.put(Float.TYPE,
                asArray(Long.TYPE, Integer.TYPE, Short.TYPE, Byte.TYPE));
        STRICT_CONVERTIBLES.put(Double.TYPE,
                asArray(Float.TYPE, Long.TYPE, Integer.TYPE, Short.TYPE, Byte.TYPE));
    }

    /**
     * Determines parameter-argument invocation compatibility.
     *
     * @param formal         the formal parameter type
     * @param actual         the argument type
     * @param strict         whether the check is strict or not
     * @param possibleVarArg whether or not we're dealing with the last parameter in the method declaration
     * @return true if compatible, false otherwise
     */
    private static boolean isInvocationConvertible(
            Class<?> formal, Class<?> actual, boolean strict, boolean possibleVarArg) {
        /* if it's a null, it means the arg was null */
        if (actual == null && !formal.isPrimitive()) {
            return true;
        }
        /* system asssignable, both sides must be array or not */
        if (actual != null && formal.isAssignableFrom(actual) && actual.isArray() == formal.isArray()) {
            return true;
        }
        /** catch all... */
        if (!strict && formal == Object.class) {
            return true;
        }
        /* Primitive conversion check. */
        if (formal.isPrimitive()) {
            Class<?>[] clist = strict ? STRICT_CONVERTIBLES.get(formal) : CONVERTIBLES.get(formal);
            if (clist != null) {
                for (int c = 0; c < clist.length; ++c) {
                    if (actual == clist[c]) {
                        return true;
                    }
                }
            }
            return false;
        }
        /* Check for vararg conversion. */
        if (possibleVarArg && formal.isArray()) {
            if (actual != null && actual.isArray()) {
                actual = actual.getComponentType();
            }
            return isInvocationConvertible(formal.getComponentType(), actual, strict, false);
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
     * we run across ambiguous overloading. Caught
     * by the introspector.
     */
    public static class AmbiguousException extends RuntimeException {
        /** Version Id for serializable. */
        private static final long serialVersionUID = -201801091655L;
        /** Whether this exception should be considered severe. */
        private final boolean severe;

        /**
         * A severe or not ambiguous exception.
         * @param flag logging flag
         */
        AmbiguousException(boolean flag) {
            this.severe = flag;
        }

        /**
         * Whether this exception is considered severe or benign.
         * <p>Note that this is meant in the context of an ambiguous exception; benign cases can only be triggered
         * by null arguments often related to runtime problems (not simply on overload signatures).
         * @return true if severe, false if benign.
         */
        public boolean isSevere() {
            return severe;
        }
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

        /**
         * Whether a constructor or method handles varargs.
         * @param app the constructor or method
         * @return true if varargs, false otherwise
         */
        protected abstract boolean isVarArgs(T app);

        // CSOFF: RedundantThrows
        /**
         * Gets the most specific method that is applicable to actual argument types.<p>
         * Attempts to find the most specific applicable method using the
         * algorithm described in the JLS section 15.12.2 (with the exception that it can't
         * distinguish a primitive type argument from an object type argument, since in reflection
         * primitive type arguments are represented by their object counterparts, so for an argument of
         * type (say) java.lang.Integer, it will not be able to decide between a method that takes int and a
         * method that takes java.lang.Integer as a parameter.
         * </p>
         * <p>
         * This turns out to be a relatively rare case where this is needed - however, functionality
         * like this is needed.
         * </p>
         *
         * @param key a method key, esp its parameters
         * @param methods a list of methods
         * @return the most specific method.
         * @throws MethodKey.AmbiguousException if there is more than one.
         */
        private T getMostSpecific(MethodKey key, T[] methods) {
            final Class<?>[] args = key.params;
            LinkedList<T> applicables = getApplicables(methods, args);
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
            for (T app : applicables) {
                final Class<?>[] parms = getParameterTypes(app);
                boolean lessSpecific = false;
                Iterator<T> maximal = maximals.iterator();
                while(!lessSpecific && maximal.hasNext()) {
                    T max = maximal.next();
                    switch (moreSpecific(args, parms, getParameterTypes(max))) {
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
                        default:
                            // nothing do do
                    }
                }

                if (!lessSpecific) {
                    maximals.addLast(app);
                }
            }
            // if we have more than one maximally specific method, this call is ambiguous...
            if (maximals.size() > 1) {
                throw ambiguousException(args, applicables);
            }
            return maximals.getFirst();
        } // CSON: RedundantThrows

        /**
         * Creates an ambiguous exception.
         * <p>
         * This method computes the severity of the ambiguity. The only <em>non-severe</em> case is when there is
         * at least one null argument and at most one applicable method or constructor has a corresponding 'Object'
         * parameter.
         * We thus consider that ambiguity is benign in presence of null arguments but in the case where
         * the corresponding parameter is of type Object in more than one applicable overloads.
         * <p>
         * Rephrasing:
         * <ul>
         *  <li>If all arguments are valid instances - no null argument -, ambiguity is severe.</li>
         *  <li>If there is at least one null argument, the ambiguity is severe if more than one method has a
         *  corresponding parameter of class 'Object'.</li>
         * </ul>
         *
         * @param classes the argument args
         * @param applicables the list of applicable methods or constructors
         * @return an ambiguous exception
         */
        private AmbiguousException ambiguousException (Class<?>[] classes, List<T> applicables) {
            boolean severe = false;
            int instanceArgCount = 0; // count the number of valid instances, aka not null
            for(int c = 0; c < classes.length; ++c) {
                Class<?> argClazz = classes[c];
                if (Void.class.equals(argClazz)) {
                    // count the number of methods for which the current arg maps to an Object parameter
                    int objectParmCount = 0;
                    for (T app : applicables) {
                        Class<?>[] parmClasses = getParameterTypes(app);
                        Class<?> parmClass =  parmClasses[c];
                        if (Object.class.equals(parmClass)) {
                            if (objectParmCount++ == 2) {
                                severe = true;
                                break;
                            }
                        }
                    }
                } else {
                    instanceArgCount += 1;
                }
            }
            return new AmbiguousException(severe || instanceArgCount == classes.length);
        }

        /**
         * Determines which method signature (represented by a class array) is more
         * specific. This defines a partial ordering on the method signatures.
         *
         * @param a the arguments signature
         * @param c1 first method signature to compare
         * @param c2 second method signature to compare
         * @return MORE_SPECIFIC if c1 is more specific than c2, LESS_SPECIFIC if
         *         c1 is less specific than c2, INCOMPARABLE if they are incomparable.
         */
         private int moreSpecific(final Class<?>[] a, final Class<?>[] c1, final Class<?>[] c2) {
            // compare lengths to handle comparisons where the size of the arrays
            // doesn't match, but the methods are both applicable due to the fact
            // that one is a varargs method
            if (c1.length > a.length) {
                return LESS_SPECIFIC;
            }
            if (c2.length > a.length) {
                return MORE_SPECIFIC;
            }
            if (c1.length > c2.length) {
                return MORE_SPECIFIC;
            }
            if (c2.length > c1.length) {
                return LESS_SPECIFIC;
            }
            // same length, keep ultimate param offset for vararg checks
            final int length = c1.length;
            final int ultimate = c1.length - 1;

             // ok, move on and compare those of equal lengths
             for (int i = 0; i < length; ++i) {
                 if (c1[i] != c2[i]) {
                     boolean last = (i == ultimate);
                     // argument is null, prefer an Object param
                     if (a[i] == Void.class) {
                         if (c1[i] == Object.class && c2[i] != Object.class) {
                             return MORE_SPECIFIC;
                         }
                         if (c1[i] != Object.class && c2[i] == Object.class) {
                             return LESS_SPECIFIC;
                         }
                     }
                     // prefer primitive on non null arg, non primitive otherwise
                     boolean c1s = isPrimitive(c1[i], last);
                     boolean c2s = isPrimitive(c2[i], last);
                     if (c1s != c2s) {
                        return (c1s == (a[i] != Void.class))? MORE_SPECIFIC : LESS_SPECIFIC;
                     }
                     // if c2 can be converted to c1 but not the opposite,
                     // c1 is more specific than c2
                     c1s = isStrictConvertible(c2[i], c1[i], last);
                     c2s = isStrictConvertible(c1[i], c2[i], last);
                     if (c1s != c2s) {
                         return c1s ? MORE_SPECIFIC : LESS_SPECIFIC;
                     }
                 }
             }
            // Incomparable due to non-related arguments (i.e.foo(Runnable) vs. foo(Serializable))
            return INCOMPARABLE;
        }

        /**
         * Checks whether a parameter class is a primitive.
         * @param c              the parameter class
         * @param possibleVarArg true if this is the last parameter which can be a primitive array (vararg call)
         * @return true if primitive, false otherwise
         */
        private boolean isPrimitive(Class<?> c, boolean possibleVarArg) {
            if (c != null) {
                if (c.isPrimitive()) {
                    return true;
                } else if (possibleVarArg) {
                    Class<?> t = c.getComponentType();
                    return t != null && t.isPrimitive();
                }
            }
            return false;
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
        private LinkedList<T> getApplicables(T[] methods, Class<?>[] classes) {
            LinkedList<T> list = new LinkedList<T>();
            for (T method : methods) {
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
         * @param actuals arguments signature for method
         * @return true if method is applicable to arguments
         */
        private boolean isApplicable(T method, Class<?>[] actuals) {
            Class<?>[] formals = getParameterTypes(method);
            // if same number or args or
            // there's just one more methodArg than class arg
            // and the last methodArg is an array, then treat it as a vararg
            if (formals.length == actuals.length) {
                // this will properly match when the last methodArg
                // is an array/varargs and the last class is the type of array
                // (e.g. String when the method is expecting String...)
                for (int i = 0; i < actuals.length; ++i) {
                    if (!isConvertible(formals[i], actuals[i], false)) {
                        // if we're on the last arg and the method expects an array
                        if (i == actuals.length - 1 && formals[i].isArray()) {
                            // check to see if the last arg is convertible
                            // to the array's component type
                            return isConvertible(formals[i], actuals[i], true);
                        }
                        return false;
                    }
                }
                return true;
            }

            // number of formal and actual differ, method must be vararg
            if (!isVarArgs(method)) {
                return false;
            }

            // less arguments than method parameters: vararg is null
            if (formals.length > actuals.length) {
                // only one parameter, the last (ie vararg) can be missing
                if (formals.length - actuals.length > 1) {
                    return false;
                }
                // check that all present args match up to the method parms
                for (int i = 0; i < actuals.length; ++i) {
                    if (!isConvertible(formals[i], actuals[i], false)) {
                        return false;
                    }
                }
                return true;
            }

            // more arguments given than the method accepts; check for varargs
            if (formals.length > 0 && actuals.length > 0) {
                // check that they all match up to the last method arg
                for (int i = 0; i < formals.length - 1; ++i) {
                    if (!isConvertible(formals[i], actuals[i], false)) {
                        return false;
                    }
                }
                // check that all remaining arguments are convertible to the vararg type
                // (last parm is an array since method is vararg)
                Class<?> vararg = formals[formals.length - 1].getComponentType();
                for (int i = formals.length - 1; i < actuals.length; ++i) {
                    if (!isConvertible(vararg, actuals[i], false)) {
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
        private boolean isConvertible(Class<?> formal, Class<?> actual, boolean possibleVarArg) {
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

        @Override
        public boolean isVarArgs(Method app) {
            return MethodKey.isVarArgs(app);
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

        @Override
        public boolean isVarArgs(Constructor<?> app) {
            return app.isVarArgs();
        }

    };
}
