/*
 * Copyright 2002-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

/**
 * <p>A JEXL Script.</p>
 * <p>A script is some valid JEXL syntax to be executed with
 * a given set of {@link JexlContext variables}.</p>
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
     * @throws Exception on any script parse or execution error.
     */
    Object execute(JexlContext context) throws Exception;

    /**
     * Returns the text of this Script.
     * @return The script to be executed.
     */
    String getText();

}
