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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ASTSwitchStatement extends JexlNode {
  /**
   * Whether this switch is a statement (true) or an expression (false).
   */
  protected boolean isStatement = true;
  /**
   * The map of cases, where the key is the case value and the value is the switch index.
   */
  protected Map<Object, Integer> cases = Collections.emptyMap();

  public ASTSwitchStatement(final int id) {
    super(id);
  }

  @Override
  public Object jjtAccept(final ParserVisitor visitor, final Object data) {
    return visitor.visit(this, data);
  }

  /**
   * Returns the array of cases values.
   * <p>Meant only for verification during tests.</p>
   * The list at each index contains the case values for that index.
   * If the values-list is empty for an index, it is the default case.
   *
   * @return an array of case values
   */
  public List<Object>[] getCasesList() {
    @SuppressWarnings("unchecked")
    final List<Object>[] list = new List[jjtGetNumChildren() -1];
    for (final Map.Entry<Object, Integer> entry : cases.entrySet()) {
      final int index = entry.getValue();
      if (index < 0 || index >= list.length) {
        throw new IndexOutOfBoundsException("switch index out of bounds: " + index);
      }
      List<Object> values = list[index];
      if (values == null) {
        list[index] = values = new ArrayList<>();
      }
      values.add(entry.getValue());
    }
    return list;
  }

  public boolean isStatement() {
    return isStatement;
  }

  public int switchIndex(final Object value) {
    final Object code = JexlParser.switchCode(value);
    Integer index = cases.get(code);
    if (index == null) {
      index = cases.get(JexlParser.DFLT);
    }
    if (index != null && index >= 1 && index < jjtGetNumChildren()) {
      return index; // index is 1-based, children are 0-based
    }
    return -1;
  }

  /**
   * Helper for switch statements.
   * <p>It detects duplicates cases and default.</p>
   */
  public static class Helper {
    private int switchIndex = 1; // switch index, starts at 1 since the first child is the switch expression
    private boolean defaultDefined;
    private final Map<Object, Integer> dispatch = new LinkedHashMap<>();

    void defineCase(final JexlParser.SwitchSet switchSet) throws ParseException {
      if (switchSet.isEmpty()) {
        if (defaultDefined) {
          throw new ParseException("default clause is already defined");
        } else {
          defaultDefined = true;
          dispatch.put(JexlParser.DFLT, switchIndex);
        }
      } else {
        for (final Object constant : switchSet) {
          if (dispatch.put(constant == null ? JexlParser.NIL : constant, switchIndex) != null) {
            throw new ParseException("duplicate case in switch statement for value: " + constant);
          }
        }
        switchSet.clear();
      }
      switchIndex += 1;
    }

    void defineSwitch(ASTSwitchStatement statement) {
      statement.cases = dispatch;
    }
  }

}
