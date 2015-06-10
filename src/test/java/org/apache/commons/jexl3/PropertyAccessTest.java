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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.internal.Debugger;
import org.apache.commons.jexl3.junit.Asserter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests for property access operator '.'
 * @since 3.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class PropertyAccessTest extends JexlTestCase {

    private Asserter asserter;

    public PropertyAccessTest() {
        super("PropertyAccessTest");
    }

    @Before
    @Override
    public void setUp() {
        asserter = new Asserter(JEXL);
    }


    @Test public void testPropertyProperty() throws Exception {
        Integer i42 = Integer.valueOf(42);
        Integer i43 = Integer.valueOf(43);
        String s42 = "fourty-two";
        String s43 = "fourty-three";
        Object[] foo = new Object[3];
        foo[0] = foo;
        foo[1] = i42;
        foo[2] = s42;
        asserter.setVariable("foo", foo);
        asserter.setVariable("zero", Integer.valueOf(0));
        asserter.setVariable("one", Integer.valueOf(1));
        asserter.setVariable("two", Integer.valueOf(2));
        for(int l = 0; l < 2; ++l) {
            asserter.assertExpression("foo.0", foo);
            asserter.assertExpression("foo.0.'0'", foo);
            asserter.assertExpression("foo.'1'", foo[1]);
            asserter.assertExpression("foo.0.'1'", foo[1]);
            asserter.assertExpression("foo.0.'1' = 43", i43);
            asserter.assertExpression("foo.0.'1'", i43);
            asserter.assertExpression("foo.0.'1' = 42", i42);
            asserter.assertExpression("foo.0.'1'", i42);
        }
    }

    public static class Container {
        String value0;
        int value1;

        public Container(String name, int number) {
            value0 = name;
            value1 = number;
        }

        public Object getProperty(String name) {
            if ("name".equals(name)) {
                return value0;
            } else if ("number".equals(name)) {
                return value1;
            } else {
                return null;
            }
        }

        public Object getProperty(int ref) {
            if (0 == ref) {
                return value0;
            } else if (1 == ref) {
                return value1;
            } else {
                return null;
            }
        }

        public void setProperty(String name, String value) {
            if ("name".equals(name)) {
                this.value0 = value;
            }
        }

        public void setProperty(String name, int value) {
            if ("number".equals(name)) {
                this.value1 = value;
            }
        }

        public void setProperty(int ref, String value) {
            if (0 == ref) {
                this.value0 = value;
            }
        }

        public void setProperty(int ref, int value) {
            if (1 == ref) {
                this.value1 = value;
            }
        }
    }

    @Test public void testInnerProperty() throws Exception {
        Container quux = new Container("quux", 42);
        JexlScript get;
        Object result;

        JexlScript getName = JEXL.createScript("foo.property.name", "foo");
        result = getName.execute(null, quux);
        Assert.assertEquals("quux", result);

        JexlScript get0 = JEXL.createScript("foo.property.0", "foo");
        result = get0.execute(null, quux);
        Assert.assertEquals("quux", result);

        JexlScript getNumber = JEXL.createScript("foo.property.number", "foo");
        result = getNumber.execute(null, quux);
        Assert.assertEquals(42, result);

        JexlScript get1 = JEXL.createScript("foo.property.1", "foo");
        result = get1.execute(null, quux);
        Assert.assertEquals(42, result);

        JexlScript setName = JEXL.createScript("foo.property.name = $0", "foo", "$0");
        setName.execute(null, quux, "QUUX");
        result = getName.execute(null, quux);
        Assert.assertEquals("QUUX", result);
        result = get0.execute(null, quux);
        Assert.assertEquals("QUUX", result);

        JexlScript set0 = JEXL.createScript("foo.property.0 = $0", "foo", "$0");
        set0.execute(null, quux, "BAR");
        result = getName.execute(null, quux);
        Assert.assertEquals("BAR", result);
        result = get0.execute(null, quux);
        Assert.assertEquals("BAR", result);

        JexlScript setNumber = JEXL.createScript("foo.property.number = $0", "foo", "$0");
        setNumber.execute(null, quux, -42);
        result = getNumber.execute(null, quux);
        Assert.assertEquals(-42, result);
        result = get1.execute(null, quux);
        Assert.assertEquals(-42, result);

        JexlScript set1 = JEXL.createScript("foo.property.1 = $0", "foo", "$0");
        set1.execute(null, quux, 24);
        result = getNumber.execute(null, quux);
        Assert.assertEquals(24, result);
        result = get1.execute(null, quux);
        Assert.assertEquals(24, result);
    }


    @Test public void testStringIdentifier() throws Exception {
        Map<String, String> foo = new HashMap<String, String>();

        JexlContext jc = new MapContext();
        jc.set("foo", foo);
        foo.put("q u u x", "456");
        JexlExpression e = JEXL.createExpression("foo.\"q u u x\"");
        Object result = e.evaluate(jc);
        Assert.assertEquals("456", result);
        e = JEXL.createExpression("foo.'q u u x'");
        result = e.evaluate(jc);
        Assert.assertEquals("456", result);
        JexlScript s = JEXL.createScript("foo.\"q u u x\"");
        result = s.execute(jc);
        Assert.assertEquals("456", result);
        s = JEXL.createScript("foo.'q u u x'");
        result = s.execute(jc);
        Assert.assertEquals("456", result);

        Debugger dbg = new Debugger();
        dbg.debug(e);
        String dbgdata = dbg.toString();
        Assert.assertEquals("foo.'q u u x'", dbgdata);
    }
}