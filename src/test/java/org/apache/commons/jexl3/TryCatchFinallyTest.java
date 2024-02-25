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
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

public class TryCatchFinallyTest extends JexlTestCase {
  public TryCatchFinallyTest() {
    super(TryCatchFinallyTest.class.getSimpleName());
  }

  @Test
  public void testStandard0x2() {
    String src = "try { 42; } finally { 169; }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    Object result = script.execute(null);
    Assert.assertEquals(42, result);
  }

  @Test
  public void testForm0x2a() {
    String src = "try(let x = 42) { x; } finally { 169; }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    Object result = script.execute(null);
    Assert.assertEquals(42, result);
  }
  @Test
  public void testForm0x2b() {
    String src = "try(let x = 19, y = 23) { x + y; } finally { 169; }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    Object result = script.execute(null);
    Assert.assertEquals(42, result);
  }
  @Test
  public void testForm0x2c() {
    String src = "try(const x = 19; let y = 23; ) { x + y; } finally { 169; }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    Object result = script.execute(null);
    Assert.assertEquals(42, result);
  }
  @Test
  public void testForm0x2d() {
    String src = "try(var x = 19; const y = 23;) { x + y; } finally { 169; }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    Object result = script.execute(null);
    Assert.assertEquals(42, result);
  }

  @Test
  public void testThrow0x2a() {
    String src = "try(let x = 42) { throw x } finally { 169; }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    try {
      Object result = script.execute(null);
      Assert.fail("throw did not throw");
    } catch(JexlException.Throw xthrow) {
      Assert.assertEquals(42, xthrow.getValue());
    }
  }
  @Test
  public void testThrow0x2b() {
    String src = "try(let x = 42) { throw x } finally { throw 169 }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    try {
      Object result = script.execute(null);
      Assert.fail("throw did not throw");
    } catch(JexlException.Throw xthrow) {
      Assert.assertEquals(169, xthrow.getValue());
    }
  }

  @Test
  public void testThrowRecurse() {
    String src = "function fact(x, f) { if (x == 1) throw f; fact(x - 1, f * x); } fact(7, 1);";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    try {
      Object result = script.execute(null);
      Assert.fail("throw did not throw");
    } catch(JexlException.Throw xthrow) {
      Assert.assertEquals(5040, xthrow.getValue());
    }
  }

  public static class Circuit implements AutoCloseable {
    boolean opened = true;

    public boolean isOpened() {
      return opened;
    }
    @Override
    public void close() throws IOException {
      opened = false;
    }
    public void raiseError() {
      throw new RuntimeException("raising error");
    }
  }

  @Test
  public void testCloseable0x2b() {
    String src = "try(let x = c) { c.isOpened()? 42 : -42; } finally { 169; }";
    JexlScript script = JEXL.createScript(src, "c");
    Circuit circuit = new Circuit();
    Assert.assertNotNull(script);
    Object result = script.execute(null, circuit);
    Assert.assertEquals(42, result);
    Assert.assertFalse(circuit.isOpened());
  }
  @Test
  public void testCloseable0x3b() {
    String src = "try(let x = c) { c.raiseError(); -42; } catch(const y) { 42; } finally { 169; }";
    JexlScript script = JEXL.createScript(src, "c");
    Circuit circuit = new Circuit();
    Assert.assertNotNull(script);
    Object result = script.execute(null, circuit);
    Assert.assertEquals(42, result);
    Assert.assertFalse(circuit.isOpened());
  }

  @Test
  public void testRedefinition0() {
    String src = "try(let x = c) { let x = 3; -42; }";
    try {
      JexlScript script = JEXL.createScript(src, "c");
    } catch(JexlException.Parsing xvar) {
      Assert.assertTrue(xvar.getMessage().contains("x: variable is already declared"));
    }
  }

  @Test
  public void testRedefinition1() {
    String src = "const x = 33; try(let x = c) { 169; }";
    try {
      JexlScript script = JEXL.createScript(src, "c");
    } catch(JexlException.Parsing xvar) {
      Assert.assertTrue(xvar.getMessage().contains("x: variable is already declared"));
    }
  }

  @Ignore
  public void testEdgeTry() throws Exception {
    int i = 0;
    while(i++ < 5) {
      System.out.println("i: " + i);
      try {
        throw new JexlException.Continue(null);
      } finally {
        continue;
      }
    }
    System.out.println("iii: " + i);

    //int x = 0;
    try(AutoCloseable x = new Circuit()) {

    }
  }
}
