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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for map literals
 * @since 1.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class MapLiteralTest extends JexlTestCase {
    public MapLiteralTest() {
        super("MapLiteralTest");
    }

    @Test
    public void testLiteralWithStrings() throws Exception {
        JexlExpression e = JEXL.createExpression("{ 'foo' : 'bar' }");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals(Collections.singletonMap("foo", "bar"), o);
    }

    @Test
    public void testLiteralWithMultipleEntries() throws Exception {
        JexlExpression e = JEXL.createExpression("{ 'foo' : 'bar', 'eat' : 'food' }");
        JexlContext jc = new MapContext();

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("foo", "bar");
        expected.put("eat", "food");

        Object o = e.evaluate(jc);
        Assert.assertEquals(expected, o);
    }

    @Test
    public void testLiteralWithNumbers() throws Exception {
        JexlExpression e = JEXL.createExpression("{ 5 : 10 }");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals(Collections.singletonMap(new Integer(5), new Integer(10)), o);

        e = JEXL.createExpression("m = { 3 : 30, 4 : 40, 5 : 'fifty', '7' : 'seven', 7 : 'SEVEN' }");
        e.evaluate(jc);

        e = JEXL.createExpression("m.3");
        o = e.evaluate(jc);
        Assert.assertEquals(new Integer(30), o);

        e = JEXL.createExpression("m[4]");
        o = e.evaluate(jc);
        Assert.assertEquals(new Integer(40), o);

        jc.set("i", Integer.valueOf(5));
        e = JEXL.createExpression("m[i]");
        o = e.evaluate(jc);
        Assert.assertEquals("fifty", o);

        e = JEXL.createExpression("m.3 = 'thirty'");
        e.evaluate(jc);
        e = JEXL.createExpression("m.3");
        o = e.evaluate(jc);
        Assert.assertEquals("thirty", o);

        e = JEXL.createExpression("m['7']");
        o = e.evaluate(jc);
        Assert.assertEquals("seven", o);

        e = JEXL.createExpression("m.7");
        o = e.evaluate(jc);
        Assert.assertEquals("SEVEN", o);

        jc.set("k", Integer.valueOf(7));
        e = JEXL.createExpression("m[k]");
        o = e.evaluate(jc);
        Assert.assertEquals("SEVEN", o);

        jc.set("k", "7");
        e = JEXL.createExpression("m[k]");
        o = e.evaluate(jc);
        Assert.assertEquals("seven", o);
    }

    @Test
    public void testSizeOfSimpleMapLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("size({ 'foo' : 'bar' })");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals(new Integer(1), o);
    }

    @Test
    public void testCallingMethodsOnNewMapLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("size({ 'foo' : 'bar' }.values())");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals(new Integer(1), o);
    }

    @Test
    public void testNotEmptySimpleMapLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("empty({ 'foo' : 'bar' })");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertFalse(((Boolean) o).booleanValue());
    }

    @Test
    public void testMapMapLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("{'foo' : { 'inner' : 'bar' }}");
        JexlContext jc = new MapContext();
        Object o = e.evaluate(jc);
        Assert.assertNotNull(o);

        jc.set("outer", o);
        e = JEXL.createExpression("outer.foo.inner");
        o = e.evaluate(jc);
        Assert.assertEquals("bar", o);
    }

    @Test
    public void testMapArrayLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("{'foo' : [ 'inner' , 'bar' ]}");
        JexlContext jc = new MapContext();
        Object o = e.evaluate(jc);
        Assert.assertNotNull(o);

        jc.set("outer", o);
        e = JEXL.createExpression("outer.foo.1");
        o = e.evaluate(jc);
        Assert.assertEquals("bar", o);
    }

    @Test
    public void testEmptyMap() throws Exception {
        JexlScript script = JEXL.createScript("map['']", "map");
        Object result = script.execute(null, Collections.singletonMap("", 42));
        Assert.assertEquals(42, result);
    }

    @Test
    public void testVariableMap() throws Exception {
        JexlScript script = JEXL.createScript("{ ['1', '2'.toString()] : someValue }", "someValue");
        Object result = script.execute(null, 42);
        Assert.assertTrue(result instanceof Map);
        Object key = null;
        Object value = null;
        for(Map.Entry<?,?> e : ((Map<?,?>) result).entrySet()) {
            key = e.getKey();
            value = e.getValue();
            break;
        }
        Object gg = ((Map) result).get(key);
        Assert.assertEquals(42, ((Number) gg).intValue());
        Assert.assertEquals(value, ((Number) gg).intValue());
    }
}
