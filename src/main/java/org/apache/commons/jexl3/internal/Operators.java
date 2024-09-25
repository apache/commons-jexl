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
package org.apache.commons.jexl3.internal;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlCache;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.internal.introspection.MethodExecutor;
import org.apache.commons.jexl3.internal.introspection.MethodKey;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Helper class to deal with operator overloading and specifics.
 * @since 3.0
 */
public final class Operators implements JexlArithmetic.Uberspect {
    /** The uberspect. */
    protected final Uberspect uberspect;
    /** The arithmetic instance being analyzed. */
    protected final JexlArithmetic arithmetic;
    /** The set of overloaded operators. */
    protected final Set<JexlOperator> overloads;

    /**
     * The comparison operators.
     * <p>Used to determine if a compare method overload might be used.</p>
     */
    private static final Set<JexlOperator> CMP_OPS =
            EnumSet.of(JexlOperator.GT, JexlOperator.LT, JexlOperator.EQ, JexlOperator.GTE, JexlOperator.LTE);

    /**
     * The postfix operators.
     * <p>Used to determine the returned value in assignment.</p>
     */
    private static final Set<JexlOperator> POSTFIX_OPS =
            EnumSet.of(JexlOperator.GET_AND_INCREMENT, JexlOperator.GET_AND_DECREMENT);

    /**
     * Creates an instance.
     * @param theUberspect the uberspect instance
     * @param theArithmetic the arithmetic instance
     * @param theOverloads  the overloaded operators
     */
    public Operators(final Uberspect theUberspect, final JexlArithmetic theArithmetic, final Set<JexlOperator> theOverloads) {
        this.uberspect = theUberspect;
        this.arithmetic = theArithmetic;
        this.overloads = theOverloads;
    }

    @Override
    public JexlMethod getOperator(final JexlOperator operator, final Object... args) {
        return overloads.contains(operator) && args != null
                ? uberspectOperator(arithmetic, operator, args)
                : null;
    }

    @Override
    public boolean overloads(final JexlOperator operator) {
        return overloads.contains(operator);
    }

    /**
     * Tidy arguments based on operator arity.
     * <p>The interpreter may add a null to the arguments of operator expecting only one parameter.</p>
     * @param operator the operator
     * @param args the arguments (as seen by the interpreter)
     * @return the tidied arguments
     */
    private Object[] arguments(final JexlOperator operator, final Object...args) {
        return operator.getArity() == 1 && args.length > 1 ? new Object[]{args[0]} : args;
    }

    /**
     * Attempts finding a method in left and eventually narrowing right.
     * @param methodName the method name
     * @param right the left argument in the operator
     * @param left the right argument in the operator
     * @return a boolean is call was possible, null otherwise
     * @throws Exception if invocation fails
     */
    private Boolean booleanDuckCall(final String methodName, final Object left, final Object right) throws Exception {
        JexlMethod vm = uberspect.getMethod(left, methodName, right);
        if (returnsBoolean(vm)) {
            return (Boolean) vm.invoke(left, right);
        }
        final Object[] argv = { right };
        if (arithmetic.narrowArguments(argv)) {
            vm = uberspect.getMethod(left, methodName, argv);
            if (returnsBoolean(vm)) {
                return (Boolean) vm.invoke(left, argv);
            }
        }
        return null;
    }

    /**
     * Throw a NPE if operator is strict and one of the arguments is null.
     * @param arithmetic the JEXL arithmetic instance
     * @param operator the operator to check
     * @param args the operands
     * @throws JexlArithmetic.NullOperand if operator is strict and an operand is null
     */
     private void controlNullOperands(final JexlArithmetic arithmetic, final JexlOperator operator, final Object...args) {
        for (final Object arg : args) {
            // only check operator if necessary
            if (arg == null) {
                // check operator only once if it is not strict
                if (arithmetic.isStrict(operator)) {
                    throw new JexlArithmetic.NullOperand();
                }
                break;
            }
        }
    }

    /**
     * Triggered when an operator fails.
     * @param ref     the node where the error originated from
     * @param operator the operator symbol
     * @param cause    the cause of error (if any)
     * @return throws JexlException if strict and not silent, null otherwise
     */
    private Object operatorError(final JexlCache.Reference ref, final JexlOperator operator, final Throwable cause) {
        JexlNode node = (ref instanceof JexlNode) ? (JexlNode) ref : null;
        Engine engine = (Engine) JexlEngine.getThreadEngine();
        if (engine == null || engine.isStrict()) {
            throw new JexlException.Operator(node, operator.getOperatorSymbol(), cause);
        }
        if (engine.logger.isDebugEnabled()) {
            engine.logger.debug(JexlException.operatorError(node, operator.getOperatorSymbol()), cause);
        }
        return null;
    }

    /**
     * Seeks an implementation of an operator method in an arithmetic instance.
     * <p>Method must <em><>not/em belong to JexlArithmetic</p>
     * @param arithmetic the arithmetic instance
     * @param operator the operator
     * @param args the arguments
     * @return a JexlMethod instance or null
     */
    private JexlMethod uberspectOperator(final JexlArithmetic arithmetic,
                                       final JexlOperator operator,
                                       final Object... args) {
        final JexlMethod me = uberspect.getMethod(arithmetic, operator.getMethodName(), args);
        if (!(me instanceof MethodExecutor) ||
            !JexlArithmetic.class.equals(((MethodExecutor) me).getMethod().getDeclaringClass())) {
            return me;
        }
        return null;
    }

    /**
     * Checks whether a method returns a boolean or a Boolean.
     * @param vm the JexlMethod (may be null)
     * @return true of false
     */
    private boolean returnsBoolean(final JexlMethod vm) {
        if (vm != null) {
            final Class<?> rc = vm.getReturnType();
            return Boolean.TYPE.equals(rc) || Boolean.class.equals(rc);
        }
        return false;
    }

    /**
     * Checks whether a method returns an int or an Integer.
     * @param vm the JexlMethod (may be null)
     * @return true of false
     */
    private boolean returnsInteger(final JexlMethod vm) {
        if (vm != null) {
            final Class<?> rc = vm.getReturnType();
            return Integer.TYPE.equals(rc) || Integer.class.equals(rc);
        }
        return false;
    }

    /**
     * Check for emptiness of various types: Collection, Array, Map, String, and anything that has a boolean isEmpty()
     * method.
     * <p>Note that the result may not be a boolean.
     *
     * @param node   the node holding the object
     * @param object the object to check the emptiness of
     * @return the evaluation result
     */
    Object empty(final JexlCache.Reference node, final Object object) {
        if (object == null) {
            return true;
        }
        Object result = overloads(JexlOperator.EMPTY)
                ? tryOverload(node, JexlOperator.EMPTY, object)
                : JexlEngine.TRY_FAILED;
        if (result == JexlEngine.TRY_FAILED) {
            result = arithmetic.isEmpty(object, null);
            if (result == null) {
                result = false;
                // check if there is an isEmpty method on the object that returns a
                // boolean and if so, just use it
                final JexlMethod vm = uberspect.getMethod(object, "isEmpty", InterpreterBase.EMPTY_PARAMS);
                if (returnsBoolean(vm)) {
                    try {
                        result = vm.invoke(object, InterpreterBase.EMPTY_PARAMS);
                    } catch (final Exception xany) {
                        return operatorError(node, JexlOperator.EMPTY, xany);
                    }
                }
            }
        }
        return !(result instanceof Boolean) || (Boolean) result;
    }

    /**
     * Calculate the {@code size} of various types:
     * Collection, Array, Map, String, and anything that has an int size() method.
     * <p>Note that the result may not be an integer.
     *
     * @param node   the node that gave the value to size
     * @param object the object to get the size of
     * @return the evaluation result
     */
    Object size(final JexlCache.Reference node, final Object object) {
        if (object == null) {
            return 0;
        }
        Object result = overloads(JexlOperator.SIZE)
                ? tryOverload(node, JexlOperator.SIZE, object)
                : JexlEngine.TRY_FAILED;
        if (result == JexlEngine.TRY_FAILED) {
            result = arithmetic.size(object, null);
            if (result == null) {
                // check if there is a size method on the object that returns an
                // integer and if so, just use it
                final JexlMethod vm = uberspect.getMethod(object, "size", InterpreterBase.EMPTY_PARAMS);
                if (returnsInteger(vm)) {
                    try {
                        result = vm.invoke(object, InterpreterBase.EMPTY_PARAMS);
                    } catch (final Exception xany) {
                        operatorError(node, JexlOperator.SIZE, xany);
                    }
                }
            }
        }
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    /**
     * The 'match'/'in' operator implementation.
     * <p>
     * Note that 'x in y' or 'x matches y' means 'y contains x' ;
     * the JEXL operator arguments order syntax is the reverse of this method call.
     * </p>
     * @param node  the node
     * @param operator    the calling operator, =~ or !~
     * @param right the left operand
     * @param left  the right operand
     * @return true if left matches right, false otherwise
     */
    boolean contains(final JexlCache.Reference node, final JexlOperator operator, final Object left, final Object right) {
        final boolean contained;
        try {
            // try operator overload
            final Object result = overloads(JexlOperator.CONTAINS)
                    ? tryOverload(node, JexlOperator.CONTAINS, left, right)
                    : null;
            if (result instanceof Boolean) {
                contained = (Boolean) result;
            } else {
                // use arithmetic / pattern matching ?
                final Boolean matched = arithmetic.contains(left, right);
                if (matched != null) {
                    contained = matched;
                } else {
                    // try a left.contains(right) method
                    final Boolean duck = booleanDuckCall("contains", left, right);
                    if (duck != null) {
                        contained = duck;
                    } else {
                        // defaults to equal
                        contained = arithmetic.equals(left, right);
                    }
                }
            }
            // not-contains is !contains
            return (JexlOperator.CONTAINS == operator) == contained;
        } catch (final Exception xrt) {
            operatorError(node, operator, xrt);
            return false;
        }
    }

    /**
     * The 'startsWith' operator implementation.
     * @param node     the node
     * @param operator the calling operator, $= or $!
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left starts with right, false otherwise
     */
    boolean startsWith(final JexlCache.Reference node, final JexlOperator operator, final Object left, final Object right) {
        final boolean starts;
        try {
            // try operator overload
            final Object result = overloads(JexlOperator.STARTSWITH)
                    ? tryOverload(node, JexlOperator.STARTSWITH, left, right)
                    : null;
            if (result instanceof Boolean) {
                starts = (Boolean) result;
            } else {
                // use arithmetic / pattern matching ?
                final Boolean matched = arithmetic.startsWith(left, right);
                if (matched != null) {
                    starts = matched;
                } else {
                    // try a left.startsWith(right) method
                    final Boolean duck = booleanDuckCall("startsWith", left, right);
                    if (duck != null) {
                        starts = duck;
                    } else {
                        // defaults to equal
                        starts = arithmetic.equals(left, right);
                    }
                }
            }
            // not-startswith is !starts-with
            return (JexlOperator.STARTSWITH == operator) == starts;
        } catch (final Exception xrt) {
            operatorError(node, operator, xrt);
            return false;
        }
    }

    /**
     * The 'endsWith' operator implementation.
     * @param node     the node
     * @param operator the calling operator, ^= or ^!
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left ends with right, false otherwise
     */
    boolean endsWith(final JexlCache.Reference node, final JexlOperator operator, final Object left, final Object right) {
        try {
            final boolean ends;
            // try operator overload
            final Object result = overloads(JexlOperator.ENDSWITH)
                ? tryOverload(node, JexlOperator.ENDSWITH, left, right)
                : null;
            if (result instanceof Boolean) {
                ends = (Boolean) result;
            } else {
                // use arithmetic / pattern matching ?
                final Boolean matched = arithmetic.endsWith(left, right);
                if (matched != null) {
                    ends = matched;
                } else {
                    // try a left.endsWith(right) method
                    final Boolean duck = booleanDuckCall("endsWith", left, right);
                    if (duck != null) {
                        ends = duck;
                    } else {
                        // defaults to equal
                        ends = arithmetic.equals(left, right);
                    }
                }
            }
            // not-endswith is !ends-with
            return (JexlOperator.ENDSWITH == operator) == ends;
        } catch (final Exception xrt) {
            operatorError(node, operator, xrt);
            return false;
        }
    }

    /**
     * Evaluates an assign operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate.
     * If an overloads returns a value not-equal to TRY_FAILED, it means the side-effect is complete.
     * Otherwise, {@code a += b <=> a = a + b}
     * </p>
     * @param node     the syntactic node
     * @param operator the operator
     * @param args     the arguments, the first one being the target of assignment
     * @return JexlEngine.TRY_FAILED if no operation was performed,
     *         the value to use as the side effect argument otherwise
     */
    Object tryAssignOverload(final JexlNode node,
                               final JexlOperator operator,
                               final Consumer<Object> assignFun,
                               final Object...args) {
        if (args.length < operator.getArity()) {
            return JexlEngine.TRY_FAILED;
        }
        Object result;
        try {
        // try to call overload with side effect; the object is modified
        if (overloads(operator)) {
            result = tryOverload(node, operator, arguments(operator, args));
            if (result != JexlEngine.TRY_FAILED) {
                return result; // 1
            }
        }
        // try to call base overload (ie + for +=)
        final JexlOperator base = operator.getBaseOperator();
        if (base != null && overloads(base)) {
            result = tryOverload(node, base, arguments(base, args));
            if (result != JexlEngine.TRY_FAILED) {
                assignFun.accept(result);
                return POSTFIX_OPS.contains(operator) ? args[0] : result; // 2
            }
        }
        // default implementation for self-* operators
        switch (operator) {
            case SELF_ADD:
                result = arithmetic.add(args[0], args[1]);
                break;
            case SELF_SUBTRACT:
                result = arithmetic.subtract(args[0], args[1]);
                break;
            case SELF_MULTIPLY:
                result = arithmetic.multiply(args[0], args[1]);
                break;
            case SELF_DIVIDE:
                result = arithmetic.divide(args[0], args[1]);
                break;
            case SELF_MOD:
                result = arithmetic.mod(args[0], args[1]);
                break;
            case SELF_AND:
                result = arithmetic.and(args[0], args[1]);
                break;
            case SELF_OR:
                result = arithmetic.or(args[0], args[1]);
                break;
            case SELF_XOR:
                result = arithmetic.xor(args[0], args[1]);
                break;
            case SELF_SHIFTLEFT:
                result = arithmetic.shiftLeft(args[0], args[1]);
                break;
            case SELF_SHIFTRIGHT:
                result = arithmetic.shiftRight(args[0], args[1]);
                break;
            case SELF_SHIFTRIGHTU:
                result = arithmetic.shiftRightUnsigned(args[0], args[1]);
                break;
            case INCREMENT_AND_GET:
                result = arithmetic.increment(args[0]);
                break;
            case DECREMENT_AND_GET:
                result = arithmetic.decrement(args[0]);
                break;
            case GET_AND_INCREMENT:
                result = args[0];
                assignFun.accept(arithmetic.increment(result));
                return result; // 3
            case GET_AND_DECREMENT: {
                result = args[0];
                assignFun.accept(arithmetic.decrement(result));
                return result; // 4
            }
            default:
                // unexpected, new operator added?
                throw new UnsupportedOperationException(operator.getOperatorSymbol());
            }
            assignFun.accept(result);
            return result; // 5
        } catch (final Exception xany) {
            operatorError(node, operator, xany);
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Attempts to call an operator.
     * <p>
     *     This performs the null argument control against the strictness of the operator.
     * </p>
     * <p>
     * This takes care of finding and caching the operator method when appropriate.
     * </p>
     * @param node     the syntactic node
     * @param operator the operator
     * @param args     the arguments
     * @return the result of the operator evaluation or TRY_FAILED
     */
    Object tryOverload(final JexlCache.Reference node, final JexlOperator operator, final Object... args) {
        controlNullOperands(arithmetic, operator, args);
        Engine engine = (Engine) JexlEngine.getThreadEngine();
        try {
            return tryEval(engine == null || engine.cache != null ? node : null, operator, args);
        } catch (final Exception xany) {
            // ignore return if lenient, will return try_failed
            operatorError(node, operator, xany);
        }
        return JexlEngine.TRY_FAILED;
    }

    @Override
    public Object tryEval(JexlCache.Reference node, JexlOperator operator, Object...args) {
        if (node != null) {
            final Object cached = node.getCache();
            if (cached instanceof JexlMethod) {
                // we found a method on previous call; try and reuse it (*1)
                final JexlMethod me = (JexlMethod) cached;
                final Object eval = me.tryInvoke(operator.getMethodName(), arithmetic, args);
                if (!me.tryFailed(eval)) {
                    return eval;
                }
            } else if (cached instanceof MethodKey) {
                // check for a fail-fast, we tried to find an overload before but could not (*2)
                final MethodKey cachedKey = (MethodKey) cached;
                final MethodKey key = new MethodKey(operator.getMethodName(), args);
                if (key.equals(cachedKey)) {
                    return JexlEngine.TRY_FAILED;
                }
            }
        }
        // trying to find an operator overload
        JexlMethod vm = getOperator(operator, args);
        // no direct overload, any special case ?
        if (vm == null) {
            vm = getAlternateOverload(operator, args);
        }
        // *1: found a method, try it and cache it if successful
        if (vm != null) {
            final Object result = vm.tryInvoke(operator.getMethodName(), arithmetic, args);
            if (node != null && !vm.tryFailed(result)) {
                node.setCache(vm);
            }
            return result;
        }
        if (node != null) {
            // *2: could not find an overload for this operator and arguments, keep track of the fail
            MethodKey key = new MethodKey(operator.getMethodName(), args);
            node.setCache(key);
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Special handling of overloads where another attempt at finding a method may be attempted.
     * <p>As of 3.4.1, only the comparison operators attempting to use compare() are handled.</p>
     * @param operator the operator
     * @param args the arguments
     * @return an instance or null
     */
    private JexlMethod getAlternateOverload(final JexlOperator operator, final Object... args) {
        // comparison operators may use the compare overload in derived arithmetic
        if (CMP_OPS.contains(operator) && args.length == 2) {
            JexlMethod cmp = getOperator(JexlOperator.COMPARE, args);
            if (cmp != null) {
                return new Operators.CompareMethod(operator, cmp);
            }
            cmp = getOperator(JexlOperator.COMPARE, args[1], args[0]);
            if (cmp != null) {
                return new Operators.AntiCompareMethod(operator, cmp);
            }
        }
        return null;
    }

    /**
     * Delegates a comparison operator to a compare method.
     * The expected signature of the derived JexlArithmetic method is:
     * int compare(L left, R right);
     */
    private static class CompareMethod implements JexlMethod {
        protected final JexlOperator operator;
        protected final JexlMethod compare;

        CompareMethod(JexlOperator op, JexlMethod m) {
            operator = op;
            compare = m;
        }

        @Override
        public Class<?> getReturnType() {
            return Boolean.TYPE;
        }

        @Override
        public Object invoke(Object arithmetic, Object... params) throws Exception {
            return operate((int) compare.invoke(arithmetic, params));
        }

        @Override
        public boolean isCacheable() {
            return true;
        }

        @Override
        public boolean tryFailed(Object rval) {
            return rval == JexlEngine.TRY_FAILED;
        }

        @Override
        public Object tryInvoke(String name, Object arithmetic, Object... params) throws JexlException.TryFailed {
            Object cmp = compare.tryInvoke(JexlOperator.COMPARE.getMethodName(), arithmetic, params);
            if (cmp instanceof Integer) {
                return operate((int) cmp);
            }
            return JexlEngine.TRY_FAILED;
        }

        protected boolean operate(final int cmp) {
            switch(operator) {
                case EQ: return cmp == 0;
                case LT: return cmp < 0;
                case LTE: return cmp <= 0;
                case GT: return cmp > 0;
                case GTE: return cmp >= 0;
            }
            throw new ArithmeticException("unexpected operator " + operator);
        }
    }

    /**
     * The reverse compare swaps left and right arguments and changes the sign of the
     * comparison result.
     */
    private class AntiCompareMethod extends CompareMethod {

        AntiCompareMethod(JexlOperator op, JexlMethod m) {
            super(op, m);
        }

        @Override
        public Object tryInvoke(String name, Object arithmetic, Object... params) throws JexlException.TryFailed {
            Object cmp = compare.tryInvoke(JexlOperator.COMPARE.getMethodName(), arithmetic, params[1], params[0]);
            if (cmp instanceof Integer) {
                return operate(-(int) cmp);
            }
            return JexlEngine.TRY_FAILED;
        }

        @Override
        public Object invoke(Object arithmetic, Object... params) throws Exception {
            return operate(-(int) compare.invoke(arithmetic, params[1], params[0]));
        }
    }
}
