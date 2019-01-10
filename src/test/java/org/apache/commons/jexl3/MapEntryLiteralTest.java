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
import java.util.AbstractMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for map entry literals
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class MapEntryLiteralTest extends JexlTestCase {
    public MapEntryLiteralTest() {
        super("MapEntryLiteralTest");
    }

    @Test
    public void testLiteralWithStrings() throws Exception {
        JexlExpression e = JEXL.createExpression("[ 'foo' : 'bar' ]");
        JexlContext jc = new MapContext();

        Object o = e.evaluate(jc);
        Assert.assertEquals(Boolean.TRUE, o instanceof Map.Entry);
        Assert.assertEquals("foo", ((Map.Entry)o).getKey());
        Assert.assertEquals("bar", ((Map.Entry)o).getValue());
    }

}
