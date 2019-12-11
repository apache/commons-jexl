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
 * These control <em>syntactical</em> constructs that will throw JexlException.Feature exceptions (a
 * subclass of JexlException.Parsing) when disabled.
 * <ul>
 * <li>Registers: register syntax (#number), used internally for {g,s}etProperty
 * <li>Reserved Names: a set of reserved variable names that can not be used as local variable (or parameter) names
 * <li>Global Side Effect : assigning/modifying values on global variables (=, += , -=, ...)
 * <li>Lexical: lexical scope, prevents redefining local variables 
 * <li>Lexical Shade: local variables shade globals, prevents confusing a global variable with a local one
 * <li>Side Effect : assigning/modifying values on any variables or left-value
 * <li>Constant Array Reference: ensures array references only use constants;they should be statically solvable.
 * <li>New Instance: creating an instance using new(...)
 * <li>Loops: loop constructs (while(true), for(...))
 * <li>Lambda: function definitions (()-&gt;{...}, function(...) ).
 * <li>Method calls: calling methods (obj.method(...) or obj['method'](...)); when disabled, leaves function calls
 * - including namespace prefixes - available
 * <li>Structured literals: arrays, lists, maps, sets, ranges
 * <li>Pragmas: #pragma x y
 * <li>Annotation: @annotation statement;
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
        "global assign/modify", "array reference", "create instance", "loop", "function",
        "method call", "set/map/array literal", "pragma", "annotation", "script", "lexical", "lexicalShade"
    };
    /** Registers feature ordinal. */
    private static final int REGISTER = 0;
    /** Reserved name feature ordinal. */
    public static final int RESERVED = 1;
    /** Locals feature ordinal. */
    public static final int LOCAL_VAR = 2;
    /** Side-effects feature ordinal. */
    public static final int SIDE_EFFECT = 3;
    /** Global side-effects feature ordinal. */
    public static final int SIDE_EFFECT_GLOBAL = 4;
    /** Array get is allowed on expr. */
    public static final int ARRAY_REF_EXPR = 5;
    /** New-instance feature ordinal. */
    public static final int NEW_INSTANCE = 6;
    /** Loops feature ordinal. */
    public static final int LOOP = 7;
    /** Lambda feature ordinal. */
    public static final int LAMBDA = 8;
    /** Lambda feature ordinal. */
    public static final int METHOD_CALL = 9;
    /** Structured literal feature ordinal. */
    public static final int STRUCTURED_LITERAL = 10;
    /** Pragma feature ordinal. */
    public static final int PRAGMA = 11;
    /** Annotation feature ordinal. */
    public static final int ANNOTATION = 12;
    /** Script feature ordinal. */
    public static final int SCRIPT = 13;
    /** Lexical feature ordinal. */
    public static final int LEXICAL = 14;
    /** Lexical shade feature ordinal. */
    public static final int LEXICAL_SHADE = 15;

    /**
     * Creates an all-features-enabled instance.
     */
    public JexlFeatures() {
        flags = (1L << LOCAL_VAR)
                | (1L << SIDE_EFFECT)
                | (1L << SIDE_EFFECT_GLOBAL)
                | (1L << ARRAY_REF_EXPR)
                | (1L << NEW_INSTANCE)
                | (1L << LOOP)
                | (1L << LAMBDA)
                | (1L << METHOD_CALL)
                | (1L << STRUCTURED_LITERAL)
                | (1L << PRAGMA)
                | (1L << ANNOTATION)
                | (1L << SCRIPT);
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

    @Override
    public int hashCode() { //CSOFF: MagicNumber
        int hash = 3;
        hash = 53 * hash + (int) (this.flags ^ (this.flags >>> 32));
        hash = 53 * hash + (this.reservedNames != null ? this.reservedNames.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JexlFeatures other = (JexlFeatures) obj;
        if (this.flags != other.flags) {
            return false;
        }
        if (this.reservedNames != other.reservedNames
                && (this.reservedNames == null || !this.reservedNames.equals(other.reservedNames))) {
            return false;
        }
        return true;
    }

    /**
     * The text corresponding to a feature code.
     * @param feature the feature number
     * @return the feature name
     */
    public static String stringify(int feature) {
        return feature >= 0 && feature < F_NAMES.length ? F_NAMES[feature] : "unsupported feature";
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
        return name != null && reservedNames.contains(name);
    }

    /**
     * Sets a feature flag.
     * @param feature the feature ordinal
     * @param flag    turn-on, turn off
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
     * Sets whether register are enabled.
     * <p>
     * This is mostly used internally during execution of JexlEngine.{g,s}etProperty.
     * <p>
     * When disabled, parsing a script/expression using the register syntax will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures register(boolean flag) {
        setFeature(REGISTER, flag);
        return this;
    }

    /**
     * @return true if register syntax is enabled
     */
    public boolean supportsRegister() {
        return getFeature(REGISTER);
    }

    /**
     * Sets whether local variables are enabled.
     * <p>
     * When disabled, parsing a script/expression using a local variable or parameter syntax
     * will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures localVar(boolean flag) {
        setFeature(LOCAL_VAR, flag);
        return this;
    }

    /**
     * @return true if local variables syntax is enabled
     */
    public boolean supportsLocalVar() {
        return getFeature(LOCAL_VAR);
    }

    /**
     * Sets whether side effect expressions on global variables (aka non local) are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactical constructs modifying variables
     * <em>including all potentially ant-ish variables</em> will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures sideEffectGlobal(boolean flag) {
        setFeature(SIDE_EFFECT_GLOBAL, flag);
        return this;
    }

    /**
     * @return true if global variables can be assigned
     */
    public boolean supportsSideEffectGlobal() {
        return getFeature(SIDE_EFFECT_GLOBAL);
    }

    /**
     * Sets whether side effect expressions are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactical constructs modifying variables
     * or members will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures sideEffect(boolean flag) {
        setFeature(SIDE_EFFECT, flag);
        return this;
    }

    /**
     * @return true if side effects are enabled, false otherwise
     */
    public boolean supportsSideEffect() {
        return getFeature(SIDE_EFFECT);
    }

    /**
     * Sets whether array references expressions are enabled.
     * <p>
     * When disabled, parsing a script/expression using 'obj[ ref ]' where ref is not a string or integer literal
     * will throw a parsing exception;
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures arrayReferenceExpr(boolean flag) {
        setFeature(ARRAY_REF_EXPR, flag);
        return this;
    }

    /**
     * @return true if array references can contain method call expressions, false otherwise
     */
    public boolean supportsArrayReferenceExpr() {
        return getFeature(ARRAY_REF_EXPR);
    }

    /**
     * Sets whether method calls expressions are enabled.
     * <p>
     * When disabled, parsing a script/expression using 'obj.method()'
     * will throw a parsing exception;
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures methodCall(boolean flag) {
        setFeature(METHOD_CALL, flag);
        return this;
    }

    /**
     * @return true if array references can contain expressions, false otherwise
     */
    public boolean supportsMethodCall() {
        return getFeature(METHOD_CALL);
    }

    /**
     * Sets whether array/map/set literal expressions are enabled.
     * <p>
     * When disabled, parsing a script/expression creating one of these literals
     * will throw a parsing exception;
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures structuredLiteral(boolean flag) {
        setFeature(STRUCTURED_LITERAL, flag);
        return this;
    }

    /**
     * @return true if array/map/set literal expressions are supported, false otherwise
     */
    public boolean supportsStructuredLiteral() {
        return getFeature(STRUCTURED_LITERAL);
    }

    /**
     * Sets whether creating new instances is enabled.
     * <p>
     * When disabled, parsing a script/expression using 'new(...)' will throw a parsing exception;
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
     * <p>
     * When disabled, parsing a script/expression using syntactic looping constructs (for,while)
     * will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures loops(boolean flag) {
        setFeature(LOOP, flag);
        return this;
    }

    /**
     * @return true if loops are enabled, false otherwise
     */
    public boolean supportsLoops() {
        return getFeature(LOOP);
    }

    /**
     * Sets whether lambda/function constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic lambda constructs (-&gt;,function)
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

    /**
     * Sets whether pragma constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic pragma constructs (#pragma)
     * will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures pragma(boolean flag) {
        setFeature(PRAGMA, flag);
        return this;
    }

    /**
     * @return true if pragma are enabled, false otherwise
     */
    public boolean supportsPragma() {
        return getFeature(PRAGMA);
    }

    /**
     * Sets whether annotation constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic annotation constructs (@annotation)
     * will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures annotation(boolean flag) {
        setFeature(ANNOTATION, flag);
        return this;
    }

    /**
     * @return true if annotation are enabled, false otherwise
     */
    public boolean supportsAnnotation() {
        return getFeature(ANNOTATION);
    }

    /**
     * Sets whether scripts constructs are enabled.
     * <p>
     * When disabled, parsing a script using syntactic script constructs (statements, ...)
     * will throw a parsing exception.
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures script(boolean flag) {
        setFeature(SCRIPT, flag);
        return this;
    }

    /**
     * @return true if scripts are enabled, false otherwise
     */
    public boolean supportsScript() {
        return getFeature(SCRIPT);
    }

    /**
     *
     * @return true if expressions (aka not scripts) are enabled, false otherwise
     */
    public boolean supportsExpression() {
        return !getFeature(SCRIPT);
    }

    /**
     * Sets whether syntactic lexical mode is enabled.
     *
     * @param flag true means syntactic lexical function scope is in effect, false implies non-lexical scoping 
     * @return this features instance
     */
    public JexlFeatures lexical(boolean flag) {
        setFeature(LEXICAL, flag);
        return this;
    }
    
    
    /** @return whether lexical scope feature is enabled */
    public boolean isLexical() {
        return getFeature(LEXICAL);
    }
        
    /**
     * Sets whether syntactic lexical shade is enabled.
     *
     * @param flag true means syntactic lexical shade is in effect and implies lexical scope
     * @return this features instance
     */
    public JexlFeatures lexicalShade(boolean flag) {
        setFeature(LEXICAL_SHADE, flag);
        if (flag) {
            setFeature(LEXICAL, true);
        }
        return this;
    }
    
    
    /** @return whether lexical shade feature is enabled */
    public boolean isLexicalShade() {
        return getFeature(LEXICAL_SHADE);
    }
}
