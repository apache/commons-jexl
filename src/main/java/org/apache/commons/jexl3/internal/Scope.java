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
package org.apache.commons.jexl3.internal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A script scope, stores the declaration of parameters and local variables.
 * @since 3.0
 */
public final class Scope {
    /**
     * The parent scope.
     */
    private Scope parent = null;
    /**
     * The number of parameters.
     */
    private int parms;
    /**
     * The number of local variables.
     */
    private int vars;
    /**
     * The number of hoisted variables.
     */
    private int hoisted;
    /**
     * The map of named registers aka script parameters.
     * Each parameter is associated to a register and is materialized as an offset in the registers array used
     * during evaluation.
     */
    private Map<String, Integer> namedRegisters = null;
    /**
     * The map of registers to parent registers when hoisted by closure.
     */
    private Map<Integer, Integer> hoistedRegisters = null;

    /**
     * Creates a new scope with a list of parameters.
     * @param parameters the list of parameters
     */
    public Scope(Scope scope, String... parameters) {
        if (parameters != null) {
            parms = parameters.length;
            namedRegisters = new LinkedHashMap<String, Integer>();
            for (int p = 0; p < parms; ++p) {
                namedRegisters.put(parameters[p], p);
            }
        } else {
            parms = 0;
        }
        vars = 0;
        hoisted = 0;
        parent = scope;
    }

    @Override
    public int hashCode() {
        return namedRegisters == null ? 0 : parms ^ namedRegisters.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Scope && equals((Scope) o);
    }

    /**
     * Whether this frame is equal to another.
     * @param frame the frame to compare to
     * @return true if equal, false otherwise
     */
    public boolean equals(Scope frame) {
        if (this == frame) {
            return true;
        } else if (frame == null || parms != frame.parms) {
            return false;
        } else if (namedRegisters == null) {
            return frame.namedRegisters == null;
        } else {
            return namedRegisters.equals(frame.namedRegisters);
        }
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie stored in a register.
     * If this fails, attempt to solve by hoisting parent registers.
     * @param name the register name
     * @return the register index
     */
    public Integer getRegister(String name) {
        return getRegister(name, true);
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie stored in a register.
     * @param name the register name
     * @param hoist whether solving by hoisting parent registers is allowed
     * @return the register index
     */
    private Integer getRegister(String name, boolean hoist) {
        Integer register = namedRegisters != null ? namedRegisters.get(name) : null;
        if (register == null && hoist && parent != null) {
            Integer pr = parent.getRegister(name, false);
            if (pr != null) {
                if (hoistedRegisters == null) {
                    hoistedRegisters = new LinkedHashMap<Integer, Integer>();
                }
                register = Integer.valueOf(namedRegisters.size());
                if (namedRegisters == null) {
                    namedRegisters = new LinkedHashMap<String, Integer>();
                }
                namedRegisters.put(name, register);
                hoistedRegisters.put(register, pr);
                hoisted += 1;
            }
        }
        return register;
    }

    /**
     * Declares a parameter.
     * <p>
     * This method creates an new entry in the named register map.
     * </p>
     * @param name the parameter name
     */
    public void declareParameter(String name) {
        if (namedRegisters == null) {
            namedRegisters = new LinkedHashMap<String, Integer>();
        } else if (vars > 0) {
            throw new IllegalStateException("cant declare parameters after variables");
        }
        Integer register = namedRegisters.get(name);
        if (register == null) {
            register = Integer.valueOf(namedRegisters.size());
            namedRegisters.put(name, register);
            parms += 1;
        }
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the named register map.
     * </p>
     * @param name the variable name
     * @return the register index storing this variable
     */
    public Integer declareVariable(String name) {
        if (namedRegisters == null) {
            namedRegisters = new LinkedHashMap<String, Integer>();
        }
        Integer register = namedRegisters.get(name);
        if (register == null) {
            register = Integer.valueOf(namedRegisters.size());
            namedRegisters.put(name, register);
            vars += 1;
        }
        return register;
    }

    /**
     * Creates a frame by copying values up to the number of parameters.
     * @param values the argument values
     * @return the arguments array
     */
    public Frame createFrame(Frame caller) {
        if (namedRegisters != null) {
            Object[] arguments = new Object[namedRegisters.size()];
            if (caller != null && hoistedRegisters != null && parent != null) {
                for (Map.Entry<Integer, Integer> hoist : hoistedRegisters.entrySet()) {
                    Integer target = hoist.getKey();
                    Integer source = hoist.getValue();
                    Object arg = caller.registers[source.intValue()];
                    arguments[target.intValue()] = arg;
                }
            }
            return new Frame(arguments, namedRegisters.keySet().toArray(new String[namedRegisters.size()]));
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
        return namedRegisters != null ? namedRegisters.keySet().toArray(new String[0]) : new String[0];
    }

    /**
     * Gets this script parameters, i.e. registers assigned before creating local variables.
     * @return the parameter names
     */
    public String[] getParameters() {
        if (namedRegisters != null && parms > 0) {
            String[] pa = new String[parms];
            int p = 0;
            for (Map.Entry<String, Integer> entry : namedRegisters.entrySet()) {
                if (entry.getValue().intValue() < parms) {
                    pa[p++] = entry.getKey();
                }
            }
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
        if (namedRegisters != null && vars > 0) {
            String[] pa = new String[parms];
            int p = 0;
            for (Map.Entry<String, Integer> entry : namedRegisters.entrySet()) {
                if (entry.getValue().intValue() >= parms) {
                    pa[p++] = entry.getKey();
                }
            }
            return pa;
        } else {
            return null;
        }
    }
    
}
