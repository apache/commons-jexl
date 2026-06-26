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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.MathContext;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlUberspect;

/**
 * Loads a YAML configuration file and applies it to a {@link JexlBuilder}.
 *
 * <p>Quick start:</p>
 * <pre>
 * try (InputStream in = getClass().getResourceAsStream("/jexl.yaml")) {
 *     JexlEngine engine = JexlConfigLoader.load(in).create();
 * }
 * </pre>
 *
 * <p>The loader understands a simple YAML subset: top-level scalars, one level of section nesting,
 * and list items.  No external YAML library is required.  Inline comments ({@code #} after
 * whitespace), empty lines, and single/double-quoted string values are supported.</p>
 *
 * <h2>Top-level scalar keys</h2>
 * <p>These map directly to the matching {@link JexlBuilder} setter.  Boolean values accept
 * {@code true/false}, {@code yes/no}, or {@code on/off}.  Example:</p>
 * <pre>
 * strict: true         # JexlBuilder.strict(true)
 * silent: false        # JexlBuilder.silent(false)
 * safe: true           # JexlBuilder.safe(true)   — enable safe-navigation ?.
 * cancellable: false   # JexlBuilder.cancellable(false)
 * antish: true         # JexlBuilder.antish(true) — resolve ant-style dotted names
 * lexical: true        # JexlBuilder.lexical(true)
 * lexicalShade: false  # JexlBuilder.lexicalShade(false)
 * strictInterpolation: false
 * booleanLogical: false
 * debug: true          # include source location in exceptions
 * cache: 512           # expression cache size (entries)
 * cacheThreshold: 64   # max expression length cached
 * stackOverflow: 512   # max recursion depth
 * collectMode: 1       # variable collection mode (0=off, 1=on)
 * charset: UTF-8       # source charset for script text
 * strategy: JEXL_STRATEGY   # property-resolver order: JEXL_STRATEGY or MAP_STRATEGY
 * </pre>
 *
 * <h2>{@code permissions:} section</h2>
 * <p>Controls which classes and members are visible to scripts.</p>
 * <pre>
 * permissions:
 *   base: SECURE           # NONE (default) | SECURE | RESTRICTED | UNRESTRICTED
 *   rules:                 # list of JexlPermissions DSL strings composed on top of base
 *     - "com.example.api +{}"           # allow whole package
 *     - "com.example.api +{ Foo{} }"    # allow specific class
 *     - "java.util +{ -Formatter { Formatter(); } }"  # deny one constructor
 *   classes:               # explicit fully-qualified class names to allow (ClassPermissions)
 *     - com.example.api.Foo
 *     - com.example.api.Bar
 *   logging: my.logger     # optional: wrap in LoggingPermissions; value = logger name
 *                          # bare "logging:" with no value uses the default logger
 * </pre>
 * <p>See {@link JexlPermissions#parse(String...)} for the DSL syntax,
 * {@link JexlPermissions#SECURE}, {@link JexlPermissions#RESTRICTED},
 * {@link JexlPermissions#logging()} for the logging wrapper.</p>
 *
 * <h2>{@code features:} section</h2>
 * <p>Controls which syntactic constructs are available at parse time.  Each key is the name of
 * a {@link JexlFeatures} boolean setter method; unknown keys are silently ignored.</p>
 * <pre>
 * features:
 *   # Script structure
 *   script: true             # multi-statement scripts (vs. single-expression mode)
 *   localVar: true           # local variable declarations (var x = ...)
 *   lambda: true             # lambda / named-function definitions
 *   loops: true              # for / while / do-while loops
 *   newInstance: true        # new(...) constructor calls
 *
 *   # Side-effects
 *   sideEffect: true         # any assignment or modification (=, +=, ...)
 *   sideEffectGlobal: true   # assignment to context / global variables
 *
 *   # Literals and operators
 *   structuredLiteral: true  # array [], map {}, set {} literals; ranges a..b
 *   arrayReferenceExpr: true # non-constant array index expressions (x[f()])
 *   methodCall: true         # method calls on objects (obj.method())
 *   annotation: true         # @annotation statements
 *   pragma: true             # #pragma directives
 *   pragmaAnywhere: true     # allow #pragma anywhere (not just at the top)
 *   namespacePragma: true    # #pragma jexl.namespace.ns ... syntax
 *   namespaceIdentifier: true# ns:fun(...) compact namespace call syntax
 *   importPragma: true       # #pragma jexl.import ... syntax
 *
 *   # Lambda arrow styles
 *   thinArrow: true          # thin-arrow lambdas  x -&gt; x + 1
 *   fatArrow: true           # fat-arrow lambdas   x =&gt; x + 1
 *
 *   # Variable capture semantics
 *   constCapture: true       # captured variables are read-only (Java-style)
 *   referenceCapture: false  # captured variables are pass-by-reference (ECMAScript-style)
 *
 *   # Lexical scoping (also settable at top level via 'lexical:' / 'lexicalShade:')
 *   lexical: true
 *   lexicalShade: false
 *
 *   # Misc
 *   comparatorNames: true    # allow 'gt', 'lt', 'ge', 'le', 'eq', 'ne' as operator aliases
 *   ambiguousStatement: true # allow statements that are syntactically ambiguous
 *   ignoreTemplatePrefix: false
 *
 *   # Reserved names (list — cannot be used as local variable or parameter names)
 *   reservedNames:
 *     - try
 *     - catch
 *     - class
 * </pre>
 *
 * <h2>{@code arithmetic:} section</h2>
 * <p>Selects and configures the {@link JexlArithmetic} implementation.</p>
 * <pre>
 * arithmetic:
 *   clazz: org.apache.commons.jexl3.JexlArithmetic  # fully-qualified class (default)
 *   strict: true       # strict arithmetic (true = default); passed to the constructor
 *   mathContext: DECIMAL64   # java.math.MathContext field: DECIMAL32 | DECIMAL64 | DECIMAL128 | UNLIMITED
 *   mathScale: 10      # BigDecimal scale; requires mathContext; -1 = use context default
 * </pre>
 * <p>When {@code mathContext} is present the constructor {@code (boolean, MathContext, int)} is used;
 * otherwise {@code (boolean)} is used.  The class must be on the classpath.</p>
 *
 * <h2>{@code namespaces:} section</h2>
 * <p>Maps namespace prefixes to fully-qualified class names.  The class is loaded and passed to
 * {@link JexlBuilder#namespaces(java.util.Map)}.  Its static (or instance) methods become
 * callable as {@code prefix:methodName(args)} from scripts.</p>
 * <pre>
 * namespaces:
 *   math: java.lang.Math          # math:abs(-1) etc.
 *   str:  com.example.StringUtils # str:trim(x) etc.
 * </pre>
 *
 * <h2>{@code imports:} section</h2>
 * <p>A list of package or class names passed to {@link JexlBuilder#imports(java.util.Collection)}.
 * Imported packages allow unqualified class names in {@code new(...)} and type references.</p>
 * <pre>
 * imports:
 *   - java.lang
 *   - java.util
 *   - com.example.api
 * </pre>
 *
 * <h2>Complete annotated example</h2>
 * <p>Every flag is listed explicitly so the configuration does not depend on any library default
 * (which may change between releases).  The {@code features:} block below reproduces the pre-3.7
 * feature set ({@link JexlFeatures#createDefault()}).</p>
 * <pre>
 * # Production engine — explicit permissions + legacy feature set
 * strict: true
 * safe: false
 * cache: 512
 *
 * permissions:
 *   base: RESTRICTED
 *   rules:
 *     - "com.example.api +{}"
 *   logging: com.example.jexl.permissions  # log allow/deny at INFO once per element
 *
 * features:
 *   script: true
 *   localVar: true
 *   lambda: true
 *   loops: true
 *   newInstance: true
 *   sideEffect: true
 *   sideEffectGlobal: true
 *   structuredLiteral: true
 *   arrayReferenceExpr: true
 *   methodCall: true
 *   annotation: true
 *   pragma: true
 *   pragmaAnywhere: true
 *   namespacePragma: true
 *   importPragma: true
 *   namespaceIdentifier: false
 *   thinArrow: true
 *   fatArrow: false
 *   constCapture: false
 *   referenceCapture: false
 *   lexical: false
 *   lexicalShade: false
 *   comparatorNames: true
 *   ambiguousStatement: false
 *   ignoreTemplatePrefix: false
 *
 * namespaces:
 *   math: java.lang.Math
 *
 * imports:
 *   - java.lang
 *   - java.util
 * </pre>
 *
 * @since 3.7.0
 */
public final class JexlConfigLoader {

    private JexlConfigLoader() { }

    /**
     * Loads configuration from a YAML {@link InputStream} (UTF-8) into a {@link JexlBuilder}.
     *
     * @param in YAML input; the caller is responsible for closing it
     * @return a configured JexlBuilder
     * @throws IOException if the stream cannot be read
     */
    public static JexlBuilder load(final InputStream in) throws IOException {
        return load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Loads configuration from a YAML {@link Reader} into a {@link JexlBuilder}.
     *
     * @param reader YAML input; the caller is responsible for closing it
     * @return a configured JexlBuilder
     * @throws IOException if the reader cannot be read
     */
    public static JexlBuilder load(final Reader reader) throws IOException {
        final JexlBuilder builder = new JexlBuilder();
        final JexlFeatures features = new JexlFeatures();
        parse(reader instanceof BufferedReader ? (BufferedReader) reader
            : new BufferedReader(reader), builder, features);
        return builder.features(features);
    }

    /**
     * Convenience: loads YAML from {@code in} and creates the engine in one call.
     *
     * @param in YAML input; the caller is responsible for closing it
     * @return a new JexlEngine configured from the YAML
     * @throws IOException if the stream cannot be read
     */
    public static JexlEngine engine(final InputStream in) throws IOException {
        return load(in).create();
    }

    // =========================================================================
    //  Parser
    // =========================================================================

    /**
     * Minimal line-by-line YAML parser that handles the subset needed for JEXL configuration:
     * top-level key:value scalars, one-level section blocks, and list items within sections.
     */
    private static void parse(final BufferedReader reader,
                              final JexlBuilder builder,
                              final JexlFeatures features) throws IOException {
        String section = null;
        Map<String, Object> sectionData = null;
        List<String> currentList = null;
        String currentListKey = null;

        for (String raw; (raw = reader.readLine()) != null; ) {
            final int indent = leadingSpaces(raw);
            final String stripped = ltrim(raw);
            if (stripped.isEmpty() || stripped.charAt(0) == '#') {
                continue;
            }
            final String line = stripComment(stripped);
            if (line.isEmpty()) {
                continue;
            }

            if (indent == 0) {
                // back to top level — flush any open section
                if (section != null) {
                    if (currentList != null && currentListKey != null) {
                        sectionData.put(currentListKey, currentList);
                    }
                    flushSection(section, sectionData, builder, features);
                    section = null;
                    sectionData = null;
                    currentList = null;
                    currentListKey = null;
                }

                final int colon = line.indexOf(':');
                final String key = colon < 0 ? line : rtrim(line.substring(0, colon));
                final String val = colon < 0 ? "" : ltrim(line.substring(colon + 1));

                if (val.isEmpty()) {
                    section = key;
                    sectionData = new LinkedHashMap<>();
                } else {
                    applyTopLevel(key, val, builder);
                }

            } else {
                // inside a section
                if (section == null) {
                    continue;
                }

                if (line.charAt(0) == '-') {
                    // list item
                    final String item = unquote(ltrim(line.substring(1)));
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                        currentListKey = section;
                    }
                    currentList.add(item);
                } else {
                    final int colon = line.indexOf(':');
                    final String key = colon < 0 ? line : rtrim(line.substring(0, colon));
                    final String val = colon < 0 ? "" : ltrim(line.substring(colon + 1));

                    if (val.isEmpty()) {
                        // nested list-key start (e.g. "rules:")
                        if (currentList != null && currentListKey != null) {
                            sectionData.put(currentListKey, currentList);
                        }
                        currentListKey = key;
                        currentList = new ArrayList<>();
                    } else {
                        // scalar within section — close any pending list first
                        if (currentList != null && currentListKey != null) {
                            sectionData.put(currentListKey, currentList);
                            currentList = null;
                            currentListKey = null;
                        }
                        sectionData.put(key, val);
                    }
                }
            }
        }

        // flush the last section
        if (section != null) {
            if (currentList != null && currentListKey != null) {
                sectionData.put(currentListKey, currentList);
            }
            flushSection(section, sectionData, builder, features);
        }
    }

    // =========================================================================
    //  Section dispatchers
    // =========================================================================

    @SuppressWarnings("unchecked")
    private static void flushSection(final String section,
                                     final Map<String, Object> data,
                                     final JexlBuilder builder,
                                     final JexlFeatures features) {
        switch (section) {
            case "permissions":
                applyPermissions(data, builder);
                break;
            case "namespaces":
                applyNamespaces(data, builder);
                break;
            case "arithmetic":
                applyArithmetic(data, builder);
                break;
            case "features":
                applyFeatures(data, features);
                break;
            case "imports": {
                final Object list = data.get(section);
                if (list instanceof List) {
                    builder.imports((List<String>) list);
                }
                break;
            }
            default:
                break;
        }
    }

    private static void applyTopLevel(final String key,
                                      final String value,
                                      final JexlBuilder builder) {
        switch (key) {
            case "strict":              builder.strict(parseBool(value)); break;
            case "silent":              builder.silent(parseBool(value)); break;
            case "safe":                builder.safe(parseBool(value)); break;
            case "cancellable":         builder.cancellable(parseBool(value)); break;
            case "antish":              builder.antish(parseBool(value)); break;
            case "lexical":             builder.lexical(parseBool(value)); break;
            case "lexicalShade":        builder.lexicalShade(parseBool(value)); break;
            case "strictInterpolation": builder.strictInterpolation(parseBool(value)); break;
            case "booleanLogical":      builder.booleanLogical(parseBool(value)); break;
            case "debug":               builder.debug(parseBool(value)); break;
            case "cache":               builder.cache(parseInt(value)); break;
            case "cacheThreshold":      builder.cacheThreshold(parseInt(value)); break;
            case "stackOverflow":       builder.stackOverflow(parseInt(value)); break;
            case "collectMode":         builder.collectMode(parseInt(value)); break;
            case "charset":             builder.charset(Charset.forName(value)); break;
            case "strategy":            builder.strategy(parseStrategy(value)); break;
            default:
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyPermissions(final Map<String, Object> data,
                                         final JexlBuilder builder) {
        final Object baseVal = data.get("base");
        // absent or "NONE" → deny-everything base (build from scratch with rules/classes)
        JexlPermissions perms = "UNRESTRICTED".equals(baseVal) ? JexlPermissions.UNRESTRICTED
            : "SECURE".equals(baseVal) ? JexlPermissions.SECURE
            : "RESTRICTED".equals(baseVal) ? JexlPermissions.RESTRICTED
            : JexlPermissions.NONE;
        final Object rules = data.get("rules");
        if (rules instanceof List && !((List<?>) rules).isEmpty()) {
            final List<String> ruleList = (List<String>) rules;
            perms = perms.compose(ruleList.toArray(new String[0]));
        }
        final Object classes = data.get("classes");
        if (classes instanceof List && !((List<?>) classes).isEmpty()) {
            perms = new JexlPermissions.ClassPermissions(perms, (List<String>) classes);
        }
        // optional logging wrapper, outermost so it reports the effective decisions;
        // a String value names the logger, a bare "logging:" uses the default logger
        final Object logging = data.get("logging");
        if (logging != null) {
            final String name = logging instanceof String ? (String) logging : "";
            perms = name.isEmpty() ? perms.logging() : perms.logging(name);
        }
        builder.permissions(perms);
    }

    private static void applyNamespaces(final Map<String, Object> data,
                                        final JexlBuilder builder) {
        final Map<String, Object> ns = new LinkedHashMap<>(data.size());
        for (final Map.Entry<String, Object> e : data.entrySet()) {
            if (e.getValue() instanceof String) {
                try {
                    ns.put(e.getKey(), Class.forName((String) e.getValue()));
                } catch (final ClassNotFoundException ex) {
                    throw new IllegalArgumentException(
                        "namespace class not found: " + e.getValue(), ex);
                }
            }
        }
        if (!ns.isEmpty()) {
            builder.namespaces(ns);
        }
    }

    private static void applyArithmetic(final Map<String, Object> data,
                                        final JexlBuilder builder) {
        final String className = data.containsKey("clazz")
            ? (String) data.get("clazz")
            : JexlArithmetic.class.getName();
        final boolean astrict = !data.containsKey("strict") || parseBool((String) data.get("strict"));
        final Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("arithmetic class not found: " + className, e);
        }
        try {
            if (data.containsKey("mathContext")) {
                final MathContext mc = (MathContext)
                    MathContext.class.getField((String) data.get("mathContext")).get(null);
                final int scale = data.containsKey("mathScale")
                    ? parseInt((String) data.get("mathScale"))
                    : -1;
                builder.arithmetic((JexlArithmetic) clazz
                    .getConstructor(boolean.class, MathContext.class, int.class)
                    .newInstance(astrict, mc, scale));
            } else {
                builder.arithmetic((JexlArithmetic) clazz
                    .getConstructor(boolean.class)
                    .newInstance(astrict));
            }
        } catch (final ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                "cannot instantiate arithmetic class " + className, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyFeatures(final Map<String, Object> data,
                                      final JexlFeatures features) {
        for (final Map.Entry<String, Object> e : data.entrySet()) {
            final String key = e.getKey();
            final Object val = e.getValue();
            if ("reservedNames".equals(key) && val instanceof List) {
                features.reservedNames((List<String>) val);
            } else if (val instanceof String) {
                try {
                    final Method m = JexlFeatures.class.getMethod(key, boolean.class);
                    m.invoke(features, parseBool((String) val));
                } catch (final NoSuchMethodException ignored) {
                    // unknown feature key — skip
                } catch (final IllegalAccessException | InvocationTargetException ex) {
                    throw new IllegalArgumentException("cannot set feature " + key, ex);
                }
            }
        }
    }

    // =========================================================================
    //  Parsing helpers
    // =========================================================================

    private static JexlUberspect.ResolverStrategy parseStrategy(final String name) {
        return "MAP_STRATEGY".equals(name)
            ? JexlUberspect.MAP_STRATEGY
            : JexlUberspect.JEXL_STRATEGY;
    }

    private static boolean parseBool(final String s) {
        return "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s);
    }

    private static int parseInt(final String s) {
        return Integer.parseInt(s.trim());
    }

    private static int leadingSpaces(final String s) {
        int i = 0;
        while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    private static String ltrim(final String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i == 0 ? s : s.substring(i);
    }

    private static String rtrim(final String s) {
        int i = s.length();
        while (i > 0 && Character.isWhitespace(s.charAt(i - 1))) {
            i--;
        }
        return i == s.length() ? s : s.substring(0, i);
    }

    private static String stripComment(final String s) {
        boolean inDouble = false;
        boolean inSingle = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '#' && !inDouble && !inSingle
                && i > 0 && Character.isWhitespace(s.charAt(i - 1))) {
                return rtrim(s.substring(0, i));
            }
        }
        return s;
    }

    private static String unquote(final String s) {
        if (s.length() >= 2) {
            final char first = s.charAt(0);
            final char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }
}
