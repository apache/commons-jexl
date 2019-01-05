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
 * Test cases for the var parameters syntax.
 *
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class VarParametersTest extends JexlTestCase {

    public VarParametersTest() {
        super("VarParametersTest");
    }

    @Test
    public void testBasic() throws Exception {
        JexlContext jc = new MapContext();

        JexlScript e = JEXL.createScript("function(var x, var y) { x + y }");
        Object o = e.execute(jc, 40, 2);
        Assert.assertEquals("Result is not as expected", 42, o);
    }

    @Test
    public void testMixed() throws Exception {
        try {
            JexlScript e = JEXL.createScript("function(var x,y) { x + y }");
            Assert.fail("Mixed parameter declaration is not allowed");
        } catch (JexlException ex) {
            // OK
        }
        try {
            JexlScript e = JEXL.createScript("function(x,var y) { x + y }");
            Assert.fail("Mixed parameter declaration is not allowed");
        } catch (JexlException ex) {
            // OK
        }
    }

}
