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
package org.apache.commons.jexl3.introspection;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JexlTestCase;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.annotations.NoJexl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests sandbox features.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class SandboxTest extends JexlTestCase {
    static final Log LOGGER = LogFactory.getLog(SandboxTest.class.getName());

    public SandboxTest() {
        super("SandboxTest");
    }


    public static class CantSeeMe {
        public boolean doIt() {
            return false;
        }
    }

    @NoJexl
    public interface CantCallMe {
        void tryMe();
    }

    public interface TryCallMe {
        @NoJexl
        void tryMeARiver();
    }

    public static abstract class CallMeNot {
        public @NoJexl
        String NONO = "should not be accessible!";

        @NoJexl
        public void callMeNot() {
            throw new RuntimeException("should not be callable!");
        }
        
        public String allowInherit() {
            return "this is allowed";
        }
    }

    public static class Foo extends CallMeNot implements CantCallMe, TryCallMe {
        String name;
        public String alias;

        public @NoJexl
        Foo(String name, String notcallable) {
            throw new RuntimeException("should not be callable!");
        }

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
        
        public int doIt() {
            return 42;
        }

        @NoJexl
        public String cantCallMe() {
            throw new RuntimeException("should not be callable!");
        }

        @Override
        public void tryMe() {
            throw new RuntimeException("should not be callable!");
        }

        @Override
        public void tryMeARiver() {
            throw new RuntimeException("should not be callable!");
        }
    }

    @Test
    public void testCtorBlack() throws Exception {
        String expr = "new('" + Foo.class.getName() + "', '42')";
        JexlScript script = JEXL.createScript(expr);
        Object result;
        result = script.execute(null);
        Assert.assertEquals("42", ((Foo) result).getName());

        JexlSandbox sandbox = new JexlSandbox();
        sandbox.black(Foo.class.getName()).execute("");
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr);
        try {
            result = script.execute(null);
            Assert.fail("ctor should not be accessible");
        } catch (JexlException.Method xmethod) {
            // ok, ctor should not have been accessible
            LOGGER.info(xmethod.toString());
        }
    }

    @Test
    public void testMethodBlack() throws Exception {
        String expr = "foo.Quux()";
        JexlScript script = JEXL.createScript(expr, "foo");
        Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo);
        Assert.assertEquals(foo.Quux(), result);

        JexlSandbox sandbox = new JexlSandbox();
        sandbox.black(Foo.class.getName()).execute("Quux");
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr, "foo");
        try {
            result = script.execute(null, foo);
            Assert.fail("Quux should not be accessible");
        } catch (JexlException.Method xmethod) {
            // ok, Quux should not have been accessible
            LOGGER.info(xmethod.toString());
        }
    }

    @Test
    public void testGetBlack() throws Exception {
        String expr = "foo.alias";
        JexlScript script = JEXL.createScript(expr, "foo");
        Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo);
        Assert.assertEquals(foo.alias, result);

        JexlSandbox sandbox = new JexlSandbox();
        sandbox.black(Foo.class.getName()).read("alias");
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr, "foo");
        try {
            result = script.execute(null, foo);
            Assert.fail("alias should not be accessible");
        } catch (JexlException.Property xvar) {
            // ok, alias should not have been accessible
            LOGGER.info(xvar.toString());
        }
    }

    @Test
    public void testSetBlack() throws Exception {
        String expr = "foo.alias = $0";
        JexlScript script = JEXL.createScript(expr, "foo", "$0");
        Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo, "43");
        Assert.assertEquals("43", result);

        JexlSandbox sandbox = new JexlSandbox();
        sandbox.black(Foo.class.getName()).write("alias");
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr, "foo", "$0");
        try {
            result = script.execute(null, foo, "43");
            Assert.fail("alias should not be accessible");
        } catch (JexlException.Property xvar) {
            // ok, alias should not have been accessible
            LOGGER.info(xvar.toString());
        }
    }
        
    @Test
    public void testCantSeeMe() throws Exception {
        JexlContext jc = new MapContext();
        String expr = "foo.doIt()";
        JexlScript script;
        Object result = null;

        JexlSandbox sandbox = new JexlSandbox(false);
        sandbox.white(Foo.class.getName());
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        jc.set("foo", new CantSeeMe());
        script = sjexl.createScript(expr);
        try {
            result = script.execute(jc);
            Assert.fail("should have failed, doIt()");
        } catch (JexlException xany) {
            //
        }
        jc.set("foo", new Foo("42"));
            result = script.execute(jc);
        Assert.assertEquals(42, ((Integer) result).intValue());
    }

    @Test
    public void testCtorWhite() throws Exception {
        String expr = "new('" + Foo.class.getName() + "', '42')";
        JexlScript script;
        Object result;

        JexlSandbox sandbox = new JexlSandbox();
        sandbox.white(Foo.class.getName()).execute("");
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr);
        result = script.execute(null);
        Assert.assertEquals("42", ((Foo) result).getName());
    }

    @Test
    public void testMethodWhite() throws Exception {
        Foo foo = new Foo("42");
        String expr = "foo.Quux()";
        JexlScript script;
        Object result;

        JexlSandbox sandbox = new JexlSandbox();
        sandbox.white(Foo.class.getName()).execute("Quux");
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr, "foo");
        result = script.execute(null, foo);
        Assert.assertEquals(foo.Quux(), result);
    }

    @Test
    public void testMethodNoJexl() throws Exception {
        Foo foo = new Foo("42");
        String[] exprs = {
            "foo.cantCallMe()",
            "foo.tryMe()",
            "foo.tryMeARiver()",
            "foo.callMeNot()",
            "foo.NONO",
            "new('org.apache.commons.jexl3.SandboxTest$Foo', 'one', 'two')"
        };
        JexlScript script;
        Object result;

        JexlEngine sjexl = new JexlBuilder().strict(true).safe(false).create();
        for (String expr : exprs) {
            script = sjexl.createScript(expr, "foo");
            try {
                result = script.execute(null, foo);
                Assert.fail("should have not been possible");
            } catch (JexlException.Method xjm) {
                // ok
                LOGGER.info(xjm.toString());
            } catch (JexlException.Property xjm) {
                // ok
                LOGGER.info(xjm.toString());
            }
        }
    }

    @Test
    public void testGetWhite() throws Exception {
        Foo foo = new Foo("42");
        String expr = "foo.alias";
        JexlScript script;
        Object result;

        JexlSandbox sandbox = new JexlSandbox();
        sandbox.white(Foo.class.getName()).read("alias");
        sandbox.get(Foo.class.getName()).read().alias("alias", "ALIAS");
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();

        script = sjexl.createScript(expr, "foo");
        result = script.execute(null, foo);
        Assert.assertEquals(foo.alias, result);

        script = sjexl.createScript("foo.ALIAS", "foo");
        result = script.execute(null, foo);
        Assert.assertEquals(foo.alias, result);
    }

    @Test
    public void testSetWhite() throws Exception {
        Foo foo = new Foo("42");
        String expr = "foo.alias = $0";
        JexlScript script;
        Object result;

        JexlSandbox sandbox = new JexlSandbox();
        sandbox.white(Foo.class.getName()).write("alias");
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();

        script = sjexl.createScript(expr, "foo", "$0");
        result = script.execute(null, foo, "43");
        Assert.assertEquals("43", result);
        Assert.assertEquals("43", foo.alias);
    }

    @Test
    public void testRestrict() throws Exception {
        JexlContext context = new MapContext();
        context.set("System", System.class);
        JexlSandbox sandbox = new JexlSandbox();
        // only allow call to currentTimeMillis (avoid exit, gc, loadLibrary, etc)
        sandbox.white(System.class.getName()).execute("currentTimeMillis");
        // can not create a new file
        sandbox.black(java.io.File.class.getName()).execute("");

        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();

        String expr;
        JexlScript script;
        Object result;

        script = sjexl.createScript("System.exit()");
        try {
            result = script.execute(context);
            Assert.fail("should not allow calling exit!");
        } catch (JexlException xjexl) {
            LOGGER.info(xjexl.toString());
        }

        script = sjexl.createScript("System.exit(1)");
        try {
            result = script.execute(context);
            Assert.fail("should not allow calling exit!");
        } catch (JexlException xjexl) {
            LOGGER.info(xjexl.toString());
        }

        script = sjexl.createScript("new('java.io.File', '/tmp/should-not-be-created')");
        try {
            result = script.execute(context);
            Assert.fail("should not allow creating a file");
        } catch (JexlException xjexl) {
            LOGGER.info(xjexl.toString());
        }

        expr = "System.currentTimeMillis()";
        script = sjexl.createScript("System.currentTimeMillis()");
        result = script.execute(context);
        Assert.assertNotNull(result);
    }

    @Test
       public void testSandboxInherit0() throws Exception {
        Object result;
        JexlContext ctxt = null;
        List<String> foo = new ArrayList<String>();
        JexlSandbox sandbox = new JexlSandbox(false, true);
        sandbox.white(java.util.List.class.getName());
        
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();
        JexlScript method = sjexl.createScript("foo.add(y)", "foo", "y");
        JexlScript set = sjexl.createScript("foo[x] = y", "foo", "x", "y");
        JexlScript get = sjexl.createScript("foo[x]", "foo", "x");

        result = method.execute(ctxt, foo, "nothing");
        Assert.assertEquals(true, result);
        result = null;
        result = get.execute(null, foo, 0);
        Assert.assertEquals("nothing", result);
        result = null;
        result = set.execute(null, foo, 0, "42");
        Assert.assertEquals("42", result);

        result = null;
        result = get.execute(null, foo, 0);
        Assert.assertEquals("42", result);
    }
    
    public abstract static class Operation {
        protected final int base;
        public Operation(int sz) {
         base = sz;
        }
        
        public abstract int someOp(int x);
        public abstract int nonCallable(int y);
    }

    public static class Operation2 extends Operation {
        public Operation2(int sz) {
            super(sz);
        }

        @Override
        public int someOp(int x) {
            return base + x;
        }

        @Override
        public int nonCallable(int y) {
            throw new UnsupportedOperationException("do NOT call");
        }
    }

    @Test
    public void testSandboxInherit1() throws Exception {
        Object result;
        JexlContext ctxt = null;
        Operation2 foo = new Operation2(12);
        JexlSandbox sandbox = new JexlSandbox(false, true);
        sandbox.white(Operation.class.getName());
        sandbox.black(Operation.class.getName()).execute("nonCallable");
        //sandbox.black(Foo.class.getName()).execute();
        JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();
        JexlScript someOp = sjexl.createScript("foo.someOp(y)", "foo", "y");
        result = someOp.execute(ctxt, foo, 30);
        Assert.assertEquals(42, result);
        JexlScript nonCallable = sjexl.createScript("foo.nonCallable(y)", "foo", "y");
        try {
            result = nonCallable.execute(null, foo, 0);
            Assert.fail("should not be possible");
        } catch (JexlException xjm) {
            // ok
            LOGGER.info(xjm.toString());
        }
    }
    
    public static class Foo42 {
        public int getFoo() {
            return 42;
        }
    }

    public static class Foo43 extends Foo42 {
        @Override
        @NoJexl
        public int getFoo() {
            return 43;
        }
    }

    public static class Foo44 extends Foo43 {
        @Override
        public int getFoo() {
            return 44;
        }
    }
    
    @Test
    public void testNoJexl312() throws Exception {
        JexlContext ctxt = new MapContext();
        
        JexlEngine sjexl = new JexlBuilder().safe(false).strict(true).create();
        JexlScript foo = sjexl.createScript("x.getFoo()", "x");
        try {
            foo.execute(ctxt, new Foo44());
            Assert.fail("should have thrown");
        } catch (JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }
}
