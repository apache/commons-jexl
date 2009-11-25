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

package org.apache.commons.jexl2.scripting;

import java.io.PrintStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements variables and methods for use by JEXL scripts.
 * <p>
 * The following items are defined:
 * <ul>
 * <li>out - System.out</li>
 * <li>err - System.err</li>
 * <li>logger - a logger</li>
 * <li>System - System.class</li>
 * <li></li>
 * </ul>
 * </p>
 * @since 2.0
 */
public class JexlScriptObject {

    /**
     * Default constructor.
     */
    public JexlScriptObject(){
    }

    /**
     * Gives access to System.out.
     * 
     * @return System.out
     */
    public static PrintStream getOut() {
        return System.out;
    }

    /**
     * Gives access to System.err.
     * 
     * @return System.err
     */
    public static PrintStream getErr() {
        return System.err;
    }

    /**
     * Gives access to System class.
     * 
     * @return System.class
     */
    public static Class<System> getSystem() {
        return System.class;
    }

    /**
     * Gives access to a logger.
     * 
     * @return a logger using the class JexlScriptEngine
     */
    public static Log getLogger(){
        return LogFactory.getLog(JexlScriptEngine.class); // TODO is this the correct class?
    }
}
