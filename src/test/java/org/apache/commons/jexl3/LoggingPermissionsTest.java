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

import java.util.List;

import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JexlPermissions#logging(org.apache.commons.logging.Log)} and the
 * {@code LoggingPermissions} wrapper: it must log what it allows/denies, but each decision only once.
 */
class LoggingPermissionsTest {

    @Test
    void testLoggingPermissionsCaptureAndDedup() throws Exception {
        final CaptureLog log = new CaptureLog();
        final JexlPermissions perms = JexlPermissions.RESTRICTED.logging(log);
        // call each element twice to exercise the once-only logging
        for (int i = 0; i < 2; i++) {
            assertTrue(perms.allow(String.class));                     // allowed class
            assertFalse(perms.allow(java.lang.Runtime.class));         // denied class
            assertTrue(perms.allow(String.class.getMethod("length"))); // allowed method
        }
        final List<String> msgs = log.getCapturedMessages();
        // Each distinct line is logged once despite two rounds (otherwise there would be 8 records).
        // allow(Method) delegates through allow(Class, Method), so a single method check yields two
        // distinct lines: the method-in-class form and the plain method form.
        assertEquals(4, msgs.size(), msgs::toString);
        assertEquals(4, log.count("info"));
        assertTrue(msgs.contains("Class java.lang.String is allowed"));
        assertTrue(msgs.contains("Class java.lang.Runtime is denied"));
        assertTrue(msgs.contains("Method java.lang.String.length() is allowed"));
        assertTrue(msgs.contains("Method java.lang.String.length() is allowed for class java.lang.String"));
    }
}
