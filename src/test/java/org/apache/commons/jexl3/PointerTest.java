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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests pointer operator.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class PointerTest extends JexlTestCase {

    public PointerTest() {
        super("PointerTest");
    }

    @Test
    public void testVarPointerGet() throws Exception {
        JexlScript e = JEXL.createScript("var x = 42; var y = &x; *y");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testVarPointerSet() throws Exception {
        JexlScript e = JEXL.createScript("var x = 1; var y = &x; *y = 42; x");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testVarPointerSideEffects() throws Exception {
        JexlScript e = JEXL.createScript("var x = 1; var y = &x; *y += 41; x");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testContextVarPointerGet() throws Exception {
        JexlScript e = JEXL.createScript("var y = &x; *y");
        JexlContext jc = new MapContext();
        jc.set("x",42);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testContextVarPointerSet() throws Exception {
        JexlScript e = JEXL.createScript("var y = &x; *y = 42");
        JexlContext jc = new MapContext();
        jc.set("x",1);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, jc.get("x"));
    }

    @Test
    public void testContextVarPointerSideEffects() throws Exception {
        JexlScript e = JEXL.createScript("var y = &x; *y += 41");
        JexlContext jc = new MapContext();
        jc.set("x",1);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, jc.get("x"));
    }

    @Test
    public void testAntishContextVarPointerGet() throws Exception {
        JexlScript e = JEXL.createScript("var y = &x.z; *y");
        JexlContext jc = new MapContext();
        jc.set("x.z",42);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testAntishContextVarPointerSet() throws Exception {
        JexlScript e = JEXL.createScript("var y = &x.z; *y = 42");
        JexlContext jc = new MapContext();
        jc.set("x.z",1);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, jc.get("x.z"));
    }

    @Test
    public void testAntishContextVarPointerSideEffects() throws Exception {
        JexlScript e = JEXL.createScript("var y = &x.z; *y += 41");
        JexlContext jc = new MapContext();
        jc.set("x.z",1);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, jc.get("x.z"));
    }

    @Test
    public void testBeanPointerGet() throws Exception {
        JexlScript e = JEXL.createScript("var x = {'a':2,'b':42}; var y = &x.b; *y");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testBeanPointerSet() throws Exception {
        JexlScript e = JEXL.createScript("var x = {'a':2,'b':1}; var y = &x.b; *y = 42; x.b");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);

        e = JEXL.createScript("var x = {'a':2,'b':42}; var y = &x.b; x = {'a':2,'b':0}; *y");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testArrayPointerGet() throws Exception {
        JexlScript e = JEXL.createScript("var x = [1,2,3,4,42]; var y = &x[4]; *y");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);

        e = JEXL.createScript("var x = [1,2,3,4,42]; var i = 4; var y = &x[i]; i = 0; *y");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);

        e = JEXL.createScript("var x = [1,2,3,4,42]; var i = 4; var y = &x[i]; x = [1,2,3,4,5]; *y");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testArrayPointerSet() throws Exception {
        JexlScript e = JEXL.createScript("var x = [1,2,3,4,5]; var y = &x[4]; *y = 42; x[4]");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);

        e = JEXL.createScript("var x = [1,2,3,4,5]; var i = 4; var y = &x[i]; i = 0; *y = 42; x[4]");
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testBadPointer() throws Exception {
        try {
            JEXL.createScript("var y = &null");
            Assert.fail("Non left values should not be allowed");
        } catch (Exception ex) {
            // OK
        }
        try {
            JEXL.createScript("var y = &(2+2)");
            Assert.fail("Non left values should not be allowed");
        } catch (Exception ex) {
            // OK
        }
    }

    @Test
    public void testOuterPointerSet() throws Exception {
        JexlScript e = JEXL.createScript("var x = 1; var f = (y) -> { *y = 42}; f(&x); x");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

}
