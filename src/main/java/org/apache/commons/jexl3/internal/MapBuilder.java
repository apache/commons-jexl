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
package org.apache.commons.jexl3.internal;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.JexlArithmetic;

/**
 * Helper class to create map literals.
 */
public class MapBuilder implements JexlArithmetic.MapBuilder {
    /** The map being created. */
    protected final Map<Object, Object> map;

    /**
     * Creates a new builder.
     * @param size the expected map size
     */
    public MapBuilder(int size) {
        map = new HashMap<Object, Object>(size);
    }

    @Override
    public void put(Object key, Object value) {
        map.put(key, value);
    }

    @Override
    public Object create() {
        return map;
    }
}
