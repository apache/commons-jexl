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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Tests for JexlScript
 */
@SuppressWarnings({"AssertEqualsBetweenInconvertibleTypes"})
public class ScriptTest extends JexlTestCase {
    /**
     * An object to call from.
     */
    public static class HttpPostRequest {
        public static String execute(final String url, final String data) throws IOException {
            return httpPostRequest(url, data);
        }
    }
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
    static final String TEST1 =  "src/test/scripts/test1.jexl";

    static final String TEST_ADD =  "src/test/scripts/testAdd.jexl";
    static final String TEST_JSON =  "src/test/scripts/httpPost.jexl";

    /**
     * Creates a simple local http server.
     * <p>Only handles POST request on /test</p>
     * @return the server
     * @throws IOException
     */
    static HttpServer createJsonServer(final Function<HttpExchange, String> responder) throws IOException {
        HttpServer server  = null;
        IOException xlatest = null;
        for(int port = 8001; server == null && port < 8127; ++port) {
            try {
                server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
            } catch (final BindException xbind) {
                xlatest = xbind;
            }
        }
        if (server == null) {
            throw xlatest;
        }
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        server.createContext("/test", httpExchange -> {
            if ("POST".equals(httpExchange.getRequestMethod())) {
                try (OutputStream outputStream = httpExchange.getResponseBody()) {
                    final String json = responder.apply(httpExchange);
                    httpExchange.sendResponseHeaders(200, json.length());
                    outputStream.write(json.toString().getBytes());
                    outputStream.flush();
                }
            } else {
                // error
                httpExchange.sendResponseHeaders(500, 0);
            }
        });
        server.setExecutor(threadPoolExecutor);
        server.start();
        return server;
    }

    /**
     *  HTTP post.
     * @param sURL the url
     * @param jsonData some json data
     * @return the result
     * @throws IOException
     */
    private static String httpPostRequest(final String sURL, final String jsonData) throws IOException {
        final URL url = new java.net.URL(sURL);
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/json");
        // send data
        if (jsonData != null) {
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            final OutputStream outputStream = con.getOutputStream();
            final byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            outputStream.write(input, 0, input.length);
        }
        // read response
        final int responseCode = con.getResponseCode();
        InputStream inputStream = null;
        inputStream = con.getInputStream();
        final StringBuilder response = new StringBuilder();
        if (inputStream != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
                String inputLine = "";
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
        }
        return response.toString();
    }

    /**
     * Create a new test case.
     */
    public ScriptTest() {
        super("ScriptTest");
    }

    @Test
    public void testArgScriptFromFile() {
        final File testScript = new File(TEST_ADD);
        final JexlScript s = JEXL.createScript(testScript,"x", "y");
        final JexlContext jc = new MapContext();
        jc.set("out", System.out);
        final Object result = s.execute(jc, 13, 29);
        assertNotNull(result, "No result");
        assertEquals(42, result, "Wrong result");
    }

    @Test
    public void testArgScriptFromURL() throws Exception {
        final URL testUrl = new File(TEST_ADD).toURI().toURL();
        final JexlScript s = JEXL.createScript(testUrl,"x", "y");
        final JexlContext jc = new MapContext();
        jc.set("out", System.out);
        final Object result = s.execute(jc, 13, 29);
        assertNotNull(result, "No result");
        assertEquals(42, result, "Wrong result");
    }

    @Test
    public void testScriptFromFile() {
        final File testScript = new File(TEST1);
        final JexlScript s = JEXL.createScript(testScript);
        final JexlContext jc = new MapContext();
        jc.set("out", System.out);
        final Object result = s.execute(jc);
        assertNotNull(result, "No result");
        assertEquals(7, result, "Wrong result");
    }

    @Test
    public void testScriptFromURL() throws Exception {
        final URL testUrl = new File(TEST1).toURI().toURL();
        final JexlScript s = JEXL.createScript(testUrl);
        final JexlContext jc = new MapContext();
        jc.set("out", System.out);
        final Object result = s.execute(jc);
        assertNotNull(result, "No result");
        assertEquals(7, result, "Wrong result");
    }

    @Test
    public void testScriptJsonFromFileJava() throws IOException {
        HttpServer server = null;
        try {
            final String response = "{  \"id\": 101}";
            server = createJsonServer(h -> response);
            final String url = "http:/" + server.getAddress().toString() + "/test";
            final String testScript = "httpr.execute('" + url + "', null)";
            final JexlScript s = JEXL.createScript(testScript);
            final JexlContext jc = new MapContext();
            jc.set("httpr", new HttpPostRequest());
            final Object result = s.execute(jc);
            assertNotNull(result);
            assertEquals(response, result);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    @Test
    public void testScriptJsonFromFileJexl() throws IOException {
        HttpServer server = null;
        try {
            final String response = "{  \"id\": 101}";
            server = createJsonServer(h -> response);
            final File httprFile = new File(TEST_JSON);
            final JexlScript httprScript = JEXL.createScript(httprFile);
            final JexlContext jc = new MapContext();
            final Object httpr = httprScript.execute(jc);
            final JexlScript s = JEXL.createScript("(httpr,url)->httpr.execute(url, null)");
            // jc.set("httpr", new HttpPostRequest());
            final String url = "http:/" + server.getAddress().toString() + "/test";
            final Object result = s.execute(jc, httpr, url);
            assertNotNull(result);
            assertEquals(response, result);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
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
        assertEquals("OK", resultatJexl.getCode());
        resultatJexl.setCode("");
        s.execute(jc);
        assertEquals("OK", resultatJexl.getCode());
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
        assertEquals(10, o, "Result is wrong");
        assertEquals(code, s.getSourceText(), "getText is wrong");
    }

    /**
     * Test creating a script from spaces.
     */
    @Test
    public void testSpacesScript() {
        final String code = " ";
        final JexlScript s = JEXL.createScript(code);
        assertNotNull(s);
    }

}
