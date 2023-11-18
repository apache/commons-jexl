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
package org.apache.commons.jexl3.internal;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jexl3.JexlCache;

/**
 * A soft referenced cache.
 * <p>
 *   The actual cache is held through a soft reference, allowing it to be GCed
 *   under memory pressure.
 * </p>
 * <p>
 *   Note that the underlying map is a synchronized LinkedHashMap.
 *   The reason is that a get() will  reorder elements (the LRU queue) and thus
 *   needs to be guarded to be thread-safe.
 * </p>
 *
 * @param <K> the cache key entry type
 * @param <V> the cache key value type
 */
public class SoftCache<K, V> implements JexlCache<K, V> {
    /**
     * The default cache load factor.
     */
    private static final float LOAD_FACTOR = 0.75f;
    /**
     * The cache size.
     */
    protected final int size;
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
        size = theSize;
    }

    /**
     * Returns the cache size.
     *
     * @return the cache size
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Clears the cache.
     */
    @Override
    public void clear() {
        final SoftReference<Map<K, V>> ref = reference;
        if (ref != null ) {
            reference = null;
            final Map<K, V> map = ref.get();
            if (map != null) {
                map.clear();
            }
        }
    }

    /**
     * Gets a value from cache.
     *
     * @param key the cache entry key
     * @return the cache entry value
     */
    @Override
    public V get(final K key) {
        final SoftReference<Map<K, V>> ref = reference;
        final Map<K, V> map = ref != null ? ref.get() : null;
        return map != null ? map.get(key) : null;
    }

    /**
     * Puts a value in cache.
     *
     * @param key the cache entry key
     * @param script the cache entry value
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
                    map = createMap(size);
                    reference = new SoftReference<>(map);
                }
            }
        }
        return map.put(key, script);
    }

    /**
     * Produces the cache entry set.
     * <p>
     * For testing only, perform deep copy of cache entries
     *
     * @return the cache entry list
     */
    @Override
    public Collection<Map.Entry<K, V>> entries() {
        final SoftReference<Map<K, V>> ref = reference;
        final Map<K, V> map = ref != null ? ref.get() : null;
        if (map == null) {
            return Collections.emptyList();
        }
        synchronized(map) {
            final Set<Map.Entry<K, V>> set = map.entrySet();
            final List<Map.Entry<K, V>> entries = new ArrayList<>(set.size());
            for (final Map.Entry<K, V> e : set) {
                entries.add(new SoftCacheEntry<>(e));
            }
            return entries;
        }
    }

    /**
     * Creates the cache store.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cacheSize the cache size, must be &gt; 0
     * @return a Map usable as a cache bounded to the given size
     */
    public <K, V> Map<K, V> createMap(final int cacheSize) {
        return Collections.synchronizedMap(
            new java.util.LinkedHashMap<K, V>(cacheSize, LOAD_FACTOR, true) {
                /**
                 * Serial version UID.
                 */
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
                    return super.size() > cacheSize;
                }
            }
        );
    }
}

/**
 * A soft cache entry.
 *
 * @param <K> key type
 * @param <V> value type
 */
final class SoftCacheEntry<K, V> implements Map.Entry<K, V> {
    /**
     * The entry key.
     */
    private final K key;
    /**
     * The entry value.
     */
    private final V value;

    /**
     * Creates an entry clone.
     *
     * @param e the entry to clone
     */
    SoftCacheEntry(final Map.Entry<K, V> e) {
        key = e.getKey();
        value = e.getValue();
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(final V v) {
        throw new UnsupportedOperationException("Not supported.");
    }
}


