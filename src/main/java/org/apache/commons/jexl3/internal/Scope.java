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
 * A script scope, stores the declaration of parameters and local variables as symbols.
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
     * The map of named variables aka script parameters and local variables.
     * Each parameter is associated to a symbol and is materialized as an offset in the stacked array used
     * during evaluation.
     */
    private Map<String, Integer> namedVariables = null;
    /**
     * The map of local hoisted variables to parent scope variables, ie closure.
     */
    private Map<Integer, Integer> hoistedVariables = null;

    /**
     * Creates a new scope with a list of parameters.
     * @param scope the parent scope if any
     * @param parameters the list of parameters
     */
    public Scope(Scope scope, String... parameters) {
        if (parameters != null) {
            parms = parameters.length;
            namedVariables = new LinkedHashMap<String, Integer>();
            for (int p = 0; p < parms; ++p) {
                namedVariables.put(parameters[p], p);
            }
        } else {
            parms = 0;
        }
        vars = 0;
        parent = scope;
    }

    @Override
    public int hashCode() {
        return namedVariables == null ? 0 : parms ^ namedVariables.hashCode();
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
        } else if (namedVariables == null) {
            return frame.namedVariables == null;
        } else {
            return namedVariables.equals(frame.namedVariables);
        }
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol.
     * If this fails, attempt to solve by hoisting parent stacked.
     * @param name the symbol name
     * @return the symbol index
     */
    public Integer getSymbol(String name) {
        return getSymbol(name, true);
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol.
     * @param name the symbol name
     * @param hoist whether solving by hoisting parent stacked is allowed
     * @return the symbol index
     */
    private Integer getSymbol(String name, boolean hoist) {
        Integer register = namedVariables != null ? namedVariables.get(name) : null;
        if (register == null && hoist && parent != null) {
            Integer pr = parent.getSymbol(name, false);
            if (pr != null) {
                if (hoistedVariables == null) {
                    hoistedVariables = new LinkedHashMap<Integer, Integer>();
                }
                if (namedVariables == null) {
                    namedVariables = new LinkedHashMap<String, Integer>();
                }
                register = Integer.valueOf(namedVariables.size());
                namedVariables.put(name, register);
                hoistedVariables.put(register, pr);
            }
        }
        return register;
    }

    /**
     * Declares a parameter.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the parameter name
     */
    public void declareParameter(String name) {
        if (namedVariables == null) {
            namedVariables = new LinkedHashMap<String, Integer>();
        } else if (vars > 0) {
            throw new IllegalStateException("cant declare parameters after variables");
        }
        Integer register = namedVariables.get(name);
        if (register == null) {
            register = Integer.valueOf(namedVariables.size());
            namedVariables.put(name, register);
            parms += 1;
        }
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the variable name
     * @return the register index storing this variable
     */
    public Integer declareVariable(String name) {
        if (namedVariables == null) {
            namedVariables = new LinkedHashMap<String, Integer>();
        }
        Integer register = namedVariables.get(name);
        if (register == null) {
            register = Integer.valueOf(namedVariables.size());
            namedVariables.put(name, register);
            vars += 1;
        }
        return register;
    }

    /**
     * Creates a frame by copying values up to the number of parameters.
     * <p>This captures the hoisted variables values.</p>
     * @param frame the caller frame
     * @return the arguments array
     */
    public Frame createFrame(Frame frame) {
        if (namedVariables != null) {
            Object[] arguments = new Object[namedVariables.size()];
            if (frame != null && hoistedVariables != null && parent != null) {
                for (Map.Entry<Integer, Integer> hoist : hoistedVariables.entrySet()) {
                    Integer target = hoist.getKey();
                    Integer source = hoist.getValue();
                    Object arg = frame.get(source.intValue());
                    arguments[target.intValue()] = arg;
                }
            }
            return new Frame(arguments);
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
     * Gets this script symbols names, i.e. parameters and local variables.
     * @return the symbol names
     */
    public String[] getSymbols() {
        return namedVariables != null ? namedVariables.keySet().toArray(new String[0]) : new String[0];
    }

    /**
     * Gets this script parameters, i.e. symbols assigned before creating local variables.
     * @return the parameter names
     */
    public String[] getParameters() {
        if (namedVariables != null && parms > 0) {
            String[] pa = new String[parms];
            int p = 0;
            for (Map.Entry<String, Integer> entry : namedVariables.entrySet()) {
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
     * Gets this script local variable, i.e. symbols assigned to local variables.
     * @return the local variable names
     */
    public String[] getLocalVariables() {
        if (namedVariables != null && vars > 0) {
            String[] pa = new String[parms];
            int p = 0;
            for (Map.Entry<String, Integer> entry : namedVariables.entrySet()) {
                if (entry.getValue().intValue() >= parms) {
                    pa[p++] = entry.getKey();
                }
            }
            return pa;
        } else {
            return null;
        }
    }

    /**
     * A call frame, created from a scope, stores the arguments and local variables in a "stack frame" (sic).
     * @since 3.0
     */
    public static final class Frame {
        /** The actual stack frame. */
        private final Object[] stack;

        /**
         * Creates a new frame.
         * @param r the stack frame
         */
        public Frame(Object[] r) {
            stack = r;
        }

        /**
         * Gets a value.
         * @param s the offset in this frame
         * @return the stacked value
         */
        public Object get(int s) {
            return stack[s];
        }

        /**
         * Sets a value.
         * @param r the offset in this frame
         * @param value the value to set in this frame
         */
        public void set(int r, Object value) {
            stack[r] = value;
        }

        /**
         * Assign values to this frame.
         * @param values the values
         * @return this frame
         */
        public Frame assign(Object... values) {
            if (stack != null && values != null && values.length > 0) {
                Object[] copy = stack.clone();
                System.arraycopy(values, 0, copy, 0, Math.min(copy.length, values.length));
                return new Frame(copy);
            }
            return this;
        }
    }
}
