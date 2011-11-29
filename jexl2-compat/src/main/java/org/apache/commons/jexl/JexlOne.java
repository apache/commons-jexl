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

import java.io.File;
import java.net.URL;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.Interpreter;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.jexl2.parser.JexlNode;
import org.apache.commons.jexl2.parser.ASTJexlScript;

/**
 * This implements Jexl-1.x (Jelly) compatible behaviors on top of Jexl-2.0.
 * @since 2.0
 * @version $Id$
 */
public class JexlOne {
    /**
     * Default cache size.
     */
    private static final int CACHE_SIZE = 256;

    /**
     * Private constructor, ensure no instance.
     */
    protected JexlOne() {}


    /**
     * Lazy JexlEngine singleton through on demand holder idiom.
     */
    private static final class EngineHolder {
        /** The shared instance. */
        static final JexlOneEngine JEXL10 = new JexlOneEngine();
        /**
         * Non-instantiable.
         */
        private EngineHolder() {}
    }


    /**
     * A Jexl1.x context wrapped into a Jexl2 context.
     */
    private static final class ContextAdapter implements org.apache.commons.jexl2.JexlContext {
        /** The Jexl1.x context. */
        private final JexlContext legacy;

        /**
         * Creates a jexl2.JexlContext from a jexl.JexlContext.
         * @param ctxt10
         */
        ContextAdapter(JexlContext ctxt10) {
            legacy = ctxt10;
        }

        /**
         * {@inheritDoc}
         */
        public Object get(String name) {
            return legacy.getVars().get(name);
        }

        /**
         * {@inheritDoc}
         */
        public void set(String name, Object value) {
            legacy.getVars().put(name, value);
        }

        /**
         * {@inheritDoc}
         */
        public boolean has(String name) {
            return legacy.getVars().containsKey(name);
        }

        /**
         * Adapts a Jexl-1.x context to a Jexl-2.0 context.
         * @param aContext a oac.jexl context
         * @return an oac.jexl2 context
         */
        static final org.apache.commons.jexl2.JexlContext adapt(JexlContext aContext) {
            return aContext == null ? JexlOneEngine.EMPTY_CONTEXT : new ContextAdapter(aContext);
        }
    }


    /**
     * An interpreter made compatible with v1.1 behavior (at least Jelly's expectations).
     */
    private static final class JexlOneInterpreter extends Interpreter {
        /**
         * Creates an instance.
         * @param jexl the jexl engine
         * @param aContext the jexl context
         */
        public JexlOneInterpreter(JexlEngine jexl, JexlContext aContext) {
            super(jexl, ContextAdapter.adapt(aContext), false, false);
        }

        /**{@inheritDoc}*/
        @Override
        public Object interpret(JexlNode node) {
            try {
                return node.jjtAccept(this, null);
            } catch (JexlException xjexl) {
                Throwable e = xjexl.getCause();
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

        /**{@inheritDoc}*/
        @Override
        protected Object invocationFailed(JexlException xjexl) {
            throw xjexl;
        }

        /**{@inheritDoc}*/
        @Override
        protected Object unknownVariable(JexlException xjexl) {
            return null;
        }
    }

    
    /**
     * An engine that uses a JexlOneInterpreter.
     */
    private static final class JexlOneEngine extends JexlEngine {
        /**
         * Default ctor, creates a cache and sets instance to verbose (ie non-silent).
         */
        private JexlOneEngine() {
            super();
            setCache(CACHE_SIZE);
            setSilent(false);
        }

        /**{@inheritDoc}*/
        protected Interpreter createInterpreter(JexlContext context) {
            return new JexlOneInterpreter(this, context);
        }

        /** {@inheritDoc} */
        @Override
        protected Script createScript(ASTJexlScript tree, String text) {
            return new JexlOneExpression(this, text, tree);
        }

        /** {@inheritDoc} */
        @Override
        protected Expression createExpression(ASTJexlScript tree, String text) {
            return new JexlOneExpression(this, text, tree);
        }
    }

    /**
     * The specific Jexl-1.x expressions implementation.
     */
    private static final class JexlOneExpression
            extends org.apache.commons.jexl2.ExpressionImpl
            implements Expression, Script {
        /**
         * Default local ctor.
         *
         * @param engine the interpreter to evaluate the expression
         * @param expr the expression.
         * @param ref the parsed expression.
         */
        private JexlOneExpression(JexlOne.JexlOneEngine engine, String expr, ASTJexlScript ref) {
            super(engine, expr, ref);
        }

        /**
         * {@inheritDoc}
         */
        public Object evaluate(JexlContext context) {
            return super.evaluate(ContextAdapter.adapt(context));
        }

        /**
         * {@inheritDoc}
         */
        public Object execute(JexlContext context) {
            return super.execute(ContextAdapter.adapt(context));
        }
    }


    /**
     * Creates a Script from a String containing valid JEXL syntax.
     * This method parses the script which validates the syntax.
     *
     * @param scriptText A String containing valid JEXL syntax
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws Exception An exception can be thrown if there is a
     *      problem parsing the script.
     * @deprecated Create a JexlEngine and use the createScript method on that instead.
     */
    @Deprecated
    public static Script createScript(String scriptText) throws Exception {
        return (Script) EngineHolder.JEXL10.createScript(scriptText);
    }

    /**
     * Creates a Script from a {@link File} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptFile A {@link File} containing valid JEXL syntax.
     *      Must not be null. Must be a readable file.
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws Exception An exception can be thrown if there is a problem
     *      parsing the script.
     * @deprecated Create a JexlEngine and use the createScript method on that instead.
     */
    @Deprecated
    public static Script createScript(File scriptFile) throws Exception {
        return (Script) EngineHolder.JEXL10.createScript(scriptFile);
    }

    /**
     * Creates a Script from a {@link URL} containing valid JEXL syntax.
     * This method parses the script and validates the syntax.
     *
     * @param scriptUrl A {@link URL} containing valid JEXL syntax.
     *      Must not be null. Must be a readable file.
     * @return A {@link Script} which can be executed with a
     *      {@link JexlContext}.
     * @throws Exception An exception can be thrown if there is a problem
     *      parsing the script.
     * @deprecated Create a JexlEngine and use the createScript method on that instead.
     */
    @Deprecated
    public static Script createScript(URL scriptUrl) throws Exception {
        return (Script) EngineHolder.JEXL10.createScript(scriptUrl);
    }

    /**
     * Creates an Expression from a String containing valid
     * JEXL syntax.  This method parses the expression which
     * must contain either a reference or an expression.
     * @param expression A String containing valid JEXL syntax
     * @return An Expression object which can be evaluated with a JexlContext
     * @throws JexlException An exception can be thrown if there is a problem
     *      parsing this expression, or if the expression is neither an
     *      expression or a reference.
     * @deprecated Create a JexlEngine and use createExpression() on that
     */
    @Deprecated
    public static Expression createExpression(String expression) {
        return (Expression) EngineHolder.JEXL10.createExpression(expression);
    }
}
