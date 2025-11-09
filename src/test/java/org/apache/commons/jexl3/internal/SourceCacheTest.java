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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.ConcurrentCache;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlCache;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlScript;
import org.junit.jupiter.api.Test;

public class SourceCacheTest {

  @Test
  void testSource() {
    final JexlFeatures features = JexlFeatures.createDefault();
    final Source src0 = new Source(features, null,"x -> -x");
    final Source src0b = new Source(features, null,"x -> -x");
    final Source src1 = new Source(features, null,"x -> +x");
    assertEquals(7, src0.length());
    assertEquals(src0, src0b);
    assertNotEquals(src0, src1);
    assertEquals(src0.hashCode(), src0b.hashCode());
    assertNotEquals(src0.hashCode(), src1.hashCode());
    assertEquals(0, src0.compareTo(src0b));
    assertTrue(src0.compareTo(src1) > 0);
    assertTrue(src1.compareTo(src0) < 0);
  }

  @Test
  public void testSourceCache() {
    final JexlFeatures features = JexlFeatures.createDefault();
    // source objects differ when symbol maps differ
    Map<String, Integer> symbols0 = new HashMap<>();
    symbols0.put("x", 0);
    symbols0.put("y", 1);
    Source src0 = new Source(features, symbols0,"x + y");
    assertFalse(src0.equals("x + y"));
    Map<String, Integer> symbols1 = new HashMap<>();
    symbols0.put("x", 0);
    symbols0.put("y", 2);
    Source src1 = new Source(features, symbols1,"x + y");
    assertNotEquals(src0, src1);
    assertNotEquals(0, src0.compareTo(src1));
    Source src2 = new Source(features, null,"x + y");
    assertNotEquals(src0, src2);
    assertNotEquals(0, src0.compareTo(src2));
    Source src3 = new Source(JexlFeatures.createNone(), symbols1,"x + y");
    assertNotEquals(src0, src3);
    assertNotEquals(0, src0.compareTo(src3));

    JexlEngine jexl = new JexlBuilder().cache(4).create();
    JexlCache<Source, Object> cache = ((Engine) jexl).getCache();
    // order of declaration of variables matters
    JexlScript script0 = jexl.createScript("x + y", "x", "y");
    JexlScript script1 = jexl.createScript("x + y", "y", "x");
    assertEquals(2, cache.size());
  }

  @Test
  void testMetaCache() {
    final MetaCache mc = new MetaCache(ConcurrentCache::new);
    JexlCache<Integer, String> cache1 = mc.createCache(3);
    cache1.put(1, "one");
    cache1.put(2, "two");
    cache1.put(3, "three");
    assertEquals(3, cache1.size());
    assertEquals("one", cache1.get(1));
    assertEquals("two", cache1.get(2));
    assertEquals("three", cache1.get(3));
    cache1.put(4, "four");
    assertEquals(3, cache1.size());
    assertNull(cache1.get(1)); // evicted
    assertEquals("two", cache1.get(2));
    assertEquals("three", cache1.get(3));
    assertEquals("four", cache1.get(4));

    JexlCache<String, String> cache2 = mc.createCache(2);
    cache2.put("a", "A");
    cache2.put("b", "B");
    assertEquals(2, cache2.size());
    assertEquals("A", cache2.get("a"));
    assertEquals("B", cache2.get("b"));
    cache2.put("c", "C");
    assertEquals(2, cache2.size());
    assertNull(cache2.get("a")); // evicted
    assertEquals("B", cache2.get("b"));
    assertEquals("C", cache2.get("c"));

    // metacache weak references test
    assertEquals(2, mc.size());
    // drop the strong references to the caches
    cache1 = null;
    assertNull(cache1);
    cache2 = null;
    assertNull(cache2);
    // trigger garbage collection
    System.gc();
    // wait for the garbage collector to do its work
    for(int i = 0; i < 5 && mc.size() != 0; ++i) {
      try {
        Thread.sleep(100);
      } catch(final InterruptedException xint) {
        // ignore
      }
    }
    // the caches should have been removed from the metacache
    assertEquals(0, mc.size(), "metacache should have no more cache references");
  }
}
