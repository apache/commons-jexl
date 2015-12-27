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

package org.apache.commons.jexl3.scripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.charset.Charset;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Test application for JexlScriptEngine (JSR-223 implementation).
 * 
 * @since 2.0
 */
public class Main {

    /**
     * Reads an input.
     * 
     * @param charset the charset or null for default charset
     * @param fileName the file name or null for stdin
     * @return the reader
     * @throws Exception if anything goes wrong
     */
    static BufferedReader read(Charset charset, String fileName) throws Exception {
        return new BufferedReader(
            new InputStreamReader(
                    fileName == null
                        ? System.in
                        : new FileInputStream(new File(fileName)),
                    charset == null
                        ? Charset.defaultCharset()
                        : charset));
    }

    /**
     * Test application for JexlScriptEngine (JSR-223 implementation).
     *
     * If a single argument is present, it is treated as a filename of a JEXL
     * script to be evaluated. Any exceptions terminate the application.
     *
     * Otherwise, lines are read from standard input and evaluated.
     * ScriptExceptions are logged, and do not cause the application to exit.
     * This is done so that interactive testing is easier.
     *
     * @param args (optional) filename to evaluate. Stored in the args variable.
     *
     * @throws Exception if parsing or IO fail
     */
    public static void main(String[] args) throws Exception {
        JexlScriptEngineFactory fac = new JexlScriptEngineFactory();
        ScriptEngine engine = fac.getScriptEngine();
        engine.put("args", args);
        if (args.length == 1){
            Object value = engine.eval(read(null, args[0]));
            System.out.println("Return value: "+value);
        } else {
            BufferedReader console = read(null, null);
            String line;
            System.out.print("> ");
            while(null != (line=console.readLine())){
                try {
                    Object value = engine.eval(line);
                    System.out.println("Return value: "+value);
                } catch (ScriptException e) {
                    System.out.println(e.getLocalizedMessage());
                }
                System.out.print("> ");
            }
        }
    }
}
