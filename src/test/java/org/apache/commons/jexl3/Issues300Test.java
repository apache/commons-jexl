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

import java.util.HashMap;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test cases for reported issue between JEXL-300 and JEXL-399.
 */
public class Issues300Test {
    @Test
    public void testIssue301a() throws Exception {
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

    @Test
    public void testIssues301b() throws Exception {
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

     @Test
    public void testIssue302() throws Exception {
        JexlContext jc = new MapContext();
        String[] strs = new String[]{
            "{if (0) 1 else 2; var x = 4;}",
            "if (0) 1; else 2; ",
            "{ if (0) 1; else 2; }",
            "{ if (0) { if (false) 1 else -3 } else 2; }"
        };
        JexlEngine jexl = new JexlBuilder().create();
        for(String str : strs) {
        JexlScript e = jexl.createScript(str);
        Object o = e.execute(jc);
        int oo = ((Number) o).intValue() % 2;
        Assert.assertEquals("Block result is wrong " + str, 0, oo);
        }
    }

    @Test
    public void testIssue304() {
        JexlEngine jexlEngine = new JexlBuilder().strict(false).create();
        JexlExpression e304 = jexlEngine.createExpression("overview.limit.var");

        HashMap<String,Object> map3 = new HashMap<String,Object>();
        map3.put("var", "4711");
        HashMap<String,Object> map2 = new HashMap<String,Object>();
        map2.put("limit", map3);
        HashMap<String,Object> map = new HashMap<String,Object>();
        map.put("overview", map2);

        JexlContext context = new MapContext(map);
        Object value = e304.evaluate(context);
        assertEquals("4711", value); // fails

        map.clear();
        map.put("overview.limit.var", 42);
        value = e304.evaluate(context);
        assertEquals(42, value);

        String allkw = "e304.if.else.do.while.new.true.false.null.var.function.empty.size.not.and.or.ne.eq.le.lt.gt.ge";
        map.put(allkw, 42);
        e304 = jexlEngine.createExpression(allkw);
        value = e304.evaluate(context);
        assertEquals(42, value);
    }

    @Test
    public void testIssue305() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e;
        e = jexl.createScript("{while(false) {}; var x = 1;}");
        String str0 = e.getParsedText();
        e =  jexl.createScript(str0);
        Assert.assertNotNull(e);
        String str1 = e.getParsedText();
        Assert.assertEquals(str0, str1);
    }

    @Test
    public void testIssue306() throws Exception {
        JexlContext ctxt = new MapContext();
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("x.y ?: 2");
        Object o1 = e.execute(null);
        Assert.assertEquals(2, o1);
        ctxt.set("x.y", null);
        Object o2 = e.execute(ctxt);
        Assert.assertEquals(2, o2);
    }

    @Test
    public void testIssue306a() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("x.y ?: 2", "x");
        Object o = e.execute(null, new Object());
        Assert.assertEquals(2, o);
        o = e.execute(null);
        Assert.assertEquals(2, o);
    }

    @Test
    public void testIssue306b() throws Exception {
        JexlEngine jexl = new JexlBuilder().create();
        JexlScript e = jexl.createScript("x?.y ?: 2", "x");
        Object o1 = e.execute(null, new Object());
        Assert.assertEquals(2, o1);
        Object o2 = e.execute(null);
        Assert.assertEquals(2, o2);
    }

    @Test
    public void testIssue306c() throws Exception {
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlScript e = jexl.createScript("x.y ?: 2", "x");
        Object o = e.execute(null, new Object());
        Assert.assertEquals(2, o);
        o = e.execute(null);
        Assert.assertEquals(2, o);
    }
    
    @Test
    public void testIssue306d() throws Exception {
        JexlEngine jexl = new JexlBuilder().safe(true).create();
        JexlScript e = jexl.createScript("x.y[z.t] ?: 2", "x");
        Object o = e.execute(null, new Object());
        Assert.assertEquals(2, o);
        o = e.execute(null);
        Assert.assertEquals(2, o);
    }
}
