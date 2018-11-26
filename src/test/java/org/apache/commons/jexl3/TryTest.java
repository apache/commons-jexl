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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests try/catch/finally statement.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class TryTest extends JexlTestCase {

    public TryTest() {
        super("TryTest");
    }

    @Test
    public void testLastValue() throws Exception {
        JexlScript e = JEXL.createScript("try {x = 1} finally {x = 2}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", 1, o);
    }

    @Test
    public void testFinallyAlwaysCalled() throws Exception {
        JexlScript e = JEXL.createScript("try {x = 1} finally {x = 42}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals(42, jc.get("x"));

        jc = new MapContext();
        e = JEXL.createScript("try {42/0} finally {x = 42}");
        try {
            o = e.execute(jc);
        } catch (Exception ex) {
            Assert.assertEquals(42, jc.get("x"));
        }

        jc = new MapContext();
        e = JEXL.createScript("try {42/0} catch (var e) {} finally {x = 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, jc.get("x"));
    }

    @Test
    public void testCatch() throws Exception {
        JexlScript e = JEXL.createScript("try {42/0} catch (e) {}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertTrue(jc.get("e") instanceof Exception);

        e = JEXL.createScript("try {42/0} catch (var e) {x = e}");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertTrue(jc.get("x") instanceof Exception);

        e = JEXL.createScript("try {return 42} catch (var e) {return 0}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testReturn() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("try {return 1} finally {return 42}");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);

        e = JEXL.createScript("try {42/0} catch(var e) {return 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        e = JEXL.createScript("try {42/0} catch(var e) {return 2} finally { return 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testBreakInsideTry() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("for (var i : 42..43) try {break} finally {}; i");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);

        e = JEXL.createScript("for (var i : 42..43) try {} finally {break}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        e = JEXL.createScript("for (var i : 42..43) try {break} catch(var e) {}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        e = JEXL.createScript("for (var i : 42..43) try {42/0} catch(var e) {break}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testContinueInsideTry() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("var i = 0; while (true) { i+=1; try {if (i < 42) continue else break} finally {}}; i");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

}
