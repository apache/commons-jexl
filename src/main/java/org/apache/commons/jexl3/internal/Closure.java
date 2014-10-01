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
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.JexlNode;

import java.util.concurrent.Callable;

/**
 * A Script closure.
 */
public class Closure extends Script {
    /** The frame. */
    protected final Scope.Frame frame;

    /**
     * Creates a closure.
     * @param theCaller the calling interpreter
     * @param lambda the lambda
     */
    protected Closure(Interpreter theCaller, ASTJexlLambda lambda) {
        super(theCaller.jexl, null, lambda);
        frame = lambda.createFrame(theCaller.frame);
    }

    @Override
    public String toString() {
        return getParsedText();
    }

    @Override
    public String getParsedText() {
        Debugger debug = new Debugger();
        debug.debug(script, false);
        return debug.toString();
    }

    /**
     * Sets the hoisted index of a given symbol, ie the target index of a parent hoisted symbol in this closure's frame.
     * <p>This is meant to allow a locally defined function to "see" and call itself as a local (hoisted) variable;
     * in other words, this allows recursive call of a function.
     * @param symbol the symbol index (in the caller of this closure)
     * @param value the value to set in the local frame
     */
    public void setHoisted(int symbol, Object value) {
        if (script instanceof ASTJexlLambda) {
            ASTJexlLambda lambda = (ASTJexlLambda) script;
            Scope scope = lambda.getScope();
            if (scope != null) {
                Integer reg = scope.getHoisted(symbol);
                if (reg != null) {
                    frame.set(reg, value);
                }
            }
        }
    }

    @Override
    public Object evaluate(JexlContext context) {
        return execute(context, (Object[])null);
    }

    @Override
    public Object execute(JexlContext context) {
        return execute(context, (Object[])null);
    }

    @Override
    public Object execute(JexlContext context, Object... args) {
        Scope.Frame callFrame = null;
        if (frame != null) {
            callFrame = frame.assign(args);
        }
        Interpreter interpreter = jexl.createInterpreter(context, callFrame);
        JexlNode block = script.jjtGetChild(script.jjtGetNumChildren() - 1);
        return interpreter.interpret(block);
    }

    @Override
    public Callable<Object> callable(JexlContext context, Object... args) {
        Scope.Frame local = null;
        if (frame != null) {
            local = frame.assign(args);
        }
        final Interpreter interpreter = jexl.createInterpreter(context, local);
        return new Callable<Object>() {
            /** Use interpreter as marker for not having run. */
            private Object result = interpreter;

            @Override
            public Object call() throws Exception {
                if (result == interpreter) {
                    JexlNode block = script.jjtGetChild(script.jjtGetNumChildren() - 1);
                    result = interpreter.interpret(block);
                }
                return result;
            }
        };
    }

}
