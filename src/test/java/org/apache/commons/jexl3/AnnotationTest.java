/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jexl3.internal.Interpreter;
import org.junit.jupiter.api.Test;

/**
 * Test cases for annotations.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})

public class AnnotationTest extends JexlTestCase {

    public static class AnnotationContext extends MapContext implements JexlContext.AnnotationProcessor {
        private int count;
        private final Set<String> names = new TreeSet<>();

        public int getCount() {
            return count;
        }

        public Set<String> getNames() {
            return names;
        }

        @Override
        public Object processAnnotation(final String name, final Object[] args, final Callable<Object> statement) throws Exception {
            count += 1;
            names.add(name);
            switch (name) {
            case "one":
                names.add(args[0].toString());
                break;
            case "two":
                names.add(args[0].toString());
                names.add(args[1].toString());
                break;
            case "error":
                names.add(args[0].toString());
                throw new IllegalArgumentException(args[0].toString());
            case "unknown":
                return null;
            case "synchronized": {
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
                break;
            }
            default:
                break;
            }
            return statement.call();
        }
    }
    /**
     * A counter whose inc method will misbehave if not mutex-ed.
     */
    public static class Counter {
        private int value;

        public int getValue() {
            return value;
        }

        public void inc() {
            final int v = value;
            // introduce some concurency
            for (int i = (int) System.currentTimeMillis() % 5; i >= 0; --i) {
                Thread.yield();
            }
            value = v + 1;
        }
    }

    public static class OptAnnotationContext extends JexlEvalContext implements JexlContext.AnnotationProcessor {
        @Override
        public Object processAnnotation(final String name, final Object[] args, final Callable<Object> statement) throws Exception {
            final JexlOptions options = getEngineOptions();
            // transient side effect for strict

            // transient side effect for silent

            // durable side effect for scale
            switch (name) {
            case "strict": {
                final boolean s = (Boolean) args[0];
                final boolean b = options.isStrict();
                options.setStrict(s);
                final Object r = statement.call();
                options.setStrict(b);
                return r;
            }
            case "silent": {
                if (args != null && args.length != 0) {
                    final boolean s = (Boolean) args[0];
                    final boolean b = options.isSilent();
                    options.setSilent(s);
                    assertEquals(s, options.isSilent());
                    final Object r = statement.call();
                    options.setSilent(b);
                    return r;
                }
                final boolean b = options.isSilent();
                try {
                    return statement.call();
                } catch (final JexlException xjexl) {
                    return null;
                } finally {
                    options.setSilent(b);
                }
            }
            case "scale":
                options.setMathScale((Integer) args[0]);
                return statement.call();
            default:
                break;
            }
            return statement.call();
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
                assertEquals(NUM_THREADS * NUM_ITERATIONS, syncCounter.getValue());
            }
        }
    }

    public static final int NUM_THREADS = 10;

    public static final int NUM_ITERATIONS = 1000;

    public AnnotationTest() {
        super("AnnotationTest");
    }

    @Test
    void test197a() throws Exception {
        final JexlContext jc = new MapContext();
        final JexlScript e = JEXL.createScript("@synchronized { return 42; }");
        final Object r = e.execute(jc);
        assertEquals(42, r);
    }

    @Test
    void testError() throws Exception {
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
                fail("should have failed");
            } else {
                assertEquals(1, log.count("warn"));
            }
        } catch (final JexlException.Annotation xjexl) {
            assertEquals("error", xjexl.getAnnotation());
        }
        assertEquals(1, jc.getCount());
        assertTrue(jc.getNames().contains("error"));
        assertTrue(jc.getNames().contains("42"));
        if (!silent) {
            assertEquals(0, log.count("warn"));
        }
    }

    @Test
    void testHoistingStatement() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("var t = 1; @synchronized for(var x : [2,3,7]) t *= x; t");
        final Object r = e.execute(jc);
        assertEquals(42, r);
        assertEquals(1, jc.getCount());
        assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    void testJexlSynchronized0() throws InterruptedException {
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

    @Test
    void testMultiple() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@one(1) @synchronized { return 42; }");
        final Object r = e.execute(jc);
        assertEquals(42, r);
        assertEquals(2, jc.getCount());
        assertTrue(jc.getNames().contains("synchronized"));
        assertTrue(jc.getNames().contains("one"));
        assertTrue(jc.getNames().contains("1"));
    }

    @Test
    void testNoArg() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@synchronized { return 42; }");
        final Object r = e.execute(jc);
        assertEquals(42, r);
        assertEquals(1, jc.getCount());
        assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    void testNoArgExpression() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@synchronized 42");
        final Object r = e.execute(jc);
        assertEquals(42, r);
        assertEquals(1, jc.getCount());
        assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    void testNoArgStatement() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@synchronized if (true) 2 * 3 * 7; else -42;");
        final Object r = e.execute(jc);
        assertEquals(42, r);
        assertEquals(1, jc.getCount());
        assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    void testOneArg() throws Exception {
        final AnnotationContext jc = new AnnotationContext();
        final JexlScript e = JEXL.createScript("@one(1) { return 42; }");
        final Object r = e.execute(jc);
        assertEquals(42, r);
        assertEquals(1, jc.getCount());
        assertTrue(jc.getNames().contains("one"));
        assertTrue(jc.getNames().contains("1"));
    }

    @Test
    /**
     * A base test to ensure synchronized makes a difference.
     */
    void testSynchronized() throws InterruptedException {
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
    void testUnknown() throws Exception {
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
                fail("should have failed");
            } else {
                assertEquals(1, log.count("warn"));
            }
        } catch (final JexlException.Annotation xjexl) {
            assertEquals("unknown", xjexl.getAnnotation());
        }
        assertEquals(1, jc.getCount());
        assertTrue(jc.getNames().contains("unknown"));
        assertFalse(jc.getNames().contains("42"));
        if (!silent) {
            assertEquals(0, log.count("warn"));
        }
    }

    @Test
    void testVarStmt() throws Exception {
        final OptAnnotationContext jc = new OptAnnotationContext();
        final JexlOptions options = jc.getEngineOptions();
        jc.getEngineOptions().set(JEXL);
        options.setSharedInstance(true);
        Object r;
        final JexlScript e = JEXL.createScript("(s, v)->{ @strict(s) @silent(v) var x = y ; 42; }");

        // wont make an error
        r = e.execute(jc, false, true);
        assertEquals(42, r);

        r = null;
        // will make an error and throw
        options.setSafe(false);
        assertThrows(JexlException.Variable.class, () -> e.execute(jc, true, false));

        r = null;
        // will make an error and will not throw but result is null
        r = e.execute(jc, true, true);
        assertNull(r);
        options.setSafe(true);

        r = null;
        // will not make an error and will not throw
        r = e.execute(jc, false, false);
        assertEquals(42, r);
        // assertEquals(42, r);
        assertTrue(options.isStrict());
        final JexlScript e2 = JEXL.createScript("@scale(5) 42;");
        r = e2.execute(jc);
        assertEquals(42, r);
        assertTrue(options.isStrict());
        assertEquals(5, options.getMathScale());
    }
}
