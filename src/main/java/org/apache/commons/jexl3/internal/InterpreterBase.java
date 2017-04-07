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
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlOperator;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParserVisitor;


import org.apache.commons.logging.Log;

/**
 * The helper base of an interpreter of JEXL syntax.
 * @since 3.0
 */
public abstract class InterpreterBase extends ParserVisitor {
    /** The JEXL engine. */
    protected final Engine jexl;
    /** The logger. */
    protected final Log logger;
    /** The uberspect. */
    protected final JexlUberspect uberspect;
    /** The arithmetic handler. */
    protected final JexlArithmetic arithmetic;
    /** The context to store/retrieve variables. */
    protected final JexlContext context;
    /** Cancellation support. */
    protected volatile boolean cancelled = false;
    /** Empty parameters for method matching. */
    protected static final Object[] EMPTY_PARAMS = new Object[0];

    /**
     * Creates an interpreter base.
     * @param engine   the engine creating this interpreter
     * @param aContext the context to evaluate expression
     */
    protected InterpreterBase(Engine engine, JexlContext aContext) {
        this.jexl = engine;
        this.logger = jexl.logger;
        this.uberspect = jexl.uberspect;
        this.context = aContext != null ? aContext : Engine.EMPTY_CONTEXT;
        JexlArithmetic jexla = jexl.arithmetic;
        this.arithmetic = jexla.options(context);
        if (arithmetic != jexla && !arithmetic.getClass().equals(jexla.getClass())) {
            logger.warn("expected arithmetic to be " + jexla.getClass().getSimpleName()
                          + ", got " + arithmetic.getClass().getSimpleName()
            );
        }
    }

    /**
     * Copy constructor.
     * @param ii the base to copy
     * @param jexla the arithmetic instance to use (or null)
     */
    protected InterpreterBase(InterpreterBase ii, JexlArithmetic jexla) {
        jexl = ii.jexl;
        logger = ii.logger;
        uberspect = ii.uberspect;
        context = ii.context;
        arithmetic = ii.arithmetic;
    }


    /** Java7 AutoCloseable interface defined?. */
    protected static final Class<?> AUTOCLOSEABLE;
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
     * Attempt to call close() if supported.
     * <p>This is used when dealing with auto-closeable (duck-like) objects
     * @param closeable the object we'd like to close
     */
    protected void closeIfSupported(Object closeable) {
        if (closeable != null) {
            //if (AUTOCLOSEABLE == null || AUTOCLOSEABLE.isAssignableFrom(closeable.getClass())) {
            JexlMethod mclose = uberspect.getMethod(closeable, "close", EMPTY_PARAMS);
            if (mclose != null) {
                try {
                    mclose.invoke(closeable, EMPTY_PARAMS);
                } catch (Exception xignore) {
                    logger.warn(xignore);
                }
            }
            //}
        }
    }

    /**
     * Whether this interpreter is currently evaluating with a strict engine flag.
     * @return true if strict engine, false otherwise
     */
    protected boolean isStrictEngine() {
        if (this.context instanceof JexlEngine.Options) {
            JexlEngine.Options opts = (JexlEngine.Options) context;
            Boolean strict = opts.isStrict();
            if (strict != null) {
                return strict.booleanValue();
            }
        }
        return jexl.isStrict();
    }

    /**
     * Whether this interpreter is currently evaluating with a silent mode.
     * @return true if silent, false otherwise
     */
    protected boolean isSilent() {
        if (this.context instanceof JexlEngine.Options) {
            JexlEngine.Options opts = (JexlEngine.Options) context;
            Boolean silent = opts.isSilent();
            if (silent != null) {
                return silent.booleanValue();
            }
        }
        return jexl.isSilent();
    }

    /** @return true if interrupt throws a JexlException.Cancel. */
    protected boolean isCancellable() {
        if (this.context instanceof JexlEngine.Options) {
            JexlEngine.Options opts = (JexlEngine.Options) context;
            Boolean ocancellable = opts.isCancellable();
            if (ocancellable != null) {
                return ocancellable.booleanValue();
            }
        }
        return jexl.isCancellable();
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
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object unsolvableVariable(JexlNode node, String var, boolean undef) {
        if (isStrictEngine() && (undef || arithmetic.isStrict())) {
            throw new JexlException.Variable(node, var, undef);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.variableError(node, var, undef));
        }
        return null;
    }

    /**
     * Triggered when a method can not be resolved.
     * @param node   the node where the error originated from
     * @param method the method name
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object unsolvableMethod(JexlNode node, String method) {
        if (isStrictEngine()) {
            throw new JexlException.Method(node, method);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.methodError(node, method));
        }
        return null;
    }

    /**
     * Triggered when a property can not be resolved.
     * @param node  the node where the error originated from
     * @param var   the property name
     * @param cause the cause if any
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object unsolvableProperty(JexlNode node, String var, Throwable cause) {
        if (isStrictEngine()) {
            throw new JexlException.Property(node, var, cause);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.propertyError(node, var), cause);
        }
        return null;
    }

    /**
     * Triggered when an operator fails.
     * @param node     the node where the error originated from
     * @param operator the method name
     * @param cause    the cause of error (if any)
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object operatorError(JexlNode node, JexlOperator operator, Throwable cause) {
        if (isStrictEngine()) {
            throw new JexlException.Operator(node, operator.getOperatorSymbol(), cause);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.operatorError(node, operator.getOperatorSymbol()), cause);
        }
        return null;
    }

    /**
     * Triggered when an annotation processing fails.
     * @param node     the node where the error originated from
     * @param annotation the annotation name
     * @param cause    the cause of error (if any)
     * @return throws a JexlException if strict and not silent, null otherwise
     */
    protected Object annotationError(JexlNode node, String annotation, Throwable cause) {
        if (isStrictEngine()) {
            throw new JexlException.Annotation(node, annotation, cause);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.annotationError(node, annotation), cause);
        }
        return null;
    }

    /**
     * Triggered when method, function or constructor invocation fails with an exception.
     * @param node       the node triggering the exception
     * @param methodName the method/function name
     * @param xany       the cause
     * @return a JexlException that will be thrown
     */
    protected JexlException invocationException(JexlNode node, String methodName, Exception xany) {
        Throwable cause = xany.getCause();
        if (cause instanceof JexlException) {
            return (JexlException) cause;
        }
        if (cause instanceof InterruptedException) {
            cancelled = true;
            return new JexlException.Cancel(node);
        }
        return new JexlException(node, methodName, xany);
    }

    /**
     * Cancels this evaluation, setting the cancel flag that will result in a JexlException.Cancel to be thrown.
     * @return false if already cancelled, true otherwise
     */
    protected synchronized boolean cancel() {
        if (cancelled) {
            return false;
        } else {
            cancelled = true;
            return true;
        }
    }

    /**
     * Checks whether this interpreter execution was cancelled due to thread interruption.
     * @return true if cancelled, false otherwise
     */
    protected synchronized boolean isCancelled() {
        if (!cancelled) {
            cancelled = Thread.currentThread().isInterrupted();
        }
        return cancelled;
    }
}
