/*
 * Copyright 2002,2004 The Apache Software Foundation.
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

import org.apache.commons.jexl.parser.ASTJexlScript;

/**
 * Simple script implementation.
 * @since 1.1
 */
public class ScriptImpl implements Script {

    /** text of the script */
    private String text;
    /** syntax tree */
    private ASTJexlScript parsedScript;
    
    public ScriptImpl(String scriptText, ASTJexlScript scriptTree) {
        text = scriptText;
        parsedScript = scriptTree;
    }

    /**
     * @see Script#execute(JexlContext)
     */
    public Object execute(JexlContext context) throws Exception {
        // TODO Auto-generated method stub
        return parsedScript.value(context);
    }

    /**
     * @see Script#getText()
     */
    public String getText() {
        return text;
    }

}
