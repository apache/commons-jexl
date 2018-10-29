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

import java.util.Map;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Test cases for synchronized calls.
 * <p>May be a base for synchronized calls.
 */
@SuppressWarnings({"boxing", "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class SynchronizedOverloadsTest extends JexlTestCase {
    public SynchronizedOverloadsTest() {
        super("SynchronizedOverloadsTest", null);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }


    @Test
    public void testSynchronizer() throws Exception {
        Map<String, Object> ns = new TreeMap<String, Object>();
        ns.put("sync", SynchronizedContext.class);
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        JexlScript js0 = jexl.createScript("sync:call(x, (y)->{y.size()})", "x");
        Object size = js0.execute(jc, "foobar");
        Assert.assertEquals(6, size);
    }

    @Test
    public void testSynchronized() throws Exception {
        Map<String, Object> ns = new TreeMap<String, Object>();
        JexlContext jc = new SynchronizedContext(new MapContext());
        JexlEngine jexl = new JexlBuilder().namespaces(ns).create();
        JexlScript js0 = jexl.createScript("@synchronized(y) {return y.size(); }", "y");
        Object size = js0.execute(jc, "foobar");
        Assert.assertEquals(6, size);
    }

    @Test
    public void testUnsafeMonitor() throws Exception {
        SynchronizedArithmetic.Monitor monitor = new SynchronizedArithmetic.UnsafeMonitor();
        Map<String, Object> foo = new TreeMap<String, Object>();
        foo.put("one", 1);
        foo.put("two", 2);
        foo.put("three", 3);
        JexlContext jc = new SynchronizedContext(new MapContext());
        JexlEngine jexl = new JexlBuilder().arithmetic(new SynchronizedArithmetic(monitor, true)).create();
        JexlScript js0 = jexl.createScript("x['four'] = 4; var t = 0.0; for(var z: x) { t += z; }; call(t, (y)->{return y});", "x");
        Object t = js0.execute(jc, foo);
        Assert.assertEquals(10.0d, t);
        Assert.assertTrue(monitor.isBalanced());
        Assert.assertEquals(2, monitor.getCount());
        t = js0.execute(jc, foo);
        Assert.assertEquals(10.0d, t);
        Assert.assertTrue(monitor.isBalanced());
        Assert.assertEquals(4, monitor.getCount());
    }
}
