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
import org.apache.commons.jexl3.internal.introspection.IndexedType;
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

    @Test
    public void testPropertyProperty() throws Exception {
        Integer i42 = Integer.valueOf(42);
        Integer i43 = Integer.valueOf(43);
        String s42 = "fourty-two";
        Object[] foo = new Object[3];
        foo[0] = foo;
        foo[1] = i42;
        foo[2] = s42;
        asserter.setVariable("foo", foo);
        asserter.setVariable("zero", Integer.valueOf(0));
        asserter.setVariable("one", Integer.valueOf(1));
        asserter.setVariable("two", Integer.valueOf(2));
        for (int l = 0; l < 2; ++l) {
            asserter.assertExpression("foo.0", foo);
            asserter.assertExpression("foo.0.'0'", foo);
            asserter.assertExpression("foo.'1'", foo[1]);
            asserter.assertExpression("foo.0.'1'", foo[1]);
            asserter.assertExpression("foo.0.'1' = 43", i43);
            asserter.assertExpression("foo.0.'1'", i43);
            asserter.assertExpression("foo.0.'1' = 42", i42);
            //
            asserter.assertExpression("foo?.0.'1'", i42);
            asserter.assertExpression("foo?.0", foo);
            asserter.assertExpression("foo?.0.'0'", foo);
            asserter.assertExpression("foo?.'1'", foo[1]);
            asserter.assertExpression("foo.0?.'1'", foo[1]);
            asserter.assertExpression("foo?.0.'1' = 43", i43);
            asserter.assertExpression("foo?.0?.'1'", i43);
            asserter.assertExpression("foo?.0.'1' = 42", i42);
            asserter.assertExpression("foo?.0.'1'", i42);
            //
            asserter.assertExpression("foo?.0.`1`", i42);
            asserter.assertExpression("foo?.0", foo);
            asserter.assertExpression("foo?.0.'0'", foo);
            asserter.assertExpression("foo?.`1`", foo[1]);
            asserter.assertExpression("foo?.0.`1`", foo[1]);
            asserter.assertExpression("foo?.0.`${one}` = 43", i43);
            asserter.assertExpression("foo.0?.`${one}`", i43);
            asserter.assertExpression("foo.0.`${one}` = 42", i42);
            asserter.assertExpression("foo?.0?.`${one}`", i42);
        }
    }

    /**
     * A base property container; can only set from string.
     */
    public static class PropertyContainer {
        String value0;
        int value1;

        public PropertyContainer(String name, int number) {
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

        public void setProperty(String name, String value) {
            if ("name".equals(name)) {
                this.value0 = value.toUpperCase();
            }
            if ("number".equals(name)) {
                this.value1 = Integer.parseInt(value) + 1000;
            }
        }
    }


    /**
     * Overloads propertySet.
     */
    public static class PropertyArithmetic extends JexlArithmetic {
        int ncalls = 0;

        public PropertyArithmetic(boolean astrict) {
            super(astrict);
        }

        public Object propertySet(IndexedType.IndexedContainer map, String key, Integer value) {
            if (map.getContainerClass().equals(PropertyContainer.class)
                && map.getContainerName().equals("property")) {
                try {
                    map.set(key, value.toString());
                    ncalls += 1;
                } catch (Exception xany) {
                    throw new JexlException.Operator(null, key + "." + value.toString(), xany);
                }
                return null;
            }
            return JexlEngine.TRY_FAILED;
        }

        public int getCalls() {
            return ncalls;
        }
    }

    @Test
    public void testInnerViaArithmetic() throws Exception {
        PropertyArithmetic pa = new PropertyArithmetic(true);
        JexlEngine jexl = new JexlBuilder().arithmetic(pa).debug(true).strict(true).cache(32).create();
        PropertyContainer quux = new PropertyContainer("bar", 169);
        Object result;

        JexlScript getName = jexl.createScript("foo.property.name", "foo");
        result = getName.execute(null, quux);
        Assert.assertEquals("bar", result);
        int calls = pa.getCalls();
        JexlScript setName = jexl.createScript("foo.property.name = $0", "foo", "$0");
        setName.execute(null, quux, 123);
        result = getName.execute(null, quux);
        Assert.assertEquals("123", result);
        setName.execute(null, quux, 456);
        result = getName.execute(null, quux);
        Assert.assertEquals("456", result);
        Assert.assertEquals(calls + 2, pa.getCalls());
        setName.execute(null, quux, "quux");
        result = getName.execute(null, quux);
        Assert.assertEquals("QUUX", result);
        Assert.assertEquals(calls + 2, pa.getCalls());

        JexlScript getNumber = jexl.createScript("foo.property.number", "foo");
        result = getNumber.execute(null, quux);
        Assert.assertEquals(169, result);
        JexlScript setNumber = jexl.createScript("foo.property.number = $0", "foo", "$0");
        setNumber.execute(null, quux, 42);
        result = getNumber.execute(null, quux);
        Assert.assertEquals(1042, result);
        setNumber.execute(null, quux, 24);
        result = getNumber.execute(null, quux);
        Assert.assertEquals(1024, result);
        Assert.assertEquals(calls + 4, pa.getCalls());
        setNumber.execute(null, quux, "42");
        result = getNumber.execute(null, quux);
        Assert.assertEquals(1042, result);
        Assert.assertEquals(calls + 4, pa.getCalls());
    }

    public static class Container extends PropertyContainer {
        public Container(String name, int number) {
            super(name, number);
        }

        public Object getProperty(int ref) {
            switch (ref) {
                case 0:
                    return value0;
                case 1:
                    return value1;
                default:
                    return null;
            }
        }

        @Override
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

    @Test
    public void testInnerProperty() throws Exception {
        PropertyArithmetic pa = new PropertyArithmetic(true);
        JexlEngine jexl = new JexlBuilder().arithmetic(pa).debug(true).strict(true).cache(32).create();
        Container quux = new Container("quux", 42);
        JexlScript get;
        Object result;

        int calls = pa.getCalls();
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

        Assert.assertEquals(calls, pa.getCalls());
    }


    @Test
    public void testStringIdentifier() throws Exception {
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

    @Test
    public void testErroneousIdentifier() throws Exception {
        MapContext ctx = new MapContext();
        JexlEngine engine = new JexlBuilder().strict(true).silent(false).create();

        // base succeeds
        String stmt = "(x)->{ x?.class ?? 'oops' }";
        JexlScript script = engine.createScript(stmt);
        Object result = script.execute(ctx, "querty");
        Assert.assertEquals("querty".getClass(), result);

        // fail with unknown property
        stmt = "(x)->{ x.class1 ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        Assert.assertEquals("oops", result);

        // succeeds with jxlt & strict navigation
        ctx.set("al", "la");
        stmt = "(x)->{ x.`c${al}ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        Assert.assertEquals("querty".getClass(), result);

        // succeeds with jxlt & lenient navigation
        stmt = "(x)->{ x?.`c${al}ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        Assert.assertEquals("querty".getClass(), result);

        // fails with jxlt & lenient navigation
        stmt = "(x)->{ x?.`c${la}ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        Assert.assertEquals("oops", result);

        // fails with jxlt & strict navigation
        stmt = "(x)->{ x.`c${la}ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        Assert.assertEquals("oops", result);

        // fails with jxlt & lenient navigation
        stmt = "(x)->{ x?.`c${la--ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        Assert.assertEquals("oops", result);

        // fails with jxlt & strict navigation
        stmt = "(x)->{ x.`c${la--ss` ?? 'oops' }";
        script = engine.createScript(stmt);
        result = script.execute(ctx, "querty");
        Assert.assertEquals("oops", result);
    }

    @Test
    public void test250() throws Exception {
        MapContext ctx = new MapContext();
        HashMap<Object, Object> x = new HashMap<Object, Object>();
        x.put(2, "123456789");
        ctx.set("x", x);
        JexlEngine engine = new JexlBuilder().strict(true).silent(false).create();
        String stmt = "x.2.class.name";
        JexlScript script = engine.createScript(stmt);
        Object result = script.execute(ctx);
        Assert.assertEquals("java.lang.String", result);

        try {
            stmt = "x.3?.class.name";
            script = engine.createScript(stmt);
            result = script.execute(ctx);
            Assert.assertNull(result);
        } catch (JexlException xany) {
            Assert.fail("Should have evaluated to null");
        }
        try {
            stmt = "x?.3.class.name";
            script = engine.createScript(stmt);
            result = script.execute(ctx);
            Assert.fail("Should have thrown, fail on 3");
            Assert.assertNull(result);
        } catch (JexlException xany) {
            Assert.assertTrue(xany.detailedMessage().contains("3"));
        }
        try {
            stmt = "x?.3?.class.name";
            script = engine.createScript(stmt);
            result = script.execute(ctx);
            Assert.assertNull(result);
        } catch (JexlException xany) {
            Assert.fail("Should have evaluated to null");
        }
        try {
            stmt = "y?.3.class.name";
            script = engine.createScript(stmt);
            result = script.execute(ctx);
            Assert.assertNull(result);
        } catch (JexlException xany) {
            Assert.fail("Should have evaluated to null");
        }
        try {
            stmt = "x?.y?.z";
            script = engine.createScript(stmt);
            result = script.execute(ctx);
            Assert.assertNull(result);
        } catch (JexlException xany) {
            Assert.fail("Should have evaluated to null");
        }
        try {
            stmt = "x? (x.y? (x.y.z ?: null) :null) : null";
            script = engine.createScript(stmt);
            result = script.execute(ctx);
            Assert.assertNull(result);
        } catch (JexlException xany) {
            Assert.fail("Should have evaluated to null");
        }
    }

    public static class Prompt {
        private final Map<String, PromptValue> values = new HashMap<String, PromptValue>();

        public Object get(String name) {
            PromptValue v = values.get(name);
            return v != null? v.getValue() : null;
        }

        public void set(String name, Object value) {
            values.put(name, new PromptValue(value));
        }
    }

    /**
     * A valued prompt.
     */
    public static class PromptValue {

        /** Prompt value. */
        private Object value;

        public PromptValue(Object v) {
           value = v;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    @Test
    public void test275a() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).safe(false).create();
        JexlContext ctxt = new MapContext();
        JexlScript script;
        Object result = null;
        Prompt p0 = new Prompt();
        p0.set("stuff", 42);
        ctxt.set("$in", p0);

        // unprotected navigation
        script = jexl.createScript("$in[p].intValue()", "p");
        try {
            result = script.execute(ctxt, "fail");
            Assert.fail("should have thrown a " + JexlException.Property.class);
        } catch (JexlException xany) {
            Assert.assertEquals(JexlException.Property.class, xany.getClass());
        }
        Assert.assertEquals(null, result);
        result = script.execute(ctxt, "stuff");
        Assert.assertEquals(42, result);

        // protected navigation
        script = jexl.createScript("$in[p]?.intValue()", "p");
        result = script.execute(ctxt, "fail");
        Assert.assertEquals(null, result);
        result = script.execute(ctxt, "stuff");
        Assert.assertEquals(42, result);

        // unprotected navigation
        script = jexl.createScript("$in.`${p}`.intValue()", "p");
        try {
            result = script.execute(ctxt, "fail");
            Assert.fail("should have thrown a " + JexlException.Property.class);
        } catch (JexlException xany) {
            Assert.assertEquals(JexlException.Property.class, xany.getClass());
        }
        result = script.execute(ctxt, "stuff");
        Assert.assertEquals(42, result);

        // protected navigation
        script = jexl.createScript("$in.`${p}`?.intValue()", "p");
        result = script.execute(ctxt, "fail");
        Assert.assertEquals(null, result);
        result = script.execute(ctxt, "stuff");
        Assert.assertEquals(42, result);

    }
     @Test
    public void test275b() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).safe(true).create();
        JexlContext ctxt = new MapContext();
        JexlScript script;
        Object result = null;
        Prompt p0 = new Prompt();
        p0.set("stuff", 42);
        ctxt.set("$in", p0);

        // unprotected navigation
        script = jexl.createScript("$in[p].intValue()", "p");
            result = script.execute(ctxt, "fail");
        Assert.assertEquals(null, result);

        result = script.execute(ctxt, "stuff");
        Assert.assertEquals(42, result);


        // unprotected navigation
        script = jexl.createScript("$in.`${p}`.intValue()", "p");
        result = script.execute(ctxt, "fail");
        Assert.assertEquals(null, result);
        result = script.execute(ctxt, "stuff");
        Assert.assertEquals(42, result);

        // protected navigation
        script = jexl.createScript("$in.`${p}`?.intValue()", "p");
        result = script.execute(ctxt, "fail");
        Assert.assertEquals(null, result);
        result = script.execute(ctxt, "stuff");
        Assert.assertEquals(42, result);

    }

}