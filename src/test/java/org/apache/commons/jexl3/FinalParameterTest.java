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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests final parameters.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class FinalParameterTest extends JexlTestCase {

    public FinalParameterTest() {
        super("FinalParameterTest");
    }

    @Test
    public void testBasic() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(final var a) {a}; x(42)");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not 42", 42, o);
    }

    @Test
    public void testMixed() throws Exception {
        JexlScript e = JEXL.createScript("var x = function(final var a, var b) {b = 0; a+b}; x(42,1)");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not 42", 42, o);
    }

    @Test
    public void testOverwritten() throws Exception {
        try {
           JexlScript e = JEXL.createScript("var x = function(final var a) {a = 0}");
           Assert.fail("Should have failed");
        } catch (JexlException ex) {
           // OK
        }
    }

    @Test
    public void testRedefined() throws Exception {
        try {
           JexlScript e = JEXL.createScript("var x = function(final var a) {var a = 0}");
           Assert.fail("Should have failed");
        } catch (JexlException ex) {
           // OK
        }
    }

}
