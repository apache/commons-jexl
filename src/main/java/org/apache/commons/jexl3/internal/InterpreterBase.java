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
import org.apache.commons.jexl3.parser.ASTArrayAccess;
import org.apache.commons.jexl3.parser.ASTFunctionNode;
import org.apache.commons.jexl3.parser.ASTIdentifier;
import org.apache.commons.jexl3.parser.ASTMethodNode;
import org.apache.commons.jexl3.parser.ASTReference;
import org.apache.commons.jexl3.parser.JexlNode;
import org.apache.commons.jexl3.parser.ParserVisitor;

import java.lang.reflect.InvocationTargetException;

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

    /**
     * Attempt to call close() if supported.
     * <p>This is used when dealing with auto-closeable (duck-like) objects
     * @param closeable the object we'd like to close
     */
    protected void closeIfSupported(Object closeable) {
        if (closeable instanceof AutoCloseable) {
            try {
                ((AutoCloseable)closeable).close();
            } catch (Exception xignore) {
                logger.warn(xignore);
            }
        } else if (closeable != null) {
            JexlMethod mclose = uberspect.getMethod(closeable, "close", EMPTY_PARAMS);
            if (mclose != null) {
                try {
                    mclose.invoke(closeable, EMPTY_PARAMS);
                } catch (Exception xignore) {
                    logger.warn(xignore);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Throwable> void doThrow(Throwable t) throws T {
        throw (T) t;
    }

    protected class ResourceManager implements AutoCloseable {
        protected Object r;

        protected ResourceManager(Object resource) {
            r = resource;
        }

        @Override
        public void close() throws Exception {
            if (r instanceof AutoCloseable) {
                ((AutoCloseable)r).close();
            } else if (r != null) {
                JexlMethod mclose = uberspect.getMethod(r, "close", EMPTY_PARAMS);
                if (mclose != null) {
                    try {
                        mclose.invoke(r, EMPTY_PARAMS);
                    } catch (InvocationTargetException ex) {
                        InterpreterBase.<RuntimeException>doThrow(ex.getCause());
                    }
                }
            }
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

    /**
     * Whether this interpreter is currently evaluating with assertions enabled.
     * @return true if assertions enabled, false otherwise
     */
    protected boolean isAssertions() {
        if (this.context instanceof JexlEngine.Options) {
            JexlEngine.Options opts = (JexlEngine.Options) context;
            Boolean assertions = opts.isAssertions();
            if (assertions != null) {
                return assertions.booleanValue();
            }
        }
        return jexl.isAssertions();
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
     * @param property   the property node
     * @param cause the cause if any
     * @param undef whether the property is undefined or null
     * @return throws JexlException if strict and not silent, null otherwise
     */
    protected Object unsolvableProperty(JexlNode node, String property, boolean undef, Throwable cause) {
        if (isStrictEngine()) {
            throw new JexlException.Property(node, property, undef, cause);
        } else if (logger.isDebugEnabled()) {
            logger.debug(JexlException.propertyError(node, property, undef));
        }
        return null;
    }
    
    /**
     * Pretty-prints a failing property (de)reference.
     * <p>Used by calls to unsolvableProperty(...).</p>
     * @param node the property node
     * @return the (pretty) string
     */
    protected String stringifyProperty(JexlNode node) {
        if (node instanceof ASTArrayAccess) {
            return "["
                    + stringifyPropertyValue(node.jjtGetChild(0))
                    + "]";
        }
        if (node instanceof ASTMethodNode) {
            return stringifyPropertyValue(node.jjtGetChild(0));
        }
        if (node instanceof ASTFunctionNode) {
            return stringifyPropertyValue(node.jjtGetChild(0));
        }
        if (node instanceof ASTIdentifier) {
            return ((ASTIdentifier) node).getName();
        }
        if (node instanceof ASTReference) {
            return stringifyPropertyValue(node.jjtGetChild(0));
        }
        return stringifyPropertyValue(node);
    }
        
    /**
     * Pretty-prints a failing property value (de)reference.
     * <p>Used by calls to unsolvableProperty(...).</p>
     * @param node the property node
     * @return the (pretty) string value
     */
    protected static String stringifyPropertyValue(JexlNode node) {
        return node != null? new Debugger().depth(1).data(node) : "???";
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
            return new JexlException.Cancel(node);
        }
        return new JexlException(node, methodName, xany);
    }

    /**
     * Cancels this evaluation, setting the cancel flag that will result in a JexlException.Cancel to be thrown.
     * @return false if already cancelled, true otherwise
     */
    protected  boolean cancel() {
        return cancelled? false : (cancelled = true);
    }

    /**
     * Checks whether this interpreter execution was cancelled due to thread interruption.
     * @return true if cancelled, false otherwise
     */
    protected boolean isCancelled() {
        return cancelled | Thread.currentThread().isInterrupted();
    }

    /**
     * Throws a JexlException.Cancel if script execution was cancelled.
     * @param node the node being evaluated
     */
    protected void cancelCheck(JexlNode node) {
        if (isCancelled()) {
            throw new JexlException.Cancel(node);
        }
    }
    
    /**
     * Concatenate arguments in call(...).
     * <p>When target == context, we are dealing with a global namespace function call
     * @param target the pseudo-method owner, first to-be argument
     * @param narrow whether we should attempt to narrow number arguments
     * @param args   the other (non null) arguments
     * @return the arguments array
     */
    protected Object[] functionArguments(Object target, boolean narrow, Object[] args) {
        // when target == context, we are dealing with the null namespace
        if (target == null || target == context) {
            if (narrow) {
                arithmetic.narrowArguments(args);
            }
            return args;
        }
        // makes target 1st args, copy others - optionally narrow numbers
        Object[] nargv = new Object[args.length + 1];
        if (narrow) {
            nargv[0] = functionArgument(true, target);
            for (int a = 1; a <= args.length; ++a) {
                nargv[a] = functionArgument(true, args[a - 1]);
            }
        } else {
            nargv[0] = target;
            System.arraycopy(args, 0, nargv, 1, args.length);
        }
        return nargv;
    }

    /**
     * Concatenate arguments in call(...).
     * @param target the pseudo-method owner, first to-be argument
     * @param narrow whether we should attempt to narrow number arguments
     * @param args   the other (non null) arguments
     * @return the arguments array
     */
    protected Object[] callArguments(Object target, boolean narrow, Object[] args) {
        // makes target 1st args, copy others - optionally narrow numbers
        Object[] nargv = new Object[args.length + 1];
        if (narrow) {
            nargv[0] = functionArgument(true, target);
            for (int a = 1; a <= args.length; ++a) {
                nargv[a] = functionArgument(true, args[a - 1]);
            }
        } else {
            nargv[0] = target;
            System.arraycopy(args, 0, nargv, 1, args.length);
        }
        return nargv;
    }
    
    /**
     * Optionally narrows an argument for a function call.
     * @param narrow whether narrowing should occur
     * @param arg    the argument
     * @return the narrowed argument
     */
    protected Object functionArgument(boolean narrow, Object arg) {
        return narrow && arg instanceof Number ? arithmetic.narrow((Number) arg) : arg;
    }

    /**
     * Cached function call.
     */
    protected static class Funcall implements JexlNode.Funcall {
        /** Whether narrow should be applied to arguments. */
        protected final boolean narrow;
        /** The JexlMethod to delegate the call to. */
        protected final JexlMethod me;
        /**
         * Constructor.
         * @param jme  the method
         * @param flag the narrow flag
         */
        protected Funcall(JexlMethod jme, boolean flag) {
            this.me = jme;
            this.narrow = flag;
        }

        /**
         * Try invocation.
         * @param ii     the interpreter
         * @param name   the method name
         * @param target the method target
         * @param args   the method arguments
         * @return the method invocation result (or JexlEngine.TRY_FAILED)
         */
        protected Object tryInvoke(InterpreterBase ii, String name, Object target, Object[] args) throws Exception {
            return me.tryInvoke(name, target, ii.functionArguments(null, narrow, args));
        }
    }

    /**
     * Cached arithmetic function call.
     */
    protected static class ArithmeticFuncall extends Funcall {
        /**
         * Constructor.
         * @param jme  the method
         * @param flag the narrow flag
         */
        protected ArithmeticFuncall(JexlMethod jme, boolean flag) {
            super(jme, flag);
        }

        @Override
        protected Object tryInvoke(InterpreterBase ii, String name, Object target, Object[] args) throws Exception {
            return me.tryInvoke(name, ii.arithmetic, ii.functionArguments(target, narrow, args));
        }
    }

    /**
     * Cached context function call.
     */
    protected static class ContextFuncall extends Funcall {
        /**
         * Constructor.
         * @param jme  the method
         * @param flag the narrow flag
         */
        protected ContextFuncall(JexlMethod jme, boolean flag) {
            super(jme, flag);
        }

        @Override
        protected Object tryInvoke(InterpreterBase ii, String name, Object target, Object[] args) throws Exception {
            return me.tryInvoke(name, ii.context, ii.functionArguments(target, narrow, args));
        }
    }
    
    /**
     * A ctor that needs a context as 1st argument.
     */
    protected static class ContextualCtor extends Funcall {
        /**
         * Constructor.
         * @param jme the method
         * @param flag the narrow flag
         */
        protected ContextualCtor(JexlMethod jme, boolean flag) {
            super(jme, flag);
        }

        @Override
        protected Object tryInvoke(InterpreterBase ii, String name, Object target, Object[] args) throws Exception {
            return me.tryInvoke(name, target, ii.callArguments(ii.context, narrow, args));
        }
    }
    
    /**
     * Helping dispatch function calls.
     */
    protected class CallDispatcher {
        /**
         * The syntactic node.
         */
        final JexlNode node;
        /**
         * Whether solution is cacheable.
         */
        boolean cacheable = true;
        /**
         * Whether arguments have been narrowed.
         */
        boolean narrow = false;
        /**
         * The method to call.
         */
        JexlMethod vm = null;
        /**
         * The method invocation target.
         */
        Object target = null;
        /**
         * The actual arguments.
         */
        Object[] argv = null;
        /**
         * The cacheable funcall if any.
         */
        Funcall funcall = null;

        /**
         * Dispatcher ctor.
         *
         * @param anode the syntactic node.
         * @param acacheable whether resolution can be cached
         */
        CallDispatcher(JexlNode anode, boolean acacheable) {
            this.node = anode;
            this.cacheable = acacheable;
        }

        /**
         * Whether the method is a target method.
         *
         * @param ntarget the target instance
         * @param mname the method name
         * @param arguments the method arguments
         * @return true if arithmetic, false otherwise
         */
        protected boolean isTargetMethod(Object ntarget, String mname, final Object[] arguments) {
            // try a method
            vm = uberspect.getMethod(ntarget, mname, arguments);
            if (vm != null) {
                argv = arguments;
                target = ntarget;
                if (cacheable && vm.isCacheable()) {
                    funcall = new Funcall(vm, narrow);
                }
                return true;
            }
            return false;
        }

        /**
         * Whether the method is a context method.
         *
         * @param mname the method name
         * @param arguments the method arguments
         * @return true if arithmetic, false otherwise
         */
        protected boolean isContextMethod(String mname, final Object[] arguments) {
            vm = uberspect.getMethod(context, mname, arguments);
            if (vm != null) {
                argv = arguments;
                target = context;
                if (cacheable && vm.isCacheable()) {
                    funcall = new ContextFuncall(vm, narrow);
                }
                return true;
            }
            return false;
        }

        /**
         * Whether the method is an arithmetic method.
         *
         * @param mname the method name
         * @param arguments the method arguments
         * @return true if arithmetic, false otherwise
         */
        protected boolean isArithmeticMethod(String mname, final Object[] arguments) {
            vm = uberspect.getMethod(arithmetic, mname, arguments);
            if (vm != null) {
                argv = arguments;
                target = arithmetic;
                if (cacheable && vm.isCacheable()) {
                    funcall = new ArithmeticFuncall(vm, narrow);
                }
                return true;
            }
            return false;
        }

        /**
         * Attempt to reuse last funcall cached in volatile JexlNode.value (if
         * it was cacheable).
         *
         * @param ntarget the target instance
         * @param mname the method name
         * @param arguments the method arguments
         * @return TRY_FAILED if invocation was not possible or failed, the
         * result otherwise
         */
        protected Object tryEval(final Object ntarget, final String mname, final Object[] arguments) throws Exception {
            // do we have  a method/function name ?
            // attempt to reuse last funcall cached in volatile JexlNode.value (if it was not a variable)
            if (mname != null && cacheable && ntarget != null) {
                Object cached = node.jjtGetValue();
                if (cached instanceof Funcall) {
                    return ((Funcall) cached).tryInvoke(InterpreterBase.this, mname, ntarget, arguments);
                }
            }
            return JexlEngine.TRY_FAILED;
        }

        /**
         * Evaluates the method previously dispatched.
         *
         * @param mname the method name
         * @return the method invocation result
         * @throws Exception when invocation fails
         */
        protected Object eval(String mname) throws Exception {
            // we have either evaluated and returned or might have found a method
            if (vm != null) {
                // vm cannot be null if xjexl is null
                Object eval = vm.invoke(target, argv);
                // cache executor in volatile JexlNode.value
                if (funcall != null) {
                    node.jjtSetValue(funcall);
                }
                return eval;
            }
            return unsolvableMethod(node, mname);
        }
    }

}
