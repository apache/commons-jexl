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

import java.math.MathContext;
import java.nio.charset.Charset;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for pragmas
 */
public class PragmaTest extends JexlTestCase {
    /**
     * Create a new test case.
     */
    public PragmaTest() {
        super("PragmaTest");
    }

    /**
     * Test creating a script from a string.
     */
    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testPragmas() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript script = JEXL.createScript("#pragma one 1\n#pragma the.very.hard 'truth'\n2;");
        Assert.assertTrue(script != null);
        Map<String, Object> pragmas = script.getPragmas();
        Assert.assertEquals(2, pragmas.size());
        Assert.assertEquals(1, pragmas.get("one"));
        Assert.assertEquals("truth", pragmas.get("the.very.hard"));
    }

    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testJxltPragmas() throws Exception {
        JexlContext jc = new MapContext();
        JxltEngine engine = new JexlBuilder().create().createJxltEngine();
        JxltEngine.Template tscript = engine.createTemplate("$$ #pragma one 1\n$$ #pragma the.very.hard 'truth'\n2;");
        Assert.assertTrue(tscript != null);
        Map<String, Object> pragmas = tscript.getPragmas();
        Assert.assertEquals(2, pragmas.size());
        Assert.assertEquals(1, pragmas.get("one"));
        Assert.assertEquals("truth", pragmas.get("the.very.hard"));
    }
    
    public static class SafeContext extends JexlEvalContext {
        // @Override
        public void processPragmas(Map<String, Object> pragmas) {
            if (pragmas != null && !pragmas.isEmpty()) {
                JexlOptions options = getEngineOptions();
                for (Map.Entry<String, Object> pragma : pragmas.entrySet()) {
                    String key = pragma.getKey();
                    Object value = pragma.getValue();
                    if ("jexl.safe".equals(key) && value instanceof Boolean) {
                        options.setSafe(((Boolean) value).booleanValue());
                    } else if ("jexl.strict".equals(key) && value instanceof Boolean) {
                        options.setStrict(((Boolean) value).booleanValue());
                    } else if ("jexl.silent".equals(key) && value instanceof Boolean) {
                        options.setSilent(((Boolean) value).booleanValue());
                    }
                }
            }
        }
    }

    @Test
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    public void testSafePragma() throws Exception {
        SafeContext jc = new SafeContext();
        jc.set("foo", null);
        JexlScript script = JEXL.createScript("#pragma jexl.safe true\nfoo.bar;");
        Assert.assertTrue(script != null);
        jc.processPragmas(script.getPragmas());
        Object result = script.execute(jc);
        Assert.assertNull(result);
        jc = new SafeContext();
        jc.set("foo", null);
        try {
            result = script.execute(jc);
            Assert.fail("should have thrown");
        } catch (JexlException xvar) {
            // ok, expected
        }
    }
}
