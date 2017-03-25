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

import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;

import java.util.List;
import java.util.ArrayList;
import org.apache.commons.jexl3.JexlBuilder;
import org.junit.Test;

/**
 *  Simple example to show how to access arrays.
 *
 *  @since 1.0
 */
public class ArrayTest {
    /**
     * An example for array access.
     */
    static void example(Output out) throws Exception {
        /**
         * First step is to retrieve an instance of a JexlEngine;
         * it might be already existing and shared or created anew.
         */
        JexlEngine jexl = new JexlBuilder().create();
        /*
         *  Second make a jexlContext and put stuff in it
         */
        JexlContext jc = new MapContext();

        List<Object> l = new ArrayList<Object>();
        l.add("Hello from location 0");
        Integer two = new Integer(2);
        l.add(two);
        jc.set("array", l);

        JexlExpression e = jexl.createExpression("array[1]");
        Object o = e.evaluate(jc);
        out.print("Object @ location 1 = ", o, two);

        e = jexl.createExpression("array[0].length()");
        o = e.evaluate(jc);

        out.print("The length of the string at location 0 is : ", o, Integer.valueOf(21));
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