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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests dereferencing operator.
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
        protected int x = 42;

        public int get() {
            return x;
        }

        public void set(Integer value) {
            x = value;
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
        protected int x = 42;

        public int value() {
            return x;
        }

        public void setValue(int i) {
            x = i;
        }
    }

    public static class IndirectArithmetic extends JexlArithmetic {
        IndirectArithmetic(boolean flag) {
            super(flag);
        }

        public Object indirect(OverloadedRef f) {
            return f.value();
        }

        public void indirectAssign(OverloadedRef f, Integer value) {
            f.setValue(value);
        }
        
        public Object selfAdd(Collection<Object> c, Object value) {
            if (value != null) {
                if (value instanceof Collection<?>) {
                    c.addAll((Collection<?>)value);
                } else {
                    c.add(value);
                }
            }
            return JexlOperator.ASSIGN;
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

    @Test
    public void testDereferencedAssign() throws Exception {
        JexlEngine jexl = new JexlBuilder().arithmetic(new IndirectArithmetic(true)).create();
        JexlScript e = jexl.createScript("*x = 42");
        JexlContext jc = new MapContext();
        AtomicInteger x = new AtomicInteger(0);
        jc.set("x", x);
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
        Assert.assertEquals("Result is not expected", 42, x.get());

        e = jexl.createScript("*x = 41");
        jc = new MapContext();
        OverloadedRef xxf = new OverloadedRef();
        jc.set("x", xxf);
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 41, o);
        Assert.assertEquals("Result is not expected", 41, xxf.value());

        e = jexl.createScript("*x = 41");
        jc = new MapContext();
        DuckTypedRef xxd = new DuckTypedRef();
        jc.set("x", xxd);
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 41, o);
        Assert.assertEquals("Result is not expected", 41, xxd.get());

        e = jexl.createScript("*x += 42");
        jc = new MapContext();
        x = new AtomicInteger(0);
        jc.set("x", x);
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", 42, o);
        Assert.assertEquals("Result is not expected", 42, x.get());

        e = jexl.createScript("*x += 42");
        jc = new MapContext();
        AtomicReference xx = new AtomicReference();
        ArrayList xxv = new ArrayList();
        xx.set(xxv);
        jc.set("x", xx);
        o = e.execute(jc);
        Assert.assertEquals("Result is not expected", o, xxv);
        Assert.assertEquals("Result is not expected", 1, xxv.size());
    }

}
