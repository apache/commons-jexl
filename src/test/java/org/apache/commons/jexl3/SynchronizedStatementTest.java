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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests synchronized statement.
 * @since 3.2
 */
@SuppressWarnings({"UnnecessaryBoxing"})
public class SynchronizedStatementTest extends JexlTestCase {

    public SynchronizedStatementTest() {
        super("SynchronizedStatementTest");
    }

    @Test
    public void testStatement() throws Exception {
        JexlContext jc = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript js = jexl.createScript("var result = 0; var x = {1,2,3}; synchronized(x) {for (var i : x) result += i}");
        Object size = js.execute(jc);
        Assert.assertEquals(6, size);
    }

}
