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
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.parser.ASTJexlScript;

import java.lang.reflect.Array;
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
     * Original expression stripped from leading and trailing spaces.
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
     * @return the script AST
     */
    protected ASTJexlScript getScript() {
        return script;
    }

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
            // version 0 of the uberSpect is an illusion due to order of construction; no need to clear cache
            if (version > 0) {
                script.clearCache();
            }
            version = uberVersion;
        }
    }

    /**
     * Creates this script frame for evaluation.
     * @param args the arguments to bind to parameters
     * @return the frame (may be null)
     */
    protected Scope.Frame createFrame(Object[] args) {
        return script.createFrame(args);
    }

    /**
     * Creates this script interpreter.
     * @param context the context
     * @param frame the calling frame
     * @return  the interpreter
     */
    protected Interpreter createInterpreter(JexlContext context, Scope.Frame frame) {
        return jexl.createInterpreter(context, frame);
    }

    /**
     * @return the engine that created this script
     */
    public JexlEngine getEngine() {
        return jexl;
    }

    @Override
    public String getSourceText() {
        return source;
    }

    @Override
    public String getParsedText() {
        return getParsedText(2);
    }

    @Override
    public String getParsedText(int indent) {
        Debugger debug = new Debugger();
        debug.setIndentation(indent);
        debug.debug(script, false);
        return debug.toString();
    }

    @Override
    public String toString() {
        CharSequence src = source;
        if (src == null) {
            Debugger debug = new Debugger();
            debug.debug(script, false);
            src = debug.toString();
        }
        return src.toString();
    }

    @Override
    public int hashCode() {
        // CSOFF: Magic number
        int hash = 17;
        hash = 31 * hash + (this.jexl != null ? this.jexl.hashCode() : 0);
        hash = 31 * hash + (this.source != null ? this.source.hashCode() : 0);
        return hash;
        // CSON: Magic number
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Script other = (Script) obj;
        if (this.jexl != other.jexl) {
            return false;
        }
        if ((this.source == null) ? (other.source != null) : !this.source.equals(other.source)) {
            return false;
        }
        return true;
    }

    @Override
    public Object evaluate(JexlContext context) {
        return execute(context);
    }

    @Override
    public Object execute(JexlContext context) {
        checkCacheVersion();
        Scope.Frame frame = createFrame(null);
        Interpreter interpreter = createInterpreter(context, frame);
        return interpreter.interpret(script);
    }

    @Override
    public Object execute(JexlContext context, Object... args) {
        checkCacheVersion();
        Scope.Frame frame = createFrame(scriptArgs(args));
        Interpreter interpreter = createInterpreter(context, frame);
        return interpreter.interpret(script);
    }

    protected boolean isArray(Object o) {
        return o != null ? (o.getClass().isArray()) : false;
    }

    protected boolean isArrayOf(Object o, Class type) {
        return o != null ? (o.getClass().isArray() && o.getClass().getComponentType() == type) : false;
    }

    protected Object castArray(Object arr, Class type) {
        return castArray(arr, type, 0);
    }

    protected Object castArray(Object arr, Class type, int from) {
        JexlArithmetic arithmetic = jexl.getArithmetic();
        // Process via untyped array to cover primitive type arrays
        int len = Array.getLength(arr);
        Object varg = Array.newInstance(type, len - from);
        for (int i = 0; i < len - from; i++) {
            Object arg = Array.get(arr, i + from);
            if (!arithmetic.getWrapperClass(type).isInstance(arg)) {
                if (arithmetic.isStrict()) {
                    arg = arithmetic.implicitCast(type, arg);
                } else {
                    arg = arithmetic.cast(type, arg);
                }
                if (type.isPrimitive() && arg == null)
                    throw new JexlException(script, "not null value required");
            }
            Array.set(varg, i, arg);
        }
        return varg;
    }

    /**
     * Prepares arguments list with regard to type-casting and vararg option.
     * @param args the passed arguments list
     * @return the script parameter list 
     */
    protected Object[] scriptArgs(Object[] args) {
        return args != null && args.length > 0 ? scriptArgs(0, args) : args;
    }

    /**
     * Prepares arguments list with regard to type-casting and vararg option.
     * @param curried the number of arguments that are already curried for the script
     * @param args the passed arguments list
     * @return the script parameter list 
     */
    protected Object[] scriptArgs(int curried, Object[] args) {

        String[] params = getParameters();
        boolean varArgs = isVarArgs();
        Scope frame = script.getScope();
        JexlArithmetic arithmetic = jexl.getArithmetic();

        Object[] result = null;

        // remaining uncurried arguments count
        int argCount = script.getArgCount() - curried;

        if (varArgs && args != null && args.length > 0 && args.length >= argCount) {
            // Class type of the last argument if any
            String name = params[params.length - 1];
            int symbol = frame.getSymbol(name);
            Class type = frame.getVariableType(symbol);
            if (type == null)
                type = Object.class;
            if (argCount > 0) {
                result = new Object[argCount];
                System.arraycopy(args, 0, result, 0, argCount - 1);
                // The number of passed arguments that should be wrapped to vararg array
                int varArgCount = args.length - argCount + 1;
                // Create last vararg parameter, cast elements to the specified type if needed
                Object varg = null;

                // Check if the only passed vararg argument is already an array, reuse it if possible
                if (varArgCount == 1) {
                    if (isArrayOf(args[args.length-1], type)) {
                        varg = args[args.length-1];
                    } else if (isArray(args[args.length-1])) {
                        varg = castArray(args[args.length-1], type);
                    }
                } 

                if (varg == null) {
                    varg = castArray(args, type, argCount - 1);
                }
                result[argCount-1] = varg;
            } else {
                // All arguments have already been curried, merge into one-element array
                Object varg = null;
                result = new Object[1];

                if (args.length == 1) {
                    if (isArrayOf(args[0], type)) {
                        varg = args[0];
                    } else if (isArray(args[0])) {
                        varg = castArray(args[0], type);
                    }
                } 

                if (varg == null) {
                    varg = castArray(args, type);
                }
                result[0] = varg;
            }
        } else if (args != null && args.length > 0 && args.length > argCount) {
            if (argCount == 0) {
                result = InterpreterBase.EMPTY_PARAMS;
            } else {
                result = new Object[argCount];
                System.arraycopy(args, 0, result, 0, argCount);
            }
        } else {
            result = args;
        }

        // Check if type-casting of all the arguments (except the vararg) is needed
        if (result != null && params != null) {
            for (int i = curried; i < params.length - (varArgs ? 1 : 0); i++) {
                int pos = i - curried;
                // Check if the passed arguments list is shorter than the parameters list
                if (pos >= result.length)
                    break;
                String name = params[i];
                int symbol = frame.getSymbol(name);
                Class type = frame.getVariableType(symbol);
                if (type != null) {
                    Object arg = result[pos];
                    if (!arithmetic.getWrapperClass(type).isInstance(arg)) {
                        if (arithmetic.isStrict()) {
                            arg = arithmetic.implicitCast(type, arg);
                        } else {
                            arg = arithmetic.cast(type, arg);
                        }
                        if (type.isPrimitive() && arg == null)
                            throw new JexlException(script, "not null value required for: " + name);
                        result[pos] = arg;
                    }
                }
            }
        }

        // Check required argument
        if (result != null && params != null) {
            for (int i = curried; i < params.length; i++) {
                int pos = i - curried;
                // Check if the passed arguments list is shorter than the parameters list
                Object arg = (pos >= result.length) ? null : result[pos];
                if (arg != null)
                    continue;
                String name = params[i];
                int symbol = frame.getSymbol(name);
                boolean isRequired = frame.isVariableRequired(symbol);
                if (isRequired) {
                    throw new JexlException(script, "not null value required for: " + name);
                }
            }
        }

        return result;
    }

    @Override
    public JexlScript curry(Object... args) {
        String[] parms = getUnboundParameters();
        if (parms == null || parms.length == 0 || args == null || args.length == 0)
            return this;
        return Closure.create(this, args);
    }

    @Override
    public String[] getParameters() {
        return script.getParameters();
    }

    @Override
    public String[] getUnboundParameters() {
        return getParameters();
    }

    /**
     * Returns true if this script support variable argument.
     * @return boolean
     * @since 3.2
     */
    @Override
    public boolean isVarArgs() {
        return script.isVarArgs();
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
    public Callable callable(JexlContext context) {
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
    public Callable callable(JexlContext context, Object... args) {
        return new CallableScript(jexl.createInterpreter(context, script.createFrame(scriptArgs(args))));
    }

    /**
     * Implements the Future and Callable interfaces to help delegation.
     */
    public class CallableScript implements Callable<Object> {
        /** The actual interpreter. */
        protected final Interpreter interpreter;
        /** Use interpreter as marker for not having run. */
        protected volatile Object result;

        /**
         * The base constructor.
         * @param intrprtr the interpreter to use
         */
        protected CallableScript(Interpreter intrprtr) {
            this.interpreter = intrprtr;
            this.result = intrprtr;
        }

        /**
         * Run the interpreter.
         * @return the evaluation result
         */
        protected Object interpret() {
            return interpreter.interpret(script);
        }

        @Override
        public Object call() throws Exception {
            synchronized(this) {
                if (result == interpreter) {
                    checkCacheVersion();
                    result = interpret();
                }
                return result;
            }
        }

        /**
         * Soft cancel the execution.
         * @return true if cancel was successful, false otherwise
         */
        public boolean cancel() {
            return interpreter.cancel();
        }

        /**
         * @return true if evaluation was cancelled, false otherwise
         */
        public boolean isCancelled() {
            return interpreter.isCancelled();
        }

        /**
         * @return true if interruption will throw a JexlException.Cancel, false otherwise
         */
        public boolean isCancellable() {
            return interpreter.isCancellable();
        }
    }

}
