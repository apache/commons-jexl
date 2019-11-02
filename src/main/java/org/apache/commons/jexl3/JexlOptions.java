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

/**
 * Flags and properties that can alter the evaluation behavior.
 * The flags, briefly explained, are the following:
 * <ul>
 * <li>silent: whether errors throw exception</li>
 * <li>safe: whether navigation through null is an error</li>
 * <li>cancellable: whether thread interruption is an error</li>
 * <li>lexical: whether redefining local variables is an error</li>
 * <li>lexicalShade: whether local variables shade global ones even outside their scope</li>
 * <li>strict: whether unknown or unsolvable identifiers are errors</li>
 * <li>strictArithmetic: whether null as operand is an error</li>
 * </ul>
 * The sensible default is cancellable, strict and strictArithmetic.
 * <p>This interface replaces the now deprecated JexlEngine.Options.
 * @since 3.2
 */
public interface JexlOptions {
    /**
     * The MathContext instance used for +,-,/,*,% operations on big decimals.
     * @return the math context
     */
    MathContext getMathContext();

    /**
     * The BigDecimal scale used for comparison and coercion operations.
     * @return the scale
     */
    int getMathScale();

    /**
     * Checks whether evaluation will attempt resolving antish variable names.
     * @return true if antish variables are solved, false otherwise
     */
    boolean isAntish();
    
    /**
     * Checks whether evaluation will throw JexlException.Cancel (true) or
     * return null (false) if interrupted.
     * @return true when cancellable, false otherwise
     */
    boolean isCancellable();

    /**
     * Checks whether runtime variable scope is lexical.
     * <p>If true, lexical scope applies to local variables and parameters.
     * Redefining a variable in the same lexical unit will generate errors.
     * @return true if scope is lexical, false otherwise
     */
    boolean isLexical();
    
    /**
     * Checks whether local variables shade global ones.
     * <p>After a symbol is defined as local, dereferencing it outside its
     * scope will trigger an error instead of seeking a global variable of the
     * same name. To further reduce potential naming ambiguity errors, 
     * global variables (ie non local) must be declared to be assigned (@link JexlContext#has(String) )
     * when this flag is on; attempting to set an undeclared global variables will
     * raise an error.
     * @return true if lexical shading is applied, false otherwise
     */
    boolean isLexicalShade();
    
    /**
     * Checks whether the engine considers null in navigation expression as
     * errors during evaluation..
     * @return true if safe, false otherwise
     */
    boolean isSafe();

    /**
     * Checks whether the engine will throw a {@link JexlException} when an
     * error is encountered during evaluation.
     * @return true if silent, false otherwise
     */
    boolean isSilent();

    /**
     * Checks whether the engine considers unknown variables, methods and
     * constructors as errors during evaluation.
     * @return true if strict, false otherwise
     */
    boolean isStrict();

    /**
     * Checks whether the arithmetic triggers errors during evaluation when null
     * is used as an operand.
     * @return true if strict, false otherwise
     */
    boolean isStrictArithmetic();
    
    /**
     * Sets whether the engine will attempt solving antish variable names from 
     * context.
     * @param flag true if antish variables are solved, false otherwise
     */
    void setAntish(boolean flag);

    /**
     * Sets whether the engine will throw JexlException.Cancel (true) or return
     * null (false) when interrupted during evaluation.
     * @param flag true when cancellable, false otherwise
     */
    void setCancellable(boolean flag);
    
    /**
     * Sets whether the engine uses a strict block lexical scope during
     * evaluation.
     * @param flag true if lexical scope is used, false otherwise
     */
    void setLexical(boolean flag);
    
    /**
     * Sets whether the engine strictly shades global variables.
     * Local symbols shade globals after definition and creating global
     * variables is prohibited evaluation.
     * @param flag true if creation is allowed, false otherwise
     */
    void setLexicalShade(boolean flag);

    /**
     * Sets the arithmetic math context.
     * @param mcontext the context
     */
    void setMathContext(MathContext mcontext);

    /**
     * Sets the arithmetic math scale.
     * @param mscale the scale
     */
    void setMathScale(int mscale);

    /**
     * Set options from engine.
     * @param jexl the engine
     * @return this instance
     */
    JexlOptions set(JexlEngine jexl);

    /**
     * Sets whether the engine considers null in navigation expression as errors
     * during evaluation.
     * @param flag true if safe, false otherwise
     */
    void setSafe(boolean flag);

    /**
     * Sets whether the engine will throw a {@link JexlException} when an error
     * is encountered during evaluation.
     * @param flag true if silent, false otherwise
     */
    void setSilent(boolean flag);

    /**
     * Sets whether the engine considers unknown variables, methods and
     * constructors as errors during evaluation.
     * @param flag true if strict, false otherwise
     */
    void setStrict(boolean flag);

    /**
     * Sets the strict arithmetic flag.
     * @param stricta true or false
     */
    void setStrictArithmetic(boolean stricta);
    
    /**
     * Creates a copy of this instance.
     * @return a copy
     */
    JexlOptions copy();
    
}