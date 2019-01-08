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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the primitive parameters.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class PrimitiveParametersTest extends JexlTestCase {

    JexlEngine jexl = new JexlBuilder().strict(false).arithmetic(new JexlArithmetic(false)).create();

    public PrimitiveParametersTest() {
        super("PrimitiveParametersTest");
    }

    @Test
    public void testBasic() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("var x = function(long i) {i}; x(42)");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42L, o);

        e = jexl.createScript("var x = function(char i) {i}; x(42)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", '*', o);

        e = jexl.createScript("var x = function(boolean i) {i}; x(1)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", Boolean.TRUE, o);
    }

    @Test
    public void testLambda() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("var x = (long i) -> {i;}; x(42)");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42L, o);
    }

    @Test
    public void testVarargsType() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("var x = function(long i...) {i}; x(42, 43, 44)");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", Boolean.TRUE, o instanceof long[]);

        e = jexl.createScript("var x = function(char i...) {i}; var y = x.curry(41, 42); y(43)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", Boolean.TRUE, o instanceof char[]);
    }

    @Test
    public void testVarargs() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("var x = function(long i...) {i[0]}; x(42)");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42L, o);

        e = jexl.createScript("var x = function(char i...) {i[1]}; x(41, 42)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", '*', o);

        e = jexl.createScript("var x = function(char i...) {i[1]}; x([41, 42])");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", '*', o);
    }

    @Test
    public void testCurried() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("var x = function(long a, long b) {a}; var y = x.curry(42); y(0)");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42L, o);

        e = jexl.createScript("var x = function(char a, char b) {b}; var y = x.curry(0); y(42)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", '*', o);
    }

    @Test
    public void testCurriedVarargs() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("var x = function(long a, long b...) {b[0]}; var y = x.curry(0); y(42)");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 42L, o);

        e = jexl.createScript("var x = function(char a...) {a[1]}; var y = x.curry(0); y(42)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", '*', o);

        e = jexl.createScript("var x = function(char a...) {a[1]}; var y = x.curry(0); y([42,43])");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", '*', o);

        e = jexl.createScript("var x = function(char a...) {a[1]}; var y = x.curry(0); var z = y.curry(42); z(43)");
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", '*', o);
    }

    @Test
    public void testNulls() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = JEXL.createScript("var x = function(long i) {i}; x(null)");
        try {
           Object o = e.execute(jc);
           Assert.fail("Nulls are not allowed in strict mode");
        } catch (JexlException ex) {
           // OK
        }
    }

}
