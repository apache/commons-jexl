/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jexl3;

import java.math.MathContext;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.jexl3.internal.Engine;

/**
 * Flags and properties that can alter the evaluation behavior.
 * The flags, briefly explained, are the following:
 * <ul>
 * <li>silent: whether errors throw exception</li>
 * <li>safe: whether navigation through null is <em>not</em>an error</li>
 * <li>cancellable: whether thread interruption is an error</li>
 * <li>lexical: whether redefining local variables is an error</li>
 * <li>lexicalShade: whether local variables shade global ones even outside their scope</li>
 * <li>strict: whether unknown or unsolvable identifiers are errors</li>
 * <li>strictArithmetic: whether null as operand is an error</li>
 * <li>sharedInstance: whether these options can be modified at runtime during execution (expert)</li>
 * <li>constCapture: whether captured variables will throw an error if an attempt is made to change their value</li>
 * <li>strictInterpolation: whether interpolation strings always return a string or attempt to parse and return integer</li>
 * <li>booleanLogical: whether logical expressions (&quot;&quot; , ||) coerce their result to boolean</li>
 * </ul>
 * The sensible default is cancellable, strict and strictArithmetic.
 * <p>This interface replaces the now deprecated JexlEngine.Options.
 * @since 3.2
 */
public final class JexlOptions {
    /** The boolean logical flag. */
    private static final int BOOLEAN_LOGICAL = 10;
    /** The interpolation string bit. */
    private static final int STRICT_INTERPOLATION= 9;
    /** The const capture bit. */
    private static final int CONST_CAPTURE = 8;
    /** The shared instance bit. */
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
    /** The flag names ordered. */
    private static final String[] NAMES = {
        "cancellable", "strict", "silent", "safe", "lexical", "antish",
        "lexicalShade", "sharedInstance", "constCapture", "strictInterpolation",
        "booleanShortCircuit"
    };
    /** Default mask .*/
    private static int DEFAULT = 1 /*<< CANCELLABLE*/ | 1 << STRICT | 1 << ANTISH | 1 << SAFE;

    /**
     * Checks the value of a flag in the mask.
     * @param ordinal the flag ordinal
     * @param mask the flags mask
     * @return the mask value with this flag or-ed in
     */
    private static boolean isSet(final int ordinal, final int mask) {
        return (mask & 1 << ordinal) != 0;
    }

    /**
     * Parses flags by name.
     * <p>A '+flag' or 'flag' will set flag as true, '-flag' set as false.
     * The possible flag names are:
     * cancellable, strict, silent, safe, lexical, antish, lexicalShade
     * @param initial the initial mask state
     * @param flags the flags to set
     * @return the flag mask updated
     */
    public static int parseFlags(final int initial, final String... flags) {
        int mask = initial;
        for (final String flag : flags) {
            boolean b = true;
            final String name;
            if (flag.charAt(0) == '+') {
                name = flag.substring(1);
            } else if (flag.charAt(0) == '-') {
                name = flag.substring(1);
                b = false;
            } else {
                name = flag;
            }
            for (int f = 0; f < NAMES.length; ++f) {
                if (NAMES[f].equals(name)) {
                    if (b) {
                        mask |= 1 << f;
                    } else {
                        mask &= ~(1 << f);
                    }
                    break;
                }
            }
        }
        return mask;
    }

    /**
     * Sets the value of a flag in a mask.
     * @param ordinal the flag ordinal
     * @param mask the flags mask
     * @param value true or false
     * @return the new flags mask value
     */
    private static int set(final int ordinal, final int mask, final boolean value) {
        return value? mask | 1 << ordinal : mask & ~(1 << ordinal);
    }

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
    public static void setDefaultFlags(final String...flags) {
        DEFAULT = parseFlags(DEFAULT, flags);
    }

    /** The arithmetic math context. */
    private MathContext mathContext;

    /** The arithmetic math scale. */
    private int mathScale = Integer.MIN_VALUE;

    /** The arithmetic strict math flag. */
    private boolean strictArithmetic = true;

    /** The default flags, all but safe. */
    private int flags = DEFAULT;

    /** The namespaces .*/
    private Map<String, Object> namespaces = Collections.emptyMap();

    /** The imports. */
    private Collection<String> imports = Collections.emptySet();

    /**
     * Default ctor.
     */
    public JexlOptions() {
        // all inits in members declarations
    }

    /**
     * Creates a copy of this instance.
     * @return a copy
     */
    public JexlOptions copy() {
        return new JexlOptions().set(this);
    }

    /**
     * Gets the optional set of imported packages.
     * @return the set of imports, may be empty, not null
     */
    public Collection<String> getImports() {
        return imports;
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
     * Gets the optional map of namespaces.
     * @return the map of namespaces, may be empty, not null
     */
    public Map<String, Object> getNamespaces() {
        return namespaces;
    }

    /**
     * Checks whether evaluation will attempt resolving antish variable names.
     * @return true if antish variables are solved, false otherwise
     */
    public boolean isAntish() {
        return isSet(ANTISH, flags);
    }

    /**
     * Gets whether logical expressions (&quot;&quot; , ||) coerce their result to boolean; if set,
     * an expression like (3 &quot;&quot; 4 &quot;&quot; 5) will evaluate to true. If not, it will evaluate to 5.
     * To preserve strict compatibility with 3.4, set the flag to true.
     * @return true if short-circuit logicals coerce their result to boolean, false otherwise
     * @since 3.5.0
     */
    public boolean isBooleanLogical() {
        return isSet(BOOLEAN_LOGICAL, flags);
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
     * Are lambda captured-variables const?
     * @return true if lambda captured-variables are const, false otherwise
     */
    public boolean isConstCapture() {
        return isSet(CONST_CAPTURE, flags);
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
     * global variables (ie non-local) must be declared to be assigned {@link JexlContext#has(String)}
     * when this flag is on; attempting to set an undeclared global variables will
     * raise an error.
     * @return true if lexical shading is applied, false otherwise
     */
    public boolean isLexicalShade() {
        return isSet(SHADE, flags);
    }

    /**
     * Checks whether the engine considers null in navigation expression as
     * errors during evaluation.
     * @return true if safe, false otherwise
     */
    public boolean isSafe() {
        return isSet(SAFE, flags);
    }

    /**
     * Gets sharing state.
     * @return false if a copy of these options is used during execution,
     * true if those can potentially be modified
     */
    public boolean isSharedInstance() {
        return isSet(SHARED, flags);
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
     * Gets the strict-interpolation flag of this options instance.
     * @return true if interpolation strings always return string, false otherwise
     */
    public boolean isStrictInterpolation() {
        return isSet(STRICT_INTERPOLATION, flags);
    }

    /**
     * Sets options from engine.
     * @param jexl the engine
     * @return {@code this} instance
     */
    public JexlOptions set(final JexlEngine jexl) {
        if (jexl instanceof Engine) {
            return ((Engine) jexl).optionsSet(this);
        }
        throw new UnsupportedOperationException("JexlEngine is not an Engine instance: " + jexl.getClass().getName());
    }

    /**
     * Sets options from options.
     * @param src the options
     * @return {@code this} instance
     */
    public JexlOptions set(final JexlOptions src) {
        mathContext = src.mathContext;
        mathScale = src.mathScale;
        strictArithmetic = src.strictArithmetic;
        flags = src.flags;
        namespaces = src.namespaces;
        imports = src.imports;
        return this;
    }

    /**
     * Sets whether the engine will attempt solving antish variable names from
     * context.
     * @param flag true if antish variables are solved, false otherwise
     */
    public void setAntish(final boolean flag) {
        flags = set(ANTISH, flags, flag);
    }

    /**
     * Sets whether logical expressions (&quot;&quot; , ||) coerce their result to boolean.
     * @param flag true or false
     */
    public void setBooleanLogical(final boolean flag) {
        flags = set(BOOLEAN_LOGICAL, flags, flag);
    }

    /**
     * Sets whether the engine will throw JexlException.Cancel (true) or return
     * null (false) when interrupted during evaluation.
     * @param flag true when cancellable, false otherwise
     */
    public void setCancellable(final boolean flag) {
        flags = set(CANCELLABLE, flags, flag);
    }

    /**
     * Sets whether lambda captured-variables are const or not.
     * <p>
     * When disabled, lambda-captured variables are implicitly converted to read-write local variable (let),
     * when enabled, those are implicitly converted to read-only local variables (const).
     * </p>
     * @param flag true to enable, false to disable
     */
    public void setConstCapture(final boolean flag) {
        flags = set(CONST_CAPTURE, flags, flag);
    }

    /**
     * Sets this option flags using the +/- syntax.
     * @param opts the option flags
     */
    public void setFlags(final String... opts) {
        flags = parseFlags(flags, opts);
    }

    /**
     * Sets the optional set of imports.
     * @param imports the imported packages
     */
    public void setImports(final Collection<String> imports) {
        this.imports = imports == null || imports.isEmpty()? Collections.emptySet() : imports;
    }

    /**
     * Sets whether the engine uses a strict block lexical scope during
     * evaluation.
     * @param flag true if lexical scope is used, false otherwise
     */
    public void setLexical(final boolean flag) {
        flags = set(LEXICAL, flags, flag);
    }

    /**
     * Sets whether the engine strictly shades global variables.
     * Local symbols shade globals after definition and creating global
     * variables is prohibited during evaluation.
     * If setting to lexical shade, lexical scope is also set.
     * @param flag true if creation is allowed, false otherwise
     */
    public void setLexicalShade(final boolean flag) {
        flags = set(SHADE, flags, flag);
        if (flag) {
            flags = set(LEXICAL, flags, true);
        }
    }

    /**
     * Sets the arithmetic math context.
     * @param mcontext the context
     */
    public void setMathContext(final MathContext mcontext) {
        this.mathContext = mcontext;
    }

    /**
     * Sets the arithmetic math scale.
     * @param mscale the scale
     */
    public void setMathScale(final int mscale) {
        this.mathScale = mscale;
    }

    /**
     * Sets the optional map of namespaces.
     * @param ns a namespaces map
     */
    public void setNamespaces(final Map<String, Object> ns) {
        this.namespaces = ns == null || ns.isEmpty()? Collections.emptyMap() : ns;
    }

    /**
     * Sets whether the engine considers null in navigation expression as null or as errors
     * during evaluation.
     * <p>If safe, encountering null during a navigation expression - dereferencing a method or a field through a null
     * object or property - will <em>not</em> be considered an error but evaluated as <em>null</em>. It is recommended
     * to use <em>setSafe(false)</em> as an explicit default.</p>
     * @param flag true if safe, false otherwise
     */
    public void setSafe(final boolean flag) {
        flags = set(SAFE, flags, flag);
    }

    /**
     * Sets wether these options are immutable at runtime.
     * <p>Expert mode; allows instance handled through context to be shared
     * instead of copied.
     * @param flag true if shared, false if not
     */
    public void setSharedInstance(final boolean flag) {
        flags = set(SHARED, flags, flag);
    }

    /**
     * Sets whether the engine will throw a {@link JexlException} when an error
     * is encountered during evaluation.
     * @param flag true if silent, false otherwise
     */
    public void setSilent(final boolean flag) {
        flags = set(SILENT, flags, flag);
    }

    /**
     * Sets whether the engine considers unknown variables, methods and
     * constructors as errors during evaluation.
     * @param flag true if strict, false otherwise
     */
    public void setStrict(final boolean flag) {
        flags = set(STRICT, flags, flag);
    }

    /**
     * Sets the strict arithmetic flag.
     * @param stricta true or false
     */
    public void setStrictArithmetic(final boolean stricta) {
        this.strictArithmetic = stricta;
    }

    /**
     * Sets the strict interpolation flag.
     * <p>When strict, interpolation strings composed only of an expression (ie `${...}`) are evaluated
     * as strings; when not strict, integer results are left untouched.</p>
     * This can affect the results of expressions like <code>map.`${key}`</code> when key is
     * an integer (or a string); it is almost always possible to use <code>map[key]</code> to ensure
     * that the key type is not altered.
     * @param strict true or false
     */
    public void setStrictInterpolation(final boolean strict) {
        flags = set(STRICT_INTERPOLATION, flags, strict);
    }

    @Override public String toString() {
        final StringBuilder strb = new StringBuilder();
        for(int i = 0; i < NAMES.length; ++i) {
            if (i > 0) {
                strb.append(' ');
            }
            strb.append((flags & 1 << i) != 0? '+':'-');
            strb.append(NAMES[i]);
        }
        return strb.toString();
    }

}
