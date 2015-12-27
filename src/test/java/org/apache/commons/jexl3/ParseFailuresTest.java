/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for malformed expressions and scripts.
 * ({@link org.apache.commons.jexl3.JexlEngine#createExpression(String)} and
 * {@link org.apache.commons.jexl3.JexlEngine#createScript(String)} should throw
 * {@link org.apache.commons.jexl3.JexlException}s).
 *
 * @since 1.1
 */
public class ParseFailuresTest extends JexlTestCase {

    static final Log LOGGER = LogFactory.getLog(ParseFailuresTest.class.getName());

    /**
     * Create the test.
     *
     * @param testName name of the test
     */
    public ParseFailuresTest() {
        super("ParseFailuresTest");
    }

    @Test
    public void testMalformedExpression1() throws Exception {
        // this will throw a JexlException
        String badExpression = "eq";
        try {
            JEXL.createExpression(badExpression);
            Assert.fail("Parsing \"" + badExpression
                    + "\" should result in a JexlException");
        } catch (JexlException pe) {
            // expected
            LOGGER.info(pe);
        }
    }

    @Test
    public void testMalformedExpression2() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a JexlException
        String badExpression = "?";
        try {
            JEXL.createExpression(badExpression);
            Assert.fail("Parsing \"" + badExpression
                    + "\" should result in a JexlException");
        } catch (JexlException pe) {
            // expected
            LOGGER.info(pe);
        }
    }

    @Test
    public void testMalformedScript1() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a JexlException
        String badScript = "eq";
        try {
            JEXL.createScript(badScript);
            Assert.fail("Parsing \"" + badScript
                    + "\" should result in a JexlException");
        } catch (JexlException pe) {
            // expected
            LOGGER.info(pe);
        }
    }

    @Test
    public void testMalformedScript2() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a JexlException
        String badScript = "?";
        try {
            JEXL.createScript(badScript);
            Assert.fail("Parsing \"" + badScript
                    + "\" should result in a JexlException");
        } catch (JexlException pe) {
            // expected
            LOGGER.info(pe);
        }
    }

    @Test
    public void testMalformedScript3() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a JexlException
        String badScript = "foo=1;bar=2;a?b:c:d;";
        try {
            JEXL.createScript(badScript);
            Assert.fail("Parsing \"" + badScript
                    + "\" should result in a JexlException");
        } catch (JexlException pe) {
            // expected
            LOGGER.error(pe);
        }
    }

}
