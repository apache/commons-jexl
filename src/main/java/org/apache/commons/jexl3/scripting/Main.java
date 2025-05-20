/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jexl3.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Test application for JexlScriptEngine (JSR-223 implementation).
 *
 * @since 2.0
 */
public class Main {

    /** Default constructor */
    public Main() { } // Keep Javadoc happy

    /**
     * Test application for JexlScriptEngine (JSR-223 implementation).
     * <p>
     * If a single argument is present, it is treated as a file name of a JEXL
     * script to be evaluated. Any exceptions terminate the application.
     * </p>
     * Otherwise, lines are read from standard input and evaluated.
     * ScriptExceptions are logged, and do not cause the application to exit.
     * This is done so that interactive testing is easier.
     * The line //q! ends the loop.
     *
     * @param args (optional) file name to evaluate. Stored in the args variable.
     * @throws Exception if parsing or IO fail
     */
    public static void main(final String[] args) throws Exception {
        try(BufferedReader in = args.length == 1? read(Paths.get(args[0])) : read(null);
            PrintWriter out =  new PrintWriter(
                new OutputStreamWriter(System.out, Charset.defaultCharset()),true)) {
            run(in, out, args);
        }
    }

    static void run(final BufferedReader in, final PrintWriter out, final Object[] args) throws Exception {
        final JexlScriptEngineFactory fac = new JexlScriptEngineFactory();
        final ScriptEngine engine = fac.getScriptEngine();
        if (args != null && args.length > 0) {
            engine.put("args", args);
            final Object value = engine.eval(in);
            out.println(">>: " + value);
        } else {
            String line;
            out.print("> ");
            while (null != (line = in.readLine())) {
                if ("//q!".equals(line)) {
                    break;
                }
                try {
                    final Object value = engine.eval(line);
                    out.println(">> " + value);
                } catch (final ScriptException e) {
                    out.println("!!>" + e.getLocalizedMessage());
                }
                out.print("> ");
            }
        }
    }

    /**
     * Reads an input.
     *
     * @param path the file path or null for stdin
     * @return the reader
     * @throws IOException if anything goes wrong
     */
    static BufferedReader read(final Path path) throws IOException {
        return new BufferedReader(new InputStreamReader(path == null
            ? System.in
            : Files.newInputStream(path), Charset.defaultCharset()));
    }
}
