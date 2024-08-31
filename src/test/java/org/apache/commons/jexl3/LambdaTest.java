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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.jexl3.internal.Closure;
import org.apache.commons.jexl3.internal.Script;
import org.junit.jupiter.api.Test;

/**
 * Tests function/lambda/closure features.
 */
@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
public class LambdaTest extends JexlTestCase {

    public LambdaTest() {
        super("LambdaTest");
    }

    @Test
    public void test270() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        final String text = base.toString();
        JexlScript script = base.curry(5, 15);
        assertEquals(text, script.toString());

        final JexlContext ctxt = new JexlEvalContext();
        ctxt.set("s", base);
        script = jexl.createScript("return s");
        Object result = script.execute(ctxt);
        assertEquals(text, result.toString());

        script = jexl.createScript("return s.curry(1)");
        result = script.execute(ctxt);
        assertEquals(text, result.toString());
    }

    @Test
    public void test271a() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("var base = 1; var x = (a)->{ var y = (b) -> {base + b}; return base + y(a)}; x(40)");
        final Object result = base.execute(null);
        assertEquals(42, result);
    }

    @Test
    public void test271b() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("var base = 2; var sum = (x, y, z)->{ base + x + y + z }; var y = sum.curry(1); y(2,3)");
        final Object result = base.execute(null);
        assertEquals(8, result);
    }

    @Test
    public void test271c() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("(x, y, z)->{ 2 + x + y + z };");
        final JexlScript y = base.curry(1);
        final Object result = y.execute(null, 2, 3);
        assertEquals(8, result);
    }

    @Test
    public void test271d() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("var base = 2; (x, y, z)->base + x + y + z;");
        final JexlScript y = ((JexlScript) base.execute(null)).curry(1);
        final Object result = y.execute(null, 2, 3);
        assertEquals(8, result);
    }

    // Redefining a captured var is not resolved correctly in left-hand side;
    // declare the var in local frame, resolved in local frame instead of parent.
    @Test
    public void test271e() {
        final JexlEngine jexl = createEngine();
        final JexlScript base = jexl.createScript("var base = 1000; var f = (x, y)->{ var base = x + y + (base?:-1000); base; }; f(100, 20)");
        final Object result = base.execute(null);
        assertEquals(1120, result);
    }

    @Test
    public void test405a() {
        final JexlEngine jexl = new JexlBuilder()
            .cache(4).strict(true).safe(false)
            .create();
        final String libSrc = "var theFunction = argFn -> { var fn = argFn; fn() }; { 'theFunction' : theFunction }";
        final String src1 = "var v0 = 42; var v1 = -42; lib.theFunction(()->{ v1 + v0 }) ";
        final JexlScript libMap = jexl.createScript(libSrc);
        final Object theLib = libMap.execute(null);
        final JexlScript f1 = jexl.createScript(src1, "lib");
        final Object result = f1.execute(null, theLib);
        assertEquals(0, result);
    }

    @Test
    public void test405b() {
        final JexlEngine jexl = new JexlBuilder()
            .cache(4).strict(true).safe(false)
            .create();
        final String libSrc = "function theFunction(argFn) { var fn = argFn; fn() }; { 'theFunction' : theFunction }";
        final String src1 = "var v0 = 42; var v1 = -42; lib.theFunction(()->{ v1 + v0 }) ";
        final JexlScript libMap = jexl.createScript(libSrc);
        final Object theLib = libMap.execute(null);
        final JexlScript f1 = jexl.createScript(src1, "lib");
        final Object result = f1.execute(null, theLib);
        assertEquals(0, result);
    }

    @Test
    public void testCompareLambdaRecurse() throws Exception {
        final JexlEngine jexl = createEngine();
        final String factSrc = "function fact(x) { x < 2? 1 : x * fact(x - 1) }";
        final JexlScript fact0 = jexl.createScript(factSrc);
        final JexlScript fact1 = jexl.createScript(fact0.toString());
        assertEquals(fact0, fact1);
        final Closure r0 = (Closure) fact0.execute(null);
        final Closure r1 = (Closure) fact1.execute(null);
        assertEquals(720, r0.execute(null, 6));
        assertEquals(720, r1.execute(null, 6));
        assertEquals(r0, r1);
        assertEquals(r1, r0);
        // ensure we did not break anything through equals
        assertEquals(720, r0.execute(null, 6));
        assertEquals(720, r1.execute(null, 6));
    }

    @Test
    public void testCurry1() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;
        String[] parms;

        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        parms = base.getUnboundParameters();
        assertEquals(3, parms.length);
        script = base.curry(5);
        parms = script.getUnboundParameters();
        assertEquals(2, parms.length);
        script = script.curry(15);
        parms = script.getUnboundParameters();
        assertEquals(1, parms.length);
        script = script.curry(22);
        parms = script.getUnboundParameters();
        assertEquals(0, parms.length);
        result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    public void testCurry2() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;
        String[] parms;

        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        script = base.curry(5, 15);
        parms = script.getUnboundParameters();
        assertEquals(1, parms.length);
        script = script.curry(22);
        result = script.execute(null);
        assertEquals(42, result);
    }

    @Test
    public void testCurry3() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;

        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        script = base.curry(5, 15);
        result = script.execute(null, 22);
        assertEquals(42, result);
    }

    @Test
    public void testCurry4() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;

        final JexlScript base = jexl.createScript("(x, y, z)->{ x + y + z }");
        script = base.curry(5);
        result = script.execute(null, 15, 22);
        assertEquals(42, result);
    }

    @Test
    public void testCurry5() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;

        final JexlScript base = jexl.createScript("var t = x + y + z; return t", "x", "y", "z");
        script = base.curry(5);
        result = script.execute(null, 15, 22);
        assertEquals(42, result);
    }

    @Test
    public void testFailParseFunc0() {
        final String src = "if (false) function foo(x) { x + x }; var foo = 1";
        final JexlEngine jexl = createEngine();
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript(src));
        assertTrue(xparse.getMessage().contains("function"));
    }

    @Test
    public void testFailParseFunc1() {
        final String src = "if (false) let foo = (x) { x + x }; var foo = 1";
        final JexlEngine jexl = createEngine();
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript(src));
        assertTrue(xparse.getMessage().contains("let"));
    }

    @Test public void testFatFact0() {
        final JexlFeatures features = new JexlFeatures();
        features.fatArrow(true);
        final String src = "function (a) { const fact = x =>{ x <= 1? 1 : x * fact(x - 1) }; fact(a) }";
        final JexlEngine jexl = createEngine(features);
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, 6);
        assertEquals(720, result);
    }

    @Test public void testFatFact1() {
        final String src = "function (a) { const fact = (x)=> x <= 1? 1 : x * fact(x - 1) ; fact(a) }";
        final JexlFeatures features = new JexlFeatures();
        features.fatArrow(true);
        final JexlEngine jexl = createEngine(features);
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, 6);
        assertEquals(720, result);
        features.fatArrow(false);
        final JexlEngine jexl1 = createEngine(features);
        final JexlException.Feature xfeature = assertThrows(JexlException.Feature.class, () -> jexl1.createScript(src));
        assertTrue(xfeature.getMessage().contains("fat-arrow"));
    }

    @Test
    public void testHoistLambda() {
        final JexlEngine jexl = createEngine();
        final JexlEvalContext ctx = new JexlEvalContext();
        ctx.getEngineOptions().setLexical(false);
        JexlScript s42;
        Object result;
        JexlScript s15;
        String[] localv;
        Set<List<String>> hvars;
        String strs;

        // hosted variables are NOT local variables
        strs = "(x)->{ (y)->{ x + y } }";
        s42 = jexl.createScript(strs);
        result = s42.execute(ctx, 15);
        assertTrue(result instanceof JexlScript);
        s15 = (JexlScript) result;
        localv = s15.getLocalVariables();
        assertEquals(0, localv.length);
        hvars = s15.getVariables();
        assertEquals(1, hvars.size());

        // declaring a local that overrides captured
        // in 3.1, such a local was considered local
        // per 3.2, this local is considered captured
        strs = "(x)->{ (y)->{ var z = 169; var x; x + y } }";
        s42 = jexl.createScript(strs);
        result = s42.execute(ctx, 15);
        assertTrue(result instanceof JexlScript);
        s15 = (JexlScript) result;
        localv = s15.getLocalVariables();
        assertNotNull(localv);
        assertEquals(1, localv.length);
        hvars = s15.getVariables();
        assertEquals(1, hvars.size());
        // evidence this is not (strictly) a local since it inherited a captured value
        result = s15.execute(ctx, 27);
        assertEquals(42, result);
    }

    @Test
    public void testIdentity() {
        final JexlEngine jexl = createEngine();
        JexlScript script;
        Object result;

        script = jexl.createScript("(x)->{ x }");
        assertArrayEquals(new String[]{"x"}, script.getParameters());
        result = script.execute(null, 42);
        assertEquals(42, result);
    }

    @Test
    public void testLambda() {
        final JexlEngine jexl = createEngine();
        String strs = "var s = function(x) { x + x }; s(21)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        assertEquals(42, result);
        strs = "var s = function(x, y) { x + y }; s(15, 27)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
    }

    @Test
    public void testLambdaClosure()  {
        final JexlEngine jexl = createEngine();
        String strs = "var t = 20; var s = function(x, y) { x + y + t}; s(15, 7)";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        assertEquals(42, result);
        strs = "var t = 19; var s = function(x, y) { var t = 20; x + y + t}; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
        strs = "var t = 20; var s = function(x, y) {x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
        strs = "var t = 19; var s = function(x, y) { var t = 20; x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
    }

    @Test public void testLambdaExpr0() {
        final String src = "(x, y) -> x + y";
        final JexlEngine jexl = createEngine();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, 11, 31);
        assertEquals(42, result);
    }

    @Test public void testLambdaExpr1() {
        final String src = "x -> x + x";
        final JexlEngine jexl = createEngine();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, 21);
        assertEquals(42, result);
    }

    @Test public void testLambdaExpr10() {
        final String src = "(a)->{ var x = x -> x + x; x(a) }";
        final JexlEngine jexl = createEngine();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, 21);
        assertEquals(42, result);
    }

    @Test public void testLambdaExpr2() {
        final String src = "x -> { { x + x } }";
        final JexlEngine jexl = createEngine();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, 21);
        assertTrue(result instanceof Set);
        final Set<?> set = (Set<?>) result;
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
    }

    @Test public void testLambdaExpr3() {
        final String src = "x -> ( { x + x } )";
        final JexlEngine jexl = createEngine();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, 21);
        assertTrue(result instanceof Set);
        final Set<?> set = (Set<?>) result;
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
    }

    @Test
    public void testLambdaLambda() {
        final JexlEngine jexl = createEngine();
        String strs = "var t = 19; ( (x, y)->{ var t = 20; x + y + t} )(15, 7);";
        JexlScript s42 = jexl.createScript(strs);
        Object result = s42.execute(null);
        assertEquals(42, result);

        strs = "( (x, y)->{ ( (xx, yy)->{xx + yy } )(x, y) } )(15, 27)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);

        strs = "var t = 19; var s = (x, y)->{ var t = 20; x + y + t}; t = 54; s(15, 7)";
        s42 = jexl.createScript(strs);
        result = s42.execute(null);
        assertEquals(42, result);
    }

    @Test public void testNamedFunc() {
        final String src = "(let a)->{ function fact(const x) { x <= 1? 1 : x * fact(x - 1); } fact(a); }";
        final JexlEngine jexl = createEngine();
        final JexlScript script = jexl.createScript(src);
        final Object result = script.execute(null, 6);
        assertEquals(720, result);
        final String parsed = simpleWhitespace(script.getParsedText());
        assertEquals(simpleWhitespace(src), parsed);
    }

    @Test public void testNamedFuncIsConst() {
        final String src = "function foo(x) { x + x }; var foo ='nonononon'";
        final JexlEngine jexl = createEngine();
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> jexl.createScript(src));
        assertTrue(xparse.getMessage().contains("foo"));
    }

    @Test
    public void testNestLambada() throws Exception {
        final JexlEngine jexl = createEngine();
        final String strs = "(x)->{ (y)->{ x + y } }";
        final JexlScript s42 = jexl.createScript(strs);
        final JexlScript s42b = jexl.createScript(s42.toString());
        assertEquals(s42.hashCode(), s42b.hashCode());
        assertEquals(s42, s42b);
        Object result = s42.execute(null, 15);
        assertTrue(result instanceof JexlScript);
        final Object resultb = s42.execute(null, 15);
        assertEquals(result.hashCode(), resultb.hashCode());
        assertEquals(result, resultb);
        assertEquals(result, jexl.createScript(resultb.toString(), "x").execute(null, 15));
        final JexlScript s15 = (JexlScript) result;
        final Callable<Object> s15b = s15.callable(null, 27);
        result = s15.execute(null, 27);
        assertEquals(42, result);
        result = s15b.call();
        assertEquals(42, result);
    }

    @Test
    public void testNestLambda() {
        final JexlEngine jexl = createEngine();
        final String strs = "( (x)->{ (y)->{ x + y } })(15)(27)";
        final JexlScript s42 = jexl.createScript(strs);
        final Object result = s42.execute(null);
        assertEquals(42, result);
    }

    @Test
    public void testRecurse() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        final JexlScript script = jexl.createScript("var fact = (x)->{ if (x <= 1) 1; else x * fact(x - 1) }; fact(5)");
        final int result = (Integer) script.execute(jc);
        assertEquals(120, result);
    }

    @Test
    public void testRecurse1() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        final String src = "var fact = (x)-> x <= 1? 1 : x * fact(x - 1);\nfact(5);\n";
        final JexlScript script = jexl.createScript(src);
        final int result = (Integer) script.execute(jc);
        assertEquals(120, result);
        final String parsed = script.getParsedText();
        assertEquals(src, parsed);
    }

    @Test
    public void testRecurse2() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        // adding some captured vars to get it confused
        final JexlScript script = jexl.createScript(
                "var y = 1; var z = 1; "
                +"var fact = (x)->{ if (x <= y) z; else x * fact(x - 1) }; fact(6)");
        final int result = (Integer) script.execute(jc);
        assertEquals(720, result);
    }

    @Test
    public void testRecurse2b() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        // adding some captured vars to get it confused
        final JexlScript fact = jexl.createScript(
                "var y = 1; var z = 1; "
                        +"var fact = (x)->{ if (x <= y) z; else x * fact(x - 1) };" +
                        "fact");
        final Script func = (Script) fact.execute(jc);
        final String[] captured = func.getCapturedVariables();
        assertEquals(3, captured.length);
        assertTrue(Arrays.asList(captured).containsAll(Arrays.asList("z", "y", "fact")));
        final int result = (Integer) func.execute(jc, 6);
        assertEquals(720, result);
    }

    @Test
    public void testRecurse3() {
        final JexlEngine jexl = createEngine();
        final JexlContext jc = new MapContext();
        // adding some captured vars to get it confused
        final JexlScript script = jexl.createScript(
                "var y = 1; var z = 1;var foo = (x)->{y + z}; "
                +"var fact = (x)->{ if (x <= y) z; else x * fact(x - 1) }; fact(6)");
        final int result = (Integer) script.execute(jc);
        assertEquals(720, result);
    }

    @Test
    public void testScriptArguments() {
        final JexlEngine jexl = createEngine();
        final JexlScript s = jexl.createScript(" x + x ", "x");
        final JexlScript s42 = jexl.createScript("s(21)", "s");
        final Object result = s42.execute(null, s);
        assertEquals(42, result);
    }

    @Test
    public void testScriptContext() {
        final JexlEngine jexl = createEngine();
        final JexlScript s = jexl.createScript("function(x) { x + x }");
        final String fsstr = s.getParsedText(0);
        assertEquals("(x)->{ x + x; }", fsstr);
        assertEquals(42, s.execute(null, 21));
        JexlScript s42 = jexl.createScript("s(21)");
        final JexlContext ctxt = new JexlEvalContext();
        ctxt.set("s", s);
        Object result = s42.execute(ctxt);
        assertEquals(42, result);
        result = s42.execute(ctxt);
        assertEquals(42, result);
        s42 = jexl.createScript("x-> { x + x }");
        result = s42.execute(ctxt, 21);
        assertEquals(42, result);
    }

    /**
     * see JEXL-426
     */
    @Test void testRefCapture1() {
        String src = "let x = 10;\n" +
                "let foo = () -> {\n" +
                "x += 2;\n" +
                "}\n" +
                "x = 40;\n" +
                "foo();\n" +
                "x";
        final JexlEngine jexl = new JexlBuilder()
                .features(new JexlFeatures().referenceCapture(true))
                .create();
        JexlScript script;
        Object result;
        script = jexl.createScript(src);
        result = script.execute(null);
        assertEquals(42, result);
    }

    @Test void testRefCapture2() {
        String src = "let x = 10; let f = () -> { x + 2 }; x = 40; f()";
        final JexlEngine jexl = new JexlBuilder()
                .features(new JexlFeatures().constCapture(true).referenceCapture(true))
                .create();
        JexlScript script;
        Object result;
        script = jexl.createScript(src);
        result = script.execute(null);
        assertEquals(42, result);
    }

    @Test void testRefCapture3() {
        String src = "let x = 10; let f = () -> { x + 2 }; x = 40; f";
        final JexlEngine jexl = new JexlBuilder()
                .features(new JexlFeatures().constCapture(true).referenceCapture(true))
                .create();
        JexlScript script;
        Object result;
        script = jexl.createScript(src);
        result = script.execute(null);
        assertTrue(result instanceof JexlScript);
        script = jexl.createScript("f()", "f");
        result = script.execute(null, result);
        assertEquals(42, result);
    }

    @Test void testRefCapture4() {
        String src = "let x = 10; let f = () -> { let x = 142; x }; x = 40; f";
        final JexlEngine jexl = new JexlBuilder()
                .features(new JexlFeatures().referenceCapture(true))
                .create();
        JexlScript script;
        Object result;
        script = jexl.createScript(src);
        result = script.execute(null);
        assertTrue(result instanceof JexlScript);
        script = jexl.createScript("f()", "f");
        result = script.execute(null, result);
        assertEquals(142, result);
    }
}
