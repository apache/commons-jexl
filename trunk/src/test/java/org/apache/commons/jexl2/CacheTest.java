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
package org.apache.commons.jexl2;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies cache & tryExecute
 */
public class CacheTest extends JexlTestCase {
    public CacheTest(String testName) {
        super(testName);
    }
    private static final JexlEngine jexl = new JexlEngine();

    static {
        jexl.setCache(512);
        jexl.setLenient(false);
        jexl.setSilent(false);
    }
    private static final int LOOPS = 1024;
    // A pseudo random mix of accessors
    private static final int[] MIX = {
        0, 0, 3, 3, 4, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 1, 1, 1, 2, 2, 2,
        3, 3, 3, 4, 4, 4, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 2, 2, 3, 3, 0
    };

    @Override
    protected void tearDown() throws Exception {
        debuggerCheck(jexl);
    }
    
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
        public Object put(String key, Object arg) {
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

    static class TestCacheArguments {
        Cached0 c0 = new Cached0();
        Cached1 c1 = new Cached1();
        Cached2 c2 = new Cached2();
        Cached3 c3 = new Cached3();
        Cached4 c4 = new Cached4();
        Object[] ca = {
            c0, c1, c2, c3, c4
        };
        Object value = null;
    }

    void doAssign(TestCacheArguments x, int loops, boolean cache) throws Exception {
        if (loops == 0) {
            loops = MIX.length;
        }
        if (cache) {
            jexl.setCache(32);
        } else {
            jexl.setCache(0);
        }
        Map<String, Object> vars = new HashMap<String,Object>();
        JexlContext jc = new MapContext(vars);
        Expression cacheGetValue = jexl.createExpression("cache.value");
        Expression cacheSetValue = jexl.createExpression("cache.value = value");
        Object result;

        for (int l = 0; l < loops; ++l) {
            int mix = MIX[l % MIX.length];

            vars.put("cache", x.ca[mix]);
            vars.put("value", x.value);
            result = cacheSetValue.evaluate(jc);
            if (x.value == null) {
                assertNull(cacheSetValue.toString(), result);
            } else {
                assertEquals(cacheSetValue.toString(), x.value, result);
            }

            result = cacheGetValue.evaluate(jc);
            if (x.value == null) {
                assertEquals(cacheGetValue.toString(), "Cached" + mix + ":na", result);
            } else {
                assertEquals(cacheGetValue.toString(), "Cached" + mix + ":" + x.value, result);
            }

        }
    }

    public void testNullAssignNoCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        doAssign(args, LOOPS, false);
    }

    public void testNullAssignCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        doAssign(args, LOOPS, true);
    }

    public void testAssignNoCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.value = "foo";
        doAssign(args, LOOPS, false);
    }

    public void testAssignCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.value = "foo";
        doAssign(args, LOOPS, true);
    }

    void doAssignBoolean(TestCacheArguments x, int loops, boolean cache) throws Exception {
        if (loops == 0) {
            loops = MIX.length;
        }
        if (cache) {
            jexl.setCache(32);
        } else {
            jexl.setCache(0);
        }
        Map<String, Object> vars = new HashMap<String,Object>();
        JexlContext jc = new MapContext(vars);
        Expression cacheGetValue = jexl.createExpression("cache.flag");
        Expression cacheSetValue = jexl.createExpression("cache.flag = value");
        Object result;

        for (int l = 0; l < loops; ++l) {
            int mix = MIX[l % MIX.length];

            vars.put("cache", x.ca[mix]);
            vars.put("value", x.value);
            result = cacheSetValue.evaluate(jc);
            assertEquals(cacheSetValue.toString(), x.value, result);

            result = cacheGetValue.evaluate(jc);
            assertEquals(cacheGetValue.toString(), x.value, result);

        }
    }

    public void testAssignBooleanNoCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.value = Boolean.TRUE;
        doAssignBoolean(args, LOOPS, false);
    }

    public void testAssignBooleanCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.value = Boolean.TRUE;
        doAssignBoolean(args, LOOPS, true);
    }

    void doAssignList(TestCacheArguments x, int loops, boolean cache) throws Exception {
        if (loops == 0) {
            loops = MIX.length;
        }
        if (cache) {
            jexl.setCache(32);
        } else {
            jexl.setCache(0);
        }
        Map<String, Object> vars = new HashMap<String,Object>();
        JexlContext jc = new MapContext(vars);
        Expression cacheGetValue = jexl.createExpression("cache.0");
        Expression cacheSetValue = jexl.createExpression("cache[0] = value");
        Object result;

        for (int l = 0; l < loops; ++l) {
            int mix = MIX[l % MIX.length] % x.ca.length;

            vars.put("cache", x.ca[mix]);
            vars.put("value", x.value);
            result = cacheSetValue.evaluate(jc);
            assertEquals(cacheSetValue.toString(), x.value, result);

            result = cacheGetValue.evaluate(jc);
            assertEquals(cacheGetValue.toString(), x.value, result);

        }
    }

    public void testAssignListNoCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.value = "foo";
        java.util.ArrayList<String> c1 = new java.util.ArrayList<String>(2);
        c1.add("foo");
        c1.add("bar");
        args.ca = new Object[]{
            new String[]{"one", "two"},
            c1
        };
        doAssignList(args, LOOPS, false);
    }

    public void testAssignListCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.value = "foo";
        java.util.ArrayList<String> c1 = new java.util.ArrayList<String>(2);
        c1.add("foo");
        c1.add("bar");
        args.ca = new Object[]{
            new String[]{"one", "two"},
            c1
        };
        doAssignList(args, LOOPS, true);
    }

    void doCompute(TestCacheArguments x, int loops, boolean cache) throws Exception {
        if (loops == 0) {
            loops = MIX.length;
        }
        if (cache) {
            jexl.setCache(32);
        } else {
            jexl.setCache(0);
        }
        Map<String, Object> vars = new HashMap<String,Object>();
        JexlContext jc = new MapContext(vars);
        jexl.setDebug(true);
        Expression compute2 = jexl.createExpression("cache.compute(a0, a1)");
        Expression compute1 = jexl.createExpression("cache.compute(a0)");
        Expression compute1null = jexl.createExpression("cache.compute(a0)");
        Expression ambiguous = jexl.createExpression("cache.ambiguous(a0, a1)");
        jexl.setDebug(false);
        Object result = null;
        String expected = null;
        for (int l = 0; l < loops; ++l) {
            int mix = MIX[l % MIX.length] % x.ca.length;

            vars.put("cache", x.ca[mix]);
            if (x.value instanceof String) {
                vars.put("a0", "S0");
                vars.put("a1", "S1");
                expected = "Cached" + mix + "@s#S0,s#S1";
            } else if (x.value instanceof Integer) {
                vars.put("a0", Integer.valueOf(7));
                vars.put("a1", Integer.valueOf(9));
                expected = "Cached" + mix + "@i#7,i#9";
            } else {
                fail("unexpected value type");
            }
            result = compute2.evaluate(jc);
            assertEquals(compute2.toString(), expected, result);

            if (x.value instanceof Integer) {
                try {
                    vars.put("a0", Short.valueOf((short) 17));
                    vars.put("a1", Short.valueOf((short) 19));
                    result = ambiguous.evaluate(jc);
                    fail("should have thrown an exception");
                } catch (JexlException xany) {
                    // throws due to ambiguous exception
                }
            }

            if (x.value instanceof String) {
                vars.put("a0", "X0");
                expected = "Cached" + mix + "@s#X0";
            } else if (x.value instanceof Integer) {
                vars.put("a0", Integer.valueOf(5));
                expected = "Cached" + mix + "@i#5";
            } else {
                fail("unexpected value type");
            }
            result = compute1.evaluate(jc);
            assertEquals(compute1.toString(), expected, result);

            try {
                vars.put("a0", null);
                jexl.setDebug(true);
                result = compute1null.evaluate(jc);
                fail("should have thrown an exception");
            } catch (JexlException xany) {
                // throws due to ambiguous exception
                String sany = xany.getMessage();
                String tname = getClass().getName();
                if (!sany.startsWith(tname)) {
                    fail("debug mode should carry caller information, "
                         + sany +", "
                         + tname);
                }
            }
            finally {
                jexl.setDebug(false);
            }
        }
    }

    public void testComputeNoCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.ca = new Object[]{
                    args.c0, args.c1, args.c2
                };
        args.value = new Integer(2);
        doCompute(args, LOOPS, false);
    }

    public void testComputeCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.ca = new Object[]{
                    args.c0, args.c1, args.c2
                };
        args.value = new Integer(2);
        doCompute(args, LOOPS, true);
    }

    void doCOMPUTE(TestCacheArguments x, int loops, boolean cache) throws Exception {
        if (loops == 0) {
            loops = MIX.length;
        }
        if (cache) {
            jexl.setCache(32);
        } else {
            jexl.setCache(0);
        }
        Map<String, Object> vars = new HashMap<String,Object>();
        JexlContext jc = new MapContext(vars);
        java.util.Map<String, Object> funcs = new java.util.HashMap<String, Object>();
        jexl.setFunctions(funcs);
        Expression compute2 = jexl.createExpression("cached:COMPUTE(a0, a1)");
        Expression compute1 = jexl.createExpression("cached:COMPUTE(a0)");
        Object result = null;
        String expected = null;
        for (int l = 0; l < loops; ++l) {
            int mix = MIX[l % MIX.length] % x.ca.length;

            funcs.put("cached", x.ca[mix]);
            if (x.value instanceof String) {
                vars.put("a0", "S0");
                vars.put("a1", "S1");
                expected = "CACHED@s#S0,s#S1";
            } else if (x.value instanceof Integer) {
                vars.put("a0", Integer.valueOf(7));
                vars.put("a1", Integer.valueOf(9));
                expected = "CACHED@i#7,i#9";
            } else {
                fail("unexpected value type");
            }
            result = compute2.evaluate(jc);
            assertEquals(compute2.toString(), expected, result);

            if (x.value instanceof String) {
                vars.put("a0", "X0");
                expected = "CACHED@s#X0";
            } else if (x.value instanceof Integer) {
                vars.put("a0", Integer.valueOf(5));
                expected = "CACHED@i#5";
            } else {
                fail("unexpected value type");
            }
            result = compute1.evaluate(jc);
            assertEquals(compute1.toString(), expected, result);
        }
    }

    public void testCOMPUTENoCache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.ca = new Object[]{
            Cached.class, Cached1.class, Cached2.class
        };
        args.value = new Integer(2);
        doCOMPUTE(args, LOOPS, false);
    }
    
    public void testCOMPUTECache() throws Exception {
        TestCacheArguments args = new TestCacheArguments();
        args.ca = new Object[]{
            Cached.class, Cached1.class, Cached2.class
        };
        args.value = new Integer(2);
        doCOMPUTE(args, LOOPS, true);
    }

}
