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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Tests JexlContext (advanced) features.
 */
@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
public class ContextNamespaceTest extends JexlTestCase {

    public static class Context346 extends MapContext {
        public int func(final int y) { return 42 * y;}
    }

    public static class ContextNs348 extends MapContext implements JexlContext.NamespaceResolver {
        ContextNs348() { }

        @Override
        public Object resolveNamespace(final String name) {
            return "ns".equals(name)? new Ns348() : null;
        }
    }

    public static class Ns348 {
        public static int func(final int y) { return 42 * y;}
    }

    public static class NsNs {
        private final int constVar;
        public NsNs(final JexlContext ctxt) {
            nsnsCtor.incrementAndGet();
            final Object n = ctxt.get("NUMBER");
            constVar = n instanceof Number ? ((Number) n).intValue() : -1;
        }

        public int callIt(final int n) {
            return n + constVar;
        }
    }

    public static class StaticNs {
        public static int callIt(final int n) {
            return n + 19;
        }
        private StaticNs() { }
    }

    /*
     * Accesses the thread context and cast it.
     */
    public static class Taxes {
        private final double vat;

        public Taxes(final double d) {
            vat = d;
        }

        public Taxes(final TaxesContext ctxt) {
            vat = ctxt.getVAT();
        }

        public double vat(final double n) {
            return n * vat / 100.;
        }
    }

    /**
     * A thread local context carrying a namespace and some inner constants.
     */
    public static class TaxesContext extends MapContext implements JexlContext.ThreadLocal, JexlContext.NamespaceResolver {
        private Taxes taxes;
        private final double vat;

        TaxesContext(final double vat) {
            this.vat = vat;
        }

        public double getVAT() {
            return vat;
        }

        @Override
        public Object resolveNamespace(final String name) {
            if ("taxes".equals(name)) {
                if (taxes == null) {
                    taxes = new Taxes(vat);
                }
                return taxes;
            }
            return null;
        }
    }

    public static class Vat {
        private double vat;

        Vat(final double vat) {
            this.vat = vat;
        }

        public double getvat() {
            throw new UnsupportedOperationException("no way");
        }

        public double getVAT() {
            return vat;
        }

        public void setvat(final double vat) {
            throw new UnsupportedOperationException("no way");
        }

        public void setVAT(final double vat) {
            this.vat = vat;
        }
    }

    static AtomicInteger nsnsCtor = new AtomicInteger();

    public ContextNamespaceTest() {
        super("ContextNamespaceTest");
    }

    private void run348a(final JexlEngine jexl, final JexlContext ctxt) {
        run348a(jexl, ctxt, "ns : ");
    }

    private void run348a(final JexlEngine jexl, final JexlContext ctxt, final String ns) {
        final String src = "empty(x) ? "+ns+"func(y) : z";
        // local vars
        final JexlScript script = jexl.createScript(src, "x", "y", "z");
        Object result = script.execute(ctxt, null, 1, 169);
        assertEquals(42, result);
        result = script.execute(ctxt, "42", 1, 169);
        assertEquals(169, result);
    }

    private void run348b(final JexlEngine jexl, final JexlContext ctxt) {
        run348b(jexl, ctxt, "ns : ");
    }

    private void run348b(final JexlEngine jexl, final JexlContext ctxt, final String ns) {
        final String src = "empty(x) ? "+ns+"func(y) : z";
        // global vars
        final JexlScript script = jexl.createScript(src);
        ctxt.set("x", null);
        ctxt.set("y", 1);
        ctxt.set("z", 169);
        Object result = script.execute(ctxt);
        assertEquals(42, result);
        ctxt.set("x", "42");
        result = script.execute(ctxt);
        assertEquals(169, result);
        //ctxt.set("x", "42");
        result = script.execute(ctxt);
        assertEquals(169, result);
    }

    private void run348c(final JexlEngine jexl, final JexlContext ctxt) {
        run348c(jexl, ctxt, "ns : ");
    }
    private void run348c(final JexlEngine jexl, final JexlContext ctxt, final String ns) {
        final String src = "empty(x) ? z : "+ns+"func(y)";
        // local vars
        final JexlScript script = jexl.createScript(src, "x", "z", "y");
        Object result = script.execute(ctxt, null, 169, 1);
        assertEquals(169, result, src);
        result = script.execute(ctxt, "42", 169, 1);
        assertEquals(42, result, src);
    }

    private void run348d(final JexlEngine jexl, final JexlContext ctxt) {
        run348d(jexl, ctxt, "ns : ");
    }
    private void run348d(final JexlEngine jexl, final JexlContext ctxt, final String ns) {
        final String src = "empty(x) ? z : "+ns+"func(y)";
        // global vars
        final JexlScript script = jexl.createScript(src);

        ctxt.set("x", null);
        ctxt.set("z", 169);
        ctxt.set("y", 1);
        Object result = script.execute(ctxt);
        assertEquals(169, result, src);
        ctxt.set("x", "42");
        result = script.execute(ctxt);
        assertEquals(42, result, src);
    }

    private void runNsNsContext(final Map<String,Object> nsMap) {
        final JexlContext ctxt = new MapContext();
        ctxt.set("NUMBER", 19);
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false).cache(32)
                .namespaces(nsMap).create();
        final JexlScript script = jexl.createScript("x ->{ nsns:callIt(x); nsns:callIt(x); }");
        Number result = (Number) script.execute(ctxt, 23);
        assertEquals(42, result);
        assertEquals(1, nsnsCtor.get());
        result = (Number) script.execute(ctxt, 623);
        assertEquals(642, result);
        assertEquals(2, nsnsCtor.get());
    }
    private void runStaticNsContext(final Map<String,Object> nsMap) {
        final JexlContext ctxt = new MapContext();
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false).cache(32)
                .namespaces(nsMap).create();
        final JexlScript script = jexl.createScript("x ->{ sns:callIt(x); sns:callIt(x); }");
        Number result = (Number) script.execute(ctxt, 23);
        assertEquals(42, result);
        result = (Number) script.execute(ctxt, 623);
        assertEquals(642, result);
    }

    @Test
    void testNamespace346a() {
        final JexlContext ctxt = new Context346();
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        final String src = "x != null ? x : func(y)";
        final JexlScript script = jexl.createScript(src,"x","y");
        Object result = script.execute(ctxt, null, 1);
        assertEquals(42, result);
        result = script.execute(ctxt, 169, -169);
        assertEquals(169, result);
    }
    @Test
    void testNamespace346b() {
        final JexlContext ctxt = new MapContext();
        final Map<String, Object> ns = new HashMap<>();
        ns.put("x", Math.class);
        ns.put(null, Math.class);
        final JexlEngine jexl = new JexlBuilder().safe(false).namespaces(ns).create();
        final String src = "x != null ? x : abs(y)";
        final JexlScript script = jexl.createScript(src,"x","y");
        Object result = script.execute(ctxt, null, 42);
        assertEquals(42, result);
        result = script.execute(ctxt, 169, -169);
        assertEquals(169, result);
    }

    @Test
    void testNamespace348a() {
        final JexlContext ctxt = new MapContext();
        final Map<String, Object> ns = new HashMap<>();
        ns.put("ns", Ns348.class);
        final JexlEngine jexl = new JexlBuilder().safe(false).namespaces(ns).create();
        run348a(jexl, ctxt);
        run348b(jexl, ctxt);
        run348c(jexl, ctxt);
        run348d(jexl, ctxt);
    }

    @Test
    void testNamespace348b() {
        final JexlContext ctxt = new ContextNs348();
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        // no space for ns name as syntactic hint
        run348a(jexl, ctxt, "ns:");
        run348b(jexl, ctxt, "ns:");
        run348c(jexl, ctxt, "ns:");
        run348d(jexl, ctxt, "ns:");
    }

    @Test
    void testNamespace348c() {
        final JexlContext ctxt = new ContextNs348();
        final Map<String, Object> ns = new HashMap<>();
        ns.put("ns", Ns348.class);
        final JexlFeatures f = new JexlFeatures();
        f.namespaceTest(n -> true);
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).features(f).safe(false).create();
        run348a(jexl, ctxt);
        run348b(jexl, ctxt);
        run348c(jexl, ctxt);
        run348d(jexl, ctxt);
    }

    @Test
    void testNamespace348d() {
        final JexlContext ctxt = new ContextNs348();
        final JexlFeatures f = new JexlFeatures();
        f.namespaceTest(n -> true);
        final JexlEngine jexl = new JexlBuilder().features(f).safe(false).create();
        run348a(jexl, ctxt);
        run348b(jexl, ctxt);
        run348c(jexl, ctxt);
        run348d(jexl, ctxt);
    }

    @Test
    void testNamespacePragma() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext context = new TaxesContext(18.6);
        // local namespace tax declared
        final String strs =
                  "#pragma jexl.namespace.tax org.apache.commons.jexl3.ContextNamespaceTest$Taxes\n"
                + "tax:vat(2000)";
        final JexlScript staxes = jexl.createScript(strs);
        final Object result = staxes.execute(context);
        assertEquals(372., result);
    }

    @Test
    void testNamespacePragmaString() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext context = new MapContext();
        // local namespace str declared
        final String strs =
                  "#pragma jexl.namespace.str java.lang.String\n"
                + "str:format('%04d', 42)";
        final JexlScript staxes = jexl.createScript(strs);
        final Object result = staxes.execute(context);
        assertEquals("0042", result);
    }

    @Test
    void testNsNsContext0() {
        nsnsCtor.set(0);
        final String clsName = NsNs.class.getName();
        runNsNsContext(Collections.singletonMap("nsns", clsName));
    }

    @Test
    void testNsNsContext1() {
        nsnsCtor.set(0);
        runNsNsContext(Collections.singletonMap("nsns", NsNs.class));
    }

    @Test
    void testObjectContext() {
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false).create();
        final Vat vat = new Vat(18.6);
        final ObjectContext<Vat> ctxt = new ObjectContext<>(jexl, vat);
        assertEquals(18.6d, (Double) ctxt.get("VAT"), 0.0001d);
        ctxt.set("VAT", 20.0d);
        assertEquals(20.0d, (Double) ctxt.get("VAT"), 0.0001d);
        assertThrows(JexlException.Property.class, () -> ctxt.get("vat"));
        assertThrows(JexlException.Property.class, () -> ctxt.set("vat", 33.0d));
    }

    @Test
    void testStaticNs0() {
        runStaticNsContext(Collections.singletonMap("sns", StaticNs.class));
    }

    @Test
    void testStaticNs1() {
        runStaticNsContext(Collections.singletonMap("sns", StaticNs.class.getName()));
    }

    @Test
    void testThreadedContext() {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext context = new TaxesContext(18.6);
        final String strs = "taxes:vat(1000)";
        final JexlScript staxes = jexl.createScript(strs);
        final Object result = staxes.execute(context);
        assertEquals(186., result);
    }
}
