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

import org.apache.commons.jexl.parser.ASTJexlScript;

/**
 * Simple script implementation.
 * @since 1.1
 */
class ScriptImpl implements Script {
    /** The engine for this expression. */
    protected final JexlEngine jexl;
    /** text of the script. */
    private final String text;
    /** syntax tree. */
    private final ASTJexlScript parsedScript;

    /**
     * Create a new Script from the given string and parsed syntax.
     * @param engine the interpreter to evaluate the expression
     * @param scriptText the text of the script.
     * @param scriptTree the parsed script.
     */
    public ScriptImpl(JexlEngine engine, String scriptText, ASTJexlScript scriptTree) {
        text = scriptText;
        parsedScript = scriptTree;
        jexl = engine;
    }

    /** {@inheritDoc} */
    public Object execute(JexlContext context) throws Exception {
        Interpreter interpreter = jexl.createInterpreter(context);
        return interpreter.interpret(parsedScript);
    }

    /** {@inheritDoc} */
    public String getText() {
        return text;
    }

}