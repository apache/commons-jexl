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
package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.parser.ASTJexlLambda;
import org.apache.commons.jexl3.parser.JexlNode;


/**
 * A Script closure.
 */
public class Closure extends Script {
    /** The frame. */
    protected final Scope.Frame frame;

    /**
     * Creates a closure.
     * @param theCaller the calling interpreter
     * @param lambda the lambda
     */
    protected Closure(Interpreter theCaller, ASTJexlLambda lambda) {
        super(theCaller.jexl, null, lambda);
        frame = lambda.createFrame(theCaller.frame);
    }

    /**
     * Creates a curried version of a script.
     * @param base the base script
     * @param args the script arguments
     */
    protected Closure(Script base, Object[] args) {
        super(base.jexl, base.source, base.script);
        Scope.Frame sf = (base instanceof Closure) ? ((Closure) base).frame :  null;
        frame = sf == null
                ? script.createFrame(args)
                : sf.assign(args);
    }

    @Override
    public int hashCode() {
        // CSOFF: Magic number
        int hash = 17;
        hash = 31 * hash + (this.jexl != null ? this.jexl.hashCode() : 0);
        hash = 31 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 31 * hash + (this.frame != null ? this.frame.hashCode() : 0);
        // CSON: Magic number
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Closure other = (Closure) obj;
        if (this.jexl != other.jexl) {
            return false;
        }
        if ((this.source == null) ? (other.source != null) : !this.source.equals(other.source)) {
            return false;
        }
        if (this.frame != other.frame && (this.frame == null || !this.frame.equals(other.frame))) {
            return false;
        }
        return true;
    }

    @Override
    public String[] getUnboundParameters() {
        return frame.getUnboundParameters();
    }

    /**
     * Sets the hoisted index of a given symbol, ie the target index of a parent hoisted symbol in this closure's frame.
     * <p>This is meant to allow a locally defined function to "see" and call itself as a local (hoisted) variable;
     * in other words, this allows recursive call of a function.
     * @param symbol the symbol index (in the caller of this closure)
     * @param value the value to set in the local frame
     */
    public void setHoisted(int symbol, Object value) {
        if (script instanceof ASTJexlLambda) {
            ASTJexlLambda lambda = (ASTJexlLambda) script;
            Scope scope = lambda.getScope();
            if (scope != null) {
                Integer reg = scope.getHoisted(symbol);
                if (reg != null) {
                    frame.set(reg, value);
                }
            }
        }
    }

    @Override
    public Object evaluate(JexlContext context) {
        return execute(context, (Object[])null);
    }

    @Override
    public Object execute(JexlContext context) {
        return execute(context, (Object[])null);
    }

    @Override
    public Object execute(JexlContext context, Object... args) {
        Scope.Frame callFrame = null;
        if (frame != null) {
            callFrame = frame.assign(args);
        }
        Interpreter interpreter = jexl.createInterpreter(context, callFrame);
        JexlNode block = script.jjtGetChild(script.jjtGetNumChildren() - 1);
        return interpreter.interpret(block);
    }

    @Override
    public Callable callable(JexlContext context, Object... args) {
        Scope.Frame local = null;
        if (frame != null) {
            local = frame.assign(args);
        }
        return new Callable(jexl.createInterpreter(context, local)) {
            @Override
            public Object interpret() {
                JexlNode block = script.jjtGetChild(script.jjtGetNumChildren() - 1);
                return interpreter.interpret(block);
            }
        };
    }

}
