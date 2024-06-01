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

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jexl3.internal.SoftCache;

/**
 * Creates a cache using an array of synchronized LinkedHashMap as backing store to spread contention.
 * <p>Just meant as a contention reducing mechanism for cache tests.</p>
 *
 * @param <K> the cached element&quote;s key
 * @param <V> the cached element&quote;s value
 * @return the cache instance
 */
public class SpreadCache<K, V> extends SoftCache<K, V> {

  /**
   * Creates a new instance of a Spread cache.
   *
   * @param theSize the cache size
   */
  public SpreadCache(final int theSize) {
    super(theSize);
  }

  @Override
  public <K, V> Map<K, V> createMap(final int cacheSize) {
    return new SpreadMap<>(cacheSize);
  }
}

/**
 * A map backed by an array of LinkedHashMap.
 * <p>This implementation is really tailored to serve the cache methods and no-other. It foregoes being efficient
 * for methods that do not serve this purpose.</p>
 * <p>We spread the map capacity over a number of synchronized sub-maps, the number being the number of
 * available processors. This is meant to spread the contention at the cost of a relaxed LRU.</p>
 *
 * @param <K>
 * @param <V>
 */
class SpreadMap<K, V> extends AbstractMap<K, V> {
  /**
   * Returns a power of two for the given target capacity.
   *
   * @param cap capacity
   * @return the smallest power of 2 greater or equal to argument
   */
  private static int closestPowerOf2(final int cap) {
    return cap > 1 ? Integer.highestOneBit(cap - 1) << 1 : 1;
  }

  /**
   * The sub-maps array.
   */
  private final Map<K, V>[] maps;

  /**
   * The unique simple constructor.
   *
   * @param capacity the overall map capacity
   */
  SpreadMap(final int capacity) {
    final int spread = closestPowerOf2(Runtime.getRuntime().availableProcessors());
    maps = new Map[spread];
    final int mapCapacity = (capacity + spread + 1) / spread;
    for (int m = 0; m < spread; ++m) {
      maps[m] = SoftCache.createSynchronizedLinkedHashMap(mapCapacity);
    }
  }

  @Override
  public void clear() {
    for (int m = 0; m < maps.length; ++m) {
      maps[m].clear();
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    final Set<Map.Entry<K, V>> entries = new LinkedHashSet<>(size());
    for (final Map<K, V> map : maps) {
      synchronized (map) {
        entries.addAll(map.entrySet());
      }
    }
    return entries;
  }

  @Override
  public V get(final Object key) {
    return getMap(key).get(key);
  }

  /**
   * Gets the map storing a given key.
   *
   * @param key the key
   * @return the map
   */
  private final Map<K, V> getMap(final Object key) {
    int h = key.hashCode();
    h ^= h >>> 16;
    // length is a power of 2, length - 1 is the mask of its modulo:
    // length = 4, length - 1 = 3 = 11b : x % 4 <=> x & 3
    return maps[h & maps.length - 1];
  }

  @Override
  public V put(final K key, final V value) {
    return getMap(key).put(key, value);
  }

  @Override
  public V remove(final Object key) {
    return getMap(key).remove(key);
  }

  @Override
  public int size() {
    int size = 0;
    for (int m = 0; m < maps.length; ++m) {
      size += maps[m].size();
    }
    return size;
  }
}