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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.junit.Test;

/**
 */
public class ParserTest {
    static final JexlFeatures FEATURES = new JexlFeatures();
    public ParserTest() {}

    /**
     * Test the escaped control characters.
     */
    @Test
    public void testControlCharacters() {
        // Both '' and "" are valid JEXL string
        // The array of tuples where the first element is an expected result and the second element is a test string.
        final String[][] strings = {
            new String[] {"a\nb\tc", "'a\nb\tc'"}, // we still honor the actual characters
            new String[] {"a\nb\tc", "'a\\nb\\tc'"},
            new String[] {"a\nb\tc", "\"a\\nb\\tc\""},
            new String[] {"\b\t\n\f\r", "'\\b\\t\\n\\f\\r'"},
            new String[] {"'hi'", "'\\'hi\\''"},
            new String[] {"\"hi\"", "'\"hi\"'"},
            new String[] {"\"hi\"", "'\"hi\"'"},
        };
        for(final String[] pair: strings) {
            final String output = StringParser.buildString(pair[1], true);
            assertEquals(pair[0], output);
        }
    }

    @Test
    public void testErrorAmbiguous() throws Exception {
        final Parser parser = new Parser(";");
        try {
            final JexlNode sn = parser.parse(null, FEATURES, "x = 1 y = 5", null);
            fail("should have failed on ambiguous statement");
        } catch (final JexlException.Ambiguous xambiguous) {
            // ok
        } catch (final JexlException xother) {
            fail(xother.toString());
        }
    }

    @Test
    public void testErrorAssign() throws Exception {
        final String[] ops = { "=", "+=", "-=", "/=", "*=", "^=", "&=", "|=" };
        for(final String op : ops) {
            final Parser parser = new Parser(";");
            try {
                final JexlNode sn = parser.parse(null, FEATURES, "foo() "+op+" 1;", null);
                fail("should have failed on invalid assignment " + op);
            } catch (final JexlException.Parsing xparse) {
                // ok
                final String ss = xparse.getDetail();
                final String sss = xparse.toString();
            }
        }
    }

    @Test
    public void testIdentifierEscape() {
        final String[] ids = {"a\\ b", "a\\ b\\ c", "a\\'b\\\"c", "a\\ \\ c"};
        for(final String id : ids) {
            final String esc0 = StringParser.unescapeIdentifier(id);
            assertFalse(esc0.contains("\\"));
            final String esc1 = StringParser.escapeIdentifier(esc0);
            assertEquals(id, esc1);
        }
    }

    /**
     * See if we can parse simple scripts
     */
    @Test
    public void testParse() throws Exception {
        final Parser parser = new Parser(";");
        JexlNode sn;
        sn = parser.parse(null, FEATURES, "foo = 1;", null);
        assertNotNull(sn, "parsed node is null");

        sn = parser.parse(null, FEATURES, "foo = \"bar\";", null);
        assertNotNull(sn, "parsed node is null");

        sn = parser.parse(null, FEATURES, "foo = 'bar';", null);
        assertNotNull(sn, "parsed node is null");
    }
}
