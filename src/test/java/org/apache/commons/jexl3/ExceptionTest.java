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

import org.apache.commons.jexl3.internal.Engine;
import org.junit.Assert;
import org.junit.Test;

/**
 * Checks various exception handling cases.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ExceptionTest extends JexlTestCase {
    /** create a named test */
    public ExceptionTest() {
        super("ExceptionTest");
    }

    public static class ThrowNPE {
        boolean doThrow = false;
        public String npe() {
            throw new NullPointerException("ThrowNPE");
        }

        public void setFail(final boolean f) {
            doThrow = f;
            if (f) {
                throw new NullPointerException("ThrowNPE/set");
            }
        }

        public boolean getFail() {
            if (doThrow) {
                throw new NullPointerException("ThrowNPE/get");
            }
            return doThrow;
        }
    }

    @Test
    public void testWrappedEx() throws Exception {
        final JexlEngine jexl = new Engine();
        final JexlExpression e = jexl.createExpression("npe()");
        final JexlContext jc = new ObjectContext<ThrowNPE>(jexl, new ThrowNPE());
        try {
            e.evaluate(jc);
            Assert.fail("Should have thrown NPE");
        } catch (final JexlException xany) {
            final Throwable xth = xany.getCause();
            Assert.assertEquals(NullPointerException.class, xth.getClass());
        }
    }

    @Test
    public void testWrappedExmore() throws Exception {
        final JexlEngine jexl = new Engine();
        final ThrowNPE npe = new ThrowNPE();
        try {
            final Object r = jexl.getProperty(npe, "foo");
            Assert.fail("Should have thrown JexlException.Property");
        } catch (final JexlException.Property xany) {
            final Throwable xth = xany.getCause();
            Assert.assertNull(xth);
        }
        try {
            jexl.setProperty(npe, "foo", 42);
            Assert.fail("Should have thrown JexlException.Property");
        } catch (final JexlException.Property xany) {
            final Throwable xth = xany.getCause();
            Assert.assertNull(xth);
        }

        final boolean b = (Boolean) jexl.getProperty(npe, "fail");
        Assert.assertFalse(b);
        try {
            jexl.setProperty(npe, "fail", false);
            jexl.setProperty(npe, "fail", true);
            Assert.fail("Should have thrown JexlException.Property");
        } catch (final JexlException.Property xany) {
            final Throwable xth = xany.getCause();
            Assert.assertEquals(NullPointerException.class, xth.getClass());
        }
        try {
            jexl.getProperty(npe, "fail");
            Assert.fail("Should have thrown JexlException.Property");
        } catch (final JexlException.Property xany) {
            final Throwable xth = xany.getCause();
            Assert.assertEquals(NullPointerException.class, xth.getClass());
        }

        try {
            jexl.invokeMethod(npe, "foo", 42);
            Assert.fail("Should have thrown JexlException.Method");
        } catch (final JexlException.Method xany) {
            final Throwable xth = xany.getCause();
            Assert.assertNull(xth);
        }
        try {
            jexl.invokeMethod(npe, "npe");
            Assert.fail("Should have thrown NullPointerException");
        } catch (final JexlException.Method xany) {
            final Throwable xth = xany.getCause();
            Assert.assertEquals(NullPointerException.class, xth.getClass());
        }
    }

    // Unknown vars and properties versus null operands
    // JEXL-73
    @Test
    public void testEx() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final JexlExpression e = jexl.createExpression("c.e * 6");
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        // make unknown vars throw
        options.setStrict(true);
        // empty cotext
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c not defined as variable should throw");
        } catch (final JexlException.Variable xjexl) {
            final String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("variable 'c.e'") > 0);
        }

        // disallow null operands
        options.setStrictArithmetic(true);
        ctxt.set("c.e", null);
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e as null operand should throw");
        } catch (final JexlException.Variable xjexl) {
            final String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("variable 'c.e'") > 0);
        }

        // allow null operands
        options.setStrictArithmetic(false);
        try {
            /* Object o = */ e.evaluate(ctxt);

        } catch (final JexlException xjexl) {
            Assert.fail("c.e in expr should not throw");
        }

        // ensure c.e is not a defined property
        ctxt.set("c", "{ 'a' : 3, 'b' : 5}");
        ctxt.set("e", Integer.valueOf(2));
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e not accessible as property should throw");
        } catch (final JexlException.Property xjexl) {
            final String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("property 'e") > 0);
        }
    }

    // null local vars and strict arithmetic effects
    @Test
    public void testExVar() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final JexlScript e = jexl.createScript("(x)->{ x * 6 }");
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        // make unknown vars throw
        options.setStrict(true);
        options.setStrictArithmetic(true);
        // empty cotext
        try {
            /* Object o = */ e.execute(ctxt);
            Assert.fail("x is null, should throw");
        } catch (final JexlException xjexl) {
            final String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("null") > 0);
        }

        // allow null operands
        options.setStrictArithmetic(false);
        try {
            final Object o = e.execute(ctxt, (Object) null);
        } catch (final JexlException.Variable xjexl) {
            Assert.fail("arithmetic allows null operands, should not throw");
        }
    }

    // Unknown vars and properties versus null operands
    @Test
    public void testExMethod() throws Exception {
        final JexlEngine jexl = createEngine(false);
        final JexlExpression e = jexl.createExpression("c.e.foo()");
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        // make unknown vars throw
        options.setStrict(true);
        // empty cotext
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c not declared as variable should throw");
        } catch (final JexlException.Variable xjexl) {
            final String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("variable 'c.e'") > 0);
        }

        // disallow null operands
        options.setStrictArithmetic(true);
        ctxt.set("c.e", null);
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e as null operand should throw");
        } catch (final JexlException xjexl) {
            final String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("variable 'c.e'") > 0);
        }
    }

    @Test
    public void test206() throws Exception {
        String src = "null.1 = 2; return 42";
        doTest206(src, false, false);
        doTest206(src, false, true);
        doTest206(src, true, false);
        doTest206(src, true, true);
        src = "x = null.1; return 42";
        doTest206(src, false, false);
        doTest206(src, false, true);
        doTest206(src, true, false);
        doTest206(src, true, true);
        src = "x = y.1; return 42";
        doTest206(src, false, false);
        doTest206(src, false, true);
        doTest206(src, true, false);
        doTest206(src, true, true);
    }
    private void doTest206(final String src, final boolean strict, final boolean silent) throws Exception {
        final CaptureLog l = new CaptureLog();
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().logger(l).strict(strict).silent(silent).create();
        JexlScript e;
        Object r = -1;
        e = jexl.createScript(src);
        try {
            r = e.execute(jc);
            if (strict && !silent) {
                Assert.fail("should have thrown an exception");
            }
        } catch(final JexlException xjexl) {
            if (!strict || silent) {
                Assert.fail(src + ": should not have thrown an exception");
            }
        }
        if (strict) {
            if (silent && l.count("warn") == 0) {
                Assert.fail(src + ": should have generated a warning");
            }
        } else {
            if (l.count("debug") == 0) {
                Assert.fail(src + ": should have generated a debug");
            }
            Assert.assertEquals(42, r);
        }
    }
}
