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
package org.apache.commons.jexl3.jexl342;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class OptionalTest {

    public static class Thing {
        String name = null;
        public Optional<String> findName() {
            return  Optional.ofNullable(name);
        }

        public Optional<List<String>> findNames() {
            if (name == null) {
                return Optional.empty();
            }
            return Optional.of(Collections.singletonList(name));
        }
    }

    public static class StreamContext extends MapContext {
        public Stream map(Collection<Object> c, JexlScript s) {
            JexlContext context = JexlEngine.getThreadContext();
            return c.stream().map(a->s.execute(context, a));
        }
        public Object reduce(Stream<Object> stream, JexlScript script) {
            return stream.reduce((identity, element)->{
                JexlContext context = JexlEngine.getThreadContext();
                return script.execute(context, identity, element);
            });
        }
    }

    @Test
    public void testStream() {
        String src = "[1, 2, 3, ...].map(x -> x * x).reduce((acc, x)->acc + x)";
        JexlBuilder builder = new JexlBuilder();
        JexlUberspect uber = builder.create().getUberspect();
        JexlArithmetic jexla = new OptionalArithmetic(true);
        JexlEngine jexl = builder.uberspect(new ReferenceUberspect(uber)).arithmetic(jexla).safe(false).create();
        JexlInfo info = new JexlInfo("testStream", 1, 1);
        MapContext context = new StreamContext();
        JexlScript script = jexl.createScript(src, "list");
        Object result = script.execute(context, Arrays.asList(1, 2, 3));
        Assert.assertEquals(14, result);
        //Optional<?> result = (Optional<?>) script.execute(context, Arrays.asList(1, 2, 3));
        //Assert.assertEquals(14, result.get());
    }

    @Test
    public void testOptionalArgs() {
        JexlBuilder builder = new JexlBuilder();
        JexlArithmetic jexla = new OptionalArithmetic(true);
        JexlUberspect uber = builder.create().getUberspect();
        JexlEngine jexl = builder.uberspect(new ReferenceUberspect(uber)).arithmetic(jexla).safe(false).create();
        JexlInfo info = new JexlInfo("testStream", 1, 1);
        MapContext context = new StreamContext();
        String src = "x + x";
        JexlScript script = jexl.createScript(src, "x");
        Optional<Integer> x = Optional.of(21);
        Object result = script.execute(context, x);
        Assert.assertEquals(42, result);
    }

    @Test
    public void test342() {
        JexlBuilder builder = new JexlBuilder();
        JexlUberspect uber = builder.create().getUberspect();
        JexlEngine jexl = builder.uberspect(new ReferenceUberspect(uber)).safe(false).create();
        JexlInfo info = new JexlInfo("test352", 1, 1);
        Thing thing = new Thing();
        JexlScript script;

        script = jexl.createScript(info.at(53, 1),"thing.name.length()", "thing");
        Object result = script.execute(null, thing);
        Assert.assertNull(result);

        thing.name = "foo";
        result = script.execute(null, thing);
        Assert.assertEquals(3, result);

        try {
            script = jexl.createScript(info.at(62, 1), "thing.name.size()", "thing");
            result = script.execute(null, thing);
            Assert.fail("should have thrown");
        } catch(JexlException.Method xmethod) {
            Assert.assertEquals("size", xmethod.getDetail());
            Assert.assertEquals("test352@62:11 unsolvable function/method 'size'", xmethod.getMessage());
        }

        try {
            script = jexl.createScript(info.at(71, 1), "thing.name?.size()", "thing");
            result = script.execute(null, thing);
        } catch(JexlException.Method xmethod) {
            Assert.fail("should not have thrown");
        }

        thing.name = null;
        script = jexl.createScript(info,"thing.names.size()", "thing");
        result = script.execute(null, thing);
        Assert.assertNull(result);
        thing.name = "froboz";
        script = jexl.createScript(info,"thing.names", "thing");
        result = script.execute(null, thing);
        Assert.assertNotNull(result);
        script = jexl.createScript(info,"thing.names.size()", "thing");
        result = script.execute(null, thing);
        Assert.assertEquals(1, result);
    }
}
