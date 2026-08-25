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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

/**
 * Tests that JXLT/template parsing enforces the engine's configured features
 * instead of silently falling back to JexlEngine.DEFAULT_FEATURES.
 * <p>(Feature-lockdown bypass: JexlParser.jxltParse used to discard the features
 * argument for the shared feature controller.)</p>
 */
class JxltFeatureEnforcementTest {

    @Test
    void testTemplateScriptHonorsLoopFeature() {
        final JexlFeatures features = new JexlFeatures().loops(false);
        final JexlEngine jexl = new JexlBuilder().features(features).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        // template directives are parsed as a JEXL script; loops are disabled by the host
        assertThrows(JexlException.Feature.class,
            () -> jxlt.createTemplate("$$ while(true);\nhello"),
            "template script must honor the engine loop feature lockdown");
    }

    @Test
    void testJxltExpressionHonorsMethodCallFeature() {
        final JexlFeatures features = new JexlFeatures().methodCall(false);
        final JexlEngine jexl = new JexlBuilder().features(features).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        // createExpression wraps the parse failure into a JxltEngine.Exception
        final JxltEngine.Exception xjxlt = assertThrows(JxltEngine.Exception.class,
            () -> jxlt.createExpression("${'abc'.size()}"),
            "JXLT expression must honor the engine method-call feature lockdown");
        assertInstanceOf(JexlException.Feature.class, xjxlt.getCause());
    }

    @Test
    void testFeaturesRestoredAfterJxltParse() {
        // an engine with loops disabled
        final JexlFeatures features = new JexlFeatures().loops(false);
        final JexlEngine jexl = new JexlBuilder().features(features).create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        assertThrows(JexlException.Feature.class, () -> jxlt.createTemplate("$$ for(var i : [1,2]) {}\nx"));
        // the shared feature controller must be restored: scripts still parse and are still controlled
        assertEquals(42, jexl.createScript("40 + 2").execute(null));
        assertThrows(JexlException.Feature.class, () -> jexl.createScript("while(true);"));
    }

    @Test
    void testPermissiveTemplateStillWorks() {
        // a permissive engine keeps working as before
        final JexlEngine jexl = new JexlBuilder().create();
        final JxltEngine jxlt = jexl.createJxltEngine();
        final JxltEngine.Template t = jxlt.createTemplate("$$ for(var i : [1,2]) {\nhello ${i}\n$$ }");
        assertNotNull(t);
        final StringWriter strw = new StringWriter();
        t.evaluate(new MapContext(), strw);
        assertEquals("hello 1\nhello 2\n", strw.toString());
    }
}
