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
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.internal.introspection.MethodKey;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Helper class to deal with operator overloading and specifics.
 * @since 3.0
 */
public final class Operators {
    /**
     * Helper for postfix assignment operators.
     * @param operator the operator
     * @return true if operator is a postfix operator (x++, y--)
     */
    private static boolean isPostfix(final JexlOperator operator) {
        return operator == JexlOperator.GET_AND_INCREMENT || operator == JexlOperator.GET_AND_DECREMENT;
    }

    /**
     * The comparison operators.
     * <p>Used to determine if a compare method overload might be used.</p>
     */
    private static final Set<JexlOperator> CMP_OPS =
            EnumSet.of(JexlOperator.GT, JexlOperator.LT, JexlOperator.EQ, JexlOperator.GTE, JexlOperator.LTE);

    /** The owner. */
    private final InterpreterBase interpreter;

    /** The overloaded arithmetic operators. */
    private final JexlArithmetic.Uberspect operators;

    /**
     * Constructs a new instance.
     * @param owner the owning interpreter
     */
    Operators(final InterpreterBase owner) {
        final JexlArithmetic arithmetic = owner.arithmetic;
        final JexlUberspect uberspect = owner.uberspect;
        this.interpreter = owner;
        this.operators = uberspect.getArithmetic(arithmetic);
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
     * Attempts finding a method in left and eventually narrowing right.
     * @param methodName the method name
     * @param right the left argument in the operator
     * @param left the right argument in the operator
     * @return a boolean is call was possible, null otherwise
     * @throws Exception if invocation fails
     */
    private Boolean booleanDuckCall(final String methodName, final Object left, final Object right) throws Exception {
        final JexlUberspect uberspect = interpreter.uberspect;
        JexlMethod vm = uberspect.getMethod(left, methodName, right);
        if (returnsBoolean(vm)) {
            return (Boolean) vm.invoke(left, right);
        }
        final JexlArithmetic arithmetic = interpreter.arithmetic;
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
    Object empty(final JexlNode node, final Object object) {
        if (object == null) {
            return true;
        }
        Object result = operators.overloads(JexlOperator.EMPTY)
                ? tryOverload(node, JexlOperator.EMPTY, object)
                : JexlEngine.TRY_FAILED;
        if (result == JexlEngine.TRY_FAILED) {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            result = arithmetic.isEmpty(object, null);
            if (result == null) {
                final JexlUberspect uberspect = interpreter.uberspect;
                result = false;
                // check if there is an isEmpty method on the object that returns a
                // boolean and if so, just use it
                final JexlMethod vm = uberspect.getMethod(object, "isEmpty", InterpreterBase.EMPTY_PARAMS);
                if (returnsBoolean(vm)) {
                    try {
                        result = vm.invoke(object, InterpreterBase.EMPTY_PARAMS);
                    } catch (final Exception xany) {
                        return interpreter.operatorError(node, JexlOperator.EMPTY, xany);
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
    Object size(final JexlNode node, final Object object) {
        if (object == null) {
            return 0;
        }
        Object result = operators.overloads(JexlOperator.SIZE)
                ? tryOverload(node, JexlOperator.SIZE, object)
                : JexlEngine.TRY_FAILED;
        if (result == JexlEngine.TRY_FAILED) {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            result = arithmetic.size(object, null);
            if (result == null) {
                final JexlUberspect uberspect = interpreter.uberspect;
                // check if there is a size method on the object that returns an
                // integer and if so, just use it
                final JexlMethod vm = uberspect.getMethod(object, "size", InterpreterBase.EMPTY_PARAMS);
                if (returnsInteger(vm)) {
                    try {
                        result = vm.invoke(object, InterpreterBase.EMPTY_PARAMS);
                    } catch (final Exception xany) {
                        interpreter.operatorError(node, JexlOperator.SIZE, xany);
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
    boolean contains(final JexlNode node, final JexlOperator operator, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final boolean contained;
        try {
            // try operator overload
            final Object result = operators.overloads(JexlOperator.CONTAINS)
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
            return (JexlOperator.CONTAINS == operator) == contained;
        } catch (final Exception xrt) {
            interpreter.operatorError(node, operator, xrt);
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
    boolean startsWith(final JexlNode node, final JexlOperator operator, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final boolean starts;
        try {
            // try operator overload
            final Object result = operators.overloads(JexlOperator.STARTSWITH)
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
            return (JexlOperator.STARTSWITH == operator) == starts;
        } catch (final Exception xrt) {
            interpreter.operatorError(node, operator, xrt);
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
    boolean endsWith(final JexlNode node, final JexlOperator operator, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        try {
            final boolean ends;
            // try operator overload
            final Object result = operators.overloads(JexlOperator.ENDSWITH)
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
            return (JexlOperator.ENDSWITH == operator) == ends;
        } catch (final Exception xrt) {
            interpreter.operatorError(node, operator, xrt);
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
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        if (args.length < operator.getArity()) {
            return JexlEngine.TRY_FAILED;
        }
        Object result;
        try {
        // try to call overload with side effect; the object is modified
        if (operators.overloads(operator)) {
            result = tryOverload(node, operator, arguments(operator, args));
            if (result != JexlEngine.TRY_FAILED) {
                return result; // 1
            }
        }
        // try to call base overload (ie + for +=)
        final JexlOperator base = operator.getBaseOperator();
        if (base != null && operators.overloads(base)) {
            result = tryOverload(node, base, arguments(base, args));
            if (result != JexlEngine.TRY_FAILED) {
                assignFun.accept(result);
                return isPostfix(operator) ? args[0] : result; // 2
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
            interpreter.operatorError(node, operator, xany);
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
    Object tryOverload(final JexlNode node, final JexlOperator operator, final Object... args) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        controlNullOperands(arithmetic, operator, args);
        try {
            final boolean cache = interpreter.cache;
            if (cache) {
                final Object cached = node.jjtGetValue();
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
            JexlMethod vm = operators.overloads(operator) ? operators.getOperator(operator, args) : null;
            // no direct overload, any special case ?
            if (vm == null) {
               vm = getAlternateOverload(operator, args);
            }
            // *1: found a method, try it and cache it if successful
            if (vm != null) {
                final Object result = vm.tryInvoke(operator.getMethodName(), arithmetic, args);
                if (cache && !vm.tryFailed(result)) {
                    node.jjtSetValue(vm);
                }
                return result;
            }
            // *2: could not find an overload for this operator and arguments, keep track of the fail
            if (cache) {
                MethodKey key = new MethodKey(operator.getMethodName(), args);
                node.jjtSetValue(key);
            }
        } catch (final Exception xany) {
            // ignore return if lenient, will return try_failed
            interpreter.operatorError(node, operator, xany);
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
        if (CMP_OPS.contains(operator)) {
            JexlMethod cmp = operators.getOperator(JexlOperator.COMPARE, args);
            if (cmp != null) {
                return new CompareMethod(operator, cmp);
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

        private boolean operate(final int cmp) {
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
}
