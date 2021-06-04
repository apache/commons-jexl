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
package org.apache.commons.jexl3.parser;

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.internal.Scope;
import org.apache.commons.jexl3.internal.LexicalScope;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;


/**
 * The base class for parsing, manages the parameter/local variable frame.
 */
public abstract class JexlParser extends StringParser {
    /**
     * The associated controller.
     */
    protected final FeatureController featureController = new FeatureController(JexlEngine.DEFAULT_FEATURES);
    /**
     * The basic source info.
     */
    protected JexlInfo info = null;
    /**
     * The source being processed.
     */
    protected String source = null;
    /**
     * The map of named registers aka script parameters.
     * <p>Each parameter is associated to a register and is materialized
     * as an offset in the registers array used during evaluation.</p>
     */
    protected Scope frame = null;
    /**
     * When parsing inner functions/lambda, need to stack the scope (sic).
     */
    protected final Deque<Scope> frames = new ArrayDeque<>();
    /**
     * The list of pragma declarations.
     */
    protected Map<String, Object> pragmas = null;
    /**
     * The known namespaces.
     */
    protected Set<String> namespaces = null;
    /**
     * The number of imbricated loops.
     */
    protected int loopCount = 0;
    /**
     * Stack of parsing loop counts.
     */
    protected final Deque<Integer> loopCounts = new ArrayDeque<>();
    /**
     * The current lexical block.
     */
    protected LexicalUnit block = null;
    /**
     * Stack of lexical blocks.
     */
    protected final Deque<LexicalUnit> blocks = new ArrayDeque<>();

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
         * Checks whether a symbol is declared in this lexical unit.
         * @param symbol the symbol
         * @return true if declared, false otherwise
         */
        boolean hasSymbol(int symbol);

        /**
         * @return the number of local variables declared in this unit
         */
        int getSymbolCount();

        LexicalScope getLexicalScope();
    }

    /**
     * Cleanup.
     * @param features the feature set to restore if any
     */
    protected void cleanup(final JexlFeatures features) {
        info = null;
        source = null;
        frame = null;
        frames.clear();
        pragmas = null;
        namespaces = null;
        loopCounts.clear();
        loopCount = 0;
        blocks.clear();
        block = null;
        this.setFeatures(features);
    }

    /**
     * Utility function to create '.' separated string from a list of string.
     * @param lstr the list of strings
     * @return the dotted version
     */
    protected static String stringify(final Iterable<String> lstr) {
        final StringBuilder strb = new StringBuilder();
        boolean dot = false;
        for(final String str : lstr) {
            if (!dot) {
               dot = true;
            } else {
               strb.append('.');
            }
            strb.append(str);
        }
        return strb.toString();
    }

    /**
     * Read a given source line.
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
     * Internal, for debug purpose only.
     * @param registers whether register syntax is recognized by this parser
     */
    public void allowRegisters(final boolean registers) {
        featureController.setFeatures(new JexlFeatures(featureController.getFeatures()).register(registers));
    }

    /**
     * Sets a new set of options.
     * @param features the parser features
     */
    protected void setFeatures(final JexlFeatures features) {
        this.featureController.setFeatures(features);
    }

    /**
     * @return the current set of features active during parsing
     */
    protected JexlFeatures getFeatures() {
        return featureController.getFeatures();
    }

    /**
     * Gets the frame used by this parser.
     * <p> Since local variables create new symbols, it is important to
     * regain access after parsing to known which / how-many registers are needed. </p>
     * @return the named register map
     */
    protected Scope getFrame() {
        return frame;
    }

    /**
     * Create a new local variable frame and push it as current scope.
     */
    protected void pushFrame() {
        if (frame != null) {
            frames.push(frame);
        }
        frame = new Scope(frame, (String[]) null);
        loopCounts.push(loopCount);
        loopCount = 0;
    }

    /**
     * Pops back to previous local variable frame.
     */
    protected void popFrame() {
        if (!frames.isEmpty()) {
            frame = frames.pop();
        } else {
            frame = null;
        }
        if (!loopCounts.isEmpty()) {
            loopCount = loopCounts.pop();
        }
    }

    /**
     * Gets the lexical unit currently used by this parser.
     * @return the named register map
     */
    protected LexicalUnit getUnit() {
        return block;
    }

    /**
     * Pushes a new lexical unit.
     * @param unit the new lexical unit
     */
    protected void pushUnit(final LexicalUnit unit) {
        if (block != null) {
            blocks.push(block);
        }
        block = unit;
    }

    /**
     * Restores the previous lexical unit.
     * @param unit restores the previous lexical scope
     */
    protected void popUnit(final LexicalUnit unit) {
        if (block == unit){
            if (!blocks.isEmpty()) {
                block = blocks.pop();
            } else {
                block = null;
            }
        }
    }

    /**
     * Checks if a symbol is defined in lexical scopes.
     * <p>This works with parsed scripts in template resolution only.
     * @param info an info linked to a node
     * @param symbol the symbol number
     * @return true if symbol accessible in lexical scope
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
     * @param name the variable name
     * @return true if a variable with that name was declared
     */
    protected boolean isVariable(String name) {
        return frame != null && frame.getSymbol(name) != null;
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol, stored in a register.
     * @param identifier the identifier
     * @param name      the identifier name
     * @return the image
     */
    protected String checkVariable(final ASTIdentifier identifier, final String name) {
        if (frame != null) {
            final Integer symbol = frame.getSymbol(name);
            if (symbol != null) {
                boolean declared = true;
                if (frame.isCapturedSymbol(symbol)) {
                    // captured are declared in all cases
                    identifier.setCaptured(true);
                } else {
                    declared = block.hasSymbol(symbol);
                    // one of the lexical blocks above should declare it
                    if (!declared) {
                        for (final LexicalUnit u : blocks) {
                            if (u.hasSymbol(symbol)) {
                                declared = true;
                                break;
                            }
                        }
                    }
                    if (!declared && info instanceof JexlNode.Info) {
                        declared = isSymbolDeclared((JexlNode.Info) info, symbol);
                    }
                }
                identifier.setSymbol(symbol, name);
                if (!declared) {
                    identifier.setShaded(true);
                    if (getFeatures().isLexicalShade()) {
                        // can not reuse a local as a global
                        throw new JexlException(identifier, name + ": variable is not defined");
                    }
                }
            }
        }
        return name;
    }

    /**
     * Whether a given variable name is allowed.
     * @param image the name
     * @return true if allowed, false if reserved
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
     * Declares a symbol.
     * @param symbol the symbol index
     * @return true if symbol can be declared in lexical scope, false (error)
     * if it is already declared
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
        return block == null || block.declareSymbol(symbol);
    }

    /**
     * Declares a local variable.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param variable the identifier used to declare
     * @param token      the variable name toekn
     */
    protected void declareVariable(final ASTVar variable, final Token token) {
        final String name = token.image;
        if (!allowVariable(name)) {
            throwFeatureException(JexlFeatures.LOCAL_VAR, token);
        }
        if (frame == null) {
            frame = new Scope(null, (String[]) null);
        }
        final int symbol = frame.declareVariable(name);
        variable.setSymbol(symbol, name);
        if (frame.isCapturedSymbol(symbol)) {
            variable.setCaptured(true);
        }
        // lexical feature error
        if (!declareSymbol(symbol)) {
            if (getFeatures().isLexical()) {
                throw new JexlException(variable, name + ": variable is already declared");
            }
            variable.setRedefined(true);
        }
    }

    /**
     * The prefix of a namespace pragma.
     */
    protected static final String PRAGMA_JEXLNS = "jexl.namespace.";

    /**
     * Adds a pragma declaration.
     * @param key the pragma key
     * @param value the pragma value
     */
    protected void declarePragma(final String key, final Object value) {
        if (!getFeatures().supportsPragma()) {
            throwFeatureException(JexlFeatures.PRAGMA, getToken(0));
        }
        if (pragmas == null) {
            pragmas = new TreeMap<>();
        }
        // declaring a namespace
        Predicate<String> ns = getFeatures().namespaceTest();
        if (ns != null && key.startsWith(PRAGMA_JEXLNS)) {
            // jexl.namespace.***
            final String nsname = key.substring(PRAGMA_JEXLNS.length());
            if (nsname != null && !nsname.isEmpty()) {
                if (namespaces == null) {
                    namespaces = new HashSet<>();
                }
                namespaces.add(nsname);
            }
        }
        pragmas.put(key, value);
    }

    /**
     * Checks whether a name identifies a declared namespace.
     * @param token the namespace token
     * @return true if the name qualifies a namespace
     */
    protected boolean isDeclaredNamespace(final Token token, final Token colon) {
        // syntactic hint, the namespace sticks to the colon
        if (colon != null && ":".equals(colon.image) && colon.beginColumn - 1 == token.endColumn) {
            return true;
        }
        // if name is shared with a variable name, use syntactic hint
        String name = token.image;
        if (!isVariable(name)) {
            final Set<String> ns = namespaces;
            // declared through local pragma ?
            if (ns != null && ns.contains(name)) {
                return true;
            }
            // declared through engine features ?
            if (getFeatures().namespaceTest().test(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Declares a local parameter.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param token the parameter name toekn
     */
    protected void declareParameter(final Token token) {
        final String identifier =  token.image;
        if (!allowVariable(identifier)) {
            throwFeatureException(JexlFeatures.LOCAL_VAR, token);
        }
        if (frame == null) {
            frame = new Scope(null, (String[]) null);
        }
        final int symbol = frame.declareParameter(identifier);
        // not sure how declaring a parameter could fail...
        // lexical feature error
        if (!block.declareSymbol(symbol) && getFeatures().isLexical()) {
            final JexlInfo xinfo = info.at(token.beginLine, token.beginColumn);
            throw new JexlException(xinfo,  identifier + ": variable is already declared", null);
        }
    }

    /**
     * Default implementation does nothing but is overridden by generated code.
     * @param top whether the identifier is beginning an l/r value
     * @throws ParseException subclasses may throw this
     */
    protected void Identifier(final boolean top) throws ParseException {
        // Overridden by generated code
    }

    /**
     * Overridden in actual parser to access tokens stack.
     * @param index 0 to get current token
     * @return the token on the stack
     */
    protected abstract Token getToken(int index);

    /**
     * Overridden in actual parser to access tokens stack.
     * @return the next token on the stack
     */
    protected abstract Token getNextToken();

    /**
     * The set of assignment operators as classes.
     */
    private static final Set<Class<? extends JexlNode>> ASSIGN_NODES = new HashSet<>(
        Arrays.asList(
            ASTAssignment.class,
            ASTSetAddNode.class,
            ASTSetMultNode.class,
            ASTSetDivNode.class,
            ASTSetAndNode.class,
            ASTSetOrNode.class,
            ASTSetXorNode.class,
            ASTSetSubNode.class
        )
    );

    /**
     * Called by parser at beginning of node construction.
     * @param node the node
     */
    protected void jjtreeOpenNodeScope(final JexlNode node) {
        // nothing
    }

    /**
     * Called by parser at end of node construction.
     * <p>
     * Detects "Ambiguous statement" and 'non-left value assignment'.</p>
     * @param node the node
     * @throws JexlException.Parsing when parsing fails
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
            if (script.getScope() != frame) {
                script.setScope(frame);
            }
            popFrame();
        } else if (ASSIGN_NODES.contains(node.getClass())) {
            final JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                throwParsingException(JexlException.Assignment.class, null);
            }
        }
        // heavy check
        featureController.controlNode(node);
    }

    /**
     * Throws Ambiguous exception.
     * <p>Seeks the end of the ambiguous statement to recover.
     * @param node the first token in ambiguous expression
     * @throws JexlException.Ambiguous in all cases
     */
    protected void throwAmbiguousException(final JexlNode node) {
        final JexlInfo begin = node.jexlInfo();
        final Token t = getToken(0);
        final JexlInfo end = info.at(t.beginLine, t.endColumn);
        final String msg = readSourceLine(source, end.getLine());
        throw new JexlException.Ambiguous(begin, end, msg);
    }

    /**
     * Throws a feature exception.
     * @param feature the feature code
     * @param info the exception surroundings
     * @throws JexlException.Feature in all cases
     */
    protected void throwFeatureException(final int feature, final JexlInfo info) {
        final String msg = info != null? readSourceLine(source, info.getLine()) : null;
        throw new JexlException.Feature(info, feature, msg);
    }

    /**
     * Throws a feature exception.
     * @param feature the feature code
     * @param token the token that triggered it
     * @throws JexlException.Parsing if actual error token can not be found
     * @throws JexlException.Feature in all other cases
     */
    protected void throwFeatureException(final int feature, Token token) {
        if (token == null) {
            token = this.getToken(0);
            if (token == null) {
                throw new JexlException.Parsing(null, JexlFeatures.stringify(feature));
            }
        }
        final JexlInfo xinfo = info.at(token.beginLine, token.beginColumn);
        throwFeatureException(feature, xinfo);
    }

    /**
     * Creates a parsing exception.
     * @param xclazz the class of exception
     * @param tok the token to report
     * @param <T> the parsing exception subclass
     * @throws JexlException.Parsing in all cases
     */
    protected <T extends JexlException.Parsing> void throwParsingException(final Class<T> xclazz, Token tok) {
        JexlInfo xinfo  = null;
        String msg = "unrecoverable state";
        JexlException.Parsing xparse = null;
        if (tok == null) {
            tok = this.getToken(0);
        }
        if (tok != null) {
            xinfo = info.at(tok.beginLine, tok.beginColumn);
            msg = tok.image;
            if (xclazz != null) {
                try {
                    final Constructor<T> ctor = xclazz.getConstructor(JexlInfo.class, String.class);
                    xparse = ctor.newInstance(xinfo, msg);
                } catch (final Exception xany) {
                    // ignore, very unlikely but then again..
                }
            }
        }
        // unlikely but safe
        throw xparse != null ? xparse : new JexlException.Parsing(xinfo, msg);
    }

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
}
