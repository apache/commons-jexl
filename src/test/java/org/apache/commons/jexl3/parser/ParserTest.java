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
package org.apache.commons.jexl3.parser;

import java.io.StringReader;

import junit.framework.TestCase;
import org.apache.commons.jexl3.JexlException;

/**
 * @since 1.0
 *
 */
public class ParserTest extends TestCase {
    public ParserTest(String testName) {
        super(testName);
    }

    /**
     * See if we can parse simple scripts
     */
    public void testParse() throws Exception {
        Parser parser = new Parser(new StringReader(";"));

        JexlNode sn;
        sn = parser.parse(null, "foo = 1;", null, false, false);
        assertNotNull("parsed node is null", sn);

        sn = parser.parse(null, "foo = \"bar\";", null, false, false);
        assertNotNull("parsed node is null", sn);

        sn = parser.parse(null, "foo = 'bar';", null, false, false);
        assertNotNull("parsed node is null", sn);
    }

    public void testErrorAssign() throws Exception {
        String[] ops = { "=", "+=", "-=", "/=", "*=", "^=", "&=", "|=" };
        for(String op : ops) {
            Parser parser = new Parser(new StringReader(";"));
            try {
                JexlNode sn = parser.parse(null, "foo() "+op+" 1;", null, false, false);
                fail("should have failed on invalid assignment " + op);
            } catch (JexlException.Parsing xparse) {
                // ok
                String ss = xparse.getDetail();
                String sss = xparse.toString();
            }
        }
    }

    public void testErrorAmbiguous() throws Exception {
        Parser parser = new Parser(new StringReader(";"));
        try {
            JexlNode sn = parser.parse(null, "x = 1 y = 5", null, false, false);
            fail("should have failed on ambiguous statement");
        } catch (JexlException.Ambiguous xambiguous) {
            // ok
        }
    }
}
