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
 * 
 */

package org.apache.commons.jexl2.scripting;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.script.Compilable;
import javax.script.CompiledScript;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import junit.framework.TestCase;

public class JexlScriptEngineOptionalTest extends TestCase {
    private final JexlScriptEngineFactory factory = new JexlScriptEngineFactory();
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final ScriptEngine engine = manager.getEngineByName("jexl");

    public void testScriptEngineOutput() throws Exception {
        String output = factory.getOutputStatement("foo\u00a9bar");
        assertEquals(output, "print('foo\\u00a9bar')");
        // redirect output to capture evaluation result
        final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        try {
            engine.eval(output);
        } finally {
            System.setOut(null);
        }
        assertEquals("foo\u00a9bar\n", outContent.toString());
    }

    public void testCompilable() throws Exception {
        assertTrue("Engine should implement Compilable", engine instanceof Compilable);
        Compilable cengine = (Compilable) engine;
        CompiledScript script = cengine.compile("40 + 2");
        assertEquals(Integer.valueOf(42), script.eval());
        assertEquals(Integer.valueOf(42), script.eval());
    }

}
