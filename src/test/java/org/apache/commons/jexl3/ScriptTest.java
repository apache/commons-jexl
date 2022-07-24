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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for JexlScript
 * @since 1.1
 */
@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
public class ScriptTest extends JexlTestCase {
    static final String TEST1 =  "src/test/scripts/test1.jexl";
    static final String TEST_ADD =  "src/test/scripts/testAdd.jexl";
    static final String TEST_JSON =  "src/test/scripts/httpPost.jexl";

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
        public void setCode(final String c) {
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
     * Test creating a script from spaces.
     */
    @Test
    public void testSpacesScript() {
        final String code = " ";
        final JexlScript s = JEXL.createScript(code);
        Assert.assertNotNull(s);
    }

    /**
     * Test creating a script from a string.
     */
    @Test
    public void testSimpleScript() {
        final String code = "while (x < 10) x = x + 1;";
        final JexlScript s = JEXL.createScript(code);
        final JexlContext jc = new MapContext();
        jc.set("x",1);

        final Object o = s.execute(jc);
        Assert.assertEquals("Result is wrong", 10, o);
        Assert.assertEquals("getText is wrong", code, s.getSourceText());
    }

    @Test
    public void testScriptJsonFromFileJexl() {
        final File testScript = new File(TEST_JSON);
        final JexlScript s = JEXL.createScript(testScript);
        final JexlContext jc = new MapContext();
        jc.set("httpr", new HttpPostRequest());
        Object result = s.execute(jc);
        Assert.assertNotNull(result);
        Assert.assertEquals("{  \"id\": 101}", result);
    }

    @Test
    public void testScriptJsonFromFileJava() {
        final String testScript ="httpr.execute('https://jsonplaceholder.typicode.com/posts', null)";
        final JexlScript s = JEXL.createScript(testScript);
        final JexlContext jc = new MapContext();
        jc.set("httpr", new HttpPostRequest());
        Object result = s.execute(jc);
        Assert.assertNotNull(result);
        Assert.assertEquals("{  \"id\": 101}", result);
    }

    /**
     * An object to call from.
     */
    public static class HttpPostRequest {
        public static String execute(String url, String data) throws IOException {
            return httpPostRequest(url, data);
        }
    }

    /**
     *  HTTP post.
     * @param sURL the url
     * @param jsonData some json data
     * @return the result
     * @throws IOException
     */
    private static String httpPostRequest(String sURL, String jsonData) throws IOException {
        URL url = new java.net.URL(sURL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json");
        // send data
        if ( jsonData != null ) {
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setDoOutput(true);

            OutputStream outputStream = con.getOutputStream();
            byte[] input = jsonData.getBytes("utf-8");
            outputStream.write(input, 0, input.length);
        }
        // read response
        int responseCode = con.getResponseCode();
        InputStream inputStream = null;
        inputStream =  con.getInputStream();
        StringBuffer response = new java.lang.StringBuffer();
        if (inputStream != null) {
            try (BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream))) {
                String inputLine = "";
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
        }
        return response.toString();
    }


    @Test
    public void testScriptFromFile() {
        final File testScript = new File(TEST1);
        final JexlScript s = JEXL.createScript(testScript);
        final JexlContext jc = new MapContext();
        jc.set("out", System.out);
        final Object result = s.execute(jc);
        Assert.assertNotNull("No result", result);
        Assert.assertEquals("Wrong result", 7, result);
    }

    @Test
    public void testArgScriptFromFile() {
        final File testScript = new File(TEST_ADD);
        final JexlScript s = JEXL.createScript(testScript,"x", "y");
        final JexlContext jc = new MapContext();
        jc.set("out", System.out);
        final Object result = s.execute(jc, 13, 29);
        Assert.assertNotNull("No result", result);
        Assert.assertEquals("Wrong result", 42, result);
    }

    @Test
    public void testScriptFromURL() throws Exception {
        final URL testUrl = new File(TEST1).toURI().toURL();
        final JexlScript s = JEXL.createScript(testUrl);
        final JexlContext jc = new MapContext();
        jc.set("out", System.out);
        final Object result = s.execute(jc);
        Assert.assertNotNull("No result", result);
        Assert.assertEquals("Wrong result", 7, result);
    }

    @Test
    public void testArgScriptFromURL() throws Exception {
        final URL testUrl = new File(TEST_ADD).toURI().toURL();
        final JexlScript s = JEXL.createScript(testUrl,"x", "y");
        final JexlContext jc = new MapContext();
        jc.set("out", System.out);
        final Object result = s.execute(jc, 13, 29);
        Assert.assertNotNull("No result", result);
        Assert.assertEquals("Wrong result", 42, result);
    }

    @Test
    public void testScriptUpdatesContext() {
        final String jexlCode = "resultat.setCode('OK')";
        final JexlExpression e = JEXL.createExpression(jexlCode);
        final JexlScript s = JEXL.createScript(jexlCode);

        final Tester resultatJexl = new Tester();
        final JexlContext jc = new MapContext();
        jc.set("resultat", resultatJexl);

        resultatJexl.setCode("");
        e.evaluate(jc);
        Assert.assertEquals("OK", resultatJexl.getCode());
        resultatJexl.setCode("");
        s.execute(jc);
        Assert.assertEquals("OK", resultatJexl.getCode());
    }

}
