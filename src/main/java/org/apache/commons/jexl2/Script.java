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
package org.apache.commons.jexl2;

/**
 * <p>A JEXL Script.</p>
 * <p>A script is some valid JEXL syntax to be executed with
 * a given set of {@link JexlContext} variabless.</p>
 * <p>A script is a group of statements, separated by semicolons.</p>
 * <p>The statements can be <code>blocks</code> (curly braces containing code),
 * Control statements such as <code>if</code> and <code>while</code>
 * as well as expressions and assignment statements.</p>
 *  
 * @since 1.1
 */
public interface Script {
    /**
     * Executes the script with the variables contained in the
     * supplied {@link JexlContext}. 
     * 
     * @param context A JexlContext containing variables.
     * @return The result of this script, usually the result of 
     *      the last statement.
     */
    Object execute(JexlContext context);
    /**
     * Executes the script with the variables contained in the
     * supplied {@link JexlContext} and a set of arguments corresponding to the
     * parameters used during parsing.
     * 
     * @param context A JexlContext containing variables.
     * @param args the arguments
     * @return The result of this script, usually the result of 
     *      the last statement.
     */
    Object execute(JexlContext context, Object... args);

    /**
     * Returns the text of this Script.
     * @return The script to be executed.
     */
    String getText();

}
