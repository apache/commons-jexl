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

import org.apache.commons.jexl3.internal.Engine32;
import org.apache.commons.jexl3.internal.OptionsContext;
import static org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED;
import org.apache.commons.jexl3.introspection.JexlSandbox;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.jexl3.internal.Util.debuggerCheck;
import static org.junit.Assert.assertEquals;

/**
 * Test cases for reported issue between JEXL-300 and JEXL-399.
 */
public class Issues400Test {

  @Test
  public void test402() {
    final JexlContext jc = new MapContext();
    final String[] sources = new String[]{
        "if (true) { return }",
        "if (true) { 3; return }",
        "(x->{ 3; return })()"
    };
    final JexlEngine jexl = new JexlBuilder().create();
    for (final String source : sources) {
      final JexlScript e = jexl.createScript(source);
      final Object o = e.execute(jc);
      Assert.assertNull(o);
    }
  }

  @Test
  public void test403() {
    for(String setmap : new String[]{
        "  map1.`${item.a}` = 1;\n",
        "  map1[`${item.a}`] = 1;\n",
        "  map1[item.a] = 1;\n"
    }) {
      String src = "var a = {'a': 1};\n" +
          "var list = [a, a];\n" +
          "let map1 = {:};\n" +
          "for (var item : list) {\n" +
          setmap +
          "}\n " +
          "map1";
      final JexlEngine jexl = new JexlBuilder().cache(64).create();
      JexlScript script = jexl.createScript(src);
      for (int i = 0; i < 2; ++i) {
        Object result = script.execute(null);
        Assert.assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(1));
        Assert.assertTrue(map.containsValue(1));
      }
    }
  }
}
