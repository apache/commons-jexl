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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author henri
 */
public class JexlParser extends StringParser {
     /**
     * The map of named registers aka script parameters.
     * Each parameter is associated to a register and is materialized as an offset in the registers array used
     * during evaluation.
     */
    protected Map<String, Integer> namedRegisters = null;

    /**
     * Sets the map of named registers in this parser.
     * <p>
     * This is used to allow parameters to be declared before parsing.
     * </p>
     * @param registers the register map
     */
    public void setNamedRegisters(Map<String, Integer> registers) {
        namedRegisters = registers;
    }
    
    /**
     * Gets the map of registers used by this parser.
     * <p>
     * Since local variables create new named registers, it is important to regain access after
     * parsing to known which / how-many registers are needed.
     * </p>
     * @return the named register map
     */
    public Map<String, Integer> getNamedRegisters() {
        return namedRegisters;
    }

    /**
     * Checks whether an identifier is a local variable or argument, ie stored in a register. 
     * @param node the identifier
     * @param image the identifier image
     * @return the image
     */
    public String checkVariable(ASTIdentifier identifier, String image) {
        if (namedRegisters != null) {
            Integer register = namedRegisters.get(image);
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
        if (namedRegisters == null) {
            namedRegisters = new LinkedHashMap<String, Integer>();
        }
        Integer register = namedRegisters.get(image);
        if (register == null) {
            register = Integer.valueOf(namedRegisters.size());
            namedRegisters.put(image, register);
        }
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
    
    void jjtreeOpenNodeScope(Node n) {}
    /**
     * Ambiguous statement detector.
     * @param n the node
     * @throws ParseException 
     */
    void jjtreeCloseNodeScope(Node n) throws ParseException {
      if (n instanceof ASTAmbiguous && n.jjtGetNumChildren() > 0) {
          Token tok = this.getToken(0);
          StringBuilder strb = new StringBuilder("Ambiguous statement ");
          if (tok != null) {
              strb.append("@");
              strb.append(tok.beginLine);
              strb.append(":");
              strb.append(tok.beginColumn);
          }
          strb.append(", missing ';' between expressions");
         throw new ParseException(strb.toString());
      }
    }
    
}
