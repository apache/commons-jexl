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

import org.apache.commons.jexl3.annotations.NoJexl;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Checks the CacheMap.MethodKey implementation
 */

public class NoJexlTest {

    public static class A {
        public int i;
        public A() {}
        public int method() { return 0; }
    }

    @NoJexl
    public interface InterNoJexl0 {
        int method();
    }

    public interface InterNoJexl1 {
        @NoJexl
        int method();
    }


    public static class A0 extends A implements InterNoJexl0 {
        @NoJexl public int i0;
        @NoJexl public A0() {}
        @Override public int method() { return 1; }
    }

    public static class A1 extends A implements InterNoJexl1 {
        private int i1;
        @NoJexl public A1() {}
        @Override public int method() { return 2; }
    }

    @NoJexl
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

    @NoJexl
    public interface InterNoJexl5 {
        int method();
    }

    @Test
    public void testNoJexlPermissions() throws Exception {
        Permissions p = Permissions.UNRESTRICTED;
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

}
