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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tests around asynchronous script execution and interrupts.
 */
public class ScriptCallableTest extends JexlTestCase {
    //Logger LOGGER = Logger.getLogger(VarTest.class.getName());
    public ScriptCallableTest() {
        super("ScriptCallableTest");
    }

    public void testFuture() throws Exception {
        JexlScript e = JEXL.createScript("while(true);");
        FutureTask<Object> future = new FutureTask<Object>(e.callable(null));

        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(future);
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
        }
        Thread.sleep(100);
        future.cancel(true);

        assertTrue(future.isCancelled());
    }

    public void testCallable() throws Exception {
        JexlScript e = JEXL.createScript("while(true);");
        Callable<Object> c = e.callable(null);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
        }
        future.cancel(true);
        assertTrue(future.isCancelled());
    }
    
    public void testCallableClosure() throws Exception {
        JexlScript e = JEXL.createScript("function(t) {while(t);}");
        e = (JexlScript) e.execute(null);
        Callable<Object> c = e.callable(null, Boolean.TRUE);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
        }
        future.cancel(true);
        assertTrue(future.isCancelled());
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
            } catch(InterruptedException xint) {
                Thread.currentThread().interrupt();
            }
            return -1;
        }
        
        public int runForever() {
            boolean x = false;
            while(true) {
                if (x) {
                   break;
                }
            }
            return 1;
        }
    }

    public void testNoWait() throws Exception {
        JexlScript e = JEXL.createScript("wait(0)");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        Object t = future.get(2, TimeUnit.SECONDS);
        assertTrue(future.isDone());
        assertEquals(0, t);
    }
    
    public void testWait() throws Exception {
        JexlScript e = JEXL.createScript("wait(1)");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        Object t = future.get(2, TimeUnit.SECONDS);
        assertEquals(1, t);
    }

    public void testCancelWait() throws Exception {
        JexlScript e = JEXL.createScript("wait(10)");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
        }
        future.cancel(true);
        assertTrue(future.isCancelled());
    }
    
    public void testCancelWaitInterrupt() throws Exception {
        JexlScript e = JEXL.createScript("waitInterrupt(42)");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
        }
        future.cancel(true);
        assertTrue(future.isCancelled());
    }
    
    public void testCancelForever() throws Exception {
        JexlScript e = JEXL.createScript("runForever()");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
        }
        future.cancel(true);
        assertTrue(future.isCancelled());
    }
    
    public void testCancelLoopWait() throws Exception {
        JexlScript e = JEXL.createScript("while (true) { wait(10) }");
        Callable<Object> c = e.callable(new TestContext());

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Future<?> future = executor.submit(c);
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("should have timed out");
        } catch (TimeoutException xtimeout) {
            // ok, ignore
        }
        future.cancel(true);
        assertTrue(future.isCancelled());
    }
}
