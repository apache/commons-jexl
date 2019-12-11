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
import org.apache.commons.jexl3.internal.Engine;

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
 * <li>sharedInstance: whether these options can be modified at runtime during execution (expert)</li>
 * </ul>
 * The sensible default is cancellable, strict and strictArithmetic.
 * <p>This interface replaces the now deprecated JexlEngine.Options.
 * @since 3.2
 */
public final class JexlOptions {
    /** The shared isntance bit. */
    private static final int SHARED = 7;
    /** The local shade bit. */
    private static final int SHADE = 6;
    /** The antish var bit. */
    private static final int ANTISH = 5;
    /** The lexical scope bit. */
    private static final int LEXICAL = 4;
    /** The safe bit. */
    private static final int SAFE = 3;
    /** The silent bit. */
    private static final int SILENT = 2;
    /** The strict bit. */
    private static final int STRICT = 1;
    /** The cancellable bit. */
    private static final int CANCELLABLE = 0;
    /** The flags names ordered. */
    private static final String[] NAMES = {
        "cancellable", "strict", "silent", "safe", "lexical", "antish", "lexicalShade", "sharedInstance"
    };
    /** Default mask .*/
    private static int DEFAULT = 1 /*<< CANCELLABLE*/ | 1 << STRICT | 1 << ANTISH | 1 << SAFE;
    /** The arithmetic math context. */
    private MathContext mathContext = null;
    /** The arithmetic math scale. */
    private int mathScale = Integer.MIN_VALUE;
    /** The arithmetic strict math flag. */
    private boolean strictArithmetic = true;
    /** The default flags, all but safe. */
    private int flags = DEFAULT;

    /**
     * Sets the value of a flag in a mask.
     * @param ordinal the flag ordinal
     * @param mask the flags mask
     * @param value true or false
     * @return the new flags mask value
     */
    private static int set(int ordinal, int mask, boolean value) {
        return value? mask | (1 << ordinal) : mask & ~(1 << ordinal);
    }

    /**
     * Checks the value of a flag in the mask.
     * @param ordinal the flag ordinal
     * @param mask the flags mask
     * @return the mask value with this flag or-ed in
     */
    private static boolean isSet(int ordinal, int mask) {
        return (mask & 1 << ordinal) != 0;
    }
        
    /**
     * Default ctor.
     */
    public JexlOptions() {}
            
    /**
     * Sets the default (static, shared) option flags.
     * <p>
     * Whenever possible, we recommend using JexlBuilder methods to unambiguously instantiate a JEXL
     * engine; this method should only be used for testing / validation.
     * <p>A '+flag' or 'flag' will set the option named 'flag' as true, '-flag' set as false.
     * The possible flag names are:
     * cancellable, strict, silent, safe, lexical, antish, lexicalShade
     * <p>Calling JexlBuilder.setDefaultOptions("+safe") once before JEXL engine creation 
     * may ease validating JEXL3.2 in your environment.
     * @param flags the flags to set 
     */
    public static void setDefaultFlags(String...flags) {
        DEFAULT = parseFlags(DEFAULT, flags);
    }
        
    /**
     * Parses flags by name.
     * <p>A '+flag' or 'flag' will set flag as true, '-flag' set as false.
     * The possible flag names are:
     * cancellable, strict, silent, safe, lexical, antish, lexicalShade
     * @param mask the initial mask state
     * @param flags the flags to set 
     * @return the flag mask updated
     */
    public static int parseFlags(int mask, String...flags) {
        for(String name : flags) {
            boolean b = true;
            if (name.charAt(0) == '+') {
                name = name.substring(1);
            } else if (name.charAt(0) == '-') {
                name = name.substring(1);
                b = false;
            }
            for(int flag = 0; flag < NAMES.length; ++flag) {
                if (NAMES[flag].equals(name)) {
                    if (b) {
                        mask |= (1 << flag);
                    } else {
                        mask &= ~(1 << flag);
                    }
                    break;
                }
            }
        }
        return mask;
    }
    
    /**
     * Sets this option flags using the +/- syntax.
     * @param opts the option flags
     */
    public void setFlags(String[] opts) {
        flags = parseFlags(flags, opts);
    }
    
    /**
     * The MathContext instance used for +,-,/,*,% operations on big decimals.
     * @return the math context
     */
    public MathContext getMathContext() {
        return mathContext;
    }

    /**
     * The BigDecimal scale used for comparison and coercion operations.
     * @return the scale
     */
    public int getMathScale() {
        return mathScale;
    }

    /**
     * Checks whether evaluation will attempt resolving antish variable names.
     * @return true if antish variables are solved, false otherwise
     */
    public boolean isAntish() {
        return isSet(ANTISH, flags);
    }
    
    /**
     * Checks whether evaluation will throw JexlException.Cancel (true) or
     * return null (false) if interrupted.
     * @return true when cancellable, false otherwise
     */
    public boolean isCancellable() {
        return isSet(CANCELLABLE, flags);
    }

    /**
     * Checks whether runtime variable scope is lexical.
     * <p>If true, lexical scope applies to local variables and parameters.
     * Redefining a variable in the same lexical unit will generate errors.
     * @return true if scope is lexical, false otherwise
     */
    public boolean isLexical() {
        return isSet(LEXICAL, flags);
    }
    
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
    public boolean isLexicalShade() {
        return isSet(SHADE, flags);
    }
    
    /**
     * Checks whether the engine considers null in navigation expression as
     * errors during evaluation..
     * @return true if safe, false otherwise
     */
    public boolean isSafe() {
        return isSet(SAFE, flags);
    }

    /**
     * Checks whether the engine will throw a {@link JexlException} when an
     * error is encountered during evaluation.
     * @return true if silent, false otherwise
     */
    public boolean isSilent() {
        return isSet(SILENT, flags);
    }

    /**
     * Checks whether the engine considers unknown variables, methods and
     * constructors as errors during evaluation.
     * @return true if strict, false otherwise
     */
    public boolean isStrict() {
        return isSet(STRICT, flags);
    }

    /**
     * Checks whether the arithmetic triggers errors during evaluation when null
     * is used as an operand.
     * @return true if strict, false otherwise
     */
    public boolean isStrictArithmetic() {
        return strictArithmetic;
    }
    
    /**
     * Sets whether the engine will attempt solving antish variable names from 
     * context.
     * @param flag true if antish variables are solved, false otherwise
     */
    public void setAntish(boolean flag) {
        flags = set(ANTISH, flags, flag);
    }

    /**
     * Sets whether the engine will throw JexlException.Cancel (true) or return
     * null (false) when interrupted during evaluation.
     * @param flag true when cancellable, false otherwise
     */
    public void setCancellable(boolean flag) {
        flags = set(CANCELLABLE, flags, flag);
    }
    
    /**
     * Sets whether the engine uses a strict block lexical scope during
     * evaluation.
     * @param flag true if lexical scope is used, false otherwise
     */
    public void setLexical(boolean flag) {
        flags = set(LEXICAL, flags, flag);
    }   
    
    /**
     * Sets whether the engine strictly shades global variables.
     * Local symbols shade globals after definition and creating global
     * variables is prohibited during evaluation.
     * If setting to lexical shade, lexical scope is also set.
     * @param flag true if creation is allowed, false otherwise
     */
    public void setLexicalShade(boolean flag) {
        flags = set(SHADE, flags, flag);
        if (flag) {
            flags = set(LEXICAL, flags, true);
        }
    }

    /**
     * Sets the arithmetic math context.
     * @param mcontext the context
     */
    public void setMathContext(MathContext mcontext) {
        this.mathContext = mcontext;
    }

    /**
     * Sets the arithmetic math scale.
     * @param mscale the scale
     */
    public void setMathScale(int mscale) {
        this.mathScale = mscale;
    }

    /**
     * Sets whether the engine considers null in navigation expression as errors
     * during evaluation.
     * @param flag true if safe, false otherwise
     */
    public void setSafe(boolean flag) {
        flags = set(SAFE, flags, flag);
    } 

    /**
     * Sets whether the engine will throw a {@link JexlException} when an error
     * is encountered during evaluation.
     * @param flag true if silent, false otherwise
     */
    public void setSilent(boolean flag) {
        flags = set(SILENT, flags, flag);
    }

    /**
     * Sets whether the engine considers unknown variables, methods and
     * constructors as errors during evaluation.
     * @param flag true if strict, false otherwise
     */
    public void setStrict(boolean flag) {
        flags = set(STRICT, flags, flag);
    }

    /**
     * Sets the strict arithmetic flag.
     * @param stricta true or false
     */
    public void setStrictArithmetic(boolean stricta) {
        this.strictArithmetic = stricta;
    }

    /**
     * Whether these options are immutable at runtime.
     * <p>Expert mode; allows instance handled through context to be shared
     * instead of copied.
     * @param flag true if shared, false if not
     */
    public void setSharedInstance(boolean flag) {
        flags = set(SHARED, flags, flag);
    }
    
    /**
     * @return false if a copy of these options is used during execution,
     * true if those can potentially be modified 
     */
    public boolean isSharedInstance() {
        return isSet(SHARED, flags);
    }
    
    /**
     * Set options from engine.
     * @param jexl the engine
     * @return this instance
     */        
    public JexlOptions set(JexlEngine jexl) {
        if (jexl instanceof Engine) {
            ((Engine) jexl).optionsSet(this);
        }
        return this;
    }

    /**
     * Set options from options.
     * @param src the options
     * @return this instance
     */
    public JexlOptions set(JexlOptions src) {
        mathContext = src.mathContext;
        mathScale = src.mathScale;
        strictArithmetic = src.strictArithmetic;
        flags = src.flags;
        return this;
    }

    /**
     * Creates a copy of this instance.
     * @return a copy
     */
    public JexlOptions copy() {
        return new JexlOptions().set(this);
    }
    
}