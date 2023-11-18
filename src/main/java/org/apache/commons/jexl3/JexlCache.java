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

import org.apache.commons.jexl3.internal.ConcurrentCache;
import org.apache.commons.jexl3.internal.SoftCache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Caching scripts or templates interface.
 * @param <K> source
 * @param <V> script or template
 */
public interface JexlCache<K, V> {
  /**
 * Returns the cache size.
 *
 * @return the cache size
 */
  int size();

  /**
   * Clears the cache.
   */
  void clear();

  /**
   * Gets a value from cache.
   *
   * @param key the cache entry key
   * @return the cache entry value
   */
  V get(K key);

  /**
   * Puts a value in cache.
   *
   * @param key    the cache entry key
   * @param script the cache entry value
   */
  V put(K key, V script);

  /**
   * Produces the cache entry set.
   * <p>
   * For implementations testing only
   * </p>
   * @return the cache entry list
   */
  default Collection<Map.Entry<K, V>> entries() {
    return Collections.emptyList();
  }

  /**
   * @return a synchronized cache factory amenable to low concurrency usage
   */
  static IntFunction<JexlCache<?,?>> createSynchronized() {
    return SoftCache::new;
  }

  /**
   * @return a concurrent cache factory amenable to high concurrency usage
   */
  static IntFunction<JexlCache<?,?>> createConcurrent() {
    return ConcurrentCache::new;
  }
}
