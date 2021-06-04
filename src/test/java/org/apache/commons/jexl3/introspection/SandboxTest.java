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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JexlTestCase;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.annotations.NoJexl;

import org.apache.commons.jexl3.internal.introspection.Permissions;
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
        Foo(final String name, final String notcallable) {
            throw new RuntimeException("should not be callable!");
        }

        public Foo(final String name) {
            this.name = name;
            this.alias = name + "-alias";
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
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
    public void testCtorBlock() throws Exception {
        final String expr = "new('" + Foo.class.getName() + "', '42')";
        JexlScript script = JEXL.createScript(expr);
        Object result;
        result = script.execute(null);
        Assert.assertEquals("42", ((Foo) result).getName());

        final JexlSandbox sandbox = new JexlSandbox();
        sandbox.block(Foo.class.getName()).execute("");
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr);
        try {
            result = script.execute(null);
            Assert.fail("ctor should not be accessible");
        } catch (final JexlException.Method xmethod) {
            // ok, ctor should not have been accessible
            LOGGER.debug(xmethod.toString());
        }
    }

    @Test
    public void testMethodBlock() throws Exception {
        final String expr = "foo.Quux()";
        JexlScript script = JEXL.createScript(expr, "foo");
        final Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo);
        Assert.assertEquals(foo.Quux(), result);

        final JexlSandbox sandbox = new JexlSandbox();
        sandbox.block(Foo.class.getName()).execute("Quux");
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr, "foo");
        try {
            result = script.execute(null, foo);
            Assert.fail("Quux should not be accessible");
        } catch (final JexlException.Method xmethod) {
            // ok, Quux should not have been accessible
            LOGGER.debug(xmethod.toString());
        }
    }

    @Test
    public void testGetBlock() throws Exception {
        final String expr = "foo.alias";
        JexlScript script = JEXL.createScript(expr, "foo");
        final Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo);
        Assert.assertEquals(foo.alias, result);

        final JexlSandbox sandbox = new JexlSandbox();
        sandbox.block(Foo.class.getName()).read("alias");
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr, "foo");
        try {
            result = script.execute(null, foo);
            Assert.fail("alias should not be accessible");
        } catch (final JexlException.Property xvar) {
            // ok, alias should not have been accessible
            LOGGER.debug(xvar.toString());
        }
    }

    @Test
    public void testSetBlock() throws Exception {
        final String expr = "foo.alias = $0";
        JexlScript script = JEXL.createScript(expr, "foo", "$0");
        final Foo foo = new Foo("42");
        Object result;
        result = script.execute(null, foo, "43");
        Assert.assertEquals("43", result);

        final JexlSandbox sandbox = new JexlSandbox();
        sandbox.block(Foo.class.getName()).write("alias");
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr, "foo", "$0");
        try {
            result = script.execute(null, foo, "43");
            Assert.fail("alias should not be accessible");
        } catch (final JexlException.Property xvar) {
            // ok, alias should not have been accessible
            LOGGER.debug(xvar.toString());
        }
    }

    @Test
    public void testCantSeeMe() throws Exception {
        final JexlContext jc = new MapContext();
        final String expr = "foo.doIt()";
        JexlScript script;
        Object result;

        final JexlSandbox sandbox = new JexlSandbox(false);
        sandbox.allow(Foo.class.getName());
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        jc.set("foo", new CantSeeMe());
        script = sjexl.createScript(expr);
        try {
            result = script.execute(jc);
            Assert.fail("should have failed, doIt()");
        } catch (final JexlException xany) {
            //
        }
        jc.set("foo", new Foo("42"));
            result = script.execute(jc);
        Assert.assertEquals(42, ((Integer) result).intValue());
    }

    @Test
    public void testCtorAllow() throws Exception {
        final String expr = "new('" + Foo.class.getName() + "', '42')";
        JexlScript script;
        Object result;

        final JexlSandbox sandbox = new JexlSandbox();
        sandbox.allow(Foo.class.getName()).execute("");
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr);
        result = script.execute(null);
        Assert.assertEquals("42", ((Foo) result).getName());
    }

    @Test
    public void testMethodAllow() throws Exception {
        final Foo foo = new Foo("42");
        final String expr = "foo.Quux()";
        JexlScript script;
        Object result;

        final JexlSandbox sandbox = new JexlSandbox();
        sandbox.allow(Foo.class.getName()).execute("Quux");
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).strict(true).safe(false).create();

        script = sjexl.createScript(expr, "foo");
        result = script.execute(null, foo);
        Assert.assertEquals(foo.Quux(), result);
    }

    @Test
    public void testMethodNoJexl() throws Exception {
        final Foo foo = new Foo("42");
        final String[] exprs = {
            "foo.cantCallMe()",
            "foo.tryMe()",
            "foo.tryMeARiver()",
            "foo.callMeNot()",
            "foo.NONO",
            "new('org.apache.commons.jexl3.SandboxTest$Foo', 'one', 'two')"
        };
        JexlScript script;
        Object result;

        final JexlEngine sjexl = new JexlBuilder().strict(true).safe(false).create();
        for (final String expr : exprs) {
            script = sjexl.createScript(expr, "foo");
            try {
                result = script.execute(null, foo);
                Assert.fail("should have not been possible");
            } catch (JexlException.Method | JexlException.Property xjm) {
                // ok
                LOGGER.debug(xjm.toString());
            }
        }
    }

    @Test
    public void testGetAllow() throws Exception {
        final Foo foo = new Foo("42");
        final String expr = "foo.alias";
        JexlScript script;
        Object result;

        final JexlSandbox sandbox = new JexlSandbox();
        sandbox.allow(Foo.class.getName()).read("alias");
        sandbox.get(Foo.class.getName()).read().alias("alias", "ALIAS");
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();

        script = sjexl.createScript(expr, "foo");
        result = script.execute(null, foo);
        Assert.assertEquals(foo.alias, result);

        script = sjexl.createScript("foo.ALIAS", "foo");
        result = script.execute(null, foo);
        Assert.assertEquals(foo.alias, result);
    }

    @Test
    public void testSetAllow() throws Exception {
        final Foo foo = new Foo("42");
        final String expr = "foo.alias = $0";
        JexlScript script;
        Object result;

        final JexlSandbox sandbox = new JexlSandbox();
        sandbox.allow(Foo.class.getName()).write("alias");
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();

        script = sjexl.createScript(expr, "foo", "$0");
        result = script.execute(null, foo, "43");
        Assert.assertEquals("43", result);
        Assert.assertEquals("43", foo.alias);
    }

    @Test
    public void testRestrict() throws Exception {
        final JexlContext context = new MapContext();
        context.set("System", System.class);
        final JexlSandbox sandbox = new JexlSandbox();
        // only allow call to currentTimeMillis (avoid exit, gc, loadLibrary, etc)
        sandbox.allow(System.class.getName()).execute("currentTimeMillis");
        // can not create a new file
        sandbox.block(java.io.File.class.getName()).execute("");

        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();

        String expr;
        JexlScript script;
        Object result;

        script = sjexl.createScript("System.exit()");
        try {
            result = script.execute(context);
            Assert.fail("should not allow calling exit!");
        } catch (final JexlException xjexl) {
            LOGGER.debug(xjexl.toString());
        }

        script = sjexl.createScript("System.exit(1)");
        try {
            result = script.execute(context);
            Assert.fail("should not allow calling exit!");
        } catch (final JexlException xjexl) {
            LOGGER.debug(xjexl.toString());
        }

        script = sjexl.createScript("new('java.io.File', '/tmp/should-not-be-created')");
        try {
            result = script.execute(context);
            Assert.fail("should not allow creating a file");
        } catch (final JexlException xjexl) {
            LOGGER.debug(xjexl.toString());
        }

        expr = "System.currentTimeMillis()";
        script = sjexl.createScript("System.currentTimeMillis()");
        result = script.execute(context);
        Assert.assertNotNull(result);
    }

    @Test
       public void testSandboxInherit0() throws Exception {
        Object result;
        final JexlContext ctxt = null;
        final List<String> foo = new ArrayList<String>();
        final JexlSandbox sandbox = new JexlSandbox(false, true);
        sandbox.allow(java.util.List.class.getName());

        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();
        final JexlScript method = sjexl.createScript("foo.add(y)", "foo", "y");
        final JexlScript set = sjexl.createScript("foo[x] = y", "foo", "x", "y");
        final JexlScript get = sjexl.createScript("foo[x]", "foo", "x");

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
        public Operation(final int sz) {
         base = sz;
        }

        public abstract int someOp(int x);
        public abstract int nonCallable(int y);
    }

    public static class Operation2 extends Operation {
        public Operation2(final int sz) {
            super(sz);
        }

        @Override
        public int someOp(final int x) {
            return base + x;
        }

        @Override
        public int nonCallable(final int y) {
            throw new UnsupportedOperationException("do NOT call");
        }
    }

    @Test
    public void testSandboxInherit1() throws Exception {
        Object result;
        final JexlContext ctxt = null;
        final Operation2 foo = new Operation2(12);
        final JexlSandbox sandbox = new JexlSandbox(false, true);
        sandbox.allow(Operation.class.getName());
        sandbox.block(Operation.class.getName()).execute("nonCallable");
        //sandbox.block(Foo.class.getName()).execute();
        final JexlEngine sjexl = new JexlBuilder().sandbox(sandbox).safe(false).strict(true).create();
        final JexlScript someOp = sjexl.createScript("foo.someOp(y)", "foo", "y");
        result = someOp.execute(ctxt, foo, 30);
        Assert.assertEquals(42, result);
        final JexlScript nonCallable = sjexl.createScript("foo.nonCallable(y)", "foo", "y");
        try {
            result = nonCallable.execute(null, foo, 0);
            Assert.fail("should not be possible");
        } catch (final JexlException xjm) {
            // ok
            LOGGER.debug(xjm.toString());
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
        final JexlContext ctxt = new MapContext();

        final JexlEngine sjexl = new JexlBuilder().safe(false).strict(true).create();
        final JexlScript foo = sjexl.createScript("x.getFoo()", "x");
        try {
            foo.execute(ctxt, new Foo44());
            Assert.fail("should have thrown");
        } catch (final JexlException xany) {
            Assert.assertNotNull(xany);
        }
    }

    @Test
    public void testGetNullKeyAllowed0() throws Exception {
        JexlEngine jexl = new JexlBuilder().sandbox(new JexlSandbox(true)).create();
        JexlExpression expression = jexl.createExpression("{null : 'foo'}[null]");
        Object o = expression.evaluate(null);
        Assert.assertEquals("foo", o);
    }

    @Test
    public void testGetNullKeyAllowed1() throws Exception {
        JexlSandbox sandbox = new JexlSandbox(true, true);
        JexlSandbox.Permissions p = sandbox.permissions("java.util.Map", false, true, true);
        p.read().add("quux");
        JexlEngine jexl = new JexlBuilder().sandbox(sandbox).create();
        // cant read quux
        String q = "'quux'"; //quotes are important!
        JexlExpression expression = jexl.createExpression("{"+q+" : 'foo'}["+q+"]");
        try {
            Object o = expression.evaluate(null);
            Assert.fail("should have blocked " + q);
        } catch (JexlException.Property xp) {
            Assert.assertTrue(xp.getMessage().contains("undefined"));
        }
        // can read foo, null
        for(String k : Arrays.asList("'foo'", "null")) {
            expression = jexl.createExpression("{"+k+" : 'foo'}["+k+"]");
            Object o = expression.evaluate(null);
            Assert.assertEquals("foo", o);
        }
    }

    @Test
    public void testGetNullKeyBlocked() throws Exception {
        JexlSandbox sandbox = new JexlSandbox(true, true);
        JexlSandbox.Permissions p = sandbox.permissions("java.util.Map", false, true, true);
        p.read().add(null);
        p.read().add("quux");
        // can read bar
        JexlEngine jexl = new JexlBuilder().sandbox(sandbox).create();
        JexlExpression e0 = jexl.createExpression("{'bar' : 'foo'}['bar']");
        Object r0 = e0.evaluate(null);
        Assert.assertEquals("foo", r0);
        // can not read quux, null
        for(String k : Arrays.asList("'quux'", "null")) {
            JexlExpression expression = jexl.createExpression("{"+k+" : 'foo'}["+k+"]");
            try {
                Object o = expression.evaluate(null);
                Assert.fail("should have blocked " + k);
            } catch (JexlException.Property xp) {
                Assert.assertTrue(xp.getMessage().contains("undefined"));
            }
        }
    }

    public static class Arithmetic350 extends JexlArithmetic {
        // cheat and keep the map builder around
        MapBuilder mb = new org.apache.commons.jexl3.internal.MapBuilder(3);
        public Arithmetic350(boolean astrict) {
            super(astrict);
        }
        public MapBuilder mapBuilder(final int size) {
            return mb;
        }
        Map<?,?> getLastMap() {
            return (Map<Object,Object>) mb.create();
        }
    }

    @Test
    public void testSetNullKeyAllowed0() throws Exception {
        Arithmetic350 a350 = new Arithmetic350(true);
        JexlEngine jexl = new JexlBuilder().arithmetic(a350).sandbox(new JexlSandbox(true)).create();
        JexlContext jc = new MapContext();
        JexlExpression expression = jexl.createExpression("{null : 'foo'}[null] = 'bar'");
        expression.evaluate(jc);
        Map<?,?> map = a350.getLastMap();
        Assert.assertEquals("bar", map.get(null));
    }

    @Test
    public void testSetNullKeyAllowed1() throws Exception {
        Arithmetic350 a350 = new Arithmetic350(true);
        JexlSandbox sandbox = new JexlSandbox(true, true);
        JexlSandbox.Permissions p = sandbox.permissions("java.util.Map", true, false, true);
        p.write().add("quux");
        JexlEngine jexl = new JexlBuilder().arithmetic(a350).sandbox(sandbox).create();
        // can not write quux
        String q = "'quux'"; //quotes are important!
        JexlExpression expression = jexl.createExpression("{"+q+" : 'foo'}["+q+"] = '42'");
        try {
            Object o = expression.evaluate(null);
            Assert.fail("should have blocked " + q);
        } catch (JexlException.Property xp) {
            Assert.assertTrue(xp.getMessage().contains("undefined"));
        }
        // can write bar, null
        expression = jexl.createExpression("{'bar' : 'foo'}['bar'] = '42'");
        expression.evaluate(null);
        Map<?, ?> map = a350.getLastMap();
        Assert.assertEquals("42", map.get("bar"));
        map.clear();
        expression = jexl.createExpression("{null : 'foo'}[null] = '42'");
        expression.evaluate(null);
        map = a350.getLastMap();
        Assert.assertEquals("42", map.get(null));
    }

    @Test
    public void testSetNullKeyBlocked() throws Exception {
        Arithmetic350 a350 = new Arithmetic350(true);
        JexlSandbox sandbox = new JexlSandbox(true, true);
        JexlSandbox.Permissions p = sandbox.permissions("java.util.Map", true, false, true);
        p.write().add(null);
        p.write().add("quux");
        JexlEngine jexl = new JexlBuilder().arithmetic(a350).sandbox(sandbox).create();
        // can write bar
        JexlExpression expression = jexl.createExpression("{'bar' : 'foo'}['bar'] = '42'");
        expression.evaluate(null);
        Map<?,?> map = a350.getLastMap();
        Assert.assertEquals("42", map.get("bar"));
        // can not write quux, null
        for(String k : Arrays.asList("'quux'", "null")) {
            expression = jexl.createExpression("{"+k+" : 'foo'}["+k+"] = '42'");
            try {
                Object o = expression.evaluate(null);
                Assert.fail("should have blocked " + k);
            } catch (JexlException.Property xp) {
                Assert.assertTrue(xp.getMessage().contains("undefined"));
            }
        }
    }
}
