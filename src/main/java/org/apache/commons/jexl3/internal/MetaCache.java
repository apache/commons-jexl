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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;

import org.apache.commons.jexl3.JexlCache;

/**
 * A meta-cache that tracks multiple JexlCache instances via weak references.
 * <p>
 * Each JexlCache created by this MetaCache is held via a WeakReference,
 * allowing it to be garbage collected as soon as no strong references exist.
 * </p>
 * <p>
 * This allows for collective management of multiple caches, in particular clearing all caches at once.
 * This operation is typically called when the uberspect class loader needs to change.
 * </p>
 */
final class MetaCache {
    // The factory to create new JexlCache instances
    private final IntFunction<JexlCache<?, ?>> factory;
    // The set of JexlCache references
    private final Set<Reference<JexlCache<?, ?>>> references;
    // Queue to receive references whose referent has been garbage collected
    private final ReferenceQueue<JexlCache<?, ?>> queue;

    /**
     * Constructs a MetaCache with the given cache factory.
     *
     * @param factory The factory function to create JexlCache instances given a capacity.
     */
    MetaCache(final IntFunction<JexlCache<?, ?>> factory) {
        this.factory = factory;
        this.references = new HashSet<>();
        this.queue = new ReferenceQueue<>();
    }

    @SuppressWarnings("unchecked")
    <K, V> JexlCache<K, V> createCache(final int capacity) {
        if (capacity > 0) {
            JexlCache<K, V> cache = (JexlCache<K, V>) factory.apply(capacity);
            if (cache != null) {
                synchronized (references) {
                    // The reference is created with the queue for automatic cleanup
                    references.add(new WeakReference<>(cache, queue));
                    // Always clean up the queue after modification to keep the set tidy
                    cleanUp();
                }
            }
            return cache;
        }
        return null;
    }

    /**
     * Clears all caches tracked by this MetaCache.
     */
    void clearCaches() {
        synchronized (references) {
            for (Reference<JexlCache<?, ?>> ref : references) {
                JexlCache<?, ?> cache = ref.get();
                if (cache != null) {
                    cache.clear();
                }
            }
            cleanUp();
        }
    }

    /**
     * Cleans up all references whose referent (the cache) has been garbage collected.
     * <p>This method must be invoked while holding the lock on {@code references}.</p>
     *
     * @return The remaining number of caches.
     */
    @SuppressWarnings("unchecked")
    private int cleanUp() {
        // The poll() method returns the next reference object in the queue, or null if none is available.
        Reference<JexlCache<?, ?>> reference;
        while ((reference = (Reference<JexlCache<?, ?>>) queue.poll()) != null) {
            // Remove the reference from the set
            references.remove(reference);
        }
        return references.size();
    }

    /**
     * Gets the number of live caches currently tracked by this MetaCache.
     *
     * <p>Any cache that is no longer strongly reference will get removed from the
     * tracked set.</p>
     * @return The number of caches.
     */
    int size() {
        synchronized (references) {
            return cleanUp();
        }
    }
}
