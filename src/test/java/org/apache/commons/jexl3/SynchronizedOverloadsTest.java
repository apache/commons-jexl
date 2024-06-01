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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test cases for synchronized calls.
 * <p>May be a base for synchronized calls.
 */
@SuppressWarnings({"boxing", "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class SynchronizedOverloadsTest extends JexlTestCase {
    public SynchronizedOverloadsTest() {
        super("SynchronizedOverloadsTest", null);
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    @Test
    public void testSynchronized() throws Exception {
        final Map<String, Object> ns = new TreeMap<>();
        final JexlContext jc = new SynchronizedContext(new MapContext());
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        final JexlScript js0 = jexl.createScript("@synchronized(y) {return y.size(); }", "y");
        final Object size = js0.execute(jc, "foobar");
        assertEquals(6, size);
    }

    @Test
    public void testSynchronizer() throws Exception {
        final Map<String, Object> ns = new TreeMap<>();
        ns.put("synchronized", SynchronizedContext.class);
        final JexlContext jc = new MapContext();
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        final JexlScript js0 = jexl.createScript("synchronized:call(x, (y)->{y.size()})", "x");
        final Object size = js0.execute(jc, "foobar");
        assertEquals(6, size);
    }

    @Test
    public void testUnsafeMonitor() throws Exception {
        final SynchronizedArithmetic.AbstractMonitor abstractMonitor = new SynchronizedArithmetic.SafeMonitor();
        final Map<String, Object> foo = new TreeMap<>();
        foo.put("one", 1);
        foo.put("two", 2);
        foo.put("three", 3);
        final JexlContext jc = new SynchronizedContext(new MapContext());
        final JexlEngine jexl = new JexlBuilder().arithmetic(new SynchronizedArithmetic(abstractMonitor, true)).create();
        final JexlScript js0 = jexl.createScript("x['four'] = 4; var t = 0.0; for(var z: x) { t += z; }; call(t, (y)->{return y});", "x");
        Object t = js0.execute(jc, foo);
        assertEquals(10.0d, t);
        assertTrue(abstractMonitor.isBalanced());
        assertEquals(2, abstractMonitor.getCount());
        t = js0.execute(jc, foo);
        assertEquals(10.0d, t);
        assertTrue(abstractMonitor.isBalanced());
        assertEquals(4, abstractMonitor.getCount());
    }
}
