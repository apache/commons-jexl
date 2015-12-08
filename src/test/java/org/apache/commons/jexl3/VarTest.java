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

    public VarTest() {
        super("VarTest");
    }

    @Test
    public void testStrict() throws Exception {
        JexlEvalContext env = new JexlEvalContext();
        JexlContext ctxt = new ReadonlyContext(env, env);
        env.setStrict(true);
        env.setSilent(false);
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

    @Test
    public void testLiteral() throws Exception {
        JexlScript e = JEXL.createScript("x.y[['z', 't']]");
        Set<List<String>> vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y", "[ 'z', 't' ]"}}), vars));

        e = JEXL.createScript("x.y[{'z': 't'}]");
        vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y", "{ 'z' : 't' }"}}), vars));
        e = JEXL.createScript("x.y.'{ \\'z\\' : \\'t\\' }'");
        vars = e.getVariables();
        Assert.assertEquals(1, vars.size());
        Assert.assertTrue(eq(mkref(new String[][]{{"x", "y", "{ 'z' : 't' }"}}), vars));
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
