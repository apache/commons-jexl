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

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies cache & tryExecute
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class CacheTest extends JexlTestCase {
    // LOOPS & THREADS
    private static final int LOOPS = 4096;
    private static final int NTHREADS = 4;
    // A pseudo random mix of accessors
    private static final int[] MIX = {
        0, 0, 3, 3, 4, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 1, 1, 1, 2, 2, 2,
        3, 3, 3, 4, 4, 4, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 2, 2, 3, 3, 0
    };
    
    public CacheTest() {
        super("CacheTest", null);
    }
    private static final JexlEngine jexlCache = new JexlBuilder().cache(1024).debug(true).strict(true).create();
    private static final JexlEngine jexlNoCache = new JexlBuilder().cache(0).debug(true).strict(true).create();
    private static JexlEngine jexl = jexlCache;

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }


    @After
    @Override
    public void tearDown() throws Exception {
        debuggerCheck(jexl);
    }

    /**
     * A set of classes that define different getter/setter methods for the same properties.
     * The goal is to verify that the cached JexlPropertyGet / JexlPropertySet in the AST Nodes are indeed
     * volatile and do not generate errors even when multiple threads concurently hammer them.
     */
    public static class Cached {
        public String compute(String arg) {
            if (arg == null) {
                arg = "na";
            }
            return getClass().getSimpleName() + "@s#" + arg;
        }

        public String compute(String arg0, String arg1) {
            if (arg0 == null) {
                arg0 = "na";
            }
            if (arg1 == null) {
                arg1 = "na";
            }
            return getClass().getSimpleName() + "@s#" + arg0 + ",s#" + arg1;
        }

        public String compute(Integer arg) {
            return getClass().getSimpleName() + "@i#" + arg;
        }

        public String compute(float arg) {
            return getClass().getSimpleName() + "@f#" + arg;
        }

        public String compute(int arg0, int arg1) {
            return getClass().getSimpleName() + "@i#" + arg0 + ",i#" + arg1;
        }

        public String ambiguous(Integer arg0, int arg1) {
            return getClass().getSimpleName() + "!i#" + arg0 + ",i#" + arg1;
        }

        public String ambiguous(int arg0, Integer arg1) {
            return getClass().getSimpleName() + "!i#" + arg0 + ",i#" + arg1;
        }

        public static String COMPUTE(String arg) {
            if (arg == null) {
                arg = "na";
            }
            return "CACHED@s#" + arg;
        }

        public static String COMPUTE(String arg0, String arg1) {
            if (arg0 == null) {
                arg0 = "na";
            }
            if (arg1 == null) {
                arg1 = "na";
            }
            return "CACHED@s#" + arg0 + ",s#" + arg1;
        }

        public static String COMPUTE(int arg) {
            return "CACHED@i#" + arg;
        }

        public static String COMPUTE(int arg0, int arg1) {
            return "CACHED@i#" + arg0 + ",i#" + arg1;
        }
    }

    public static class Cached0 extends Cached {
        protected String value = "Cached0:new";
        protected Boolean flag = Boolean.FALSE;

        public Cached0() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String arg) {
            if (arg == null) {
                arg = "na";
            }
            value = "Cached0:" + arg;
        }

        public void setFlag(boolean b) {
            flag = Boolean.valueOf(b);
        }

        public boolean isFlag() {
            return flag.booleanValue();
        }
    }

    public static class Cached1 extends Cached0 {
        @Override
        public void setValue(String arg) {
            if (arg == null) {
                arg = "na";
            }
            value = "Cached1:" + arg;
        }
    }

    public static class Cached2 extends Cached {
        boolean flag = false;
        protected String value;

        public Cached2() {
            value = "Cached2:new";
        }

        public Object get(String prop) {
            if ("value".equals(prop)) {
                return value;
            } else if ("flag".equals(prop)) {
                return Boolean.valueOf(flag);
            }
            throw new RuntimeException("no such property");
        }

        public void set(String p, Object v) {
            if (v == null) {
                v = "na";
            }
            if ("value".equals(p)) {
                value = getClass().getSimpleName() + ":" + v;
            } else if ("flag".equals(p)) {
                flag = Boolean.parseBoolean(v.toString());
            } else {
                throw new RuntimeException("no such property");
            }
        }
    }

    public static class Cached3 extends java.util.TreeMap<String, Object> {
        private static final long serialVersionUID = 1L;
        boolean flag = false;

        public Cached3() {
            put("value", "Cached3:new");
            put("flag", "false");
        }

        @Override
        public Object get(Object key) {
            return super.get(key.toString());
        }

        @Override
        public final Object put(String key, Object arg) {
            if (arg == null) {
                arg = "na";
            }
            arg = "Cached3:" + arg;
            return super.put(key, arg);
        }

        public void setflag(boolean b) {
            flag = b;
        }

        public boolean isflag() {
            return flag;
        }
    }

    public static class Cached4 extends java.util.ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public Cached4() {
            super.add("Cached4:new");
            super.add("false");
        }

        public String getValue() {
            return super.get(0);
        }

        public void setValue(String arg) {
            if (arg == null) {
                arg = "na";
            }
            super.set(0, "Cached4:" + arg);
        }

        public void setflag(Boolean b) {
            super.set(1, b.toString());
        }

        public boolean isflag() {
            return Boolean.parseBoolean(super.get(1));
        }
    }

    /**
     * A helper class to pass arguments in tests (instances of getter/setter exercising classes).
     */
    static class TestCacheArguments {
        Cached0 c0 = new Cached0();
        Cached1 c1 = new Cached1();
        Cached2 c2 = new Cached2();
        Cached3 c3 = new Cached3();
        Cached4 c4 = new Cached4();
        Object[] ca = {
            c0, c1, c2, c3, c4
        };
        Object[] value = null;
    }

    /**
     * Run same test function in NTHREADS in parallel.
     * @param ctask the task / test
     * @param loops number of loops to perform
     * @param cache whether jexl cache is used or not
     * @throws Exception if anything goes wrong
     */
    @SuppressWarnings("boxing")
    void runThreaded(Class<? extends Task> ctask, int loops, boolean cache) throws Exception {
        if (loops == 0) {
            loops = MIX.length;
        }
        if (!cache) {
            jexl = jexlNoCache;
        } else {
            jexl = jexlCache;
        }
        java.util.concurrent.ExecutorService execs = java.util.concurrent.Executors.newFixedThreadPool(NTHREADS);
        List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>(NTHREADS);
        for (int t = 0; t < NTHREADS; ++t) {
            tasks.add(jexl.newInstance(ctask, loops));
        }
        // let's not wait for more than a minute
        List<Future<Integer>> futures = execs.invokeAll(tasks, 60, TimeUnit.SECONDS);
        // check that all returned loops
        for (Future<Integer> future : futures) {
            Assert.assertEquals(Integer.valueOf(loops), future.get());
        }
    }

    /**
     * The base class for MT tests.
     */
    public abstract static class Task implements Callable<Integer> {
        final TestCacheArguments args = new TestCacheArguments();
        final int loops;
        final Map<String, Object> vars = new HashMap<String, Object>();
        final JexlEvalContext jc = new JexlEvalContext(vars);

        Task(int loops) {
            this.loops = loops;
        }

        @Override
        public abstract Integer call() throws Exception;

        /**
         * The actual test function; assigns and checks.
         * <p>
         * The expression will be evaluated against different classes in parallel.
         * This verifies that neither the volatile cache in the AST nor the expression cache in the JEXL engine
         * induce errors.</p>
         * <p>
         * Using it as a micro benchmark, it shows creating expression as the dominating cost; the expression
         * cache takes care of this.
         * By moving the expression creations out of the main loop, it also shows that the volatile cache speeds
         * things up around 2x.
         * </p>
         * @param value the argument value to control
         * @return the number of loops performed
         */
        public Integer runAssign(Object value) {
            args.value = new Object[]{value};
            Object result;

            JexlExpression cacheGetValue = jexl.createExpression("cache.value");
            JexlExpression cacheSetValue = jexl.createExpression("cache.value = value");
            for (int l = 0; l < loops; ++l) {
                int px = (int) Thread.currentThread().getId();
                int mix = MIX[(l + px) % MIX.length];

                vars.put("cache", args.ca[mix]);
                vars.put("value", args.value[0]);
                result = cacheSetValue.evaluate(jc);
                if (args.value[0] == null) {
                    Assert.assertNull(cacheSetValue.toString(), result);
                } else {
                    Assert.assertEquals(cacheSetValue.toString(), args.value[0], result);
                }

                result = cacheGetValue.evaluate(jc);
                if (args.value[0] == null) {
                    Assert.assertEquals(cacheGetValue.toString(), "Cached" + mix + ":na", result);
                } else {
                    Assert.assertEquals(cacheGetValue.toString(), "Cached" + mix + ":" + args.value[0], result);
                }

            }

            return Integer.valueOf(loops);
        }
    }

    /**
     * A task to check assignment.
     */
    public static class AssignTask extends Task {
        public AssignTask(int loops) {
            super(loops);
        }

        @Override
        public Integer call() throws Exception {
            return runAssign("foo");
        }
    }

    /**
     * A task to check null assignment.
     */
    public static class AssignNullTask extends Task {
        public AssignNullTask(int loops) {
            super(loops);
        }

        @Override
        public Integer call() throws Exception {
            return runAssign(null);
        }
    }

    /**
     * A task to check boolean assignment.
     */
    public static class AssignBooleanTask extends Task {
        public AssignBooleanTask(int loops) {
            super(loops);
        }

        @Override
        public Integer call() throws Exception {
            return runAssignBoolean(Boolean.TRUE);
        }

        /** The actual test function. */
        private Integer runAssignBoolean(Boolean value) {
            args.value = new Object[]{value};
            JexlExpression cacheGetValue = jexl.createExpression("cache.flag");
            JexlExpression cacheSetValue = jexl.createExpression("cache.flag = value");
            Object result;

            for (int l = 0; l < loops; ++l) {
                int px = (int) Thread.currentThread().getId();
                int mix = MIX[(l + px) % MIX.length];

                vars.put("cache", args.ca[mix]);
                vars.put("value", args.value[0]);
                result = cacheSetValue.evaluate(jc);
                Assert.assertEquals(cacheSetValue.toString(), args.value[0], result);

                result = cacheGetValue.evaluate(jc);
                Assert.assertEquals(cacheGetValue.toString(), args.value[0], result);

            }

            return Integer.valueOf(loops);
        }
    }

    /**
     * A task to check list assignment.
     */
    public static class AssignListTask extends Task {
        public AssignListTask(int loops) {
            super(loops);
        }

        @Override
        public Integer call() throws Exception {
            return runAssignList();
        }

        /** The actual test function. */
        private Integer runAssignList() {
            args.value = new Object[]{"foo"};
            java.util.ArrayList<String> c1 = new java.util.ArrayList<String>(2);
            c1.add("foo");
            c1.add("bar");
            args.ca = new Object[]{
                new String[]{"one", "two"},
                c1
            };

            JexlExpression cacheGetValue = jexl.createExpression("cache.0");
            JexlExpression cacheSetValue = jexl.createExpression("cache[0] = value");
            Object result;

            for (int l = 0; l < loops; ++l) {
                int px = (int) Thread.currentThread().getId();
                int mix = MIX[(l + px) % MIX.length] % args.ca.length;

                vars.put("cache", args.ca[mix]);
                vars.put("value", args.value[0]);
                result = cacheSetValue.evaluate(jc);
                Assert.assertEquals(cacheSetValue.toString(), args.value[0], result);

                result = cacheGetValue.evaluate(jc);
                Assert.assertEquals(cacheGetValue.toString(), args.value[0], result);
            }

            return Integer.valueOf(loops);
        }
    }

    @Test
    public void testNullAssignNoCache() throws Exception {
        runThreaded(AssignNullTask.class, LOOPS, false);
    }

    @Test
    public void testNullAssignCache() throws Exception {
        runThreaded(AssignNullTask.class, LOOPS, true);
    }

    @Test
    public void testAssignNoCache() throws Exception {
        runThreaded(AssignTask.class, LOOPS, false);
    }

    @Test
    public void testAssignCache() throws Exception {
        runThreaded(AssignTask.class, LOOPS, true);
    }

    @Test
    public void testAssignBooleanNoCache() throws Exception {
        runThreaded(AssignBooleanTask.class, LOOPS, false);
    }

    @Test
    public void testAssignBooleanCache() throws Exception {
        runThreaded(AssignBooleanTask.class, LOOPS, true);
    }

    @Test
    public void testAssignListNoCache() throws Exception {
        runThreaded(AssignListTask.class, LOOPS, false);
    }

    @Test
    public void testAssignListCache() throws Exception {
        runThreaded(AssignListTask.class, LOOPS, true);
    }

    /**
     * A task to check method calls.
     */
    public static class ComputeTask extends Task {
        public ComputeTask(int loops) {
            super(loops);
        }

        @Override
        public Integer call() throws Exception {
            args.ca = new Object[]{args.c0, args.c1, args.c2};
            args.value = new Object[]{new Integer(2), "quux"};
            //jexl.setDebug(true);
            JexlExpression compute2 = jexl.createExpression("cache.compute(a0, a1)");
            JexlExpression compute1 = jexl.createExpression("cache.compute(a0)");
            JexlExpression compute1null = jexl.createExpression("cache.compute(a0)");
            JexlExpression ambiguous = jexl.createExpression("cache.ambiguous(a0, a1)");
            //jexl.setDebug(false);

            Object result = null;
            String expected = null;
            for (int l = 0; l < loops; ++l) {
                int mix = MIX[l % MIX.length] % args.ca.length;
                Object value = args.value[l % args.value.length];

                vars.put("cache", args.ca[mix]);
                if (value instanceof String) {
                    vars.put("a0", "S0");
                    vars.put("a1", "S1");
                    expected = "Cached" + mix + "@s#S0,s#S1";
                } else if (value instanceof Integer) {
                    vars.put("a0", Integer.valueOf(7));
                    vars.put("a1", Integer.valueOf(9));
                    expected = "Cached" + mix + "@i#7,i#9";
                } else {
                    Assert.fail("unexpected value type");
                }
                result = compute2.evaluate(jc);
                Assert.assertEquals(compute2.toString(), expected, result);

                if (value instanceof Integer) {
                    try {
                        vars.put("a0", Short.valueOf((short) 17));
                        vars.put("a1", Short.valueOf((short) 19));
                        result = ambiguous.evaluate(jc);
                        Assert.fail("should have thrown an exception");
                    } catch (JexlException xany) {
                        // throws due to ambiguous exception
                    }
                }

                if (value instanceof String) {
                    vars.put("a0", "X0");
                    expected = "Cached" + mix + "@s#X0";
                } else if (value instanceof Integer) {
                    vars.put("a0", Integer.valueOf(5));
                    expected = "Cached" + mix + "@i#5";
                } else {
                    Assert.fail("unexpected value type");
                }
                result = compute1.evaluate(jc);
                Assert.assertEquals(compute1.toString(), expected, result);

                try {
                    vars.put("a0", null);
                    result = compute1null.evaluate(jc);
                    Assert.fail("should have thrown an exception");
                } catch (JexlException xany) {
                    // throws due to ambiguous exception
                    String sany = xany.getMessage();
                    String tname = getClass().getName();
                    if (!sany.startsWith(tname)) {
                        Assert.fail("debug mode should carry caller information, "
                                + sany + ", "
                                + tname);
                    }
                }
            }
            return Integer.valueOf(loops);
        }
    }

    @Test
    public void testComputeNoCache() throws Exception {
        runThreaded(ComputeTask.class, LOOPS, false);
    }

    @Test
    public void testComputeCache() throws Exception {
        runThreaded(ComputeTask.class, LOOPS, true);
    }

    public static class JexlContextNS extends JexlEvalContext {
        final Map<String, Object> funcs;

        JexlContextNS(Map<String, Object> vars, Map<String, Object> funcs) {
            super(vars);
            this.funcs = funcs;
        }

        @Override
        public Object resolveNamespace(String name) {
            return funcs.get(name);
        }

    }

    /**
     * The remaining tests exercise the namespaced namespaces; not MT.
     * @param x
     * @param loops
     * @param cache
     * @throws Exception
     */
    void doCOMPUTE(TestCacheArguments x, int loops, boolean cache) throws Exception {
        if (loops == 0) {
            loops = MIX.length;
        }
        if (!cache) {
            jexl.clearCache();
        }
        Map<String, Object> vars = new HashMap<String, Object>();
        java.util.Map<String, Object> funcs = new java.util.HashMap<String, Object>();
        JexlEvalContext jc = new JexlContextNS(vars, funcs);
        JexlExpression compute2 = jexl.createExpression("cached:COMPUTE(a0, a1)");
        JexlExpression compute1 = jexl.createExpression("cached:COMPUTE(a0)");
        Object result = null;
        String expected = null;
        for (int l = 0; l < loops; ++l) {
            int mix = MIX[l % MIX.length] % x.ca.length;
            Object value = x.value[l % x.value.length];

            funcs.put("cached", x.ca[mix]);
            if (value instanceof String) {
                vars.put("a0", "S0");
                vars.put("a1", "S1");
                expected = "CACHED@s#S0,s#S1";
            } else if (value instanceof Integer) {
                vars.put("a0", Integer.valueOf(7));
                vars.put("a1", Integer.valueOf(9));
                expected = "CACHED@i#7,i#9";
            } else {
                Assert.fail("unexpected value type");
            }
            result = compute2.evaluate(jc);
            Assert.assertEquals(compute2.toString(), expected, result);

            if (value instanceof String) {
                vars.put("a0", "X0");
                expected = "CACHED@s#X0";
            } else if (value instanceof Integer) {
                vars.put("a0", Integer.valueOf(5));
                expected = "CACHED@i#5";
            } else {
                Assert.fail("unexpected value type");
            }
            result = compute1.evaluate(jc);
            Assert.assertEquals(compute1.toString(), expected, result);
        }
    }

    @Test
    public void testCOMPUTENoCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.ca = new Object[]{
            Cached.class, Cached1.class, Cached2.class
        };
        args.value = new Object[]{new Integer(2), "quux"};
        doCOMPUTE(args, LOOPS, false);
    }

    @Test
    public void testCOMPUTECache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.ca = new Object[]{
            Cached.class, Cached1.class, Cached2.class
        };
        args.value = new Object[]{new Integer(2), "quux"};
        doCOMPUTE(args, LOOPS, true);
    }
}
