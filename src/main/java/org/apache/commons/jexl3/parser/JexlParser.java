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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.math.BigInteger;
import java.math.BigDecimal;

import static org.apache.commons.jexl3.parser.ParserConstants.EOF;
import static org.apache.commons.jexl3.parser.ParserConstants.SEMICOL;

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
    protected Stack<Scope> frames = new Stack<Scope>();
    /**
     * The list of pragma declarations.
     */
    protected Map<String, Object> pragmas = null;

    /**
     * Read a given source line.
     * @param src the source
     * @param lineno the line number
     * @return the line
     */
    protected static String readSourceLine(String src, int lineno) {
        String msg = "";
        if (src != null && lineno >= 0) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(src));
                for (int l = 0; l < lineno; ++l) {
                    msg = reader.readLine();
                }
            } catch (IOException xio) {
                // ignore, very unlikely but then again...
            }
        }
        return msg;
    }

    /**
     * Internal, for debug purpose only.
     * @param registers whether register syntax is recognized by this parser
     */
    public void allowRegisters(boolean registers) {
        featureController.setFeatures(new JexlFeatures(featureController.getFeatures()).register(registers));
    }

    /**
     * Sets a new set of options.
     * @param features
     */
    protected void setFeatures(JexlFeatures features) {
        this.featureController.setFeatures(features);
    }

    /**
     * @return the current set of features active during parsing
     */
    protected JexlFeatures getFeatures() {
        return featureController.getFeatures();
    }

    /**
     * Sets the frame to use by this parser.
     * <p> This is used to allow parameters to be declared before parsing. </p>
     * @param theFrame the register map
     */
    protected void setFrame(Scope theFrame) {
        frame = theFrame;
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

        if (branchScope != null) {
            branchScopes.push(branchScope);
        }
        branchScope = new BranchScope();
    }

    /**
     * Pops back to previous local variable frame.
     */
    protected void popFrame() {

        if (!branchScopes.isEmpty()) {
            branchScope = branchScopes.pop();
        } else {
            branchScope = null;
        }

        if (!frames.isEmpty()) {
            frame = frames.pop();
        } else {
            frame = null;
        }
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie a symbol, stored in a register.
     * @param identifier the identifier
     * @param image      the identifier image
     * @return the image
     */
    protected String checkVariable(ASTIdentifier identifier, String image) {
        if (frame != null) {
            Integer register = frame.getSymbol(image);
            if (register != null) {
                identifier.setSymbol(register.intValue(), image);
            }
        }
        return image;
    }

    /**
     * Checks whether a local variable is final.
     * @param image      the identifier image
     * @return true if final, false otherwise
     */
    protected boolean isFinalVariable(String image) {
        if (frame != null) {
            Integer register = frame.getSymbol(image);
            if (register != null) {
                return frame.isVariableFinal(register);
            }
        }
        return false;
    }

    protected boolean allowVariable(String image) {
        JexlFeatures features = getFeatures();
        if (!features.supportsLocalVar()) {
            return false;
        }
        if (features.isReservedName(image)) {
            return false;
        }
        return true;
    }

    /**
     * Declares a local variable.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param var the identifier used to declare
     * @param token      the variable name toekn
     */
    protected void declareVariable(ASTVar var, Token token) {
        String identifier = token.image;
        if (!allowVariable(identifier)) {
            throwFeatureException(JexlFeatures.LOCAL_VAR, token);
        }
        if (frame == null) {
            frame = new Scope(null, (String[]) null);
        }
        Integer register = frame.declareVariable(identifier, var.getType(), var.isFinal());
        var.setSymbol(register.intValue(), identifier);
    }

    /**
     * Adds a pragma declaration.
     * @param key the pragma key
     * @param value the pragma value
     */
    protected void declarePragma(String key, Object value) {
        if (!getFeatures().supportsPragma()) {
            throwFeatureException(JexlFeatures.PRAGMA, getToken(0));
        }
        if (pragmas == null) {
            pragmas = new TreeMap<String, Object>();
        }
        pragmas.put(key, value);
    }

    /**
     * Declares a local parameter.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param token the parameter name toekn
     */
    protected void declareParameter(Token token) {
        String identifier =  token.image;
        if (!allowVariable(identifier)) {
            throwFeatureException(JexlFeatures.LOCAL_VAR, token);
        }
        if (frame == null) {
            frame = new Scope(null, (String[]) null);
        }
        frame.declareParameter(identifier);
    }

    /**
     * Declares a local vararg parameter.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param token the parameter name toekn
     */
    protected void declareVarArgSupport() {
        frame.declareVarArgs();
    }

    /**
     * Default implementation does nothing but is overriden by generated code.
     * @param top whether the identifier is beginning an l/r value
     * @throws ParseException subclasses may throw this
     */
    protected void Identifier(boolean top) throws ParseException {
        // Overriden by generated code
    }

    final protected void Identifier() throws ParseException {
        Identifier(false);
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
    @SuppressWarnings("unchecked")
    private static final Set<Class<? extends JexlNode>> ASSIGN_NODES = new HashSet<Class<? extends JexlNode>>(
        Arrays.asList(
            ASTAssignment.class,
            ASTSetAddNode.class,
            ASTSetMultNode.class,
            ASTSetDivNode.class,
            ASTSetAndNode.class,
            ASTSetOrNode.class,
            ASTSetXorNode.class,
            ASTSetSubNode.class,
            ASTSetShlNode.class,
            ASTSetSarNode.class,
            ASTSetShrNode.class,
            ASTIncrementNode.class,
            ASTDecrementNode.class,
            ASTIncrementPostfixNode.class,
            ASTDecrementPostfixNode.class
        )
    );

    /**
     * Called by parser at beginning of node construction.
     * @param node the node
     */
    protected void jjtreeOpenNodeScope(JexlNode node) {
        // nothing
    }

    /**
     * Called by parser at end of node construction.
     * <p>
     * Detects "Ambiguous statement" and 'non-left value assignment'.</p>
     * @param node the node
     * @throws ParseException
     */
    protected void jjtreeCloseNodeScope(JexlNode node) throws ParseException {
        if (node instanceof ASTAmbiguous) {
            throwAmbiguousException(node);
        }
        if (node instanceof ASTJexlScript) {
            if (node instanceof ASTJexlLambda && !getFeatures().supportsLambda()) {
                throwFeatureException(JexlFeatures.LAMBDA, node.jexlInfo());
            }
            ASTJexlScript script = (ASTJexlScript) node;
            // reaccess in case local variables have been declared
            if (script.getScope() != frame) {
                script.setScope(frame);
            }
            popFrame();
        } else if (ASSIGN_NODES.contains(node.getClass())) {
            JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                throwParsingException(JexlException.Assignment.class, null);
            }
        } else if (node instanceof ASTPointerNode) {
            JexlNode lv = node.jjtGetChild(0);
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
     */
    protected void throwAmbiguousException(JexlNode node) {
        JexlInfo begin = node.jexlInfo();
        Token t = getToken(0);
        JexlInfo end = info.at(t.beginLine, t.endColumn);
        String msg = readSourceLine(source, end.getLine());
        throw new JexlException.Ambiguous(begin, end, msg);
    }

    /**
     * Throws a feature exception.
     * @param feature the feature code
     * @param info the exception surroundings
     */
    protected void throwFeatureException(int feature, JexlInfo info) {
        String msg = info != null? readSourceLine(source, info.getLine()) : null;
        throw new JexlException.Feature(info, feature, msg);
    }

    /**
     * Throws a feature exception.
     * @param feature the feature code
     * @param token the token that triggered it
     */
    protected void throwFeatureException(int feature, Token token) {
        if (token == null) {
            token = this.getToken(0);
            if (token == null) {
                throw new JexlException.Parsing(null, JexlFeatures.stringify(feature));
            }
        }
        JexlInfo xinfo = info.at(token.beginLine, token.beginColumn);
        throwFeatureException(feature, xinfo);
    }

    /**
     * Throws a parsing exception.
     * @param node the node that caused it
     */
    protected void throwParsingException(JexlNode node) {
        throwParsingException(null, null);
    }

    /**
     * Creates a parsing exception.
     * @param xclazz the class of exception
     * @param tok the token to report
     * @param <T> the parsing exception subclass
     */
    protected <T extends JexlException.Parsing> void throwParsingException(Class<T> xclazz, Token tok) {
        JexlInfo xinfo  = null;
        String msg = "unrecoverable state";
        JexlException.Parsing xparse = null;
        if (tok == null) {
            tok = this.getToken(0);
        }
        if (tok != null) {
            xinfo = info.at(tok.beginLine, tok.beginColumn);
            msg = readSourceLine(source, tok.beginLine);
            if (xclazz != null) {
                try {
                    Constructor<T> ctor = xclazz.getConstructor(JexlInfo.class, String.class);
                    xparse = ctor.newInstance(xinfo, msg);
                } catch (Exception xany) {
                    // ignore, very unlikely but then again..
                }
            }
        }
        // unlikely but safe
        throw xparse != null ? xparse : new JexlException.Parsing(xinfo, msg);
    }

    protected static String[] implicitPackages = {"java.lang.","java.util.","java.io.","java.net."};

    /**
     * Resolves a type by its name.
     * @param name the name of the type
     * @return the Class
     */
    protected static Class resolveType(String name) {
        if (name == null)
            return null;
        switch (name) {
            case "char" : return Character.TYPE;
            case "boolean" : return Boolean.TYPE;
            case "byte" : return Byte.TYPE;
            case "short" : return Short.TYPE;
            case "int" : return Integer.TYPE;
            case "long" : return Long.TYPE;
            case "float" : return Float.TYPE;
            case "double" : return Double.TYPE;
            case "Character" : return Character.class;
            case "Boolean" : return Boolean.class;
            case "Byte" : return Byte.class;
            case "Short" : return Short.class;
            case "Integer" : return Integer.class;
            case "Long" : return Long.class;
            case "Float" : return Float.class;
            case "Double" : return Double.class;
            case "BigInteger" : return BigInteger.class;
            case "BigDecimal" : return BigDecimal.class;
        }

        if (name.indexOf(".") == -1) {
            for (String prefix : implicitPackages) {
                String className = prefix + name;
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException ex) {
                }
            }
            return null;
        }
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    /**
     * Resolves an instantiable type by its name.
     * @param name the name of the type
     * @return the Class
     */
    protected static Class resolveInstantiableType(String name) {
        if (name == null)
            return null;
        switch (name) {
            case "char" : 
            case "boolean" : 
            case "byte" : 
            case "short" : 
            case "int" : 
            case "long" : 
            case "float" : 
            case "double" : return null;
            case "Character" : return Character.class;
            case "Boolean" : return Boolean.class;
            case "Byte" : return Byte.class;
            case "Short" : return Short.class;
            case "Integer" : return Integer.class;
            case "Long" : return Long.class;
            case "Float" : return Float.class;
            case "Double" : return Double.class;
            case "BigInteger" : return BigInteger.class;
            case "BigDecimal" : return BigDecimal.class;
        }

        Class result = null;

        if (name.indexOf(".") == -1) {
            for (String prefix : implicitPackages) {
                String className = prefix + name;
                try {
                    result = Class.forName(className);
                    break;                   
                } catch (ClassNotFoundException ex) {
                }
            }
        }
        if (result == null) {
            try {
                result = Class.forName(name);
            } catch (ClassNotFoundException ex) {
                return null;
            }
        }
        return (result.isInterface() || result.isMemberClass() || result.isAnnotation() || result.isEnum() || result.isArray() || 
                Modifier.isAbstract(result.getModifiers())) ? null : result;
    }

    /**
     * The target scope class for break/continue/remove statements.
     */
    protected class BranchScope {

        protected int loopCount = 0;
        protected int foreachLoopCount = 0;
        protected int switchCount = 0;

        protected Stack<String> breakLabels = new Stack<String> ();
        protected Stack<String> continueLabels = new Stack<String> ();
        protected Stack<String> removeLabels = new Stack<String> ();

        protected boolean breakSupported() {
            return loopCount != 0 || foreachLoopCount != 0 || switchCount != 0;
        }

        protected boolean breakSupported(String label) {
            return breakLabels.contains(label);
        }

        protected boolean continueSupported() {
            return loopCount != 0 || foreachLoopCount != 0;
        }

        protected boolean continueSupported(String label) {
            return continueLabels.contains(label);
        }

        protected boolean removeSupported() {
            return foreachLoopCount != 0;
        }

        protected boolean removeSupported(String label) {
            return removeLabels.contains(label);
        }

        protected void pushBlockLabel(String label) {
            breakLabels.push(label);
        }

        protected void popBlockLabel() {
            breakLabels.pop();
        }

        protected void pushLoopLabel(String label) {
            breakLabels.push(label);
            continueLabels.push(label);
        }

        protected void popLoopLabel() {
            breakLabels.pop();
            continueLabels.pop();
        }

        protected void pushForeachLabel(String label) {
            breakLabels.push(label);
            continueLabels.push(label);
            removeLabels.push(label);
        }

        protected void popForeachLabel() {
            breakLabels.pop();
            continueLabels.pop();
            removeLabels.pop();
        }
    }

    /**
     * The current scope for break/continue/remove statements.
     */
    protected BranchScope branchScope = null;

    /**
     * When parsing inner functions/lambda, need to stack the target scope
     */
    protected Stack<BranchScope> branchScopes = new Stack<BranchScope> ();

}
