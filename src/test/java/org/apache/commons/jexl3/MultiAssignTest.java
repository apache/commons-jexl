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
 * Test cases for multiple assignment.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class MultiAssignTest extends JexlTestCase {

    public static class Quux {
        String str;
        int value;
        public Quux(String str, int value) {
            this.str = str;
            this.value = value;
        }
        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public void setValue(int v) {
            value = v;
        }
        public int getValue() {
            return value;
        }
    }

    public MultiAssignTest() {
        super("MultiAssignTest", new JexlBuilder().cache(512).strict(true).silent(false).create());
    }

    @Test
    public void testArray() throws Exception {
        JexlScript assign = JEXL.createScript("(x,y) = [40,2,6]");
        JexlContext jc = new MapContext();
        jc.set("x", 10);
        jc.set("y", 20);
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not 40", new Integer(40), jc.get("x"));
        Assert.assertEquals("Result is not 2", new Integer(2), jc.get("y"));
    }

    @Test
    public void testMap() throws Exception {
        JexlScript assign = JEXL.createScript("(x,y) = {'z':22,'x':40,'y':2}");
        JexlContext jc = new MapContext();
        jc.set("x", 10);
        jc.set("y", 20);
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not 40", new Integer(40), jc.get("x"));
        Assert.assertEquals("Result is not 2", new Integer(2), jc.get("y"));
    }

    @Test
    public void testObject() throws Exception {
        JexlScript assign = JEXL.createScript("(str,value) = quux");
        JexlContext jc = new MapContext();
        jc.set("x", 10);
        jc.set("y", 20);
        jc.set("quux", new Quux("bar",42));
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not bar", "bar", jc.get("str"));
        Assert.assertEquals("Result is not 42", new Integer(42), jc.get("value"));
    }

    @Test
    public void testNull() throws Exception {
        JexlScript assign = JEXL.createScript("(x,y) = null");
        JexlContext jc = new MapContext();
        jc.set("x", 10);
        jc.set("y", 20);
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not null", null, jc.get("x"));
        Assert.assertEquals("Result is not null", null, jc.get("y"));
    }

    @Test
    public void testUnderflow() throws Exception {
        JexlScript assign = JEXL.createScript("(x,y,z) = [40,2]");
        JexlContext jc = new MapContext();
        jc.set("x", 10);
        jc.set("y", 20);
        jc.set("z", 30);
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not null", null, jc.get("z"));
    }

    @Test
    public void testOverflow() throws Exception {
        JexlScript assign = JEXL.createScript("(x,y) = [40,2,1]");
        JexlContext jc = new MapContext();
        jc.set("x", 10);
        jc.set("y", 20);
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not null", new Integer(2), o);
    }

    @Test
    public void testVar() throws Exception {
        JexlScript assign = JEXL.createScript("var (x,y) = [40,2,6]; y");
        JexlContext jc = new MapContext();
        jc.set("x", 10);
        jc.set("y", 20);
        Object o = assign.execute(jc);
        Assert.assertEquals("Result is not 2", new Integer(2), o);
    }

}