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
 * Tests labeled statements.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class LabeledStatementsTest extends JexlTestCase {

    public LabeledStatementsTest() {
        super("LabeledStatements");
    }

    @Test
    public void testBlock() throws Exception {
        JexlScript e = JEXL.createScript("egg : {x = 1; break egg; x = 2}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, jc.get("x"));

        e = JEXL.createScript("foo : { bar : {x = 1; break foo; x = 2}; x = 3}");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, jc.get("x"));
    }

    @Test
    public void testBadBlock() throws Exception {
        try {
            JEXL.createScript("egg : {break eggs}");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : {x = function() {break egg}}");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : {break}");
            Assert.fail("Unlabelled break inside block should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : {continue egg}");
            Assert.fail("Unsupported continue on labeled block");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : {remove egg}");
            Assert.fail("Unsupported remove on labeled block");
        } catch (Exception ex) {
        }
    }

    @Test
    public void testWhile() throws Exception {
        JexlScript e = JEXL.createScript("x = 0; foo : while (x < 7) bar : while(x < 10) {x = x + 1; if (x >= 5) break foo}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, jc.get("x"));

        e = JEXL.createScript("x = 0; foo : while (x < 7) {x = x + 1; if (x >= 5) break}");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, jc.get("x"));
    }

    @Test
    public void testBadWhile() throws Exception {
        try {
            JEXL.createScript("egg : while(true) break eggs");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : while(true) x = function() {break egg}");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : while(true) remove egg");
            Assert.fail("Unsupported remove on labeled while");
        } catch (Exception ex) {
        }
    }

    @Test
    public void testDoWhile() throws Exception {
        JexlScript e = JEXL.createScript("x = 0; foo : do { bar : do {x = x + 1; if (x >= 5) break foo} while(x < 10)} while (x < 7)");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, jc.get("x"));

        e = JEXL.createScript("x = 0; foo : do { x = x + 1; if (x >= 5) break} while (x < 7)");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, jc.get("x"));
    }

    @Test
    public void testBadDoWhile() throws Exception {
        try {
            JEXL.createScript("egg : do { break eggs } while(true)");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : do { x = function() {break egg}} while(true)");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : do { remove egg } while(true)");
            Assert.fail("Unsupported remove on labeled do-while");
        } catch (Exception ex) {
        }
    }

    @Test
    public void testSwitch() throws Exception {
        JexlScript e = JEXL.createScript("x = 0; foo : switch(true) { default : { bar : switch (false) { default : break foo}}; x = 1}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 0, jc.get("x"));

        e = JEXL.createScript("x = 0; foo : switch(true) { default : break; x = 1}");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 0, jc.get("x"));
    }

    @Test
    public void testBadSwitch() throws Exception {
        try {
            JEXL.createScript("egg : switch(true) {default : break eggs}");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : switch(true) {default : x = function() {break egg}}");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : switch(true) {default : continue egg}");
            Assert.fail("Unsupported continue on labeled switch");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : switch(true) {default : remove egg}");
            Assert.fail("Unsupported remove on labeled switch");
        } catch (Exception ex) {
        }
    }

    @Test
    public void testFor() throws Exception {
        JexlScript e = JEXL.createScript("x = 0; foo : for (; x < 7; ) bar : for (; x < 10; ) {x = x + 1; if (x >= 5) break foo}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, jc.get("x"));

        e = JEXL.createScript("x = 0; foo : for (; x < 7; ) {x = x + 1; if (x >= 5) break}");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, jc.get("x"));
    }

    @Test
    public void testBadFor() throws Exception {
        try {
            JEXL.createScript("egg : for(;;) break eggs");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : for(;;) x = function() {break egg}");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : for(;;) remove egg");
            Assert.fail("Unsupported remove on labeled for");
        } catch (Exception ex) {
        }
    }

    @Test
    public void testForeach() throws Exception {
        JexlScript e = JEXL.createScript("x = 0; foo : for (var i : 1..5) bar : for (var y : 0..10) {x = x + 1; if (x >= 5) break foo}");
        JexlContext jc = new MapContext();
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, jc.get("x"));

        e = JEXL.createScript("x = 0; foo : for (var i : 1..5) {x = x + 1; if (x >= 5) break}");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 5, jc.get("x"));
    }

    @Test
    public void testBadForeach() throws Exception {
        try {
            JEXL.createScript("egg : for(var i : 1..5) break eggs");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
        try {
            JEXL.createScript("egg : for(var i : 1..5) x = function() {break egg}");
            Assert.fail("Undeclared break label should not be allowed");
        } catch (Exception ex) {
        }
    }

    @Test
    public void testRepeatedLabeld() throws Exception {
        try {
            JEXL.createScript("egg : { egg : {} }");
            Assert.fail("Already declared labels should not be allowed");
        } catch (Exception ex) {
        }
    }

}
