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
package org.apache.commons.jexl3.parser;

/**
 * Array access supporting (optional) safe notation.
 */
public class ASTArrayAccess extends JexlLexicalNode {
  private static final long serialVersionUID = 1L;
  /** Which children are accessed using a safe notation.
   * Note that this does not really work after the 64th child.
   * However, an expression like 'a?[b]?[c]?...?[b0]' with 64 terms is very unlikely
   * to occur in real life and a bad idea anyhow.
   */
  private long safe;

  public ASTArrayAccess(final int id) {
    super(id);
  }

  public ASTArrayAccess(final Parser p, final int id) {
    super(id);
  }

  public boolean isSafeChild(final int c) {
    return (safe & 1L << c) != 0;
  }

  @Override
  public boolean isSafeLhs(boolean safe) {
    return isSafeChild(0) || super.isSafeLhs(safe);
  }

  @Override
  public Object jjtAccept(final ParserVisitor visitor, final Object data) {
    return visitor.visit(this, data);
  }

  void setSafe(final long s) {
    this.safe = s;
  }
}
