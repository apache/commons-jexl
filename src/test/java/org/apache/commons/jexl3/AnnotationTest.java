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

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for annotations.
 * @since 3.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})

public class AnnotationTest extends JexlTestCase {

    public AnnotationTest() {
        super("AnnotationTest");
    }

    @Test
    public void test197a() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("@synchronized { return 42; }");
        Object r = e.execute(jc);
        Assert.assertEquals(42, r);
    }

    public static class AnnotationContext extends MapContext implements JexlContext.AnnotationProcessor {
        private int count = 0;
        private final Set<String> names = new TreeSet<String>();

        @Override
        public Object processAnnotation(String name, Object[] args, Callable<Object> statement) throws Exception {
            count += 1;
            names.add(name);
            if ("one".equals(name)) {
                names.add(args[0].toString());
            } else if ("two".equals(name)) {
                names.add(args[0].toString());
                names.add(args[1].toString());
            } else if ("error".equals(name)) {
                names.add(args[0].toString());
                throw new IllegalArgumentException(args[0].toString());
            } else if ("unknown".equals(name)) {
                return null;
            }
            return statement.call();
        }

        public int getCount() {
            return count;
        }

        public Set<String> getNames() {
            return names;
        }
    }

    @Test
    public void testNoArg() throws Exception {
        AnnotationContext jc = new AnnotationContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("@synchronized { return 42; }");
        Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("synchronized"));
    }

    @Test
    public void testNoArgExpression() throws Exception {
        AnnotationContext jc = new AnnotationContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("@synchronized 42");
        Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("synchronized"));
    }


    @Test
    public void testOneArg() throws Exception {
        AnnotationContext jc = new AnnotationContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("@one(1) { return 42; }");
        Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("one"));
        Assert.assertTrue(jc.getNames().contains("1"));
    }

    @Test
    public void testMultiple() throws Exception {
        AnnotationContext jc = new AnnotationContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("@one(1) @synchronized { return 42; }");
        Object r = e.execute(jc);
        Assert.assertEquals(42, r);
        Assert.assertEquals(2, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("synchronized"));
        Assert.assertTrue(jc.getNames().contains("one"));
        Assert.assertTrue(jc.getNames().contains("1"));
    }

    @Test
    public void testError() throws Exception {
        AnnotationContext jc = new AnnotationContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("@error('42') { return 42; }");
        try {
            Object r = e.execute(jc);
            Assert.fail("should have failed");
        } catch (JexlException.Annotation xjexl) {
            Assert.assertEquals("error", xjexl.getAnnotation());
        }
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("error"));
        Assert.assertTrue(jc.getNames().contains("42"));
    }

    @Test
    public void testUnknown() throws Exception {
        AnnotationContext jc = new AnnotationContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("@unknown('42') { return 42; }");
        try {
            Object r = e.execute(jc);
            Assert.fail("should have failed");
        } catch (JexlException.Annotation xjexl) {
            Assert.assertEquals("unknown", xjexl.getAnnotation());
        }
        Assert.assertEquals(1, jc.getCount());
        Assert.assertTrue(jc.getNames().contains("unknown"));
        Assert.assertFalse(jc.getNames().contains("42"));
    }
}
