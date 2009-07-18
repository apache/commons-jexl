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
package org.apache.commons.jexl;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import org.apache.commons.jexl.parser.ASTAddNode;
import org.apache.commons.jexl.parser.ASTAndNode;
import org.apache.commons.jexl.parser.ASTArrayAccess;
import org.apache.commons.jexl.parser.ASTAssignment;
import org.apache.commons.jexl.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl.parser.ASTBlock;
import org.apache.commons.jexl.parser.ASTDivNode;
import org.apache.commons.jexl.parser.ASTEQNode;
import org.apache.commons.jexl.parser.ASTEmptyFunction;
import org.apache.commons.jexl.parser.ASTExpression;
import org.apache.commons.jexl.parser.ASTExpressionExpression;
import org.apache.commons.jexl.parser.ASTFalseNode;
import org.apache.commons.jexl.parser.ASTFunctionNode;
import org.apache.commons.jexl.parser.ASTFloatLiteral;
import org.apache.commons.jexl.parser.ASTForeachStatement;
import org.apache.commons.jexl.parser.ASTGENode;
import org.apache.commons.jexl.parser.ASTGTNode;
import org.apache.commons.jexl.parser.ASTIdentifier;
import org.apache.commons.jexl.parser.ASTIfStatement;
import org.apache.commons.jexl.parser.ASTIntegerLiteral;
import org.apache.commons.jexl.parser.ASTJexlScript;
import org.apache.commons.jexl.parser.ASTLENode;
import org.apache.commons.jexl.parser.ASTLTNode;
import org.apache.commons.jexl.parser.ASTMapEntry;
import org.apache.commons.jexl.parser.ASTMapLiteral;
import org.apache.commons.jexl.parser.ASTMethodNode;
import org.apache.commons.jexl.parser.ASTModNode;
import org.apache.commons.jexl.parser.ASTMulNode;
import org.apache.commons.jexl.parser.ASTNENode;
import org.apache.commons.jexl.parser.ASTNotNode;
import org.apache.commons.jexl.parser.ASTNullLiteral;
import org.apache.commons.jexl.parser.ASTOrNode;
import org.apache.commons.jexl.parser.ASTReference;
import org.apache.commons.jexl.parser.ASTReferenceExpression;
import org.apache.commons.jexl.parser.ASTSizeFunction;
import org.apache.commons.jexl.parser.ASTSizeMethod;
import org.apache.commons.jexl.parser.ASTStatementExpression;
import org.apache.commons.jexl.parser.ASTStringLiteral;
import org.apache.commons.jexl.parser.ASTSubtractNode;
import org.apache.commons.jexl.parser.ASTTernaryNode;
import org.apache.commons.jexl.parser.ASTTrueNode;
import org.apache.commons.jexl.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl.parser.ASTWhileStatement;
import org.apache.commons.jexl.parser.Node;
import org.apache.commons.jexl.parser.SimpleNode;
import org.apache.commons.jexl.parser.ParserVisitor;

import org.apache.commons.jexl.util.introspection.Info;
import org.apache.commons.jexl.util.introspection.Uberspect;
import org.apache.commons.jexl.util.introspection.VelMethod;
import org.apache.commons.jexl.util.introspection.VelPropertyGet;
import org.apache.commons.jexl.util.introspection.VelPropertySet;

/**
 * An interpreter of JEXL syntax.
 *
 * @since 2.0
 */
public class Interpreter implements ParserVisitor {
    /** The logger. */
    protected final Log logger;
    /** The uberspect. */
    protected final Uberspect uberspect;
    /** The arithmetic handler. */
    protected final Arithmetic arithmetic;
    /** The map of registered functions. */
    protected final Map<String, Object> functions;
    /** The context to store/retrieve variables. */
    protected final JexlContext context;
    /** Strict interpreter flag. */
    protected final boolean strict;
    /** Silent intepreter flag. */
    protected boolean silent;
    /** Registers made of 2 pairs of {register-name, value}. */
    protected Object[] registers = null;
    /** Dummy velocity info. */
    protected static final Info DUMMY = new Info("", 1, 1);
    /** Empty parameters for method matching. */
    protected static final Object[] EMPTY_PARAMS = new Object[0];

    /**
     * Creates an interpreter.
     * @param jexl the engine creating this interpreter
     * @param aContext the context to evaluate expression
     */
    public Interpreter(JexlEngine jexl, JexlContext aContext) {
        this.logger = jexl.logger;
        this.uberspect = jexl.uberspect;
        this.arithmetic = jexl.arithmetic;
        this.functions = jexl.functions;
        this.strict = !this.arithmetic.isLenient();
        this.silent = jexl.silent;
        this.context = aContext;
    }

    /**
     * Sets whether this interpreter throws JexlException during evaluation.
     * @param flag true means no JexlException will be thrown but will be logged
     *        as info through the Jexl engine logger, false allows them to be thrown.
     */
    public void setSilent(boolean flag) {
        this.silent = flag;
    }

    /**
     * Checks whether this interpreter throws JexlException during evaluation.
     * @return true if silent, false otherwise
     */
    public boolean isSilent() {
        return this.silent;
    }

    /**
     * Interpret the given script/expression.
     * <p>
     * If the underlying JEXL engine is silent, errors will be logged through its logger as info.
     * </p>
     * @param node the script or expression to interpret.
     * @return the result of the interpretation.
     * @throws JexlException if any error occurs during interpretation.
     */
    public Object interpret(SimpleNode node) {
        try {
            return node.jjtAccept(this, null);
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl;
        }
    }

    /**
     * Gets the uberspect.
     * @return an {@link Uberspect}
     */
    protected Uberspect getUberspect() {
        return uberspect;
    }

    /**
     * Sets this interpreter registers for bean access/assign expressions.
     * @param theRegisters the array of registers
     */
    protected void setRegisters(Object[] theRegisters) {
        this.registers = theRegisters;
    }

    /**
     * Finds the node causing a NPE for diadic operators.
     * @param xrt the RuntimeException
     * @param node the parent node
     * @param left the left argument
     * @param right the right argument
     * @return the left, right or parent node
     */
    protected Node findNullOperand(RuntimeException xrt, Node node, Object left, Object right) {
        if (xrt instanceof NullPointerException
                && JexlException.NULL_OPERAND == xrt.getMessage()) {
            if (left == null) {
                return node.jjtGetChild(0);
            }
            if (right == null) {
                return node.jjtGetChild(1);
            }
        }
        return node;
    }

    /** {@inheritDoc} */
    public Object visit(SimpleNode node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAddNode node, Object data) {
        /**
         * The pattern for exception mgmt is to let the child*.jjtAccept
         * out of the try/catch loop so that if one fails, the ex will
         * traverse up to the interpreter.
         * In cases where this is not convenient/possible, JexlException must
         * be caught explicitly and rethrown.
         */
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.add(left, right);
        } catch (RuntimeException xrt) {
            Node xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "add error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (!leftValue) {
                return Boolean.FALSE;
            }
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (!rightValue) {
                return Boolean.FALSE;
            }
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.TRUE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTArrayAccess node, Object data) {
        // first objectNode is the identifier
        Object object = node.jjtGetChild(0).jjtAccept(this, data);
        // can have multiple nodes - either an expression, integer literal or
        // reference
        int numChildren = node.jjtGetNumChildren();
        for (int i = 1; i < numChildren; i++) {
            Node nindex = node.jjtGetChild(i);
            Object index = nindex.jjtAccept(this, null);
            object = getAttribute(object, index, nindex);
        }

        return object;
    }

    /** {@inheritDoc} */
    public Object visit(ASTAssignment node, Object data) {
        // left contains the reference to assign to
        Node left = node.jjtGetChild(0);
        if (!(left instanceof ASTReference)) {
            throw new JexlException(left, "illegal assignment form");
        }
        // right is the value expression to assign
        Object right = node.jjtGetChild(1).jjtAccept(this, data);

        // determine initial object & property:
        Node objectNode = null;
        Object object = null;
        Node propertyNode = null;
        Object property = null;
        boolean isVariable = true;
        StringBuilder variableName = null;
        // 1: follow children till penultimate
        int last = left.jjtGetNumChildren() - 1;
        for (int c = 0; c < last; ++c) {
            objectNode = left.jjtGetChild(c);
            // evaluate the property within the object
            object = objectNode.jjtAccept(this, object);
            if (object != null) {
                continue;
            }
            isVariable &= objectNode instanceof ASTIdentifier;
            // if we get null back as a result, check for an ant variable
            if (isVariable) {
                String name = ((ASTIdentifier) objectNode).image;
                if (c == 0) {
                    variableName = new StringBuilder(name);
                } else {
                    variableName.append('.');
                    variableName.append(name);
                }
                object = context.getVars().get(variableName.toString());
                // disallow mixing ant & bean with same root; avoid ambiguity
                if (object != null) {
                    isVariable = false;
                }
            } else {
                throw new JexlException(objectNode, "illegal assignment form");
            }
        }
        // 2: last objectNode will perform assignement in all cases
        propertyNode = left.jjtGetChild(last);
        if (propertyNode instanceof ASTIdentifier) {
            property = ((ASTIdentifier) propertyNode).image;
            // deal with ant variable
            if (isVariable && object == null) {
                if (variableName != null) {
                    if (last > 0) {
                        variableName.append('.');
                    }
                    variableName.append(property);
                    property = variableName.toString();
                }
                context.getVars().put(property, right);
                return right;
            }
        } else if (propertyNode instanceof ASTArrayAccess) {
            // first objectNode is the identifier
            objectNode = propertyNode;
            ASTArrayAccess narray = (ASTArrayAccess) objectNode;
            Object nobject = narray.jjtGetChild(0).jjtAccept(this, object);
            if (nobject == null) {
                throw new JexlException(objectNode, "array element is null");
            } else {
                object = nobject;
            }
            // can have multiple nodes - either an expression, integer literal or
            // reference
            last = narray.jjtGetNumChildren() - 1;
            for (int i = 1; i < last; i++) {
                objectNode = narray.jjtGetChild(i);
                Object index = objectNode.jjtAccept(this, null);
                object = getAttribute(object, index, objectNode);
            }
            property = narray.jjtGetChild(last).jjtAccept(this, null);
        } else {
            throw new JexlException(objectNode, "illegal assignment form");
        }
        if (property == null) {
            // no property, we fail
            throw new JexlException(propertyNode, "property is null");
        }
        if (object == null) {
            // no object, we fail
            throw new JexlException(objectNode, "bean is null");
        }
        // one before last, assign
        setAttribute(object, property, right, propertyNode);
        return right;
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        int n = 0;
        // coerce these two values longs and 'and'.
        try {
            long l = arithmetic.toLong(left);
            n = 1;
            long r = arithmetic.toLong(right);
            return new Long(l & r);
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(n), "long coercion error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseComplNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            long l = arithmetic.toLong(left);
            return new Long(~l);
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(0), "long coercion error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        int n = 0;
        // coerce these two values longs and 'or'.
        try {
            long l = arithmetic.toLong(left);
            n = 1;
            long r = arithmetic.toLong(right);
            return new Long(l | r);
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(n), "long coercion error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTBitwiseXorNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        int n = 0;
        // coerce these two values longs and 'xor'.
        try {
            long l = arithmetic.toLong(left);
            n = 1;
            long r = arithmetic.toLong(right);
            return new Long(l ^ r);
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(n), "long coercion error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTBlock node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTDivNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.divide(left, right);
        } catch (RuntimeException xrt) {
            if (!strict && xrt instanceof ArithmeticException) {
                return 0.0;
            }
            Node xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "divide error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTEmptyFunction node, Object data) {
        Object o = node.jjtGetChild(0).jjtAccept(this, data);
        if (o == null) {
            return Boolean.TRUE;
        }
        if (o instanceof String && "".equals(o)) {
            return Boolean.TRUE;
        }
        if (o.getClass().isArray() && ((Object[]) o).length == 0) {
            return Boolean.TRUE;
        }
        if (o instanceof Collection<?> && ((Collection<?>) o).isEmpty()) {
            return Boolean.TRUE;
        }
        // Map isn't a collection
        if (o instanceof Map<?, ?> && ((Map<?, ?>) o).isEmpty()) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTEQNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (RuntimeException xrt) {
            throw new JexlException(node, "== error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTExpressionExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTFalseNode node, Object data) {

        return Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTFloatLiteral node, Object data) {

        return Float.valueOf(node.image);
    }

    /** {@inheritDoc} */
    public Object visit(ASTForeachStatement node, Object data) {
        Object result = null;
        /* first objectNode is the loop variable */
        ASTReference loopReference = (ASTReference) node.jjtGetChild(0);
        ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);
        /* second objectNode is the variable to iterate */
        Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);
        // make sure there is a value to iterate on and a statement to execute
        if (iterableValue != null && node.jjtGetNumChildren() >= 3) {
            /* third objectNode is the statement to execute */
            SimpleNode statement = (SimpleNode) node.jjtGetChild(2);
            // get an iterator for the collection/array etc via the
            // introspector.
            Iterator<?> itemsIterator = getUberspect().getIterator(iterableValue, DUMMY);
            while (itemsIterator.hasNext()) {
                // set loopVariable to value of iterator
                Object value = itemsIterator.next();
                context.getVars().put(loopVariable.image, value);
                // execute statement
                result = statement.jjtAccept(this, data);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTGENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.greaterThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (RuntimeException xrt) {
            throw new JexlException(node, ">= error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTGTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.greaterThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (RuntimeException xrt) {
            throw new JexlException(node, "> error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTIdentifier node, Object data) {
        String name = node.image;
        if (data == null) {
            if (registers != null) {
                if (registers[0].equals(name)) {
                    return registers[1];
                }
                if (registers[2].equals(name)) {
                    return registers[3];
                }
            }
            Object value = context.getVars().get(name);
            if (value == null
                && !(node.jjtGetParent() instanceof ASTReference)
                && !context.getVars().containsKey(name)) {
                JexlException xjexl = new JexlException(node, "undefined variable " + name);
                if (strict) {
                    throw xjexl;
                }
                if (!silent) {
                    logger.warn(xjexl.getMessage());
                }
            }
            return value;
        } else {
            return getAttribute(data, name, node);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTIfStatement node, Object data) {
        int n = 0;
        try {
            Object result = null;
            /* first objectNode is the expression */
            Object expression = node.jjtGetChild(0).jjtAccept(this, data);
            if (arithmetic.toBoolean(expression)) {
                // first objectNode is true statement
                n = 1;
                result = node.jjtGetChild(1).jjtAccept(this, data);
            } else {
                // if there is a false, execute it. false statement is the second
                // objectNode
                if (node.jjtGetNumChildren() == 3) {
                    n = 2;
                    result = node.jjtGetChild(2).jjtAccept(this, data);
                }
            }
            return result;
        } catch (JexlException error) {
            throw error;
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(n), "if error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTIntegerLiteral node, Object data) {
        Integer value = Integer.valueOf(node.image);
        return (data == null) ? value : getAttribute(data, value);
    }

    /** {@inheritDoc} */
    public Object visit(ASTJexlScript node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            Node child = node.jjtGetChild(i);
            result = child.jjtAccept(this, data);
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTLENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.lessThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (RuntimeException xrt) {
            throw new JexlException(node, "<= error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTLTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.lessThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (RuntimeException xrt) {
            throw new JexlException(node, "< error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapEntry node, Object data) {
        Object key = node.jjtGetChild(0).jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);
        return new Object[]{key, value};
    }

    /** {@inheritDoc} */
    public Object visit(ASTMapLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        Map<Object, Object> map = new HashMap<Object, Object>();

        for (int i = 0; i < childCount; i++) {
            Object[] entry = (Object[]) (node.jjtGetChild(i)).jjtAccept(this, data);
            map.put(entry[0], entry[1]);
        }

        return map;
    }

    /** {@inheritDoc} */
    public Object visit(ASTMethodNode node, Object data) {
        // the object to invoke the method on should be in the data argument
        if (data == null) {
            // if the first child of the (ASTReference) parent,
            // it is considered as calling a 'top level' function
            if (node.jjtGetParent().jjtGetChild(0) == node) {
                data = functions.get(null);
                if (data == null) {
                    throw new JexlException(node, "no default function namespace");
                }
            } else {
                throw new JexlException(node, "attempting to call method on null");
            }
        }
        // objectNode 0 is the identifier (method name), the others are parameters.
        String methodName = ((ASTIdentifier) node.jjtGetChild(0)).image;

        // get our params
        int paramCount = node.jjtGetNumChildren() - 1;
        Object[] params = new Object[paramCount];
        for (int i = 0; i < paramCount; i++) {
            params[i] = node.jjtGetChild(i + 1).jjtAccept(this, null);
        }

        JexlException xjexl = null;
        try {
            VelMethod vm = getUberspect().getMethod(data, methodName, params, DUMMY);
            // DG: If we can't find an exact match, narrow the parameters and
            // try again!
            if (vm == null) {

                // replace all numbers with the smallest type that will fit
                for (int i = 0; i < params.length; i++) {
                    Object param = params[i];
                    if (param instanceof Number) {
                        params[i] = arithmetic.narrow((Number) param);
                    }
                }
                vm = getUberspect().getMethod(data, methodName, params, DUMMY);
                if (vm == null) {
                    xjexl = new JexlException(node, "unknown method", null);
                }
            }
            if (xjexl == null) {
                return vm.invoke(data, params);
            }
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (!(t instanceof Exception)) {
                t = e;
            }
            xjexl = new JexlException(node, "method invocation error", t);
        } catch (Exception e) {
            xjexl = new JexlException(node, "method error", e);
        }
        if (xjexl != null) {
            if (strict) {
                throw xjexl;
            }
            logger.warn(xjexl.getMessage(), xjexl.getCause());
        }
        return null;
    }

    /** {@inheritDoc} */
    public Object visit(ASTFunctionNode node, Object data) {
        // objectNode 0 is the prefix
        String prefix = ((ASTIdentifier) node.jjtGetChild(0)).image;
        Object functor = functions.get(prefix);
        if (functor == null) {
            throw new JexlException(node, "no such function namespace " + prefix);
        }
        // objectNode 1 is the identifier , the others are parameters.
        String methodName = ((ASTIdentifier) node.jjtGetChild(1)).image;

        // get our params
        int paramCount = node.jjtGetNumChildren() - 2;
        Object[] params = new Object[paramCount];
        for (int i = 0; i < paramCount; i++) {
            params[i] = node.jjtGetChild(i + 2).jjtAccept(this, null);
        }

        JexlException xjexl = null;
        try {
            VelMethod vm = getUberspect().getMethod(functor, methodName, params, DUMMY);
            // DG: If we can't find an exact match, narrow the parameters and
            // try again!
            if (vm == null) {

                // replace all numbers with the smallest type that will fit
                for (int i = 0; i < params.length; i++) {
                    Object param = params[i];
                    if (param instanceof Number) {
                        params[i] = arithmetic.narrow((Number) param);
                    }
                }
                vm = getUberspect().getMethod(functor, methodName, params, DUMMY);
                if (vm == null) {
                    xjexl = new JexlException(node, "unknown function", null);
                }
            }
            if (xjexl == null) {
                return vm.invoke(functor, params);
            }
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (!(t instanceof Exception)) {
                t = e;
            }
            xjexl = new JexlException(node, "function invocation error", t);
        } catch (Exception e) {
            xjexl = new JexlException(node, "function error", e);
        }
        if (xjexl != null) {
            if (strict) {
                throw xjexl;
            }
            logger.warn(xjexl.getMessage(), xjexl.getCause());
        }
        return null;
    }

    /** {@inheritDoc} */
    public Object visit(ASTModNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.mod(left, right);
        } catch (RuntimeException xrt) {
            if (!strict && xrt instanceof ArithmeticException) {
                return 0.0;
            }
            Node xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "% error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTMulNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.multiply(left, right);
        } catch (RuntimeException xrt) {
            Node xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "* error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTNENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
        } catch (RuntimeException xrt) {
            Node xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "!= error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTNotNode node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        return arithmetic.toBoolean(val) ? Boolean.FALSE : Boolean.TRUE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTNullLiteral node, Object data) {
        return null;
    }

    /** {@inheritDoc} */
    public Object visit(ASTOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (leftValue) {
                return Boolean.TRUE;
            }
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (rightValue) {
                return Boolean.TRUE;
            }
        } catch (RuntimeException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.FALSE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTReference node, Object data) {
        // could be array access, identifier or map literal
        // followed by zero or more ("." and array access, method, size,
        // identifier or integer literal)

        int numChildren = node.jjtGetNumChildren();

        // pass first piece of data in and loop through children
        Object result = null;
        StringBuilder variableName = null;
        Map<?, ?> vars = context.getVars();
        boolean isVariable = true;
        for (int i = 0; i < numChildren; i++) {
            Node theNode = node.jjtGetChild(i);
            isVariable &= (theNode instanceof ASTIdentifier);
            result = theNode.jjtAccept(this, result);
            // if we get null back a result, check for an ant variable
            if (result == null && isVariable) {
                String name = ((ASTIdentifier) theNode).image;
                if (i == 0) {
                    variableName = new StringBuilder(name);
                } else {
                    variableName.append('.');
                    variableName.append(name);
                }
                result = vars.get(variableName.toString());
            }
        }
        if (result == null) {
            if (isVariable
                && !(node.jjtGetParent() instanceof ASTTernaryNode)
                && !vars.containsKey(variableName.toString())) {
                JexlException xjexl = new JexlException(node, "undefined variable " + variableName.toString());
                if (strict) {
                    throw xjexl;
                }
                logger.warn(xjexl.getMessage());
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    public Object visit(ASTReferenceExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeFunction node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);

        if (val == null) {
            throw new JexlException(node, "size() : argument is null", null);
        }

        return new Integer(sizeOf(node, val));
    }

    /** {@inheritDoc} */
    public Object visit(ASTSizeMethod node, Object data) {
        return new Integer(sizeOf(node, data));
    }

    /** {@inheritDoc} */
    public Object visit(ASTStatementExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /** {@inheritDoc} */
    public Object visit(ASTStringLiteral node, Object data) {
        return node.image;
    }

    /** {@inheritDoc} */
    public Object visit(ASTSubtractNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.subtract(left, right);
        } catch (RuntimeException xrt) {
            Node xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "- error", xrt);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTTernaryNode node, Object data) {
        Object condition = node.jjtGetChild(0).jjtAccept(this, data);
        if (node.jjtGetNumChildren() == 3) {
            if (arithmetic.toBoolean(condition)) {
                return node.jjtGetChild(1).jjtAccept(this, data);
            } else {
                return node.jjtGetChild(2).jjtAccept(this, data);
            }
        }
        if (condition != null && !Boolean.FALSE.equals(condition)) {
            return condition;
        } else {
            return node.jjtGetChild(1).jjtAccept(this, data);
        }
    }

    /** {@inheritDoc} */
    public Object visit(ASTTrueNode node, Object data) {
        return Boolean.TRUE;
    }

    /** {@inheritDoc} */
    public Object visit(ASTUnaryMinusNode node, Object data) {
        Node valNode = node.jjtGetChild(0);
        Object val = valNode.jjtAccept(this, data);
        if (val instanceof Byte) {
            byte valueAsByte = ((Byte) val).byteValue();
            return new Byte((byte) -valueAsByte);
        } else if (val instanceof Short) {
            short valueAsShort = ((Short) val).shortValue();
            return new Short((short) -valueAsShort);
        } else if (val instanceof Integer) {
            int valueAsInt = ((Integer) val).intValue();
            return new Integer(-valueAsInt);
        } else if (val instanceof Long) {
            long valueAsLong = ((Long) val).longValue();
            return new Long(-valueAsLong);
        } else if (val instanceof Float) {
            float valueAsFloat = ((Float) val).floatValue();
            return new Float(-valueAsFloat);
        } else if (val instanceof Double) {
            double valueAsDouble = ((Double) val).doubleValue();
            return new Double(-valueAsDouble);
        } else if (val instanceof BigDecimal) {
            BigDecimal valueAsBigD = (BigDecimal) val;
            return valueAsBigD.negate();
        } else if (val instanceof BigInteger) {
            BigInteger valueAsBigI = (BigInteger) val;
            return valueAsBigI.negate();
        }
        throw new JexlException(valNode, "not a number");
    }

    /** {@inheritDoc} */
    public Object visit(ASTWhileStatement node, Object data) {
        Object result = null;
        /* first objectNode is the expression */
        Node expressionNode = (Node) node.jjtGetChild(0);
        while (arithmetic.toBoolean(expressionNode.jjtAccept(this, data))) {
            // execute statement
            result = node.jjtGetChild(1).jjtAccept(this, data);
        }

        return result;
    }

    /**
     * Calculate the <code>size</code> of various types: Collection, Array,
     * Map, String, and anything that has a int size() method.
     * @param node the node that gave the value to size
     * @param val the object to get the size of.
     * @return the size of val
     */
    private int sizeOf(Node node, Object val) {
        if (val instanceof Collection<?>) {
            return ((Collection<?>) val).size();
        } else if (val.getClass().isArray()) {
            return Array.getLength(val);
        } else if (val instanceof Map<?, ?>) {
            return ((Map<?, ?>) val).size();
        } else if (val instanceof String) {
            return ((String) val).length();
        } else {
            // check if there is a size method on the object that returns an
            // integer and if so, just use it
            Object[] params = new Object[0];
            VelMethod vm = uberspect.getMethod(val, "size", EMPTY_PARAMS, DUMMY);
            if (vm != null && vm.getReturnType() == Integer.TYPE) {
                Integer result;
                try {
                    result = (Integer) vm.invoke(val, params);
                } catch (Exception e) {
                    throw new JexlException(node, "size() : error executing", e);
                }
                return result.intValue();
            }
            throw new JexlException(node, "size() : unsupported type : " + val.getClass(), null);
        }
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @return the attribute value
     */
    public Object getAttribute(Object object, Object attribute) {
        return getAttribute(object, attribute, null);
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @param node the node that evaluated as the object
     * @return the attribute value
     */
    protected Object getAttribute(Object object, Object attribute, Node node) {
        if (object == null) {
            throw new JexlException(node, "object is null");
        }
        // maps do accept null keys; check attribute null status after trying
        if (object instanceof Map<?, ?>) {
            try {
                return ((Map<Object, Object>) object).get(attribute);
            } catch (RuntimeException xrt) {
                throw node == null ? xrt : new JexlException(node, "get map element error", xrt);
            }
        }
        if (attribute == null) {
            throw new JexlException(node, "object property is null");
        }
        if (object instanceof List<?>) {
            try {
                int idx = arithmetic.toInteger(attribute);
                return ((List<?>) object).get(idx);
            } catch (RuntimeException xrt) {
                throw node == null ? xrt : new JexlException(node, "get list element error", xrt);
            }
        }
        if (object.getClass().isArray()) {
            try {
                int idx = arithmetic.toInteger(attribute);
                return Array.get(object, idx);
            } catch (RuntimeException xrt) {
                throw node == null ? xrt : new JexlException(node, "get array element error", xrt);
            }
        }
        // look up bean property of data and return
        VelPropertyGet vg = getUberspect().getPropertyGet(object, attribute.toString(), DUMMY);
        if (vg != null) {
            try {
                return vg.invoke(object);
            } catch (Exception xany) {
                if (node == null) {
                    throw new RuntimeException(xany);
                }
                else {
                    throw new JexlException(node, "get object property error", xany);
                }
            }
        }

        return null;
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @param value the value to assign to the object's attribute
     */
    public void setAttribute(Object object, Object attribute, Object value) {
        setAttribute(object, attribute, null);
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or
     *            key for a map
     * @param value the value to assign to the object's attribute
     * @param node the node that evaluated as the object
     */
    protected void setAttribute(Object object, Object attribute, Object value, Node node) {
        if (object instanceof JexlContext) {
            ((JexlContext) object).getVars().put(attribute, value);
            return;
        }

        if (object instanceof Map<?, ?>) {
            try {
                ((Map<Object, Object>) object).put(attribute, value);
                return;
            } catch (RuntimeException xrt) {
                throw node == null ? xrt : new JexlException(node, "set map element error", xrt);
            }
        }
        if (object instanceof List<?>) {
            try {
                int idx = arithmetic.toInteger(attribute);
                ((List<Object>) object).set(idx, value);
                return;
            } catch (RuntimeException xrt) {
                throw node == null ? xrt : new JexlException(node, "set list element error", xrt);
            }
        }

        if (object == null) {
            throw new JexlException(node, "object is null");
        }

        if (object.getClass().isArray()) {
            try {
                int idx = arithmetic.toInteger(attribute);
                Array.set(object, idx, value);
                return;
            } catch (RuntimeException xrt) {
                throw node == null ? xrt : new JexlException(node, "set array element error", xrt);
            }
        }

        // "Otherwise (a JavaBean object)..." huh? :)
        String s = attribute.toString();
        VelPropertySet vs = getUberspect().getPropertySet(object, s, value, DUMMY);
        if (vs != null) {
            try {
                vs.invoke(object, value);
            } catch (RuntimeException xrt) {
                throw node == null ? xrt : new JexlException(node, "set object property error", xrt);
            } catch (Exception xany) {
                if (node == null) {
                    throw new RuntimeException(xany);
                } else {
                    throw new JexlException(node, "set object property error", xany);
                }
            }
            return;
        }
        if (node == null) {
            new UnsupportedOperationException("unable to set object property, "
                            + "object:" + object + ", property: " + attribute);
        }
        throw new JexlException(node, "unable to set bean property", null);
    }
}