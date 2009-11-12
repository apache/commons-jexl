/**
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

import junit.framework.TestCase;

import org.apache.commons.jexl.parser.ParseException;

/**
 * Tests for malformed expressions and scripts.
 * ({@link ExpressionFactory} and {@link ScriptFactory} should throw
 * {@link ParseException}s).
 *
 * @since 1.1
 */
public class ParseFailuresTest extends TestCase {

    /**
     * Create the test.
     *
     * @param testName name of the test
     */
    public ParseFailuresTest(String testName) {
        super(testName);
    }

    public void testMalformedExpression1() throws Exception {
        // this will throw a ParseException
        String badExpression = "eq";
        try {
            ExpressionFactory.createExpression(badExpression);
            fail("Parsing \"" + badExpression
                + "\" should result in a ParseException");
        } catch (ParseException pe) {
            // expected
        }
    }

    public void testMalformedExpression2() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a ParseException
        String badExpression = "?";
        try {
            ExpressionFactory.createExpression(badExpression);
            fail("Parsing \"" + badExpression
                + "\" should result in a ParseException");
        } catch (ParseException pe) {
            // expected
        }
    }

    public void testMalformedScript1() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a ParseException
        String badScript = "eq";
        try {
            ScriptFactory.createScript(badScript);
            fail("Parsing \"" + badScript
                + "\" should result in a ParseException");
        } catch (ParseException pe) {
            // expected
        }
    }


    public void testMalformedScript2() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a ParseException
        String badScript = "?";
        try {
            ScriptFactory.createScript(badScript);
            fail("Parsing \"" + badScript
                + "\" should result in a ParseException");
        } catch (ParseException pe) {
            // expected
        }
    }

    public void testMalformedScript3() throws Exception {
        // this will throw a TokenMgrErr, which we rethrow as a ParseException
        String badScript = "foo=1;bar=2;a?b:c;";
        try {
            ScriptFactory.createScript(badScript);
            fail("Parsing \"" + badScript
                + "\" should result in a ParseException");
        } catch (ParseException pe) {
            // expected
        }
    }

}
