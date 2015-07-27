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

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.JexlNode;

/**
 * Helper class to deal with operator overloading and specifics.
 * @since 3.0
 */
public class Operators {
    /** The owner. */
    protected final Interpreter interpreter;
    /** The overloaded arithmetic operators. */
    protected final JexlArithmetic.Uberspect operators;

    /**
     * Constructor.
     * @param owner the owning interpreter
     */
    protected Operators(Interpreter owner) {
        final JexlArithmetic arithmetic = owner.arithmetic;
        final JexlUberspect uberspect = owner.uberspect;
        this.interpreter = owner;
        this.operators = uberspect.getArithmetic(arithmetic);
    }

    /**
     * Checks whether a method returns a boolean or a Boolean
     * @param vm the JexlMethod (may be null)
     * @return true of false
     */
    private boolean returnsBoolean(JexlMethod vm) {
        if (vm !=null) {
            Class<?> rc = vm.getReturnType();
            return Boolean.TYPE.equals(rc) || Boolean.class.equals(rc);
        }
        return false;
    }

    /**
     * Attempts to call a monadic operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate
     * @param node     the syntactic node
     * @param operator the operator
     * @param arg      the argument
     * @return the result of the operator evaluation or TRY_FAILED
     */
    protected Object tryOverload(JexlNode node, JexlOperator operator, Object arg) {
        if (operators != null && operators.overloads(operator)) {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            final boolean cache = interpreter.cache;
            if (cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlMethod) {
                    JexlMethod me = (JexlMethod) cached;
                    Object eval = me.tryInvoke(operator.getMethodName(), arithmetic, arg);
                    if (!me.tryFailed(eval)) {
                        return eval;
                    }
                }
            }
            try {
                JexlMethod emptym = operators.getOperator(operator, arg);
                if (emptym != null) {
                    Object result = emptym.invoke(arithmetic, arg);
                    if (cache) {
                        node.jjtSetValue(emptym);
                    }
                    return result;
                }
            } catch (Exception xany) {
                interpreter.operatorError(node, operator, xany);
            }
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Attempts to call a diadic operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate
     * @param node     the syntactic node
     * @param operator the operator
     * @param lhs      the left hand side argument
     * @param rhs      the right hand side argument
     * @return the result of the operator evaluation or TRY_FAILED
     */
    protected Object tryOverload(JexlNode node, JexlOperator operator, Object lhs, Object rhs) {
        if (operators != null && operators.overloads(operator)) {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            final boolean cache = interpreter.cache;
            if (cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlMethod) {
                    JexlMethod me = (JexlMethod) cached;
                    Object eval = me.tryInvoke(operator.getMethodName(), arithmetic, lhs, rhs);
                    if (!me.tryFailed(eval)) {
                        return eval;
                    }
                }
            }
            try {
                JexlMethod emptym = operators.getOperator(operator, lhs, rhs);
                if (emptym != null) {
                    Object result = emptym.invoke(arithmetic, lhs, rhs);
                    if (cache) {
                        node.jjtSetValue(emptym);
                    }
                    return result;
                }
            } catch (Exception xany) {
                interpreter.operatorError(node, operator, xany);
            }
        }
        return JexlEngine.TRY_FAILED;
    }

    /**
     * Evaluates an assign operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate.
     * If an overloads returns Operator.ASSIGN, it means the side-effect is complete.
     * Otherwise, a += b &lt;=&gt; a = a + b
     * </p>
     * @param node     the syntactic node
     * @param operator the operator
     * @param lhs      the left hand side, target of the side-effect
     * @param rhs      the right hand side, argument of the (base) operator
     * @return the result of the operator evaluation
     */
    protected Object tryAssignOverload(JexlNode node, JexlOperator operator, Object lhs, Object rhs) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        // try to call overload on side effect
        Object result = tryOverload(node, operator, lhs, rhs);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        // call base operator
        JexlOperator base = operator.getBaseOperator();
        if (base == null) {
            throw new IllegalArgumentException("must be called with a side-effect operator");
        }
        if (operators != null && operators.overloads(base)) {
            // in case there is an overload
            try {
                JexlMethod emptym = operators.getOperator(base, lhs, rhs);
                if (emptym != null) {
                    result = emptym.invoke(arithmetic, lhs, rhs);
                    if (result != JexlEngine.TRY_FAILED) {
                        return result;
                    }
                }
            } catch (Exception xany) {
                interpreter.operatorError(node, base, xany);
            }
        }
        // base eval
        switch (operator) {
            case SELF_ADD:
                return arithmetic.add(lhs, rhs);
            case SELF_SUBTRACT:
                return arithmetic.subtract(lhs, rhs);
            case SELF_MULTIPLY:
                return arithmetic.multiply(lhs, rhs);
            case SELF_DIVIDE:
                return arithmetic.divide(lhs, rhs);
            case SELF_MOD:
                return arithmetic.mod(lhs, rhs);
            case SELF_AND:
                return arithmetic.and(lhs, rhs);
            case SELF_OR:
                return arithmetic.or(lhs, rhs);
            case SELF_XOR:
                return arithmetic.xor(lhs, rhs);
            default:
                throw new JexlException.Operator(node, operator.getOperatorSymbol(), null);
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
    protected boolean startsWith(JexlNode node, String operator, Object left, Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        try {
            // try operator overload
            Object result = tryOverload(node, JexlOperator.STARTSWITH, left, right);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            // use arithmetic / pattern matching ?
            Boolean matched = arithmetic.startsWith(left, right);
            if (matched != null) {
                return matched;
            }
            // try a startsWith method (duck type)
            try {
                Object[] argv = {right};
                JexlMethod vm = uberspect.getMethod(left, "startsWith", argv);
                if (returnsBoolean(vm)) {
                    return (Boolean) vm.invoke(left, argv);
                } else if (arithmetic.narrowArguments(argv)) {
                    vm = uberspect.getMethod(left, "startsWith", argv);
                    if (returnsBoolean(vm)) {
                        return (Boolean) vm.invoke(left, argv);
                    }
                }
            } catch (Exception e) {
                throw new JexlException(node, operator + " error", e);
            }
            // defaults to equal
            return arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, operator + " error", xrt);
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
    protected boolean endsWith(JexlNode node, String operator, Object left, Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        try {
            // try operator overload
            Object result = tryOverload(node, JexlOperator.ENDSWITH, left, right);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            // use arithmetic / pattern matching ?
            Boolean matched = arithmetic.endsWith(left, right);
            if (matched != null) {
                return matched;
            }
            // try a endsWith method (duck type)
            try {
                Object[] argv = {right};
                JexlMethod vm = uberspect.getMethod(left, "endsWith", argv);
                if (returnsBoolean(vm)) {
                    return (Boolean) vm.invoke(left, argv);
                } else if (arithmetic.narrowArguments(argv)) {
                    vm = uberspect.getMethod(left, "endsWith", argv);
                    if (returnsBoolean(vm)) {
                        return (Boolean) vm.invoke(left, argv);
                    }
                }
            } catch (Exception e) {
                throw new JexlException(node, operator + " error", e);
            }
            // defaults to equal
            return arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, operator + " error", xrt);
        }
    }

    /**
     * The 'match'/'in' operator implementation.
     * <p>
     * Note that 'x in y' or 'x matches y' means 'y contains x' ;
     * the JEXL operator arguments order syntax is the reverse of this method call.
     * </p>
     * @param node  the node
     * @param op    the calling operator, =~ or !=
     * @param right the left operand
     * @param left  the right operand
     * @return true if left matches right, false otherwise
     */
    protected boolean contains(JexlNode node, String op, Object left, Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        try {
            // try operator overload
            Object result = tryOverload(node, JexlOperator.CONTAINS, left, right);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            // use arithmetic / pattern matching ?
            Boolean matched = arithmetic.contains(left, right);
            if (matched != null) {
                return matched;
            }
            // try a contains method (duck type set)
            try {
                Object[] argv = {right};
                JexlMethod vm = uberspect.getMethod(left, "contains", argv);
                if (returnsBoolean(vm)) {
                    return (Boolean) vm.invoke(left, argv);
                } else if (arithmetic.narrowArguments(argv)) {
                    vm = uberspect.getMethod(left, "contains", argv);
                    if (returnsBoolean(vm)) {
                        return (Boolean) vm.invoke(left, argv);
                    }
                }
            } catch (Exception e) {
                throw new JexlException(node, op + " error", e);
            }
            // defaults to equal
            return arithmetic.equals(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, op + " error", xrt);
        }
    }

    /**
     * Check for emptyness of various types: Collection, Array, Map, String, and anything that has a boolean isEmpty()
     * method.
     *
     * @param node   the node holding the object
     * @param object the object to check the emptyness of.
     * @return the boolean
     */
    protected Boolean empty(JexlNode node, Object object) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        if (object == null) {
            return Boolean.TRUE;
        }
        Object opcall = Operators.this.tryOverload(node, JexlOperator.EMPTY, object);
        if (opcall instanceof Boolean) {
            return (Boolean) opcall;
        }
        Boolean result = arithmetic.isEmpty(object);
        if (result == null) {
            result = false;
            // check if there is an isEmpty method on the object that returns a
            // boolean and if so, just use it
            JexlMethod vm = uberspect.getMethod(object, "isEmpty", Interpreter.EMPTY_PARAMS);
            if (returnsBoolean(vm)) {
                try {
                    result = (Boolean) vm.invoke(object, Interpreter.EMPTY_PARAMS);
                } catch (Exception xany) {
                    interpreter.operatorError(node, JexlOperator.EMPTY, xany);
                }
            }
        }
        return result;
    }

    /**
     * Calculate the <code>size</code> of various types:
     * Collection, Array, Map, String, and anything that has a int size() method.
     *
     * @param node   the node that gave the value to size
     * @param object the object to get the size of.
     * @return the size of val
     */
    protected Integer size(JexlNode node, Object object) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        if (object == null) {
            return 0;
        }
        Object opcall = Operators.this.tryOverload(node, JexlOperator.SIZE, object);
        if (opcall instanceof Integer) {
            return (Integer) opcall;
        }
        Integer result = arithmetic.size(object);
        if (result == null) {
            // check if there is a size method on the object that returns an
            // integer and if so, just use it
            JexlMethod vm = uberspect.getMethod(object, "size", Interpreter.EMPTY_PARAMS);
            if (vm != null && (Integer.TYPE.equals(vm.getReturnType()) || Integer.class.equals(vm.getReturnType()))) {
                try {
                    result = (Integer) vm.invoke(object, Interpreter.EMPTY_PARAMS);
                } catch (Exception xany) {
                    interpreter.operatorError(node, JexlOperator.SIZE, xany);
                }
            }
        }
        return result;
    }
}
