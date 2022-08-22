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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.jexl3.internal.introspection.SimpleClassNameSolver;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

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

    static class ImportContext extends MapContext implements JexlContext.ClassNameResolver, JexlContext.PragmaProcessor {
        private final SimpleClassNameSolver solver;
        ImportContext(SimpleClassNameSolver parent) {
            solver = new SimpleClassNameSolver(parent);
        }

        @Override
        public String resolveClassName(String name) {
            return name.indexOf('.') < 0 ? solver.getQualifiedName(name) : name;
        }

        @Override
        public void processPragma(String key, Object value) {
            processPragma(null, key, value);
        }

        @Override
        public void processPragma(JexlOptions opts, String key, Object value) {
            if ("jexl.import".equals(key)) {
                if (value instanceof Collection<?>) {
                    for(Object pkg : ((Collection<?>) value)) {
                        solver.addPackages(Collections.singletonList(pkg.toString()));
                    }
                } else {
                    solver.addPackages(Collections.singletonList(value.toString()));
                }
            }
        }
    }

    @Test
    public void testScriptJsonFromFileJexl() {
        SimpleClassNameSolver baseSolver = new SimpleClassNameSolver(null, Arrays.asList("java.lang"));
        HttpServer server = null;
        try {
            final String response = "{  \"id\": 101}";
            server = createJsonServer(h -> response);
            final File httprFile = new File(TEST_JSON);
            final JexlScript httprScript = JEXL.createScript(httprFile);
            final JexlContext jc = new ImportContext(baseSolver);
            Object httpr = httprScript.execute(jc);
            final JexlScript s = JEXL.createScript("(httpr,url)->httpr.execute(url, null)");
            //jc.set("httpr", new HttpPostRequest());
            Object result = s.execute(jc, httpr, "http://localhost:8001/test");
            Assert.assertNotNull(result);
            Assert.assertEquals(response, result);
        } catch(IOException xio) {
            Assert.fail(xio.getMessage());
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    @Test
    public void testScriptJsonFromFileJava() {
        HttpServer server = null;
        try {
            final String response = "{  \"id\": 101}";
            server = createJsonServer(h -> response);
            final String testScript = "httpr.execute('http://localhost:8001/test', null)";
            final JexlScript s = JEXL.createScript(testScript);
            final JexlContext jc = new MapContext();
            jc.set("httpr", new HttpPostRequest());
            Object result = s.execute(jc);
            Assert.assertNotNull(result);
            Assert.assertEquals(response, result);
        } catch(IOException xio) {
            Assert.fail(xio.getMessage());
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
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
            con.setRequestProperty("Content-Type", "application/json");
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

    /**
     * Creates a simple local http server.
     * <p>Only handles POST request on /test</p>
     * @return the server
     * @throws IOException
     */
    static HttpServer createJsonServer(final Function<HttpExchange, String> responder) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        server.createContext("/test", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                if ("POST".equals(httpExchange.getRequestMethod())) {
                    OutputStream outputStream = httpExchange.getResponseBody();
                    String json = responder.apply(httpExchange);
                    httpExchange.sendResponseHeaders(200, json.length());
                    outputStream.write(json.toString().getBytes());
                    outputStream.flush();
                    outputStream.close();
                } else {
                    // error
                    httpExchange.sendResponseHeaders(500, 0);
                }
            }
        });
        server.setExecutor(threadPoolExecutor);
        server.start();
        return server;
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
