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

import static org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED;
import static org.apache.commons.jexl3.introspection.JexlPermissions.SECURE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.stream.BaseStream;

import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.junit.jupiter.api.Test;

/**
 * Checks the RESTRICTED (and SECURE) deny-list closes the containment gaps around
 * process handles, module/resource loading, JVM-global mutators, uninterruptible
 * blockers and off-heap/common-pool resources.
 */
class PermissionsRestrictedSweepTest {

    private static Method getMethod(final Class<?> clazz, final String name) {
        for (final Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new AssertionError("Missing method: " + clazz.getName() + "#" + name);
    }

    /** The build targets Java 8: Java 9+ classes must be referenced reflectively. */
    private static Class<?> forName(final String name) {
        try {
            return Class.forName(name);
        } catch (final ClassNotFoundException xnf) {
            return null;
        }
    }

    @Test
    void testProcessHandleDenied() {
        // ProcessHandle can enumerate, inspect and destroy same-user OS processes (Java 9+)
        final Class<?> processHandle = forName("java.lang.ProcessHandle");
        assumeTrue(processHandle != null);
        final Class<?> processHandleInfo = forName("java.lang.ProcessHandle$Info");
        assertFalse(RESTRICTED.allow(processHandle));
        assertFalse(RESTRICTED.allow(processHandleInfo));
        assertFalse(RESTRICTED.allow(getMethod(processHandle, "current")));
        assertFalse(RESTRICTED.allow(getMethod(processHandle, "allProcesses")));
        assertFalse(RESTRICTED.allow(getMethod(processHandle, "destroyForcibly")));
        final JexlUberspect uber = new Uberspect(null, null, RESTRICTED);
        assertNull(uber.getClassByName("java.lang.ProcessHandle"));
    }

    @Test
    void testWholeClassDenialCoversNestedClasses() {
        // -X{} must deny X$Nested as well: the classKey nests as Outer$Inner and the deny
        // walk inherits a whole-class denial from enclosing classes
        final Class<?> loggerFinder = forName("java.lang.System$LoggerFinder");
        if (loggerFinder != null) {
            assertFalse(RESTRICTED.allow(loggerFinder));
        }
        assertFalse(RESTRICTED.allow(ProcessBuilder.Redirect.class));
        assertFalse(RESTRICTED.allow(Thread.State.class));
        // control: nested classes of non-denied classes stay visible
        assertTrue(RESTRICTED.allow(java.util.Map.Entry.class));
    }

    @Test
    void testModuleAndResourceLoadingDenied() {
        // Module/ModuleLayer supply loader-free resource reading and class loading (Java 9+)
        final Class<?> module = forName("java.lang.Module");
        assumeTrue(module != null);
        assertFalse(RESTRICTED.allow(module));
        assertFalse(RESTRICTED.allow(forName("java.lang.ModuleLayer")));
        assertFalse(RESTRICTED.allow(getMethod(module, "getResourceAsStream")));
        // ResourceBundle.Control.newBundle re-opens property-file reading and class loading
        assertFalse(RESTRICTED.allow(ResourceBundle.Control.class));
        assertFalse(RESTRICTED.allow(getMethod(ResourceBundle.Control.class, "newBundle")));
        // SECURE parity: RESTRICTED denies ListResourceBundle too
        assertFalse(RESTRICTED.allow(ListResourceBundle.class));
        // control: ResourceBundle class itself stays visible, only its loader members are denied
        assertFalse(RESTRICTED.allow(getMethod(ResourceBundle.class, "getBundle")));
        assertTrue(RESTRICTED.allow(getMethod(ResourceBundle.class, "getString")));
    }

    @Test
    void testGlobalMutatorsDenied() {
        // Locale.setDefault/TimeZone.setDefault flip JVM-process-global defaults
        for (final org.apache.commons.jexl3.introspection.JexlPermissions p
                : new org.apache.commons.jexl3.introspection.JexlPermissions[] { RESTRICTED, SECURE }) {
            assertFalse(p.allow(getMethod(Locale.class, "setDefault")));
            assertFalse(p.allow(getMethod(TimeZone.class, "setDefault")));
        }
        // controls: the read-only side stays visible
        assertTrue(RESTRICTED.allow(getMethod(Locale.class, "getDefault")));
        assertTrue(RESTRICTED.allow(getMethod(TimeZone.class, "getDefault")));
    }

    @Test
    void testUninterruptibleBlockersDenied() {
        // interrupt-immune waits defeat the documented cancellation mitigation
        assertFalse(RESTRICTED.allow(getMethod(CompletableFuture.class, "join")));
        assertFalse(RESTRICTED.allow(getMethod(Semaphore.class, "acquireUninterruptibly")));
        assertFalse(RESTRICTED.allow(getMethod(Phaser.class, "awaitAdvance")));
        assertFalse(RESTRICTED.allow(getMethod(Phaser.class, "arriveAndAwaitAdvance")));
        // controls: the interruptible counterparts stay visible
        assertTrue(RESTRICTED.allow(getMethod(CompletableFuture.class, "get")));
        assertTrue(RESTRICTED.allow(getMethod(Semaphore.class, "acquire")));
        assertTrue(RESTRICTED.allow(getMethod(Phaser.class, "awaitAdvanceInterruptibly")));
        // Timer starts a non-daemon thread that outlives the evaluation
        assertFalse(RESTRICTED.allow(Timer.class));
        assertFalse(RESTRICTED.allow(TimerTask.class));
    }

    @Test
    void testResourceBurnDenied() {
        // common fork-join pool burn and off-heap allocation
        assertFalse(RESTRICTED.allow(getMethod(BaseStream.class, "parallel")));
        assertFalse(RESTRICTED.allow(getMethod(Collection.class, "parallelStream")));
        assertFalse(RESTRICTED.allow(getMethod(ByteBuffer.class, "allocateDirect")));
        // controls: sequential streaming and heap allocation stay visible
        assertTrue(RESTRICTED.allow(getMethod(BaseStream.class, "sequential")));
        assertTrue(RESTRICTED.allow(getMethod(Collection.class, "stream")));
        assertTrue(RESTRICTED.allow(getMethod(ByteBuffer.class, "allocate")));
        final Method spliterator = getMethod(Collection.class, "spliterator");
        assertNotNull(spliterator);
        assertTrue(RESTRICTED.allow(spliterator));
    }
}
