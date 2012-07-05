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

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A Script closure.
 */
public class Closure extends Script {
    /** The frame. */
    protected final Scope.Frame frame;
    /** The map of registered functions. */
    protected final Map<String, Object> functors;

    /**
     * Creates a closure.
     * @param theCaller the calling interpreter
     * @param lambda the lambda
     */
    protected Closure(Interpreter theCaller, ASTJexlLambda lambda) {
        super(theCaller.jexl, null, lambda);
        functors = theCaller.functors;
        frame = lambda.createFrame(theCaller.frame);
    }

    @Override
    public String getParsedText() {
        Debugger debug = new Debugger();
        debug.debug(script);
        return debug.toString();
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
        if (frame != null) {
            frame.assign(args);
        }
        Interpreter interpreter = jexl.createInterpreter(context, frame);
        interpreter.functors = functors;
        JexlNode block = script.jjtGetChild(script.jjtGetNumChildren() - 1);
        return interpreter.interpret(block);
    }

    @Override
    public Callable<Object> callable(JexlContext context, Object... args) {
        if (frame != null) {
            frame.assign(args);
        }
        final Interpreter interpreter = jexl.createInterpreter(context, frame);
        interpreter.functors = functors;
        return new Callable<Object>() {
            /** Use interpreter as marker for not having run. */
            private Object result = interpreter;

            @Override
            public Object call() throws Exception {
                if (result == interpreter) {
                    JexlNode block = script.jjtGetChild(script.jjtGetNumChildren() - 1);
                    return interpreter.interpret(block);
                }
                return result;
            }
        };
    }

}
