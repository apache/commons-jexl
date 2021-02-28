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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.commons.jexl3.internal.LexicalScope;
import org.apache.commons.jexl3.internal.Script;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for lexical option and feature.
 */
public class LexicalTest {

    @Test
    public void testLexical0a() throws Exception {
        runLexical0(false);
    }

    @Test
    public void testLexical0b() throws Exception {
        runLexical0(true);
    }

    void runLexical0(final boolean feature) throws Exception {
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
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x = 0; for(var y : null) { var y = 1;");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x = 0; for(var x : null) {};");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("(x)->{ var x = 0; x; }");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x; if (true) { if (true) { var x = 0; x; } }");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("if (a) { var y = (x)->{ var x = 0; x; }; y(2) }", "a");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        try {
            script = jexl.createScript("(x)->{ for(var x : null) { x; } }");
            if (!feature) {
                script.execute(ctxt, 42);
            }
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }
        // no fail
        script = jexl.createScript("var x = 32; (()->{ for(var x : null) { x; }})();");
        if (!feature) {
            script.execute(ctxt, 42);
        }
    }

    @Test
    public void testLexical1a() throws Exception {
        runLexical1(false);
    }

    @Test
    public void testLexical1b() throws Exception {
        runLexical1(true);
    }

    void runLexical1(final boolean shade) throws Exception {
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
                Assert.fail("local shade means 'x' should be undefined");
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
                Assert.fail("local shade means 'x = 42' should be undefined");
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
                Assert.fail("local shade means 'y = 42' should be undefined (y is undefined)");
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
                Assert.assertEquals(42, result);
            } else {
                Assert.fail("local shade means 'y = 42' should be undefined");
            }
        } catch (final JexlException xany) {
            if (!shade) {
                throw xany;
            }
        }
    }

    @Test
    public void testLexical1() throws Exception {
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
        //Assert.assertEquals(42, result);
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }

        try {
            script = jexl.createScript("(x)->{ if (x) { var x = 7 * (x + x); x; } }");
            result = script.execute(ctxt, 3);
            Assert.fail();
        } catch (final JexlException xany) {
            final String ww = xany.toString();
        }

        script = jexl.createScript("{ var x = 0; } var x = 42; x");
        result = script.execute(ctxt, 21);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testLexical2a() throws Exception {
        runLexical2(true);
    }

    @Test
    public void testLexical2b() throws Exception {
        runLexical2(false);
    }

    protected void runLexical2(final boolean lexical) throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(lexical).create();
        final JexlContext ctxt = new MapContext();
        final JexlScript script = jexl.createScript("{var x = 42}; {var x; return x; }");
        final Object result = script.execute(ctxt);
        if (lexical) {
            Assert.assertNull(result);
        } else {
            Assert.assertEquals(42, result);
        }
    }

    @Test
    public void testLexical3() throws Exception {
        final String str = "var s = {}; for (var i : [1]) s.add(i); s";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlScript e = jexl.createScript(str);
        final JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue(((Set)o).contains(1));

        e = jexl.createScript(str);
        o = e.execute(jc);
        Assert.assertTrue(((Set)o).contains(1));
    }

    @Test
    public void testLexical4() throws Exception {
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
        Assert.assertEquals(ctl, output);
    }

    public static class DebugContext extends MapContext {
        public DebugContext() {
        }
        public Object debug(final Object arg) {
            return arg;
        }
    }

    @Test
    public void testLexical5() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        final JexlContext ctxt = new DebugContext();
        JexlScript script;
        Object result;
            script = jexl.createScript("var x = 42; var y = () -> { {var x = debug(-42); }; return x; }; y()");
        try {
            result = script.execute(ctxt);
            Assert.assertEquals(42, result);
        } catch (final JexlException xany) {
            final String ww = xany.toString();
            Assert.fail(ww);
        }
    }

    @Test
    public void testLexical6a() throws Exception {
        final String str = "i = 0; { var i = 32; }; i";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        final Object o = e.execute(ctxt);
        Assert.assertEquals(0, o);
    }

    @Test
    public void testLexical6b() throws Exception {
        final String str = "i = 0; { var i = 32; }; i";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(true).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        try {
            final Object o = e.execute(ctxt);
            Assert.fail("i should be shaded");
        } catch (final JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    @Test
    public void testLexical6c() throws Exception {
        final String str = "i = 0; for (var i : [42]) i; i";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(false).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        final Object o = e.execute(ctxt);
        Assert.assertEquals(0, o);
    }

    @Test
    public void testLexical6d() throws Exception {
        final String str = "i = 0; for (var i : [42]) i; i";
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(true).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        try {
            final Object o = e.execute(ctxt);
            Assert.fail("i should be shaded");
        } catch (final JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    @Test
    public void testPragmaOptions() throws Exception {
        // same as 6d but using a pragma
        final String str = "#pragma jexl.options '+strict +lexical +lexicalShade -safe'\n"
                + "i = 0; for (var i : [42]) i; i";
        final JexlEngine jexl = new JexlBuilder().strict(false).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        try {
            final Object o = e.execute(ctxt);
            Assert.fail("i should be shaded");
        } catch (final JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    @Test
    public void testPragmaNoop() throws Exception {
        // unknow pragma
        final String str = "#pragma jexl.options 'no effect'\ni = -42; for (var i : [42]) i; i";
        final JexlEngine jexl = new JexlBuilder().lexical(false).strict(true).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        final Object result = e.execute(ctxt);
        Assert.assertEquals(42, result);
    }


    @Test
    public void testScopeFrame() throws Exception {
        final LexicalScope scope = new LexicalScope();
        for(int i = 0; i < 128; i += 2) {
            Assert.assertTrue(scope.addSymbol(i));
            Assert.assertFalse(scope.addSymbol(i));
        }
        for(int i = 0; i < 128; i += 2) {
            Assert.assertTrue(scope.hasSymbol(i));
            Assert.assertFalse(scope.hasSymbol(i + 1));
        }
    }

    @Test
    public void testContextualOptions0() throws Exception {
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
        Assert.fail("setting options.lexical should have no effect during execution");
        } catch(final JexlException xf) {
            Assert.assertNotNull(xf);
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

    @Test
    public void testContextualOptions1() throws Exception {
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
              + "tryCatch(test, catch, 42);",
                "flag", "test", "catch");
        final JexlScript tested = jexl.createScript("(y)->{ {var x = y;} x }");
        final JexlScript catchFn = jexl.createScript("(xany)-> { xany }");
        Object result;
        // run it once, old 3.1 semantics, lexical/shade = false
        result = runner.execute(ctxt, false, tested, catchFn);
        // result 42
        Assert.assertEquals(42, result);
        // run it a second time, new 3.2 semantics, lexical/shade = true
        result = runner.execute(ctxt, true, tested, catchFn);
        // result is exception!
        Assert.assertTrue(result instanceof JexlException.Variable);
    }

    @Test
    public void testParameter0() throws Exception {
        final String str = "function(u) {}";
        final JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript(str);
        Assert.assertEquals(1, e.getParameters().length);
        e = jexl.createScript(new JexlInfo("TestScript", 1, 1), str);
        Assert.assertEquals(1, e.getParameters().length);
    }

    @Test
    public void testParameter1() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        final JexlContext jc = new MapContext();
        final String strs = "var s = function(x) { for (var i : 1..3) {if (i > 2) return x}}; s(42)";
        final JexlScript s42 = jexl.createScript(strs);
        final Object result = s42.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testInnerAccess0() throws Exception {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript(
                "var x = 32; ("
                        + "()->{ for(var x : null) { var c = 0; {return x; }} })"
                        + "();");
        Assert.assertNull(script.execute(null));
    }

    @Test
    public void testInnerAccess1() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        final JexlScript script = jexl.createScript("var x = 32; (()->{ for(var x : null) { var c = 0; {return x; }} })();");
    }

    @Test
    public void testForVariable0() throws Exception {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        try {
            final JexlScript script = jexl.createScript("for(var x : 1..3) { var c = 0}; return x");
            Assert.fail("Should not have been parsed");
        } catch (final JexlException ex) {
           // OK
        }
    }

    @Test
    public void testForVariable1() throws Exception {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        try {
            final JexlScript script = jexl.createScript("for(var x : 1..3) { var c = 0} for(var x : 1..3) { var c = 0}; return x");
            Assert.fail("Should not have been parsed");
        } catch (final JexlException ex) {
           // OK
           Assert.assertTrue(ex instanceof JexlException);
        }
    }

    @Test
    public void testUndeclaredVariable() throws Exception {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        try {
            final JexlScript script = jexl.createScript("{var x = 0}; return x");
            Assert.fail("Should not have been parsed");
        } catch (final Exception ex) {
           // OK
           Assert.assertTrue(ex instanceof JexlException);
        }
    }

    @Test
    public void testLexical6a1() throws Exception {
        final String str = "i = 0; { var i = 32; }; i";
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript e = jexl.createScript(str);
        final JexlContext ctxt = new MapContext();
        final Object o = e.execute(ctxt);
        Assert.assertEquals(0, o);
    }

    public static class VarContext extends MapContext implements JexlContext.PragmaProcessor, JexlContext.OptionsHandle {
        private JexlOptions options = new JexlOptions();

        JexlOptions snatchOptions() {
            final JexlOptions o = options;
            options = new JexlOptions();
            return o;
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

        @Override
        public JexlOptions getEngineOptions() {
            return options;
        }
    }

    @Test
    public void testInternalLexicalFeatures() throws Exception {
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
        Assert.assertTrue(features.isLexical());
        Assert.assertTrue(features.isLexicalShade());
        final Object result = e.execute(vars);
        Assert.assertEquals(42, result);
        Assert.assertTrue(opts.isLexical());
        Assert.assertTrue(opts.isLexicalShade());
    }

    @Test
    public void testOptionsPragma() throws Exception {
        try {
            JexlOptions.setDefaultFlags("+safe", "-lexical", "-lexicalShade");
            final VarContext vars = new VarContext();
            final JexlEngine jexl = new JexlBuilder().create();
            int n42;
            JexlOptions o;

            n42 = (Integer) jexl.createScript("#pragma jexl.options none\n-42").execute(vars);
            Assert.assertEquals(-42, n42);
            o = vars.snatchOptions();
            Assert.assertNotNull(o);
            Assert.assertTrue(o.isStrict());
            Assert.assertTrue(o.isSafe());
            Assert.assertTrue(o.isCancellable());
            Assert.assertFalse(o.isLexical());
            Assert.assertFalse(o.isLexicalShade());

            n42 = (Integer) jexl.createScript("#pragma jexl.options canonical\n42").execute(vars);
            Assert.assertEquals(42, n42);
            o = vars.snatchOptions();
            Assert.assertNotNull(o);
            Assert.assertTrue(o.isStrict());
            Assert.assertFalse(o.isSafe());
            Assert.assertTrue(o.isCancellable());
            Assert.assertTrue(o.isLexical());
            Assert.assertTrue(o.isLexicalShade());
            Assert.assertFalse(o.isSharedInstance());
        } finally {
            JexlOptions.setDefaultFlags("-safe", "+lexical");
        }
    }

    @Test
    public void testVarLoop0() throws Exception {
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

        // and check some features features
        Assert.assertEquals(ff0, ff1);
        Assert.assertEquals(ft0, ft1);
        Assert.assertNotEquals(ff0, ft0);
        final String sff0 = ff0.toString();
        final String sff1 = ff1.toString();
        Assert.assertEquals(sff0, sff1);
        final String sft1 = ft1.toString();
        Assert.assertNotEquals(sff0, sft1);
    }

    private JexlFeatures runVarLoop(final boolean flag, final String src) throws Exception {
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
        final List<Integer> out = new ArrayList<Integer>(10);
        vars.set("$out", out);
        final Object result = script.execute(vars);
        Assert.assertEquals(true, result);
        Assert.assertEquals(10, out.size());
        return features;
    }

    public static class OptAnnotationContext extends JexlEvalContext implements JexlContext.AnnotationProcessor {
        @Override
        public Object processAnnotation(final String name, final Object[] args, final Callable<Object> statement) throws Exception {
            // transient side effect for strict
            if ("scale".equals(name)) {
                final JexlOptions options = this.getEngineOptions();
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

    @Test
    public void testAnnotation() throws Exception {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript("@scale(13) @test var i = 42");
        final JexlContext jc = new OptAnnotationContext();
        final Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testNamed() throws Exception {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript("var i = (x, y, z)->{return x + y + z}; i(22,18,2)");
        final JexlContext jc = new MapContext();
        final Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test
    public void tesstCaptured0() throws Exception {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript(
                "var x = 10; (b->{ x + b })(32)");
        final JexlContext jc = new MapContext();
        final Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testCaptured1() throws Exception {
        final JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        final JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        final JexlScript script = jexl.createScript(
                "{var x = 10; } (b->{ x + b })(32)");
        final JexlContext jc = new MapContext();
        jc.set("x", 11);
        final Object result = script.execute(jc);
        Assert.assertEquals(43, result);
    }
}
