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
package org.apache.commons.jexl2.parser;

import java.io.StringReader;

import junit.framework.TestCase;

/**
 * @since 1.0
 *
 */
public class ParserTest extends TestCase {
    public ParserTest(String testName) {
        super(testName);
    }

    /**
     *  parse test : see if we can parse a little script
     */
    public void testParse1() throws Exception {
        Parser parser = new Parser(new StringReader(";"));

        SimpleNode sn = parser.parse(new StringReader("foo = 1;"), null);
        assertNotNull("parsed node is null", sn);
    }

    public void testParse2() throws Exception {
        Parser parser = new Parser(new StringReader(";"));

        SimpleNode sn = parser.parse(new StringReader("foo = \"bar\";"), null);
        assertNotNull("parsed node is null", sn);

        sn = parser.parse(new StringReader("foo = 'bar';"), null);
        assertNotNull("parsed node is null", sn);
    }

    public void testReadLines() throws Exception {
        String input = "foo bar\n\rquux\n\r\npizza";
        StringBuilder output = new StringBuilder(32);
        int read = StringParser.readLines(output, input, 0, null);
        assertEquals(input.length(), read);
        String outstr = output.toString();
        assertEquals("foo bar quux pizza ", outstr);
    }

    public void testReadLines$$() throws Exception {
        String input = "$$foo bar\n\r$$quux\n\r\n$$pizza";
        StringBuilder output = new StringBuilder(32);
        int read = StringParser.readLines(output, input, 0, "$$");
        assertEquals(input.length(), read);
        String outstr = output.toString();
        assertEquals("foo bar quux pizza ", outstr);
    }
}
