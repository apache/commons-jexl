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

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.internal.Scope;

import java.util.Stack;

/**
 * The base class for parsing, manages the parameter/local variable frame.
 * @author henri
 */
public abstract class JexlParser extends StringParser {
    /**
     * The map of named registers aka script parameters.
     * <p>Each parameter is associated to a register and is materialized
     * as an offset in the registers array used during evaluation.</p>
     */
    protected Scope frame = null;
    protected Stack<Scope> frames = new Stack<Scope>();

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
     * @param image the identifier image
     * @return the image
     */
    public String checkVariable(ASTIdentifier identifier, String image) {
        if (frame != null) {
            Integer register = frame.getSymbol(image);
            if (register != null) {
                identifier.setSymbol(register.intValue());
            }
        }
        return image;
    }

    /**
     * Declares a local variable.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param identifier the identifier used to declare
     * @param image the variable name
     */
    public void declareVariable(ASTVar identifier, String image) {
        if (frame == null) {
            frame = new Scope(null, (String[]) null);
        }
        Integer register = frame.declareVariable(image);
        identifier.setSymbol(register.intValue());
        identifier.image = image;
    }

    /**
     * Declares a local parameter.
     * <p> This method creates an new entry in the symbol map. </p>
     * @param identifier the identifier used to declare
     * @param image the variable name
     */
    public void declareParameter(String image) {
        if (frame == null) {
            frame = new Scope(null, (String[]) null);
        }
        frame.declareParameter(image);
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

    void jjtreeOpenNodeScope(JexlNode n) {
    }

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
        } else if (node instanceof ASTAmbiguous && node.jjtGetNumChildren() > 0) {
            final JexlInfo dbgInfo;
            Token tok = this.getToken(0);
            if (tok != null) {
                dbgInfo = new JexlInfo(tok.image, tok.beginLine, tok.beginColumn);
            } else {
                dbgInfo = node.jexlInfo();
            }
            throw new JexlException.Parsing(dbgInfo, "Ambiguous statement, missing ';' between expressions", null);
        } else if (node instanceof ASTAssignment) {
            JexlNode lv = node.jjtGetChild(0);
            if (!lv.isLeftValue()) {
                final JexlInfo dbgInfo;
                Token tok = this.getToken(0);
                if (tok != null) {
                    dbgInfo = new JexlInfo(tok.image, tok.beginLine, tok.beginColumn);
                } else {
                    dbgInfo = node.jexlInfo();
                }
                throw new JexlException.Parsing(dbgInfo, "Invalid assignment expression", null);
            }
        }
    }
}
