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
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.parser.ASTJexlLambda;

import java.util.Arrays;
import java.util.Objects;

/**
 * A Script closure.
 */
public class Closure extends Script {
    /** The frame. */
    protected final Frame frame;
    /** The options. */
    protected final JexlOptions options;

    /**
     * Creates a closure.
     * @param theCaller the calling interpreter
     * @param lambda the lambda
     */
    protected Closure(final Interpreter theCaller, final ASTJexlLambda lambda) {
        super(theCaller.jexl, null, lambda);
        frame = lambda.createFrame(theCaller.frame);
        final JexlOptions callerOptions = theCaller.options;
        options = callerOptions != null ? callerOptions.copy() :  null;
    }

    /**
     * Creates a curried version of a script.
     * @param base the base script
     * @param args the script arguments
     */
    protected Closure(final Script base, final Object[] args) {
        super(base.jexl, base.source, base.script);
        final Frame sf = (base instanceof Closure) ? ((Closure) base).frame :  null;
        frame = sf == null
                ? script.createFrame(args)
                : sf.assign(args);
        JexlOptions closureOptions = null;
        if (base instanceof Closure) {
            closureOptions = ((Closure) base).options;
        }
        options = closureOptions != null ? closureOptions.copy() :  null;
    }

    @Override
    public int hashCode() {
        // CSOFF: Magic number
        int hash = 17;
        hash = 31 * hash + Objects.hashCode(jexl);
        hash = 31 * hash + Objects.hashCode(source);
        hash = 31 * hash + (frame != null ? Arrays.deepHashCode(frame.nocycleStack(this)) : 0);
        // CSON: Magic number
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
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
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (this.frame == other.frame) {
            return true;
        }
        return Arrays.deepEquals(
                this.frame.nocycleStack(this),
                other.frame.nocycleStack(other));
    }

    @Override
    public String[] getUnboundParameters() {
        return frame.getUnboundParameters();
    }

    /**
     * Sets the captured index of a given symbol, ie the target index of a parent
     * captured symbol in this closure's frame.
     * <p>This is meant to allow a locally defined function to "see" and call
     * itself as a local (captured) variable;
     * in other words, this allows recursive call of a function.
     * @param symbol the symbol index (in the caller of this closure)
     * @param value the value to set in the local frame
     */
    public void setCaptured(final int symbol, final Object value) {
        if (script instanceof ASTJexlLambda) {
            final ASTJexlLambda lambda = (ASTJexlLambda) script;
            final Scope scope = lambda.getScope();
            if (scope != null) {
                final Integer reg = scope.getCaptured(symbol);
                if (reg != null) {
                    frame.set(reg, value);
                }
            }
        }
    }

    @Override
    public Object evaluate(final JexlContext context) {
        return execute(context, (Object[])null);
    }

    @Override
    public Object execute(final JexlContext context) {
        return execute(context, (Object[])null);
    }

    @Override
    public Object execute(final JexlContext context, final Object... args) {
        final Frame local = frame != null ? frame.assign(args) : null;
        final Interpreter interpreter = createInterpreter(context, local, options);
        return interpreter.runClosure(this);
    }

    @Override
    public Callable callable(final JexlContext context, final Object... args) {
        final Frame local = frame != null ? frame.assign(args) : null;
        return new Callable(createInterpreter(context, local, options)) {
            @Override
            public Object interpret() {
                return interpreter.runClosure(Closure.this);
            }
        };
    }

    boolean hasParent(Scope scope) {
        Scope s = frame != null? frame.getScope() : null;
        return scope != null  && s != null
            ? scope == s.getParent()
            : false;
    }
}
