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
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

/**
 * Checks various exception handling cases.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
class ExceptionTest extends JexlTestCase {
    public static class ThrowNPE {
        boolean doThrow;
        public boolean getFail() {
            if (doThrow) {
                throw new NullPointerException("ThrowNPE/get");
            }
            return doThrow;
        }

        public String npe() {
            throw new NullPointerException("ThrowNPE");
        }

        public void setFail(final boolean f) {
            doThrow = f;
            if (f) {
                throw new NullPointerException("ThrowNPE/set");
            }
        }
    }

    public static class ConstructNPE {
        public ConstructNPE() {
            throw new NullPointerException("ConstructNPE");
        }
    }

    /** Create a named test */
    public ExceptionTest() {
        super("ExceptionTest");
    }

    private void doTest206(final String src, final boolean strict, final boolean silent) {
        final CaptureLog l = new CaptureLog();
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().logger(l).strict(strict).silent(silent).create();
        JexlScript e;
        Object r = -1;
        e = jexl.createScript(src);
        try {
            r = e.execute(jc);
            if (strict && !silent) {
                fail("should have thrown an exception");
            }
        } catch (final JexlException xjexl) {
            if (!strict || silent) {
                fail(src + ": should not have thrown an exception");
            }
        }
        if (strict) {
            if (silent && l.count("warn") == 0) {
                fail(src + ": should have generated a warning");
            }
        } else {
            if (l.count("debug") == 0) {
                fail(src + ": should have generated a debug");
            }
            assertEquals(42, r);
        }
    }

    @Test
    void test206() throws Exception {
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

    // Unknown vars and properties versus null operands
    // JEXL-73
    @Test
    void testEx() {
        final JexlEngine jexl = createEngine(false);
        final JexlExpression e = jexl.createExpression("c.e * 6");
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        // make unknown vars throw
        options.setStrict(true);
        // empty cotext
        JexlException.Variable xjexl = assertThrows(JexlException.Variable.class, () -> e.evaluate(ctxt), "c not defined as variable should throw");
        String msg = xjexl.getMessage();
        assertTrue(msg.indexOf("variable 'c.e'") > 0);

        // disallow null operands
        options.setStrictArithmetic(true);
        ctxt.set("c.e", null);
        xjexl = assertThrows(JexlException.Variable.class, () -> e.evaluate(ctxt), "c.e as null operand should throw");
        msg = xjexl.getMessage();
        assertTrue(msg.indexOf("variable 'c.e'") > 0);

        // allow null operands
        options.setStrictArithmetic(false);
        /* Object o = */ e.evaluate(ctxt);

        // ensure c.e is not a defined property
        ctxt.set("c", "{ 'a' : 3, 'b' : 5}");
        ctxt.set("e", Integer.valueOf(2));
        final JexlException.Property ep = assertThrows(JexlException.Property.class, () -> e.evaluate(ctxt));
        msg = ep.getMessage();
        assertTrue(msg.indexOf("property 'e") > 0);
    }

    // Unknown vars and properties versus null operands
    @Test
    void testExMethod() {
        final JexlEngine jexl = createEngine(false);
        final JexlExpression e = jexl.createExpression("c.e.foo()");
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setSilent(false);
        // make unknown vars throw
        options.setStrict(true);
        // empty cotext
        JexlException xjexl = assertThrows(JexlException.class, () -> e.evaluate(ctxt), "c not declared as variable should throw");
        String msg = xjexl.getMessage();
        assertTrue(msg.indexOf("variable 'c.e'") > 0);

        // disallow null operands
        options.setStrictArithmetic(true);
        ctxt.set("c.e", null);
        xjexl = assertThrows(JexlException.class, () -> e.evaluate(ctxt));
        msg = xjexl.getMessage();
        assertTrue(msg.indexOf("variable 'c.e'") > 0);
    }

    // null local vars and strict arithmetic effects
    @Test
    void testExVar() {
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
        final JexlException xjexl = assertThrows(JexlException.class, () -> e.execute(ctxt));
        final String msg = xjexl.getMessage();
        assertTrue(msg.indexOf("null") > 0);

        // allow null operands
        options.setStrictArithmetic(false);
        assertEquals(0, e.execute(ctxt, (Object) null));
    }

    @Test
    void testWrappedEx() {
        runWrappedEx(true, true);
        runWrappedEx(false, true);
        runWrappedEx(true, false);
        runWrappedEx(false, false);
    }

    /**
     * Runs various calls that throw NPE wrapped in JexlExceptions.
     * @param debug if true/false, debug mode is on/off
     * @param silent if true/false, silent mode is on/off
     */
    private static void runWrappedEx(boolean debug, boolean silent) {
        final CaptureLog log = new CaptureLog();
        final JexlBuilder builder = new JexlBuilder().safe(false).strict(true)
                .logger(log).debug(debug).silent(silent);
        final JexlEngine jexl = builder.create();
        final JexlContext jc = new ObjectContext<>(jexl, new ThrowNPE());
        callWrappedEx(debug, silent, log, () -> jexl.createExpression("npe()").evaluate(jc));
        callWrappedEx(debug, silent, log, () -> jexl.newInstance(ConstructNPE.class));
        ThrowNPE npe = new ThrowNPE();
        callWrappedEx(debug, silent, log, () -> jexl.invokeMethod(npe, "npe"));
        // side effect to set failure in getter coming next
        callWrappedEx(debug, silent, log, () -> {
            jexl.setProperty(npe, "fail", true);
            return null;
        });
        callWrappedEx(debug, silent, log, () -> jexl.getProperty(npe, "fail"));
    }

    /**
     * Calls a NPE throwing call wrapped in JexlException and checks the outcome.
     * @param debug if true/false, debug mode is on/off
     * @param silent if true/false, silent mode is on/off
     * @param npeCall the NPE throwing call
     */
    private static void callWrappedEx(boolean debug, boolean silent, CaptureLog log, Supplier<Object> npeCall) {
        try {
            npeCall.get();
            if (!silent) {
                fail("should have thrown");
            } else {
                // in silent mode, ensure a log was captured
                assertEquals(1, log.count("warn"), "should have 1 warn log");
                String msg = log.getCapturedMessages().get(0);
                assertEquals(debug, msg.contains("runWrappedEx"), "class/method/line?");
                log.clear();
            }
        } catch (final JexlException exception) {
            if (silent) {
                fail("should not have throw, should be silent");
            }
            assertEquals(debug, exception.getMessage().contains("runWrappedEx"), "class/method/line?");
            final Throwable xth = exception.getCause();
            assertEquals(NullPointerException.class, xth.getClass(), "Should have thrown NPE");
        }
    }

    @Test
    void testWrappedExmore() {
        final JexlEngine jexl = new JexlBuilder().debug(true).safe(false).create();
        final ThrowNPE npe = new ThrowNPE();
        assertNull(assertThrows(JexlException.Property.class, () -> jexl.getProperty(npe, "foo")).getCause());
        assertNull(assertThrows(JexlException.Property.class, () -> jexl.setProperty(npe, "foo", 42)).getCause());

        final boolean b = (Boolean) jexl.getProperty(npe, "fail");
        assertFalse(b);
        jexl.setProperty(npe, "fail", false);
        assertEquals(NullPointerException.class, assertThrows(JexlException.Property.class, () -> jexl.setProperty(npe, "fail", true)).getCause().getClass());
        assertEquals(NullPointerException.class, assertThrows(JexlException.Property.class, () -> jexl.getProperty(npe, "fail")).getCause().getClass());
        assertNull(assertThrows(JexlException.Method.class, () -> jexl.invokeMethod(npe, "foo", 42)).getCause());
        assertEquals(NullPointerException.class, assertThrows(JexlException.Method.class, () -> jexl.invokeMethod(npe, "npe")).getCause().getClass());
    }
}
