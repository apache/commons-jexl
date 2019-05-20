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
import org.junit.Ignore;

/**
 * Test cases for reported issue between JEXL-300 and JEXL-399.
 */
public class Issues300Test {
    @Ignore
    public void testNullArrayAccess301a() throws Exception {
        JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(new JexlArithmetic(false)).create();
        String[] srcs = new String[]{
            "var x = null; x.0", "var x = null; x[0]", "var x = [null,1]; x[0][0]"
        };
        for (int i = 0; i < srcs.length; ++i) {
            String src = srcs[i];
            JexlScript s = jexl.createScript(src);
            try {
                Object o = s.execute(null);
                if (i > 0) {
                    Assert.fail(src + ": Should have failed");
                }
            } catch (Exception ex) {
                //
            }
        }
    }

    @Ignore
    public void testNullArrayAccess301b() throws Exception {
        JexlEngine jexl = new JexlBuilder().safe(false).arithmetic(new JexlArithmetic(false)).create();
        Object[] xs = new Object[]{null, null, new Object[]{null, 1}};
        String[] srcs = new String[]{
            "x.0", "x[0]", "x[0][0]"
        };
        JexlContext ctxt = new MapContext();
        for (int i = 0; i < xs.length; ++i) {
            ctxt.set("x", xs[i]);
            String src = srcs[i];
            JexlScript s = jexl.createScript(src);
            try {
                Object o = s.execute(null);
                Assert.fail(src + ": Should have failed");
            } catch (Exception ex) {
                //
            }
        }
    }
}
