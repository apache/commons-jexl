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

import org.apache.commons.jexl3.junit.Asserter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ArithmeticTest extends JexlTestCase {
    /** A small delta to compare doubles. */
    private static final double EPSILON = 1.e-6;
    private final Asserter asserter;

    public ArithmeticTest() {
        super("ArithmeticTest");
        asserter = new Asserter(JEXL);
    }

    @Before
    @Override
    public void setUp() {
    }

    @Test
    public void testUndefinedVar() throws Exception {
        asserter.failExpression("objects[1].status", ".* undefined variable objects.*");
    }

    @Test
    public void testLeftNullOperand() throws Exception {
        asserter.setVariable("left", null);
        asserter.setVariable("right", Integer.valueOf(8));
        asserter.setStrict(true);
        asserter.failExpression("left + right", ".*null.*");
        asserter.failExpression("left - right", ".*null.*");
        asserter.failExpression("left * right", ".*null.*");
        asserter.failExpression("left / right", ".*null.*");
        asserter.failExpression("left % right", ".*null.*");
        asserter.failExpression("left & right", ".*null.*");
        asserter.failExpression("left | right", ".*null.*");
        asserter.failExpression("left ^ right", ".*null.*");
    }

    @Test
    public void testRightNullOperand() throws Exception {
        asserter.setVariable("left", Integer.valueOf(9));
        asserter.setVariable("right", null);
        asserter.failExpression("left + right", ".*null.*");
        asserter.failExpression("left - right", ".*null.*");
        asserter.failExpression("left * right", ".*null.*");
        asserter.failExpression("left / right", ".*null.*");
        asserter.failExpression("left % right", ".*null.*");
        asserter.failExpression("left & right", ".*null.*");
        asserter.failExpression("left | right", ".*null.*");
        asserter.failExpression("left ^ right", ".*null.*");
    }

    @Test
    public void testNullOperands() throws Exception {
        asserter.setVariable("left", null);
        asserter.setVariable("right", null);
        asserter.failExpression("left + right", ".*null.*");
        asserter.failExpression("left - right", ".*null.*");
        asserter.failExpression("left * right", ".*null.*");
        asserter.failExpression("left / right", ".*null.*");
        asserter.failExpression("left % right", ".*null.*");
        asserter.failExpression("left & right", ".*null.*");
        asserter.failExpression("left | right", ".*null.*");
        asserter.failExpression("left ^ right", ".*null.*");
    }

    @Test
    public void testNullOperand() throws Exception {
        asserter.setVariable("right", null);
        asserter.failExpression("~right", ".*null.*");
    }

    @Test
    public void testBigDecimal() throws Exception {
        asserter.setVariable("left", new BigDecimal(2));
        asserter.setVariable("right", new BigDecimal(6));
        asserter.assertExpression("left + right", new BigDecimal(8));
        asserter.assertExpression("right - left", new BigDecimal(4));
        asserter.assertExpression("right * left", new BigDecimal(12));
        asserter.assertExpression("right / left", new BigDecimal(3));
        asserter.assertExpression("right % left", new BigDecimal(0));
    }

    @Test
    public void testBigInteger() throws Exception {
        asserter.setVariable("left", new BigInteger("2"));
        asserter.setVariable("right", new BigInteger("6"));
        asserter.assertExpression("left + right", new BigInteger("8"));
        asserter.assertExpression("right - left", new BigInteger("4"));
        asserter.assertExpression("right * left", new BigInteger("12"));
        asserter.assertExpression("right / left", new BigInteger("3"));
        asserter.assertExpression("right % left", new BigInteger("0"));
    }

    /**
     * test some simple mathematical calculations
     */
    @Test
    public void testUnaryMinus() throws Exception {
        asserter.setVariable("aByte", new Byte((byte) 1));
        asserter.setVariable("aShort", new Short((short) 2));
        asserter.setVariable("anInteger", new Integer(3));
        asserter.setVariable("aLong", new Long(4));
        asserter.setVariable("aFloat", new Float(5.5));
        asserter.setVariable("aDouble", new Double(6.6));
        asserter.setVariable("aBigInteger", new BigInteger("7"));
        asserter.setVariable("aBigDecimal", new BigDecimal("8.8"));

        asserter.assertExpression("-3", new Integer("-3"));
        asserter.assertExpression("-3.0", new Double("-3.0"));
        asserter.assertExpression("-aByte", new Byte((byte) -1));
        asserter.assertExpression("-aShort", new Short((short) -2));
        asserter.assertExpression("-anInteger", new Integer(-3));
        asserter.assertExpression("-aLong", new Long(-4));
        asserter.assertExpression("-aFloat", new Float(-5.5));
        asserter.assertExpression("-aDouble", new Double(-6.6));
        asserter.assertExpression("-aBigInteger", new BigInteger("-7"));
        asserter.assertExpression("-aBigDecimal", new BigDecimal("-8.8"));
    }

    /**
     * test some simple mathematical calculations
     */
    @Test
    public void testCalculations() throws Exception {
        asserter.setStrict(true, false);
        /*
         * test new null coersion
         */
        asserter.setVariable("imanull", null);
        asserter.assertExpression("imanull + 2", new Integer(2));
        asserter.assertExpression("imanull + imanull", new Integer(0));
        asserter.setVariable("foo", new Integer(2));

        asserter.assertExpression("foo + 2", new Integer(4));
        asserter.assertExpression("3 + 3", new Integer(6));
        asserter.assertExpression("3 + 3 + foo", new Integer(8));
        asserter.assertExpression("3 * 3", new Integer(9));
        asserter.assertExpression("3 * 3 + foo", new Integer(11));
        asserter.assertExpression("3 * 3 - foo", new Integer(7));

        /*
         * test parenthesized exprs
         */
        asserter.assertExpression("(4 + 3) * 6", new Integer(42));
        asserter.assertExpression("(8 - 2) * 7", new Integer(42));

        /*
         * test some floaty stuff
         */
        asserter.assertExpression("3 * \"3.0\"", new Double(9));
        asserter.assertExpression("3 * 3.0", new Double(9));

        /*
         * test / and %
         */
        asserter.setStrict(false, false);
        asserter.assertExpression("6 / 3", new Integer(6 / 3));
        asserter.assertExpression("6.4 / 3", new Double(6.4 / 3));
        asserter.assertExpression("0 / 3", new Integer(0 / 3));
        asserter.assertExpression("3 / 0", new Double(0));
        asserter.assertExpression("4 % 3", new Integer(1));
        asserter.assertExpression("4.8 % 3", new Double(4.8 % 3));

    }

    @Test
    public void testCoercions() throws Exception {
        asserter.assertExpression("1", new Integer(1)); // numerics default to Integer
        asserter.assertExpression("5L", new Long(5));

        asserter.setVariable("I2", new Integer(2));
        asserter.setVariable("L2", new Long(2));
        asserter.setVariable("L3", new Long(3));
        asserter.setVariable("B10", BigInteger.TEN);

        // Integer & Integer => Integer
        asserter.assertExpression("I2 + 2", new Integer(4));
        asserter.assertExpression("I2 * 2", new Integer(4));
        asserter.assertExpression("I2 - 2", new Integer(0));
        asserter.assertExpression("I2 / 2", new Integer(1));

        // Integer & Long => Long
        asserter.assertExpression("I2 * L2", new Long(4));
        asserter.assertExpression("I2 / L2", new Long(1));

        // Long & Long => Long
        asserter.assertExpression("L2 + 3", new Long(5));
        asserter.assertExpression("L2 + L3", new Long(5));
        asserter.assertExpression("L2 / L2", new Long(1));
        asserter.assertExpression("L2 / 2", new Long(1));

        // BigInteger
        asserter.assertExpression("B10 / 10", BigInteger.ONE);
        asserter.assertExpression("B10 / I2", new BigInteger("5"));
        asserter.assertExpression("B10 / L2", new BigInteger("5"));
    }

    // JEXL-24: long integers (and doubles)
    @Test
    public void testLongLiterals() throws Exception {
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "{a = 10L; b = 10l; c = 42.0D; d = 42.0d; e=56.3F; f=56.3f; g=63.5; h=0x10; i=010; j=0x10L; k=010l}";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(10L, ctxt.get("a"));
        Assert.assertEquals(10l, ctxt.get("b"));
        Assert.assertEquals(42.0D, ctxt.get("c"));
        Assert.assertEquals(42.0d, ctxt.get("d"));
        Assert.assertEquals(56.3f, ctxt.get("e"));
        Assert.assertEquals(56.3f, ctxt.get("f"));
        Assert.assertEquals(63.5d, ctxt.get("g"));
        Assert.assertEquals(0x10, ctxt.get("h"));
        Assert.assertEquals(010, ctxt.get("i"));
        Assert.assertEquals(0x10L, ctxt.get("j"));
        Assert.assertEquals(010l, ctxt.get("k"));
    }

    @Test
    public void testBigLiteralValue() throws Exception {
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        JexlExpression e = JEXL.createExpression("9223372036854775806.5B");
        String res = String.valueOf(e.evaluate(ctxt));
        Assert.assertEquals("9223372036854775806.5", res);
    }

    @Test
    public void testBigdOp() throws Exception {
        BigDecimal sevendot475 = new BigDecimal("7.475");
        BigDecimal SO = new BigDecimal("325");
        JexlContext jc = new MapContext();
        jc.set("SO", SO);

        String expr = "2.3*SO/100";

        Object evaluate = JEXL.createExpression(expr).evaluate(jc);
        Assert.assertEquals(sevendot475, (BigDecimal) evaluate);
    }

    // JEXL-24: big integers and big decimals
    @Test
    public void testBigLiterals() throws Exception {
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "{a = 10H; b = 10h; c = 42.0B; d = 42.0b;}";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(new BigInteger("10"), ctxt.get("a"));
        Assert.assertEquals(new BigInteger("10"), ctxt.get("b"));
        Assert.assertEquals(new BigDecimal("42.0"), ctxt.get("c"));
        Assert.assertEquals(new BigDecimal("42.0"), ctxt.get("d"));
    }

    // JEXL-24: big decimals with exponent
    @Test
    public void testBigExponentLiterals() throws Exception {
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "{a = 42.0e1B; b = 42.0E+2B; c = 42.0e-1B; d = 42.0E-2b; e=4242.4242e1b}";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(new BigDecimal("42.0e+1"), ctxt.get("a"));
        Assert.assertEquals(new BigDecimal("42.0e+2"), ctxt.get("b"));
        Assert.assertEquals(new BigDecimal("42.0e-1"), ctxt.get("c"));
        Assert.assertEquals(new BigDecimal("42.0e-2"), ctxt.get("d"));
        Assert.assertEquals(new BigDecimal("4242.4242e1"), ctxt.get("e"));
    }

    // JEXL-24: doubles with exponent
    @Test
    public void test2DoubleLiterals() throws Exception {
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "{a = 42.0e1D; b = 42.0E+2D; c = 42.0e-1d; d = 42.0E-2d;}";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(Double.valueOf("42.0e+1"), ctxt.get("a"));
        Assert.assertEquals(Double.valueOf("42.0e+2"), ctxt.get("b"));
        Assert.assertEquals(Double.valueOf("42.0e-1"), ctxt.get("c"));
        Assert.assertEquals(Double.valueOf("42.0e-2"), ctxt.get("d"));
    }

    /**
     *
     * if silent, all arith exception return 0.0
     * if not silent, all arith exception throw
     * @throws Exception
     */
    @Test
    public void testDivideByZero() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        JexlEvalContext context = new JexlEvalContext(vars);
        vars.put("aByte", new Byte((byte) 1));
        vars.put("aShort", new Short((short) 2));
        vars.put("aInteger", new Integer(3));
        vars.put("aLong", new Long(4));
        vars.put("aFloat", new Float(5.5));
        vars.put("aDouble", new Double(6.6));
        vars.put("aBigInteger", new BigInteger("7"));
        vars.put("aBigDecimal", new BigDecimal("8.8"));

        vars.put("zByte", new Byte((byte) 0));
        vars.put("zShort", new Short((short) 0));
        vars.put("zInteger", new Integer(0));
        vars.put("zLong", new Long(0));
        vars.put("zFloat", new Float(0));
        vars.put("zDouble", new Double(0));
        vars.put("zBigInteger", new BigInteger("0"));
        vars.put("zBigDecimal", new BigDecimal("0"));

        String[] tnames = {
            "Byte", "Short", "Integer", "Long",
            "Float", "Double",
            "BigInteger", "BigDecimal"
        };
        // number of permutations this will generate
        final int PERMS = tnames.length * tnames.length;

        JexlEngine jexl = JEXL;
        // for non-silent, silent...
        for (int s = 0; s < 2; ++s) {
            boolean strict = Boolean.valueOf(s != 0);
            context.setStrict(true, strict);
            int zthrow = 0;
            int zeval = 0;
            // for vars of all types...
            for (String vname : tnames) {
                // for zeros of all types...
                for (String zname : tnames) {
                    // divide var by zero
                    String expr = "a" + vname + " / " + "z" + zname;
                    try {
                        JexlExpression zexpr = jexl.createExpression(expr);
                        Object nan = zexpr.evaluate(context);
                        // check we have a zero & incremement zero count
                        if (nan instanceof Number) {
                            double zero = ((Number) nan).doubleValue();
                            if (zero == 0.0) {
                                zeval += 1;
                            }
                        }
                    } catch (Exception any) {
                        // increment the exception count
                        zthrow += 1;
                    }
                }
            }
            if (strict) {
                Assert.assertTrue("All expressions should have thrown " + zthrow + "/" + PERMS,
                        zthrow == PERMS);
            } else {
                Assert.assertTrue("All expressions should have zeroed " + zeval + "/" + PERMS,
                        zeval == PERMS);
            }
        }
        debuggerCheck(jexl);
    }

    @Test
    public void testNaN() throws Exception {
        Map<String, Object> ns = new HashMap<String, Object>();
        ns.put("double", Double.class);
        JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        JexlScript script;
        Object result;
        script = jexl.createScript("#NaN");
        result = script.execute(null);
        Assert.assertTrue(Double.isNaN((Double) result));
        script = jexl.createScript("NaN");
        result = script.execute(null);
        Assert.assertTrue(Double.isNaN((Double) result));
        script = jexl.createScript("double:isNaN(#NaN)");
        result = script.execute(null);
        Assert.assertTrue((Boolean) result);
        script = jexl.createScript("double:isNaN(NaN)");
        result = script.execute(null);
        Assert.assertTrue((Boolean) result);
    }

    @Test
    /**
     * JEXL-156.
     */
    public void testMultClass() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JexlContext jc = new MapContext();
        Object ra = jexl.createExpression("463.0d * 0.1").evaluate(jc);
        Assert.assertEquals(Double.class, ra.getClass());
        Object r0 = jexl.createExpression("463.0B * 0.1").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r0.getClass());
        Object r1 = jexl.createExpression("463.0B * 0.1B").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r1.getClass());
    }

    @Test
    public void testDivClass() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JexlContext jc = new MapContext();
        Object ra = jexl.createExpression("463.0d / 0.1").evaluate(jc);
        Assert.assertEquals(Double.class, ra.getClass());
        Object r0 = jexl.createExpression("463.0B / 0.1").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r0.getClass());
        Object r1 = jexl.createExpression("463.0B / 0.1B").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r1.getClass());
    }

    @Test
    public void testPlusClass() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JexlContext jc = new MapContext();
        Object ra = jexl.createExpression("463.0d + 0.1").evaluate(jc);
        Assert.assertEquals(Double.class, ra.getClass());
        Object r0 = jexl.createExpression("463.0B + 0.1").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r0.getClass());
        Object r1 = jexl.createExpression("463.0B + 0.1B").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r1.getClass());
    }

    @Test
    public void testMinusClass() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JexlContext jc = new MapContext();
        Object ra = jexl.createExpression("463.0d - 0.1").evaluate(jc);
        Assert.assertEquals(Double.class, ra.getClass());
        Object r0 = jexl.createExpression("463.0B - 0.1").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r0.getClass());
        Object r1 = jexl.createExpression("463.0B - 0.1B").evaluate(jc);
        Assert.assertEquals(java.math.BigDecimal.class, r1.getClass());
    }

    @Test
    public void testAddWithStringsLenient() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic(false)).create();
        JexlScript script;
        Object result;
        script = jexl.createScript("'a' + 0");
        result = script.execute(null);
        Assert.assertEquals("a0", result);

        script = jexl.createScript("0 + 'a' ");
        result = script.execute(null);
        Assert.assertEquals("0a", result);

        script = jexl.createScript("0 + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals(1.2d, (Double) result, EPSILON);

        script = jexl.createScript("'1.2' + 1.2 ");
        result = script.execute(null);
        Assert.assertEquals(2.4d, (Double) result, EPSILON);

        script = jexl.createScript("1.2 + 1.2 ");
        result = script.execute(null);
        Assert.assertEquals(2.4d, (Double) result, EPSILON);

        script = jexl.createScript("1.2 + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals(2.4d, (Double) result, EPSILON);

        script = jexl.createScript("'1.2' + 0 ");
        result = script.execute(null);
        Assert.assertEquals(1.2d, (Double) result, EPSILON);

        script = jexl.createScript("'1.2' + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals("1.21.2", result);
    }

    @Test
    public void testAddWithStringsStrict() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic(true)).create();
        JexlScript script;
        Object result;
        script = jexl.createScript("'a' + 0");
        result = script.execute(null);
        Assert.assertEquals("a0", result);

        script = jexl.createScript("0 + 'a' ");
        result = script.execute(null);
        Assert.assertEquals("0a", result);

        script = jexl.createScript("0 + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals("01.2", result);

        script = jexl.createScript("'1.2' + 1.2 ");
        result = script.execute(null);
        Assert.assertEquals("1.21.2", result);

        script = jexl.createScript("1.2 + 1.2 ");
        result = script.execute(null);
        Assert.assertEquals(2.4d, (Double) result, EPSILON);

        script = jexl.createScript("1.2 + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals("1.21.2", result);

        script = jexl.createScript("'1.2' + 0 ");
        result = script.execute(null);
        Assert.assertEquals("1.20", result);

        script = jexl.createScript("'1.2' + '1.2' ");
        result = script.execute(null);
        Assert.assertEquals("1.21.2", result);
    }

    @Test
    public void testOption() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>();
        JexlEvalContext context = new JexlEvalContext(vars);
        JexlScript script = JEXL.createScript("0 + '1.2' ");
        Object result;

        context.setStrictArithmetic(true);
        result = script.execute(context);
        Assert.assertEquals("01.2", result);

        context.setStrictArithmetic(false);
        result = script.execute(context);
        Assert.assertEquals(1.2d, (Double) result, EPSILON);
    }

    @Test
    public void testIsFloatingPointPattern() throws Exception {
        JexlArithmetic ja = new JexlArithmetic(true);

        Assert.assertFalse(ja.isFloatingPointNumber("floating point"));
        Assert.assertFalse(ja.isFloatingPointNumber("a1."));
        Assert.assertFalse(ja.isFloatingPointNumber("b1.2"));
        Assert.assertFalse(ja.isFloatingPointNumber("-10.2a-34"));
        Assert.assertFalse(ja.isFloatingPointNumber("+10.2a+34"));
        Assert.assertFalse(ja.isFloatingPointNumber("0"));
        Assert.assertFalse(ja.isFloatingPointNumber("1"));
        Assert.assertFalse(ja.isFloatingPointNumber("12A"));
        Assert.assertFalse(ja.isFloatingPointNumber("2F3"));
        Assert.assertFalse(ja.isFloatingPointNumber("23"));
        Assert.assertFalse(ja.isFloatingPointNumber("+3"));
        Assert.assertFalse(ja.isFloatingPointNumber("+34"));
        Assert.assertFalse(ja.isFloatingPointNumber("+3-4"));
        Assert.assertFalse(ja.isFloatingPointNumber("+3.-4"));
        Assert.assertFalse(ja.isFloatingPointNumber("3ee4"));

        Assert.assertTrue(ja.isFloatingPointNumber("0."));
        Assert.assertTrue(ja.isFloatingPointNumber("1."));
        Assert.assertTrue(ja.isFloatingPointNumber("1.2"));
        Assert.assertTrue(ja.isFloatingPointNumber("1.2e3"));
        Assert.assertTrue(ja.isFloatingPointNumber("2e3"));
        Assert.assertTrue(ja.isFloatingPointNumber("+2e-3"));
        Assert.assertTrue(ja.isFloatingPointNumber("+23E-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("+23.E-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("-23.4E+45"));
        Assert.assertTrue(ja.isFloatingPointNumber("1.2e34"));
        Assert.assertTrue(ja.isFloatingPointNumber("10.2e34"));
        Assert.assertTrue(ja.isFloatingPointNumber("+10.2e34"));
        Assert.assertTrue(ja.isFloatingPointNumber("-10.2e34"));
        Assert.assertTrue(ja.isFloatingPointNumber("10.2e-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("10.2e+34"));
        Assert.assertTrue(ja.isFloatingPointNumber("-10.2e-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("+10.2e+34"));
        Assert.assertTrue(ja.isFloatingPointNumber("-10.2E-34"));
        Assert.assertTrue(ja.isFloatingPointNumber("+10.2E+34"));
    }

    public static class EmptyTestContext extends MapContext implements JexlContext.NamespaceResolver {
        public static int log(Object fmt, Object... arr) {
            //System.out.println(String.format(fmt.toString(), arr));
            return arr == null ? 0 : arr.length;
        }

        public static int log(Object fmt, int... arr) {
            //System.out.println(String.format(fmt.toString(), arr));
            return arr == null ? 0 : arr.length;
        }

        @Override
        public Object resolveNamespace(String name) {
            return this;
        }
    }

    @Test
    public void testEmpty() throws Exception {
        Object[] SCRIPTS = {
            "var x = null; log('x = %s', x);", 0,
            "var x = 'abc'; log('x = %s', x);", 1,
            "var x = 333; log('x = %s', x);", 1,
            "var x = [1, 2]; log('x = %s', x);", 2,
            "var x = ['a', 'b']; log('x = %s', x);", 2,
            "var x = {1:'A', 2:'B'}; log('x = %s', x);", 1,
            "var x = null; return empty(x);", true,
            "var x = ''; return empty(x);", true,
            "var x = 'abc'; return empty(x);", false,
            "var x = 0; return empty(x);", true,
            "var x = 333; return empty(x);", false,
            "var x = []; return empty(x);", true,
            "var x = [1, 2]; return empty(x);", false,
            "var x = ['a', 'b']; return empty(x);", false,
            "var x = [...]; return empty(x);", true,
            "var x = [1, 2,...]; return empty(x);", false,
            "var x = {:}; return empty(x);", true,
            "var x = {1:'A', 2:'B'}; return empty(x);", false,
            "var x = {}; return empty(x);", true,
            "var x = {'A','B'}; return empty(x);", false
        };
        JexlEngine jexl = new JexlBuilder().create();
        JexlContext jc = new EmptyTestContext();
        JexlScript script;

        for (int e = 0; e < SCRIPTS.length; e += 2) {
            String stext = (String) SCRIPTS[e];
            Object expected = SCRIPTS[e + 1];
            script = jexl.createScript(stext);
            Object result = script.execute(jc);
            Assert.assertEquals("failed on " + stext, expected, result);
        }
    }

    public static class Var {
        int value;

        Var(int v) {
            value = v;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    // an arithmetic that know how to subtract strings
    public static class ArithmeticPlus extends JexlArithmetic {
        public ArithmeticPlus(boolean strict) {
            super(strict);
        }

        public boolean equals(Var lhs, Var rhs) {
            return lhs.value == rhs.value;
        }

        public boolean lessThan(Var lhs, Var rhs) {
            return lhs.value < rhs.value;
        }

        public boolean lessThanOrEqual(Var lhs, Var rhs) {
            return lhs.value <= rhs.value;
        }

        public boolean greaterThan(Var lhs, Var rhs) {
            return lhs.value > rhs.value;
        }

        public boolean greaterThanOrEqual(Var lhs, Var rhs) {
            return lhs.value >= rhs.value;
        }

        public Var add(Var lhs, Var rhs) {
            return new Var(lhs.value + rhs.value);
        }

        public Var subtract(Var lhs, Var rhs) {
            return new Var(lhs.value - rhs.value);
        }

        public Var divide(Var lhs, Var rhs) {
            return new Var(lhs.value / rhs.value);
        }

        public Var multiply(Var lhs, Var rhs) {
            return new Var(lhs.value * rhs.value);
        }

        public Var mod(Var lhs, Var rhs) {
            return new Var(lhs.value / rhs.value);
        }

        public Var negate(Var arg) {
            return new Var(-arg.value);
        }

        public Var and(Var lhs, Var rhs) {
            return new Var(lhs.value & rhs.value);
        }

        public Var or(Var lhs, Var rhs) {
            return new Var(lhs.value | rhs.value);
        }

        public Var xor(Var lhs, Var rhs) {
            return new Var(lhs.value ^ rhs.value);
        }

        public Boolean contains(Var lhs, Var rhs) {
            return lhs.toString().contains(rhs.toString());
        }

        public Boolean startsWith(Var lhs, Var rhs) {
            return lhs.toString().startsWith(rhs.toString());
        }

        public Boolean endsWith(Var lhs, Var rhs) {
            return lhs.toString().endsWith(rhs.toString());
        }

        public Var complement(Var arg) {
            return new Var(~arg.value);
        }

        public Object subtract(String x, String y) {
            int ix = x.indexOf(y);
            if (ix < 0) {
                return x;
            }
            StringBuilder strb = new StringBuilder(x.substring(0, ix));
            strb.append(x.substring(ix + y.length()));
            return strb.toString();
        }

        public Object negate(final String str) {
            final int length = str.length();
            StringBuilder strb = new StringBuilder(str.length());
            for (int c = length - 1; c >= 0; --c) {
                strb.append(str.charAt(c));
            }
            return strb.toString();
        }

        public Object not(Var x) {
            throw new NullPointerException("make it fail");
        }
    }

    @Test
    public void testArithmeticPlus() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(64).arithmetic(new ArithmeticPlus(false)).create();
        JexlContext jc = new EmptyTestContext();
        runOverload(jexl, jc);
        runOverload(jexl, jc);
    }

    @Test
    public void testArithmeticPlusNoCache() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(0).arithmetic(new ArithmeticPlus(false)).create();
        JexlContext jc = new EmptyTestContext();
        runOverload(jexl, jc);
    }

    protected void runOverload(JexlEngine jexl, JexlContext jc) {
        JexlScript script;
        Object result;

        script = jexl.createScript("(x, y)->{ x < y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(true, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(43), new Var(42));
        Assert.assertEquals(false, result);

        script = jexl.createScript("(x, y)->{ x <= y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(41), new Var(44));
        Assert.assertEquals(true, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(45), new Var(40));
        Assert.assertEquals(false, result);

        script = jexl.createScript("(x, y)->{ x > y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(false, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(43), new Var(42));
        Assert.assertEquals(true, result);

        script = jexl.createScript("(x, y)->{ x >= y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(41), new Var(44));
        Assert.assertEquals(false, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(45), new Var(40));
        Assert.assertEquals(true, result);

        script = jexl.createScript("(x, y)->{ x == y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(41), new Var(44));
        Assert.assertEquals(false, result);
        result = script.execute(jc, 43, 42);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(45), new Var(40));
        Assert.assertEquals(false, result);

        script = jexl.createScript("(x, y)->{ x != y }");
        result = script.execute(jc, 42, 43);
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(42), new Var(43));
        Assert.assertEquals(true, result);
        result = script.execute(jc, new Var(44), new Var(44));
        Assert.assertEquals(false, result);
        result = script.execute(jc, 44, 44);
        Assert.assertEquals(false, result);
        result = script.execute(jc, new Var(45), new Var(40));
        Assert.assertEquals(true, result);

        script = jexl.createScript("(x, y)->{ x % y }");
        result = script.execute(jc, 4242, 100);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(4242), new Var(100));
        Assert.assertEquals(42, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x * y }");
        result = script.execute(jc, 6, 7);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(6), new Var(7));
        Assert.assertEquals(42, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x + y }");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(35), new Var(7));
        Assert.assertEquals(42, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x - y }");
        result = script.execute(jc, 49, 7);
        Assert.assertEquals(42, result);
        result = script.execute(jc, "foobarquux", "bar");
        Assert.assertEquals("fooquux", result);
        result = script.execute(jc, 50, 8);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(50), new Var(8));
        Assert.assertEquals(42, ((Var) result).value);

        script = jexl.createScript("(x)->{ -x }");
        result = script.execute(jc, -42);
        Assert.assertEquals(42, result);
        result = script.execute(jc, new Var(-42));
        Assert.assertEquals(42, ((Var) result).value);
        result = script.execute(jc, "pizza");
        Assert.assertEquals("azzip", result);
        result = script.execute(jc, -142);
        Assert.assertEquals(142, result);

        script = jexl.createScript("(x)->{ ~x }");
        result = script.execute(jc, -1);
        Assert.assertEquals(0L, result);
        result = script.execute(jc, new Var(-1));
        Assert.assertEquals(0L, ((Var) result).value);
        result = script.execute(jc, new Var(-42));
        Assert.assertEquals(41, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x ^ y }");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(36L, result);
        result = script.execute(jc, new Var(35), new Var(7));
        Assert.assertEquals(36L, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x & y }");
        result = script.execute(jc, 35, 7);
        Assert.assertEquals(3L, result);
        result = script.execute(jc, new Var(35), new Var(7));
        Assert.assertEquals(3L, ((Var) result).value);

        script = jexl.createScript("(x, y)->{ x =^ y }");
        result = script.execute(jc, 3115, 31);
        Assert.assertFalse((Boolean) result);
        result = script.execute(jc, new Var(3115), new Var(31));
        Assert.assertTrue((Boolean) result);

        script = jexl.createScript("(x, y)->{ x !^ y }");
        result = script.execute(jc, 3115, 31);
        Assert.assertTrue((Boolean) result);
        result = script.execute(jc, new Var(3115), new Var(31));
        Assert.assertFalse((Boolean) result);

        script = jexl.createScript("(x, y)->{ x =$ y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertFalse((Boolean) result);
        result = script.execute(jc, new Var(3115), new Var(15));
        Assert.assertTrue((Boolean) result);

        script = jexl.createScript("(x, y)->{ x !$ y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertTrue((Boolean) result);
        result = script.execute(jc, new Var(3115), new Var(15));
        Assert.assertFalse((Boolean) result);

        script = jexl.createScript("(x, y)->{ x =~ y }");
        result = script.execute(jc, 3155, 15);
        Assert.assertFalse((Boolean) result);
        result = script.execute(jc, new Var(3155), new Var(15));
        Assert.assertFalse((Boolean) result);
        result = script.execute(jc, new Var(15), new Var(3155));
        Assert.assertTrue((Boolean) result);

        script = jexl.createScript("(x, y)->{ x !~ y }");
        result = script.execute(jc, 3115, 15);
        Assert.assertTrue((Boolean) result);
        result = script.execute(jc, new Var(3155), new Var(15));
        Assert.assertTrue((Boolean) result);
        result = script.execute(jc, new Var(15), new Var(3155));
        Assert.assertFalse((Boolean) result);

        script = jexl.createScript("(x)->{ !x }");
        try {
            result = script.execute(jc, new Var(-42));
            Assert.fail("should fail");
        } catch (JexlException xany) {
            Assert.assertTrue(xany instanceof JexlException.Operator);
        }
    }

    public class Callable173 {
        public Object call(String... arg) {
            return 42;
        }
        public Object call(Integer... arg) {
            return arg[0] * arg[1];
        }
    }

    @Test
    public void testJexl173() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JexlContext jc = new MapContext();
        Callable173 c173 = new Callable173();
        JexlScript e = jexl.createScript( "c173(9, 6)", "c173" );
        Object result = e.execute(jc, c173);
        Assert.assertEquals(54, result);
        e = jexl.createScript( "c173('fourty', 'two')", "c173" );
        result = e.execute(jc, c173);
        Assert.assertEquals(42, result);

    }

    public static class Arithmetic132 extends JexlArithmetic {
        public Arithmetic132() {
            super(false);
        }

        protected double divideZero(BigDecimal x) {
            int ls = x.signum();
            if (ls < 0) {
                return Double.NEGATIVE_INFINITY;
            } else if (ls > 0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return Double.NaN;
            }
        }

        protected double divideZero(BigInteger x) {
            int ls = x.signum();
            if (ls < 0) {
                return Double.NEGATIVE_INFINITY;
            } else if (ls > 0) {
                return Double.POSITIVE_INFINITY;
            } else {
                return Double.NaN;
            }
        }

        @Override
        public Object divide(Object left, Object right) {
            if (left == null && right == null) {
                return controlNullNullOperands();
            }
            // if either are bigdecimal use that type
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                BigDecimal l = toBigDecimal(left);
                BigDecimal r = toBigDecimal(right);
                if (BigDecimal.ZERO.equals(r)) {
                    return divideZero(l);
                }
                BigDecimal result = l.divide(r, getMathContext());
                return narrowBigDecimal(left, right, result);
            }
            // if either are floating point (double or float) use double
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                double l = toDouble(left);
                double r = toDouble(right);
                return new Double(l / r);
            }
            // otherwise treat as integers
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            if (BigInteger.ZERO.equals(r)) {
                return divideZero(l);
            }
            BigInteger result = l.divide(r);
            return narrowBigInteger(left, right, result);
        }

        @Override
        public Object mod(Object left, Object right) {
            if (left == null && right == null) {
                return controlNullNullOperands();
            }
            // if either are bigdecimal use that type
            if (left instanceof BigDecimal || right instanceof BigDecimal) {
                BigDecimal l = toBigDecimal(left);
                BigDecimal r = toBigDecimal(right);
                if (BigDecimal.ZERO.equals(r)) {
                    return divideZero(l);
                }
                BigDecimal remainder = l.remainder(r, getMathContext());
                return narrowBigDecimal(left, right, remainder);
            }
            // if either are floating point (double or float) use double
            if (isFloatingPointNumber(left) || isFloatingPointNumber(right)) {
                double l = toDouble(left);
                double r = toDouble(right);
                return new Double(l % r);
            }
            // otherwise treat as integers
            BigInteger l = toBigInteger(left);
            BigInteger r = toBigInteger(right);
            BigInteger result = l.mod(r);
            if (BigInteger.ZERO.equals(r)) {
                return divideZero(l);
            }
            return narrowBigInteger(left, right, result);
        }
    }

    @Test
    public void testInfiniteArithmetic() throws Exception {
        Map<String, Object> ns = new HashMap<String, Object>();
        ns.put("math", Math.class);
        JexlEngine jexl = new JexlBuilder().arithmetic(new Arithmetic132()).namespaces(ns).create();

        Object evaluate = jexl.createExpression("1/0").evaluate(null);
        Assert.assertTrue(Double.isInfinite((Double) evaluate));

        evaluate = jexl.createExpression("-1/0").evaluate(null);
        Assert.assertTrue(Double.isInfinite((Double) evaluate));

        evaluate = jexl.createExpression("1.0/0.0").evaluate(null);
        Assert.assertTrue(Double.isInfinite((Double) evaluate));

        evaluate = jexl.createExpression("-1.0/0.0").evaluate(null);
        Assert.assertTrue(Double.isInfinite((Double) evaluate));

        evaluate = jexl.createExpression("math:abs(-42)").evaluate(null);
        Assert.assertEquals(42, evaluate);
    }

    private static Document getDocument(String xml) throws Exception {
        DocumentBuilder xmlBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputStream stringInputStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        return xmlBuilder.parse(stringInputStream);
    }

    public static class XmlArithmetic extends JexlArithmetic {
        public XmlArithmetic(boolean lenient) {
            super(lenient);
        }

        public boolean empty(org.w3c.dom.Element elt) {
            return !elt.hasAttributes() && !elt.hasChildNodes();
        }

        public int size(org.w3c.dom.Element elt) {
            return elt.getChildNodes().getLength();
        }
    }

    @Test
    /**
     * Inspired by JEXL-16{1,2}.
     */
    public void testXmlArithmetic() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new XmlArithmetic(false)).create();
        JexlScript e0 = jexl.createScript("x.empty()", "x");
        JexlScript e1 = jexl.createScript("empty(x)", "x");
        JexlScript s0 = jexl.createScript("x.size()", "x");
        JexlScript s1 = jexl.createScript("size(x)", "x");
        Document xml;
        Node x;
        Boolean empty;
        int size;
        xml = getDocument("<node info='123'/>");
        x = xml.getLastChild();
        empty = (Boolean) e0.execute(null, x);
        Assert.assertFalse(empty);
        empty = (Boolean) e1.execute(null, x);
        Assert.assertFalse(empty);
        size = (Integer) s0.execute(null, x);
        Assert.assertEquals(0, size);
        size = (Integer) s1.execute(null, x);
        Assert.assertEquals(0, size);
        xml = getDocument("<node><a/><b/></node>");
        x = xml.getLastChild();
        empty = (Boolean) e0.execute(null, x);
        Assert.assertFalse(empty);
        empty = (Boolean) e1.execute(null, x);
        Assert.assertFalse(empty);
        size = (Integer) s0.execute(null, x);
        Assert.assertEquals(2, size);
        size = (Integer) s1.execute(null, x);
        Assert.assertEquals(2, size);
        xml = getDocument("<node/>");
        x = xml.getLastChild();
        empty = (Boolean) e0.execute(null, x);
        Assert.assertTrue(empty);
        empty = (Boolean) e1.execute(null, x);
        Assert.assertTrue(empty);
        size = (Integer) s0.execute(null, x);
        Assert.assertEquals(0, size);
        size = (Integer) s1.execute(null, x);
        Assert.assertEquals(0, size);
        xml = getDocument("<node info='123'/>");
        NamedNodeMap nnm = xml.getLastChild().getAttributes();
        Attr info = (Attr) nnm.getNamedItem("info");
        Assert.assertEquals("123", info.getValue());

        // JEXL-161
        JexlContext jc = new MapContext();
        jc.set("x", xml.getLastChild());
        final String y = "456";
        jc.set("y", y);
        JexlScript s = jexl.createScript("x.attribute.info = y");
        Object r = s.execute(jc);
        nnm = xml.getLastChild().getAttributes();
        info = (Attr) nnm.getNamedItem("info");
        Assert.assertEquals(y, r);
        Assert.assertEquals(y, info.getValue());
    }

    @Test
    public void testEmptyLong() throws Exception {
        Object x;
        x = JEXL.createScript("new('java.lang.Long', 4294967296)").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        x = JEXL.createScript("new('java.lang.Long', '4294967296')").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        x = JEXL.createScript("4294967296l").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        x = JEXL.createScript("4294967296L").execute(null);
        Assert.assertEquals(4294967296L, ((Long) x).longValue());
        checkEmpty(x, false);
        x = JEXL.createScript("0L").execute(null);
        Assert.assertEquals(0, ((Long) x).longValue());
        checkEmpty(x, true);
    }

    @Test
    public void testEmptyFloat() throws Exception {
        Object x;
        x = JEXL.createScript("4294967296.f").execute(null);
        Assert.assertEquals(4294967296.0f, (Float) x, EPSILON);
        checkEmpty(x, false);
        x = JEXL.createScript("4294967296.0f").execute(null);
        Assert.assertEquals(4294967296.0f, (Float) x, EPSILON);
        checkEmpty(x, false);
        x = JEXL.createScript("0.0f").execute(null);
        Assert.assertEquals(0.0f, (Float) x, EPSILON);
        checkEmpty(x, true);
        x = Float.NaN;
        checkEmpty(x, true);
    }

    @Test
    public void testEmptyDouble() throws Exception {
        Object x;
        x = JEXL.createScript("4294967296.d").execute(null);
        Assert.assertEquals(4294967296.0d, (Double) x, EPSILON);
        checkEmpty(x, false);
        x = JEXL.createScript("4294967296.0d").execute(null);
        Assert.assertEquals(4294967296.0d, (Double) x, EPSILON);
        checkEmpty(x, false);
        x = JEXL.createScript("0.0d").execute(null);
        Assert.assertEquals(0.0d, (Double) x, EPSILON);
        checkEmpty(x, true);
        x = Double.NaN;
        checkEmpty(x, true);

    }

    void checkEmpty(Object x, boolean expect) {
        JexlScript s0 = JEXL.createScript("empty(x)", "x");
        boolean empty = (Boolean) s0.execute(null, x);
        Assert.assertEquals(expect, empty);
        JexlScript s1 = JEXL.createScript("empty x", "x");
        empty = (Boolean) s1.execute(null, x);
        Assert.assertEquals(expect, empty);
        JexlScript s2 = JEXL.createScript("x.empty()", "x");
        empty = (Boolean) s2.execute(null, x);
        Assert.assertEquals(expect, empty);
    }

    @Test
    public void testCoerceInteger() throws Exception {
        JexlArithmetic ja = JEXL.getArithmetic();
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H;";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(34, ja.toInteger(ctxt.get("a")));
        Assert.assertEquals(45, ja.toInteger(ctxt.get("b")));
        Assert.assertEquals(56, ja.toInteger(ctxt.get("c")));
        Assert.assertEquals(67, ja.toInteger(ctxt.get("d")));
        Assert.assertEquals(78, ja.toInteger(ctxt.get("e")));
        Assert.assertEquals(10, ja.toInteger("10"));
        Assert.assertEquals(1, ja.toInteger(true));
        Assert.assertEquals(0, ja.toInteger(false));
    }

    @Test
    public void testCoerceLong() throws Exception {
        JexlArithmetic ja = JEXL.getArithmetic();
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H;";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(34L, ja.toLong(ctxt.get("a")));
        Assert.assertEquals(45L, ja.toLong(ctxt.get("b")));
        Assert.assertEquals(56L, ja.toLong(ctxt.get("c")));
        Assert.assertEquals(67L, ja.toLong(ctxt.get("d")));
        Assert.assertEquals(78L, ja.toLong(ctxt.get("e")));
        Assert.assertEquals(10L, ja.toLong("10"));
        Assert.assertEquals(1L, ja.toLong(true));
        Assert.assertEquals(0L, ja.toLong(false));
    }

    @Test
    public void testCoerceDouble() throws Exception {
        JexlArithmetic ja = JEXL.getArithmetic();
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "{a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H; }";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(34, ja.toDouble(ctxt.get("a")), EPSILON);
        Assert.assertEquals(45, ja.toDouble(ctxt.get("b")), EPSILON);
        Assert.assertEquals(56, ja.toDouble(ctxt.get("c")), EPSILON);
        Assert.assertEquals(67, ja.toDouble(ctxt.get("d")), EPSILON);
        Assert.assertEquals(78, ja.toDouble(ctxt.get("e")), EPSILON);
        Assert.assertEquals(10d, ja.toDouble("10"), EPSILON);
        Assert.assertEquals(1.D, ja.toDouble(true), EPSILON);
        Assert.assertEquals(0.D, ja.toDouble(false), EPSILON);
    }

    @Test
    public void testCoerceBigInteger() throws Exception {
        JexlArithmetic ja = JEXL.getArithmetic();
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "{a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H; }";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(BigInteger.valueOf(34), ja.toBigInteger(ctxt.get("a")));
        Assert.assertEquals(BigInteger.valueOf(45), ja.toBigInteger(ctxt.get("b")));
        Assert.assertEquals(BigInteger.valueOf(56), ja.toBigInteger(ctxt.get("c")));
        Assert.assertEquals(BigInteger.valueOf(67), ja.toBigInteger(ctxt.get("d")));
        Assert.assertEquals(BigInteger.valueOf(78), ja.toBigInteger(ctxt.get("e")));
        Assert.assertEquals(BigInteger.valueOf(10), ja.toBigInteger("10"));
        Assert.assertEquals(BigInteger.valueOf(1), ja.toBigInteger(true));
        Assert.assertEquals(BigInteger.valueOf(0), ja.toBigInteger(false));
    }

    @Test
    public void testCoerceBigDecimal() throws Exception {
        JexlArithmetic ja = JEXL.getArithmetic();
        JexlEvalContext ctxt = new JexlEvalContext();
        ctxt.setStrictArithmetic(true);
        String stmt = "{a = 34L; b = 45.0D; c=56.0F; d=67B; e=78H; }";
        JexlScript expr = JEXL.createScript(stmt);
        /* Object value = */ expr.execute(ctxt);
        Assert.assertEquals(BigDecimal.valueOf(34), ja.toBigDecimal(ctxt.get("a")));
        Assert.assertEquals(BigDecimal.valueOf(45.), ja.toBigDecimal(ctxt.get("b")));
        Assert.assertEquals(BigDecimal.valueOf(56.), ja.toBigDecimal(ctxt.get("c")));
        Assert.assertEquals(BigDecimal.valueOf(67), ja.toBigDecimal(ctxt.get("d")));
        Assert.assertEquals(BigDecimal.valueOf(78), ja.toBigDecimal(ctxt.get("e")));
        Assert.assertEquals(BigDecimal.valueOf(10), ja.toBigDecimal("10"));
        Assert.assertEquals(BigDecimal.valueOf(1.), ja.toBigDecimal(true));
        Assert.assertEquals(BigDecimal.valueOf(0.), ja.toBigDecimal(false));
    }

    @Test
    public void testAtomicBoolean() throws Exception {
        // in a condition
        JexlScript e = JEXL.createScript("if (x) 1 else 2;", "x");
        JexlContext jc = new MapContext();
        AtomicBoolean ab = new AtomicBoolean(false);
        Object o;
        o = e.execute(jc, ab);
        Assert.assertEquals("Result is not 2", new Integer(2), o);
        ab.set(true);
        o = e.execute(jc, ab);
        Assert.assertEquals("Result is not 1", new Integer(1), o);
        // in a binary logical op
        e = JEXL.createScript("x && y", "x", "y");
        ab.set(true);
        o = e.execute(jc, ab, Boolean.FALSE);
        Assert.assertFalse((Boolean) o);
        ab.set(true);
        o = e.execute(jc, ab, Boolean.TRUE);
        Assert.assertTrue((Boolean) o);
        ab.set(false);
        o = e.execute(jc, ab, Boolean.FALSE);
        Assert.assertFalse((Boolean) o);
        ab.set(false);
        o = e.execute(jc, ab, Boolean.FALSE);
        Assert.assertFalse((Boolean) o);
        // in arithmetic op
        e = JEXL.createScript("x + y", "x", "y");
        ab.set(true);
        o = e.execute(jc, ab, 10);
        Assert.assertEquals(11, o);
        o = e.execute(jc, 10, ab);
        Assert.assertEquals(11, o);
        o = e.execute(jc, ab, 10.d);
        Assert.assertEquals(11.d, (Double) o, EPSILON);
        o = e.execute(jc, 10.d, ab);
        Assert.assertEquals(11.d, (Double) o, EPSILON);

        BigInteger bi10 = BigInteger.TEN;
        ab.set(false);
        o = e.execute(jc, ab, bi10);
        Assert.assertEquals(bi10, o);
        o = e.execute(jc, bi10, ab);
        Assert.assertEquals(bi10, o);

        BigDecimal bd10 = BigDecimal.TEN;
        ab.set(false);
        o = e.execute(jc, ab, bd10);
        Assert.assertEquals(bd10, o);
        o = e.execute(jc, bd10, ab);
        Assert.assertEquals(bd10, o);

        // in a (the) monadic op
        e = JEXL.createScript("!x", "x");
        ab.set(true);
        o = e.execute(jc, ab);
        Assert.assertFalse((Boolean) o);
        ab.set(false);
        o = e.execute(jc, ab);
        Assert.assertTrue((Boolean) o);

        // in a (the) monadic op
        e = JEXL.createScript("-x", "x");
        ab.set(true);
        o = e.execute(jc, ab);
        Assert.assertFalse((Boolean) o);
        ab.set(false);
        o = e.execute(jc, ab);
        Assert.assertTrue((Boolean) o);
    }
}
