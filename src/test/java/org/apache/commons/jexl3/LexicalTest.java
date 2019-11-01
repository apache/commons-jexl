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
import java.util.Set;
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
    public void testLexical2() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).lexical(true).create();
        JexlEvalContext ctxt = new JexlEvalContext();

        JexlScript script = jexl.createScript("{var x = 42}; {var x; return x; }");
        Object result = script.execute(ctxt);
        Assert.assertNull(result);
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
}
