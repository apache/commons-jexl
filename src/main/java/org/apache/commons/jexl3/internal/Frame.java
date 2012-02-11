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

/**
 * A call frame, created from a scope, stores the arguments and local variables as "registers".
 * @since 3.0
 */
public final class Frame {
    /** Registers or arguments. */
    final Object[] registers;
    /** Parameter and argument names if any. */
    final String[] parameters;

    /**
     * Creates a new frame.
     * @param r the registers
     * @param p the parameters
     */
    public Frame(Object[] r, String[] p) {
        registers = r;
        parameters = p;
    }

    /**
     * @return the registers
     */
    public Object[] getRegisters() {
        return registers;
    }

    /**
     * @return the parameters
     */
    public String[] getParameters() {
        return parameters;
    }

    /**
     * Assign arguments to the frame.
     * @param values the argument values
     * @return this frame
     */
    public Frame assign(Object... values) {
        if (registers != null && values != null && values.length > 0) {
            System.arraycopy(values, 0, registers, 0, Math.min(registers.length, values.length));
        }
        return this;
    }
    
}
