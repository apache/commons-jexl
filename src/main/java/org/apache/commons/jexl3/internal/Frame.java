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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A call frame, created from a scope, stores the arguments and local variables in a "stack frame" (sic).
 * @since 3.0
 */
public class Frame {
    /** The scope. */
    private final Scope scope;
    /** The actual stack frame. */
    protected final Object[] stack;
    /** Number of curried parameters. */
    private final int curried;

    /**
     * Creates a new frame.
     * @param s the scope
     * @param r the stack frame
     * @param c the number of curried parameters
     */
    protected Frame(final Scope s, final Object[] r, final int c) {
        scope = s;
        stack = r;
        curried = c;
    }

    /**
     * Assign values to this frame.
     * @param values the values
     * @return this frame
     */
    Frame assign(final Object... values) {
        if (stack != null) {
            final int nparm = scope.getArgCount();
            final Object[] copy = stack.clone();
            int ncopy = 0;
            if (values != null && values.length > 0) {
                ncopy = Math.min(nparm - curried, Math.min(nparm, values.length));
                System.arraycopy(values, 0, copy, curried, ncopy);
            }
            // unbound parameters are defined as null
            Arrays.fill(copy, curried + ncopy, nparm, null);
            return newFrame(scope, copy, curried + ncopy);
        }
        return this;
    }

    /**
     * Creates a new from of this frame&quot;s class.
     * @param s the scope
     * @param r the arguments
     * @param c the number of curried parameters
     * @return a new instance of frame
     */
    Frame newFrame(final Scope s, final Object[] r, final int c) {
        return new Frame(s, r, c);
    }

    /**
     * Captures a value.
     * @param s the offset in this frame
     * @return the stacked value
     */
    Object capture(final int s) {
        return stack[s];
    }

    /**
     * Gets a value.
     * @param s the offset in this frame
     * @return the stacked value
     */
    Object get(final int s) {
        return stack[s];
    }

    /**
     * Sets a value.
     * @param r the offset in this frame
     * @param value the value to set in this frame
     */
    void set(final int r, final Object value) {
        stack[r] = value;
    }

    /**
     * Gets the scope.
     * @return this frame scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Gets this script unbound parameters, i.e. parameters not bound through curry().
     * @return the parameter names
     */
    public String[] getUnboundParameters() {
        return scope.getParameters(curried);
    }

    /**
     * Whether this frame defines a symbol, ie declared it and assigned it a value.
     * @param s the offset in this frame
     * @return true if this symbol has been assigned a value, false otherwise
     */
    boolean has(final int s) {
        return s >= 0 && s < stack.length && stack[s] != Scope.UNDECLARED;
    }

    /**
     * Replace any instance of a closure in this stack by its (fuzzy encoded) offset in it.
     * <p>This is to avoid the cyclic dependency between the closure and its frame stack that
     * may point back to it that occur with recursive function definitions.</p>
     * @param closure the owning closure
     * @return the cleaned-up stack or the stack itself (most of the time)
     */
    Object[] nocycleStack(final Closure closure) {
        Object[] ns = stack;
        for(int i = 0; i < stack.length; ++i) {
            if (stack[i] == closure) {
                if (ns == stack) {
                    ns = stack.clone();
                }
                // fuzz it a little
                ns[i] = Closure.class.hashCode() + i;
            }
        }
        return ns;
    }
}

/**
 * Pass-by-reference frame.
 */
class ReferenceFrame extends Frame {
    ReferenceFrame(final Scope s, final Object[] r, final int c) {
        super(s, r, c);
    }

    @Override
    Frame newFrame(final Scope s, final Object[] r, final int c) {
        return new ReferenceFrame(s, r, c);
    }

    @Override
    CaptureReference capture(final int s) {
        final Object o = stack[s];
        if (o instanceof CaptureReference) {
            return (CaptureReference) o;
        } else {
            // change the type of the captured register, wrap the value in a reference
            final CaptureReference captured = new CaptureReference(o);
            stack[s] = captured;
            return captured;
        }
    }

    @Override
    Object get(final int s) {
        final Object o = stack[s];
        return o instanceof CaptureReference ? ((CaptureReference) o).get() : o;
    }

    @Override
    void set(final int r, final Object value) {
        final Object o = stack[r];
        if (o instanceof CaptureReference) {
            if (value != Scope.UNDEFINED && value != Scope.UNDECLARED) {
                ((CaptureReference) o).set(value);
            }
        } else {
            stack[r] = value;
        }
    }
}

/**
 * Captured variable reference.
 */
class CaptureReference extends AtomicReference<Object> {
    CaptureReference(final Object o) {
        super(o);
    }
}