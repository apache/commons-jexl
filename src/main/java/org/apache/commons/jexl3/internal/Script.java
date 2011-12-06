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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.parser.ASTJexlScript;

/**
 * <p>A JexlScript implementation.</p>
 * @since 1.1
 */
public class Script implements JexlScript {
    /** The engine for this expression. */
    protected final Engine jexl;
    /**
     * Original expression stripped from leading & trailing spaces.
     */
    protected final String expression;
    /**
     * The resulting AST we can interpret.
     */
    protected final ASTJexlScript script;

    /**
     * Do not let this be generally instantiated with a 'new'.
     *
     * @param engine the interpreter to evaluate the expression
     * @param expr the expression.
     * @param ref the parsed expression.
     */
    protected Script(Engine engine, String expr, ASTJexlScript ref) {
        jexl = engine;
        expression = expr;
        script = ref;
    }

    @Override
    public Object evaluate(JexlContext context) {
        if (script.jjtGetNumChildren() < 1) {
            return null;
        }
        Engine.Frame frame = script.createFrame((Object[]) null);
        Interpreter interpreter = jexl.createInterpreter(context, frame);
        return interpreter.interpret(script.jjtGetChild(0));
    }

    @Override
    public String dump() {
        Debugger debug = new Debugger();
        boolean d = debug.debug(script);
        return debug.data() + (d ? " /*" + debug.start() + ":" + debug.end() + "*/" : "/*?:?*/ ");
    }

    @Override
    public String getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        String expr = getExpression();
        return expr == null ? "" : expr;
    }

    /**
     * Provide a string representation of this expression.
     * @return the expression or blank if it's null.
     */
    @Override
    public String getText() {
        return toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(JexlContext context) {
        Engine.Frame frame = script.createFrame((Object[]) null);
        Interpreter interpreter = jexl.createInterpreter(context, frame);
        return interpreter.interpret(script);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(JexlContext context, Object... args) {
        Engine.Frame frame = script.createFrame(args);
        Interpreter interpreter = jexl.createInterpreter(context, frame);
        return interpreter.interpret(script);
    }

    /**
     * Gets this script parameters.
     * @return the parameters or null
     * @since 3.0
     */
    @Override
    public String[] getParameters() {
        return script.getParameters();
    }

    /**
     * Gets this script local variables.
     * @return the local variables or null
     */
    @Override
    public String[] getLocalVariables() {
        return script.getLocalVariables();
    }

    /**
     * Gets this script variables.
     * <p>Note that since variables can be in an ant-ish form (ie foo.bar.quux), each variable is returned as 
     * a list of strings where each entry is a fragment of the variable ({"foo", "bar", "quux"} in the example.</p>
     * @return the variables or null
     */
    @Override
    public Set<List<String>> getVariables() {
        return jexl.getVariables(script);
    }

    /**
     * Creates a Callable from this script.
     * <p>This allows to submit it to an executor pool and provides support for asynchronous calls.</p>
     * <p>The interpreter will handle interruption/cancellation gracefully if needed.</p>
     * @param context the context
     * @return the callable
     */
    @Override
    public Callable<Object> callable(JexlContext context) {
        return callable(context, (Object[]) null);
    }

    /**
     * Creates a Callable from this script.
     * <p>This allows to submit it to an executor pool and provides support for asynchronous calls.</p>
     * <p>The interpreter will handle interruption/cancellation gracefully if needed.</p>
     * @param context the context
     * @param args the script arguments
     * @return the callable
     */
    @Override
    public Callable<Object> callable(JexlContext context, Object... args) {
        final Interpreter interpreter = jexl.createInterpreter(context, script.createFrame(args));
        return new Callable<Object>() {
            /** Use interpreter as marker for not having run. */
            private Object result = interpreter;
            @Override
            public Object call() throws Exception {
                if (result == interpreter) {
                    result = interpreter.interpret(script);
                }
                return result;
            }
        };
    }
}