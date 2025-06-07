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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

/**
 * Testing JEXL caching performance.
 * <p>
 * The core idea is to create and evaluate scripts concurrently by a number (THREADS) of threads. The scripts source are number constants to keep the actual
 * cost of evaluation to a minimum but we still want these to occur; the cost of creating the evaluation environment, etc is not negligible and there is no use
 * in trying to cache/get scripts if its not to evaluate them. Each script is evaluated a number of times (HIT) in a tight loop to try and elicit good hit
 * ratios but the number of potential scripts (SCRIPTS) is greater than the cache capacity (CACHED) to force eviction.
 * </p>
 * <p>
 * The results vary a bit with parameters but the wall-clock times of the tests shows the worst case as 135% of best (the former being the default cache, the
 * latter being the Google based cache). This indicates that the basic caching mechanism will likely not be a performance bottleneck in normal usage.
 * </p>
 */
public class CachePerformanceTest {
    /**
     * A task randomly chooses to run scripts (CACHED * HIT times). Tasks will be run
     */
    static class Task implements Callable<Integer> {
        private final JexlEngine jexl;
        private final BlockingQueue<?> queue;

        Task(final JexlEngine jexl, final BlockingQueue<?> queue) {
            this.jexl = jexl;
            this.queue = queue;
        }

        @Override
        public Integer call() {
            int count = 0;
            Object arg;
            try {
                while ((arg = queue.take()) != Task.class) {
                    final Random rnd = new Random((int) arg);
                    for (int l = 0; l < LOOPS; ++l) {
                        for (int c = 0; c < CACHED; ++c) {
                            final int ctl = rnd.nextInt(SCRIPTS);
                            for (int r = 0; r < HIT; ++r) {
                                final JexlScript script = jexl.createScript(Integer.toString(ctl));
                                final Object result = script.execute(null);
                                assertEquals(((Number) result).intValue(), ctl);
                                count += 1;
                            }
                        }
                    }
                }
                return count;
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Timer {
        long begin;
        long end;

        String elapse() {
            final long delta = end - begin;
            final NumberFormat fmt = new DecimalFormat("#.###");
            return fmt.format(delta / 1000.d);
        }

        void start() {
            begin = System.currentTimeMillis();
        }

        void stop() {
            end = System.currentTimeMillis();
        }
    }

    /** Number of test loops. */
    private static final int LOOPS = 10; // 0;
    /** Number of different scripts. */
    private static final int SCRIPTS = 800; // 0;
    /** Cache capacity. */
    private static final int CACHED = 500; // 0;
    /** Number of times each script is evaluated. */
    private static final int HIT = 5;

    /** Number of concurrent threads. */
    private static final int THREADS = 8;

    /** The logger. */
    Log LOGGER = LogFactory.getLog(getClass());

    /**
     * Launches the tasks in parallel.
     *
     * @param jexl the jexl engine
     * @throws Exception if something goes wrong
     */
    protected void runTest(final String name, final JexlEngine jexl) throws Exception {
        final ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(THREADS);
        final List<Future<Integer>> results = new ArrayList<>(THREADS);
        // seed the cache
        for (int i = 0; i < CACHED; ++i) {
            final JexlScript script = jexl.createScript(Integer.toString(i));
            assertNotNull(script);
        }
        // create a set of tasks ready to go
        for (int t = 0; t < THREADS; ++t) {
            results.add(exec.submit(new Task(jexl, queue)));
        }
        final Timer tt = new Timer();
        tt.start();
        // run each with its own sequence of random seeded by t
        for (int t = 0; t < THREADS; ++t) {
            queue.put(t);
        }
        // send the poison pill
        for (int t = 0; t < THREADS; ++t) {
            queue.put(Task.class);
        }
        int total = 0;
        for (final Future<Integer> result : results) {
            total += result.get();
        }
        exec.shutdown();
        tt.stop();
        assertEquals(total, LOOPS * CACHED * THREADS * HIT);
        LOGGER.info(name + " : " + tt.elapse());
    }

    @Test
    void testConcurrent() throws Exception {
        final JexlBuilder builder = new JexlBuilder().cacheFactory(ConcurrentCache::new).cache(CACHED);
        final JexlEngine jexl = builder.create();
        runTest("testConcurrent", jexl);
    }

    @Test
    void testSpread() throws Exception {
        final JexlBuilder builder = new JexlBuilder().cacheFactory(SpreadCache::new).cache(CACHED);
        final JexlEngine jexl = builder.create();
        runTest("testSpread", jexl);
    }

    @Test
    void testSynchronized() throws Exception {
        final JexlBuilder builder = new JexlBuilder().cache(CACHED);
        final JexlEngine jexl = builder.create();
        runTest("testSynchronized", jexl);
    }
}
