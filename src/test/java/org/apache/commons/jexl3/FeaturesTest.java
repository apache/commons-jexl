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

import static org.apache.commons.jexl3.JexlFeatures.AMBIGUOUS_STATEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

/**
 * Tests for blocks
 */
@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
public class FeaturesTest extends JexlTestCase {
    private final JexlEngine jexl = new JexlBuilder().create();
    /**
     * Create the test
     */
    public FeaturesTest() {
        super("FeaturesTest");
    }

    private void assertOk(final String[] scripts) {
        for(final String str : scripts) {
            try {
                final JexlScript e = jexl.createScript(str);
            } catch (final JexlException.Feature xfeature) {
                fail(str + " :: should not fail parse: " + xfeature.getMessage());
            }
        }
    }

    /**
     * Checks that the script is valid with all features on then verifies it
     * throws a feature exception with the given set (in features param).
     * @param features the features
     * @param scripts the scripts
     * @throws Exception
     */
    private void checkFeature(final JexlFeatures features, final String[] scripts) {
        for (final String script : scripts) {
            final JexlScript ctl = JEXL.createScript(script);
            assertNotNull(ctl);
            final JexlException.Parsing xfeature = assertThrows(JexlException.Parsing.class, () -> jexl.createScript(features, null, script));
            assertNotNull(xfeature.getMessage());
        }
    }

    @Test
    void test410a() {
        final long x = JexlFeatures.createAll().getFlags();
        assertEquals(AMBIGUOUS_STATEMENT + 1, Long.bitCount(x));
        assertTrue((x & 1L << AMBIGUOUS_STATEMENT) != 0);

        final JexlFeatures all = JexlFeatures.createAll();
        final JexlEngine jexl = new JexlBuilder().features(all).create();
        final JexlScript script = jexl.createScript("#0 * #1", "#0", "#1");
        final Object r = script.execute(null, 6, 7);
        assertEquals(42, r);
    }

    @Test
    void test410b() {
        final JexlFeatures features = JexlFeatures.createScript();
        assertTrue(features.isLexical());
        assertTrue(features.isLexicalShade());
        assertTrue(features.supportsConstCapture());
        //features.pragmaAnywhere(false);
        assertFalse(features.supportsPragmaAnywhere());
        //features.comparatorNames(false);
        assertFalse(features.supportsComparatorNames());

        final JexlEngine jexl = new JexlBuilder().features(features).create();
        final Collection<String> reserved = features.getReservedNames();
        for(final String varName : reserved) {
            final String src = "var " + varName;
            //JexlScript script = jexl.createScript(src);
            assertThrows(JexlException.Feature.class, () -> jexl.createScript(src), src);
        }
        final String[] cmpNameScripts = {
            "1 eq 1",
            "2 ne 3",
            "1 lt 2",
            "3 le 3",
            "4 gt 2",
            "3 ge 2"
        };
        for(final String src : cmpNameScripts) {
            assertThrows(JexlException.Ambiguous.class, () -> jexl.createScript(src), src);
        }
    }

    @Test
    void testAnnotations() { 
        final JexlFeatures f = new JexlFeatures().annotation(false);
        final String[] scripts = {
            "@synchronized(2) { return 42; }",
            "@two var x = 3;"
        };
        checkFeature(f, scripts);
    }

    @Test
    void testArrayRefs() { 
        final JexlFeatures f = new JexlFeatures().arrayReferenceExpr(false);

        final String[] scripts = {
            "x[y]",
            "x['a'][b]",
            "x()['a'][b]",
            "x.y['a'][b]"
        };
        checkFeature(f, scripts);
        assertOk(scripts);
        // same ones with constant array refs should work
        final String[] scriptsOk = {
            "x['y']",
            "x['a'][1]",
            "x()['a']['b']",
            "x.y['a']['b']"
        };
        assertOk(scriptsOk);
    }

    @Test
    void testConstCapture() { 
        final JexlFeatures f = new JexlFeatures().constCapture(true);
        final String[] scripts = {
            "let x = 0; const f = y -> x += y; f(42)",
            "let x = 0; function f(y) { z -> x *= y }; f(42)"
        };
        checkFeature(f, scripts);
        final JexlFeatures nof = new JexlFeatures().constCapture(true);
        assertOk(scripts);
    }

    @Test
    void testCreate() {
        final JexlFeatures f = JexlFeatures.createNone();
        assertTrue(f.supportsExpression());

        assertFalse(f.supportsAnnotation());
        assertFalse(f.supportsArrayReferenceExpr());
        assertFalse(f.supportsComparatorNames());
        assertFalse(f.supportsFatArrow());
        assertFalse(f.supportsImportPragma());
        assertFalse(f.supportsLambda());
        assertFalse(f.supportsLocalVar());
        assertFalse(f.supportsLoops());
        assertFalse(f.supportsMethodCall());
        assertFalse(f.supportsNamespacePragma());
        assertFalse(f.supportsNewInstance());
        assertFalse(f.supportsPragma());
        assertFalse(f.supportsPragmaAnywhere());
        assertFalse(f.supportsScript());
        assertFalse(f.supportsStructuredLiteral());

        assertFalse(f.isLexical());
        assertFalse(f.isLexicalShade());
        assertFalse(f.supportsConstCapture());

        final JexlEngine jnof = new JexlBuilder().features(f).create();
        assertThrows(JexlException.Feature.class, ()->jnof.createScript("{ 3 + 4 }"));
        assertNotNull(jnof.createExpression("3 + 4"));
    }

    @Test
    void testIssue409() {
        final JexlFeatures baseFeatures =  JexlFeatures.createDefault();
        assertFalse(baseFeatures.isLexical());
        assertFalse(baseFeatures.isLexicalShade());
        assertFalse(baseFeatures.supportsConstCapture());

        final JexlFeatures scriptFeatures = JexlFeatures.createScript();
        assertTrue(scriptFeatures.isLexical());
        assertTrue(scriptFeatures.isLexicalShade());
        scriptFeatures.lexical(false);
        assertFalse(scriptFeatures.isLexical());
        assertFalse(scriptFeatures.isLexicalShade());
    }

    @Test
    void testMethodCalls() { 
        final JexlFeatures f = new JexlFeatures().methodCall(false);
        final String[] scripts = {
            "x.y(z)",
            "x['a'].m(b)",
            "x()['a'](b)",
            "x.y['a'](b)"
        };
        checkFeature(f, scripts);
        // same ones with constant array refs should work
        final String[] scriptsOk = {
            "x('y')",
            "x('a')[1]",
            "x()['a']['b']",
        };
        assertOk(scriptsOk);
    }

    @Test
    void testMixedFeatures() { 
        // no new, no local, no lambda, no loops, no-side effects
        final JexlFeatures f = new JexlFeatures()
                .newInstance(false)
                .localVar(false)
                .lambda(false)
                .loops(false)
                .sideEffectGlobal(false);
        final String[] scripts = {
            "return new(clazz);",
            "()->{ return 0 };",
            "var x = 0;",
            "(x, y)->{ return 0 };",
            "for(var i : {0 .. 10}) { bar(i); }",
            "x += 1",
            "x.y += 1"
        };
        checkFeature(f, scripts);
    }
    @Test
    void testNoComparatorNames() { 
        final JexlFeatures f = new JexlFeatures().comparatorNames(false);
        final String[] scripts = {
            "1 eq 1",
            "2 ne 3",
            "1 lt 2",
            "3 le 3",
            "4 gt 2",
            "3 ge 2"
        };
        checkFeature(f, scripts);
    }

    @Test
    void testNoLambda() { 
        final JexlFeatures f = new JexlFeatures().lambda(false);
        final String[] scripts = {
            "var x  = ()->{ return 0 };",
            "()->{ return 0 };",
            "(x, y)->{ return 0 };",
            "function() { return 0 };",
            "function(x, y) { return 0 };",
            "if (false) { (function(x, y) { return x + y })(3, 4) }"
        };
        checkFeature(f, scripts);
    }

    @Test
    void testNoLocals() { 
        final JexlFeatures f = new JexlFeatures().localVar(false);
        final String[] scripts = {
            "var x = 0;",
            "(x)->{ x }"
        };
        checkFeature(f, scripts);
    }

    @Test
    void testNoLoop() { 
        final JexlFeatures f = new JexlFeatures().loops(false);
        final String[] scripts = {
            "while(true);",
            "for(var i : {0 .. 10}) { bar(i); }"
        };
        checkFeature(f, scripts);
    }
    @Test
    void testNoNew() { 
        final JexlFeatures f = new JexlFeatures().newInstance(false);
        final String[] scripts = {
            "return new(clazz);",
            "new('java.math.BigDecimal', 12) + 1"
        };
        checkFeature(f, scripts);
    }

    @Test
    void testNoScript() { 
        final JexlFeatures f = new JexlFeatures().script(false);
        assertTrue(f.supportsExpression());
        final String[] scripts = {
            "if (false) { block(); }",
            "{ noway(); }",
            "while(true);",
            "for(var i : {0 .. 10}) { bar(i); }"
        };
        checkFeature(f, scripts);
    }

    @Test
    void testNoSideEffects() { 
        final JexlFeatures f = new JexlFeatures().sideEffect(false);
        final String[] scripts = {
            "x = 1",
            "x.y = 1",
            "x().y = 1",
            "x += 1",
            "x.y += 1",
            "x().y += 1",
            "x -= 1",
            "x *= 1",
            "x /= 1",
            "x %= 1",
            "x ^= 1",
            "x &= 1",
            "x |= 1",
            "x >>= 1",
            "x <<= 1",
            "x >>>= 1",
        };
        checkFeature(f, scripts);
    }

    @Test
    void testNoSideEffectsGlobal() { 
        final JexlFeatures f = new JexlFeatures().sideEffectGlobal(false);
        final String[] scripts = {
            "x = 1",
            "x.y = 1",
            "x().y = 1",
            "x += 1",
            "x.y += 1",
            "x().y += 1",
            "x -= 1",
            "x *= 1",
            "x /= 1",
            "x ^= 1",
            "x &= 1",
            "x |= 1",
            "4 + (x.y = 1)",
            "if (true) x.y.z = 4"
        };
        // these should all fail with x undeclared as local, thus x as global
        checkFeature(f, scripts);
        // same ones with x as local should work
        for(final String str : scripts) {
            try {
                final JexlScript e = jexl.createScript("var x = foo(); " + str);
            } catch (final JexlException.Feature xfeature) {
                fail(str + " :: should not fail parse: " + xfeature.getMessage());
            }
        }
    }

    @Test
    void testPragma() { 
        final JexlFeatures f = new JexlFeatures().pragma(false);
        final String[] scripts = {
            "#pragma foo 42",
            "#pragma foo 'bar'\n@two var x = 3;"
        };
        checkFeature(f, scripts);
    }

    @Test
    void testPragmaAnywhere() { 
        final JexlFeatures f = new JexlFeatures().pragmaAnywhere(false);
        final String[] scripts = {
                "var x = 3;\n#pragma foo 42",
        };
        checkFeature(f, scripts);
    }

    @Test
    void testReservedVars() { 
        final JexlFeatures f = new JexlFeatures().reservedNames(Arrays.asList("foo", "bar"));
        final String[] scripts = {
            "var foo = 0;",
            "(bar)->{ bar }",
            "var f = function(bar) { bar; }"
        };
        checkFeature(f, scripts);
        final String[] scriptsOk = {
            "var foo0 = 0;",
            "(bar1)->{ bar }",
            "var f = function(bar2) { bar2; }"
        };
        assertOk(scriptsOk);
    }

    @Test
    void testStructuredLiterals() { 
        final JexlFeatures f = new JexlFeatures().structuredLiteral(false);
        final String[] scripts = {
            "{1, 2, 3}",
            "[1, 2, 3]",
            "{ 1 :'one', 2 : 'two', 3 : 'three' }",
            "(1 .. 5)"
        };
        checkFeature(f, scripts);
        assertOk(scripts);
    }
}
