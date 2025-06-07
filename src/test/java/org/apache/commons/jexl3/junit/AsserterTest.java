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

package org.apache.commons.jexl3.junit;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.jexl3.Foo;
import org.apache.commons.jexl3.JexlTestCase;
import org.junit.jupiter.api.Test;

/**
 *  Simple tests
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class AsserterTest extends JexlTestCase {
    public AsserterTest() {
        super("AsserterTest");
    }

    @Test
    void testThis() throws Exception {
        final Asserter asserter = new Asserter(JEXL);
        asserter.setVariable("this", new Foo());
        asserter.assertExpression("this.repeat('abc')", "Repeat : abc");
        assertThrows(AssertionError.class, () -> asserter.assertExpression("this.count", "Wrong Value"));
    }

    @Test
    void testVariable() throws Exception {
        final Asserter asserter = new Asserter(JEXL);
        asserter.setSilent(true);
        asserter.setVariable("foo", new Foo());
        asserter.setVariable("person", "James");

        asserter.assertExpression("person", "James");
        asserter.assertExpression("size(person)", Integer.valueOf(5));

        asserter.assertExpression("foo.getCount()", Integer.valueOf(5));
        asserter.assertExpression("foo.count", Integer.valueOf(5));

        assertThrows(AssertionError.class, () -> asserter.assertExpression("bar.count", Integer.valueOf(5)));
    }
}
