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

package org.apache.commons.jexl3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.jexl3.junit.Asserter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for arithmetic operators which are called in statetement-like fashion.
 * @since 3.0
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ArithmeticStatementTest extends JexlTestCase {
    private Asserter asserter;

    @Before
    @Override
    public void setUp() {
        asserter = new Asserter(JEXL);
        asserter.setStrict(false);
    }

    /**
     * Create the named test.
     * @param name test name
     */
    public ArithmeticStatementTest() {
        super("ArithmeticStatementTest");
    }

    public static class StatementArithmetic extends JexlArithmetic {
        StatementArithmetic(boolean flag) {
            super(flag);
        }

        public void raise() throws Exception {
            throw new Exception("Do not execute");
        }

    }

    @Test
    public void testStatementArithmetic() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().cache(32).arithmetic(new StatementArithmetic(true)).create();
        JexlScript expr0 = jexl.createScript("raise; return 2");

        try {
           Object value0 = expr0.execute(jc);
           Assert.fail("should have cancelled");
        } catch (Exception ex) {
           // Ok
        }
    }

}
