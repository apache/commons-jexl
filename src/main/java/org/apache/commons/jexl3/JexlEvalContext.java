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

import java.math.MathContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A JEXL evaluation context wrapping variables and options.
 */
public class JexlEvalContext implements JexlContext, JexlEngine.Options {
    /** The marker for the empty vars. */
    protected static final Map<String,Object> EMPTY_MAP = Collections.<String,Object>emptyMap();
    /** The wrapped variable vars (if any).*/
    protected final Map<String, Object> vars;
    /** Whether the engine should be silent. */
    private Boolean silent = null;
    /** Whether the engine should be strict. */
    private Boolean strict = null;
    /** Whether the arithmetic should be strict. */
    private Boolean mathStrict = null;
    /** The math scale the arithmetic should use. */
    private int mathScale = Integer.MIN_VALUE;
    /** The math context the arithmetic should use. */
    private MathContext mathContext = null;

    /**
     * Default constructor.
     */
    public JexlEvalContext() {
        this(EMPTY_MAP);
    }

    /**
     * Creates a MapContext wrapping an existing user provided vars.
     * <p>The supplied vars should be null only in derived classes that override the get/set/has methods.
     * For a default vars context with a code supplied vars, use the default no-parameter contructor.</p>
     * @param map the variables map
     */
    public JexlEvalContext(Map<String, Object> map) { 
        this.vars = map == EMPTY_MAP ? new HashMap<String, Object>() : map;
    }

    @Override
    public boolean has(String name) {
        return vars.containsKey(name);
    }

    @Override
    public Object get(String name) {
        return vars.get(name);
    }

    @Override
    public void set(String name, Object value) {
        vars.put(name, value);
    }

    /**
     * Clears the variable vars.
     */
    public void clearVariables() {
        vars.clear();
    }

    /**
     * Clear all options.
     */
    public void clearOptions() {
        silent = null;
        strict = null;
        mathScale = -1;
        mathContext = null;
    }

    /**
     * Sets whether the engine will throw JexlException during evaluation when an error is triggered.
     * @param s true means no JexlException will occur, false allows them
     */
    public void setSilent(boolean s) {
        this.silent = s ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public Boolean isSilent() {
        return this.silent;
    }

    /**
     * Sets the engine and arithmetic strict flags in one call.
     * @param se the engine strict flag
     * @param sa the arithmetic strict flag
     */
    public void setStrict(boolean se, boolean sa) {
        this.strict = se ? Boolean.TRUE : Boolean.FALSE;
        this.mathStrict = sa ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Sets whether the engine will consider unknown variables, methods and constructors as errors or evaluates them
     * as null.
     * @param se true means strict error reporting, false allows mentioned conditions to be evaluated as null
     */
    public void setStrict(boolean se) {
        setStrict(se, se);
    }

    @Override
    public Boolean isStrict() {
        if (strict == null) {
            return null;
        } else {
            return strict.booleanValue() ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    /**
     * Sets whether the arithmetic will consider null arguments as errors during evaluation.
     * @param s true means strict error reporting, false allows mentioned conditions to be evaluated as 0
     */
    public void setStrictArithmetic(boolean s) {
        this.mathStrict = s ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public Boolean isStrictArithmetic() {
        if (mathStrict == null) {
            return null;
        } else {
            return mathStrict.booleanValue() ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    @Override
    public MathContext getArithmeticMathContext() {
        return mathContext;
    }

    /**
     * Sets the {@link MathContext} to use by the {@link JexlArithmetic} during evaluation.
     * @param mc the math context
     */
    public void setMathContext(MathContext mc) {
        mathContext = mc;
    }

    @Override
    public int getArithmeticMathScale() {
        return mathScale;
    }

    /**
     * Sets the math scale to use to use by the {@link JexlArithmetic} during evaluation.
     * @param scale the math scale
     */
    public void setMathScale(int scale) {
        mathScale = scale;
    }

}
