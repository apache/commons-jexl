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

/**
 * Identifiers, variables, ie symbols.
 */
public class ASTIdentifier extends JexlNode {
    protected String name = null;
    protected int symbol = -1;
    protected int flags = 0;
    
    /** The redefined variable flag. */
    private static final int REDEFINED = 0;
    /** The shaded variable flag. */
    private static final int SHADED = 1;
    /** The captured variable flag. */
    private static final int CAPTURED = 2;

    ASTIdentifier(int id) {
        super(id);
    }

    ASTIdentifier(Parser p, int id) {
        super(p, id);
    }

    @Override
    public String toString() {
        return name;
    }

    void setSymbol(String identifier) {
        if (identifier.charAt(0) == '#') {
            symbol = Integer.parseInt(identifier.substring(1));
        }
        name = identifier;
    }

    void setSymbol(int r, String identifier) {
        symbol = r;
        name = identifier;
    }
    
    public int getSymbol() {
        return symbol;
    }
        
    /**
     * Sets the value of a flag in a mask.
     * @param ordinal the flag ordinal
     * @param mask the flags mask
     * @param value true or false
     * @return the new flags mask value
     */
    private static int set(int ordinal, int mask, boolean value) {
        return value? mask | (1 << ordinal) : mask & ~(1 << ordinal);
    }

    /**
     * Checks the value of a flag in the mask.
     * @param ordinal the flag ordinal
     * @param mask the flags mask
     * @return the mask value with this flag or-ed in
     */
    private static boolean isSet(int ordinal, int mask) {
        return (mask & 1 << ordinal) != 0;
    }
      
    public void setRedefined(boolean f) {
        flags = set(REDEFINED, flags, f);
    }
     
    public boolean isRedefined() {
        return isSet(REDEFINED, flags);
    }
    
    public void setShaded(boolean f) {
        flags = set(SHADED, flags, f);
    }
    
    public boolean isShaded() {
        return isSet(SHADED, flags);
    }
    
    public void setCaptured(boolean f) {
        flags = set(CAPTURED, flags, f);
    }
    
    public boolean isCaptured() {
        return isSet(CAPTURED, flags);
    }
    
    public String getName() {
        return name;
    }
    
    public String getNamespace() {
        return null;
    }

    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}
