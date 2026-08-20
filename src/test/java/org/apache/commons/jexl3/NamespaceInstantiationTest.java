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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.jexl3.annotations.NoJexl;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link JexlFeatures#namespaceInstantiation(boolean)} knob that gates the reflective
 * auto-instantiation of a namespace functor from a class or class-name binding.
 */
class NamespaceInstantiationTest extends JexlTestCase {

    public NamespaceInstantiationTest() {
        super("NamespaceInstantiationTest");
    }

    /** Counts constructor invocations to prove (non-)instantiation. */
    static final AtomicInteger CTOR = new AtomicInteger();

    /** A namespace whose only method is an instance method; usable only if auto-instantiated. */
    public static class InstanceNs {
        private final int base;
        public InstanceNs(final JexlContext ctxt) {
            CTOR.incrementAndGet();
            final Object n = ctxt.get("BASE");
            base = n instanceof Number ? ((Number) n).intValue() : 0;
        }
        public int callIt(final int n) {
            return n + base;
        }
    }

    /** A namespace with a usable static method; no instantiation required. */
    public static class StaticNs {
        private StaticNs() { }
        public static int callIt(final int n) {
            return n + 19;
        }
    }

    /** A namespace hidden from JEXL via {@code @NoJexl}; must not be reachable as a namespace. */
    @NoJexl
    public static class HiddenNs {
        public static int callIt(final int n) {
            return n + 1;
        }
    }

    @Test
    void testFeatureDefaultsOn() {
        assertTrue(new JexlFeatures().supportsNamespaceInstantiation());
        assertTrue(new JexlFeatures().namespaceInstantiation(false).namespaceInstantiation(true)
                .supportsNamespaceInstantiation());
        assertFalse(new JexlFeatures().namespaceInstantiation(false).supportsNamespaceInstantiation());
    }

    @Test
    void testInstantiationEnabled() {
        CTOR.set(0);
        final JexlContext ctxt = new MapContext();
        ctxt.set("BASE", 19);
        // class-bound namespace auto-instantiates (default feature on) and the instance method runs
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false)
                .features(new JexlFeatures().namespaceInstantiation(true))
                .namespaces(Collections.singletonMap("nsns", InstanceNs.class)).create();
        final JexlScript s = jexl.createScript("x -> { nsns:callIt(x); }");
        assertEquals(42, s.execute(ctxt, 23));
        assertEquals(1, CTOR.get());
    }

    @Test
    void testInstantiationDisabledBlocksInstanceNs() {
        CTOR.set(0);
        final JexlContext ctxt = new MapContext();
        ctxt.set("BASE", 19);
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false)
                .features(new JexlFeatures().namespaceInstantiation(false))
                .namespaces(Collections.singletonMap("nsns", InstanceNs.class)).create();
        final JexlScript s = jexl.createScript("x -> { nsns:callIt(x); }");
        // with instantiation disabled, the class is treated as a static-method namespace only;
        // InstanceNs has no static callIt(int), so resolution fails and no constructor is invoked
        assertThrows(JexlException.class, () -> s.execute(ctxt, 23));
        assertEquals(0, CTOR.get());
    }

    @Test
    void testInstantiationDisabledKeepsStaticNs() {
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false)
                .features(new JexlFeatures().namespaceInstantiation(false))
                .namespaces(Collections.singletonMap("sns", StaticNs.class)).create();
        final JexlScript s = jexl.createScript("x -> { sns:callIt(x); }");
        assertEquals(42, s.execute(new MapContext(), 23));
    }

    @Test
    void testInstantiationDisabledKeepsStaticNsByName() {
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false)
                .features(new JexlFeatures().namespaceInstantiation(false))
                .namespaces(Collections.singletonMap("sns", StaticNs.class.getName())).create();
        final JexlScript s = jexl.createScript("x -> { sns:callIt(x); }");
        assertEquals(42, s.execute(new MapContext(), 23));
    }

    @Test
    void testStringNamespaceDeniedClass() {
        // a namespace bound to a class-name that resolves to a permission-denied class must throw
        // when used for static methods, rather than silently loading the class (f047)
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false)
                .namespaces(Collections.singletonMap("h", HiddenNs.class.getName())).create();
        final JexlScript script = jexl.createScript("h:callIt(41)");
        final JexlException xany = assertThrows(JexlException.class, () -> script.execute(new MapContext()));
        assertTrue(xany.getMessage().contains("class namespace") || xany.getMessage().contains("HiddenNs"),
                xany.getMessage());
    }
}
