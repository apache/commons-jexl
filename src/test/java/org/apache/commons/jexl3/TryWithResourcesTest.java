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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.StringReader;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests try-with-resources statement.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class TryWithResourcesTest extends JexlTestCase {

    public TryWithResourcesTest() {
        super("TryWithResourcesTest");
    }

    @Test
    public void testLastValue() throws Exception {
        JexlScript e = JEXL.createScript("try (r) {}");
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not last evaluated expression", null, o);
    }

    @Test
    public void testClosed() throws Exception {
        JexlEngine jexl = new JexlBuilder().strict(true).silent(false).create();
        JexlScript e = jexl.createScript("try (r) {}; r.read(); return 42");
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        Object o = null;
        try {
            o = e.execute(jc);
            Assert.fail("should have thrown");
        } catch (JexlException xjexl) {
            Assert.assertNull(o);
        }
    }

    public static class BadStream implements AutoCloseable {

        public BadStream() {
        }

        public BadStream(boolean fail) throws Exception {
            if (fail) 
                throw new Exception("Should not be created");
        }

        @Override
        public void close() throws Exception {
            throw new Exception("Should be ignored");
        }
    }

    @Test
    public void testFinallyAlwaysCalled() throws Exception {

        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        JexlScript e = JEXL.createScript("try (r) {x = 1} finally {x = 42}");
        Object o = e.execute(jc);
        Assert.assertEquals(42, jc.get("x"));

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        e = JEXL.createScript("try (r) {42/0} finally {x = 42}");
        try {
            o = e.execute(jc);
        } catch (Exception ex) {
            Assert.assertEquals(42, jc.get("x"));
        }

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        e = JEXL.createScript("try (r) {42/0} catch (var e) {} finally {x = 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, jc.get("x"));

        jc = new MapContext();
        jc.set("r", new BadStream());

        e = JEXL.createScript("try (r) {} finally {x = 42}");
        try {
            o = e.execute(jc);
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertNull(o);
            Assert.assertEquals(42, jc.get("x"));
        }

        e = JEXL.createScript("try (var r = 42/0) {} finally {x = 42}");
        try {
            o = e.execute(jc);
            Assert.fail("should have thrown");
        } catch (Exception ex) {
            Assert.assertNull(o);
            Assert.assertEquals(42, jc.get("x"));
        }
    }

    @Test
    public void testCatch() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));

        JexlScript e = JEXL.createScript("try (r) {42/0} catch (e) {}");
        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        Object o = e.execute(jc);
        Assert.assertTrue(jc.get("e") instanceof Exception);

        e = JEXL.createScript("try (r) {42/0} catch (var e) {x = e}");
        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        o = e.execute(jc);
        Assert.assertTrue(jc.get("x") instanceof Exception);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("try (r) {return 42} catch (var e) {return 0}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        e = JEXL.createScript("try (r) {return 42} catch(e) {}");
        jc = new MapContext();
        jc.set("r", new BadStream());
        o = e.execute(jc);
        Assert.assertNull(jc.get("e"));

        e = JEXL.createScript("try (r = 42/0) {} catch (var e) {x = e}");
        jc = new MapContext();
        o = e.execute(jc);
        Assert.assertTrue(jc.get("x") instanceof Exception);
    }

    @Test
    public void testReturn() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        JexlScript e = JEXL.createScript("try (r) {return 1} finally {return 42}");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("try (r) {42/0} catch(var e) {return 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("try (r) {42/0} catch(var e) {return 2} finally { return 42}");
        o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testBreakInsideTry() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        JexlScript e = JEXL.createScript("for (var i : 42..43) try (r) {break} finally {}; i");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("for (var i : 42..43) try (r) {} finally {break}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("for (var i : 42..43) try (r) {break} catch(var e) {}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);

        jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        e = JEXL.createScript("for (var i : 42..43) try (r) {42/0} catch(var e) {break}; i");
        o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

    @Test
    public void testContinueInsideTry() throws Exception {
        JexlContext jc = new MapContext();
        jc.set("r", new StringReader("foo"));
        JexlScript e = JEXL.createScript("var i = 0; while (true) { i+=1; try (r) {if (i < 42) continue else break} finally {}}; i");
        Object o = e.execute(jc);
        Assert.assertEquals(42, o);
    }

}
