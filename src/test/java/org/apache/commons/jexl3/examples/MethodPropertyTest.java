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

package org.apache.commons.jexl3.examples;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.junit.Test;

/**
 *  Simple example to show how to access method and properties.
 *
 *  @since 1.0
 */
public class MethodPropertyTest {
    /**
     * An example for method access.
     */
    public static void example(final Output out) throws Exception {
        /**
         * First step is to retrieve an instance of a JexlEngine;
         * it might be already existing and shared or created anew.
         */
        JexlEngine jexl = new JexlBuilder().create();
        /*
         *  Second make a jexlContext and put stuff in it
         */
        JexlContext jc = new MapContext();

        /**
         * The Java equivalents of foo and number for comparison and checking
         */
        Foo foo = new Foo();
        Integer number = new Integer(10);

        jc.set("foo", foo);
        jc.set("number", number);

        /*
         *  access a method w/o args
         */
        JexlExpression e = jexl.createExpression("foo.getFoo()");
        Object o = e.evaluate(jc);
        out.print("value returned by the method getFoo() is : ", o, foo.getFoo());

        /*
         *  access a method w/ args
         */
        e = jexl.createExpression("foo.convert(1)");
        o = e.evaluate(jc);
        out.print("value of " + e.getParsedText() + " is : ", o, foo.convert(1));

        e = jexl.createExpression("foo.convert(1+7)");
        o = e.evaluate(jc);
        out.print("value of " + e.getParsedText() + " is : ", o, foo.convert(1+7));

        e = jexl.createExpression("foo.convert(1+number)");
        o = e.evaluate(jc);
        out.print("value of " + e.getParsedText() + " is : ", o, foo.convert(1+number.intValue()));

        /*
         * access a property
         */
        e = jexl.createExpression("foo.bar");
        o = e.evaluate(jc);
        out.print("value returned for the property 'bar' is : ", o, foo.get("bar"));

    }

    /**
     * Helper example class.
     */
    public static class Foo {
        /**
         * Gets foo.
         * @return a string.
         */
        public String getFoo() {
            return "This is from getFoo()";
        }

        /**
         * Gets an arbitrary property.
         * @param arg property name.
         * @return arg prefixed with 'This is the property '.
         */
        public String get(String arg) {
            return "This is the property " + arg;
        }

        /**
         * Gets a string from the argument.
         * @param i a long.
         * @return The argument prefixed with 'The value is : '
         */
        public String convert(long i) {
            return "The value is : " + i;
        }
    }


    /**
     * Unit test entry point.
     * @throws Exception
     */
    @Test
    public void testExample() throws Exception {
        example(Output.JUNIT);
    }

    /**
     * Command line entry point.
     * @param args command line arguments
     * @throws Exception cos jexl does.
     */
    public static void main(String[] args) throws Exception {
        example(Output.SYSTEM);
    }
}