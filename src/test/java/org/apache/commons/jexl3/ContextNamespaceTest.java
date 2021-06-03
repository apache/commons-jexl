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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tests JexlContext (advanced) features.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ContextNamespaceTest extends JexlTestCase {

    public ContextNamespaceTest() {
        super("ContextNamespaceTest");
    }

    /*
     * Accesses the thread context and cast it.
     */
    public static class Taxes {
        private final double vat;

        public Taxes(final TaxesContext ctxt) {
            vat = ctxt.getVAT();
        }

        public Taxes(final double d) {
            vat = d;
        }

        public double vat(final double n) {
            return (n * vat) / 100.;
        }
    }

    /**
     * A thread local context carrying a namespace and some inner constants.
     */
    public static class TaxesContext extends MapContext implements JexlContext.ThreadLocal, JexlContext.NamespaceResolver {
        private Taxes taxes = null;
        private final double vat;

        TaxesContext(final double vat) {
            this.vat = vat;
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

        public double getVAT() {
            return vat;
        }
    }

    @Test
    public void testThreadedContext() throws Exception {
        final JexlEngine jexl = new JexlBuilder().create();
        final TaxesContext context = new TaxesContext(18.6);
        final String strs = "taxes:vat(1000)";
        final JexlScript staxes = jexl.createScript(strs);
        final Object result = staxes.execute(context);
        Assert.assertEquals(186., result);
    }

    @Test
    public void testNamespacePragma() throws Exception {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext context = new TaxesContext(18.6);
        // local namespace tax declared
        final String strs =
                  "#pragma jexl.namespace.tax org.apache.commons.jexl3.ContextNamespaceTest$Taxes\n"
                + "tax:vat(2000)";
        final JexlScript staxes = jexl.createScript(strs);
        final Object result = staxes.execute(context);
        Assert.assertEquals(372., result);
    }

    public static class Context346 extends MapContext {
        public int func(int y) { return 42 * y;}
    }

    @Test
    public void testNamespace346a() throws Exception {
        JexlContext ctxt = new Context346();
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        String src = "x != null ? x : func(y)";
        final JexlScript script = jexl.createScript(src,"x","y");
        Object result = script.execute(ctxt, null, 1);
        Assert.assertEquals(42, result);
        result = script.execute(ctxt, 169, -169);
        Assert.assertEquals(169, result);
    }

    @Test
    public void testNamespace346b() throws Exception {
        JexlContext ctxt = new MapContext();
        Map<String, Object> ns = new HashMap<String, Object>();
        ns.put("x", Math.class);
        ns.put(null, Math.class);
        final JexlEngine jexl = new JexlBuilder().safe(false).namespaces(ns).create();
        String src = "x != null ? x : abs(y)";
        final JexlScript script = jexl.createScript(src,"x","y");
        Object result = script.execute(ctxt, null, 42);
        Assert.assertEquals(42, result);
        result = script.execute(ctxt, 169, -169);
        Assert.assertEquals(169, result);
    }

    public static class Ns348 {
        public static int func(int y) { return 42 * y;}
    }

    public static class ContextNs348 extends MapContext implements JexlContext.NamespaceResolver {
        ContextNs348() { super(); }

        @Override
        public Object resolveNamespace(String name) {
            return "ns".equals(name)? new Ns348() : null;
        }
    }

    @Test
    public void testNamespace348a() throws Exception {
        JexlContext ctxt = new MapContext();
        Map<String, Object> ns = new HashMap<String, Object>();
        ns.put("ns", Ns348.class);
        final JexlEngine jexl = new JexlBuilder().safe(false).namespaces(ns).create();
        run348a(jexl, ctxt);
        run348b(jexl, ctxt);
        run348c(jexl, ctxt);
        run348d(jexl, ctxt);
    }

    @Test
    public void testNamespace348b() throws Exception {
        JexlContext ctxt = new ContextNs348();
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        // no space for ns name as syntactic hint
        run348a(jexl, ctxt, "ns:");
        run348b(jexl, ctxt, "ns:");
        run348c(jexl, ctxt, "ns:");
        run348d(jexl, ctxt, "ns:");
    }

    @Test
    public void testNamespace348c() throws Exception {
        JexlContext ctxt = new ContextNs348();
        Map<String, Object> ns = new HashMap<String, Object>();
        ns.put("ns", Ns348.class);
        JexlFeatures f = new JexlFeatures();
        f.namespaceTest((n)->true);
        final JexlEngine jexl = new JexlBuilder().namespaces(ns).features(f).safe(false).create();
        run348a(jexl, ctxt);
        run348b(jexl, ctxt);
        run348c(jexl, ctxt);
        run348d(jexl, ctxt);
    }

    @Test
    public void testNamespace348d() throws Exception {
        JexlContext ctxt = new ContextNs348();
        JexlFeatures f = new JexlFeatures();
        f.namespaceTest((n)->true);
        final JexlEngine jexl = new JexlBuilder().features(f).safe(false).create();
        run348a(jexl, ctxt);
        run348b(jexl, ctxt);
        run348c(jexl, ctxt);
        run348d(jexl, ctxt);
    }

    private void run348a(JexlEngine jexl, JexlContext ctxt) {
        run348a(jexl, ctxt, "ns : ");
    }
    private void run348a(JexlEngine jexl, JexlContext ctxt, String ns) {
        String src = "empty(x) ? "+ns+"func(y) : z";
        // local vars
        JexlScript script = jexl.createScript(src, "x", "y", "z");
        Object result = script.execute(ctxt, null, 1, 169);
        Assert.assertEquals(42, result);
        result = script.execute(ctxt, "42", 1, 169);
        Assert.assertEquals(169, result);
    }

    private void run348b(JexlEngine jexl, JexlContext ctxt) {
        run348b(jexl, ctxt, "ns : ");
    }
    private void run348b(JexlEngine jexl, JexlContext ctxt, String ns) {
        String src = "empty(x) ? "+ns+"func(y) : z";
        // global vars
        JexlScript script = jexl.createScript(src);
        ctxt.set("x", null);
        ctxt.set("y", 1);
        ctxt.set("z", 169);
        Object result = script.execute(ctxt);
        Assert.assertEquals(42, result);
        ctxt.set("x", "42");
        result = script.execute(ctxt);
        Assert.assertEquals(169, result);
    }

    private void run348c(JexlEngine jexl, JexlContext ctxt) {
        run348c(jexl, ctxt, "ns : ");
    }
    private void run348c(JexlEngine jexl, JexlContext ctxt, String ns) {
        String src = "empty(x) ? z : "+ns+"func(y)";
        // local vars
        JexlScript script = jexl.createScript(src, "x", "z", "y");
        Object result = script.execute(ctxt, null, 169, 1);
        Assert.assertEquals(169, result);
        result = script.execute(ctxt, "42", 169, 1);
        Assert.assertEquals(42, result);
    }

    private void run348d(JexlEngine jexl, JexlContext ctxt) {
        run348d(jexl, ctxt, "ns : ");
    }
    private void run348d(JexlEngine jexl, JexlContext ctxt, String ns) {
        String src = "empty(x) ? z : "+ns+"func(y)";
        // global vars
        JexlScript script = jexl.createScript(src);
        ctxt.set("x", null);
        ctxt.set("z", 169);
        ctxt.set("y", 1);
        Object result = script.execute(ctxt);
        Assert.assertEquals(169, result);
        ctxt.set("x", "42");
        result = script.execute(ctxt);
        Assert.assertEquals(42, result);
    }

    @Test
    public void testNamespacePragmaString() throws Exception {
        final JexlEngine jexl = new JexlBuilder().create();
        final JexlContext context = new MapContext();
        // local namespace str declared
        final String strs =
                  "#pragma jexl.namespace.str java.lang.String\n"
                + "str:format('%04d', 42)";
        final JexlScript staxes = jexl.createScript(strs);
        final Object result = staxes.execute(context);
        Assert.assertEquals("0042", result);
    }

    public static class Vat {
        private double vat;

        Vat(final double vat) {
            this.vat = vat;
        }

        public double getVAT() {
            return vat;
        }

        public void setVAT(final double vat) {
            this.vat = vat;
        }

        public double getvat() {
            throw new UnsupportedOperationException("no way");
        }

        public void setvat(final double vat) {
            throw new UnsupportedOperationException("no way");
        }
    }

    @Test
    public void testObjectContext() throws Exception {
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false).create();
        final Vat vat = new Vat(18.6);
        final ObjectContext<Vat> ctxt = new ObjectContext<Vat>(jexl, vat);
        Assert.assertEquals(18.6d, (Double) ctxt.get("VAT"), 0.0001d);
        ctxt.set("VAT", 20.0d);
        Assert.assertEquals(20.0d, (Double) ctxt.get("VAT"), 0.0001d);

        try {
            ctxt.get("vat");
            Assert.fail("should have failed");
        } catch(final JexlException.Property xprop) {
            //
        }

        try {
            ctxt.set("vat", 33.0d);
            Assert.fail("should have failed");
        } catch(final JexlException.Property xprop) {
            //
        }
    }

}
