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

import java.util.Set;

import org.apache.commons.jexl3.JexlInfo;

/**
 * A JexlInfo that also carries the set of tokens that should be ignored during parsing,
 * most notably the prefix for templates.
 */
public class TemplateInfo extends JexlInfo {
  private final Set<String> ignoredTokens;

  public TemplateInfo(final JexlInfo info, final Set<String> ignoredTokens) {
    super(info);
    this.ignoredTokens = ignoredTokens;
  }

  public Set<String> getIgnoredTokens() {
    return ignoredTokens;
  }
}
