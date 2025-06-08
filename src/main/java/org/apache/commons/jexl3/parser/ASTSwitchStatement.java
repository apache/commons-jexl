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
  /** Pointless serial UID */
  private static final long serialVersionUID = 1L;

  /**
   * The map of cases, where the key is the case value and the value is the switch index.
   */
  protected transient Map<Object, Integer> cases = Collections.emptyMap();

  public ASTSwitchStatement(int id) {
    super(id);
  }

  @Override
  public Object jjtAccept(ParserVisitor visitor, Object data) {
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
    List<Object>[] list = (List<Object>[]) new List[jjtGetNumChildren() -1];
    for (Map.Entry<Object, Integer> entry : cases.entrySet()) {
      int index = entry.getValue();
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

  @SuppressWarnings("unchecked")
  public void setCases(Map cases) {
    this.cases = cases == null ? Collections.emptyMap() : (Map<Object, Integer>) cases;
  }

  Map<Object, Integer> getCases() {
    return cases;
  }

  public int switchIndex(Object value) {
    Object code = JexlParser.switchCode(value);
    Integer index = cases.get(code);
    if (index == null) {
      index = cases.get(JexlParser.DFLT);
    }
    if (index != null && index >= 0 && index < jjtGetNumChildren()) {
      return index; // index is 1-based, children are 0-based
    }
    return -1;
  }

  /**
   * Helper for switch statements.
   * <p>It detects duplicates cases and default.</p>
   */
  public static class Helper {
    private int nswitch = 0;
    private boolean defaultDefined = false;
    private final Map<Object, Integer> dispatch = new LinkedHashMap<>();

    void defineCase(JexlParser.SwitchSet constants) throws ParseException {
      if (constants.isEmpty()) {
        if (defaultDefined) {
          throw new ParseException("default clause is already defined");
        } else {
          defaultDefined = true;
          dispatch.put(JexlParser.DFLT, nswitch);
        }
      } else {
        for (Object constant : constants) {
          if (dispatch.put(constant == null ? JexlParser.NIL : constant, nswitch) != null) {
            throw new ParseException("duplicate case in switch statement for value: " + constant);
          }
        }
        constants.clear();
      }
      nswitch += 1;
    }

    void defineSwitch(ASTSwitchStatement statement) {
      statement.cases = dispatch;
    }
  }

}
