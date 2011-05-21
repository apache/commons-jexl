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

import java.math.MathContext;

/**
 * A derived arithmetic that allows different threads to operate with
 * different strict/lenient/math modes using the same JexlEngine.
 */
public class JexlThreadedArithmetic extends JexlArithmetic {
    
    /** Holds the threaded version of some arithmetic features. */
    static class Features {
        Features() {
        }
        /** Whether this JexlArithmetic instance behaves in strict or lenient mode. */
        Boolean lenient = null;
        /** The big decimal math context. */
        MathContext mathContext = null;
        /** The big decimal scale. */
        Integer mathScale = null;
    }

    /**
     * Standard ctor.
     * @param lenient lenient versus strict evaluation flag
     */
    public JexlThreadedArithmetic(boolean lenient) {
        super(lenient);
    }
    
    /**
     * Creates a JexlThreadedArithmetic.
     * @param lenient whether this arithmetic is lenient or strict
     * @param bigdContext the math context instance to use for +,-,/,*,% operations on big decimals.
     * @param bigdScale the scale used for big decimals.
     */
    public JexlThreadedArithmetic(boolean lenient, MathContext bigdContext, int bigdScale) {
        super(lenient, bigdContext, bigdScale);
    }
    
    /** Whether this JexlArithmetic instance behaves in strict or lenient mode for this thread. */
    static final ThreadLocal<Features> FEATURES = new ThreadLocal<Features>() {
        @Override
        protected synchronized Features initialValue() {
            return new Features();
        }
    };


    /**
     * Overrides the default behavior and sets whether this JexlArithmetic instance triggers errors
     * during evaluation when null is used as an operand for the current thread.
     * <p>It is advised to protect calls by either calling JexlThreadedArithmetic.setLenient explicitly before evaluation
     * or add a try/finally clause resetting the flag to avoid unexpected reuse of the lenient
     * flag value through thread pools side-effects.</p>
     * @see JexlEngine#setSilent
     * @see JexlEngine#setDebug
     * @param flag true means no JexlException will occur, false allows them, null reverts to default behavior
     */
    public static void setLenient(Boolean flag) {
        FEATURES.get().lenient = flag == null? null : flag;
    }
    
    /**
     * Sets the math scale.
     * <p>The goal and constraints are the same than for setLenient.</p>
     * @param scale the scale
     */
    public static void setMathScale(Integer scale) {
        FEATURES.get().mathScale = scale;
    }
    
    /**
     * Sets the math context.
     * <p>The goal and constraints are the same than for setLenient.</p>
     * @param mc the math context
     */
    public static void setMathContext(MathContext mc) {
        FEATURES.get().mathContext = mc;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLenient() {
        Boolean lenient = FEATURES.get().lenient;
        return lenient == null ? super.isLenient() : lenient.booleanValue();
    }
    
    @Override
    public int getMathScale() {
        Integer scale = FEATURES.get().mathScale;
        return scale == null ? super.getMathScale() : scale.intValue();
    }
    
    @Override
    public MathContext getMathContext() {
        MathContext mc = FEATURES.get().mathContext;
        return mc == null? super.getMathContext() : mc;
    }
}
