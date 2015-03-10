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

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.parser.ASTJexlScript;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * <p>A JexlScript implementation.</p>
 * @since 1.1
 */
public class Script implements JexlScript, JexlExpression {
    /**
     * The engine for this expression.
     */
    protected final Engine jexl;
    /**
     * Original expression stripped from leading & trailing spaces.
     */
    protected final String source;
    /**
     * The resulting AST we can interpret.
     */
    protected final ASTJexlScript script;
    /**
     * The engine version (as class loader change count) that last evaluated this script.
     */
    protected int version;

    /**
     * Do not let this be generally instantiated with a 'new'.
     *
     * @param engine the interpreter to evaluate the expression
     * @param expr   the expression source.
     * @param ref    the parsed expression.
     */
    protected Script(Engine engine, String expr, ASTJexlScript ref) {
        jexl = engine;
        source = expr;
        script = ref;
        version = jexl.getUberspect().getVersion();
    }

    /**
     * Checks that this script cached methods (wrt introspection) matches the engine version.
     * <p>
     * If the engine class loader has changed since we last evaluated this script, the script local cache
     * is invalidated to drop references to obsolete methods. It is not strictly necessary since the tryExecute
     * will fail because the class wont match but it seems cleaner nevertheless.
     * </p>
     */
    protected void checkCacheVersion() {
        int uberVersion = jexl.getUberspect().getVersion();
        if (version != uberVersion) {
            script.clearCache();
            version = uberVersion;
        }
    }

    @Override
    public Object evaluate(JexlContext context) {
        if (script.jjtGetNumChildren() < 1) {
            return null;
        }
        checkCacheVersion();
        Scope.Frame frame = script.createFrame((Object[]) null);
        Interpreter interpreter = jexl.createInterpreter(context, frame);
        return interpreter.interpret(script.jjtGetChild(0));
    }

    /**
     * Gets this script original script source.
     * @return the contents of the input source as a String.
     */
    @Override
    public String getSourceText() {
        return source;
    }

    /**
     * Gets a string representation of this script underlying AST.
     * @return the script as text
     */
    @Override
    public String getParsedText() {
        Debugger debug = new Debugger();
        debug.debug(script);
        return debug.toString();
    }

    @Override
    public String toString() {
        CharSequence src = source;
        if (src == null) {
            Debugger debug = new Debugger();
            debug.debug(script);
            src = debug.toString();
        }
        return src == null ? "/*no source*/" : src.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(JexlContext context) {
        checkCacheVersion();
        Scope.Frame frame = script.createFrame((Object[]) null);
        Interpreter interpreter = jexl.createInterpreter(context, frame);
        return interpreter.interpret(script);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(JexlContext context, Object... args) {
        checkCacheVersion();
        Scope.Frame frame = script.createFrame(args != null && args.length > 0 ? args : null);
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
     * Get this script pragmas
     * <p>Pragma keys are ant-ish variables, their values are scalar literals..
     * @return the pragmas
     */
    @Override
    public Map<String, Object> getPragmas() {
        return script.getPragmas();
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
     * @param args    the script arguments
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
                    checkCacheVersion();
                    result = interpreter.interpret(script);
                }
                return result;
            }
        };
    }
}