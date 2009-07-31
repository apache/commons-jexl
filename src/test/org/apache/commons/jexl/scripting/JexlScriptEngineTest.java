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

package org.apache.commons.jexl.scripting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import junit.framework.TestCase;

public class JexlScriptEngineTest extends TestCase {
    
    public void testScripting() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        assertNotNull("Manager should not be null", manager);
        ScriptEngine engine = manager.getEngineByName("jexl");
        assertNotNull("Engine should not be null (name)", engine);
        engine = manager.getEngineByExtension("jexl");
        assertNotNull("Engine should not be null (ext)", engine);
        final Integer initialValue = Integer.valueOf(123);
        assertEquals(initialValue,engine.eval("123"));
        assertEquals(initialValue,engine.eval("0;123"));// multiple statements
        long time1 = System.currentTimeMillis();
        Long time2 = (Long) engine.eval("" +
        		"sys=context.class.forName(\"java.lang.System\");"
        		+"now=sys.currentTimeMillis();"
        		);
        assertTrue("Must take some time to process this",time1 <= time2.longValue());
        engine.put("value", initialValue);
        assertEquals(initialValue,engine.get("value"));
        final Long newValue = Long.valueOf(124);
        assertEquals(newValue,engine.eval("old=value;value=value+1"));
        assertEquals(initialValue,engine.get("old"));
        assertEquals(newValue,engine.get("value"));
    }

}
