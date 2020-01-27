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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A script scope, stores the declaration of parameters and local variables as symbols.
 * <p>This also acts as the functional scope and variable definition store.
 * @since 3.0
 */
public final class Scope {
    /**
     * The value of an as-yet  undeclared but variable, for instance: x; before var x;.
     */
    static final Object UNDECLARED = new Object() {
        @Override public String toString() {
            return "??";
        }
    };
    /**
     * The value of a declared but undefined variable, for instance: var x;.
     */
    static final Object UNDEFINED = new Object() {
        @Override public String toString() {
            return "?";
        }
    };
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
     * The map of local captured variables to parent scope variables, ie closure.
     */
    private Map<Integer, Integer> capturedVariables = null;
    /**
     * The empty string array.
     */
    private static final String[] EMPTY_STRS = new String[0];

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
        if (this == o) {
            return true;
        }
        if (!(o instanceof Scope)) {
            return false;
        }
        Scope scope = (Scope) o;
        if (parms != scope.parms) {
            return false;
        }
        if (namedVariables == null) {
            return scope.namedVariables == null;
        }
        return namedVariables.equals(scope.namedVariables);
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol.
     * If this fails, look in parents for symbol that can be captured.
     * @param name the symbol name
     * @return the symbol index
     */
    public Integer getSymbol(String name) {
        return getSymbol(name, true);
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol.
     * @param name the symbol name
     * @param capture whether solving by capturing a parent symbol is allowed
     * @return the symbol index
     */
    private Integer getSymbol(String name, boolean capture) {
        Integer register = namedVariables != null ? namedVariables.get(name) : null;
        if (register == null && capture && parent != null) {
            Integer pr = parent.getSymbol(name, true);
            if (pr != null) {
                if (capturedVariables == null) {
                    capturedVariables = new LinkedHashMap<Integer, Integer>();
                }
                if (namedVariables == null) {
                    namedVariables = new LinkedHashMap<String, Integer>();
                }
                register = namedVariables.size();
                namedVariables.put(name, register);
                capturedVariables.put(register, pr);
            }
        }
        return register;
    }

    /**
     * Checks whether a given symbol is captured.
     * @param symbol the symbol number
     * @return true if captured, false otherwise
     */
    public boolean isCapturedSymbol(int symbol) {
        return capturedVariables != null && capturedVariables.containsKey(symbol);
    }

    /**
     * Declares a parameter.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the parameter name
     * @return the register index storing this variable
     */
    public int declareParameter(String name) {
        if (namedVariables == null) {
            namedVariables = new LinkedHashMap<String, Integer>();
        } else if (vars > 0) {
            throw new IllegalStateException("cant declare parameters after variables");
        }
        Integer register = namedVariables.get(name);
        if (register == null) {
            register = namedVariables.size();
            namedVariables.put(name, register);
            parms += 1;
        }
        return register;
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     * @param name the variable name
     * @return the register index storing this variable
     */
    public int declareVariable(String name) {
        if (namedVariables == null) {
            namedVariables = new LinkedHashMap<String, Integer>();
        }
        Integer register = namedVariables.get(name);
        if (register == null) {
            register = namedVariables.size();
            namedVariables.put(name, register);
            vars += 1;
            // check if local is redefining captured
            if (parent != null) {
                Integer pr = parent.getSymbol(name, true);
                if (pr != null) {
                    if (capturedVariables == null) {
                        capturedVariables = new LinkedHashMap<Integer, Integer>();
                    }
                    capturedVariables.put(register, pr);
                }
            }
        }
        return register;
    }

    /**
     * Creates a frame by copying values up to the number of parameters.
     * <p>This captures the captured variables values.</p>
     * @param frame the caller frame
     * @param args the arguments
     * @return the arguments array
     */
    public Frame createFrame(Frame frame, Object...args) {
        if (namedVariables != null) {
            Object[] arguments = new Object[namedVariables.size()];
            Arrays.fill(arguments, UNDECLARED);
            if (frame != null && capturedVariables != null && parent != null) {
                for (Map.Entry<Integer, Integer> capture : capturedVariables.entrySet()) {
                    Integer target = capture.getKey();
                    Integer source = capture.getValue();
                    Object arg = frame.get(source);
                    arguments[target] = arg;
                }
            }
            return new Frame(this, arguments, 0).assign(args);
        } else {
            return null;
        }
    }

    /**
     * Gets the captured index of a given symbol, ie the target index of a symbol in a child frame.
     * @param symbol the symbol index
     * @return the target symbol index or null if the symbol is not captured
     */
    public Integer getCaptured(int symbol) {
        if (capturedVariables != null) {
            for (Map.Entry<Integer, Integer> capture : capturedVariables.entrySet()) {
                Integer source = capture.getValue();
                if (source == symbol) {
                    return capture.getKey();
                }
            }
        }
        return null;
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
        return namedVariables != null ? namedVariables.keySet().toArray(new String[0]) : EMPTY_STRS;
    }

    /**
     * Gets this script parameters, i.e. symbols assigned before creating local variables.
     * @return the parameter names
     */
    public String[] getParameters() {
        return getParameters(0);
    }

    /**
     * Gets this script parameters.
     * @param bound number of known bound parameters (curry)
     * @return the parameter names
     */
    protected String[] getParameters(int bound) {
        int unbound = parms - bound;
        if (namedVariables != null && unbound > 0) {
            String[] pa = new String[unbound];
            int p = 0;
            for (Map.Entry<String, Integer> entry : namedVariables.entrySet()) {
                int argn = entry.getValue();
                if (argn >= bound && argn < parms) {
                    pa[p++] = entry.getKey();
                }
            }
            return pa;
        } else {
            return EMPTY_STRS;
        }
    }

    /**
     * Gets this script local variable, i.e. symbols assigned to local variables excluding captured variables.
     * @return the local variable names
     */
    public String[] getLocalVariables() {
        if (namedVariables != null && vars > 0) {
            List<String> locals = new ArrayList<String>(vars);
            for (Map.Entry<String, Integer> entry : namedVariables.entrySet()) {
                int symnum = entry.getValue();
                if (symnum >= parms && (capturedVariables == null || !capturedVariables.containsKey(symnum))) {
                    locals.add(entry.getKey());
                }
            }
            return locals.toArray(new String[locals.size()]);
        } else {
            return EMPTY_STRS;
        }
    }

}
