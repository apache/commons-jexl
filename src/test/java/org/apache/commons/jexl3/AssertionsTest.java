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
package org.apache.commons.jexl3;

import java.util.Arrays;
import java.util.List;

import java.math.MathContext;
import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for assert statement.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class AssertionsTest extends JexlTestCase {

    public AssertionsTest() {
        super("AssertionsTest");
    }

    @Test
    public void testInitiallyDisabledAssertions() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript s = JEXL.createScript("assert false");
        Object o = s.execute(jc);
        Assert.assertNull(o);
    }

    @Test
    public void testEnabledAssertions() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().assertions(true).create();

        JexlScript s = jexl.createScript("assert true");
        Object o = s.execute(jc);
        Assert.assertNull(o);

        s = jexl.createScript("assert false");
        try {
             o = s.execute(jc);
             Assert.fail("Should have failed");
        } catch (AssertionError ex) {
             // OK
        }
    }

    @Test
    public void testAssertionMessages() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().assertions(true).create();
        JexlScript s = jexl.createScript("assert false : 'check'");
        try {
             Object o = s.execute(jc);
             Assert.fail("Should have failed");
        } catch (AssertionError ex) {
             Assert.assertEquals("check", ex.getMessage());
        }
    }

    public class AssertionsContext implements JexlContext, JexlEngine.Options {

        /** The wrapped context. */
        private final JexlContext wrapped;

        private final boolean assertions;

        /**
         * Creates a new readonly context.
         * @param context the wrapped context
         * @param eopts the engine evaluation options
         */
        public AssertionsContext(JexlContext context, boolean assertions) {
            wrapped = context;
            this.assertions = assertions;
        }

        @Override
        public Object get(String name) {
            return wrapped.get(name);
        }

        @Override
        public void set(String name, Object value) {
            wrapped.set(name, value);
        }

        @Override
        public boolean has(String name) {
            return wrapped.has(name);
        }

        @Override
        public Boolean isSilent() {
            return false;
        }

        @Override
        public Boolean isStrict() {
            return true;
        }

        @Override
        public Boolean isAssertions() {
            return assertions;
        }

        @Override
        public Boolean isCancellable() {
            return true;
        }

        @Override
        public Boolean isStrictArithmetic() {
            return true;
        }

        @Override
        public MathContext getArithmeticMathContext() {
            return null;
        }

        @Override
        public int getArithmeticMathScale() {
            return -1;
        }

        @Override
        public Charset getCharset() {
            return Charset.defaultCharset();
        }
    }

    @Test
    public void testContextEnabledAssertions() throws Exception {
        JexlContext jc = new AssertionsContext(new MapContext(), true);
        JexlEngine jexl = new JexlBuilder().assertions(false).create();
        JexlScript s = jexl.createScript("assert false");
        try {
             Object o = s.execute(jc);
             Assert.fail("Should have failed");
        } catch (AssertionError ex) {
             // OK
        }
    }

    @Test
    public void testContextDisabledAssertions() throws Exception {
        JexlContext jc = new AssertionsContext(new MapContext(), false);
        JexlEngine jexl = new JexlBuilder().assertions(true).create();
        JexlScript s = jexl.createScript("assert false");
        Object o = s.execute(jc);
        Assert.assertNull(o);
    }

}
