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
package org.apache.commons.jexl3.internal.introspection;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JexlTestCase;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.internal.introspection.nojexlpackage.Invisible;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Checks the CacheMap.MethodKey implementation
 */

public class PermissionsTest {


    public static class A {
        public int i;
        public A() {}
        public int method() { return 0; }
    }

    //@NoJexl
    public interface InterNoJexl0 {
        int method();
    }

    public interface InterNoJexl1 {
        //@NoJexl
        int method();
    }

    public static class A0 extends A implements InterNoJexl0 {
        /*@NoJexl*/ public int i0;
        /*@NoJexl*/ public A0() {}
        @Override public int method() { return 1; }
    }

    public static class A1 extends A implements InterNoJexl1 {
        private int i1;
        /*@NoJexl*/ public A1() {}
        @Override public int method() { return 2; }
    }

    //@NoJexl
    public static class A2 extends A  {
        public A2() {}
        @Override public int method() { return 3; }
    }

    protected static class A3 {
        protected int i3;
        protected A3() {}
        int method() { return 4; }
    }

    public static class A5 implements InterNoJexl5 {
        public A5() {}
        @Override public int method() { return 0; }
    }

    //@NoJexl
    public interface InterNoJexl5 {
        int method();
    }

    JexlPermissions permissions0() {
        final String src = " org.apache.commons.jexl3.internal.introspection { PermissionsTest { " +
                "InterNoJexl0 { } " +
                "InterNoJexl1 { method(); } " +
                "A0 { A0(); i0; } " +
                "A1 { A1(); } " +
                "A2 { } " +
                "InterNoJexl5 { } " +
                "} }";
        final JexlPermissions p = JexlPermissions.parse(src);
        return p;
    }

    @Test
    public void testPermissions0() throws Exception {
        runTestPermissions(permissions0());
    }

    @Test
    public void testPermissions1() throws Exception {
        runTestPermissions(new JexlPermissions.Delegate(permissions0()) {
            @Override public String toString() {
                return "delegate:" + base.toString();
            }
        });
    }

    @Test
    public void testPermissions2() throws Exception {
        runTestPermissions(new JexlPermissions.ClassPermissions(permissions0(), Collections.emptySet()));
    }

    private void runTestPermissions(final JexlPermissions p) throws Exception {
        Assert.assertFalse(p.allow((Field) null));
        Assert.assertFalse(p.allow((Package) null));
        Assert.assertFalse(p.allow((Method) null));
        Assert.assertFalse(p.allow((Constructor<?>) null));
        Assert.assertFalse(p.allow((Class<?>) null));

        Assert.assertFalse(p.allow(A2.class));
        Assert.assertTrue(p.allow(A3.class));
        Assert.assertTrue(p.allow(A5.class));

        final Method mA = A.class.getMethod("method");
        Assert.assertNotNull(mA);
        final Method mA0 = A0.class.getMethod("method");
        Assert.assertNotNull(mA0);
        final Method mA1 = A1.class.getMethod("method");
        Assert.assertNotNull(mA1);
        final Method mA2 = A2.class.getMethod("method");
        Assert.assertNotNull(mA2);
        final Method mA3 = A2.class.getDeclaredMethod("method");
        Assert.assertNotNull(mA3);

        Assert.assertTrue(p.allow(mA));
        Assert.assertFalse(p.allow(mA0));
        Assert.assertFalse(p.allow(mA1));
        Assert.assertFalse(p.allow(mA2));
        Assert.assertFalse(p.allow(mA3));

        final Field fA = A.class.getField("i");
        Assert.assertNotNull(fA);
        Assert.assertTrue(p.allow(fA));

        final Field fA0 = A0.class.getField("i0");
        Assert.assertNotNull(fA0);
        Assert.assertFalse(p.allow(fA0));
        final Field fA1 = A1.class.getDeclaredField("i1");
        Assert.assertNotNull(fA1);
        Assert.assertFalse(p.allow(fA0));

        final Constructor<?> cA = A.class.getConstructor();
        Assert.assertNotNull(cA);
        Assert.assertTrue(p.allow(cA));

        final Constructor<?> cA0 = A0.class.getConstructor();
        Assert.assertNotNull(cA0);
        Assert.assertFalse(p.allow(cA0));

        final Constructor<?> cA3 = A3.class.getDeclaredConstructor();
        Assert.assertNotNull(cA3);
        Assert.assertFalse(p.allow(cA3));
    }

    static Method getMethod(final Class<?> clazz, final String method) {
        return Arrays.stream(clazz.getMethods()).filter(mth->mth.getName().equals(method)).findFirst().get();
    }

    @Test
    public void testParsePermissions0() throws Exception {
        final String src = "java.lang { Runtime { exit(); exec(); } }\njava.net { URL {} }";
        final Permissions p = (Permissions) JexlPermissions.parse(src);
        final Map<String, Permissions.NoJexlPackage> nojexlmap = p.getPackages();
        Assert.assertNotNull(nojexlmap);
        final Permissions.NoJexlPackage njp = nojexlmap.get("java.lang");
        Assert.assertNotNull(njp);
        final Method exit = getMethod(java.lang.Runtime.class,"exit");
        Assert.assertNotNull(exit);
        Assert.assertFalse(p.allow(exit));
        final Method exec = getMethod(java.lang.Runtime.class,"exec");
        Assert.assertNotNull(exec);
        Assert.assertFalse(p.allow(exec));
        final JexlUberspect uber = new Uberspect(null, null, p);
        Assert.assertNull(uber.getClassByName("java.net.URL"));
    }

    public static class Outer {
        public static class Inner {
            public void callMeNot() {}
        }
    }

    @Test
    public void testGetPackageName() {
        final String PKG = "org.apache.commons.jexl3.internal.introspection";
        String pkg = ClassTool.getPackageName(Outer.class);
        Assert.assertEquals(PKG, pkg);
        pkg = ClassTool.getPackageName(Outer.Inner.class);
        Assert.assertEquals(PKG, pkg);
        final Outer[] oo = {};
        pkg = ClassTool.getPackageName(oo.getClass());
        Assert.assertEquals(PKG, pkg);
        final Outer.Inner[] ii = {};
        pkg = ClassTool.getPackageName(ii.getClass());
        Assert.assertEquals(PKG, pkg);
        pkg = ClassTool.getPackageName(Process.class);
        Assert.assertEquals("java.lang", pkg);
        pkg = ClassTool.getPackageName(Integer.TYPE);
        Assert.assertEquals("java.lang", pkg);
    }

    @Test
    public void testParsePermissions1() {
        final String[] src = {
                "java.lang.*",
                "java.math.*",
                "java.text.*",
                "java.util.*",
                "java.lang { Runtime {} }",
                "java.rmi {}",
                "java.io { File {} }",
                "java.nio { Path {} }" ,
                "org.apache.commons.jexl3.internal.introspection { " +
                    "PermissionsTest { #level 0\n" +
                        " Outer { #level 1\n" +
                            " Inner { #level 2\n" +
                                " callMeNot();" +
                            " }" +
                        " }" +
                    " }" +
                " }"};
        final Permissions p = (Permissions) JexlPermissions.parse(src);
        final Map<String, Permissions.NoJexlPackage> nojexlmap = p.getPackages();
        Assert.assertNotNull(nojexlmap);
        final Set<String> wildcards = p.getWildcards();
        Assert.assertEquals(4, wildcards.size());

        final JexlEngine jexl = new JexlBuilder().permissions(p).safe(false).lexical(true).create();

        final Method exit = getMethod(java.lang.Runtime.class,"exit");
        Assert.assertNotNull(exit);
        Assert.assertFalse(p.allow(exit));
        final Method exec = getMethod(java.lang.Runtime.class,"getRuntime");
        Assert.assertNotNull(exec);
        Assert.assertFalse(p.allow(exec));
        final Method callMeNot = getMethod(Outer.Inner.class, "callMeNot");
        Assert.assertNotNull(callMeNot);
        Assert.assertFalse(p.allow(callMeNot));
        JexlScript script = jexl.createScript("o.callMeNot()", "o");
        try {
            final Object result = script.execute(null, new Outer.Inner());
            Assert.fail("callMeNot should be uncallable");
        } catch (final JexlException.Method xany) {
            Assert.assertEquals("callMeNot", xany.getMethod());
        }
        final Method uncallable = getMethod(Invisible.class, "uncallable");
        Assert.assertFalse(p.allow(uncallable));
        final Package ip = Invisible.class.getPackage();
        Assert.assertFalse(p.allow(ip));
        script = jexl.createScript("o.uncallable()", "o");
        try {
            final Object result = script.execute(null, new Invisible());
            Assert.fail("uncallable should be uncallable");
        } catch (final JexlException.Method xany) {
            Assert.assertEquals("uncallable", xany.getMethod());
        }
    }

    @Test
    public void testWildCardPackages() {
        Set<String> wildcards;
        boolean found;
        wildcards = new HashSet<>(Arrays.asList("com.apache.*"));
        found = Permissions.wildcardAllow(wildcards, "com.apache.commons.jexl3");
        Assert.assertTrue(found);
        found = Permissions.wildcardAllow(wildcards, "com.google.spexl");
        Assert.assertFalse(found);
    }

    @Test
    public void testSecurePermissions() {
        Assert.assertNotNull(JexlTestCase.SECURE);
        final List<Class<?>> acs = Arrays.asList(
            java.lang.Runtime.class,
            java.math.BigDecimal.class,
            java.text.SimpleDateFormat.class,
            java.util.Map.class);
        for(final Class<?> ac: acs) {
            final Package p = ac.getPackage();
            Assert.assertNotNull(ac.getName(), p);
            Assert.assertTrue(ac.getName(), JexlTestCase.SECURE.allow(p));
        }
        final List<Class<?>> nacs = Arrays.asList(
                java.lang.annotation.ElementType.class,
                java.lang.instrument.ClassDefinition.class,
                java.lang.invoke.CallSite.class,
                java.lang.management.BufferPoolMXBean.class,
                java.lang.ref.SoftReference.class,
                java.lang.reflect.Method.class);
        for(final Class<?> nac : nacs) {
            final Package p = nac.getPackage();
            Assert.assertNotNull(nac.getName(), p);
            Assert.assertFalse(nac.getName(), JexlTestCase.SECURE.allow(p));
        }
    }


    @Test
    public void testParsePermissionsFailures() {
        final String[] srcs = {
                "java.lang.*.*",
                "java.math.*.",
                "java.text.*;",
                "java.lang {{ Runtime {} }",
                "java.rmi {}}",
                "java.io { Text File {} }",
                "java.io { File { m.x } }"
        };
        for(final String src : srcs) {
            try {
                final Permissions p = (Permissions) JexlPermissions.parse(src);
                Assert.fail(src);
            } catch (final IllegalStateException xill) {
                Assert.assertNotNull(xill);
            }
        }
    }

    protected static class Foo2 {
        protected String protectedMethod() {
            return "foo2";
        }
        public String publicMethod() {
            return "foo2";
        }
    }

    public static class Foo3 extends Foo2 {
        @Override public String protectedMethod() {
            return "foo3";
        }
        @Override public String publicMethod() {
            return "foo3";
        }
    }

    @Test public void testProtectedOverride0() {
        JexlScript script;
        Object r;
        final Foo2 foo3 = new Foo3();
        final JexlEngine jexl = new JexlBuilder().safe(false).create();
        // call public override of protected, nok
        Foo2 foo2 = new Foo2();
        script = jexl.createScript("x.protectedMethod()", "x");
        try {
            r = script.execute(null, foo2);
            Assert.fail("protectedMethod() is not public through superclass Foo2");
        } catch (final JexlException xjexl) {
            Assert.assertNotNull(xjexl);
        }
        // call public override, ok
        foo2 = new Foo3();
        r = script.execute(null, foo3);
        Assert.assertEquals("foo3",r);
    }

    @Test public void testProtectedOverride1() {
        final List<String> a = new LinkedList<>();
        a.add("aaa");
        a.add("bbb");

        final String src = "a.clone()";
        final JexlEngine jexl = new JexlBuilder().safe(true).create();
        final JexlScript script = jexl.createScript(src);
        final JexlContext context = new MapContext();
        context.set("a", a);
        final Object result = script.execute(context, a);
        Assert.assertNotNull(result);
    }

    public class I33Arithmetic extends JexlArithmetic {
        public I33Arithmetic(final boolean astrict) {
            super(astrict);
        }

        /**
         * Same name signature as default private method.
         * @param s the string
         * @return a double, NaN if fail
         */
        public double parseDouble(final String s) {
            try {
                return Double.parseDouble(s);
            } catch (final NumberFormatException nfe) {
                return Double.NaN;
            }
        }
    }

    @Test public void testPrivateOverload1() throws Exception {
        final String src = "parseDouble(\"PHM1\".substring(3)).intValue()";
        final JexlArithmetic jexla = new I33Arithmetic(true);
        final JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(jexla).create();
        final JexlScript script = jexl.createScript(src);
        Assert.assertNotNull(script);
        final Object result = script.execute(null);
        Assert.assertEquals(1, result);
    }
}
