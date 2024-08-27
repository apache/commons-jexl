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

import java.util.Map;

import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.internal.Frame;
import org.apache.commons.jexl3.internal.Scope;

/**
 * Enhanced script to allow parameters declaration.
 */
public class ASTJexlScript extends JexlLexicalNode  {
    /** Serial uid.*/
    private static final long serialVersionUID = 202112111533L;
    /** The pragmas. */
    private Map<String, Object> pragmas;
    /** Features. */
    private transient JexlFeatures features;
    /** The script scope. */
    private transient Scope scope;

    public ASTJexlScript(final int id) {
        super(id);
    }

    public ASTJexlScript(final Parser p, final int id) {
        super(id);
    }

    /**
     * Creates an array of arguments by copying values up to the number of parameters.
     * @param caller the calling frame
     * @param values the argument values
     * @return the arguments array
     */
    public Frame createFrame(final Frame caller, final Object... values) {
        return scope != null ? scope.createFrame(features.supportsReferenceCapture(), caller, values) : null;
    }

    /**
     * Creates an array of arguments by copying values up to the number of parameters.
     * @param values the argument values
     * @return the arguments array
     */
    public Frame createFrame(final Object... values) {
        return createFrame(null, values);
    }

    /**
     * Gets the (maximum) number of arguments this script expects.
     * @return the number of parameters
     */
    public int getArgCount() {
        return scope != null ? scope.getArgCount() : 0;
    }

    /**
     * Gets this script captured variable, i.e. symbols captured from outer scopes.
     * @return the captured variable names
     */
    public String[] getCapturedVariables() {
        return scope != null ? scope.getCapturedVariables() : null;
    }

    /**
     * @return this script scope
     */
    public JexlFeatures getFeatures() {
        return features;
    }

    /**
     * Gets this script local variable, i.e. symbols assigned to local variables.
     * @return the local variable names
     */
    public String[] getLocalVariables() {
        return scope != null ? scope.getLocalVariables() : null;
    }

    /**
     * Gets this script parameters, i.e. symbols assigned before creating local variables.
     * @return the parameter names
     */
    public String[] getParameters() {
        return scope != null ? scope.getParameters() : null;
    }

    /**
     * @return this script pragmas.
     */
    public Map<String, Object> getPragmas() {
        return pragmas;
    }

    /**
     * @return this script scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Gets this script symbols, i.e. parameters and local variables.
     * @return the symbol names
     */
    public String[] getSymbols() {
        return scope != null ? scope.getSymbols() : null;
    }

    /**
     * Checks whether a given symbol is captured.
     * @param symbol the symbol number
     * @return true if captured, false otherwise
     */
    public boolean isCapturedSymbol(final int symbol) {
        return scope != null && scope.isCapturedSymbol(symbol);
    }

    @Override
    public Object jjtAccept(final ParserVisitor visitor, final Object data) {
        return visitor.visit(this, data);
    }

    /**
     * Consider script with no parameters that return lambda as parametric-scripts.
     * @return the script
     */
    public ASTJexlScript script() {
        if (scope == null && jjtGetNumChildren() == 1 && jjtGetChild(0) instanceof ASTJexlLambda) {
            final ASTJexlLambda lambda = (ASTJexlLambda) jjtGetChild(0);
            lambda.jjtSetParent(null);
            lambda.setFeatures(getFeatures());
            return lambda;
        }
        return this;
    }

    /**
     * Sets this script features.
     * @param theFeatures the features
     */
    public void setFeatures(final JexlFeatures theFeatures) {
        this.features = theFeatures;
    }

    /**
     * Sets this script pragmas.
     * @param thePragmas the pragmas
     */
    public void setPragmas(final Map<String, Object> thePragmas) {
        this.pragmas = thePragmas;
    }

    /**
     * Sets this script scope.
     * @param theScope the scope
     */
    public void setScope(final Scope theScope) {
        this.scope = theScope;
        if (theScope != null) {
            for(int a = 0; a < theScope.getArgCount(); ++a) {
                declareSymbol(a);
            }
        }
    }
}
