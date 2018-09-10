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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for comprehensions operator.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ComprehensionTest extends JexlTestCase {

    public ComprehensionTest() {
        super("ComprehensionTest");
    }

    @Test
    public void testArrayComprehensionLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [1, 2, 3]; var y = [...x]").execute(jc);
        Assert.assertTrue(o instanceof int[]);
        Assert.assertEquals(3, ((int[]) o).length);

        o = JEXL.createScript("var x = null; var y = [1, 2, 3, ...x]").execute(jc);
        Assert.assertEquals(3, ((int[]) o).length);
    }

    @Test
    public void testSetComprehensionLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [1, 2, 3]; var y = {...x}").execute(jc);
        Assert.assertTrue(o instanceof Set);
        Assert.assertEquals(3, ((Set) o).size());

        o = JEXL.createScript("var x = null; var y = {1, 2, 3, ...x}").execute(jc);
        Assert.assertEquals(3, ((Set) o).size());

        o = JEXL.createScript("var x = [3, 4, 5]; var y = {1, 2, 3, ...x}").execute(jc);
        Assert.assertEquals(5, ((Set) o).size());
    }

    @Test
    public void testMapComprehensionLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = {1:1, 2:2, 3:3}; var y = {*:...x}").execute(jc);
        Assert.assertTrue(o instanceof Map);
        Assert.assertEquals(3, ((Map) o).size());

        o = JEXL.createScript("var x = null; var y = {1:1, 2:2, 3:3, *:...x}").execute(jc);
        Assert.assertEquals(3, ((Map) o).size());

        o = JEXL.createScript("var x = [3, 4, 5]; var y = {1:1, 2:2, 3:3, *:...x}").execute(jc);
        Assert.assertEquals(5, ((Map) o).size());
    }

    @Test
    public void testArgumentComprehension() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (a, b, c) -> {return a + b + c}; var y = [1, 2, 3]; x(...y)").execute(jc);
        Assert.assertEquals(6, o);
    }

    @Test
    public void testIteratorEmpty() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = [1, 2, 3]; not empty ...x").execute(jc);
        Assert.assertEquals(Boolean.TRUE, o);

        o = JEXL.createScript("var x = []; empty ...x").execute(jc);
        Assert.assertEquals(Boolean.TRUE, o);
    }

    @Test
    public void testGeneratorIterator() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = ...(0 : x -> {x < 10 ? x + 2 : null}); return [...x]").execute(jc);
        Assert.assertEquals(6, ((int[]) o).length);

        o = JEXL.createScript("var x = ...(0 : (i,x) -> {i < 10 ? x + 2 : null}); return [...x]").execute(jc);
        Assert.assertEquals(10, ((int[]) o).length);
    }

}
