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
package org.apache.commons.jexl3.internal;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.jexl3.JexlCache;

/**
 * A soft referenced cache.
 * <p>
 *   The actual cache is held through a soft reference, allowing it to be GCed
 *   under memory pressure.
 * </p>
 * <p>
 *   Note that the underlying map is a synchronized LinkedHashMap.
 *   The reason is that a get() will reorder elements (the LRU queue) and thus
 *   needs synchronization to ensure thread-safety.
 * </p>
 * <p>
 *   When caching JEXL scripts or expressions, one should expect the execution cost of those
 *   to be several fold the cost of the cache handling; after some (synthetic) tests, measures indicate
 *   cache handling is a marginal latency factor.
 * </p>
 *
 * @param <K> the cache key entry type
 * @param <V> the cache key value type
 */
public class SoftCache<K, V> implements JexlCache<K, V> {
    /**
     * The default cache load factor.
     */
    protected static final float LOAD_FACTOR = 0.75f;
    /**
     * Creates a synchronized LinkedHashMap.
     * @param capacity the map capacity
     * @return the map instance
     * @param <K> key type
     * @param <V> value type
     */
    public static <K, V> Map<K, V> createSynchronizedLinkedHashMap(final int capacity) {
        return Collections.synchronizedMap(new java.util.LinkedHashMap<K, V>(capacity, LOAD_FACTOR, true) {
            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
                return super.size() > capacity;
            }
        });
    }
    /**
     * The cache capacity.
     */
    protected final int capacity;

    /**
     * The soft reference to the cache map.
     */
    protected volatile SoftReference<Map<K, V>> reference;

    /**
     * Creates a new instance of a soft cache.
     *
     * @param theSize the cache size
     */
    public SoftCache(final int theSize) {
        capacity = theSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacity() {
        return capacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        final SoftReference<Map<K, V>> ref = reference;
        if (ref != null) {
            reference = null;
            final Map<K, V> map = ref.get();
            if (map != null) {
                map.clear();
            }
        }
    }

    /**
     * Creates a cache store.
     *
     * @param <KT> the key type
     * @param <VT> the value type
     * @param cacheSize the cache size, must be &gt; 0
     * @return a Map usable as a cache bounded to the given size
     */
    protected <KT, VT> Map<KT, VT> createMap(final int cacheSize) {
        return createSynchronizedLinkedHashMap(cacheSize);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Map.Entry<K, V>> entries() {
        final SoftReference<Map<K, V>> ref = reference;
        final Map<K, V> map = ref != null ? ref.get() : null;
        return map == null? Collections.emptyList() : map.entrySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(final K key) {
        final SoftReference<Map<K, V>> ref = reference;
        final Map<K, V> map = ref != null ? ref.get() : null;
        return map != null ? map.get(key) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public V put(final K key, final V script) {
        SoftReference<Map<K, V>> ref = reference;
        Map<K, V> map = ref != null ? ref.get() : null;
        if (map == null) {
            synchronized (this) {
                ref = reference;
                map = ref != null ? ref.get() : null;
                if (map == null) {
                    map = createMap(capacity);
                    reference = new SoftReference<>(map);
                }
            }
        }
        return map.put(key, script);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        final SoftReference<Map<K, V>> ref = reference;
        final Map<K, V> map = ref != null ? ref.get() : null;
        return map != null ? map.size() : 0;
    }
}

