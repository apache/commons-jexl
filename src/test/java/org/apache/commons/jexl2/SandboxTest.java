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

import java.util.logging.Logger;
import org.apache.commons.jexl2.introspection.Sandbox;
import org.apache.commons.jexl2.introspection.SandboxUberspectImpl;
import org.apache.commons.jexl2.introspection.Uberspect;

/**
 * Tests sandbox features.
 */
public class SandboxTest extends JexlTestCase {
    static final Logger LOGGER = Logger.getLogger(VarTest.class.getName());

    public static class Foo {
        String name;
        public String alias;

        public Foo(String name) {
            this.name = name;
            this.alias = name + "-alias";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String Quux() {
            return name + "-quux";
        }
    }

    public void testCtorBlack() throws Exception {
        String expr = "new('" + Foo.class.getName() + "', '42')";
        Script script = JEXL.createScript(expr);
        Object result;
        result = script.execute(null);
        assertEquals("42", ((Foo) result).getName());

        Sandbox sandbox = new Sandbox();
        sandbox.black(Foo.class.getName()).execute("");
        Uberspect uber = new SandboxUberspectImpl(null, sandbox);
        JexlEngine sjexl = new JexlEngine(uber, null, null, null);
        sjexl.setStrict(true);

        script = sjexl.createScript(expr);
        try {
            result = script.execute(null);
            fail("ctor should not be accessible");
        } catch (JexlException.Method xmethod) {
            // ok, ctor should not have been accessible
            LOGGER.info(xmethod.toString());
        }
    }

    public void testMethodBlack() throws Exception {
        String expr = "foo.Quux()";
        Script script = JEXL.createScript(expr, "foo");
        Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo);
        assertEquals(foo.Quux(), result);

        Sandbox sandbox = new Sandbox();
        sandbox.black(Foo.class.getName()).execute("Quux");
        Uberspect uber = new SandboxUberspectImpl(null, sandbox);
        JexlEngine sjexl = new JexlEngine(uber, null, null, null);
        sjexl.setStrict(true);

        script = sjexl.createScript(expr, "foo");
        try {
            result = script.execute(null, foo);
            fail("Quux should not be accessible");
        } catch (JexlException.Method xmethod) {
            // ok, Quux should not have been accessible
            LOGGER.info(xmethod.toString());
        }
    }

    public void testGetBlack() throws Exception {
        String expr = "foo.alias";
        Script script = JEXL.createScript(expr, "foo");
        Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo);
        assertEquals(foo.alias, result);

        Sandbox sandbox = new Sandbox();
        sandbox.black(Foo.class.getName()).read("alias");
        Uberspect uber = new SandboxUberspectImpl(null, sandbox);
        JexlEngine sjexl = new JexlEngine(uber, null, null, null);
        sjexl.setStrict(true);

        script = sjexl.createScript(expr, "foo");
        try {
            result = script.execute(null, foo);
            fail("alias should not be accessible");
        } catch (JexlException.Variable xvar) {
            // ok, alias should not have been accessible
            LOGGER.info(xvar.toString());
        }
    }

    public void testSetBlack() throws Exception {
        String expr = "foo.alias = $0";
        Script script = JEXL.createScript(expr, "foo", "$0");
        Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo, "43");
        assertEquals("43", result);

        Sandbox sandbox = new Sandbox();
        sandbox.black(Foo.class.getName()).write("alias");
        Uberspect uber = new SandboxUberspectImpl(null, sandbox);
        JexlEngine sjexl = new JexlEngine(uber, null, null, null);
        sjexl.setStrict(true);

        script = sjexl.createScript(expr, "foo", "$0");
        try {
            result = script.execute(null, foo, "43");
            fail("alias should not be accessible");
        } catch (JexlException.Variable xvar) {
            // ok, alias should not have been accessible
            LOGGER.info(xvar.toString());
        }
    }

    public void testCtorWhite() throws Exception {
        String expr = "new('" + Foo.class.getName() + "', '42')";
        Script script;
        Object result;

        Sandbox sandbox = new Sandbox();
        sandbox.white(Foo.class.getName()).execute("");
        Uberspect uber = new SandboxUberspectImpl(null, sandbox);
        JexlEngine sjexl = new JexlEngine(uber, null, null, null);
        sjexl.setStrict(true);

        script = sjexl.createScript(expr);
        result = script.execute(null);
        assertEquals("42", ((Foo) result).getName());
    }

    public void testMethodWhite() throws Exception {
        Foo foo = new Foo("42");
        String expr = "foo.Quux()";
        Script script;
        Object result;

        Sandbox sandbox = new Sandbox();
        sandbox.white(Foo.class.getName()).execute("Quux");
        Uberspect uber = new SandboxUberspectImpl(null, sandbox);
        JexlEngine sjexl = new JexlEngine(uber, null, null, null);
        sjexl.setStrict(true);

        script = sjexl.createScript(expr, "foo");
        result = script.execute(null, foo);
        assertEquals(foo.Quux(), result);
    }

    public void testGetWhite() throws Exception {
        Foo foo = new Foo("42");
        String expr = "foo.alias";
        Script script;
        Object result;

        Sandbox sandbox = new Sandbox();
        sandbox.white(Foo.class.getName()).read("alias");
        Uberspect uber = new SandboxUberspectImpl(null, sandbox);
        JexlEngine sjexl = new JexlEngine(uber, null, null, null);
        sjexl.setStrict(true);

        script = sjexl.createScript(expr, "foo");
        result = script.execute(null, foo);
        assertEquals(foo.alias, result);
    }

    public void testSetWhite() throws Exception {
        Foo foo = new Foo("42");
        String expr = "foo.alias = $0";
        Script script;
        Object result;

        Sandbox sandbox = new Sandbox();
        sandbox.white(Foo.class.getName()).write("alias");
        Uberspect uber = new SandboxUberspectImpl(null, sandbox);
        JexlEngine sjexl = new JexlEngine(uber, null, null, null);
        sjexl.setStrict(true);

        script = sjexl.createScript(expr, "foo", "$0");
        result = script.execute(null, foo, "43");
        assertEquals("43", result);
        assertEquals("43", foo.alias);
    }
}
