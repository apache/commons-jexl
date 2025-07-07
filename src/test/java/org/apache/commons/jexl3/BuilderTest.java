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
package org.apache.commons.jexl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.jexl3.internal.introspection.SandboxUberspect;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.junit.jupiter.api.Test;

/**
 * Checking the builder basics.
 */
class BuilderTest {
    private static JexlBuilder builder() {
        return new JexlBuilder();
    }

    @Test
    void testFlags() {
        assertTrue(builder().antish(true).antish());
        assertFalse(builder().antish(false).antish());
        assertTrue(builder().cancellable(true).cancellable());
        assertFalse(builder().cancellable(false).cancellable());
        assertTrue(builder().safe(true).safe());
        assertFalse(builder().safe(false).safe());
        assertTrue(builder().silent(true).silent());
        assertFalse(builder().silent(false).silent());
        assertTrue(builder().lexical(true).lexical());
        assertFalse(builder().lexical(false).lexical());
        assertTrue(builder().lexicalShade(true).lexicalShade());
        assertFalse(builder().lexicalShade(false).lexicalShade());
        assertTrue(builder().silent(true).silent());
        assertFalse(builder().silent(false).silent());
        assertTrue(builder().strict(true).strict());
        assertFalse(builder().strict(false).strict());
        assertTrue(builder().booleanLogical(true).options().isBooleanLogical());
        assertFalse(builder().booleanLogical(false).options().isBooleanLogical());
        assertTrue(builder().strictInterpolation(true).options().isStrictInterpolation());
        assertFalse(builder().strictInterpolation(false).options().isStrictInterpolation());
    }

    @Test
    void testOther() {
        final ClassLoader cls = getClass().getClassLoader().getParent();
        assertEquals(cls, builder().loader(cls).loader());
        final Charset cs = StandardCharsets.UTF_16;
        assertEquals(cs, builder().charset(cs).charset());
        assertEquals(cs, builder().loader(cs).charset());
        final JexlUberspect u0 = builder().create().getUberspect();
        final JexlSandbox sandbox = new JexlSandbox();
        final JexlUberspect uberspect = new SandboxUberspect(u0, sandbox);
        assertEquals(sandbox, builder().sandbox(sandbox).sandbox());
        assertEquals(uberspect, builder().uberspect(uberspect).uberspect());
    }

    @Test
    void testValues() {
        assertEquals(1, builder().collectMode(1).collectMode());
        assertEquals(0, builder().collectMode(0).collectMode());
        assertEquals(32, builder().cacheThreshold(32).cacheThreshold());
        assertEquals(8, builder().stackOverflow(8).stackOverflow());
    }
}
