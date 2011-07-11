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

/**
 * Enhanced script to allow parameters declaration.
 */
public class ASTJexlScript extends JexlNode {
    /** The number of parameters defined. */
    private int parms = 0;
    /** Each parameter will use a register but so do local script variables. */
    private String[] registers = null;

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
     * @param parms the number of parameters
     * @param registers the array of register names
     */
    public void setParameters(int parms, String[] registers) {
        if (parms > registers.length) {
            throw new IllegalArgumentException();
        }
        this.parms = parms;
        this.registers = registers;
    }
    
    /**
     * Creates an array of arguments by copying values up to the number of parameters 
     * @param values the argument values
     * @return the arguments array
     */
    public Object[] createArguments(Object... values) {
        if (registers != null) {
            Object[] frame = new Object[registers.length];
            if (values != null) {
                System.arraycopy(values, 0, frame, 0, Math.min(parms, values.length));
            }
            return frame;
        } else {
            return null;
        }
    }
    
    /**
     * Gets the (maximum) number of arguments this script expects.
     * @return the number of parameters
     */
    public int getArgCount() {
        return parms;
    }
    
    /**
     * Gets this script registers, i.e. parameters and local variables.
     * @return the register names
     */
    public String[] getRegisters() {
        return registers;
    }

    /**
     * Gets this script parameters, i.e. registers assigned before creating local variables.
     * @return the parameter names
     */
    public String[] getParameters() {
        if (registers != null && parms > 0) {
            String[] pa = new String[parms];
            System.arraycopy(registers, 0, pa, 0, parms);
            return pa;
        } else {
            return null;
        }
    }

    /**
     * Gets this script local variable, i.e. registers assigned to local variables.
     * @return the parameter names
     */
    public String[] getLocalVariables() {
        if (registers != null) {
            String[] pa = new String[registers.length - parms];
            System.arraycopy(registers, 0, pa, 0, registers.length - parms);
            return pa;
        } else {
            return null;
        }
    }
}
