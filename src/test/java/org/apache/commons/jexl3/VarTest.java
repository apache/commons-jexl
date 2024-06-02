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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.jexl3.parser.StringParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

/**
 * Tests local variables.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class VarTest extends JexlTestCase {
    public static class NumbersContext extends MapContext implements JexlContext.NamespaceResolver {
        public Object numbers() {
            return new int[]{5, 17, 20};
        }

        @Override
        public Object resolveNamespace(final String name) {
            return name == null ? this : null;
        }
    }
    public static class TheVarContext {
        private int x;
        private String color;

        public String getColor() {
            return color;
        }

        public int getX() {
            return x;
        }

        public void setColor(final String color) {
            this.color = color;
        }

        public void setX(final int x) {
            this.x = x;
        }
    }
    /**
     * Dates that can return multiple properties in one call.
     */
    public static class VarDate {
        private final Calendar cal;

        public VarDate(final Date date) {
            cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(date);
            cal.setLenient(true);
        }
        public VarDate(final String date) throws Exception {
            this(SDF.parse(date));
        }

        /**
         * Gets a list of properties.
         * @param keys the property names
         * @return the property values
         */
        public List<String> get(final List<String> keys) {
            final List<String> values = new ArrayList<>();
            for(final String key : keys) {
                final String value = get(key);
                if (value != null) {
                    values.add(value);
                }
            }
            return values;
        }

        /**
         * Gets a map of properties.
         * <p>Uses each map key as a property name and each value as an alias
         * used to key the resulting property value.
         * @param map a map of property name to alias
         * @return the alia map
         */
        public Map<String,Object> get(final Map<String,String> map) {
            final Map<String,Object> values = new LinkedHashMap<>();
            for(final Map.Entry<String,String> entry : map.entrySet()) {
                final String value = get(entry.getKey());
                if (value != null) {
                    values.put(entry.getValue(), value);
                }
            }
            return values;
        }

        /**
         * Gets a date property
         * @param property yyyy or MM or dd
         * @return the string representation of year, month or day
         */
        public String get(final String property) {
            if ("yyyy".equals(property)) {
                return Integer.toString(cal.get(Calendar.YEAR));
            }
            if ("MM".equals(property)) {
                return Integer.toString(cal.get(Calendar.MONTH) + 1);
            }

            if ("dd".equals(property)) {
                return Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
            }
            return null;
        }

        /**
         * Gets a list of properties.
         * @param keys the property names
         * @return the property values
         */
        public List<String> get(final String[] keys) {
            return get(Arrays.asList(keys));
        }
    }

    static final Log LOGGER = LogFactory.getLog(VarTest.class.getName());

    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Getting properties from an array, set or map.
     * @param str the stringified source
     * @return the properties array
     */
    private static String[] readIdentifiers(final String str) {
        final List<String> ids = new ArrayList<>();
        StringBuilder strb = null;
        String id = null;
        char kind = 0; // array, set or map kind using first char
        for (int i = 0; i < str.length(); ++i) {
            final char c = str.charAt(i);
            // strb != null when array,set or map deteced
            if (strb == null) {
                if (c == '{' || c == '(' || c == '[') {
                    strb = new StringBuilder();
                    kind = c;
                }
                continue;
            }
            // identifier pending to be added (only add map keys)
            if (id != null && c == ']' || c == ')'
                || kind != '{' && c == ',' // array or set
                || kind == '{' && c == ':') // map key
            {
                ids.add(id);
                id = null;
            }
            else if (c == '\'' || c == '"') {
                strb.append(c);
                final int l = StringParser.readString(strb, str, i + 1, c);
                if (l > 0) {
                    id = strb.substring(1, strb.length() - 1);
                    strb.delete(0, l + 1);
                    i = l;
                }
            }
            // discard all chars not in identifier
        }
        return ids.toArray(new String[0]);
    }

    public VarTest() {
        super("VarTest");
    }

    /**
     * Checks that two sets of variable references are equal
     * @param lhs the left set
     * @param rhs the right set
     * @return true if equal, false otherwise
     */
    boolean eq(final Set<List<String>> lhs, final Set<List<String>> rhs) {
        if (lhs.size() != rhs.size()) {
            return false;
        }
        final List<String> llhs = stringify(lhs);
        final List<String> lrhs = stringify(rhs);
        for(int s = 0; s < llhs.size(); ++s) {
            final String l = llhs.get(s);
            final String r = lrhs.get(s);
            if (!l.equals(r)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a variable reference set from an array of array of strings.
     * @param refs the variable reference set
     * @return the set of variables
     */
    Set<List<String>> mkref(final String[][] refs) {
        final Set<List<String>> set = new HashSet<>();
        for(final String[] ref : refs) {
            set.add(Arrays.asList(ref));
        }
        return set;
    }

    List<String> stringify(final Set<List<String>> sls) {
        final List<String> ls = new ArrayList<>();
        for(final List<String> l : sls) {
        final StringBuilder strb = new StringBuilder();
        for(final String s : l) {
            strb.append(s);
            strb.append('|');
        }
            ls.add(strb.toString());
        }
        Collections.sort(ls);
        return ls;
    }

    @Test
    public void testLiteral() throws Exception {
        JexlBuilder builder = new JexlBuilder().collectMode(2);
        assertEquals(2, builder.collectMode());
        assertTrue(builder.collectAll());

        JexlEngine jexld = builder.create();
        JexlScript e = jexld.createScript("x.y[['z', 't']]");
        Set<List<String>> vars = e.getVariables();
        assertEquals(1, vars.size());
        assertTrue(eq(mkref(new String[][]{{"x", "y", "[ 'z', 't' ]"}}), vars));

        e = jexld.createScript("x.y[{'z': 't'}]");
        vars = e.getVariables();
        assertEquals(1, vars.size());
        assertTrue(eq(mkref(new String[][]{{"x", "y", "{ 'z' : 't' }"}}), vars));

        e = jexld.createScript("x.y.'{ \\'z\\' : \\'t\\' }'");
        vars = e.getVariables();
        assertEquals(1, vars.size());
        assertTrue(eq(mkref(new String[][]{{"x", "y", "{ 'z' : 't' }"}}), vars));

        // only string or number literals
        builder = builder.collectAll(true);
        assertEquals(1, builder.collectMode());
        assertTrue(builder.collectAll());

        jexld = builder.create();
        e = jexld.createScript("x.y[{'z': 't'}]");
        vars = e.getVariables();
        assertEquals(1, vars.size());
        assertTrue(eq(mkref(new String[][]{{"x", "y"}}), vars));

        e = jexld.createScript("x.y[['z', 't']]");
        vars = e.getVariables();
        assertEquals(1, vars.size());
        assertTrue(eq(mkref(new String[][]{{"x", "y"}}), vars));

        e = jexld.createScript("x.y['z']");
        vars = e.getVariables();
        assertEquals(1, vars.size());
        assertTrue(eq(mkref(new String[][]{{"x", "y", "z"}}), vars));

        e = jexld.createScript("x.y[42]");
        vars = e.getVariables();
        assertEquals(1, vars.size());
        assertTrue(eq(mkref(new String[][]{{"x", "y", "42"}}), vars));
    }

    @Test
    public void testLocalBasic() throws Exception {
        final JexlScript e = JEXL.createScript("var x; x = 42");
        final Object o = e.execute(null);
        assertEquals(Integer.valueOf(42), o);
    }

    @Test
    public void testLocalFor() throws Exception {
        final JexlScript e = JEXL.createScript("var y  = 0; for(var x : [5, 17, 20]) { y = y + x; } y;");
        final Object o = e.execute(null);
        assertEquals(Integer.valueOf(42), o);
    }

    @Test
    public void testLocalForFunc() throws Exception {
        final JexlContext jc = new NumbersContext();
        final JexlScript e = JEXL.createScript("var y  = 0; for(var x : numbers()) { y = y + x; } y;");
        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(42), o);
    }

    @Test
    public void testLocalForFuncReturn() throws Exception {
        final JexlContext jc = new NumbersContext();
        final JexlScript e = JEXL.createScript("var y  = 42; for(var x : numbers()) { if (x > 10) return x } y;");
        final Object o = e.execute(jc);
        assertEquals(Integer.valueOf(17), o);

        assertTrue(e.getVariables().isEmpty(), () -> toString(e.getVariables()));
    }

    @Test
    public void testLocalSimple() throws Exception {
        final JexlScript e = JEXL.createScript("var x = 21; x + x");
        final Object o = e.execute(null);
        assertEquals(Integer.valueOf(42), o);
    }

    @Test
    public void testMix() throws Exception {
        JexlScript e;
        // x is a parameter, y a context variable, z a local variable
        e = JEXL.createScript("if (x) { y } else { var z = 2 * x}", "x");
        final Set<List<String>> vars = e.getVariables();
        final String[] parms = e.getParameters();
        final String[] locals = e.getLocalVariables();

        assertTrue(eq(mkref(new String[][]{{"y"}}), vars));
        assertEquals(1, parms.length);
        assertEquals("x", parms[0]);
        assertEquals(1, locals.length);
        assertEquals("z", locals[0]);
    }

    @Test
    public void testObjectContext() throws Exception {
        final TheVarContext vars = new TheVarContext();
        final JexlContext jc = new ObjectContext<>(JEXL, vars);
        try {
            JexlScript script;
            Object result;
            script = JEXL.createScript("x = 3");
            result = script.execute(jc);
            assertEquals(3, vars.getX());
            assertEquals(3, result);
            script = JEXL.createScript("x == 3");
            result = script.execute(jc);
            assertTrue((Boolean) result);
            assertTrue(jc.has("x"));

            script = JEXL.createScript("color = 'blue'");
            result = script.execute(jc);
            assertEquals("blue", vars.getColor());
            assertEquals("blue", result);
            script = JEXL.createScript("color == 'blue'");
            result = script.execute(jc);
            assertTrue((Boolean) result);
            assertTrue(jc.has("color"));
        } catch (final JexlException.Method ambiguous) {
            fail("total() is solvable");
        }
    }

    @Test
    public void testReferenceLiteral() throws Exception {
        final JexlEngine jexld = new JexlBuilder().collectMode(2).create();
        JexlScript script;
        List<String> result;
        Set<List<String>> vars;
        // in collectAll mode, the collector grabs all syntactic variations of
        // constant variable references including map/arry/set literals
        final JexlContext ctxt = new MapContext();
        //d.yyyy = 1969; d.MM = 7; d.dd = 20
        ctxt.set("moon.landing", new VarDate("1969-07-20"));

        script = jexld.createScript("moon.landing[['yyyy', 'MM', 'dd']]");
        result = (List<String>) script.execute(ctxt);
        assertEquals(Arrays.asList("1969", "7", "20"), result);

        vars = script.getVariables();
        assertEquals(1, vars.size());
        List<String> var = vars.iterator().next();
        assertEquals("moon", var.get(0));
        assertEquals("landing", var.get(1));
        assertArrayEquals(new String[]{"yyyy", "MM", "dd"}, readIdentifiers(var.get(2)));

        script = jexld.createScript("moon.landing[ { 'yyyy' : 'year', 'MM' : 'month', 'dd' : 'day' } ]");
        final Map<String, String> mapr = (Map<String, String>) script.execute(ctxt);
        assertEquals(3, mapr.size());
        assertEquals("1969", mapr.get("year"));
        assertEquals("7", mapr.get("month"));
        assertEquals("20", mapr.get("day"));

        vars = script.getVariables();
        assertEquals(1, vars.size());
        var = vars.iterator().next();
        assertEquals("moon", var.get(0));
        assertEquals("landing", var.get(1));
        assertArrayEquals(new String[]{"yyyy", "MM", "dd"}, readIdentifiers(var.get(2)));
    }

    @Test
    public void testRefs() throws Exception {
        JexlScript e;
        Set<List<String>> vars;
        Set<List<String>> expect;

        e = JEXL.createScript("a[b]['c']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"},{"b"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a.'b + c'");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a", "b + c"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e[f]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e"},{"f"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e[f][g]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e"},{"f"},{"g"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e['f'].goo");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e","f","goo"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e['f']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e","f"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e[f]['g']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e"},{"f"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e['f']['g']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e","f","g"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a['b'].c['d'].e");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a", "b", "c", "d", "e"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a + b.c + b.c.d + e['f']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b", "c"}, {"b", "c", "d"}, {"e", "f"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("D[E[F]]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"D"}, {"E"}, {"F"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("D[E[F[G[H]]]]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"D"}, {"E"}, {"F"}, {"G"}, {"H"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript(" A + B[C] + D[E[F]] + x[y[z]] ");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}, {"B"}, {"C"}, {"D"}, {"E"}, {"F"}, {"x"} , {"y"}, {"z"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript(" A + B[C] + D.E['F'] + x[y.z] ");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}, {"B"}, {"C"}, {"D", "E", "F"}, {"x"} , {"y", "z"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("(A)");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("not(A)");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("not((A))");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a[b]['c']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a['b'][c]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a", "b"}, {"c"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a[b].c");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a[b].c[d]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}, {"d"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a[b][e].c[d][f]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}, {"d"}, {"e"}, {"f"}});
        assertTrue(eq(expect, vars));
    }

    @Test
    public void testStrict() throws Exception {
        final JexlEvalContext env = new JexlEvalContext();
        final JexlOptions options = env.getEngineOptions();
        final JexlContext ctxt = new ReadonlyContext(env, options);
        options.setStrict(true);
        options.setSilent(false);
        options.setSafe(false);

        JexlScript e0 = JEXL.createScript("x");
        assertThrows(JexlException.class, () -> e0.execute(ctxt), "should have thrown an unknown var exception");

        JexlScript e1 = JEXL.createScript("x = 42");
        assertThrows(JexlException.class, () -> e1.execute(ctxt), "should have thrown a readonly context exception");

        env.set("x", "fourty-two");
        JexlScript e2 = JEXL.createScript("x.theAnswerToEverything()");
        assertThrows(JexlException.class, () -> e2.execute(ctxt), "should have thrown an unknown method exception");
    }

    @Test
    public void testSyntacticVariations() throws Exception {
        final JexlScript script = JEXL.createScript("sum(TOTAL) - partial.sum() + partial['sub'].avg() - sum(partial.sub)");
        final Set<List<String>> vars = script.getVariables();

        assertEquals(3, vars.size());
    }

    @Test
    public void testVarCollectNotAll() throws Exception {
        JexlScript e;
        Set<List<String>> vars;
        Set<List<String>> expect;
        final JexlEngine jexl = new JexlBuilder().strict(true).silent(false).cache(32).collectAll(false).create();

        e = jexl.createScript("a['b'][c]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"c"}});
        assertTrue(eq(expect, vars));

        e = jexl.createScript(" A + B[C] + D[E[F]] + x[y[z]] ");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}, {"B"}, {"C"}, {"D"}, {"E"}, {"F"}, {"x"} , {"y"}, {"z"}});
        assertTrue(eq(expect, vars));

        e = jexl.createScript("e['f']['g']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e"}});
        assertTrue(eq(expect, vars));

        e = jexl.createScript("a[b][e].c[d][f]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}, {"d"}, {"e"}, {"f"}});
        assertTrue(eq(expect, vars));

        e = jexl.createScript("a + b.c + b.c.d + e['f']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b", "c"}, {"b", "c", "d"}, {"e"}});
        assertTrue(eq(expect, vars));
    }

    /**
     * Generate a string representation of Set&lt;List&t;String>>, useful to dump script variables
     * @param refs the variable reference set
     * @return  the string representation
     */
    String toString(final Set<List<String>> refs) {
        final StringBuilder strb = new StringBuilder("{");
        int r = 0;
        for (final List<String> strs : refs) {
            if (r++ > 0) {
                strb.append(", ");
            }
            strb.append("{");
            for (int s = 0; s < strs.size(); ++s) {
                if (s > 0) {
                    strb.append(", ");
                }
                strb.append('"');
                strb.append(strs.get(s));
                strb.append('"');
            }
            strb.append("}");
        }
        strb.append("}");
        return strb.toString();
    }

}
