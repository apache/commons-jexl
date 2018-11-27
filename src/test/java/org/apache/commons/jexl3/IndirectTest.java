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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests indirect reference operator.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class IndirectTest extends JexlTestCase {

    public IndirectTest() {
        super("IndirectTest");
    }

    @Test
    public void testValue() throws Exception {
        JexlScript e = JEXL.createScript("*x");
        JexlContext jc = new MapContext();
        jc.set("x", new AtomicInteger(42));
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    public class DuckTypedRef {
        public int get() {
            return 42;
        }
    }

    @Test
    public void testDuckTypedReference() throws Exception {
        JexlScript e = JEXL.createScript("*x");
        JexlContext jc = new MapContext();
        jc.set("x", new DuckTypedRef());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    public class OverloadedRef {
        public int value() {
            return 42;
        }
    }

    public static class IndirectArithmetic extends JexlArithmetic {
        IndirectArithmetic(boolean flag) {
            super(flag);
        }

        public Object indirect(OverloadedRef f) {
            return f.value();
        }
    }

    @Test
    public void testOverloadedReference() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new IndirectArithmetic(true)).create();
        JexlScript e = jexl.createScript("*x");
        JexlContext jc = new MapContext();
        jc.set("x", new OverloadedRef());
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
    }

    @Test
    public void testStrictNullDereference() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).create();
        JexlScript e = jexl.createScript("*x");
        JexlContext jc = new MapContext();
        try {
            Object o = e.execute(jc);
            Assert.fail("Should have failed");
        } catch (Exception ex) {
            ///
        }
        jexl = new JexlBuilder().strict(false).create();
        e = jexl.createScript("*x");
        Object o = e.execute(jc);
        Assert.assertNull(o);
    }

}
