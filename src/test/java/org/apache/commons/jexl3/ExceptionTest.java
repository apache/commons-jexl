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

    public void testWrappedEx() throws Exception {
        JexlEngine jexl = new Engine();
        JexlExpression e = jexl.createExpression("method()");
        JexlContext jc = new ObjectContext<ThrowNPE>(jexl, new ThrowNPE());
        try {
            e.evaluate(jc);
            fail("Should have thrown NPE");
        } catch (JexlException xany) {
            Throwable xth = xany.getCause();
            assertEquals(NullPointerException.class, xth.getClass());
        }
    }

    // Unknown vars and properties versus null operands
    public void testEx() throws Exception {
        JexlEngine jexl = createEngine(false);
        JexlExpression e = jexl.createExpression("c.e * 6");
        JexlEvalContext ctxt = new JexlEvalContext();
        // ensure errors will throw
        ctxt.setSilent(false);
        // make unknown vars throw
        ctxt.setStrict(true);
        // empty cotext
        try {
            /* Object o = */ e.evaluate(ctxt);
            fail("c.e not declared as variable should throw");
        } catch (JexlException.Variable xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("c.e") > 0);
        }

        // disallow null operands
        ctxt.setStrictArithmetic(true);
        ctxt.set("c.e", null);
        try {
            /* Object o = */ e.evaluate(ctxt);
            fail("c.e as null operand should throw");
        } catch (JexlException xjexl) {
            String msg = xjexl.getMessage();
            assertTrue(msg.indexOf("null operand") > 0);
        }

        // allow null operands
        ctxt.setStrictArithmetic(false);
        try {
            /* Object o = */ e.evaluate(ctxt);

        } catch (JexlException xjexl) {
            fail("c.e in expr should not throw");
        }

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
