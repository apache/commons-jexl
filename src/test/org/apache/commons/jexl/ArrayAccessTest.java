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
package org.apache.commons.jexl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl.junit.Asserter;

import junit.framework.TestCase;

/**
 * Tests for array access operator []
 * 
 * @since 2.0
 */
public class ArrayAccessTest extends TestCase {

    private Asserter asserter;

    protected static final String GET_METHOD_STRING = "GetMethod string";
    protected static final String[] GET_METHOD_ARRAY =
        new String[] { "One", "Two", "Three" };

    protected static final String[][] GET_METHOD_ARRAY2 =
        new String[][] { {"One", "Two", "Three"},{"Four", "Five", "Six"} };

    public void setUp() {
        asserter = new Asserter();
    }

    /**
     * test simple array access
     */
    public void testArrayAccess() throws Exception {

        /*
         * test List access
         */

        List l = new ArrayList();
        l.add(new Integer(1));
        l.add(new Integer(2));
        l.add(new Integer(3));

        asserter.setVariable("list", l);

        asserter.assertExpression("list[1]", new Integer(2));
        asserter.assertExpression("list[1+1]", new Integer(3));
        asserter.setVariable("loc", new Integer(1));
        asserter.assertExpression("list[loc+1]", new Integer(3));

        /*
         * test array access
         */

        String[] args = { "hello", "there" };
        asserter.setVariable("array", args);
        asserter.assertExpression("array[0]", "hello");

        /*
         * to think that this was an intentional syntax...
         */
        asserter.assertExpression("array.0", "hello");

        /*
         * test map access
         */
        Map m = new HashMap();
        m.put("foo", "bar");

        asserter.setVariable("map", m);
        asserter.setVariable("key", "foo");

        asserter.assertExpression("map[\"foo\"]", "bar");
        asserter.assertExpression("map[key]", "bar");

        /*
         * test bean access
         */
        asserter.setVariable("foo", new Foo());
        asserter.assertExpression("foo[\"bar\"]", GET_METHOD_STRING);
        asserter.assertExpression("foo[\"bar\"] == foo.bar", Boolean.TRUE);
    }

    /**
     * test some simple double array lookups
     */
    public void testDoubleArrays() throws Exception {
        Object[][] foo = new Object[2][2];
        foo[0][0] = "one";
        foo[0][1] = "two";

        asserter.setVariable("foo", foo);

        asserter.assertExpression("foo[0][1]", "two");
    }

    public void testArrayProperty() throws Exception {
        Foo foo = new Foo();

        asserter.setVariable("foo", foo);

        asserter.assertExpression("foo.array[1]", GET_METHOD_ARRAY[1]);
        asserter.assertExpression("foo.array.1", GET_METHOD_ARRAY[1]);
        asserter.assertExpression("foo.array2[1][1]", GET_METHOD_ARRAY2[1][1]);
        // asserter.assertExpression("foo.array2.1.1", GET_METHOD_ARRAY2[1][1]);
    }

}