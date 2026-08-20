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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.util.Collections;

import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

/**
 * Tests for pragmas
 */
class ComposePermissionsTest extends JexlTestCase {
    static final String SAMPLE_JSON = "src/test/scripts/sample.json";

    /**
     * Create a new test case.
     */
    public ComposePermissionsTest() {
        super("PermissionsTest");
    }

    void runComposePermissions(final JexlPermissions p) throws Exception {
        final String check = "http://example.com/content.jpg";
        final File jsonFile = new File(SAMPLE_JSON);
        final Gson gson = new Gson();
        final Object json;
        try (final FileReader reader = new FileReader(jsonFile)) {
            json = gson.fromJson(reader, Object.class);
            assertNotNull(json);
        }

        // will succeed because the base permissions (UNRESTRICTED) allow the gson LinkedTreeMap
        final JexlEngine j0 = createEngine(false, p);
        final JexlScript s0 = j0.createScript("json.pageInfo.pagePic", "json");
        final Object r0 = s0.execute(null, json);
        assertEquals(check, r0);

        // will fail if gson package is denied
        JexlEngine j1 = createEngine(false, p.compose("com.google.gson.internal {}"));
        final JexlScript s1 = j1.createScript("json.pageInfo.pagePic", "json");
        JexlException.Property xproperty = assertThrows(JexlException.Property.class, () -> s1.execute(null, json));
        assertEquals("pageInfo", xproperty.getProperty());

        // will fail since gson package is denied
        j1 = createEngine(false, p.compose("com.google.gson.internal { LinkedTreeMap {} }"));
        final JexlScript s2 = j1.createScript("json.pageInfo.pagePic", "json");
        xproperty = assertThrows(JexlException.Property.class, () -> s2.execute(null, json));
        assertEquals("pageInfo", xproperty.getProperty());

        // will succeed once the gson package is explicitly allowed (no reach-through via java.util.Map)
        j1 = createEngine(false, JexlPermissions.RESTRICTED.compose("com.google.gson.internal +{}"));
        final JexlScript s3 = j1.createScript("json.pageInfo.pagePic", "json");
        final Object r3 = s3.execute(null, json);
        assertEquals(check, r3);
    }

    @Test
    void testComposePermissions() throws Exception {
        runComposePermissions(JexlPermissions.UNRESTRICTED);
    }

    @Test
    void testComposePreservesBaseDenials() throws Exception {
        // deny markers must survive the copy performed by compose(): a whole-class denial in the
        // base must still deny class and constructor visibility after composing unrelated rules
        final Constructor<?> pbCtor = ProcessBuilder.class.getConstructor(String[].class);
        assertFalse(JexlPermissions.RESTRICTED.allow(pbCtor));
        assertFalse(JexlPermissions.RESTRICTED.allow(ProcessBuilder.class));
        final JexlPermissions composed = JexlPermissions.RESTRICTED.compose("java.math +{}");
        assertFalse(composed.allow(pbCtor));
        assertFalse(composed.allow(ProcessBuilder.class));
        assertFalse(composed.allow(Runtime.class));
        // a whole-package denial in the base must survive composition as well
        final JexlPermissions pkgDeny = JexlPermissions.parse("java.lang.*", "java.net {}");
        assertFalse(pkgDeny.allow(java.net.URI.class));
        final JexlPermissions pkgDenyComposed = pkgDeny.compose("java.math +{}");
        assertFalse(pkgDenyComposed.allow(java.net.URI.class));
    }

    @Test
    void testComposePreservesBaseAllows() throws Exception {
        // the allow marker must survive the copy performed by compose(): a whole-class allowance
        // in the base must not (fail-closed) revoke constructor visibility after composition
        final Constructor<?> swCtor = java.io.StringWriter.class.getConstructor();
        assertTrue(JexlPermissions.RESTRICTED.allow(swCtor));
        assertTrue(JexlPermissions.RESTRICTED.allow(java.io.StringWriter.class));
        final JexlPermissions composed = JexlPermissions.RESTRICTED.compose("java.math +{}");
        assertTrue(composed.allow(swCtor));
        assertTrue(composed.allow(java.io.StringWriter.class));
    }

    @Test
    void testComposePermissions1() throws Exception {
        runComposePermissions(new JexlPermissions.Delegate(JexlPermissions.UNRESTRICTED) {
            @Override
            public String toString() {
                return "delegate:" + base.toString();
            }
        });
    }

    @Test
    void testComposePermissions2() throws Exception {
        runComposePermissions(new JexlPermissions.ClassPermissions(JexlPermissions.UNRESTRICTED, Collections.emptySet()));
    }
}
