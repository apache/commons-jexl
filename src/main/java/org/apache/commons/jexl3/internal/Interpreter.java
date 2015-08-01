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
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlPropertySet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.ASTAddNode;
import org.apache.commons.jexl3.parser.ASTAndNode;
import org.apache.commons.jexl3.parser.ASTArguments;
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTArrayLiteral;
import org.apache.commons.jexl3.parser.ASTAssignment;
import org.apache.commons.jexl3.parser.ASTBitwiseAndNode;
import org.apache.commons.jexl3.parser.ASTBitwiseComplNode;
import org.apache.commons.jexl3.parser.ASTBitwiseOrNode;
import org.apache.commons.jexl3.parser.ASTBitwiseXorNode;
import org.apache.commons.jexl3.parser.ASTBlock;
import org.apache.commons.jexl3.parser.ASTBreak;
import org.apache.commons.jexl3.parser.ASTConstructorNode;
import org.apache.commons.jexl3.parser.ASTContinue;
import org.apache.commons.jexl3.parser.ASTDivNode;
import org.apache.commons.jexl3.parser.ASTEQNode;
import org.apache.commons.jexl3.parser.ASTERNode;
import org.apache.commons.jexl3.parser.ASTEWNode;
import org.apache.commons.jexl3.parser.ASTEmptyFunction;
import org.apache.commons.jexl3.parser.ASTEmptyMethod;
import org.apache.commons.jexl3.parser.ASTExtendedLiteral;
import org.apache.commons.jexl3.parser.ASTFalseNode;
import org.apache.commons.jexl3.parser.ASTForeachStatement;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTGENode;
import org.apache.commons.jexl3.parser.ASTGTNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTIdentifierAccess;
import org.apache.commons.jexl3.parser.ASTIfStatement;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.apache.commons.jexl3.parser.ASTLENode;
import org.apache.commons.jexl3.parser.ASTLTNode;
import org.apache.commons.jexl3.parser.ASTMapEntry;
import org.apache.commons.jexl3.parser.ASTMapLiteral;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTModNode;
import org.apache.commons.jexl3.parser.ASTMulNode;
import org.apache.commons.jexl3.parser.ASTNENode;
import org.apache.commons.jexl3.parser.ASTNEWNode;
import org.apache.commons.jexl3.parser.ASTNRNode;
import org.apache.commons.jexl3.parser.ASTNSWNode;
import org.apache.commons.jexl3.parser.ASTNotNode;
import org.apache.commons.jexl3.parser.ASTNullLiteral;
import org.apache.commons.jexl3.parser.ASTNumberLiteral;
import org.apache.commons.jexl3.parser.ASTOrNode;
import org.apache.commons.jexl3.parser.ASTRangeNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.ASTReferenceExpression;
import org.apache.commons.jexl3.parser.ASTReturnStatement;
import org.apache.commons.jexl3.parser.ASTSWNode;
import org.apache.commons.jexl3.parser.ASTSetAddNode;
import org.apache.commons.jexl3.parser.ASTSetAndNode;
import org.apache.commons.jexl3.parser.ASTSetDivNode;
import org.apache.commons.jexl3.parser.ASTSetLiteral;
import org.apache.commons.jexl3.parser.ASTSetModNode;
import org.apache.commons.jexl3.parser.ASTSetMultNode;
import org.apache.commons.jexl3.parser.ASTSetOrNode;
import org.apache.commons.jexl3.parser.ASTSetSubNode;
import org.apache.commons.jexl3.parser.ASTSetXorNode;
import org.apache.commons.jexl3.parser.ASTSizeFunction;
import org.apache.commons.jexl3.parser.ASTSizeMethod;
import org.apache.commons.jexl3.parser.ASTStringLiteral;
import org.apache.commons.jexl3.parser.ASTSubNode;
import org.apache.commons.jexl3.parser.ASTTernaryNode;
import org.apache.commons.jexl3.parser.ASTTrueNode;
import org.apache.commons.jexl3.parser.ASTUnaryMinusNode;
import org.apache.commons.jexl3.parser.ASTVar;
import org.apache.commons.jexl3.parser.ASTWhileStatement;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.Node;
import org.apache.commons.jexl3.parser.ParserVisitor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * An interpreter of JEXL syntax.
 *
 * @since 2.0
 */
public class Interpreter extends ParserVisitor {
    /** The JEXL engine. */
    protected final Engine jexl;
    /** The logger. */
    protected final Logger logger;
    /** The uberspect. */
    protected final JexlUberspect uberspect;
    /** The arithmetic handler. */
    protected final JexlArithmetic arithmetic;
    /** The operators evaluation delegate. */
    protected final Operators operators;
    /** The map of symboled functions. */
    protected final Map<String, Object> functions;
    /** The map of symboled functions. */
    protected Map<String, Object> functors;
    /** The context to store/retrieve variables. */
    protected final JexlContext context;
    /** The context to store/retrieve variables. */
    protected final JexlContext.NamespaceResolver ns;
    /** Strict interpreter flag (may temporarily change when calling size and empty as functions). */
    protected boolean strictEngine;
    /** Strict interpreter flag. */
    protected final boolean strictArithmetic;
    /** Silent interpreter flag. */
    protected final boolean silent;
    /** Cache executors. */
    protected final boolean cache;
    /** symbol values. */
    protected final Scope.Frame frame;
    /** Cancellation support. */
    protected volatile boolean cancelled = false;
    /** Empty parameters for method matching. */
    protected static final Object[] EMPTY_PARAMS = new Object[0];

    /**
     * Creates an interpreter.
     * @param engine   the engine creating this interpreter
     * @param aContext the context to evaluate expression
     * @param eFrame   the interpreter evaluation frame
     */
    protected Interpreter(Engine engine, JexlContext aContext, Scope.Frame eFrame) {
        this.jexl = engine;
        this.logger = jexl.logger;
        this.uberspect = jexl.uberspect;
        this.context = aContext != null ? aContext : Engine.EMPTY_CONTEXT;
        if (this.context instanceof JexlEngine.Options) {
            JexlEngine.Options opts = (JexlEngine.Options) context;
            Boolean ostrict = opts.isStrict();
            Boolean osilent = opts.isSilent();
            this.strictEngine = ostrict == null ? jexl.isStrict() : ostrict;
            this.silent = osilent == null ? jexl.isSilent() : osilent;
            this.arithmetic = jexl.arithmetic.options(opts);
        } else {
            this.strictEngine = jexl.isStrict();
            this.silent = jexl.isSilent();
            this.arithmetic = jexl.arithmetic;
        }
        if (this.context instanceof JexlContext.NamespaceResolver) {
            ns = ((JexlContext.NamespaceResolver) context);
        } else {
            ns = Engine.EMPTY_NS;
        }
        this.functions = jexl.functions;
        this.strictArithmetic = this.arithmetic.isStrict();
        this.cache = jexl.cache != null;
        this.frame = eFrame;
        this.functors = null;
        this.operators = new Operators(this);
    }

    /**
     * Interpret the given script/expression.
     * <p>
     * If the underlying JEXL engine is silent, errors will be logged through
     * its logger as warning.
     * @param node the script or expression to interpret.
     * @return the result of the interpretation.
     * @throws JexlException if any error occurs during interpretation.
     */
    public Object interpret(JexlNode node) {
        JexlContext.ThreadLocal local = null;
        try {
            if (context instanceof JexlContext.ThreadLocal) {
                local = jexl.putThreadLocal((JexlContext.ThreadLocal) context);
            }
            return node.jjtAccept(this, null);
        } catch (JexlException.Return xreturn) {
            Object value = xreturn.getValue();
            return value;
        } catch (JexlException xjexl) {
            if (silent) {
                logger.warn(xjexl.getMessage(), xjexl.getCause());
                return null;
            }
            throw xjexl.clean();
        } finally {
            if (functors != null && AUTOCLOSEABLE != null) {
                for (Object functor : functors.values()) {
                    if (functor != null && AUTOCLOSEABLE.isAssignableFrom(functor.getClass())) {
                        try {
                            jexl.invokeMethod(functor, "close", EMPTY_PARAMS);
                        } catch (Exception xclose) {
                            logger.warn(xclose.getMessage(), xclose.getCause());
                        }
                    }
                }
            }
            functors = null;
            if (context instanceof JexlContext.ThreadLocal) {
                jexl.putThreadLocal(local);
            }
        }
    }

    /** Java7 AutoCloseable interface defined?. */
    private static final Class<?> AUTOCLOSEABLE;
    static {
        Class<?> c;
        try {
            c = Class.forName("java.lang.AutoCloseable");
        } catch (ClassNotFoundException xclass) {
            c = null;
        }
        AUTOCLOSEABLE = c;
    }

    /**
     * Finds the node causing a NPE for diadic operators.
     * @param xrt   the RuntimeException
     * @param node  the parent node
     * @param left  the left argument
     * @param right the right argument
     * @return the left, right or parent node
     */
    protected JexlNode findNullOperand(RuntimeException xrt, JexlNode node, Object left, Object right) {
        if (xrt instanceof JexlArithmetic.NullOperand) {
            if (left == null) {
                return node.jjtGetChild(0);
            }
            if (right == null) {
                return node.jjtGetChild(1);
            }
        }
        return node;
    }

    /**
     * Triggered when a variable can not be resolved.
     * @param node  the node where the error originated from
     * @param var   the variable name
     * @param undef whether the variable is undefined or null
     * @return throws JexlException if isStrict, null otherwise
     */
    protected Object unsolvableVariable(JexlNode node, String var, boolean undef) {
        if (strictEngine && (undef || arithmetic.isStrict())) {
            throw new JexlException.Variable(node, var, undef);
        }
        if (!silent) {
            logger.warn(JexlException.variableError(node, var, undef));
        }
        return null;
    }

    /**
     * Triggered when a method can not be resolved.
     * @param node   the node where the error originated from
     * @param method the method name
     * @return throws JexlException if isStrict, null otherwise
     */
    protected Object unsolvableMethod(JexlNode node, String method) {
        if (strictEngine) {
            throw new JexlException.Method(node, method);
        }
        if (!silent) {
            logger.warn(JexlException.methodError(node, method));
        }
        return null;
    }

    /**
     * Triggered when a property can not be resolved.
     * @param node  the node where the error originated from
     * @param var   the property name
     * @param cause the cause if any
     * @return throws JexlException if isStrict, null otherwise
     */
    protected Object unsolvableProperty(JexlNode node, String var, Throwable cause) {
        if (strictEngine) {
            throw new JexlException.Property(node, var, cause);
        }
        if (!silent) {
            logger.warn(JexlException.propertyError(node, var));
        }
        return null;
    }

    /**
     * Triggered when an operator fails.
     * @param node     the node where the error originated from
     * @param operator the method name
     * @param cause    the cause of error (if any)
     * @throws JexlException if isStrict
     */
    protected void operatorError(JexlNode node, JexlOperator operator, Throwable cause) {
        if (cause != null) {
            if (strictEngine) {
                throw new JexlException.Operator(node, operator.getOperatorSymbol(), cause);
            }
            if (!silent) {
                logger.warn(JexlException.operatorError(node, operator.getOperatorSymbol()));
            }
        }
    }

    /**
     * Triggered when method, function or constructor invocation fails.
     * @param xjexl the JexlException wrapping the original error
     * @return throws JexlException if isStrict, null otherwise
     */
    protected Object invocationFailed(JexlException xjexl) {
        if (strictEngine || xjexl instanceof JexlException.Return) {
            throw xjexl;
        }
        if (!silent) {
            logger.warn(xjexl.getMessage(), xjexl.getCause());
        }
        return null;
    }

    /**
     * Checks whether this interpreter execution was cancelled due to thread interruption.
     * @return true if cancelled, false otherwise
     */
    protected boolean isCancelled() {
        if (cancelled | Thread.interrupted()) {
            cancelled = true;
        }
        return cancelled;
    }

    /**
     * Resolves a namespace, eventually allocating an instance using context as constructor argument.
     * <p>
     * The lifetime of
     * such instances span the current expression or script evaluation.</p>
     * @param prefix the prefix name (may be null for global namespace)
     * @param node   the AST node
     * @return the namespace instance
     */
    protected Object resolveNamespace(String prefix, JexlNode node) {
        Object namespace;
        // check whether this namespace is a functor
        if (functors != null) {
            namespace = functors.get(prefix);
            if (namespace != null) {
                return namespace;
            }
        }
        // check if namespace is a resolver
        namespace = ns.resolveNamespace(prefix);
        if (namespace == null) {
            namespace = functions.get(prefix);
            if (prefix != null && namespace == null) {
                throw new JexlException(node, "no such function namespace " + prefix, null);
            }
        }
        // allow namespace to instantiate a functor with context if possible, not an error otherwise
        Object functor = null;
        if (namespace instanceof JexlContext.NamespaceFunctor) {
            functor = ((JexlContext.NamespaceFunctor) namespace).createFunctor(context);
        } else if (namespace instanceof Class<?>) {
            Object[] args = new Object[]{context};
            JexlMethod ctor = uberspect.getConstructor(namespace, args);
            if (ctor != null) {
                try {
                    functor = ctor.invoke(namespace, args);
                } catch (Exception xinst) {
                    throw new JexlException(node, "unable to instantiate namespace " + prefix, xinst);
                }
            }
        }
        // got a functor, store it and return it
        if (functor != null) {
            if (functors == null) {
                functors = new HashMap<String, Object>();
            }
            functors.put(prefix, functor);
            return functor;
        } else {
            return namespace;
        }
    }

    @Override
    protected Object visit(ASTAddNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.ADD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.add(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "+ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTSubNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.SUBTRACT, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.subtract(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "- error", xrt);
        }
    }

    @Override
    protected Object visit(ASTMulNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.MULTIPLY, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.multiply(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "* error", xrt);
        }
    }

    @Override
    protected Object visit(ASTDivNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.DIVIDE, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.divide(left, right);
        } catch (ArithmeticException xrt) {
            if (!strictArithmetic) {
                return 0.0d;
            }
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "/ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTModNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.MOD, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.mod(left, right);
        } catch (ArithmeticException xrt) {
            if (!strictArithmetic) {
                return 0.0d;
            }
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "% error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseAndNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.AND, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.and(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "& error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.OR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.or(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "| error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseXorNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.XOR, left, right);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.xor(left, right);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "^ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTEQNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.equals(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "== error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.EQ, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? arithmetic.toBoolean(result) ? Boolean.FALSE : Boolean.TRUE
                   : arithmetic.equals(left, right) ? Boolean.FALSE : Boolean.TRUE;
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, "!= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTGENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.GTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.greaterThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, ">= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTGTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.GT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.greaterThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "> error", xrt);
        }
    }

    @Override
    protected Object visit(ASTLENode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.LTE, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.lessThanOrEqual(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "<= error", xrt);
        }
    }

    @Override
    protected Object visit(ASTLTNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.LT, left, right);
            return result != JexlEngine.TRY_FAILED
                   ? result
                   : arithmetic.lessThan(left, right) ? Boolean.TRUE : Boolean.FALSE;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "< error", xrt);
        }
    }

    @Override
    protected Object visit(ASTSWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.startsWith(node, "^=", left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNSWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.startsWith(node, "^!", left, right) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTEWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.endsWith(node, "$=", left, right) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNEWNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.endsWith(node, "$!", left, right) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTERNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.contains(node, "=~", right, left) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNRNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        return operators.contains(node, "!~", right, left) ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTRangeNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            return arithmetic.createRange(left, right);
        } catch (ArithmeticException xrt) {
            JexlNode xnode = findNullOperand(xrt, node, left, right);
            throw new JexlException(xnode, ".. error", xrt);
        }
    }

    @Override
    protected Object visit(ASTUnaryMinusNode node, Object data) {
        JexlNode valNode = node.jjtGetChild(0);
        Object val = valNode.jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.NEGATE, val);
            if (result != JexlEngine.TRY_FAILED) {
                return result;
            }
            Object number = arithmetic.negate(val);
            // attempt to recoerce to literal class
            if (valNode instanceof ASTNumberLiteral && number instanceof Number) {
                number = arithmetic.narrowNumber((Number) number, ((ASTNumberLiteral) valNode).getLiteralClass());
            }
            return number;
        } catch (ArithmeticException xrt) {
            throw new JexlException(valNode, "- error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBitwiseComplNode node, Object data) {
        Object arg = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.COMPLEMENT, arg);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.complement(arg);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "~ error", xrt);
        }
    }

    @Override
    protected Object visit(ASTNotNode node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            Object result = operators.tryOverload(node, JexlOperator.NOT, val);
            return result != JexlEngine.TRY_FAILED ? result : arithmetic.not(val);
        } catch (ArithmeticException xrt) {
            throw new JexlException(node, "! error", xrt);
        }
    }

    @Override
    protected Object visit(ASTIfStatement node, Object data) {
        int n = 0;
        try {
            Object result = null;
            // first objectNode is the condition
            Object expression = node.jjtGetChild(0).jjtAccept(this, null);
            if (arithmetic.toBoolean(expression)) {
                // first objectNode is true statement
                n = 1;
                result = node.jjtGetChild(n).jjtAccept(this, null);
            } else {
                // if there is a false, execute it. false statement is the second
                // objectNode
                if (node.jjtGetNumChildren() == 3) {
                    n = 2;
                    result = node.jjtGetChild(n).jjtAccept(this, null);
                }
            }
            return result;
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(n), "if error", xrt);
        }
    }

    @Override
    protected Object visit(ASTBlock node, Object data) {
        int numChildren = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < numChildren; i++) {
            result = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return result;
    }

    @Override
    protected Object visit(ASTReturnStatement node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        throw new JexlException.Return(node, null, val);
    }

    @Override
    protected Object visit(ASTContinue node, Object data) {
        throw new JexlException.Continue(node);
    }

    @Override
    protected Object visit(ASTBreak node, Object data) {
        throw new JexlException.Break(node);
    }

    @Override
    protected Object visit(ASTForeachStatement node, Object data) {
        Object result = null;
        /* first objectNode is the loop variable */
        ASTReference loopReference = (ASTReference) node.jjtGetChild(0);
        ASTIdentifier loopVariable = (ASTIdentifier) loopReference.jjtGetChild(0);
        int symbol = loopVariable.getSymbol();
        /* second objectNode is the variable to iterate */
        Object iterableValue = node.jjtGetChild(1).jjtAccept(this, data);
        // make sure there is a value to iterate on and a statement to execute
        if (iterableValue != null && node.jjtGetNumChildren() >= 3) {
            /* third objectNode is the statement to execute */
            JexlNode statement = node.jjtGetChild(2);
            // get an iterator for the collection/array etc via the
            // introspector.
            Iterator<?> itemsIterator = uberspect.getIterator(iterableValue);
            if (itemsIterator != null) {
                while (itemsIterator.hasNext()) {
                    if (isCancelled()) {
                        throw new JexlException.Cancel(node);
                    }
                    // set loopVariable to value of iterator
                    Object value = itemsIterator.next();
                    if (symbol < 0) {
                        context.set(loopVariable.getName(), value);
                    } else {
                        frame.set(symbol, value);
                    }
                    try {
                        // execute statement
                        result = statement.jjtAccept(this, data);
                    } catch (JexlException.Break stmtBreak) {
                        break;
                    } catch (JexlException.Continue stmtContinue) {
                        //continue;
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTWhileStatement node, Object data) {
        Object result = null;
        /* first objectNode is the expression */
        Node expressionNode = node.jjtGetChild(0);
        while (arithmetic.toBoolean(expressionNode.jjtAccept(this, data))) {
            if (isCancelled()) {
                throw new JexlException.Cancel(node);
            }
            if (node.jjtGetNumChildren() > 1) {
                try {
                    // execute statement
                    result = node.jjtGetChild(1).jjtAccept(this, data);
                } catch (JexlException.Break stmtBreak) {
                    break;
                } catch (JexlException.Continue stmtContinue) {
                    //continue;
                }
            }
        }
        return result;
    }

    @Override
    protected Object visit(ASTAndNode node, Object data) {
        /**
         * The pattern for exception mgmt is to let the child*.jjtAccept out of the try/catch loop so that if one fails,
         * the ex will traverse up to the interpreter. In cases where this is not convenient/possible, JexlException
         * must be caught explicitly and rethrown.
         */
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
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTOrNode node, Object data) {
        Object left = node.jjtGetChild(0).jjtAccept(this, data);
        try {
            boolean leftValue = arithmetic.toBoolean(left);
            if (leftValue) {
                return Boolean.TRUE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(0), "boolean coercion error", xrt);
        }
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        try {
            boolean rightValue = arithmetic.toBoolean(right);
            if (rightValue) {
                return Boolean.TRUE;
            }
        } catch (ArithmeticException xrt) {
            throw new JexlException(node.jjtGetChild(1), "boolean coercion error", xrt);
        }
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNullLiteral node, Object data) {
        return null;
    }

    @Override
    protected Object visit(ASTTrueNode node, Object data) {
        return Boolean.TRUE;
    }

    @Override
    protected Object visit(ASTFalseNode node, Object data) {
        return Boolean.FALSE;
    }

    @Override
    protected Object visit(ASTNumberLiteral node, Object data) {
        if (data != null && node.isInteger()) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTStringLiteral node, Object data) {
        if (data != null) {
            return getAttribute(data, node.getLiteral(), node);
        }
        return node.getLiteral();
    }

    @Override
    protected Object visit(ASTArrayLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        JexlArithmetic.ArrayBuilder ab = arithmetic.arrayBuilder(childCount);
        if (ab != null) {
            boolean extended = false;
            for (int i = 0; i < childCount; i++) {
                JexlNode child = node.jjtGetChild(i);
                if (child instanceof ASTExtendedLiteral) {
                    extended = true;
                } else {
                    Object entry = node.jjtGetChild(i).jjtAccept(this, data);
                    ab.add(entry);
                }
            }
            return ab.create(extended);
        } else {
            return null;
        }
    }

    @Override
    protected Object visit(ASTExtendedLiteral node, Object data) {
        return node;
    }

    @Override
    protected Object visit(ASTSetLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        JexlArithmetic.SetBuilder mb = arithmetic.setBuilder(childCount);
        if (mb != null) {
            for (int i = 0; i < childCount; i++) {
                Object entry = node.jjtGetChild(i).jjtAccept(this, data);
                mb.add(entry);
            }
            return mb.create();
        } else {
            return null;
        }
    }

    @Override
    protected Object visit(ASTMapLiteral node, Object data) {
        int childCount = node.jjtGetNumChildren();
        JexlArithmetic.MapBuilder mb = arithmetic.mapBuilder(childCount);
        if (mb != null) {
            for (int i = 0; i < childCount; i++) {
                Object[] entry = (Object[]) (node.jjtGetChild(i)).jjtAccept(this, data);
                mb.put(entry[0], entry[1]);
            }
            return mb.create();
        } else {
            return null;
        }
    }

    @Override
    protected Object visit(ASTMapEntry node, Object data) {
        Object key = node.jjtGetChild(0).jjtAccept(this, data);
        Object value = node.jjtGetChild(1).jjtAccept(this, data);
        return new Object[]{key, value};
    }

    @Override
    protected Object visit(ASTTernaryNode node, Object data) {
        Object condition = node.jjtGetChild(0).jjtAccept(this, data);
        if (node.jjtGetNumChildren() == 3) {
            if (condition != null && arithmetic.toBoolean(condition)) {
                return node.jjtGetChild(1).jjtAccept(this, data);
            } else {
                return node.jjtGetChild(2).jjtAccept(this, data);
            }
        }
        if (condition != null && arithmetic.toBoolean(condition)) {
            return condition;
        } else {
            return node.jjtGetChild(1).jjtAccept(this, data);
        }
    }

    @Override
    protected Object visit(ASTSizeFunction node, Object data) {
        boolean isStrict = this.strictEngine;
        try {
            strictEngine = false;
            Object val = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.size(node, val);
        } finally {
            strictEngine = isStrict;
        }
    }

    @Override
    protected Object visit(ASTSizeMethod node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        return operators.size(node, val);
    }

    @Override
    protected Object visit(ASTEmptyFunction node, Object data) {
        boolean isStrict = this.strictEngine;
        try {
            strictEngine = false;
            Object value = node.jjtGetChild(0).jjtAccept(this, data);
            return operators.empty(node, value);
        } finally {
            strictEngine = isStrict;
        }
    }

    @Override
    protected Object visit(ASTEmptyMethod node, Object data) {
        Object val = node.jjtGetChild(0).jjtAccept(this, data);
        return operators.empty(node, val);
    }

    @Override
    protected Object visit(ASTJexlScript node, Object data) {
        if (node instanceof ASTJexlLambda && !((ASTJexlLambda) node).isTopLevel()) {
            return new Closure(this, (ASTJexlLambda) node);
        } else {
            final int numChildren = node.jjtGetNumChildren();
            Object result = null;
            for (int i = 0; i < numChildren; i++) {
                JexlNode child = node.jjtGetChild(i);
                result = child.jjtAccept(this, data);
            }
            return result;
        }
    }

    @Override
    protected Object visit(ASTVar node, Object data) {
        return visit((ASTIdentifier) node, data);
    }

    @Override
    protected Object visit(ASTReferenceExpression node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    protected Object visit(ASTIdentifier node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        String name = node.getName();
        if (data == null) {
            int symbol = node.getSymbol();
            if (symbol >= 0) {
                return frame.get(symbol);
            }
            Object value = context.get(name);
            if (value == null
                    && !(node.jjtGetParent() instanceof ASTReference)
                    && !context.has(name)
                    && !isTernaryProtected(node)) {
                return unsolvableVariable(node, name, true);
            }
            return value;
        } else {
            return getAttribute(data, name, node);
        }
    }

    @Override
    protected Object visit(ASTArrayAccess node, Object data) {
        // first objectNode is the identifier
        Object object = data;
        // can have multiple nodes - either an expression, integer literal or reference
        int numChildren = node.jjtGetNumChildren();
        for (int i = 0; i < numChildren; i++) {
            JexlNode nindex = node.jjtGetChild(i);
            if (object == null) {
                return null;
            }
            Object index = nindex.jjtAccept(this, null);
            object = getAttribute(object, index, nindex);
        }
        return object;
    }

    /**
     * Check if a null evaluated expression is protected by a ternary expression.
     * <p>
     * The rationale is that the ternary / elvis expressions are meant for the user to explictly take control
     * over the error generation; ie, ternaries can return null even if the engine in isStrict mode
     * would normally throw an exception.
     * </p>
     * @param node the expression node
     * @return true if nullable variable, false otherwise
     */
    protected boolean isTernaryProtected(JexlNode node) {
        for (JexlNode walk = node.jjtGetParent(); walk != null; walk = walk.jjtGetParent()) {
            if (walk instanceof ASTTernaryNode) {
                return true;
            } else if (!(walk instanceof ASTReference || walk instanceof ASTArrayAccess)) {
                break;
            }
        }
        return false;
    }

    /**
     * Checks whether a reference child node holds a local variable reference.
     * @param node  the reference node
     * @param which the child we are checking
     * @return true if child is local variable, false otherwise
     */
    protected boolean isLocalVariable(ASTReference node, int which) {
        return (node.jjtGetNumChildren() > which
                && node.jjtGetChild(which) instanceof ASTIdentifier
                && ((ASTIdentifier) node.jjtGetChild(which)).getSymbol() >= 0);
    }

    @Override
    protected Object visit(ASTIdentifierAccess node, Object data) {
        return data != null ? getAttribute(data, node.getIdentifier(), node) : null;
    }

    @Override
    protected Object visit(ASTReference node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        final int numChildren = node.jjtGetNumChildren();
        JexlNode parent = node.jjtGetParent();
        // pass first piece of data in and loop through children
        Object object = null;
        JexlNode objectNode;
        StringBuilder ant = null;
        boolean antish = !(parent instanceof ASTReference);
        int v = 1;
        main:
        for (int c = 0; c < numChildren; c++) {
            objectNode = node.jjtGetChild(c);
            if (objectNode instanceof ASTMethodNode && object == null) {
                break;
            }
            // attempt to evaluate the property within the object (visit(ASTIdentifierAccess node))
            object = objectNode.jjtAccept(this, object);
            if (object != null) {
                // disallow mixing antish variable & bean with same root; avoid ambiguity
                antish = false;
            } else if (antish) {  // if we still have a null object, check for an antish variable
                if (ant == null) {
                    JexlNode first = node.jjtGetChild(0);
                    if (first instanceof ASTIdentifier && ((ASTIdentifier) first).getSymbol() < 0) {
                        ant = new StringBuilder(((ASTIdentifier) first).getName());
                    } else {
                        break;
                    }
                }
                for (; v <= c; ++v) {
                    JexlNode child = node.jjtGetChild(v);
                    if (child instanceof ASTIdentifierAccess) {
                        ant.append('.');
                        ant.append(((ASTIdentifierAccess) objectNode).getName());
                    } else {
                        break;
                    }
                }
                object = context.get(ant.toString());
            } else {
                break;
            }
        }
        if (object == null && antish && ant != null && !isTernaryProtected(node)) {
            boolean undefined = !(context.has(ant.toString()) || isLocalVariable(node, 0));
            // variable unknown in context and not a local
            return unsolvableVariable(node, ant.toString(), undefined);
        }
        return object;
    }

    @Override
    protected Object visit(ASTAssignment node, Object data) {
        return executeAssign(node, null, data);
    }

    @Override
    protected Object visit(ASTSetAddNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_ADD, data);
    }

    @Override
    protected Object visit(ASTSetSubNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_SUBTRACT, data);
    }

    @Override
    protected Object visit(ASTSetMultNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_MULTIPLY, data);
    }

    @Override
    protected Object visit(ASTSetDivNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_DIVIDE, data);
    }

    @Override
    protected Object visit(ASTSetModNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_MOD, data);
    }

    @Override
    protected Object visit(ASTSetAndNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_AND, data);
    }

    @Override
    protected Object visit(ASTSetOrNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_OR, data);
    }

    @Override
    protected Object visit(ASTSetXorNode node, Object data) {
        return executeAssign(node, JexlOperator.SELF_XOR, data);
    }

    /**
     * Executes an assignment with an optional side-effect operator.
     * @param node     the node
     * @param assignop the assignment operator or null if simply assignment
     * @param data     the data
     * @return the left hand side
     */
    protected Object executeAssign(JexlNode node, JexlOperator assignop, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // left contains the reference to assign to
        final JexlNode left = node.jjtGetChild(0);
        // right is the value expression to assign
        Object right = node.jjtGetChild(1).jjtAccept(this, data);
        Object object = null;
        int symbol = -1;
        boolean antish = true;
        // 0: determine initial object & property:
        final int last = left.jjtGetNumChildren() - 1;
        if (left instanceof ASTIdentifier) {
            ASTIdentifier var = (ASTIdentifier) left;
            symbol = var.getSymbol();
            if (symbol >= 0) {
                // check we are not assigning a symbol itself
                if (last < 0) {
                    if (assignop != null) {
                        Object self = frame.get(symbol);
                        right = operators.tryAssignOverload(node, assignop, self, right);
                        if (right == JexlOperator.ASSIGN) {
                            return self;
                        }
                    }
                    frame.set(symbol, right);
                    // make the closure accessible to itself, ie hoist the currently set variable after frame creation
                    if (right instanceof Closure) {
                        ((Closure) right).setHoisted(symbol, right);
                    }
                    return right; // 1
                }
                object = frame.get(symbol);
                // top level is a symbol, can not be an antish var
                antish = false;
            } else {
                // check we are not assigning direct global
                if (last < 0) {
                    if (assignop != null) {
                        Object self = context.get(var.getName());
                        right = operators.tryAssignOverload(node, assignop, self, right);
                        if (right == JexlOperator.ASSIGN) {
                            return self;
                        }
                    }
                    try {
                        context.set(var.getName(), right);
                    } catch (UnsupportedOperationException xsupport) {
                        throw new JexlException(node, "context is readonly", xsupport);
                    }
                    return right; // 2
                }
                object = context.get(var.getName());
                // top level accesses object, can not be an antish var
                if (object != null) {
                    antish = false;
                }
            }
        } else if (!(left instanceof ASTReference)) {
            throw new JexlException(left, "illegal assignment form 0");
        }
        // 1: follow children till penultimate, resolve dot/array
        JexlNode objectNode = null;
        StringBuilder ant = null;
        int v = 1;
        // start at 1 if symbol
        for (int c = symbol >= 0 ? 1 : 0; c < last; ++c) {
            objectNode = left.jjtGetChild(c);
            object = objectNode.jjtAccept(this, object);
            if (object != null) {
                // disallow mixing antish variable & bean with same root; avoid ambiguity
                antish = false;
            } else if (antish) {
                if (ant == null) {
                    JexlNode first = left.jjtGetChild(0);
                    if (first instanceof ASTIdentifier && ((ASTIdentifier) first).getSymbol() < 0) {
                        ant = new StringBuilder(((ASTIdentifier) first).getName());
                    } else {
                        break;
                    }
                }
                for (; v <= c; ++v) {
                    JexlNode child = left.jjtGetChild(v);
                    if (child instanceof ASTIdentifierAccess) {
                        ant.append('.');
                        ant.append(((ASTIdentifierAccess) objectNode).getName());
                    } else {
                        break;
                    }
                }
                object = context.get(ant.toString());
            } else {
                throw new JexlException(objectNode, "illegal assignment form");
            }
        }
        // 2: last objectNode will perform assignement in all cases
        Object property = null;
        JexlNode propertyNode = left.jjtGetChild(last);
        if (propertyNode instanceof ASTIdentifierAccess) {
            property = ((ASTIdentifierAccess) propertyNode).getIdentifier();
            // deal with antish variable
            if (ant != null && object == null) {
                if (last > 0) {
                    ant.append('.');
                }
                ant.append(String.valueOf(property));
                if (assignop != null) {
                    Object self = context.get(ant.toString());
                    right = operators.tryAssignOverload(node, assignop, self, right);
                    if (right == JexlOperator.ASSIGN) {
                        return self;
                    }
                }
                try {
                    context.set(ant.toString(), right);
                } catch (UnsupportedOperationException xsupport) {
                    throw new JexlException(node, "context is readonly", xsupport);
                }
                return right; // 3
            }
        } else if (propertyNode instanceof ASTArrayAccess) {
            // can have multiple nodes - either an expression, integer literal or reference
            int numChildren = propertyNode.jjtGetNumChildren() - 1;
            for (int i = 0; i < numChildren; i++) {
                JexlNode nindex = propertyNode.jjtGetChild(i);
                Object index = nindex.jjtAccept(this, null);
                object = getAttribute(object, index, nindex);
            }
            propertyNode = propertyNode.jjtGetChild(numChildren);
            property = propertyNode.jjtAccept(this, null);
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
        // 3: one before last, assign
        if (assignop != null) {
            Object self = getAttribute(object, property, propertyNode);
            right = operators.tryAssignOverload(node, assignop, self, right);
            if (right == JexlOperator.ASSIGN) {
                return self;
            }
        }
        setAttribute(object, property, right, propertyNode);
        return right; // 4
    }

    @Override
    protected Object[] visit(ASTArguments node, Object data) {
        final int argc = node.jjtGetNumChildren();
        final Object[] argv = new Object[argc];
        for (int i = 0; i < argc; i++) {
            argv[i] = node.jjtGetChild(i).jjtAccept(this, data);
        }
        return argv;
    }

    @Override
    protected Object visit(final ASTMethodNode node, Object data) {
        // left contains the reference to the method
        final JexlNode methodNode = node.jjtGetChild(0);
        Object object = null;
        JexlNode objectNode = null;
        Object method;
        // 1: determine object and method or functor
        if (methodNode instanceof ASTIdentifierAccess) {
            method = methodNode;
            object = data;
            if (object == null) {
                // no object, we fail
                throw new JexlException(objectNode, "object is null");
            }
        } else {
            method = methodNode.jjtAccept(this, null);
        }
        Object result = method;
        for (int a = 1; a < node.jjtGetNumChildren(); ++a) {
            if (result == null) {
                // no method, we fail
                throw new JexlException(methodNode, "method is null");
            }
            ASTArguments argNode = (ASTArguments) node.jjtGetChild(a);
            result = call(node, object, result, argNode);
            object = null;
        }
        return result;
    }

    /**
     * Calls a method (or function).
     * <p>
     * Method resolution is a follows:
     * 1 - attempt to find a method in the bean passed as parameter;
     * 3 - if this fails, seeks a JexlScript or JexlMethod as a property of that bean;
     * 2 - if this fails, narrow the arguments and try again
     * </p>
     *
     * @param node    the method node
     * @param bean    the bean this method should be invoked upon
     * @param functor the object carrying the method or function
     * @param argNode the node carrying the arguments
     * @return the result of the method invocation
     */
    protected Object call(JexlNode node, Object bean, Object functor, ASTArguments argNode) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        JexlException xjexl;
        // evaluate the arguments
        Object[] argv = visit(argNode, null);

        // get the method name if identifier
        String methodName = null;
        int symbol = -1;
        if (functor instanceof ASTIdentifier) {
            ASTIdentifier methodIdentifier = (ASTIdentifier) functor;
            symbol = methodIdentifier.getSymbol();
            methodName = methodIdentifier.getName();
            functor = null;
        } else if (functor instanceof ASTIdentifierAccess) {
            methodName = ((ASTIdentifierAccess) functor).getName();
            functor = null;
        }
        try {
            boolean cacheable = cache;
            boolean narrow = true;
            JexlMethod vm = null;
            // pseudo loop and a half
            while (true) {
                if (methodName != null) {
                    // attempt to reuse last executor cached in volatile JexlNode.value
                    if (cache) {
                        Object cached = node.jjtGetValue();
                        if (cached instanceof JexlMethod) {
                            JexlMethod me = (JexlMethod) cached;
                            Object eval = me.tryInvoke(methodName, bean, argv);
                            if (!me.tryFailed(eval)) {
                                return eval;
                            }
                        }
                    }
                    // try a method
                    vm = uberspect.getMethod(bean, methodName, argv);
                    if (vm != null || !narrow) {
                        // if there is a method name, we will exit here on first or second pass
                        break;
                    }
                }
                // could not find a method, try as a var
                if (functor == null) {
                    if (symbol >= 0) {
                        functor = frame.get(symbol);
                    } else {
                        JexlPropertyGet get = uberspect.getPropertyGet(bean, methodName);
                        if (get != null) {
                            functor = get.tryInvoke(bean, methodName);
                        }
                    }
                }
                // lambda, script or jexl method will do
                if (functor instanceof JexlScript) {
                    return ((JexlScript) functor).execute(context, argv);
                }
                if (functor instanceof JexlMethod) {
                    return ((JexlMethod) functor).invoke(bean, argv);
                }
                // if we did not find an exact method by name and we haven't tried yet,
                // attempt to narrow the parameters and if this succeeds, try again in next loop
                if (methodName == null || !arithmetic.narrowArguments(argv)) {
                    break;
                } else {
                    narrow = false;
                }
            }
            // we have either evaluated and returned or might have found a method
            if (vm != null) {
                // vm cannot be null if xjexl is null
                Object eval = vm.invoke(bean, argv);
                // cache executor in volatile JexlNode.value
                if (cacheable && vm.isCacheable()) {
                    node.jjtSetValue(vm);
                }
                return eval;
            } else {
                return unsolvableMethod(node, methodName);
            }
        } catch (JexlException.Method xmethod) {
            throw xmethod;
        } catch (Exception xany) {
            xjexl = new JexlException(node, methodName, xany);
        }
        return invocationFailed(xjexl);
    }

    @Override
    protected Object visit(ASTFunctionNode node, Object data) {
        int argc = node.jjtGetNumChildren();
        if (argc == 2) {
            Object namespace = resolveNamespace(null, node);
            if (namespace == null) {
                namespace = context;
            }
            ASTIdentifier functionNode = (ASTIdentifier) node.jjtGetChild(0);
            ASTArguments argNode = (ASTArguments) node.jjtGetChild(1);
            return call(node, namespace, functionNode, argNode);
        } else {
            // objectNode 0 is the prefix
            String prefix = ((ASTIdentifier) node.jjtGetChild(0)).getName();
            Object namespace = resolveNamespace(prefix, node);
            // objectNode 1 is the identifier , the others are parameters.
            ASTIdentifier functionNode = (ASTIdentifier) node.jjtGetChild(1);
            ASTArguments argNode = (ASTArguments) node.jjtGetChild(2);
            return call(node, namespace, functionNode, argNode);
        }
    }

    @Override
    protected Object visit(ASTConstructorNode node, Object data) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // first child is class or class name
        Object cobject = node.jjtGetChild(0).jjtAccept(this, data);
        // get the ctor args
        int argc = node.jjtGetNumChildren() - 1;
        Object[] argv = new Object[argc];
        for (int i = 0; i < argc; i++) {
            argv[i] = node.jjtGetChild(i + 1).jjtAccept(this, data);
        }

        JexlException xjexl = null;
        try {
            // attempt to reuse last constructor cached in volatile JexlNode.value
            if (cache) {
                Object cached = node.jjtGetValue();
                if (cached instanceof JexlMethod) {
                    JexlMethod mctor = (JexlMethod) cached;
                    Object eval = mctor.tryInvoke(null, cobject, argv);
                    if (!mctor.tryFailed(eval)) {
                        return eval;
                    }
                }
            }
            JexlMethod ctor = uberspect.getConstructor(cobject, argv);
            // DG: If we can't find an exact match, narrow the parameters and try again
            if (ctor == null) {
                if (arithmetic.narrowArguments(argv)) {
                    ctor = uberspect.getConstructor(cobject, argv);
                }
                if (ctor == null) {
                    String dbgStr = cobject != null ? cobject.toString() : null;
                    return unsolvableMethod(node, dbgStr);
                }
            }
            Object instance = ctor.invoke(cobject, argv);
            // cache executor in volatile JexlNode.value
            if (cache && ctor.isCacheable()) {
                node.jjtSetValue(ctor);
            }
            return instance;
        } catch (JexlException.Method xmethod) {
            throw xmethod;
        } catch (Exception xany) {
            String dbgStr = cobject != null ? cobject.toString() : null;
            xjexl = new JexlException(node, dbgStr, xany);
        }
        return invocationFailed(xjexl);
    }

    /**
     * Determines the property {s,g}etter strategy to use.
     * @param node the syntactic node
     * @param obj the instance we are seeking the {s,g}etter from
     * @return a list of resolvers, not null
     */
    protected List<JexlUberspect.ResolverType> getPropertyResolvers(JexlNode node, Object obj) {
        List<JexlUberspect.ResolverType> strategy = node == null
                                                    ? null
                                                    : node.jjtGetParent() instanceof ASTArrayAccess
                                                    ? JexlUberspect.MAP
                                                    : JexlUberspect.POJO;
        return strategy;
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object    to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @return the attribute value
     */
    public Object getAttribute(Object object, Object attribute) {
        return getAttribute(object, attribute, null);
    }

    /**
     * Gets an attribute of an object.
     *
     * @param object    to retrieve value from
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param node      the node that evaluated as the object
     * @return the attribute value
     */
    protected Object getAttribute(Object object, Object attribute, JexlNode node) {
        if (object == null) {
            throw new JexlException(node, "object is null");
        }
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // attempt to reuse last executor cached in volatile JexlNode.value
        if (node != null && cache) {
            Object cached = node.jjtGetValue();
            if (cached instanceof JexlPropertyGet) {
                JexlPropertyGet vg = (JexlPropertyGet) cached;
                Object value = vg.tryInvoke(object, attribute);
                if (!vg.tryFailed(value)) {
                    return value;
                }
            }
        }
        // resolve that property
        Exception xcause = null;
        List<JexlUberspect.ResolverType> strategy = getPropertyResolvers(node, object);
        JexlPropertyGet vg = uberspect.getPropertyGet(strategy, object, attribute);
        if (vg != null) {
            try {
                Object value = vg.invoke(object);
                // cache executor in volatile JexlNode.value
                if (node != null && cache && vg.isCacheable()) {
                    node.jjtSetValue(vg);
                }
                return value;
            } catch (Exception xany) {
                xcause = xany;
            }
        }
        // lets fail
        if (node != null) {
            String attrStr = attribute != null ? attribute.toString() : null;
            return unsolvableProperty(node, attrStr, xcause);
        } else {
            // direct call
            String error = "unable to get object property"
                    + ", class: " + object.getClass().getName()
                    + ", property: " + attribute;
            throw new UnsupportedOperationException(error, xcause);
        }
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object    to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param value     the value to assign to the object's attribute
     */
    public void setAttribute(Object object, Object attribute, Object value) {
        setAttribute(object, attribute, value, null);
    }

    /**
     * Sets an attribute of an object.
     *
     * @param object    to set the value to
     * @param attribute the attribute of the object, e.g. an index (1, 0, 2) or key for a map
     * @param value     the value to assign to the object's attribute
     * @param node      the node that evaluated as the object
     */
    protected void setAttribute(Object object, Object attribute, Object value, JexlNode node) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
        // attempt to reuse last executor cached in volatile JexlNode.value
        if (node != null && cache) {
            Object cached = node.jjtGetValue();
            if (cached instanceof JexlPropertySet) {
                JexlPropertySet setter = (JexlPropertySet) cached;
                Object eval = setter.tryInvoke(object, attribute, value);
                if (!setter.tryFailed(eval)) {
                    return;
                }
            }
        }
        Exception xcause = null;
        List<JexlUberspect.ResolverType> strategy = getPropertyResolvers(node, object);
        JexlPropertySet vs = uberspect.getPropertySet(strategy, object, attribute, value);
        // if we can't find an exact match, narrow the value argument and try again
        if (vs == null) {
            // replace all numbers with the smallest type that will fit
            Object[] narrow = {value};
            if (arithmetic.narrowArguments(narrow)) {
                vs = uberspect.getPropertySet(strategy, object, attribute, narrow[0]);
            }
        }
        if (vs != null) {
            try {
                // cache executor in volatile JexlNode.value
                vs.invoke(object, value);
                if (node != null && cache && vs.isCacheable()) {
                    node.jjtSetValue(vs);
                }
                return;
            } catch (Exception xany) {
                xcause = xany;
            }
        }
        // lets fail
        if (node != null) {
            String attrStr = attribute != null ? attribute.toString() : null;
            unsolvableProperty(node, attrStr, xcause);
        } else {
            // direct call
            String error = "unable to set object property"
                    + ", class: " + object.getClass().getName()
                    + ", property: " + attribute
                    + ", argument: " + value.getClass().getSimpleName();
            throw new UnsupportedOperationException(error, xcause);
        }
    }
}
