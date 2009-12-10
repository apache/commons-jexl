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
package org.apache.commons.jexl;

import org.apache.commons.jexl2.JexlEngine;

/**
 * <p>
 * Creates {@link Script}s.  To create a JEXL Script, pass
 * valid JEXL syntax to the static createScript() method:
 * </p>
 *
 * <pre>
 * String jexl = "y = x * 12 + 44; y = y * 4;";
 * Script script = ScriptFactory.createScript( jexl );
 * </pre>
 *
 * <p>
 * When an {@link Script} is created, the JEXL syntax is
 * parsed and verified.
 * </p>
 *
 * <p>
 * This is a convenience class; using an instance of a {@link JexlEngine}
 * that serves the same purpose with more control is recommended.
 * </p>
 * @since 1.1
 * @version $Id: ScriptFactory.java 884175 2009-11-25 16:23:41Z henrib $
 * @deprecated Create a JexlEngine and use the createScript method on that instead.
 */
@Deprecated
public final class ScriptFactory extends JexlOne {}

