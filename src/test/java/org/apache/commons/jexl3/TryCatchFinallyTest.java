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

import java.io.Closeable;
import java.io.IOException;

public class TryCatchFinallyTest extends JexlTestCase {
  public TryCatchFinallyTest() {
    super(TryCatchFinallyTest.class.getSimpleName());
  }

  @Test
  public void testForm0x14() {
    String src = "try { 42; } finally { 169; }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    Object result = script.execute(null);
    Assert.assertEquals(42, result);
  }

  @Test
  public void testForm0x17() {
    String src = "try(let x = 42) { x; } finally { 169; }";
    JexlScript script = JEXL.createScript(src);
    Assert.assertNotNull(script);
    Object result = script.execute(null);
    Assert.assertEquals(42, result);
  }

  public static class Circuit implements Closeable {
    boolean opened = true;

    public boolean isOpened() {
      return opened;
    }
    @Override
    public void close() throws IOException {
      opened = false;
    }
  }

  @Test
  public void testForm0x17a() {
    String src = "try(let x = c) { c.isOpened()? 42 : -42; } finally { 169; }";
    JexlScript script = JEXL.createScript(src, "c");
    Circuit circuit = new Circuit();
    Assert.assertNotNull(script);
    Object result = script.execute(null, circuit);
    Assert.assertEquals(42, result);
    Assert.assertFalse(circuit.isOpened());
  }


//  @Test
//  public void testEdgeTry() {
//    int i = 0;
//    while(i++ < 5) {
//      System.out.println("i: " + i);
//      try {
//        throw new JexlException.Continue(null);
//      } finally {
//        continue;
//      }
//    }
//    System.out.println("iii: " + i);
//  }
}
