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
package org.example;

import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlOptions;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.internal.Frame;
import org.apache.commons.jexl3.internal.Interpreter;
import org.apache.commons.jexl3.internal.TemplateEngine;
import org.apache.commons.jexl3.internal.TemplateInterpreter;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.parser.JexlNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SomeTest {

  public static class MyMath {
    public double cos(final double x) {
      return Math.cos(x);
    }
  }

  /**
   * User namespace needs to be allowed through permissions.
   */
  @Test
  public void testCustomFunctionPermissions() {
      Map<String, Object> funcs = new HashMap<String, Object>();
      funcs.put("math", new MyMath());
      JexlPermissions permissions = JexlPermissions.parse("org.example.*");
      JexlEngine jexl = new JexlBuilder().permissions(permissions).namespaces(funcs).create();
      JexlContext jc = new MapContext();
      jc.set("pi", Math.PI);
      JexlExpression e = jexl.createExpression("math:cos(pi)");
      Number result = (Number) e.evaluate(jc);
      Assert.assertEquals(-1, result.intValue());
  }

  /**
   * Engine creating dedicated template interpreter.
   */
  public static class Engine406 extends Engine {
    public Engine406(final JexlBuilder conf) {
      super(conf);
    }
    @Override public TemplateInterpreter createTemplateInterpreter(final TemplateInterpreter.Arguments args) {
      return new TemplateInterpreter406(args);
    }
  }

  /** Counting the number of node interpretation calls. */
  static AtomicInteger CALL406 = new AtomicInteger(0);

  public static class TemplateInterpreter406 extends TemplateInterpreter {
    protected TemplateInterpreter406(final Arguments args) {
      super(args);
    }
    public Object interpret(final JexlNode node) {
      CALL406.incrementAndGet();
      return super.interpret(node);
    }
  }

  @Test
  public void test406b() {
    final JexlEngine jexl = new JexlBuilder() {
      @Override
      public JexlEngine create() {
        return new Engine406(this);
      }
    }.cache(64).strict(true).safe(false).create();
    String src = "`Call ${x}`";
    JexlScript script = jexl.createScript(src, "x");
    Object result = script.execute(null, 406);
    Assert.assertEquals("Call 406", result);
    Assert.assertEquals(1, CALL406.get());
    result = script.execute(null, 42);
    Assert.assertEquals("Call 42", result);
    Assert.assertEquals(2, CALL406.get());
  }
}
