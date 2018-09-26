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

        public void setFail(boolean f) {
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
        JexlEngine jexl = new Engine();
        JexlExpression e = jexl.createExpression("npe()");
        JexlContext jc = new ObjectContext<ThrowNPE>(jexl, new ThrowNPE());
        try {
            e.evaluate(jc);
            Assert.fail("Should have thrown NPE");
        } catch (JexlException xany) {
            Throwable xth = xany.getCause();
            Assert.assertEquals(NullPointerException.class, xth.getClass());
        }
    }

    @Test
    public void testWrappedExmore() throws Exception {
        JexlEngine jexl = new Engine();
        ThrowNPE npe = new ThrowNPE();
        try {
            Object r = jexl.getProperty(npe, "foo");
            Assert.fail("Should have thrown JexlException.Property");
        } catch (JexlException.Property xany) {
            Throwable xth = xany.getCause();
            Assert.assertNull(xth);
        }
        try {
            jexl.setProperty(npe, "foo", 42);
            Assert.fail("Should have thrown JexlException.Property");
        } catch (JexlException.Property xany) {
            Throwable xth = xany.getCause();
            Assert.assertNull(xth);
        }

        boolean b = (Boolean) jexl.getProperty(npe, "fail");
        Assert.assertFalse(b);
        try {
            jexl.setProperty(npe, "fail", false);
            jexl.setProperty(npe, "fail", true);
            Assert.fail("Should have thrown JexlException.Property");
        } catch (JexlException.Property xany) {
            Throwable xth = xany.getCause();
            Assert.assertEquals(NullPointerException.class, xth.getClass());
        }
        try {
            jexl.getProperty(npe, "fail");
            Assert.fail("Should have thrown JexlException.Property");
        } catch (JexlException.Property xany) {
            Throwable xth = xany.getCause();
            Assert.assertEquals(NullPointerException.class, xth.getClass());
        }
        
        try {
            jexl.invokeMethod(npe, "foo", 42);
            Assert.fail("Should have thrown JexlException.Method");
        } catch (JexlException.Method xany) {
            Throwable xth = xany.getCause();
            Assert.assertNull(xth);
        }
        try {
            jexl.invokeMethod(npe, "npe");
            Assert.fail("Should have thrown NullPointerException");
        } catch (JexlException.Method xany) {
            Throwable xth = xany.getCause();
            Assert.assertEquals(NullPointerException.class, xth.getClass());
        }
    }


    // Unknown vars and properties versus null operands
    @Test
    public void testEx() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlExpression e = jexl.createExpression("c.e * 6");
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        // make unknown vars throw
        ctxt.setStrict(true);
        // empty cotext
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e not defined as variable should throw");
        } catch (JexlException.Variable xjexl) {
            String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("c.e") > 0);
        }

        // disallow null operands
        ctxt.setStrictArithmetic(true);
        ctxt.set("c.e", null);
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e as null operand should throw");
        } catch (JexlException.Variable xjexl) {
            String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("c.e") > 0);
        }

        // allow null operands
        ctxt.setStrictArithmetic(false);
        try {
            /* Object o = */ e.evaluate(ctxt);

        } catch (JexlException xjexl) {
            Assert.fail("c.e in expr should not throw");
        }

        // ensure c.e is not a defined property
        ctxt.set("c", "{ 'a' : 3, 'b' : 5}");
        ctxt.set("e", Integer.valueOf(2));
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e not accessible as property should throw");
        } catch (JexlException.Property xjexl) {
            String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("e") > 0);
        }
    }

    // null local vars and strict arithmetic effects
    @Test
    public void testExVar() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlScript e = jexl.createScript("(x)->{ x * 6 }");
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        // make unknown vars throw
        ctxt.setStrict(true);
        ctxt.setStrictArithmetic(true);
        // empty cotext
        try {
            /* Object o = */ e.execute(ctxt);
            Assert.fail("x is null, should throw");
        } catch (JexlException xjexl) {
            String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("null") > 0);
        }

        // allow null operands
        ctxt.setStrictArithmetic(false);
        try {
            Object o = e.execute(ctxt);
        } catch (JexlException.Variable xjexl) {
            Assert.fail("arithmetic allows null operands, should not throw");
        }
    }

    // Unknown vars and properties versus null operands
    @Test
    public void testExMethod() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlExpression e = jexl.createExpression("c.e.foo()");
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        // make unknown vars throw
        ctxt.setStrict(true);
        // empty cotext
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e not declared as variable should throw");
        } catch (JexlException.Variable xjexl) {
            String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("c.e") > 0);
        }

        // disallow null operands
        ctxt.setStrictArithmetic(true);
        ctxt.set("c.e", null);
        try {
            /* Object o = */ e.evaluate(ctxt);
            Assert.fail("c.e as null operand should throw");
        } catch (JexlException xjexl) {
            String msg = xjexl.getMessage();
            Assert.assertTrue(msg.indexOf("c.e") > 0);
        }
    }


    @Test
    public void test206() throws Exception {
        String src = "(null).1 = 2; return 42";
        doTest206(src, false, false);
        doTest206(src, false, true);
        doTest206(src, true, false);
        doTest206(src, true, true);
        src = "x = (null).1; return 42";
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
    private void doTest206(String src, boolean strict, boolean silent) throws Exception {
        CaptureLog l = new CaptureLog();
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().logger(l).strict(strict).silent(silent).create();
        JexlScript e;
        Object r = -1;
        e = jexl.createScript(src);
        try {
            r = e.execute(jc);
            if (strict && !silent) {
                Assert.fail("should have thrown an exception");
            }
        } catch(JexlException xjexl) {
            if (!strict || silent) {
                Assert.fail("should not have thrown an exception");
            }
        }
        if (strict) {
            if (silent && l.count("warn") == 0) {
                Assert.fail("should have generated a warning");
            }
        } else {
            if (l.count("debug") == 0) {
                Assert.fail("should have generated a debug");
            }
            Assert.assertEquals(42, r);
        }
    }
}
