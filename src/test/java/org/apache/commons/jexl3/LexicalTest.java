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

    void runLexical0(boolean feature) throws Exception {
        JexlFeatures f = new JexlFeatures();
        f.lexical(feature);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        JexlEvalContext ctxt = new JexlEvalContext();
        JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setLexical(true);
        JexlScript script;
        try {
            script = jexl.createScript("var x = 0; var x = 1;");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x = 0; for(var y : null) { var y = 1;");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x = 0; for(var x : null) {};");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
        }
        try {
            script = jexl.createScript("(x)->{ var x = 0; x; }");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
        }
        try {
            script = jexl.createScript("var x; if (true) { if (true) { var x = 0; x; } }");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
        }
        try {
            script = jexl.createScript("if (a) { var y = (x)->{ var x = 0; x; }; y(2) }", "a");
            if (!feature) {
                script.execute(ctxt);
            }
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
        }
        try {
            script = jexl.createScript("(x)->{ for(var x : null) { x; } }");
            if (!feature) {
                script.execute(ctxt, 42);
            }
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
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

    void runLexical1(boolean shade) throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).create();
        JexlEvalContext ctxt = new JexlEvalContext();
        Object result;
        ctxt.set("x", 4242);
        JexlOptions options = ctxt.getEngineOptions();
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
        } catch (JexlException xany) {
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
        } catch (JexlException xany) {
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
        } catch (JexlException xany) {
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
        } catch (JexlException xany) {
            if (!shade) {
                throw xany;
            }
        }
    }

    @Test
    public void testLexical1() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).create();
        JexlEvalContext ctxt = new JexlEvalContext();
        JexlOptions options = ctxt.getEngineOptions();
        // ensure errors will throw
        options.setLexical(true);
        JexlScript script;
        Object result;

        script = jexl.createScript("var x = 0; for(var y : [1]) { var x = 42; return x; };");
        try {
        result = script.execute(ctxt);
        //Assert.assertEquals(42, result);
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
        }

        try {
            script = jexl.createScript("(x)->{ if (x) { var x = 7 * (x + x); x; } }");
            result = script.execute(ctxt, 3);
            Assert.fail();
        } catch (JexlException xany) {
            String ww = xany.toString();
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

    protected void runLexical2(boolean lexical) throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(lexical).create();
        JexlContext ctxt = new MapContext();
        JexlScript script = jexl.createScript("{var x = 42}; {var x; return x; }");
        Object result = script.execute(ctxt);
        if (lexical) {
            Assert.assertNull(result);
        } else {
            Assert.assertEquals(42, result);
        }
    }

    @Test
    public void testLexical3() throws Exception {
        String str = "var s = {}; for (var i : [1]) s.add(i); s";
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlScript e = jexl.createScript(str);
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue(((Set)o).contains(1));

        e = jexl.createScript(str);
        o = e.execute(jc);
        Assert.assertTrue(((Set)o).contains(1));
    }

    @Test
    public void testLexical4() throws Exception {
        JexlEngine Jexl = new JexlBuilder().silent(false).strict(true).lexical(true).create();
        JxltEngine Jxlt = Jexl.createJxltEngine();
        JexlContext ctxt = new MapContext();
        String rpt
                = "<report>\n"
                + "\n$$var y = 1; var x = 2;"
                + "\n${x + y}"
                + "\n</report>\n";
        JxltEngine.Template t = Jxlt.createTemplate("$$", new StringReader(rpt));
        StringWriter strw = new StringWriter();
        t.evaluate(ctxt, strw);
        String output = strw.toString();
        String ctl = "<report>\n\n3\n</report>\n";
        Assert.assertEquals(ctl, output);
    }
    
    public static class DebugContext extends MapContext {
        public DebugContext() {
            super();
        }
        public Object debug(Object arg) {
            return arg;
        }
    }

    @Test
    public void testLexical5() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlContext ctxt = new DebugContext();
        JexlScript script;
        Object result;
            script = jexl.createScript("var x = 42; var y = () -> { {var x = debug(-42); }; return x; }; y()");
        try {
            result = script.execute(ctxt);
            Assert.assertEquals(42, result);
        } catch (JexlException xany) {
            String ww = xany.toString();
            Assert.fail(ww);
        }
    }
        
    @Test
    public void testLexical6a() throws Exception {
        String str = "i = 0; { var i = 32; }; i";
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlScript e = jexl.createScript(str);
        JexlContext ctxt = new MapContext();
        Object o = e.execute(ctxt);
        Assert.assertEquals(0, o);
    }   

    @Test
    public void testLexical6b() throws Exception {
        String str = "i = 0; { var i = 32; }; i";
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(true).create();
        JexlScript e = jexl.createScript(str);
        JexlContext ctxt = new MapContext();
        try {
            Object o = e.execute(ctxt);
            Assert.fail("i should be shaded");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    @Test
    public void testLexical6c() throws Exception {
        String str = "i = 0; for (var i : [42]) i; i";
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(false).create();
        JexlScript e = jexl.createScript(str);
        JexlContext ctxt = new MapContext();
        Object o = e.execute(ctxt);
        Assert.assertEquals(0, o);
    }

    @Test
    public void testLexical6d() throws Exception {
        String str = "i = 0; for (var i : [42]) i; i";
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).lexicalShade(true).create();
        JexlScript e = jexl.createScript(str);
        JexlContext ctxt = new MapContext();
        try {
            Object o = e.execute(ctxt);
            Assert.fail("i should be shaded");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }
    
    @Test
    public void testPragmaOptions() throws Exception {
        // same as 6d but using a pragma
        String str = "#pragma jexl.options '+strict +lexical +lexicalShade -safe'\n"
                + "i = 0; for (var i : [42]) i; i";
        JexlEngine jexl = new JexlBuilder().strict(false).create();
        JexlScript e = jexl.createScript(str);
        JexlContext ctxt = new MapContext();
        try {
            Object o = e.execute(ctxt);
            Assert.fail("i should be shaded");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }
    
    @Test
    public void testPragmaNoop() throws Exception {
        // unknow pragma
        String str = "#pragma jexl.options 'no effect'\ni = -42; for (var i : [42]) i; i";
        JexlEngine jexl = new JexlBuilder().lexical(false).strict(true).create();
        JexlScript e = jexl.createScript(str);
        JexlContext ctxt = new MapContext();
        Object result = e.execute(ctxt);
        Assert.assertEquals(42, result);
    }
    
    
    @Test
    public void testScopeFrame() throws Exception {
        LexicalScope scope = new LexicalScope();
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
        JexlFeatures f= new JexlFeatures();
        JexlEngine jexl = new JexlBuilder().features(f).strict(true).create();
        JexlEvalContext ctxt = new JexlEvalContext();
        JexlOptions options = ctxt.getEngineOptions();
        options.setSharedInstance(false);
        options.setLexical(true);
        options.setLexicalShade(true);
        ctxt.set("options", options);
        JexlScript script = jexl.createScript("{var x = 42;} options.lexical = false; options.lexicalShade=false; x");
        try {
        Object result = script.execute(ctxt);
        Assert.fail("setting options.lexical should have no effect during execution");
        } catch(JexlException xf) {
            Assert.assertNotNull(xf);
        }
    }
    
    /**
     * Context augmented with a tryCatch.
     */
    public static class TestContext extends JexlEvalContext {
        public TestContext() {}
        public TestContext(Map<String, Object> map) {
            super(map);
        }

        /**
         * Allows calling a script and catching its raised exception.
         * @param tryFn the lambda to call
         * @param catchFn the lambda catching the exception
         * @param args the arguments to the lambda
         * @return the tryFn result or the catchFn if an exception was raised
         */
        public Object tryCatch(JexlScript tryFn, JexlScript catchFn, Object... args) {
            Object result;
            try {
                result = tryFn.execute(this, args);
            } catch (Throwable xthrow) {
                result = catchFn != null ? catchFn.execute(this, xthrow) : xthrow;
            }
            return result;
        }
    }

    @Test
    public void testContextualOptions1() throws Exception {
        JexlFeatures f = new JexlFeatures();
        JexlEngine jexl = new JexlBuilder().features(f).strict(true).create();
        JexlEvalContext ctxt = new TestContext();
        JexlOptions options = ctxt.getEngineOptions();
        options.setSharedInstance(true);
        options.setLexical(true);
        options.setLexicalShade(true);
        ctxt.set("options", options);
        JexlScript runner = jexl.createScript(
                "options.lexical = flag; options.lexicalShade = flag;"
              + "tryCatch(test, catch, 42);",
                "flag", "test", "catch");
        JexlScript tested = jexl.createScript("(y)->{ {var x = y;} x }");
        JexlScript catchFn = jexl.createScript("(xany)-> { xany }");
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
        String str = "function(u) {}";
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript(str);
        Assert.assertEquals(1, e.getParameters().length);
        e = jexl.createScript(new JexlInfo("TestScript", 1, 1), str);
        Assert.assertEquals(1, e.getParameters().length);
    } 
    
    @Test
    public void testParameter1() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlContext jc = new MapContext();
        String strs = "var s = function(x) { for (var i : 1..3) {if (i > 2) return x}}; s(42)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(jc);
        Assert.assertEquals(42, result);
    }
        
    @Test
    public void testInnerAccess0() throws Exception {
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        JexlScript script = jexl.createScript(
                "var x = 32; ("
                        + "()->{ for(var x : null) { var c = 0; {return x; }} })"
                        + "();");
        Assert.assertNull(script.execute(null));
    }
    
    @Test
    public void testInnerAccess1() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlScript script = jexl.createScript("var x = 32; (()->{ for(var x : null) { var c = 0; {return x; }} })();");
    }
        
    @Test
    public void testForVariable0() throws Exception {
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        try {
            JexlScript script = jexl.createScript("for(var x : 1..3) { var c = 0}; return x");
            Assert.fail("Should not have been parsed");
        } catch (JexlException ex) {
           // OK
        }
    }
           
    @Test
    public void testForVariable1() throws Exception {
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        try {
            JexlScript script = jexl.createScript("for(var x : 1..3) { var c = 0} for(var x : 1..3) { var c = 0}; return x");
            Assert.fail("Should not have been parsed");
        } catch (JexlException ex) {
           // OK
           Assert.assertTrue(ex instanceof JexlException);
        }
    }
      
    @Test
    public void testUndeclaredVariable() throws Exception {
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        try {
            JexlScript script = jexl.createScript("{var x = 0}; return x");
            Assert.fail("Should not have been parsed");
        } catch (Exception ex) {
           // OK
           Assert.assertTrue(ex instanceof JexlException);
        }
    }
    
    @Test
    public void testLexical6a1() throws Exception {
        String str = "i = 0; { var i = 32; }; i";
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        JexlScript e = jexl.createScript(str);
        JexlContext ctxt = new MapContext();
        Object o = e.execute(ctxt);
        Assert.assertEquals(0, o);    
    }

    public static class VarContext extends MapContext implements JexlContext.PragmaProcessor, JexlContext.OptionsHandle {
        private JexlOptions options = new JexlOptions();

        JexlOptions snatchOptions() {
            JexlOptions o = options;
            options = new JexlOptions();
            return o;
        }

        @Override
        public void processPragma(String key, Object value) {
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
        String str = "42";
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        f.lexicalShade(true);
        JexlEngine jexl = new JexlBuilder().features(f).create();
        JexlScript e = jexl.createScript(str);
        VarContext vars = new VarContext();
        JexlOptions opts = vars.getEngineOptions();
        // so we can see the effect of features on options
        opts.setSharedInstance(true);
        Script script = (Script) e;
        JexlFeatures features = script.getFeatures();
        Assert.assertTrue(features.isLexical());
        Assert.assertTrue(features.isLexicalShade());
        Object result = e.execute(vars);
        Assert.assertEquals(42, result);
        Assert.assertTrue(opts.isLexical());
        Assert.assertTrue(opts.isLexicalShade());
    }
    
    @Test
    public void testOptionsPragma() throws Exception {
        try {
            JexlOptions.setDefaultFlags("+safe", "-lexical", "-lexicalShade");
            VarContext vars = new VarContext();
            JexlEngine jexl = new JexlBuilder().create();
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
        String src0 = "var count = 10;\n"
                + "for (var i : 0 .. count-1) {\n"
                + "  $out.add(i);\n"
                + "}";
        String src1 = "var count = [0,1,2,3,4,5,6,7,8,9];\n"
                + "for (var i : count) {\n"
                + "  $out.add(i);\n"
                + "}";
        String src2 = "var count = 10;\n" +
                "  var outer = 0;\n"
                + "for (var i : 0 .. count-1) {\n"
                + "  $out.add(i);\n"
                + "  outer = i;"
                + "}\n"
                + "outer == 9";
        JexlFeatures ff0 = runVarLoop(false, src0);
        JexlFeatures ft0= runVarLoop(true, src0);
        JexlFeatures ff1 = runVarLoop(false, src1);
        JexlFeatures ft1= runVarLoop(true, src1);
        JexlFeatures ff2 = runVarLoop(false, src2);
        JexlFeatures ft2= runVarLoop(true, src2);
        
        // and check some features features
        Assert.assertEquals(ff0, ff1);
        Assert.assertEquals(ft0, ft1);
        Assert.assertNotEquals(ff0, ft0);
        String sff0 = ff0.toString();
        String sff1 = ff1.toString();
        Assert.assertEquals(sff0, sff1);
        String sft1 = ft1.toString();
        Assert.assertNotEquals(sff0, sft1);
    }

    private JexlFeatures runVarLoop(boolean flag, String src) throws Exception {
        try {
            VarContext vars = new VarContext();
            JexlOptions options = vars.getEngineOptions();
            options.setLexical(true);
            options.setLexicalShade(true);
            options.setSafe(false);
            JexlFeatures features = new JexlFeatures();
            if (flag) {
                features.lexical(true).lexicalShade(true);
            }
            JexlEngine jexl = new JexlBuilder().features(features).create();
            JexlScript script = jexl.createScript(src);
            List<Integer> out = new ArrayList<Integer>(10);
            vars.set("$out", out);
            Object result = script.execute(vars);
            Assert.assertEquals(true, result);
            Assert.assertEquals(10, out.size());
            return features;
        } catch(JexlException xany) {
            throw xany;
        }
    }
    
    public static class OptAnnotationContext extends JexlEvalContext implements JexlContext.AnnotationProcessor {
        @Override
        public Object processAnnotation(String name, Object[] args, Callable<Object> statement) throws Exception {
            // transient side effect for strict
            if ("scale".equals(name)) {
                JexlOptions options = this.getEngineOptions();
                int scale = options.getMathScale();
                int newScale = (Integer) args[0];
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
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        JexlScript script = jexl.createScript("@scale(13) @test var i = 42");
        JexlContext jc = new OptAnnotationContext();
        Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }
         
    @Test
    public void testNamed() throws Exception {
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        JexlScript script = jexl.createScript("var i = (x, y, z)->{return x + y + z}; i(22,18,2)");
        JexlContext jc = new MapContext();
        Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }
      
    @Test
    public void tesstCaptured0() throws Exception {
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        JexlScript script = jexl.createScript(
                "var x = 10; (b->{ x + b })(32)");
        JexlContext jc = new MapContext();
        Object result = script.execute(jc);
        Assert.assertEquals(42, result);
    }
    
    @Test
    public void testCaptured1() throws Exception {
        JexlFeatures f = new JexlFeatures();
        f.lexical(true);
        JexlEngine jexl = new JexlBuilder().strict(true).features(f).create();
        JexlScript script = jexl.createScript(
                "{var x = 10; } (b->{ x + b })(32)");
        JexlContext jc = new MapContext();
        jc.set("x", 11);
        Object result = script.execute(jc);
        Assert.assertEquals(43, result);
    }  
}
