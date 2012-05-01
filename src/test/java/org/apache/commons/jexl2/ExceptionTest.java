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
package org.apache.commons.jexl2;

/**
 * Checks various exception handling cases.
 */
public class ExceptionTest extends JexlTestCase {
    /** create a named test */
    public ExceptionTest(String name) {
        super(name);
    }

    public static class ThrowNPE {
        public String method() {
            throw new NullPointerException("ThrowNPE");
        }
    }

    public static class ThrowNPEContext extends ObjectContext<ThrowNPE> implements NamespaceResolver {
        public ThrowNPEContext(JexlEngine jexl, ThrowNPE arg) {
            super(jexl, arg);
        }

        public Object resolveNamespace(String name) {
            return name == null? object : null;
        }
    }

    public void testWrappedEx() throws Exception {
        JexlEngine jexl = new JexlEngine();
        // make unknown vars throw
        jexl.setSilent(false);
        jexl.setStrict(true);
        Expression e = jexl.createExpression("method()");
        JexlContext jc = new ThrowNPEContext(jexl, new ThrowNPE());
        try {
            e.evaluate(jc);
            fail("Should have thrown NPE");
        } catch (JexlException xany) {
            Throwable xth = xany.getCause();
            assertEquals(NullPointerException.class, xth != null? xth.getClass() : null);
        }
    }

    // Unknown vars and properties versus null operands
    public void testEx() throws Exception {
        JexlEngine jexl = createEngine(false);
        Expression e = jexl.createExpression("c.e * 6");
        JexlContext ctxt = new MapContext();
        // make unknown vars throw
        jexl.setSilent(false);
        jexl.setStrict(true);
        assertFalse(jexl.getArithmetic().isLenient());
        assertTrue(jexl.isStrict());
        // empty cotext
        try {
            /* Object o = */ e.evaluate(ctxt);
            fail("c.e not declared as variable should throw");
        } catch (JexlException.Variable xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("c.e") > 0);
        }

        // disallow null operands
        jexl.getArithmetic().setLenient(false);
        assertFalse(jexl.getArithmetic().isLenient());
        assertTrue(jexl.isStrict());
        ctxt.set("c.e", null);
        try {
            /* Object o = */ e.evaluate(ctxt);
            fail("c.e as null operand should throw");
        } catch (JexlException xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("null operand") > 0);
        }

        // allow null operands
        jexl.setStrict(true);
        jexl.getArithmetic().setLenient(true);
        assertTrue(jexl.getArithmetic().isLenient());
        assertTrue(jexl.isStrict());
        try {
            /* Object o = */ e.evaluate(ctxt);

        } catch (JexlException xjexl) {
            fail("c.e in expr should not throw");
        }

        jexl.getArithmetic().setLenient(false);
        jexl.setStrict(true);
        assertFalse(jexl.getArithmetic().isLenient());
        assertTrue(jexl.isStrict());
        // ensure c.e is not a defined property
        ctxt.set("c", "{ 'a' : 3, 'b' : 5}");
        ctxt.set("e", Integer.valueOf(2));
        try {
            /* Object o = */ e.evaluate(ctxt);
            fail("c.e not accessible as property should throw");
        } catch (JexlException.Property xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("e") > 0);
        }
    }

}
