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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests support of @FunctionalInterface.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class FunctionalInterfaceTest extends JexlTestCase {

    public FunctionalInterfaceTest() {
        super("FunctionalInterfaceTest");
    }

    public class TestContext implements JexlContext {

        /** The wrapped context. */
        private final JexlContext wrapped;

        private final ExecutorService srv;

        /**
         * Creates a new readonly context.
         * @param context the wrapped context
         * @param eopts the engine evaluation options
         */
        public TestContext(JexlContext context) {
            wrapped = context;
            srv = Executors.newSingleThreadExecutor();
        }

        @Override
        public Object get(String name) {
            return wrapped.get(name);
        }

        @Override
        public void set(String name, Object value) {
            wrapped.set(name, value);
        }

        @Override
        public boolean has(String name) {
            return wrapped.has(name);
        }

        public void sort(Object[] a, Comparator<Object> c) {
            Arrays.sort(a, c);
        }

        public void submit(Callable<Object> c) {
            Future f = srv.submit(c);
            // wait
            try {
                f.get();
            } catch (Exception ie) {
            }
        }
    }

    @Test
    public void testComparator() throws Exception {
        JexlContext jc = new TestContext(new MapContext());
        JexlScript e = JEXL.createScript("x = new Object[] {1,2,3,42}; sort(x, (a,b) -> {(a > b) ? -1 : (a < b) ? 1 : 0}); x[0]");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not 42", 42, o);
    }

    @Test
    public void testCallable() throws Exception {
        JexlContext jc = new TestContext(new MapContext());
        JexlScript e = JEXL.createScript("x = new Object[] {1,2,3,42}; submit(() -> {x[0] = 42}); x[0]");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not 42", 42, o);
    }

    @Test
    public void testFunction() throws Exception {
        JexlContext jc = new TestContext(new MapContext());
        JexlScript e = JEXL.createScript("x = {1:2,3:41}; x.computeIfAbsent(2, y -> {42})");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not 42", 42, o);
    }

    @Test
    public void testBiFunction() throws Exception {
        JexlContext jc = new TestContext(new MapContext());
        JexlScript e = JEXL.createScript("x = {1:2,3:41}; x.merge(3, 3, (a,b) -> {42}); x[3]");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not 42", 42, o);
    }

}
