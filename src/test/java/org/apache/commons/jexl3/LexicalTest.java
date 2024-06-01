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

import static org.apache.commons.jexl3.JexlTestCase.createEngine;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.jexl3.internal.LexicalScope;
import org.apache.commons.jexl3.internal.Script;
import org.junit.jupiter.api.Test;

/**
 * Test cases for lexical option and feature.
 */
public class LexicalTest {

    public static class DebugContext extends MapContext {
        public DebugContext() {
        }
        public Object debug(final Object arg) {
            return arg;
        }
    }

    public static class OptAnnotationContext extends JexlEvalContext implements JexlContext.AnnotationProcessor {
        @Override
        public Object processAnnotation(final String name, final Object[] args, final Callable<Object> statement) throws Exception {
            // transient side effect for strict
            if ("scale".equals(name)) {
                final JexlOptions options = getEngineOptions();
                final int scale = options.getMathScale();
                final int newScale = (Integer) args[0];
                options.setMathScale(newScale);
                try {
                    return statement.call();
                } finally {
                    options.setMathScale(scale);
                }
            }
            return statement.call();
        }
    }

    /**
     * Context augmented with a tryCatch.
     */
    public static class TestContext extends JexlEvalContext {
        public TestContext() {}
        public TestContext(final Map<String, Object> map) {
            super(map);
        }

        /**
         * Allows calling a script and catching its raised exception.
         * @param tryFn the lambda to call
         * @param catchFn the lambda catching the exception
         * @param args the arguments to the lambda
         * @return the tryFn result or the catchFn if an exception was raised
         */
        public Object tryCatch(final JexlScript tryFn, final JexlScript catchFn, final Object... args) {
            Object result;
            try {
                result = tryFn.execute(this, args);
            } catch (final Throwable xthrow) {
                result = catchFn != null ? catchFn.execute(this, xthrow) : xthrow;
            }
            return result;
        }
    }

    public static class VarContext extends MapContext implements JexlContext.PragmaProcessor, JexlContext.OptionsHandle {
        private JexlOptions options = new JexlOptions();

        @Override
        public JexlOptions getEngineOptions() {
            return options;
        }

        @Override
        public void processPragma(final String key, final Object value) {
            if ("jexl.options".equals(key) && "canonical".equals(value)) {
                options.setStrict(true);
                options.setLexical(true);
                options.setLexicalShade(true);
                options.setSafe(false);
            }
        }

        JexlOptions snatchOptions() {
            final JexlOptions o = options;
            options = new JexlOptions();
            return o;
        }
    }

    private void checkParse(final JexlFeatures f, final List<String> srcs, final boolean expected) {
        final JexlEngine jexl = new JexlBuilder().features(f).strict(true).create();
        for(final String src : srcs) {
            if (!src.isEmpty()) {
                try {
                    final JexlScript script = jexl.createScript(src);
                    if (!expected) {
                        fail(src);
                    }
                } catch (final JexlException.Parsing xlexical) {
                    if (expected) {
                        fail(src);
                    }
                    //assertTrue(xlexical.detailedMessage().contains("x"));
                }
            }
        }

    }

    private void checkParse(final List<String> srcs, final boolean expected) {
        checkParse(null, srcs, expected);
    }

    void runLexical0(final boolean feature) {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(feature);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setLexical(true);
        JexlScript script;
        try {
            script = jexl.createScript("var x = 0; var x = 1;");
            if (!feature) {
                script.execute(ctxt);
            }
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x = 0; for(var y : null) { var y = 1;");
            if (!feature) {
                script.execute(ctxt);
            }
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x = 0; for(var x : null) {};");
            if (!feature) {
                script.execute(ctxt);
            }
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("(x)->{ var x = 0; x; }");
            if (!feature) {
                script.execute(ctxt);
            }
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x; if (true) { if (true) { var x = 0; x; } }");
            if (!feature) {
                script.execute(ctxt);
            }
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("if (a) { var y = (x)->{ var x = 0; x; }; y(2) }", "a");
            if (!feature) {
                script.execute(ctxt);
            }
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("(x)->{ for(var x : null) { x; } }");
            if (!feature) {
                script.execute(ctxt, 42);
            }
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        // no fail
        script = jexl.createScript("var x = 32; (()->{ for(var x : null) { x; }})();");
        if (!feature) {
            script.execute(ctxt, 42);
        }
    }

    void runLexical1(final boolean shade) {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlEvalContext ctxt = new JexlEvalContext();
        Object result;
        ctxt.set("x", 4242);
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setLexical(true);
        options.setLexicalShade(shade);
        JexlScript script;
        try {
            // if local shade, x is undefined
            script = jexl.createScript("{ var x = 0; } x");
            script.execute(ctxt);
            if (shade) {
                fail("local shade means 'x' should be undefined");
            }
        } catch (final JexlException xany) {
            if (!shade) {
                throw xany;
            }
        }
        try {
            // if local shade, x = 42 is undefined
            script = jexl.createScript("{ var x = 0; } x = 42");
            script.execute(ctxt);
            if (shade) {
                fail("local shade means 'x = 42' should be undefined");
            }
        } catch (final JexlException xany) {
            if (!shade) {
                throw xany;
            }
        }
        try {
            // if local shade, x = 42 is undefined
            script = jexl.createScript("{ var x = 0; } y = 42");
            script.execute(ctxt);
            if (shade) {
                fail("local shade means 'y = 42' should be undefined (y is undefined)");
            }
        } catch (final JexlException xany) {
            if (!shade) {
                throw xany;
            }
        }
        // no fail
        script = jexl.createScript("var x = 32; (()->{ for(var x : null) { x; }})();");
        //if (!feature) {
            script.execute(ctxt, 42);
        //}
        // y being defined as global
        ctxt.set("y", 4242);
        try {
            // if no shade and global y being defined,
            script = jexl.createScript("{ var y = 0; } y = 42");
            result = script.execute(ctxt);
            if (!shade) {
                assertEquals(42, result);
            } else {
                fail("local shade means 'y = 42' should be undefined");
            }
        } catch (final JexlException xany) {
            if (!shade) {
                throw xany;
            }
        }
    }

    protected void runLexical2(final boolean lexical) {
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(lexical).create();
        final JexlContext ctxt = new MapContext();
        final JexlScript script = jexl.createScript("{var x = 42}; {var x; return x; }");
        final Object result = script.execute(ctxt);
        if (lexical) {
            assertNull(result);
        } else {
            assertEquals(42, result);
        }
    }

    void runTestScope(final LexicalScope scope, final int init, final int count, final int step) {
        final int size = (count - init) / step;
        for(int i = init; i < count; i += step) {
            assertTrue(scope.addSymbol(i));
            if (i % (step + 1) == 1) {
                assertTrue(scope.addConstant(i));
            }
            assertFalse(scope.addSymbol(i));
        }
        for(int i = init; i < count; i += step) {
            assertTrue(scope.hasSymbol(i));
            for(int s = 1; s < step; ++s) {
                assertFalse(scope.hasSymbol(i + s));
            }
            if (i % (step + 1) == 1) {
                assertTrue(scope.isConstant(i));
            }
        }
        assertEquals(size, scope.getSymbolCount());
        final BitSet collect = new BitSet();
        scope.clearSymbols(b -> collect.set(b));
        for(int i = init; i < count; i += step) {
            assertTrue(collect.get(i), "missing " + i);
        }
        assertEquals(0, scope.getSymbolCount());
    }

    private JexlFeatures runVarLoop(final boolean flag, final String src) {
        final VarContext vars = new VarContext();
        final JexlOptions options = vars.getEngineOptions();
        options.setLexical(true);
        options.setLexicalShade(true);
        options.setSafe(false);
        final JexlFeatures features = new JexlFeatures();
        if (flag) {
            features.lexical(true).lexicalShade(true);
        }
        final JexlEngine jexl = new JexlBuilder().features(features).create();
        final JexlScript script = jexl.createScript(src);
        final List<Integer> out = new ArrayList<>(10);
        vars.set("$out", out);
        final Object result = script.execute(vars);
        assertEquals(true, result);
        assertEquals(10, out.size());
        return features;
    }

    @Test
    public void testAnnotation() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript("@scale(13) @test var i = 42");
        final JexlContext jc = new OptAnnotationContext();
        final Object result = script.execute(jc);
        assertEquals(42, result);
    }

    @Test
    public void testCaptured0() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript(
                "var x = 10; (b->{ x + b })(32)");
        final JexlContext jc = new MapContext();
        final Object result = script.execute(jc);
        assertEquals(42, result);
    }

    @Test
    public void testCaptured1() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript(
                "{ var x = 10; } (b->{ x + b })(32)");
        final JexlContext jc = new MapContext();
        jc.set("x", 11);
        final Object result = script.execute(jc);
        assertEquals(43, result);
    }

    @Test
    public void testConst0a() {
        final JexlFeatures f = new JexlFeatures();
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlScript script = jexl.createScript(
                "{ const x = 10; x + 1 }; { let x = 20; x = 22}");
        final JexlContext jc = new MapContext();
        final Object result = script.execute(jc);
        assertEquals(22, result);
    }

    @Test
    public void testConst0b() {
        final JexlFeatures f = new JexlFeatures();
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlScript script = jexl.createScript(
                "{ const x = 10; }{ const x = 20; }");
        final JexlContext jc = new MapContext();
        final Object result = script.execute(jc);
        assertEquals(20, result);
    }

    @Test
    public void testConst1() {
        final JexlFeatures f = new JexlFeatures();
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript("const foo;  foo"),
                "should fail, const foo must be followed by assign.");
        assertTrue(xparse.getMessage().contains("const"));

    }

    @Test
    public void testConst2a() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        for (final String op : Arrays.asList("=", "+=", "-=", "/=", "*=", "%=", "<<=", ">>=", ">>>=", "^=", "&=", "|=")) {
            final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript("const foo = 42;  foo " + op + " 1;"),
                    "should fail, const precludes assignment");
            assertTrue(xparse.getMessage().contains("foo"));
        }
    }

    @Test
    public void testConst2b() {
        final JexlFeatures f = new JexlFeatures();
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        for (final String op : Arrays.asList("=", "+=", "-=", "/=", "*=", "%=", "<<=", ">>=", ">>>=", "^=", "&=", "|=")) {
            final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript("const foo = 42;  if (true) { foo " + op + " 1; }"),
                    "should fail, const precludes assignment");
            assertTrue(xparse.getMessage().contains("foo"));
        }
    }

    @Test
    public void testConst2c() {
        final JexlFeatures f = new JexlFeatures();
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        for(final String op : Arrays.asList("=", "+=", "-=", "/=", "*=", "%=", "<<=", ">>=", ">>>=", "^=", "&=", "|=")) {
            try {
                final JexlScript script = jexl.createScript("{ const foo = 42; } { let foo  = 0; foo " + op + " 1; }");
                assertNotNull(script);
            } catch (final JexlException.Parsing xparse) {
                fail(xparse.toString());
            }
        }
    }

    @Test public void testConst3a() {
        final JexlEngine jexl = new JexlBuilder().create();
        final List<String> srcs = Arrays.asList(
                "const f = ()->{ var foo = 3; foo = 5; }",
                "const y = '42'; const f = (let y)->{ var foo = 3; foo = 5; }",
                "const foo = '34'; const f = ()->{ var foo = 3; foo = 5; };",
                "const bar = '34'; const f = ()->{ var f = 3; f = 5; };",
                "const bar = '34'; const f = ()->{ var bar = 3; z ->{ bar += z; } };");
        for(final String src: srcs) {
            final JexlScript script = jexl.createScript(src);
            final Object result = script.execute(null);
            assertNotNull(result, src);
        }
    }

    @Test public void testConst3b() {
        final JexlEngine jexl = new JexlBuilder().create();
        final List<String> srcs = Arrays.asList(
                "const f = ()->{ var foo = 3; f = 5; }",
                "const y = '42'; const f = (let z)->{ y += z; }",
                "const foo = '34'; const f = ()->{ foo = 3; };",
                "const bar = '34'; const f = ()->{  bar = 3; z ->{ bar += z; } };",
                "let bar = '34'; const f = ()->{  const bar = 3; z ->{ bar += z; } };");
        for(final String src: srcs) {
            try {
                final JexlScript script = jexl.createScript(src);
                final Object result = script.execute(null);
                fail(src);
            } catch (final JexlException.Assignment xassign) {
                assertNotNull(xassign, src); // debug breakpoint
            }
        }
    }

    @Test
    public void testConstCaptures() {
        final List<String> srcsFalse = Arrays.asList(
                "const x = 0;  x = 1;",
                "const x = 0; x *= 1;",
                "const x = 0; var x = 1;",
                "const x = 0; if (true) { var x = 1;}" ,
                "const x = 0; if (true) { x = 1;}" ,
                "const x = 0; if (true) { var f  = y -> { x = y + 1; x } }" ,
                "const x = 0; if (true) { var f  = y -> { z -> { x = y + 1; x } } }" ,
                "const x = 0; if (true) { if (false) { y -> { x = y + 1; x } } }" ,
                "const x = 0; if (true) { if (false) { y -> { z -> { x = y + 1; x } } }" ,
                ""
        );
        checkParse(srcsFalse, false);
        final List<String> srcsTrue = Arrays.asList(
            "const x = 0; if (true) { var f  = x -> x + 1;}" ,
            "const x = 0; if (true) { var f  = y -> { var x = y + 1; x } }" ,
            "const x = 0; if (true) { var f  = y -> { const x = y + 1; x } }" ,
            "const x = 0; if (true) { var f  = y -> { z -> { let x = y + 1; x } } }" ,
        "");
        checkParse(srcsTrue, true);
    }

    @Test
    public void testContextualOptions0() {
        final JexlFeatures f= new JexlFeatures();
        final JexlEngine jexl = new JexlBuilder().features(f).strict(true).create();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setSharedInstance(false);
        options.setLexical(true);
        options.setLexicalShade(true);
        ctxt.set("options", options);
        final JexlScript script = jexl.createScript("{var x = 42;} options.lexical = false; options.lexicalShade=false; x");
        try {
        final Object result = script.execute(ctxt);
        fail("setting options.lexical should have no effect during execution");
        } catch (final JexlException xf) {
            assertNotNull(xf);
        }
    }

    @Test
    public void testContextualOptions1() {
        final JexlFeatures f = new JexlFeatures();
        final JexlEngine jexl = new JexlBuilder().features(f).strict(true).create();
        final JexlEvalContext ctxt = new TestContext();
        final JexlOptions options = ctxt.getEngineOptions();
        options.setSharedInstance(true);
        options.setLexical(true);
        options.setLexicalShade(true);
        ctxt.set("options", options);
        final JexlScript runner = jexl.createScript(
                "options.lexical = flag; options.lexicalShade = flag;"
              + "tryCatch(test, catcher, 42);",
                "flag", "test", "catcher");
        final JexlScript tested = jexl.createScript("(y)->{ {var x = y;} x }");
        final JexlScript catchFn = jexl.createScript("(xany)-> { xany }");
        Object result;
        // run it once, old 3.1 semantics, lexical/shade = false
        result = runner.execute(ctxt, false, tested, catchFn);
        // result 42
        assertEquals(42, result);
        // run it a second time, new 3.2 semantics, lexical/shade = true
        result = runner.execute(ctxt, true, tested, catchFn);
        // result is exception!
        assertTrue(result instanceof JexlException.Variable);
    }

    @Test
    public void testForVariable0a() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = createEngine(f);
        try {
            final JexlScript script = jexl.createScript("for(let x : 1..3) { let c = 0}; return x");
            fail("Should not have been parsed");
        } catch (final JexlException ex) {
           // OK
        }
    }

    @Test
    public void testForVariable0b() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = createEngine(f);
        try {
            final JexlScript script = jexl.createScript("for(var x : 1..3) { var c = 0}; return x");
            fail("Should not have been parsed");
        } catch (final JexlException ex) {
            // OK
        }
    }

    @Test
    public void testForVariable1a() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = createEngine(f);
        try {
            final JexlScript script = jexl.createScript("for(var x : 1..3) { var c = 0} for(var x : 1..3) { var c = 0}; return x");
            fail("Should not have been parsed");
        } catch (final JexlException ex) {
           // OK
           assertTrue(ex instanceof JexlException);
        }
    }

    @Test
    public void testForVariable1b() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = createEngine(f);
        try {
            final JexlScript script = jexl.createScript("for(let x : 1..3) { let c = 0} for(let x : 1..3) { var c = 0}; return x");
            fail("Should not have been parsed");
        } catch (final JexlException ex) {
            // OK
            assertTrue(ex instanceof JexlException);
        }
    }

    @Test
    public void testInnerAccess0() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript(
                "var x = 32; ("
                        + "()->{ for(var x : null) { var c = 0; {return x; }} })"
                        + "();");
        assertNull(script.execute(null));
    }

    @Test
    public void testInnerAccess1a() {
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        final JexlScript script = jexl.createScript("var x = 32; (()->{ for(var x : null) { var c = 0; {return x; }} })();");
        assertNotNull(script);
    }

    @Test
    public void testInnerAccess1b() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlScript script = jexl.createScript("let x = 32; (()->{ for(let x : null) { let c = 0; { return x; } } } )(); ");
        assertNotNull(script);
        final String dbg = JexlTestCase.toString(script);
        final String src = script.getSourceText();
        assertTrue(JexlTestCase.equalsIgnoreWhiteSpace(src, dbg));
    }

    @Test
    public void testInternalLexicalFeatures() {
        final String str = "42";
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = new JexlBuilder().features(f).create();
        final JexlScript e = jexl.createScript(str);
        final VarContext vars = new VarContext();
        final JexlOptions opts = vars.getEngineOptions();
        // so we can see the effect of features on options
        opts.setSharedInstance(true);
        final Script script = (Script) e;
        final JexlFeatures features = script.getFeatures();
        assertTrue(features.isLexical());
        assertTrue(features.isLexicalShade());
        final Object result = e.execute(vars);
        assertEquals(42, result);
        assertTrue(opts.isLexical());
        assertTrue(opts.isLexicalShade());
    }

    @Test
    public void testLet0() {
        final JexlFeatures f = new JexlFeatures();
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlScript script = jexl.createScript(
                "{ let x = 10; } (b->{ x + b })(32)");
        final JexlContext jc = new MapContext();
        jc.set("x", 11);
        final Object result = script.execute(jc);
        assertEquals(43, result);

    }

    @Test
    public void testLetFail() {
        final List<String> srcs = Arrays.asList(
            "let x = 0; var x = 1;",
            "var x = 0; let x = 1;",
            "let x = 0; let x = 1;",
            "var x = 0; const f = (var x) -> { let x = 1; } f()",
            "var x = 0; const f = (let x) -> { let x = 1; } f()",
            "var x = 0; const f = (let x) -> { var x = 1; } f()",
            ""
        );
        checkParse(srcs, false);
    }

    @Test
    public void testLetSucceed() {
        final List<String> srcs = Arrays.asList(
            "var x = 1; var x = 0;",
            "{ let x = 0; } var x = 1;",
            "var x = 0; var f = () -> { let x = 1; } f()",
            //"let x = 0; function f() { let x = 1; }; f()" ,
            "var x = 0; var f = (let x) -> { x = 1; } f()",
            "var x = 0; let f = (let x) -> { x = 1; } f()",
            "var x = 0; const f = (let x) -> { x = 1; } f()",
            ""
        );
        checkParse(srcs, true);
    }

    @Test
    public void testLexical0a() {
        runLexical0(false);
    }

    @Test
    public void testLexical0b() {
        runLexical0(true);
    }

    @Test
    public void testLexical1() {
        final JexlEngine jexl = new JexlBuilder().strict(true).create();
        final JexlEvalContext ctxt = new JexlEvalContext();
        final JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setLexical(true);
        JexlScript script;
        Object result;

        script = jexl.createScript("var x = 0; for(var y : [1]) { var x = 42; return x; };");
        try {
        result = script.execute(ctxt);
        //assertEquals(42, result);
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }

        try {
            script = jexl.createScript("(x)->{ if (x) { var x = 7 * (x + x); x; } }");
            result = script.execute(ctxt, 3);
            fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }

        script = jexl.createScript("{ var x = 0; } var x = 42; x");
        result = script.execute(ctxt, 21);
        assertEquals(42, result);
    }

    @Test
    public void testLexical1a() {
        runLexical1(false);
    }

    @Test
    public void testLexical1b() {
        runLexical1(true);
    }

    @Test
    public void testLexical2a() {
        runLexical2(true);
    }

    @Test
    public void testLexical2b() {
        runLexical2(false);
    }

    @Test
    public void testLexical3() {
        final String str = "var s = {}; for (var i : [1]) s.add(i); s";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlScript e = jexl.createScript(str);
        final JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        assertTrue(((Set)o).contains(1));

        e = jexl.createScript(str);
        o = e.execute(jc);
        assertTrue(((Set)o).contains(1));
    }

    @Test
    public void testLexical4() {
        final JexlEngine Jexl = new JexlBuilder().silent(false).strict(true).lexical(true).create();
        final JxltEngine Jxlt = Jexl.createJxltEngine();
        final JexlContext ctxt = new MapContext();
        final String rpt
                = "<report>\n"
                + "\n$$var y = 1; var x = 2;"
                + "\n${x + y}"
                + "\n</report>\n";
        final JxltEngine.Template t = Jxlt.createTemplate("$$", new StringReader(rpt));
        final StringWriter strw = new StringWriter();
        t.evaluate(ctxt, strw);
        final String output = strw.toString();
        final String ctl = "<report>\n\n3\n</report>\n";
        assertEquals(ctl, output);
    }

    @Test
    public void testLexical5() {
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        final JexlContext ctxt = new DebugContext();
        JexlScript script;
        Object result;
            script = jexl.createScript("var x = 42; var y = () -> { {var x = debug(-42); }; return x; }; y()");
        try {
            result = script.execute(ctxt);
            assertEquals(42, result);
        } catch (final JexlException xany) {
            final String ww = xany.toString();
            fail(ww);
        }
    }

    @Test
    public void testLexical6a() {
        final String str = "i = 0; { var i = 32; }; i";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        final Object o = e.execute(ctxt);
        assertEquals(0, o);
    }

    @Test
    public void testLexical6a1() {
        final String str = "i = 0; { var i = 32; }; i";
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = createEngine(f);
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        final Object o = e.execute(ctxt);
        assertEquals(0, o);
    }

    @Test
    public void testLexical6b() {
        final String str = "i = 0; { var i = 32; }; i";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(true).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        try {
            final Object o = e.execute(ctxt);
            fail("i should be shaded");
        } catch (final JexlException xany) {
            assertNotNull(xany);
        }
    }

    @Test
    public void testLexical6c() {
        final String str = "i = 0; for (var i : [42]) i; i";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(false).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        final Object o = e.execute(ctxt);
        assertEquals(0, o);
    }

    @Test
    public void testLexical6d() {
        final String str = "i = 0; for (var i : [42]) i; i";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(true).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        try {
            final Object o = e.execute(ctxt);
            fail("i should be shaded");
        } catch (final JexlException xany) {
            assertNotNull(xany);
        }
    }

    @Test public void testManyConst() {
        final String text = "const x = 1, y = 41; x + y";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(text);
        final Object result = script.execute(null);
        assertEquals(42, result);
        final String s0 = script.getParsedText();
        final String s1 = script.getSourceText();
        assertNotEquals(s0, s1);
    }

    @Test public void testManyLet() {
        final String text = "let x = 1, y = 41, z; x + y";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(text);
        final Object result = script.execute(null);
        assertEquals(42, result);
        final String s0 = script.getParsedText();
        final String s1 = script.getSourceText();
        assertNotEquals(s0, s1);
    }

    @Test
    public void testNamed() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript("var i = (x, y, z)->{return x + y + z}; i(22,18,2)");
        final JexlContext jc = new MapContext();
        final Object result = script.execute(jc);
        assertEquals(42, result);
    }

    @Test
    public void testOptionsPragma() {
        try {
            JexlOptions.setDefaultFlags("+safe", "-lexical", "-lexicalShade");
            final VarContext vars = new VarContext();
            final JexlEngine jexl = new JexlBuilder().create();
            int n42;
            JexlOptions o;

            n42 = (Integer) jexl.createScript("#pragma jexl.options none\n-42").execute(vars);
            assertEquals(-42, n42);
            o = vars.snatchOptions();
            assertNotNull(o);
            assertTrue(o.isStrict());
            assertTrue(o.isSafe());
            assertTrue(o.isCancellable());
            assertFalse(o.isLexical());
            assertFalse(o.isLexicalShade());

            n42 = (Integer) jexl.createScript("#pragma jexl.options canonical\n42").execute(vars);
            assertEquals(42, n42);
            o = vars.snatchOptions();
            assertNotNull(o);
            assertTrue(o.isStrict());
            assertFalse(o.isSafe());
            assertTrue(o.isCancellable());
            assertTrue(o.isLexical());
            assertTrue(o.isLexicalShade());
            assertFalse(o.isSharedInstance());
        } finally {
            JexlOptions.setDefaultFlags("-safe", "+lexical");
        }
    }

    @Test
    public void testParameter0() {
        final String str = "function(u) {}";
        final JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript(str);
        assertEquals(1, e.getParameters().length);
        e = jexl.createScript(new JexlInfo("TestScript", 1, 1), str);
        assertEquals(1, e.getParameters().length);
    }

    @Test
    public void testParameter1() {
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        final JexlContext jc = new MapContext();
        final String strs = "var s = function(x) { for (var i : 1..3) {if (i > 2) return x}}; s(42)";
        final JexlScript s42 = jexl.createScript(strs);
        final Object result = s42.execute(jc);
        assertEquals(42, result);
    }

    @Test
    public void testPragmaNoop() {
        // unknow pragma
        final String str = "#pragma jexl.options 'no effect'\ni = -42; for (var i : [42]) i; i";
        final JexlEngine jexl = new JexlBuilder().lexical(false).strict(true).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        final Object result = e.execute(ctxt);
        assertEquals(42, result);
    }

    @Test
    public void testPragmaOptions() {
        // same as 6d but using a pragma
        final String str = "#pragma jexl.options '+strict +lexical +lexicalShade -safe'\n"
                + "i = 0; for (var i : [42]) i; i";
        final JexlEngine jexl = new JexlBuilder().strict(false).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        try {
            final Object o = e.execute(ctxt);
            fail("i should be shaded");
        } catch (final JexlException xany) {
            assertNotNull(xany);
        }
    }

    @Test
    public void testScopeFrame() {
        final LexicalScope scope = new LexicalScope();
        runTestScope(scope, 0, 128, 2);
        runTestScope(scope, 33, 55, 1);
        runTestScope(scope, 15, 99, 3);
        runTestScope(scope, 3, 123, 5);
    }

    @Test
    public void testSingleStatementDeclFail() {
        final List<String> srcs = Arrays.asList(
                "if (true) let x ;",
                "if (true) let x = 1;",
                "if (true) var x = 1;",
                "if (true) { 1 } else let x ;",
                "if (true) { 1 } else let x = 1;",
                "if (true) { 1 } else var x = 1;",
                "while (true) let x ;",
                "while (true) let x = 1;",
                "while (true) var x = 1;",
                "do let x ; while (true)",
                "do let x = 1; while (true)",
                "do var x = 1; while (true)",
                "for (let i:ii) let x ;",
                "for (let i:ii) let x = 1;",
                "for (let i:ii) var x = 1;",
                ""
        );
        final JexlFeatures f=  new JexlFeatures();
        f.lexical(true).lexicalShade(true);
        checkParse(f, srcs, false);
    }

    @Test
    public void testSingleStatementVarSucceed() {
        final List<String> srcs = Arrays.asList(
                "if (true) var x = 1;",
                "if (true) { 1 } else var x = 1;",
                "while (true) var x = 1;",
                "do var x = 1 while (true)",
                "for (let i:ii) var x = 1;",
                ""
        );
        checkParse(srcs, true);
    }

    @Test
    public void testUndeclaredVariable() {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = createEngine(f);
        try {
            final JexlScript script = jexl.createScript("{var x = 0}; return x");
            fail("Should not have been parsed");
        } catch (final Exception ex) {
           // OK
           assertTrue(ex instanceof JexlException);
        }
    }

    @Test
    public void testVarFail() {
        final List<String> srcs = Arrays.asList(
                "var x = 0; var x = 1;",
                "var x = 0; let x = 1;",
                "let x = 0; var x = 1;",
                "var x = 0; const f = (var x) -> { let x = 1; } f()",
                "var x = 0; const f = (let x) -> { var x = 1; } f()",
                "var x = 0; const f = (var x) -> { var x = 1; } f()",
                ""
        );
        final JexlFeatures f=  new JexlFeatures();
        f.lexical(true).lexicalShade(true);
        checkParse(f, srcs, false);
    }

    @Test
    public void testVarLoop0() {
        final String src0 = "var count = 10;\n"
                + "for (var i : 0 .. count-1) {\n"
                + "  $out.add(i);\n"
                + "}";
        final String src1 = "var count = [0,1,2,3,4,5,6,7,8,9];\n"
                + "for (var i : count) {\n"
                + "  $out.add(i);\n"
                + "}";
        final String src2 = "var count = 10;\n" +
                "  var outer = 0;\n"
                + "for (var i : 0 .. count-1) {\n"
                + "  $out.add(i);\n"
                + "  outer = i;"
                + "}\n"
                + "outer == 9";
        final JexlFeatures ff0 = runVarLoop(false, src0);
        final JexlFeatures ft0= runVarLoop(true, src0);
        final JexlFeatures ff1 = runVarLoop(false, src1);
        final JexlFeatures ft1= runVarLoop(true, src1);
        final JexlFeatures ff2 = runVarLoop(false, src2);
        final JexlFeatures ft2= runVarLoop(true, src2);

        // and check some features
        assertEquals(ff0, ff1);
        assertEquals(ft0, ft1);
        assertNotEquals(ff0, ft0);
        final String sff0 = ff0.toString();
        final String sff1 = ff1.toString();
        assertEquals(sff0, sff1);
        final String sft1 = ft1.toString();
        assertNotEquals(sff0, sft1);
    }
}
