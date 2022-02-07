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

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.JexlTestCase;
import org.apache.commons.jexl3.internal.introspection.nojexlpackage.Invisible;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
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

    @Test
    public void testPermissions() throws Exception {
        String src = " org.apache.commons.jexl3.internal.introspection { PermissionsTest { "+
                "InterNoJexl0 { } "+
                "InterNoJexl1 { method(); } "+
                "A0 { A0(); i0; } "+
                "A1 { A1(); } "+
                "A2 { } "+
                "InterNoJexl5 { } "+
                "} }";

        JexlPermissions p = (Permissions) JexlPermissions.parse(src);
        Assert.assertFalse(p.allow((Field) null));
        Assert.assertFalse(p.allow((Package) null));
        Assert.assertFalse(p.allow((Method) null));
        Assert.assertFalse(p.allow((Constructor<?>) null));
        Assert.assertFalse(p.allow((Class<?>) null));

        Assert.assertFalse(p.allow(A2.class));
        Assert.assertTrue(p.allow(A3.class));
        Assert.assertTrue(p.allow(A5.class));

        Method mA = A.class.getMethod("method");
        Assert.assertNotNull(mA);
        Method mA0 = A0.class.getMethod("method");
        Assert.assertNotNull(mA0);
        Method mA1 = A1.class.getMethod("method");
        Assert.assertNotNull(mA1);
        Method mA2 = A2.class.getMethod("method");
        Assert.assertNotNull(mA2);
        Method mA3 = A2.class.getDeclaredMethod("method");
        Assert.assertNotNull(mA3);

        Assert.assertTrue(p.allow(mA));
        Assert.assertFalse(p.allow(mA0));
        Assert.assertFalse(p.allow(mA1));
        Assert.assertFalse(p.allow(mA2));
        Assert.assertFalse(p.allow(mA3));

        Field fA = A.class.getField("i");
        Assert.assertNotNull(fA);
        Assert.assertTrue(p.allow(fA));

        Field fA0 = A0.class.getField("i0");
        Assert.assertNotNull(fA0);
        Assert.assertFalse(p.allow(fA0));
        Field fA1 = A1.class.getDeclaredField("i1");
        Assert.assertNotNull(fA1);
        Assert.assertFalse(p.allow(fA0));

        Constructor<?> cA = A.class.getConstructor();
        Assert.assertNotNull(cA);
        Assert.assertTrue(p.allow(cA));

        Constructor<?> cA0 = A0.class.getConstructor();
        Assert.assertNotNull(cA0);
        Assert.assertFalse(p.allow(cA0));

        Constructor<?> cA3 = A3.class.getDeclaredConstructor();
        Assert.assertNotNull(cA3);
        Assert.assertFalse(p.allow(cA3));
    }

    static Method getMethod(Class<?> clazz, String method) {
        return Arrays.stream(clazz.getMethods()).filter(mth->mth.getName().equals(method)).findFirst().get();
    }

    @Test
    public void testParsePermissions0() throws Exception {
        String src = "java.lang { Runtime { exit(); exec(); } }";
        Permissions p = (Permissions) JexlPermissions.parse(src);
        Map<String, Permissions.NoJexlPackage> nojexlmap = p.getPackages();
        Assert.assertNotNull(nojexlmap);
        Permissions.NoJexlPackage njp = nojexlmap.get("java.lang");
        Assert.assertNotNull(njp);
        Method exit = getMethod(java.lang.Runtime.class,"exit");
        Assert.assertNotNull(exit);
        Assert.assertFalse(p.allow(exit));
        Method exec = getMethod(java.lang.Runtime.class,"exec");
        Assert.assertNotNull(exec);
        Assert.assertFalse(p.allow(exec));
    }

    public static class Outer {
        public static class Inner {
            public void callMeNot() {}
        }
    }

    @Test
    public void testParsePermissions1() {
        String[] src = new String[]{
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
        Permissions p = (Permissions) JexlPermissions.parse(src);
        Map<String, Permissions.NoJexlPackage> nojexlmap = p.getPackages();
        Assert.assertNotNull(nojexlmap);
        Set<String> wildcards = p.getWildcards();
        Assert.assertEquals(4, wildcards.size());

        JexlEngine jexl = new JexlBuilder().permissions(p).safe(false).lexical(true).create();

        Method exit = getMethod(java.lang.Runtime.class,"exit");
        Assert.assertNotNull(exit);
        Assert.assertFalse(p.allow(exit));
        Method exec = getMethod(java.lang.Runtime.class,"getRuntime");
        Assert.assertNotNull(exec);
        Assert.assertFalse(p.allow(exec));
        Method callMeNot = getMethod(Outer.Inner.class, "callMeNot");
        Assert.assertNotNull(callMeNot);
        Assert.assertFalse(p.allow(callMeNot));
        JexlScript script = jexl.createScript("o.callMeNot()", "o");
        try {
            Object result = script.execute(null, new Outer.Inner());
            Assert.fail("callMeNot should be uncallable");
        } catch(JexlException.Method xany) {
            Assert.assertEquals("callMeNot", xany.getMethod());
        }
        Method uncallable = getMethod(Invisible.class, "uncallable");
        Assert.assertFalse(p.allow(uncallable));
        Package ip = Invisible.class.getPackage();
        Assert.assertFalse(p.allow(ip));
        script = jexl.createScript("o.uncallable()", "o");
        try {
            Object result = script.execute(null, new Invisible());
            Assert.fail("uncallable should be uncallable");
        } catch(JexlException.Method xany) {
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
        List<Class<?>> acs = Arrays.asList(
            java.lang.Runtime.class,
            java.math.BigDecimal.class,
            java.text.SimpleDateFormat.class,
            java.util.Map.class);
        for(Class<?> ac: acs) {
            Package p = ac.getPackage();
            Assert.assertNotNull(ac.getName(), p);
            Assert.assertTrue(ac.getName(), JexlTestCase.SECURE.allow(p));
        }
        List<Class<?>> nacs = Arrays.asList(
                java.lang.annotation.ElementType.class,
                java.lang.instrument.ClassDefinition.class,
                java.lang.invoke.CallSite.class,
                java.lang.management.BufferPoolMXBean.class,
                java.lang.ref.SoftReference.class,
                java.lang.reflect.Method.class);
        for(Class<?> nac : nacs) {
            Package p = nac.getPackage();
            Assert.assertNotNull(nac.getName(), p);
            Assert.assertFalse(nac.getName(), JexlTestCase.SECURE.allow(p));
        }
    }


    @Test
    public void testParsePermissionsFailures() {
        String[] srcs = new String[]{
                "java.lang.*.*",
                "java.math.*.",
                "java.text.*;",
                "java.lang {{ Runtime {} }",
                "java.rmi {}}",
                "java.io { Text File {} }",
                "java.io { File { m.x } }"
        };
        for(String src : srcs) {
            try {
                Permissions p = (Permissions) JexlPermissions.parse(src);
                Assert.fail(src);
            } catch(IllegalStateException xill) {
                Assert.assertNotNull(xill);
            }
        }
    }
}
