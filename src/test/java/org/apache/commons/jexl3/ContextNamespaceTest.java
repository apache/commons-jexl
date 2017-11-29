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

import org.apache.commons.jexl3.internal.Engine;
import org.junit.Assert;
import org.junit.Test;

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
        public double vat(double n) {
            TaxesContext context = (TaxesContext) JexlEngine.getThreadContext();
            return n * context.getVAT() / 100.;
        }
    }

    /**
     * A thread local context carrying a namespace and some inner constants.
     */
    public static class TaxesContext extends MapContext implements JexlContext.ThreadLocal, JexlContext.NamespaceResolver {
        private final Taxes taxes = new Taxes();
        private final double vat;

        TaxesContext(double vat) {
            this.vat = vat;
        }

        @Override
        public Object resolveNamespace(String name) {
            return "taxes".equals(name) ? taxes : null;
        }

        public double getVAT() {
            return vat;
        }
    }

    @Test
    public void testThreadedContext() throws Exception {
        JexlEngine jexl = new Engine();
        TaxesContext context = new TaxesContext(18.6);
        String strs = "taxes:vat(1000)";
        JexlScript staxes = jexl.createScript(strs);
        Object result = staxes.execute(context);
        Assert.assertEquals(186., result);
    }

    public static class Vat {
        private double vat;

        Vat(double vat) {
            this.vat = vat;
        }

        public double getVAT() {
            return vat;
        }

        public void setVAT(double vat) {
            this.vat = vat;
        }

        public double getvat() {
            throw new UnsupportedOperationException("no way");
        }

        public void setvat(double vat) {
            throw new UnsupportedOperationException("no way");
        }
    }

    @Test
    public void testObjectContext() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).silent(false).create();
        Vat vat = new Vat(18.6);
        ObjectContext<Vat> ctxt = new ObjectContext<Vat>(jexl, vat);
        Assert.assertEquals(18.6d, (Double) ctxt.get("VAT"), 0.0001d);
        ctxt.set("VAT", 20.0d);
        Assert.assertEquals(20.0d, (Double) ctxt.get("VAT"), 0.0001d);

        try {
            ctxt.get("vat");
            Assert.fail("should have failed");
        } catch(JexlException.Property xprop) {
            //
        }

        try {
            ctxt.set("vat", 33.0d);
            Assert.fail("should have failed");
        } catch(JexlException.Property xprop) {
            //
        }
    }

}
