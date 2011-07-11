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
package org.apache.commons.jexl2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Tests local variables.
 */
public class VarTest extends JexlTestCase {
    Logger LOGGER = Logger.getLogger(VarTest.class.getName());

    public VarTest(String testName) {
        super(testName);
    }

    public void testBasic() throws Exception {
        Script e = JEXL.createScript("var x; x = 42");
        Object o = e.execute(null);
        assertEquals("Result is not 42", new Integer(42), o);
    }

    public void testSimple() throws Exception {
        Script e = JEXL.createScript("var x = 21; x + x");
        Object o = e.execute(null);
        assertEquals("Result is not 42", new Integer(42), o);
    }

    public void testFor() throws Exception {
        Script e = JEXL.createScript("var y  = 0; for(var x : [5, 17, 20]) { y = y + x; } y;");
        Object o = e.execute(null);
        assertEquals("Result is not 42", new Integer(42), o);
    }

    public static class NumbersContext extends MapContext implements NamespaceResolver {
        public Object resolveNamespace(String name) {
            return name == null ? this : null;
        }

        public Object numbers() {
            return new int[]{5, 17, 20};
        }
    }

    public void testForFunc() throws Exception {
        JexlContext jc = new NumbersContext();
        Script e = JEXL.createScript("var y  = 0; for(var x : numbers()) { y = y + x; } y;");
        Object o = e.execute(jc);
        assertEquals("Result is not 42", new Integer(42), o);
    }

    public void testForFuncReturn() throws Exception {
        JexlContext jc = new NumbersContext();
        Script e = JEXL.createScript("var y  = 42; for(var x : numbers()) { if (x > 10) return x } y;");
        Object o = e.execute(jc);
        assertEquals("Result is not 17", new Integer(17), o);
        
        assertTrue(toString(JEXL.getVariables(e)), JEXL.getVariables(e).isEmpty());
    }

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
    
    Set<List<String>> mkref(String[][] refs) {
        Set<List<String>> set = new HashSet<List<String>>();
        for(String[] ref : refs) {
            set.add(Arrays.asList(ref));
        }
        return set;
    }
    
    boolean eq(Set<List<String>> lhs, Set<List<String>> rhs) {
        if (lhs.size() != rhs.size()) {
            return false;
        }
        for(List<String> ref : lhs) {
            if (!rhs.contains(ref)) {
                return false;
            }
        }
        return true;
    }

    public void testRefs() throws Exception {
        Script e;
        Set<List<String>> vars;
        Set<List<String>> expect;

        e = JEXL.createScript("e[f]");
        vars = JEXL.getVariables(e);
        expect = mkref(new String[][]{{"e"},{"f"}});
        assertTrue(eq(expect, vars));
        
        
        e = JEXL.createScript("e[f][g]");
        vars = JEXL.getVariables(e);
        expect = mkref(new String[][]{{"e"},{"f"},{"g"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e['f'].goo");
        vars = JEXL.getVariables(e);
        expect = mkref(new String[][]{{"e","f","goo"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e['f']");
        vars = JEXL.getVariables(e);
        expect = mkref(new String[][]{{"e","f"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("e[f]['g']");
        vars = JEXL.getVariables(e);
        expect = mkref(new String[][]{{"e"},{"f"}});
        assertTrue(eq(expect, vars));
        
        e = JEXL.createScript("e['f']['g']");
        vars = JEXL.getVariables(e);
        expect = mkref(new String[][]{{"e","f","g"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a['b'].c['d'].e");
        vars = JEXL.getVariables(e);
        expect = mkref(new String[][]{{"a", "b", "c", "d", "e"}});
        assertTrue(eq(expect, vars));

        e = JEXL.createScript("a + b.c + b.c.d + e['f']");
        //LOGGER.info(flattenedStr(e));
        vars = JEXL.getVariables(e);
        expect = mkref(new String[][]{{"a"}, {"b", "c"}, {"b", "c", "d"}, {"e", "f"}});
        assertTrue(eq(expect, vars));
    }


}
