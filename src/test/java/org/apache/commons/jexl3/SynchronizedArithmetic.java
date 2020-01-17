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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
//import java.lang.reflect.Field;
//import sun.misc.Unsafe;

/**
 *
 * An example arithmetic that uses object intrinsic monitors to synchronize get/set/iteration on Maps.
 */
public class SynchronizedArithmetic extends JexlArithmetic {
    /**
     * Monitor/synchronized protected access to gets/set/iterator on maps.
     */
    private final Monitor monitor;

    /**
     * A base synchronized arithmetic.
     * @param monitor the synchronization monitor
     * @param strict  whether the arithmetic is strict or not
     */
    protected SynchronizedArithmetic(Monitor monitor, boolean strict) {
        super(strict);
        this.monitor = monitor;
    }


    /**
     * An indirect way to determine we are actually calling what is needed.
     * <p>
     * This class counts how many times we called enter & exit; they should be balanced
     */
    public static abstract class Monitor{
        /* Counts the number of times enter is called. */
        private final AtomicInteger enters = new AtomicInteger(0);
        /* Counts the number of times exit is called. */
        private final AtomicInteger exits = new AtomicInteger(0);

        /**
         * Enter an object monitor.
         * @param o the monitored object
         */
        protected void monitorEnter(Object o) {
            enters.incrementAndGet();
        }

        /**
         * Exits an object monitor.
         * @param o the monitored object
         */
        protected void monitorExit(Object o) {
            exits.incrementAndGet();
        }

        /**
         * Whether the number of monitor enter is equals to the number of exits.
         * @return true if balanced, false otherwise
         */
        public boolean isBalanced() {
            return enters.get() == exits.get();
        }

        /**
         * The number of enter calls.
         * @return how many enter were executed
         */
        public int getCount() {
            return enters.get();
        }

    }

    /**
     * You should know better than to use this...
     */
//    private static Unsafe UNSAFE;
//    static {
//        try {
//            Field f = Unsafe.class.getDeclaredField("theUnsafe");
//            f.setAccessible(true);
//            UNSAFE = (Unsafe) f.get(null);
//        } catch (Exception e) {
//            UNSAFE = null;
//        }
//    }
//
//    /**
//     * Using the unsafe to enter & exit object intrinsic monitors.
//     */
//    static class UnsafeMonitor extends Monitor {
//        @Override protected void monitorEnter(Object o) {
//            UNSAFE.monitorEnter(o);
//            super.monitorEnter(o);
//        }
//
//        @Override protected void monitorExit(Object o) {
//            UNSAFE.monitorExit(o);
//            super.monitorExit(o);
//        }
//    }
    
    /**
     * Crude monitor replacement...
     */
    static class SafeMonitor extends Monitor {
         private final Map<Object, Object> monitored = new IdentityHashMap<Object, Object>();

        @Override
        protected void monitorEnter(Object o) {
            Object guard;
            try {
                while (true) {
                    synchronized (monitored) {
                        guard = monitored.get(o);
                        if (guard == null) {
                            guard = new Object();
                            monitored.put(o, guard);
                            super.monitorEnter(o);
                            break;
                        }
                    }
                    synchronized (guard) {
                        guard.wait();
                    }
                }
            } catch (InterruptedException xint) {
                // oops
            }
        }

        @Override protected void monitorExit(Object o) {
            final Object guard;
            synchronized(monitored) {
                guard = monitored.remove(o);
            }
            if (guard != null) {
                synchronized(guard) {
                    guard.notifyAll();
                }
                super.monitorExit(o);
            }
        }
    }
    
    /**
     * An iterator that implements Closeable (at least implements a close method)
     * and uses monitors to protect iteration.
     */
    public class SynchronizedIterator implements /*Closeable,*/ Iterator<Object> {
        private final Object monitored;
        private Iterator<Object> iterator;

        SynchronizedIterator(Object locked, Iterator<Object> ii) {
            monitored = locked;
            monitor.monitorEnter(monitored);
            try {
                iterator = ii;
            } finally {
                if (iterator == null) {
                    monitor.monitorExit(monitored);
                }
            }
        }

        @Override
        protected void finalize() throws Throwable {
            close();
            super.finalize();
        }

        //@Override
        public void close() {
            if (iterator != null) {
                monitor.monitorExit(monitored);
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

    /**
     * Jexl pseudo-overload for array-like access get operator (map[key]) for maps.
     * <p>synchronized(map) { return map.get(key); }
     * @param map the map
     * @param key the key
     * @return the value associated to the key in the map
     */
    public Object arrayGet(Map<?, ?> map, Object key) {
        monitor.monitorEnter(map);
        try {
            return map.get(key);
        } finally {
            monitor.monitorExit(map);
        }
    }
    /**
     * Jexl pseudo-overload for array-like access set operator (map[key] = value) for maps.
     * <p>synchronized(map) { map.put(key, value); }
     * @param map the map
     * @param key the key
     * @param value the value
     */
    public void arraySet(Map<Object, Object> map, Object key, Object value) {
        monitor.monitorEnter(map);
        try {
            map.put(key, value);
        } finally {
            monitor.monitorExit(map);
        }
    }
    /**
     * Jexl pseudo-overload for property access get operator (map.key) for maps.
     * <p>synchronized(map) { return map.get(key); }
     *
     * @param map the map
     * @param key the key
     * @return the value associated to the key in the map
     */
    public Object propertyGet(Map<?, ?> map, Object key) {
        monitor.monitorEnter(map);
        try {
            return map.get(key);
        } finally {
            monitor.monitorExit(map);
        }
    }

   /**
     * Jexl pseudo-overload for array-like access set operator (map.key = value) for maps.
     * <p>synchronized(map) { map.put(key, value); }
     * @param map the map
     * @param key the key
     * @param value the value
     */
    public void propertySet(Map<Object, Object> map, Object key, Object value) {
        monitor.monitorEnter(map);
        try {
            map.put(key, value);
        } finally {
            monitor.monitorExit(map);
        }
    }

    /**
     * Creates an iterator on the map values that executes under the map monitor.
     * @param map the map
     * @return the iterator
     */
    public Iterator<Object> forEach(Map<Object, Object> map) {
        return new SynchronizedIterator(map, map.values().iterator());
    }
}
