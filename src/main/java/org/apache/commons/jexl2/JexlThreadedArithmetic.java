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
package org.apache.commons.jexl2;

/**
 * A derived arithmetic that allows different threads to operate with
 * different strict/lenient modes using the same JexlEngine.
 */
public class JexlThreadedArithmetic extends JexlArithmetic {
    /** Whether this JexlArithmetic instance behaves in strict or lenient mode for this thread. */
    protected static final ThreadLocal<Boolean> lenient = new ThreadLocal<Boolean>();

    /**
     * Standard ctor.
     * @param lenient lenient versus strict evaluation flag
     */
    public JexlThreadedArithmetic(boolean lenient) {
        super(lenient);
    }

    /**
     * Overrides the default behavior and sets whether this JexlArithmetic instance triggers errors
     * during evaluation when null is used as an operand for the current thread.
     * <p>It is advised to protect calls by either calling JexlThreadedArithmetic.setLenient explicitly before evaluation
     * or add a try/finally clause resetting the flag to avoid unexpected reuse of the lenient
     * flag value through thread pools side-effects.</p>
     * @see JexlEngine#setSilent
     * @see JexlEngine#setDebug
     * @param flag true means no JexlException will occur, false allows them, null reverts to default behavior
     */
    public static void setLenient(Boolean flag) {
        lenient.set(flag);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLenient() {
        Boolean tl = lenient.get();
        return tl == null? super.isLenient() : tl.booleanValue();
    }
}
