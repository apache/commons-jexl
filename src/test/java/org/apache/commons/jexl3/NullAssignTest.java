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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for null assignment operator.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class NullAssignTest extends JexlTestCase {

    public static class Froboz {
        Integer value;

        public Froboz() {
        }

        public Froboz(int v) {
            value = v;
        }

        public void setValue(Integer v) {
            value = v;
        }
        public Integer getValue() {
            return value;
        }
    }

    public NullAssignTest() {
        super("NullAssignTest", new JexlBuilder().cache(512).strict(false).silent(false).create());
    }

    @Test
    public void testNull() throws Exception {
        JexlScript assign = JEXL.createScript("var x = null; x ?= 1");
        JexlContext jc = new MapContext();
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testNotNull() throws Exception {
        JexlScript assign = JEXL.createScript("var x = 42; x ?= 1");
        JexlContext jc = new MapContext();
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testSideEffects() throws Exception {
        JexlScript assign = JEXL.createScript("var x = 42; x ?= (a = 1)");
        JexlContext jc = new MapContext();
        Object o = assign.execute(jc);
        Assert.assertNull("Result is not as expected", jc.get("a"));
    }

    @Test
    public void testArray() throws Exception {
        JexlScript assign = JEXL.createScript("var x = [41, 42]; x[1] ?= 1; x[1]");
        JexlContext jc = new MapContext();
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);

        assign = JEXL.createScript("var x = [41, null]; x[1] ?= 42; x[1]");
        o = assign.execute(jc);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testAntish() throws Exception {
        JexlScript assign = JEXL.createScript("froboz.value ?= 10");
        JexlContext jc = new MapContext();
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not 10", 10, o);
        Assert.assertEquals("Result is not 10", 10, jc.get("froboz.value"));
    }

    @Test
    public void testStrict() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();
        JexlScript assign = jexl.createScript("froboz.value ?= 10");
        JexlContext jc = new MapContext();
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not 10", 10, o);
        Assert.assertEquals("Result is not 10", 10, jc.get("froboz.value"));
    }

    @Test
    public void testBean() throws Exception {
        JexlScript assign = JEXL.createScript("froboz.value ?= 10");
        JexlContext jc = new MapContext();
        Froboz fb = new Froboz();
        jc.set("froboz", fb);
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not 10", 10, o);
        Assert.assertEquals("Result is not 10", 10, (int) fb.getValue());
        assign = JEXL.createScript("froboz.value ?= 42");
        o = assign.execute(jc);
        Assert.assertEquals("Result is not 10", 10, o);
        Assert.assertEquals("Result is not 10", 10, (int) fb.getValue());
    }

}