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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;


/**
 * The base class for parsing, manages the parameter/local variable frame.
 */
public abstract class JexlParser extends StringParser {
    /**
     * The features.
     */
    JexlFeatures features = JexlEngine.DEFAULT_FEATURES;
    /**
     * The source being processed.
     */
    String source = null;
    /**
     * The map of named registers aka script parameters.
     * <p>Each parameter is associated to a register and is materialized
     * as an offset in the registers array used during evaluation.</p>
     */
    Scope frame = null;
    Stack<Scope> frames = new Stack<Scope>();
    /**
     * The list of pragma declarations.
     */
    Map<String, Object> pragmas = null;

    /**
     * Internal, for debug purpose only.
     */
    public void allowRegisters(boolean registers) {
        features = new JexlFeatures(features).registers(registers);
    }

    /**
     * Sets a new set of options.
     * @param features
     */
    public void setFeatures(JexlFeatures features) {
        this.features = features;
    }

    /**
     * Sets the frame to use by this parser.
     * <p> This is used to allow parameters to be declared before parsing. </p>
     * @param theFrame the register map
     */
    public void setFrame(Scope theFrame) {
        frame = theFrame;
    }

    /**
     * Gets the frame used by this parser.
     * <p> Since local variables create new symbols, it is important to
     * regain access after parsing to known which / how-many registers are needed. </p>
     * @return the named register map
     */
    public Scope getFrame() {
        return frame;
    }

    /**
     * Create a new local variable frame and push it as current scope.
     */
    public void pushFrame() {
        if (frame != null) {
            frames.push(frame);
        }
        frame = new Scope(frame, (String[]) null);
    }

    /**
     * Pops back to previous local variable frame.
     */
    public void popFrame() {
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
    public String checkVariable(ASTIdentifier identifier, String image) {
        if (frame != null) {
            Integer register = frame.getSymbol(image);
            if (register != null) {
                identifier.setSymbol(register.intValue(), image);
            }
        }
        return image;
    }

    /**
     * Declares a local variable.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param identifier the identifier used to declare
     * @param image      the variable name
     */
    public void declareVariable(ASTVar identifier, String image) {
        if (!features.supportsLocals()) {
            throwFeatureException( JexlFeatures.LOCALS, identifier);
        }
        if (features.isReservedName(image)) {
            throwFeatureException(JexlFeatures.RESERVED, identifier);
        }
        if (frame == null) {
            frame = new Scope(null, (String[]) null);
        }
        Integer register = frame.declareVariable(image);
        identifier.setSymbol(register.intValue(), image);
    }

    /**
     * Adds a pragma declaration.
     * @param key the pragma key
     * @param value the pragma value
     */
    public void declarePragma(String key, Object value) {
        if (pragmas == null) {
            pragmas = new TreeMap<String, Object>();
        }
        pragmas.put(key, value);
    }

    /**
     * Declares a local parameter.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param identifier the parameter name
     */
    public void declareParameter(String identifier) {
        if (!features.supportsLocals()) {
            throwFeatureException(JexlFeatures.LOCALS, null);
        }
        if (features.isReservedName(identifier)) {
            throwFeatureException(JexlFeatures.RESERVED, null);
        }
        if (frame == null) {
            frame = new Scope(null, (String[]) null);
        }
        frame.declareParameter(identifier);
    }

    /**
     * Default implementation does nothing but is overriden by generated code.
     * @param top whether the identifier is beginning an l/r value
     * @throws ParseException subclasses may throw this
     */
    public void Identifier(boolean top) throws ParseException {
        // Overriden by generated code
    }

    final public void Identifier() throws ParseException {
        Identifier(false);
    }

    public abstract Token getToken(int index);

    void jjtreeOpenNodeScope(JexlNode node) {
    }

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
            ASTSetSubNode.class
        )
    );
    /**
     * Called by parser at end of node construction.
     * <p>Detects "Ambiguous statement" and 'non-left value assignment'.</p>
     * @param node the node
     * @throws ParseException
     */
    void jjtreeCloseNodeScope(JexlNode node) throws ParseException {
        if (node instanceof ASTJexlScript) {
            if (node instanceof ASTJexlLambda && !features.supportsLambda()) {
                throwFeatureException(JexlFeatures.LAMBDA, node);
            }
            ASTJexlScript script = (ASTJexlScript) node;
            // reaccess in case local variables have been declared
            if (script.getScope() != frame) {
                script.setScope(frame);
            }
            popFrame();
            return;
        }
        if (node instanceof ASTAmbiguous) {
            throwParsingException(JexlException.Ambiguous.class, node);
        }
        if (ASSIGN_NODES.contains(node.getClass())) {
            JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                throwParsingException(JexlException.Assignment.class, lv);
            }
            if (!features.supportsSideEffectsGlobal() &&  lv.isGlobalVar()) {
                throwFeatureException(JexlFeatures.SIDE_EFFECTS_GLOBALS, lv);
            }
            if (!features.supportsSideEffects()) {
                throwFeatureException(JexlFeatures.SIDE_EFFECTS, lv);
            }
        }
        if (node instanceof ASTWhileStatement && !features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOPS, node);
        }
        if (node instanceof ASTForeachStatement && !features.supportsLoops()) {
            throwFeatureException(JexlFeatures.LOOPS, node);
        }
        if (node instanceof ASTConstructorNode && !features.supportsNewInstance()) {
            throwFeatureException(JexlFeatures.NEW_INSTANCE, node);
        }
    }

    /**
     * Utility function to create '.' separated string from a list of string.
     * @param lstr the list of strings
     * @return the dotted version
     */
    String stringify(List<String> lstr) {
        StringBuilder strb = new StringBuilder();
        boolean dot = false;
        for(String str : lstr) {
            if (!dot) {
               dot = true;
            } else {
               strb.append('.');
            }
            strb.append(str);
        }
        return strb.toString();
    }

    private String readSourceLine(int lineno) {
        String msg = "";
        if (source != null) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(source));
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
     * Throws a feature exception.
     * @param feature the feature code
     * @param node the node that caused it
     */
    void throwFeatureException(int feature, JexlNode node) {
        Token tok = this.getToken(0);
        if (tok == null) {
            throw new JexlException.Parsing(null, "unrecoverable state");
        }
        JexlInfo dbgInfo = new JexlInfo(tok.image, tok.beginLine, tok.beginColumn);
        String msg = readSourceLine(tok.beginLine);
        throw new JexlException.Feature(dbgInfo, feature, msg);
    }

    /**
     * Throws a parsing exception.
     * @param node the node that caused it
     */
    protected void throwParsingException(JexlNode node) {
        throwParsingException(null, node);
    }

    /**
     * Throws a parsing exception.
     * @param xclazz the class of exception
     * @param node the node that caused it
     */
    void throwParsingException(Class<? extends JexlException> xclazz, JexlNode node) {
        Token tok = this.getToken(0);
        if (tok == null) {
            throw new JexlException.Parsing(null, "unrecoverable state");
        }
        JexlInfo dbgInfo = new JexlInfo(tok.image, tok.beginLine, tok.beginColumn);
        String msg = readSourceLine(tok.beginLine);
        JexlException xjexl = null;
        if (xclazz != null) {
            try {
                Constructor<? extends JexlException> ctor = xclazz.getConstructor(JexlInfo.class, String.class);
                xjexl = ctor.newInstance(dbgInfo, msg);
            } catch (Exception xany) {
                // ignore, very unlikely but then again..
            }
        }
        throw xjexl != null ? xjexl : new JexlException.Parsing(dbgInfo, msg);
    }
}
