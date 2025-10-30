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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ASTCaseStatement extends JexlNode {
  /** Pointless serial UID */
  private static final long serialVersionUID = 1L;

  /** The values of the case statement. */
  protected List<Object> values = Collections.emptyList();

  public ASTCaseStatement(final int id) {
    super(id);
  }

  @Override
  public Object jjtAccept(final ParserVisitor visitor, final Object data) {
    return visitor.visit(this, data);
  }

  public void setValue(final Object value) {
    if (value == null) {
      this.values = Collections.emptyList();
    } else {
      this.values = Collections.singletonList(value);
    }
  }

  public void setValues(final List<Object> values) {
    if (values == null) {
      this.values = Collections.emptyList();
    } else if (values.size() == 1) {
      this.values = Collections.singletonList(values.get(0));
    } else {
      this.values = new ArrayList<>(values);
    }
  }

  public List<Object> getValues() {
    return Collections.unmodifiableList(values);
  }
}
