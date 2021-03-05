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

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.jexl3.internal.Interpreter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for annotations.
 * @since 3.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})

public class AnnotationTest extends JexlTestCase {

    public final static int NUM_THREADS = 10;
    public final static int NUM_ITERATIONS = 1000;

    public AnnotationTest() {
        super("AnnotationTest");
    }

    @Test
    public void test197a() throws Exception {
        final JexlContext jc = new MapContext();
        final JexlScript e = JEXL.createScript("@synchronized { return 42; }");
        final Object r = e.execute(jc);
        Assert.assertEquals(42, r);
    }

    public static class AnnotationContext extends MapContext implements JexlContext.AnnotationProcessor {
        private int count = 0;
        private final Set<String> names = new TreeSet<String>();

        @Override
        public Object processAnnotation(final String name, final Object[] args, final Callable<Object> statement) throws Exception {
            count += 1;
            names.add(name);
            if ("one".equals(name)) {
                names.add(args[0].toString());
            } else if ("two".equals(name)) {
                names.add(args[0].toString());
                names.add(args[1].toString());
            } else if ("error".equals(name)) {
                names.add(args[0].toString());
                throw new IllegalArgumentException(args[0].toString());
            } else if ("unknown".equals(name)) {
                return null;
            } else if ("synchronized".equals(name)) {
                if (statement instanceof Interpreter.AnnotatedCall) {
                    final Object sa = ((Interpreter.AnnotatedCall) statement).getStatement();
                    if (sa != null) {
                        synchronized (sa) {
                            return statement.call();
                        }
                    }
                }
                final JexlEngine jexl = JexlEngine.getThreadEngine();
                if (jexl != null) {
                    synchronized (jexl) {
                        return statement.call();
                    }
                }
            }
            return statement.call();
        }

        public int getCount() {
            return count;
        }

        public Set<String> getNames() {
            return names;
        }
    }

    public static class OptAnnotationContext extends JexlEvalContext implements JexlContext.AnnotationProcessor {
        @Override
        public Object processAnnotation(final String name, final Object[] args, final Callable<Object> statement) throws Exception {
            final JexlOptions options = this.getEngineOptions();
            // transient side effect for strict
            if ("strict".equals(name)) {
                final boolean s = (Boolean) args[0];
                final boolean b = options.isStrict();
                options.setStrict(s);
                final Object r = statement.call();
                options.setStrict(b);
                return r;
            }
            // transient side effect for silent
            if ("silent".equals(name)) {
                if ((args != null) && (args.length != 0)) {
                    final boolean s = (Boolean) args[0];
                    final boolean b = options.isSilent();
                    options.setSilent(s);
                    Assert.assertEquals(s, options.isSilent());
                    final Object r = statement.call();
                    options.setSilent(b);
                    return r;
                }
                final boolean b = options.isSilent();
                try {
                    return statement.call();
                } catch(final JexlException xjexl) {
                    return null;
                } finally {
                    options.setSilent(b);
                }
            }
            // durable side effect for scale
            if ("scale".equals(name)) {
                options.setMathScale((Integer) args[0]);
                return statement.call();
            }
            return statement.call();
        }
    }

    @Test
    public void testVarStmt() throws Exception {
        final OptAnnotationContext jc = new OptAnnotationContext();
        final JexlOptions options = jc.getEngineOptions();
        jc.getEngineOptions().set(JEXL);
        options.setSharedInstance(true);
        JexlScript e;
        Object r;
        e = JEXL.createScript("(s, v)->{ @strict(s) @silent(v) var x = y ; 42; }");

        // wont make an error
        try {
            r = e.execute(jc, false, true);
            Assert.assertEquals(42, r);
        } catch (final JexlException.Variable xjexl) {
            Assert.fail("should not have thrown");
        }

        r = null;
        // will make an error and throw
        options.setSafe(false);
        try {
            r = e.execute(jc, true, false);
            Assert.fail("should have thrown");
        } catch (final JexlException.Variable xjexl) {
            Assert.assertNull(r);
        }

        r = null;
        // will make an error and will not throw but result is null
        try {
            r = e.execute(jc, true, true);
            Assert.assertNull(r);
        } catch (final JexlException.Variable xjexl) {
            Assert.fail("should not have thrown");
        }
        options.setSafe(true);

        r = null;
        // will not make an error and will not throw
        try {
            r = e.execute(jc, false, false);
            Assert.assertEquals(42, r);
        } catch (final JexlException.Variable xjexl) {
            Assert.fail("should not have thrown");
        }
        //Assert.assertEquals(42, r);
        Assert.assertTrue(options.isStrict());
        e = JEXL.createScript("@scale(5) 42;");
        r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertTrue(options.isStrict());
        Assert.assertEquals(5, options.getMathScale());
    }

    @Test
    public void testNoArg() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@synchronized { return 42; }");
        final Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    public void testNoArgExpression() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@synchronized 42");
        final Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    public void testNoArgStatement() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@synchronized if (true) 2 * 3 * 7; else -42;");
        final Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    public void testHoistingStatement() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("var t = 1; @synchronized for(var x : [2,3,7]) t *= x; t");
        final Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    public void testOneArg() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@one(1) { return 42; }");
        final Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("one"));
        Assert.assertTrue(jc.getNames().contains("1"));
    }

    @Test
    public void testMultiple() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@one(1) @synchronized { return 42; }");
        final Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(2, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("synchronized"));
        Assert.assertTrue(jc.getNames().contains("one"));
        Assert.assertTrue(jc.getNames().contains("1"));
    }

    @Test
    public void testError() throws Exception {
        testError(true);
        testError(false);
    }

    private void testError(final boolean silent) throws Exception {
        final CaptureLog log = new CaptureLog();
        final AnnotationContext jc = new AnnotationContext();
        final JexlEngine jexl = new JexlBuilder().logger(log).strict(true).silent(silent).create();
        final JexlScript e = jexl.createScript("@error('42') { return 42; }");
        try {
            final Object r = e.execute(jc);
            if (!silent) {
                Assert.fail("should have failed");
            } else {
                Assert.assertEquals(1, log.count("warn"));
            }
        } catch (final JexlException.Annotation xjexl) {
            Assert.assertEquals("error", xjexl.getAnnotation());
        }
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("error"));
        Assert.assertTrue(jc.getNames().contains("42"));
        if (!silent) {
            Assert.assertEquals(0, log.count("warn"));
        }
    }

    @Test
    public void testUnknown() throws Exception {
        testUnknown(true);
        testUnknown(false);
    }

    private void testUnknown(final boolean silent) throws Exception {
        final CaptureLog log = new CaptureLog();
        final AnnotationContext jc = new AnnotationContext();
        final JexlEngine jexl = new JexlBuilder().logger(log).strict(true).silent(silent).create();
        final JexlScript e = jexl.createScript("@unknown('42') { return 42; }");
        try {
            final Object r = e.execute(jc);
            if (!silent) {
                Assert.fail("should have failed");
            } else {
                Assert.assertEquals(1, log.count("warn"));
            }
        } catch (final JexlException.Annotation xjexl) {
            Assert.assertEquals("unknown", xjexl.getAnnotation());
        }
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("unknown"));
        Assert.assertFalse(jc.getNames().contains("42"));
        if (!silent) {
            Assert.assertEquals(0, log.count("warn"));
        }
    }

    /**
     * A counter whose inc method will misbehave if not mutex-ed.
     */
    public static class Counter {
        private int value = 0;

        public void inc() {
            final int v = value;
            // introduce some concurency
            for (int i = (int) System.currentTimeMillis() % 5; i >= 0; --i) {
                Thread.yield();
            }
            value = v + 1;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Runs a counter test with n-thread in //.
     */
    public static class TestRunner {
        public final Counter syncCounter = new Counter();
        public final Counter concCounter = new Counter();

        public void run(final Runnable runnable) throws InterruptedException {
            final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
            for (int i = 0; i < NUM_THREADS; i++) {
                executor.submit(runnable);
            }
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            // this may succeed concurrently if there is only one 'real' thread
            // during execution; we can only prove the 'synchronized' if the unsync-ed
            // version fails...
            if (NUM_THREADS * NUM_ITERATIONS != concCounter.getValue()) {
                Assert.assertEquals(NUM_THREADS * NUM_ITERATIONS, syncCounter.getValue());
            }
        }
    }

    @Test
    /**
     * A base test to ensure synchronized makes a difference.
     */
    public void testSynchronized() throws InterruptedException {
        final TestRunner tr = new TestRunner();
        final Counter syncCounter = tr.syncCounter;
        final Counter concCounter = tr.concCounter;
        tr.run(() -> {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                synchronized (syncCounter) {
                    syncCounter.inc();
                }
                concCounter.inc();
            }
        });
    }

    @Test
    public void testJexlSynchronized0() throws InterruptedException {
        final TestRunner tr = new TestRunner();
        final AnnotationContext ctxt = new AnnotationContext();
        final JexlScript script = JEXL.createScript(
                "for(var i : 1..NUM_ITERATIONS) {"
                + "@synchronized { syncCounter.inc(); }"
                + "concCounter.inc();"
                + "}",
                "NUM_ITERATIONS",
                "syncCounter",
                "concCounter");
        // will sync on syncCounter
        tr.run(() -> {
            script.execute(ctxt, NUM_ITERATIONS, tr.syncCounter, tr.concCounter);
        });
    }
}
