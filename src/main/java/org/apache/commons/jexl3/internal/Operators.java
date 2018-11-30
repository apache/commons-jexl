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
     * Checks whether a method returns a boolean or a Boolean.
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
     * Attempts to call an operator.
     * <p>
     * This takes care of finding and caching the operator method when appropriate
     * @param node     the syntactic node
     * @param operator the operator
     * @param args     the arguments
     * @return the result of the operator evaluation or TRY_FAILED
     */
    protected Object tryOverload(JexlNode node, JexlOperator operator, Object... args) {
        if (operators != null && operators.overloads(operator)) {
            final JexlArithmetic arithmetic = interpreter.arithmetic;
            final boolean cache = interpreter.cache && node != null;
            try {
                if (cache) {
                    Object cached = node.jjtGetValue();
                    if (cached instanceof JexlMethod) {
                        JexlMethod me = (JexlMethod) cached;
                        Object eval = me.tryInvoke(operator.getMethodName(), arithmetic, args);
                        if (!me.tryFailed(eval)) {
                            return eval;
                        }
                    }
                }
                JexlMethod vm = operators.getOperator(operator, args);
                if (vm != null) {
                    Object result = vm.invoke(arithmetic, args);
                    if (cache) {
                        node.jjtSetValue(vm);
                    }
                    return result;
                }
            } catch (Exception xany) {
                return interpreter.operatorError(node, operator, xany);
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
     * @param args     the arguments, the first one being the target of assignment
     * @return the result of the operator evaluation
     */
    protected Object tryAssignOverload(JexlNode node, JexlOperator operator, Object...args) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        if (args.length != operator.getArity()) {
            return JexlEngine.TRY_FAILED;
        }
        // try to call overload on side effect
        Object result = tryOverload(node, operator, args);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        // call base operator
        JexlOperator base = operator.getBaseOperator();
        if (operators != null && base != null && operators.overloads(base)) {
            // in case there is an overload
            try {
                JexlMethod vm = operators.getOperator(base, args);
                if (vm != null) {
                    result = vm.invoke(arithmetic, args);
                    if (result != JexlEngine.TRY_FAILED) {
                        return result;
                    }
                }
            } catch (Exception xany) {
                interpreter.operatorError(node, base, xany);
            }
        }
        // base eval
        try {
            switch (operator) {
                case SELF_ADD:
                    return arithmetic.selfAdd(args[0], args[1]);
                case SELF_SUBTRACT:
                    return arithmetic.selfSubtract(args[0], args[1]);
                case SELF_MULTIPLY:
                    return arithmetic.selfMultiply(args[0], args[1]);
                case SELF_DIVIDE:
                    return arithmetic.selfDivide(args[0], args[1]);
                case SELF_MOD:
                    return arithmetic.selfMod(args[0], args[1]);
                case SELF_AND:
                    return arithmetic.selfAnd(args[0], args[1]);
                case SELF_OR:
                    return arithmetic.selfOr(args[0], args[1]);
                case SELF_XOR:
                    return arithmetic.selfXor(args[0], args[1]);
                case SELF_SHL:
                    return arithmetic.selfLeftShift(args[0], args[1]);
                case SELF_SAR:
                    return arithmetic.selfRightShift(args[0], args[1]);
                case SELF_SHR:
                    return arithmetic.selfRightShiftUnsigned(args[0], args[1]);
                case INCREMENT:
                    return arithmetic.increment(args[0]);
                case DECREMENT:
                    return arithmetic.decrement(args[0]);
                default:
                    // unexpected, new operator added?
                    throw new UnsupportedOperationException(operator.getOperatorSymbol());
            }
        } catch (Exception xany) {
            interpreter.operatorError(node, base, xany);
        }
        return JexlEngine.TRY_FAILED;
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
     * @param op    the calling operator, =~ or !~
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
     * <p>Note that the result may not be a boolean.
     *
     * @param node   the node holding the object
     * @param object the object to check the emptyness of
     * @return the evaluation result
     */
    protected Object empty(JexlNode node, Object object) {
        if (object == null) {
            return Boolean.TRUE;
        }
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        Object result = tryOverload(node, JexlOperator.EMPTY, object);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        result = arithmetic.isEmpty(object);
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
     * <p>Note that the result may not be an integer.
     *
     * @param node   the node that gave the value to size
     * @param object the object to get the size of.
     * @return the evaluation result
     */
    protected Object size(JexlNode node, Object object) {
        if (object == null) {
            return 0;
        }
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        Object result = tryOverload(node, JexlOperator.SIZE, object);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        result = arithmetic.size(object);
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

    /**
     * Dereferences anything that has an Object get() method.
     *
     * @param node   the node holding the object
     * @param object the object to be dereferenced
     * @return the evaluation result
     */
    protected Object indirect(JexlNode node, Object object) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        Object result = tryOverload(node, JexlOperator.INDIRECT, object);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        result = arithmetic.indirect(object);
        if (result == JexlEngine.TRY_FAILED) {
            // check if there is a get() method on the object if so, just use it
            JexlMethod vm = uberspect.getMethod(object, "get", Interpreter.EMPTY_PARAMS);
            if (vm != null) {
                try {
                    result = vm.invoke(object, Interpreter.EMPTY_PARAMS);
                } catch (Exception xany) {
                    interpreter.operatorError(node, JexlOperator.INDIRECT, xany);
                }
            }
        }
        return result;
    }

    /**
     * Assigns a value to anything that has an Object set(Object value) method.
     *
     * @param node   the node holding the object
     * @param object the object to be dereferenced
     * @param right  the value to be assigned
     * @return the evaluation result
     */
    protected Object indirectAssign(JexlNode node, Object object, Object right) {
        final JexlArithmetic arithmetic = interpreter.arithmetic;
        final JexlUberspect uberspect = interpreter.uberspect;
        Object result = tryOverload(node, JexlOperator.INDIRECT_ASSIGN, object, right);
        if (result != JexlEngine.TRY_FAILED) {
            return result;
        }
        result = arithmetic.indirectAssign(object, right);
        if (result == JexlEngine.TRY_FAILED) {
            // check if there is a set(Object) method on the object and if so, just use it
            Object[] argv = {right};
            JexlMethod vm = uberspect.getMethod(object, "set", argv);
            if (vm != null) {
                try {
                    result = vm.invoke(object, argv);
                } catch (Exception xany) {
                    interpreter.operatorError(node, JexlOperator.INDIRECT_ASSIGN, xany);
                }
            }
        }
        return result;
    }

}
