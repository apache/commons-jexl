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

import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.internal.introspection.MethodExecutor;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Helper class to deal with operator overloading and specifics.
 * @since 3.0
 */
public class Operators {
    /**
     * Helper for postfix assignment operators.
     * @param operator the operator
     * @return true if operator is a postfix operator (x++, y--)
     */
    private static boolean isPostfix(final JexlOperator operator) {
        return operator == JexlOperator.GET_AND_INCREMENT || operator == JexlOperator.GET_AND_DECREMENT;
    }
    /** The owner. */
    protected final InterpreterBase interpreter;

    /** The overloaded arithmetic operators. */
    protected final JexlArithmetic.Uberspect operators;

    /**
     * Constructs a new instance.
     * @param owner the owning interpreter
     */
    protected Operators(final InterpreterBase owner) {
        final JexlArithmetic arithmetic = owner.arithmetic;
        final JexlUberspect uberspect = owner.uberspect;
        this.interpreter = owner;
        this.operators = uberspect.getArithmetic(arithmetic);
    }

    /**
     * Tidy arguments based on operator arity.
     * <p>The interpreter may add a null to the arguments of operator expecting only one parameter.</p>
     * @param operator the operator
     * @param args the arguements (as seen by the interpreter)
     * @return the tidied arguments
     */
    private Object[] arguments(final JexlOperator operator, final Object...args) {
        return operator.getArity() == 1 && args.length > 1 ? new Object[]{args[0]} : args;
    }

    /**
     * The 'match'/'in' operator implementation.
     * <p>
     * Note that 'x in y' or 'x matches y' means 'y contains x' ;
     * the JEXL operator arguments order syntax is the reverse of this method call.
     * </p>
     * @param node  the node
     * @param op    the calling operator, =~ or !~
     * @param right the left operand
     * @param left  the right operand
     * @return true if left matches right, false otherwise
     */
    protected boolean contains(final JexlNode node, final String op, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        try {
            // try operator overload
            final Object result = tryOverload(node, JexlOperator.CONTAINS, left, right);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            // use arithmetic / pattern matching ?
            final Boolean matched = arithmetic.contains(left, right);
            if (matched != null) {
                return matched;
            }
            // try a contains method (duck type set)
            try {
                final Object[] argv = {right};
                JexlMethod vm = uberspect.getMethod(left, "contains", argv);
                if (returnsBoolean(vm)) {
                    return (Boolean) vm.invoke(left, argv);
                }
                if (arithmetic.narrowArguments(argv)) {
                    vm = uberspect.getMethod(left, "contains", argv);
                    if (returnsBoolean(vm)) {
                        return (Boolean) vm.invoke(left, argv);
                    }
                }
            } catch (final Exception e) {
                throw new JexlException(node, op + " error", e);
            }
            // defaults to equal
            return arithmetic.equals(left, right);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, op + " error", xrt);
        }
    }

    /**
     * Throw a NPE if operator is strict and one of the arguments is null.
     * @param arithmetic the JEXL arithmetic instance
     * @param operator the operator to check
     * @param args the operands
     * @throws JexlArithmetic.NullOperand if operator is strict and an operand is null
     */
    protected void controlNullOperands(final JexlArithmetic arithmetic, final JexlOperator operator, final Object...args) {
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
     * Check for emptyness of various types: Collection, Array, Map, String, and anything that has a boolean isEmpty()
     * method.
     * <p>Note that the result may not be a boolean.
     *
     * @param node   the node holding the object
     * @param object the object to check the emptyness of
     * @return the evaluation result
     */
    protected Object empty(final JexlNode node, final Object object) {
        if (object == null) {
            return true;
        }
        Object result = tryOverload(node, JexlOperator.EMPTY, object);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
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
                    interpreter.operatorError(node, JexlOperator.EMPTY, xany);
                }
            }
        }
        return !(result instanceof Boolean) || (Boolean) result;
    }

    /**
     * The 'endsWith' operator implementation.
     * @param node     the node
     * @param operator the calling operator, ^= or ^!
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left ends with right, false otherwise
     */
    protected boolean endsWith(final JexlNode node, final String operator, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        try {
            // try operator overload
            final Object result = tryOverload(node, JexlOperator.ENDSWITH, left, right);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            // use arithmetic / pattern matching ?
            final Boolean matched = arithmetic.endsWith(left, right);
            if (matched != null) {
                return matched;
            }
            // try a endsWith method (duck type)
            try {
                final Object[] argv = {right};
                JexlMethod vm = uberspect.getMethod(left, "endsWith", argv);
                if (returnsBoolean(vm)) {
                    return (Boolean) vm.invoke(left, argv);
                }
                if (arithmetic.narrowArguments(argv)) {
                    vm = uberspect.getMethod(left, "endsWith", argv);
                    if (returnsBoolean(vm)) {
                        return (Boolean) vm.invoke(left, argv);
                    }
                }
            } catch (final Exception e) {
                throw new JexlException(node, operator + " error", e);
            }
            // defaults to equal
            return arithmetic.equals(left, right);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, operator + " error", xrt);
        }
    }

    /**
     * Checks whether a method is a JexlArithmetic method.
     * @param vm the JexlMethod (may be null)
     * @return true of false
     */
    private boolean isArithmetic(final JexlMethod vm) {
        if (vm instanceof MethodExecutor) {
            final Method method = ((MethodExecutor) vm).getMethod();
            return JexlArithmetic.class.equals(method.getDeclaringClass());
        }
        return false;
    }

    /**
     * Checks whether a method returns a boolean or a Boolean.
     * @param vm the JexlMethod (may be null)
     * @return true of false
     */
    private boolean returnsBoolean(final JexlMethod vm) {
        if (vm !=null) {
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
        if (vm !=null) {
            final Class<?> rc = vm.getReturnType();
            return Integer.TYPE.equals(rc) || Integer.class.equals(rc);
        }
        return false;
    }

    /**
     * Calculate the {@code size} of various types:
     * Collection, Array, Map, String, and anything that has a int size() method.
     * <p>Note that the result may not be an integer.
     *
     * @param node   the node that gave the value to size
     * @param object the object to get the size of
     * @return the evaluation result
     */
    protected Object size(final JexlNode node, final Object object) {
        if (object == null) {
            return 0;
        }
        Object result = tryOverload(node, JexlOperator.SIZE, object);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
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
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }

    /**
     * The 'startsWith' operator implementation.
     * @param node     the node
     * @param operator the calling operator, $= or $!
     * @param left     the left operand
     * @param right    the right operand
     * @return true if left starts with right, false otherwise
     */
    protected boolean startsWith(final JexlNode node, final String operator, final Object left, final Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        try {
            // try operator overload
            final Object result = tryOverload(node, JexlOperator.STARTSWITH, left, right);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            // use arithmetic / pattern matching ?
            final Boolean matched = arithmetic.startsWith(left, right);
            if (matched != null) {
                return matched;
            }
            // try a startsWith method (duck type)
            try {
                final Object[] argv = {right};
                JexlMethod vm = uberspect.getMethod(left, "startsWith", argv);
                if (returnsBoolean(vm)) {
                    return (Boolean) vm.invoke(left, argv);
                }
                if (arithmetic.narrowArguments(argv)) {
                    vm = uberspect.getMethod(left, "startsWith", argv);
                    if (returnsBoolean(vm)) {
                        return (Boolean) vm.invoke(left, argv);
                    }
                }
            } catch (final Exception e) {
                throw new JexlException(node, operator + " error", e);
            }
            // defaults to equal
            return arithmetic.equals(left, right);
        } catch (final ArithmeticException xrt) {
            throw new JexlException(node, operator + " error", xrt);
        }
    }

    /**
     * Evaluates an assign operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate.
     * If an overloads returns Operator.ASSIGN, it means the side-effect is complete.
     * Otherwise, {@code a += b <=> a = a + b}
     * </p>
     * @param node     the syntactic node
     * @param operator the operator
     * @param args     the arguments, the first one being the target of assignment
     * @return JexlOperator.ASSIGN if operation assignment has been performed,
     *         JexlEngine.TRY_FAILED if no operation was performed,
     *         the value to use as the side effect argument otherwise
     */
    protected Object tryAssignOverload(final JexlNode node,
                                       final JexlOperator operator,
                                       final Consumer<Object> assignFun,
                                       final Object...args) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        if (args.length < operator.getArity()) {
            return JexlEngine.TRY_FAILED;
        }
        Object result;
        try {
        // if some overloads exist...
        if (operators != null) {
            // try to call overload with side effect; the object is modified
            result = tryOverload(node, operator, arguments(operator, args));
            if (result != JexlEngine.TRY_FAILED) {
                return result; // 1
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
        }
        // base eval
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
    protected Object tryOverload(final JexlNode node, final JexlOperator operator, final Object... args) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        controlNullOperands(arithmetic, operator, args);
        if (operators != null && operators.overloads(operator)) {
            final boolean cache = interpreter.cache;
            try {
                if (cache) {
                    final Object cached = node.jjtGetValue();
                    if (cached instanceof JexlMethod) {
                        final JexlMethod me = (JexlMethod) cached;
                        final Object eval = me.tryInvoke(operator.getMethodName(), arithmetic, args);
                        if (!me.tryFailed(eval)) {
                            return eval;
                        }
                    }
                }
                final JexlMethod vm = operators.getOperator(operator, args);
                if (vm != null && !isArithmetic(vm)) {
                    final Object result = vm.invoke(arithmetic, args);
                    if (cache && !vm.tryFailed(result)) {
                        node.jjtSetValue(vm);
                    }
                    return result;
                }
            } catch (final Exception xany) {
                // ignore return if lenient, will return try_failed
                interpreter.operatorError(node, operator, xany);
            }
        }
        return JexlEngine.TRY_FAILED;
    }
}
