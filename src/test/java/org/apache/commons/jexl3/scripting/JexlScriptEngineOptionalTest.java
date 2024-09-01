/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jexl3.scripting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.StringWriter;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.Test;

public class JexlScriptEngineOptionalTest {
    private final JexlScriptEngineFactory factory = new JexlScriptEngineFactory();
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final ScriptEngine engine = manager.getEngineByName("jexl");

    @Test
    public void testCompilable() throws Exception {
        assertInstanceOf(Compilable.class, engine, "Engine should implement Compilable");
        final Compilable cengine = (Compilable) engine;
        final CompiledScript script = cengine.compile("40 + 2");
        assertEquals(42, script.eval());
        assertEquals(42, script.eval());
    }

    @Test
    public void testError() throws Exception {
        final String error = "JEXL.err.print('ERROR')";
        // redirect error to capture evaluation result
        final StringWriter outContent = new StringWriter();
        engine.getContext().setErrorWriter(outContent);
        engine.eval(error);
        assertEquals("ERROR", outContent.toString());
    }

    @Test
    public void testOutput() throws Exception {
        final String output = factory.getOutputStatement("foo\u00a9bar");
        assertEquals("JEXL.out.print('foo\\u00a9bar')", output);
        // redirect output to capture evaluation result
        final StringWriter outContent = new StringWriter();
        engine.getContext().setWriter(outContent);
        engine.eval(output);
        assertEquals("foo\u00a9bar", outContent.toString());
    }
}
