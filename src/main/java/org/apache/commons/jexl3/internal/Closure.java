/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.parser.ASTJexlLambda;

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
     *
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
     *
     * @param base the base script
     * @param args the script arguments
     */
    protected Closure(final Script base, final Object[] args) {
        super(base.jexl, base.source, base.script);
        final Frame sf = base instanceof Closure ? ((Closure) base).frame :  null;
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
    public Callable callable(final JexlContext context, final Object... args) {
        final Frame local = frame != null ? frame.assign(args) : null;
        return new Callable(createInterpreter(context, local, options)) {
            @Override
            public Object interpret() {
                return interpreter.runClosure(Closure.this);
            }
        };
    }

    /**
     * Enable lambda recursion.
     * <p>Assign this lambda in its own frame if the symbol it is assigned to in its definition scope
     * is captured in its body.</p>
     * <p>This done allow a locally defined function to "see" and call  itself as a local (captured) variable.</p>
     * Typical case is: {@code const f = (x)->x <= 0? 1 : x*f(x-1)}. Since assignment of f occurs after
     * the lambda creation, we need to patch the lambda frame to expose itself through the captured symbol.
     *
     * @param parentFrame the parent calling frame
     * @param symbol the symbol index (in the caller of this closure)
     */
    void captureSelfIfRecursive(final Frame parentFrame, final int symbol) {
        if (script instanceof ASTJexlLambda) {
            final Scope parentScope = parentFrame != null ? parentFrame.getScope() : null;
            final Scope localScope = frame != null ? frame.getScope() : null;
            if (parentScope != null && localScope != null && parentScope == localScope.getParent()) {
                final Integer reg = localScope.getCaptured(symbol);
                if (reg != null) {
                    frame.set(reg, this);
                }
            }
        }
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
    public String[] getUnboundParameters() {
        return frame.getUnboundParameters();
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

}
