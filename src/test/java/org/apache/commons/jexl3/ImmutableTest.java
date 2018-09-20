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
 * Tests for immutable array/set/map literals.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ImmutableTest extends JexlTestCase {

    public ImmutableTest() {
        super("ImmutableTest");
    }

    @Test
    public void testImmutableArrayLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = #[1, 2, 3]").execute(jc);
        Assert.assertTrue(o instanceof List<?>);
        Assert.assertEquals(3, ((List<?>) o).size());

        try {
           o = JEXL.createScript("var x = #[1, 2, 3]; x.remove(1)").execute(jc);
           Assert.fail("should have failed");
        } catch(JexlException xjexl) {
            //
        }
    }

    @Test
    public void testImmutableSetLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = #{1, 2, 3}").execute(jc);
        Assert.assertTrue(o instanceof Set);
        Assert.assertEquals(3, ((Set) o).size());

        try {
           o = JEXL.createScript("var x = #{1, 2, 3}; x.remove(1)").execute(jc);
           Assert.fail("should have failed");
        } catch(JexlException xjexl) {
            //
        }
    }

    @Test
    public void testImmutableMapLiteral() throws Exception {
        JexlContext jc = new MapContext();
        Object o = JEXL.createScript("var x = #{1:1, 2:2, 3:3}").execute(jc);
        Assert.assertTrue(o instanceof Map);
        Assert.assertEquals(3, ((Map) o).size());

        try {
           o = JEXL.createScript("var x = #{1:1, 2:2, 3:3}; x.remove(1)").execute(jc);
           Assert.fail("should have failed");
        } catch(JexlException xjexl) {
            //
        }
    }

}
