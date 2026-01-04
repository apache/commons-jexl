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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * A set of language feature options.
 * <p>
 * These control <em>syntactical</em> constructs that will throw JexlException.Feature exceptions (a
 * subclass of JexlException.Parsing) when disabled.
 * </p>
 * <p>It is recommended to be explicit in choosing the features you need rather than rely on the default
 * constructor: the 2 convenience methods {@link JexlFeatures#createNone()} and {@link JexlFeatures#createAll()}
 * are the recommended starting points to selectively enable or disable chosen features.</p>
 * <ul>
 * <li>Registers: register syntax (#number), used internally for {g,s}etProperty</li>
 * <li>Reserved Names: a set of reserved variable names that cannot be used as local variable (or parameter) names</li>
 * <li>Global Side Effect : assigning/modifying values on global variables (=, += , -=, ...)</li>
 * <li>Lexical: lexical scope, prevents redefining local variables</li>
 * <li>Lexical Shade: local variables shade globals, prevents confusing a global variable with a local one</li>
 * <li>Side Effect : assigning/modifying values on any variables or left-value</li>
 * <li>Constant Array Reference: ensures array references only use constants;they should be statically solvable.</li>
 * <li>New Instance: creating an instance using new(...)</li>
 * <li>Loops: loop constructs (while(true), for(...))</li>
 * <li>Lambda: function definitions (()-&gt;{...}, function(...) ).</li>
 * <li>Method calls: calling methods (obj.method(...) or obj['method'](...)); when disabled, leaves function calls
 * - including namespace prefixes - available</li>
 * <li>Structured literals: arrays, lists, maps, sets, ranges</li>
 * <li>Pragma: pragma construct as in {@code #pragma x y}</li>
 * <li>Annotation: @annotation statement;</li>
 * <li>Thin-arrow: use the thin-arrow, ie {@code ->} for lambdas as in {@code x -> x + x}</li>
 * <li>Fat-arrow: use the  fat-arrow, ie {@code =>} for lambdas as in {@code x => x + x}
 * <li>Namespace pragma: whether the {@code #pragma jexl.namespace.ns namespace} syntax is allowed</li>
 * <li>Namespace identifier: whether the {@code ns:fun(...)} parser treats the ns:fun as one identifier, no spaces allowed</li>
 * <li>Import pragma: whether the {@code #pragma jexl.import fully.qualified.class.name} syntax is allowed</li>
 * <li>Comparator names: whether the comparator operator names can be used (as in {@code gt} for &gt;,
 * {@code lt} for &lt;, ...)</li>
 * <li>Pragma anywhere: whether pragma, that are <em>not</em> statements and handled before execution begins,
 * can appear anywhere in the source or before any statements - ie at the beginning of a script.</li>
 * <li>Const Capture: whether variables captured by lambdas are read-only (aka const, same as Java) or read-write.</li>
 * <li>Reference Capture: whether variables captured by lambdas are pass-by-reference or pass-by-value.</li>
 * </ul>
 *
 * @since 3.2
 */
public final class JexlFeatures {

    /** The false predicate. */
    public static final Predicate<String> TEST_STR_FALSE = s -> false;

    /** Te feature names (for toString()). */
    private static final String[] F_NAMES = {
        "register", "reserved variable", "local variable", "assign/modify",
        "global assign/modify", "array reference", "create instance", "loop", "function",
        "method call", "set/map/array literal", "pragma", "annotation", "script", "lexical", "lexicalShade",
        "thin-arrow", "fat-arrow", "namespace pragma", "namespace identifier", "import pragma", "comparator names", "pragma anywhere",
        "const capture", "ref capture", "ambiguous statement"
    };

    /** Registers feature ordinal. */
    private static final int REGISTER = 0;

    /** Reserved future feature ordinal (unused as of 3.3.1). */
    public static final int RESERVED = 1;

    /** Locals feature ordinal. */
    public static final int LOCAL_VAR = 2;

    /** Side effects feature ordinal. */
    public static final int SIDE_EFFECT = 3;

    /** Global side effects feature ordinal. */
    public static final int SIDE_EFFECT_GLOBAL = 4;

    /** Expressions allowed in array reference ordinal. */
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

    /** Thin-arrow lambda syntax. */
    public static final int THIN_ARROW = 16;

    /** Fat-arrow lambda syntax. */
    public static final int FAT_ARROW = 17;

    /** Namespace pragma feature ordinal. */
    public static final int NS_PRAGMA = 18;

    /** Namespace syntax as an identifier (no space). */
    public static final int NS_IDENTIFIER = 19;

    /** Import pragma feature ordinal. */
    public static final int IMPORT_PRAGMA = 20;

    /** Comparator names (legacy) syntax. */
    public static final int COMPARATOR_NAMES = 21;

    /** The pragma anywhere feature ordinal. */
    public static final int PRAGMA_ANYWHERE = 22;

    /** Captured variables are const. */
    public static final int CONST_CAPTURE = 23;

    /** Captured variables are reference. */
    public static final int REF_CAPTURE = 24;

    /** Ambiguous or strict statement allowed. */
    public static final int AMBIGUOUS_STATEMENT = 25;

    /** Bad naming, use AMBIGUOUS_STATEMENT.
     * @deprecated 3.6
     */
    @Deprecated
    public static final int STRICT_STATEMENT = 25;

    /**
     * All features.
     * Ensure this is updated if additional features are added.
     */
    private static final long ALL_FEATURES = (1L << AMBIGUOUS_STATEMENT + 1) - 1L; // MUST REMAIN PRIVATE

    /**
     * The default features flag mask.
     * <p>Meant for compatibility with scripts written before 3.3.1</p>
     */
    private static final long DEFAULT_FEATURES = // MUST REMAIN PRIVATE
        1L << LOCAL_VAR
        | 1L << SIDE_EFFECT
        | 1L << SIDE_EFFECT_GLOBAL
        | 1L << ARRAY_REF_EXPR
        | 1L << NEW_INSTANCE
        | 1L << LOOP
        | 1L << LAMBDA
        | 1L << METHOD_CALL
        | 1L << STRUCTURED_LITERAL
        | 1L << PRAGMA
        | 1L << ANNOTATION
        | 1L << SCRIPT
        | 1L << THIN_ARROW
        | 1L << NS_PRAGMA
        | 1L << IMPORT_PRAGMA
        | 1L << COMPARATOR_NAMES
        | 1L << PRAGMA_ANYWHERE;

    /**
     * The canonical scripting (since 3.3.1) features flag mask based on the original default.
     * <p>Adds lexical, lexical-shade and const-capture but removes comparator-names and pragma-anywhere</p>
     */
    private static final long SCRIPT_FEATURES = // MUST REMAIN PRIVATE
        (DEFAULT_FEATURES
        | 1L << LEXICAL
        | 1L << LEXICAL_SHADE
        | 1L << CONST_CAPTURE) // these parentheses are necessary :-)
        & ~(1L << COMPARATOR_NAMES)
        & ~(1L << PRAGMA_ANYWHERE);

    /**
     * Protected future syntactic elements.
     * <p><em>class, jexl, $jexl</em></p>
     *
     * @since 3.3.1
     */
    private static final Set<String> RESERVED_WORDS =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList("class", "jexl", "$jexl")));

    /*
     * *WARNING*
     * Static fields may be inlined by the Java compiler, so their _values_ effectively form part of the external API.
     * Classes that reference them need to be recompiled to pick up new values.
     * This means that changes in value are not binary compatible.
     * Such fields must be private or problems may occur.
     */

     /**
     * Creates an all features enabled set.
     *
     * @return a new instance of all features set
     * @since 3.3.1
     */
    public static JexlFeatures createAll() {
        return new JexlFeatures(ALL_FEATURES, null, null);
    }

    /**
     * Creates a default features set suitable for basic but complete scripting needs.
     * <p>Maximizes compatibility with older version scripts (before 3.3), new projects should
     * use {@link JexlFeatures#createScript()} or equivalent features as a base.</p>
     * <p>The following scripting features are enabled:</p>
     * <ul>
     *   <li>local variable, {@link JexlFeatures#supportsLocalVar()}</li>
     *   <li>side effect, {@link JexlFeatures#supportsSideEffect()}</li>
     *   <li>global side effect, {@link JexlFeatures#supportsSideEffectGlobal()}</li>
     *   <li>array reference expression, {@link JexlFeatures#supportsStructuredLiteral()}</li>
     *   <li>new instance, {@link JexlFeatures#supportsNewInstance()} </li>
     *   <li>loop, {@link JexlFeatures#supportsLoops()}</li>
     *   <li>lambda, {@link JexlFeatures#supportsLambda()}</li>
     *   <li>method call, {@link JexlFeatures#supportsMethodCall()}</li>
     *   <li>structured literal, {@link JexlFeatures#supportsStructuredLiteral()}</li>
     *   <li>pragma, {@link JexlFeatures#supportsPragma()}</li>
     *   <li>annotation, {@link JexlFeatures#supportsAnnotation()}</li>
     *   <li>script, {@link JexlFeatures#supportsScript()}</li>
     *   <li>comparator names,  {@link JexlFeatures#supportsComparatorNames()}</li>
     *   <li>namespace pragma,  {@link JexlFeatures#supportsNamespacePragma()}</li>
     *   <li>import pragma, {@link JexlFeatures#supportsImportPragma()}</li>
     *   <li>pragma anywhere, {@link JexlFeatures#supportsPragmaAnywhere()}</li>
     * </ul>
     *
     * @return a new instance of a default scripting features set
     * @since 3.3.1
     */
    public static JexlFeatures createDefault() {
        return new JexlFeatures(DEFAULT_FEATURES, null, null);
    }

    /**
     * Creates an empty feature set.
     * <p>This is the strictest base-set since no feature is allowed, suitable as-is only
     * for the simplest expressions.</p>
     *
     * @return a new instance of an empty features set
     * @since 3.3.1
     */
    public static JexlFeatures createNone() {
        return new JexlFeatures(0L, null, null);
    }

    /**
     * The modern scripting features set.
     * <p>This is the recommended set for new projects.</p>
     * <p>All default features with the following differences:</p>
     * <ul>
     * <li><em>disable</em> pragma-anywhere, {@link JexlFeatures#supportsPragmaAnywhere()}</li>
     * <li><em>disable</em> comparator-names, {@link JexlFeatures#supportsComparatorNames()}</li>
     * <li><em>enable</em> lexical, {@link JexlFeatures#isLexical()}</li>
     * <li><em>enable</em> lexical-shade, {@link JexlFeatures#isLexicalShade()} </li>
     * <li><em>enable</em> const-capture, {@link JexlFeatures#supportsConstCapture()}</li>
     * </ul>
     * <p>It also adds a set of reserved words to enable future unencumbered syntax evolution:
     * <em>try, catch, throw, finally, switch, case, default, class, instanceof</em>
     * </p>
     *
     * @return a new instance of a modern scripting features set
     * @since 3.3.1
     */
    public static JexlFeatures createScript() {
        return new JexlFeatures(SCRIPT_FEATURES, RESERVED_WORDS, null);
    }

    /**
     * The text corresponding to a feature code.
     *
     * @param feature the feature number
     * @return the feature name
     */
    public static String stringify(final int feature) {
        return feature >= 0 && feature < F_NAMES.length ? F_NAMES[feature] : "unsupported feature";
    }

    /** The feature flags. */
    private long flags;

    /** The set of reserved names, aka global variables that cannot be masked by local variables or parameters. */
    private Set<String> reservedNames;

    /** The namespace names. */
    private Predicate<String> nameSpaces;

    /**
     * Creates default instance, equivalent to the result of calling the preferred alternative
     * {@link JexlFeatures#createDefault()}
     */
    public JexlFeatures() {
        this(DEFAULT_FEATURES, null, null);
    }

    /**
     * Copy constructor.
     *
     * @param features the feature to copy from
     */
    public JexlFeatures(final JexlFeatures features) {
        this(features.flags, features.reservedNames, features.nameSpaces);
    }

    /**
     * An all member constructor for derivation.
     * <p>Not respecting immutability or thread-safety constraints for this class constructor arguments will
     * likely result in unexpected behavior.</p>
     *
     * @param f flag
     * @param r reserved variable names; must be an immutable Set or thread-safe (concurrent or synchronized set)
     * @param n namespace predicate; must be stateless or thread-safe
     */
    protected JexlFeatures(final long f, final Set<String> r, final Predicate<String> n) {
        this.flags = f;
        this.reservedNames = r == null? Collections.emptySet() : r;
        this.nameSpaces = n == null? TEST_STR_FALSE : n;
    }

    /**
     * Sets whether annotation constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic annotation constructs (@annotation)
     * will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures annotation(final boolean flag) {
        setFeature(ANNOTATION, flag);
        return this;
    }

    /**
     * Sets whether array references expressions are enabled.
     * <p>
     * When disabled, parsing a script/expression using 'obj[ ref ]' where ref is not a string or integer literal
     * will throw a parsing exception;
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures arrayReferenceExpr(final boolean flag) {
        setFeature(ARRAY_REF_EXPR, flag);
        return this;
    }

    /**
     * Sets whether the legacy comparison operator names syntax is enabled.
     * <p>
     * When disabled, comparison operators names (eq;ne;le;lt;ge;gt)
     * will be treated as plain identifiers.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     * @since 3.3
     */
    public JexlFeatures comparatorNames(final boolean flag) {
        setFeature(COMPARATOR_NAMES, flag);
        return this;
    }

    /**
     * Sets whether lambda captured-variables are constant or mutable.
     * <p>
     * When disabled, lambda-captured variables are implicitly converted to read-write local variable (let),
     * when enabled, those are implicitly converted to read-only local variables (const).
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures constCapture(final boolean flag) {
        setFeature(CONST_CAPTURE, flag);
        return this;
    }

    /**
     * Sets whether lambda captured-variables are references or values.
     * <p>When variables are pass-by-reference, side effects are visible from inner lexical scopes
     * to outer-scope.</p>
     * <p>
     * When disabled, lambda-captured variables use pass-by-value semantic,
     * when enabled, those use pass-by-reference semantic.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures referenceCapture(final boolean flag) {
        setFeature(REF_CAPTURE, flag);
        return this;
    }

    @Override
    public boolean equals(final Object obj) {
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
        if (this.nameSpaces != other.nameSpaces) {
            return false;
        }
        if (!Objects.equals(this.reservedNames, other.reservedNames)) {
            return false;
        }
        return true;
    }

    /**
     * Sets whether fat-arrow lambda syntax is enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic fat-arrow (=&lt;)
     * will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     * @since 3.3
     */
    public JexlFeatures fatArrow(final boolean flag) {
        setFeature(FAT_ARROW, flag);
        return this;
    }

    /**
     * Gets a feature flag value.
     *
     * @param feature feature ordinal
     * @return true if on, false if off
     */
    private boolean getFeature(final int feature) {
        return (flags & 1L << feature) != 0L;
    }

    /**
     * Gets the feature flags
     *
     * @return these features&quot;s flags
     */
    public long getFlags() {
        return flags;
    }

    /**
     * Gets the immutable set of reserved names.
     *
     * @return the (unmodifiable) set of reserved names.
     */
    public Set<String> getReservedNames() {
        return reservedNames;
    }

    @Override
    public int hashCode() { //CSOFF: MagicNumber
        int hash = 3;
        hash = 53 * hash + (int) (this.flags ^ this.flags >>> 32);
        hash = 53 * hash + (this.reservedNames != null ? this.reservedNames.hashCode() : 0);
        return hash;
    }

    /**
     * Sets whether import pragma constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic import pragma constructs
     * (#pragma jexl.import....) will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     * @since 3.3
     */
    public JexlFeatures importPragma(final boolean flag) {
        setFeature(IMPORT_PRAGMA, flag);
        return this;
    }

    /**
     * Is the lexical scope feature enabled?
     *
     * @return whether lexical scope feature is enabled */
    public boolean isLexical() {
        return getFeature(LEXICAL);
    }

    /**
     * Is the lexical shade feature enabled?
     *
     * @return whether lexical shade feature is enabled */
    public boolean isLexicalShade() {
        return getFeature(LEXICAL_SHADE);
    }

    /**
     * Checks whether a name is reserved.
     *
     * @param name the name to check
     * @return true if reserved, false otherwise
     */
    public boolean isReservedName(final String name) {
        return name != null && reservedNames.contains(name);
    }

    /**
     * Sets whether lambda/function constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic lambda constructs (-&gt;,function)
     * will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures lambda(final boolean flag) {
        setFeature(LAMBDA, flag);
        return this;
    }

    /**
     * Sets whether syntactic lexical mode is enabled.
     *
     * @param flag true means syntactic lexical function scope is in effect, false implies non-lexical scoping
     * @return this features instance
     */
    public JexlFeatures lexical(final boolean flag) {
        setFeature(LEXICAL, flag);
        if (!flag) {
            setFeature(LEXICAL_SHADE, false);
        }
        return this;
    }

    /**
     * Sets whether syntactic lexical shade is enabled.
     *
     * @param flag true means syntactic lexical shade is in effect and implies lexical scope
     * @return this features instance
     */
    public JexlFeatures lexicalShade(final boolean flag) {
        setFeature(LEXICAL_SHADE, flag);
        if (flag) {
            setFeature(LEXICAL, true);
        }
        return this;
    }

    /**
     * Sets whether local variables are enabled.
     * <p>
     * When disabled, parsing a script/expression using a local variable or parameter syntax
     * will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures localVar(final boolean flag) {
        setFeature(LOCAL_VAR, flag);
        return this;
    }

    /**
     * Sets whether looping constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic looping constructs (for, while)
     * will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures loops(final boolean flag) {
        setFeature(LOOP, flag);
        return this;
    }

    /**
     * Sets whether method calls expressions are enabled.
     * <p>
     * When disabled, parsing a script/expression using 'obj.method()'
     * will throw a parsing exception;
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures methodCall(final boolean flag) {
        setFeature(METHOD_CALL, flag);
        return this;
    }

    /**
     * Sets whether namespace pragma constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic namespace pragma constructs
     * (#pragma jexl.namespace....) will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     * @since 3.3
     */
    public JexlFeatures namespacePragma(final boolean flag) {
        setFeature(NS_PRAGMA, flag);
        return this;
    }

    /**
     * Sets whether namespace as identifier syntax is enabled.
     * <p>
     * When enabled, a namespace call must be of the form <code>ns:fun(...)</code> with no
     * spaces between the namespace name and the function.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     * @since 3.5.0
     */
    public JexlFeatures namespaceIdentifier(final boolean flag) {
        setFeature(NS_IDENTIFIER, flag);
        return this;
    }

    /**
     * Gets the declared namespaces test.
     *
     * @return the declared namespaces test.
     */
    public Predicate<String> namespaceTest() {
        return nameSpaces;
    }

    /**
     * Sets a test to determine namespace declaration.
     *
     * @param names the name predicate
     * @return this features instance
     */
    public JexlFeatures namespaceTest(final Predicate<String> names) {
        nameSpaces = names == null ? TEST_STR_FALSE : names;
        return this;
    }

    /**
     * Sets whether creating new instances is enabled.
     * <p>
     * When disabled, parsing a script/expression using 'new(...)' will throw a parsing exception;
     * using a class as functor will fail at runtime.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures newInstance(final boolean flag) {
        setFeature(NEW_INSTANCE, flag);
        return this;
    }

    /**
     * Sets whether pragma constructs are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic pragma constructs (#pragma)
     * will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures pragma(final boolean flag) {
        setFeature(PRAGMA, flag);
        if (!flag) {
            setFeature(NS_PRAGMA, false);
            setFeature(IMPORT_PRAGMA, false);
        }
        return this;
    }

    /**
     * Sets whether pragma constructs can appear anywhere in the code.
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     * @since 3.3
     */
    public JexlFeatures pragmaAnywhere(final boolean flag) {
        setFeature(PRAGMA_ANYWHERE, flag);
        return this;
    }

    /**
     * Sets whether register are enabled.
     * <p>
     * This is mostly used internally during execution of JexlEngine.{g,s}etProperty.
     * </p>
     * <p>
     * When disabled, parsing a script/expression using the register syntax will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures register(final boolean flag) {
        setFeature(REGISTER, flag);
        return this;
    }

    /**
     * Sets a collection of reserved r precluding those to be used as local variables or parameter r.
     *
     * @param names the r to reserve
     * @return this features instance
     */
    public JexlFeatures reservedNames(final Collection<String> names) {
        if (names == null || names.isEmpty()) {
            reservedNames = Collections.emptySet();
        } else {
            reservedNames = Collections.unmodifiableSet(new TreeSet<>(names));
        }
        return this;
    }

    /**
     * Sets whether scripts constructs are enabled.
     * <p>
     * When disabled, parsing a script using syntactic script constructs (statements, ...)
     * will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures script(final boolean flag) {
        setFeature(SCRIPT, flag);
        return this;
    }

    /**
     * Sets a feature flag.
     *
     * @param feature the feature ordinal
     * @param flag    turn-on, turn off
     */
    private void setFeature(final int feature, final boolean flag) {
        if (flag) {
            flags |= 1L << feature;
        } else {
            flags &= ~(1L << feature);
        }
    }

    /**
     * Sets whether statements can be ambiguous.
     * <p>
     * When enabled, the semicolumn is not required between expressions that otherwise are considered
     * ambiguous. The default will report ambiguity in cases like <code>if (true) { x 5 }</code> considering this
     * may be missing an operator or that the intent is not clear.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures ambiguousStatement(final boolean flag) {
        setFeature(AMBIGUOUS_STATEMENT, flag);
        return this;
    }

    /**
     * Checks whether statements can be ambiguous.
     * <p>
     * When enabled, the semicolumn is not required between expressions that otherwise are considered
     * ambiguous. The default will report ambiguity in cases like <code>if (true) { x 5 }</code> considering this
     * may be missing an operator or that the intent is not clear.
     * </p>
     *
     * @return true if statements can be ambiguous, false otherwise
     */
    public boolean supportsAmbiguousStatement() {
        return getFeature(AMBIGUOUS_STATEMENT);
    }

    /**
     * Sets whether side effect expressions are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactical constructs modifying variables
     * or members will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures sideEffect(final boolean flag) {
        setFeature(SIDE_EFFECT, flag);
        return this;
    }

    /**
     * Sets whether side effect expressions on global variables (aka non-local) are enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactical constructs modifying variables
     * <em>including all potentially ant-ish variables</em> will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures sideEffectGlobal(final boolean flag) {
        setFeature(SIDE_EFFECT_GLOBAL, flag);
        return this;
    }

    /**
     * Sets whether array/map/set literal expressions are enabled.
     * <p>
     * When disabled, parsing a script/expression creating one of these literals
     * will throw a parsing exception;
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     */
    public JexlFeatures structuredLiteral(final boolean flag) {
        setFeature(STRUCTURED_LITERAL, flag);
        return this;
    }

    /**
     * Does the engine support annotations?
     *
     * @return true if annotation are enabled, false otherwise
     */
    public boolean supportsAnnotation() {
        return getFeature(ANNOTATION);
    }

    /**
     * Does the engine support array references which contain method call expressions?
     *
     * @return true if array references can contain method call expressions, false otherwise
     */
    public boolean supportsArrayReferenceExpr() {
        return getFeature(ARRAY_REF_EXPR);
    }

    /**
     * Does the engine support legacy comparison operator names syntax?
     *
     * @return true if legacy comparison operator names syntax is enabled, false otherwise
     * @since 3.3
     */
    public boolean supportsComparatorNames() {
        return getFeature(COMPARATOR_NAMES);
    }

    /**
     * Does the engine support lambda captured-variables as const?
     *
     * @return true if lambda captured-variables are const, false otherwise
     */
    public boolean supportsConstCapture() {
        return getFeature(CONST_CAPTURE);
    }

    /**
     * Does the engine support lambda captured-variables as references?
     *
     * @return true if lambda captured-variables are references, false otherwise
     */
    public boolean supportsReferenceCapture() {
        return getFeature(REF_CAPTURE);
    }

    /**
     * Does the engine support expressions (aka not scripts)
     *
     * @return true if expressions (aka not scripts) are enabled, false otherwise
     */
    public boolean supportsExpression() {
        return !getFeature(SCRIPT);
    }

    /**
     * Does the engine support fat-arrow lambda syntax?
     *
     * @return true if fat-arrow lambda syntax is enabled, false otherwise
     * @since 3.3
     */
    public boolean supportsFatArrow() {
        return getFeature(FAT_ARROW);
    }

    /**
     * Does the engine support import pragma?
     *
     * @return true if import pragma are enabled, false otherwise
     * @since 3.3
     */
    public boolean supportsImportPragma() {
        return getFeature(IMPORT_PRAGMA);
    }

    /**
     * Does the engine support lambdas?
     *
     * @return true if lambda are enabled, false otherwise
     */
    public boolean supportsLambda() {
        return getFeature(LAMBDA);
    }

    /**
     * Is local variables syntax enabled?
     *
     * @return true if local variables syntax is enabled
     */
    public boolean supportsLocalVar() {
        return getFeature(LOCAL_VAR);
    }

    /**
     * Are loops enabled?
     *
     * @return true if loops are enabled, false otherwise
     */
    public boolean supportsLoops() {
        return getFeature(LOOP);
    }

    /**
     * Can array references contain expressions?
     *
     * @return true if array references can contain expressions, false otherwise
     */
    public boolean supportsMethodCall() {
        return getFeature(METHOD_CALL);
    }

    /**
     * Is namespace pragma enabled?
     *
     * @return true if namespace pragma are enabled, false otherwise
     * @since 3.3
     */
    public boolean supportsNamespacePragma() {
        return getFeature(NS_PRAGMA);
    }

    /**
     * Is namespace identifier syntax enabled?
     *
     * @return true if namespace identifier syntax is enabled, false otherwise
     * @since 3.5.0
     */
    public boolean supportsNamespaceIdentifier() {
        return getFeature(NS_IDENTIFIER);
    }

    /**
     * Is creating new instances enabled?
     *
     * @return true if creating new instances is enabled, false otherwise
     */
    public boolean supportsNewInstance() {
        return getFeature(NEW_INSTANCE);
    }

    /**
     * Is the namespace pragma enabled?
     *
     * @return true if namespace pragma are enabled, false otherwise
     */
    public boolean supportsPragma() {
        return getFeature(PRAGMA);
    }

    /**
     * Can pragma constructs appear anywhere in the code?
     *
     * @return true if pragma constructs can appear anywhere in the code, false otherwise
     * @since 3.3
     */
    public boolean supportsPragmaAnywhere() {
        return getFeature(PRAGMA_ANYWHERE);
    }

    /**
     * Is register syntax enabled?
     *
     * @return true if register syntax is enabled
     */
    public boolean supportsRegister() {
        return getFeature(REGISTER);
    }

    /**
     * Are scripts enabled?
     *
     * @return true if scripts are enabled, false otherwise
     */
    public boolean supportsScript() {
        return getFeature(SCRIPT);
    }

    /**
     * Are side effects enabled?
     *
     * @return true if side effects are enabled, false otherwise
     */
    public boolean supportsSideEffect() {
        return getFeature(SIDE_EFFECT);
    }

    /**
     * Can global variables be assigned?
     *
     * @return true if global variables can be assigned
     */
    public boolean supportsSideEffectGlobal() {
        return getFeature(SIDE_EFFECT_GLOBAL);
    }

    /**
     * Are array/map/set literal expressions supported?
     *
     * @return true if array/map/set literal expressions are supported, false otherwise
     */
    public boolean supportsStructuredLiteral() {
        return getFeature(STRUCTURED_LITERAL);
    }

    /**
     * Is thin-arrow lambda syntax enabled?
     *
     * @return true if thin-arrow lambda syntax is enabled, false otherwise
     * @since 3.3
     */
    public boolean supportsThinArrow() {
        return getFeature(THIN_ARROW);
    }

    /**
     * Sets whether thin-arrow lambda syntax is enabled.
     * <p>
     * When disabled, parsing a script/expression using syntactic thin-arrow (-&lt;)
     * will throw a parsing exception.
     * </p>
     *
     * @param flag true to enable, false to disable
     * @return this features instance
     * @since 3.3
     */
    public JexlFeatures thinArrow(final boolean flag) {
        setFeature(THIN_ARROW, flag);
        return this;
    }
}
