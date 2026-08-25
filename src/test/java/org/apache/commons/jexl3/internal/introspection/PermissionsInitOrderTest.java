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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

/**
 * Regression test for the class-initialization cycle between {@code Permissions} and
 * {@link org.apache.commons.jexl3.introspection.JexlPermissions}.
 * <p>{@code Permissions implements JexlPermissions}, and {@code JexlPermissions} carries default
 * methods, so initializing {@code Permissions} first forces {@code JexlPermissions.<clinit>} to run
 * while {@code Permissions} is only partially initialized (JLS 12.4.2). {@code JexlPermissions.<clinit>}
 * computes {@code RESTRICTED}/{@code SECURE} through {@code PermissionsParser}, which reads the
 * allow/deny marker singletons. If those markers (or the {@code UNRESTRICTED} singleton) were still
 * {@code null} at that point, initialization threw an NPE.</p>
 * <p>Because class initialization is a once-per-loader event, this test must run each ordering in a
 * fresh child-first class loader that re-defines every {@code org.apache.commons.jexl3.*} class, so
 * that touching {@code Permissions} really does trigger initialization from scratch.</p>
 */
class PermissionsInitOrderTest {
    /** A child-first loader that re-defines all jexl3 classes so their {@code <clinit>} runs afresh. */
    private static final class FreshLoader extends ClassLoader {
        FreshLoader() {
            super(FreshLoader.class.getClassLoader());
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            final String path = name.replace('.', '/') + ".class";
            try (InputStream in = getParent().getResourceAsStream(path)) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) >= 0) {
                    bos.write(buf, 0, r);
                }
                final byte[] b = bos.toByteArray();
                return defineClass(name, b, 0, b.length);
            } catch (final IOException ex) {
                throw new ClassNotFoundException(name, ex);
            }
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("org.apache.commons.jexl3.")) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        c = findClass(name);
                    }
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                }
            }
            return super.loadClass(name, resolve);
        }
    }

    /**
     * Initializes the classes in the given order within a fresh loader and asserts that the
     * public {@code JexlPermissions} singletons come out fully initialized and functional.
     *
     * @param first fully-qualified name of the class to initialize first
     * @throws Exception if any reflective access fails (an init NPE surfaces as ExceptionInInitializerError)
     */
    private void assertSingletonsUsable(final String first) throws Exception {
        final ClassLoader cl = new FreshLoader();
        // touching 'first' with initialize=true triggers its <clinit> before anything else
        Class.forName(first, true, cl);
        final Class<?> iface = Class.forName("org.apache.commons.jexl3.introspection.JexlPermissions", true, cl);
        for (final String name : new String[] {"UNRESTRICTED", "RESTRICTED", "SECURE", "NONE"}) {
            final Object singleton = iface.getField(name).get(null);
            assertNotNull(singleton, name + " singleton must be initialized");
        }
        // RESTRICTED must actually be usable: deny ProcessBuilder, allow StringWriter
        final Object restricted = iface.getField("RESTRICTED").get(null);
        final boolean denyPB = (Boolean) iface.getMethod("allow", Class.class)
            .invoke(restricted, ProcessBuilder.class);
        final boolean allowSW = (Boolean) iface.getMethod("allow", Class.class)
            .invoke(restricted, java.io.StringWriter.class);
        assertTrue(!denyPB, "RESTRICTED must deny ProcessBuilder regardless of init order");
        assertTrue(allowSW, "RESTRICTED must allow StringWriter regardless of init order");
    }

    @Test
    void testInitJexlPermissionsFirst() throws Exception {
        assertSingletonsUsable("org.apache.commons.jexl3.introspection.JexlPermissions");
    }

    @Test
    void testInitPermissionsFirst() throws Exception {
        // the problematic order: Permissions before JexlPermissions
        assertSingletonsUsable("org.apache.commons.jexl3.internal.introspection.Permissions");
    }

    @Test
    void testInitPermissionsParserFirst() throws Exception {
        assertSingletonsUsable("org.apache.commons.jexl3.internal.introspection.PermissionsParser");
    }
}
