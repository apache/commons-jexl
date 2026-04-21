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

package org.apache.commons.jexl3;

import java.util.Arrays;
import org.apache.commons.jexl3.internal.Debugger;
import org.apache.commons.jexl3.internal.OptionsContext;
import org.apache.commons.jexl3.internal.Util;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;


/**
 * Implements runTest methods to dynamically instantiate and invoke a test,
 * wrapping the call with setUp(), tearDown() calls.
 * Eases the implementation of main methods to debug.
 */
public class JexlTestCase {
  /**
   * The restricted permissions that are used by default in JEXL tests.
   * <p>Need to add jexl3 package but removes most internals</p>
   */
  public static final JexlPermissions TEST_PERMS = JexlPermissions.RESTRICTED.compose(
"org.apache.commons.jexl3.*",
      "org.apache.commons.jexl3 +{ -JexlBuilder{} }",
      "org.apache.commons.jexl3.introspection -{ JexlPermissions{} JexlPermissions$ClassPermissions{} }",
      "org.apache.commons.jexl3.internal -{ Engine{} Engine32{} TemplateEngine{} }",
      "org.apache.commons.jexl3.internal.introspection -{ Uberspect{} Introspector{} }",
      "java.net -{ +URI { -toURL() } }");

  // define mode pro50
  static final JexlOptions MODE_PRO50 = new JexlOptions();

  // The default options: all tests where engine lexicality is
  // important can be identified by the builder  calling lexical(...).
  static {
    JexlBuilder.setDefaultPermissions(TEST_PERMS);
    JexlOptions.setDefaultFlags("-safe", "+lexical");
    MODE_PRO50.setFlags("+strict +cancellable +lexical +lexicalShade -safe".split(" "));
  }

  /**
   * A default JEXL engine instance.
   */
  protected final JexlEngine JEXL;

  public JexlTestCase(final String name) {
    this(name, new JexlBuilder().imports(Arrays.asList("java.lang", "java.math")).permissions(null).cache(128).create());
  }

  protected JexlTestCase(final String name, final JexlEngine jexl) {
    //super(name);
    JEXL = jexl;
  }

  static JexlEngine createEngine() {
    return new JexlBuilder().create();
  }

  public static JexlEngine createEngine(final boolean lenient) {
    return createEngine(lenient, TEST_PERMS);
  }

  public static JexlEngine createEngine(final boolean lenient, final JexlPermissions permissions) {
    return new JexlBuilder().uberspect(new Uberspect(null, null, permissions)).arithmetic(new JexlArithmetic(!lenient)).cache(128).create();
  }

  static JexlEngine createEngine(final JexlFeatures features) {
    return new JexlBuilder().features(features).create();
  }

  /**
   * Will force testing the debugger for each derived test class by
   * recreating each expression from the JexlNode in the JexlEngine cache &
   * testing them for equality with the origin.
   *
   * @throws Exception
   */
  public static void debuggerCheck(final JexlEngine ijexl) throws Exception {
    Util.debuggerCheck(ijexl);
  }

  /**
   * Compare strings ignoring white space differences.
   * <p>This replaces any sequence of whitespaces (ie \\s) by one space (ie ASCII 32) in both
   * arguments then compares them.</p>
   *
   * @param lhs left hand side
   * @param rhs right hand side
   * @return true if strings are equal besides whitespace
   */
  public static boolean equalsIgnoreWhiteSpace(final String lhs, final String rhs) {
    final String lhsw = lhs.trim().replaceAll("\\s+", "");
    final String rhsw = rhs.trim().replaceAll("\\s+", "");
    return lhsw.equals(rhsw);
  }

  public static String toString(final JexlScript script) {
    final Debugger d = new Debugger().lineFeed("").indentation(0);
    d.debug(script);
    return d.toString();
  }

  @BeforeAll
  static void setUpClass() {
    JexlBuilder.setDefaultPermissions(TEST_PERMS);
  }

  public String simpleWhitespace(final String arg) {
    return arg.trim().replaceAll("\\s+", " ");
  }

  public void setUp() throws Exception {
    // nothing to do
  }

  @AfterEach
  public void tearDown() throws Exception {
    debuggerCheck(JEXL);
  }

  public static class PragmaticContext extends OptionsContext implements JexlContext.PragmaProcessor, JexlContext.OptionsHandle {
    private final JexlOptions options;

    public PragmaticContext() {
      this(new JexlOptions());
    }

    public PragmaticContext(final JexlOptions o) {
      this.options = o;
    }

    @Override
    public JexlOptions getEngineOptions() {
      return options;
    }

    @Override
    public void processPragma(final JexlOptions opts, final String key, final Object value) {
      if ("script.mode".equals(key) && "pro50".equals(value)) {
        opts.set(MODE_PRO50);
      }
    }

    @Override
    public void processPragma(final String key, final Object value) {
      processPragma(null, key, value);
    }
  }
}
