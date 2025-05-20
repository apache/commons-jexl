/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests for map literals
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class MapLiteralTest extends JexlTestCase {
    public MapLiteralTest() {
        super("MapLiteralTest");
    }

    @Test
    public void testCallingMethodsOnNewMapLiteral() throws Exception {
        final JexlExpression e = JEXL.createExpression("size({ 'foo' : 'bar' }.values())");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertEquals(Integer.valueOf(1), o);
    }

    @Test
    public void testEmptyMap() throws Exception {
        final JexlScript script = JEXL.createScript("map['']", "map");
        final Object result = script.execute(null, Collections.singletonMap("", 42));
        assertEquals(42, result);
    }

    @Test
    public void testLiteralWithMultipleEntries() throws Exception {
        final Map<String, String> expected = new HashMap<>();
        expected.put("foo", "bar");
        expected.put("eat", "food");
        final List<String> sources = Arrays.asList("{ 'foo' : 'bar', 'eat' : 'food' }", "{ 'foo' : 'bar', 'eat' : 'food', }");
        for(final String src : sources) {
            final JexlExpression e = JEXL.createExpression("{ 'foo' : 'bar', 'eat' : 'food' }");
            final Object o = e.evaluate(null);
            assertEquals(expected, o);
        }
    }

    @Test
    public void testLiteralWithNumbers() throws Exception {
        JexlExpression e = JEXL.createExpression("{ 5 : 10 }");
        final JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        assertEquals(Collections.singletonMap(Integer.valueOf(5), Integer.valueOf(10)), o);

        e = JEXL.createExpression("m = { 3 : 30, 4 : 40, 5 : 'fifty', '7' : 'seven', 7 : 'SEVEN' }");
        e.evaluate(jc);

        e = JEXL.createExpression("m.3");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(30), o);

        e = JEXL.createExpression("m[4]");
        o = e.evaluate(jc);
        assertEquals(Integer.valueOf(40), o);

        jc.set("i", Integer.valueOf(5));
        e = JEXL.createExpression("m[i]");
        o = e.evaluate(jc);
        assertEquals("fifty", o);

        e = JEXL.createExpression("m.3 = 'thirty'");
        e.evaluate(jc);
        e = JEXL.createExpression("m.3");
        o = e.evaluate(jc);
        assertEquals("thirty", o);

        e = JEXL.createExpression("m['7']");
        o = e.evaluate(jc);
        assertEquals("seven", o);

        e = JEXL.createExpression("m.7");
        o = e.evaluate(jc);
        assertEquals("SEVEN", o);

        jc.set("k", Integer.valueOf(7));
        e = JEXL.createExpression("m[k]");
        o = e.evaluate(jc);
        assertEquals("SEVEN", o);

        jc.set("k", "7");
        e = JEXL.createExpression("m[k]");
        o = e.evaluate(jc);
        assertEquals("seven", o);
    }

    @Test
    public void testLiteralWithStrings() throws Exception {
        final List<String> sources = Arrays.asList("{ 'foo' : 'bar' }", "{ 'foo' : 'bar', }");
        for(final String src : sources) {
            final JexlExpression e = JEXL.createExpression(src);
            final Object o = e.evaluate(null);
            assertEquals(Collections.singletonMap("foo", "bar"), o);
        }
        assertThrows(JexlException.Parsing.class, () -> JEXL.createExpression("{  : , }"));
    }

    @Test
    public void testMapArrayLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("{'foo' : [ 'inner' , 'bar' ]}");
        final JexlContext jc = new MapContext();
        Object o = e.evaluate(jc);
        assertNotNull(o);

        jc.set("outer", o);
        e = JEXL.createExpression("outer.foo.1");
        o = e.evaluate(jc);
        assertEquals("bar", o);
    }

    @Test
    public void testMapMapLiteral() throws Exception {
        JexlExpression e = JEXL.createExpression("{'foo' : { 'inner' : 'bar' }}");
        final JexlContext jc = new MapContext();
        Object o = e.evaluate(jc);
        assertNotNull(o);

        jc.set("outer", o);
        e = JEXL.createExpression("outer.foo.inner");
        o = e.evaluate(jc);
        assertEquals("bar", o);
    }

    @Test
    public void testNotEmptySimpleMapLiteral() throws Exception {
        final JexlExpression e = JEXL.createExpression("empty({ 'foo' : 'bar' })");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertFalse((Boolean) o);
    }

    @Test
    public void testSizeOfSimpleMapLiteral() throws Exception {
        final JexlExpression e = JEXL.createExpression("size({ 'foo' : 'bar' })");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertEquals(Integer.valueOf(1), o);
    }

    @Test
    public void testVariableMap() throws Exception {
        final JexlScript script = JEXL.createScript("{ ['1', '2'.toString()] : someValue }", "someValue");
        final Object result = script.execute(null, 42);
        assertInstanceOf(Map.class, result);
        Object key = null;
        Object value = null;
        for(final Map.Entry<?,?> e : ((Map<?,?>) result).entrySet()) {
            key = e.getKey();
            value = e.getValue();
            break;
        }
        final Object gg = ((Map) result).get(key);
        assertEquals(42, ((Number) gg).intValue());
        assertEquals(value, ((Number) gg).intValue());
    }
}
