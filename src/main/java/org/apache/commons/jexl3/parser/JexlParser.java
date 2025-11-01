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
package org.apache.commons.jexl3.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.internal.LexicalScope;
import org.apache.commons.jexl3.internal.Scope;
import org.apache.commons.jexl3.introspection.JexlUberspect;

/**
 * The base class for parsing, manages the parameter/local variable frame.
 */
public abstract class JexlParser extends StringParser implements JexlScriptParser {
    /**
     * A lexical unit is the container defining local symbols and their
     * visibility boundaries.
     */
    public interface LexicalUnit {
        /**
         * Declares a local symbol.
         * @param symbol the symbol index in the scope
         * @return true if declaration was successful, false if symbol was already declared
         */
        boolean declareSymbol(int symbol);
        /**
         * @return the set of symbols identifiers declared in this unit
         */
        LexicalScope getLexicalScope();

        /**
         * @return the number of local variables declared in this unit
         */
        int getSymbolCount();
        /**
         * Checks whether a symbol is declared in this lexical unit.
         * @param symbol the symbol
         * @return true if declared, false otherwise
         */
        boolean hasSymbol(int symbol);

        boolean isConstant(int symbol);

        void setConstant(int symbol);
    }
    /**
     * The name of the options pragma.
     */
    public static final String PRAGMA_OPTIONS = "jexl.options";
    /**
     * The prefix of a namespace pragma.
     */
    public static final String PRAGMA_JEXLNS = "jexl.namespace.";
    /**
     * The prefix of a module pragma.
     */
    public static final String PRAGMA_MODULE = "jexl.module.";
    /**
     * The import pragma.
     */
    public static final String PRAGMA_IMPORT = "jexl.import";
    /**
     * The set of assignment operators as classes.
     */
    private static final Set<Class<? extends JexlNode>> ASSIGN_NODES = new HashSet<>(
        Arrays.asList(
            ASTAssignment.class,
            ASTSetAddNode.class,
            ASTSetSubNode.class,
            ASTSetMultNode.class,
            ASTSetDivNode.class,
            ASTSetModNode.class,
            ASTSetAndNode.class,
            ASTSetOrNode.class,
            ASTSetXorNode.class,
            ASTSetShiftLeftNode.class,
            ASTSetShiftRightNode.class,
            ASTSetShiftRightUnsignedNode.class,
            ASTIncrementGetNode.class,
            ASTDecrementGetNode.class,
            ASTGetDecrementNode.class,
            ASTGetIncrementNode.class
        )
    );
    /**
     * Pick the most significant token for error reporting.
     * @param tokens the tokens to choose from
     * @return the token
     */
    protected static Token errorToken(final Token... tokens) {
        for (final Token token : tokens) {
            if (token != null && token.image != null && !token.image.isEmpty()) {
                return token;
            }
        }
        return null;
    }
    /**
     * Reads a given source line.
     * @param src the source
     * @param lineno the line number
     * @return the line
     */
    protected static String readSourceLine(final String src, final int lineno) {
        String msg = "";
        if (src != null && lineno >= 0) {
            try {
                final BufferedReader reader = new BufferedReader(new StringReader(src));
                for (int l = 0; l < lineno; ++l) {
                    msg = reader.readLine();
                }
            } catch (final IOException xio) {
                // ignore, very unlikely but then again...
            }
        }
        return msg;
    }
    /**
     * Utility function to create '.' separated string from a list of string.
     * @param lstr the list of strings
     * @return the dotted version
     */
    protected static String stringify(final Iterable<String> lstr) {
        return String.join(".", lstr);
    }
    /**
     * The associated controller.
     */
    protected final FeatureController featureController;
    /**
     * The basic source info.
     */
    protected JexlInfo info;
    /**
     * The source being processed.
     */
    protected String source;
    /**
     * The map of named registers aka script parameters.
     * <p>Each parameter is associated with a register and is materialized
     * as an offset in the registers array used during evaluation.</p>
     */
    protected final AtomicReference<Scope> scopeReference;
    /**
     * When parsing inner functions/lambda, need to stack the scope (sic).
     */
    protected final Deque<Scope> scopes;
    /**
     * The list of pragma declarations.
     */
    protected Map<String, Object> pragmas;
    /**
     * The optional class name and constant resolver.
     */
    protected final AtomicReference<JexlUberspect.ClassConstantResolver> fqcnResolver;
    /**
     * The list of imports.
     * <p>Imports are used to resolve simple class names into fully qualified class names.</p>
     */
    protected final List<String> imports;


    void addImport(final String importName) {
        if (importName != null && !importName.isEmpty() && !imports.contains(importName)) {
            imports.add(importName);
        }
    }

    Object resolveConstant(final String name) {
        JexlUberspect.ClassConstantResolver resolver = fqcnResolver.get();
        if (resolver == null) {
            final JexlEngine engine = JexlEngine.getThreadEngine();
            if (engine instanceof JexlUberspect.ConstantResolverFactory) {
                resolver = ((JexlUberspect.ConstantResolverFactory) engine).createConstantResolver(imports);
                fqcnResolver.set(resolver);
            }
        }
        return resolver != null
            ? resolver.resolveConstant(name)
            : JexlEngine.TRY_FAILED;
    }

    /**
     * Whether automatic semicolon insertion is enabled.
     */
    protected boolean autoSemicolon = true;
    /**
     * The known namespaces.
     */
    protected Set<String> namespaces;
    /**
     * The number of nested loops.
     */
    protected AtomicInteger loopCount;
    /**
     * Stack of parsing loop counts.
     */
    protected final Deque<Integer> loopCounts;
    /**
     * The current lexical block.
     */
    protected final AtomicReference<LexicalUnit> blockReference;
    /**
     * Stack of lexical blocks.
     */
    protected final Deque<LexicalUnit> blocks;
    /**
     * The map of lexical to functional blocks.
     */
    protected final Map<LexicalUnit, Scope> blockScopes;
    /**
     * The parent parser if any.
     */
    protected final JexlParser parent;

    /**
     * Creates a new parser.
     * <p>
     * This constructor is protected so that it can only be used by subclasses.
     * </p>
     */
    protected JexlParser() {
        this(null);
    }

    /**
     * Creates a new inner-parser.
     * <p>
     * This is the constructor used to create a parser for template expressions.
     * </p>
     */
    protected JexlParser(final JexlParser parser) {
        this.info = null;
        this.source = null;
        if (parser != null) {
            parent = parser;
            featureController = parser.featureController;
            scopeReference = parser.scopeReference;
            scopes = parser.scopes;
            pragmas = parser.pragmas;
            namespaces = parser.namespaces;
            loopCount = parser.loopCount;
            loopCounts = parser.loopCounts;
            blockReference = parser.blockReference;
            blocks = parser.blocks;
            blockScopes = parser.blockScopes;
            fqcnResolver = parser.fqcnResolver;
            imports = parser.imports;
            autoSemicolon = parser.autoSemicolon;
        } else {
            parent = null;
            featureController = new FeatureController(JexlEngine.DEFAULT_FEATURES);
            scopeReference = new AtomicReference<>();
            blockReference = new AtomicReference<>();
            fqcnResolver = new AtomicReference<>();
            loopCount = new AtomicInteger();
            scopes = new ArrayDeque<>();
            loopCounts = new ArrayDeque<>();
            blocks = new ArrayDeque<>();
            blockScopes = new IdentityHashMap<>();
            imports = new ArrayList<>();
        }
    }

    /**
     * The name of the null case constant.
     */
    public static final Object NIL = new Object() {
        @Override public String toString() {
        return "null";
    }};

    /**
     * The name of the default case constant.
     */
    public static final Object DFLT = new Object() {
    @Override public String toString() {
        return "default";
    }};

    /**
     * The name of the default NaN constant.
     */
    public static final Object NAN = new Object() {
    @Override public String toString() {
        return "NaN";
    }};

    /**
     * Encode a value to a switch predicate.
     *
     * @param value the value.
     * @return the encoded value, which is either the value itself, or NAN (for NaN) or NIL (for null).
     */
    static Object switchCode(final Object value) {
        if (value == null) {
            return NIL;
        }
        if (value instanceof Double && ((Double) value).isNaN()) {
            return NAN;
        }
        return value;
    }

    /**
     * Decodes a value of a switch predicate.
     *
     * @param value an encoded value, which is either a value or NAN (for NaN) or NIL (for null).
     * @return the decoded value.
     */
    static Object switchDecode(final Object value) {
        if (value == NIL) {
            return null;
        }
        if (value == NAN) {
            return Double.NaN;
        }
        return value;
    }

    /**
     * Constructs a set of constants amenable to switch expression.
     */
    protected SwitchSet switchSet() {
        return new SwitchSet();
    }

    protected class SwitchSet implements Iterable<Object> {

        private final Set<Object> values = new LinkedHashSet<>();

        /**
         * Adds a collection of values to the set.
         *
         * @param values the values to add.
         */
        void addAll(final Collection<Object> values) {
            for (final Object value : values) {
                add(value);
            }
        }

        /**
         * Adds a value to the set.
         *
         * @param value the value to add.
         */
        void add(final Object value) {
            final Object code = switchCode(value);
            if (!values.add(code)) {
                throw new JexlException.Parsing(info, "duplicate constant value: " + value);
            }
        }

        void clear() {
            values.clear();
        }

        boolean isEmpty() {
            return values.isEmpty();
        }

        int size() {
            return values.size();
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                private final Iterator<Object> iter = values.iterator();

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Object next() {
                    return switchDecode(iter.next());
                }

                @Override
                public void remove() {
                    iter.remove();
                }
            };
        }
    }

    /**
     * Internal, for debug purpose only.
     *
     * @param registers sets whether this parser recognizes the register syntax
     */
    public void allowRegisters(final boolean registers) {
        featureController.setFeatures(new JexlFeatures(featureController.getFeatures()).register(registers));
    }

    /**
     * Tests whether a given variable name is allowed.
     *
     * @param image the name.
     * @return true if allowed, false if reserved.
     */
    protected boolean allowVariable(final String image) {
        final JexlFeatures features = getFeatures();
        if (!features.supportsLocalVar()) {
            return false;
        }
        if (features.isReservedName(image)) {
            return false;
        }
        return true;
    }

    /**
     * Check fat vs thin arrow syntax feature.
     *
     * @param token the arrow token.
     */
    protected void checkLambda(final Token token) {
        final String arrow = token.image;
        if ("->".equals(arrow)) {
            if (!getFeatures().supportsThinArrow()) {
                throwFeatureException(JexlFeatures.THIN_ARROW, token);
            }
            return;
        }
        if ("=>".equals(arrow) && !getFeatures().supportsFatArrow()) {
            throwFeatureException(JexlFeatures.FAT_ARROW, token);
        }
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol, stored in a register.
     *
     * @param identifier the identifier.
     * @param name      the identifier name.
     * @return the image.
     */
    protected String checkVariable(final ASTIdentifier identifier, final String name) {
        final Scope scope = scopeReference.get();
        if (scope != null) {
            final Integer symbol = scope.getSymbol(name);
            if (symbol != null) {
                identifier.setLexical(scope.isLexical(symbol));
                boolean declared = true;
                if (scope.isCapturedSymbol(symbol)) {
                    // captured are declared in all cases
                    identifier.setCaptured(true);
                } else {
                    LexicalUnit unit = getUnit();
                    declared = unit.hasSymbol(symbol);
                    // one of the lexical blocks above should declare it
                    if (!declared) {
                        for (final LexicalUnit u : blocks) {
                            if (u.hasSymbol(symbol)) {
                                unit = u;
                                declared = true;
                                break;
                            }
                        }
                    }
                    if (declared) {
                        // track if const is defined or not
                        if (unit.isConstant(symbol)) {
                            identifier.setConstant(true);
                        }
                    } else if (info instanceof JexlNode.Info) {
                        declared = isSymbolDeclared((JexlNode.Info) info, symbol);
                    }
                }
                identifier.setSymbol(symbol, name);
                if (!declared) {
                    if (getFeatures().isLexicalShade()) {
                        // cannot reuse a local as a global
                        throw new JexlException.Parsing(info, name + ": variable is not declared").clean();
                    }
                    identifier.setShaded(true);
                }
            }
        }
        return name;
    }

    /**
     * Cleanup.
     *
     * @param features the feature set to restore if any.
     */
    protected void cleanup(final JexlFeatures features) {
        info = null;
        source = null;
        if (parent == null ) {
            scopeReference.set(null);
            scopes.clear();
            pragmas = null;
            namespaces = null;
            fqcnResolver.set(null);
            imports.clear();
            loopCounts.clear();
            loopCount.set(0);
            blocks.clear();
            blockReference.set(null);
            blockScopes.clear();
            setFeatures(features);
        }
    }

    /**
     * Disables pragma feature if pragma-anywhere feature is disabled.
     */
    protected void controlPragmaAnywhere() {
        final JexlFeatures features = getFeatures();
        if (features.supportsPragma() && !features.supportsPragmaAnywhere()) {
            featureController.setFeatures(new JexlFeatures(featureController.getFeatures()).pragma(false));
        }
    }

    /**
     * Declares a local function.
     *
     * @param variable the identifier used to declare.
     * @param token      the variable name token.
     */
    protected void declareFunction(final ASTVar variable, final Token token) {
        final String name = token.image;
        // function foo() ... <=> const foo = ()->...
        Scope scope = scopeReference.get();
        if (scope == null) {
            scope = new Scope(null);
            scopeReference.set(scope);
        }
        final int symbol = scope.declareVariable(name);
        variable.setSymbol(symbol, name);
        variable.setLexical(true);
        if (scope.isCapturedSymbol(symbol)) {
            variable.setCaptured(true);
        }
        // function is const fun...
        if (declareSymbol(symbol)) {
            scope.addLexical(symbol);
            final LexicalUnit block = getUnit();
            block.setConstant(symbol);
        } else {
            if (getFeatures().isLexical()) {
                throw new JexlException(variable, name + ": variable is already declared");
            }
            variable.setRedefined(true);
        }
    }

    /**
     * Declares a local parameter.
     * <p>
     * This method creates a new entry in the symbol map.
     * </p>
     *
     * @param token the parameter name token.
     * @param lexical whether the parameter is lexical or not.
     * @param constant whether the parameter is constant or not.
     */
    protected void declareParameter(final Token token, final boolean lexical, final boolean constant) {
        final String identifier =  token.image;
        if (!allowVariable(identifier)) {
            throwFeatureException(JexlFeatures.LOCAL_VAR, token);
        }
        Scope scope = scopeReference.get();
        if (scope == null) {
            scope = new Scope(null, (String[]) null);
            scopeReference.set(scope);
        }
        final int symbol = scope.declareParameter(identifier);
        // not sure how declaring a parameter could fail...
        // lexical feature error
        final LexicalUnit block = getUnit();
        if (!block.declareSymbol(symbol)) {
            if (lexical || getFeatures().isLexical()) {
                final JexlInfo xinfo = info.at(token.beginLine, token.beginColumn);
                throw new JexlException.Parsing(xinfo, identifier + ": parameter is already declared").clean();
            }
        } else if (lexical) {
            scope.addLexical(symbol);
            if (constant) {
                block.setConstant(symbol);
            }
        }
    }

    /**
     * Adds a pragma declaration.
     *
     * @param key the pragma key.
     * @param value the pragma value.
     */
    protected void declarePragma(final String key, final Object value) {
        final JexlFeatures features = getFeatures();
        if (!features.supportsPragma()) {
            throwFeatureException(JexlFeatures.PRAGMA, getToken(0));
        }
        if (PRAGMA_IMPORT.equals(key) && !features.supportsImportPragma()) {
            throwFeatureException(JexlFeatures.IMPORT_PRAGMA, getToken(0));
        }
        if (pragmas == null) {
            pragmas = new TreeMap<>();
        }
        // declaring a namespace or module
        final String[] nsprefixes = { PRAGMA_JEXLNS, PRAGMA_MODULE };
        for(final String nsprefix : nsprefixes) {
            if (key.startsWith(nsprefix)) {
                if (!features.supportsNamespacePragma()) {
                    throwFeatureException(JexlFeatures.NS_PRAGMA, getToken(0));
                }
                final String nsname = key.substring(nsprefix.length());
                if (!nsname.isEmpty()) {
                    if (namespaces == null) {
                        namespaces = new HashSet<>();
                    }
                    namespaces.add(nsname);
                }
                break;
            }
        }
        // merge new value into a set created on the fly if key is already mapped
        if (value == null) {
            pragmas.putIfAbsent(key, null);
        } else {
            pragmas.merge(key, value, (previous, newValue) -> {
                if (previous instanceof Set<?>) {
                    ((Set<Object>) previous).add(newValue);
                    return previous;
                }
                final Set<Object> values = new LinkedHashSet<>();
                values.add(previous);
                values.add(newValue);
                return values;
            });
        }
    }

    /**
     * Declares a symbol.
     *
     * @param symbol the symbol index.
     * @return true if symbol can be declared in lexical scope, false (error)
     * if it is already declared.
     */
    private boolean declareSymbol(final int symbol) {
        for (final LexicalUnit lu : blocks) {
            if (lu.hasSymbol(symbol)) {
                return false;
            }
            // stop at first new scope reset, aka lambda
            if (lu instanceof ASTJexlLambda) {
                break;
            }
        }
        final LexicalUnit block = getUnit();
        return block == null || block.declareSymbol(symbol);
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the symbol map.
     * </p>
     *
     * @param variable the identifier used to declare.
     * @param lexical  whether the symbol is lexical.
     * @param constant whether the symbol is constant.
     * @param token    the variable name token.
     */
    protected void declareVariable(final ASTVar variable, final Token token, final boolean lexical, final boolean constant) {
        final String name = token.image;
        if (!allowVariable(name)) {
            throwFeatureException(JexlFeatures.LOCAL_VAR, token);
        }
        Scope scope = scopeReference.get();
        if (scope == null) {
            scope = new Scope(null);
            scopeReference.set(scope);
        }
        final int symbol = scope.declareVariable(name);
        variable.setSymbol(symbol, name);
        variable.setLexical(lexical);
        variable.setConstant(constant);
        if (scope.isCapturedSymbol(symbol)) {
            variable.setCaptured(true);
        }
        // if not the first time we declare this symbol...
        if (!declareSymbol(symbol)) {
            if (lexical || scope.isLexical(symbol) || getFeatures().isLexical()) {
                final JexlInfo location = info.at(token.beginLine, token.beginColumn);
                throw new JexlException.Parsing(location, name + ": variable is already declared").clean();
            }
            // not lexical, redefined nevertheless
            variable.setRedefined(true);
        } else if (lexical) {
            scope.addLexical(symbol);
            if (constant) {
                getUnit().setConstant(symbol);
            }
        }
    }

    /**
     * Gets the current set of features active during parsing.
     *
     * @return the current set of features active during parsing.
     */
    protected JexlFeatures getFeatures() {
        return featureController.getFeatures();
    }

    /**
     * Gets the frame used by this parser.
     * <p>
     * Since local variables create new symbols, it is important to
     * regain access after parsing to known which / how-many registers are needed.
     * </p>
     * @return the named register map
     */
    protected Scope getScope() {
        return scopeReference.get();
    }

    /**
     * Overridden in actual parser to access tokens stack.
     *
     * @param index 0 to get current token.
     * @return the token on the stack.
     */
    protected abstract Token getToken(int index);

    /**
     * Gets the lexical unit used by this parser.
     * @return the named register map.
     */
    protected LexicalUnit getUnit() {
        return blockReference.get();
    }

    /**
     * Default implementation does nothing but is overridden by generated code.
     *
     * @param top whether the identifier is beginning an l/r value.
     * @throws ParseException subclasses may throw ParseException.
     */
    @SuppressWarnings("unused") // subclasses may throw ParseException
    protected void Identifier(final boolean top) throws ParseException {
        // Overridden by generated code
    }

    /**
     * Checks whether a symbol has been declared as a const in the current stack of lexical units.
     *
     * @param symbol the symbol.
     * @return true if constant, false otherwise.
     */
    private boolean isConstant(final int symbol) {
        if (symbol >= 0) {
            final LexicalUnit block = getUnit();
            if (block != null && block.hasSymbol(symbol)) {
                return block.isConstant(symbol);
            }
            Scope blockScope = blockScopes.get(block);
            int lexical = symbol;
            for (final LexicalUnit unit : blocks) {
                final Scope unitScope = blockScopes.get(unit);
                // follow through potential capture
                if (blockScope != unitScope) {
                    final int declared = blockScope.getCaptureDeclaration(lexical);
                    if (declared >= 0) {
                        lexical = declared;
                    }
                    if (unitScope != null) {
                        blockScope = unitScope;
                    }
                }
                if (unit.hasSymbol(lexical)) {
                    return unit.isConstant(lexical);
                }
            }
        }
        return false;
    }

    /**
     * Checks whether a name is a declared namespace.
     *
     * @param name the namespace name.
     * @return true if declared, false otherwise.
     */
    private boolean isNamespace(final String name) {
        // templates
        if ("jexl".equals(name) || "$jexl".equals(name)) {
            return true;
        }
        final Set<String> ns = namespaces;
        // declared through local pragma ?
        if (ns != null && ns.contains(name)) {
            return true;
        }
        // declared through engine features ?
        return getFeatures().namespaceTest().test(name);
    }

    /**
     * Semantic check identifying whether a list of 4 tokens forms a namespace function call.
     * <p>This is needed to disambiguate ternary operator, map entries and actual calls.</p>
     * <p>Note that this check is performed before syntactic check so the expected parameters need to be
     * verified.</p>
     *
     * @param ns the namespace token.
     * @param colon expected to be &quot;:&quot;
     * @param fun the function name
     * @param paren expected to be &quot;(&quot;
     * @return true if the name qualifies a namespace function call.
     */
    protected boolean isNamespaceFuncall(final Token ns, final Token colon, final Token fun, final Token paren) {
        // let's make sure this is a namespace function call
        if (!":".equals(colon.image)) {
            return false;
        }
        if (!"(".equals(paren.image)) {
            return false;
        }
        // namespace as identifier means no spaces in between ns, colon and fun, no matter what
        if (featureController.getFeatures().supportsNamespaceIdentifier()) {
            return colon.beginColumn - 1 == ns.endColumn
                    && colon.endColumn == fun.beginColumn - 1;
        }
        // if namespace name is shared with a variable name
        // or if fun is a variable name (likely a function call),
        // use syntactic hint
        if (isVariable(ns.image) || isVariable(fun.image)) {
            // the namespace sticks to the colon as in 'ns:fun()' (vs 'ns : fun()')
            return colon.beginColumn - 1 == ns.endColumn
                    && (colon.endColumn == fun.beginColumn - 1 || isNamespace(ns.image));
        }
        return true;
    }

    /**
     * Checks if a symbol is defined in lexical scopes.
     * <p>This works with parsed scripts in template resolution only.
     *
     * @param info an info linked to a node.
     * @param symbol the symbol number.
     * @return true if symbol accessible in lexical scope.
     */
    private boolean isSymbolDeclared(final JexlNode.Info info, final int symbol) {
        JexlNode walk = info.getNode();
        while(walk != null) {
            if (walk instanceof JexlParser.LexicalUnit) {
                final LexicalScope scope = ((JexlParser.LexicalUnit) walk).getLexicalScope();
                if (scope != null && scope.hasSymbol(symbol)) {
                    return true;
                }
                // stop at first new scope reset, aka lambda
                if (walk instanceof ASTJexlLambda) {
                    break;
                }
            }
            walk = walk.jjtGetParent();
        }
        return false;
    }

    /**
     * Checks whether an identifier is a local variable or argument.
     *
     * @param name the variable name.
     * @return true if a variable with that name was declared.
     */
    protected boolean isVariable(final String name) {
        final Scope scope = scopeReference.get();
        return scope != null && scope.getSymbol(name) != null;
    }

    /**
     * Checks whether a statement is ambiguous.
     * <p>
     * This is used to detect statements that are not terminated by a semicolon,
     * and that may be confused with an expression.
     * </p>
     *
     * @param semicolon the semicolon token kind.
     * @return true if statement is ambiguous, false otherwise.
     */
    protected boolean isAmbiguousStatement(final int semicolon) {
        if (autoSemicolon) {
            final Token current = getToken(0);
            final Token next = getToken(1);
            if (current != null && next != null && current.endLine != next.beginLine) {
                // if the next token is on a different line, no ambiguity reported
                return false;
            }
        }
        return !getFeatures().supportsAmbiguousStatement();
    }

    /**
     * Called by parser at end of node construction.
     * <p>
     * Detects "Ambiguous statement" and 'non-left value assignment'.</p>
     *
     * @param node the node.
     * @throws JexlException.Parsing when parsing fails.
     */
    protected void jjtreeCloseNodeScope(final JexlNode node) {
        if (node instanceof ASTAmbiguous) {
            throwAmbiguousException(node);
        }
        if (node instanceof ASTJexlScript) {
            if (node instanceof ASTJexlLambda && !getFeatures().supportsLambda()) {
                throwFeatureException(JexlFeatures.LAMBDA, node.jexlInfo());
            }
            final ASTJexlScript script = (ASTJexlScript) node;
            // reaccess in case local variables have been declared
            final Scope scope = scopeReference.get();
            if (script.getScope() != scope) {
                script.setScope(scope);
            }
        } else if (ASSIGN_NODES.contains(node.getClass())) {
            final JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                JexlInfo xinfo = lv.jexlInfo();
                xinfo = info.at(xinfo.getLine(), xinfo.getColumn());
                final String msg = readSourceLine(source, xinfo.getLine());
                throw new JexlException.Assignment(xinfo, msg).clean();
            }
            if (lv instanceof ASTIdentifier && !(lv instanceof ASTVar)) {
                final ASTIdentifier varName = (ASTIdentifier) lv;
                if (isConstant(varName.getSymbol())) { // if constant, fail...
                    JexlInfo xinfo = lv.jexlInfo();
                    xinfo = info.at(xinfo.getLine(), xinfo.getColumn());
                    throw new JexlException.Assignment(xinfo, varName.getName()).clean();
                }
            }
        }
        // heavy check
        featureController.controlNode(node);
    }

    /**
     * Parses an embedded Jexl expression within an interpolation node.
     * <p>This creates a sub-parser that shares the scopes of the parent parser.</p>
     *
     * @param info the JexlInfo
     * @param src the source to parse
     * @return the parsed tree
     */
    @Override
    public ASTJexlScript jxltParse(final JexlInfo info, final JexlFeatures features, final String src, final Scope scope) {
        return new Parser(this).parse(info, features, src, scope);
    }

    /**
     * Called by parser at the beginning of a node construction.
     *
     * @param node the node.
     */
    protected void jjtreeOpenNodeScope(final JexlNode node) {
        // nothing
    }

    /**
     * Starts the definition of a lambda.
     *
     * @param jjtThis the script.
     */
    protected void beginLambda(final ASTJexlScript jjtThis) {
        jjtThis.setFeatures(getFeatures());
        pushScope();
        pushUnit(jjtThis);
    }

    /**
     * Ends the definition of a lambda.
     *
     * @param jjtThis the script.
     */
    protected void endLambda(final ASTJexlScript jjtThis) {
        popUnit(jjtThis);
        popScope();
    }

    /**
     * Pops back to previous local variable scope.
     */
    protected void popScope() {
        final Scope scope = scopes.isEmpty() ? null : scopes.pop();
        scopeReference.set(scope);
        if (!loopCounts.isEmpty()) {
            loopCount.set(loopCounts.pop());
        }
    }

    /**
     * Restores the previous lexical unit.
     *
     * @param unit restores the previous lexical scope.
     */
    protected void popUnit(final LexicalUnit unit) {
        final LexicalUnit block = blockReference.get();
        if (block == unit){
            blockScopes.remove(unit);
            blockReference.set(blocks.isEmpty()? null : blocks.pop());
        }
    }

    /**
     * Creates a new local variable scope and push it as current.
     */
    protected void pushScope() {
        Scope scope = scopeReference.get();
        if (scope != null) {
            scopes.push(scope);
        }
        scope = new Scope(scope, (String[]) null);
        scopeReference.set(scope);
        loopCounts.push(loopCount.getAndSet(0));
    }

    /**
     * Pushes a new lexical unit.
     *
     * @param unit the new lexical unit.
     */
    protected void pushUnit(final LexicalUnit unit) {
        final Scope scope = scopeReference.get();
        blockScopes.put(unit, scope);
        final LexicalUnit block = blockReference.get();
        if (block != null) {
            blocks.push(block);
        }
        blockReference.set(unit);
    }

    protected void pushLoop() {
        loopCounts.push(loopCount.getAndSet(0));
    }

    protected void popLoop() {
        if (!loopCounts.isEmpty()) {
            loopCount.set(loopCounts.pop());
        }
    }

    /**
     * Sets a new set of options.
     *
     * @param features the parser features
     */
    protected void setFeatures(final JexlFeatures features) {
        this.featureController.setFeatures(features);
    }

    /**
     * Throws Ambiguous exception.
     * <p>
     * Seeks the end of the ambiguous statement to recover.
     * </p>
     *
     * @param node the first token in ambiguous expression.
     * @throws JexlException.Ambiguous in all cases.
     */
    protected void throwAmbiguousException(final JexlNode node) {
        final JexlInfo begin = node.jexlInfo(info.getName());
        final Token t = getToken(0);
        final JexlInfo end = info.at(t.beginLine, t.endColumn);
        final String msg = readSourceLine(source, end.getLine());
        throw new JexlException.Ambiguous(begin, end, msg).clean();
    }

    /**
     * Throws a feature exception.
     *
     * @param feature the feature code.
     * @param info the exception surroundings.
     * @throws JexlException.Feature in all cases.
     */
    protected void throwFeatureException(final int feature, final JexlInfo info) {
        final String msg = info != null ? readSourceLine(source, info.getLine()) : null;
        throw new JexlException.Feature(info, feature, msg).clean();
    }

    /**
     * Throws a feature exception.
     *
     * @param feature the feature code.
     * @param trigger the token that triggered it.
     * @throws JexlException.Parsing if actual error token cannot be found.
     * @throws JexlException.Feature in all other cases.
     */
    protected void throwFeatureException(final int feature, final Token trigger) {
        Token token = trigger;
        if (token == null) {
            token = getToken(0);
            if (token == null) {
                throw new JexlException.Parsing(null, JexlFeatures.stringify(feature)).clean();
            }
        }
        final JexlInfo xinfo = info.at(token.beginLine, token.beginColumn);
        throwFeatureException(feature, xinfo);
    }

    /**
     * Throws a parsing exception.
     *
     * @param parsed the token to report.
     * @throws JexlException.Parsing in all cases.
     */
    protected void throwParsingException(final Token parsed) {
        JexlInfo xinfo  = null;
        String msg = "unrecoverable state";
        Token token = parsed;
        if (token == null) {
            token = getToken(0);
        }
        if (token != null) {
            xinfo = info.at(token.beginLine, token.beginColumn);
            msg = token.image;
        }
        throw new JexlException.Parsing(xinfo, msg).clean();
    }
}
