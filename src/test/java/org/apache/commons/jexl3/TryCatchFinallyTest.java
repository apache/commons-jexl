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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class TryCatchFinallyTest extends JexlTestCase {
    public static class Circuit implements AutoCloseable {
        boolean opened = true;

        @Override
        public void close() throws IOException {
            opened = false;
        }

        public boolean isOpened() {
            return opened;
        }

        public void raiseError() {
            throw new RuntimeException("raising error");
        }
    }

    public TryCatchFinallyTest() {
        super(TryCatchFinallyTest.class.getSimpleName());
    }

    @Test
    void testCloseable0x2b() {
        final String src = "try(let x = c) { c.isOpened()? 42 : -42; } finally { 169; }";
        final JexlScript script = JEXL.createScript(src, "c");
        final Circuit circuit = new Circuit();
        assertNotNull(script);
        final Object result = script.execute(null, circuit);
        assertEquals(42, result);
        assertFalse(circuit.isOpened());
    }

    @Test
    void testCloseable0x3b() {
        final String src = "try(let x = c) { c.raiseError(); -42; } catch(const y) { 42; } finally { 169; }";
        final JexlScript script = JEXL.createScript(src, "c");
        final Circuit circuit = new Circuit();
        assertNotNull(script);
        final Object result = script.execute(null, circuit);
        assertEquals(42, result);
        assertFalse(circuit.isOpened());
    }

    @Disabled
    void testEdgeTry() throws Exception {
        int i = 0;
        while (i++ < 5) {
            // System.out.println("i: " + i);
            try {
                throw new JexlException.Continue(null);
            } finally {
                continue;
            }
        }
        // System.out.println("iii: " + i);

        // int x = 0;
        try (AutoCloseable x = new Circuit()) {
            // empty
        }
    }

    @Test
    void testExceptionType() throws Exception {
        final JexlScript e = JEXL.createScript("try { 'asb'.getBytes('NoSuchCharacterSet'); } catch (let ex) { ex }");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertInstanceOf(UnsupportedEncodingException.class, o);
    }

    @Test
    void testForm0x2a() {
        final String src = "try(let x = 42) { x; } finally { 169; }";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void testForm0x2b() {
        final String src = "try(let x = 19, y = 23) { x + y; } finally { 169; }";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void testForm0x2c() {
        final String src = "try(const x = 19; let y = 23; ) { x + y; } finally { 169; }";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void testForm0x2d() {
        final String src = "try(var x = 19; const y = 23;) { x + y; } finally { 169; }";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void testRedefinition0() {
        final String src = "try(let x = c) { let x = 3; -42; }";
        final JexlException.Parsing xvar = assertThrows(JexlException.Parsing.class, () -> JEXL.createScript(src, "c"));
        assertTrue(xvar.getMessage().contains("x: variable is already declared"));
    }

    @Test
    void testRedefinition1() {
        final String src = "const x = 33; try(let x = c) { 169; }";
        final JexlException.Parsing xvar = assertThrows(JexlException.Parsing.class, () -> JEXL.createScript(src, "c"));
        assertTrue(xvar.getMessage().contains("x: variable is already declared"));
    }

    @Test
    void testStandard0x2() {
        final String src = "try { 42; } finally { 169; }";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void testThrow0x2a() {
        final String src = "try(let x = 42) { throw x } finally { 169; }";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final JexlException.Throw xthrow = assertThrows(JexlException.Throw.class, () -> script.execute(null));
        assertEquals(42, xthrow.getValue());
    }

    @Test
    void testThrow0x2b() {
        final String src = "try(let x = 42) { throw x } finally { throw 169 }";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final JexlException.Throw xthrow = assertThrows(JexlException.Throw.class, () -> script.execute(null));
        assertEquals(169, xthrow.getValue());
    }

    @Test
    void testThrowCatchBreakFinallyContinue() {
        final String src = "let r = 0; for(let i : 37..42) { try(let x = 169) { r = i; throw -x } catch(const y) { break } finally { continue } } r";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void testThrowCatchContinueFinallyBreak() {
        final String src = "let r = 0; for(let i : 42..37) { try(let x = 169) { r = i; throw -x } catch(const y) { continue } finally { break } } r";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void testThrowCatchThrow() {
        final String src = "try(let x = 42) { throw x } catch(const y) { throw -(y.value) } ";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final JexlException.Throw xthrow = assertThrows(JexlException.Throw.class, () -> script.execute(null));
        assertEquals(-42, xthrow.getValue());
    }

    @Test
    void testThrowCatchThrowFinallyThrow() {
        final String src = "try(let x = 42) { throw x } catch(const y) { throw -(y.value) } finally { throw 169 }";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final JexlException.Throw xthrow = assertThrows(JexlException.Throw.class, () -> script.execute(null));
        assertEquals(169, xthrow.getValue());
    }

    @Test
    void testThrowRecurse() {
        final String src = "function fact(x, f) { if (x == 1) throw f; fact(x - 1, f * x); } fact(7, 1);";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final JexlException.Throw xthrow = assertThrows(JexlException.Throw.class, () -> script.execute(null));
        assertEquals(5040, xthrow.getValue());
    }

    @Test
    void testTryReturn() {
        final String src = "try(let x = 42) { return x } catch(const y) { throw -(y.value) } ";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    void testTryReturnFinallyReturn() {
        final String src = "try(let x = 42) { return x } finally { return 169 } ";
        final JexlScript script = JEXL.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(169, result);
    }
}
