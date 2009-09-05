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
package org.apache.commons.jexl.util.introspection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.jexl.util.Introspector;
import org.apache.commons.jexl.util.ListGetExecutor;
import org.apache.commons.jexl.util.ListSetExecutor;
import org.apache.commons.jexl.util.MapGetExecutor;
import org.apache.commons.jexl.util.MapSetExecutor;

/**
 * Tests for checking introspection discovery.
 * 
 * @since 2.0
 */
public class DiscoveryTest extends TestCase {

    public void testListIntrospection() throws Exception {
        Uberspect uber = Introspector.getUberspect();
        Introspector intro = (Introspector) uber;
        List<Object> list = new ArrayList<Object>();

        assertTrue(intro.getGetExecutor(list, "1") instanceof ListGetExecutor);
        assertTrue(intro.getSetExecutor(list, "1", "foo") instanceof ListSetExecutor);

        assertTrue(uber.getPropertyGet(list, "1", null) instanceof ListGetExecutor);
        assertTrue(uber.getPropertySet(list, "1", "foo", null) instanceof ListSetExecutor);
    }

    public void testMapIntrospection() throws Exception {
        Uberspect uber = Introspector.getUberspect();
        Introspector intro = (Introspector) uber;
        Map<String, Object> list = new HashMap<String, Object>();

        assertTrue(intro.getGetExecutor(list, "foo") instanceof MapGetExecutor);
        assertTrue(intro.getSetExecutor(list, "foo", "bar") instanceof MapSetExecutor);

        assertTrue(uber.getPropertyGet(list, "foo", null) instanceof MapGetExecutor);
        assertTrue(uber.getPropertySet(list, "foo", "bar", null) instanceof MapSetExecutor);
    }

}
