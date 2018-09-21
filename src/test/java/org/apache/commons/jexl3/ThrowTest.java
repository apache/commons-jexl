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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests throw statement.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ThrowTest extends JexlTestCase {

    public ThrowTest() {
        super("ThrowTest");
    }

    @Test
    public void testMessage() throws Exception {
        JexlScript e = JEXL.createScript("throw 'Error'");
        JexlContext jc = new MapContext();
        try {
            Object o = e.execute(jc);
        } catch (Exception ex) {
           Assert.assertEquals("Result is not last evaluated expression", "Error", ex.getMessage());
        }
    }

    @Test
    public void testException() throws Exception {
        JexlScript e = JEXL.createScript("throw new ('java.lang.IllegalStateException', 'Error')");
        JexlContext jc = new MapContext();
        try {
            Object o = e.execute(jc);
        } catch (Exception ex) {
           Assert.assertTrue(ex instanceof IllegalStateException);
        }
    }

}
