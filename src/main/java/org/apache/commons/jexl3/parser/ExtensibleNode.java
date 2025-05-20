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

import org.apache.commons.jexl3.internal.Debugger;

/**
 * Base node for array/set/map literals.
 * <p>Captures constness and extensibility (...)</p>
 */
public class ExtensibleNode extends JexlNode {
  /** Whether this array/set/map is constant or not. */
  protected boolean constant;
  /** Whether this array/set/map is extended or not. */
  private boolean extended;

  public ExtensibleNode(final int id) {
    super(id);
  }

  @Override
  protected boolean isConstant(final boolean literal) {
    return constant;
  }

  public boolean isExtended() {
    return extended;
  }

  @Override
  public void jjtClose() {
    constant = super.isConstant(true);
  }

  public void setExtended(final boolean e) {
    this.extended = e;
  }

  @Override
  public String toString() {
    final Debugger dbg = new Debugger();
    return dbg.data(this);
  }

}
