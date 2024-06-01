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

import static org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.junit.Test;

/**
 * Checks the CacheMap.MethodKey implementation
 */

public class PermissionsTest {

    public static class A {
        public int i;
        public A() {}
        public int method() { return 0; }
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

    //@NoJexl
    public interface InterNoJexl0 {
        int method();
    }

    public interface InterNoJexl1 {
        //@NoJexl
        int method();
    }

    //@NoJexl
    public interface InterNoJexl5 {
        int method();
    }

    public static class Outer {
        public static class Inner {
            public void callMeNot() {}
        }
    }

    static Method getMethod(final Class<?> clazz, final String method) {
        return Arrays.stream(clazz.getMethods()).filter(mth->mth.getName().equals(method)).findFirst().get();
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

    private void runTestPermissions(final JexlPermissions p) throws Exception {
        assertFalse(p.allow((Field) null));
        assertFalse(p.allow((Package) null));
        assertFalse(p.allow((Method) null));
        assertFalse(p.allow((Constructor<?>) null));
        assertFalse(p.allow((Class<?>) null));

        assertFalse(p.allow(A2.class));
        assertTrue(p.allow(A3.class));
        assertTrue(p.allow(A5.class));

        final Method mA = A.class.getMethod("method");
        assertNotNull(mA);
        final Method mA0 = A0.class.getMethod("method");
        assertNotNull(mA0);
        final Method mA1 = A1.class.getMethod("method");
        assertNotNull(mA1);
        final Method mA2 = A2.class.getMethod("method");
        assertNotNull(mA2);
        final Method mA3 = A2.class.getDeclaredMethod("method");
        assertNotNull(mA3);

        assertTrue(p.allow(mA));
        assertFalse(p.allow(mA0));
        assertFalse(p.allow(mA1));
        assertFalse(p.allow(mA2));
        assertFalse(p.allow(mA3));

        final Field fA = A.class.getField("i");
        assertNotNull(fA);
        assertTrue(p.allow(fA));

        final Field fA0 = A0.class.getField("i0");
        assertNotNull(fA0);
        assertFalse(p.allow(fA0));
        final Field fA1 = A1.class.getDeclaredField("i1");
        assertNotNull(fA1);
        assertFalse(p.allow(fA0));

        final Constructor<?> cA = A.class.getConstructor();
        assertNotNull(cA);
        assertTrue(p.allow(cA));

        final Constructor<?> cA0 = A0.class.getConstructor();
        assertNotNull(cA0);
        assertFalse(p.allow(cA0));

        final Constructor<?> cA3 = A3.class.getDeclaredConstructor();
        assertNotNull(cA3);
        assertFalse(p.allow(cA3));
    }

    @Test
    public void testGetPackageName() {
        final String PKG = "org.apache.commons.jexl3.internal.introspection";
        String pkg = ClassTool.getPackageName(Outer.class);
        assertEquals(PKG, pkg);
        pkg = ClassTool.getPackageName(Outer.Inner.class);
        assertEquals(PKG, pkg);
        final Outer[] oo = {};
        pkg = ClassTool.getPackageName(oo.getClass());
        assertEquals(PKG, pkg);
        final Outer.Inner[] ii = {};
        pkg = ClassTool.getPackageName(ii.getClass());
        assertEquals(PKG, pkg);
        pkg = ClassTool.getPackageName(Process.class);
        assertEquals("java.lang", pkg);
        pkg = ClassTool.getPackageName(Integer.TYPE);
        assertEquals("java.lang", pkg);
    }

    @Test
    public void testParsePermissions0a() throws Exception {
        final String src = "java.lang { Runtime { exit(); exec(); } }\njava.net { URL {} }";
        final Permissions p = (Permissions) JexlPermissions.parse(src);
        final Map<String, Permissions.NoJexlPackage> nojexlmap = p.getPackages();
        assertNotNull(nojexlmap);
        final Permissions.NoJexlPackage njp = nojexlmap.get("java.lang");
        assertNotNull(njp);
        final Method exit = getMethod(java.lang.Runtime.class,"exit");
        assertNotNull(exit);
        assertFalse(p.allow(exit));
        final Method exec = getMethod(java.lang.Runtime.class,"exec");
        assertNotNull(exec);
        assertFalse(p.allow(exec));
        final Method avp = getMethod(java.lang.Runtime.class,"availableProcessors");
        assertNotNull(avp);
        assertTrue(p.allow(avp));
        final JexlUberspect uber = new Uberspect(null, null, p);
        assertNull(uber.getClassByName("java.net.URL"));
    }

    @Test
    public void testParsePermissions0b() throws Exception {
        final String src = "java.lang { -Runtime { exit(); } }";
        final Permissions p = (Permissions) JexlPermissions.parse(src);
        final Method exit = getMethod(java.lang.Runtime.class,"exit");
        assertNotNull(exit);
        assertFalse(p.allow(exit));
    }

    @Test
    public void testParsePermissions0c() throws Exception {
        final String src = "java.lang { +Runtime { availableProcessorCount(); } }";
        final Permissions p = (Permissions) JexlPermissions.parse(src);
        final Method exit = getMethod(java.lang.Runtime.class,"exit");
        assertNotNull(exit);
        assertFalse(p.allow(exit));
    }

    @Test
    public void testParsePermissions0d() throws Exception {
        final String src = "java.lang { +System { currentTimeMillis(); } }";
        final JexlPermissions p = RESTRICTED.compose(src);
        final Field in = System.class.getField("in");
        assertNotNull(in);
        assertFalse(p.allow(in));
        final Method ctm = getMethod(java.lang.System.class,"currentTimeMillis");
        assertNotNull(ctm);
        assertTrue(p.allow(ctm));
    }

    @Test
    public void testParsePermissions0e() throws Exception {
        final String src = "java.lang { +System { in; } }";
        final JexlPermissions p = RESTRICTED.compose(src);
        final Field in = System.class.getField("in");
        assertNotNull(in);
        assertTrue(p.allow(in));
        final Method ctm = getMethod(java.lang.System.class,"currentTimeMillis");
        assertNotNull(ctm);
        assertFalse(p.allow(ctm));
    }

    @Test
    public void testParsePermissions0f() throws Exception {
        final String src = "java.lang { +Class { getName(); getSimpleName(); } }";
        final JexlPermissions p = RESTRICTED.compose(src);
        final Method getName = getMethod(java.lang.Class.class,"getName");
        assertNotNull(getName);
        assertTrue(p.allow(getName));
        assertFalse(RESTRICTED.allow(getName));
        final Method getSimpleName = getMethod(java.lang.Class.class,"getSimpleName");
        assertNotNull(getSimpleName);
        assertTrue(p.allow(getSimpleName));
        assertFalse(RESTRICTED.allow(getSimpleName));

        final Method getMethod = getMethod(java.lang.Class.class,"getMethod");
        assertNotNull(getMethod);
        assertFalse(p.allow(getMethod));

        final Method exit = getMethod(java.lang.Runtime.class,"exit");
        assertNotNull(exit);
        assertFalse(p.allow(exit));
    }

    @Test
    public void testParsePermissions0g() throws Exception {
        final String src = "java.lang { +Class {  } }";
        final JexlPermissions p = RESTRICTED.compose(src);
        final Method getName = getMethod(java.lang.Class.class,"getName");
        assertNotNull(getName);
        assertTrue(p.allow(getName));
        final Method getMethod = getMethod(java.lang.Class.class,"getMethod");
        assertNotNull(getMethod);
        assertTrue(p.allow(getMethod));

        final Method exit = getMethod(java.lang.Runtime.class,"exit");
        assertNotNull(exit);
        assertFalse(p.allow(exit));
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
        assertNotNull(nojexlmap);
        final Set<String> wildcards = p.getWildcards();
        assertEquals(4, wildcards.size());

        final JexlEngine jexl = new JexlBuilder().permissions(p).safe(false).lexical(true).create();

        final Method exit = getMethod(java.lang.Runtime.class,"exit");
        assertNotNull(exit);
        assertFalse(p.allow(exit));
        final Method exec = getMethod(java.lang.Runtime.class,"getRuntime");
        assertNotNull(exec);
        assertFalse(p.allow(exec));
        final Method callMeNot = getMethod(Outer.Inner.class, "callMeNot");
        assertNotNull(callMeNot);
        assertFalse(p.allow(callMeNot));
        JexlScript script = jexl.createScript("o.callMeNot()", "o");
        try {
            final Object result = script.execute(null, new Outer.Inner());
            fail("callMeNot should be uncallable");
        } catch (final JexlException.Method xany) {
            assertEquals("callMeNot", xany.getMethod());
        }
        final Method uncallable = getMethod(Invisible.class, "uncallable");
        assertFalse(p.allow(uncallable));
        final Package ip = Invisible.class.getPackage();
        assertFalse(p.allow(ip));
        script = jexl.createScript("o.uncallable()", "o");
        try {
            final Object result = script.execute(null, new Invisible());
            fail("uncallable should be uncallable");
        } catch (final JexlException.Method xany) {
            assertEquals("uncallable", xany.getMethod());
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
                fail(src);
            } catch (final IllegalStateException xill) {
                assertNotNull(xill);
            }
        }
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

    @Test public void testPrivateOverload1() throws Exception {
        final String src = "parseDouble(\"PHM1\".substring(3)).intValue()";
        final JexlArithmetic jexla = new I33Arithmetic(true);
        final JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(jexla).create();
        final JexlScript script = jexl.createScript(src);
        assertNotNull(script);
        final Object result = script.execute(null);
        assertEquals(1, result);
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
            fail("protectedMethod() is not public through superclass Foo2");
        } catch (final JexlException xjexl) {
            assertNotNull(xjexl);
        }
        // call public override, ok
        foo2 = new Foo3();
        r = script.execute(null, foo3);
        assertEquals("foo3",r);
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
        assertNotNull(result);
    }

    @Test
    public void testSecurePermissions() {
        assertNotNull(JexlTestCase.SECURE);
        final List<Class<?>> acs = Arrays.asList(
            java.lang.Runtime.class,
            java.math.BigDecimal.class,
            java.text.SimpleDateFormat.class,
            java.util.Map.class);
        for(final Class<?> ac: acs) {
            final Package p = ac.getPackage();
            assertNotNull(p, ac::getName);
            assertTrue(JexlTestCase.SECURE.allow(p), ac::getName);
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
            assertNotNull(p, nac::getName);
            assertFalse(JexlTestCase.SECURE.allow(p), nac::getName);
        }
    }

    @Test
    public void testWildCardPackages() {
        Set<String> wildcards;
        boolean found;
        wildcards = new HashSet<>(Arrays.asList("com.apache.*"));
        found = Permissions.wildcardAllow(wildcards, "com.apache.commons.jexl3");
        assertTrue(found);
        found = Permissions.wildcardAllow(wildcards, "com.google.spexl");
        assertFalse(found);
    }
}
