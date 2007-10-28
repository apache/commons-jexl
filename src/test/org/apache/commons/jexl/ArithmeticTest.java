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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.jexl.junit.Asserter;

import junit.framework.TestCase;

public class ArithmeticTest extends TestCase {
    private Asserter asserter;

    public void setUp() {
        asserter = new Asserter();
    }

    public void testBigDecimal() throws Exception {
        asserter.setVariable("left", new BigDecimal(2));
        asserter.setVariable("right", new BigDecimal(6));
        asserter.assertExpression("left + right", new BigDecimal(8));
        asserter.assertExpression("right - left", new BigDecimal(4));
        asserter.assertExpression("right * left", new BigDecimal(12));
        asserter.assertExpression("right / left", new BigDecimal(3));
        asserter.assertExpression("right % left", new BigDecimal(0));
    }

    public void testBigInteger() throws Exception {
        asserter.setVariable("left", new BigInteger("2"));
        asserter.setVariable("right", new BigInteger("6"));
        asserter.assertExpression("left + right", new BigInteger("8"));
        asserter.assertExpression("right - left", new BigInteger("4"));
        asserter.assertExpression("right * left", new BigInteger("12"));
        asserter.assertExpression("right / left", new BigInteger("3"));
        asserter.assertExpression("right % left", new BigInteger("0"));
    }

    /**
     * test some simple mathematical calculations
     */
    public void testUnaryMinus() throws Exception {
        asserter.setVariable("aByte", new Byte((byte) 1));
        asserter.setVariable("aShort", new Short((short) 2));
        asserter.setVariable("anInteger", new Integer(3));
        asserter.setVariable("aLong", new Long(4));
        asserter.setVariable("aFloat", new Float(5.5));
        asserter.setVariable("aDouble", new Double(6.6));
        asserter.setVariable("aBigInteger", new BigInteger("7"));
        asserter.setVariable("aBigDecimal", new BigDecimal("8.8"));

        asserter.assertExpression("-3", new Integer("-3"));
        asserter.assertExpression("-3.0", new Float("-3.0"));
        asserter.assertExpression("-aByte", new Byte((byte) -1));
        asserter.assertExpression("-aShort", new Short((short) -2));
        asserter.assertExpression("-anInteger", new Integer(-3));
        asserter.assertExpression("-aLong", new Long(-4));
        asserter.assertExpression("-aFloat", new Float(-5.5));
        asserter.assertExpression("-aDouble", new Double(-6.6));
        asserter.assertExpression("-aBigInteger", new BigInteger("-7"));
        asserter.assertExpression("-aBigDecimal", new BigDecimal("-8.8"));
    }

    /**
     * test some simple mathematical calculations
     */
    public void testCalculations() throws Exception {

        asserter.setVariable("foo", new Integer(2));

        asserter.assertExpression("foo + 2", new Long(4));
        asserter.assertExpression("3 + 3", new Long(6));
        asserter.assertExpression("3 + 3 + foo", new Long(8));
        asserter.assertExpression("3 * 3", new Long(9));
        asserter.assertExpression("3 * 3 + foo", new Long(11));
        asserter.assertExpression("3 * 3 - foo", new Long(7));

        /*
         * test some floaty stuff
         */
        asserter.assertExpression("3 * \"3.0\"", new Double(9));
        asserter.assertExpression("3 * 3.0", new Double(9));

        /*
         * test / and %
         */
        asserter.assertExpression("6 / 3", new Double(6 / 3));
        asserter.assertExpression("6.4 / 3", new Double(6.4 / 3));
        asserter.assertExpression("0 / 3", new Double(0 / 3));
        asserter.assertExpression("3 / 0", new Double(0));
        asserter.assertExpression("4 % 3", new Long(1));
        asserter.assertExpression("4.8 % 3", new Double(4.8 % 3));

        /*
         * test new null coersion
         */
        asserter.setVariable("imanull", null);
        asserter.assertExpression("imanull + 2", new Long(2));
        asserter.assertExpression("imanull + imanull", new Long(0));
    }
}
