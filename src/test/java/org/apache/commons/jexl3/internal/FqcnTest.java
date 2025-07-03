/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.jexl3.internal;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlTestCase;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;



public class FqcnTest extends JexlTestCase {
  public static final int FORTYTWO = 42;
  public FqcnTest() {
    super("FqcnTest");
  }

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    // ensure jul logging is only error
    java.util.logging.Logger.getLogger(org.apache.commons.jexl3.JexlEngine.class.getName()).setLevel(java.util.logging.Level.SEVERE);
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public enum FqcnScope {
    UNDEFINED, UNDECLARED, GLOBAL, LOCAL, THIS, SUPER;
  }

  Object getConstant(List<String> imports, String name) throws Exception {
    JexlUberspect uber = new Uberspect(null, null, JexlPermissions.UNRESTRICTED);
    final FqcnResolver resolver = new FqcnResolver(uber, imports);
    return resolver.resolveConstant(name);
  }

  @Test
  void testBadImport0() {
    List<String> imports = Collections.singletonList("org.apache.commons.jexl4242");
    assertThrows(JexlException.class, () -> new JexlBuilder().imports(imports).create());
  }

  @Test
  void testBadImport1() {
    // permissions will not allow this import
    List<String> imports = Collections.singletonList("org.apache.commons.jexl.JexlEngine");
    assertThrows(JexlException.class, () -> new JexlBuilder().imports(imports).create());
  }

  @Test
  public void testFqcn() throws Exception {
    List<String> imports = Arrays.asList("org.apache.commons.jexl3.internal.FqcnTest", "org.apache.commons.jexl3.internal", "java.lang");
    Object c = getConstant(imports, "FqcnScope.UNDEFINED");
    assertNotNull(c);
    assertEquals(FqcnScope.UNDEFINED, c);
    c = getConstant(imports, "FqcnScope.SUPER");
    assertEquals(FqcnScope.SUPER, c);
    c = getConstant(imports, "FqcnScope.SUPER");
    assertEquals(FqcnScope.SUPER, c);
    c = getConstant(imports, "FqcnTest.FORTYTWO");
    assertEquals(42, c);
  }
}
