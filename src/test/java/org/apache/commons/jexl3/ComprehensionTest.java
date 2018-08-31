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
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for array literals.
 * @since 2.0
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
    public void testArgumentComprehension() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = (a, b, c) -> {return a + b + c}; var y = [1, 2, 3]; x(...y)").execute(jc);
        Assert.assertEquals(6, o);
    }

}
