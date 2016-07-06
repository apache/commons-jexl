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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.internal.Scope;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Stack;

/**
 * The base class for parsing, manages the parameter/local variable frame.
 */
public abstract class JexlParser extends StringParser {
    /** Whether the parser will allow user-named registers (aka #0 syntax). */
    boolean ALLOW_REGISTERS = false;
    /** The source being processed. */
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
        ALLOW_REGISTERS = registers;
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

    public Token getToken(int index) {
        return null;
    }

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
     * <p>Detects "Ambiguous statement" and 'non-leaft value assignment'.</p>
     * @param node the node
     * @throws ParseException
     */
    void jjtreeCloseNodeScope(JexlNode node) throws ParseException {
        if (node instanceof ASTJexlScript) {
            ASTJexlScript script = (ASTJexlScript) node;
            // reaccess in case local variables have been declared
            if (script.getScope() != frame) {
                script.setScope(frame);
            }
            popFrame();
        } else if (node instanceof ASTAmbiguous) {
            throwParsingException(JexlException.Ambiguous.class, node);
        } else if (ASSIGN_NODES.contains(node.getClass())) {
            JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                throwParsingException(JexlException.Assignment.class, lv);
            }
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
    private void throwParsingException(Class<? extends JexlException> xclazz, JexlNode node) {
        final JexlInfo dbgInfo;
        Token tok = this.getToken(0);
        if (tok != null) {
            dbgInfo = new JexlInfo(tok.image, tok.beginLine, tok.beginColumn);
        } else {
            dbgInfo = node.jexlInfo();
        }
        String msg = null;
        try {
            if (source != null) {
                BufferedReader reader = new BufferedReader(new StringReader(source));
                for (int l = 0; l < dbgInfo.getLine(); ++l) {
                    msg = reader.readLine();
                }
            } else {
                msg = "";
            }
        } catch (IOException xio) {
            // ignore
        }
        if (JexlException.Ambiguous.class.equals(xclazz)) {
            throw new JexlException.Ambiguous(dbgInfo, msg);
        }
        if (JexlException.Assignment.class.equals(xclazz)) {
            throw new JexlException.Assignment(dbgInfo, msg);
        }
        throw new JexlException.Parsing(dbgInfo, msg);
    }
}
