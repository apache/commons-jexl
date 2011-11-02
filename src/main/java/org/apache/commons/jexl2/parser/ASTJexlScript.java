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
package org.apache.commons.jexl2.parser;

import org.apache.commons.jexl2.JexlEngine;

/**
 * Enhanced script to allow parameters declaration.
 */
public class ASTJexlScript extends JexlNode {
    /** The script scope. */
    private JexlEngine.Scope scope = null;

    public ASTJexlScript(int id) {
        super(id);
    }

    public ASTJexlScript(Parser p, int id) {
        super(p, id);
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Sets the parameters and registers
     * @param theScope the scope
     */
    public void setScope(JexlEngine.Scope theScope) {
        this.scope = theScope;
    }
    
    /**
     * Gets this script scope.
     */
    public JexlEngine.Scope getScope() {
        return scope;
    }
    
    /**
     * Creates an array of arguments by copying values up to the number of parameters.
     * @param values the argument values
     * @return the arguments array
     */
    public JexlEngine.Frame createFrame(Object... values) {
        return scope != null? scope.createFrame(values) : null;
    }
    
    /**
     * Gets the (maximum) number of arguments this script expects.
     * @return the number of parameters
     */
    public int getArgCount() {
        return scope != null? scope.getArgCount() : 0;
    }
    
    /**
     * Gets this script registers, i.e. parameters and local variables.
     * @return the register names
     */
    public String[] getRegisters() {
        return scope != null? scope.getRegisters() : null;
    }

    /**
     * Gets this script parameters, i.e. registers assigned before creating local variables.
     * @return the parameter names
     */
    public String[] getParameters() {
        return scope != null? scope.getParameters() : null;
    }

    /**
     * Gets this script local variable, i.e. registers assigned to local variables.
     * @return the parameter names
     */
    public String[] getLocalVariables() {
        return scope != null? scope.getLocalVariables() : null;
    }
}
