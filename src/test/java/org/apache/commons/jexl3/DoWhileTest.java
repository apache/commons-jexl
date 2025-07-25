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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests do while statement.
 */
@SuppressWarnings({ "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes" })
class DoWhileTest extends JexlTestCase {

    public DoWhileTest() {
        super("DoWhileTest");
    }

    @Test
    void testEmptyBody() throws Exception {
        final JexlScript e = JEXL.createScript("var i = 0; do ; while((i+=1) < 10); i");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals(10, o);
    }

    @Test
    void testEmptyStmtBody() throws Exception {
        final JexlScript e = JEXL.createScript("var i = 0; do {} while((i+=1) < 10); i");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals(10, o);
    }

    @Test
    void testForEachBreakInsideFunction() throws Exception {
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class, () -> JEXL.createScript("for (i : 1..2) {  y = function() { break; } }"),
                "break is out of loop!");
        final String str = xparse.detailedMessage();
        assertTrue(str.contains("break"));
    }

    @Test
    void testForEachContinueInsideFunction() throws Exception {
        final JexlException.Parsing xparse = assertThrows(JexlException.Parsing.class,
                () -> JEXL.createScript("for (i : 1..2) {  y = function() { continue; } }"), "break is out of loop!");
        final String str = xparse.detailedMessage();
        assertTrue(str.contains("continue"));
    }

    @Test
    void testForEachLambda() throws Exception {
        final JexlScript e = JEXL.createScript("(x)->{ for (i : 1..2) {  continue; var y = function() { 42; } break; } }");
        assertNotNull(e);
    }

    @Test
    void testForLoop0() {
        final String src = "(l)->{ for(let x = 0; x < 4; ++x) { l.add(x); } }";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(src);
        final List<Integer> l = new ArrayList<>();
        final Object result = script.execute(null, l);
        assertNotNull(result);
        assertEquals(Arrays.asList(0, 1, 2, 3), l);
    }

    @Test
    void testForLoop1() {
        final String src = "(l)->{ for(var x = 0; x < 4; ++x) { l.add(x); } }";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(src);
        final List<Integer> l = new ArrayList<>();
        final Object result = script.execute(null, l);
        assertNotNull(result);
        assertEquals(Arrays.asList(0, 1, 2, 3), l);
    }

    @Test
    void testForLoop2() {
        final String src = "(l)->{ for(x = 0; x < 4; ++x) { l.add(x); } }";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(src);
        final List<Integer> l = new ArrayList<>();
        final JexlContext ctxt = new MapContext();
        final Object result = script.execute(ctxt, l);
        assertNotNull(result);
        assertEquals(Arrays.asList(0, 1, 2, 3), l);
    }

    @Test
    void testSimpleWhileFalse() throws Exception {
        JexlScript e = JEXL.createScript("do {} while (false)");
        final JexlContext jc = new MapContext();

        Object o = e.execute(jc);
        assertNull(o);
        e = JEXL.createScript("do {} while (false); 23");
        o = e.execute(jc);
        assertEquals(23, o);

    }

    @Test
    void testWhileEmptyBody() throws Exception {
        final JexlScript e = JEXL.createScript("var i = 0; while((i+=1) < 10); i");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals(10, o);
    }

    @Test
    void testWhileEmptyStmtBody() throws Exception {
        final JexlScript e = JEXL.createScript("var i = 0; while((i+=1) < 10) {}; i");
        final JexlContext jc = new MapContext();
        final Object o = e.execute(jc);
        assertEquals(10, o);
    }

    @Test
    void testWhileExecutesExpressionWhenLooping() throws Exception {
        JexlScript e = JEXL.createScript("do x = x + 1 while (x < 10)");
        final JexlContext jc = new MapContext();
        jc.set("x", 1);

        Object o = e.execute(jc);
        assertEquals(10, o);
        assertEquals(10, jc.get("x"));

        e = JEXL.createScript("var x = 0; do x += 1; while (x < 23)");
        o = e.execute(jc);
        assertEquals(23, o);

        jc.set("x", 1);
        e = JEXL.createScript("do x += 1; while (x < 23); return 42;");
        o = e.execute(jc);
        assertEquals(23, jc.get("x"));
        assertEquals(42, o);
    }

    @Test
    void testWhileWithBlock() throws Exception {
        final JexlScript e = JEXL.createScript("do { x = x + 1; y = y * 2; } while (x < 10)");
        final JexlContext jc = new MapContext();
        jc.set("x", Integer.valueOf(1));
        jc.set("y", Integer.valueOf(1));

        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(512), o, "Result is wrong");
        assertEquals(Integer.valueOf(10), jc.get("x"), "x is wrong");
        assertEquals(Integer.valueOf(512), jc.get("y"), "y is wrong");
    }
}
