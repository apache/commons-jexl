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
import org.apache.commons.jexl3.internal.Script;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests around asynchronous script execution and interrupts.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ScriptCallableTest extends JexlTestCase {
    //private Log logger = LogFactory.getLog(JexlEngine.class);
    public ScriptCallableTest() {
        super("ScriptCallableTest");
    }

    @Test
    public void testFuture() throws Exception {
        JexlScript e = JEXL.createScript("while(true);");
        FutureTask<Object> future = new FutureTask<Object>(e.callable(null));

        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(future);
        Object t = 42;
        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            executor.shutdown();
        }

        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
    }

    @Test
    public void testCallableCancel() throws Exception {
        List<Runnable> lr = null;
        final Semaphore latch = new Semaphore(0);
        JexlContext ctxt = new MapContext();
        ctxt.set("latch", latch);

        JexlScript e = JEXL.createScript("latch.release(); while(true);");
        final Script.CallableScript c = (Script.CallableScript) e.callable(ctxt);
        Object t = 42;
        Callable<Object> kc = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                latch.acquire();
                return c.cancel();
            }
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> future = executor.submit(c);
        Future<?> kfc = executor.submit(kc);
        try {
            Assert.assertTrue((Boolean) kfc.get());
            t = future.get();
            Assert.fail("should have been cancelled");
        } catch (ExecutionException xexec) {
            // ok, ignore
            Assert.assertTrue(xexec.getCause() instanceof JexlException.Cancel);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(c.isCancelled());
        Assert.assertTrue(lr == null || lr.isEmpty());
    }

    @Test
    public void testCallableTimeout() throws Exception {
        List<Runnable> lr = null;
        final Semaphore latch = new Semaphore(0);
        JexlContext ctxt = new MapContext();
        ctxt.set("latch", latch);

        JexlScript e = JEXL.createScript("latch.release(); while(true);");
        Callable<Object> c = e.callable(ctxt);
        Object t = 42;

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        try {
            latch.acquire();
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCallableClosure() throws Exception {
        List<Runnable> lr = null;
        JexlScript e = JEXL.createScript("function(t) {while(t);}");
        Callable<Object> c = e.callable(null, Boolean.TRUE);
        Object t = 42;

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    public static class TestContext extends MapContext implements JexlContext.NamespaceResolver {
        @Override
        public Object resolveNamespace(String name) {
            return name == null ? this : null;
        }

        public int wait(int s) throws InterruptedException {
            Thread.sleep(1000 * s);
            return s;
        }

        public int waitInterrupt(int s) {
            try {
                Thread.sleep(1000 * s);
                return s;
            } catch (InterruptedException xint) {
                Thread.currentThread().interrupt();
            }
            return -1;
        }

        public int runForever() {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
            return 1;
        }

        public int interrupt() throws InterruptedException {
            Thread.currentThread().interrupt();
            return 42;
        }

        public void sleep(long millis) throws InterruptedException {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException xint) {
                throw xint;
            }
        }

        public int hangs(Object t) {
            return 1;
        }
    }

    @Test
    public void testNoWait() throws Exception {
        List<Runnable> lr = null;
        JexlScript e = JEXL.createScript("wait(0)");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<?> future = executor.submit(c);
            Object t = future.get(2, TimeUnit.SECONDS);
            Assert.assertTrue(future.isDone());
            Assert.assertEquals(0, t);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testWait() throws Exception {
        List<Runnable> lr = null;
        JexlScript e = JEXL.createScript("wait(1)");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<?> future = executor.submit(c);
            Object t = future.get(2, TimeUnit.SECONDS);
            Assert.assertEquals(1, t);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelWait() throws Exception {
        List<Runnable> lr = null;
        JexlScript e = JEXL.createScript("wait(10)");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<?> future = executor.submit(c);
            Object t = 42;
            try {
                t = future.get(100, TimeUnit.MILLISECONDS);
                Assert.fail("should have timed out");
            } catch (TimeoutException xtimeout) {
                // ok, ignore
                future.cancel(true);
            }
            Assert.assertTrue(future.isCancelled());
            Assert.assertEquals(42, t);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelWaitInterrupt() throws Exception {
        List<Runnable> lr = null;
        JexlScript e = JEXL.createScript("waitInterrupt(42)");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelForever() throws Exception {
        List<Runnable> lr = null;
        final Semaphore latch = new Semaphore(0);
        JexlContext ctxt = new TestContext();
        ctxt.set("latch", latch);

        JexlScript e = JEXL.createScript("latch.release(); runForever()");
        Callable<Object> c = e.callable(ctxt);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            latch.acquire();
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testCancelLoopWait() throws Exception {
        List<Runnable> lr = null;
        JexlScript e = JEXL.createScript("while (true) { wait(10) }");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        Object t = 42;

        try {
            t = future.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            future.cancel(true);
        } finally {
            lr = executor.shutdownNow();
        }
        Assert.assertTrue(future.isCancelled());
        Assert.assertEquals(42, t);
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testInterruptVerboseStrict() throws Exception {
        runInterrupt(new JexlBuilder().silent(false).strict(true).create());
    }

    @Test
    public void testInterruptVerboseLenient() throws Exception {
        runInterrupt(new JexlBuilder().silent(false).strict(false).create());
    }

    @Test
    public void testInterruptSilentStrict() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(true).create());
    }

    @Test
    public void testInterruptSilentLenient() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(false).create());
    }

    @Test
    public void testInterruptCancellable() throws Exception {
        runInterrupt(new JexlBuilder().silent(true).strict(true).cancellable(true).create());
    }

    /**
     * Redundant test with previous ones but impervious to JEXL engine configuation.
     * @param silent silent engine flag
     * @param strict strict (aka not lenient) engine flag
     * @throws Exception if there is a regression
     */
    private void runInterrupt(JexlEngine jexl) throws Exception {
        List<Runnable> lr = null;
        ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            JexlContext ctxt = new TestContext();

            // run an interrupt
            JexlScript sint = jexl.createScript("interrupt(); return 42");
            Object t = null;
            Script.CallableScript c = (Script.CallableScript) sint.callable(ctxt);
            try {
                t = c.call();
                if (c.isCancellable()) {
                    Assert.fail("should have thrown a Cancel");
                }
            } catch (JexlException.Cancel xjexl) {
                if (!c.isCancellable()) {
                    Assert.fail("should not have thrown " + xjexl);
                }
            }
            Assert.assertTrue(c.isCancelled());
            Assert.assertNotEquals(42, t);

            // self interrupt
            Future<Object> f = null;
            c = (Script.CallableScript) sint.callable(ctxt);
            try {
                f = exec.submit(c);
                t = f.get();
                if (c.isCancellable()) {
                    Assert.fail("should have thrown a Cancel");
                }
            } catch (ExecutionException xexec) {
                if (!c.isCancellable()) {
                    Assert.fail("should not have thrown " + xexec);
                }
            }
            Assert.assertTrue(c.isCancelled());
            Assert.assertNotEquals(42, t);

            // timeout a sleep
            JexlScript ssleep = jexl.createScript("sleep(30000); return 42");
            try {
                f = exec.submit(ssleep.callable(ctxt));
                t = f.get(100L, TimeUnit.MILLISECONDS);
                Assert.fail("should timeout");
            } catch (TimeoutException xtimeout) {
                if (f != null) {
                    f.cancel(true);
                }
            }
            Assert.assertNotEquals(42, t);

            // cancel a sleep
            try {
                final Future<Object> fc = exec.submit(ssleep.callable(ctxt));
                Runnable cancels = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(200L);
                        } catch (Exception xignore) {

                        }
                        fc.cancel(true);
                    }
                };
                exec.submit(cancels);
                t = f.get(100L, TimeUnit.MILLISECONDS);
                Assert.fail("should be cancelled");
            } catch (CancellationException xexec) {
                // this is the expected result
            }

            // timeout a while(true)
            JexlScript swhile = jexl.createScript("while(true); return 42");
            try {
                f = exec.submit(swhile.callable(ctxt));
                t = f.get(100L, TimeUnit.MILLISECONDS);
                Assert.fail("should timeout");
            } catch (TimeoutException xtimeout) {
                if (f != null) {
                    f.cancel(true);
                }
            }
            Assert.assertNotEquals(42, t);

            // cancel a while(true)
            try {
                final Future<Object> fc = exec.submit(swhile.callable(ctxt));
                Runnable cancels = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(200L);
                        } catch (Exception xignore) {

                        }
                        fc.cancel(true);
                    }
                };
                exec.submit(cancels);
                t = fc.get();
                Assert.fail("should be cancelled");
            } catch (CancellationException xexec) {
                // this is the expected result
            }
            Assert.assertNotEquals(42, t);
        } finally {
            lr = exec.shutdownNow();
        }
        Assert.assertTrue(lr.isEmpty());
    }

    @Test
    public void testHangs() throws Exception {
        JexlScript e = JEXL.createScript("hangs()");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            Future<?> future = executor.submit(c);
            Object t = future.get(1, TimeUnit.SECONDS);
            Assert.fail("hangs should not be solved");
        } catch(ExecutionException xexec) {
            Assert.assertTrue(xexec.getCause() instanceof JexlException.Method);
        } finally {
            executor.shutdown();
        }
    }

    public static class AnnotationContext extends MapContext implements JexlContext.AnnotationProcessor {
        @Override
        public Object processAnnotation(String name, Object[] args, Callable<Object> statement) throws Exception {
            if ("timeout".equals(name) && args != null && args.length > 0) {
                long ms = args[0] instanceof Number
                          ? ((Number) args[0]).longValue()
                          : Long.parseLong(args[0].toString());
                Object def = args.length > 1? args[1] : null;
                if (ms > 0) {
                    ExecutorService executor = Executors.newFixedThreadPool(1);
                    Future<?> future = null;
                    try {
                        future = executor.submit(statement);
                        return future.get(ms, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException xtimeout) {
                        if (future != null) {
                            future.cancel(true);
                        }
                    } finally {
                        executor.shutdown();
                    }

                }
                return def;
            }
            return statement.call();
        }

        public void sleep(long ms) throws InterruptedException {
           Thread.sleep(ms);
        }

    }

    @Test
    public void testTimeout() throws Exception {
        JexlScript script = JEXL.createScript("(flag)->{ @timeout(100) { while(flag); return 42 }; 'cancelled' }");
        JexlContext ctxt = new AnnotationContext();
        Object result = null;
        try {
            result = script.execute(ctxt, true);
        } catch (Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals("cancelled", result);

        result = script.execute(ctxt, false);
        Assert.assertEquals(42, result);
        script = JEXL.createScript("(flag)->{ @timeout(100, 'cancelled') { while(flag); 42; } }");
        try {
            result = script.execute(ctxt, true);
        } catch (Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals("cancelled", result);

        result = script.execute(ctxt, false);
        Assert.assertEquals(42, result);
        script = JEXL.createScript("@timeout(10) {sleep(1000); 42; } -42;");
        try {
            result = script.execute(ctxt);
        } catch (Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals(-42, result);

        script = JEXL.createScript("@timeout(10) {sleep(1000); return 42; } return -42;");
        try {
            result = script.execute(ctxt);
        } catch (Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals(-42, result);
        script = JEXL.createScript("@timeout(1000) {sleep(10); return 42; } return -42;");
        try {
            result = script.execute(ctxt);
        } catch (Exception xany) {
            Assert.fail(xany.toString());
        }
        Assert.assertEquals(42, result);
    }
}
