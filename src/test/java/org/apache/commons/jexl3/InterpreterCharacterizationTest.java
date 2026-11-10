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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Characterization tests pinning the current behavior of loops, catch, lexical blocks
 * and antish variable resolution/assignment in the interpreter.
 * <p>Each case is {mode, script, expected} where expected is either "= value" or
 * "ExceptionSimpleName: @line:col message".</p>
 */
class InterpreterCharacterizationTest extends JexlTestCase {

    private static final String[][] CASES = {
        {"lex", "var s = 0; for (var i = 0; i < 10; ++i) { if (i == 2) continue; if (i == 5) break; s += i; } s", "= 8"},
        {"nolex", "var s = 0; for (var i = 0; i < 10; ++i) { if (i == 2) continue; if (i == 5) break; s += i; } s", "= 8"},
        {"lex", "var s = 0; for (var i = 0, j = 5; i < j; ++i) { s += i; } s", "= 10"},
        {"nolex", "var s = 0; for (var i = 0, j = 5; i < j; ++i) { s += i; } s", "= 10"},
        {"lex", "var i = 0; var s = 0; do { i++; if (i == 2) continue; if (i == 4) break; s += i; } while (i < 10); s", "= 4"},
        {"nolex", "var i = 0; var s = 0; do { i++; if (i == 2) continue; if (i == 4) break; s += i; } while (i < 10); s", "= 4"},
        {"lex", "var i = 0; var s = 0; while (i < 5) { i++; if (i % 2 == 0) continue; s += i; } s", "= 9"},
        {"nolex", "var i = 0; var s = 0; while (i < 5) { i++; if (i % 2 == 0) continue; s += i; } s", "= 9"},
        {"lex", "var s = 0; for (var x : [1, 2, 3]); s", "= 0"},
        {"nolex", "var s = 0; for (var x : [1, 2, 3]); s", "= 0"},
        {"lex", "var s = 0; for (var x : [1, 2, 3, 4]) { if (x == 2) continue; if (x == 4) break; s += x; } s", "= 4"},
        {"nolex", "var s = 0; for (var x : [1, 2, 3, 4]) { if (x == 2) continue; if (x == 4) break; s += x; } s", "= 4"},
        {"lex", "var s = 0; for (x : [1, 2, 3]) { s += x; } s + x", "= 9"},
        {"nolex", "var s = 0; for (x : [1, 2, 3]) { s += x; } s + x", "= 9"},
        {"lex", "var s = 0; for (let x : [1, 2, 3]) { s += x; } s", "= 6"},
        {"nolex", "var s = 0; for (let x : [1, 2, 3]) { s += x; } s", "= 6"},
        {"lex", "var s = 0; var k = 3; switch (k) { case 1: s = 1; break; case 3: s = 3; break; default: s = 9; } s", "= 3"},
        {"nolex", "var s = 0; var k = 3; switch (k) { case 1: s = 1; break; case 3: s = 3; break; default: s = 9; } s", "= 3"},
        {"lex", "try { throw 'a' } catch (e) { e }", "= Throw"},
        {"nolex", "try { throw 'a' } catch (e) { e }", "= Throw"},
        {"lex", "try { throw 'a' } catch (var e) { e }", "= Throw"},
        {"nolex", "try { throw 'a' } catch (var e) { e }", "= Throw"},
        {"lex", "try { throw 'a' } catch (let e) { e }", "= Throw"},
        {"nolex", "try { throw 'a' } catch (let e) { e }", "= Throw"},
        {"lex", "let e = 1; try { throw 'a' } catch (let e) { e }", "Parsing: @1:41 parsing error in 'e: variable is already declared'"},
        {"nolex", "let e = 1; try { throw 'a' } catch (let e) { e }", "Parsing: @1:41 parsing error in 'e: variable is already declared'"},
        {"lex", "try { throw 'a' } catch (e) { 1 }; e", "= Throw"},
        {"nolex", "try { throw 'a' } catch (e) { 1 }; e", "= Throw"},
        {"lex", "var r = 0; { let y = 2; r = y; } r", "= 2"},
        {"nolex", "var r = 0; { let y = 2; r = y; } r", "= 2"},
        {"lex", "let x = 1; { let x = 2; }", "Parsing: @1:18 parsing error in 'x: variable is already declared'"},
        {"nolex", "let x = 1; { let x = 2; }", "Parsing: @1:18 parsing error in 'x: variable is already declared'"},
        {"ant", "x.y.z", "Property: @1:5 undefined property 'z'"},
        {"noant", "x.y.z", "Property: @1:1 null value property '.y'"},
        {"strict", "x.y.z", "Property: @1:5 undefined property 'z'"},
        {"safe", "x.y.z", "Property: @1:5 undefined property 'z'"},
        {"ant", "x.y.z = 3", "Property: @1:7 undefined property 'z'"},
        {"noant", "x.y.z = 3", "JexlException: @1:1 JEXL error : illegal assignment form"},
        {"strict", "x.y.z = 3", "Property: @1:7 undefined property 'z'"},
        {"safe", "x.y.z = 3", "Property: @1:7 undefined property 'z'"},
        {"ant", "x.y.z += 2", "Property: @1:7 undefined property 'z'"},
        {"noant", "x.y.z += 2", "JexlException: @1:1 JEXL error : illegal assignment form"},
        {"strict", "x.y.z += 2", "Property: @1:7 undefined property 'z'"},
        {"safe", "x.y.z += 2", "Property: @1:7 undefined property 'z'"},
        {"ant", "x.y?.z.w = 3", "Property: @1:7 undefined property '<null>.<?>'"},
        {"noant", "x.y?.z.w = 3", "JexlException: @1:1 JEXL error : illegal assignment form"},
        {"strict", "x.y?.z.w = 3", "Property: @1:7 undefined property '<null>.<?>'"},
        {"safe", "x.y?.z.w = 3", "Property: @1:7 undefined property '<null>.<?>'"},
        {"ant", "x.y.`'z'`.w = 3", "Property: @1:10 undefined property ''z''"},
        {"noant", "x.y.`'z'`.w = 3", "JexlException: @1:1 JEXL error : illegal assignment form"},
        {"strict", "x.y.`'z'`.w = 3", "Property: @1:10 undefined property ''z''"},
        {"safe", "x.y.`'z'`.w = 3", "Property: @1:10 undefined property ''z''"},
        {"ant", "x.y.size()", "= 2"},
        {"noant", "x.y.size()", "Property: @1:1 null value property '.y'"},
        {"strict", "x.y.size()", "= 2"},
        {"safe", "x.y.size()", "= 2"},
        {"ant", "x.y.foo()", "Method: @1:4 unsolvable function/method 'foo'"},
        {"noant", "x.y.foo()", "Property: @1:1 null value property '.y'"},
        {"strict", "x.y.foo()", "Method: @1:4 unsolvable function/method 'foo'"},
        {"safe", "x.y.foo()", "= null"},
        {"ant", "x.a.b", "Variable: @1:1 variable 'x.a.b' is undefined"},
        {"noant", "x.a.b", "Property: @1:1 null value property '.a'"},
        {"strict", "x.a.b", "Variable: @1:1 variable 'x.a.b' is undefined"},
        {"safe", "x.a.b", "= null"},
        {"ant", "x.y.z.w", "Property: @1:6 undefined property 'z'"},
        {"noant", "x.y.z.w", "Property: @1:1 null value property '.y'"},
        {"strict", "x.y.z.w", "Property: @1:6 undefined property 'z'"},
        {"safe", "x.y.z.w", "Property: @1:6 undefined property 'z'"},
        {"ant", "var x; x.y.z", "Variable: @1:8 variable 'x' is null"},
        {"noant", "var x; x.y.z", "Property: @1:8 null value property '.y'"},
        {"strict", "var x; x.y.z", "Variable: @1:8 variable 'x' is null"},
        {"safe", "var x; x.y.z", "= null"},
        {"ant", "var x; x.y.z = 1", "Property: @1:8 undefined property '<null>.<?>'"},
        {"noant", "var x; x.y.z = 1", "JexlException: @1:8 JEXL error : illegal assignment form"},
        {"strict", "var x; x.y.z = 1", "Property: @1:8 undefined property '<null>.<?>'"},
        {"safe", "var x; x.y.z = 1", "Property: @1:8 undefined property '<null>.<?>'"},
        {"ant", "m.y = 3; m.y", "= 3"},
        {"noant", "m.y = 3; m.y", "= 3"},
        {"strict", "m.y = 3; m.y", "= 3"},
        {"safe", "m.y = 3; m.y", "= 3"},
        {"ant", "m.y.z = 3", "JexlException: @1:4 JEXL error : illegal assignment form"},
        {"noant", "m.y.z = 3", "JexlException: @1:4 JEXL error : illegal assignment form"},
        {"strict", "m.y.z = 3", "JexlException: @1:4 JEXL error : illegal assignment form"},
        {"safe", "m.y.z = 3", "JexlException: @1:4 JEXL error : illegal assignment form"},
        {"ant", "m.q.z", "Property: @1:1 undefined property '.q'"},
        {"noant", "m.q.z", "Property: @1:1 undefined property '.q'"},
        {"strict", "m.q.z", "Property: @1:1 undefined property '.q'"},
        {"safe", "m.q.z", "= null"},
        {"ant", "x.y[0]", "= 1"},
        {"noant", "x.y[0]", "Property: @1:1 null value property '.y'"},
        {"strict", "x.y[0]", "= 1"},
        {"safe", "x.y[0]", "= 1"},
        {"ant", "x.y[0] = 1", "= 1"},
        {"noant", "x.y[0] = 1", "JexlException: @1:1 JEXL error : illegal assignment form"},
        {"strict", "x.y[0] = 1", "= 1"},
        {"safe", "x.y[0] = 1", "= 1"},
        {"ant", "(x.y).z", "Property: @1:7 undefined property 'z'"},
        {"noant", "(x.y).z", "Property: @1:2 null value property '.y'"},
        {"strict", "(x.y).z", "Property: @1:7 undefined property 'z'"},
        {"safe", "(x.y).z", "Property: @1:7 undefined property 'z'"},
        {"ant", "x.y.z.size()", "Property: @1:6 undefined property 'z'"},
        {"noant", "x.y.z.size()", "Property: @1:1 null value property '.y'"},
        {"strict", "x.y.z.size()", "Property: @1:6 undefined property 'z'"},
        {"safe", "x.y.z.size()", "Property: @1:6 undefined property 'z'"},
        {"ant", "a.b.c?.d", "= null"},
        {"noant", "a.b.c?.d", "Property: @1:1 null value property '.b'"},
        {"strict", "a.b.c?.d", "= null"},
        {"safe", "a.b.c?.d", "= null"},
        {"ant", "a.b?.c.d", "Variable: @1:1 variable 'a.b' is undefined"},
        {"noant", "a.b?.c.d", "= null"},
        {"strict", "a.b?.c.d", "Variable: @1:1 variable 'a.b' is undefined"},
        {"safe", "a.b?.c.d", "= null"},
        {"ant", "nul.y", "Variable: @1:1 variable 'nul.y' is undefined"},
        {"noant", "nul.y", "Property: @1:1 null value property '.y'"},
        {"strict", "nul.y", "Variable: @1:1 variable 'nul.y' is undefined"},
        {"safe", "nul.y", "= null"},
        {"ant", "nul.y.z", "Variable: @1:1 variable 'nul.y.z' is undefined"},
        {"noant", "nul.y.z", "Property: @1:1 null value property '.y'"},
        {"strict", "nul.y.z", "Variable: @1:1 variable 'nul.y.z' is undefined"},
        {"safe", "nul.y.z", "= null"},
        {"ant", "nul.y = 1", "= 1"},
        {"noant", "nul.y = 1", "JexlException: @1:1 JEXL error : illegal assignment form"},
        {"strict", "nul.y = 1", "= 1"},
        {"safe", "nul.y = 1", "= 1"},
    };

    public InterpreterCharacterizationTest() {
        super("InterpreterCharacterizationTest");
    }

    /**
     * Creates a context for a given mode.
     * <p>The context carries antish variables {@code x.y.z}, {@code x.y}, {@code a.b.c}
     * and the globals {@code m} (a map) and {@code nul} (null).</p>
     *
     * @param mode the mode
     * @return the context
     */
    private static JexlEvalContext createContext(final String mode) {
        final JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.set("x.y.z", 40);
        ctxt.set("x.y", new ArrayList<>(Arrays.asList(1, 2)));
        ctxt.set("a.b.c", null);
        ctxt.set("m", new HashMap<String, Object>());
        ctxt.set("nul", null);
        final JexlOptions options = ctxt.getEngineOptions();
        switch (mode) {
            case "nolex": options.setLexical(false); break;
            case "noant": options.setAntish(false); break;
            case "strict": options.setStrict(true); break;
            case "safe": options.setSafe(true); break;
            default: break;
        }
        return ctxt;
    }

    private String run(final String src, final JexlContext ctxt) {
        try {
            final Object o = JEXL.createScript(src).execute(ctxt);
            return o instanceof Throwable ? "= " + o.getClass().getSimpleName() : "= " + o;
        } catch (final JexlException x) {
            final String m = x.getMessage();
            return x.getClass().getSimpleName() + ": " + m.substring(m.indexOf('@'));
        }
    }

    @Test
    void testCharacterization() {
        final List<String> failures = new ArrayList<>();
        for (final String[] c : CASES) {
            final String actual = run(c[1], createContext(c[0]));
            if (!c[2].equals(actual)) {
                failures.add("[" + c[0] + "] " + c[1] + "\n  expected: " + c[2] + "\n  actual:   " + actual);
            }
        }
        assertEquals("", String.join("\n", failures));
    }
}
