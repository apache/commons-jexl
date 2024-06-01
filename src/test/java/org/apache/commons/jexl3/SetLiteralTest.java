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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for set literals
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class SetLiteralTest extends JexlTestCase {

    private static Set<?> createSet(final Object... args) {
        return new HashSet<>(Arrays.asList(args));
    }

    public SetLiteralTest() {
        super("SetLiteralTest");
    }

    @Test
    public void testLiteralWithOneEntry() throws Exception {
        final List<String> sources = Arrays.asList("{ 'foo' }", "{ 'foo', }");
        for(final String src : sources) {
            final JexlExpression e = JEXL.createExpression(src);
            final JexlContext jc = new MapContext();

            final Object o = e.evaluate(jc);
            final Set<?> check = createSet("foo");
            assertEquals(check, o);
        }
    }

    @Test
    public void testNotEmptySimpleSetLiteral() throws Exception {
        final JexlExpression e = JEXL.createExpression("empty({ 'foo' , 'bar' })");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertFalse((Boolean) o);
    }

    @Test
    public void testSetLiteralWithNulls() throws Exception {
        final String[] exprs = {
            "{  }",
            "{ 10 }",
            "{ 10 , null }",
            "{ 10 , null , 20}",
            "{ '10' , null }",
            "{ null, '10' , 20 }"
        };
        final Set<?>[] checks = {
            Collections.emptySet(),
            createSet(Integer.valueOf(10)),
            createSet(Integer.valueOf(10), null),
            createSet(Integer.valueOf(10), null, Integer.valueOf(20)),
            createSet("10", null),
            createSet(null, "10", Integer.valueOf(20))
        };
        final JexlContext jc = new MapContext();
        for (int t = 0; t < exprs.length; ++t) {
            final JexlScript e = JEXL.createScript(exprs[t]);
            final Object o = e.execute(jc);
            assertEquals(checks[t], o, exprs[t]);
        }

    }

    @Test
    public void testSetLiteralWithNumbers() throws Exception {
        final JexlExpression e = JEXL.createExpression("{ 5.0 , 10 }");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        final Set<?> check = createSet(Double.valueOf(5.0), Integer.valueOf(10));
        assertEquals(check, o);
    }

    @Test
    public void testSetLiteralWithOneEntryBlock() throws Exception {
        final JexlScript e = JEXL.createScript("{ { 'foo' }; }");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        final Set<?> check = createSet("foo");
        assertEquals(check, o);
    }

    @Test
    public void testSetLiteralWithOneEntryScript() throws Exception {
        final JexlScript e = JEXL.createScript("{ 'foo' }");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        final Set<?> check = createSet("foo");
        assertEquals(check, o);
    }

    @Test
    public void testSetLiteralWithOneNestedSet() throws Exception {
        final JexlScript e = JEXL.createScript("{ { 'foo' } }");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        final Set<?> check = createSet(createSet("foo"));
        assertEquals(check, o);
    }

    @Test
    public void testSetLiteralWithStrings() throws Exception {
        final List<String> sources = Arrays.asList("{ 'foo', 'bar' }", "{ 'foo', 'bar', ... }", "{ 'foo', 'bar', }");
        for(final String src : sources) {
            final JexlExpression e = JEXL.createExpression(src);
            final JexlContext jc = new MapContext();

            final Object o = e.evaluate(jc);
            final Set<?> check = createSet("foo", "bar");
            assertEquals(check, o);
        }
        try {
            JEXL.createExpression("{ , }");
            fail("syntax");
        } catch(final JexlException.Parsing parsing) {
            assertNotNull(parsing);
        }
    }

    @Test
    public void testSetLiteralWithStringsScript() throws Exception {
        final JexlScript e = JEXL.createScript("{ 'foo' , 'bar' }");
        final JexlContext jc = new MapContext();

        final Object o = e.execute(jc);
        final Set<?> check = createSet("foo", "bar");
        assertEquals(check, o);
    }

    @Test
    public void testSizeOfSimpleSetLiteral() throws Exception {
        final JexlExpression e = JEXL.createExpression("size({ 'foo' , 'bar'})");
        final JexlContext jc = new MapContext();

        final Object o = e.evaluate(jc);
        assertEquals(Integer.valueOf(2), o);
    }

}
