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

import java.util.Map;

import org.apache.commons.jexl3.internal.SoftCache;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * A cache whose underlying map is a ConcurrentLinkedHashMap.
 *
 * @param <K> the cache key entry type
 * @param <V> the cache key value type
 */
public class ConcurrentCache<K, V>  extends SoftCache<K, V> {
  /**
   * Creates a new instance of a concurrent cache.
   *
   * @param theSize the cache size
   */
  public ConcurrentCache(final int theSize) {
    super(theSize);
  }

  @Override
  protected <K, V> Map<K, V> createMap(final int cacheSize) {
    return  new ConcurrentLinkedHashMap.Builder<K, V>()
        .concurrencyLevel(Runtime.getRuntime().availableProcessors())
        .maximumWeightedCapacity(cacheSize)
        .build();
  }
}

