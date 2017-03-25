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
package org.apache.commons.jexl3;

import java.io.File;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for JexlScript
 * @since 1.1
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class ScriptTest extends JexlTestCase {
    static final String TEST1 =  "src/test/scripts/test1.jexl";
    static final String TEST_ADD =  "src/test/scripts/testAdd.jexl";

    // test class for testScriptUpdatesContext
    // making this class private static will cause the test to fail.
    // this is due to unusual code in ClassMap.getAccessibleMethods(Class)
    // that treats non-public classes in a specific way. Why getAccessibleMethods
    // does this is not known yet.
    public static class Tester {
        private String code;
        public String getCode () {
            return code;
        }
        public void setCode(String c) {
            code = c;
        }
    }
    /**
     * Create a new test case.
     */
    public ScriptTest() {
        super("ScriptTest");
    }

    /**
     * Test creating a script from a string.
     */
    @Test
    public void testSimpleScript() throws Exception {
        String code = "while (x < 10) x = x + 1;";
        JexlScript s = JEXL.createScript(code);
        JexlContext jc = new MapContext();
        jc.set("x", new Integer(1));

        Object o = s.execute(jc);
        Assert.assertEquals("Result is wrong", new Integer(10), o);
        Assert.assertEquals("getText is wrong", code, s.getSourceText());
    }

    @Test
    public void testScriptFromFile() throws Exception {
        File testScript = new File(TEST1);
        JexlScript s = JEXL.createScript(testScript);
        JexlContext jc = new MapContext();
        jc.set("out", System.out);
        Object result = s.execute(jc);
        Assert.assertNotNull("No result", result);
        Assert.assertEquals("Wrong result", new Integer(7), result);
    }

    @Test
    public void testArgScriptFromFile() throws Exception {
        File testScript = new File(TEST_ADD);
        JexlScript s = JEXL.createScript(testScript,new String[]{"x","y"});
        JexlContext jc = new MapContext();
        jc.set("out", System.out);
        Object result = s.execute(jc, 13, 29);
        Assert.assertNotNull("No result", result);
        Assert.assertEquals("Wrong result", new Integer(42), result);
    }

    @Test
    public void testScriptFromURL() throws Exception {
        URL testUrl = new File(TEST1).toURI().toURL();
        JexlScript s = JEXL.createScript(testUrl);
        JexlContext jc = new MapContext();
        jc.set("out", System.out);
        Object result = s.execute(jc);
        Assert.assertNotNull("No result", result);
        Assert.assertEquals("Wrong result", new Integer(7), result);
    }

    @Test
    public void testArgScriptFromURL() throws Exception {
        URL testUrl = new File(TEST_ADD).toURI().toURL();
        JexlScript s = JEXL.createScript(testUrl,new String[]{"x","y"});
        JexlContext jc = new MapContext();
        jc.set("out", System.out);
        Object result = s.execute(jc, 13, 29);
        Assert.assertNotNull("No result", result);
        Assert.assertEquals("Wrong result", new Integer(42), result);
    }

    @Test
    public void testScriptUpdatesContext() throws Exception {
        String jexlCode = "resultat.setCode('OK')";
        JexlExpression e = JEXL.createExpression(jexlCode);
        JexlScript s = JEXL.createScript(jexlCode);

        Tester resultatJexl = new Tester();
        JexlContext jc = new MapContext();
        jc.set("resultat", resultatJexl);

        resultatJexl.setCode("");
        e.evaluate(jc);
        Assert.assertEquals("OK", resultatJexl.getCode());
        resultatJexl.setCode("");
        s.execute(jc);
        Assert.assertEquals("OK", resultatJexl.getCode());
    }

}
