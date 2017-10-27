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

package org.apache.commons.jexl3;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * A set of language feature options.
 *
 * <ul>
 * <li>Reserved Names: a set of reserved variable names that can not be used as local variable (or parameter) names
 * <li>Registers: boolean property allowing parsing of register syntax (#number)
 * <li>Global Side Effect : boolean property controlling assigning/modifying values on global variables
 * <li>Side Effect: boolean property controlling side effects assigning/modifying values on any variable
 * <li>New Instance: boolean property controlling creating new instances through new(...) or using class as functor
 * <li>Loops: boolean property controlling usage of loop constructs (while(true), for(...))
 * <li>Lambda: boolean property controlling usage of script function declarations
 * </ul>
 * @since 3.2
 */
public final class JexlFeatures {
    /** The feature flags. */
    private long flags;
    /** The set of reserved names, aka global variables that can not be masked by local variables or parameters. */
    private Set<String> reservedNames;
    /** Te feature names (for toString()). */
    private static final String[] F_NAMES = {
        "register", "reserved variable", "local variable", "assign/modify",
        "global assign/modify", "new(...)", "for(...)/while(...)", "function"
    };
    /** Registers feature ordinal. */
    private static final int REGISTERS = 0;
    /** Reserved name feature ordinal. */
    public static final int RESERVED = 1;
    /** Locals feature ordinal. */
    public static final int LOCALS = 2;
    /** Side-effects feature ordinal. */
    public static final int SIDE_EFFECTS = 3;
    /** Global side-effects feature ordinal. */
    public static final int SIDE_EFFECTS_GLOBALS = 4;
    /** New-instance feature ordinal. */
    public static final int NEW_INSTANCE = 5;
    /** Loops feature ordinal. */
    public static final int LOOPS = 6;
    /** Lambda feature ordinal. */
    public static final int LAMBDA = 7;

    /**
     * Creates an all-features-enabled instance.
     */
    public JexlFeatures() {
        flags = (1L) // << REGISTERS)
                | (1L << LOCALS)
                | (1L << SIDE_EFFECTS)
                | (1L << SIDE_EFFECTS_GLOBALS)
                | (1L << NEW_INSTANCE)
                | (1L << LOOPS)
                | (1L << LAMBDA);
        reservedNames = Collections.emptySet();
    }

    /**
     * Copy constructor.
     * @param features the feature to copy from
     */
    public JexlFeatures(JexlFeatures features) {
        this.flags = features.flags;
        this.reservedNames = features.reservedNames;
    }

    /**
     * The text corresponding to a feature code.
     * @param feature the feature number
     * @return the feature name
     */
    public static String stringify(int feature) {
        return feature >= 0 && feature < F_NAMES.length? F_NAMES[feature] : "unsupported feature";
    }

    /**
     * Sets a collection of reserved names precluding those to be used as local variables or parameter names.
     * @param names the names to reserve
     * @return this features instance
     */
    public JexlFeatures reservedNames(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            reservedNames = Collections.emptySet();
        } else {
            reservedNames = Collections.unmodifiableSet(new TreeSet<String>(names));
        }
        setFeature(RESERVED, !reservedNames.isEmpty());
        return this;
    }

    /**
     * @return the (unmodifiable) set of reserved names.
     */
    public Set<String> getReservedNames() {
        return reservedNames;
    }

    /**
     * Checks whether a name is reserved.
     * @param name the name to check
     * @return true if reserved, false otherwise
     */
    public boolean isReservedName(String name) {
        return reservedNames.contains(name);
    }

    /**
     * Sets a feature flag.
     * @param feature the feature ordinal
     * @param flag turn-on, turn off
     */
    private void setFeature(int feature, boolean flag) {
        if (flag) {
            flags |= (1 << feature);
        } else {
            flags &= ~(1L << feature);
        }
    }

    /**
     * Gets a feature flag value.
     * @param feature feature ordinal
     * @return true if on, false if off
     */
    private boolean getFeature(int feature) {
        return (flags & (1L << feature)) != 0L;
    }

    /**
     * Sets whether registers are enabled.
     * <p>When disabled, parsing a script/expression using the register syntax will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures registers(boolean flag) {
        setFeature(REGISTERS, flag);
        return this;
    }

    /**
     * @return true if register syntax is enabled
     */
    public boolean supportsRegisters() {
        return getFeature(REGISTERS);
    }

    /**
     * Sets whether local variables are enabled.
     * <p>When disabled, parsing a script/expression using a local variable or parameter syntax
     * will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures locals(boolean flag) {
        setFeature(LOCALS, flag);
        return this;
    }

    /**
     * @return true if local variables syntax is enabled
     */
    public boolean supportsLocals() {
        return getFeature(LOCALS);
    }

   /**
     * Sets whether side effect expressions on global variables (aka non local) are enabled.
     * <p>When disabled, parsing a script/expression using syntactical constructs modifying variables
     * <em>including all potentially ant-ish variables</em> will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures sideEffectsGlobal(boolean flag) {
        setFeature(SIDE_EFFECTS_GLOBALS, flag);
        return this;
    }

    /**
     * @return true if global variables can be assigned
     */
    public boolean supportsSideEffectsGlobal() {
        return getFeature(SIDE_EFFECTS_GLOBALS);
    }

   /**
     * Sets whether side effect expressions are enabled.
     * <p>When disabled, parsing a script/expression using syntactical constructs modifying variables
     * or members will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures sideEffects(boolean flag) {
        setFeature(SIDE_EFFECTS, flag);
        return this;
    }

    /**
     * @return true if side effects are enabled, false otherwise
     */
    public boolean supportsSideEffects() {
        return getFeature(SIDE_EFFECTS);
    }

   /**
     * Sets whether creating new instances is enabled.
     * <p>When disabled, parsing a script/expression using 'new(...)' will throw a parsing exception;
     * using a class as functor will fail at runtime.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures newInstance(boolean flag) {
        setFeature(NEW_INSTANCE, flag);
        return this;
    }

    /**
     * @return true if creating new instances is enabled, false otherwise
     */
    public boolean supportsNewInstance() {
        return getFeature(NEW_INSTANCE);
    }

    /**
     * Sets whether looping constructs are enabled.
     * <p>When disabled, parsing a script/expression using syntactic looping constructs (for,while)
     * will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures loops(boolean flag) {
        setFeature(LOOPS, flag);
        return this;
    }

    /**
     * @return true if loops are enabled, false otherwise
     */
    public boolean supportsLoops() {
        return getFeature(LOOPS);
    }

       /**
     * Sets whether lambda/function constructs are enabled.
     * <p>When disabled, parsing a script/expression using syntactic lambda constructs (->,function)
     * will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures lambda(boolean flag) {
        setFeature(LAMBDA, flag);
        return this;
    }

    /**
     * @return true if lambda are enabled, false otherwise
     */
    public boolean supportsLambda() {
        return getFeature(LAMBDA);
    }

}
