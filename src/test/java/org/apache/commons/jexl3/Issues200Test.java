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
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for reported issue between JEXL-200 and JEXL-299.
 */
@SuppressWarnings({"boxing", "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class Issues200Test extends JexlTestCase {
    public Issues200Test() {
        super("Issues200Test", null);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        // ensure jul logging is only error to avoid warning in silent mode
        java.util.logging.Logger.getLogger(JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
    }

    public static class Eval {
        private JexlEngine jexl;

        public JexlScript fn(String src) {
            return jexl.createScript(src);
        }

        void setJexl(JexlEngine je) {
            jexl = je;
        }
    }

    @Test
    public void test200() throws Exception {
        JexlContext jc = new MapContext();
        Map<String, Object> funcs = new HashMap<String, Object>();
        Eval eval = new Eval();
        funcs.put(null, eval);
        JexlEngine jexl = new JexlBuilder().namespaces(funcs).create();
        eval.setJexl(jexl);
        String src = "var f = fn(\'(x)->{x + 42}\'); f(y)";
        JexlScript s200 = jexl.createScript(src, "y");
        Assert.assertEquals(142, s200.execute(jc, 100));
        Assert.assertEquals(52, s200.execute(jc, 10));
    }

    @Test
    public void test200b() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("var x = 0; var f = (y)->{ x = y; }; f(42); x");
        Object r = e.execute(jc);
        Assert.assertEquals(0, r);
    }

    @Test
    public void test209a() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("var x = new('java.util.HashMap'); x.a = ()->{return 1}; x['a']()");
        Object r = e.execute(jc);
        Assert.assertEquals(1, r);
    }

    @Test
    public void test209b() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("var x = new('java.util.HashMap'); x['a'] = ()->{return 1}; x.a()");
        Object r = e.execute(jc);
        Assert.assertEquals(1, r);
    }

    public class T210 {
        public void npe() {
            throw new NullPointerException("NPE210");
        }
    }

    @Test
    public void test210() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("v210", new T210());
        JexlEngine jexl = new JexlBuilder().strict(false).silent(false).create();
        JexlScript e = jexl.createScript("v210.npe()");
        try {
            e.execute(jc);
            Assert.fail("should have thrown an exception");
        } catch(JexlException xjexl) {
            Throwable th = xjexl.getCause();
            Assert.assertEquals("NPE210", th.getMessage());
        }
    }

    @Test
    public void test217() throws Exception {
        JexlEvalContext jc = new JexlEvalContext();
        jc.set("foo", new int[]{0, 1, 2, 42});
        JexlEngine jexl;
        JexlScript e;
        Object r;
        jexl = new JexlBuilder().strict(false).silent(false).create();
        e = jexl.createScript("foo[3]");
        r = e.execute(jc);
        Assert.assertEquals(42, r);

        // cache and fail?
        jc.set("foo", new int[]{0, 1});
        jc.setStrict(true);
        try {
            r = e.execute(jc);
            Assert.fail("should have thrown an exception");
        } catch(JexlException xjexl) {
            Throwable th = xjexl.getCause();
            Assert.assertTrue(ArrayIndexOutOfBoundsException.class.equals(th.getClass()));
        }
        //
        jc.setStrict(false);
        r = e.execute(jc);
        Assert.assertNull("oob adverted", r);
    }


    @Test
    public void test221() throws Exception {
        JexlEvalContext jc = new JexlEvalContext();
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("one", 1);
        jc.set("map", map);
        JexlEngine jexl = new JexlBuilder().cache(256).create();
        JexlScript e = jexl.createScript("(x)->{ map[x] }");
        Object r;
        r = e.execute(jc, (Object) null);
        Assert.assertEquals(null, r);
        r = e.execute(jc, (Object) null);
        Assert.assertEquals(null, r);
        r = e.execute(jc, "one");
        Assert.assertEquals(1, r);
    }


    public static class JexlArithmetic224 extends JexlArithmetic {
        public JexlArithmetic224(boolean astrict) {
            super(astrict);
        }

        protected Object nth(Collection<?> c, int i) {
            if (c instanceof List) {
                // tell engine to use default
                return JexlEngine.TRY_FAILED;
            }
            for (Object o : c) {
                if (i-- == 0) {
                    return o;
                }
            }
            return null;
        }

        public Object propertyGet(Collection<?> c, Number n) {
            return nth(c, n.intValue());
        }

        public Object arrayGet(Collection<?> c, Number n) {
            return nth(c, n.intValue());
        }

        public Object call(Collection<?> c, Number n) {
            if (c instanceof List) {
                return ((List) c).get(n.intValue());
            }
            return nth(c, n.intValue());
        }
    }

    @Test
    public void test224() throws Exception {
        List<String> a0 = Arrays.asList("one", "two");
        Set<String> a1 = new TreeSet<String>(a0);
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().arithmetic(new JexlArithmetic224(true)).create();
        Object r;
        JexlScript e = jexl.createScript("(map, x)->{ map[x] }");
        r = e.execute(jc, a0, 1);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1, 1);
        Assert.assertEquals("two", r);
        e = jexl.createScript("(map)->{ map.1 }");
        r = e.execute(jc, a0);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1);
        Assert.assertEquals("two", r);
        e = jexl.createScript("(map, x)->{ map(x) }");
        r = e.execute(jc, a0, 1);
        Assert.assertEquals("two", r);
        r = e.execute(jc, a1, 1);
        Assert.assertEquals("two", r);
    }

    public static class Context225 extends MapContext {
        public String bar(){
            return "bar";
        }
    }

    @Test
    public void test225() throws Exception {
        Context225 df = new Context225();
        JexlEngine jexl = new JexlBuilder().create();

        JexlExpression expression = jexl.createExpression("bar()");
        Assert.assertEquals("bar", expression.evaluate(df));
        ObjectContext<Object> context = new ObjectContext<Object>(jexl, df);
        Assert.assertEquals("bar", expression.evaluate(context));
    }

    private static void handle(ExecutorService pool, final JexlScript script, final Map<String, Object> payload) {
       pool.submit(new Runnable() {
            @Override public void run() {
                script.execute(new MapContext(payload));
            }
        });
    }

    @Test
    public void test241() throws Exception {
        ExecutorService pool;
        JexlScript script = new JexlBuilder().create().createScript("`${item}`");

        pool = Executors.newFixedThreadPool(4);

        Map<String, Object> m1 = new HashMap<String, Object>();
        m1.put("item", "A");
        Map<String, Object> m2 = new HashMap<String, Object>();
        m2.put("item", "B");

        handle(pool, script, m1);
        script.execute(new MapContext(m2));
        pool.shutdown();
    }

    @Test
    public void test242() throws Exception {
        Double a = -40.05d;
        Double b = -8.01d;
        Double c = a + b;
        final JexlContext context = new MapContext();
        context.set("a", a);
        context.set("b", b);
        JexlEngine JEXL_ENGINE = new JexlBuilder().strict(true).silent(true).create();
        JexlExpression jsp = JEXL_ENGINE.createExpression("a + b");
        Double e = (Double) jsp.evaluate(context);
        Assert.assertTrue(Double.doubleToLongBits(e) + " != " + Double.doubleToLongBits(c), c.doubleValue() == e.doubleValue());
        Assert.assertTrue(Double.doubleToLongBits(e) + " != " + Double.doubleToLongBits(c), a + b == e);
    }


    @Test
    public void test243a() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(32).create();
        JexlScript script = jexl.createScript("while(true);");
        try {
            JexlExpression expr = jexl.createExpression("while(true);");
            Assert.fail("should have failed!, expr do not allow 'while' statement");
        } catch (JexlException.Parsing xparse) {
            // ok
        } catch (JexlException xother) {
            // ok
        }
    }

    public static class Foo245 {
        private Object bar = null;

        void setBar(Object bar) {
            this.bar = bar;
        }

        public Object getBar() {
            return bar;
        }
    }

    @Test
    public void test245() throws Exception {
        MapContext ctx = new MapContext();
        Foo245 foo245 = new Foo245();
        ctx.set("foo", foo245);

        JexlEngine engine = new JexlBuilder().strict(true).safe(false).silent(false).create();
        JexlExpression foobar = engine.createExpression("foo.bar");
        JexlExpression foobaz = engine.createExpression("foo.baz");
        JexlExpression foobarbaz = engine.createExpression("foo.bar.baz");
        // add ambiguity with null & not-null
        Object[] args = { null, 245 };
        for(Object arg : args ){
            foo245.setBar(arg);
            // ok
            Assert.assertEquals(foo245.getBar(), foobar.evaluate(ctx));
            // fail level 1
            try {
                foobaz.evaluate(ctx);
                Assert.fail("foo.baz is not solvable, exception expected");
            } catch(JexlException xp) {
                Assert.assertTrue(xp instanceof JexlException.Property);
            }
            // fail level 2
            try {
                foobarbaz.evaluate(ctx);
                Assert.fail("foo.bar.baz is not solvable, exception expected");
            } catch(JexlException xp) {
                Assert.assertTrue(xp instanceof JexlException.Property);
            }
        }
    }

    @Test
    public void test256() throws Exception {
        MapContext ctx = new MapContext() {
            @Override public void set(String name, Object value) {
                if ("java".equals(name)) {
                    throw new JexlException(null, "can not set " + name);
                }
                super.set(name, value);
            }
            @Override public Object get(String name) {
                if ("java".equals(name)) {
                    return null;
                }
                return super.get(name);
            }
            @Override public boolean has(String name) {
                if ("java".equals(name)) {
                    return false;
                }
                return super.has(name);
            }
        };
        ctx.set("java.version", 10);
        JexlEngine engine = new JexlBuilder().strict(true).silent(false).create();
        Object result = null;
        JexlScript script;
        script = engine.createScript("java = 3");
        try {
             script.execute(ctx);
             Assert.fail("should have failed!");
        } catch(JexlException xjexl) {
            // expected
        }
        script = engine.createScript("java.version");
        result = script.execute(ctx);
        Assert.assertEquals(10, result);
    }

    @Test
    public void test230() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(4).create();
        JexlContext ctxt = new MapContext();
        int[] foo = {42};
        ctxt.set("fo o", foo);
        Object value;
        for (int l = 0; l < 2; ++l) {
            value = jexl.createExpression("fo\\ o[0]").evaluate(ctxt);
            Assert.assertEquals(42, value);
            value = jexl.createExpression("fo\\ o[0] = 43").evaluate(ctxt);
            Assert.assertEquals(43, value);
            value = jexl.createExpression("fo\\ o.0").evaluate(ctxt);
            Assert.assertEquals(43, value);
            value = jexl.createExpression("fo\\ o.0 = 42").evaluate(ctxt);
            Assert.assertEquals(42, value);
        }
    }

    @Test
    public void test265() throws Exception {
        JexlEngine jexl = new JexlBuilder().cache(4).create();
        JexlContext ctxt = new MapContext();
        ctxt.set("x", 42);
        Object result;
        JexlScript script;
        try {
            script = jexl.createScript("(true) ? x : abs(1)");
        } catch (JexlException.Parsing xparse) {
            // ambiguous, parsing fails
        }
        script = jexl.createScript("(true) ? (x) : abs(2)");
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
        script = jexl.createScript("(true) ? x : (abs(3))");
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
        script = jexl.createScript("(!true) ? abs(4) : x");
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
    }


    /**
     * An iterator that implements Closeable (at least implements a close method).
     */
    public static class Iterator266 implements /*Closeable,*/ Iterator<Object> {
        private Iterator<Object> iterator;

        Iterator266(Iterator<Object> ator) {
            iterator = ator;
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }

        //@Override
        public void close() {
            if (iterator != null) {
                Arithmetic266.closeIterator(this);
                iterator = null;
            }
        }

        @Override
        public boolean hasNext() {
            if (iterator == null) {
                return false;
            }
            boolean n = iterator.hasNext();
            if (!n) {
                close();
            }
            return n;
        }

        @Override
        public Object next() {
            if (iterator == null) {
                throw new NoSuchElementException();
            }
            return iterator.next();
        }

        @Override
        public void remove() {
            if (iterator != null) {
                iterator.remove();
            }
        }
    }
    public static class Arithmetic266 extends JexlArithmetic {
        static final ThreadLocal<Deque<Iterator266>> TLS_FOREACH = new ThreadLocal<Deque<Iterator266>>() {
            @Override
            public Deque<Iterator266> initialValue() {
                return new LinkedList<Iterator266>();
            }
        };
        public Arithmetic266(boolean strict) {
            super(strict);
        }

        static void closeIterator(Iterator266 i266) {
            Deque<Iterator266> queue = TLS_FOREACH.get();
            if (queue != null) {
                queue.remove(i266);
            }
        }

        public Iterator<?> forEach(Iterable<?> collection) {
            Iterator266 it266 = new Iterator266((Iterator<Object>) collection.iterator());
            Deque<Iterator266> queue = TLS_FOREACH.get();
            queue.addFirst(it266);
            return it266;
        }

        public Iterator<?> forEach(Map<?,?> collection) {
            return forEach(collection.values());
        }

        public void remove() {
            Deque<Iterator266> queue = TLS_FOREACH.get();
            Iterator266 i266 = queue.getFirst();
            if (i266 != null) {
                i266.remove();
                throw new JexlException.Continue(null, null);
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    @Test
    public void test266() throws Exception {
        Object result;
        JexlScript script;
        JexlEngine jexl = new JexlBuilder().arithmetic(new Arithmetic266(true)).create();
        JexlContext ctxt = new MapContext();

        List<Integer> li = new ArrayList<Integer>(Arrays.asList(1, 2, 3, 4, 5 ,6));
        ctxt.set("list", li);
        script = jexl.createScript("for (var item : list) { if (item <= 3) remove(); } return size(list)");
        result = script.execute(ctxt);
        Assert.assertEquals(3, result);
        Assert.assertEquals(3, li.size());

        Map<String, Integer> msi = new HashMap<String, Integer>();
        msi.put("a", 1);
        msi.put("b", 2);
        msi.put("c", 3);
        msi.put("d", 4);
        msi.put("e", 5);
        msi.put("f", 6);
        ctxt.set("map", msi);
        script = jexl.createScript("for (var item : map) { if (item <= 2) remove(); } return size(map)");
        result = script.execute(ctxt);
        Assert.assertEquals(4, result);
        Assert.assertEquals(4, msi.size());
    }

    @Test
    public void test267() throws Exception {
        Object result;
        JexlScript script;
        JexlEngine jexl = new JexlBuilder().create();
        JexlContext ctxt = new MapContext();
        // API declared params
        script = jexl.createScript("x + y", "x", "y");
        result = script.execute(ctxt, 20, 22);
        Assert.assertEquals(42, result);
        // script declared params
        script = jexl.createScript("(x, y)->{ x + y}");
        result = script.execute(ctxt, 22, 20);
        Assert.assertEquals(42, result);
        // explicitly returning the lambda
        script = jexl.createScript("return (x, y)->{ x + y}");
        result = script.execute(ctxt);
        Assert.assertTrue(result instanceof JexlScript);
    }


    @Test
    public void test274() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).safe(true).stackOverflow(5).create();
        JexlContext ctxt = new MapContext();
        JexlScript script= jexl.createScript("var f = (x)->{ x > 1? x * f(x - 1) : x }; f(a)", "a");
        Object result = null;
        result = script.execute(ctxt, 3);
        Assert.assertEquals(6, result);
        try {
            result = script.execute(ctxt, 32);
            Assert.fail("should have overflown");
        } catch(JexlException.StackOverflow xstack) {
            // expected
            String sxs = xstack.toString();
            Assert.assertTrue(sxs.contains("jexl"));
        }
        jexl = new JexlBuilder().strict(true).create();
        script= jexl.createScript("var f = (x)->{ x * f(x - 1) }; f(a)", "a");
        try {
            result = script.execute(ctxt, 32);
            Assert.fail("should have overflown");
        } catch(JexlException.StackOverflow xstack) {
            // expected
            String sxs = xstack.toString();
            Assert.assertTrue(sxs.contains("jvm"));
        }
    }

    @Test
    public void test278() throws Exception {
        String[] srcs = new String[]{
            "return union x143('arg',5,6) ",
            "return union y143('arg',5,6)   ;",
            "return union\n z143('arg',5,6)   ;",
            "var f =()->{ return union 143 } foo[0]"
        };
        Object[] ctls = new Object[]{
            "42","42","42", 42
        };
        JexlEngine jexl = new JexlBuilder().cache(4).create();
        JexlContext ctxt = new MapContext();
        int[] foo = {42};
        ctxt.set("foo", foo);
        ctxt.set("union", "42");
        Object value;
        JexlScript jc;
        for(int i = 0; i < srcs.length; ++i) {
            String src = srcs[i];
            try {
                jc = jexl.createScript(src);
                Assert.fail("should have failed, " + (jc != null));
            } catch(JexlException.Ambiguous xa) {
                String str = xa.toString();
                Assert.assertTrue(str.contains("143"));
                src = xa.tryCleanSource(src);
            }
            jc = jexl.createScript(src);
            value = jc.execute(ctxt);
            Assert.assertEquals(src, ctls[i], value);
        }
    }

    @Test
    public void test279() throws Exception {
        Object result;
        JexlScript script;
        JexlContext ctxt = new MapContext();
        ctxt.set("y.z", null);
        ctxt.set("z", null);
        String[] srcs = new String[]{
             "var x = null; x[0];",
             "var x = null; x.0;",
             "z[0]",
             "z.0",
             "y.z[0]",
             "y.z.0",
        };
        for(boolean strict : new boolean[]{ true, false} ) {
            JexlEngine jexl = new JexlBuilder().safe(false).strict(strict).create();
            for (String src : srcs) {
                script = jexl.createScript(src);
                try {
                    result = script.execute(ctxt);
                    if (strict) {
                        Assert.fail("should have failed: " + src);
                    }
                    // not reachable
                    Assert.assertNull("non-null result ?!", result);
                } catch (JexlException xany) {
                   if (!strict) {
                       Assert.fail(src + ", should not have thrown " + xany);
                   }
                }
            }
        }
    }
}
