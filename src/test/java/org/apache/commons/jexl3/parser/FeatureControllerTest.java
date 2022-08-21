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
package org.apache.commons.jexl3.parser;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlTestCase;
import org.apache.commons.jexl3.junit.Asserter;
import org.junit.Test;

public class FeatureControllerTest extends JexlTestCase {

    public FeatureControllerTest() {
        super("FeatureControllerTest");

    }

    private JexlEngine createEngine(JexlFeatures features) {
        return new JexlBuilder().features(features).create();
    }

    @Test
    public void testLoopFeatureSwitch() throws Exception {
        Asserter onAsserter = new Asserter(createEngine(new JexlFeatures().loops(true)));
        onAsserter.setVariable("cond", true);
        onAsserter.setVariable("i", 0);

        Asserter offAsserter = new Asserter(createEngine(new JexlFeatures().loops(false)));
        offAsserter.setVariable("cond", true);
        offAsserter.setVariable("i", 0);
        String matchException = "@1:1 loop error in ''";
        String whileExpr = "while(cond) { i++;  cond = false; }; i;";
        onAsserter.assertExpression(whileExpr, 1);
        offAsserter.failExpression(whileExpr, matchException);

        String doWhileExpr = "do { i++; } while(false); i;";
        onAsserter.assertExpression(doWhileExpr, 2);
        offAsserter.failExpression(doWhileExpr, matchException);

        String forExpr = "for (j : [1, 1]) { i = i + j; }; i;";
        onAsserter.assertExpression(forExpr, 4);
        offAsserter.failExpression(forExpr, matchException);

    }

    @Test
    public void testNewInstanceFeatureSwitch() throws Exception {
        Asserter onAsserter = new Asserter(createEngine(new JexlFeatures().newInstance(true)));
        Asserter offAsserter = new Asserter(createEngine(new JexlFeatures().newInstance(false)));
        String expr = "new('java.lang.String', 'JEXL')";
        onAsserter.assertExpression(expr, "JEXL");
        offAsserter.failExpression(expr, "@1:1 create instance error in ''");
    }

    @Test
    public void testMethodCallFeatureSwitch() throws Exception {
        Asserter onAsserter = new Asserter(createEngine(new JexlFeatures().methodCall(true)));
        Asserter offAsserter = new Asserter(createEngine(new JexlFeatures().methodCall(false)));
        String expr = "'jexl'.toUpperCase()";
        onAsserter.assertExpression(expr, "JEXL");
        offAsserter.failExpression(expr, "@1:7 method call error in ''");
    }

    @Test
    public void testAnnotationFeatureSwitch() throws Exception {
        Asserter onAsserter = new Asserter(createEngine(new JexlFeatures().methodCall(true).annotation(true)));
        Asserter offAsserter = new Asserter(createEngine(new JexlFeatures().methodCall(true).annotation(false)));
        String expr = "@silent ''.toString()";
        onAsserter.assertExpression(expr, "");
        offAsserter.failExpression(expr, "@1:1 annotation error in ''");
    }

    @Test
    public void testStructuredLiteralFeatureSwitch() throws Exception {
        Asserter onAsserter = new Asserter(createEngine(new JexlFeatures().structuredLiteral(true)));
        Asserter offAsserter = new Asserter(createEngine(new JexlFeatures().structuredLiteral(false)));
        String arrayLitExpr = "[1, 2, 3, 4][3]";
        onAsserter.assertExpression(arrayLitExpr, 4);
        offAsserter.failExpression(arrayLitExpr, "@1:1 set/map/array literal error in ''");

        String mapLitExpr = "{'A' : 1, 'B' : 2}['B']";
        onAsserter.assertExpression(mapLitExpr, 2);
        offAsserter.failExpression(mapLitExpr, "@1:1 set/map/array literal error in ''");

        String setLitExpr = "{'A', 'B'}.size()";
        onAsserter.assertExpression(setLitExpr, 2);
        offAsserter.failExpression(setLitExpr, "@1:1 set/map/array literal error in ''");

        String rangeLitExpr = "(0..3).size()";
        onAsserter.assertExpression(rangeLitExpr, 4);
        offAsserter.failExpression(rangeLitExpr, "@1:5 set/map/array literal error in ''");
    }

    @Test
    public void testSideEffectEnabled() throws Exception {
        Asserter asserter = new Asserter(createEngine(new JexlFeatures().sideEffect(true)));
        asserter.assertExpression("i = 1", 1); // 1
        asserter.assertExpression("i = i + 1", 2); // 1 + 1 = 2
        asserter.assertExpression("i = i - 1", 1); // 2 - 1 = 1
        asserter.assertExpression("i = i * 2", 2); // 1 * 2 = 2
        asserter.assertExpression("i = i / 2", 1); // 2 / 2 = 1
        asserter.assertExpression("i = i % 2", 1); // 1 % 1 = 1
        asserter.assertExpression("i = i ^ 0", 1L); // 1 ^ 0 = 1
        asserter.assertExpression("i = i << 1", 2L); // 1 << 1 = 2
        asserter.assertExpression("i = i >> 1", 1L); // 1 >> 1 = 1
        asserter.assertExpression("i = i >>> 1", 0L); // 1 >>> 1 = 0

        // reset
        asserter.assertExpression("i = 1", 1);
        asserter.assertExpression("i += 1", 2);
        asserter.assertExpression("i -= 1", 1);
        asserter.assertExpression("i *= 2", 2);
        asserter.assertExpression("i /= 2", 1);
        asserter.assertExpression("i %= 2", 1);
        asserter.assertExpression("i ^= 0", 1L);
        asserter.assertExpression("i <<= 1", 2L);
        asserter.assertExpression("i >>= 1", 1L);
        asserter.assertExpression("i >>>= 1", 0L);

        // reset
        asserter.assertExpression("i = 1", 1);
        asserter.assertExpression("++i", 2);
        asserter.assertExpression("--i", 1);
        asserter.assertExpression("i++", 1);
        asserter.assertExpression("i--", 2);
    }

    @Test
    public void testSideEffectDisabled() throws Exception {
        Asserter asserter = new Asserter(createEngine(new JexlFeatures().sideEffect(false)));
        asserter.setVariable("i", 1);
        String matchException = "@1:1 assign/modify error in ''";
        asserter.failExpression("i = 1", matchException);
        asserter.failExpression("i = i + 1", matchException);
        asserter.failExpression("i = i - 1", matchException);
        asserter.failExpression("i = i * 2", matchException);
        asserter.failExpression("i = i / 2", matchException);
        asserter.failExpression("i = i % 2", matchException);
        asserter.failExpression("i = i ^ 0", matchException);
        asserter.failExpression("i = i << 1", matchException);
        asserter.failExpression("i = i >> 1", matchException);
        asserter.failExpression("i = i >>> 1", matchException);

        asserter.failExpression("i += 1", matchException);
        asserter.failExpression("i -= 1", matchException);
        asserter.failExpression("i *= 2", matchException);
        asserter.failExpression("i /= 2", matchException);
        asserter.failExpression("i %= 2", matchException);
        asserter.failExpression("i ^= 0", matchException);
        asserter.failExpression("i <<= 1", matchException);
        asserter.failExpression("i >>= 1", matchException);
        asserter.failExpression("i >>>= 1", matchException);

        asserter.failExpression("i++", matchException);
        asserter.failExpression("i--", matchException);
        matchException = "@1:3 assign/modify error in ''";
        asserter.failExpression("++i", matchException);
        asserter.failExpression("--i", matchException);
    }

}
