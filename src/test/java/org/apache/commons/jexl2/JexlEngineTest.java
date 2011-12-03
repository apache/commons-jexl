/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.apache.commons.jexl2;

import junit.framework.TestCase;

public class JexlEngineTest extends TestCase {
    
    public void testEngine() {
        JexlEngine je;
        je = new JexlEngine();
        assertNotNull(je);
        je.createInfo("test", 1, 2); // fails with NoSuchMethodError on 2.1 currently
    }
    
    public void testExtend() {
        JexlEngine je;
        je = new JexlEngine() {
            @Override
            protected
            JexlInfo createInfo(String s, int i , int j){
                return new DebugInfo(s, i, j);
            }
        };
        assertNotNull(je);
        je.createInfo("test", 1, 2); // fails with NoSuchMethodError on 2.1 currently
    }

}
