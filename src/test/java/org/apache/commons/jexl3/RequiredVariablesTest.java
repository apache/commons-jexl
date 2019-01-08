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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the non-null variables.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class RequiredVariablesTest extends JexlTestCase {

    JexlEngine jexl = new JexlBuilder().strict(false).arithmetic(new JexlArithmetic(false)).create();

    public RequiredVariablesTest() {
        super("RequiredVariablesTest");
    }

    @Test
    public void testBasic() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("var &x = 1");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testMultiple() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = jexl.createScript("var (&x,y) = [1,2]; x");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not as expected", 1, o);
    }

    @Test
    public void testUninitialized() throws Exception {
        try {
           JexlScript e = JEXL.createScript("var &x");
           Assert.fail("Non null variables should require an initialization");
        } catch (JexlException ex) {
           // OK
        }
    }

    @Test
    public void testNullInitialization() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("var &x = null");
        try {
           Object o = e.execute(jc);
           Assert.fail("Null values should not be assigned to non null variables");
        } catch (JexlException ex) {
           // OK
        }
    }

    @Test
    public void testNullAssignment() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("var &x = 1; x = null");
        try {
           Object o = e.execute(jc);
           Assert.fail("Null values should not be assigned to non null variables");
        } catch (JexlException ex) {
           // OK
        }

        e = JEXL.createScript("var (&x,y) = [1,2]; x = null");
        try {
           Object o = e.execute(jc);
           Assert.fail("Null values should not be assigned to non null variables");
        } catch (JexlException ex) {
           // OK
        }
    }

    @Test
    public void testMultipleNullInitialization() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("var (&x,y) = [null,3]");
        try {
           Object o = e.execute(jc);
           Assert.fail("Null values should not be assigned to non null variables");
        } catch (JexlException ex) {
           // OK
        }
    }

    @Test
    public void testMultipleNullAssignment() throws Exception {
        JexlContext jc = new MapContext();
        JexlScript e = JEXL.createScript("var (&x,y) = [41,42]; (x,y) = [null,44]");
        try {
           Object o = e.execute(jc);
           Assert.fail("Null values should not be assigned to non null variables");
        } catch (JexlException ex) {
           // OK
        }

        e = JEXL.createScript("var (x,&y) = [41,42]; (x,y) = [44]");
        try {
           Object o = e.execute(jc);
           Assert.fail("Null values should not be assigned to non null variables");
        } catch (JexlException ex) {
           // OK
        }

    }

}
