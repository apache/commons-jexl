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

package org.apache.commons.jexl3.scripting;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.junit.Assert;
import org.junit.Test;

public class JexlScriptEngineTest {
    private static final List<String> NAMES = Arrays.asList("JEXL", "Jexl", "jexl",
                                                            "JEXL2", "Jexl2", "jexl2",
                                                            "JEXL3", "Jexl3", "jexl3");
    private static final List<String> EXTENSIONS = Arrays.asList("jexl", "jexl2", "jexl3");
    private static final List<String> MIMES = Arrays.asList("application/x-jexl",
                                                            "application/x-jexl2",
                                                            "application/x-jexl3");

    @Test
    public void testScriptEngineFactory() throws Exception {
        final JexlScriptEngineFactory factory = new JexlScriptEngineFactory();
        Assert.assertEquals("JEXL Engine", factory.getParameter(ScriptEngine.ENGINE));
        Assert.assertEquals("3.2", factory.getParameter(ScriptEngine.ENGINE_VERSION));
        Assert.assertEquals("JEXL", factory.getParameter(ScriptEngine.LANGUAGE));
        Assert.assertEquals("3.2", factory.getParameter(ScriptEngine.LANGUAGE_VERSION));
        Assert.assertNull(factory.getParameter("THREADING"));
        Assert.assertEquals(NAMES, factory.getParameter(ScriptEngine.NAME));
        Assert.assertEquals(EXTENSIONS, factory.getExtensions());
        Assert.assertEquals(MIMES, factory.getMimeTypes());

        Assert.assertEquals("42;", factory.getProgram("42"));
        Assert.assertEquals("str.substring(3,4)", factory.getMethodCallSyntax("str", "substring", "3", "4"));
    }

    @Test
    public void testScriptingGetBy() throws Exception {
        final ScriptEngineManager manager = new ScriptEngineManager();
        Assert.assertNotNull("Manager should not be null", manager);
        for (final String name : NAMES) {
            final ScriptEngine engine = manager.getEngineByName(name);
            Assert.assertNotNull("Engine should not be null (name)", engine);
        }
        for (final String extension : EXTENSIONS) {
            final ScriptEngine engine = manager.getEngineByExtension(extension);
            Assert.assertNotNull("Engine should not be null (extension)", engine);
        }
        for (final String mime : MIMES) {
            final ScriptEngine engine = manager.getEngineByMimeType(mime);
            Assert.assertNotNull("Engine should not be null (mime)", engine);
        }
    }

    @Test
    public void testScripting() throws Exception {
        final ScriptEngineManager manager = new ScriptEngineManager();
        Assert.assertNotNull("Manager should not be null", manager);
        final ScriptEngine engine = manager.getEngineByName("jexl3");
        final Integer initialValue = 123;
        Assert.assertEquals(initialValue,engine.eval("123"));
        Assert.assertEquals(initialValue,engine.eval("0;123"));// multiple statements
        final long time1 = System.currentTimeMillis();
        final Long time2 = (Long) engine.eval(
             "sys=context.class.forName(\"java.lang.System\");"
            +"now=sys.currentTimeMillis();"
            );
        Assert.assertTrue("Must take some time to process this",time1 <= time2);
        engine.put("value", initialValue);
        Assert.assertEquals(initialValue,engine.get("value"));
        final Integer newValue = 124;
        Assert.assertEquals(newValue,engine.eval("old=value;value=value+1"));
        Assert.assertEquals(initialValue,engine.get("old"));
        Assert.assertEquals(newValue,engine.get("value"));
        Assert.assertEquals(engine.getContext(),engine.get(JexlScriptEngine.CONTEXT_KEY));
        // Check behavior of JEXL object
        Assert.assertEquals(engine.getContext().getReader(),engine.eval("JEXL.in"));
        Assert.assertEquals(engine.getContext().getWriter(),engine.eval("JEXL.out"));
        Assert.assertEquals(engine.getContext().getErrorWriter(),engine.eval("JEXL.err"));
        Assert.assertEquals(System.class,engine.eval("JEXL.System"));
    }

    @Test
    public void testNulls() throws Exception {
        final ScriptEngineManager manager = new ScriptEngineManager();
        Assert.assertNotNull("Manager should not be null", manager);
        final ScriptEngine engine = manager.getEngineByName("jexl3");
        Assert.assertNotNull("Engine should not be null (name)", engine);
        try {
            engine.eval((String)null);
            Assert.fail("Should have caused NPE");
        } catch (final NullPointerException e) {
            // NOOP
        }
        try {
            engine.eval((Reader)null);
            Assert.fail("Should have caused NPE");
        } catch (final NullPointerException e) {
            // NOOP
        }
    }

    @Test
    public void testScopes() throws Exception {
        final ScriptEngineManager manager = new ScriptEngineManager();
        Assert.assertNotNull("Manager should not be null", manager);
        final ScriptEngine engine = manager.getEngineByName("JEXL");
        Assert.assertNotNull("Engine should not be null (JEXL)", engine);
        manager.put("global", 1);
        engine.put("local", 10);
        manager.put("both", 7);
        engine.put("both", 7);
        engine.eval("local=local+1");
        engine.eval("global=global+1");
        engine.eval("both=both+1"); // should update engine value only
        engine.eval("newvar=42;");
        Assert.assertEquals(2,manager.get("global"));
        Assert.assertEquals(11,engine.get("local"));
        Assert.assertEquals(7,manager.get("both"));
        Assert.assertEquals(8,engine.get("both"));
        Assert.assertEquals(42,engine.get("newvar"));
        Assert.assertNull(manager.get("newvar"));
    }

    @Test
    public void testDottedNames() throws Exception {
        final ScriptEngineManager manager = new ScriptEngineManager();
        Assert.assertNotNull("Manager should not be null", manager);
        final ScriptEngine engine = manager.getEngineByName("JEXL");
        Assert.assertNotNull("Engine should not be null (JEXL)", engine);
        engine.eval("this.is.a.test=null");
        Assert.assertNull(engine.get("this.is.a.test"));
        Assert.assertEquals(Boolean.TRUE, engine.eval("empty(this.is.a.test)"));
        final Object mymap = engine.eval("testmap={ 'key1' : 'value1', 'key2' : 'value2' }");
        Assert.assertTrue(mymap instanceof Map<?, ?>);
        Assert.assertEquals(2,((Map<?, ?>)mymap).size());
    }

    @Test
    public void testDirectNew() throws Exception {
        final ScriptEngine engine = new JexlScriptEngine();
        final Integer initialValue = 123;
        Assert.assertEquals(initialValue,engine.eval("123"));
    }
}
