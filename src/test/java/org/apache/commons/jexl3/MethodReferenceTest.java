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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jexl3.internal.MethodReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for method references on objects
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class MethodReferenceTest extends JexlTestCase {

    public MethodReferenceTest() {
        super("MethodReferenceTest");
    }

    @Test
    public void testMethodSyntax() throws Exception {
        JexlContext context = new MapContext();
        JexlScript rm = JEXL.createScript("'string'::hashCode");
        Object o = rm.execute(context);
        Assert.assertTrue("Result is not as expected", o instanceof MethodReference);
        rm = JEXL.createScript("123::toString");
        o = rm.execute(context);
        Assert.assertTrue("Result is not as expected", o instanceof MethodReference);
    }

    @Test
    public void testStaticSyntax() throws Exception {
        JexlContext context = new MapContext();
        context.set("Integer", Integer.class);
        JexlScript rm = JEXL.createScript("Integer::hashCode");
        Object o = rm.execute(context);
        Assert.assertTrue("Result is not as expected", o instanceof MethodReference);
    }

    @Test
    public void testInstanceSyntax() throws Exception {
        JexlContext context = new MapContext();
        context.set("Integer", Integer.class);
        JexlScript rm = JEXL.createScript("Integer::floatValue");
        Object o = rm.execute(context);
        Assert.assertTrue("Result is not as expected", o instanceof MethodReference);
    }

    @Test
    public void testConstructorSyntax() throws Exception {
        JexlContext context = new MapContext();
        context.set("String", String.class);
        JexlScript rm = JEXL.createScript("String::new");
        Object o = rm.execute(context);
        Assert.assertTrue("Result is not as expected", o instanceof MethodReference);
    }

    @Test
    public void testCallable() throws Exception {
        JexlContext context = new MapContext();
        JexlScript rm = JEXL.createScript("var x = 123::shortValue; x()");
        Object o = rm.execute(context);
        Assert.assertEquals("Result is not as expected", (short) 123, o);
    }

    @Test
    public void testMethod() throws Exception {
        JexlContext context = new MapContext();
        JexlScript rm = JEXL.createScript("{'string','number'}.stream().filter('string'::equals).count()");
        Object o = rm.execute(context);
        Assert.assertEquals("Result is not as expected", 1L, o);
    }

    @Test
    public void testInstance() throws Exception {
        JexlContext context = new MapContext();
        context.set("Long", Long.class);
        JexlScript rm = JEXL.createScript("{1L,2L,3L}.stream().mapToInt(Long::intValue).sum()");
        Object o = rm.execute(context);
        Assert.assertEquals("Result is not as expected", 6, o);
    }

    @Test
    public void testStatic() throws Exception {
        JexlContext context = new MapContext();
        context.set("Integer", Integer.class);
        JexlScript rm = JEXL.createScript("{'3', '6', '8'}.stream().mapToInt(Integer::decode).sum()");
        Object o = rm.execute(context);
        Assert.assertEquals("Result is not as expected", 17, o);
    }

    @Test
    public void testConstructor() throws Exception {
        JexlContext context = new MapContext();
        context.set("Character", Character.class);
        JexlScript rm = JEXL.createScript("'abc'.chars().mapToObj(c => (char) c).map(Character::new).count()");
        Object o = rm.execute(context);
        Assert.assertEquals("Result is not as expected", 3L, o);
    }

}
