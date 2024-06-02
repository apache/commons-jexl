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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.jexl3.internal.Script;
import org.apache.commons.lang3.ThreadUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests around asynchronous script execution and interrupts.
 */
@SuppressWarnings({ "UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes" })
public class ScriptCallableTest extends JexlTestCase {
    public static class AnnotationContext extends MapContext implements JexlContext.AnnotationProcessor {
        @Override
        public Object processAnnotation(final String name, final Object[] args, final Callable<Object> statement) throws Exception {
            if ("timeout".equals(name) && args != null && args.length > 0) {
                final long ms = args[0] instanceof Number ? ((Number) args[0]).longValue() : Long.parseLong(args[0].toString());
                final Object def = args.length > 1 ? args[1] : null;
                if (ms > 0) {
                    final ExecutorService executor = Executors.newFixedThreadPool(1);
                    Future<?> future = null;
                    try {
                        future = executor.submit(statement);
                        return future.get(ms, TimeUnit.MILLISECONDS);
                    } catch (final TimeoutException xtimeout) {
                        future.cancel(true);
                    } finally {
                        executor.shutdown();
                    }

                }
                return def;
            }
            return statement.call();
        }

        public void sleep(final long ms) throws InterruptedException {
            Thread.sleep(ms);
        }

    }

    public static class CancellationContext extends MapContext implements JexlContext.CancellationHandle {
        private final AtomicBoolean cancellation;

        CancellationContext(final AtomicBoolean c) {
            cancellation = c;
        }

        @Override
        public AtomicBoolean getCancellation() {
            return cancellation;
        }
    }

    public static class TestContext extends MapContext implements JexlContext.NamespaceResolver {
        public int hangs(final Object t) {
            return 1;
        }

        public int interrupt() {
            Thread.currentThread().interrupt();
            return 42;
        }

        @Override
        public Object resolveNamespace(final String name) {
            return name == null ? this : null;
        }

        public int runForever() {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
            return 1;
        }

        public void sleep(final long millis) throws InterruptedException {
            Thread.sleep(millis);
        }

        public int wait(final int s) throws InterruptedException {
            Thread.sleep(1000 * s);
            return s;
        }

        public int waitInterrupt(final int s) {
            try {
                Thread.sleep(1000 * s);
                return s;
            } catch (final InterruptedException xint) {
                Thread.currentThread().interrupt();
            }
            return -1;
        }
    }

    // private Log logger = LogFactory.getLog(JexlEngine.class);
    public ScriptCallableTest() {
        super("ScriptCallableTest");
    }

    /**
     * Redundant test with previous ones but impervious to JEXL engine configuration.
     *
     * @throws Exception if there is a regression
     */
    private void runInterrupt(final JexlEngine jexl) throws Exception {
        List<Runnable> lr = null;
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            final JexlContext ctxt = new TestContext();

            // run an interrupt
            final JexlScript sint = jexl.createScript("interrupt(); return 42");
            Object t = null;
            Script.Callable c = (Script.Callable) sint.callable(ctxt);
            try {
                t = c.call();
                assertFalse(c.isCancellable(), "should have thrown a Cancel");
            } catch (final JexlException.Cancel xjexl) {
                assertTrue(c.isCancellable(), () -> "should not have thrown " + xjexl);
            }
            assertTrue(c.isCancelled());
            assertNotEquals(42, t);

            // self interrupt
            c = (Script.Callable) sint.callable(ctxt);
            final Future<Object> f = executorService.submit(c);
            try {
                t = f.get();
                assertFalse(c.isCancellable(), "should have thrown a Cancel");
            } catch (final ExecutionException xexec) {
                assertTrue(c.isCancellable(), () -> "should not have thrown " + xexec);
            }
            assertTrue(c.isCancelled());
            assertNotEquals(42, t);

            // timeout a sleep
            final JexlScript ssleep = jexl.createScript("sleep(30000); return 42");
            final Future<Object> f0 = executorService.submit(ssleep.callable(ctxt));
            assertThrows(TimeoutException.class, () -> f0.get(100L, TimeUnit.MILLISECONDS));
            f0.cancel(true);
            assertNotEquals(42, t);

            // cancel a sleep
            final Future<Object> fc0 = executorService.submit(ssleep.callable(ctxt));
            final Runnable cancels0 = () -> {
                ThreadUtils.sleepQuietly(Duration.ofMillis(200));
                fc0.cancel(true);
            };
            executorService.submit(cancels0);
            assertThrows(CancellationException.class, () -> f0.get(100L, TimeUnit.MILLISECONDS));

            // timeout a while(true)
            final JexlScript swhile = jexl.createScript("while(true); return 42");
            final Future<Object> f1 = executorService.submit(swhile.callable(ctxt));
            assertThrows(TimeoutException.class, () -> f1.get(100L, TimeUnit.MILLISECONDS));
            f1.cancel(true);

            assertNotEquals(42, t);

            // cancel a while(true)
            final Future<Object> fc = executorService.submit(swhile.callable(ctxt));
            final Runnable cancels = () -> {
                ThreadUtils.sleepQuietly(Duration.ofMillis(200));
                fc.cancel(true);
            };
            executorService.submit(cancels);
            assertThrows(CancellationException.class, fc::get);
            assertNotEquals(42, t);
        } finally {
            lr = executorService.shutdownNow();
        }
        assertTrue(lr.isEmpty());
    }

    @Test
    public void testCallableCancel() throws Exception {
        final Semaphore latch = new Semaphore(0);
        final JexlContext ctxt = new MapContext();
        ctxt.set("latch", latch);

        final JexlScript e = JEXL.createScript("latch.release(); while(true);");
        final Script.Callable c = (Script.Callable) e.callable(ctxt);
        final Callable<Object> kc = () -> {
            latch.acquire();
            return c.cancel();
        };
        final List<Runnable> list;
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<?> future = executor.submit(c);
            final Future<?> kfc = executor.submit(kc);
            assertTrue((Boolean) kfc.get());
            final ExecutionException xexec = assertThrows(ExecutionException.class, future::get);
            assertTrue(xexec.getCause() instanceof JexlException.Cancel);
        } finally {
            list = executor.shutdownNow();
        }
        assertTrue(c.isCancelled());
        assertTrue(list == null || list.isEmpty());
    }

    /**
     * Tests JEXL-317.
     */
    @Test
    public void testCallableCancellation() throws Exception {
        final Semaphore latch = new Semaphore(0);
        final AtomicBoolean cancel = new AtomicBoolean(false);
        final JexlContext ctxt = new CancellationContext(cancel);
        ctxt.set("latch", latch);

        final JexlScript e = JEXL.createScript("latch.release(); while(true);");
        final Script.Callable c = (Script.Callable) e.callable(ctxt);
        final Callable<Object> kc = () -> {
            latch.acquire();
            return cancel.compareAndSet(false, true);
        };
        final List<Runnable> list;
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<?> future = executor.submit(c);
            final Future<?> kfc = executor.submit(kc);
            try {
                assertTrue((Boolean) kfc.get());
                future.get();
                fail("should have been cancelled");
            } catch (final ExecutionException xexec) {
                // ok, ignore
                assertTrue(xexec.getCause() instanceof JexlException.Cancel);
            }
        } finally {
            list = executor.shutdownNow();
        }
        assertTrue(c.isCancelled());
        assertTrue(list == null || list.isEmpty());
    }

    @Test
    public void testCallableClosure() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("function(t) {while(t);}");
        final Callable<Object> c = e.callable(null, Boolean.TRUE);
        Object t = 42;

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        assertTrue(future.isCancelled());
        assertEquals(42, t);
        assertTrue(lr.isEmpty());
    }

    @Test
    public void testCallableTimeout() throws Exception {
        List<Runnable> lr = null;
        final Semaphore latch = new Semaphore(0);
        final JexlContext ctxt = new MapContext();
        ctxt.set("latch", latch);

        final JexlScript e = JEXL.createScript("latch.release(); while(true);");
        final Callable<Object> c = e.callable(ctxt);
        Object t = 42;

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        try {
            latch.acquire();
            t = future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        assertTrue(future.isCancelled());
        assertEquals(42, t);
        assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelForever() throws Exception {
        List<Runnable> lr = null;
        final Semaphore latch = new Semaphore(0);
        final JexlContext ctxt = new TestContext();
        ctxt.set("latch", latch);

        final JexlScript e = JEXL.createScript("latch.release(); runForever()");
        final Callable<Object> c = e.callable(ctxt);

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            latch.acquire();
            t = future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        assertTrue(future.isCancelled());
        assertEquals(42, t);
        assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelLoopWait() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("while (true) { wait(10) }");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        assertTrue(future.isCancelled());
        assertEquals(42, t);
        assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelWait() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("wait(10)");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            final Future<?> future = executor.submit(c);
            Object t = 42;
            try {
                t = future.get(100, TimeUnit.MILLISECONDS);
                fail("should have timed out");
            } catch (final TimeoutException xtimeout) {
                // ok, ignore
                future.cancel(true);
            }
            assertTrue(future.isCancelled());
            assertEquals(42, t);
        } finally {
            lr = executor.shutdownNow();
        }
        assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelWaitInterrupt() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("waitInterrupt(42)");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        final Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        assertTrue(future.isCancelled());
        assertEquals(42, t);
        assertTrue(lr.isEmpty());
    }

    @Test
    public void testFuture() throws Exception {
        final JexlScript e = JEXL.createScript("while(true);");
        final FutureTask<Object> future = new FutureTask<>(e.callable(null));

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(future);
        Object t = 42;
        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (final TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            executor.shutdown();
        }

        assertTrue(future.isCancelled());
        assertEquals(42, t);
    }

    @Test
    public void testHangs() throws Exception {
        final JexlScript e = JEXL.createScript("hangs()");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            final Future<?> future = executor.submit(c);
            final Object t = future.get(1, TimeUnit.SECONDS);
            fail("hangs should not be solved");
        } catch (final ExecutionException xexec) {
            assertTrue(xexec.getCause() instanceof JexlException.Method);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testInterruptCancellable() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(true).cancellable(true).create());
    }

    @Test
    public void testInterruptSilentLenient() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(false).create());
    }

    @Test
    public void testInterruptSilentStrict() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(true).create());
    }

    @Test
    public void testInterruptVerboseLenient() throws Exception {
        runInterrupt(new JexlBuilder().silent(false).strict(false).create());
    }

    @Test
    public void testInterruptVerboseStrict() throws Exception {
        runInterrupt(new JexlBuilder().silent(false).strict(true).create());
    }

    @Test
    public void testNoWait() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("wait(0)");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            final Future<?> future = executor.submit(c);
            final Object t = future.get(2, TimeUnit.SECONDS);
            assertTrue(future.isDone());
            assertEquals(0, t);
        } finally {
            lr = executor.shutdownNow();
        }
        assertTrue(lr.isEmpty());
    }

    @Test
    public void testTimeout() throws Exception {
        JexlScript script = JEXL.createScript("(flag)->{ @timeout(100) { while(flag); return 42 }; 'cancelled' }");
        final JexlContext ctxt = new AnnotationContext();
        Object result = null;
        try {
            result = script.execute(ctxt, true);
        } catch (final Exception xany) {
            if (xany.getCause() != null) {
                fail(xany.getCause().toString());
            } else {
                fail(xany.toString());
            }
        }
        assertEquals("cancelled", result);

        result = script.execute(ctxt, false);
        assertEquals(42, result);
        script = JEXL.createScript("(flag)->{ @timeout(100, 'cancelled') { while(flag); 42; } }");
        try {
            result = script.execute(ctxt, true);
        } catch (final Exception xany) {
            fail(xany.toString());
        }
        assertEquals("cancelled", result);

        result = script.execute(ctxt, false);
        assertEquals(42, result);
        script = JEXL.createScript("@timeout(100) {sleep(1000); 42; } -42;");
        try {
            result = script.execute(ctxt);
        } catch (final Exception xany) {
            fail(xany.toString());
        }
        assertEquals(-42, result);

        script = JEXL.createScript("@timeout(100) {sleep(1000); return 42; } return -42;");
        try {
            result = script.execute(ctxt);
        } catch (final Exception xany) {
            fail(xany.toString());
        }
        assertEquals(-42, result);
        script = JEXL.createScript("@timeout(1000) {sleep(100); return 42; } return -42;");
        try {
            result = script.execute(ctxt);
        } catch (final Exception xany) {
            fail(xany.toString());
        }
        assertEquals(42, result);
    }

    @Test
    public void testWait() throws Exception {
        List<Runnable> lr = null;
        final JexlScript e = JEXL.createScript("wait(1)");
        final Callable<Object> c = e.callable(new TestContext());

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            final Future<?> future = executor.submit(c);
            final Object t = future.get(2, TimeUnit.SECONDS);
            assertEquals(1, t);
        } finally {
            lr = executor.shutdownNow();
        }
        assertTrue(lr.isEmpty());
    }
}
