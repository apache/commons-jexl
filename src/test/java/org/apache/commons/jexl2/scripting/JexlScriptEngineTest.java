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

package org.apache.commons.jexl2.scripting;

import java.io.Reader;
import java.util.Arrays;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import junit.framework.TestCase;

public class JexlScriptEngineTest extends TestCase {

    public void testScriptEngineFactory() throws Exception {
        JexlScriptEngineFactory factory = new JexlScriptEngineFactory();
        assertEquals("JEXL Engine", factory.getParameter(ScriptEngine.ENGINE));
        assertEquals("2.0", factory.getParameter(ScriptEngine.ENGINE_VERSION)); // 2.1 ; version 2.0 supports jexl2
        assertEquals("JEXL", factory.getParameter(ScriptEngine.LANGUAGE));
        assertEquals("2.0", factory.getParameter(ScriptEngine.LANGUAGE_VERSION));
        assertEquals(Arrays.asList("JEXL", "Jexl", "jexl", "JEXL2", "Jexl2", "jexl2"), factory.getParameter(ScriptEngine.NAME));
        assertNull(factory.getParameter("THREADING"));

        assertEquals(Arrays.asList("jexl", "jexl2"), factory.getExtensions());
        assertEquals(Arrays.asList("application/x-jexl", "application/x-jexl2"), factory.getMimeTypes());

        assertEquals("42;", factory.getProgram(new String[]{"42"}));
        assertEquals("str.substring(3,4)", factory.getMethodCallSyntax("str", "substring", new String[]{"3", "4"}));
    }

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
        Long time2 = (Long) engine.eval(
             "sys=context.class.forName(\"java.lang.System\");"
            +"now=sys.currentTimeMillis();"
            );
        assertTrue("Must take some time to process this",time1 <= time2.longValue());
        engine.put("value", initialValue);
        assertEquals(initialValue,engine.get("value"));
        final Integer newValue = Integer.valueOf(124);
        assertEquals(newValue,engine.eval("old=value;value=value+1"));
        assertEquals(initialValue,engine.get("old"));
        assertEquals(newValue,engine.get("value"));
        assertEquals(engine.getContext(),engine.get(JexlScriptEngine.CONTEXT_KEY));
        // Check behaviour of JEXL object
        assertEquals(engine.getContext().getReader(),engine.eval("JEXL.in"));
        assertEquals(engine.getContext().getWriter(),engine.eval("JEXL.out"));
        assertEquals(engine.getContext().getErrorWriter(),engine.eval("JEXL.err"));
        assertEquals(System.class,engine.eval("JEXL.System"));
    }
    
    public void testNulls() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        assertNotNull("Manager should not be null", manager);
        ScriptEngine engine = manager.getEngineByName("jexl");
        assertNotNull("Engine should not be null (name)", engine);
        try {
            engine.eval((String)null);
            fail("Should have caused NPE");
        } catch (NullPointerException e) {
            // NOOP
        }
        try {
            engine.eval((Reader)null);
            fail("Should have caused NPE");
        } catch (NullPointerException e) {
            // NOOP
        }
    }

    public void testEngineNames() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        assertNotNull("Manager should not be null", manager);
        ScriptEngine engine = manager.getEngineByName("JEXL");
        assertNotNull("Engine should not be null (JEXL)", engine);        
        engine = manager.getEngineByName("Jexl");
        assertNotNull("Engine should not be null (Jexl)", engine);        
        engine = manager.getEngineByName("jexl");
        assertNotNull("Engine should not be null (jexl)", engine);        
    }

    public void testScopes() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        assertNotNull("Manager should not be null", manager);
        ScriptEngine engine = manager.getEngineByName("JEXL");
        assertNotNull("Engine should not be null (JEXL)", engine);
        manager.put("global",Integer.valueOf(1));
        engine.put("local", Integer.valueOf(10));
        manager.put("both",Integer.valueOf(7));
        engine.put("both", Integer.valueOf(7));
        engine.eval("local=local+1");
        engine.eval("global=global+1");
        engine.eval("both=both+1"); // should update engine value only
        engine.eval("newvar=42;");
        assertEquals(Integer.valueOf(2),manager.get("global"));
        assertEquals(Integer.valueOf(11),engine.get("local"));
        assertEquals(Integer.valueOf(7),manager.get("both"));
        assertEquals(Integer.valueOf(8),engine.get("both"));
        assertEquals(Integer.valueOf(42),engine.get("newvar"));
        assertNull(manager.get("newvar"));
    }

    public void testDottedNames() throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        assertNotNull("Manager should not be null", manager);
        ScriptEngine engine = manager.getEngineByName("JEXL");
        assertNotNull("Engine should not be null (JEXL)", engine);
        engine.eval("this.is.a.test=null");
        assertNull(engine.get("this.is.a.test"));
        assertEquals(Boolean.TRUE, engine.eval("empty(this.is.a.test)"));
        final Object mymap = engine.eval("testmap={ 'key1' : 'value1', 'key2' : 'value2' }");
        assertTrue(mymap instanceof Map<?, ?>);
        assertEquals(2,((Map<?, ?>)mymap).size());
    }
}
