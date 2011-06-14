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

import java.math.BigDecimal;
import java.math.BigInteger;

public class ASTNumberLiteral extends JexlNode implements JexlNode.Literal<Number> {
    /** The type literal value. */
    Number literal = null;
    /** The expected class. */
    Class<?> clazz = null;

    public ASTNumberLiteral(int id) {
        super(id);
    }

    public ASTNumberLiteral(Parser p, int id) {
        super(p, id);
    }
    
    /**
     * Gets the literal value.
     * @return the number literal
     */
    public Number getLiteral() {
        return literal;
    }
    
    /** {@inheritDoc} */
    @Override
    public Object jjtAccept(ParserVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
    
    public Class<?> getLiteralClass() {
        return clazz;
    }
    
    public boolean isInteger() {
        return Integer.class.equals(clazz);
    }
        
    public boolean isDouble() {
        return Double.class.equals(clazz);
    }
    
    public void setNatural(String s) {
        Number result;
        Class<?> rclass;
        int last = s.length() - 1;
        switch (s.charAt(last)) {
            case 'l':
            case 'L':
                result = Long.valueOf(s.substring(0, last));
                rclass = Long.class;
                break;
            case 'h':
            case 'H':
                result = new BigInteger(s.substring(0, last));
                rclass = BigInteger.class;
                break;
            default:
                rclass = Integer.class;
                try {
                    result = Integer.valueOf(s);
                } catch(NumberFormatException xnumber) {
                    result = Long.valueOf(s);
                }
                break;
        }
        literal = result;
        clazz = rclass;
    }

    public void setReal(String s) {
        Number result;
        Class<?> rclass;
        int last = s.length() - 1;
        switch (s.charAt(last)) {
            case 'b':
            case 'B':
                result = new BigDecimal(s.substring(0, last));
                rclass = BigDecimal.class;
                break;
            case 'd':
            case 'D':
                result = Double.valueOf(s);
                rclass = Double.class;
                break;
            case 'f':
            case 'F':
            default:
                rclass = Float.class;
                try {
                    result = Float.valueOf(s);
                } catch(NumberFormatException xnumber) {
                    result = Double.valueOf(s);
                }
                break;
        }
        literal = result;
        clazz = rclass;
    }
}
