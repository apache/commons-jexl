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

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.junit.Assert;
import org.junit.Test;

/**
 * @since 1.0
 *
 */
public class ParserTest {
    static final JexlFeatures FEATURES = new JexlFeatures();
    public ParserTest() {}

    /**
     * See if we can parse simple scripts
     */
    @Test
    public void testParse() throws Exception {
        Parser parser = new Parser(new StringReader(";"));
        JexlNode sn;
        sn = parser.parse(null, FEATURES, "foo = 1;", null);
        Assert.assertNotNull("parsed node is null", sn);

        sn = parser.parse(null, FEATURES, "foo = \"bar\";", null);
        Assert.assertNotNull("parsed node is null", sn);

        sn = parser.parse(null, FEATURES, "foo = 'bar';", null);
        Assert.assertNotNull("parsed node is null", sn);
    }

    @Test
    public void testErrorAssign() throws Exception {
        String[] ops = { "=", "+=", "-=", "/=", "*=", "^=", "&=", "|=" };
        for(String op : ops) {
            Parser parser = new Parser(new StringReader(";"));
            try {
                JexlNode sn = parser.parse(null, FEATURES, "foo() "+op+" 1;", null);
                Assert.fail("should have failed on invalid assignment " + op);
            } catch (JexlException.Parsing xparse) {
                // ok
                String ss = xparse.getDetail();
                String sss = xparse.toString();
            }
        }
    }

    @Test
    public void testErrorAmbiguous() throws Exception {
        Parser parser = new Parser(new StringReader(";"));
        try {
            JexlNode sn = parser.parse(null, FEATURES, "x = 1 y = 5", null);
            Assert.fail("should have failed on ambiguous statement");
        } catch (JexlException.Ambiguous xambiguous) {
            // ok
        } catch(JexlException xother) {
            Assert.fail(xother.toString());
        }
    }
        
    @Test
    public void testIdentifierEscape() {
        String[] ids = new String[]{"a\\ b", "a\\ b\\ c", "a\\'b\\\"c", "a\\ \\ c"};
        for(String id : ids) {
            String esc0 = StringParser.unescapeIdentifier(id);
            Assert.assertFalse(esc0.contains("\\"));
            String esc1 = StringParser.escapeIdentifier(esc0);
            Assert.assertEquals(id, esc1);
        }
    }
}
