/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal.introspection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.jexl3.annotations.NoJexl;
import org.junit.jupiter.api.Test;

/**
 * Checks the CacheMap.MethodKey implementation
 */

class NoJexlTest {

    public static class A {
        public int i;
        public A() {}
        public int method() { return 0; }
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
    public interface InterNoJexl0 {
        int method();
    }

    public interface InterNoJexl1 {
        @NoJexl
        int method();
    }

    @NoJexl
    public interface InterNoJexl5 {
        int method();
    }

    @Test
    void testNoJexlPermissions() throws Exception {
        final Permissions p = Permissions.UNRESTRICTED;
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

}
