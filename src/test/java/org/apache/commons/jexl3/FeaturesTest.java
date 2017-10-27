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

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for blocks
 * @since 1.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class FeaturesTest extends JexlTestCase {

    /**
     * Create the test
     */
    public FeaturesTest() {
        super("BlockTest");
    }

    private void checkFeature(JexlEngine jexl, String[] scripts) throws Exception {
        for (String script : scripts) {
            JexlScript ctl = JEXL.createScript(script);
            Assert.assertNotNull(ctl);
            try {
                JexlScript e = jexl.createScript(script);
                Assert.fail("should fail parse: " + script);
            } catch (JexlException.Feature xfeature) {
                String msg = xfeature.getMessage();
                Assert.assertNotNull(msg);
            }
        }
    }

    @Test
    public void testNoLoop() throws Exception {
        JexlFeatures f = new JexlFeatures().loops(false);
        JexlEngine jexl = new JexlBuilder().features(f).create();
        String[] scripts = new String[]{
            "while(true);",
            "for(var i : {0 .. 10}) { bar(i); }"
        };
        checkFeature(jexl, scripts);
    }

    @Test
    public void testNoLambda() throws Exception {
        JexlFeatures f = new JexlFeatures().lambda(false);
        JexlEngine jexl = new JexlBuilder().features(f).create();

        String[] scripts = new String[]{
            "var x  = ()->{ return 0 };",
            "()->{ return 0 };",
            "(x, y)->{ return 0 };",
            "function() { return 0 };",
            "function(x, y) { return 0 };",
            "if (false) { (function(x, y) { return x + y })(3, 4) }"
        };
        checkFeature(jexl, scripts);
    }

    @Test
    public void testNoNew() throws Exception {
        JexlFeatures f = new JexlFeatures().newInstance(false);
        JexlEngine jexl = new JexlBuilder().features(f).create();

        String[] scripts = new String[]{
            "return new(clazz);",
            "new('java.math.BigDecimal', 12) + 1"
        };
        checkFeature(jexl, scripts);
    }

    @Test
    public void testNoSideEffects() throws Exception {
        JexlFeatures f = new JexlFeatures().sideEffects(false);
        JexlEngine jexl = new JexlBuilder().features(f).create();

        String[] scripts = new String[]{
            "x = 1",
            "x.y = 1",
            "x().y = 1",
            "x += 1",
            "x.y += 1",
            "x().y += 1",
            "x -= 1",
            "x *= 1",
            "x /= 1",
            "x ^= 1",
            "x &= 1",
            "x |= 1",
        };
        checkFeature(jexl, scripts);
    }

    @Test
    public void testNoSideEffectsGlobal() throws Exception {
        JexlFeatures f = new JexlFeatures().sideEffectsGlobal(false);
        JexlEngine jexl = new JexlBuilder().features(f).create();

        String[] scripts = new String[]{
            "x = 1",
            "x.y = 1",
            "x().y = 1",
            "x += 1",
            "x.y += 1",
            "x().y += 1",
            "x -= 1",
            "x *= 1",
            "x /= 1",
            "x ^= 1",
            "x &= 1",
            "x |= 1",
            "4 + (x.y = 1)",
            "if (true) x.y.z = 4"
        };
        checkFeature(jexl, scripts);
        // same ones with x as local should work
        for(String str : scripts) {
            try {
                JexlScript e = jexl.createScript("var x = foo(); " + str);
            } catch (JexlException.Feature xfeature) {
                Assert.fail(str + " :: should not fail parse: " + xfeature.getMessage());
            }
        }
    }

    @Test
    public void testNoLocals() throws Exception {
        JexlFeatures f = new JexlFeatures().locals(false);
        JexlEngine jexl = new JexlBuilder().features(f).create();

        String[] scripts = new String[]{
            "var x = 0;",
            "(x)->{ x }"
        };
        checkFeature(jexl, scripts);
    }

    @Test
    public void testReservedVars() throws Exception {
        JexlFeatures f = new JexlFeatures().reservedNames(Arrays.asList("foo", "bar"));
        JexlEngine jexl = new JexlBuilder().features(f).create();

        String[] scripts = new String[]{
            "var foo = 0;",
            "(bar)->{ bar }"
        };
        checkFeature(jexl, scripts);
    }

    @Test
    public void testMixedFeatures() throws Exception {
        // no new, no local, no lambda, no loops, no-side effects
        JexlFeatures f = new JexlFeatures()
                .newInstance(false)
                .locals(false)
                .lambda(false)
                .loops(false)
                .sideEffectsGlobal(false);
        JexlEngine jexl = new JexlBuilder().features(f).create();
        String[] scripts = new String[]{
            "return new(clazz);",
            "()->{ return 0 };",
            "var x = 0;",
            "(x, y)->{ return 0 };",
            "for(var i : {0 .. 10}) { bar(i); }",
            "x += 1",
            "x.y += 1"
        };
        checkFeature(jexl, scripts);
    }

}
