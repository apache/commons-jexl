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
/* $Id$ */
package org.apache.commons.jexl3.junit;

import org.apache.commons.jexl3.Foo;
import org.apache.commons.jexl3.JexlTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 *  Simple testcases
 *
 *  @since 1.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class AsserterTest extends JexlTestCase {
    public AsserterTest() {
        super("AsserterTest");
    }

    @Test
    public void testThis() throws Exception {
        Asserter asserter = new Asserter(JEXL);
        asserter.setVariable("foo", new Foo());
        asserter.assertExpression("foo.repeat('abc')", "Repeat : abc");
        try {
            asserter.assertExpression("foo.count", "Wrong Value");
            Assert.fail("This method should have thrown an assertion exception");
        }
        catch (AssertionError e) {
            // it worked!
        }
    }

    @Test
    public void testVariable() throws Exception {
        Asserter asserter = new Asserter(JEXL);
        asserter.setSilent(true);
        asserter.setVariable("foo", new Foo());
        asserter.setVariable("person", "James");

        asserter.assertExpression("person", "James");
        asserter.assertExpression("size(person)", new Integer(5));

        asserter.assertExpression("foo.getCount()", new Integer(5));
        asserter.assertExpression("foo.count", new Integer(5));

        try {
            asserter.assertExpression("bar.count", new Integer(5));
            Assert.fail("This method should have thrown an assertion exception");
        }
        catch (AssertionError e) {
            // it worked!
        }
    }
}
