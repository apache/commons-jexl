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
package org.apache.commons.jexl2.parser;

import org.apache.commons.jexl2.DebugInfo;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;

/**
 * The base class for parsing, manages the parameter/local variable frame.
 * @author henri
 */
public class JexlParser extends StringParser {
    /**
     * The map of named registers aka script parameters.
     * Each parameter is associated to a register and is materialized as an offset in the registers array used
     * during evaluation.
     */
    protected JexlEngine.Scope frame;

    /**
     * Sets the frame to use bythis parser.
     * <p>
     * This is used to allow parameters to be declared before parsing.
     * </p>
     * @param registers the register map
     */
    public void setFrame(JexlEngine.Scope theFrame) {
        frame = theFrame;
    }
    
    /**
     * Gets the frame used by this parser.
     * <p>
     * Since local variables create new named registers, it is important to regain access after
     * parsing to known which / how-many registers are needed.
     * </p>
     * @return the named register map
     */
    public JexlEngine.Scope getFrame() {
        return frame;
    }
    

    /**
     * Checks whether an identifier is a local variable or argument, ie stored in a register. 
     * @param identifier the identifier
     * @param image the identifier image
     * @return the image
     */
    public String checkVariable(ASTIdentifier identifier, String image) {
        if (frame != null) {
            Integer register = frame.getRegister(image);
            if (register != null) {
                identifier.setRegister(register);
            }
        }
        return image;
    }

    /**
     * Declares a local variable.
     * <p>
     * This method creates an new entry in the named register map.
     * </p>
     * @param identifier the identifier used to declare
     * @param image the variable name
     */
    public void declareVariable(ASTVar identifier, String image) {
        if (frame == null) {
            frame = new JexlEngine.Scope((String[])null);
        }
        Integer register = frame.declareVariable(image);
        identifier.setRegister(register);
        identifier.image = image;
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
     * Ambiguous statement detector.
     * @param n the node
     * @throws ParseException 
     */
    void jjtreeCloseNodeScope(JexlNode n) throws ParseException {
        if (n instanceof ASTAmbiguous && n.jjtGetNumChildren() > 0) {
            DebugInfo dbgInfo = null;
            Token tok = this.getToken(0);
            if (tok != null) {
                dbgInfo = new DebugInfo(tok.image, tok.beginLine, tok.beginColumn);
            } else {
                dbgInfo = n.debugInfo();
            }
            throw new JexlException.Parsing(dbgInfo, "Ambiguous statement, missing ';' between expressions", null);
        }
    }
}
