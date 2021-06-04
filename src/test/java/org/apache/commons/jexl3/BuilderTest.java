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

import org.apache.commons.jexl3.internal.introspection.SandboxUberspect;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;

/**
 * Checking the builder basics.
 */
public class BuilderTest {
    private static JexlBuilder builder() {
        return new JexlBuilder();
    }

    @Test
    public void testFlags() {
        Assert.assertTrue(builder().antish(true).antish());
        Assert.assertFalse(builder().antish(false).antish());
        Assert.assertTrue(builder().cancellable(true).cancellable());
        Assert.assertFalse(builder().cancellable(false).cancellable());
        Assert.assertTrue(builder().safe(true).safe());
        Assert.assertFalse(builder().safe(false).safe());
        Assert.assertTrue(builder().silent(true).silent());
        Assert.assertFalse(builder().silent(false).silent());
        Assert.assertTrue(builder().lexical(true).lexical());
        Assert.assertFalse(builder().lexical(false).lexical());
        Assert.assertTrue(builder().lexicalShade(true).lexicalShade());
        Assert.assertFalse(builder().lexicalShade(false).lexicalShade());
        Assert.assertTrue(builder().silent(true).silent());
        Assert.assertFalse(builder().silent(false).silent());
        Assert.assertTrue(builder().strict(true).strict());
        Assert.assertFalse(builder().strict(false).strict());
    }

    @Test
    public void testValues() {
        Assert.assertEquals(1, builder().collectMode(1).collectMode());
        Assert.assertEquals(0, builder().collectMode(0).collectMode());
        Assert.assertEquals(32, builder().cacheThreshold(32).cacheThreshold());
        Assert.assertEquals(8, builder().stackOverflow(8).stackOverflow());
    }

    @Test
    public void testOther() {
        ClassLoader cls = getClass().getClassLoader().getParent();
        Assert.assertEquals(cls, builder().loader(cls).loader());
        Charset cs = Charset.forName("UTF16");
        Assert.assertEquals(cs, builder().charset(cs).charset());
        Assert.assertEquals(cs, builder().loader(cs).charset());
        JexlUberspect u0 = builder().create().getUberspect();
        JexlSandbox sandbox = new JexlSandbox();
        JexlUberspect uberspect = new SandboxUberspect(u0, sandbox);
        Assert.assertEquals(sandbox, builder().sandbox(sandbox).sandbox());
        Assert.assertEquals(uberspect, builder().uberspect(uberspect).uberspect());
    }
}
