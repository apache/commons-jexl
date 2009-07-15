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
package org.apache.commons.jexl;

import junit.framework.TestCase;
/**
 * Test cases for the UnifiedEL.
 */
public class UnifiedJEXLTest extends TestCase {
    static JexlEngine JEXL = new JexlEngine();
    static {
        JEXL.setSilent(false);
    }
    static UnifiedJEXL EL = new UnifiedJEXL(JEXL);
    
    public static class Froboz {
        int value;
        public Froboz(int v) {
            value = v;
        }
        public void setValue(int v) {
            value = v;
        }
        public int getValue() {
            return value;
        }
    }
    
    public static class Quux {
        String str;
        Froboz froboz;
        public Quux(String str, int fro) {
            this.str = str;
            froboz = new Froboz(fro);
        }
        
        public Froboz getFroboz() {
            return froboz;
        }
        
        public void setFroboz(Froboz froboz) {
            this.froboz = froboz;
        }
        
        public String getStr() {
            return str;
        }
        
        public void setStr(String str) {
            this.str = str;
        }
    }

    public UnifiedJEXLTest(String testName) {
        super(testName);
    }


    public void testAssign() throws Exception {
        UnifiedJEXL.Expression assign = EL.parse("${froboz.value = 10}");
        UnifiedJEXL.Expression check = EL.parse("${froboz.value}");
        JexlContext jc = JexlHelper.createContext();
        Object o = assign.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
        o = check.evaluate(jc);
        assertEquals("Result is not 10", new Integer(10), o);
    }
    
    public void testDear() throws Exception {
        UnifiedJEXL.Expression assign = EL.parse("Dear ${p} ${name};");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("p", "Mr");
        jc.getVars().put("name", "Doe");
        Object o = assign.evaluate(jc);
        assertEquals("Dear Mr Doe;", o);
        jc.getVars().put("p", "Ms");
        jc.getVars().put("name", "Jones");
        o = assign.evaluate(jc);
        assertEquals("Dear Ms Jones;", o);
    }

    public void testDear2Phase() throws Exception {
        UnifiedJEXL.Expression assign = EL.parse("Dear #{p} ${name};");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("name", "Doe");
        UnifiedJEXL.Expression  phase1 = assign.prepare(jc);
        jc.getVars().put("p", "Mr");
        jc.getVars().put("name", "Should not be used in 2nd phase");
        Object o = phase1.evaluate(jc);
        assertEquals("Dear Mr Doe;", o);
    }
    
    public void testNested() throws Exception {
        UnifiedJEXL.Expression expr = EL.parse("#{${hi}+'.world'}");
        JexlContext jc = JexlHelper.createContext();
        jc.getVars().put("hi", "hello");
        jc.getVars().put("hello.world", "Hello World!");
        Object o = expr.evaluate(jc);
        assertEquals("Hello World!", o);
    }

    public void testImmediate() throws Exception {
        UnifiedJEXL.Expression expr = EL.parse("${'Hello ' + 'World!'}");
        JexlContext jc = JexlHelper.createContext();
        Object o = expr.evaluate(jc);
        assertEquals("Hello World!", o);
    }

    public void testDeferred() throws Exception {
        UnifiedJEXL.Expression expr = EL.parse("#{'world'}");
        JexlContext jc = JexlHelper.createContext();
        Object o = expr.evaluate(jc);
        assertEquals("world", o);
    }
    
    public void testCharAtBug() throws Exception {
        JexlContext jc = JexlHelper.createContext();

        jc.getVars().put("foo", "abcdef");
        UnifiedJEXL.Expression expr = EL.parse("${foo.substring(2,4)/*comment*/}");
        Object o = expr.evaluate(jc);
        assertEquals("cd", o);
        
        jc.getVars().put("bar", "foo");
        EL.getEngine().setSilent(true);
        expr = EL.parse("#{${bar}+'.charAt(-2)'}");
        expr = expr.prepare(jc);
        o = expr.evaluate(jc);
        assertEquals(null, o);

    }
 
    public static void main(String[] args) throws Exception {
        //new UnifiedELTest("debug").testClassHash();
        new UnifiedJEXLTest("debug").testCharAtBug();
    }
}
