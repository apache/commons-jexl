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
import org.apache.commons.jexl3.parser.JexlParser;
import org.junit.Assert;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests local variables.
 */
@SuppressWarnings({"UnnecessaryBoxing", "AssertEqualsBetweenInconvertibleTypes"})
public class VarTest extends JexlTestCase {
    static final Log LOGGER = LogFactory.getLog(VarTest.class.getName());
    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public VarTest() {
        super("VarTest");
    }

    @Test
    public void testStrict() throws Exception {
        JexlEvalContext env = new JexlEvalContext();
        JexlOptions options = env.getEngineOptions();
        JexlContext ctxt = new ReadonlyContext(env, options);
        options.setStrict(true);
        options.setSilent(false);
        options.setSafe(false);
        JexlScript e;

        e = JEXL.createScript("x");
        try {
            Object o = e.execute(ctxt);
            Assert.fail("should have thrown an unknown var exception");
        } catch(JexlException xjexl) {
            // ok since we are strict and x does not exist
        }
        e = JEXL.createScript("x = 42");
        try {
            Object o = e.execute(ctxt);
            Assert.fail("should have thrown a readonly context exception");
        } catch(JexlException xjexl) {
            // ok since we are strict and context is readonly
        }

        env.set("x", "fourty-two");
        e = JEXL.createScript("x.theAnswerToEverything()");
        try {
            Object o = e.execute(ctxt);
            Assert.fail("should have thrown an unknown method exception");
        } catch(JexlException xjexl) {
            // ok since we are strict and method does not exist
        }
    }

    @Test
    public void testLocalBasic() throws Exception {
        JexlScript e = JEXL.createScript("var x; x = 42");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not 42", new Integer(42), o);
    }

    @Test
    public void testLocalSimple() throws Exception {
        JexlScript e = JEXL.createScript("var x = 21; x + x");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not 42", new Integer(42), o);
    }

    @Test
    public void testLocalFor() throws Exception {
        JexlScript e = JEXL.createScript("var y  = 0; for(var x : [5, 17, 20]) { y = y + x; } y;");
        Object o = e.execute(null);
        Assert.assertEquals("Result is not 42", new Integer(42), o);
    }

    public static class NumbersContext extends MapContext implements JexlContext.NamespaceResolver {
        @Override
        public Object resolveNamespace(String name) {
            return name == null ? this : null;
        }

        public Object numbers() {
            return new int[]{5, 17, 20};
        }
    }

    @Test
    public void testLocalForFunc() throws Exception {
        JexlContext jc = new NumbersContext();
        JexlScript e = JEXL.createScript("var y  = 0; for(var x : numbers()) { y = y + x; } y;");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not 42", new Integer(42), o);
    }

    @Test
    public void testLocalForFuncReturn() throws Exception {
        JexlContext jc = new NumbersContext();
        JexlScript e = JEXL.createScript("var y  = 42; for(var x : numbers()) { if (x > 10) return x } y;");
        Object o = e.execute(jc);
        Assert.assertEquals("Result is not 17", new Integer(17), o);

        Assert.assertTrue(toString(e.getVariables()), e.getVariables().isEmpty());
    }

    /**
     * Generate a string representation of Set&lt;List&t;String>>, useful to dump script variables
     * @param refs the variable reference set
     * @return  the string representation
     */
    String toString(Set<List<String>> refs) {
        StringBuilder strb = new StringBuilder("{");
        int r = 0;
        for (List<String> strs : refs) {
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

    /**
     * Creates a variable reference set from an array of array of strings.
     * @param refs the variable reference set
     * @return the set of variables
     */
    Set<List<String>> mkref(String[][] refs) {
        Set<List<String>> set = new HashSet<List<String>>();
        for(String[] ref : refs) {
            set.add(Arrays.asList(ref));
        }
        return set;
    }

    /**
     * Checks that two sets of variable references are equal
     * @param lhs the left set
     * @param rhs the right set
     * @return true if equal, false otherwise
     */
    boolean eq(Set<List<String>> lhs, Set<List<String>> rhs) {
        if (lhs.size() != rhs.size()) {
            return false;
        }
        List<String> llhs = stringify(lhs);
        List<String> lrhs = stringify(rhs);
        for(int s = 0; s < llhs.size(); ++s) {
            String l = llhs.get(s);
            String r = lrhs.get(s);
            if (!l.equals(r)) {
                return false;
            }
        }
        return true;
    }

    List<String> stringify(Set<List<String>> sls) {
        List<String> ls = new ArrayList<String>();
        for(List<String> l : sls) {
        StringBuilder strb = new StringBuilder();
        for(String s : l) {
            strb.append(s);
            strb.append('|');
        }
            ls.add(strb.toString());
        }
        Collections.sort(ls);
        return ls;
    }

    @Test
    public void testRefs() throws Exception {
        JexlScript e;
        Set<List<String>> vars;
        Set<List<String>> expect;

        e = JEXL.createScript("a[b]['c']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"},{"b"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("a.'b + c'");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a", "b + c"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("e[f]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e"},{"f"}});
        Assert.assertTrue(eq(expect, vars));


        e = JEXL.createScript("e[f][g]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e"},{"f"},{"g"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("e['f'].goo");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e","f","goo"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("e['f']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e","f"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("e[f]['g']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e"},{"f"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("e['f']['g']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e","f","g"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("a['b'].c['d'].e");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a", "b", "c", "d", "e"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("a + b.c + b.c.d + e['f']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b", "c"}, {"b", "c", "d"}, {"e", "f"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("D[E[F]]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"D"}, {"E"}, {"F"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("D[E[F[G[H]]]]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"D"}, {"E"}, {"F"}, {"G"}, {"H"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript(" A + B[C] + D[E[F]] + x[y[z]] ");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}, {"B"}, {"C"}, {"D"}, {"E"}, {"F"}, {"x"} , {"y"}, {"z"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript(" A + B[C] + D.E['F'] + x[y.z] ");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}, {"B"}, {"C"}, {"D", "E", "F"}, {"x"} , {"y", "z"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("(A)");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("not(A)");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("not((A))");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("a[b]['c']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("a['b'][c]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a", "b"}, {"c"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("a[b].c");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("a[b].c[d]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}, {"d"}});
        Assert.assertTrue(eq(expect, vars));

        e = JEXL.createScript("a[b][e].c[d][f]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}, {"d"}, {"e"}, {"f"}});
        Assert.assertTrue(eq(expect, vars));
    }

    @Test
    public void testVarCollectNotAll() throws Exception {
        JexlScript e;
        Set<List<String>> vars;
        Set<List<String>> expect;
        JexlEngine jexl = new JexlBuilder().strict(true).silent(false).cache(32).collectAll(false).create();

        e = jexl.createScript("a['b'][c]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"c"}});
        Assert.assertTrue(eq(expect, vars));

        e = jexl.createScript(" A + B[C] + D[E[F]] + x[y[z]] ");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"A"}, {"B"}, {"C"}, {"D"}, {"E"}, {"F"}, {"x"} , {"y"}, {"z"}});
        Assert.assertTrue(eq(expect, vars));

        e = jexl.createScript("e['f']['g']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"e"}});
        Assert.assertTrue(eq(expect, vars));

        e = jexl.createScript("a[b][e].c[d][f]");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b"}, {"d"}, {"e"}, {"f"}});
        Assert.assertTrue(eq(expect, vars));

        e = jexl.createScript("a + b.c + b.c.d + e['f']");
        vars = e.getVariables();
        expect = mkref(new String[][]{{"a"}, {"b", "c"}, {"b", "c", "d"}, {"e"}});
        Assert.assertTrue(eq(expect, vars));
    }

    @Test
    public void testMix() throws Exception {
        JexlScript e;
        // x is a parameter, y a context variable, z a local variable
        e = JEXL.createScript("if (x) { y } else { var z = 2 * x}", "x");
        Set<List<String>> vars = e.getVariables();
        String[] parms = e.getParameters();
        String[] locals = e.getLocalVariables();

        Assert.assertTrue(eq(mkref(new String[][]{{"y"}}), vars));
        Assert.assertEquals(1, parms.length);
        Assert.assertEquals("x", parms[0]);
        Assert.assertEquals(1, locals.length);
        Assert.assertEquals("z", locals[0]);
    }

    /**
     * Dates that can return multiple properties in one call.
     */
    public static class VarDate {
        private final Calendar cal;

        public VarDate(String date) throws Exception {
            this(SDF.parse(date));
        }
        public VarDate(Date date) {
            cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTime(date);
            cal.setLenient(true);
        }
        
        /**
         * Gets a date property
         * @param property yyyy or MM or dd
         * @return the string representation of year, month or day
         */
        public String get(String property) {
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
        public List<String> get(String[] keys) {
            return get(Arrays.asList(keys));
        }
               
        /**
         * Gets a list of properties.
         * @param keys the property names
         * @return the property values
         */
        public List<String> get(List<String> keys) {
            List<String> values = new ArrayList<String>();
            for(String key : keys) {
                String value = get(key);
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
        public Map<String,Object> get(Map<String,String> map) {
            Map<String,Object> values = new LinkedHashMap<String,Object>();
            for(Map.Entry<String,String> entry : map.entrySet()) {
                String value = get(entry.getKey());
                if (value != null) {
                    values.put(entry.getValue(), value);
                }
            }
            return values;
        }
    }

    /**
     * Getting properties from an array, set or map.
     * @param str the stringified source
     * @return the properties array
     */
    private static String[] readIdentifiers(String str) {
        List<String> ids = new ArrayList<String>();
        StringBuilder strb = null;
        String id = null;
        char kind = 0; // array, set or map kind using first char
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
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
                || (kind != '{' && c == ',') // array or set
                || (kind == '{' && c == ':')) // map key
            {
                ids.add(id);
                id = null;
            }
            else if (c == '\'' || c == '"') {
                strb.append(c);
                int l = JexlParser.readString(strb, str, i + 1, c);
                if (l > 0) {
                    id = strb.substring(1, strb.length() - 1);
                    strb.delete(0, l + 1);
                    i = l;
                }
            }
            // discard all chars not in identifier
        }
        return ids.toArray(new String[ids.size()]);
    }
    
    @Test
    public void testReferenceLiteral() throws Exception {
        JexlEngine jexld = new JexlBuilder().collectMode(2).create();
        JexlScript script;
        List<String> result;
        Set<List<String>> vars;
        // in collectAll mode, the collector grabs all syntactic variations of
        // constant variable references including map/arry/set literals
        JexlContext ctxt = new MapContext();
        //d.yyyy = 1969; d.MM = 7; d.dd = 20
        ctxt.set("moon.landing", new VarDate("1969-07-20"));
        
        script = jexld.createScript("moon.landing[['yyyy', 'MM', 'dd']]");
        result = (List<String>) script.execute(ctxt);
        Assert.assertEquals(Arrays.asList("1969", "7", "20"), result);
        
        vars = script.getVariables();
        Assert.assertEquals(1, vars.size());
        List<String> var = vars.iterator().next();
        Assert.assertEquals("moon", var.get(0));
        Assert.assertEquals("landing", var.get(1));
        Assert.assertArrayEquals(new String[]{"yyyy", "MM", "dd"}, readIdentifiers(var.get(2)));
        
        script = jexld.createScript("moon.landing[ { 'yyyy' : 'year', 'MM' : 'month', 'dd' : 'day' } ]");
        Map<String, String> mapr = (Map<String, String>) script.execute(ctxt);
        Assert.assertEquals(3, mapr.size());
        Assert.assertEquals("1969", mapr.get("year"));
        Assert.assertEquals("7", mapr.get("month"));
        Assert.assertEquals("20", mapr.get("day"));
      
        vars = script.getVariables();
        Assert.assertEquals(1, vars.size());
        var = vars.iterator().next();
        Assert.assertEquals("moon", var.get(0));
        Assert.assertEquals("landing", var.get(1));
        Assert.assertArrayEquals(new String[]{"yyyy", "MM", "dd"}, readIdentifiers(var.get(2)));
    }

    @Test
    public void testLiteral() throws Exception {
        JexlBuilder builder = new JexlBuilder().collectMode(2);
        Assert.assertEquals(2, builder.collectMode());
        Assert.assertTrue(builder.collectAll());
        
        JexlEngine jexld = builder.create();
        JexlScript e = jexld.createScript("x.y[['z', 't']]");
        Set<List<String>> vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y", "[ 'z', 't' ]"}}), vars));

        e = jexld.createScript("x.y[{'z': 't'}]");
        vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y", "{ 'z' : 't' }"}}), vars));
        
        e = jexld.createScript("x.y.'{ \\'z\\' : \\'t\\' }'");
        vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y", "{ 'z' : 't' }"}}), vars));
        
        // only string or number literals
        builder = builder.collectAll(true);
        Assert.assertEquals(1, builder.collectMode());
        Assert.assertTrue(builder.collectAll());
        
        jexld = builder.create();
        e = jexld.createScript("x.y[{'z': 't'}]");
        vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y"}}), vars));
        
        e = jexld.createScript("x.y[['z', 't']]");
        vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y"}}), vars));
        
        e = jexld.createScript("x.y['z']");
        vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y", "z"}}), vars));
        
        e = jexld.createScript("x.y[42]");
        vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y", "42"}}), vars));
    }

    @Test
    public void testSyntacticVariations() throws Exception {
        JexlScript script = JEXL.createScript("sum(TOTAL) - partial.sum() + partial['sub'].avg() - sum(partial.sub)");
        Set<List<String>> vars = script.getVariables();

        Assert.assertTrue(vars.size() == 3);
    }

    public static class TheVarContext {
        private int x;
        private String color;

        public void setX(int x) {
            this.x = x;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public int getX() {
            return x;
        }

        public String getColor() {
            return color;
        }
    }

    @Test
    public void testObjectContext() throws Exception {
        TheVarContext vars = new TheVarContext();
        JexlContext jc = new ObjectContext<TheVarContext>(JEXL, vars);
        try {
            JexlScript script;
            Object result;
            script = JEXL.createScript("x = 3");
            result = script.execute(jc);
            Assert.assertEquals(3, vars.getX());
            Assert.assertEquals(3, result);
            script = JEXL.createScript("x == 3");
            result = script.execute(jc);
            Assert.assertTrue((Boolean) result);
            Assert.assertTrue(jc.has("x"));

            script = JEXL.createScript("color = 'blue'");
            result = script.execute(jc);
            Assert.assertEquals("blue", vars.getColor());
            Assert.assertEquals("blue", result);
            script = JEXL.createScript("color == 'blue'");
            result = script.execute(jc);
            Assert.assertTrue((Boolean) result);
            Assert.assertTrue(jc.has("color"));
        } catch (JexlException.Method ambiguous) {
            Assert.fail("total() is solvable");
        }
    }

}
